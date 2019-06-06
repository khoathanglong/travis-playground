import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
}

project.extra.apply {
    set("kafkaVersion", "2.2.0-cp2")
    set("okhttpVersion", "3.14.1")
    set("jacksonVersion", "2.9.9")
    set("openCsvVersion", "4.6")
    set("confluentVersion", "5.0.0")
    set("radarSchemaVersion", "0.5.0")
}

repositories {
    jcenter()
    maven(url = "http://packages.confluent.io/maven/")
    maven(url = "https://dl.bintray.com/radar-cns/org.radarcns")
}

sourceSets {
    create("integrationTest") {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/integrationTest/java")
            resources.srcDir("src/integrationTest/resources")
            compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
            runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
        }
    }
}

dependencies {
    api("com.squareup.okhttp3:okhttp:${project.extra["okhttpVersion"]}")
    api("io.confluent:kafka-connect-avro-converter:${project.extra["confluentVersion"]}")
    api("org.radarcns:radar-schemas-commons:${project.extra["radarSchemaVersion"]}")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${project.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${project.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${project.extra["jacksonVersion"]}")
    implementation("com.opencsv:opencsv:${project.extra["openCsvVersion"]}")


    // Included in connector runtime
    compileOnly("org.apache.kafka:connect-api:${project.extra["kafkaVersion"]}")
    implementation(kotlin("stdlib-jdk8"))


    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.apache.kafka:connect-api:${project.extra["kafkaVersion"]}")
}

// config JVM target to 1.8 for kotlin compilation tasks
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

task<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mustRunAfter(tasks["test"])
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}


tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath.get().files)
    into("${buildDir}/third-party")
}

tasks.register("downloadDependencies") {

    configurations["runtimeClasspath"].files
    configurations["compileClasspath"].files

    doLast {
        println("Downloaded all dependencies")
    }
}