plugins {
    java
    id("io.papermc.paperweight.userdev")
}

java {
    // Paper 26.3's API and runtime require Java 25 or newer.
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

// Paper 26.3's Paperclip patcher also requires Java 25.
paperweight {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/maven-releases/")
}

dependencies {
    // Paper's supported 26.3 dev bundle coordinate; this resolves the latest 26.3 build.
    // 26.3 is currently alpha-only on the PaperMC repo; keep the dynamic coordinate so
    // later stable builds pick up automatically, matching the v26_2 module.
    paperweight.paperDevBundle("26.3.build.+")
    implementation(project(":core"))
}
