package com.columnber.columnber;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by CKosidowski11 on 2/16/2015.
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {
    public float baseSpeed = 0;
    public float jumpYAccel = 0;
    public float jumpXAccel = 0;
    public boolean started = false;
    public boolean jump = false;
    public boolean inAir = false;
    public boolean endOfJump = false;
    public boolean goingLeft = false;
    public boolean goingRight = false;
    public boolean falling = false;
    private int jumpCol = 1;
    public int nextCol;
    private Protrusion[] mProtrusions = new Protrusion[9];
    private Player mPlayer;
    private LinkedList<Integer> protQueue = new LinkedList<>();
    //    private GLText mGLText;
    private static final int COLUMNS = 3;
    private static final float BOTTOM_OF_SCREEN = -1.0f;
    private static final float TOP_OF_SCREEN = 1.0f;
    private static final float XGAP = 0.5f;
    private static final float YGAP = -0.465f;
    private static final float STARTPOS = BOTTOM_OF_SCREEN + XGAP;
    private static final float JUMP_SPEED = -1f;
    private static final float START_SPEED = -0.01f;
    private static final float MAX_SPEED = -0.03f;
    private static final float ACCEL = -.000005f;
    private boolean needNext;
    private Context context;
    private int score = 0;

    public static float vertices[];
    public static short indices[];
    public static float uvs[];
    public FloatBuffer vertexBuffer;
    public FloatBuffer uvBuffer;
    public ShortBuffer drawListBuffer;
    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];
    private TextManager tm;
    private TextObject scoreTxt;
    float ssu = 1.0f;
    float ssx = 1.0f;
    float ssy = 1.0f;
    float swp = 320.0f;
    float shp = 480.0f;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    public MyGLRenderer(Context c) {
        context = c;
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Setup our scaling system
        SetupScaling();
        // Create the triangles
        SetupTriangle();
        // Create the image information
        SetupImage();
        // Create our texts
        SetupText();

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Create the shaders, images
        int vertexShader = riGraphicTools.loadShader(GLES20.GL_VERTEX_SHADER, riGraphicTools.vs_Image);
        int fragmentShader = riGraphicTools.loadShader(GLES20.GL_FRAGMENT_SHADER, riGraphicTools.fs_Image);

        riGraphicTools.sp_Image = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(riGraphicTools.sp_Image, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(riGraphicTools.sp_Image, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(riGraphicTools.sp_Image);                  // creates OpenGL ES program executables

        // Text shader
        int vshadert = riGraphicTools.loadShader(GLES20.GL_VERTEX_SHADER,
                riGraphicTools.vs_Text);
        int fshadert = riGraphicTools.loadShader(GLES20.GL_FRAGMENT_SHADER,
                riGraphicTools.fs_Text);

        riGraphicTools.sp_Text = GLES20.glCreateProgram();
        GLES20.glAttachShader(riGraphicTools.sp_Text, vshadert);
        GLES20.glAttachShader(riGraphicTools.sp_Text, fshadert);
        GLES20.glLinkProgram(riGraphicTools.sp_Text);

        // Set our shader programm
        GLES20.glUseProgram(riGraphicTools.sp_Image);

        mPlayer = new Player();
        mPlayer.TransMatrix[13] = STARTPOS;

        for (int i = 0; i < mProtrusions.length; i++) {
            mProtrusions[i] = new Protrusion(i % COLUMNS);

            if (i == 1) {
                mProtrusions[i].TransMatrix[13] = STARTPOS;
                protQueue.push(i);
            } else if (i > 1 && i <= 3) {
                mProtrusions[i].TransMatrix[13] = STARTPOS + ((i - 1) * XGAP);
                protQueue.push(i);
            } else
                mProtrusions[i].TransMatrix[13] = -2.0f;
        }
    }

    public void onDrawFrame(GL10 unused) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        Matrix.translateM(mPlayer.TransMatrix, 0, jumpXAccel, (baseSpeed + jumpYAccel), 0);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(mPlayer.playerMatrix, 0, mMVPMatrix, 0, mPlayer.TransMatrix, 0);

        // Render our example
        Render(mtrxProjectionAndView);

        scoreTxt.text = score + " ";

        // Render the text
        if (tm != null) {
            // Add it to our manager
            tm.updateText(scoreTxt);

            // Prepare the text for rendering
            tm.PrepareDraw();

            tm.Draw(mtrxProjectionAndView);
        }

        for (Protrusion p : mProtrusions) {
            if (p.TransMatrix[13] >= BOTTOM_OF_SCREEN - .5f) {
                Matrix.translateM(p.TransMatrix, 0, 0, baseSpeed, 0);

                Matrix.multiplyMM(p.protMatrix, 0, mMVPMatrix, 0, p.TransMatrix, 0);
            }
        }

        if (started) {
            if (baseSpeed == 0) {
                baseSpeed = START_SPEED;
            }

            if (baseSpeed >= MAX_SPEED) {
                baseSpeed += ACCEL;
            }

            Random rand = new Random();

            int randNum = rand.nextInt(COLUMNS);

            if (mProtrusions[protQueue.getFirst()].TransMatrix[13] < (1 - XGAP)) {
                needNext = true;

                for (int i = 0; i < mProtrusions.length && needNext; i++) {
                    if (i % COLUMNS == randNum) {
                        if (mProtrusions[i].TransMatrix[13] < BOTTOM_OF_SCREEN) {
                            mProtrusions[i].TransMatrix[13] = TOP_OF_SCREEN;
                            protQueue.push(i);
                            needNext = false;
                        }
                    }
                }
            }
        }

        if (jump && !inAir) {
            if (protQueue.size() > 1) {
                protQueue.removeLast();
                inAir = true;
                jumpCol = nextCol;
            }
            jump = false;
        }

        if (inAir && !falling) {
            if (mPlayer.TransMatrix[13] >= (mProtrusions[protQueue.getLast()].TransMatrix[13] + (Player.HEIGHT)) && !endOfJump) {
                mPlayer.TransMatrix[13] = (mProtrusions[protQueue.getLast()].TransMatrix[13] + (Player.HEIGHT) - .001f);
                jumpYAccel = -baseSpeed * (JUMP_SPEED * 22f * ((mProtrusions[protQueue.getLast()].TransMatrix[13] + (Player.HEIGHT)) - mPlayer.TransMatrix[13]));
                endOfJump = true;
            } else if (endOfJump && mPlayer.TransMatrix[13] > mProtrusions[protQueue.getLast()].TransMatrix[13]) {
                jumpYAccel = -baseSpeed * (JUMP_SPEED * 22f * ((mProtrusions[protQueue.getLast()].TransMatrix[13] + (Player.HEIGHT)) - mPlayer.TransMatrix[13]));
            } else if (endOfJump && mPlayer.TransMatrix[13] <= mProtrusions[protQueue.getLast()].TransMatrix[13]) {
                mPlayer.TransMatrix[13] = mProtrusions[protQueue.getLast()].TransMatrix[13];
                jumpYAccel = 0;
                endOfJump = false;
            } else if (mPlayer.TransMatrix[13] < mProtrusions[protQueue.getLast()].TransMatrix[13]) {
                jumpYAccel = baseSpeed * (JUMP_SPEED * 22f * ((mProtrusions[protQueue.getLast()].TransMatrix[13] + (Player.HEIGHT)) - mPlayer.TransMatrix[13]));
            }

            if ((mPlayer.TransMatrix[12] <= (jumpCol - 1) * YGAP && goingRight) || (mPlayer.TransMatrix[12] >= (jumpCol - 1) * YGAP && goingLeft)) {
                jumpXAccel = 0;
                mPlayer.TransMatrix[12] = (jumpCol - 1) * YGAP;
                goingLeft = false;
                goingRight = false;
            } else if (mPlayer.TransMatrix[12] < (jumpCol - 1) * YGAP && !goingRight && !goingLeft) {
                // Needs to be -2 * baseSpeed for 1 distance hops and -4 * baseSpeed for 2 distance hops
                jumpXAccel = (baseSpeed * 1.3f * (JUMP_SPEED + (JUMP_SPEED * (Math.abs((jumpCol % 2) - mPlayer.TransMatrix[12] / YGAP)))));
                goingLeft = true;
            } else if (mPlayer.TransMatrix[12] > (jumpCol - 1) * YGAP && !goingRight && !goingLeft) {
                // Needs to be 2 * baseSpeed for 1 distance hops and 4 * baseSpeed for 2 distance hops
                jumpXAccel = (-baseSpeed * 1.3f * (JUMP_SPEED + (JUMP_SPEED * (Math.abs((jumpCol % 2) + mPlayer.TransMatrix[12] / YGAP)))));
                goingRight = true;
            }

            if (jumpYAccel == 0 && jumpXAccel == 0) {
                inAir = false;
                if (jumpCol != (protQueue.getLast() % 3)) {
                    falling = true;
                } else
                    score++;
            }
        }

        if (falling || mPlayer.TransMatrix[13] < (BOTTOM_OF_SCREEN - Protrusion.HEIGHT - Player.HEIGHT)) {
            jumpYAccel = -baseSpeed * JUMP_SPEED;
            baseSpeed = 0;
        }

        if (mPlayer.TransMatrix[13] >= (BOTTOM_OF_SCREEN - Protrusion.HEIGHT - Player.HEIGHT))
            mPlayer.draw(mPlayer.playerMatrix);

        for (Protrusion p : mProtrusions) {
            if (p.TransMatrix[13] >= (BOTTOM_OF_SCREEN - Protrusion.HEIGHT)) {
                // Draw triangle
                p.draw(p.protMatrix);
            }
        }
    }

    public void onSurfaceChanged(GL10 unused, int wid, int hgt) {
        GLES20.glViewport(0, 0, wid, hgt);

        float ratio = (float) wid / hgt;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

        // Clear our matrices
        for (int i = 0; i < 16; i++) {
            mtrxProjection[i] = 0.0f;
            mtrxView[i] = 0.0f;
            mtrxProjectionAndView[i] = 0.0f;
        }

        // Setup our screen width and height for normal sprite translation.
        Matrix.orthoM(mtrxProjection, 0, 0f, wid, 0.0f, hgt, 0, 50);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);

        // Setup our scaling system
        SetupScaling();
    }

    public void SetupText() {
        // Create our text manager
        tm = new TextManager();

        // Tell our text manager to use index 1 of textures loaded
        tm.setTextureID(1);

        // Pass the uniform scale
        tm.setUniformscale(ssu);

        // Create our new textobject
        scoreTxt = new TextObject(score + " ", (context.getResources().getDisplayMetrics().widthPixels / 5), (9 * context.getResources().getDisplayMetrics().heightPixels / 10));

        // Add it to our manager
        tm.addText(scoreTxt);

        // Prepare the text for rendering
        tm.PrepareDraw();
    }

    public void SetupScaling() {
        // The screen resolutions
        swp = (context.getResources().getDisplayMetrics().widthPixels);
        shp = (context.getResources().getDisplayMetrics().heightPixels);

        // Orientation is assumed portrait
        ssx = swp / 320.0f;
        ssy = shp / 480.0f;

        // Get our uniform scaler
        if (ssx > ssy)
            ssu = ssy;
        else
            ssu = ssx;
    }

    public void UpdateSprite() {
        // Get new transformed vertices
        //vertices = sprite.getTransformedVertices();

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    private void Render(float[] m) {
        // Set our shaderprogram to image shader
        GLES20.glUseProgram(riGraphicTools.sp_Image);

        // clear Screen and Depth Buffer, we have set the clear color as black.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // get handle to vertex shader's vPosition member and add vertices
        int mPositionHandle = GLES20.glGetAttribLocation(riGraphicTools.sp_Image, "vPosition");
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Get handle to texture coordinates location and load the texture uvs
        int mTexCoordLoc = GLES20.glGetAttribLocation(riGraphicTools.sp_Image, "a_texCoord");
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);

        // Get handle to shape's transformation matrix and add our matrix
        int mtrxhandle = GLES20.glGetUniformLocation(riGraphicTools.sp_Image, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

        // Get handle to textures locations
        int mSamplerLoc = GLES20.glGetUniformLocation(riGraphicTools.sp_Image, "s_texture");

        // Set the sampler texture unit to 0, where we have saved the texture.
        GLES20.glUniform1i(mSamplerLoc, 0);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);
    }

    public void SetupTriangle() {
        // We will need a randomizer
        Random rnd = new Random();

        // Our collection of vertices
        vertices = new float[30 * 4 * 3];

        // Create the vertex data
        for (int i = 0; i < 30; i++) {
            int offset_x = rnd.nextInt((int) swp);
            int offset_y = rnd.nextInt((int) shp);

            // Create the 2D parts of our 3D vertices, others are default 0.0f
            vertices[i * 12] = offset_x;
            vertices[(i * 12) + 1] = offset_y + (30.0f * ssu);
            vertices[(i * 12) + 2] = 0f;
            vertices[(i * 12) + 3] = offset_x;
            vertices[(i * 12) + 4] = offset_y;
            vertices[(i * 12) + 5] = 0f;
            vertices[(i * 12) + 6] = offset_x + (30.0f * ssu);
            vertices[(i * 12) + 7] = offset_y;
            vertices[(i * 12) + 8] = 0f;
            vertices[(i * 12) + 9] = offset_x + (30.0f * ssu);
            vertices[(i * 12) + 10] = offset_y + (30.0f * ssu);
            vertices[(i * 12) + 11] = 0f;
        }

        // The indices for all textured quads
        indices = new short[30 * 6];
        int last = 0;
        for (int i = 0; i < 30; i++) {
            // We need to set the new indices for the new quad
            indices[i * 6] = (short) last;
            indices[(i * 6) + 1] = (short) (last + 1);
            indices[(i * 6) + 2] = (short) (last + 2);
            indices[(i * 6) + 3] = (short) last;
            indices[(i * 6) + 4] = (short) (last + 2);
            indices[(i * 6) + 5] = (short) (last + 3);

            // Our indices are connected to the vertices so we need to keep them
            // in the correct order.
            // normal quad = 0,1,2,0,2,3 so the next one will be 4,5,6,4,6,7
            last = last + 4;
        }

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);
    }

    public void SetupImage() {
        // We will use a randomizer for randomizing the textures from texture atlas.
        // This is strictly optional as it only effects the output of our app,
        // Not the actual knowledge.
        Random rnd = new Random();

        // 30 imageobjects times 4 vertices times (u and v)
        uvs = new float[30 * 4 * 2];

        // We will make 30 randomly textures objects
        for (int i = 0; i < 30; i++) {
            int random_u_offset = rnd.nextInt(2);
            int random_v_offset = rnd.nextInt(2);

            // Adding the UV's using the offsets
            uvs[i * 8] = random_u_offset * 0.5f;
            uvs[(i * 8) + 1] = random_v_offset * 0.5f;
            uvs[(i * 8) + 2] = random_u_offset * 0.5f;
            uvs[(i * 8) + 3] = (random_v_offset + 1) * 0.5f;
            uvs[(i * 8) + 4] = (random_u_offset + 1) * 0.5f;
            uvs[(i * 8) + 5] = (random_v_offset + 1) * 0.5f;
            uvs[(i * 8) + 6] = (random_u_offset + 1) * 0.5f;
            uvs[(i * 8) + 7] = random_v_offset * 0.5f;
        }

        // The texture buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        // Generate Textures, if more needed, alter these numbers.
        int[] texturenames = new int[2];
        GLES20.glGenTextures(2, texturenames, 0);

        // Retrieve our image from resources.
/*        int id = context.getResources().getIdentifier("drawable/textureatlas", null, context.getPackageName());

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
        int id = context.getResources().getIdentifier("drawable/font", null, context.getPackageName());
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), id);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[1]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();
    }

    public static int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
}