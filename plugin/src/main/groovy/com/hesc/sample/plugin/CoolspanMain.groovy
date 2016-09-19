package com.hesc.sample.plugin

import javassist.ClassPool
import javassist.CtClass
import org.gradle.api.Plugin
import org.gradle.api.Project;

class CoolspanMain implements Plugin<Project>{

    @Override
    void apply(Project project) {

        project.afterEvaluate{
            project.android.applicationVariants.each{ variant->
                if(variant.name.capitalize().contains("Debug")){

                    def preDexTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")
                    if(preDexTask) {
                        def javassistDebug = "javassistDebug"
                        project.task(javassistDebug) << {
                            println("begin executing javassist...")

                            def buildDir = project.buildDir.getAbsolutePath() +
                                    "/intermediates/classes/" + variant.name

                            //这里实例化ClassPool必须传入参数true，否则会报No class：System.out错误
                            ClassPool classes = new ClassPool(true)
                            classes.clearImportedPackages()
                            classes.appendClassPath(buildDir)

                            def classname = "com.hesc.sample.javassist.MainActivity"
                            CtClass c = classes.getCtClass(classname)
                            //如果类已经被装载，则会冻结住不能再修改；这里要先进行解冻
                            if (c.isFrozen()) {
                                c.defrost()
                            }

                            if (c.getConstructors().length > 0) {
                                def constructor = c.getConstructors()[0]
                                if (constructor != null) {
                                    constructor.insertBefore("System.out.println(\"hello java!\");")
                                }
                            }

                            c.writeFile(buildDir)
                        }

                        //貌似对系统存在的task执行dofirst无效，下面这句不会打印 hello java
//                        javassistDebugTask.doFirst = {
//                            println "hello java"
//                        }

                        def javassistDebugTask = project.tasks[javassistDebug]
                        javassistDebugTask.dependsOn preDexTask.taskDependencies.getDependencies(preDexTask)
                        preDexTask.dependsOn javassistDebugTask
                    }
                }

            }
        }
    }
}