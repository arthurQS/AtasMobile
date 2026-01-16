# ARCHITECTURE

## Overview
- Modular Android app under `android/`.
- Offline-first com persistencia local (Room/DataStore).

## Layers
- app
- core:data
- core:database
- core:drive
- core:ui
- feature:meetings
- feature:backup

## Data flow
- UI (Compose) -> ViewModel -> Repositorio offline -> Room/DataStore.
- Export/Import -> SAF/DocumentFile -> Zip com banco local.
- Planejado: Firebase para sincronizacao colaborativa com senha master.

## Firebase (proposto)
- **Auth**: login anonimo + vinculo a uma unidade (bispado).
- **Master password**: entrada validada via Cloud Function; gera membership em `wards/{wardId}/members/{uid}`; senha armazenada como hash.
- **Firestore**: agendas em `wards/{wardId}/agendas/{agendaId}` com `updatedAt`, `updatedBy`, `version`.
- **Auditoria**: subcolecao `edits` ou campos de trilha (ultimo editor + timestamp).
- **Conflitos**: detectar por `version` e exibir status; merge simples (last-write-wins) como baseline.

## Key decisions
- Offline-first para uso sem internet.
- Separacao por modulos para isolar dependencias.

