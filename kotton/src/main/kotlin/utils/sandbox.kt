package madhat.kotton.utils

import java.math.*


// following is not legal apparently
// val prn1 = print
fun prn2(x: Any) = println(x)


class Sandbox {

    // fun add(x: BigDecimal, y: BigDecimal): BigDecimal = x + y

    var a = 1

    fun run() {
        println("Hello World!")
        val x = "Interpolating $a"
        a = 2
        println(x)
        println("Interpolating $a")
    }

}
