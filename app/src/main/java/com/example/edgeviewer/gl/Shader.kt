package com.example.edgeviewer.gl

import android.opengl.GLES20

object Shader {

    private const val vertexCode = """
        attribute vec2 vPosition;
        attribute vec2 vTexCoord;
        varying vec2 texCoord;
        void main() {
            gl_Position = vec4(vPosition, 1.0, 1.0);
            texCoord = vTexCoord;
        }
    """

    private const val fragmentCode = """
        precision mediump float;
        varying vec2 texCoord;
        uniform sampler2D tex;
        void main() {
            gl_FragColor = texture2D(tex, texCoord);
        }
    """

    val program: Int = run {
        val vShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexCode)
        val fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode)

        val programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vShader)
        GLES20.glAttachShader(programId, fShader)
        GLES20.glLinkProgram(programId)

        programId
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}
