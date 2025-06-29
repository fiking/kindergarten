#ifndef STACKUNWINDER_H
#define STACKUNWINDER_H
#include <stdint.h>
#include <stddef.h>
namespace maplert {

class FrameCursor;
class FrameCursorFactory {
public:
  FrameCursorFactory() = default;
  void InitializeFrameCursor(FrameCursor &cursor, uintptr_t *regs_addr);
};


class AArch64State {
public:
  static const size_t N_GPREGS = 31; // General Purpose Registers
  static const size_t N_FPREGS = 32; // Vector Registers

  static const int GPREGS_CALLEESAVED_FIRST = 19; // First callee-saved GP register
  static const int GPREGS_CALLEESAVED_LAST = 30;  // Last callee-saved GP register
  static const int FPREGS_CALLEESAVED_FIRST = 8;  // First callee-saved FP register 
  static const int FPREGS_CALLEESAVED_LAST = 15;  // Last callee-saved FP register

  struct FPReg {
    uint64_t lo;
    uint64_t hi;
  };

  uint64_t gpregs[N_GPREGS]; // General Purpose Registers
  FPReg fpregs[N_FPREGS];     // Floating Point Registers
  uint64_t pc;                // Program Counter
  uint64_t sp;                // Stack Pointer
};

class FrameCursor {
public:
  AArch64State state; 
}; 

typedef uintptr_t (*save_registers_and_run_callback_t)(uintptr_t regs_addr, void *unserdata);
extern "C" uintptr_t mapleRT__save_registers_and_run(save_registers_and_run_callback_t callback, void *userdata);

static const uintptr_t SAVEDREGS_V8_OFFSET = 0x60; // Offset for saved V8 registers in the stack frame
static const uintptr_t SAVEDREGS_OLD_SP_OFFSET = 0xe0; // Offset for old SP in the stack frame
} // namespace maplert

#endif // STACKUNWINDER_H
