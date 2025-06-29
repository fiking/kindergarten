#include <cstdint>
#include <cstddef>
#ifndef STACKUNWINDER_H
#define STACKUNWINDER_H

#ifdef __cplusplus
namespace maplert {
extern "C" {
#endif

class AArch64State {
public:
  enum Register {
    R0 = 0,
    R1,
    R2,
    R3,
    R4,
    R5,
    R6,
    R7,
    R8,
    R9,
    R10,
    R11,
    R12,
    R13,
    R14,
    R15,
    R16,
    R17,
    R18,
    R19,
    R20,
    R21,
    R22,
    R23,
    R24,
    R25,
    R26,
    R27,
    R28,
    R29,
    R30,
    V0,
    V1,
    V2,
    V3,
    V4,
    V5,
    V6,
    V7,
    V8,
    V9,
    V10,
    V11,
    V12,
    V13,
    V14,
    V15,
    V16,
    V17,
    V18,
    V19,
    V20,
    V21,
    V22,
    V23,
    V24,
    V25,
    V26,
    V27,
    V28,
    V29,
    V30,
    V31,
    PC,
    SP,
    IP0 = 16,
    IP1 = 17,
    FP = 29,
    LR = 30,     
  };

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

  bool IsPreserved(Register reg) const;
  uint64_t GetGPReg(Register reg) const;
  FPReg GetFPReg(Register reg) const;

  uint64_t SetGPReg(Register reg);
  FPReg SetFPReg(Register reg);
  
};

class FrameCursor {
public:
  AArch64State state; 
}; 

#ifdef __cplusplus
} // extern "C"
} // namespace maplert
#endif

#endif // STACKUNWINDER_H
