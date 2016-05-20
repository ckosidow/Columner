package squareUp

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Protrusion(col: Double) {
    companion object {
        val WIDTH = 0.15f
        val LEFT = 0.54f
        val COORDS_PER_VERTEX = 3
        val HEIGHT = 0.025f
    }

    val vertexBuffer: FloatBuffer
    val drawListBuffer: ShortBuffer
    var mProgram: Int = 0
    var mPositionHandle: Int = 0
    var protMatrix = FloatArray(16)
    val transMatrix = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)
    val color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    val protCoords = floatArrayOf(LEFT, HEIGHT, 0.0f,
            LEFT, 0.0f, 0.0f,
            (LEFT - WIDTH), 0.0f, 0.0f,
            (LEFT - WIDTH), HEIGHT, 0.0f)
    val DRAW_ORDER = shortArrayOf(0, 1, 2, 0, 2, 3)
    val vertexStride = COORDS_PER_VERTEX shl 2
    val vertexCount = protCoords.size / COORDS_PER_VERTEX
    val vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    " gl_Position = uMVPMatrix * vPosition;" +
                    "}"

    val fragmentShaderCode =
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

        vertexBuffer = ByteBuffer.allocateDirect(protCoords.size shl 2).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(protCoords)
        vertexBuffer.position(0)

        drawListBuffer = ByteBuffer.allocateDirect(DRAW_ORDER.size shl 1).order(ByteOrder.nativeOrder()).asShortBuffer()
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

        GLES20.glUniform4fv(GLES20.glGetUniformLocation(mProgram, "vColor"), 1, color, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }
}