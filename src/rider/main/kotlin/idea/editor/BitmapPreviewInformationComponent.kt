package me.fornever.avaloniarider.idea.editor

import com.intellij.ui.components.JBLabel
import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

class BitmapPreviewInformationComponent : JPanel() {

    private val dimensionLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.LEFT
    }

    public fun updateDimensions(width: Int, height: Int) {
        dimensionLabel.text = "${width}x${height}"
    }

    init {
        layout = GridLayout(1, 2)

        add(JBLabel("Dimensions:"))
        add(dimensionLabel.apply {
            updateDimensions(0,0)
            horizontalAlignment = SwingConstants.LEFT
        })
    }
}
