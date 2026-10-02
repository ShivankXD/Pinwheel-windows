# Working rules

Read docs/Pinwheel-Windows-Build-Brief.md before editing. The mobile repository
Read docs/OWNER-DECISIONS.md for owner amendments, including the Windows target,
debug-only entitlement control and read-only D:/Pinwheel-Windows-refs inputs.
at D:\Nativeoffice-photo&videoeditor is read-only. Never build or change it.
Baseline mobile status is exactly `?? output/`. Check with GIT_OPTIONAL_LOCKS=0
before work and at handoff. Keep all Windows work in this repository.

Work in phase order, with no beyond-mobile implementation before owner P8 approval.
Do not claim parity without the brief's required evidence. Preserve stable IDs and JSON keys.
Commit as the owner's configured identity. Commit messages must contain no em dash,
AI co-author trailer, or generated-by attribution. Push every commit to origin.
Never rewrite published history to change author metadata.
