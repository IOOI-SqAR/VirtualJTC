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
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainModule = "VirtualJTCCommon"
    mainClass = "org.sqar.virtualjtc.jtcemu.Main"
}

tasks.test {
    useJUnitPlatform()
}