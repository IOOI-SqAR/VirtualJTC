plugins {
    id("java")
    id("application")
}

group = "org.sqar.virtualjtc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":JTCEMUCommon"))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainModule = "JTCEMU"
    mainClass = "org.jens_mueller.jtcemu.platform.se.Main"
}

tasks.test {
    useJUnitPlatform()
}