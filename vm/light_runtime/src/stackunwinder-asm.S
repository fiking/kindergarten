.text
.align 2
.globl _mapleRT__save_registers_and_run

// void mapleRT__save_registers_and_run(void (*func)(void*), void* arg)
// x0: func, x1: arg
_mapleRT__save_registers_and_run:
    .cfi_startproc
    sub sp, sp, 0xe0 // (224)
    .cfi_def_cfa_offset 0xe0

    // 保存通用寄存器 x19-x30
    stp x19, x20, [sp, 0x00]
    .cfi_offset x19, -0xe0
    .cfi_offset x20, -0xd8
    stp x21, x22, [sp, 0x10]
    .cfi_offset x21, -0xd0
    .cfi_offset x22, -0xc8
    stp x23, x24, [sp, 0x20]
    .cfi_offset x23, -0xc0
    .cfi_offset x24, -0xb8
    stp x25, x26, [sp, 0x30]
    .cfi_offset x25, -0xb0
    .cfi_offset x26, -0xa8
    stp x27, x28, [sp, 0x40]
    .cfi_offset x27, -0xa0
    .cfi_offset x28, -0x98
    stp x29, x30, [sp, 0x50]  
    .cfi_offset x29, -0x90
    .cfi_offset x30, -0x88

    // 保存 fp/simd 寄存器 q8-q15
    stp q8, q9,   [sp, 0x60]
    .cfi_offset q8, -0x80
    .cfi_offset q9, -0x70
    stp q10, q11, [sp, 0x80]
    .cfi_offset q10, -0x60
    .cfi_offset q11, -0x50
    stp q12, q13, [sp, 0xa0]
    .cfi_offset q12, -0x40
    .cfi_offset q13, -0x30
    stp q14, q15, [sp, 0xc0]
    .cfi_offset q14, -0x20
    .cfi_offset q15, -0x10

    // call the callback
    mov x2, x0
    mov x0, sp
    blr x2

    
    // 恢复 fp/simd 寄存器 q8-q15
    ldp q14, q15, [sp, 0xc0]
    .cfi_restore q14
    .cfi_restore q15
    ldp q12, q13, [sp, 0xa0]
    .cfi_restore q12
    .cfi_restore q13
    ldp q10, q11, [sp, 0x80]
    .cfi_restore q10
    .cfi_restore q11
    ldp q8,  q9,  [sp, 0x60]
    .cfi_restore q8
    .cfi_restore q9

    // 恢复通用寄存器 x19-x30
    ldp x29, x30, [sp, 0x50]
    .cfi_restore x29
    .cfi_restore x30
    ldp x27, x28, [sp, 0x40]
    .cfi_restore x27
    .cfi_restore x28
    ldp x25, x26, [sp, 0x30]
    .cfi_restore x25
    .cfi_restore x26
    ldp x23, x24, [sp, 0x20]
    .cfi_restore x23
    .cfi_restore x24
    ldp x21, x22, [sp, 0x10]
    .cfi_restore x21
    .cfi_restore x22
    ldp x19, x20, [sp, 0x00]
    .cfi_restore x19
    .cfi_restore x20

    add sp, sp, 0xe0
    .cfi_def_cfa sp, 0
    ret
    .cfi_endproc
