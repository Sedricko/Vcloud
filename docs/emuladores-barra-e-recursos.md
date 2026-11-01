# vPhoneOS 5.2.1 e Dual Space (Virtual Master) 3.2.86 — barra/bolha e recursos para o Vcloud

Origem: APKs enviados pelo usuário (01/11), analisados SÓ por leitura (dex, manifesto e textos); nada foi executado.
Marcação: **[lido]** = aparece nos textos/classes do APK; **[dedução]** = conclusão minha a partir disso.

## vPhoneOS (`com.yoyo.snake.rush`, minSdk 29, targetSdk 36) — Android inteiro dentro do app
- Permissões de janela flutuante: `SYSTEM_ALERT_WINDOW`, `SYSTEM_OVERLAY_WINDOW`. [lido]
- **Bolha (float ball)**: `Enable float ball`; gestos: `Double-click float ball > Back`, `Triple-click float ball > Recent`. [lido]
- **Janela flutuante do celular virtual** (`FloatPhoneWindow`): posição, tamanho, rotação e tamanho do Android salvos
  (`floatPhoneWindowX/Y/Width/Height/RotationDegree`); gestos na janela: duplo toque = Voltar, triplo = trocar tarefa,
  segurar = voltar à tela principal; botão de fixar no topo e de girar. [lido]
- **Navegação flutuante**: `Enable floating navigation window`, `Navigation bar layout`, `Block back gesture`,
  `Keep real machine's back gesture in full-screen mode`. [lido]
- **Menu personalizável** (`MenuCustomizeActivity`, chave `QuickMenuShortcuts`): "Long press and drag to reorder, add up to N items". [lido]
- **Modo passagem**: `Show float ball in passthrough mode` (toques atravessam a janela). [lido]
- Outros: vibração, sensor de gestos, trackpad, mapeamento de teclas/gamepad (arrastar, tocar para editar, segurar para
  redimensionar, transparência), contador de FPS, `Better performance (SurfaceView)`, resolução/DPI por instância,
  compartilhar área de transferência, snapshots, backup (`Documents/VPhoneOS_Backup`), Magisk/root, sincronizar toques
  de uma instância mestre para as outras, `Background keep-alive` ("Click to stop running in background"). [lido]
- [dedução] A bolha e o menu fazem parte da própria tela do emulador; por isso tocar neles nunca "sai do jogo" e o
  Unit/outros apps lá dentro não vazam para fora.

## Dual Space / Virtual Master (`com.clone.android.dual.space`, minSdk 22, targetSdk 36)
- Permissão `SYSTEM_ALERT_WINDOW`; biblioteca de janela flutuante **EasyFloat** (`com.lzf.easyfloat`); `FloatWindow`,
  `MiniBallView`, `VMDisplayActivity`; monitor de tecla do sistema. [lido]
- Textos: `Keep this app pinned on top as a floating window`, `Remember small window size and orientation`,
  `Long press the app card and tap the Floating Window icon`, `Tip: Floating capsule and notification quick reply are ready`. [lido]
- **Desativador do "matador de processos fantasma" (Android 12+)**: pareamento por **Depuração sem fio** com código de 6
  dígitos (janela flutuante sobre as Configurações), Shizuku (`moe.shizuku.manager.permission.API_V23`) e o resultado
  `max_phantom_processes set to 2,147,483,647` / `Phantom restriction disabled successfully!` /
  `Multi-process runtime is ready. Background tasks stay active.` [lido]
- Orientação do Google Play Services (atualizar/ativar). [lido]

## Opções para o Vcloud (esforço = estimativa minha)
| # | Opção | Vem de | Esforço | Valor |
|---|---|---|---|---|
| 1 | **Voltar / Girar / Bloquear gesto de voltar** dentro do app (novo comando para o app de dentro: IBActivityThread) | vPhoneOS | médio | alto: base das opções 2 e 7 |
| 2 | **Gestos na bolha**: duplo toque = Voltar, triplo = Recentes, segurar = Início do Vcloud | vPhoneOS | baixo (depois da 1) | alto |
| 3 | **Personalizar o painel**: arrastar para reordenar, escolher itens, limite de itens | vPhoneOS | médio | médio |
| 4 | **Manter apps vivos** (matador de processos): assistente com Shizuku/ADB sem fio, ou só guia + comando único | Dual Space | médio a alto | alto: já vimos "morto por pouca memória" |
| 5 | **Backup/restauração de saves** por app | vPhoneOS (backup) | médio | alto (já está no PLANO F4) |
| 6 | **Contador de FPS** e **gravar a tela** | vPhoneOS | médio | médio |
| 7 | **Notificação de segundo plano** com "Parar tudo" | vPhoneOS | baixo | médio |
| 8 | **Janela pequena flutuante** do app, com tamanho lembrado | ambos | alto/incerto (depende do One UI) | médio |
| 9 | **DPI/resolução por app** | vPhoneOS | alto/arriscado | baixo/médio |
| 10 | Mapeamento de teclas/gamepad, sincronizar instâncias | vPhoneOS | alto | baixo para o objetivo atual |
Não se aplicam ao Vcloud: compartilhar área de transferência e arquivos (o app roda no próprio celular, mesmas pastas), Magisk (o root é interno).
