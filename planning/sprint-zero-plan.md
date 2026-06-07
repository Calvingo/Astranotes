# Sprint Zero Plan

## Goal
Prepare AstraNotes for implementation by validating architecture, establishing governance, and building the initial planning artifacts.

## Objectives
- Confirm SQLite local-first architecture and encryption approach
- Establish working agreement, DoD, and backlog structure
- Create the first planning artifacts and traceability documents
- Validate AI collaboration process and prompt library structure

## Project setup and repo structure
- Keep planning artifacts in `planning/`:
  - `planning/requirements.md`
  - `planning/user-stories.md`
  - `planning/backlog.md`
  - `planning/sprint-zero-plan.md`
- Keep governance and workflow artifacts in `plans/`:
  - `plans/WORKING-AGREEMENT.md`
  - `plans/DECISIONS.md`
  - `plans/plan-astraNotes.prompt.md`
- Keep reusable AI prompt patterns in `prompts/library.md`.
- Use Maven and Java 17 as the baseline build structure.

## Core planning artifacts
1. Review and finalize requirements and user stories
2. Create initial backlog and prioritize core stories
3. Establish planning folder and baseline documents
4. Define the first `WORKING-AGREEMENT.md` and DoD in `plans/`
5. Document AI workflow and prompt library conventions

## Initial technical decisions
- Use SQLite as the local source of truth.
- Use AES-GCM for note body encryption in the current implementation.
- Keep SQLCipher as an optional hardening path if whole-database encryption is required later.
- Use HMAC or equivalent integrity checks for stored note content.
- Use FTS5 for search once CRUD is stable.

## Risk reduction items
- Confirm the database schema supports create/read/update/delete before UI work expands.
- Test encryption and HMAC behavior early so security risks do not appear late.
- Keep search performance measurable with a benchmark once FTS5 is implemented.
- Track the SQLCipher versus application-level encryption decision in `plans/DECISIONS.md`.
- Avoid using real note content in AI prompts; use synthetic examples only.

## Quality and workflow setup
- Apply the Week 2.1 Definition of Done to each story and backlog item.
- Require each implementation task to map back to a requirement and user story.
- Run Maven tests before marking implementation work complete.
- Record AI prompts, accepted outputs, and rejected/gap findings in the prompt library or decision log.
- Update traceability documents when a requirement changes.

## Deliverables
- `planning/requirements.md`
- `planning/user-stories.md`
- `planning/backlog.md`
- `planning/sprint-zero-plan.md`
- AI reflection section in this file

## Timeline
- Day 1: Finalize requirements and user stories
- Day 2: Prioritize backlog and define sprint structure
- Day 3: Confirm governance docs and set up planning folder
- Day 4: Validate AI workflow and review artifacts
- Day 5: Prepare for Sprint 1 implementation

## Sprint Zero exit criteria
- The planning folder contains requirements, user stories, backlog, and Sprint Zero plan.
- Each selected story has specific acceptance criteria.
- The backlog uses high, medium, and low priority labels.
- At least one priority decision and major dependency is documented.
- The Working Agreement and Definition of Done can be used to review the planning artifacts.

## AI reflection
AI helped accelerate artifact creation by generating an initial governance framework, refining requirements into actionable stories, and suggesting traceability structure. During the process, I refined the plan to make it more concrete, rejected generic Agile descriptions, and changed abstract Spotify wording into operational steps for AstraNotes. I also rejected any AI output that did not map directly to the project's FR/NFR/SEC requirements or lacked measurable acceptance criteria.
