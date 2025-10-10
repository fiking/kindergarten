package kotlib.kotlin

external fun malloc_array(size: Int) : Int
external fun kotlinclib_get_int(src: Int, index: Int): Int
external fun kotlinclib_set_int(src: Int, index: Int, value: Int)


class IntArray(var size: Int) {
    val data: Int

    /** Returns the number of elements in the array. */
    //size: Int

    init {
        this.data = malloc_array(4 * this.size)
    }

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    operator fun get(index: Int): Int {
        return kotlinclib_get_int(this.data, index)
    }


    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    operator fun set(index: Int, value: Int) {
        kotlinclib_set_int(this.data, index, value)
    }


    fun clone(): IntArray {
        val newInstance = IntArray(this.size)
        var index = 0
        while (index < this.size) {
            val value = this.get(index)
            newInstance.set(index, value)
            index = index + 1
        }

        return newInstance
    }

}
