package me.fornever.avaloniarider.idea.editor

import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorTextField
import com.jetbrains.rider.ideaInterop.fileTypes.xaml.XamlFileType
import me.fornever.avaloniarider.idea.settings.AvaloniaPreviewerMethod
import me.fornever.avaloniarider.idea.settings.AvaloniaSettings

class AvaloniaPreviewEditorProvider : FileEditorProvider, DumbAware {

    private val acceptExtensions =  arrayOf<String>("xaml", "paml", "axaml")

    override fun getEditorTypeId() = "AvaloniaPreviewerEditor"
    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun accept(project: Project, file: VirtualFile): Boolean {
        //return FileTypeRegistry.getInstance().isFileOfType(file, XamlFileType);
        return acceptExtensions.contains(file.extension?.toLowerCase()) // TODO: Backend XAML file check (#42)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        val previewerEditor = when (AvaloniaSettings.getInstance(project).previewerTransportType) {
            AvaloniaPreviewerMethod.AvaloniaRemote -> AvaloniaRemotePreviewEditor(project, file)
            AvaloniaPreviewerMethod.Html -> AvaloniaHtmlPreviewEditor(project, file)
        }
        return PreviewerSplitterEditor(textEditor, previewerEditor)
    }
}
