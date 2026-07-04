# Banco de Ideias — Longo Prazo

Ideias **fora do roadmap comprometido** (nenhuma tem prioridade acima de P3 hoje). Cada uma registra o gatilho que a tornaria relevante e seus pré-requisitos, para que decisões futuras sejam rápidas. Nada aqui deve influenciar decisões de curto prazo — exceto onde indicado que a arquitetura já preserva a opção.

## 1. Plataforma

| Ideia | Descrição | Gatilho para considerar | Pré-requisitos |
|---|---|---|---|
| **iOS via Kotlin Multiplatform** | `:core:engine` + `:core:model` (já planejados como Kotlin puro) viram módulos KMP; UI em Compose Multiplatform ou SwiftUI | Tração comprovada no Android (retenção + downloads) | RR-08 concluído; disciplina de manter core sem Android (`androidx.*`) — **a arquitetura alvo já garante isso de graça** |
| **Cloud save** | Progresso sincronizado (Play Games Services salvos, ou backend próprio) | Reclamações de perda de progresso / multi-device | RR-06 (DataStore como fonte única serializável); resolução de conflito por "melhor progresso vence" |
| **Play Games Services** | Conquistas + leaderboards (modo contrarrelógio/diário) | GP-05 diário ativo e engajado | Login opcional, nunca obrigatório |
| **Tablets/foldables** | Layout adaptativo (board à esquerda, HUD à direita em landscape grande) | Métricas mostrarem base relevante | `WindowSizeClass`; RR-01 (estado sobrevive a resize) |

## 2. Conteúdo e liveops

| Ideia | Descrição | Gatilho | Pré-requisitos |
|---|---|---|---|
| **Fases remotas** | Catálogo de fases baixado (Remote Config/CDN) sem release; abre caminho para cadência semanal de conteúdo | > 50 fases e cadência quinzenal apertando | RR-16 (JSON versionado com `schemaVersion` + validação) — o formato já nasce pronto para isso |
| **Editor interno de fases** | Ferramenta desktop/web (Compose Desktop reusando `:core:engine`!) para desenhar boards, simular N execuções com seeds e medir taxa de vitória automaticamente | Balanceamento manual virar gargalo | RR-08; suporte a seed (já existe) |
| **Eventos sazonais** | Reskins temáticos (Natal, festa junina — café com paçoca!) via sistema de temas do design system | UX-01 com temas plugáveis + base ativa | GP-06 |
| **Narrativa leve** | Personagens da cafeteria (a dona, o torrefador, clientes recorrentes) com diálogos curtos entre regiões do mapa | Mapa (UX-04b) implantado | Roteirista/IA + i18n |
| **Colecionáveis** | "Cartas de grãos do mundo" (Etiópia, Colômbia, Brasil...) dropadas por 3 estrelas; álbum com curiosidades reais de café | Precisar de mais motivos de replay | Aqui sim, avaliar Room para inventário |

## 3. Social e competitivo

| Ideia | Descrição | Gatilho | Pré-requisitos |
|---|---|---|---|
| **Duelo assíncrono** | Ambos jogam a mesma seed do diário; compara-se pontuação (sem tempo real — barato e justo) | Base de amigos/social pedir | Backend leve ou Play Games; seed determinística (já existe) |
| **Ligas semanais** | Buckets de ~30 jogadores por pontuação semanal | Retenção estagnar com conteúdo solo | Backend; antifraude básico (validação server-side de scores plausíveis) |
| **Compartilhar replay** | Exportar GIF/vídeo curto de uma cascata grande (o desktop já gerou GIFs para o README — a ideia tem DNA no projeto) | Busca de crescimento orgânico | Captura de frames do Canvas |

## 4. Técnica

| Ideia | Descrição | Gatilho | Pré-requisitos |
|---|---|---|---|
| **Simulador de dificuldade** | Bot que joga N partidas por fase (greedy + heurísticas) reportando distribuição de resultados; roda no CI para detectar fases quebradas | Catálogo > 30 fases | Engine pura (já é); RR-19 |
| **Física de partículas própria** | Sistema simples de partículas no Canvas (grãos, respingos) com pooling | UX-06 pedir mais "juice" | RR-20 |
| **Acessibilidade avançada** | Modo uma-mão, controle por switch access, narração completa de board | Feedback de usuários | UX-11 |
| **Testes de propriedade** | Property-based testing (Kotest) na engine: "após resolver, nunca há match residual", "shuffle sempre deixa ≥ 1 movimento" | Bugs sutis de board recorrentes | RR-19 |

## 5. Ideias descartadas (e por quê — para não rediscutir)

| Ideia | Motivo do descarte |
|---|---|
| Migrar para Unity/libGDX | Reescrita total sem ganho para o gênero; Compose + Canvas atende (architecture.md §2.1) |
| Sistema de vidas/energia | Anti-tese da marca "sem stress"; monetização por frustração contradiz o posicionamento |
| Multiplayer tempo real | Custo de infra e complexidade desproporcionais ao gênero |
| Chat/social aberto | Moderação e risco (menores) sem retorno claro |
| Loot boxes | Risco regulatório/reputacional; contra a filosofia definida em monetization.md |
