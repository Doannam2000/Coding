package com.natncompany.videoeditor

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Surface
import com.natncompany.media.TimelineClip
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToInt

class RealtimeGpuVideoView(context: Context) : GLSurfaceView(context) {
    private val renderer = RealtimeGpuVideoRenderer { surface ->
        post { surfaceListener?.invoke(surface) }
    }
    private var surfaceListener: ((Surface?) -> Unit)? = null

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        renderer.requestRender = { requestRender() }
    }

    fun setOnInputSurfaceChanged(listener: (Surface?) -> Unit) {
        surfaceListener = listener
    }

    fun updateClip(clip: TimelineClip?, bypassEffects: Boolean = false) {
        queueEvent { renderer.updateClip(clip, bypassEffects) }
        requestRender()
    }

    override fun onDetachedFromWindow() {
        queueEvent { renderer.release() }
        surfaceListener?.invoke(null)
        super.onDetachedFromWindow()
    }
}

private class RealtimeGpuVideoRenderer(
    private val onSurfaceReady: (Surface?) -> Unit
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    var requestRender: (() -> Unit)? = null

    private var program = 0
    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var inputSurface: Surface? = null
    private var hasFrame = false

    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0
    private var brightnessHandle = 0
    private var contrastHandle = 0
    private var saturationHandle = 0
    private var filterModeHandle = 0

    private val vertexBuffer = floatBufferOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )
    private val texCoordBuffer = floatBufferOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private var brightness = 0f
    private var contrast = 1f
    private var saturation = 1f
    private var filterMode = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        brightnessHandle = GLES20.glGetUniformLocation(program, "uBrightness")
        contrastHandle = GLES20.glGetUniformLocation(program, "uContrast")
        saturationHandle = GLES20.glGetUniformLocation(program, "uSaturation")
        filterModeHandle = GLES20.glGetUniformLocation(program, "uFilterMode")

        oesTextureId = createOesTexture()
        surfaceTexture = SurfaceTexture(oesTextureId).apply {
            setOnFrameAvailableListener(this@RealtimeGpuVideoRenderer)
        }
        inputSurface = Surface(surfaceTexture)
        onSurfaceReady(inputSurface)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(this) {
            if (hasFrame) {
                surfaceTexture?.updateTexImage()
                hasFrame = false
            }
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(textureHandle, 0)
        GLES20.glUniform1f(brightnessHandle, brightness)
        GLES20.glUniform1f(contrastHandle, contrast)
        GLES20.glUniform1f(saturationHandle, saturation)
        GLES20.glUniform1i(filterModeHandle, filterMode)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        texCoordBuffer.position(0)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(this) { hasFrame = true }
        requestRender?.invoke()
    }

    fun updateClip(clip: TimelineClip?, bypassEffects: Boolean) {
        if (bypassEffects) {
            brightness = 0f
            contrast = 1f
            saturation = 1f
            filterMode = 0
        } else {
            brightness = clip?.transform?.brightness?.coerceIn(-1f, 1f) ?: 0f
            contrast = clip?.transform?.contrast?.coerceIn(0f, 2f) ?: 1f
            saturation = clip?.transform?.saturation?.coerceIn(0f, 2f) ?: 1f
            filterMode = when (clip?.effect?.parameters?.get(ClipEffectParameterFilterName)) {
                "Sepia" -> 1
                "Mono", "Monochrome", "Luminance" -> 2
                else -> 0
            }
        }
        texCoordBuffer.putTextureCoordsFor(clip)
    }

    fun release() {
        onSurfaceReady(null)
        inputSurface?.release()
        inputSurface = null
        surfaceTexture?.release()
        surfaceTexture = null
        if (oesTextureId != 0) GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
        oesTextureId = 0
        if (program != 0) GLES20.glDeleteProgram(program)
        program = 0
    }

    private fun FloatBuffer.putTextureCoordsFor(clip: TimelineClip?) {
        var left = clip?.transform?.crop?.left ?: 0f
        var top = clip?.transform?.crop?.top ?: 0f
        var right = clip?.transform?.crop?.right ?: 1f
        var bottom = clip?.transform?.crop?.bottom ?: 1f
        if (clip?.transform?.flipHorizontal == true) {
            val oldLeft = left
            left = right
            right = oldLeft
        }
        if (clip?.transform?.flipVertical == true) {
            val oldTop = top
            top = bottom
            bottom = oldTop
        }
        val base = when (((clip?.transform?.rotationDegrees ?: 0f) / 90f).roundToInt().floorMod4()) {
            1 -> floatArrayOf(right, bottom, right, top, left, bottom, left, top)
            2 -> floatArrayOf(right, top, left, top, right, bottom, left, bottom)
            3 -> floatArrayOf(left, top, left, bottom, right, top, right, bottom)
            else -> floatArrayOf(left, bottom, right, bottom, left, top, right, top)
        }
        position(0)
        put(base)
        position(0)
    }

    private fun Int.floorMod4(): Int = ((this % 4) + 4) % 4
}

private fun createOesTexture(): Int {
    val textures = IntArray(1)
    GLES20.glGenTextures(1, textures, 0)
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    return textures[0]
}

private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
    val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    return GLES20.glCreateProgram().also { program ->
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val info = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            error("Unable to link realtime preview shader program: $info")
        }
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
    }
}

private fun compileShader(type: Int, source: String): Int {
    return GLES20.glCreateShader(type).also { shader ->
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val info = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("Unable to compile realtime preview shader: $info")
        }
    }
}

private fun floatBufferOf(vararg values: Float): FloatBuffer {
    return ByteBuffer
        .allocateDirect(values.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(values)
            position(0)
        }
}

private const val VERTEX_SHADER = """
attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
void main() {
    gl_Position = aPosition;
    vTexCoord = aTexCoord;
}
"""

private const val FRAGMENT_SHADER = """
#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES uTexture;
uniform float uBrightness;
uniform float uContrast;
uniform float uSaturation;
uniform int uFilterMode;
varying vec2 vTexCoord;

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    color.rgb += uBrightness;
    color.rgb = ((color.rgb - 0.5) * uContrast) + 0.5;
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = mix(vec3(gray), color.rgb, uSaturation);
    if (uFilterMode == 1) {
        color.rgb = vec3(
            dot(color.rgb, vec3(0.393, 0.769, 0.189)),
            dot(color.rgb, vec3(0.349, 0.686, 0.168)),
            dot(color.rgb, vec3(0.272, 0.534, 0.131))
        );
    } else if (uFilterMode == 2) {
        color.rgb = vec3(gray);
    }
    gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), color.a);
}
"""
