package me.fornever.avaloniarider.previewer.framebuffer

import com.intellij.ui.ColorUtil
import com.intellij.util.ui.UIUtil
import me.fornever.avaloniarider.controlmessages.FrameMessage
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*

class FrameBufferComponent : JComponent() {

    private var scale: Float = 1.0f

    private var bufferedTileImage : BufferedImage? = null
    private var bufferedTileBackground : Color? = null

    private fun createTileImage(color: Color) : BufferedImage {

        if (bufferedTileImage == null || bufferedTileBackground == null || bufferedTileBackground!!.rgb != color.rgb) {

            bufferedTileBackground = color

            bufferedTileImage = UIUtil.createImage(this, 16, 16, BufferedImage.TYPE_INT_RGB).apply {
                var col: Int
                var x: Int
                var y: Int

                var graphics2D = this.createGraphics()

                val secondaryColor = if (ColorUtil.isDark(bufferedTileBackground!!)) bufferedTileBackground!!.brighter() else bufferedTileBackground!!.darker()

                var row = 0
                while (row < this.height) {
                    col = 0
                    while (col < this.width) {
                        x = (col * 8)
                        y = (row * 8)
                        if (row % 2 == col % 2) graphics2D.color = bufferedTileBackground!! else graphics2D.color = secondaryColor
                        graphics2D.fillRect(x, y, 8, 8)
                        col++
                    }
                    row++
                }

                graphics2D.dispose()
            }
        }

        return bufferedTileImage!!
    }


    private val frameBufferImage = FrameBufferImage()

    private val frameBufferPanel = FrameBufferPanel(frameBufferImage)

    fun updateDimensions(width: Int, height: Int) {
        frameBufferPanel.updateDimensions(width, height)
        frameBufferImage.updateDimensions(width, height, true)
        this.repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        //Get the offset of the frameBufferPanel so we can render the texture are the correct co-ordinates
        val offset = frameBufferPanel.getOffset()

        val leftMod = offset.x % 8
        val topMod = offset.y % 8

        val checkerBoardPaint = TexturePaint(createTileImage(this.background), Rectangle(leftMod, topMod, 16, 16))

        val graphics2D = g as Graphics2D
        graphics2D.paint = checkerBoardPaint
        graphics2D.fill(graphics2D.clip)
    }

    //TODO: Not implemented
    fun decreaseScale(factor: Float) {
        this.scale = (this.scale - factor)
    }

    //TODO: Not implemented
    fun increaseScale(factor: Float) {
        this.scale = (this.scale + factor)
    }

    fun updateFrame(frame: FrameMessage?) {

        val scaleWidth = ((frame?.width ?: 0) * scale).toInt()
        val scaleHeight = ((frame?.height ?: 0) * scale).toInt()

        if (frameBufferImage.width != scaleWidth || frameBufferImage.height != scaleHeight)
        {
            updateDimensions(scaleWidth, scaleHeight)
        }

        frameBufferImage.renderFrame(frame)
    }

    init {
        layout = GridBagLayout()

        isDoubleBuffered = true

        add(frameBufferPanel)
    }
}
