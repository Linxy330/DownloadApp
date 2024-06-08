// 根 build.gradle.kts 文件

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
