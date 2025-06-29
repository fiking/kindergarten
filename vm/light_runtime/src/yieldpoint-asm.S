.text
.align 2
.globl mapleRT__yieldpoint

// void mapleRT__yieldpoint()
// 协作式GC的线程让步点，AArch64实现
mapleRT__yieldpoint:
    // 这里可以插入内存屏障、信号检测等逻辑
//    mov x0, mapleRT__yieldpoint_handler
//    mov x1, zr
//    br _mapleRT__save_registers_and_run
    ret
