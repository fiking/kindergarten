/*
 * 扩展示例：如何扩展简单编译器
 *
 * 本文件展示了如何修改编译器来支持更多功能。
 * 这只是示例代码，不需要在编译中包含。
 */

// ==================== 示例 1: 添加支持更多操作符 ====================
/*
在 BinaryExprAST::codegen 中，添加新的 case 语句：

case '%':
    return Builder.CreateFRem(L, R, "modtmp");    // 浮点模运算
case '^':
    // 实现幂运算（需要调用库函数 pow）
    ...
*/

// ==================== 示例 2: 支持一元操作符 ====================
/*
class UnaryExprAST : public ExprAST {
    char Op;
    std::unique_ptr<ExprAST> Operand;
public:
    UnaryExprAST(char Op, std::unique_ptr<ExprAST> Operand)
        : Op(Op), Operand(std::move(Operand)) {}

    char getOp() const { return Op; }
    ExprAST *getOperand() { return Operand.get(); }
};

// 在 Parser 中处理一元操作符
std::unique_ptr<ExprAST> parseUnary() {
    if (CurTok == '-' || CurTok == '+') {
        char Op = CurTok;
        getNextToken();
        auto Operand = parseUnary();
        return std::make_unique<UnaryExprAST>(Op, std::move(Operand));
    }
    return parsePrimary();
}

// 在 CodeGen 中生成代码
case '-':
    Result = Builder.CreateFNeg(Operand, "negtmp");
    break;
*/

// ==================== 示例 3: 添加函数调用支持 ====================
/*
class CallExprAST : public ExprAST {
    std::string Callee;
    std::vector<std::unique_ptr<ExprAST>> Args;
public:
    CallExprAST(const std::string &Callee,
                std::vector<std::unique_ptr<ExprAST>> Args)
        : Callee(Callee), Args(std::move(Args)) {}
};

// 使用示例：
// sin(a) + cos(b)
// sqrt(a * b)
*/

// ==================== 示例 4: 支持变量定义和赋值 ====================
/*
class VarExprAST : public ExprAST {
    std::vector<std::pair<std::string, std::unique_ptr<ExprAST>>> VarNames;
    std::unique_ptr<ExprAST> Body;
public:
    // var a = 5, b = 3 in a + b
};
*/

// ==================== 示例 5: 条件表达式 ====================
/*
class IfExprAST : public ExprAST {
    std::unique_ptr<ExprAST> Cond, ThenExpr, ElseExpr;
public:
    IfExprAST(std::unique_ptr<ExprAST> Cond,
              std::unique_ptr<ExprAST> ThenExpr,
              std::unique_ptr<ExprAST> ElseExpr)
        : Cond(std::move(Cond)),
          ThenExpr(std::move(ThenExpr)),
          ElseExpr(std::move(ElseExpr)) {}
};

// 使用示例：
// if (a > 5) then a else b
*/

// ==================== 示例 6: 循环表达式 ====================
/*
class ForExprAST : public ExprAST {
    std::string VarName;
    std::unique_ptr<ExprAST> Start, End, Step, Body;
public:
    // for i = 1, i < 10, 1.0 in ...
};
*/

// ==================== 示例 7: 编译到文件 ====================
/*
#include "llvm/Support/FileSystem.h"
#include "llvm/Target/TargetMachine.h"
#include "llvm/CodeGen/CodeGenFileTypes.h"

// 生成 LLVM IR 文件
void EmitLLVMIR(Module *M, const std::string &Filename) {
    std::error_code EC;
    raw_fd_ostream dest(Filename, EC, sys::fs::OF_Text);
    if (EC) {
        errs() << "Could not open file: " << EC.message();
        return;
    }
    M->print(dest, nullptr);
    dest.close();
}

// 生成目标代码
void EmitObjectCode(Module *M, const std::string &Filename) {
    auto TargetTriple = sys::getDefaultTargetTriple();
    InitializeAllTargets();
    InitializeAllTargetMCs();
    InitializeAllAsmPrinters();

    std::string Error;
    auto Target = TargetRegistry::lookupTarget(TargetTriple, Error);

    auto TargetMachine = Target->createTargetMachine(
        TargetTriple, "generic", "", TargetOptions(), None);
    M->setDataLayout(TargetMachine->createDataLayout());
    M->setTargetTriple(TargetTriple);

    std::error_code EC;
    raw_fd_ostream dest(Filename, EC, sys::fs::OF_None);

    legacy::PassManager pass;
    TargetMachine->addPassesToEmitFile(
        pass, dest, nullptr, CGFT_ObjectFile);
    pass.run(*M);
    dest.close();
}
*/

// ==================== 示例 8: 命令行参数处理 ====================
/*
#include "llvm/Support/CommandLine.h"

static cl::opt<std::string> InputExpr(
    cl::Positional,
    cl::desc("<expression to compile>"),
    cl::Required);

static cl::opt<std::string> OutputFile(
    "o", cl::desc("Specify output file"), cl::ValueRequired);

static cl::opt<bool> EmitIR(
    "emit-ir", cl::desc("Emit LLVM IR instead of running"));

// 使用示例：
// ./compiler "a + b" -o result.ll --emit-ir
// ./compiler "a * b"
*/

// ==================== 示例 9: 优化 ====================
/*
#include "llvm/Passes/PassBuilder.h"
#include "llvm/Analysis/TargetTransformInfo.h"

void OptimizeModule(Module *M) {
    PassBuilder PB;
    LoopAnalysisManager LAM;
    FunctionAnalysisManager FAM;
    CGSCCAnalysisManager CGAM;
    ModuleAnalysisManager MAM;

    PB.registerLoopAnalyses(LAM);
    PB.registerFunctionAnalyses(FAM);
    PB.registerCGSCCAnalyses(CGAM);
    PB.registerModuleAnalyses(MAM);

    auto OptLevel = PassBuilder::OptimizationLevel::O2;
    ModulePassManager MPM = PB.buildPerModuleDefaultPipeline(OptLevel);
    MPM.run(*M, MAM);
}
*/

// ==================== 示例 10: 调试信息 ====================
/*
在生成代码时添加调试信息：

DIBuilder DIB(*Module);

auto File = DIB.createFile("<string>", ".");
auto CU = DIB.createCompileUnit(
    dwarf::DW_LANG_C, File, "LLVM Compiler", false, "", 0);

// 为每个表达式添加位置信息
DIScope *Scope = CU;
auto DI_Loc = DIB.createDebugLoc(1, 0, Scope);

// 在创建指令时附加调试信息
auto inst = Builder.CreateFAdd(L, R, "addtmp");
inst->setDebugLoc(DI_Loc);
*/

// ==================== 示例 11: 测试框架 ====================
/*
// 测试编译器
struct TestCase {
    std::string expression;
    double expected_result;
};

TestCase test_cases[] = {
    {"a + b", 8.0},      // 5 + 3
    {"a - b", 2.0},      // 5 - 3
    {"a * b", 15.0},     // 5 * 3
    {"a / b", 1.66666},  // 5 / 3
    {"(a + b) * 2", 16.0}, // (5 + 3) * 2
};

void run_tests() {
    for (auto &test : test_cases) {
        // 编译和执行 test.expression
        // 验证结果等于 test.expected_result
    }
}
*/

// ==================== 示例 12: 性能优化 ====================
/*
基于LLVM的优化选项：

1. 常数折叠（Constant Folding）
   - 在编译时计算常数表达式

2. 死代码消除（Dead Code Elimination）
   - 移除未使用的变量和表达式

3. 内联（Inlining）
   - 将小函数内联到调用者中

4. 循环展开（Loop Unrolling）
   - 展开循环以改进性能

5. 向量化（Vectorization）
   - 使用SIMD指令处理数据

使用LLVM的 PassManager 应用这些优化：

legacy::FunctionPassManager FPM(Module);
FPM.add(createPromoteMemoryToRegisterPass());
FPM.add(createInstructionCombiningPass());
FPM.add(createReassociatePass());
FPM.add(createGVNPass());
FPM.add(createCFGSimplificationPass());
FPM.doInitialization();
for (auto &F : *Module) FPM.run(F);
*/

// ==================== 额外资源 ====================
/*
推荐阅读：

1. Kaleidoscope Tutorial
   https://llvm.org/docs/tutorial/MyFirstLanguageFrontend/

2. LLVM Language Reference Manual
   https://llvm.org/docs/LangRef/

3. LLVM Programmer's Manual
   https://llvm.org/docs/ProgrammersManual/

4. LLVM Developer Group
   https://llvm.org/
*/
