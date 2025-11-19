# Repository Guidelines

## Estrutura e modulos
- Todo o aplicativo Android mora em `android/` e usa Gradle Kotlin DSL. Mantenha codigo de producao em submodulos (`app`, `core:*`, `feature:*`) e recursos (assets, manifests) dentro de `src/main`.
- `app` orquestra navegacao Compose, Hilt e as dependencias entre modulos. `core:data` define modelos/contratos, `core:database` implementa Room/DataStore e repositorios offline, `core:drive` encapsula integracoes com DocumentFile/Google Drive e `core:ui` concentra o tema Compose. `feature:meetings` e `feature:backup` expõem UI + view models especificos.
- Tests unitarios ficam em `src/test/java` no mesmo modulo da classe testada. Espelhe os pacotes e prefira arquivos `*Test.kt`.
- Arquivos estaticos (`hymns.json`, XMLs, drawables) residem em `android/app/src/main/assets|res`. Evite duplicar recursos em features; sirva-os via repositorios no modulo `core`.

## Fluxo de desenvolvimento
- Use Android Studio Koala/Jellyfish ou a CLI com o Gradle Wrapper (`./gradlew` ou `.\gradlew`). Nao instale plugins globais.
- Configuracao minima: JDK 17, SDK 35 e `local.properties` apontando para o SDK. Execute builds/tests sempre a partir de `android/`.
- Rode `./gradlew :app:assembleDebug` para validar builds e `./gradlew :app:installDebug` para publicar no device/emulador selecionado.
- Live Edit/Preview do Compose so para prototipar; confirme qualquer mudanca executando o app completo.

## Estilo de codigo e qualidade
- Siga o estilo oficial do Kotlin (indentacao de 4 espacos, imports ordenados). Formate com Android Studio (`Code > Reformat`) antes do commit.
- Componentes Compose devem ser pequenos, estaveis e livres de logica de negocio; mantenha regras em view models/repositorios. Exponha `StateFlow<T>` e consuma via `collectAsStateWithLifecycle`.
- Os modulos `core` sao livres de dependencias de UI; compartilhe modelos via `core:data` e injete dependencias com modules Hilt dedicados.
- Use camelCase para variaveis/funcoes, PascalCase para classes e kebab-case para pastas Gradle. Strings de UI permanecem em portugues.

## Build, lint e testes
- Antes de abrir PR execute:
  - `./gradlew lintDebug`
  - `./gradlew :core:database:test`
  - `./gradlew :feature:meetings:test`
- Toda nova regra (validacao de formulario, posicionamento de hino, backup/restore) precisa de teste no modulo correspondente.
- Alterou sincronizacao/backup? Valide manualmente na tela de backup e inclua prints/logs no PR.

## Documentacao e rastreabilidade
- Atualize `README.md` ao alterar arquitetura, comandos ou fluxos de usuario. Use `docs/` para guias longos quando necessario.
- Commits seguem Conventional Commits (`feat:`, `fix:`, `chore:`, ...). PRs precisam de descricao objetiva, link para issue (se houver), evidencias visuais/CLI e checklist dos testes executados.

## Seguranca e configuracao
- Nunca versione chaves ou senhas. Caminhos de SDK/keystore ficam em `local.properties` ou variaveis de ambiente. Dados sensiveis de Drive/DataStore permanecem somente no dispositivo.
- Parametrize caminhos/IDs externos via configuracao e valide entrada de Intents/URIs antes de tocar o sistema de arquivos.

## Instrucoes finais
- Priorize entregas incrementais, siga os padroes do modulo tocado e sempre adicione testes e documentacao quando alterar comportamento observavel.
