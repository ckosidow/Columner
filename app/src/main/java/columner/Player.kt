package columner

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

    private val vertexBuffer: FloatBuffer
    private val drawListBuffer: ShortBuffer
    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mMVPMatrixHandle: Int = 0
    var playerMatrix = FloatArray(16)
    val transMatrix = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)
    private val COLOR = floatArrayOf(0.6f, 0.3f, 0.6f, 1.0f)
    private val DRAW_ORDER = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices
    private val playerCoords = floatArrayOf((WIDTH / 2), (PLAYER_HEIGHT + PROTRUSION_HEIGHT), 0.0f, // top left
            -(WIDTH / 2), (PLAYER_HEIGHT + PROTRUSION_HEIGHT), 0.0f, // top right
            -(WIDTH / 2), PROTRUSION_HEIGHT, 0.0f, // bottom left
            (WIDTH / 2), PROTRUSION_HEIGHT, 0.0f) // bottom right
    private val vertexStride = COORDS_PER_VERTEX shl 2 // 4 bytes per vertex
    private val vertexCount = playerCoords.size / COORDS_PER_VERTEX
    private val vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    " gl_Position = uMVPMatrix * vPosition;" +
                    "}"
    private val fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    " gl_FragColor = vColor;" +
                    "}"

    init {
        // initialize vertex byte buffer for shape coordinates
        val byteBuffer = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                playerCoords.size shl 2)
        byteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = byteBuffer.asFloatBuffer()
        vertexBuffer.put(playerCoords)
        vertexBuffer.position(0)
        // initialize byte buffer for the draw list
        val dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                DRAW_ORDER.size shl 1)
        dlb.order(ByteOrder.nativeOrder())
        drawListBuffer = dlb.asShortBuffer()
        drawListBuffer.put(DRAW_ORDER)
        drawListBuffer.position(0)

        val vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram()
        // add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader)
        // add the fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader)
        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram)
    }

    fun draw(mvpMatrix: FloatArray) {
        // pass in the calculated transformation matrix
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer)
        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, COLOR, 0)
        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }
}