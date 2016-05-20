package squareUp

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
        val COLUMNS = 3
        val BOTTOM_OF_SCREEN = -1.0f
        val TOP_OF_SCREEN = 1.0f
        val XGAP = 0.5f
        val YGAP = -0.465f
        val START_SPEED = -0.01f
        val MAX_SPEED = -0.03f
        val ACCEL = -0.000005f
        lateinit var indices: ShortArray

        fun loadShader(
                type: Int,
                shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)

            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            return shader
        }
    }

    var baseSpeed: Float = 0.toFloat()
    var started: Boolean = false
    var jumpCol = 1
    var nextCol: Int = 0
    val mProtrusions = arrayOfNulls<Protrusion>(6)
    lateinit var mPlayer: Player
    val protQueue = LinkedList<Int>()
    val context: Context
    var score: Int = 0
    lateinit var config: EGLConfig
    lateinit var vertexBuffer: FloatBuffer
    lateinit var uvBuffer: FloatBuffer
    lateinit var drawListBuffer: ShortBuffer
    val mtrxProjection = FloatArray(16)
    val mtrxView = FloatArray(16)
    val mtrxProjectionAndView = FloatArray(16)
    lateinit var tm: TextManager
    lateinit var scoreTxt: TextObject
    var ssu = 1.0f
    var ssx = 1.0f
    var ssy = 1.0f
    var swp = 320.0f
    var shp = 480.0f
    val mMVPMatrix = FloatArray(16)
    val mProjectionMatrix = FloatArray(16)
    val mViewMatrix = FloatArray(16)

    init {
        this.context = context
    }

    override fun onSurfaceCreated(
            unused: GL10,
            c: EGLConfig) {
        config = c
        protQueue.clear()
        mPlayer = Player()
        mPlayer.falling = false
        mPlayer.inAir = false
        mPlayer.jump = false
        mPlayer.goingLeft = false
        mPlayer.goingRight = false
        mPlayer.endOfJump = false
        jumpCol = 1
        this.setupScaling()
        this.setupTriangle()
        this.setupImage()
        this.setupText()

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        sp_Image = GLES20.glCreateProgram()
        GLES20.glAttachShader(sp_Image, loadShader(GLES20.GL_VERTEX_SHADER, vs_Image))
        GLES20.glAttachShader(sp_Image, loadShader(GLES20.GL_FRAGMENT_SHADER, fs_Image))
        GLES20.glLinkProgram(sp_Image)

        sp_Text = GLES20.glCreateProgram()
        GLES20.glAttachShader(sp_Text, loadShader(GLES20.GL_VERTEX_SHADER, vs_Text))
        GLES20.glAttachShader(sp_Text, loadShader(GLES20.GL_FRAGMENT_SHADER, fs_Text))
        GLES20.glLinkProgram(sp_Text)

        GLES20.glUseProgram(sp_Image)
        mPlayer.transMatrix[13] = mPlayer.STARTPOS

        for (i in mProtrusions.indices) {
            mProtrusions[i] = Protrusion((i % COLUMNS).toDouble())

            if (i == 1) {
                mProtrusions[i]!!.transMatrix[13] = mPlayer.STARTPOS
                protQueue.push(i)
            } else if (i > 1 && i <= 3) {
                mProtrusions[i]!!.transMatrix[13] = mPlayer.STARTPOS + (i - 1) * XGAP
                protQueue.push(i)
            } else {
                mProtrusions[i]!!.transMatrix[13] = -2.0f
            }
        }
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, -3.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)
        Matrix.translateM(mPlayer.transMatrix, 0, mPlayer.jumpXAccel, baseSpeed + mPlayer.jumpYAccel, 0.0f)
        Matrix.multiplyMM(mPlayer.playerMatrix, 0, mMVPMatrix, 0, mPlayer.transMatrix, 0)

        this.Render(mtrxProjectionAndView)
        scoreTxt.text = "$score "

        tm.updateText(scoreTxt)
        tm.prepareDraw()
        tm.Draw(mtrxProjectionAndView)

        mProtrusions.filter { it!!.transMatrix[13] >= BOTTOM_OF_SCREEN - 0.5f }.forEach {
            Matrix.translateM(it?.transMatrix, 0, 0.0f, baseSpeed, 0.0f)
            Matrix.multiplyMM(it?.protMatrix, 0, mMVPMatrix, 0, it?.transMatrix, 0)
        }

        if (started) {
            if (baseSpeed == 0.0f && !mPlayer.inAir) {
                baseSpeed = START_SPEED
                score = 0
            } else if (baseSpeed >= MAX_SPEED) {
                baseSpeed += ACCEL
            }

            val randNum = Random().nextInt(COLUMNS)

            if (mProtrusions[protQueue.first]!!.transMatrix[13] < 1 - XGAP) {
                for (i in 0..mProtrusions.size - 1) {
                    if (i % COLUMNS == randNum) {
                        if (mProtrusions[i]!!.transMatrix[13] < BOTTOM_OF_SCREEN) {
                            mProtrusions[i]!!.transMatrix[13] = TOP_OF_SCREEN
                            protQueue.push(i)

                            break
                        }
                    }
                }
            }

            if (!mPlayer.falling) {
                if (mPlayer.jump && !mPlayer.inAir) {
                    if (protQueue.size > 1) {
                        protQueue.removeLast()
                        mPlayer.inAir = true
                        jumpCol = nextCol
                    }

                    mPlayer.jump = false
                }

                if (mPlayer.inAir) {
                    val playerYPos = mPlayer.transMatrix[13]
                    val lastProtrutionYPos = mProtrusions[protQueue.last]!!.transMatrix[13]

                    if (playerYPos >= lastProtrutionYPos + mPlayer.HEIGHT && !mPlayer.endOfJump) {
                        mPlayer.transMatrix[13] = lastProtrutionYPos + mPlayer.HEIGHT - 0.001f
                        mPlayer.jumpYAccel = -baseSpeed * (mPlayer.JUMP_SPEED * 22.0f * (lastProtrutionYPos + mPlayer.HEIGHT
                                - mPlayer.transMatrix[13]))
                        mPlayer.endOfJump = true
                    } else if (mPlayer.endOfJump) {
                        if (playerYPos > lastProtrutionYPos) {
                            mPlayer.jumpYAccel = -baseSpeed * (mPlayer.JUMP_SPEED * 22.0f * (lastProtrutionYPos + mPlayer.HEIGHT - playerYPos))
                        } else {
                            mPlayer.transMatrix[13] = lastProtrutionYPos

                            if (jumpCol != protQueue.last % 3) {
                                mPlayer.falling = true
                            } else {
                                mPlayer.jumpYAccel = 0.0f
                            }

                            mPlayer.endOfJump = false
                        }
                    } else if (playerYPos < lastProtrutionYPos) {
                        mPlayer.jumpYAccel = baseSpeed * (mPlayer.JUMP_SPEED * 22.0f * (lastProtrutionYPos + mPlayer.HEIGHT - playerYPos))
                    }

                    val playerXPos = mPlayer.transMatrix[12]

                    if (playerXPos <= (jumpCol - 1) * YGAP && mPlayer.goingRight || playerXPos >= (jumpCol - 1) * YGAP && mPlayer.goingLeft) {
                        if (jumpCol != protQueue.last % 3) {
                            mPlayer.falling = true
                        } else {
                            mPlayer.jumpXAccel = 0.0f
                        }

                        mPlayer.transMatrix[12] = (jumpCol - 1) * YGAP
                        mPlayer.goingLeft = false
                        mPlayer.goingRight = false
                    } else if (!mPlayer.goingRight && !mPlayer.goingLeft) {
                        if (playerXPos < (jumpCol - 1) * YGAP) {
                            mPlayer.jumpXAccel = baseSpeed * 1.3f * (mPlayer.JUMP_SPEED + mPlayer.JUMP_SPEED * Math.abs(jumpCol % 2
                                    - playerXPos / YGAP))
                            mPlayer.goingLeft = true
                        } else if (playerXPos > (jumpCol - 1) * YGAP) {
                            mPlayer.jumpXAccel = -baseSpeed * 1.3f * (mPlayer.JUMP_SPEED + mPlayer.JUMP_SPEED * Math.abs(jumpCol % 2
                                    + playerXPos / YGAP))
                            mPlayer.goingRight = true
                        }
                    }

                    if (mPlayer.jumpYAccel == 0.0f && mPlayer.jumpXAccel == 0.0f) {
                        score++
                        mPlayer.inAir = false
                    }
                }
            } else {
                baseSpeed = 0.0f
            }

            if (mPlayer.transMatrix[13] < BOTTOM_OF_SCREEN - Protrusion.HEIGHT - mPlayer.HEIGHT) {
                started = false
                baseSpeed = 0.0f
                mPlayer.jumpYAccel = 0.0f
                mPlayer.jumpXAccel = 0.0f
                this.onSurfaceCreated(unused, config)
            }
        }

        mPlayer.draw(mPlayer.playerMatrix)
        mProtrusions.filter { it!!.transMatrix[13] >= BOTTOM_OF_SCREEN - Protrusion.HEIGHT }.forEach { it?.draw(it.protMatrix) }
    }

    override fun onSurfaceChanged(
            unused: GL10,
            wid: Int,
            hgt: Int) {
        GLES20.glViewport(0, 0, wid, hgt)

        val ratio = wid.toFloat() / hgt
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 3.0f, 7.0f)

        for (i in 0..15) {
            mtrxProjection[i] = 0.0f
            mtrxView[i] = 0.0f
            mtrxProjectionAndView[i] = 0.0f
        }

        Matrix.orthoM(mtrxProjection, 0, 0.0f, wid.toFloat(), 0.0f, hgt.toFloat(), 0.0f, 50.0f)
        Matrix.setLookAtM(mtrxView, 0, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0)

        this.setupScaling()
    }

    fun setupText() {
        tm = TextManager()

        tm.setTextureID(1)

        tm.uniformscale = ssu

        scoreTxt = TextObject("$score ", context.resources.displayMetrics.widthPixels / 5.0f,
                9 * context.resources.displayMetrics.heightPixels / 10.0f)

        tm.addText(scoreTxt)

        tm.prepareDraw()
    }

    fun setupScaling() {
        val displayMetrics = context.resources.displayMetrics
        swp = displayMetrics.widthPixels.toFloat()
        shp = displayMetrics.heightPixels.toFloat()

        ssx = swp / 320.0f
        ssy = shp / 480.0f

        ssu = if (ssx > ssy) ssy else ssx
    }

    private fun Render(matrix: FloatArray) {
        GLES20.glUseProgram(sp_Image)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        val mPositionHandle = GLES20.glGetAttribLocation(sp_Image, "vPosition")
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(mPositionHandle)

        val mTexCoordLoc = GLES20.glGetAttribLocation(sp_Image, "a_texCoord")
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer)
        GLES20.glEnableVertexAttribArray(mTexCoordLoc)

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(sp_Image, "uMVPMatrix"), 1, false, matrix, 0)

        GLES20.glUniform1i(GLES20.glGetUniformLocation(sp_Image, "s_texture"), 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)

        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTexCoordLoc)
    }

    fun setupTriangle() {
        val rnd = Random()
        val vertices = FloatArray(30 * 4 * 3)

        for (i in 0..29) {
            val offset_x = rnd.nextInt(swp.toInt())
            val offset_y = rnd.nextInt(shp.toInt())
            val offset = i * 12

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

        indices = ShortArray(30 * 6)

        var last = 0

        for (i in 0..29) {
            val offset = i * 6

            indices[offset] = last.toShort()
            indices[offset + 1] = (last + 1).toShort()
            indices[offset + 2] = (last + 2).toShort()
            indices[offset + 3] = last.toShort()
            indices[offset + 4] = (last + 2).toShort()
            indices[offset + 5] = (last + 3).toShort()

            last += 4
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size shl 2).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        drawListBuffer = ByteBuffer.allocateDirect(indices.size shl 1).order(ByteOrder.nativeOrder()).asShortBuffer()
        drawListBuffer.put(indices)
        drawListBuffer.position(0)
    }

    fun setupImage() {
        val rnd = Random()
        val uvs = FloatArray(30 shl 3)

        for (i in 0..29) {
            val random_u_offset = rnd.nextInt(2)
            val random_v_offset = rnd.nextInt(2)
            val offset = i shl 3

            uvs[offset] = random_u_offset * 0.5f
            uvs[offset + 1] = random_v_offset * 0.5f
            uvs[offset + 2] = random_u_offset * 0.5f
            uvs[offset + 3] = (random_v_offset + 1) * 0.5f
            uvs[offset + 4] = (random_u_offset + 1) * 0.5f
            uvs[offset + 5] = (random_v_offset + 1) * 0.5f
            uvs[offset + 6] = (random_u_offset + 1) * 0.5f
            uvs[offset + 7] = random_v_offset * 0.5f
        }

        val byteBuffer = ByteBuffer.allocateDirect(uvs.size shl 2)
        byteBuffer.order(ByteOrder.nativeOrder())
        uvBuffer = byteBuffer.asFloatBuffer()
        uvBuffer.put(uvs)
        uvBuffer.position(0)

        val texturenames = IntArray(2)
        GLES20.glGenTextures(2, texturenames, 0)

        //        int id = context.getResources().getIdentifier("drawable/textureatlas", null, context.getPackageName());
        //        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), id);
        //
        //        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
        //
        //        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        //        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //
        //        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        //        bmp.recycle();

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[1])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        val bmp = BitmapFactory.decodeResource(context.resources, context.resources.getIdentifier("drawable/font", null, context.packageName))
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        bmp.recycle()
    }
}