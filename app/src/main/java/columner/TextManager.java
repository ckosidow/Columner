package columner;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Iterator;
import java.util.Vector;

public class TextManager {
    private static final float RI_TEXT_UV_BOX_WIDTH = 0.125f;
    private static final float RI_TEXT_WIDTH = 32.0f;
    private static final float RI_TEXT_SPACESIZE = 20.0f;

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private FloatBuffer colorBuffer;
    private ShortBuffer drawListBuffer;

    private float[] vecs;
    private float[] uvs;
    private short[] indices;
    private float[] colors;

    private int index_vecs;
    private int index_indices;
    private int index_uvs;
    private int index_colors;

    private int texturenr;

    private float uniformscale;

    public static int[] lSize = {36, 29, 30, 34, 25, 25, 34, 33,
            11, 20, 31, 24, 48, 35, 39, 29,
            42, 31, 27, 31, 34, 35, 46, 35,
            31, 27, 30, 26, 28, 26, 31, 28,
            28, 28, 29, 29, 14, 24, 30, 18,
            26, 14, 14, 14, 25, 28, 31, 0,
            0, 38, 39, 12, 36, 34, 0, 0,
            0, 38, 0, 0, 0, 0, 0, 0};

    public Vector<TextObject> txtcollection;

    public TextManager() {
        // Create our container
        txtcollection = new Vector<>();

        // Create the arrays
        vecs = new float[3 * 10];
        colors = new float[4 * 10];
        uvs = new float[2 * 10];
        indices = new short[10];

        // init as 0 as default
        texturenr = 0;
    }

    public void addText(final TextObject obj) {
        // Add text object to our collection
        txtcollection.add(obj);
    }

    public void updateText(final TextObject obj) {
        // Add text object to our collection
        txtcollection.remove(txtcollection.size() - 1);
        txtcollection.add(obj);
    }

    public void setTextureID(final int val) {
        texturenr = val;
    }

    public void addCharRenderInformation(
            final float[] vec,
            final float[] cs,
            final float[] uv,
            final short[] indi
    ) {
        // We need a base value because the object has indices related to
        // that object and not to this collection so basicly we need to
        // translate the indices to align with the vertexlocation in ou
        // vecs array of vectors.
        final short base = (short) (index_vecs / 3);

        // We should add the vec, translating the indices to our saved vector
        for (final float vector : vec) {
            vecs[index_vecs] = vector;
            index_vecs++;
        }

        // We should add the colors, so we can use the same texture for multiple effects.
        for (final float color : cs) {
            colors[index_colors] = color;
            index_colors++;
        }

        // We should add the uvs
        for (final float unit : uv) {
            uvs[index_uvs] = unit;
            index_uvs++;
        }

        // We handle the indices
        for (final float index : indi) {
            indices[index_indices] = (short) (base + index);
            index_indices++;
        }
    }

    public void prepareDrawInfo() {
        // Reset the indices.
        index_vecs = 0;
        index_indices = 0;
        index_uvs = 0;
        index_colors = 0;

        // Get the total amount of characters
        int charcount = 0;

        for (final TextObject txt : txtcollection) {
            if (txt != null && txt.text != null) {
                charcount += txt.text.length();
            }
        }

        // Create the arrays we need with the correct size.
        vecs = null;
        colors = null;
        uvs = null;
        indices = null;

        vecs = new float[charcount * 12];
        colors = new float[charcount << 4];
        uvs = new float[charcount << 3];
        indices = new short[charcount * 6];
    }

    public void prepareDraw() {
        // Setup all the arrays
        this.prepareDrawInfo();

        // Using the iterator protects for problems with concurrency
        for (final Iterator<TextObject> it = txtcollection.iterator(); it.hasNext(); ) {
            final TextObject txt = it.next();

            if (txt != null && txt.text != null) {
                this.convertTextToTriangleInfo(txt);
            }
        }
    }

    public void Draw(final float[] matrix) {
        // Set the correct shader for our grid object.
        GLES20.glUseProgram(riGraphicTools.sp_Text);

        // The vertex buffer.
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vecs.length << 2);
        byteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuffer.asFloatBuffer();
        vertexBuffer.put(vecs);
        vertexBuffer.position(0);

        // The vertex buffer.
        final ByteBuffer bb3 = ByteBuffer.allocateDirect(colors.length << 2);
        bb3.order(ByteOrder.nativeOrder());
        colorBuffer = bb3.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);

        // The texture buffer
        final ByteBuffer bb2 = ByteBuffer.allocateDirect(uvs.length << 2);
        bb2.order(ByteOrder.nativeOrder());
        textureBuffer = bb2.asFloatBuffer();
        textureBuffer.put(uvs);
        textureBuffer.position(0);

        // initialize byte buffer for the draw list
        final ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length << 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

        // get handle to vertex shader's vPosition member
        final int mPositionHandle = GLES20.glGetAttribLocation(riGraphicTools.sp_Text, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the background coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer);

        final int mTexCoordLoc = GLES20.glGetAttribLocation(riGraphicTools.sp_Text, "a_texCoord");

        // Prepare the texturecoordinates
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT,
                false,
                0, textureBuffer);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);

        final int mColorHandle = GLES20.glGetAttribLocation(riGraphicTools.sp_Text, "a_Color");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mColorHandle);

        // Prepare the background coordinate data
        GLES20.glVertexAttribPointer(mColorHandle, 4,
                GLES20.GL_FLOAT, false,
                0, colorBuffer);

        // get handle to shape's transformation matrix
        final int mtrxhandle = GLES20.glGetUniformLocation(riGraphicTools.sp_Text, "uMVPMatrix");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, matrix, 0);

        final int mSamplerLoc = GLES20.glGetUniformLocation(riGraphicTools.sp_Text, "s_texture");

        // Set the sampler texture unit to our selected id
        GLES20.glUniform1i(mSamplerLoc, texturenr);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);
        GLES20.glDisableVertexAttribArray(mColorHandle);

    }

    private static int convertCharToIndex(final int cVal) {
        int indx = -1;

        // Retrieve the index
        if (cVal > 64 && cVal < 91) { // A-Z
            indx = cVal - 65;
        } else if (cVal > 96 && cVal < 123) { // a-z
            indx = cVal - 97;
        } else if (cVal > 47 && cVal < 58) { // 0-9
            indx = cVal - 48 + 26;
        } else if (cVal == 43) { // +
            indx = 38;
        } else if (cVal == 45) { // -
            indx = 39;
        } else if (cVal == 33) { // !
            indx = 36;
        } else if (cVal == 63) { // ?
            indx = 37;
        } else if (cVal == 61) { // =
            indx = 40;
        } else if (cVal == 58) { // :
            indx = 41;
        } else if (cVal == 46) { // .
            indx = 42;
        } else if (cVal == 44) { // ,
            indx = 43;
        } else if (cVal == 42) { // *
            indx = 44;
        } else if (cVal == 36) { // $
            indx = 45;
        }

        return indx;
    }

    private void convertTextToTriangleInfo(final TextObject val) {
        // Get attributes from text object
        float x = val.x;
        final float y = val.y;
        final String text = val.text;

        // Create
        for (int j = 0; j < text.length(); j++) {
            final int indx = this.convertCharToIndex(text.charAt(j));

            if (indx == -1) {
                // unknown character, we will add a space for it to be save.
                x += ((RI_TEXT_SPACESIZE) * uniformscale);
                continue;
            }

            // Calculate the uv parts
            final int row = indx / 8;
            final int col = indx % 8;

            final float v = row * RI_TEXT_UV_BOX_WIDTH;
            final float v2 = v + RI_TEXT_UV_BOX_WIDTH;
            final float u = col * RI_TEXT_UV_BOX_WIDTH;
            final float u2 = u + RI_TEXT_UV_BOX_WIDTH;

            // Creating the triangle information
            final float[] vec = new float[12];
            final float[] uv = new float[8];
            final float[] colors;

            vec[0] = x;
            vec[1] = y + RI_TEXT_WIDTH * uniformscale;
            vec[2] = 0.99f;
            vec[3] = x;
            vec[4] = y;
            vec[5] = 0.99f;
            vec[6] = x + RI_TEXT_WIDTH * uniformscale;
            vec[7] = y;
            vec[8] = 0.99f;
            vec[9] = x + RI_TEXT_WIDTH * uniformscale;
            vec[10] = y + RI_TEXT_WIDTH * uniformscale;
            vec[11] = 0.99f;

            colors = new float[]{val.color[0], val.color[1], val.color[2], val.color[3],
                    val.color[0], val.color[1], val.color[2], val.color[3],
                    val.color[0], val.color[1], val.color[2], val.color[3],
                    val.color[0], val.color[1], val.color[2], val.color[3]
            };

            // 0.001f = texture bleeding hack/fix
            uv[0] = u + 0.001f;
            uv[1] = v + 0.001f;
            uv[2] = u + 0.001f;
            uv[3] = v2 - 0.001f;
            uv[4] = u2 - 0.001f;
            uv[5] = v2 - 0.001f;
            uv[6] = u2 - 0.001f;
            uv[7] = v + 0.001f;

            final short[] inds = {0, 1, 2, 0, 2, 3};

            // Add our triangle information to our collection for 1 render call.
            this.addCharRenderInformation(vec, colors, uv, inds);

            // Calculate the new position
            x += ((lSize[indx] / 2) * uniformscale);
        }
    }

    public float getUniformscale() {
        return uniformscale;
    }

    public void setUniformscale(final float uniformscale) {
        this.uniformscale = uniformscale;
    }
}