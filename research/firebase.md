# Firebase Sync Design

## Goals
- Permitir colaboracao em agendas entre membros do bispado.
- Restringir acesso por unidade (ward) com senha master.
- Manter offline-first com sincronizacao eventual.
- Registrar auditoria basica (quem/ quando alterou).

## Auth
- Firebase Auth anonimo para identificar o dispositivo.
- UID anonimo vira membro depois de validar a senha master.

## Collections (Firestore)
```
wards/{wardId}
  name: string
  createdAt: timestamp
  masterPasswordHash: string
  masterPasswordSalt: string

wards/{wardId}/members/{uid}
  role: "admin" | "editor" | "viewer"
  displayName: string
  joinedAt: timestamp
  lastActiveAt: timestamp

wards/{wardId}/agendas/{agendaId}
  title: string
  date: string (ISO-8601)
  data: map (estrutura da agenda)
  version: number
  updatedAt: timestamp
  updatedBy: uid
  status: "draft" | "final"

wards/{wardId}/agendas/{agendaId}/edits/{editId}
  changedAt: timestamp
  changedBy: uid
  summary: string
  patch: map (opcional, campo a campo)
```

## Master Password Flow
1) App coleta senha master.
2) Chama Cloud Function `joinWard(wardCode, password)`.
3) Function valida hash+salt e cria `members/{uid}` com role.
4) Retorna `wardId` e `role` para o cliente.

## Conflict Strategy (baseline)
- `version` incrementa a cada update.
- Cliente envia `expectedVersion`.
- Cloud Function ou regra rejeita update com versao diferente.
- App exibe conflito e permite:
  - Recarregar remoto.
  - Forcar overwrite (admin).
  - Mesclar manual (futuro).

## Security Rules (sketch)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /wards/{wardId} {
      allow read: if isMember(wardId);
      allow write: if false;

      match /members/{uid} {
        allow read: if isMember(wardId);
        allow write: if request.auth.uid == uid;
      }

      match /agendas/{agendaId} {
        allow read: if isMember(wardId);
        allow create, update: if canEdit(wardId) && isValidVersion();
        allow delete: if isAdmin(wardId);
      }

      match /agendas/{agendaId}/edits/{editId} {
        allow read: if isMember(wardId);
        allow create: if canEdit(wardId);
      }
    }
  }
}
```

## Required Cloud Functions
- `joinWard(wardCode, password)`: valida senha master e cria membership.
- `updateAgenda(wardId, agendaId, payload, expectedVersion)`: valida versao e atualiza atomico.
- `createWard(name, password)`: cria unidade e grava hash/salt (admin tool).

## Data Migration
- `wardId` pode ser slug ou UUID.
- Para migrar dados locais, criar agenda no Firestore com `version=1`.

## Risks
- Offline edits concorrentes sem merge avancado.
- Exposicao de dados se regras estiverem abertas.
- Custos com writes frequentes (sincronizacao automatica).
