package com.jacoco.android.task


import com.android.utils.FileUtils
import com.jacoco.android.extension.JacocoExtension
import com.jacoco.android.report.ReportGenerator
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jacoco.core.data.MethodInfo
import org.jacoco.core.diff.DiffAnalyzer
import com.jacoco.android.util.ClientUploadUtils

class BranchDiffTask extends DefaultTask {
    JacocoExtension jacocoExtension

    @TaskAction
    def getDiffClass() {
        println "downloadEcData start"
        downloadEcData()
        println "downloadEcData end"

        //生成差异报告
        println "pullDiffClasses start"
        pullDiffClasses()
        println "pullDiffClasses end"

        if (jacocoExtension.reportDirectory == null) {
            jacocoExtension.reportDirectory = "${project.buildDir.getAbsolutePath()}/outputs/report"
        }
        ReportGenerator generator = new ReportGenerator(jacocoExtension.coverageDirectory, jacocoExtension.classDirectories,
                toFileList(jacocoExtension.sourceDirectories), new File(jacocoExtension.reportDirectory))
        generator.create()
        ClientUploadUtils upload = new ClientUploadUtils()
        upload.upload(jacocoExtension.reportDirectory)
        System.out.println("点击链接查看测试报告:" + jacocoExtension.host + "/report/index.html")
    }

    //下载ec数据文件
    def downloadEcData() {
        if (jacocoExtension.coverageDirectory == null) {
            jacocoExtension.coverageDirectory = "${project.buildDir}/outputs/coverage/"
        }
        new File(jacocoExtension.coverageDirectory).mkdirs()

        def host = jacocoExtension.host
        def android = project.extensions.android
        def appName = android.defaultConfig.applicationId.replace(".","")
        def versionCode = android.defaultConfig.versionCode

        def curl = "curl ${host}/file/query?path=/${appName}/${versionCode}"
        println "execute curl = ${curl}"
        def text = curl.execute().text
        def paths = new JsonSlurper().parseText(text).ecFiles
        if (paths != null && paths.size() > 0) {
            for (String path : paths) {
                def name = path.substring(path.lastIndexOf("/") + 1)
                def file = new File(jacocoExtension.coverageDirectory, name)
                if (file.exists() && file.length() > 0) //存在
                    continue
                println "execute curl -o ${file.getAbsolutePath()} ${host}${path}"
                "curl -o ${file.getAbsolutePath()} ${host}${path}".execute().text
            }
        }
    }

    def pullDiffClasses() {
        //获得两个分支的差异文件
        def includes = jacocoExtension.includes.collect {include -> return "*main/java/$include*"}.join(" ")
        def diff = "git diff origin/${jacocoExtension.contrastBranch} origin/${jacocoExtension.currentBranch} --name-only $includes".execute().text
        List<String> diffFiles = diff.split("\n")
        writerDiffToFile(diffFiles)

        //两个分支差异文件的目录
        File file = new File(jacocoExtension.gitPath)
        def workDir = "${file.getParent()}/work/${project.rootProject.name}/"
        def currentDir = "${workDir}${jacocoExtension.currentBranch}/"
        def contrastDir = "${workDir}${jacocoExtension.contrastBranch}/"

        project.delete(currentDir)
        project.delete(contrastDir)
        new File(currentDir).mkdirs()
        new File(contrastDir).mkdirs()

        //先把两个分支的所有class copy到temp目录
        copyBranchClass(contrastDir, currentDir)

        //再根据diffFiles 删除不需要的class
        deleteOtherFile(currentDir, diffFiles)
        deleteOtherFile(contrastDir, diffFiles)

        createDiffMethod(currentDir, contrastDir)
        writerDiffMethodToFile()
    }

    def writerDiffToFile(List<String> diffFiles) {
        String path = "${project.buildDir.getAbsolutePath()}/outputs/diff/diffFiles.txt"
        File parent = new File(path).getParentFile()
        if (!parent.exists()) parent.mkdirs()
        FileOutputStream fos = new FileOutputStream(path)
        for (String file: diffFiles)
            fos.write((file + "\n").getBytes())
        fos.close()
    }


    private void copyBranchClass(GString contrastDir, GString currentDir) {
        def tempClassDir = jacocoExtension.classDirectories.replace("${project.rootDir.getAbsolutePath()}/", "")
        String[] cmds = [jacocoExtension.copyClassShell, jacocoExtension.contrastBranch, jacocoExtension.currentBranch, tempClassDir, contrastDir, currentDir]
        println("cmds=" + cmds)

        Process process = cmds.execute()
        def out = new StringBuilder()
        def err = new StringBuilder()
        process.consumeProcessOutput( out, err )
        process.waitFor()
        if( out.size() > 0 ) println ("jacoco git success :\n" + out)
    }

    def deleteOtherFile(String dirPath, List<String> diffFiles) {
        def tempClassDir = jacocoExtension.classDirectories.replace("${project.rootDir.getAbsolutePath()}/", "")
        def diffFilesStr = diffFiles.toString()
        deleteFile(dirPath, {
            def path = ((File) it).getAbsolutePath()
            path = path.substring(path.indexOf(tempClassDir) + tempClassDir.length(), path.indexOf(".class"))
            if (path.contains("\$")) {
                path = path.substring(0, path.indexOf("\$"))
            }
            return !diffFilesStr.contains(path)
        })
    }

    void deleteFile(String dirPath, Closure closure) {
        File file = new File(dirPath)
        for (File subFile : file.listFiles()) {
            if (subFile.isDirectory()) {
                deleteFile(subFile.getAbsolutePath(), closure)
            } else {
                if (subFile.getName().endsWith(".class")) {
                    if (closure.call(subFile)) {
                        subFile.delete()
                    }
                } else {
                    subFile.delete()
                }
            }
        }
        file.delete()
    }

    def createDiffMethod(def currentDir, def contrastDir) {
        //生成差异方法
        DiffAnalyzer.getInstance().reset()
        DiffAnalyzer.readClasses(currentDir, DiffAnalyzer.CURRENT)
        DiffAnalyzer.readClasses(contrastDir, DiffAnalyzer.BRANCH)
        DiffAnalyzer.getInstance().diff()

        println("excludeMethod before diff.size=${DiffAnalyzer.getInstance().getDiffList().size()}")

        //excludeMethod
        if (jacocoExtension.excludeMethod != null) {
            Iterator<MethodInfo> iterator = DiffAnalyzer.getInstance().getDiffList().iterator()
            while (iterator.hasNext()) {
                MethodInfo info = iterator.next()
                if (jacocoExtension.excludeMethod.call(info))
                    iterator.remove()
            }
        }
        println("excludeMethod after diff.size=${DiffAnalyzer.getInstance().getDiffList().size()}")

    }

    def writerDiffMethodToFile() {
        String path = "${project.buildDir.getAbsolutePath()}/outputs/diff/diffMethod.txt"

        println("writerDiffMethodToFile size=" + DiffAnalyzer.getInstance().getDiffList().size() + " >" + path)

        FileUtils.writeToFile(new File(path), DiffAnalyzer.getInstance().toString())
    }

    static def toFileList(List<String> path) {
        List<File> list = new ArrayList<>(path.size())
        for (String s : path)
            list.add(new File(s))
        return list
    }
}