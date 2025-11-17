# Repository Guidelines

## Project Structure & Module Organization
- Place production code in `src/`, tests in `tests/`, assets in `assets/`, and scripts in `scripts/`. Keep configuration in `config/` and documentation in `docs/`.
- Example layout:
  - `src/` (application modules)
  - `tests/` (mirrors `src/` folders; one test file per module)
  - `assets/` (images, styles, static files)
  - `scripts/` (dev/build helpers)
  - `config/` (env, build/test configs)

## Build, Test, and Development Commands
- Install deps: `npm ci` (Node) | `pip install -r requirements.txt` (Python) | `dotnet restore` (.NET).
- Run locally: `npm run dev` | `python -m src` | `dotnet run`.
- Build: `npm run build` | `python -m build` | `dotnet build`.
- Tests: `npm test` (Jest/Vitest) | `pytest` | `dotnet test`.
- Use `scripts/` helpers when present (e.g., `pwsh scripts/setup.ps1`). Prefer cross‑platform commands.

## Coding Style & Naming Conventions
- Indentation: 2 spaces (JS/TS), 4 spaces (Python), editorconfig respected when present.
- Naming: files and folders kebab‑case (`meeting-minutes/`), classes PascalCase, functions/variables camelCase, constants SCREAMING_SNAKE_CASE.
- Formatting/Linting: Prettier + ESLint (Node), Black + isort + Ruff (Python). Run before pushing: `npm run lint && npm run format` or `ruff check . && black .`.

## Testing Guidelines
- Co‑locate tests under `tests/` mirroring `src/` paths.
- Name tests `*.spec.ts`/`*.test.ts` (Node), `test_*.py` (Python), or `*.Tests.cs` (.NET).
- Aim for meaningful coverage on core modules; add edge‑case tests for date/time, i18n, and file I/O.

## Commit & Pull Request Guidelines
- Use Conventional Commits (e.g., `feat: add agenda export` / `fix: correct pagination` / `chore: update deps`).
- PRs must include: concise description, linked issue (if any), screenshots/CLI output for UX/CLI changes, and notes on tests added/updated.
- Keep PRs focused and small; avoid drive‑by refactors.

## Security & Configuration Tips
- Never commit secrets. Store env in `.env` and document in `.env.example`.
- Prefer parameterized configs in `config/`; validate inputs at boundaries.

## Agent‑Specific Instructions
- Follow this guide for any edits. Keep changes minimal, align with existing patterns, and update tests/docs when touching behavior.
