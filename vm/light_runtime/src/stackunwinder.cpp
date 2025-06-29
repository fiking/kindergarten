#include "stackunwinder.h"
#include <cstring>

namespace maplert {
void FrameCursorFactory::InitializeFrameCursor(FrameCursor &cursor, uintptr_t *regs_addr) {
    int i;
    AArch64State &state = cursor.state;
    memset(&state, 0, sizeof(AArch64State));

    uint64_t *x19s = reinterpret_cast<uint64_t *>(regs_addr);
    for (i = AArch64State::GPREGS_CALLEESAVED_FIRST; i <= AArch64State::GPREGS_CALLEESAVED_LAST; ++i) {
        state.gpregs[i] = x19s[i - AArch64State::GPREGS_CALLEESAVED_FIRST];
    }

    AArch64State::FPReg *v8s = reinterpret_cast<AArch64State::FPReg *>(regs_addr + SAVEDREGS_V8_OFFSET);
    for (i = AArch64State::FPREGS_CALLEESAVED_FIRST; i <= AArch64State::FPREGS_CALLEESAVED_LAST; ++i) {
        state.fpregs[i - AArch64State::FPREGS_CALLEESAVED_FIRST].lo = v8s[i - AArch64State::FPREGS_CALLEESAVED_FIRST].lo;
        state.fpregs[i - AArch64State::FPREGS_CALLEESAVED_FIRST].hi = v8s[i - AArch64State::FPREGS_CALLEESAVED_FIRST].hi;
    }
    state.pc = x19s[30]; // Assuming PC is stored in x30
    state.sp = (uint64_t)(regs_addr + SAVEDREGS_OLD_SP_OFFSET); // Assuming SP is at the offset defined
}

} // namespace maplert
