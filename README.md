# AndroidJacoco
AndroidJacoco 是用于Android App的增量代码测试覆盖率工具。

## 接入

#### 1.部署和配置服务器
&ensp;&ensp;&ensp;&ensp;因为在运行时会把ec数据文件上传到服务器，生成报告时会去服务器下载ec，所以要先配置服务器。服务器源码在main分支WebServer 文件夹。
* [使用IDEA将Web项目打成war包](/document/web1.md)
* [将Web项目war包部署到Tomcat服务器](/document/web2.md)

#### 2.添加插件版本
在gradle/dependency_versions.gradle文件下（一般Android项目都会用一个gradle文件来管理插件版本）
```gradle
ext {
    ifDependencies = [
        androidJacoco: '0.0.0008'
    ]
}
```

#### 3.在项目根目录的build.gradle添加jitpack仓库与插件
```gradle
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        //见 jacoco/jitpack 最新版
        classpath "com.github.Vicxiaoyezi.AndroidJacoco:plugin:${rootProject.ext.ifDependencies.androidJacoco}"
    }
}
    
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

#### 4.复制`app/shell`文件夹到你项目的`app`目录下，注意保持目录一致

#### 5.在app/build.gradle中应用插件

```gradle
buildTypes {
        release {
            buildConfigField "String", "host", "\"${jacocoCoverageConfig.host}\""
        }
        debug{
            buildConfigField "String", "host", "\"${jacocoCoverageConfig.host}\""
        }
    }
    
    
def ArrayList<String> getAllJavaDir() {
    Set<Project> projects = project.rootProject.subprojects
    List<String> javaDir = new ArrayList<>(projects.size())
    projects.forEach {
        javaDir.add("$it.projectDir/src/main/java")
    }
    return javaDir
}
    
dependencies {
    debugImplementation "com.github.Vicxiaoyezi.AndroidJacoco:rt:${rootProject.ext.ifDependencies.androidJacoco}"
    releaseImplementation "com.github.Vicxiaoyezi.AndroidJacoco:rt:${rootProject.ext.ifDependencies.androidJacoco}"
}
    
apply plugin: 'com.jacoco.android'
    
jacocoCoverageConfig {
    jacocoEnable = true
    contrastBranch = "release"
    host = "http://10.0.0.99:8080"
    coverageDirectory = "${project.buildDir.absolutePath}/outputs/coverage"
    sourceDirectories = getAllJavaDir()
    classDirectories = ["${rootProject.projectDir.absolutePath}/app/classes"]
    gitPushShell = "${project.projectDir}/shell/gitPushShell.sh"
    copyClassShell = "${project.projectDir}/shell/pullDiffClass.sh"
    includes = ['com.pokemon.xerneas']
    excludeClass = {
//        return it.contains("databinding")
        return false
    }
    excludeMethod = {
        return false
    }
}
    
```
* **jacocoCoverageConfig** 是代码覆盖的配置。
* **jacocoEnable：** 是总开关，开启会copy class,执行 git命令等，插入代码。
* **contrastBranch:** 要对比的分支名，一般为线上稳定分支，如release，用于切换到该分支copy class
* **host:** 上传和下载ec文件的服务器
* **coverageDirectory：** 生成报告时从服务器下载ec 文件的存放目录
* **classDirectories：** class的存放路径
* **gitPushShell、copyClassShell：** git 命令
* **includes：** 要测试的class 包名
* **excludeClass：** 过滤某些自动生成的class包,例如databinding....。`return it.contains("databinding")` return true表示过滤
* **excludeMethod：** 过滤某些方法，因为在编译时，会自动生成某些方法。如带 $ 的虚方法。
* **reportDirectory：** 报告输出目录，默认为 `"${project.buildDir.getAbsolutePath()}/outputs/report"`
* **rt：** 是运行时的库

#### 6.在Application中添加触发生成和上传ec文件（项目全局搜Application() ）
```java
public class MyApp extends Application {
    public static Application app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        CodeCoverageManager.init(app, BuildConfig.host);//内网 服务器地址);
        CodeCoverageManager.uploadData();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            CodeCoverageManager.generateCoverageFile();
        }
    }
}
    
```
运行一会杀掉后台重启App，这时 App 会把上次的 ec 文件上传到服务器。

## 运行 
1. Android Studio 设置 Gradle JDK 为本地JDK
Android Studio ——> Preferences ——> Build ——> Build Tools ——> Gradle ——> Gradle JDK 
选择本地JDK，例如：/Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk/Contents/Home

2. `git merge contrastBranch`分支（release），build后会自动上传class文件到`contrastBranch`分支，只builde一次不需要运行

3. `git merge currentBranch`分支（开发测试的分支），build后会自动上传class文件到 `currentBranch`分支，使用打好包的包进行测试，测结束一定要杀掉后台重启App，触发生成和上传ec文件

## 生成报告
项目根目录下执行`./gradlew generateReport`生成报告，
报告目录为: `app/builds/outputs/report`
浏览器打开`index.html`，就可以看见本次的覆盖率报告了。

