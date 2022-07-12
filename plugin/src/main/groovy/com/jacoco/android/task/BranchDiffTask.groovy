package com.jacoco.android.task


import com.android.utils.FileUtils
import com.jacoco.android.extension.JacocoExtension
import com.jacoco.android.report.ReportGenerator
import com.jacoco.android.util.Utils
import groovy.json.JsonSlurper
import org.codehaus.groovy.runtime.IOGroovyMethods
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jacoco.core.data.MethodInfo
import org.jacoco.core.diff.DiffAnalyzer
import com.jacoco.android.util.ClientUploadUtils

class BranchDiffTask extends DefaultTask {
    def currentBranch//当前分支
    def gitPth = "git rev-parse --show-toplevel".execute().text.replaceAll("\n", "")
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
        ReportGenerator generator = new ReportGenerator(jacocoExtension.coverageDirectory, toFileList(jacocoExtension.classDirectories),
                toFileList(jacocoExtension.sourceDirectories), new File(jacocoExtension.reportDirectory))
        generator.create()
        ClientUploadUtils upload = new ClientUploadUtils()
        upload.upload(jacocoExtension.reportDirectory)
        System.out.println("点击链接查看测试报告:" + jacocoExtension.host + "/report/index.html")
    }

    def toFileList(List<String> path) {
        List<File> list = new ArrayList<>(path.size())
        for (String s : path)
            list.add(new File(s))
        return list
    }

    def pullDiffClasses() {
        currentBranch = "git symbolic-ref --short HEAD".execute().text.replaceAll("\n", "")

        //获得两个分支的差异文件
        def diff = "git diff origin/${jacocoExtension.contrastBranch} origin/${currentBranch} --name-only".execute().text
        List<String> diffFiles = getDiffFiles(diff)
        println("diffFiles size=" + diffFiles.size())
        writerDiffToFile(diffFiles)

        //两个分支差异文件的目录
        File file = new File(gitPth)
        def currentDir = "${file.getParent()}/work/${currentBranch}/app"
        def contrastDir = "${file.getParent()}/work/${jacocoExtension.contrastBranch}/app"

        project.delete(currentDir)
        project.delete(contrastDir)
        new File(currentDir).mkdirs()
        new File(contrastDir).mkdirs()

        //先把两个分支的所有class copy到temp目录
        copyBranchClass(jacocoExtension.contrastBranch, contrastDir)
        copyBranchClass(currentBranch, currentDir)
        //再根据diffFiles 删除不需要的class
        deleteOtherFile(currentDir, diffFiles)
        deleteOtherFile(contrastDir, diffFiles)

        //删除空文件夹
        deleteEmptyDir(new File(currentDir))
        deleteEmptyDir(new File(contrastDir))
        createDiffMethod(currentDir, contrastDir)
        writerDiffMethodToFile()
    }

    def writerDiffToFile(List<String> diffFiles) {
        String path = "${project.buildDir.getAbsolutePath()}/outputs/diff/diffFiles.txt"
        File parent = new File(path).getParentFile()
        if (!parent.exists()) parent.mkdirs()

        println("writerDiffToFile size=" + diffFiles.size() + " to >" + path)

        FileOutputStream fos = new FileOutputStream(path)
        for (String str : diffFiles) {
            fos.write((str + "\n").getBytes())
        }
        fos.close()
    }

    def writerDiffMethodToFile() {
        String path = "${project.buildDir.getAbsolutePath()}/outputs/diff/diffMethod.txt"

        println("writerDiffMethodToFile size=" + DiffAnalyzer.getInstance().getDiffList().size() + " >" + path)

        FileUtils.writeToFile(new File(path), DiffAnalyzer.getInstance().toString())
    }

    def deleteOtherFile(String dirPath, List<String> diffFiles) {
        def targetPath
        if (project.rootDir.getPath() == gitPth){
            targetPath = "app"
        } else {
            targetPath = project.rootDir.getPath().replace("${gitPth}/", "") + "/app"
        }
        readFiles(dirPath, {
            String path = ((File) it).getAbsolutePath().replace(dirPath, targetPath)
            return diffFiles.contains(path)
        })
    }

    void readFiles(String dirPath, Closure closure) {
        File file = new File(dirPath)
        if (!file.exists()) {
            return
        }
        File[] files = file.listFiles()
        for (File classFile : files) {
            if (classFile.isDirectory()) {
                readFiles(classFile.getAbsolutePath(), closure)
            } else {
                if (classFile.getName().endsWith(".class")) {
                    if (!closure.call(classFile)) {
                        classFile.delete()
                    }
                } else {
                    classFile.delete()
                }
            }
        }
    }

    private void copyBranchClass(String contrastBranch, GString contrastDir) {
        String[] cmds
        if (Utils.windows) {
            cmds = new String[5]
            cmds[0] = jacocoExtension.getGitBashPath()
            cmds[1] = jacocoExtension.copyClassShell
            cmds[2] = contrastBranch
            cmds[3] = project.rootDir.getAbsolutePath()
            cmds[4] = contrastDir.toString()
        } else {
            cmds = new String[4]
            cmds[0] = jacocoExtension.copyClassShell
            cmds[1] = contrastBranch
            cmds[2] = project.rootDir.getAbsolutePath()
            cmds[3] = contrastDir.toString()
        }

        println("cmds=" + cmds)
        Process proc = Runtime.getRuntime().exec(cmds)
        String result = IOGroovyMethods.getText(new BufferedReader(new InputStreamReader(proc.getIn())))
        String error = IOGroovyMethods.getText(new BufferedReader(new InputStreamReader(proc.getErr())))

        println("copyClassShell success :" + result)
        println("copyClassShell error :" + error)

        proc.closeStreams()
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

    List<String> getDiffFiles(String diff) {
        List<String> diffFiles = new ArrayList<>()
        if (diff == null || diff == '') {
            return diffFiles
        }
        String[] strings = diff.split("\n")
        def classes = "/classes/"
        strings.each {
            if (it.endsWith('.class')) {
                String classPath = it.substring(it.indexOf(classes) + classes.length())
                if (isInclude(classPath)) {
                    if (jacocoExtension.excludeClass != null) {
                        boolean exclude = jacocoExtension.excludeClass.call(it)
                        if (!exclude) {
                            diffFiles.add(it)
                        }
                    } else {
                        diffFiles.add(it)
                    }
                }
            }
        }
        return diffFiles
    }

    def isInclude(String classPath) {
        List<String> includes = jacocoExtension.includes
        for (String str : includes) {
            if (classPath.startsWith(str.replaceAll("\\.", "/"))) {
                return true
            }
        }
        return false
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

        def curl = "curl ${host}/query?path=/${appName}/${versionCode}"
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
        println "downloadData 下载完成"
    }


    boolean deleteEmptyDir(File dir) {
        if (dir.isDirectory()) {
            boolean flag = true
            for (File f : dir.listFiles()) {
                if (deleteEmptyDir(f))
                    f.delete()
                else
                    flag = false
            }
            return flag
        }
        return false
    }
}