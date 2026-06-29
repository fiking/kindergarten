import tvm

def test_tensor_dom_infer():
    A = tvm.Tensor(2, name='A')
    B = tvm.Tensor(2, name='B')
    rd = tvm.RDom(tvm.Range(A.shape[1]))
    T = tvm.Tensor(2, lambda i, j:
                   tvm.reduce_sum(A(i, rd.index[0]) * B(j, rd.index[0]), rdom=rd),
                   shape=(A.shape[0], B.shape[0]))
    C = tvm.Tensor(2, lambda i, j: T(i,j),
                   shape=(A.shape[0], B.shape[0]))

    cdom = [tvm.Range(0, 10), tvm.Range(1, 11)]
    tdom = C.infer_input_domains(cdom, inputs=[T])[T]
    assert T.is_rtensor
    assert str(tdom[0]) == "(0, 10)"

test_tensor_dom_infer()
