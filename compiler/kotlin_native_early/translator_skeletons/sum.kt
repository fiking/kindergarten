class MyClass(val a: Int) {
  fun Add() {}
}

fun readClassField() : Int {
  var value = MyClass(2)
  var rst = value.Add()
  return value.a
}

fun ifTest() : Int {
  val a = 2;
  val b = 3;
  if (a > b) {
    return a
  }
    return b
}

fun classConstruct() {
  val a = 2
  val clazz = MyClass(a)
}

fun cmp() : Boolean {
  val a = 2;
  val b = 3;
  val c = a == b
  return c
}

fun callee(a: Int, b: Int) {
}


fun sum(a: Int, b: Int) : Int {
//  val c = callee(a, b)
  return 2 + 3;
}

