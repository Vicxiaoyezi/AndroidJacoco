package com.jacoco.android.extension

class JacocoExtension {
    boolean jacocoEnable
    String contrastBranch
    String currentBranch = "git symbolic-ref --short HEAD".execute().text.replaceAll("\n", "")
    String host
    String coverageDirectory
    List<String> sourceDirectories
    String classDirectories
    String gitPushShell
    String copyClassShell
    List<String> includes
    List<String> excludes
    String reportDirectory
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