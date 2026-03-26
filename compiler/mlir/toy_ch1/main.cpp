#include "Dialect.h"
#include "MLIRGen.h"
#include "Parser.h"
#include "Passes.h"

#include "mlir/Dialect/Affine/Passes.h"
#include "mlir/ExecutionEngine/ExecutionEngine.h"
#include "mlir/ExecutionEngine/OptUtils.h"
#include "mlir/IR/AsmState.h"
#include "mlir/IR/BuiltinOps.h"
#include "mlir/IR/MLIRContext.h"
#include "mlir/IR/Verifier.h"
#include "mlir/InitAllDialects.h"
#include "mlir/Parser/Parser.h"
#include "mlir/Pass/Pass.h"
#include "mlir/Pass/PassManager.h"
#include "mlir/Target/LLVMIR/Dialect/LLVMIR/LLVMToLLVMIRTranslation.h"
#include "mlir/Target/LLVMIR/Export.h"
#include "mlir/Transforms/Passes.h"

#include "llvm/ADT/StringRef.h"
#include "llvm/IR/Module.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/Support/ErrorOr.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/SourceMgr.h"
#include "llvm/Support/TargetSelect.h"
#include "llvm/Support/raw_ostream.h"

#include <iostream>

using namespace  toy;
namespace cl = llvm::cl;

static cl::opt<std::string> inputFilename(cl::Positional,
                                          cl::desc("<input toy file>"),
                                          cl::init("-"),
                                          cl::value_desc("filename"));

namespace {
enum Action {
  None,
  DumpAST,
  DumpMLIR,
  DumpMLIRAffine,
  DumpMLIRLLVM,
  DumpLLVMIR,
  RunJIT
};
}

static cl::opt<enum Action>
  emitAction("emit", cl::desc("Select the kind of output desired"),
             cl::values(clEnumValN(DumpAST, "ast", "Dump the AST of the input file"),
                        clEnumValN(DumpMLIR, "mlir", "output the MLIR dump"),
                        clEnumValN(DumpMLIRAffine, "affine", "output the MLIR dump after affine lowering"),
                        clEnumValN(DumpMLIRLLVM, "mlir-llvm", "output the MLIR dump after llvm lowering"),
                        clEnumValN(DumpLLVMIR, "llvm", "output the LLVM IR dump"),
                        clEnumValN(RunJIT, "jit", "JIT the code and run it by invoking the main function")));

static cl::opt<bool> enableOpt("opt", cl::desc("Enable optimizations"));

namespace {
enum InputType { Toy, MLIR };
} // namespace
static cl::opt<enum InputType> inputType(
    "x", cl::init(Toy), cl::desc("Decided the kind of output desired"),
    cl::values(clEnumValN(Toy, "toy", "load the input file as a Toy source.")),
    cl::values(clEnumValN(MLIR, "mlir",
                          "load the input file as an MLIR file")));

std::unique_ptr<toy::ModuleAST> parseInputFile(const std::string &filename) {
  llvm::ErrorOr<std::unique_ptr<llvm::MemoryBuffer>> fileOrErr =
      llvm::MemoryBuffer::getFileOrSTDIN(filename);
  if (std::error_code ec = fileOrErr.getError()) {
    llvm::errs() << "Error reading file: " << ec.message() << "\n";
    return nullptr;
  }

  auto buffer = fileOrErr.get()->getBuffer();
  LexerBuffer lexer(buffer.begin(), buffer.end(), std::string(filename));
  Parser parser(lexer);
  return parser.parseModule();
}

int dumpLLVMIR(mlir::ModuleOp module) {
  // Register the translation to LLVM IR with the MLIR context.
  mlir::registerLLVMDialectTranslation(*module->getContext());

  // Convert the module to LLVM IR in a new LLVM IR context.
  llvm::LLVMContext llvmContext;
  auto llvmModule = mlir::translateModuleToLLVMIR(module, llvmContext);
  if (!llvmModule) {
    llvm::errs() << "Failed to emit LLVM IR\n";
    return -1;
  }

  // Initialize LLVM targets.
  llvm::InitializeNativeTarget();
  llvm::InitializeNativeTargetAsmPrinter();
  mlir::ExecutionEngine::setupTargetTriple(llvmModule.get());

  /// Optionally run an optimization pipeline over the llvm module.
  auto optPipeline = mlir::makeOptimizingTransformer(
      /*optLevel=*/enableOpt ? 3 : 0, /*sizeLevel=*/0,
      /*targetMachine=*/nullptr);
  if (auto err = optPipeline(llvmModule.get())) {
    llvm::errs() << "Failed to optimize LLVM IR " << err << "\n";
    return -1;
  }
  llvm::errs() << *llvmModule << "\n";
  return 0;
}

int runJit(mlir::ModuleOp module) {
  // Initialize LLVM targets.
  llvm::InitializeNativeTarget();
  llvm::InitializeNativeTargetAsmPrinter();

  // Register the translation from MLIR to LLVM IR, which must happen before we
  // can JIT-compile.
  mlir::registerLLVMDialectTranslation(*module->getContext());

  // An optimization pipeline to use within the execution engine.
  auto optPipeline = mlir::makeOptimizingTransformer(
      /*optLevel=*/enableOpt ? 3 : 0, /*sizeLevel=*/0,
      /*targetMachine=*/nullptr);

  // Create an MLIR execution engine. The execution engine eagerly JIT-compiles
  // the module.
  mlir::ExecutionEngineOptions engineOptions;
  engineOptions.transformer = optPipeline;
  auto maybeEngine = mlir::ExecutionEngine::create(module, engineOptions);
  assert(maybeEngine && "failed to construct an execution engine");
  auto &engine = maybeEngine.get();

  // Invoke the JIT-compiled function.
  auto invocationResult = engine->invokePacked("main");
  if (invocationResult) {
    llvm::errs() << "JIT invocation failed\n";
    return -1;
  }

  return 0;
}

int dumpAST() {
  if (inputType == InputType::MLIR) {
    llvm::errs() << "Can't dump a Toy AST when the input is MLIR\n";
    return 5;
  }

  auto moduleAST = parseInputFile(inputFilename);
  if (!moduleAST)
    return 1;

  dump(*moduleAST);
  return 0;
}

int dumpMLIR() {
  mlir::MLIRContext context;
  context.getOrLoadDialect<mlir::toy::ToyDialect>();
  // Check to see what granularity of MLIR we are compiling to.
  bool isLoweringToAffine = emitAction >= Action::DumpMLIRAffine;
  bool isLoweringToLLVM = emitAction >= Action::DumpMLIRLLVM;

  // Handle '.toy' input to the compiler.
  if (inputType != InputType::MLIR &&
      !llvm::StringRef(inputFilename).endswith(".mlir")) {
    auto moduleAST = parseInputFile(inputFilename);
    if (!moduleAST)
      return 6;
    mlir::OwningOpRef<mlir::ModuleOp> module = mlirGen(context, *moduleAST);
    if (!module)
      return 1;

    mlir::PassManager pm(&context);
    if (enableOpt || isLoweringToAffine) {
      // Apply any generic pass manager command line options and run the pipeline.
      applyPassManagerCLOptions(pm);

      // Inline all functions into main and then delete them.
      pm.addPass(mlir::createInlinerPass());

      mlir::OpPassManager &optPM = pm.nest<mlir::toy::FuncOp>();
      optPM.addPass(mlir::toy::createShapeInferencePass());
      optPM.addPass(mlir::createCanonicalizerPass());
      optPM.addPass(mlir::createCSEPass());
    }
    if (isLoweringToAffine) {
      // Partially lower the toy dialect.
      pm.addPass(mlir::toy::createLowerToAffinePass());

      // Add a few cleanups post lowering.
      mlir::OpPassManager &optPM = pm.nest<mlir::func::FuncOp>();
      optPM.addPass(mlir::createCanonicalizerPass());
      optPM.addPass(mlir::createCSEPass());

      // Add optimizations if enabled.
      if (enableOpt) {
        optPM.addPass(mlir::createLoopFusionPass());
        optPM.addPass(mlir::createAffineScalarReplacementPass());
      }
    }
    if (isLoweringToLLVM) {
      // Finish lowering the toy IR to the LLVM dialect.
      pm.addPass(mlir::toy::createLowerToLLVMPass());
   }
    if (mlir::failed(pm.run(*module)))
      return 4;
    if (emitAction == Action::DumpLLVMIR) {
      return dumpLLVMIR(*module);
    }
    if (emitAction == Action::RunJIT) {
      return runJit(*module);
    }
    module->dump();
    return 0;
  }

  // Otherwise, the input is '.mlir'.
  llvm::ErrorOr<std::unique_ptr<llvm::MemoryBuffer>> fileOrErr =
      llvm::MemoryBuffer::getFileOrSTDIN(inputFilename);
  if (std::error_code ec = fileOrErr.getError()) {
    llvm::errs() << "Could not open input file: " << ec.message() << "\n";
    return -1;
  }

  // Parse the input mlir.
  llvm::SourceMgr sourceMgr;
  sourceMgr.AddNewSourceBuffer(std::move(*fileOrErr), llvm::SMLoc());
  mlir::OwningOpRef<mlir::ModuleOp> module =
      mlir::parseSourceFile<mlir::ModuleOp>(sourceMgr, &context);
  if (!module) {
    llvm::errs() << "Error can't load file " << inputFilename << "\n";
    return 3;
  }

  mlir::PassManager pm(&context);

  if (enableOpt || isLoweringToAffine) {
      // Apply any generic pass manager command line options and run the pipeline.
      applyPassManagerCLOptions(pm);

      // Inline all functions into main and then delete them.
      pm.addPass(mlir::createInlinerPass());

      // Add a run of the canonicalizer to optimize the mlir module.
      pm.addNestedPass<mlir::toy::FuncOp>(mlir::createCanonicalizerPass());
      if (mlir::failed(pm.run(*module)))
        return 4;
  }
  if (isLoweringToAffine) {
    // Partially lower the toy dialect.
    pm.addPass(mlir::toy::createLowerToAffinePass());
     // Add a few cleanups post lowering.
    mlir::OpPassManager &optPM = pm.nest<mlir::func::FuncOp>();
    optPM.addPass(mlir::createCanonicalizerPass());
    optPM.addPass(mlir::createCSEPass());

    // Add optimizations if enabled.
    if (enableOpt) {
      optPM.addPass(mlir::createLoopFusionPass());
      optPM.addPass(mlir::createAffineScalarReplacementPass());
    }
  }
  if (isLoweringToLLVM) {
    // Finish lowering the toy IR to the LLVM dialect.
    pm.addPass(mlir::toy::createLowerToLLVMPass());
  }
  if (emitAction == Action::DumpLLVMIR) {
    return dumpLLVMIR(*module);
  }
  module->dump();
  return 0;
}

int main(int argc, char** argv) {
    cl::ParseCommandLineOptions(argc, argv, "Toy Compiler - Chapter 1\n");
    switch (emitAction) {
    case Action::DumpAST:
      return dumpAST();
    case Action::DumpMLIR:
    case Action::DumpMLIRAffine:
    case Action::DumpMLIRLLVM:
    case Action::DumpLLVMIR:
    case Action::RunJIT:
      return dumpMLIR();

    default:
      llvm::errs() << "No action specified (parsing only?), use -emit=<action>\n";
    }
    std::cout << "Hello, Toy Compiler!" << std::endl;
    return 0;
}