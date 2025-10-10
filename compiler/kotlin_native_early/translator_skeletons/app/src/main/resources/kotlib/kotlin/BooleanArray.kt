package kotlib.kotlin

class BooleanArray(var size: Int) {
    val data: Int

    /** Returns the number of elements in the array. */
    //size: Int

    init {
        this.data = malloc_array(this.size)
    }

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    fun get(index: Int): Boolean {
        val res = kotlinclib_get_byte(this.data, index) == Int.toByte()
        return res
    }


    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    fun set(index: Int, value: Boolean) {
        if (value == true) {
            kotlinclib_set_byte(this.data, index, Int.toByte())
        }
        else{
            kotlinclib_set_byte(this.data, index, Int.toByte())
        }
    }


    fun clone(): BooleanArray {
        val newInstance = BooleanArray(this.size)
        var index = 0
        while (index < this.size) {
            val value = this.get(index)
            newInstance.set(index, value)
            index = index + 1
        }

        return newInstance
    }

}