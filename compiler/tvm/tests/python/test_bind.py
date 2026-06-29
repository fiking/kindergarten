import tvm
from tvm import expr

def test_bind():
    x = tvm.Var('x')
    y = x + 1
    z = tvm.bind(y, {x: tvm.const(10) + 9})
    assert tvm.format_str(z) == '((10 + 9) + 1)'

test_bind()
