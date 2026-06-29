import tvm

def test_tensor_reduce():
    A = tvm.Tensor(2, name='A')
    B = tvm.Tensor(2, name='B')
    T = tvm.Tensor(3, lambda i, j, k: A(i, k) * B(j, k),
                   shape=(A.shape[0], B.shape[0], A.shape[1]))
    rd = tvm.RDom(tvm.Range(A.shape[1]))
    C = tvm.Tensor(2, lambda i, j: tvm.reduce_sum(T(i, j, rd.index[0]), rdom=rd),
                   shape=(A.shape[0], B.shape[0]))
    print(tvm.format_str(C.expr))

test_tensor_reduce()
