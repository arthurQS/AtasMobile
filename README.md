# Atas Mobile (Android)

Aplicativo Android nativo para criação e consulta de atas da reunião sacramental. O projeto foi reestruturado para seguir uma arquitetura modular em Kotlin com Jetpack Compose, Hilt, Room e sincronização opcional via Google Drive.

## Estrutura do monorepo
- `android/app` — container principal (Navigation Compose, Hilt, WorkManager).
- `core:data` — modelos serializáveis e contratos de repositório.
- `core:database` — Room, DataStore e repositórios locais (inclui `AssetHymnRepository`).
- `core:drive` — camadas para backup/restauração no Google Drive.
- `core:ui` — tema, paleta e componentes Compose reutilizáveis.
- `feature:meetings` — lista e formulário completo de atas (inclusive regras dos hinos).
- `feature:backup` — fluxo de configuração dos backups.

## Requisitos funcionais destacados
- O banco de dados é local (Room) e pode exportar/restaurar via Google Drive.
- O formulário replica o sistema web antigo, com dropdown de hinos preenchido por `hymns.json` (`android/app/src/main/assets`).
- O hino intermediário segue regras específicas: com 2 oradores ele fica entre ambos; com 3 ou mais, entre o 2º e o 3º; sem oradores o formulário assume uma “Reunião de Testemunhos”.
- Cada ata pode ser exportada para PDF diretamente do editor (ícone de PDF no AppBar); o arquivo é gerado em cache e exposto via FileProvider para compartilhamento.
- A tela de backup permite salvar/restaurar arquivos `.zip` contendo a base (`atas.db`, `-wal`, `-shm`) em qualquer local escolhido via Storage Access Framework.

## Pré-requisitos
- Android Studio Jellyfish/Koala (ou CLI) com JDK 17+.
- Android SDK 35 e Build-Tools 35.0.0 instalados em `C:\Users\Arthur\AppData\Local\Android\Sdk`.
- Emulador Android ou dispositivo físico com depuração USB/Wi-Fi habilitada.

## Como executar
1. Abra `android/` no Android Studio e sincronize o Gradle (ou use a CLI).
2. Configure e conecte o dispositivo desejado.
3. Compile e instale:
   ```powershell
   cd android
   .\gradlew :app:assembleDebug
   .\gradlew :app:installDebug
   ```
4. Para enviar manualmente a um device específico:  
   `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`

## Testes
- Parser de hinos e persistência local:
  ```powershell
  cd android
  .\gradlew :core:database:test
  ```
- Regras do formulário (posicionamento do hino intermediário e derivados):
  ```powershell
  cd android
  .\gradlew :feature:meetings:test
  ```

## Próximos passos sugeridos
- Implementar o `DriveBackupCoordinator` com a API oficial do Google Drive (`appDataFolder`).
- Adicionar testes de UI/instrumentados para fluxos críticos (edição, backup e restauração).
- Documentar o fluxo de distribuição (Play Store/Test Lab) e automação de build.
