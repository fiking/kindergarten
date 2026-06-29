"Init proptype of the TVM"

print("hello TVM")

from .op import *
from .expr import Var, const
from .expr_util import *
from .tensor import Tensor
from .domain import RDom, Range, infer_range
