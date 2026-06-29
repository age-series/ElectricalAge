plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.named<Test>("test") {
    exclude("**/*BenchmarkTest.*", "**/*ProfilingTest.*")
}

tasks.register<Test>("benchmarkTest") {
    description = "Runs benchmark and profiling tests separately from the regular correctness test suite."
    group = "verification"

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    include("**/*BenchmarkTest.*", "**/*ProfilingTest.*")
    shouldRunAfter(tasks.named("test"))
}

tasks.named("check") {
    dependsOn(tasks.named("test"))
}
