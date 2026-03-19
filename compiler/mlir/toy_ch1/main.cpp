#include "llvm/IR/LLVMContext.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/Type.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Verifier.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/ADT/StringRef.h"

#include <memory>
#include <string>
#include <cctype>
#include <map>
#include <vector>
#include <iostream>
#include <cerrno>
#include <iostream>

using namespace llvm;

// ======================== AST节点定义 ========================

class ExprAST {
public:
    virtual ~ExprAST() {}
};

// 数字表达式，例如 1, 2.0
class NumberExprAST : public ExprAST {
    double Val;

public:
    NumberExprAST(double Val) : Val(Val) {}
    double getVal() const { return Val; }
};

// 变量表达式，例如 a, b
class VariableExprAST : public ExprAST {
    std::string Name;

public:
    VariableExprAST(const std::string &Name) : Name(Name) {}
    const std::string &getName() const { return Name; }
};

// 二元运算表达式，例如 a + b
class BinaryExprAST : public ExprAST {
    char Op;
    std::unique_ptr<ExprAST> LHS, RHS;

public:
    BinaryExprAST(char Op, std::unique_ptr<ExprAST> LHS,
                  std::unique_ptr<ExprAST> RHS)
        : Op(Op), LHS(std::move(LHS)), RHS(std::move(RHS)) {}

    char getOp() const { return Op; }
    ExprAST *getLHS() { return LHS.get(); }
    ExprAST *getRHS() { return RHS.get(); }
};

// ======================== 词法分析器（Lexer） ========================

class Lexer {
    std::string Input;
    size_t Pos;

public:
    enum Token {
        tok_eof = -1,
        tok_number = -2,
        tok_identifier = -3,
        tok_plus = '+',
        tok_minus = '-',
        tok_star = '*',
        tok_slash = '/',
        tok_lparen = '(',
        tok_rparen = ')'
    };

    Lexer(const std::string &Input) : Input(Input), Pos(0) {}

    Token getTok(std::string &Identifier, double &NumVal) {
        // 跳过空白字符
        while (Pos < Input.length() && std::isspace(Input[Pos])) {
            Pos++;
        }

        if (Pos >= Input.length()) {
            return tok_eof;
        }

        char CurChar = Input[Pos];

        // 标识符 [a-zA-Z][a-zA-Z0-9]*
        if (std::isalpha(CurChar)) {
            Identifier = "";
            while (Pos < Input.length() && std::isalnum(Input[Pos])) {
                Identifier += Input[Pos++];
            }
            return tok_identifier;
        }

        // 数字 [0-9.]+
        if (std::isdigit(CurChar) || CurChar == '.') {
            std::string NumStr = "";
            while (Pos < Input.length() && (std::isdigit(Input[Pos]) || Input[Pos] == '.')) {
                NumStr += Input[Pos++];
            }
            NumVal = std::stod(NumStr);
            return tok_number;
        }

        // 单字符token
        Token result = static_cast<Token>(CurChar);
        Pos++;
        return result;
    }
};

// ======================== 语法分析器（Parser） ========================

class Parser {
    Lexer &lexer;
    Lexer::Token CurTok;
    std::string Identifier;
    double NumVal;

    Lexer::Token getNextToken() {
        CurTok = lexer.getTok(Identifier, NumVal);
        return CurTok;
    }

public:
    Parser(Lexer &lexer) : lexer(lexer) {
        getNextToken();
    }

    // primary
    //   ::= identifier
    //   ::= number
    //   ::= '(' expression ')'
    std::unique_ptr<ExprAST> parsePrimary() {
        if (CurTok == Lexer::tok_identifier) {
            std::string IdName = Identifier;
            getNextToken();
            return std::make_unique<VariableExprAST>(IdName);
        }

        if (CurTok == Lexer::tok_number) {
            double NumValue = NumVal;
            getNextToken();
            return std::make_unique<NumberExprAST>(NumValue);
        }

        if (CurTok == Lexer::tok_lparen) {
            getNextToken();
            auto E = parseExpression();
            if (CurTok != Lexer::tok_rparen) {
                std::cerr << "Expected ')'\n";
                return nullptr;
            }
            getNextToken();
            return E;
        }

        return nullptr;
    }

    // binoprhs
    //   ::= ('+' primary)*
    std::unique_ptr<ExprAST> parseBinOpRHS(int ExprPrec,
                                            std::unique_ptr<ExprAST> LHS) {
        while (true) {
            int TokPrec = getTokPrecedence();

            if (TokPrec < ExprPrec)
                return LHS;

            char BinOp = CurTok;
            getNextToken();

            auto RHS = parsePrimary();
            if (!RHS)
                return nullptr;

            int NextPrec = getTokPrecedence();
            if (TokPrec < NextPrec) {
                RHS = parseBinOpRHS(TokPrec + 1, std::move(RHS));
                if (!RHS)
                    return nullptr;
            }

            LHS =
                std::make_unique<BinaryExprAST>(BinOp, std::move(LHS), std::move(RHS));
        }
    }

    // expression
    //   ::= primary binoprhs
    std::unique_ptr<ExprAST> parseExpression() {
        auto LHS = parsePrimary();
        if (!LHS)
            return nullptr;

        return parseBinOpRHS(0, std::move(LHS));
    }

private:
    int getTokPrecedence() {
        if (CurTok == Lexer::tok_plus || CurTok == Lexer::tok_minus)
            return 20;
        if (CurTok == Lexer::tok_star || CurTok == Lexer::tok_slash)
            return 40;
        return -1;
    }
};

// ======================== 代码生成器（CodeGen） ========================

class CodeGen {
public:
    LLVMContext &Context;
    std::unique_ptr<llvm::Module> Module;
    IRBuilder<> Builder;
    std::map<std::string, Value *> NamedValues;
    std::map<std::string, double> Variables;

public:
    CodeGen(LLVMContext &Context, const std::string &ModuleName)
        : Context(Context), Module(std::make_unique<llvm::Module>(ModuleName, Context)),
          Builder(Context) {}

    llvm::Module *getModule() { return Module.get(); }

    void setVariables(const std::map<std::string, double> &Vars) {
        Variables = Vars;
    }

    Value *codegen(ExprAST *Expr) {
        if (auto *E = dynamic_cast<NumberExprAST *>(Expr)) {
            return ConstantFP::get(Builder.getDoubleTy(), E->getVal());
        }

        if (auto *E = dynamic_cast<VariableExprAST *>(Expr)) {
            Value *V = NamedValues[E->getName()];
            if (!V) {
                // 如果变量不存在，创建一个新的分配
                double val = Variables[E->getName()];
                V = ConstantFP::get(Builder.getDoubleTy(), val);
            }
            return V;
        }

        if (auto *E = dynamic_cast<BinaryExprAST *>(Expr)) {
            Value *L = codegen(E->getLHS());
            Value *R = codegen(E->getRHS());

            if (!L || !R)
                return nullptr;

            switch (E->getOp()) {
                case '+':
                    return Builder.CreateFAdd(L, R, "addtmp");
                case '-':
                    return Builder.CreateFSub(L, R, "subtmp");
                case '*':
                    return Builder.CreateFMul(L, R, "multmp");
                case '/':
                    return Builder.CreateFDiv(L, R, "divtmp");
                default:
                    std::cerr << "Unknown operator\n";
                    return nullptr;
            }
        }

        return nullptr;
    }
};

// ======================== 主程序 ========================

int main(int argc, char *argv[]) {
    // 创建LLVM上下文
    LLVMContext Context;

    // 输入表达式
    std::string Input = "a + b";
    std::cout << "========== Simple LLVM Compiler ==========\n";
    std::cout << "Expression: " << Input << "\n";
    std::cout << "Variables: a = 5.0, b = 3.0\n\n";

    // 词法分析
    Lexer lexer(Input);

    // 语法分析
    Parser parser(lexer);
    auto ast = parser.parseExpression();

    if (!ast) {
        std::cerr << "Failed to parse expression\n";
        return 1;
    }

    // 代码生成
    CodeGen codegen(Context, "simple_compiler");

    // 设置变量值
    std::map<std::string, double> vars;
    vars["a"] = 5.0;
    vars["b"] = 3.0;
    codegen.setVariables(vars);

    // 生成用于表达式计算的函数
    FunctionType *FT = FunctionType::get(Type::getDoubleTy(Context), false);
    Function *F = Function::Create(FT, Function::ExternalLinkage, "compute",
                                    codegen.getModule());

    // 创建基本块
    BasicBlock *BB = BasicBlock::Create(Context, "entry", F);
    codegen.Builder.SetInsertPoint(BB);

    // 生成代码
    Value *result = codegen.codegen(ast.get());
    if (!result) {
        std::cerr << "Failed to generate code\n";
        return 1;
    }

    // 返回计算结果
    codegen.Builder.CreateRet(result);

    // 验证LLVM模块
    if (verifyModule(*codegen.getModule(), &errs())) {
        std::cerr << "LLVM module verification failed\n";
        return 1;
    }

    // 输出LLVM IR代码
    std::cout << "========== Generated LLVM IR ==========\n";
    codegen.getModule()->print(outs(), nullptr);

    std::cout << "\n========== Summary ==========\n";
    std::cout << "✓ Expression successfully compiled to LLVM IR\n";
    std::cout << "✓ Generated function: compute()\n";
    std::cout << "✓ Expected result: 8.0 (5.0 + 3.0)\n";

    return 0;
}
