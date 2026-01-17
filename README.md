# Agenda Mobile (Android)

Aplicativo Android nativo para criacao e consulta das agendas da reuniao sacramental. O app e offline-first, escrito em Kotlin com Jetpack Compose, Hilt, Room, DataStore e sincronizacao opcional via Google Drive/Storage Access Framework.

## Arquitetura modular
- `android/app`: container principal. Exibe o `AgendaApp`, configura Hilt, Navigation Compose e agrega os demais modulos.
- `core:data`: modelos serializaveis (agendas, hinos, backup) e contratos de repositario.
- `core:database`: implementacao Room + DataStore (`MeetingDao`, `OfflineMeetingRepository`, `AssetHymnRepository`, `BackupSettingsDataStore`, adapters JSON).
- `core:drive`: integracoes com DocumentFile/SAF (`DriveDocumentBackupCoordinator`, `DriveFolderSettingsDataStore`).
- `core:ui`: tema Compose compartilhado (cores, tipografia, `AgendaTheme`).
- `feature:meetings`: lista e editor de agendas (`MeetingsRoute`, `MeetingEditorRoute`, regras de hinos, gerador de PDF).
- `feature:backup`: tela dedicada a backup/restore local e sincronizacao manual com Drive.

Cada modulo possui `src/main` e `src/test`. Os testes unitarios rodam via Gradle (`./gradlew :core:database:test`, `./gradlew :feature:meetings:test`).

## Recursos principais
- **Lista e edicao Compose**: `MeetingsRoute` consome o Room via `MeetingsViewModel`. O editor (Compose) valida data/titulo e consome `HymnRepository` para dropdowns.
- **Regras de hinos**: `MeetingHymnPlacement` posiciona automaticamente o hino intermediario (0 oradores = testemunhos; 2 = entre ambos; 3+ = apos o segundo). O `hymns.json` em `android/app/src/main/assets` abastece os seletores.
- **PDF instantaneo**: `MeetingPdfGenerator` monta um PDF completo com sessoes da reuniao, salva em cache e compartilha via `FileProvider`.
- **Backup local**: a tela de backup exporta/ importa `.zip` contendo `atas.db`, `atas.db-wal` e `atas.db-shm` para qualquer pasta escolhida via SAF.
- **Backup no Drive**: ao conectar uma pasta (URI persistido pelo `DriveFolderSettingsDataStore`), o `DriveDocumentBackupCoordinator` compacta o banco e faz upload. O restore busca o `.zip` mais recente.

## Requisitos
- Android Studio Jellyfish/Koala (ou CLI) com JDK 17.
- Android SDK 35 + Build-Tools 35.0.0 configurados em `local.properties`.
- Emulador ou dispositivo fisico com depuracao USB/Wi-Fi e `adb`.

## Configuracao rapida
1. Instale o SDK 35 e abra `android/` no Android Studio.
2. Sincronize o Gradle e confirme o `local.properties`.
3. Configure `JAVA_HOME` (JDK 25). Exemplo: `C:\Users\Arthur Stumpf\.jdks\temurin-25.0.1`.
4. Adicione `android/app/google-services.json` (Firebase) se for usar sincronizacao.
5. Opcional: revise `android/app/src/main/assets/hymns.json` caso adicione novos hinos.

## Executando
### Android Studio
1. Escolha um device/emulador.
2. Rode a configuracao `app`.
3. Use o botao de backup na AppBar para validar exportacao/importacao.

### CLI
```powershell
cd android
.\gradlew :app:assembleDebug
.\gradlew :app:installDebug
```
Para direcionar o deploy use `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`.

## Testes e verificacoes
- Persistencia/parse de hinos:
  ```powershell
  cd android
  .\gradlew :core:database:test
  ```
- Logica da UI de agendas (regras de hinos, PDF, validacoes):
  ```powershell
  cd android
  .\gradlew :feature:meetings:test
  ```
- Lint:
  ```powershell
  cd android
  .\gradlew lintDebug
  ```
Execute os testes sempre que alterar regras de negocio. Para alteracoes em backup, valide tambem via dispositivo e anexe prints/logs ao PR.

## Pastas importantes
- `android/app/src/main/assets/hymns.json`: fonte dos hinos.
- `android/app/src/main/res/xml/filepaths.xml`: configuracao do `FileProvider` para compartilhar PDFs.
- `android/core/database/src/main/java/.../datastore`: preferencias locais (datas de backup, ultima pasta do Drive).
- `android/feature/backup/src/main/java/...`: ViewModel e UI do fluxo de backup.

## Proximos passos sugeridos
1. Migrar o `DriveDocumentBackupCoordinator` para a API oficial do Drive (`appDataFolder`).
2. Adicionar testes instrumentados para criacao/edicao de agendas, exportacao de PDF e backup.
3. Documentar pipeline de distribuicao (Play Store/Test Lab) e automatizar builds de release.
