package me.fornever.avaloniarider.idea.editor

import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.UIUtil
import com.jetbrains.rd.platform.util.application
import com.jetbrains.rd.util.lifetime.Lifetime
import me.fornever.avaloniarider.controlmessages.FrameMessage
import me.fornever.avaloniarider.controlmessages.UpdateXamlResultMessage
import me.fornever.avaloniarider.idea.concurrency.adviseOnUiThread
import me.fornever.avaloniarider.plainTextToHtml
import me.fornever.avaloniarider.previewer.AvaloniaPreviewerSessionController
import me.fornever.avaloniarider.previewer.AvaloniaPreviewerSessionController.Status
import me.fornever.avaloniarider.previewer.nonTransparent
import me.fornever.avaloniarider.previewer.renderFrame
import org.jdesktop.swingx.border.DropShadowBorder
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder

private class AvaloniaPreviewPanel : JPanel() {

    private val borderSize = 16

    private val previewPanel = JBLabel().apply {
        verticalAlignment = SwingConstants.CENTER
        horizontalAlignment = SwingConstants.CENTER

        val compBorder = CompoundBorder(
            DropShadowBorder().apply {
                this.shadowColor = background.darker()
                this.shadowOpacity = 0.75f
                this.shadowSize = borderSize
                this.cornerSize = borderSize
                this.isShowLeftShadow = true
                this.isShowBottomShadow = true
                this.isShowRightShadow = true
                this.isShowTopShadow = true
            },
            EmptyBorder(borderSize,borderSize,borderSize,borderSize)
        )

        border = compBorder

    }

    public override fun paintComponent(g: Graphics) {
        //g.color = Color.green
        //g.fillRect(8, 8,8, 8)

        var col: Int
        var x: Int
        var y: Int

        g.color = this.background.darker();//Color(255, 255, 255, 127)

        var row = 0
        while (row < height / 8) {
            col = 0
            while (col < width / 8) {
                x = col * 8
                y = row * 8
                if (row % 2 == col % 2) g.color = this.background.brighter() else g.color = this.background.darker()
                g.fillRect(x, y, 8, 8)
                col++
            }
            row++
        }

        super.paintComponent(g)
    }

    public fun clearPreview(){
        previewPanel.icon = null
    }

    public fun applyPreview(image: BufferedImage, width: Int, height: Int, scale: Double){

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        if (newHeight != previewPanel.preferredSize.height || newWidth != previewPanel.preferredSize.width) {
            //preferredSize = Dimension(newWidth + (borderSize * 2) /*+ (newWidth * .5).toInt()*/, newHeight + (borderSize * 2)/* + (newHeight * .5).toInt()*/)
            previewPanel.preferredSize = Dimension(newWidth + (borderSize * 2), newHeight + (borderSize * 2))
            preferredSize = previewPanel.preferredSize.clone() as Dimension

        }

        previewPanel.icon = ImageIcon(image.getScaledInstance(newWidth, newHeight, Image.SCALE_AREA_AVERAGING)) // TODO[F]: Find a clever way to update that (#40)
    }

    init {
        layout = GridBagLayout().apply {

        }
        this.isOpaque = false
        this.border = EmptyBorder(0, 0, 0, 0)

        add(previewPanel/*, GridBagLayout.CENTER*/)
    }
}

class BitmapPreviewEditorComponent(lifetime: Lifetime, controller: AvaloniaPreviewerSessionController) : JPanel() {
    companion object {
        private val logger = Logger.getInstance(BitmapPreviewEditorComponent::class.java)
    }

    private val infoLabel = JBLabel()
    private val previewControlPanel = ComboBox<String>().apply {
        addItem("200%")
        addItem("170%")
        addItem("150%")
        addItem("125%")
        addItem("100%")
        addItem("75%")
        addItem("50%")
        addItem("25%")
        selectedIndex = 4
    }
    private val mainScrollView = JBScrollPane()
    private val frameBufferView = lazy {
        AvaloniaPreviewPanel().apply {
            //verticalAlignment = SwingConstants.CENTER
            //horizontalAlignment = SwingConstants.CENTER
        }
    }
    private val spinnerView = lazy { AsyncProcessIcon.Big("Loading") }
    private val errorLabel = lazy {
        JBLabel().apply {
            setCopyable(true)
        }
    }
    private val errorView = lazy {
        JPanel().apply {
            layout = GridBagLayout()
            add(JLabel(AllIcons.General.Error))
            add(errorLabel.value)
        }
    }
    private val terminatedView = lazy { JLabel("Previewer has been terminated") }

    private var status = Status.Idle

    init {
        layout = BorderLayout()

        infoLabel.border = EmptyBorder(4, 8, 4, 8)

        add(infoLabel, BorderLayout.NORTH)

        add(mainScrollView, BorderLayout.CENTER)

        add(previewControlPanel, BorderLayout.SOUTH)

        controller.requestViewportResize.advise(lifetime) {
            // TODO[F]: Update the image size for the renderer (#40)
        }

        controller.status.adviseOnUiThread(lifetime, ::handleStatus)
        controller.updateXamlResult.adviseOnUiThread(lifetime, ::handleXamlResult)
        controller.criticalError.adviseOnUiThread(lifetime, ::handleCriticalError)

        controller.frame.adviseOnUiThread(lifetime) { frame ->
            if (nonTransparent(frame)) // TODO[F]: Remove after fix of https://github.com/AvaloniaUI/Avalonia/issues/4264
                handleFrame(frame)
            controller.acknowledgeFrame(frame)
        }
    }

    private fun handleStatus(newStatus: Status) {
        application.assertIsDispatchThread()
        mainScrollView.viewport.view = when (newStatus) {
            Status.Idle, Status.Connecting -> spinnerView.value
            Status.Working -> frameBufferView.value
            Status.XamlError -> errorView.value
            Status.Suspended -> spinnerView.value
            Status.Terminated -> terminatedView.value
        }

        status = newStatus
        logger.info("Status: $status")
    }

    private fun handleFrame(frame: FrameMessage) {
        application.assertIsDispatchThread()
        if (status != Status.Working) {
            logger.warn("Had to skip a frame because it came during status $status")
            return
        }

        //logger.debug("Frame Dimensions: ${frame.width} x ${frame.height}")

        val frameBuffer = frameBufferView.value
        if (frame.height <= 0 || frame.width <= 0) {
            //frameBuffer.icon = null
            frameBuffer.clearPreview()
            infoLabel.text = ""
            return
        }

        infoLabel.text = "${frame.width} x ${frame.height}"

        //if (frame.height != frameBuffer.preferredSize.height || frame.width != frameBuffer.preferredSize.width) {
        //    frameBuffer.preferredSize = Dimension(frame.width, frame.height)
        //}


        val image = UIUtil.createImage(this, frame.width, frame.height, BufferedImage.TYPE_INT_RGB)


        image.renderFrame(frame)

        val scale = when (previewControlPanel.selectedIndex) {
            0 -> 2.0
            1 -> 1.75
            2 -> 1.50
            3 -> 1.25
            5 -> .75
            6 -> .5
            7 -> .25
            else -> 1.0
        }

        frameBuffer.applyPreview(image, frame.width, frame.height, scale)
        //frameBuffer.icon = ImageIcon(image.getScaledInstance( (frame.width * scale).toInt() , (frame.height * scale).toInt() , Image.SCALE_SMOOTH)) // TODO[F]: Find a clever way to update that (#40)
    }

    private fun handleXamlResult(message: UpdateXamlResultMessage) {
        val errorMessage =
            if (message.exception == null && message.error == null) ""
            else {
                val buffer = StringBuilder()
                val exception = message.exception
                if (exception != null) {
                    val exceptionType = exception.exceptionType ?: "Error"
                    buffer.append(exceptionType)
                    if (exception.lineNumber != null || exception.linePosition != null) {
                        buffer.append(" at ").append(when {
                            exception.lineNumber != null && exception.linePosition != null ->
                                "${exception.lineNumber}:${exception.linePosition}"
                            exception.lineNumber != null -> "${exception.lineNumber}"
                            else -> "position ${exception.linePosition}"
                        })
                    }

                    if (exception.message != null) {
                        buffer.append(": ").append(exception.message)
                    }
                }
                if (message.error != null) {
                    if (message.exception != null) buffer.append("\n")
                    buffer.append(message.error)
                }
                buffer.toString()
            }

        if (errorMessage.isNotEmpty())
            errorLabel.value.text = errorMessage.plainTextToHtml()
    }

    private fun handleCriticalError(error: Throwable) {
        terminatedView.value.text = "Previewer has been terminated: ${error.localizedMessage}".plainTextToHtml()
    }
}
