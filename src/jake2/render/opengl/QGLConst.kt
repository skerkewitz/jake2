package jake2.render.opengl


interface QGLConst {
    companion object {

        /*
     * alpha functions
     */
        val GL_NEVER = 0x0200
        val GL_LESS = 0x0201
        val GL_EQUAL = 0x0202
        val GL_LEQUAL = 0x0203
        val GL_GREATER = 0x0204
        val GL_NOTEQUAL = 0x0205
        val GL_GEQUAL = 0x0206
        val GL_ALWAYS = 0x0207

        /*
     * attribute masks
     */
        val GL_DEPTH_BUFFER_BIT = 0x00000100
        val GL_STENCIL_BUFFER_BIT = 0x00000400
        val GL_COLOR_BUFFER_BIT = 0x00004000

        /*
     * begin modes
     */
        val GL_POINTS = 0x0000
        val GL_LINES = 0x0001
        val GL_LINE_LOOP = 0x0002
        val GL_LINE_STRIP = 0x0003
        val GL_TRIANGLES = 0x0004
        val GL_TRIANGLE_STRIP = 0x0005
        val GL_TRIANGLE_FAN = 0x0006
        val GL_QUADS = 0x0007
        val GL_QUAD_STRIP = 0x0008
        val GL_POLYGON = 0x0009

        /*
     * blending factors
     */
        val GL_ZERO = 0
        val GL_ONE = 1
        val GL_SRC_COLOR = 0x0300
        val GL_ONE_MINUS_SRC_COLOR = 0x0301
        val GL_SRC_ALPHA = 0x0302
        val GL_ONE_MINUS_SRC_ALPHA = 0x0303
        val GL_DST_ALPHA = 0x0304
        val GL_ONE_MINUS_DST_ALPHA = 0x0305

        /*
     * boolean
     */
        val GL_TRUE = 1
        val GL_FALSE = 0

        /*
     * data types
     */
        val GL_BYTE = 0x1400
        val GL_UNSIGNED_BYTE = 0x1401
        val GL_SHORT = 0x1402
        val GL_UNSIGNED_SHORT = 0x1403
        val GL_INT = 0x1404
        val GL_UNSIGNED_INT = 0x1405
        val GL_FLOAT = 0x1406

        /*
     * draw buffer modes
     */
        val GL_FRONT = 0x0404
        val GL_BACK = 0x0405
        val GL_FRONT_AND_BACK = 0x0408

        /*
     * errors
     */
        val GL_NO_ERROR = 0
        val GL_POINT_SMOOTH = 0x0B10
        val GL_CULL_FACE = 0x0B44
        val GL_DEPTH_TEST = 0x0B71
        val GL_MODELVIEW_MATRIX = 0x0BA6
        val GL_ALPHA_TEST = 0x0BC0
        val GL_BLEND = 0x0BE2

        val GL_SCISSOR_TEST = 0x0C11
        val GL_PACK_ALIGNMENT = 0x0D05
        val GL_TEXTURE_2D = 0x0DE1

        /*
     * hints
     */
        val GL_PERSPECTIVE_CORRECTION_HINT = 0x0C50
        val GL_DONT_CARE = 0x1100
        val GL_FASTEST = 0x1101
        val GL_NICEST = 0x1102

        /*
     * matrix modes
     */
        val GL_MODELVIEW = 0x1700
        val GL_PROJECTION = 0x1701

        /*
     * pixel formats
     */
        val GL_COLOR_INDEX = 0x1900
        val GL_RED = 0x1903
        val GL_GREEN = 0x1904
        val GL_BLUE = 0x1905
        val GL_ALPHA = 0x1906
        val GL_RGB = 0x1907
        val GL_RGBA = 0x1908
        val GL_LUMINANCE = 0x1909
        val GL_LUMINANCE_ALPHA = 0x190A

        /*
     * polygon modes
     */
        val GL_POINT = 0x1B00
        val GL_LINE = 0x1B01
        val GL_FILL = 0x1B02

        /*
     * shading models
     */
        val GL_FLAT = 0x1D00
        val GL_SMOOTH = 0x1D01
        val GL_REPLACE = 0x1E01

        /*
     * string names
     */
        val GL_VENDOR = 0x1F00
        val GL_RENDERER = 0x1F01
        val GL_VERSION = 0x1F02
        val GL_EXTENSIONS = 0x1F03

        /*
     * TextureEnvMode
     */
        val GL_MODULATE = 0x2100

        /*
     * TextureEnvParameter
     */
        val GL_TEXTURE_ENV_MODE = 0x2200
        val GL_TEXTURE_ENV_COLOR = 0x2201

        /*
     * TextureEnvTarget
     */
        val GL_TEXTURE_ENV = 0x2300
        val GL_NEAREST = 0x2600
        val GL_LINEAR = 0x2601
        val GL_NEAREST_MIPMAP_NEAREST = 0x2700
        val GL_LINEAR_MIPMAP_NEAREST = 0x2701
        val GL_NEAREST_MIPMAP_LINEAR = 0x2702
        val GL_LINEAR_MIPMAP_LINEAR = 0x2703

        /*
     * TextureParameterName
     */
        val GL_TEXTURE_MAG_FILTER = 0x2800
        val GL_TEXTURE_MIN_FILTER = 0x2801
        val GL_TEXTURE_WRAP_S = 0x2802
        val GL_TEXTURE_WRAP_T = 0x2803

        /*
     * TextureWrapMode
     */
        val GL_CLAMP = 0x2900
        val GL_REPEAT = 0x2901

        /*
     * texture
     */
        val GL_LUMINANCE8 = 0x8040
        val GL_INTENSITY8 = 0x804B
        val GL_R3_G3_B2 = 0x2A10
        val GL_RGB4 = 0x804F
        val GL_RGB5 = 0x8050
        val GL_RGB8 = 0x8051
        val GL_RGBA2 = 0x8055
        val GL_RGBA4 = 0x8056
        val GL_RGB5_A1 = 0x8057
        val GL_RGBA8 = 0x8058

        /*
     * vertex arrays
     */
        val GL_VERTEX_ARRAY = 0x8074
        val GL_COLOR_ARRAY = 0x8076
        val GL_TEXTURE_COORD_ARRAY = 0x8078
        val GL_T2F_V3F = 0x2A27

        /*
     * OpenGL 1.2, 1.3 constants
     */
        val GL_SHARED_TEXTURE_PALETTE_EXT = 0x81FB
        val GL_TEXTURE0 = 0x84C0
        val GL_TEXTURE1 = 0x84C1

        val GL_TEXTURE0_ARB = 0x84C0
        val GL_TEXTURE1_ARB = 0x84C1
        val GL_BGR = 0x80E0
        val GL_BGRA = 0x80E1

        /*
     * point parameters
     */
        val GL_POINT_SIZE_MIN_EXT = 0x8126
        val GL_POINT_SIZE_MAX_EXT = 0x8127
        val GL_POINT_FADE_THRESHOLD_SIZE_EXT = 0x8128
        val GL_DISTANCE_ATTENUATION_EXT = 0x8129
    }

}