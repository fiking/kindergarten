class MyClass(val a: Int)

fun callee(val a: Int, val b: Int) {
}

fun sum(val a: Int, val b: Int) : Int {
  val c = callee(a, b)
  return 2 + 3;
}
