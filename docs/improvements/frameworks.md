# Frameworks, Bibliotecas e Ferramentas Recomendadas

Para cada recomendação: **por que usar**, **problema que resolve**, **impacto da migração** e **prioridade**. Versões exatas devem ser resolvidas no momento da implementação (usar as estáveis mais recentes); o importante aqui é o papel de cada peça.

> Estado atual: Kotlin 1.9.24, AGP 8.5.2, Compose BOM 2024.09, Material 3, `SharedPreferences`, `MediaPlayer`, sem DI, sem navegação, sem testes, sem CI.

---

## 1. Toolchain (fazer primeiro)

### 1.1 Version Catalog (`gradle/libs.versions.toml`) — `P0`
- **Por quê:** fonte única de versões; pré-requisito prático para modularização.
- **Resolve:** strings de versão espalhadas nos `build.gradle.kts`; upgrades manuais propensos a inconsistência.
- **Impacto:** mecânico e seguro; nenhum comportamento muda.

### 1.2 Kotlin 2.x + plugin `org.jetbrains.kotlin.plugin.compose` — `P0`
- **Por quê:** o compilador Compose passou a ser distribuído junto com o Kotlin (plugin oficial), eliminando o acoplamento manual `kotlinCompilerExtensionVersion` ↔ versão do Kotlin que o projeto tem hoje.
- **Resolve:** upgrade travado; acesso a melhorias de compilador (K2), strong skipping e APIs novas de Compose.
- **Impacto:** remover `composeOptions.kotlinCompilerExtensionVersion`; atualizar AGP/BOM juntos; rodar app + testes. Risco baixo, feito em PR isolado.

### 1.3 KSP — `P1` (junto com Hilt)
- **Por quê:** processamento de anotações moderno e mais rápido que KAPT.
- **Resolve:** tempo de build ao adotar Hilt/Room.
- **Impacto:** nenhum no código de produção.

---

## 2. Arquitetura de app

### 2.1 Lifecycle ViewModel + `StateFlow` — `P0`
- **Por quê:** padrão oficial para estado com ciclo de vida; `SavedStateHandle` para sobreviver a process death.
- **Resolve:** RR-01 (partida perdida em rotação; scope vazando; controller intestável).
- **Impacto:** a maior refatoração individual do projeto — mover a orquestração de `CoffeeCrushController` para `GameViewModel`/`MenuViewModel`. Fazer após portar os testes de engine.

### 2.2 Hilt — `P1`
- **Por quê:** DI padrão Android com validação em compile time e integração nativa com ViewModel/Navigation.
- **Resolve:** RR-05 (wiring manual em `MainActivity`, impossibilidade de trocar fakes em teste).
- **Impacto:** `@HiltAndroidApp`, módulos por camada (`DataModule`, `EngineModule`, `MonetizationModule`), `@HiltViewModel`. Alternativa: **Koin** (mais simples, sem codegen, validação em runtime) — aceitável se preferir menos cerimônia; escolher um e não misturar.

### 2.3 Navigation Compose (rotas type-safe com kotlinx.serialization) — `P1`
- **Por quê:** back stack correto, argumentos tipados (`data class Game(val stageId: Int)`), transições, deep links.
- **Resolve:** RR-04 (navegação via `when`, botão voltar fechando o app).
- **Impacto:** substituir o `when(state.screen)` por `NavHost`; `stageId` passa a chegar pelo `SavedStateHandle` do ViewModel.

### 2.4 DataStore (Proto) — `P1`
- **Por quê:** persistência assíncrona, transacional, tipada e observável (`Flow`).
- **Resolve:** RR-06 (`SharedPreferences` síncrono, sem migração, menu que não reage a mudanças de progresso).
- **Impacto:** definir proto `PlayerProgress`; usar `SharedPreferencesMigration` mapeando as chaves atuais (`highest_unlocked_stage`, `current_stage`, `stage_N_best_score`). **Teste de migração obrigatório** — progresso do jogador não pode ser perdido.

### 2.5 kotlinx.serialization — `P1`
- **Por quê:** serialização Kotlin-first, multiplataforma, sem reflexão.
- **Resolve:** RR-16 — `.properties` não expressa o gameplay 2.0 (listas de objetivos, obstáculos por célula, camadas). Também usada nas rotas type-safe.
- **Impacto:** novo formato `stages.json` com `schemaVersion`; parser com validação e mensagens claras; manter override local em `files/config/`.

### 2.6 kotlinx.collections.immutable — `P2`
- **Por quê:** coleções imutáveis estáveis para Compose (evita recomposições por instabilidade de `List`).
- **Resolve:** parte do RR-20 (estabilidade de `GameUiState`).
- **Impacto:** trocar `List<List<Int>>` do estado por estruturas estáveis/`ImmutableList`.

---

## 3. Mídia e efeitos

### 3.1 SoundPool (SFX) + MediaPlayer/Media3 (música) — `P2`
- **Por quê:** `SoundPool` é a API certa para efeitos curtos de baixa latência (match, combo, botão); Media3/ExoPlayer só se a música evoluir para streaming/playlist — para loop de faixa local, `MediaPlayer` atual é suficiente com pequenos ajustes (fade in/out, pausar em `onStop`).
- **Resolve:** RR-22 (ausência total de SFX; feedback pobre).
- **Impacto:** novo `SoundEffectPlayer` em `:core:ui` (ou `:core:audio`), injetado; respeitar toggle de som e `AudioAttributes.USAGE_GAME`. Converter WAVs para OGG (menor).

### 3.2 Lottie Compose — `P2`
- **Por quê:** animações vetoriais de celebração (confete, estrelas, vapor de café) baratas de produzir e leves.
- **Resolve:** telas de vitória/derrota estáticas (UX-07).
- **Impacto:** dependência pequena; usar apenas fora do loop do tabuleiro (o board continua Canvas).

### 3.3 Coil — `P3`
- **Por quê:** carregamento de imagens com cache. Só necessário quando houver imagens dinâmicas (avatares, eventos remotos, loja).
- **Resolve:** nada hoje — sprites locais têm pipeline próprio (performance.md §3).
- **Impacto:** adiar até existir conteúdo remoto.

---

## 4. Qualidade e testes (detalhes em code-quality.md)

| Ferramenta | Papel | Prioridade |
|---|---|---|
| JUnit 5 + kotlin-test | Testes unitários (engine, use cases, parsers) | P0 |
| Turbine | Testar `StateFlow`/emissões do ViewModel | P0 (junto de RR-01) |
| MockK | Dublês para repositórios/gateways | P1 |
| kotlinx-coroutines-test | `runTest`, controle virtual de tempo (essencial para os delays de animação) | P0 |
| Compose UI Test (`createComposeRule`) | Testes de interação de tela | P1 |
| Roborazzi (ou Paparazzi) | Screenshot tests do design system e telas | P2 |
| detekt + ktlint (ou detekt-formatting) | Análise estática + estilo | P0 |
| Android Lint (com baseline) | Problemas Android-specific | P0 |
| Kover | Cobertura (substitui o papel do JaCoCo usado no desktop) | P2 |

## 5. CI/CD e release

| Ferramenta | Papel | Prioridade |
|---|---|---|
| GitHub Actions | PR: build + detekt + lint + unit tests; main: apk debug artifact; tag: bundle release | P0 |
| R8 + resource shrinking | Tamanho e ofuscação (RR-13) | P1 |
| Baseline Profiles + Macrobenchmark | Startup e jank medidos e otimizados (performance.md §6) | P2 |
| Play App Signing + faixas internas | Distribuição segura (internal testing → closed → production) | P2 |

## 6. Serviços (preparação, sem ativar nada de monetização)

| Serviço | Papel | Prioridade |
|---|---|---|
| Firebase Crashlytics | Crashes em produção — primeiro serviço a entrar quando o app for distribuído | P1 (na primeira release pública) |
| Firebase Analytics | Funil de fases, retenção, balanceamento (eventos: `stage_start`, `stage_win`, `stage_fail`, `moves_left`, `booster_used`) | P2 |
| Firebase Remote Config | Backend das **feature flags** (monetização desligada por padrão, experimentos de balanceamento) — a interface local de flags vem antes e não depende do Firebase (monetization.md §3) | P2 |
| AdMob / Play Billing / RevenueCat | **Não instalar agora.** Somente quando as flags de monetização forem ativadas (monetization.md §4) | P3 |

---

## 7. O que conscientemente NÃO adotar agora

| Tecnologia | Motivo |
|---|---|
| libGDX / Unity / Godot | Reescrita total; Compose + Canvas atende match-3 com folga (decisão em architecture.md §2.1). |
| Room | Não há dados relacionais ainda; DataStore cobre progresso/settings. Reavaliar com colecionáveis/histórico (ideas.md). |
| KMP/Compose Multiplatform | Adiar; apenas manter `:core:engine`/`:core:model` livres de Android para preservar a opção. |
| RxJava | Coroutines/Flow já são o padrão do projeto. |
| Firebase Realtime/Firestore | Sem features online ainda; cloud save entra em ideas.md. |
