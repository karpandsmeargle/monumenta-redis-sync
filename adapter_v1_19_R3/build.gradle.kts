plugins {
    id("com.playmonumenta.redissync.java-conventions")
    id("io.papermc.paperweight.userdev") version "1.3.3"
}

dependencies {
    compileOnly(project(":adapter_api"))
    paperDevBundle("1.19.4-R0.1-SNAPSHOT")
}

description = "v1_19_R3"
version = rootProject.version

tasks {
    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
}
