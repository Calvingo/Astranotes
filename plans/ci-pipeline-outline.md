# Week 10.1 Lab: AstraNotes CI/CD Workflow Exercise

## AstraNotes Repo State Summary

AstraNotes currently has a runnable Spring Boot web application, Maven build, draft GitHub Actions workflow, planning/design artifacts, and a multi-user class-demo implementation.

Current repo state:

- Repository: AstraNotes GitHub repository with `main` as the primary branch.
- Current implementation: Java 17 Spring Boot web app with Thymeleaf pages, SQLite storage, encryption/HMAC, demo login, session handling, note ownership, and read-only sharing.
- Build command: `mvn -B -V clean test package`.
- Run command: `mvn spring-boot:run`.
- Demo URL: `http://127.0.0.1:8080/login`.
- Current automated tests: model, storage, integration, search benchmark, validation, and multi-user authorization tests.
- Draft CI file: `.github/workflows/ci.yml`.
- Known maturity limit: this is a class-demo web application, not production authentication or hosted deployment.

## Selected Problem Scenario

**Scenario C: Sharing or permission risk**

This is now the best match for AstraNotes because the project includes demo users, note ownership, and read-only note sharing. The most important CI/CD risk is allowing a change that breaks owner-only edit/delete/share behavior.

Secondary concern:

**Scenario A: Broken main**

The project still needs stable build/test gates so `main` does not break after manual-only validation.

## CI/CD Check Table

| Stage | Check | Advisory or Blocking? | Why? | Evidence Produced |
|---|---|---|---|---|
| Commit | Compile with Java 17 | Blocking | Catches syntax, dependency, and runtime target errors quickly. | Maven compile log |
| Commit | Stable unit/service tests | Blocking | Title validation and service behavior should fail fast before PR review. | JUnit result |
| Commit | Local package check | Blocking before push when build files changed | Confirms Spring Boot packaging still works. | Maven package log and JAR |
| PR | Full Maven build: `mvn -B -V clean test package` | Blocking | Prevents broken build, failed tests, or missing package artifact from entering `main`. | GitHub Actions log |
| PR | SQLite storage integration tests | Blocking | Storage, encryption fallback, FTS search, delete, purge, and export/import are core risk areas. | Surefire test report |
| PR | Multi-user authorization tests | Blocking | Owner-only edit/delete/share is the highest privacy risk in the web app. | JUnit permission test report |
| PR | Search benchmark output | Advisory | Useful signal, but timing can be noisy across machines and should not block healthy work yet. | `SEARCH_BENCHMARK_AVG_MS` log |
| PR | AI-generated code/test review note | Advisory | AI output must be checked by the student; this is governance evidence, not an automatic pass/fail. | PR comment or checklist |
| Release | Final clean build on `main` | Blocking | Confirms release candidate starts from the merged branch, not a local-only state. | Release build log |
| Release | Demo workflow test | Blocking | Final demo should show login, create, search, edit, share, shared read-only access, delete, export/import. | Manual checklist or browser evidence |
| Release | Dependency/security review | Advisory now | Useful governance check, but no automated scanner is configured yet. | Dependency list or scan report |

## Draft Pipeline Outline

The repository contains a draft workflow at `.github/workflows/ci.yml`.

```yaml
name: CI

on:
  push:
    branches:
      - main
      - master
  pull_request:
    branches:
      - main
      - master

jobs:
  build:
    name: Build and Test
    runs-on: ubuntu-latest
    steps:
      - checkout repository
      - set up JDK 17
      - cache Maven dependencies
      - run mvn -B -V clean test package
      - verify target/astraNotes-0.1.0.jar exists
```

### Blocking Checks

These should block merge into `main`:

- Java 17 compile
- stable JUnit tests
- SQLite storage integration tests
- multi-user authorization tests
- Spring Boot package build
- packaged JAR existence check

### Advisory Checks

These should produce evidence but not block merge yet:

- search benchmark timing
- dependency/security review
- AI-generated artifact review notes
- full browser automation until it is stable

### Checks To Add Next

These should become blocking after controller tests are added:

- logged-out `/notes` redirects to `/login`
- logged-in user can access notes workspace
- shared user cannot access owner edit route
- logout invalidates session

## Local CI Simulation

I ran the local build and tests in the same order the current CI workflow would run them.

Command used:

```bash
mvn -B -V clean test package
```

Result:

- Build result: pass
- Java runtime used locally: Java 17
- Package result: Spring Boot JAR produced at `target/astraNotes-0.1.0.jar`
- Test areas: model, storage, integration, search, validation, and multi-user authorization

Observation:

- SQLCipher is not installed in the local/default CI path, so the app logs a SQLCipher fallback warning and continues with application-level AES/GCM encryption.
- Search benchmark timing is useful but should remain advisory until thresholds are stable.

## Operational Risk Note

**Risk:**
A change could accidentally allow a shared user to edit or delete a note owned by another user.

**Why it matters:**
This would break the core privacy promise of a multi-user note-taking app and would make the final demo unsafe to defend.

**Control:**
Run `mvn -B -V clean test package` automatically on pull requests and pushes to `main`. Treat compile, stable tests, integration tests, package artifact verification, and multi-user authorization tests as blocking gates. Keep noisy performance and browser checks advisory until they are stable.

**Owner:**
The student developer responsible for merging PRs and preparing the final demo.

## AI Prompt and Human Refinement Note

Prompt used:

```text
Critique this AstraNotes CI/CD workflow. Identify missing checks, noisy gates, over-automation risk, and anything that should be advisory instead of blocking.
```

Useful AI suggestion:

- Separate stable checks from noisy checks. Compile, unit tests, integration tests, package verification, and authorization checks should block PRs, but benchmark timing should be advisory.

Suggestion changed or rejected:

- AI suggested adding broad browser automation immediately. I changed this because the current controller/authorization tests are more stable and higher signal for the immediate class demo.

Final human decision:

- The defensible Week 10.1 pipeline automates Maven build/test/package now, blocks on multi-user authorization tests, collects benchmark and dependency review as advisory evidence, and adds browser automation only after it is stable.

## Summary

The Week 10.1 CI/CD plan reduces the most realistic current AstraNotes risk: a broken or unsafe multi-user web demo caused by manual-only validation. The workflow is matched to the current maturity of the project: Java 17 Spring Boot web app, SQLite storage, encryption/HMAC, user ownership, sharing, and automated tests for core permission behavior.
