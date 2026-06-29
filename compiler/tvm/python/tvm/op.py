_binary_op_cls = None

class BinaryOp(object):
    """Base class of binary operator"""
    def __call__(self, lhs, rhs):
        return _binary_op_cls(self, lhs, rhs)

class AddOp(BinaryOp):
    pass

add = AddOp()
