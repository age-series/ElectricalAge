# Agents

If you learn things about making the gradle build work in the sandbox, document it under the Sandbox section.

# Various Notes

* This is a Minecraft Forge 1.7.10 mod called Electrical Age (Eln). It uses an updated GTNH gradle build system.
* We are aiming to move to 100% Kotlin. Where possible, use Kotlin for new code.
* Java source files belong in the `src/main/java` directory. Kotlin source files belong in the `src/main/kotlin` directory.
* Do not update the gralde configuration or wrapper without asking first. They are from an upstream repository.
* If you create documentation, please add it do the `docs` directory.

# Sandbox

* When `./gradlew` fails inside the sandbox claiming it “Could not determine a usable wildcard IP”, rerun with `GRADLE_USER_HOME=$PWD/.gradle` and, if needed, request escalation—the build otherwise succeeds once it can write its cache and bind to an IP.
