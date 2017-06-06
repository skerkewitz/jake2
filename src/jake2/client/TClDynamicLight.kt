package jake2.client

/**
 * Created by tropper on 06.06.17.
 */
internal class TClDynamicLight {
    var key: Int = 0 // so entities can reuse same entry

    var color = floatArrayOf(0f, 0f, 0f)

    var origin = floatArrayOf(0f, 0f, 0f)

    var radius: Float = 0.toFloat()

    var die: Float = 0.toFloat() // stop lighting after this time

    var minlight: Float = 0.toFloat() // don't add when contributing less

    fun clear() {
        color[2] = 0f
        color[1] = color[2]
        color[0] = color[1]
        minlight = color[0]
        radius = minlight
    }
}
