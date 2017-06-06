package jake2.client

import jake2.Defines

/**
 * Created by tropper on 06.06.17.
 */
internal class TClLightStyle {
    var length: Int = 0

    var value = FloatArray(3)

    var map = FloatArray(Defines.MAX_QPATH)

    fun clear() {
        length = 0
        value[2] = length.toFloat()
        value[1] = value[2]
        value[0] = value[1]
        for (i in map.indices) {
            map[i] = 0.0f
        }
    }
}
