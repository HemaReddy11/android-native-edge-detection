package com.example.edgeviewer.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLTextureRenderer(private val requestRenderCallback: () -> Unit) : GLSurfaceView.Renderer {

    // full-screen quad
    private val vertices = floatArrayOf(
        -1f,  1f,
        -1f, -1f,
        1f,  1f,
        1f, -1f
    )
    private val texCoords = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 0f,
        1f, 1f
    )

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texBuffer: FloatBuffer

    private var program = 0
    private var textureId = 0

    // These will be updated by Java thread; consumed in GL thread
    @Volatile private var pendingRGBA: ByteArray? = null
    @Volatile private var pendingWidth = 0
    @Volatile private var pendingHeight = 0

    @Volatile private var textureCreated = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        texBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        texBuffer.put(texCoords).position(0)

        // compile shaders
        val vShaderCode = """
            attribute vec2 aPos;
            attribute vec2 aTex;
            varying vec2 vTex;
            void main() {
                gl_Position = vec4(aPos, 0.0, 1.0);
                vTex = aTex;
            }
        """
        val fShaderCode = """
            precision mediump float;
            varying vec2 vTex;
            uniform sampler2D uTex;
            void main() {
                gl_FragColor = texture2D(uTex, vTex);
            }
        """

        val v = loadShader(GLES20.GL_VERTEX_SHADER, vShaderCode)
        val f = loadShader(GLES20.GL_FRAGMENT_SHADER, fShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, v)
            GLES20.glAttachShader(it, f)
            GLES20.glLinkProgram(it)
        }

        // create 2D texture
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        textureId = tex[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        textureCreated = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // if there's pending RGBA data, upload as texture
        val data = pendingRGBA
        if (data != null && pendingWidth > 0 && pendingHeight > 0 && textureCreated) {
            // upload using glTexImage2D or glTexSubImage2D
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            // allocate or update
            val bb = ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder())
            bb.put(data).position(0)
            // specify pixel format RGBA
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                pendingWidth, pendingHeight, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb
            )
            // clear pending so we don't constantly re-upload same buffer
            pendingRGBA = null
        }

        GLES20.glUseProgram(program)
        val posLoc = GLES20.glGetAttribLocation(program, "aPos")
        val texLoc = GLES20.glGetAttribLocation(program, "aTex")
        val texUni = GLES20.glGetUniformLocation(program, "uTex")

        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texLoc)
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 8, texBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(texUni, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(texLoc)
    }

    private fun loadShader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        return s
    }

    // Called from UI thread to push RGBA bytes for next frame
    fun pushRGBA(bytes: ByteArray, width: Int, height: Int) {
        // copy into a new array to avoid modification while GL thread reads it
        val copy = ByteArray(bytes.size)
        System.arraycopy(bytes, 0, copy, 0, bytes.size)
        pendingRGBA = copy
        pendingWidth = width
        pendingHeight = height
        requestRenderCallback()
    }
}
