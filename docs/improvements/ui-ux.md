# UI/UX — Interface e Experiência Visual

Objetivo: aparência moderna e profissional **sem perder a identidade** — o universo de cafeteria artesanal (paleta terrosa, xícaras, grãos) é o diferencial e deve ser amplificado, não substituído. Itens identificados como `UX-nn` (consolidados em [backlog.md](backlog.md)).

## 1. Identidade visual e design system — `UX-01` (P1, M)

O tema atual (CoffeeDark/Caramel/Latte/Cream + Gold/Mint) é um bom ponto de partida, mas está espalhado: telas referenciam cores cruas (`Caramel.copy(alpha=…)`) em vez de roles semânticos, e há hardcodes de gradiente por tela.

**Ações:**
- Criar um **design system mínimo** em `:core:ui`: tokens semânticos (`surfaceBoard`, `pieceHighlight`, `success`, `danger`, `hudText`…) mapeados sobre a paleta café; nenhuma tela usa cor crua diretamente.
- Componentes canônicos com preview + screenshot test: `CoffeeButton` (primário/secundário/ícone), `CoffeePanel` (cards com textura sutil de papel kraft), `StatChip`, `StarRating`, `ProgressCup` (barra de progresso em forma de xícara enchendo — assinatura visual do jogo).
- Suporte real a **tema escuro** (o `DarkColors` existe mas `darkTheme` está fixo em `false`): respeitar `isSystemInDarkTheme()` com paleta "café noturno" (tons de espresso + âmbar).

**Tipografia — `UX-02` (P2, P):** `Type.kt` está vazio (Material default). Adotar par de fontes via Google Fonts (downloadable ou empacotada): uma display arredondada e "food-friendly" para títulos/números do HUD (ex.: Baloo 2, Fredoka) + uma legível para corpo (ex.: Nunito Sans). Números de pontuação com `fontFeatureSettings "tnum"` (tabular) para o placar não "dançar".

## 2. Ícone, splash e primeira impressão — `UX-03` (P1, P)

- O manifest **não define `android:icon`** — criar ícone adaptativo (foreground: xícara com grãos; background: gradiente caramelo) + ícone monocromático (Android 13+ themed icons).
- Splash screen oficial (`core-splashscreen`) com o logo `coffe-crush-logo` — cobre o carregamento assíncrono (performance.md §1).
- Edge-to-edge (`enableEdgeToEdge()`) com insets tratados; hoje o conteúdo usa `padding(16.dp)` fixo e ignora status/navigation bars.
- Travar orientação retrato na tela de jogo (com RR-01 resolvendo restauração, rotação deixa de ser destrutiva, mas retrato é o formato do gênero).

**Feito em UX-03.** Ícone adaptativo (`mipmap-anydpi-v26/ic_launcher(.xml/_round.xml)` + `drawable/ic_launcher_background` com gradiente caramelo + `drawable/ic_launcher_foreground` com um vetor de xícara/vapor, reaproveitado como camada `monochrome`); como `minSdk` já é 26, não há mipmaps legados de fallback. Splash via `androidx.core:core-splashscreen` (`Theme.CoffeeCrushMobile.Starting` → `postSplashScreenTheme` para o tema normal), com `setKeepOnScreenCondition` ligado a `MenuViewModel.uiState.isLoading` (a mesma instância injetada por Hilt tanto na Activity quanto no Compose, já que `hiltViewModel()` sem rota reutiliza o `ViewModelStore` da Activity). `enableEdgeToEdge()` habilitado e `Modifier.safeDrawingPadding()` aplicado na raiz do `NavHost`/loading — cobre o requisito sem redesenhar o HUD (isso é escopo do UX-05). Orientação travada para o **app inteiro** via `android:screenOrientation="portrait"` na única Activity (não dá para travar só a tela de jogo em uma arquitetura single-Activity sem um comportamento visualmente estranho ao trocar de orientação no meio da navegação); gera os lint warnings esperados `LockedOrientationActivity`/`DiscouragedApi` (Android 16 vai ignorar orientações fixas em telas grandes) — aceito conscientemente pois o gênero match-3 é portrait-only, reavaliar se/quando o app ganhar suporte a tablet.

## 3. Menu de fases → Mapa de progressão — `UX-04` (P1, G)

O menu atual (grid de cards com cadeado) é funcional, mas genérico.

**Evolução em dois passos:**
1. **Curto prazo (M):** melhorar o grid — mostrar **estrelas conquistadas** (0–3) por fase (ver GP-03), melhor pontuação, animação de "pulse" na próxima fase jogável, cabeçalho com total de estrelas e xícara de progresso geral.
2. **Médio prazo (G):** substituir por **mapa vertical rolável** ("Jornada do Café"): trilha sinuosa em que cada nó é uma fase, agrupada por regiões temáticas que seguem a narrativa já presente nos nomes das fases (Introdução → Moagem → Torrefação → Espresso → … → Mestre do Café). Regiões trocam paleta de fundo e trilha musical. É o padrão do gênero porque comunica progressão e futuro conteúdo de forma visceral (o jogador **vê** o caminho à frente).

**Por que melhora a experiência:** estrelas criam meta secundária (replay), o mapa cria senso de jornada e antecipação, e a região temática reforça identidade.

## 4. Tela de jogo e HUD — `UX-05` (P1, M)

Problemas atuais: HUD é um card de 3 números (Pontos/Meta/Mov) sem hierarquia; mensagens de feedback aparecem como `Text` cru ("Movimento invalido."); tabuleiro num card branco sem ambientação; botão "Voltar ao menu" gigante no fluxo principal.

**Ações:**
- **HUD superior:** barra de progresso da meta (a `ProgressCup` enchendo até a meta — muito mais legível que "Pontos 3200 / Meta 9000"), contador de movimentos com destaque e mudança de cor/pulso quando ≤ 5, objetivos da fase como chips com ícone + contador (preparado para gameplay 2.0).
- **Fundo ambientado:** gradiente/ilustração da região da fase atrás do tabuleiro (com blur/escurecimento para contraste), no lugar do gradiente genérico.
- **Feedback de pontos in-place:** números flutuantes (`+500`) subindo do match, escalando com combo; banner "Combo x3!" com animação de escala/fade em vez de texto estático.
- **Movimento inválido:** shake horizontal das duas peças + haptic leve (`HapticFeedbackType`), sem texto.
- Botões de sistema (voltar, som, pausa) como ícones discretos no topo; sair da partida em andamento pede confirmação (`UX-09`, também ligado ao back correto de RR-04).

**Feito em UX-09.** `GameScreen` ganhou `BackHandler(enabled = !game.finished)` e o botão "Voltar ao menu" passou a chamar a mesma função `requestExit`: se a fase já terminou (`game.finished`), sai direto (o dialog de vitória/derrota já é a confirmação); caso contrário, mostra um `AlertDialog` de confirmação (strings em `strings.xml`: `exit_game_confirmation_*`) antes de invocar `onBackToMenu`. O botão de ícone discreto/HUD novo continua para o UX-05; aqui só o comportamento de confirmação foi implementado, reaproveitando o botão texto atual. O back do menu para sair do app já funcionava desde o RR-04 (sem `BackHandler` no `StageMenuScreen`, cai no comportamento padrão do sistema).

## 5. Animações e efeitos — `UX-06` (P1/P2, G — depende de RR-20/RR-07)

Estado atual: peças caem "teleportando" frame a frame (65 ms por passo), explosão é um sprite de fogo com alpha/scale, swap não tem animação visível (o board só re-renderiza).

**Alvo (habilitado pelo board em Canvas + engine por eventos):**

| Momento | Animação alvo | Porquê |
|---|---|---|
| Swap | Deslizamento das duas peças (120–150 ms, easing suave); swap inválido vai e volta | Feedback físico direto da ação |
| Match | Squash & stretch rápido + partículas de grãos/respingos de café estourando + som | Recompensa sensorial — é o "juice" central de um match-3 |
| Queda | Interpolação contínua com leve bounce no pouso (não frames discretos) | Fluidez percebida define a qualidade do jogo inteiro |
| Cascata | Pitch do som de match sobe a cada nível de cascata; tremor sutil da câmera em cascatas ≥ 3 | Escalada de excitação; ensina o valor de cascatas |
| Peças especiais | Efeitos próprios (linha de vapor, moinho girando — ver gameplay.md) | Legibilidade das mecânicas |
| Ocioso | A cada ~8 s sem input, brilho sutil numa jogada válida (hint) | Reduz frustração de iniciantes sem tirar agência |

**Vitória/derrota — `UX-07` (P2, M):** substituir o `AlertDialog` por telas/overlays dedicados: vitória com xícara enchendo, 1–3 estrelas entrando com bounce sequencial, contagem animada de pontos e confete (Lottie); derrota acolhedora ("Faltou pouco!"), mostrando progresso até a meta e CTA de replay — e, no futuro, o ponto natural do rewarded ad de +5 movimentos (**desligado por padrão**, ver monetization.md §5).

## 6. Som como UX — `UX-08` (P2, M — RR-22)

Música existe; efeitos, não. Adicionar SFX curtos: seleção (clique de xícara), swap, match (pitch por nível de cascata), peça especial, vitória (sino + vapor), botão. Controles separados de **música** e **efeitos** nas configurações; haptics sutis em match/combo/vitória (com toggle). Som é metade do "juice" de um match-3 — o custo é baixo e o impacto na percepção de qualidade é enorme.

**Feito em RR-22:** `SfxPlayer` (`audio/SfxPlayer.kt`) usa `SoundPool` — pré-carrega os 5 efeitos (`SfxEffect`: `SELECT`/`SWAP`/`INVALID_MOVE`/`MATCH`/`VICTORY`) uma única vez no construtor, evitando a latência de decodificação por toque que um `MediaPlayer` teria. `GameScreen` reage a transições de `GameUiState` (seleção, `animating`, `explodingMatches`, mensagem de movimento invalido, vitória) com `LaunchedEffect` para disparar SFX + `LocalHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)`; `StageMenuScreen` ganhou o mesmo botão de "Efeitos: ON/OFF" ao lado do de música. Ambos os toggles persistem via `SettingsStore` (chave nova `sfx_muted`, paralela à `music_muted` existente) — nenhuma migração necessária pois é uma chave nova, não uma mudança de formato. Os áudios de SFX de placeholder foram sintetizados (tons curtos gerados por `ffmpeg`/`sine`) até haver arte sonora definitiva; os 3 WAVs de música em `res/raw` foram convertidos para OGG (RR-22, reduz ~1/3 do tamanho sem perda audível perceptível). `MusicaFundoPlayer` ganhou `pausar()`/`retomar()`, acionados por um `DefaultLifecycleObserver` em `ProcessLifecycleOwner` (registrado em `CoffeeCrushApplication.onCreate`) — a música agora pausa quando o app vai para background e retoma ao voltar, em vez de continuar tocando indefinidamente.

## 7. Novas telas de suporte — `UX-10` (P2, M)

- **Configurações:** música, efeitos, haptics, idioma (futuro), créditos (atribuição das músicas freetouse.com — obrigação de licença), política de privacidade (obrigatória nas lojas), "restaurar progresso" futuro.
- **Onboarding/tutorial (GP-08):** primeira fase guiada com overlay apontando a jogada, em vez de texto.
- **Pausa:** overlay com continuar/recomeçar/sair (hoje não existe pausa).

## 8. Acessibilidade — `UX-11` (P2, M)

Estado atual: imagens com `contentDescription = null`, células sem semântica, diferenciação de peças só por cor/forma do sprite, sem consideração a motion/contraste.

| Área | Ação |
|---|---|
| TalkBack | Semântica por célula no Canvas (Compose permite `Modifier.semantics` com retângulos virtuais): "grão vermelho, linha 3, coluna 4, selecionado"; ações customizadas de swap |
| Daltonismo | Modo "símbolos" nas configurações: pequeno glifo distinto sobre cada tipo de peça (os sprites de café já têm formas variadas — validar com simulador de daltonismo e reforçar onde necessário) |
| Motion | Respeitar `Settings.Global.ANIMATOR_DURATION_SCALE` ≈ 0 / preferência de movimento reduzido: desligar tremor de câmera, partículas e bounce |
| Toque | Alvos ≥ 48 dp (células em boards 8×8 num telefone pequeno podem ficar < 40 dp → aumentar hit area além do visual) |
| Texto | Suportar font scale do sistema sem quebrar o HUD (testar em 1.3×/2×); contraste WCAG AA nos textos sobre fundos caramelo (o `CoffeeDark.copy(alpha=0.7f)` atual sobre Cream é limítrofe — validar) |
| Sem áudio | Nenhuma informação exclusivamente sonora (combos também são visuais) |

## 9. Microcopy e tom — `UX-12` (P3, P)

Tom de cafeteria acolhedora, curto e caloroso: "Café coado com perfeição! ☕" em vez de "Fase concluida!"; "Os grãos se recusaram a combinar…" em vez de "Movimento invalido.". Centralizar tudo em `strings.xml` (junto com RR-09). Evitar humilhar o jogador na derrota; sempre apontar o próximo passo.

## 10. Resumo priorizado

| ID | Item | Prio. | Esf. | Depende de |
|---|---|---|---|---|
| UX-03 | Ícone + splash + edge-to-edge + retrato | P1 | P | — |
| UX-01 | Design system + tokens + dark theme | P1 | M | — |
| UX-05 | HUD novo (ProgressCup, feedback in-place) | P1 | M | RR-01 |
| UX-04a | Menu com estrelas/best score | P1 | M | GP-03 |
| UX-06 | Animações fluidas (swap/queda/partículas) | P1 | G | RR-20, RR-07 |
| UX-08 | SFX + haptics + controles separados | P2 | M | **Feito** |
| UX-07 | Telas de vitória/derrota | P2 | M | UX-01 |
| UX-02 | Tipografia própria | P2 | P | UX-01 |
| UX-10 | Configurações/pausa/créditos | P2 | M | RR-04 |
| UX-11 | Acessibilidade | P2 | M | RR-20 |
| UX-04b | Mapa de jornada | P2 | G | UX-04a |
| UX-09 | Confirmação ao sair + back correto | P1 | P | RR-04 |
| UX-12 | Microcopy | P3 | P | RR-09 |
