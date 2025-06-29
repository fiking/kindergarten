#include "stackunwinder.h"
#include <stdio.h>
#include <string.h>

namespace maplert {

bool AArch64State::IsPreserved(Register reg) const {
    // 以 AArch64 ABI 为例，x19-x28、fp(x29)、lr(x30)、sp 通常为 callee-saved
    return (reg >= R19 && reg <= R28) || reg == FP || reg == LR || reg == SP;
}

uint64_t AArch64State::GetGPReg(Register reg) const {
    if (reg >= 0 && reg < N_GPREGS) {
        return gpregs[reg];
    }
    return 0;
}

AArch64State::FPReg AArch64State::GetFPReg(Register reg) const {
    if (reg >= V0 && reg < V0 + N_FPREGS) {
        return fpregs[reg - V0];
    }
    return {0, 0};
}

uint64_t AArch64State::SetGPReg(Register reg) {
    if (reg >= 0 && reg < N_GPREGS) {
        gpregs[reg] = 0; // 示例：实际应赋值
        return gpregs[reg];
    }
    return 0;
}

AArch64State::FPReg AArch64State::SetFPReg(Register reg) {
    // if (reg >= V0 && reg < V0 + N_FPREGS) {
    //     fpregs[reg - V0] = {0, 0}; // 示例：实际应赋值
    //     return fpregs[reg - V0];
    // }
    // return {0, 0};
}
} // namespace maplert
