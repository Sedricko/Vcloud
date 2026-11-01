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

---
## X8 Sandbox (`com.x8zs.sandbox` 0.7.6.4.23, edição chinesa 64 bits, minSdk 22, targetSdk 30) — Android VM com modo PIP
Origem: APK de 503 MB (17 partes .7z). Textos traduzidos do chinês por mim. **[lido]** = nos textos/classes; **[dedução]** = minha.
- **Janela pequena / PIP próprio** ("画中画"): "Executa a máquina virtual em uma janela pequena flutuante"; guia: "jogue e estude/veja série/converse ao
  mesmo tempo". Exige "permitir exibir sobre outros apps". Recursos: `ic_pip_to_win`/`ic_pip_to_ball` (vira bolinha e volta a janela),
  `pip_dismiss_view` ("arraste para baixo para esconder"), `pip_menu_view`, tamanho mínimo/redimensionável, vídeo-guia. [lido]
- **Menu do PIP** (`pip_logo_menu`): **Voltar, Desktop, Multi-instância, Tarefas (recentes)** — os mesmos itens da bolha do vPhoneOS. [lido]
- **Troca rápida** ("快捷切换"): "ícone minúsculo meio transparente/meio escondido na tela principal para trocar entre instâncias sem sair
  da tela"; opções: ligar, posição do ícone, recolher o painel sozinho, mostrar só os instalados. [lido] (= a nossa aba)
- **Root**: chave **"Root global: ao ligar, todos os apps podem pedir root"** + tela por app (`VMRootManageActivity`); ligar/desligar pede
  reiniciar; "Root & Xposed: root embutido, Xposed em um toque" (`guide_root_xp`). [lido]
- **Esconder apps** ("应用隐藏": os outros apps não detectam o app) e bolha de "variador de velocidade e clique automático" (x8 ball). [lido]
  — fora do escopo do projeto (não escondemos de proteção/anti-cheat); clique automático/velocidade só se o usuário pedir.
- **Anti-limpeza em segundo plano** ("后台防清理", há versão "reforçada"): "evita ser limpo pelo sistema em segundo plano/tela bloqueada";
  serviço central em primeiro plano ("o serviço principal está rodando em segundo plano"); permissão `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. [lido]
- **Conserto do Android 12** (`FixAndroid12Activity`): baixa e instala um app auxiliar separado (`com.x8zs.wirelessadb`, depuração
  sem fio) e permite "ignorar" o aviso. [lido] (o Dual Space faz dentro do próprio app, com Shizuku/pareamento)
- Resolução personalizada e orientação automática (captura/gravação acompanham retrato/paisagem), "o mais tela cheia possível" (notch),
  rede liga/desliga ("网络开关"), câmera, senha por app, teclas virtuais embutidas, atalho de app, multi-instância (até 9), troca de ROM/reparo,
  troca de arquivos com o celular real, aviso de Huawei (modo puro). VIP pago: sem anúncios, 9 multi-instâncias, janela pequena customizada,
  resolução customizada, anti-limpeza, "todos os recursos avançados". [lido]

### O que isto muda nas opções
- Os 3 emuladores têm o mesmo núcleo de menu: **Voltar, Início, Recentes** (+ multi-instância). O painel v2 já cobre.
- **Novas ideias** (além das 10 acima): (11) **Modo offline por app** ("rede liga/desliga"): bom para jogos offline, mas sem VPN
  é difícil bloquear de verdade; (12) **Xposed em um toque** (o Vcloud já lista módulos XP); (13) **Senha/biometria por app**;
  (14) **Voltar ao Vcloud sem sair: aba que vira bolinha** (já temos a aba; falta arrastar-para-esconder); (15) **clique automático/velocidade** só mediante pedido.
- **Janela pequena**: nos 3 emuladores ela é desenhada pelo emulador (o Android virtual é uma imagem dentro de uma janela). No Vcloud o app é
  uma tela real: só dá com PIP nativo do Android (toque limitado) ou janela livre do One UI (incerto). Fica como pesquisa.
