package com.example.edgeviewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import android.media.Image
import java.nio.ByteBuffer

class YuvToRgbConverter(context: Context) {
    private val rs: RenderScript = RenderScript.create(context)
    private val yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    private var yuvAlloc: Allocation? = null
    private var rgbAlloc: Allocation? = null

    // Convert Image (YUV_420_888) to ARGB_8888 ByteArray (RGBA order)
    fun yuvToRgba(image: Image): ByteArray {
        val width = image.width
        val height = image.height

        val yuvBytes = imageToByteArray(image)

        val yuvType = Type.Builder(rs, Element.U8(rs)).setX(yuvBytes.size)
        yuvAlloc = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
        yuvAlloc!!.copyFrom(yuvBytes)

        val rgbType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
        rgbAlloc = Allocation.createTyped(rs, rgbType.create(), Allocation.USAGE_SCRIPT)

        yuvToRgbIntrinsic.setInput(yuvAlloc)
        yuvToRgbIntrinsic.forEach(rgbAlloc)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        rgbAlloc!!.copyTo(bitmap)

        // Convert Bitmap to RGBA byte array
        val buffer = ByteBuffer.allocate(width * height * 4)
        bitmap.copyPixelsToBuffer(buffer)
        return buffer.array()
    }

    private fun imageToByteArray(image: Image): ByteArray {
        val crop = image.cropRect
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        val ySize = width * height
        val uvSize = width * height / 4

        val yuvBytes = ByteArray(ySize + uvSize * 2)
        val imagePlanes = image.planes

        // Fill Y plane
        var position = 0
        val yBuffer = imagePlanes[0].buffer
        val yRowStride = imagePlanes[0].rowStride
        val yPixelStride = imagePlanes[0].pixelStride
        for (row in 0 until height) {
            val rowStart = yRowStride * row
            for (col in 0 until width) {
                yuvBytes[position++] = yBuffer.get(rowStart + col * yPixelStride)
            }
        }

        // Fill U and V planes (interleaved as NV21 expects)
        val uBuffer = imagePlanes[1].buffer
        val vBuffer = imagePlanes[2].buffer
        val uRowStride = imagePlanes[1].rowStride
        val uPixelStride = imagePlanes[1].pixelStride

        // This simple copy works for many devices but some require handling pixel stride properly.
        // Append V and U interleaved in NV21-like layout expected by RS intrinsic
        val chromaHeight = height / 2
        val chromaWidth = width / 2
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val uIndex = row * uRowStride + col * uPixelStride
                val vIndex = row * imagePlanes[2].rowStride + col * imagePlanes[2].pixelStride
                // NV21 style: V then U
                yuvBytes[position++] = vBuffer.get(vIndex)
                yuvBytes[position++] = uBuffer.get(uIndex)
            }
        }

        return yuvBytes
    }
}
