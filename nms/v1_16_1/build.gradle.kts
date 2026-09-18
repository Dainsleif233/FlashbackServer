plugins {
    java
}

// Paper 1.16.1 has no paperweight 2.x dev-bundle. Compile against the patched
// Paper server jar (Spigot NMS names under net.minecraft.server.v1_16_R1).
// Produce that jar once by running paper-1.16.1-*.jar (paperclip); it lands in
// libs/paperclip-run/cache/patched_1.16.1.jar.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    // Paper 1.16.1 supports Java <= 14; emit Java 11 class files to match core.
    options.release.set(8)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(files("libs/paperclip-run/cache/patched_1.16.1.jar"))
    implementation(project(":core"))
}
