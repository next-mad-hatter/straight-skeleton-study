package madhat.kotton.utils

import java.math.*

class Sandbox {

    fun add(x: BigDecimal, y: BigDecimal): BigDecimal = x + y

    // following is not legal apparently
    // val prn1 = print
    fun prn2(x: String) = print(x)

    var a = 1

    fun run() {
        println("Hello World!")
        val x = "Interpolating $a"
        a = 2
        println(x)
        println("Interpolating $a")
    }

}
