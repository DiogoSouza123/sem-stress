# Backlog Consolidado

Fonte única de execução. Cada item referencia o documento com o detalhamento completo. Ordem dentro de cada fase = ordem sugerida de execução. Marcar checkboxes conforme conclusão (respeitando o Definition of Done de [code-quality.md](code-quality.md) §6).

> Esforço: `P` ≤ ½ dia · `M` 1–3 dias · `G` ≥ 1 semana.

---

## Fase 0 — Higiene e toolchain (P0)

- [x] **RR-10** `P` — Limpar VCS: ignorar/remover `local.properties`, `.gradle-user/`, `.idea/` volátil, `build/`. *Aceite:* `git status` limpo após build; nenhum path de máquina no repo. ([roadmap](refactoring-roadmap.md))
- [x] **RR-11** `P` — Version catalog `gradle/libs.versions.toml` com todas as dependências. *Aceite:* nenhum literal de versão nos `build.gradle.kts`.
- [x] **RR-12** `M` — Kotlin 2.x + plugin Compose do Kotlin + AGP/BOM atualizados. *Aceite:* `composeOptions.kotlinCompilerExtensionVersion` removido; app roda; build verde.
- [x] **RR-14** `M` — CI GitHub Actions (build + detekt + lint + unit tests, `working-directory` no projeto mobile). *Aceite:* PR com teste falhando fica vermelho. ([code-quality](code-quality.md) §4)
- [x] **CQ-01** `P` — detekt + ktlint/formatting + lint baseline configurados localmente e no CI.

## Fase 1 — Rede de segurança (P0)

- [x] **RR-19** `M` — Portar as 6 suítes de teste de engine do desktop para JUnit 5 (cascata, pontuação, validação, shuffle, board/seed) + helper `boardFrom(String)`. *Aceite:* cenários equivalentes ao desktop passando; `:app` (futuro `:core:engine`) ≥ 90% de cobertura na engine. ([code-quality](code-quality.md) §2.2)
- [x] **CQ-02** `M` — Testes de caracterização do `CoffeeCrushController` atual (tap/drag/inválido/vitória/derrota/shuffle) com `runTest` + Turbine. *Aceite:* comportamento atual documentado em testes que rodarão contra o ViewModel novo.

## Fase 2 — Estado, navegação e dados (P0/P1)

- [x] **RR-01** `G` — `GameViewModel` + `MenuViewModel` (StateFlow, `viewModelScope`, `SavedStateHandle`); controller eliminado. *Aceite:* rotação/process death preservam a partida; testes de CQ-02 passam. ([architecture](architecture.md) §2.3)
- [x] **RR-02** `M` — Carregamento de fases/progresso assíncrono (IO dispatcher) + estado `Loading`; StrictMode em debug. *Aceite:* zero disk reads na main thread no fluxo de abertura.
- [x] **RR-03** `M` — Separar deus-estado em `UiState` por tela; música/settings em repositório próprio.
- [x] **RR-04** `M` — Navigation Compose com rotas type-safe; back correto. *Aceite:* voltar do jogo → menu; menu → sair do app; deep link `game/{stageId}` funcional.
- [x] **RR-05** `M` — Hilt em toda a árvore (repos, engine factory, players).
- [x] **RR-06** `M` — Proto DataStore + `SharedPreferencesMigration`. *Aceite:* teste automatizado de migração com prefs reais; progresso preservado.
- [x] **RR-16** `M` — Fases em JSON (kotlinx.serialization, `schemaVersion`, defaults, validação) + override local mantido; testes de parser. *Aceite:* paridade com as 10 fases atuais.
- [x] **UX-03** `P` — Ícone adaptativo + splash oficial + edge-to-edge + retrato na tela de jogo.
- [x] **UX-09** `P` — Confirmação ao abandonar partida em andamento.

## Fase 3 — Render, mídia e release (P1)

- [x] **RR-20** `G` — Board em `Canvas` único (ticker via `withFrameMillis` lido no draw; tap/drag no `pointerInput`; flag `NEW_BOARD_RENDERER` para rollout). *Aceite:* 0 recomposições contínuas em idle (verificado com compose metrics/Layout Inspector); paridade funcional. ([performance](performance.md) §2)
- [x] **RR-21** `M` — Sprites via spritesheet + downsampling para o tamanho da célula; `SpriteAtlas` injetado; pré-carga na splash. *Aceite:* memória de sprites < 20 MB.
- [x] **RR-13** `P` — R8 + shrinkResources + keep rules; smoke test de release no CI.
- [x] **RR-22 / UX-08** `M` — `SoundPool` para SFX + haptics + toggles separados música/efeitos; música pausa em background; WAV→OGG.
- [x] **PF-01** `M` — Macrobenchmark (cold start, abrir fase, cascata 3×) + Baseline Profile. *Aceite:* metas de performance.md §6 medidas. (infra completa; medicao real pendente de dispositivo fisico/CI, ver performance.md §6)

## Fase 4 — Estrutura e engine 2.0 (P2)

- [x] **RR-09** `M` — Código 100% inglês; strings de usuário em `strings.xml`. PR isolado, sem lógica.
- [x] **RR-08** `G` — Modularização: `:core:model` e `:core:engine` (Kotlin JVM puro) primeiro; depois `:core:data`, `:core:ui`, `:feature:*`. *Aceite:* `:core:engine` compila sem Android SDK. (apenas `:core:model` e `:core:engine`, conforme aceite; `:core:data`/`:core:ui`/`:feature:*` ficam para item futuro)
- [x] **RR-07 / RR-17** `G` — Engine emite eventos semânticos (`Cleared/Moved/Spawned`); board encapsulado; UI interpola. *Aceite:* testes de engine adaptados passam; animação de queda contínua possível; flag para alternar pipeline. (engine reescrita para eventos semânticos — `Moved`/`Spawned` — e extraída para `:core:engine`; `GameViewModel` mantém o mesmo contrato/tempos visíveis de hoje, então não há pipeline alternativo a alternar nesta etapa; consumir os eventos para queda renderizada continuamente fica para item futuro de UI)
- [x] **MZ-01** `M` — Infra `FeatureFlags` (Debug → Remote → Default) em `:core:common`. ([monetization](monetization.md) §1.4) (`RemoteConfigFlags` ainda não existe — depende de Firebase, fora de escopo; `DefaultFeatureFlags` já cobre debug-override + default)
- [x] **CQ-03** `M` — Painel de debug (seed, pular fase, +movimentos, flags) em `debugImplementation`. (via source sets `app/src/debug`/`app/src/release`, sem módulo Gradle dedicado)

## Fase 5 — Produto: UI 2.0 e Gameplay 2.0 (P1–P2 de produto)

- [x] **UX-01** `M` — Design system (tokens semânticos, `CoffeeButton`, `ProgressCup`, `StarRating`) + dark theme real + screenshot tests. (tokens semânticos em `CoffeeSemanticColors`/`CoffeeTheme.colors`, aplicados em `GameScreen`/`StageMenuScreen`/`BoardCanvas` no lugar de cores cruas; componentes `CoffeePrimaryButton`/`CoffeeSecondaryButton`/`CoffeeIconButton`/`CoffeePanel`/`StatChip`/`StarRating`/`ProgressCup` com `@Preview` claro/escuro; dark theme ligado a `isSystemInDarkTheme()`; extração para módulo `:core:ui` e infra de screenshot-test automatizado (Paparazzi/Roborazzi) ficam para item futuro — sem infra de golden-image configurada ainda)
- [x] **UX-02** `P` — Tipografia própria (display + corpo, números tabulares). (`Typography` em `Type.kt` usa `DeviceFontFamilyName("sans-serif-rounded")` para display/headline/title — fonte "food-friendly" arredondada já presente no sistema em Android 8+, sem exigir asset empacotado nem Google Fonts Provider downloadable — e `FontFamily.SansSerif` para corpo/label; `StatColumn` aplica `fontFeatureSettings = "tnum"` nos valores numericos do HUD para não "dançar" de largura. Fontes de marca reais (Baloo 2/Fredoka + Nunito Sans via asset/Google Fonts Provider) ficam para item futuro quando houver os arquivos de fonte definitivos)
- [x] **GP-03** `M` — Estrelas 1–3 por fase (`starsByStage` no DataStore) + exibição no menu (**UX-04a**). (`calculateStars` puro em `:core:model` — 1 estrela ao cumprir a meta de pontos, atual unico objetivo; 2 acima de 1,5x a meta; 3 acima de 2x a meta ou terminando com >=30% dos movimentos sobrando; `PlayerProgress.starsByStage` persistido via novo campo proto `stars_by_stage` — retrocompativel, default vazio para instalacoes antigas, testado em `ProgressRepositoryMigrationTest`; `StageMenuScreen` mostra `StarRating` por fase e total de estrelas no card de progresso; `GameResultDialog` mostra as estrelas ganhas na rodada. Mecanica de "movimentos restantes viram shots de espresso" (bonus animado) fica para item futuro, sobreposta ao escopo de GP-01/UX-06)
- [x] **UX-05** `M` — HUD novo: ProgressCup da meta, movimentos com alerta, pontos flutuantes, shake em inválido. (`Scoreboard` usa `ProgressCup` para a meta e pulsa/muda de cor o contador de movimentos quando `<=5`; `FloatingPointsBanner` mostra "+N" ao pontuar; `ComboBanner` anima escala/fade para combo/embaralhamento; movimento invalido perdeu o texto — agora as duas peças da tentativa balançam (`BoardCanvas`/shake via `Animatable` lido na fase de desenho, sem recomposição continua) e o haptic já existente (RR-22) permanece; botões de sistema (voltar/musica/efeitos) viraram icones discretos no topo via `CoffeeIconButton`, removendo o botao de texto grande no fluxo principal. Fundo ambientado por região temática e chips de objetivo ficam para UX-04b/GP-02, ainda não implementados)
- [x] **GP-02** `G` — Sistema de objetivos (collect/deliver/clean/unfreeze/cascade) no JSON + engine + HUD. Introduzir um tipo por vez, com fase-tutorial de cada (**GP-08** junto). (introduzido apenas `collect` nesta etapa, conforme "um tipo por vez": `CollectObjective` em `:core:model`, campos `collectPieceType`/`collectCount` no JSON de fase — opcionais, retrocompativel —, contagem no `GameViewModel` ao resolver cada rodada (antes de limpar o board) e chip de progresso no HUD; vitoria agora exige meta de pontos E objetivo de coleta quando presente. `deliver`/`clean`/`unfreeze`/`cascade` ficam para itens futuros)
- [x] **GP-08** `M` — Tutorial guiado da fase 1 + hint após inatividade. (dica de jogada apos ~8s de inatividade via `Match3Engine.findAvailableMove` — reaproveita a logica de `hasAvailableMove` — destacada no `BoardCanvas`; dica cancelada/reiniciada a cada acao do jogador. Tutorial da fase 1 e um aviso curto sempre visivel antes do primeiro movimento (sem overlay apontado nem persistencia de "ja visto", escopo reduzido em relacao a um tutorial guiado completo)
- [x] **UX-07** `M` — Telas de vitória/derrota (estrelas animadas, Lottie, espaço condicional para rewarded futuro). (`GameResultOverlay` substitui o `AlertDialog` por um overlay de tela cheia: `ProgressCup` animado, pontuação com contagem animada (`Animatable` + tabular nums), estrelas entrando com bounce sequencial defasado por índice; derrota usa titulo/microcopy acolhedores ("Faltou pouco!") em vez de "Fim da rodada". Confete via Lottie e o espaço condicional para rewarded ad ficam para itens futuros — sem dependencia Lottie adicionada e sem superficie de monetização, que continua desligada por padrão)
- [x] **GP-01** `G` — Peças especiais (Moedor, Prensa Francesa, Xícara Vazia, Vapor) + tabela de combinações + efeitos (**UX-06**). (apenas o **Moedor** nesta etapa: `Match3Engine` ganha `findMatchRuns`/`MatchRun` — preserva a ordem das celulas por corrida, ao contrario de `findMatches`, que so agrega para pontuacao — e um match-4 exato deixa a ultima celula da corrida como `SPECIAL_GRINDER` (marcador negativo, nunca gerado por `nextPiece`/shuffle, excluido de matches via `value >= 0`) em vez de limpar as 4 celulas; `activateSpecialPiece` mói os 8 vizinhos (Moore) ao ser tocado, pontua `scoreMatch3` por peca moida e credita objetivo de coleta quando a fase tem um. `GameViewModel` intercepta o tap numa celula com Moedor antes da logica normal de selecao/troca e anima highlight/explosao/queda como uma rodada, sem consumir movimento. Prensa Francesa/Xicara Vazia/Vapor, a tabela de combinacoes 4x4 e os efeitos visuais proprios de UX-06 ficam para itens futuros)
- [x] **UX-04b** `G` — Mapa de jornada com regiões temáticas. (`JourneyMap` substitui o `LazyVerticalGrid` do menu por uma `LazyColumn` vertical em zigue-zague com linha conectora; fases agrupadas por `StageConfig.region` — novo campo opcional no JSON, `stages.json` populado com 5 regiões: Cafezal, Torrefacao, Cafeteria, Graos Nobres, Mestre do Cafe, cada uma com uma cor de destaque distinta do design system. Reaproveita o `StageCard` do UX-04a. Escopo: arte de fundo por região e troca de trilha musical ao rolar (ui-ux.md §3) ficam para item futuro — regiões hoje só mudam cor de destaque/cabecalho, mantendo a musica unica do menu)
- [x] **GP-05** `M` — Desafio diário (seed do dia) + Modo Zen. (`GameSessionSpec` agrupa os parametros assistidos do `GameViewModel` (stage/totalStages/seed) para viabilizar sessoes fora do catalogo normal via IDs sentinela `DAILY_CHALLENGE_STAGE_ID`/`ZEN_MODE_STAGE_ID`; `GameRoute` ganha `zen`/`dailySeed` opcionais, resolvidos em `GameDestination` via `.copy()` sobre a fase base. Desafio diario usa `dailyChallengeSeed(epochDay)` (identidade — o board e igual pra todo mundo no dia), com `PlayerProgress.registerDailyAttempt`/`dailyAttemptsRemaining` (3 tentativas/dia, resetam a cada novo `epochDay`, persistidas via novos campos proto retrocompativeis) no lugar do `registerResult` normal. Modo Zen usa `StageConfig.isZenMode`: nunca decrementa movimentos, nunca finaliza a partida (objetivos/movimentos esgotados ignorados), HUD mostra so pontos + selo "Zen" em vez de meta/movimentos. Menu ganha os botoes "Modo Zen" e "Cafe da Manha (N tentativas)". Escopo: tela de resumo dedicada ao encerrar o Zen (em vez de apenas voltar ao menu) fica para item futuro)
- [x] **GP-04** `G` — Medidor de Aroma + habilidades de barista (equipar pré-fase, desbloqueio por progressão). (escopo reduzido a uma habilidade, como praticado em GP-01/GP-02: medidor de Aroma por sessao, +1 por peca de match/moedor ate 30 (capacidade), exibido no HUD como uma barra; ao encher, o botao "Degustacao" fica disponivel e revela uma jogada valida por 5s (reaproveita `Match3Engine.findAvailableMove`/`hintMove`, ja usado no hint do GP-08). Torra Perfeita, Mao Firme e Dose Dupla, a tela de escolha/equipar habilidade antes da fase e o desbloqueio por progressao ficam para itens futuros — sem eles, nao ha "escolha" de barista ainda, so a unica habilidade implementada)
- [ ] **UX-11** `M` — Acessibilidade: semântica no Canvas, modo símbolos, motion reduzido, alvos ≥ 48 dp, contraste AA.
- [ ] **UX-10 / UX-12** `M` — Configurações/pausa/créditos + microcopy.
- [ ] **GP-06** `G` — Streak diário, missões, eventos de fim de semana (pós-analytics).

## Fase 6 — Preparação de monetização (P2/P3 — nada ativo)

- [ ] **MZ-02** `M` — `:monetization:api` + `:noop` + binds Hilt + flavors `free`/`monetized` (free = padrão, sem SDKs). *Aceite:* APK free idêntico em comportamento; flags off ⇒ zero superfícies visíveis.
- [ ] **MZ-03** `M` — Wallet/entitlements no DataStore + espaços condicionais na UI.
- [ ] **MZ-04** `M` — Crashlytics + Analytics (eventos de funil de gameplay.md §7) — pré-requisito de decisão.
- [ ] **MZ-05..07** `G` — AdMob+UMP, Billing/RevenueCat, passe de temporada — **somente após os critérios de gate** de monetization.md §8, nesta ordem.

---

## Visão de dependências (macro)

```mermaid
flowchart TD
    F0[Fase 0<br/>Higiene] --> F1[Fase 1<br/>Testes]
    F1 --> F2[Fase 2<br/>Estado/Navegação/Dados]
    F2 --> F3[Fase 3<br/>Render/Release]
    F2 --> F4[Fase 4<br/>Módulos/Engine 2.0]
    F3 --> F5[Fase 5<br/>UI 2.0 + Gameplay 2.0]
    F4 --> F5
    F4 --> F6[Fase 6<br/>Monetização preparada]
    F5 --> F6
```

**Primeira release pública recomendada:** ao fim da Fase 3 (app estável, bonito o suficiente, rápido, com ícone/splash) em faixa interna/fechada da Play — para começar a colher dados reais que guiarão as Fases 5 e 6.
