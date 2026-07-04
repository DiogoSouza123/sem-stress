# Sprite Packs

- `current/`: pack ativo para a aplicacao.
- `packs/v1-small-64/`: pack atual em baixa resolucao (64x64 por frame).

## Como trocar para um pack futuro (HD)
1. Gere o novo pack em `packs/<novo-pack-id>/` mantendo a mesma estrutura de pastas.
2. Substitua o conteudo de `current/` pelo novo pack.
3. Nao altere nomes de item (`coffee-red`, `coffee-yellow`, ..., `fire`).

## Estrutura por item
- `items/<item>/sheet.png` (4x3, 12 frames)
- `items/<item>/frames/frame_00.png` ate `frame_11.png`
