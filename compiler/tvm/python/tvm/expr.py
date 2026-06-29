"""Base class of symbolic expression"""
from numbers import Number as _Number
from . import op as _op

class Expr(object):
    """Base class of expression."""

    def children(self):
        """All expr must define this.
        Returns
        -------
        children : generator of children
        """

    def __add__(self, other):
        return BinaryOpExpr(_op.add, self, other)

def _symbol(value):
    """Convert a value to expression"""
    if isinstance(value, Expr):
        return value
    elif isinstance(value, _Number):
        return ConstExpr(value)
    else:
        raise TypeError("type %s not supported" % str(type(other)))

class ConstExpr(Expr):
    """Constant expression."""
    def __init__(self, value):
        assert isinstance(value, _Number)
        self.value = value

class BinaryOpExpr(Expr):
    """Binary operator expression."""
    def __init__(self, op, lhs, rhs):
        self.op = op
        self.lhs = _symbol(lhs)
        self.rhs = _symbol(rhs)

    def children(self):
        return (self.lhs, self.rhs)

_op.binary_op_cls = BinaryOpExpr
