import org.gradle.kotlin.dsl.java

plugins {
    `maven-publish`
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

group = "cz.lukynka"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("org.jetbrains.exposed:exposed-core:0.37.3")
    implementation("org.jetbrains.exposed:exposed-dao:0.37.3")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.37.3")

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

sourceSets["main"].resources.srcDir("${buildDir}/generated/resources/")

sourceSets["main"].java.srcDir("src/main/kotlin")

tasks {
    val sourcesJar by creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier = "sources"
        from(sourceSets["main"].allSource)
    }

    artifacts {
        add("archives", sourcesJar)
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        maven {
            url = if(version.toString().endsWith("-SNAPSHOT")) {
                uri("https://mvn.devos.one/snapshots")
            } else {
                uri("https://mvn.devos.one/releases")
            }
            credentials {
                username = System.getenv()["MAVEN_USER"]
                password = System.getenv()["MAVEN_PASS"]
            }
        }
    }
    publications {
        register<MavenPublication>("maven") {
            groupId = "cz.lukynka"
            artifactId = "hollow"
            version = version.toString()
            from(components["java"])
        }
    }
}
