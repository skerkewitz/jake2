/*
 * Timer.java
 * Copyright (C) 2005
 */
package jake2.qcommon

import jake2.client.Context


class Timer private constructor()/* Singleton. */ {

    private val base = System.nanoTime()

    private fun currentTimeMillis(): Long {
        val time = System.nanoTime()
        var delta = time - base
        if (delta < 0) {
            delta += java.lang.Long.MAX_VALUE + 1
        }
        return (delta * 0.000001).toLong()
    }

    companion object {

        private val t = Timer()

        @JvmStatic fun Milliseconds(): Int {
            Context.curtime = t.currentTimeMillis().toInt()
            return Context.curtime
        }
    }
}
