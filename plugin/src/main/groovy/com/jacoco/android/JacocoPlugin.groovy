package com.jacoco.android

import com.android.build.gradle.AppExtension
import com.jacoco.android.extension.JacocoExtension
import com.jacoco.android.task.BranchDiffTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException

class JacocoPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        JacocoExtension jacocoExtension = project.extensions.create("jacocoCoverageConfig", JacocoExtension)

        project.configurations.all { configuration ->
            def name = configuration.name
            if (name != "implementation" && name != "compile") {
                return
            }
        }

        def android = project.extensions.android


        if (android instanceof AppExtension) {
            JacocoTransform jacocoTransform = new JacocoTransform(project, jacocoExtension)
            android.registerTransform(jacocoTransform)
            // throw an exception in instant run mode
            android.applicationVariants.all { variant ->
                def variantName = variant.name.capitalize()
                try {
                    def instantRunTask = project.tasks.getByName("transformClassesWithInstantRunFor${variantName}")
                    if (instantRunTask) {
                        throw new GradleException("不支持instant run")
                    }
                } catch (UnknownTaskException ignored) {
                }
            }
        }

        project.afterEvaluate {
            android.applicationVariants.all { variant ->
                if (project.tasks.findByName('generateReport') == null) {
                    BranchDiffTask branchDiffTask = project.tasks.create('generateReport', BranchDiffTask)
                    branchDiffTask.setGroup("jacoco")
                    branchDiffTask.jacocoExtension = jacocoExtension
                }
            }
        }
    }
}