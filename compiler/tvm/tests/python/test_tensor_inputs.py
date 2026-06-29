import tvm

def test_tensor_inputs():
    A = tvm.Tensor(2, name = 'A')
    B = tvm.Tensor(2, name = 'B')
    T = tvm.Tensor(3, lambda i, j, k: A(i, k) * B(j, k),
                   shape=(A.shape[0], B.shape[0], A.shape[1]))
    print(T.input_tensors())
    print([A, B])
    assert(T.input_tensors() == [A, B])


test_tensor_inputs()
