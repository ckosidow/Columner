package columner

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Protrusion(col: Double) {
    companion object {
        val WIDTH = 0.15f
        private val LEFT = 0.54f
        internal val COORDS_PER_VERTEX = 3
    }

    private val vertexBuffer: FloatBuffer
    private val drawListBuffer: ShortBuffer
    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mMVPMatrixHandle: Int = 0
    var protMatrix = FloatArray(16)
    val transMatrix = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)
    private val color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    private val protCoords = floatArrayOf(LEFT, PROTRUSION_HEIGHT, 0.0f,
            LEFT, 0.0f, 0.0f,
            (LEFT - WIDTH), 0.0f, 0.0f,
            (LEFT - WIDTH), PROTRUSION_HEIGHT, 0.0f)
    private val DRAW_ORDER = shortArrayOf(0, 1, 2, 0, 2, 3)
    private val vertexStride = COORDS_PER_VERTEX shl 2
    private val vertexCount = protCoords.size / COORDS_PER_VERTEX
    private val vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    " gl_Position = uMVPMatrix * vPosition;" +
                    "}"

    private val fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    " gl_FragColor = vColor;" +
                    "}"

    init {
        protCoords[0] -= (.465 * col).toFloat()
        protCoords[3] -= (.465 * col).toFloat()
        protCoords[6] -= (.465 * col).toFloat()
        protCoords[9] -= (.465 * col).toFloat()

        val byteBuffer = ByteBuffer.allocateDirect(protCoords.size shl 2)
        byteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = byteBuffer.asFloatBuffer()
        vertexBuffer.put(protCoords)
        vertexBuffer.position(0)

        val dlb = ByteBuffer.allocateDirect(DRAW_ORDER.size shl 1)
        dlb.order(ByteOrder.nativeOrder())
        drawListBuffer = dlb.asShortBuffer()
        drawListBuffer.put(DRAW_ORDER)
        drawListBuffer.position(0)

        mProgram = GLES20.glCreateProgram()

        GLES20.glAttachShader(mProgram, MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode))
        GLES20.glAttachShader(mProgram, MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode))

        GLES20.glLinkProgram(mProgram)
    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(mProgram)
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        GLES20.glUniform4fv(mColorHandle, 1, color, 0)
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }
}