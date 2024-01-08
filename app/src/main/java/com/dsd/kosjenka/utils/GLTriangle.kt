package com.dsd.kosjenka.utils

import android.R
import android.R.attr.x
import android.R.attr.y
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


class GLTriangle() {
    companion object{
        // number of coordinates per vertex in this array
        val COORDS_PER_VERTEX = 3
        var triangleCoords = floatArrayOf(     // in counterclockwise order:
            0.0f, 0.1f, 0.0f,      // top
            -0.1f, -0.1f, 0.0f,    // bottom left
            0.1f, -0.1f, 0.0f      // bottom right
        )
    }

    // Set color with red, green, blue and alpha (opacity) values
    val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)

    private val vertexShaderCode =
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        // Note that the uMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        "  gl_Position = uMVPMatrix * vPosition;" +
        "}"
    // Use to access and set the view transformation
    private var vPMatrixHandle: Int = 0

    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    private var vertexBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(triangleCoords.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(triangleCoords)
                // set the buffer to read the first coordinate
                position(0)
            }
        }

    private var mProgram: Int
    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0

    private val vertexCount: Int = triangleCoords.size / COORDS_PER_VERTEX
    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    init {
        // ...

        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram().also {

            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }
    }

    fun loadShader(type: Int, shaderCode: String): Int {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        return GLES20.glCreateShader(type).also { shader ->

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    fun draw(mvpMatrix: FloatArray) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(it)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )

            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->

                // Set color for drawing the triangle
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(it)
        }

        // get handle to shape's transformation matrix
        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    fun isPointInTriangle(x: Float, y: Float, mvpMatrix: FloatArray): Boolean {
        // Convert screen coordinates to OpenGL coordinates
        val invertedMatrix = FloatArray(16)
        Matrix.invertM(invertedMatrix, 0, mvpMatrix, 0)

        val normalizedCoordinates = FloatArray(4)
        normalizedCoordinates[0] = x
        normalizedCoordinates[1] = y
        normalizedCoordinates[2] = 0.0f
        normalizedCoordinates[3] = 1.5f

        val scratch = FloatArray(4)
        Matrix.multiplyMV(scratch, 0, invertedMatrix, 0, normalizedCoordinates, 0)

        // Check if the point is inside the triangle
        return _isPointInTriangle_(
            scratch[0],
            scratch[1],
            triangleCoords
        )
    }
    private fun _isPointInTriangle_(x: Float, y: Float, triangleVertices: FloatArray): Boolean {
        val x1 = triangleVertices[0]
        val y1 = triangleVertices[1]
        val x2 = triangleVertices[3]
        val y2 = triangleVertices[4]
        val x3 = triangleVertices[6]
        val y3 = triangleVertices[7]

        // Compute barycentric coordinates
        val detT = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3)
        val alpha = ((y2 - y3) * (x - x3) + (x3 - x2) * (y - y3)) / detT
        val beta = ((y3 - y1) * (x - x3) + (x1 - x3) * (y - y3)) / detT
        val gamma = 1 - alpha - beta

        // Check if the point is inside the triangle
        return alpha >= 0 && beta >= 0 && gamma >= 0
    }

}
