package columner

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class TextManager {
    private var vecs: FloatArray
    private var uvs: FloatArray
    private var indices: ShortArray
    private var colors: FloatArray
    private var index_vecs: Int = 0
    private var index_indices: Int = 0
    private var index_uvs: Int = 0
    private var index_colors: Int = 0
    private var texturenr: Int = 0
    var uniformscale: Float = 0.toFloat()
    var txtcollection: Vector<TextObject>

    init {
        // Create our container
        txtcollection = Vector<TextObject>()
        // Create the arrays
        vecs = FloatArray(3 * 10)
        colors = FloatArray(4 * 10)
        uvs = FloatArray(2 * 10)
        indices = ShortArray(10)
        // init as 0 as default
        texturenr = 0
    }

    fun addText(obj: TextObject) {
        // Add text object to our collection
        txtcollection.add(obj)
    }

    fun updateText(obj: TextObject) {
        // Add text object to our collection
        txtcollection.removeAt(txtcollection.size - 1)
        txtcollection.add(obj)
    }

    fun setTextureID(`val`: Int) {
        texturenr = `val`
    }

    fun addCharRenderInformation(
            vec: FloatArray,
            cs: FloatArray,
            uv: FloatArray,
            indi: ShortArray) {
        // We need a base value because the object has indices related to
        // that object and not to this collection so basicly we need to
        // translate the indices to align with the vertexlocation in ou
        // vecs array of vectors.
        val base = (index_vecs / 3).toShort()
        // We should add the vec, translating the indices to our saved vector
        for (vector in vec) {
            vecs[index_vecs] = vector
            index_vecs++
        }
        // We should add the colors, so we can use the same texture for multiple effects.
        for (color in cs) {
            colors[index_colors] = color
            index_colors++
        }
        // We should add the uvs
        for (unit in uv) {
            uvs[index_uvs] = unit
            index_uvs++
        }
        // We handle the indices
        for (index in indi) {
            indices[index_indices] = (base + index).toShort()
            index_indices++
        }
    }

    fun prepareDrawInfo() {
        // Reset the indices.
        index_vecs = 0
        index_indices = 0
        index_uvs = 0
        index_colors = 0
        // Get the total amount of characters
        var charcount = 0

        for (txt in txtcollection) {
            if (txt != null) {
                charcount += txt.text.length
            }
        }

        // Create the arrays we need with the correct size.
        vecs = FloatArray(charcount * 12)
        colors = FloatArray(charcount shl 4)
        uvs = FloatArray(charcount shl 3)
        indices = ShortArray(charcount * 6)
    }

    fun prepareDraw() {
        // Setup all the arrays
        this.prepareDrawInfo()
        // Using the iterator protects for problems with concurrency
        val it = txtcollection.iterator()
        while (it.hasNext()) {
            val txt = it.next()
            if (txt != null) {
                this.convertTextToTriangleInfo(txt)
            }
        }
    }

    fun Draw(matrix: FloatArray) {
        // Set the correct shader for our grid object.
        GLES20.glUseProgram(sp_Text)
        // The vertex buffer.
        val byteBuffer = ByteBuffer.allocateDirect(vecs.size shl 2)
        byteBuffer.order(ByteOrder.nativeOrder())
        var vertexBuffer = byteBuffer.asFloatBuffer()
        vertexBuffer.put(vecs)
        vertexBuffer.position(0)
        // The vertex buffer.
        val bb3 = ByteBuffer.allocateDirect(colors.size shl 2)
        bb3.order(ByteOrder.nativeOrder())
        var colorBuffer = bb3.asFloatBuffer()
        colorBuffer.put(colors)
        colorBuffer.position(0)
        // The texture buffer
        val bb2 = ByteBuffer.allocateDirect(uvs.size shl 2)
        bb2.order(ByteOrder.nativeOrder())
        var textureBuffer = bb2.asFloatBuffer()
        textureBuffer.put(uvs)
        textureBuffer.position(0)
        // initialize byte buffer for the draw list
        val dlb = ByteBuffer.allocateDirect(indices.size shl 2)
        dlb.order(ByteOrder.nativeOrder())
        var drawListBuffer = dlb.asShortBuffer()
        drawListBuffer.put(indices)
        drawListBuffer.position(0)

        // get handle to vertex shader's vPosition member
        val mPositionHandle = GLES20.glGetAttribLocation(sp_Text, "vPosition")
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        // Prepare the background coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer)
        val mTexCoordLoc = GLES20.glGetAttribLocation(sp_Text, "a_texCoord")
        // Prepare the texturecoordinates
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT,
                false,
                0, textureBuffer)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mTexCoordLoc)
        val mColorHandle = GLES20.glGetAttribLocation(sp_Text, "a_Color")
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mColorHandle)
        // Prepare the background coordinate data
        GLES20.glVertexAttribPointer(mColorHandle, 4,
                GLES20.GL_FLOAT, false,
                0, colorBuffer)
        // get handle to shape's transformation matrix
        val mtrxhandle = GLES20.glGetUniformLocation(sp_Text, "uMVPMatrix")
        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, matrix, 0)
        val mSamplerLoc = GLES20.glGetUniformLocation(sp_Text, "s_texture")
        // Set the sampler texture unit to our selected id
        GLES20.glUniform1i(mSamplerLoc, texturenr)
        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTexCoordLoc)
        GLES20.glDisableVertexAttribArray(mColorHandle)
    }

    private fun convertTextToTriangleInfo(textObj: TextObject) {
        // Get attributes from text object
        var x = textObj.x
        val y = textObj.y
        val text = textObj.text

        // Create
        for (j in 0..text.length - 1) {
            val indx = convertCharToIndex(text.get(j).toInt())

            if (indx == -1) {
                // unknown character, we will add a space for it to be save.
                x += ((RI_TEXT_SPACESIZE) * uniformscale)
                continue
            }

            // Calculate the uv parts
            val row = indx / 8
            val col = indx % 8
            val v = row * RI_TEXT_UV_BOX_WIDTH
            val v2 = v + RI_TEXT_UV_BOX_WIDTH
            val u = col * RI_TEXT_UV_BOX_WIDTH
            val u2 = u + RI_TEXT_UV_BOX_WIDTH
            // Creating the triangle information
            val vec = FloatArray(12)
            val uv = FloatArray(8)
            val colors: FloatArray
            vec[0] = x
            vec[1] = y + RI_TEXT_WIDTH * uniformscale
            vec[2] = 0.99f
            vec[3] = x
            vec[4] = y
            vec[5] = 0.99f
            vec[6] = x + RI_TEXT_WIDTH * uniformscale
            vec[7] = y
            vec[8] = 0.99f
            vec[9] = x + RI_TEXT_WIDTH * uniformscale
            vec[10] = y + RI_TEXT_WIDTH * uniformscale
            vec[11] = 0.99f
            colors = floatArrayOf(textObj.color[0], textObj.color[1], textObj.color[2], textObj.color[3], textObj.color[0],
                    textObj.color[1], textObj.color[2], textObj.color[3], textObj.color[0], textObj.color[1], textObj.color[2],
                    textObj.color[3], textObj.color[0], textObj.color[1], textObj.color[2], textObj.color[3])
            // 0.001f = texture bleeding hack/fix
            uv[0] = u + 0.001f
            uv[1] = v + 0.001f
            uv[2] = u + 0.001f
            uv[3] = v2 - 0.001f
            uv[4] = u2 - 0.001f
            uv[5] = v2 - 0.001f
            uv[6] = u2 - 0.001f
            uv[7] = v + 0.001f
            val inds = shortArrayOf(0, 1, 2, 0, 2, 3)
            // Add our triangle information to our collection for 1 render call.
            this.addCharRenderInformation(vec, colors, uv, inds)
            // Calculate the new position
            x += (lSize[indx] / 2) * uniformscale
        }
    }

    companion object {
        private val RI_TEXT_UV_BOX_WIDTH = 0.125f
        private val RI_TEXT_WIDTH = 32.0f
        private val RI_TEXT_SPACESIZE = 20.0f
        var lSize = intArrayOf(36, 29, 30, 34, 25, 25, 34, 33, 11, 20, 31, 24, 48, 35, 39, 29, 42, 31, 27, 31, 34, 35, 46, 35, 31, 27, 30, 26, 28, 26, 31, 28, 28, 28, 29, 29, 14, 24, 30, 18, 26, 14, 14, 14, 25, 28, 31, 0, 0, 38, 39, 12, 36, 34, 0, 0, 0, 38, 0, 0, 0, 0, 0, 0)

        private fun convertCharToIndex(cVal: Int): Int {
            var indx = -1
            // Retrieve the index
            if (cVal > 64 && cVal < 91) {
                // A-Z
                indx = cVal - 65
            } else if (cVal > 96 && cVal < 123) {
                // a-z
                indx = cVal - 97
            } else if (cVal > 47 && cVal < 58) {
                // 0-9
                indx = cVal - 48 + 26
            } else if (cVal == 43) {
                // +
                indx = 38
            } else if (cVal == 45) {
                // -
                indx = 39
            } else if (cVal == 33) {
                // !
                indx = 36
            } else if (cVal == 63) {
                // ?
                indx = 37
            } else if (cVal == 61) {
                // =
                indx = 40
            } else if (cVal == 58) {
                // :
                indx = 41
            } else if (cVal == 46) {
                // .
                indx = 42
            } else if (cVal == 44) {
                // ,
                indx = 43
            } else if (cVal == 42) {
                // *
                indx = 44
            } else if (cVal == 36) {
                // $
                indx = 45
            }
            return indx
        }
    }
}