import tvm

def test_range_infer():
    x = tvm.Var('x')
    y = tvm.Var('y')
    t = tvm.Var('t')
    z = x + y + t
    zr = tvm.infer_range(z, {x: tvm.Range(10, 20), y : tvm.Range(10, 11)})
    assert str(zr) == "((t0 + 20), (t0 + 30))"

test_range_infer()
