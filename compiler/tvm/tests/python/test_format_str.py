import tvm
from tvm import expr

def test_format_str():
    a = tvm.Var('a')
    b = tvm.Var('b')
    c = a + b
    assert tvm.format_str(c) == f'({a.name} + {b.name})'

test_format_str()
