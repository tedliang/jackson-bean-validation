plugins {
    `java-library`
    kotlin("jvm") version "2.3.21"
    signing
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

repositories {
    mavenCentral()
}


val jacksonVersion = "2.15.2"

// Create the javaOnlyTest source set (replaces the test-sets plugin)
sourceSets {
    create("javaOnlyTest") {
        java.srcDir("src/java-only-test/java")
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

configurations {
    named("javaOnlyTestImplementation") {
        extendsFrom(configurations["implementation"])
    }
    named("javaOnlyTestRuntimeOnly") {
        extendsFrom(configurations["runtimeOnly"])
    }
}

val javaOnlyTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["javaOnlyTest"].output.classesDirs
    classpath = sourceSets["javaOnlyTest"].runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn(javaOnlyTest)
}


dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("jakarta.validation:jakarta.validation-api:3.0.2")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("reflect"))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.24")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    testImplementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.2.3")

    "javaOnlyTestImplementation"(platform("org.junit:junit-bom:5.11.4"))
    "javaOnlyTestImplementation"("org.junit.jupiter:junit-jupiter-api")
    "javaOnlyTestImplementation"("org.hibernate.validator:hibernate-validator:8.0.1.Final")
    "javaOnlyTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine")
    "javaOnlyTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll("-Xjvm-default=all", "-java-parameters")
    }
}


tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

val jacksonCompatVersions = mapOf(
    "2.13" to (0..5), "2.14" to (0..3), "2.15" to (0..2)
).flatMap { (majorMinor, patchVersions) -> patchVersions.map { "$majorMinor.$it" } }

for (compatVersion in jacksonCompatVersions) {
    val testConfiguration: Configuration = project.configurations.create("jacksonCompatTest_$compatVersion")
    testConfiguration.extendsFrom(configurations["testRuntimeClasspath"])
    testConfiguration.resolutionStrategy.eachDependency {
        if (requested.group.startsWith("com.fasterxml.jackson.") && requested.name != "jackson-annotations") {
            useVersion(compatVersion)
        }
    }

    val testTask = project.tasks.register("jacksonCompatTest_$compatVersion", Test::class) {
        group = JavaBasePlugin.VERIFICATION_GROUP
        shouldRunAfter("test")
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].output + sourceSets["main"].output + testConfiguration
        exclude("**/KotlinValidationTest.class")
    }

    tasks.named("check") {
        dependsOn(testTask)
    }
}



publishing {
    publications {
        create("mavenJava", MavenPublication::class) {
            from(components["java"])
            pom {
                val githubRepo = providers.gradleProperty("githubRepo")
                val githubUrl = githubRepo.map { "https://github.com/$it" }

                name.set(providers.gradleProperty("projectName"))
                description.set(providers.gradleProperty("projectDescription"))
                url.set(providers.gradleProperty("projectUrl"))
                licenses {
                    license {
                        name.set(providers.gradleProperty("projectLicenseName"))
                        url.set(providers.gradleProperty("projectLicenseUrl"))
                    }
                }
                developers {
                    developer {
                        name.set(providers.gradleProperty("developerName"))
                        email.set(providers.gradleProperty("developerEmail"))
                        url.set(providers.gradleProperty("developerUrl"))
                    }
                }
                scm {
                    url.set(githubUrl.map { "$it/tree/master" })
                    connection.set(githubRepo.map { "scm:git:git://github.com/$it.git" })
                    developerConnection.set(githubRepo.map { "scm:git:ssh://github.com:$it.git" })
                }
                issueManagement {
                    url.set(githubUrl.map { "$it/issues" })
                    system.set("GitHub")
                }
            }
        }
    }

    repositories {
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("repos/releases"))
        }
    }
}

/*
signing {
    sign(publishing.publications["mavenJava"])
}
*/

nexusPublishing {
    repositories {
        sonatype()
    }
}
