# Performance

Diagnóstico dos gargalos reais encontrados no código atual e correções recomendadas, em ordem de severidade. Regra geral: **medir antes e depois** (ver §6) — nada de otimização especulativa além do listado aqui, que foi identificado por leitura de código.

## 1. Inicialização: I/O síncrono na main thread (crítico — RR-02)

**Onde:** `CoffeeCrushApp()` faz `remember { StageRepository(appContext).load() }` (abre assets, faz parsing de `Properties`, checa overrides em disco) e o construtor do controller chama `progressRepository.load()` (leitura de `SharedPreferences`) — tudo na primeira composição, na main thread.

**Impacto:** frames perdidos no cold start hoje; com mais fases/configs, risco de ANR. Também bloqueia a estratégia de splash screen.

**Correção (junto de RR-01/RR-02):**
- Carregamento em `viewModelScope` + `Dispatchers.IO`; `UiState.Loading` renderiza splash/skeleton.
- `androidx.core:core-splashscreen` segurando a splash até `uiState !is Loading` (`setKeepOnScreenCondition`).
- Ativar `StrictMode` em builds debug (`detectDiskReads/Writes/NetworkOps` + `penaltyLog`) para impedir regressão.

## 2. Renderização do tabuleiro: recomposição contínua (crítico — RR-20)

**Onde:** `GameScreen` tem um ticker global (`rememberSpriteFrame`) que incrementa um `mutableStateOf` a cada 80 ms. Esse valor é lido por **todas** as células → o tabuleiro inteiro (até 8×8 = 64 `PieceCell`, cada um com Box aninhados, `AnimatedContent` e 4 `animateFloatAsState`) recompõe ~12,5×/segundo, para sempre, mesmo com o jogo parado.

**Impacto:** jank em aparelhos médios, consumo de bateria, e piora quadrática com tabuleiros maiores. É o teto de qualidade das animações futuras.

**Correção — board como um único `Canvas`:**

```kotlin
@Composable
fun BoardCanvas(state: BoardUi, sprites: SpriteAtlas, modifier: Modifier) {
    val frameTime = rememberFrameTicker()   // produz tempo, lido SÓ no draw
    Canvas(modifier.pointerInput(state.rows, state.cols) { /* tap + drag */ }) {
        // leitura de frameTime aqui invalida apenas a fase de DESENHO,
        // sem recomposição nem novo layout
        val frame = ((frameTime.value / 80) % sprites.frameCount).toInt()
        state.cells.forEach { cell ->
            drawImage(
                image = sprites.sheet(cell.piece),
                srcOffset = sprites.srcOffsetFor(cell.piece, frame),
                srcSize = sprites.frameSize,
                dstOffset = cell.topLeftPx,
                dstSize = cellSizePx,
            )
        }
        // overlays de seleção/highlight/explosão: drawRoundRect/drawImage aqui mesmo
    }
}

@Composable
private fun rememberFrameTicker(): State<Long> {
    val time = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { time.longValue = it }  // sincronizado com vsync
    }
    return time
}
```

Pontos-chave:
- Ler o tempo **dentro do draw scope** limita a invalidação à fase de desenho (Compose separa composição/layout/desenho).
- Interpolações (queda, swap, explosão) viram funções `t -> offset/alpha/scale` desenhadas no Canvas, destravando a animação de queda contínua (UX-05) que o modelo atual de "frames discretos de snapshot" não permite.
- Tap/drag: o `resolveCellAtOffset` atual já faz a matemática célula↔offset; reaproveitar no `pointerInput` do Canvas.
- Migração segura: manter `BoardView` antigo atrás de flag local até o Canvas atingir paridade (mesma estratégia de flags de monetization.md §3).

## 3. Sprites e memória (RR-21)

**Onde:** `GameSpritePack` decodifica 12 PNGs × 7 itens com `BitmapFactory.decodeStream` sem `inSampleSize`/resize, num cache singleton global mutável (`SpritePackCache`/`BitmapCache`).

**Problemas e correções:**

| Problema | Correção |
|---|---|
| Frames decodificados no tamanho original independentemente do tamanho da célula (~40–60 dp) | Decodificar com downsampling para ≈ tamanho de célula × densidade (usar `BitmapFactory.Options.inSampleSize` ou `ImageDecoder` com `setTargetSize`) |
| 84 bitmaps separados | Usar as **spritesheets** que já existem em assets (`sheet.png` por item; os `composed/` foram gerados pelo `SpriteSheetGenerator`); um bitmap por item + `srcOffset` no `drawImage` → menos objetos, melhor cache de GPU |
| Cache singleton global mutável escondido | `SpriteAtlas` carregado por `SpriteRepository` injetado (escopo de app), pré-carregado durante a splash |
| `AnimatedContent` + slide/fade por célula | Morre junto com a migração para Canvas (§2) |

## 4. Alocações na engine (RR-07/RR-17)

**Onde:** cada rodada de cascata gera `snapshot()` (nova `List<List<Int>>`) antes/depois da limpeza e **um snapshot por passo de queda** (`fallFrames`). Uma cascata 3× em board 8×8 aloca dezenas de listas aninhadas; além disso `syncGameState()` copia o board a cada frame de animação.

**Impacto:** pressão de GC durante exatamente o momento mais sensível (animação).

**Correção (em duas etapas, ver RR-07):**
1. Curto prazo: reduzir cópias — snapshot como `IntArray` plano (`IntArray(rows*cols)`) em vez de listas aninhadas; reutilizar buffers.
2. Definitivo: engine emite **eventos semânticos** (`Cleared(positions)`, `Moved(from, to)`, `Spawned(pos, piece)`) e a UI interpola — elimina frames de snapshot por completo e habilita animações contínuas.

## 5. Áudio

- `MediaPlayer.create()` + `release()` a cada troca de faixa: aceitável para música, mas adicionar **fade** e pausar em `onStop` (hoje a música continua com o app em background — bug de UX e de bateria; usar `DisposableEffect` com `LifecycleObserver` ou mover o player para um componente ciente de ciclo de vida).
- SFX (novos, RR-22): `SoundPool` com sons pré-carregados; nunca `MediaPlayer` para efeitos curtos (latência).
- Converter WAV → OGG (os 3 WAVs em `res/raw` são grandes; OGG reduz APK substancialmente sem perda audível).

## 6. Build, release e medição

| Item | Ação | Prioridade |
|---|---|---|
| R8 (RR-13) | `isMinifyEnabled = true` + `isShrinkResources = true` no release; testar fluxo completo em build release no CI | P1 |
| Baseline Profiles | Gerar com Macrobenchmark (startup + abrir fase + executar jogada); melhora cold start e jank do primeiro uso | P2 |
| Macrobenchmark | Cenários: cold start → menu; abrir fase; jogada com cascata 3×. Rodar em CI agendado; comparar antes/depois de cada otimização deste doc | P2 |
| JankStats | Coletar frames lentos em produção (quando houver analytics) com tag da tela | P3 |
| Compose compiler metrics | Habilitar relatórios de estabilidade ao investigar recomposição (`-P ...compose.compiler.plugin.metricsDestination`) e confirmar que `GameUiState`/`BoardUi` são stable | P2 (durante RR-20) |
| Configuration cache | Habilitar em `gradle.properties` para DX | P2 |

**Metas mensuráveis:**
- Cold start (P50, device médio): < 1,5 s até menu interativo.
- 0 frames > 32 ms durante cascata 3× em board 8×8 (medido no Macrobenchmark).
- Memória de sprites < 20 MB.
- APK release < 25 MB (hoje inflado por WAVs e ausência de shrinking).
