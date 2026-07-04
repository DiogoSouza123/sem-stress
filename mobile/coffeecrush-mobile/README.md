# Coffee Crush Mobile (Kotlin)

Versao mobile do Coffee Crush criada em Kotlin com foco em visual moderno e arquitetura de baixo risco para evolucao.

## Frameworks escolhidos
- Kotlin
- Android + Jetpack Compose
- Material 3

## Planejamento cauteloso (roadmap)

### Fase 1 - Fundacao segura (concluida)
- Criar app mobile em pasta isolada (`mobile/coffeecrush-mobile`)
- Nao tocar no codigo Swing existente
- Estruturar projeto para evolucao incremental

### Fase 2 - Porta do core (concluida)
- Portar logica de tabuleiro para Kotlin
- Portar regras de match, pontuacao, cascata e shuffle
- Manter comportamento funcional equivalente ao desktop

### Fase 3 - UX e identidade visual (concluida na V1)
- UI com Compose + Material 3
- Menu de fases com cards e status (liberada/bloqueada/concluida)
- Tela de jogo responsiva com tabuleiro dinamico

### Fase 4 - Progressao e dados (concluida na V1)
- Persistencia de progresso em `SharedPreferences`
- Desbloqueio da proxima fase ao vencer
- Registro de melhor pontuacao por fase

### Fase 5 - Evolucao recomendada (proximos passos)
- Migrar persistencia para DataStore
- Ler fases de JSON remoto/local em vez de hardcoded
- Integrar trilha sonora por fase com ExoPlayer
- Adicionar animacoes avancadas (Lottie/Transitions)
- Telemetria de eventos e balanceamento de dificuldade

## Funcionalidades da V1 mobile
- Menu de fases bonito e funcional
- Tabuleiro match-3 jogavel
- Pontuacao, meta e movimentos
- Troca por toque e por arraste (drag swap)
- Vitoria/derrota com dialog
- Shuffle automatico quando nao houver movimentos
- Progresso salvo entre sessoes

## Fases dinamicas por arquivo (sem hardcode)

O app carrega fases via arquivos `.properties`:

- `app/src/main/assets/config/configuracao-jogo.properties` (base)
- `app/src/main/assets/config/fases.properties` (catalogo de fases)

Se existir override no dispositivo, ele tem prioridade:

- `files/config/configuracao-jogo.properties`
- `files/config/fases.properties`
- `externalFilesDir/config/configuracao-jogo.properties`
- `externalFilesDir/config/fases.properties`

Assim, voce pode evoluir e balancear fases sem mexer no codigo fonte.

## Estrutura principal
- `app/src/main/java/com/semstress/mobile/domain/` modelos
- `app/src/main/java/com/semstress/mobile/engine/` motor match-3
- `app/src/main/java/com/semstress/mobile/data/` repositorios
- `app/src/main/java/com/semstress/mobile/ui/` telas e estado

## Como abrir e rodar

### Requisitos
- Android Studio (Koala ou superior recomendado)
- SDK Android 34
- JDK 17 (o Android Studio ja inclui)

### Passos
1. Abra o Android Studio
2. Selecione `Open`
3. Escolha a pasta `mobile/coffeecrush-mobile`
4. Aguarde o Gradle Sync
5. Rode no emulador/dispositivo Android

## Observacao importante
Esta versao foi criada para ser base solida de evolucao mobile. O desktop Java Swing continua como referencia funcional oficial e nao foi removido.
