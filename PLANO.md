# Vcloud: plano

## Objetivo (uma frase)
Um espaço virtual leve para **apps que precisam de root e os jogos em que eles mexem**, com root que só vale lá
dentro, **sem root no celular**. O resto fica no celular normal.

## A ideia
- Sem Android inteiro (diferente do vPhoneOS e do Virtual Master): o jogo roda no Android do próprio celular,
  hospedado pelo nosso app, como processo do mesmo usuário. Por isso o Unit lê a memória sem root.
- **Dentro do Vcloud:** apps de root (Unit, GameGuardian...) e os jogos em que eles mexem (Orecraft, DYSMANTLE).
  Um app de root só enxerga o que está dentro.
- **No celular normal:** todo o resto (WhatsApp, banco, jogos online, Play Store). Fica fora do alcance dos apps de root.
- **Root do Vcloud:** `su` interno com janela "permitir root?", só vale lá dentro. Chave por app: ligada nos apps de
  root, desligada nos jogos.
- **Natural de usar:** atalho na tela inicial, mesmas pastas do celular, mesma internet, notificações e copiar e colar.
- Meta: Android 7 até o **mais atual (Android 17)**; o S25 (Android novo) é o teste principal, quando o usuário autorizar.
  Se o 17 quebrar o motor, estudar as correções de quem já cuida do 17 (`docs/pesquisa.md`) e trazer só elas.

## Onde estamos (01/11)
- Analisados: vPhoneOS (`docs/vphone.md`) e Virtual Master (`docs/virtualmaster.md`). Os dois confirmam o princípio
  "mesmo usuário = lê a memória". Os dois são fechados: só servem para aprender.
- **F0 feita:** BlackBox 2.1.0 (64 bits, APK pronto das releases) rodando dentro do Virtual Master (bolinha "V" nos
  prints; versão do Android a confirmar). O **Orecraft abriu, rodou liso e salvou** (Cobre ficou após reabrir).
  Ainda não testado: jogo com login Google e outros apps.
- Visto nos prints: o BlackBox já traz "GMS Manager" (Google dentro dele, útil para a F1b) e opções Xposed.
  As opções "Hide Root" e "Hide Xposed" (esconder de verificações) **saem do nosso build** (regra do projeto).
- **F1 em andamento:** motor escolhido para começar = **NewBlackbox (LamPPKK)**, Apache 2.0, Android 5–16
  (minSdk 21, targetSdk 36), última mudança 25/08/2026. Copiado para `android/` (origem e mudanças em
  `android/NOTICE-VCLOUD.md`). Tirado: envio de registros para servidor de terceiros, esconder root, pular verificações,
  falsificar aparelho (Pixel 6/Android 12).
- **Compilação pelo GitHub:** `.github/workflows/android.yml` gera o APK a cada mudança em `android/` (aba Actions,
  artefato `vcloud-debug-apk`). **Build passou (01/11, run 2)**: APK arm64 de ~10 MB, pacote ainda
  `top.niunaijun.blackbox` (renomear para Vcloud depois). **Primeiro teste no S25 de verdade (instalado pelo usuário):** o nosso APK abriu
  e o app "Tinker" abriu dentro dele. Prints: o app de root "Tinker" abre lá dentro e mostra "Cannot run program su" (esperado: ainda não há
  root interno, é a F2). Tirado o pedido de VPN ao abrir.
  **Relatório de erros local:** quando algo trava, ou pelo menu "Gerar relatório de erros", grava
  `Download/Vcloud/relatorio-AAAAMMDD-HHMMSS.txt` (aparelho + logcat do Vcloud e dos apps de dentro); o usuário manda o
  arquivo na conversa. Nada sai do celular sozinho. **S25 = Android 17 (API 37), confirmado pelo relatório** (SM-S938B, patch 2026-10-05).
  Primeiro relatório: o jogo `com.evrika.miner.camp` fechava ao abrir por uma mudança do Android 17
  (`IServiceConnection.connected` ganhou um parâmetro `IBinderSession`). Corrigido no motor e **confirmado pelo 2º relatório** (erro sumiu).
  2º relatório (23:05): Orecraft, DYSMANTLE e com.slg.policewar abrem mas pedem **Google Play Services** ("missing").
  Outros erros vistos: (a) abrir a Play Store de dentro cai no "IntentRedirect Hardening" do Android 17 (intent embrulhado
  sem token); (b) Tinker caiu por ForegroundServiceDidNotStartInTime no ProxyService. Corrigidos no motor em 01/11 (aguardando teste).
  **Falta na F1:** ativar "Serviços do Google" no Vcloud e ver se o Orecraft roda e salva no S25.

**Atualização 01/11 10:25:** estado completo, correções e problemas atuais em `docs/passagem.md` (ler primeiro). DYSMANTLE deitado e balão do Unit funcionando; Play Store abre até o login.

**Atualização 01/11 (tarde):** (1) **Root interno v1** (commit 95b0512): chave por app (segurar o app > Root interno); com a chave
ligada o `su` (atalho do `sh`) entra no `PATH` do app e os caminhos `/system/xbin/su` etc. "existem". NÃO é uid 0 e exec por
caminho absoluto não é redirecionado. Aguardando teste no Tinker. (2) **Diagnóstico interno**: diário
(`blackbox/diagnostico.log`) com ABRINDO/ABRIU/NÃO ABRIU/FECHOU + causa em português (`Diagnostico`, `CausaDeErro`, testada em
`app/src/test`), tela Configurações > Diagnóstico, resumo no topo do relatório e aviso (toast) quando um app fecha de forma
anormal. **Próximo bloco do root (depende do teste do Tinker):** `su` nativo + janela "permitir root?" (Uma vez/Sempre/Negar),
uid 0 de mentira (`LD_PRELOAD` com `libfakeuid.so` ou `su` que responde sozinho), tela Root (lista) e registro de pedidos.

**Tela inicial W12 em tela cheia (01/11):** painel com margens de 12dp (antes deixava 96dp vazios embaixo), botão Adicionar
dentro do painel (linha das bolinhas), barra do topo transparente (sem o azul sólido) e grade de apps com colunas pela
largura da tela. Layout: `activity_main.xml`, `view_toolbar.xml`, `AppsFragment`. Falta: prévia em tela cheia (busca no topo,
barra de baixo da prévia `docs/design/inicio-w12.png`) e o teste do usuário em pé e deitado.

**Central de diagnóstico única (01/11):** as 3 entradas de relatório (Configurações > Gerar relatório, menu ⋮ > Gerar relatório e
a tela Diagnóstico) viraram uma só: **Central de diagnóstico** (Configurações e menu ⋮). Ela mostra o histórico dos apps e tem
Atualizar, Copiar, Gerar relatório (mostra o caminho) e Limpar. Telas agora respeitam as barras do sistema (Android 15+, targetSdk 36
desenha por baixo): a Central herda de `BaseActivity` e a tela inicial não aplica mais o inset duas vezes.

## Fases
Foco: **o root funcionar lá dentro** e **o que estiver lá dentro rodar bem**. O Unit é um dos apps de root.

| Fase | O que | Pronto quando |
|---|---|---|
| F0 | Testar o BlackBox pronto: o Orecraft, um jogo com login Google e mais 2 ou 3 apps que o usuário quer usar dentro | Abrem, rodam e salvam |
| F1 | Compilar o BlackBox e cortar o que não usamos (espaço mínimo) | O nosso build roda os mesmos apps |
| F1b | Ponte para o Google do celular (login, Play Games, licença); jogos entram copiados dos instalados pela Play Store | Um jogo loga com a conta do usuário lá dentro |
| F2 | Root do Vcloud (`su` interno com janela "permitir root?") | Um verificador de root dentro diz "tem root" |
| F3 | Apps de root funcionando: o Unit (e outro app de root) lendo e mudando o Orecraft lá dentro | Achar, editar e congelar o Cobre |
| F4 | Pastas: mesmas do celular + pasta dos saves visível; opção "isolado" | Backup do save pelo app Arquivos |
| F5 | Testar em Android antigo e novo (vPhone, outro emulador, S25 se autorizar) | Funciona do 7 ao 17 |
| Lab | Plano B: APK do jogo com uma pecinha nossa | Só se algum jogo não rodar no espaço |
| Lab | Google completo dentro do Vcloud (Play Store + Play Services) | Só se a ponte da F1b não servir para algum jogo |

## Como construir
- **O Vcloud é nosso.** vPhoneOS, Virtual Master, BlackBox e as continuações dele (`docs/pesquisa.md`) são
  **material de estudo**: aprendemos com eles; de código aberto só aproveitamos peças se a licença permitir.
- **Cada parte é uma peça separada, com o seu próprio teste** (núcleo, Google, root, pastas, tela). Trocar ou ajustar
  uma peça não obriga a refazer as outras.
- Google **de verdade, sem fingir**: jogo vem da loja, licença confirmada pela loja, login com a conta real.
  Play Integrity não é enganado; jogo que recusar fica no celular normal.

## Visual (escolhido em 01/11)
- **Conceito Windows 12**: fundo claro com ondas azuis, painel de vidro, blocos (Fixados, Espaço Vcloud, Celular,
  Todos os apps), busca no topo, barra embaixo. Prévia aprovada: `docs/design/inicio-w12.png` (fonte `.html` junto).
- Ícones por enquanto com a letra do app (sem cor); trocar pelos ícones reais só se o usuário pedir.
- A tela só é programada depois do motor e do root funcionando; cada tela nova ganha prévia antes.

## Limpeza feita (01/11)
- Tela inicial no conceito Windows 12; opção "Mostrar apps do sistema" (desligada por padrão) na lista de adicionar.
- Removidas opções antigas: ocultar root, VPN, permitir captura de tela, localização falsa, link de código aberto.
- Identidade própria: pacote `com.vcloud.app` (o app antigo `top.niunaijun.blackbox` precisa ser desinstalado; os
  apps de dentro precisam ser adicionados de novo).

## Próxima tarefa (para uma conversa nova): ferramenta de diagnóstico para ver erros
- Objetivo: ver os erros do Vcloud sem ler 14 mil linhas. **Só o Vcloud e os apps de dentro dele**; nada sai do celular.
- Resumo em português no **topo do relatório** (`Download/Vcloud/relatorio-*.txt`): quais apps caíram, a hora, o erro e a
  causa; avisos conhecidos com o que fazer (ex.: "precisa do Google Play Services → ative em Configurações > Serviços
  do Google"); os tipos de erro mais frequentes.
- **Aviso discreto quando um app de dentro fecha:** "Orecraft fechou — relatório salvo".
- Depois, com prévia antes: tela "Diagnóstico" (apps rodando, memória, últimos erros, botões Relatório e Parar app).
- Até lá: o usuário manda o relatório e o agente faz o resumo na conversa.

## Cuidados já conhecidos
- Matador de processos do Android 12+ (os dois emuladores tratam dele). Testar na F0/F1.
- Apps que recusam espaço virtual: não escondemos, só ficam de fora.

## Regras do projeto
Só usar o Unit e o root em jogos offline single-player; nada pago com dinheiro real; nada de anúncios com
recompensa; nada contra anti-trapaça nem para esconder o Vcloud; não mexer no celular de verdade sem pedir; prévia
antes de mudar tela; ideia fora de ordem vai para um ramo de laboratório.
