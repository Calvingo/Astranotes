# AstraNotes Final Documentation Index

This index organizes the AstraNotes repository for the Week 11 final project checklist. The final product direction is a Spring Boot web demo with login, per-user ownership, read-only sharing, SQLite persistence, encryption support, and test evidence.

## Final Demo And Setup

| Artifact | Purpose |
| --- | --- |
| [README.md](README.md) | Final project overview, demo users, run commands, implemented features, and known limits. |
| [BUILD_AND_RUN.md](BUILD_AND_RUN.md) | Final web quick start plus retained historical Week 6 desktop build notes. |
| [SUBMISSION_WEEK10_2.md](SUBMISSION_WEEK10_2.md) | Final demo readiness notes and class demo workflow. |

## Requirements And Planning

| Artifact | Purpose |
| --- | --- |
| [planning/requirements.md](planning/requirements.md) | Initial requirements baseline. |
| [planning/refined-requirement-baseline.md](planning/refined-requirement-baseline.md) | Refined functional and non-functional requirements with ambiguity review. |
| [planning/user-stories.md](planning/user-stories.md) | User stories and acceptance criteria. |
| [planning/backlog.md](planning/backlog.md) | Prioritized early backlog and sequencing notes. |
| [planning/sprint-zero-plan.md](planning/sprint-zero-plan.md) | Sprint Zero setup, risk reduction, and workflow preparation. |

## Governance, AI Use, And Ethics

| Artifact | Purpose |
| --- | --- |
| [plans/WORKING-AGREEMENT.md](plans/WORKING-AGREEMENT.md) | Working Agreement and Definition of Done for AI-native project work. |
| [plans/DECISIONS.md](plans/DECISIONS.md) | Architecture and governance decision log. |
| [prompts/library.md](prompts/library.md) | Prompt patterns and AI collaboration evidence. |
| [plans/privacy-governance-review.md](plans/privacy-governance-review.md) | Privacy, PII, AI leakage, licensing, and governance memo. |

## Architecture And UML

| Artifact | Purpose |
| --- | --- |
| [planning/uml-design-package.md](planning/uml-design-package.md) | UML package with class, object, use case, activity, deployment, and final Web MVC addendum. |
| [plans/week7-1-architecture-quality-mvc.md](plans/week7-1-architecture-quality-mvc.md) | MVC architecture quality review and Web MVC mapping. |

## Traceability And Validation

| Artifact | Purpose |
| --- | --- |
| [planning/traceability-matrix.md](planning/traceability-matrix.md) | Requirements-to-UML traceability matrix, metrics, and gap analysis. |
| [WEEK7_TESTING_STRATEGY.md](WEEK7_TESTING_STRATEGY.md) | Testing strategy and first test set. |
| [planning/Week9_AstraNotes_Test_Improvement_Log.md](planning/Week9_AstraNotes_Test_Improvement_Log.md) | Test improvement log for validation, search, authorization, mocking, and coverage gaps. |
| [plans/ci-pipeline-outline.md](plans/ci-pipeline-outline.md) | CI pipeline outline and quality gate plan. |

## Source Code Evidence

| Area | Evidence |
| --- | --- |
| Web app entry point | `src/main/java/com/astraNotes/web/AstraNotesWebApplication.java` |
| Controllers | `src/main/java/com/astraNotes/web/controller/` |
| Services and DTOs | `src/main/java/com/astraNotes/web/service/`, `src/main/java/com/astraNotes/web/dto/` |
| Templates and CSS | `src/main/resources/templates/`, `src/main/resources/static/css/app.css` |
| Storage and security | `src/main/java/com/astraNotes/storage/SQLiteNoteStorage.java`, `src/main/java/com/astraNotes/encryption/` |
| Web tests | `src/test/java/com/astraNotes/web/service/NoteServiceTest.java`, `src/test/java/com/astraNotes/web/controller/NoteWebControllerTest.java` |

## Historical Weekly Artifacts

These files are retained because they show project evolution across the quarter. Some early files describe the Swing prototype or earlier gaps that were addressed later.

| Artifact | Purpose |
| --- | --- |
| [WEEK6_QUICKSTART.md](WEEK6_QUICKSTART.md) | Week 6.1 quick start for the original prototype. |
| [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) | Week 6.1 development submission. |
| [SUBMISSION_WEEK6_2.md](SUBMISSION_WEEK6_2.md) | Week 6.2 quality audit and decision to move to web multi-user scope. |
| [SUBMISSION_WEEK8.md](SUBMISSION_WEEK8.md) | Later planning and implementation evidence. |
| [DELIVERABLES.md](DELIVERABLES.md) | Week 6 deliverable checklist retained as historical evidence. |

## Final Checklist Coverage

| Week 11 expectation | Repository evidence |
| --- | --- |
| README with overview and setup | [README.md](README.md), [BUILD_AND_RUN.md](BUILD_AND_RUN.md) |
| Requirements and planning artifacts | `planning/requirements.md`, `planning/refined-requirement-baseline.md`, user stories, backlog, Sprint Zero plan |
| Architecture and UML artifacts | `planning/uml-design-package.md`, `plans/week7-1-architecture-quality-mvc.md` |
| Traceability and validation artifacts | `planning/traceability-matrix.md`, testing strategy, Week 9 test improvement log |
| Source code or prototype materials | `src/main/java/com/astraNotes/` and `src/main/resources/` |
| Testing strategy and test artifacts | `src/test/java/com/astraNotes/`, `WEEK7_TESTING_STRATEGY.md`, `planning/Week9_AstraNotes_Test_Improvement_Log.md` |
| Security, deployment, maintenance notes | privacy governance review, CI pipeline outline, README known limits |
