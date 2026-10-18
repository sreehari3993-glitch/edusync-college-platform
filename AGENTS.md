# Project Agent Rules

## Git Rules
- **Do NOT push to GitHub after each change.** Only push to GitHub when explicitly instructed by the user.

## AI Testing Policy
- **For AI updates, test conditions ONLY when the user explicitly requests it.**
- Do NOT automatically run live LLM / RAG test invocations or wait on token generation after making code changes, as this consumes significant time and CPU resources.
