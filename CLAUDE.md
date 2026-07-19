# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

IntelliJ IDEA Ultimate plugin for Apache Grails (plugin id `org.intellij.grails`): GSP language support, Grails project structure/navigation, run configurations, taglib/domain-class support. Originally developed by JetBrains, imported from `JetBrains/intellij-obsolete-plugins`, now being migrated to ASF governance — see `MIGRATION-PLAN.md` (compliance/CI) and `IMPROVEMENT-PLAN.md` (functional roadmap, two-plugin strategy: this legacy line vs. a future Grails 7+ plugin).

## Commands

Requires Java 25 and targets IntelliJ platform 2026.2 (`sinceBuild` 262); versions are pinned in `.sdkmanrc` and `gradle.properties`.

```bash
./gradlew check                              # compile + run all tests (what CI runs)
./gradlew test --tests 'GrailsCodecTest'     # run a single test class
./gradlew buildPlugin                        # plugin ZIP → build/distributions/
./gradlew runIde                             # sandbox IDE with the plugin
./gradlew verifyPlugin                       # IntelliJ Plugin Verifier (CI gates on it)
./gradlew rat                                # Apache RAT license audit
```

Tests are JUnit 4 + AssertJ. Some tests need IDE sources locally: `test.idea.home.path` in `gradle.properties` points to a local intellij-community checkout (unset/nonexistent is fine — CI runs without it).

## Layout and build wiring

Non-standard source roots on the root project: `src/` (main Java/Kotlin), `gen/` (generated code — the GSP lexers, regenerated from `src/.../lang/gsp/lexer/core/*.flex` via JFlex; don't hand-edit), `test/`, `resources/` + `compatibilityResources/`, and `testdata/` (mock Grails 1.x projects/JARs — content is test input, never add license headers there).

Subprojects and how they attach:
- `copyright`, `coverage`, `hibernate`, `i18n`, `langInjection`, `maven` — optional plugin modules (`pluginModule(...)` in `build.gradle.kts`, declared in `<content>` in `plugin.xml` as `intellij.groovy.grails.*`).
- `grails-rt`, `grails-compiler-patch`, `jps-plugin`, `gradle-tooling` — runtime/build-process code injected into the user's app or the JPS build; these target Java 8/11, not 25.
- `testFramework` — base test classes, plus Groovy/Gradle-plugin test infrastructure copied from the original plugins' test sources.

`resources/standardDsls/` is deliberately excluded from normal resource packaging and copied to `lib/standardDsls/` by a `PrepareSandboxTask` customization.

## Architecture

Everything lives under the legacy packages `org.jetbrains.plugins.grails` (main) and `org.jetbrains.plugins.groovy.{mvc,grails,dsl}` — keep these package names (a rename is a deliberate, separate effort per MIGRATION-PLAN decision 2).

- **Project structure model** (`grails/structure/`): `GrailsApplication` abstraction with two eras — `OldGrails*` (Grails 2.x, BuildConfig.groovy/Gant) and `Grails3Application` (Gradle-based). `GrailsApplicationManager` discovers apps; `GrailsApplicationProvider` is an extension point. The older `org.jetbrains.plugins.groovy.mvc` package (`MvcModuleStructureSynchronizer` etc.) is the generic MVC-framework layer this predates and still runs on.
- **GSP language** (`grails/lang/gsp/`): complete custom language — JFlex lexers (in `gen/`), parser, PSI, formatting, folding, completion, with HTML/Groovy/CSS/JS injection. JS/CSS integration is via optional `depends` config files (`grails-js-integration.xml`, `grails-css-integration.xml`).
- **Artefact handlers** (`grails/artefact/`): model Grails conventions (controllers, domain classes, services, taglibs); extension point `artefactHandler`.
- **Groovy magic** (`grails/references/`, `grails/gorm/`, `src/org/jetbrains/plugins/groovy/dsl`): ~20 non-code member contributors for GORM criteria/named queries/constraints, URL-mapping references, Spring bean discovery.
- **Extension points** (declared in `plugin.xml`): `facetProvider`, `applicationProvider`, `commandExecutor`, `commandProvider`, `artefactHandler`, `viewNodeProvider`.

`resources/META-INF/plugin.xml` is the wiring hub; it depends on many Ultimate-only bundled plugins (javaee, jsp, spring, persistence, database, microservices), mirrored in `build.gradle.kts` `bundledPlugin(...)` calls — a new platform dependency must be added in both places.

## Conventions

- **License headers**: existing JetBrains-copyright files keep their `Copyright 2000-20xx JetBrains s.r.o. and contributors` Apache-2.0 header verbatim — do NOT convert them to the ASF "Licensed to the Apache Software Foundation" header (no software grant is recorded yet; MIGRATION-PLAN decision 1). New ASF-authored files (e.g. CI config) use the ASF header. `./gradlew rat` enforces headers; any new RAT exclude must carry a justification comment.
- Mixed Java/Kotlin codebase; match the language of the code you're touching.
