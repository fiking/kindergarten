#ifndef STACKUNWINDER_H
#define STACKUNWINDER_H
#include <stdint.h>
#include <stddef.h>
namespace maplert {
class AArch64State {
public:
  const static size_t N_GPREGS = 31; // General Purpose Registers
  const static size_t N_FPREGS = 32; // Vector Registers

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

} // namespace maplert

#endif // STACKUNWINDER_H
