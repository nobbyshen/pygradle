package com.innobead.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import com.innobead.gradle.GradleSupport
import com.innobead.gradle.plugin.pythonPluginExtension
import com.innobead.gradle.plugin.taskName
import java.io.File


@GradleSupport
class PythonDependenciesTask : DefaultTask() {

    val virtualenvDir by lazy {
        project.extensions.pythonPluginExtension.virtualenvDir
    }

    val pipOptions by lazy {
        project.extensions.pythonPluginExtension.pipOptions
    }

    var copyLibsDir: File? = null

    init {
        description = "Install Python dependencies"

        project.afterEvaluate {
            dependsOn.add(project.tasks.getByName(PythonRuntimeTask::class.taskName))
        }
    }

    @TaskAction
    fun action() {
        if (!project.file("requirements.txt").exists()) {
            logger.lifecycle("Ignored to install dependencies, because requirements.txt not found")
            return
        }

        logger.lifecycle("Installing dependencies in requirements.txt")

        project.exec {
            it.commandLine(listOf(
                    "bash", "-c",
                    "source '$virtualenvDir/bin/activate'; pip install -r requirements.txt $pipOptions".trim()
            ))
        }.rethrowFailure()

        val libsDir = File(project.buildDir, "libs")
        libsDir.mkdirs()

        logger.lifecycle("Downloading dependencies to $libsDir")

        project.exec {
            it.commandLine(listOf(
                    "bash", "-c",
                    "source '$virtualenvDir/bin/activate'; " +
                            "pip install -I --prefix='$libsDir' -r requirements.txt $pipOptions".trim()
            ))
        }.rethrowFailure()

        if (copyLibsDir != null) {
            with(File(libsDir, "lib/python2.7/site-packages")) {
                if (this.exists()) {
                    this.copyRecursively(copyLibsDir!!, overwrite = true)
                }
            }
        }
    }

}