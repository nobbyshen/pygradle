package com.innobead.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import com.innobead.gradle.GradleSupport
import com.innobead.gradle.plugin.pythonPluginExtension


@GradleSupport
class PythonRuntimeTask : DefaultTask() {

    val virtualenvDir by lazy {
        project.extensions.pythonPluginExtension.virtualenvDir
    }

    val pythonDir by lazy {
        project.extensions.pythonPluginExtension.pythonDir
    }

    init {
        description = "Create Python sandbox (virtualenv)"
    }

    @TaskAction
    fun action() {
        val commands = mutableListOf<String>()

        logger.lifecycle("Installing pip")

        project.exec {
            it.executable("bash")
            it.args("-c", "which pip > /dev/null 2>&1")
        }.exitValue.also {
            if (it != 0) {
                commands.addAll(listOf(
                        "curl -OL https://bootstrap.pypa.io/get-pip.py",
                        "python get-pip.py --prefix $pythonDir",
                        "rm get-pip.py",
                        "export PYTHONPATH='$pythonDir/lib/python2.7/site-packages:\$PYTHONPATH'",
                        "export PATH='$pythonDir/bin:\$PATH'"
                ))
            }
        }

        logger.lifecycle("Installing virtualenv and creating an environment")
        project.exec {
            it.executable("bash")
            it.args("-c", "which virtualenv > /dev/null 2>&1")
        }.exitValue.also {
            when (it) {
                0 -> {
                    commands.addAll(listOf(
                            "virtualenv $virtualenvDir"
                    ))
                }
                else -> {
                    commands.addAll(listOf(
                            "pip install --no-cache-dir virtualenv --prefix $pythonDir",
                            "$pythonDir/bin/virtualenv $virtualenvDir"
                    ))
                }
            }
        }

        project.exec {
            it.workingDir(project.extensions.pythonPluginExtension.tmpDir)
            it.executable("bash")
            it.args(listOf(
                    "-c",
                    commands.joinToString(";")
            ))
        }.rethrowFailure()
    }

}