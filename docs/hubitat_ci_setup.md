# Hubitat CI setup

## Prerequisites
- JDK 11 installed and selected in your shell/IDE.
- Git (for cloning your scripts/repos).
- Gradle or Maven if you build with those; plain Groovy works with `@Grab`.

## Getting the library
- **Published artifact** (preferred): `me.biocomp.hubitat_ci:hubitat_ci:0.25.2` (version from current build file).
- **Local checkout**: clone this repo and either publish to `mavenLocal` or reference the built JAR under `build/libs`.

## Gradle (Groovy DSL) dependency
```groovy
dependencies {
    testImplementation 'me.biocomp.hubitat_ci:hubitat_ci:0.25.2'
}
```
If using a local checkout JAR, add:
```groovy
repositories {
    mavenLocal()
    flatDir { dirs "$rootDir/build/libs" }
}
```

## Maven dependency
```xml
<dependency>
  <groupId>me.biocomp.hubitat_ci</groupId>
  <artifactId>hubitat_ci</artifactId>
  <version>0.25.2</version>
  <scope>test</scope>
</dependency>
```
If using a locally built JAR, install it with `mvn install:install-file` or point to a local repo (`mavenLocal`).

## Plain Groovy / @Grab
Add a grab to your script or test runner:
```groovy
@Grab('me.biocomp.hubitat_ci:hubitat_ci:0.25.2')
@GrabConfig(systemClassLoader=true)
```
`systemClassLoader` helps when mixing with Spock/JUnit runners.

## Building locally
From repo root:
```bash
gradlew clean build
```
Artifacts appear under `build/libs` (main JAR, sources JAR, groovydoc JAR). Publish locally if desired:
```bash
gradlew publishToMavenLocal
```

## IDE setup
### IntelliJ IDEA (Community or Ultimate)
- Install Groovy plugin (bundled in Ultimate; enable if disabled).
- Set Project SDK to JDK 11.
- Import project as Gradle (recommended). IntelliJ will download dependencies and index sources.
- To debug tests and scripts: use the Gradle tool window to run tests with debugger attached.
- If using a local JAR: add a `flatDir` repo as above or add the JAR as a module library; attach `hubitat_ci-sources.jar` for navigation.

### VS Code
- Install **Groovy** and **Java** extensions (e.g., Red Hat Language Support for Java) and **Gradle Tasks** for convenience.
- Use JDK 11 in your VS Code Java configuration (`java.configuration.runtimes`).
- Open the folder; the Java tooling will detect Gradle/Maven builds. Use the Test Explorer to run Spock/JUnit tests.
- For plain Groovy scripts, use the Groovy extension to run via CodeLens, ensuring `@Grab` pulls `hubitat_ci`.

## Choosing published vs local builds
- Use the published artifact for stability in CI and shared repos.
- Use local builds when you need unreleased fixes; remember to keep `mavenLocal` or `flatDir` entries confined to local-only builds to avoid breaking CI.

## JDK 11 notes
- The library is built against Groovy 2.5.x and runs well on JDK 11.
- Newer JDKs may work but are less tested; if you see classloader/module warnings, prefer sticking to JDK 11.

