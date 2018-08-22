import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude

plugins {
    `java-library`
}

repositories {
    jcenter()
}

dependencies {
    val grpcArtifact = fun(name: String): String { return "io.grpc:grpc-$name:1.14.0" }
    implementation(grpcArtifact("core"))
    testImplementation(grpcArtifact("testing")) {
        exclude("junit", "junit")
    }
    testRuntimeOnly(grpcArtifact("netty"))

    val junitJupiterArtifact = fun(name: String): String { return "org.junit.jupiter:junit-jupiter-$name:5.1.0" }
    testImplementation(junitJupiterArtifact("api"))
    testRuntimeOnly(junitJupiterArtifact("engine"))

    testImplementation("org.awaitility:awaitility:3.1.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
