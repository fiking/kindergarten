import tvm

def test_tensor():
    A = tvm.Tensor(2, name = 'A')
    B = tvm.Tensor(2, name = 'B')
    T = tvm.Tensor(3, lambda i, j, k: A(i, k) * B(j, k))
    print(tvm.format_str(T.expr))

test_tensor()
