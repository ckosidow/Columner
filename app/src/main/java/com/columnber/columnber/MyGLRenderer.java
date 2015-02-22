package com.columnber.columnber;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.util.LinkedList;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by CKosidowski11 on 2/16/2015.
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {
    public float baseAccel = 0;
    public float jumpYAccel = 0;
    public float jumpXAccel = 0;
    public boolean started = false;
    public boolean jump = false;
    public boolean inAir = false;
    public boolean goingLeft = false;
    public boolean goingRight = false;
    public int jumpCol = 1;
    private long time = 0;
    private Protrusion[] mProtrusions = new Protrusion[9];
    private Player mPlayer;
    private LinkedList<Integer> protQueue = new LinkedList<Integer>();
    private static final int COLUMNS = 3;
    private static final float BOTTOM_OF_SCREEN = -1.0f;
    private static final float TOP_OF_SCREEN = 1.0f;
    private static final float XGAP = 0.5f;
    private static final float YGAP = -0.51f;
    private static final float STARTPOS = BOTTOM_OF_SCREEN + XGAP;
    private boolean needNext;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        for (int i = 0; i < mProtrusions.length; i++) {
            mProtrusions[i] = new Protrusion(i % COLUMNS);
            mPlayer = new Player();

            mPlayer.TransMatrix[13] = STARTPOS;

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

    public void onDrawFrame(GL10 gl) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        Matrix.translateM(mPlayer.TransMatrix, 0, jumpXAccel, (baseAccel + jumpYAccel), 0);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(mPlayer.playerMatrix, 0, mMVPMatrix, 0, mPlayer.TransMatrix, 0);

        for (Protrusion p : mProtrusions) {
            Matrix.translateM(p.TransMatrix, 0, 0, baseAccel, 0);

            Matrix.multiplyMM(p.protMatrix, 0, mMVPMatrix, 0, p.TransMatrix, 0);
        }

        if (jump) {
            if (protQueue.size() > 1) {
                jumpYAccel = (baseAccel * -2.5f);
                protQueue.removeLast();
                inAir = true;
            }
            jump = false;
        }

        if (inAir) {
            if (mPlayer.TransMatrix[13] >= mProtrusions[protQueue.getLast()].TransMatrix[13]) {
                mPlayer.TransMatrix[13] = mProtrusions[protQueue.getLast()].TransMatrix[13];
                jumpYAccel = 0;
            } else if (mPlayer.TransMatrix[13] < mProtrusions[protQueue.getLast()].TransMatrix[13]) {
                jumpYAccel = (baseAccel * -2f);
            }

            if ((mPlayer.TransMatrix[12] <= (jumpCol - 1) * YGAP && goingRight) || (mPlayer.TransMatrix[12] >= (jumpCol - 1) * YGAP && goingLeft)) {
                jumpXAccel = 0;
                mPlayer.TransMatrix[12] = (jumpCol - 1) * YGAP;
                goingLeft = false;
                goingRight = false;
            } else if (mPlayer.TransMatrix[12] < (jumpCol - 1) * YGAP && !goingRight && !goingLeft) {
                // Needs to be -2 * baseAccel for 1 distance hops and -4 * baseAccel for 2 distance hops
                jumpXAccel = baseAccel * (-2f - (2 * (Math.abs((jumpCol % 2) - mPlayer.TransMatrix[12] / YGAP))));
                goingLeft = true;
            } else if (mPlayer.TransMatrix[12] > (jumpCol - 1) * YGAP && !goingRight && !goingLeft) {
                // Needs to be 2 * baseAccel for 1 distance hops and 4 * baseAccel for 2 distance hops
                jumpXAccel = -baseAccel * (-2f - (2 * (Math.abs((jumpCol % 2) + mPlayer.TransMatrix[12] / YGAP))));
                goingRight = true;
            }

            if (jumpYAccel == 0 && jumpXAccel == 0)
                inAir = false;
        }

        if (started) {
            if (baseAccel == 0) {
                baseAccel = -0.0050f;
                time = SystemClock.uptimeMillis();
            }

            // Create a rotation transformation for the triangle
            long runtime = SystemClock.uptimeMillis() - time;

            if (baseAccel >= -0.03) {
                baseAccel *= (float) (1 + (runtime / 50000000.0));
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

        for (Protrusion p : mProtrusions) {
            if (p.TransMatrix[13] >= BOTTOM_OF_SCREEN) {
                // Draw triangle
                p.draw(p.protMatrix);
            }
        }

        mPlayer.draw(mPlayer.playerMatrix);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
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

    public float getAccel() {
        return baseAccel;
    }

    public void setAccel(float y) {
        baseAccel = y;
    }
}