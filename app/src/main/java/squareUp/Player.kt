package squareUp

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Player {
    companion object {
        val WIDTH = 0.075f
        internal val COORDS_PER_VERTEX = 3
    }

    val HEIGHT = 0.075f
    val STARTPOS = MyGLRenderer.BOTTOM_OF_SCREEN + MyGLRenderer.XGAP
    val JUMP_SPEED = -1.0f
    val vertexBuffer: FloatBuffer
    val drawListBuffer: ShortBuffer
    var mProgram: Int = 0
    var mPositionHandle: Int = 0
    var mColorHandle: Int = 0
    var mMVPMatrixHandle: Int = 0
    var playerMatrix = FloatArray(16)
    val transMatrix = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)
    val COLOR = floatArrayOf(0.6f, 0.3f, 0.6f, 1.0f)
    val DRAW_ORDER = shortArrayOf(0, 1, 2, 0, 2, 3)
    val playerCoords = floatArrayOf((WIDTH / 2), (HEIGHT + Protrusion.HEIGHT), 0.0f,
            -(WIDTH / 2), (HEIGHT + Protrusion.HEIGHT), 0.0f,
            -(WIDTH / 2), Protrusion.HEIGHT, 0.0f,
            (WIDTH / 2), Protrusion.HEIGHT, 0.0f)
    val vertexStride = COORDS_PER_VERTEX shl 2
    val vertexCount = playerCoords.size / COORDS_PER_VERTEX
    var goingRight = false
    var goingLeft = false
    var jump: Boolean = false
    var inAir: Boolean = false
    var endOfJump: Boolean = false
    var falling: Boolean = false
    var jumpYAccel: Float = 0.toFloat()
    var jumpXAccel: Float = 0.toFloat()
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
        val byteBuffer = ByteBuffer.allocateDirect(playerCoords.size shl 2)
        byteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = byteBuffer.asFloatBuffer()
        vertexBuffer.put(playerCoords)
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
        GLES20.glUniform4fv(mColorHandle, 1, COLOR, 0)

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

    fun jump() {

    }
}