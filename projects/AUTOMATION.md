# 99-Day Build Automation Rules

Start at the first row marked NEXT in `projects/PROJECTS.md`.

For each run:
- Build only one project.
- Keep the implementation small enough to understand and test.
- Prefer standard libraries or well-maintained dependencies.
- Never hard-code secrets, API keys, passwords, or private data.
- Add a README with problem, scope, setup, usage, examples, limitations, and license.
- Add automated tests for core behavior.
- Add GitHub Actions CI appropriate to the language.
- Run the tests available in the execution environment.
- Update `projects/PROJECTS.md`: current row DONE, following row NEXT.
- Add a dated release note under `projects/releases/`.
- Commit all changes with a descriptive conventional-style message.
- Do not claim a project is complete when tests fail or when publishing failed.

Repository constraint:
The connected GitHub tool currently exposes file/branch/commit operations but not repository creation. Until repository creation is exposed, isolate each daily project under `projects/NN-slug/` in the public PRO repository. Never create a fake repository URL.
