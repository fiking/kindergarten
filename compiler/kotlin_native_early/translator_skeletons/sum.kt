class MyClass(val a: Int)

fun ifTest() {
  val a = 2;
  val b = 3;
  if (a > b) {
    return a
  }
}

fun classConstruct() {
  val a = 2
  val clazz = MyClass(a)
}

fun cmp() {
  val a = 2;
  val b = 3;
  val c = a == b
  return c
}

fun callee(val a: Int, val b: Int) {
}

fun sum(val a: Int, val b: Int) : Int {
  val c = callee(a, b)
  return 2 + 3;
}
