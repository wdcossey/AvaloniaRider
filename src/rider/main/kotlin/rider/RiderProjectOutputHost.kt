package me.fornever.avaloniarider.rider

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.RdGetProjectOutputArgs
import com.jetbrains.rider.model.RdProjectOutput
import com.jetbrains.rider.model.riderProjectOutputModel
import com.jetbrains.rider.projectView.nodes.ProjectModelNode
import com.jetbrains.rider.projectView.solution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.fornever.avaloniarider.idea.concurrency.ApplicationAnyModality
import me.fornever.avaloniarider.idea.concurrency.await

@Service
class RiderProjectOutputHost(private val project: Project) {
    companion object {
        fun getInstance(project: Project): RiderProjectOutputHost =
            project.getService(RiderProjectOutputHost::class.java)
    }

    suspend fun getProjectOutput(lifetime: Lifetime, projectNode: ProjectModelNode): RdProjectOutput =
        withContext(Dispatchers.ApplicationAnyModality) {
            val model = project.solution.riderProjectOutputModel
            val projectFilePath = projectNode.getVirtualFile()!!.path

            model.getProjectOutput.start(lifetime, RdGetProjectOutputArgs(projectFilePath)).await(lifetime)
        }
}
