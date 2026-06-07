# AstraNotes Prompt Library

This file records reusable AI prompts and the quality gates used before accepting AI-assisted output.

## Prompt: Refine AstraNotes Requirement Baseline

**Goal**: Convert high-level project goals into functional, non-functional, security, and governance requirements.

**Usage**: Use during planning or when a lab asks for requirement refinement.

**Input**:
- Current AstraNotes project description
- Existing requirements or user stories
- Any professor-provided lab constraints

**Expected output**:
- Requirements grouped by category
- Acceptance criteria or validation notes
- Gaps, risks, and assumptions

**Quality gates**:
- Must be specific to AstraNotes, not a generic Agile summary
- Must separate feature requirements from security/privacy/governance requirements
- Must identify contradictions with current implementation
- Must be reviewed by the student before submission

**Revisions**:
- 2026-06-06: Added as the initial reusable prompt pattern for Lab 2.1 governance.

## Prompt: Review AstraNotes Code Against Definition of Done

**Goal**: Check whether a feature or artifact is ready to move forward under the project DoD.

**Usage**: Use before marking backlog items done or preparing lab submissions.

**Input**:
- The relevant lab instructions
- Files changed or artifact being reviewed
- Related requirement IDs from the backlog or requirements baseline

**Expected output**:
- What is complete
- What is missing
- Concrete fix plan
- Whether the item is GO, HOLD, or NO-GO

**Quality gates**:
- Must cite specific files or requirements
- Must include testing or validation evidence where appropriate
- Must flag security, privacy, and governance gaps
- Must not accept AI output without student review

**Revisions**:
- 2026-06-06: Added for recurring lab-by-lab review workflow.
