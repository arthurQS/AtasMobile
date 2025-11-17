# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a meeting minutes (atas) management system for LDS/SUD sacrament meetings, built as a full-stack monorepo. The system allows users to create, edit, and manage meeting records through a web interface and store them in a SQLite database. PDF export is handled via browser print functionality.

## Architecture

**Monorepo Structure:**
- `server/` - Express.js REST API backend with SQLite database (better-sqlite3)
- `web/` - Vite + React frontend with React Router
- `data/` - Auto-generated SQLite database storage (`app.db`)

**Key Architectural Patterns:**

1. **Database Layer (`server/db.js`):**
   - Single SQLite database with WAL mode enabled for better concurrency
   - Prepared statements using better-sqlite3 for all CRUD operations
   - Meeting records store structured JSON in the `data` TEXT column
   - Date-based indexing for efficient queries

2. **REST API (`server/index.js`):**
   - RESTful endpoints under `/api/` prefix
   - Serves static frontend from `web/dist` in production
   - CORS enabled for development (localhost:5173)
   - All dates stored and queried as ISO 8601 strings

3. **Frontend Structure:**
   - `App.jsx` - Main router component with navigation
   - `pages/` - Route components (List, Edit, Print, Calendar)
   - `components/` - Reusable components (e.g., HymnSelect)
   - `data/` - Static JSON data (hymns catalog)
   - `api.js` - Centralized fetch wrapper for backend calls

4. **Meeting Data Model:**
   Each meeting has:
   - `id` (auto-increment)
   - `date` (ISO string)
   - `title` (string)
   - `data` (JSON object with meeting details - see `emptyData` in `web/src/pages/Edit.jsx:7-24`)

   The `data` field structure includes: orgao, tipo, ala, preside, dirige, organista, regente, oficiantesSacramento, hinos (object), oracoes (object), anuncios, oradores (array), chamados, desobrigacoes, observacoes.

## Development Commands

**Initial Setup:**
```bash
npm run setup
```
Installs dependencies for both `server/` and `web/` workspaces.

**Development Mode:**
```bash
npm run dev
```
Runs both API server (port 3000) and Vite dev server (port 5173) concurrently.

**Server-only (with watch mode):**
```bash
cd server
npm run dev
```

**Web-only:**
```bash
cd web
npm run dev
```

**Production Build:**
```bash
npm run build
```
Builds the frontend to `web/dist/`.

**Production Start:**
```bash
npm start
```
Starts the server which serves both API and static frontend from `web/dist/`.

**Mock Data Population:**
```bash
cd server
npm run mock
```
Runs `server/scripts/populate-mock.js` to seed database with sample meetings.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/meetings` | List all meetings (ordered by date DESC) |
| GET | `/api/meetings?from=YYYY-MM-DD&to=YYYY-MM-DD` | List meetings in date range |
| GET | `/api/meetings/:id` | Get single meeting by ID |
| GET | `/api/meetings/by-date/:date` | List meetings on specific date (YYYY-MM-DD) |
| POST | `/api/meetings` | Create meeting (body: `{date, title, data}`) |
| PUT | `/api/meetings/:id` | Update meeting (body: `{date, title, data}`) |
| DELETE | `/api/meetings/:id` | Delete meeting |

## Important Conventions

**Date Handling:**
- All dates stored in database as ISO 8601 strings (e.g., `"2025-01-15T12:00:00.000Z"`)
- Frontend date inputs use YYYY-MM-DD format
- Backend converts YYYY-MM-DD to ISO for storage and queries
- Date range queries: upper bound is exclusive (adds 1 day internally)

**Code Style:**
- 2-space indentation
- ES modules (`"type": "module"` in package.json)
- Async/await preferred over promises
- React hooks: useState, useEffect, useMemo, useNavigate, useParams

**File Naming:**
- React components: PascalCase (e.g., `Edit.jsx`, `HymnSelect.jsx`)
- Utilities: camelCase (e.g., `api.js`, `db.js`)
- Pages stored in `web/src/pages/`, components in `web/src/components/`

**Environment Variables:**
- `VITE_API_URL` - Override API endpoint (defaults to `http://localhost:3000/api`)
- `PORT` - Server port (defaults to 3000)

## Adding New Features

**New Meeting Data Fields:**
1. Update `emptyData` object in `web/src/pages/Edit.jsx`
2. Add corresponding form fields in the Edit component
3. Update Print page layout in `web/src/pages/Print.jsx`

**New API Endpoints:**
Add route handlers in `server/index.js`. Database operations go in `server/db.js` as prepared statements exported via the `repo` object.

**New Pages:**
1. Create component in `web/src/pages/`
2. Add route in `web/src/App.jsx`
3. Add navigation link if needed

## Print/PDF Generation

**Browser Print:**
- Navigate to `/print/:id` route
- Use browser's Print dialog → Save as PDF
- Print-specific CSS in `web/src/styles.css` with `@media print` rules

## Database Schema

```sql
CREATE TABLE meetings (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  date TEXT NOT NULL,              -- ISO 8601 datetime
  title TEXT NOT NULL,             -- e.g., "Reunião Sacramental"
  data TEXT NOT NULL,              -- JSON string with meeting details
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX idx_meetings_date ON meetings(date);
```

## Special Notes

- **SQLite Location:** Database file created at `data/app.db` relative to server directory
- **Static Serving:** In production, Express serves frontend from `web/dist` and handles SPA routing (catch-all for non-API routes)
- **CORS:** Configured for localhost:5173 and 127.0.0.1:5173 in development
- **Workspaces:** Root package.json defines npm workspaces for server and web
- **Hymn Data:** Sample LDS hymn catalog in `web/src/data/hymns-sample.json` used by HymnSelect component
- **Field Aliases:** Edit page includes backward compatibility mapping (e.g., `local` → `ala`, `conduz` → `dirige`, `comunicados` → `anuncios`)
