package me.fornever.avaloniarider.previewer.framebuffer

import com.intellij.ui.paint.PaintUtil
import com.intellij.util.JBHiDPIScaledImage
import com.intellij.util.ui.UIUtil
import me.fornever.avaloniarider.controlmessages.FrameMessage
import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import javax.swing.JComponent
import javax.swing.border.EmptyBorder
import kotlin.math.ceil

class FrameBufferImage: JComponent() {

    private var image: BufferedImage? = null

    private fun fromByte(b: Byte): Int = b.toInt() and 0xFF


    private fun getScaledDimensions(width: Double, height: Double) : Pair<Double, Double> {

        val defaultConfig = this.graphicsConfiguration.device.defaultConfiguration

        val scaleX = defaultConfig.defaultTransform.scaleX
        val scaleY = defaultConfig.defaultTransform.scaleY

        val scaledWidth = (width / scaleX)
        val scaledHeight = (height / scaleY)

        return Pair(scaledWidth, scaledHeight)
    }

    private fun updateScaledDimensions(width: Double, height: Double): Boolean {
        val scaledDimensions  = getScaledDimensions(width, height)

        if (this.width.toDouble() != ceil(scaledDimensions.first) || this.height.toDouble() != ceil(scaledDimensions.second)) {
            this.preferredSize = Dimension(ceil(scaledDimensions.first).toInt(), ceil(scaledDimensions.second).toInt())
        }

        return false
    }

    fun updateDimensions(width: Int, height: Int, forceRepaint: Boolean = false){
        if (this.width != width || this.height != height) {
            //this.preferredSize = Dimension(width, height)

            if (forceRepaint && updateScaledDimensions(width.toDouble(), height.toDouble()))
                this.repaint()
        }
    }

    fun renderFrame(frame: FrameMessage?) {
        if (this.image == null || this.image!!.width != frame!!.width || this.image!!.height != frame!!.height) {

            val scaledDimensions  = getScaledDimensions(frame!!.width.toDouble(), frame!!.height.toDouble())

            val scaledWidth = scaledDimensions.first
            val scaledHeight = scaledDimensions.second

            this.image = UIUtil.createImage(this.graphicsConfiguration, scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB, PaintUtil.RoundingMode.CEIL)

            if (this.image is JBHiDPIScaledImage) {
                this.updateDimensions((this.image as JBHiDPIScaledImage).getUserWidth(this), (this.image as JBHiDPIScaledImage).getUserHeight(this))
            }
            else {
                this.updateDimensions(this.image!!.width, this.image!!.height)
                //this.preferredSize = Dimension(this.image!!.width, this.image!!.height)
            }

            //updateDimensions(frame!!.width, frame!!.height, false)
            //this.image = UIUtil.createImage(this, frame!!.width, frame!!.height, BufferedImage.TYPE_INT_RGB)
        }

        val pixelData = (this.image!!.raster.dataBuffer as DataBufferInt).data

        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val pixelIndex = y * frame.width * 4 + x * 4 // 4 bytes per px
                val bytes = frame.data.slice(pixelIndex..pixelIndex + 3)
                val color = Color(fromByte(bytes[0]), fromByte(bytes[1]), fromByte(bytes[2]))
                pixelData[x + y * frame.width ] = /*if ((x % 8) == 0 || (y % 8) == 0) color.rgb xor 0x00ffffff else*/ color.rgb
            }
        }

        this.repaint()
    }

    override fun isVisible(): Boolean {
        return this.image != null && this.image!!.width > 0 && this.image!!.height > 0
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        if (image != null) {
            val graphics2D = g as Graphics2D
            graphics2D.drawImage(this.image/*!!.getScaledInstance(this.width, this.height, Image.SCALE_DEFAULT)*/, 0, 0, width, height, this)
        }
    }

    init {
        border = EmptyBorder(0, 0, 0, 0)
        isOpaque = false
        background = Color(0,0,0,0)
        isDoubleBuffered = true
    }
}
