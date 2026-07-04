# CLAUDE.md — Coffee Crush Mobile

## Contexto

Este é o **coffeecrush-mobile**: jogo match-3 em Kotlin + Jetpack Compose (Material 3, minSdk 26), evolução do projeto desktop legado que vive na raiz do repositório (`../../`, Java 8 + Swing).

**Regra de ouro:** o projeto desktop (`../../src`, `../../build.xml`) está CONGELADO. Nunca o modifique. Ele serve apenas como oráculo de comportamento (regras de negócio e testes de engine em `../../test/com/semstress/`).

## Fonte do plano de trabalho

Todo o trabalho deste projeto segue o plano de modernização documentado em `../../docs/improvements/`.

**Antes de qualquer tarefa, leia nesta ordem:**

1. `../../docs/improvements/README.md` — visão geral, estado atual, convenções de prioridade (P0–P3) e esforço (P/M/G), e o índice de todos os documentos.
2. `../../docs/improvements/backlog.md` — a **fonte única de execução**: fases ordenadas (0 a 6), checkboxes, critérios de aceite e dependências. A próxima tarefa é sempre o primeiro item não marcado da fase mais baixa incompleta, salvo instrução contrária do usuário.

## Como navegar na pasta improvements

Cada item do backlog tem um ID (`RR-nn`, `UX-nn`, `GP-nn`, `MZ-nn`, `CQ-nn`, `PF-nn`) e aponta para o documento com o detalhamento completo. Consulte o documento temático ANTES de implementar o item:

| Prefixo do item | Documento de detalhe | Conteúdo |
|---|---|---|
| `RR-` | `refactoring-roadmap.md` | Problema, impacto, solução, riscos de cada refatoração; grafo de dependências |
| `RR-` (racional) | `architecture.md` | Arquitetura alvo (MVVM/UDF, módulos, contratos de código), plano de migração incremental |
| libs/versões | `frameworks.md` | Qual biblioteca usar para quê, e o que NÃO adotar |
| `CQ-` | `code-quality.md` | Convenções (código em inglês, strings em pt-BR), estratégia de testes, CI, Definition of Done |
| `PF-` e RR-20/21 | `performance.md` | Correções de render (board em Canvas), sprites, memória, metas mensuráveis |
| `UX-` | `ui-ux.md` | Design system, HUD, animações, acessibilidade, microcopy |
| `GP-` | `gameplay.md` | Peças especiais, objetivos, estrelas, modos, ordem de implementação de gameplay |
| `MZ-` | `monetization.md` | Arquitetura de monetização DESLIGADA por padrão, flags, LGPD/lojas |
| Futuro distante | `ideas.md` | Não implementar nada daqui sem pedido explícito |

## Fluxo de trabalho por tarefa

1. Ler o item no `backlog.md` (critério de aceite) + a seção correspondente no documento temático.
2. Verificar dependências do item — se uma dependência não está concluída, avisar o usuário em vez de prosseguir.
3. Implementar em mudanças pequenas; um item do backlog por vez, um PR/commit por item.
4. Rodar testes e validações antes de concluir:
   - `./gradlew testDebugUnitTest` (sempre)
   - `./gradlew lint` e `./gradlew detekt` (quando configurados — Fase 0)
   - `./gradlew assembleDebug` para confirmar build
5. Marcar o checkbox do item em `../../docs/improvements/backlog.md` no mesmo commit.
6. Se o plano precisou mudar durante a implementação, atualizar também o documento temático correspondente.

## Restrições permanentes

- **Progresso do jogador é sagrado:** qualquer mudança em persistência exige migração testada (nunca resetar `SharedPreferences`/DataStore de quem já instalou).
- **Nada de monetização ativa:** SDKs de ads/billing não entram no APK; superfícies de monetização só existem atrás de feature flags com default `false` (ver `monetization.md`).
- **Código novo em inglês; strings visíveis ao jogador em pt-BR via `strings.xml`** (nunca hardcoded em composables).
- **Testes antes de refatorar:** não tocar em `engine/` ou no controller/ViewModel sem os testes das Fases 1 (RR-19/CQ-02) verdes.
- **Comportamento da engine:** em caso de dúvida sobre regra (pontuação, cascata, shuffle), o desktop legado e seus testes decidem.
- Renomeações (RR-09) nunca misturadas com mudanças de lógica no mesmo commit.
- `local.properties`, `.gradle-user/` e `build/` nunca são versionados.

## Comandos

```bash
./gradlew testDebugUnitTest   # testes unitários
./gradlew assembleDebug       # build debug
./gradlew lint detekt         # qualidade (após Fase 0)
```

O app roda via Android Studio ou `./gradlew installDebug` com emulador/dispositivo conectado.
