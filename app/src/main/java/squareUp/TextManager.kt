package squareUp

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class TextManager {
    var vecs: FloatArray
    var uvs: FloatArray
    var indices: ShortArray
    var colors: FloatArray
    var index_vecs: Int = 0
    var index_indices: Int = 0
    var index_uvs: Int = 0
    var index_colors: Int = 0
    var texturenr: Int = 0
    var uniformscale: Float = 0.toFloat()
    var txtcollection: Vector<TextObject>

    companion object {
        val RI_TEXT_UV_BOX_WIDTH = 0.125f
        val RI_TEXT_WIDTH = 32.0f
        val RI_TEXT_SPACESIZE = 20.0f
        var lSize = intArrayOf(36, 29, 30, 34, 25, 25, 34, 33, 11, 20, 31, 24, 48, 35, 39, 29, 42, 31, 27, 31, 34, 35, 46, 35, 31, 27, 30, 26, 28,
                26, 31, 28, 28, 28, 29, 29, 14, 24, 30, 18, 26, 14, 14, 14, 25, 28, 31, 0, 0, 38, 39, 12, 36, 34, 0, 0, 0, 38, 0, 0, 0, 0, 0, 0)

        private fun convertCharToIndex(cVal: Int): Int {
            var indx = -1

            if (cVal > 64 && cVal < 91) {
                indx = cVal - 65 // A-Z
            } else if (cVal > 96 && cVal < 123) {
                indx = cVal - 97 // a-z
            } else if (cVal > 47 && cVal < 58) {
                indx = cVal - 48 + 26 // 0-9
            } else if (cVal == 43) {
                indx = 38 // +
            } else if (cVal == 45) {
                indx = 39 // -
            } else if (cVal == 33) {
                indx = 36 // !
            } else if (cVal == 63) {
                indx = 37 // ?
            } else if (cVal == 61) {
                indx = 40 // =
            } else if (cVal == 58) {
                indx = 41 // :
            } else if (cVal == 46) {
                indx = 42 // .
            } else if (cVal == 44) {
                indx = 43 // ,
            } else if (cVal == 42) {
                indx = 44 // *
            } else if (cVal == 36) {
                indx = 45  // $
            }

            return indx
        }
    }

    init {
        txtcollection = Vector<TextObject>()
        vecs = FloatArray(3 * 10)
        colors = FloatArray(4 * 10)
        uvs = FloatArray(2 * 10)
        indices = ShortArray(10)
        texturenr = 0
    }

    fun addText(obj: TextObject) {
        txtcollection.add(obj)
    }

    fun updateText(obj: TextObject) {
        txtcollection.removeAt(txtcollection.size - 1)
        txtcollection.add(obj)
    }

    fun setTextureID(texVal: Int) {
        texturenr = texVal
    }

    fun addCharRenderInformation(
            vec: FloatArray,
            cs: FloatArray,
            uv: FloatArray,
            indi: ShortArray) {
        val base = (index_vecs / 3).toShort()

        vec.forEach {
            vecs[index_vecs] = it
            index_vecs++
        }

        cs.forEach {
            colors[index_colors] = it
            index_colors++
        }

        uv.forEach {
            uvs[index_uvs] = it
            index_uvs++
        }

        indi.forEach {
            indices[index_indices] = (base + it).toShort()
            index_indices++
        }
    }

    fun prepareDrawInfo() {
        index_vecs = 0
        index_indices = 0
        index_uvs = 0
        index_colors = 0
        var charcount = 0

        txtcollection.filter { it != null }.forEach { charcount += it.text.length };

        vecs = FloatArray(charcount * 12)
        colors = FloatArray(charcount shl 4)
        uvs = FloatArray(charcount shl 3)
        indices = ShortArray(charcount * 6)
    }

    fun prepareDraw() {
        this.prepareDrawInfo()

        txtcollection.forEach {
            if (it != null) {
                this.convertTextToTriangleInfo(it)
            }
        }
    }

    fun Draw(matrix: FloatArray) {
        GLES20.glUseProgram(sp_Text)

        val vertexBuffer = ByteBuffer.allocateDirect(vecs.size shl 2).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vecs)
        vertexBuffer.position(0)

        val colorBuffer = ByteBuffer.allocateDirect(colors.size shl 2).order(ByteOrder.nativeOrder()).asFloatBuffer()
        colorBuffer.put(colors)
        colorBuffer.position(0)

        val textureBuffer = ByteBuffer.allocateDirect(uvs.size shl 2).order(ByteOrder.nativeOrder()).asFloatBuffer()
        textureBuffer.put(uvs)
        textureBuffer.position(0)

        val drawListBuffer = ByteBuffer.allocateDirect(indices.size shl 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        drawListBuffer.put(indices)
        drawListBuffer.position(0)

        val mPositionHandle = GLES20.glGetAttribLocation(sp_Text, "vPosition")
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        val mTexCoordLoc = GLES20.glGetAttribLocation(sp_Text, "a_texCoord")
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mTexCoordLoc)

        val mColorHandle = GLES20.glGetAttribLocation(sp_Text, "a_Color")
        GLES20.glEnableVertexAttribArray(mColorHandle)
        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer)

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(sp_Text, "uMVPMatrix"), 1, false, matrix, 0)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(sp_Text, "s_texture"), texturenr)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)

        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTexCoordLoc)
        GLES20.glDisableVertexAttribArray(mColorHandle)
    }

    private fun convertTextToTriangleInfo(textObj: TextObject) {
        var x = textObj.x
        val y = textObj.y
        val text = textObj.text

        for (j in 0..text.length - 1) {
            val indx = convertCharToIndex(text[j].toInt())

            if (indx == -1) {
                x += ((RI_TEXT_SPACESIZE) * uniformscale)
                continue
            }

            val vec = floatArrayOf(
                    x, y + RI_TEXT_WIDTH * uniformscale, 0.99f, x, y, 0.99f,
                    x + RI_TEXT_WIDTH * uniformscale, y, 0.99f,
                    x + RI_TEXT_WIDTH * uniformscale,
                    y + RI_TEXT_WIDTH * uniformscale, 0.99f)

            val colors = floatArrayOf(textObj.color[0], textObj.color[1], textObj.color[2], textObj.color[3], textObj.color[0],
                    textObj.color[1], textObj.color[2], textObj.color[3], textObj.color[0], textObj.color[1], textObj.color[2],
                    textObj.color[3], textObj.color[0], textObj.color[1], textObj.color[2], textObj.color[3])
            val uv = FloatArray(8)
            val u = indx % 8 * RI_TEXT_UV_BOX_WIDTH
            val u2 = u + RI_TEXT_UV_BOX_WIDTH
            val v = indx / 8 * RI_TEXT_UV_BOX_WIDTH
            val v2 = v + RI_TEXT_UV_BOX_WIDTH

            uv[0] = u + 0.001f
            uv[1] = v + 0.001f
            uv[2] = u + 0.001f
            uv[3] = v2 - 0.001f
            uv[4] = u2 - 0.001f
            uv[5] = v2 - 0.001f
            uv[6] = u2 - 0.001f
            uv[7] = v + 0.001f

            this.addCharRenderInformation(vec, colors, uv, shortArrayOf(0, 1, 2, 0, 2, 3))

            x += (lSize[indx] / 2) * uniformscale
        }
    }
}