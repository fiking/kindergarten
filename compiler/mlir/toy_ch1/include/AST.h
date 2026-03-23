#ifndef AST_H
#define AST_H

#include "Lexer.h"

#include "llvm/ADT/ArrayRef.h"
#include "llvm/ADT/StringRef.h"
#include "llvm/Support/Casting.h"

#include <memory>
#include <string>
#include <vector>

namespace toy {

struct VarType {
  std::vector<int64_t> shape;
};

// ======================== AST 节点定义 ========================
class ExprAST {
public:
  enum ExprASTKind {
    Expr_VarDecl,
    Expr_Return,
    Expr_Num,
    Expr_Literal,
    Expr_Var,
    Expr_BinOp,
    Expr_Call,
    Expr_Print,
  };

  ExprAST(ExprASTKind kind, Location location)
      : kind(kind), location(std::move(location)) {}
  virtual ~ExprAST() = default;

  ExprASTKind getKind() const {
    return kind;
  }

  const Location &loc() {
    return location;
  }
private:
  const ExprASTKind kind;
  Location location;
};

using ExprASTList = std::vector<std::unique_ptr<ExprAST>>;

class NumberExprAST : public ExprAST {
public:
  NumberExprAST(Location loc, double val)
      : ExprAST(Expr_Num, std::move(loc)), val(val) {}

  double getValue() {
    return val;
  }

  static bool classof(const ExprAST *c) {
    return c->getKind() == Expr_Num;
  }

private:
  double val;
};

class LiteralExprAST : public ExprAST {
public:
  LiteralExprAST(Location loc, std::vector<std::unique_ptr<ExprAST>> values,
                 std::vector<int64_t> dims)
      : ExprAST(Expr_Literal, std::move(loc)), values(std::move(values)), dims(std::move(dims)) {}

  llvm::ArrayRef<std::unique_ptr<ExprAST>> getValues() {
    return values;
  }

  llvm::ArrayRef<int64_t> getDims() {
    return dims;
  }

  static bool classof(const ExprAST *c) {
    return c->getKind() == Expr_Literal;
  }

private:
  std::vector<std::unique_ptr<ExprAST>> values;
  std::vector<int64_t> dims;
};

class VariableExprAST : public ExprAST {
public:
  VariableExprAST(Location loc, std::string name)
      : ExprAST(Expr_Var, std::move(loc)), name(std::move(name)) {}

  llvm::StringRef getName() {
    return name;
  }

  static bool classof(const ExprAST *c) {
    return c->getKind() == Expr_Var;
  }
private:
  std::string name;
};

class VarDeclExprAST : public ExprAST {
public:
  VarDeclExprAST(Location loc, std::string name,
                 VarType type, std::unique_ptr<ExprAST> initVal)
      : ExprAST(Expr_VarDecl, std::move(loc)), name(std ::move(name)), type(std ::move(type)), initVal(std::move(initVal)) {}

  llvm::StringRef getName() {
    return name;
  }
  ExprAST *getInitVal() {
    return initVal.get();
  }
  VarType getType() {
    return type;
  }

  static bool classof(const ExprAST *c) {
    return c->getKind() == Expr_VarDecl;
  }
private:
  std::string name;
  VarType type;
  std::unique_ptr<ExprAST> initVal;
};

class ReturnExprAST : public ExprAST {
public:
  ReturnExprAST(Location loc, llvm::Optional<std::unique_ptr<ExprAST>> expr)
      : ExprAST(Expr_Return, std::move(loc)), expr(std::move(expr)) {}

  llvm::Optional<ExprAST*> getExpr() {
    if (expr.hasValue())
      return expr->get();
    return llvm::None;
  }

  static bool classof(const ExprAST *c) {
    return c->getKind() == Expr_Return;
}
private:
  llvm::Optional<std::unique_ptr<ExprAST>> expr;
};

class BinaryExprAST : public ExprAST {
public:
  BinaryExprAST(Location loc, char op, std::unique_ptr<ExprAST> lhs, std::unique_ptr<ExprAST> rhs)
      : ExprAST(Expr_BinOp, std::move(loc)), op(op), lhs(std::move(lhs)), rhs(std::move(rhs)) {}

  char getOp() {
    return op;
  }
  ExprAST *getLHS() {
    return lhs.get();
  }
  ExprAST *getRHS() {
    return rhs.get();
  }

  static bool classof(const ExprAST *c) {
    return c->getKind() == Expr_BinOp;
  }

private:
  char op;
  std::unique_ptr<ExprAST> lhs;
  std::unique_ptr<ExprAST> rhs;
};

class CallExprAST : public ExprAST {
public:
  CallExprAST(Location loc, std::string callee, std::vector<std::unique_ptr<ExprAST>> args)
      : ExprAST(Expr_Call, std::move(loc)), callee(std::move(callee)), args(std::move(args)) {}

  llvm::StringRef getCallee() {
    return callee;
  }
  llvm::ArrayRef<std::unique_ptr<ExprAST>> getArgs() {
    return args;
  }

  static bool classof(const ExprAST *c) {
    return c->getKind() == Expr_Call;
  }

private:
  std::string callee;
  std::vector<std::unique_ptr<ExprAST>> args;
};

class PrintExprAST : public ExprAST {
public:
  PrintExprAST(Location loc, std::unique_ptr<ExprAST> args)
      : ExprAST(Expr_Print, std::move(loc)), args(std::move(args)) {}

  ExprAST* getArg() {
    return args.get();
  }

  static bool classof(const ExprAST *c) {
    return c->getKind() == Expr_Print;
  }

private:
  std::unique_ptr<ExprAST> args;
};

class PrototypeAST {
public:
  PrototypeAST(Location location, const std::string &name, std::vector<std::unique_ptr<VariableExprAST>> args)
      : location(std::move(location)), name(name), args(std::move(args)) {}

  const Location &loc() { return location; }
  llvm::StringRef getName() { return name; }
  llvm::ArrayRef<std::unique_ptr<VariableExprAST>> getArgs() { return args; }

private:
  Location location;
  std::string name;
  std::vector<std::unique_ptr<VariableExprAST>> args;
};

class FunctionAST {
public:
  FunctionAST(std::unique_ptr<PrototypeAST> proto, std::unique_ptr<ExprASTList> body)
      : proto(std::move(proto)), body(std::move(body)) {}

  PrototypeAST *getProto() {
    return proto.get();
  }
  ExprASTList *getBody() {
    return body.get();
  }

private:
  std::unique_ptr<PrototypeAST> proto;
  std::unique_ptr<ExprASTList> body;
};

class ModuleAST {
public:
  ModuleAST(std::vector<FunctionAST> functions)
      : functions(std::move(functions)) {}

  auto begin() {
    return functions.begin();
  }
  auto end() {
    return functions.end();
  }

private:
  std::vector<FunctionAST> functions;
};

void dump(ModuleAST &module);

}; // namespace toy
#endif // AST_H
