package com.nantcompany.clipy.filters.gpu

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * A bridge between GPUImageFilter (original GPUImage source) and Media3 GlEffect.
 */
@UnstableApi
class GPUImageEffect(
    private val filter: GPUImageFilter
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return GPUImageShaderProgram(filter, useHdr)
    }
}

@UnstableApi
private class GPUImageShaderProgram(
    private val filter: GPUImageFilter,
    useHdr: Boolean
) : BaseGlShaderProgram(useHdr, 1) {

    private val cubeBuffer: FloatBuffer = ByteBuffer.allocateDirect(CUBE.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(CUBE)

    private val textureBuffer: FloatBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(TEXTURE_NO_ROTATION)

    init {
        filter.ifNeedInit()
    }

    override fun configure(inputWidth: Int, inputHeight: Int): androidx.media3.common.util.Size {
        filter.onOutputSizeChanged(inputWidth, inputHeight)
        return androidx.media3.common.util.Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        // Original GPUImage onDraw takes the textureId and buffers
        filter.onDraw(inputTexId, cubeBuffer, textureBuffer)
    }

    override fun release() {
        super.release()
        filter.destroy()
    }

    companion object {
        private val CUBE = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
        )
        private val TEXTURE_NO_ROTATION = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )
    }
}
