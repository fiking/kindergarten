import tvm

def test_tensor():
    A = tvm.Tensor(2, name = 'A')
    B = tvm.Tensor(2, name = 'B')
    T = tvm.Tensor(3, lambda i, j, k: A(i, k) * B(j, k),
                   shape=(A.shape[0], B.shape[0], A.shape[1]))
    print(tvm.format_str(T.expr))

test_tensor()
