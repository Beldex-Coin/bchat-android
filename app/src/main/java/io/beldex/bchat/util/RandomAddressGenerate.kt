package io.beldex.bchat.util

import java.util.Random

class RandomAddressGenerate {
    fun randomAddress():String{
        val random = Random()
        val id1 = String.format("%09d", random.nextInt(1000000000))
        val id2 = String.format("%09d", random.nextInt(1000000000))
        val id3 = String.format("%09d", random.nextInt(1000000000))
        val id4 = String.format("%09d", random.nextInt(1000000000))
        val id5 = String.format("%09d", random.nextInt(1000000000))
        val id6 = String.format("%09d", random.nextInt(1000000000))
        val id7 = String.format("%09d", random.nextInt(1000000000))
        val id8 = String.format("%09d", random.nextInt(1000000000))
        val id9 = String.format("%09d", random.nextInt(1000000000))
        val id10 = String.format("%09d", random.nextInt(1000000000))
        val id11 = String.format("%05d", random.nextInt(100000))
        return "bx$id1$id2$id3$id4$id5$id6$id7$id8$id9$id10$id11"
    }
}