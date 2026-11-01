# Passagem de bastão — Vcloud (01/11/2026, 10:25 BRT)

Leia também: `PLANO.md`, `.claude/skills/espaco-virtual/SKILL.md`, `android/NOTICE-VCLOUD.md`, `docs/analise-app.md`, `docs/pesquisa.md`.
Responda ao usuário em **português**, frases simples, uma mudança por vez, APK testado por ele entre as mudanças.

## O que é
App Android "espaço virtual" (sem Android inteiro): os apps/jogos rodam dentro do Vcloud, no Android do próprio celular,
como processos do próprio Vcloud (`com.vcloud.app:pN`). Motor = fork do NewBlackbox (LamPPKK, Apache 2.0) renomeado:
`android/Bcore` (`com.vcloud.motor`, Java + C++) e `android/app` (telas, `com.vcloud.app`, Kotlin).
Celular de teste: **Samsung S25 (SM-S938B), Android 17 (API 37)**. Visual: conceito Windows 12.

## Como compilar e entregar
- Repositório `Sedricko/Vcloud`, ramo **`claude/lucid-euler-fwkwoc`**.
- Cada push em `android/**` dispara `.github/workflows/android.yml` (GitHub Actions, ~3,5 min) → artefato
  `vcloud-debug-apk` (`Vcloud_4.0.0_arm64-v8a-debug.apk`). Baixar pelo `download_workflow_run_artifact` (link temporário),
  descompactar e mandar ao usuário. Compilar localmente não funciona na sessão.
- **Chave fixa**: `android/vcloud-debug.keystore` (senha `vcloud-debug`, alias `vcloud`). Todo APK instala por cima.
  Não trocar a chave.
- Relatório de erros: Vcloud > Configurações (ou menu ⋮) > "Gerar relatório de erros" → `Download/Vcloud/relatorio-*.txt`
  (logcat do Vcloud e dos apps de dentro; nada sai do celular). O usuário manda os arquivos na conversa (às vezes reenvia
  antigos junto: olhe a hora no nome).
- Tags de log úteis: `VcloudOrientacao`, `AppInstrumentation` ("Orientação:"), `VcloudSobrepor`, `VcloudAssinatura`,
  `BActivityThread` ("O app trocou o Instrumentation"), `FATAL EXCEPTION`, `BProcessManager: initProcess`.

## Última versão entregue: commit 3d1dd2f (build run 42 OK) — ainda não testada pelo usuário
Pedido ao usuário: abrir o Vcloud e esperar "Serviços do Google prontos"; abrir o Orecraft (não deve pedir Play Services);
ativar o balão do Unit e jogar alguns minutos (não deve aparecer "Vcloud não está respondendo"); mandar relatório.

## Estado confirmado pelos relatórios
| Item | Estado |
|---|---|
| Abre no S25 (Android 17), tela inicial W12, ícone, instala por cima | OK |
| Orecraft roda e salva | OK |
| **DYSMANTLE deitado** | **OK** (09:46: janela 2340x1080). Causa: o jogo troca o Instrumentation no `Application.onCreate`; correção recoloca o `AppInstrumentation` (BActivityThread.handleBindApplication) |
| DYSMANTLE passa da checagem do Google | OK (Play Store verdadeira, não a falsa) |
| **Balão do Unit (sobrepor)** | **OK** (10:11: `VcloudSobrepor ... -> 0` e balão na tela) |
| Play Store de dentro abre | OK até a tela de login (`UnauthenticatedMainActivity`), sem conta Google ainda |
| Unit: "Vcloud não está respondendo" com o balão | corrigido em f7eaa28, **aguardando teste** |
| Serviços do Google por padrão | feito em 3d1dd2f, **aguardando teste** |

## Correções desta conversa (todas no ramo)
| Problema | Causa | Correção (arquivo) |
|---|---|---|
| Apps fechavam ao abrir no Android 17 | `IServiceConnection.connected` ganhou `IBinderSession` | `ServiceConnectionDelegate` + stub `android/app/IBinderSession.java` |
| Tela preta na abertura | `splash.xml` usava `<bitmap>` com ícone adaptável | `<item android:drawable=...>` |
| Tela inicial travava esperando o motor | usuários pedidos na linha da UI | `MainActivity.initViewPager` em segundo plano |
| DYSMANTLE "assinatura inválida" | Play Store falsa sem assinatura | removida (`IPackageManagerProxy`) |
| Queda do Play Services | broadcast com usuário -1 | `IActivityManagerProxy.BroadcastIntent` |
| Abrir outro app de dentro | Intent Redirect (16/17) | `removeLaunchSecurityProtection` (`ActivityManagerCommonProxy`) |
| DYSMANTLE em pé | jogo troca o Instrumentation | `BActivityThread.handleBindApplication` chama `checkEnv(AppInstrumentation)` depois do onCreate; `AppInstrumentation` lê a orientação do APK e reaplica no resume |
| Play Store caía (ClassCastException) | Android 17: `getInstalledPackages` devolve `PackageInfoList` | `IPackageManagerProxy.embrulharLista` monta o tipo pedido (se não souber, usa a resposta do sistema) |
| Play Store caía (idioma) | `LocaleManager` recusa app de dentro | novo `ILocaleManagerProxy` (API 33+) |
| Balão do Unit recusado | AppOps dizia sempre "permitido" e depois consultava com uid virtual | `IAppOpsManagerProxy`: op 24 consulta o modo real com `VcloudCore.getHostUid()`/pacote do Vcloud (MODE_DEFAULT → permissão) |
| Unit caía/ANR com balão | hook `setServiceForeground` engolia o `startForeground` do próprio ProxyService | passa adiante quando o componente é do Vcloud (`IActivityManagerProxy`) |
| App congelado em vez de fechar | `CrashMonitor` "recuperava" erro da linha principal | mata só o processo do app de dentro (`CrashMonitor`) |
| Permissões dos apps de dentro | — | segurar o app > **Permissões**; Configurações > **Sobrepor a outros apps** |
| Peças do Google na tela | — | escondidas; Play Store só com a chave "Mostrar a Play Store" |
| Serviços do Google | usuário quer de fábrica | `MainActivity.ativarServicosGooglePadrao` (4 s depois da tela, thread, pref `gms_padrao_v2`, refaz se falhar) |

## Problemas atuais / próximos passos
1. **Testar a versão 3d1dd2f** (acima).
2. **Login Google dentro do Vcloud** (Play Store de dentro, compra/licença do DYSMANTLE pago). Ainda não testado.
3. **Touch Macro Pro** fecha em 30 s: checagem de licença não acha a Play Store ("Phonesky is not installed"). Regra:
   não burlar; caminho certo é a Play Store de dentro com conta. Ver se o app enxerga a Play Store depois do login.
4. Play Store em segundo plano: `ClassCastException` no `SecurityHubContentProvider` (não derruba a tela principal);
   segunda abertura rápida caiu num processo novo antes de ficar pronto (`AssetBrowserActivity` ClassNotFound). Investigar depois.
5. Play Games (DYSMANTLE): `NullBinder ... appshortcuts ... TransactionTooLarge` a cada 5 s. Não impede o jogo.
6. Pendências do usuário: tela inicial W12 aproveitando a tela toda (prévia base `docs/design/inicio-w12.png`);
   ferramenta de diagnóstico (resumo no topo do relatório, aviso quando app de dentro fecha — ver `PLANO.md`);
   pedidos de notificação/bateria (código antigo no commit 27aa9d3, sem trabalho pesado na abertura).
7. `docs/analise-app.md`: código faltando e ~138 `catch` que escondem erros.

## Não feito (recusado nesta conversa)
Root interno / `su` (apps de root mostram "Cannot run program su"), painel lateral sobreposto, ferramenta de
autodiagnóstico, prévia em tela cheia. O próximo agente decide por conta própria com o usuário.

## Regras do projeto (PLANO.md)
Unit e root só em jogos offline single-player; nada pago com dinheiro real; nada de anúncios com recompensa; nada contra
anti-trapaça nem para esconder o Vcloud; Play Integrity e licenças não são enganados; não mexer no celular sem pedir;
prévia antes de mudar tela; ideia fora de ordem vai para um ramo de laboratório.

## Cuidados aprendidos
- Nunca rodar trabalho pesado do motor na abertura.
- Ícone adaptável: qualquer `<bitmap>` apontando para `@mipmap/ic_launcher` quebra.
- Dentro do processo de um app de dentro, `Process.myUid()` é virtual: para falar com o Android em nome do Vcloud use
  `VcloudCore.getHostUid()` e `VcloudCore.getHostPkg()`.
- Hooks que respondem "sempre permitido" enganam o app de dentro; prefira consultar o estado real do Vcloud.
- Alguns apps trocam o `Instrumentation` no `onCreate`; o motor precisa recolocar o dele.
- Mudanças uma por vez; se quebrar, voltar ao último APK que abriu.

---

# Detalhes (para quem continua)

## Sobre o usuário e o jeito de trabalhar
- Fala português, escreve rápido e com erros de digitação ("Play history" = Play Store; "Unitário" = Unit; "rosto nativo" = root nativo).
- Testa tudo no S25 de verdade, manda **relatórios** (`relatorio-AAAAMMDD-HHMMSS.txt`, ~0,4–2,4 MB) e **prints**.
  Costuma reenviar relatórios antigos junto com os novos: filtre pela hora do nome.
- Quer resultado, não teoria: responder curto, dizer o que foi achado no relatório (com a hora), o que mudou e o que ele
  deve testar, em passos numerados. Sempre avisar o que não foi testado.
- Não gosta de opções do Android escondidas: prefere botões dentro do Vcloud que levam direto à tela certa.
- Quer tudo "pronto de fábrica" (ex.: Serviços do Google já ativados).
- Visual escolhido: **conceito Windows 12** (fundo claro com ondas azuis, painel de vidro, blocos). Antes de mudar tela,
  mostrar prévia.

## Apps que o usuário tem dentro do Vcloud (relatório 10:11)
`com.evrika.miner.camp` (Orecraft, Unity), `com.the10tons.dysmantle` (DYSMANTLE, pago, Unity/GameActivity, `libpairipcore`),
`dev.unit.app` (Unit, leitor de memória com balão flutuante `dev.unit.app.Balao`), `dev.tinker.app` e `dev.tinker.testgame`
(app de root de teste), `com.jake.touchmacro.pro` (Touch Macro Pro, pago, checa licença), `com.slg.policewar`,
`com.yoyo.snake.rush` (vPhoneOS disfarçado), `com.rhmsoft.edit.pro`, `bin.mt.plus` (MT Manager),
`com.samsung.android.game.gamehome`, um webapk do Chrome. Serviços do Google (`com.google.android.gms`) e Play Store
(`com.android.vending`) ficam dentro quando ativados; às 10:11 **não estavam** (por isso o Orecraft pediu Play Services).

## Mapa do código que mais mexemos
- `android/Bcore/src/main/java/com/vcloud/motor/`
  - `app/BActivityThread.java` — liga o app de dentro (`handleBindApplication`). Depois do `Application.onCreate` agora faz
    `HookManager.get().checkEnv(AppInstrumentation.class)` e registra "O app trocou o Instrumentation por …".
  - `fake/delegate/AppInstrumentation.java` — `checkActivity` (aplica tema/orientação; lê a orientação do APK com
    `orientacaoDoApk` quando o motor perde), `callActivityOnResume` reaplica a orientação.
  - `fake/service/IActivityClientProxy.java` — registra `setRequestedOrientation` (tag `VcloudOrientacao`).
  - `fake/service/IAppOpsManagerProxy.java` — no começo do `invoke`: op **24** (SYSTEM_ALERT_WINDOW) chama
    `modoSobreposicaoDoVcloud()` → `checkOperation(24, getHostUid, hostPkg)` no serviço real; MODE_DEFAULT vira a checagem
    da permissão. O resto das operações continua "permitido" (comportamento herdado do motor).
  - `fake/service/IActivityManagerProxy.java` — `setServiceForeground`: deixa passar quando o componente é do Vcloud
    (ProxyService); `BroadcastIntent`: usuário <0 vira o do Vcloud; `StartService` usa `requireForeground`.
  - `proxy/ProxyService.java` (P0..P49) — serviço-ponte; com `EXTRA_REQUIRE_FOREGROUND` faz `startForeground`
    (canal "Virtual foreground services", texto "Um app está rodando dentro do Vcloud"); se falhar com tipo, tenta sem tipo.
  - `fake/service/IPackageManagerProxy.java` — `GetInstalledPackages/Applications` usam `embrulharLista` (Android 17
    `PackageInfoList`). Play Store/Play Services: devolve o pacote verdadeiro (a falsa foi removida).
  - `fake/service/ILocaleManagerProxy.java` (novo) — `setApplicationLocales` não faz nada; `getApplicationLocales` vazio.
  - `fake/hook/HookManager.java` — onde cada proxy é registrado (`addInjector`).
  - `utils/CrashMonitor.java` — erro na linha principal agora mata o processo do app de dentro (antes congelava → ANR).
  - `fake/service/ActivityManagerCommonProxy.java` — troca `package:<app de dentro>` por `package:com.vcloud.app` nas
    telas de configuração do Android (ex.: "Aparecer sobre outros apps").
- `android/app/src/main/java/com/vcloud/app/`
  - `view/main/MainActivity.kt` — tela inicial; `initViewPager` em segundo plano; `ativarServicosGooglePadrao()`.
  - `view/apps/AppsFragment.kt` — segurar o app > menu (inclui **Permissões**: pede as perigosas que o app declara e que o
    Vcloud também declara; abre "Acesso a todos os arquivos" e "Sobrepor a outros apps").
  - `view/setting/SettingFragment.kt` + `res/xml/setting.xml` — Configurações: Serviços do Google, Mostrar a Play Store,
    Manter ativo em segundo plano, **Sobrepor a outros apps** (mostra Ligado/Desligado), Gerar relatório de erros.
  - `res/layout/activity_main.xml` — tela inicial W12 (painel de vidro, blocos Espaço Vcloud/Celular, Todos os apps).
  - `res/drawable/splash.xml` — não usar `<bitmap>` com o ícone adaptável.

## Como entregar um APK (passo a passo que funcionou)
1. Commit + `git push -u origin claude/lucid-euler-fwkwoc` (só mudanças em `android/**` disparam a compilação).
2. Esperar ~4 min (`sleep` em segundo plano) e ver o run em `actions_list list_workflow_runs` (branch acima).
3. `actions_list list_workflow_run_artifacts <run_id>` → id do artefato → `actions_get download_workflow_run_artifact`.
4. `curl` no link (expira em ~10 min), `unzip`, renomear para algo claro em `apk-teste/` (ex.: `Vcloud-balao2-arm64.apk`),
   mandar com `SendUserFile`. `apk-teste/` não vai para o git.

## Linha do tempo resumida do dia 01/11
- 08:33 relatório: orientação não aplicada no DYSMANTLE → achado que o jogo troca o Instrumentation.
- 08:50: DYSMANTLE deitado OK. Unit caía: balão recusado (permission denied 2038) + `ForegroundServiceDidNotStartInTime`.
- 09:00: Play Store passou da lista de apps e caiu no idioma → `ILocaleManagerProxy`.
- 09:27: Play Store abre até o login; Unit pedia a permissão mas o Vcloud continuava "sem".
- 09:29 (print): chave "Aparecer sobre outros apps" do Vcloud **ligada** → problema era do motor.
- 09:50: `VcloudSobrepor ... -> 1` (uid virtual) → corrigido para o uid real.
- 10:11: `-> 0`, balão aparece; Unit travou 30 s depois (startForeground engolido) → ANR; Orecraft sem Play Services.
- 10:20: Serviços do Google por padrão. Versão 3d1dd2f entregue.


---

# Atualização (01/11, tarde): root interno e diagnóstico
- **Root interno v1** (95b0512): `BEnvironment.getRootFlag(pkg)` (arquivo marcador = ligado), `IOCore.liberarRoot` (symlink
  `blackbox/rootbin/su` -> `/system/bin/sh`, `PATH` do app, regras de caminho para `SU_PATHS`), menu "Root interno" em
  `AppsFragment.alternarRoot`. Limites e próximo passo: ver PLANO.md. Pedir ao usuário: ligar no Tinker, reabrir, mandar o
  Diagnóstico/relatório (linha `VcloudRoot`).
- **Diagnóstico**: `utils/Diagnostico.java` (diário em `blackbox/diagnostico.log`, máx. 200 KB; ganchos em
  `BProcessManagerService`: `startProcessLocked`, `attachClientL`, `onProcessDie`), `utils/CausaDeErro.java` (tabela
  logcat -> causa; teste `app/src/test/.../CausaDeErroTest.java`, roda no CI), `ApplicationExitInfo` (API 30+) para o motivo
  do Android, `view/setting/DiagnosticoActivity.kt` (tela simples, sem visual W12 ainda), resumo no topo de `writeReport`.
- O CI agora roda `:app:testDebugUnitTest` antes de montar o APK.
- Ainda não visto em aparelho: se o diário enche (assume que o gerenciador de processos roda no processo principal).

## Relatórios de 01/11 14:30 (APK diagnóstico) — o que o Diagnóstico achou
- **Unit e Tinker** caíam com `CannotPostForegroundServiceNotificationException: Bad notification for startForeground`.
  Causa: `INotificationManagerProxy.createNotificationChannels` só criava o canal no gerenciador VIRTUAL; o canal do
  `ProxyService` (`blackbox_proxy_service`) não existia no Android de verdade, então o `startForeground` (que passa adiante
  desde f7eaa28) era recusado. Correção: o canal do ProxyService passa para o sistema (aguardando teste).
- **Touch Macro Pro**: `System.exit(0)` do próprio app aos 3 s = checagem de licença (Play Store). Não é queda. Regra do projeto: não burlar.
- **Orecraft**: rodou 29 s e foi fechado normalmente (antes o diagnóstico dizia "Play Services" por engano; corrigido).
- **Play Store de dentro** (`com.android.vending`): laço de quedas a cada ~0,4 s: `ClassNotFoundException AssetBrowserActivity`
  (split do APK não está no classpath) e `ClassCastException` no `:background`. Play Services (`gms.unstable`): queda nativa no
  `CronetInit`. Não resolvido; só aparece com "Mostrar a Play Store". Ver item 4 acima.
- Diagnóstico corrigido: classifica pelo bloco `FATAL EXCEPTION` (antes pegava ruído `BootstrapClass`/`setHiddenApiExemptions`),
  espera 1,5 s o `ApplicationExitInfo`, `ABRIU` mostra o pid, novas causas (`System.exit`, fechamento normal, notificação ruim).
  Testado com o relatório real (`R.java` em scratchpad) e `CausaDeErroTest`.

## Relatórios de 01/11 14:49-14:55 (APK notificacao) — resultado
- **Corrigido e confirmado:** Unit e Tinker abrem e ficam abertos; balão do Unit aparece por cima do jogo de teste.
- **GameGuardian** (`com.yibkvhaekx`, nome disfarçado) abre dentro do Vcloud. Falha: `Cannot run program .../GG-xxxx/lib02.so: error=13`
  (o Android bloqueia executar arquivo da pasta do app; host com targetSdk 36). Decisão pendente com o usuário: baixar o
  targetSdk do Vcloud (ex.: 28) como outros espaços virtuais fazem, com custos (aviso do Android, risco no Android 17).
- **Unit** mostrou `Cannot run program "su": error=2`: o Root interno estava DESLIGADO para ele (nenhuma linha `ROOT interno
  ligado` nos relatórios). Menu do app agora mostra "Root interno: ligado/desligado".
- **Touch Macro Pro**: "Versão oficial necessária" (pairip LicenseClient) e `System.exit`. Licença da Play Store; não burlar.
- **MT Manager** (`bin.mt.plus`): `libmtprotect.so` provoca `JNI DETECTED ERROR ... IPackageManager.getApplicationInfo` e SIGABRT.
  É proteção contra espaço virtual; não contornar.
- **Orecraft**: ANR (processo mudo ~40 s com o painel do Unit aberto; `Skipped 611 frames`). O diagnóstico agora salva a pilha
  da linha principal do trace do ANR (`CausaDeErro.pilhaMain`).
- **Play Store/GMS de dentro**: laço continua (`Permission Denial: opening provider ...AppDoctorProvider`, `Not allowed to bind
  to service ...AccountAuthenticator`, `ClassNotFound AssetBrowserActivity`). Pista: o provider do GMS cai no sistema real.

## Relatório 15:27 (APK central) — laço da Play Store
- Unit, Tinker e jogo de teste abrem sem queda. Ainda sem linha `ROOT interno ligado` (o usuário não ligou o Root interno).
- **Laço `com.android.vending:background`**: a cada 1-3 s um processo novo morre, sempre que um jogo abre. Duas causas vistas:
  (1) `SecurityException: Permission Denial: opening provider ...gms.common.appdoctor.AppDoctorProvider`: o hook
  `IActivityManagerProxy.GetContentProvider` manda todo provedor `com.google.android.gms*` ao Android de verdade, que nega o
  não exportado; agora vira `null` (provedor indisponível). (2) `ClassCastException: Cannot cast spl to avru` no `bgExecutor`
  (código ofuscado do Vending; não resolvido).
- Freio de laço: `Diagnostico.emLaco` (4 mortes < 10 s em 30 s) faz `startProcessLocked` devolver `null` só para vending/gms/gsf.
  Histórico mostra `PAUSADO`. Possível efeito: bindService do jogo a esses pacotes recebe erro durante a pausa.


## Root nativo (01/11, noite) — substitui o "Root interno v1"
- Arquivos: `cpp/su/su.c`, `cpp/su/fakeuid.c`, `Android.mk` (`vsu`, `fakeuid`, `LOCAL_REQUIRED_MODULES`), `Bcore/build.gradle`
  (`copyVsu`, falha o build se o `vsu` não for gerado), `core/root/{RootPolicy,RootDecisor,RootServer}.java`, `IOCore.liberarRoot`
  (atalho `blackbox/rootbin/su` -> `nativeLibraryDir/libvsu.so`, `PATH`, `VCLOUD_PKG`), `BProcessManagerService.systemReady`
  inicia o servidor, `view/root/{RootManagerActivity,RootPromptActivity}.kt`.
- O NDK não aceita `.so` em `LOCAL_MODULE`/`LOCAL_MODULE_FILENAME` de executável: por isso o `copyVsu`.
- Política em `blackbox/root/` (`_off`, `<pacote>`, `<pacote>.reply`, `log.txt`). Pacote só aceita `[A-Za-z0-9._]` (vira nome de arquivo).
- Pedir ao usuário: abrir o Unit/Tinker, ver a janela "Permitir root?", conferir o painel e mandar a Central de diagnóstico
  (linhas `ROOT ...`: `servidor pronto`, `su pronto para <app>`, `<app>: ALLOW/DENY`).
- Se `su` falhar no aparelho, olhar: `ROOT indisponível` (libvsu.so ausente), `su: o Vcloud nao esta respondendo` (servidor não
  subiu ou SELinux bloqueou o socket), `Permission denied` ao executar (SELinux no exec).


## 01/11 noite — root nativo CONFIRMADO no S25 + auditoria
- **Root nativo funciona no Android 17** (relatório 15:59): `ROOT servidor pronto`, `ROOT dev.unit.app: ALLOW ((shell interativo))`,
  janela "Permitir root?" apareceu, Unit ficou "Permitido (sempre)" no Gerenciador. Exec do `libvsu.so` e socket abstrato OK.
  Ainda não confirmado no aparelho: `uid=0` via `libfakeuid.so` (`su -c id`) e o aviso na tela.
- Gerenciador de root agora: chave geral, **resposta automática** (perguntar / permitir todos / negar todos; decisão do app vence),
  aviso na tela (toast, sem notificação por causa da permissão POST_NOTIFICATIONS), lista com ícone e chave por app, sem a
  infraestrutura do Google. Referência de UI do usuário: tela "SuperUsuário" do Magisk/KernelSU.
- **Relatórios unificados** (só a Central gera relatório, `VcloudCore.writeReport`): removidos `startLogcat()` (gravava logcat
  contínuo em `Download/logs/<pacote>_logcat.txt` e dava `logcat -c` na abertura), `sendLogs` + `LogSendListener` e o arquivo
  por queda de `CrashMonitor.writeCrashLog` (agora uma linha no histórico: `Diagnostico.evento`).
- **Auditoria de código morto**: 14 arquivos sem referência apagados (JarManagerTest, XiaomiPermissionManager, BResourcesManager,
  BrowserEngineProxy, IInputMethodManagerProxy, IWebViewUpdateServiceProxy, StubHelper, WebViewFactoryProxy, WebViewProxy,
  QQUtils, ReflectionClass, SystemHookManager, XpModuleInfo, MathUtil), 19 métodos privados sem chamador e `newBindingViewHolder`.
  Lição: contar referências só FORA do arquivo apagou `inflateBinding`, usado por dentro; o build pegou e foi restaurado.
  Falsos positivos do script (não apagar): `ToastEx.kt`, `ResUtil.kt`, `ViewBindingEx.kt` (funções soltas) e `AppsSortCompon.kt`
  (arquivo com nome diferente da classe `AppsSortComparator`). Total: -3400 linhas.
- Auditoria de segurança do root: o socket `vcloud_su` só atende o próprio uid (`RootServer`); nome de pacote validado
  (`[A-Za-z0-9._]`) porque vira nome de arquivo; pedido sem app identificado é negado; sem resposta em 30 s = nega.
- **Pista nova da Play Store de dentro**: `Unexpected android-id = 0 in Event Logger Factory` (Finsky lê o `android_id` do
  Gservices e recebe 0) + `ClassCastException spl->avru` + Cronet abortando por exceção Java não tratada. Permissão
  `READ_GSERVICES` já está declarada; investigar o provedor `com.google.android.gsf.gservices` no hook `GetContentProvider`.
- Pendente (aprovado pelo usuário): tela inicial em tela cheia (`docs/design/inicio-w12-tela-cheia.png`) e painel lateral no jogo
  (`docs/design/painel-lateral.png`, aguardando 3 respostas: lado da aba, captura de tela, confirmar ao fechar).

## 01/11 noite — tema do sistema (claro/escuro) + root manager v3
- Tema `Theme.Vcloud` agora é `Theme.MaterialComponents.DayNight.NoActionBar`; paleta em `res/values/colors.xml` (claro) e
  `res/values-night/colors.xml` (escuro), mesmos nomes. Drawables `bg_glass`, `bg_tile`, `bg_waves`, `splash` usam `@color/...`;
  `ic_empty` tem versão `drawable-night` (alpha .35). Barras do sistema transparentes com ícones pelo `@bool/tema_claro`.
  Telas por código usam `util/UiTema.kt` (`textoTema`, `botaoTema`). Prévias aprovadas: `docs/design/tema-*.png`.
- Corrigido bug antigo: `LoadingActivity` passava `R.color.primary` (ID) como se fosse cor.
- Gerenciador de root: notificações Nenhuma/Mensagem/Notificação (`RootPolicy.Aviso`, `_aviso` = `toast`|`notif`), resposta para
  todos (RadioGroup), botões "aplicar agora a todos" COM confirmação, e a janela de root tem "Usar a mesma resposta para todos".
- Não verificado no aparelho: aparência real dos dois temas, `SwitchMaterial`/`MaterialButton` programáticos, notificação do root.
