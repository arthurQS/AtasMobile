# INTEGRATIONS

## Android
- Storage Access Framework (SAF)
- DocumentFile

## Google Drive (optional)
- DriveDocumentBackupCoordinator (current implementation)

## Firebase (planned)
- Sync colaborativo usando senha master do bispado
- Firebase Auth (anonimo) para identificar dispositivos
- Cloud Functions para validar senha master e criar membership
- Firestore para agendas e auditoria
 - Senha master armazenada como hash + salt (nao em texto puro)
- Design detalhado em `research/firebase.md`

