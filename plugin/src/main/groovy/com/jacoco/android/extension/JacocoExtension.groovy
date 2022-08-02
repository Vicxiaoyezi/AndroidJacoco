package com.jacoco.android.extension

class JacocoExtension {
    //jacoco开关，false时不会进行probe插桩
    boolean jacocoEnable
    //需要对比的分支名
    String contrastBranch
    String currentBranch = "git symbolic-ref --short HEAD".execute().text.replaceAll("\n", "")
    //下载ec 的服务器
    String host
    //exec文件路径，支持多个ec文件，自动合并
    String coverageDirectory
    //源码目录，支持多个源码
    List<String> sourceDirectories
    //class目录，支持多个class目录
    String classDirectories
    //需要插桩的文件
    List<String> includes
    List<String> excludes
    //生成报告的目录
    String reportDirectory
    //git 提交命令
    String gitPushShell
    //复制class 的shell
    String copyClassShell

    String gitPath = "git rev-parse --show-toplevel".execute().text.replaceAll("\n", "")

    /**
     *
     * 方法过滤器 返回true 的将会被过滤
     * exclude{*     it = MethodInfo
     *}*/
    Closure excludeMethod

    List<String> getIncludes(){
        includes = includes.collect {include -> return include.replaceAll("\\.", "/")}
        return includes
    }
}