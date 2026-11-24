// Top-level build file. Plugin versions are declared here and applied (without `apply`)
// in modules that need them.
plugins {
    id("com.android.application") version "9.3.3" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
