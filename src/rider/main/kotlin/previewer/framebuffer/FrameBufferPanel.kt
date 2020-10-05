package me.fornever.avaloniarider.previewer.framebuffer

import com.intellij.util.ui.UIUtil
import org.jdesktop.swingx.border.DropShadowBorder
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import java.awt.Point
import javax.swing.JComponent
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder

class FrameBufferPanel(frameBufferImage: FrameBufferImage): JComponent() {

    private val frameBufferImage : FrameBufferImage = frameBufferImage

    fun updateDimensions(width: Int, height: Int){
        this.revalidate()
    }

    override fun isVisible(): Boolean {

        return this.frameBufferImage.isVisible
    }

    public fun getBorderSize() : Dimension {
        var compoundBorder = this.border as CompoundBorder

        var borderWidth = compoundBorder.outsideBorder.getBorderInsets(this).left +
            compoundBorder.outsideBorder.getBorderInsets(this).right +
            compoundBorder.insideBorder.getBorderInsets(this).left +
            compoundBorder.insideBorder.getBorderInsets(this).right

        var borderHeight = compoundBorder.outsideBorder.getBorderInsets(this).top +
            compoundBorder.outsideBorder.getBorderInsets(this).bottom +
            compoundBorder.insideBorder.getBorderInsets(this).top +
            compoundBorder.insideBorder.getBorderInsets(this).bottom

        return Dimension(borderWidth, borderHeight)
    }

    public fun getOffset() : Point {
        var location = this.location
        var compoundBorder = this.border as CompoundBorder

        var leftOffset = location.x + compoundBorder.outsideBorder.getBorderInsets(this).left + compoundBorder.insideBorder.getBorderInsets(this).left
        var topOffset = location.y + compoundBorder.outsideBorder.getBorderInsets(this).top + compoundBorder.insideBorder.getBorderInsets(this).top

        return Point(leftOffset, topOffset)
    }

    init {
        isDoubleBuffered = true
        layout = FlowLayout(FlowLayout.CENTER, 0, 0)
        isOpaque = false
        border = CompoundBorder(
            DropShadowBorder(UIUtil.getPanelBackground().darker(), 12, .75f, 12, true, true, true, true),
            EmptyBorder(Insets(0, 0, 0, 0))
        )

        add(frameBufferImage)
    }
}
