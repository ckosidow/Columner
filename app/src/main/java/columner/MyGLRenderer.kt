package columner

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer(context: Context) : GLSurfaceView.Renderer {
    companion object {
        // private GLText mGLText;
        private val COLUMNS = 3
        private val BOTTOM_OF_SCREEN = -1.0f
        private val TOP_OF_SCREEN = 1.0f
        private val XGAP = 0.5f
        private val YGAP = -0.465f
        private val STARTPOS = BOTTOM_OF_SCREEN + XGAP
        private val JUMP_SPEED = -1.0f
        private val START_SPEED = -0.01f
        private val MAX_SPEED = -0.03f
        private val ACCEL = -0.000005f
        lateinit var indices: ShortArray

        fun loadShader(
                type: Int,
                shaderCode: String): Int {
            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            val shader = GLES20.glCreateShader(type)
            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }
    }

    var baseSpeed: Float = 0.toFloat()
    var jumpYAccel: Float = 0.toFloat()
    var jumpXAccel: Float = 0.toFloat()
    var started: Boolean = false
    var jump: Boolean = false
    var inAir: Boolean = false
    var endOfJump: Boolean = false
    var goingLeft: Boolean = false
    var goingRight: Boolean = false
    var falling: Boolean = false
    private var jumpCol = 1
    var nextCol: Int = 0
    private val mProtrusions = arrayOfNulls<Protrusion>(9)
    private lateinit var mPlayer: Player
    private val protQueue = LinkedList<Int>()
    private var needNext: Boolean = false
    private val context: Context
    private var score: Int = 0
    private lateinit var config: EGLConfig
    lateinit var vertexBuffer: FloatBuffer
    lateinit var uvBuffer: FloatBuffer
    lateinit var drawListBuffer: ShortBuffer
    private val mtrxProjection = FloatArray(16)
    private val mtrxView = FloatArray(16)
    private val mtrxProjectionAndView = FloatArray(16)
    private lateinit var tm: TextManager
    lateinit private var scoreTxt: TextObject
    private var ssu = 1.0f
    private var ssx = 1.0f
    private var ssy = 1.0f
    private var swp = 320.0f
    private var shp = 480.0f
    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private val mMVPMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)

    init {
        this.context = context
    }

    override fun onSurfaceCreated(
            unused: GL10,
            c: EGLConfig) {
        config = c
        protQueue.clear()
        falling = false
        inAir = false
        jump = false
        endOfJump = false
        goingLeft = false
        goingRight = false
        jumpCol = 1
        this.setupScaling()
        this.setupTriangle()
        this.setupImage()
        this.setupText()
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        // Create the shaders, images
        val vertexShader = riGraphicTools.loadShader(GLES20.GL_VERTEX_SHADER, vs_Image)
        val fragmentShader = riGraphicTools.loadShader(GLES20.GL_FRAGMENT_SHADER, fs_Image)
        sp_Image = GLES20.glCreateProgram() // create empty OpenGL ES Program
        GLES20.glAttachShader(sp_Image, vertexShader) // add the vertex shader to program
        GLES20.glAttachShader(sp_Image, fragmentShader) // add the fragment shader to program
        GLES20.glLinkProgram(sp_Image) // creates OpenGL ES program executables
        // Text shader
        val vshadert = riGraphicTools.loadShader(GLES20.GL_VERTEX_SHADER, vs_Text)
        val fshadert = riGraphicTools.loadShader(GLES20.GL_FRAGMENT_SHADER, fs_Text)
        sp_Text = GLES20.glCreateProgram()
        GLES20.glAttachShader(sp_Text, vshadert)
        GLES20.glAttachShader(sp_Text, fshadert)
        GLES20.glLinkProgram(sp_Text)
        // Set our shader programm
        GLES20.glUseProgram(sp_Image)
        mPlayer = Player()
        mPlayer.transMatrix[13] = STARTPOS

        for (i in mProtrusions.indices) {
            mProtrusions[i] = Protrusion((i % COLUMNS).toDouble())

            if (i == 1) {
                mProtrusions[i]!!.transMatrix[13] = STARTPOS
                protQueue.push(i)
            } else if (i > 1 && i <= 3) {
                mProtrusions[i]!!.transMatrix[13] = STARTPOS + (i - 1) * XGAP
                protQueue.push(i)
            } else {
                mProtrusions[i]!!.transMatrix[13] = -2.0f
            }
        }
    }

    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, -3.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)
        Matrix.translateM(mPlayer.transMatrix, 0, jumpXAccel, baseSpeed + jumpYAccel, 0.0f)
        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(mPlayer.playerMatrix, 0, mMVPMatrix, 0, mPlayer.transMatrix, 0)
        // Render our example
        this.Render(mtrxProjectionAndView)
        scoreTxt.text = "$score "
        // Render the text

        // Add it to our manager
        tm.updateText(scoreTxt)
        // Prepare the text for rendering
        tm.prepareDraw()
        tm.Draw(mtrxProjectionAndView)

        for (p in mProtrusions) {
            if (p!!.transMatrix[13] >= BOTTOM_OF_SCREEN - 0.5f) {
                Matrix.translateM(p.transMatrix, 0, 0.0f, baseSpeed, 0.0f)
                Matrix.multiplyMM(p.protMatrix, 0, mMVPMatrix, 0, p.transMatrix, 0)
            }
        }

        if (started) {
            if (baseSpeed == 0.0f) {
                baseSpeed = START_SPEED
                score = 0
            }
            if (baseSpeed >= MAX_SPEED) {
                baseSpeed += ACCEL
            }

            val rand = Random()
            val randNum = rand.nextInt(COLUMNS)

            if (mProtrusions[protQueue.first]!!.transMatrix[13] < 1 - XGAP) {
                needNext = true
                var i = 0

                while (i < mProtrusions.size && needNext) {
                    if (i % COLUMNS == randNum) {
                        if (mProtrusions[i]!!.transMatrix[13] < BOTTOM_OF_SCREEN) {
                            mProtrusions[i]!!.transMatrix[13] = TOP_OF_SCREEN
                            protQueue.push(i)
                            needNext = false
                        }
                    }

                    i++
                }
            }
        }

        if (jump && !inAir && !falling) {
            if (protQueue.size > 1) {
                protQueue.removeLast()
                inAir = true
                jumpCol = nextCol
            }

            jump = false
        }

        if (inAir && !falling) {
            if (mPlayer.transMatrix[13] >= mProtrusions[protQueue.last]!!.transMatrix[13] + PLAYER_HEIGHT && !endOfJump) {
                mPlayer.transMatrix[13] = mProtrusions[protQueue.last]!!.transMatrix[13] + PLAYER_HEIGHT - 0.001f
                jumpYAccel = -baseSpeed * (JUMP_SPEED * 22.0f * (mProtrusions[protQueue.last]!!.transMatrix[13] + PLAYER_HEIGHT
                        - mPlayer.transMatrix[13]))
                endOfJump = true
            } else if (endOfJump && mPlayer.transMatrix[13] > mProtrusions[protQueue.last]!!.transMatrix[13]) {
                jumpYAccel = -baseSpeed * (JUMP_SPEED * 22.0f * (mProtrusions[protQueue.last]!!.transMatrix[13] + PLAYER_HEIGHT
                        - mPlayer.transMatrix[13]))
            } else if (endOfJump && mPlayer.transMatrix[13] <= mProtrusions[protQueue.last]!!.transMatrix[13]) {
                mPlayer.transMatrix[13] = mProtrusions[protQueue.last]!!.transMatrix[13]

                if (jumpCol != protQueue.last % 3) {
                    falling = true
                } else {
                    jumpYAccel = 0.0f
                }

                endOfJump = false
            } else if (mPlayer.transMatrix[13] < mProtrusions[protQueue.last]!!.transMatrix[13]) {
                jumpYAccel = baseSpeed * (JUMP_SPEED * 22.0f * (mProtrusions[protQueue.last]!!.transMatrix[13] + PLAYER_HEIGHT
                        - mPlayer.transMatrix[13]))
            }
            if (mPlayer.transMatrix[12] <= (jumpCol - 1) * YGAP && goingRight || mPlayer.transMatrix[12] >= (jumpCol - 1) * YGAP && goingLeft) {
                if (jumpCol != protQueue.last % 3) {
                    falling = true
                } else {
                    jumpXAccel = 0.0f
                }

                mPlayer.transMatrix[12] = (jumpCol - 1) * YGAP
                goingLeft = false
                goingRight = false
            } else if (mPlayer.transMatrix[12] < (jumpCol - 1) * YGAP && !goingRight && !goingLeft) {
                // Needs to be -2 * baseSpeed for 1 distance hops and -4 * baseSpeed for 2 distance hops
                jumpXAccel = baseSpeed * 1.3f * (JUMP_SPEED + JUMP_SPEED * Math.abs(jumpCol % 2 - mPlayer.transMatrix[12] / YGAP))
                goingLeft = true
            } else if (mPlayer.transMatrix[12] > (jumpCol - 1) * YGAP && !goingRight && !goingLeft) {
                // Needs to be 2 * baseSpeed for 1 distance hops and 4 * baseSpeed for 2 distance hops
                jumpXAccel = -baseSpeed * 1.3f * (JUMP_SPEED + JUMP_SPEED * Math.abs(jumpCol % 2 + mPlayer.transMatrix[12] / YGAP))
                goingRight = true
            }
            if (jumpYAccel == 0.0f && jumpXAccel == 0.0f) {
                score++
                inAir = false
            }
        }
        if (falling || mPlayer.transMatrix[13] < BOTTOM_OF_SCREEN - PROTRUSION_HEIGHT - PLAYER_HEIGHT) {
            baseSpeed = 0f
            started = false
        }
        if (mPlayer.transMatrix[13] >= BOTTOM_OF_SCREEN - PROTRUSION_HEIGHT - PLAYER_HEIGHT) {
            mPlayer.draw(mPlayer.playerMatrix)
        } else {
            baseSpeed = 0.0f
            jumpYAccel = 0.0f
            jumpXAccel = 0.0f
            this.onSurfaceCreated(unused, config)
        }

        for (p in mProtrusions) {
            if (p!!.transMatrix[13] >= BOTTOM_OF_SCREEN - PROTRUSION_HEIGHT) {
                p.draw(p.protMatrix)
            }
        }
    }

    override fun onSurfaceChanged(
            unused: GL10,
            wid: Int,
            hgt: Int) {
        GLES20.glViewport(0, 0, wid, hgt)
        val ratio = wid.toFloat() / hgt
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 3.0f, 7.0f)
        // Clear our matrices
        for (i in 0..15) {
            mtrxProjection[i] = 0.0f
            mtrxView[i] = 0.0f
            mtrxProjectionAndView[i] = 0.0f
        }
        // Setup our screen width and height for normal sprite translation.
        Matrix.orthoM(mtrxProjection, 0, 0.0f, wid.toFloat(), 0.0f, hgt.toFloat(), 0.0f, 50.0f)
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0)
        // Setup our scaling system
        this.setupScaling()
    }

    fun setupText() {
        // Create our text manager
        tm = TextManager()
        // Tell our text manager to use index 1 of textures loaded
        tm.setTextureID(1)
        // Pass the uniform scale
        tm.uniformscale = ssu
        // Create our new textobject
        scoreTxt = TextObject("$score ", context.resources.displayMetrics.widthPixels / 5.0f,
                9 * context.resources.displayMetrics.heightPixels / 10.0f)
        // Add it to our manager
        tm.addText(scoreTxt)
        // Prepare the text for rendering
        tm.prepareDraw()
    }

    fun setupScaling() {
        // The screen resolutions
        swp = context.resources.displayMetrics.widthPixels.toFloat()
        shp = context.resources.displayMetrics.heightPixels.toFloat()
        // Orientation is assumed portrait
        ssx = swp / 320.0f
        ssy = shp / 480.0f
        // Get our uniform scaler
        ssu = if (ssx > ssy)
            ssy
        else
            ssx
    }

    private fun Render(matrix: FloatArray) {
        // Set our shaderprogram to image shader
        GLES20.glUseProgram(sp_Image)
        // clear Screen and Depth Buffer, we have set the clear color as black.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        // get handle to vertex shader's vPosition member and add vertices
        val mPositionHandle = GLES20.glGetAttribLocation(sp_Image, "vPosition")
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        // Get handle to texture coordinates location and load the texture uvs
        val mTexCoordLoc = GLES20.glGetAttribLocation(sp_Image, "a_texCoord")
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer)
        GLES20.glEnableVertexAttribArray(mTexCoordLoc)
        // Get handle to shape's transformation matrix and add our matrix
        val mtrxhandle = GLES20.glGetUniformLocation(sp_Image, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, matrix, 0)
        // Get handle to textures locations
        val mSamplerLoc = GLES20.glGetUniformLocation(sp_Image, "s_texture")
        // Set the sampler texture unit to 0, where we have saved the texture.
        GLES20.glUniform1i(mSamplerLoc, 0)
        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTexCoordLoc)
    }

    fun setupTriangle() {
        // We will need a randomizer
        val rnd = Random()
        // Our collection of vertices
        val vertices = FloatArray(30 * 4 * 3)

        // Create the vertex data
        for (i in 0..29) {
            val offset_x = rnd.nextInt(swp.toInt())
            val offset_y = rnd.nextInt(shp.toInt())
            val offset = i * 12
            // Create the 2D parts of our 3D vertices, others are default 0.0f
            vertices[offset] = offset_x.toFloat()
            vertices[offset + 1] = offset_y + 30.0f * ssu
            vertices[offset + 2] = 0f
            vertices[offset + 3] = offset_x.toFloat()
            vertices[offset + 4] = offset_y.toFloat()
            vertices[offset + 5] = 0f
            vertices[offset + 6] = offset_x + 30.0f * ssu
            vertices[offset + 7] = offset_y.toFloat()
            vertices[offset + 8] = 0f
            vertices[offset + 9] = offset_x + 30.0f * ssu
            vertices[offset + 10] = offset_y + 30.0f * ssu
            vertices[offset + 11] = 0.0f
        }
        // The indices for all textured quads
        indices = ShortArray(30 * 6)
        var last = 0

        for (i in 0..29) {
            val offset = i * 6
            // We need to set the new indices for the new quad
            indices[offset] = last.toShort()
            indices[offset + 1] = (last + 1).toShort()
            indices[offset + 2] = (last + 2).toShort()
            indices[offset + 3] = last.toShort()
            indices[offset + 4] = (last + 2).toShort()
            indices[offset + 5] = (last + 3).toShort()
            // Our indices are connected to the vertices so we need to keep them
            // in the correct order.
            // normal quad = 0,1,2,0,2,3 so the next one will be 4,5,6,4,6,7
            last += 4
        }

        // The vertex buffer.
        val byteBuffer = ByteBuffer.allocateDirect(vertices.size shl 2)
        byteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = byteBuffer.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)
        // initialize byte buffer for the draw list
        val dlb = ByteBuffer.allocateDirect(indices.size shl 1)
        dlb.order(ByteOrder.nativeOrder())
        drawListBuffer = dlb.asShortBuffer()
        drawListBuffer.put(indices)
        drawListBuffer.position(0)
    }

    fun setupImage() {
        // We will use a randomizer for randomizing the textures from texture atlas.
        // This is strictly optional as it only effects the output of our app,
        // Not the actual knowledge.
        val rnd = Random()
        // 30 imageobjects times 4 vertices times (u and v)
        val uvs = FloatArray(30 shl 3)
        // We will make 30 randomly textures objects
        for (i in 0..29) {
            val random_u_offset = rnd.nextInt(2)
            val random_v_offset = rnd.nextInt(2)
            val offset = i shl 3
            // Adding the UV's using the offsets
            uvs[offset] = random_u_offset * 0.5f
            uvs[offset + 1] = random_v_offset * 0.5f
            uvs[offset + 2] = random_u_offset * 0.5f
            uvs[offset + 3] = (random_v_offset + 1) * 0.5f
            uvs[offset + 4] = (random_u_offset + 1) * 0.5f
            uvs[offset + 5] = (random_v_offset + 1) * 0.5f
            uvs[offset + 6] = (random_u_offset + 1) * 0.5f
            uvs[offset + 7] = random_v_offset * 0.5f
        }
        // The texture buffer
        val byteBuffer = ByteBuffer.allocateDirect(uvs.size shl 2)
        byteBuffer.order(ByteOrder.nativeOrder())
        uvBuffer = byteBuffer.asFloatBuffer()
        uvBuffer.put(uvs)
        uvBuffer.position(0)
        // Generate Textures, if more needed, alter these numbers.
        val texturenames = IntArray(2)
        GLES20.glGenTextures(2, texturenames, 0)
        // Retrieve our image from resources.
        /* int id = context.getResources().getIdentifier("drawable/textureatlas", null, context.getPackageName());
     // Temporary create a bitmap
     Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), id);
     // Bind texture to texturename
     GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
     GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
     // Set filtering
     GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
     GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
     // Load the bitmap into the bound texture.
     GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
     // We are done using the bitmap so we should recycle it.
     bmp.recycle(); */
        // Again for the text texture
        val id = context.resources.getIdentifier("drawable/font", null, context.packageName)
        val bmp = BitmapFactory.decodeResource(context.resources, id)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[1])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        bmp.recycle()
    }
}