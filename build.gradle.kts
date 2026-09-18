plugins {
    java
    // Declared here so subprojects can apply them; not applied to the root itself.
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.22" apply false
    id("xyz.jpenilla.run-paper") version "3.0.2"
    // Shadow assembles the single bundled plugin jar.
    id("com.gradleup.shadow") version "8.3.6"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/maven-releases/")
}

dependencies {
    // Bundle :core compiled classes + resources (plugin.yml/config.yml) into the shaded jar.
    implementation(project(":core"))
}

// ── Bundled plugin jar ─────────────────────────────────────────────────────
// The final deployable jar = :core classes + resources + the REOBF'd :nms:v1_21_5 adapter classes.
// Paper's PluginRemapper remaps the (mojang→spigot) nms references in the reobf classes at load.
// Ensure the nms subprojects are configured first so their paperweight `reobfJar` tasks exist.
val nmsVersions = listOf("5", "6", "7", "8", "9", "10", "11")
nmsVersions.forEach { v -> evaluationDependsOn(":nms:v1_21_$v") }
val nmsReobfJars = nmsVersions.associateWith { v ->
    project(":nms:v1_21_$v").tasks.named("reobfJar")
}

tasks.shadowJar {
    archiveClassifier.set("")
    // Pull in each version adapter's remapped NMS classes (not the mojang-mapped ones).
    nmsReobfJars.forEach { (v, reobfJar) ->
        dependsOn(reobfJar)
        from(zipTree(reobfJar.map { it.outputs.files.singleFile })) {
            include("dev/zeffut/flashbackserver/version/v1_21_$v/**")
        }
    }
    // Paper 26.1+ runs Mojang-mapped plugins, so these adapters must not be reobfuscated.
    val mojangMappedNms = listOf("v26_2", "v26_3")
    mojangMappedNms.forEach { nms ->
        val jarTask = project(":nms:$nms").tasks.named<Jar>("jar")
        dependsOn(jarTask)
        from(zipTree(jarTask.map { it.outputs.files.singleFile })) {
            include("dev/zeffut/flashbackserver/version/$nms/**")
        }
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// ── Integration tests ──────────────────────────────────────────────────────
// The harness (in :core test) deploys the BUNDLED root jar via the flashback.plugin.jar property.
val integrationTest by tasks.registering(Test::class) {
    val coreTest = project(":core").extensions
        .getByType<SourceSetContainer>()["test"]
    useJUnitPlatform { includeTags("integration") }
    testClassesDirs = coreTest.output.classesDirs
    classpath = coreTest.runtimeClasspath
    dependsOn(tasks.shadowJar)
    systemProperty(
        "flashback.plugin.jar",
        tasks.shadowJar.get().archiveFile.get().asFile.absolutePath
    )
    shouldRunAfter(project(":core").tasks.named("test"))
}

// Focused real-server smoke for the Java-25 Paper 26.x adapters. This avoids
// conflating them with the Java-21 regression suite while running the exact
// deployed shadow jar, booting the server, asserting plugin enablement, and
// performing its controlled shutdown.
fun registerPaper26Smoke(taskName: String, testClass: String) =
    tasks.register<Test>(taskName) {
        val coreTest = project(":core").extensions
            .getByType<SourceSetContainer>()["test"]
        useJUnitPlatform { includeTags("integration") }
        filter { includeTestsMatching(testClass) }
        testClassesDirs = coreTest.output.classesDirs
        classpath = coreTest.runtimeClasspath
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        })
        dependsOn(tasks.shadowJar)
        systemProperty(
            "flashback.plugin.jar",
            tasks.shadowJar.get().archiveFile.get().asFile.absolutePath
        )
        shouldRunAfter(project(":core").tasks.named("test"))
    }

val paper26_2Smoke = registerPaper26Smoke(
    "paper26_2Smoke",
    "dev.zeffut.flashbackserver.harness.Paper26_2SmokeIT"
)
val paper26_3Smoke = registerPaper26Smoke(
    "paper26_3Smoke",
    "dev.zeffut.flashbackserver.harness.Paper26_3SmokeIT"
)

tasks.runServer {
    minecraftVersion("1.21.5")
    // Run the bundled jar so manual testing matches the deployed artifact.
    pluginJars(tasks.shadowJar.flatMap { it.archiveFile })
}
