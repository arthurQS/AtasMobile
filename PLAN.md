# PLAN

<plan>
  <task id="T1">
    <title>Implementar sincronizacao Firebase</title>
    <steps>
      <step>Criar projeto Firebase e registrar o app Android.</step>
      <step>Adicionar dependencias Firebase (Auth, Firestore, Functions) no Gradle.</step>
      <step>Configurar `google-services.json` e plugin `com.google.gms.google-services`.</step>
      <step>Implementar Auth anonimo e persistir UID localmente.</step>
      <step>Implementar fluxo de senha master (UI + chamada `joinWard`).</step>
      <step>Adicionar repositorio Sync no `core:data` e implementacao em `core:drive` ou novo `core:sync`.</step>
      <step>Modelar colecoes Firestore (`wards`, `members`, `agendas`, `edits`).</step>
      <step>Salvar/atualizar agendas remotas com controle de `version`.</step>
      <step>Implementar resolucao de conflito (recarregar/overwrite admin).</step>
      <step>Adicionar status de sincronizacao na UI (conectado, pendente, conflito).</step>
      <step>Registrar auditoria basica (`updatedBy`, `updatedAt`).</step>
      <step>Escrever regras de seguranca do Firestore.</step>
      <step>Criar Cloud Functions: `joinWard`, `updateAgenda`, `createWard`.</step>
      <step>Documentar setup do Firebase e variaveis necessarias.</step>
    </steps>
    <verification>
      <step>Build debug do app.</step>
      <step>Criar agenda offline e sincronizar quando online.</step>
      <step>Testar senha master com UID nao autorizado.</step>
      <step>Testar conflito de versao em dois dispositivos.</step>
      <step>Validar regras do Firestore com emulador.</step>
    </verification>
  </task>
</plan>

