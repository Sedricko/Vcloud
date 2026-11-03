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
- (ERRADO, revertido às 16:45) `LoadingActivity` passava `R.color.primary` (ID) e NÃO era bug: `CatLoadingView.setBackgroundColor` espera o ID do recurso. Passar o valor da cor causou `Resources$NotFoundException: Resource ID #0xff12172a` (16x no relatório 16:39), que derrubava a pausa da MainActivity ao abrir um app. Lição: ler o código da biblioteca antes de "corrigir" uma chamada.
- Gerenciador de root: notificações Nenhuma/Mensagem/Notificação (`RootPolicy.Aviso`, `_aviso` = `toast`|`notif`), resposta para
  todos (RadioGroup), botões "aplicar agora a todos" COM confirmação, e a janela de root tem "Usar a mesma resposta para todos".
- Não verificado no aparelho: aparência real dos dois temas, `SwitchMaterial`/`MaterialButton` programáticos, notificação do root.

## 01/11 noite — correção: tema claro volta ao visual ORIGINAL
- O usuário mandou print do visual original (vidro escuro sobre ondas claras, texto branco). A paleta clara tinha virado vidro
  branco, o que fugia do design ativo. Agora `values/colors.xml` (claro) tem os valores originais; `values-night` mantém o escuro.
  REGRA: o tema do sistema alterna claro/escuro, mas o claro é o visual original; não redesenhar sem prévia aprovada.
- Fundo de ondas virou `window.setBackgroundDrawableResource` (vai até as bordas, atrás das barras do sistema); a barra do topo
  da tela inicial usa as cores do vidro (`glass_text`).
- As prévias `docs/design/tema-inicio.png` e `amostra-*.png` mostram o claro ANTIGO (vidro branco): estão superadas.
- Pendente: layout em tela cheia aprovado (`inicio-w12-tela-cheia.png`: busca, Fixados, barra de baixo, usuário/desligar) NÃO foi
  programado; só o tema.

## 01/11 16:45 — relatório 16:39
- Root OK no Tinker (`ROOT dev.tinker.app: ALLOW`). Regressão minha corrigida (CatLoadingView, acima).
- O histórico estava poluído por `ERRO JavaException em null` repetido: agora `Diagnostico.erroCapturado` junta o mesmo erro por 30 s e escreve
  "(o próprio Vcloud)" quando não há pacote.

## 01/11 noite — painel lateral sobre a tela cheia (implementado, NÃO testado no aparelho)
- `view/painel/PainelLateral.kt` (roda só no processo `:black`, iniciado em `App.onCreate`): a cada 1 s vê se um processo
  `com.vcloud.app:pN` está em primeiro plano (`IMPORTANCE_FOREGROUND`); se sim e `canDrawOverlays`, mostra a aba (arrastável,
  lado e altura salvos em prefs `painel_lateral`); tocar abre painel em tela cheia com fundo escurecido (toque fora fecha).
  Itens: Minimizar (volta à MainActivity, não à tela do celular), Capturar tela, Trocar de app (apps abertos, `launchApk`),
  Configurações, Gerenciador de root, Diagnóstico, Fechar app (com confirmação; abre a MainActivity antes de `stopPackage`).
- Captura: `CapturaActivity` pede a permissão do Android (ela pergunta a CADA captura) e `CapturaService` (FGS mediaProjection)
  salva PNG em `Pictures/Vcloud`. A aba some 15 s para não sair na foto.
- Ações abrem a tela ANTES de fechar o painel: o Android só deixa serviço em segundo plano abrir tela com janela visível.
- Riscos a verificar no S25: `IMPORTANCE_FOREGROUND` do processo guest com jogo na frente; abrir activity a partir do `:black`;
  a permissão de captura; Unity pausar com o painel aberto (a janela não toma foco, então não deveria).

## 01/11 noite — janelas flutuantes não podem sair do Vcloud
- Print do usuário: o painel do Unit (janela `TYPE_APPLICATION_OVERLAY` do app de dentro) aparecia sobre o app do Claude. Janela de
  sistema aparece sobre qualquer app. Correção: `utils/GuardaSobreposicao.java`. O `:black` (`PainelLateral.tique`) escreve o sinal
  `blackbox/fora_do_vcloud` (existe = nenhum processo `com.vcloud.app*` com `IMPORTANCE_FOREGROUND`); cada app de dentro
  (iniciado em `BActivityThread.handleBindApplication`) lê o sinal a cada 1 s e põe `INVISIBLE` nas janelas tipo 2038/2002/2003 de
  `WindowManagerGlobal.mViews` (reflexão; só devolve as que ele mesmo escondeu). Histórico mostra `SOBREPOSIÇÃO escondida/liberada`.
- NÃO testado no aparelho. Risco: reflexão em `WindowManagerGlobal` no Android 17 (APK debuggable ignora a restrição de API oculta).
- Pendente: os botões do painel lateral abrem OUTRAS telas (saem do jogo). Nos emuladores (vPhoneOS/Virtual Master) a barra é parte
  da própria tela do emulador, por isso nunca sai. Os APKs deles não estão nesta sessão; aguardando decisão do usuário.

## 01/11 noite — painel lateral v2 (voltar/girar/gestos) — implementado, NÃO testado no aparelho
- Novo canal host -> app de dentro: `IBActivityThread.executarComando(int)` (AIDL) -> `BActivityThread` -> `utils/ComandoDoPainel`
  (`VOLTAR` = `dispatchKeyEvent` BACK na tela do topo; `GIRAR` = alterna `setRequestedOrientation`). O topo é registrado em
  `AppInstrumentation.callActivityOnResume/Pause`. Voltar/Girar agem só DENTRO do app.
- `PainelLateral`: gestos na aba (1 toque = painel, 2 = Voltar, 3 = Apps recentes, segurar = Início do Vcloud; espera 300 ms),
  linha "Root do Vcloud" com chave, "Ocultar painel", aba com opacidade (`PainelConfig`: Normal/Discreta/Quase invisível;
  arquivos em `blackbox/painel/`) e Configurações > "Painel lateral" (mostrar/ocultar e opacidade).
- NÃO feito: "Bloquear gesto de voltar" (precisa interceptar a entrada de voltar do app; sem gancho simples) e personalizar menu.
- Pendente: analisar X8 Sandbox (modo PIP, 17 partes .7z) enviado pelo usuário; opções 3 (personalizar painel) e 4 (manter apps vivos).

## 01/11 noite — Manter apps vivos (implementado, NÃO testado no aparelho)
- `view/vivos/{VivosActivity,VivosFragment}.kt` + `res/xml/vivos.xml` (PreferenceScreen, mesmo molde das Configurações; entrada em
  Configurações > "Manter apps vivos"). Linhas: restrição de bateria (`PowerManager.isIgnoringBatteryOptimizations`; botão abre o pedido
  do Android; permissão `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), limite de processos fantasma (lê `Settings.Global
  settings_enable_monitor_phantom_procs`; se não conseguir ler diz "não consegui verificar"), encerrados pelo Android
  (`CausaDeErro.contarEncerradosPeloAndroid` no histórico) e atalho para a chave de segundo plano.
- Pedido do usuário: SEM ADB/comandos; levar direto à opção. O botão abre `ACTION_APPLICATION_DEVELOPMENT_SETTINGS` com os extras
  `:settings:fragment_args_key` = `disable_phantom_process_monitor` (chave do AOSP, lida do código-fonte via WebFetch; não confirmada
  no One UI). Se as Opções do desenvolvedor estão desligadas, abre "Sobre o telefone" (`ACTION_DEVICE_INFO_SETTINGS`).
- Regra de design reforçada: telas novas seguem o padrão nativo das Configurações (linhas título+resumo, sem ícones/cartões coloridos).

## 01/11 18:34 — histórico do usuário (Central)
- Confirmado no aparelho: root (`ROOT dev.unit.app/dev.tinker.app: ALLOW`), guarda de sobreposição (`SOBREPOSIÇÃO escondida/liberada`),
  freio de laço (`PAUSADO com.android.vending`).
- Play Store de dentro: `Unexpected android-id = 0 in Event Logger Factory` + `Unable to fetch checkin consistency token: empty token`
  (relatórios anteriores) => o Google Play Services de dentro não faz o "checkin" e a Play Store fica sem identificador; o erro em
  `libhttpengine.so` (HttpEngine do Android) é consequência. Resolver = fazer o GMS de dentro registrar o aparelho (grande e incerto).
  Decisão pendente do usuário: perseguir isso ou deixar a Play Store de dentro desativada.
- Histórico: erros agora em UMA linha (`CausaDeErro.resumirMensagem`) e erros repetidos de pacotes do Google no máximo 1x/5 min.

## 01/11 noite — conserto da Play Store: etapa 1 (medir)
- Hipóteses: (a) a consulta ao Gservices volta sem a linha `android_id`; (b) falha por outro caminho. `ContentProviderStub.invoke` engole
  erros de uid/permissão e devolve vazio (agora vira evento `PROVEDOR erro engolido` no histórico). NÃO havia essas linhas no logcat antigo.
- `utils/ProbeGoogle.java`: `consultarAndroidId` (mesma consulta da Play Store: `content://com.google.android.gsf.gservices`, arg `android_id`),
  `relatorio` (permissão READ_GSERVICES, ANDROID_ID, versões GSF/GMS/Play Store no celular e dentro do Vcloud) e `medirDentro`
  (roda 3 s depois de abrir vending/gms e escreve `GOOGLE dentro de <pkg>: Gservices -> ...`). Central ganhou "Testar Google".
- Próximo: com a resposta, escolher o conserto (identidade da consulta, ANDROID_ID, checkin do GSF).

## Google completo dentro do Vcloud (experimental)
- Chave em Ajustes: "Google completo dentro do Vcloud" (arquivo `blackbox/google_completo`; desligada = como antes).
- Ligada: os provedores do Google (gms/gsf/vending) pedidos por apps de dentro usam a cópia de dentro quando ela existe; senão caem no celular (anotado no histórico: `GOOGLE provedor ...`).
- Não testado no aparelho. Incógnitas: checkin do GMS de dentro como app comum, login de conta, licenças (não se burla). Teste: ligar, abrir a Play Store de dentro, Central > Testar Google > Copiar.

## Regra: versão do app
- A cada APK entregue ao usuário, subir `versionCode`/`versionName` em `android/build.gradle` (o instalador mostra "já está instalada" se for igual). Atual: 4.0.35 (435).

## Play Store de dentro: erro de uid no provedor (4.0.2)
- Relatório 19:33: `PROVEDOR erro engolido (query) em gms: Calling uid: 10014 doesn't match source uid: 10454` (modo Google completo ligado, caminho de dentro ativo).
- Causa: o gancho nativo do Binder devolve o uid virtual de quem chama (10014), mas a origem levava o uid do celular (10454). Correção: `ContentProviderStub.fixarOrigem` põe o uid virtual do chamador na origem. Não testado no aparelho.
- Ainda aberto: `android-id = 0` no Finsky e `AssetBrowserActivity` "Unable to instantiate" (classe não achada na cópia da Play Store) — próximo passo.

## 4.0.3
- Relatório 19:39: o ajuste de uid da 4.0.2 resolveu o provedor do gms, mas inverteu no provedor do próprio Vcloud (`Calling uid 10454 != source 10014`). Regra: provedor do Vcloud (processo real) usa uid do celular; provedor de dentro usa uid virtual do chamador.
- `android_id=null` no Gservices continua (sem checkin do GMS de dentro). `AssetBrowserActivity` não instancia: só o APK base é instalado por cópia (sem splits); 4.0.3 registra no histórico o motivo real (`ATIVIDADE não instanciou`).

## 4.0.4
- Relatório 19:47: `AssetBrowserActivity` = ClassNotFoundException mesmo com base+splits no loader → é um `activity-alias` (sem classe). `AppInstrumentation.newActivity` agora abre o `targetActivity` do alias (histórico: `ATIVIDADE alias X -> Y`). Não testado.
- No mesmo relatório o modo "Google completo" aparecia desligado (a chave pode ter sido desligada). `android_id=null` até no Google do celular real pelo Gservices (sonda pode estar sem permissão efetiva); `android-id = 0` no Finsky segue aberto — não vou inventar um id falso.

## 4.0.5
- Relatório 19:55 (modo ligado): sem mais `AssetBrowserActivity` nem erro de uid nos provedores; provedores do Google resolvem "cópia de dentro" (gservices, chimera, fonts, appdoctor).
- Queda do gms/vending: `Cronet(HttpEngine) -> ConnectivityManager.getNetworkInfoForUid` com uid virtual → `SecurityException` (precisa ACCESS_DOWNLOAD_MANAGER/NETWORK_STACK) → abort. Correção: `UidMethodProxy("getNetworkInfoForUid", 1)` em `IConnectivityManagerProxy` (uid virtual → uid do celular). Não testado.
- Segue aberto: `android-id = 0` (checkin do GMS de dentro).

## 4.0.6
- Relatório 20:02 (4.0.5): a Play Store de dentro ABRE (tela "Fazer login"); Cronet não derruba mais.
- Uid do provedor: `callingBUid` é gravado só quando o processo nasce, então o "uid de quem chamou" fica velho (`10015 != 10014`). Agora a origem leva o uid do celular e `NativeCore.getCallingUid` devolve o uid do celular quando a pilha está em `AttributionSource`/`ContentProvider$Transport`. Não testado.
- ABERTO (grande): "Fazer login" → `BAccountManagerService.bindToAuthenticator` faz `bindService` no processo `:black` direto no AMS real, que recusa (`Not allowed to bind ... GoogleAccountAuthenticatorService`). Precisa ligar o bind pelo AMS virtual (intent de proxy). Decisão pendente do usuário.

## 4.0.7
- Relatório 20:20 (4.0.6): sem erros de uid; só o login falha (`Not allowed to bind ... GoogleAccountAuthenticatorService`). O logcat do relatório não traz o processo `:black`, e `BindServiceCommon` engole a falha do bind virtual e cai no AMS real. 4.0.7 só registra o motivo no histórico (`BIND virtual falhou ...` / `BIND sem serviço de dentro ...`). Correção do login vem depois de ler isso.

## 4.0.8
- Relatório 20:46 (4.0.7): nenhuma linha `BIND ...`. "Not allowed to bind to service" é o `ContextImpl.bindServiceCommon` quando o AMS devolve `res < 0` (não é exceção do AMS): o bind chegou ao Android e foi recusado, sem passar pelos casos já registrados. 4.0.8 registra todo bind do gms no `:black` (`BIND pedido`), o resultado negativo (`BIND recusado ... proxy=`) e a falta de intent de proxy.

## 4.0.9
- Relatório 21:02 (4.0.8): o gancho de bind registra os binds do gms (`BIND pedido ... de dentro=true`), mas NENHUM para `GoogleAccountAuthenticatorService`: o bind do login, feito pelo `BAccountManagerService` no `:black`, não passa pelo gancho e cai no Android real (recusado).
- `bindToAuthenticator` agora pede o intent-ponte direto ao `BActivityManagerService` e faz o bind nele (histórico: `LOGIN bind do autenticador ... -> ponte=...`). Sem `binder` no registro (o unbind não desconta contagem). Não testado.

## 4.0.10
- Relatório 21:18 (4.0.9): o bind do login FUNCIONOU (`LOGIN bind ... -> ponte=ProxyService$P1`, a Play Store não fecha mais). Agora o autenticador do gms recusa `addAccount`: `SecurityException: Access denied, requires android.permission.ACCOUNT_MANAGER` (Android 17: `PermissionEnforcer`), porque quem chama é o uid do Vcloud.
- 4.0.10: `PermissionCheckerProxy` (serviço `permission_checker`) e ganchos `checkPermission/checkUidPermission` em `IPermissionManagerProxy` concedem SÓ `ACCOUNT_MANAGER`. Não sei qual dos caminhos o Android 17 usa; histórico: `LOGIN permissão ACCOUNT_MANAGER concedida (permission_checker)` quando o primeiro for o usado.
- Também visto: `GservicesPersistence: Package com.vcloud.app is not approved to access the device ID` (o gms de dentro recusa o uid do Vcloud) → ligado ao `android_id=null`; próximo.

## 4.0.11
- Relatório 21:24 (4.0.10): `Access denied, requires ACCOUNT_MANAGER` continua e a linha `permissão concedida` não apareceu: os ganchos de `permission_checker`/`permissionmgr` não são o caminho do Android 17 → removidos.
- Agora: `NativeCore.getCallingUid` devolve o uid do sistema só dentro de `android.accounts.*_enforcePermission` (o autenticador confere `checkPermission(perm, pid, getCallingUid())`; uid 1000 passa). Não testado.
- Segue: `GservicesPersistence: Package com.vcloud.app is not approved to access the device ID` e `android_id=null` / `Unexpected android-id = 0` no vending:background.

## 4.0.12 (causa real do ACCOUNT_MANAGER, lida no código-fonte do Android)
- `PermissionEnforcer.enforcePermission(perm,pid,uid)` (sem app-op) → `Context.checkPermission` → `PermissionManager.checkPermission(perm,pid,uid,deviceId)` → cache → `IActivityManager.checkPermissionForDevice(...)` (uid root/sistema passa no cliente). Fontes: AOSP `PermissionEnforcer.java`, `ContextImpl.java`, `PermissionManager.java` (branch main; o Android 17 do aparelho pode diferir em detalhes).
- O gancho do motor era só `checkPermission` → nunca pegava `checkPermissionForDevice`. 4.0.12 adiciona `checkPermissionForDevice` (mesma regra; uid está no índice 2) e REMOVE o remendo do uid de sistema da 4.0.11 e os ganchos inúteis da 4.0.10.
- Lição: ler o código do Android antes de mexer (regra do usuário: "analise, não chute").

## 4.0.13 (lido no relatório 22:10, app 4.0.12)
- O botão "Fazer login" FUNCIONA: várias `call addAccount`; `Access denied ... ACCOUNT_MANAGER` = 0 → o gancho `checkPermissionForDevice` resolveu.
- Próxima barreira (log): `AccountAuthenticator addAccount(com.google)` → `SecurityException: Only system may: get application restrictions for other user/app com.google.android.gms` (`IUserManager.getApplicationRestrictionsForUser`, `UserManagerService.checkSystemOrRoot`) → Finsky `AuthenticatorException: addAccount failed`.
- Causa: `IUserManagerProxy` tinha o gancho `getApplicationRestrictions` (nome antigo); o método real é `getApplicationRestrictionsForUser(pkg, userId)`. 4.0.13 adiciona o gancho com o nome certo (pacote e userId do Vcloud). Não testado.
- Também no log: `GservicesPersistence: Package com.vcloud.app is not approved to access the device ID` (ligado ao android_id) — ainda aberto.
- Processo de UI do login: `com.google.android.gms.ui` (p2) já sobe ("Prewarming auth UI process").

## 4.0.14 (relatório 22:52, app 4.0.13)
- `getApplicationRestrictionsForUser` resolveu: o addAccount agora chega ao autenticador, o processo da tela de login (`gms.ui`) sobe e o WebView é pré-aquecido.
- Nova barreira (log, gms pid p1): `AccountAuthenticator addAccount(com.google)` → `SecurityException: Only 0p apps are allowed to call this API` logo depois de `PackageManagerStub: 0 , com.google.android.gms GetPackagesForUid: [com.google.android.gms]` (o gms perguntou pelo uid de quem chama e recebeu 0). Código ofuscado (`atmv.i`); não achei a regra em fonte pública — hipótese: só aceita sistema/Google (o AccountManagerService do sistema chama como uid 1000).
- 4.0.14 (hipótese): `NativeCore.getCallingUid` devolve 1000 só no gms e só dentro de `AbstractAccountAuthenticator$Transport`; `GetPackagesForUid` passa uids < 10000 ao Android real (1000 → "android"). Histórico: `LOGIN chamador do autenticador: uid X -> sistema`.
- Aberto ainda: `BIND sem serviço de dentro para com.google.android.webview/...SandboxedProcessService0`; `Unable to load font Google Sans` (fonts provider); `android-id = 0`/`not approved to access the device ID`.

## 4.0.15 — Auto diagnóstico
- Pedido do usuário: "auto diagnóstico que lê tudo no Vcloud e tudo que acontece dentro dele".
- `utils/AutoAnalise.java` (Java puro, testado em `AutoAnaliseTest`, 7 testes): lê o logcat inteiro (todos os processos do Vcloud: tela, `:black` e apps de dentro) + o histórico; agrupa por regra, dá nome ao processo (pid → pacote), marca `[novo]` (últimos 10 min do log) e mostra causa, o que fazer e a linha de prova. Regras vêm de falhas reais: permissão de sistema negada, "Only system may", "Only 0p apps", bind recusado, uid de origem, android-id 0, tela não instanciou, JNI/proteção, queda nativa, ANR, fonte, provedor, etc. Erros sem regra: só os 8 mais novos.
- `utils/AutoDiagnostico.java`: leitura do estado agora (versão, chaves Google completo/root/painel, sobreposição, bateria, espaço livre, apps instalados dentro, processos do Vcloud rodando) + a análise.
- `VcloudCore.writeReport`: o logcat é lido na memória (limite 24 MB) e o auto diagnóstico vai no TOPO do relatório (antes do resumo e do logcat). Sem mudança de tela (o botão "Gerar relatório" já existente passa a incluí-lo).
- Validado em relatório real (22:52): achou android-id 0, "Only 0p", provedor de fonte, bind sem serviço (webview) e um achado novo: `SecurityException: Package com.android.vending does not belong to 10454` (pacote/uid — relevante ao android-id).
- Não feito: botão/tela própria na Central (precisa de prévia); vigia contínuo em segundo plano.

## 4.0.16 (relatório 23:24, app 4.0.15 — o primeiro com o auto diagnóstico no topo)
- Auto diagnóstico funcionou: o topo do relatório já mostra estado + problemas agrupados.
- Achados reais:
  1. `Unknown calling package name 'com.android.vending'` (FATAL, derruba a Play Store; vem do módulo de medição do gms) e `Only 0p apps` na checagem do `DefaultAuthDelegate`: o gms vê o chamador errado. Causa: `AppConfig.callingBUid` é gravado UMA vez quando o processo nasce (quem criou o processo) e `NativeCore.getCallingUid` o devolve para toda chamada.
  2. `IPackageInstaller.getMySessions` → `Package com.android.vending does not belong to 10454` (nome do app não bate com o uid do Vcloud).
  3. `LOGIN chamador ... -> sistema (1000)` rodou, mas o `GetPackagesForUid` seguinte perguntou pelo uid do celular (10454→10015): a checagem `0p` usa outro caminho.
- 4.0.16: (1) `QuemChama` (arquivos `pids/PID` = uid virtual, gravados pelo servidor ao iniciar/limpos ao morrer/ao ligar) + `NativeCore.getCallingUid` responde o dono do `Binder.getCallingPid()` (cache de 3 s); fica por último o comportamento antigo. (2) `getPackageInstaller` devolve um proxy que troca o nome do app pelo do Vcloud. (3) diagnóstico: `UID perguntou pacotes do uid do celular ... quem perguntou: <pilha>` (gms) para descobrir quem faz a checagem `0p`. (4) `AutoAnalise` ganhou as regras "does not belong to" e "Unknown calling package name".
- Não testado no aparelho. Risco: o uid de quem chama agora muda em TODO app de dentro que recebe chamadas de outro app de dentro.

## 4.0.17 — levantamento "o que o Google exige x o que existe dentro"
- Pergunta do usuário: "Analisou se todos os serviços que o app exige estão disponíveis na instância?" → resposta honesta: até aqui só por falha isolada. Agora há a seção `REQUISITOS DO GOOGLE` no relatório (`AutoDiagnostico.requisitosGoogle`): pacotes (celular x dentro, com aviso "EXISTE NO CELULAR E FALTA DENTRO"), tipos de conta registrados dentro, provedores e serviços (ações) que a Play Store procura, telas/serviços de login do gms de dentro (nome, exportado, ligado/permissão).
- Play Store 53.8.11-34 (APK enviado pelo usuário, analisada só por leitura): sem sharedUserId; `uses-library` todas `required=false`; `queries` só maps/CustomTabs/setupwizard; pede permissões de sistema (INSTALL_PACKAGES, MANAGE_USERS, GET_ACCOUNTS_PRIVILEGED...) que a cópia não recebe (não são do login). Strings do código: `com.google.android.gsf.action.GET_GLS` (login do gsf), `content://com.google.android.gsf.gservices` (+ `killswitch_to_allow_zero_android_id`), `com.google.android.gms.checkin.BIND_TO_SERVICE`, `"The com.google.android.gms.auth.accounts provider is not available."`, `getMySessions`, `READ_GSERVICES`.
- A recusa `Only 0p apps` está no gms, não na Play Store: a resposta definitiva precisa do APK do gms 26.43.30 (peça ao usuário).

## 4.0.18 (relatório 23:56, app 4.0.17)
- `REQUISITOS DO GOOGLE` (primeira leitura): faltam dentro `com.google.android.webview` (existe no celular), e não existem dentro: serviço `GET_GLS` (gsf login) e `aang.events.services.START`. Provedores (`auth.accounts`, `gservices`, `chimera`, `phenotype`, `fonts`, `appdoctor`) existem. As linhas "Google Play Services de dentro: 0 telas..." e "Tipos de conta: NENHUM" NÃO são confiáveis (lidas da tela principal; os guests veem os componentes) — não tirar conclusão delas.
- `UID perguntou pacotes do uid do celular ... AbstractGmsTracer` = rastreio do gms (inofensivo).
- `Only 0p apps`: logo antes (`DefaultAuthDelegateChimeraService`) o gms perguntou os pacotes do uid 10025 = ele mesmo, com assinatura do Google (`VcloudAssinatura ... signatures=1`) e foi recusado → "0p" não é só assinatura: exige app de sistema. A cópia nasce de um APK comum sem `FLAG_SYSTEM`.
- 4.0.18 (hipótese, ainda sem prova): `PackageManagerCompat.marcarComoDoCelular` copia do celular os bits públicos FLAG_SYSTEM/FLAG_UPDATED_SYSTEM_APP para os pacotes Google de dentro (`GmsCore.isGoogleAppOrService`). Histórico: `GOOGLE <pkg> marcado como no celular`. Não mexe em privateFlags (privilegiado).

## 4.0.19 — regra do "0p" lida no código do Google Play Services 26.43.30 (APK enviado pelo usuário, só leitura)
- Erro: `atmv.i(Context)` (DefaultAuthDelegateChimeraService) → `if (!bteg.U(ctx)) throw SecurityException("Only 0p apps...")`; `bteg.U(ctx) = dodv.a(ctx, Binder.getCallingUid())`.
- `dodv.a(ctx, uid)`: **`uid == Process.myUid()` → true**; senão `dodv.c(ctx, uid)`: cache de uids aprovados; senão `getPackagesForUid(uid)` e algum pacote na lista `donn.a` {com.google.android.gms, .isolated, .isolated.droidguard, .supervision, .isolated.auth.account} E `brfg.g(pkg)` (assinado Google/plataforma, via módulo googlecertificates ou `getPackageInfo(pkg, GET_SIGNING_CERTIFICATES|GET_SIGNATURES)`).
- Causa no Vcloud: `Process.myUid()` mostra o uid do celular (10454) mas `NativeCore.getCallingUid` devolvia o uid virtual (10025) mesmo para chamadas do próprio app (gms→gms, ou depois de `clearCallingIdentity`) → nunca iguais → caía na checagem pesada e falhava. (Prova: `PackageManagerStub: 10025 , gms GetPackagesForUid` logo antes do erro.)
- 4.0.19: nos apps de dentro, `getCallingUid` devolve o uid do celular quando o chamador é o próprio processo ou outro processo do MESMO app (pid → `QuemChama` → mesmo uid virtual); também no caminho antigo (`callingBUid == meu buid`). REMOVIDOS: remendo "sistema 1000" (4.0.14), marca FLAG_SYSTEM (4.0.18, nunca disparou) e o diagnóstico do rastreio do gms.
- O gms real vem em SPLITS (`split_GoogleCertificates_installtime`, `DynamiteLoader`, `CronetDynamite`, `AdsDynamite`, `MeasurementDynamite`, `DynamiteModulesA/C`, `MapsDynamite`, config.*) e o Vcloud só copia o `base.apk` (`installPackageAsUser(sourceDir)`); o log mostra `Selected local version of googlecertificates` (vem do base) e módulos dynamite "remote". Possível problema futuro.
- Não testado no aparelho.

## 4.0.20 (relatório 00:37, app 4.0.19) — o `0p` PASSOU
- Sem `Only 0p` no log: a regra lida no gms (`getCallingUid()==myUid()`) estava certa. As telas de login do Google agora ABREM (`AccountIntroActivity`, `AccountPickerActivity`, processo `gms.ui`) e caem por permissões do Android:
  1. `UserManager.isMainUser()` → `SecurityException: need MANAGE_USERS, CREATE_USERS, or QUERY_USERS` (derruba `AccountIntroActivity.onCreate`).
  2. `DevicePolicyManager.getAccountTypesWithManagementDisabledAsUser` → `Caller with uid 10454 is not com.google.android.gms` (derruba `AccountPickerActivity.onResume`).
  3. Bug meu: `GetContentProvider` com `initProcess` nulo (app pausado) → NPE.
- 4.0.20: `IUserManagerProxy.invoke` e `IDevicePolicyManagerProxy.invoke` capturam essas recusas e devolvem resposta neutra (`ValorPadrao`: `isMainUser/isUserUnlocked/...` = true, o resto vazio/falso/0); `GetContentProvider` trata `appConfig == null`. Regras novas no `AutoAnalise`. Histórico: `USUÁRIO <método> recusado...`, `POLÍTICA <método> recusada...`.
- Ainda abertos: `android-id = 0`; WebView (`SandboxedProcessService0`) ausente dentro; gms só com `base.apk` (sem splits).

## 4.0.21 (relatório 00:53, app 4.0.20)
- As respostas neutras funcionaram (`USUÁRIO isMainUser recusado... true`, `getSeedAccountOptions... null`, `POLÍTICA ... neutra`); a tela de login do Google passou pelo `AccountIntroActivity`.
- Nova queda: `BiometricPrompt.authenticate` → `IAuthService.authenticate` → `SecurityException: com.google.android.gms from uid 10454 not allowed to perform USE_BIOMETRIC` (o serviço confere se o pacote é do uid de quem chama).
- 4.0.21: `PacoteDoVcloudProxy` (base) troca o nome do app de dentro pelo do Vcloud em todas as chamadas dos serviços `auth` (`IAuthServiceProxy`) e `biometric` (`IBiometricServiceProxy`), como já feito no PackageInstaller. A janela de confirmação (tela de bloqueio/biometria) é do Android e pede a identidade do próprio usuário. Não testado.
- Abertos: `android-id = 0`, WebView ausente dentro, gms só com `base.apk`.

## 4.0.22 (relatório 00:59, app 4.0.21)
- O nome do pacote nos serviços de biometria resolveu `not allowed to perform USE_BIOMETRIC`; nova recusa, agora no SISTEMA: `Must have SET_BIOMETRIC_DIALOG_ADVANCED permission` (`AuthService.authenticate`, via `IAuthService$Stub$Proxy.authenticate`).
- Lido no AOSP (`PromptInfo.requiresAdvancedPermission`, `AuthService.authenticate`): a permissão só é exigida se o pedido tem `mLogoRes != 0`, `mLogoBitmap`, `mLogoDescription` ou `mContentView` (com botão "mais opções"), ou autenticador IDENTITY_CHECK. Fonte: branch main do AOSP; o Android 17 do aparelho pode diferir.
- 4.0.22: `IAuthServiceProxy` limpa `mLogoRes/mLogoBitmap/mLogoDescription/mContentView` do `PromptInfo` antes de `authenticate` (só decoração; a confirmação é a mesma). Histórico: `BIOMETRIA recursos avançados ... tirados`. Não testado. Se o Google pedir IDENTITY_CHECK, não resolve.

## 4.0.23 (relatório 01:17, app 4.0.22) — o login chega ao checkin
- Sem nenhuma queda do Google. Fluxo no log: `AccountIntro` → `WrapperControlled` → `PreAddAccountActivity` → (10 s) → `ErrorActivity`. `BiometricPrompt: onAuthenticationSucceeded` (a confirmação do bloqueio PASSOU; o `BIOMETRIA ... tirados` do histórico confirma a limpeza do PromptInfo).
- Causa do erro (prova): `Auth: [CheckinHelper] Checkin timed out ... 10000 ms` → `setCheckinResult done with result=false` → `RESULT_ERROR`. No mesmo instante: `GservicesPersistence: Package com.vcloud.app is not approved to access the device ID` (pid do gms) — o Gservices só dá/grava o device id a pacotes aprovados, e o pedido chegava com o NOME do Vcloud (que eu mesmo fixei na origem do provedor na 4.0.6). Isso também explica `android_id=null` desde o início.
- 4.0.23: `ContentProviderStub.fixarOrigem` mantém o uid do celular (o Android confere) mas põe como NOME da origem o pacote do app de dentro que pede (`BActivityThread.getAppPackageName()`); provedor do próprio Vcloud segue com o nome do Vcloud. `ContextCompat.fixAttributionSourceState(obj, uid, pacote)`. Regra `Checkin timed out` no AutoAnalise. Não testado.
- Se o checkin ainda não completar: faltaria a rede do gms (HTTP para o Google) ou o `GservicesPersistence` aprovar por outro critério.

## 4.0.24 (relatório 01:24, app 4.0.23) — regra do device id lida no gms
- Checkin continua `timed out` e `Package com.vcloud.app is not approved...` (a troca de nome da 4.0.23 não pegou).
- Código do gms 26.43.30 (só leitura): `GservicesChimeraProvider.query` → `cwly.h(ctx, Binder.getCallingUid(), getCallingPackage(), chave)`. Para `android_id`: se `uid == Process.myUid()` → libera; senão se `pkg != null`: `brfg.g(pkg)` (assinado Google) → libera; senão `DevicePolicyManager.isDeviceOwnerApp/isProfileOwnerApp(pkg)` → libera; senão LOG "Package X is not approved..." e NEGA. (`brfg.g` → `brfg.a(pkg,false,false).b`, checa assinatura Google/plataforma.) Logo: o chamador legítimo (gms/vending) passa pelo PACOTE, desde que `getCallingPackage()` seja o do app de dentro.
- Hipótese: `ContextCompat.fixAttributionSourceState` não altera nada no Android 17 (`_check_mAttributionSourceState()` nulo?) e o nome fica o do Vcloud. 4.0.24: `ContentProviderStub.fixarOrigem` lê o nome antes/depois (`getPackageName()`), e se não mudou cria uma `AttributionSource(uid do celular, nome do app de dentro)` nova no argumento. Histórico: `ORIGEM do pedido ao provedor de X: nome antes=.. depois=.. (quer=..)`. Não testado.
- NÃO fazer: devolver `Process.myUid()` para todo mundo no Gservices (anularia o controle de acesso do device id).

## 4.0.25 (relatório 01:44, app 4.0.24)
- O diagnóstico `ORIGEM` mostra a troca funcionando entre processos (`antes=com.vcloud.app depois=com.android.vending / com.google.android.gms / com.google.android.setupwizard`), mas `Package com.vcloud.app is not approved...` e `Checkin timed out` continuam: o aviso vem do PRÓPRIO processo do gms (pid do checkin = pid do provedor).
- Causa: provedores do app que moram no mesmo processo (Gservices dentro do gms) são embrulhados em `ContentProviderDelegate.init()` com `new ContentProviderStub().wrapper(.., hostPkg)`; o filtro da 4.0.23/24 trata `mAppPkg == hostPkg` como "provedor do Vcloud" e mantém o nome do Vcloud. 
- 4.0.25: `ContentProviderStub.local()`; `ContentProviderDelegate.LOCAIS` (autoridades instaladas em `BActivityThread.installProviders`); `init()` marca como locais; local → nome da origem = app de dentro. Histórico: `ORIGEM do pedido ao provedor local: ...`. Não testado.

## 4.0.26 (relatório 02:10, app 4.0.25)
- RESOLVIDO: `Package com.vcloud.app is not approved to access the device ID` não aparece mais (provedores locais agora com o nome do app de dentro: `ORIGEM do pedido ao provedor local: antes=com.vcloud.app depois=com.google.android.gms`).
- AINDA: `[CheckinHelper] Checkin timed out` (10 s) → `ErrorActivity`.
- Código do gms 26.43.30 (só leitura): `CheckinApiStub.startCheckinAndGetCheckinResult` (`bkyv.f`) → se `jzca.c()`: executa tarefa `blag(ctx, bundle, listener)`; senão registra listener `blak` e `startService(Intent)` onde o Intent = `blbm.a(ctx, bundle)` = `IntentOperation.getStartIntent(ctx, "com.google.android.gms.checkin.CheckinIntentOperation", "com.google.android.gms.checkin.CHECKIN_START_ACTION")` + extra `config_flags_bundle`. Ou seja, o checkin roda como Chimera IntentOperation (serviço `GmsIntentOperationService`).
- No log: depois de `startCheckinAndGetCheckinResult` NADA mais aparece (sem rede, sem erro, nenhuma linha "IntentOperation" em 3 min) → ou o serviço não inicia, ou o checkin não é logado.
- 4.0.26 (só diagnóstico): `START serviço: <comp> ação=.. | de dentro=..` para serviços com.google.android.* no `StartService`, e `START do serviço-ponte falhou ...` em `ActiveServices.startService`. Sem correção nova.

## 4.0.27 (relatório 02:30, app 4.0.26)
- `START serviço: com.google.android.gms/.chimera.GmsIntentOperationService ação=com.google.android.gms.checkin.CHECKIN_START_ACTION | de dentro=true` → o motor ACEITOU iniciar o checkin virtualmente; sem `START do serviço-ponte falhou`. Outros starts do gms também aceitos (WRITE_JOURNAL_UPDATE, NO_OP, GcmService, UiIntentOperationService WARM_UP_UI_PROCESS).
- Mesmo assim: nenhuma linha de execução de IntentOperation em todo o log (nem as outras) e `Checkin timed out` segue. Falta saber se `ProxyService.onStartCommand` chega a rodar e se `GmsIntentOperationService.onStartCommand` executa/falha.
- 4.0.27 (só diagnóstico): `AppServiceDispatcher.onStartCommand` registra `SERVIÇO iniciando onStartCommand: <serviço> ação=..`, `SERVIÇO não foi criado`, `SERVIÇO onStartCommand FALHOU ... | <erro>` e `SERVIÇO comando sem dados do serviço`. Sem correção nova.

## 4.0.28 (relatório 02:39, app 4.0.27) — o checkin EXECUTA e cai
- Diagnóstico mostrou: `START ... CHECKIN_START_ACTION` → `SERVIÇO iniciando onStartCommand: GmsIntentOperationService ação=...CHECKIN_START_ACTION` → o serviço roda (todos os outros também) e o executor cai: `FATAL EXCEPTION [GmsIntentOperationChimeraService-Executor]: SecurityException: getIccSerialNumber: The uid 10454 does not meet the requirements to access device identifiers` (`TelephonyManager.getSimSerialNumber` ← `CheckinIntentOperation.onHandleIntent`). O serviço de checkin explode e por isso o login dá `Checkin timed out`.
- Também (não fatal): `Finsky: SecurityException when adding listener to subscriptions`.
- 4.0.28: `IPhoneSubInfoProxy.invoke` captura SecurityException de identificadores (`device identifiers`, `READ_PRIVILEGED_PHONE_STATE`, `READ_PHONE_STATE`) e devolve `ValorPadrao` (null para String) + histórico `TELEFONE <método> recusado...`. Regra nova no AutoAnalise. Não testado. O checkin pode esbarrar em outros identificadores (IMEI, serial, Wi-Fi MAC...) e em rede/aceitação do Google.

## 4.0.29 (relatório 02:46, app 4.0.28)
- A 4.0.28 funcionou: `TELEFONE getIccSerialNumberForSubscriber / getSubscriberIdForSubscriber recusado ...; resposta: sem valor`; o checkin passou desse ponto.
- Nova queda no mesmo executor (`CheckinIntentOperation.onHandleIntent` → `SubscriptionManager.getActiveSubscriptionIdList` → `ISub$Stub$Proxy.getActiveSubIdList`): `SecurityException: getActiveSubIdList. Does not have any of the following permissions. [android.permission.READ_PRIVILEGED_PHONE_STATE]`.
- 4.0.29: `ISubProxy` (serviço `isub`, base `PacoteDoVcloudProxy`) captura essas recusas e devolve `ValorPadrao` (int[] vazio...). Histórico: `CHIP <método> recusado pelo Android (permissão privilegiada); resposta: neutra`. Regra `READ_PRIVILEGED_PHONE_STATE` no AutoAnalise. Não testado.
- Padrão: cada pergunta de telefonia/chip que o checkin faz pode dar uma recusa nova. Se vierem muitas, trocar por um filtro genérico para todo o serviço de telefonia.

## 4.0.30 (relatório 02:52, app 4.0.29)
- Sem queda do Google, mas `Checkin timed out` continua. Nesta rodada NÃO aparece nenhuma linha `TELEFONE`/`CHIP` (nas rodadas 4.0.27/28 o executor do checkin chegava ao telefone ~5 s depois do `START CHECKIN_START_ACTION`, junto de um `START ... NO_OP`; agora não há NO_OP). Hipótese a verificar: o checkin não está chegando a executar (ou travou antes do telefone).
- Erro real novo (Play Store): `FATAL EXCEPTION: CronetInit` → `IConnectivityManager.getNetworkCapabilities` → `SecurityException: Package com.android.vending does not belong to 10454` (11x no log). Corrigido: `IConnectivityManagerProxy.invoke` troca o nome do app de dentro pelo do Vcloud.
- 4.0.30: log de TODA consulta de chip/telefone (`TELEFONE consulta isub.<método>` / `phonesubinfo.<método>`) para ver até onde o checkin vai.

## 4.0.31 (relatório 02:59, app 4.0.30)
- Nas rodadas 4.0.27/28 o executor do checkin (`CheckinIntentOperation.onHandleIntent`) chegava ao telefone ~5 s depois do `START CHECKIN_START_ACTION` (e caía); nas 4.0.29/30 NÃO chega: nenhuma consulta de telefone no processo do checkin (gms.ui) em 10 s (as `TELEFONE consulta isub.*` do log são só da inicialização do gms/Play Store). `ISubProxy` está instalado em todos os processos.
- Código do gms (só leitura): `afqe.onStartCommand` põe o Intent numa fila (`l`) e `notifyAll()`; as operações rodam num `ThreadPoolExecutor(core 1, max 25, keepAlive 5 s, SynchronousQueue, DiscardPolicy)` (`afpz`); `NO_OP/KEEPALIVE/RELEASE_WAKELOCK` só ajustam contadores. Suspeita: o NO_OP ~5 s depois era o fim da operação (finally), não o gatilho.
- 4.0.31 (só diagnóstico): 8 s depois de `CHECKIN_START_ACTION`, `AppServiceDispatcher.fotoDasThreads()` grava no histórico `FOTO das threads do gms ...` (nome, estado e 9 linhas de pilha das threads de operação/checkin) para ver onde o checkin está parado.

## 4.0.32 (relatório 03:10, app 4.0.31) — foto das threads
- Foto (`FOTO das threads do gms 8 s depois do checkin`): `[GmsIntentOperationChimeraService] processing` está WAITING em `afqc.run:38` (`Object.wait`); `PersistentIntentOperationChimeraService` idem; o executor de operações (keepAlive 5 s) já não existe. Código (só leitura): `afqc.run` espera em `k.wait()` até a fila `l` ter intents; `afqe.onStartCommand` adiciona o intent e `notifyAll()`. Logo o checkin foi tratado ou descartado sem erro e sem nenhuma consulta de telefone (nenhum `phonesubinfo.getIccSerialNumber...`).
- Hipótese (NÃO confirmada): estado salvo de tentativas anteriores no `gms` (as rodadas 4.0.27/28 caíram no meio do checkin) faz o checkin pular ("espera/backoff"); resetar os dados do Google de dentro (Serviços do Google: desinstalar e instalar) limparia isso.
- 4.0.32: registros `TELEFONE consulta ...` agora com limite de 3 s (antes 120 s escondia repetições no mesmo processo); a foto passa a mostrar o processo e o pid.

## 4.0.33 (relatório 03:19, app 4.0.32)
- Foto das threads (agora com processo/pid = gms.ui): `GmsIntentOperationChimeraService [processing]` WAITING em `afqc.run` (espera a fila `l`); sem nenhuma consulta de telefone depois do `CHECKIN_START_ACTION`. O checkin some sem erro.
- Código do gms 26.43.30 (só leitura), `blan.a()` (CheckinTask): portões antes de iniciar: (1) com flag, `ActivityManager.getRunningServices` contendo `com.google.android.gsf/.checkin.CheckinService` → "GSF Checkin still active" e aborta; (2) `ConnectivityManager.getActiveNetworkInfo() == null` → "checkin scheduled: no active network"; (3) "GmsCore checkin is not enabled."; (4) mensagens "Checkin will retry later, network is not available" / "Unable to run checkin now.". O motor responde NetworkInfo não nulo e CONNECTED; o `getServices` virtual só lista serviços do processo do chamador (não deve incluir o do gsf). Essas mensagens usam o logger `La.V` (não vai ao logcat) → portão não observável.
- O log não mostra reinício dos dados do Google (nenhum `onPackageUninstalled/installPackage`), então o passo manual provavelmente não foi feito.
- 4.0.33: `GoogleModo.reiniciarDadosUmaVez("google_reset_4_0_33")` chamado em `App.onCreate` (processo principal, 8 s depois): com o modo Google completo ligado, `clearPackage` de gms/gsf/vending uma única vez (arquivo-marca em `blackbox/`); histórico `GOOGLE dados de dentro reiniciados uma vez ...`. Hipótese NÃO confirmada: o Chimera guarda registro de quedas (`GCore-Chimera-Crash`) e pula a operação que caiu 4x nas rodadas 4.0.27/28.

## 4.0.34 (relatório 08:14, app 4.0.33) — o checkin CHEGA ao Google
- `GOOGLE dados de dentro reiniciados uma vez (google_reset_4_0_33)`: hipótese confirmada — depois do reinício o `CheckinIntentOperation` voltou a executar (log do próprio checkin: `[CheckinOperation] Starting Checkin`, `[AndroidCheckinProtoModule] Checkin reason type: 5 attempt count: 1`).
- O pedido foi ao servidor e VOLTOU recusado: `Checkin failed: https://android.googleapis.com/checkin ... invalid hardware identifier: "5628c3bc4e63dbcaa448a7627ff47ce6" is not a valid IMEI (expect 15 decimal digits)` → `Checkin Operation finished with result: FAILURE` → ErrorActivity em ~4 s.
- Origem: `ITelephonyManagerProxy` (BlackBox) responde `md5(pacote do Vcloud)` para `getDeviceId`, `getImeiForSlot`, `getMeidForSlot`, `getSubscriberId`, `getDeviceIdWithFeature`.
- 4.0.34: `idDoAparelho()`: para apps do Google (`GmsCore.isGoogleAppOrService`) responde `null` (sem valor, não inventa IMEI); os outros apps continuam com o md5. Regra `not a valid IMEI` no AutoAnalise. Não testado. Próximos riscos: outros identificadores no pedido (serial, MAC, ICCID) ou o Google recusar um aparelho sem IMEI/virtual.

## 4.0.35 (relatório 08:38, app 4.0.34) — LOGIN DO GOOGLE FUNCIONOU
- `[CheckinRequestProcessor] ... Checkin Succeeded: https://android.googleapis.com/checkin` → `[CheckinOperation] Checkin Operation finished with result: SUCCESS` → `Gservices -> android_id=4483960649116985303` (primeiro android_id de dentro) → `[AddAccountHelper] Account added successfully to AccountManager` / `[AuthAccountManager] Account added successfully.` (08:37:25). A correção do IMEI (4.0.34: `null` para apps do Google) resolveu o checkin.
- Falhas de acabamento:
  1. `NullPointerException: Expected to unbox a 'int' primitive type but was returned null` em `ActivityManager.getUidImportance → getUidProcessState` (gms caía 3x): `IActivityManagerProxy.invoke` devolvia `null` no catch de SecurityException para métodos `get*`. Corrigido: `ValorPadrao.de(tipo de retorno)`.
  2. `GoogleServicesActivity` caía: `IBackupManager.isBackupServiceActive → SecurityException: ... android.permission.BACKUP`. 4.0.35: `IBackupManagerProxy` (serviço `backup`, base `PacoteDoVcloudProxy`) responde neutro. Histórico `BACKUP <método> recusado...`.
- Também visto (não tratado): `ClassNotFoundException BackgroundProcessEndpointService` da Play Store (`com.google.frameworks.client.data.android.server.play`) — classe provavelmente em split da Play Store (só o `base.apk` é copiado).
- Aberto: a conta existe dentro; falta conferir a Play Store autenticada, licenças dos jogos (Touch Macro/DYSMANTLE) e WebView/splits.

## 4.0.36
- Play Store travava ao abrir: `SecurityException: Binding only allowed within app` (Firebase Messaging confere `Binder.getCallingUid() == Process.myUid()`).
- Causa (hipótese forte, lida no código): `Process.myUid()` devolve o uid virtual (OsStub.getuid → getBAppId) e `getCallingUid` devolvia o uid do celular quando a chamada é do próprio processo.
- Mudança: na chamada do próprio processo, `NativeCore.getCallingUid` devolve `Process.myUid()`.
- Não verificado no aparelho. Risco: outro app de dentro que comparava com o uid do celular nessa situação.

## 4.0.37 (só diagnóstico)
- Relatório 08:55 (4.0.36): a Play Store abre sem travar (o crash do 4.0.35 sumiu). Mas mostra "Fazer login"; o Finsky diz "No current account found" e o login do Google diz "Esta conta já existe no seu dispositivo".
- Hipótese: a Play Store e o gms veem listas de contas diferentes (ou a lista do AccountManager de dentro está vazia e o gms guarda a conta no banco dele). Não confirmado.
- Mudança: log `CONTAS` no servidor (getAccountsAsUser: tipo, quantas devolveu, total, pid; e quando adiciona conta).
- Outros: `ITelephonyRegistry.addOnSubscriptionsChangedListener` ainda dá "Package com.android.vending does not belong to 10454" (não fatal).

## 4.0.38
- Relatório 09:01 (4.0.37), log CONTAS: o servidor tem 1 conta (com.google). Quem pede por tipo `com.google` recebe 1. A Play Store pergunta com tipo `null` (getAccounts) e recebia 0 → "No current account found".
- Mudança: `getAccountsAsUser` com tipo null devolve todas as contas (como no Android).
- Não verificado no aparelho. Outras funções por pacote (`getAccountsForPackage`) não mexidas.

## 4.0.39
- Relatório 09:09 (4.0.38): **a Play Store entrou na conta** (Finsky: "Loaded library for account", "Sending payloads for account"). O `getAccountsAsUser(null)` da 4.0.38 resolveu.
- Novo erro no gms: `ClassCastException: Integer → SyncNotedAppOp` em `AppOpsManager.startOpNoThrow` (IAppOpsManagerProxy devolvia sempre int nos note*/start*).
- Mudança: `permitido(method,args)` devolve pelo tipo de retorno (int, void, SyncNotedAppOp criado por reflexão).
- Não corrigido: `DeviceOriginProvider` (setupwizard) recusou leitura pela Play Store 1x (permissão READ_DEVICE_ORIGIN); a Play Store seguiu funcionando. `Package com.android.vending does not belong to 10454` em `ITelephonyRegistry.addOnSubscriptionsChangedListener` (não fatal). `Cannot cast spl to avru` é antigo (11-01).
- Não verificado no aparelho.

## 4.0.40
- Relatório 09:17 (4.0.39): o erro `SyncNotedAppOp` sumiu. Play Store logada abre e carrega. Sem crash novo do Vcloud.
- Ainda no log: `Finsky: Cannot find com.android.providers.downloads` (a Play Store não acha o gerenciador de downloads), `Cannot cast spl to avru` em CachedPackageContentProvider (processo :background, antigo e repetido), `Package com.android.vending does not belong to 10454` (isub/telephony registry).
- Mudança: `com.android.providers.downloads` entra nos pacotes abertos do sistema (AppSystemEnv), então o pacote do celular fica visível.
- Não verificado: se o download pela Play Store funciona. Atenção: o instalador usa o PackageInstaller do celular (GetPackageInstaller troca pelo pacote do Vcloud), então instalar pela Play Store pode instalar fora do Vcloud, não dentro.

## 4.0.41 — instalador virtual (etapa 1: instalar, atualizar, desinstalar pela Play Store)
- Pedido do usuário: a Play Store de dentro deve fazer tudo como a da loja do celular (instalar, atualizar, desinstalar, comprar). Sem burlar nada.
- Causa (do código, não de um teste): a Play Store usava o PackageInstaller REAL do celular; o Vcloud só trocava o nome do pacote.
- Mudança: `fake/service/InstaladorVirtual.java`, chamado em `IPackageManagerProxy.GetPackageInstaller` antes da chamada real.
  - createSession/openSession/getSessionInfo/getAllSessions/getMySessions/abandonSession/registerCallback: sessões em `blackbox/instalador/ID` (arquivos, porque a Play Store tem vários processos).
  - openWrite: servidor do "FileBridge" (socket; comandos 1 grava, 2 fsync, 3 fecha; cabeçalho 8 bytes big-endian).
  - commit: instala o APK base com `BPackageManager.installPackageAsUser(..., installByStorage)` e avisa o IntentSender (EXTRA_STATUS 0 ou 1).
  - uninstall: só se o pacote estiver instalado dentro; remove com `uninstallPackageAsUser`; pacote fora do Vcloud devolve STATUS_FAILURE_INVALID (nunca mexe no celular).
  - Google (gms/vending/gsf...) não é atualizado nem removido por aqui (recusa com mensagem).
  - Eventos `INSTALADOR ...` no relatório.
- Não feito ainda: splits (config de ABI/densidade/idioma; só o base.apk é instalado), callbacks entre processos, compra, WebView.
- Não verificado no aparelho; a classe nunca foi executada (só compilada pelo CI).

## 4.0.42
- Relatório 09:50 (4.0.41): nenhum evento `INSTALADOR` → a atualização nem chegou ao instalador virtual (a Play Store desiste antes; causa não achada). Perfil não abre: sem erro fatal; o gms nega o serviço #468 (`ZERO_PARTY`, AppImportanceApiService) à Play Store: `dozn.a` (gms) só deixa se `brft.d(pkg).b` (verificador de assinatura do Google) ou pacote na lista extra (flag do servidor). Não resolvido.
- Mudança pequena: `ITelephonyRegistryProxy.invoke` troca o nome do app de dentro pelo do Vcloud em qualquer método (corrige `Package com.android.vending does not belong to 10454` em `addOnSubscriptionsChangedListener`).
- Não verificado no aparelho se isso muda perfil/atualização.

## 4.0.43
- Relatório 10:27 (4.0.42): o erro `Package ... does not belong` sumiu. Continua sem `INSTALADOR` (atualização nunca chega ao instalador). Achado: o processo `com.android.vending:background` (onde roda a fila de instalação/atualização) leva FATAL EXCEPTION toda vez que abre: `ClassCastException: Cannot cast spl to avru` em `CachedPackageContentProvider.a` e `SecurityHubContentProvider.a` → `arwl.f` → `soy.e` (`Class.cast` do componente de injeção). Acontece durante o `Application.onCreate` (antes de `afterApplicationOnCreate`).
- Hipótese (do código, não testada): o onCreate desses dois provedores dispara uma tarefa em thread de fundo que pede ao componente da loja uma interface que só existe depois do onCreate do app terminar. No Android real o onCreate é rápido e a tarefa perde a corrida; no Vcloud é mais lento.
- Mudança: em `BActivityThread.handleBindApplication`, esses dois provedores da Play Store são instalados DEPOIS do `Application.onCreate`.
- Não verificado no aparelho.

## 4.0.44
- Relatório 10:35 (4.0.43): a hipótese da 4.0.43 (adiar provedores) NÃO resolveu: o `Cannot cast spl to avru` continua; a pilha agora é `adsj.run → CachedPackageContentProvider.a → arwl.f → soy.e`.
- Causa provável (código): `spl` (componente da loja no :background) não implementa `avru`/`auxo`; essas tarefas são dos provedores do processo PRINCIPAL. `BActivityThread.installProviders` instala em qualquer processo os provedores cujo `processName` é o pacote (condição `providerInfo.processName.equals(context.getPackageName())`), ou seja, instalava provedores do processo principal também no `:background`.
- Mudança: 4.0.43 revertida; na Play Store (`com.android.vending`) os provedores do processo principal não são instalados nos outros processos (só ela, para não mexer no resto, p.ex. gms).
- Não verificado no aparelho.

## 4.0.45
- Usuário (após a 4.0.44): compra funciona; download, perfil e desinstalar não.
- Relatório 10:48 (4.0.44): o `:background` não cai mais; a Play Store enfileira ("Will queue ... downloaded") mas logo recebe do servidor `PERMISSION_DENIED: Rejected by (internal-only) security policy` (chamada gRPC; qual não sei). Nenhum `INSTALADOR` apareceu.
- Mudança 1: `ActivityManagerCommonProxy.StartActivity`: pedido de tela ACTION_DELETE / UNINSTALL_PACKAGE com `package:X` de um app instalado DENTRO do Vcloud (não Google) remove de dentro (`uninstallPackageAsUser`), sem abrir a tela do celular. Sem confirmação (a Play Store costuma pedir a dela antes: não verificado).
- Mudança 2 (diagnóstico): `VENDING abre: ação=... dados=... componente=...` (cada tela que a Play Store abre) e `INSTALADOR chamada <método>` (cada método do instalador que ela usa).
- Não verificado no aparelho. Download e perfil seguem sem causa conhecida; precisam do relatório com essas linhas.

## 4.0.46
- Usuário: sem entrar no perfil da Play Store não dá nem para chegar em "desinstalar". (Relatório 12:47 foi tirado depois de ele desativar e reativar os serviços do Google na tela do Vcloud: dados do gms apagados, android_id=null; nada de perfil nele.)
- Achado no relatório: `FacsCacheGmsModule ... SecurityException: Calling uid: 10454 doesn't match source uid: 10032` em `IAppSearchManager.initialize` (gms de dentro, cache de conta). Candidato à causa do perfil; não confirmado.
- Mudança: `IAppSearchProxy` (serviço "app_search"): corrige o AttributionSource (uid do celular) nos argumentos, como já é feito em provedores.
- Não verificado no aparelho. Se o perfil continuar, falta o relatório logo após tocar no perfil (com a Play Store logada e o android_id preenchido).

## 4.0.47
- Relatório 13:19 (4.0.46): `android_id=null` continua; Play Store: `AuthFailureError: User needs to (re)enter credentials`; login diz "Esta conta já existe". Causa (log CONTAS: total=1): reativar os serviços do Google (tela do Vcloud) apaga os dados do gms mas a conta `com.google` continuava na lista do AccountManager de dentro → conta órfã, sem login, e sem poder entrar de novo. É por isso que perfil/desinstalar/download não dá para testar.
- Mudança: `BAccountManagerService.removerContasDoTipo`; chamado ao limpar dados ou desinstalar o gms (`BPackageManagerService`). A marca de reinício único passou para `google_reset_4_0_47` (App.kt): na primeira abertura limpa gms/gsf/Play Store e tira a conta órfã. Será preciso entrar na conta Google de novo (uma vez).
- Não verificado no aparelho.

## 4.0.48
- Relatório 13:59 (4.0.47): login funciona (checkin SUCCESS, conta adicionada). Depois a Play Store mostra "Estamos atualizando o Google Play ... Instalando..." e não sai (LocaleChangedModeActivity). Log: `Self-update check on locale change to ... splits=[config.arm64_v8a, config.pt, phonesky_data_loader, ...]`; `Locally installed splits [...splits=[]] missing some server available splits` → ela quer se auto-atualizar com splits. Nenhum pedido ao instalador virtual (só getMySessions).
- Mudança: `IPackageManagerProxy.GetPackageInfo` devolve `splitNames` (as 6 partes do servidor, idioma do aparelho) para `com.android.vending` no modo Google completo, para a Play Store achar que não falta nada e não entrar na auto-atualização.
- Risco: a Play Store pode tentar carregar essas partes (não existem). Não verificado no aparelho. Splits reais continuam para fazer.

## 4.0.49 — causa do "internal-only" achada no código da Play Store
- Lido no dex da Play Store 53.8.11: o futuro que falha com `PERMISSION_DENIED: Rejected by (internal-only) security policy` vem de `agfr.p → ahmm.b → ahmp.f()` = serviço gRPC `com.google.android.finsky.ipc.installqueue.InstallQueue` método `Schedule` (e `Uninstall` em `ahmp.g`). Não é servidor do Google: é gRPC sobre Binder (grpc-binder) entre o processo principal da Play Store e o `:background`, com a política `SecurityPolicies.internalOnly()` (só aceita quem tem o MESMO uid: `Binder.getCallingUid() == Process.myUid()`).
- Causa no Vcloud: `NativeCore.getCallingUid` devolvia o uid do celular para chamadas do mesmo app em outro processo, mas `Process.myUid()` devolve o uid virtual (OsStub.getuid) → nunca igual → rejeitado. Mesma raiz do "Binding only allowed within app" (4.0.36, caso do mesmo processo).
- Mudança: mesmo app, outro processo → devolve `Process.myUid()`.
- Isso explica download, atualização e desinstalar (usam o `InstallQueue` Schedule/Uninstall) falharem. Não verificado no aparelho.

## 4.0.50 (diagnóstico)
- Relatório 16:10 (4.0.49): `internal-only` continua. Lido no dex: a política é `Lcifj;->a(int uid)` = `uid == Lcifm;->a` e `Lcifm;.<clinit>` faz `a = Process.myUid()` UMA vez (capturado quando a classe carrega). Se carregar antes de `BActivityThread.isInit()` (mBoundApplication), `OsStub.getFakeUid` devolve o uid do celular; depois devolve o virtual → `a` e o uid devolvido por `getCallingUid` podem divergir.
- Mudança só de diagnóstico: `PLAYSTORE (...) política internal-only: cifm.a=... myUid agora=... buid=... host=...` ao abrir a Play Store, e `UID chamada de outro processo: orig cpid quem eu(buid) myUid -> resultado` em `NativeCore.getCallingUid`.
- Próximo: com esses números, igualar `getCallingUid` ao `cifm.a` ou fixar `Process.myUid()` cedo.

## 4.0.51
- Relatório 16:23 (4.0.50): `cifm.a=10031` = `myUid` = buid da Play Store (ok, sem problema de captura cedo). Mas NENHUMA linha de chamada do mesmo app em outro processo (`quem=10031 eu=10031`) → a chamada do grpc-binder InstallQueue chega SEM pid (`cpid=0`, `quem=0`: "SEM dono"): é transação `oneway`, que no Android não traz o pid de quem chama. Caía no caminho antigo e devolvia o uid do celular (10454 ≠ 10031) → `internal-only`.
- Mudança: `NativeCore.getCallingUid`: no processo da Play Store, `cpid==0` → `Process.myUid()`. Só nela (os outros apps seguem como antes).
- Risco: um app de dentro que chame a Play Store por oneway passaria por ela mesma; dentro do Vcloud já compartilham o mesmo uid real. Não verificado no aparelho.

## 4.0.52
- Relatório 16:53 (4.0.51): **o `internal-only` sumiu**. A Play Store de dentro agora cria sessão no instalador virtual (`INSTALADOR sessão 700000 criada para com.slg.policewar`), baixa (apk 860 KB + config.arm64_v8a 27 MB + .dm, ~23 s) e tenta instalar.
- Falha seguinte: `IV2::PreSV: Session doesn't contain resources of required artifacts for package com.slg.policewar. Required artifacts: [com.slg.policewar.apk, config.arm64_v8a, gpdeku, gpdeku.config.arm64_v8a...]` → `SM: Session operation failed statusCode=5425 ... Status: 6573`. Causa: `InstaladorVirtual.nomeSeguro` acrescentava ".apk" a nomes sem ".apk" ("config.arm64_v8a" virava "config.arm64_v8a.apk"), então `getNames()` não batia com os artefatos.
- Mudança: nomes ficam como a Play Store gravou; base = `<pacote>.apk` (ou base.apk / maior .apk); depois de instalar o base, copia as `.so` das outras partes (`NativeUtils.copyNativeLib`, ex.: `config.arm64_v8a`) para a pasta de bibliotecas do app. Partes de recursos (densidade/idioma) continuam sem uso (próxima etapa).
- Também visto: `IV2::AdvHelper ... NOT_FOUND: Session not found: 700000` (gRPC de verificação; só aviso) e, para blackdeck, `RF::PP-Copy ENOENT .../dataloader/.../upfront/stream..*.dm` (status 6401): não investigado.
- Não verificado no aparelho.

## 4.0.53
- Relatório 19:16 (4.0.52): a Play Store cria sessão, baixa e CHEGA A CONFIRMAR ("Submitter: start committing"), mas `SM: Session operation failed ... InvocationTargetException` por `NullPointerException ... FileBridge$FileBridgeOutputStream.<init> ... PackageInstaller$Session.openWriteAppMetadata` (via `setAppMetadata`): meu `openWriteAppMetadata` da sessão virtual devolvia null.
- Também visto: 1ª tentativa de cada app tenta o "data loader/streaming" e falha com `ENOENT .../dataloader/<pkg>/upfront/stream..*` (status 6401); a Play Store então repete pelo caminho normal ("Incremental install not requested") — é este que importa.
- Mudança: `openWriteAppMetadata` devolve escrita (arquivo `_app_metadata`, fora de getNames/bibliotecas). Log `INSTALADOR sessão método X` (1x/min) para ver os métodos usados.
- Não verificado no aparelho.

## 4.0.54
- Relatório 19:24 (4.0.53): **a Play Store instalou um app dentro do Vcloud** (`INSTALADOR sessão 700002 instalou com.avegagames.magic.craft.survival`; aparece na lista de apps de dentro, 1.8.96). Pedido do instalador: getNames, setChecksums, openWriteAppMetadata, commit.
- Logo depois: `Tipos de conta registrados dentro: NENHUM` e `bind failure` / `Error while retrieving auth token` (110x) → a Play Store perde o login e os próximos downloads falham (1400). Causa: `BAccountManagerService.loadAuthenticatorCache(pacote)` (chamado em `onPackageInstalled`) fazia `authenticators.clear()` e lia só o pacote novo → apagava o autenticador do Google toda vez que se instalava um app.
- Mudança: com pacote, só as entradas daquele pacote são refeitas (sem clear geral).
- Não verificado no aparelho (instalar 2 apps seguidos; abrir o app instalado).

## 4.0.55
- Usuário (19:33): Play Store mostra 4 apps "Instalando..." sem terminar; Zombie Waves e outros já aparecem na lista do Vcloud (instalam), mas a loja não sabe que acabou. Craft Survival (jogo com licença Play) abre e fecha em 1 s: `LicenseClient NameNotFoundException`, `PSR: Package ... not installed`, `Package name ... is not owned by caller`, `getInstallSourceInfo NameNotFound`.
- Causa achada no código do Vcloud: o aviso final do instalador (`IntentSender.sendIntent` com `EXTRA_STATUS`) vai para um PendingIntent de broadcast "de fachada" (`ProxyBroadcastReceiver`); o Android coloca os extras de preenchimento no intent de fachada, e `ProxyBroadcastRecord.create` só entregava o intent original → a Play Store nunca recebia o status.
- Mudança: `ProxyBroadcastRecord.create` copia os extras de preenchimento do intent de fachada para o original.
- Ainda a fazer: `getInstallSourceInfo`/installer do app (Play Store como instalador, para a licença); callbacks de sessão (`onFinished`); PACKAGE_ADDED aos apps de dentro; partes de recursos.
- Não verificado no aparelho.

## 4.0.56 (diagnóstico do perfil)
- Usuário: tocar no ícone de perfil da Play Store sai da loja. Relatório 19:34 (4.0.54): `CrashMonitor: Crash detected: Crash[JavaException] com.android.vending` → `Sending signal PID 602 SIG: 9` logo depois do toque (AppInfoManager / OneGoogle). A exceção (mensagem "Package com.android.vending does not belong to 10454") é de uma exceção NÃO TRATADA na linha principal; o `CrashMonitor.installGlobalCrashHandlers` mata o processo e a pilha nunca era impressa (só havia a mensagem). Esse mesmo "does not belong to 10454" aparece em todo relatório e é a provável causa dos "fechou logo ao abrir" da Play Store.
- Mudança só de diagnóstico: `CrashMonitor.handleCrash` imprime `FATAL EXCEPTION ... (Vcloud)` com a pilha completa (Log.e AndroidRuntime).
- Também (sync/instalação): `Instalando...` pode ter sido resolvido na 4.0.55 (extras de preenchimento); não confirmado.

## 4.0.57 — perfil da Play Store
- Relatório 19:47 (4.0.56, com a pilha): tocar no perfil cai (linha principal) em `ICrossProfileApps$Stub$Proxy.getTargetUserProfiles ← CrossProfileApps.getTargetUserProfiles ← Play Store (onClick)`: `SecurityException: Package com.android.vending does not belong to 10454`. Outra, em thread de fundo: `IUsageStatsManager.queryEventsWithFilter`: `com.android.vending from uid 10454 not allowed to perform GET_USAGE_STATS`.
- Mudança: `ICrossProfileAppsProxy` (serviço "crossprofileapps": nome do app → Vcloud; recusa → vazio) e `IUsageStatsProxy` ("usagestats": recusa → vazio), no bloco isR do HookManager.
- Não verificado no aparelho.

## 4.0.58
- Usuário: perfil abre (4.0.57 ok). Alguns apps instalam (Soul Huntress, Zombie Waves...), outros não. Relatório 20:01: Soul Huntress instala dentro (`sessão ... instalou`), mas a Play Store não vira para "instalado" (nada depois do commit); os que "falham" (blackdeck 6252, stronghold 5426) são pedidos ANTIGOS reiniciados (`IV2: restart active install`, `SM: session ... is closed`, `PSV: Package ... was not installed`): a sessão virtual deles já não existe.
- Achado no manifesto da Play Store: `AtomicInstallBroadcastReceiverMain/Background` com `android.content.pm.action.SESSION_UPDATED`. No Android real o sistema manda esse broadcast (e os callbacks de sessão) ao fim; o Vcloud não mandava.
- Mudança: `InstaladorVirtual.notificarSessao`: chama os callbacks registrados (`onSessionFinished`) e envia SESSION_UPDATED (com SessionInfo) aos receptores do instalador via `BActivityManager.scheduleBroadcastReceiver` com um `PendingResultData` tipo TYPE_UNREGISTERED. Log `INSTALADOR SESSION_UPDATED enviado`.
- Não verificado no aparelho. Se a loja ainda não marcar como instalado, falta também PACKAGE_ADDED aos receptores dinâmicos.

## 4.0.59
- Relatório 20:11 (4.0.58): Soul Huntress e Black Deck instalam e aparecem na lista do Vcloud; a Play Store mostra Soul Huntress em "Cancelar" (ainda "instalando"). O SESSION_UPDATED chegou ao receptor da Play Store (`Handling session update intent`) mas ele só serve para sessões "staged": `IllegalStateException: Session is not marked as staged` em `SessionInfo.isStagedSessionReady` (`AtomicInstallBroadcastReceiver`, experimento desligado). Não é o caminho dos installs normais.
- Mudança: SESSION_UPDATED removido (callbacks `onSessionFinished` mantidos). Diagnóstico para achar o caminho certo: `VENDING cria PendingIntent tipo=... ação=... componente=...` (GetIntentSender, só Play Store) e `INSTALADOR aviso entregue ao IntentSender de <pacote>`.
- Hipótese aberta: a Play Store espera o `EXTRA_STATUS` do IntentSender do commit e/ou PACKAGE_ADDED (PSR/PSV: "Package ... was not installed", versão e lastUpdateTime). Precisamos do tipo de PendingIntent (broadcast/service/activity) para ver se o aviso é entregue.

## 4.0.60
- Relatório 20:33 (4.0.59): o commit da Play Store usa `PendingIntent` tipo=1 (broadcast) IMPLÍCITO: ação `com.android.vending.INTENT_PACKAGE_INSTALL_COMMIT.<pacote>:<id>`, sem componente, pacote=com.android.vending. O Vcloud entregou o aviso (`aviso entregue ao IntentSender de com.vcloud.app (status 0)`) mas a loja nunca reage.
- Causa: `GetIntentSender` trocava QUALQUER PendingIntent de broadcast pelo componente `ProxyBroadcastReceiver` (só alcança receptores de manifesto). Receptores registrados em tempo de execução dentro do app (o caso do instalador) nunca recebiam.
- Mudança: se o broadcast do PendingIntent é implícito (sem componente, com ação), o intent de fachada leva a ação + pacote do Vcloud (como `broadcastIntent` já faz), então o Android entrega aos receptores registrados (dinâmicos e de manifesto de apps de dentro); os extras de preenchimento (EXTRA_STATUS) são mesclados por `ProxyBroadcastRecord.create` (4.0.55).
- Não verificado no aparelho.

## 4.0.61
- Usuário: falta a opção de desinstalar apps na Play Store; a loja não sincroniza com os apps já instalados. Relatórios anteriores: `Unable to check installer for <pacote>: NameNotFoundException (ApplicationPackageManager.getInstallSourceInfo)` e `PSR: Package <pacote> not installed`, mesmo com o app instalado dentro.
- Mudança: `IPackageManagerProxy.GetInstallSourceInfo`: para app instalado dentro do Vcloud devolve `InstallSourceInfo` (reflexão) com instalador "com.android.vending" (igual ao `getInstallerPackageName`).
- Não verificado no aparelho; pode faltar também o `PACKAGE_ADDED` aos receptores dinâmicos.

## 4.0.62
- Relatório 22:14 (4.0.61): a opção de desinstalar só aparece para um app. Log: `PSR: Package <pacote> not installed` para TODOS os apps de jogos instalados dentro (e "Package cache is empty, setup cache").
- Lido no dex: `PackageStateRepository` (`anuk.D` → `getInstalledPackages`) converte cada `PackageInfo` em registro (`anuk.s`) só se `PackageInfo.signatures` não for nulo/vazio. No Vcloud, `PackageManagerCompat.generatePackageInfo` usava `p.mSignatures` para apps que não existem no celular; jogos assinados só com v2/v3 ficam com `mSignatures` vazio (as assinaturas estão em `mSigningDetails.signatures`) → a loja não os vê como instalados.
- Mudança: se `mSignatures` vier vazio, usa `mSigningDetails.signatures`.
- Não verificado no aparelho. (Hipótese: o app que já funcionava seria assinado com v1.)

## 4.0.63
- Relatório 22:23 (4.0.61, chegou atrasado): a Play Store diz `Submitter: commit timeout (TimeoutFuture ...)` 10 min depois do commit de policewar: o resultado do commit (PendingIntent broadcast implícito `...INTENT_PACKAGE_INSTALL_COMMIT.<pacote>:<versão>`) NUNCA chega, mesmo com `aviso entregue (status 0)`. O timeout depois cancela e a sessão é apagada; o Vcloud tenta instalar de novo e falha (`parser apk error` por arquivo já apagado) — efeito, não causa.
- Mudança: o intent original de cada PendingIntent de broadcast da Play Store é guardado (`InstaladorVirtual.lembrar`, no `GetIntentSender`); no fim do commit o resultado vai por `Context.sendBroadcast(original + EXTRA_STATUS...)` (caminho de broadcast normal do Vcloud, que alcança receptores registrados no app); se não achar o original, volta ao `sendIntent`. Logs `INSTALADOR resultado enviado por broadcast direto` / `sem o intent original`.
- Não verificado no aparelho.

## 4.0.64 — Play Protect
- Usuário: o Play Protect não abre. Relatório 22:39: `gms VerifyAppsSettingsActivity` ↔ `vending ExternalPlayProtectHomeEntryPoint` abrindo um ao outro em ciclo (vários por segundo).
- Manifesto da Play Store: `ExternalPlayProtectHomeEntryPoint` é um `activity-alias` com `android:enabled="false"` (ação `com.google.android.finsky.PLAY_PROTECT`). No Android real, desligado, a intenção do gms não acha nada e o gms mostra o Play Protect dele. O motor do Vcloud ignorava `enabled` e `setComponentEnabledSetting` → achava o alias e voltava ao gms (ciclo).
- Mudança: `utils/ComponenteHabilitado` (estado por arquivo); `setComponentEnabledSetting` grava o estado de componentes de apps de dentro; `BPackageManagerService.queryIntentActivities` (por pacote, intent implícito) tira telas desligadas (estado 1 = ligado vence).
- Não verificado no aparelho. Risco: apps que dependem de telas desligadas no manifesto serem achadas por intent implícito (antes achadas por engano).

## 4.0.65
- Relatório 06:02 (4.0.64): **o resultado do commit chega** (`INSTALADOR resultado enviado por broadcast direto: ...INTENT_PACKAGE_INSTALL_COMMIT...`) e policewar e soulhuntress instalam (aparecem na lista). Play Protect ainda em ciclo: `ExternalPlayProtectHomeEntryPoint` continua sendo criada.
- Causa: o filtro de "desligado" da 4.0.64 ficou só em `queryIntentActivities(intent, flags, resolvedType, userId)` (método público); `resolveActivity/resolveIntent` usam o PRIVADO `queryIntentActivities(intent, resolvedType, flags, userId)`.
- Mudança: o mesmo filtro (`ComponenteHabilitado.ativo`) no método privado.
- Não verificado no aparelho.

## 4.0.66 (diagnóstico)
- Relatório 06:11 (4.0.65): Police Chief aparece como instalado na Play Store ("Jogar"); Soul Huntress continua "baixando" e `PSR: Package com.pantheraplay.soulhuntress not installed` (mesmo instalado dentro do Vcloud).
- Mudança só de diagnóstico: `VENDING getInstalledPackages flags=... <pacote>:sig=,si=,ai=,v=` (só a Play Store, 1x/min) para ver o que falta no PackageInfo do Soul Huntress (assinatura? signingInfo? versionCode?).

## 4.0.67
- Usuário: o menu de segurar o app (Limpar dados / Parar / Desinstalar app / Criar atalho / Permissões / Root) "só aparece às vezes".
- Causa (código, `AppsFragment.initData`): o menu é criado no toque longo mas só é mostrado ao soltar o dedo (ACTION_UP), e apenas se o toque durou MENOS de 500 ms (`scrollDuration < 500`). Um toque longo normal dura mais que isso → o menu só aparecia se o dedo soltasse rápido.
- Mudança: limite de 500 ms → 15 s. Sem mudança visual.
- Também nesta versão: diagnóstico `VENDING getInstalledPackages` (4.0.66).

## 4.0.68
- Relatório 06:23 (4.0.67): a Play Store volta à tela "Reinicie o app para concluir a atualização" (novo idioma) em ciclo: `Language Split Installed = false` → `Enabling locale changed mode` a cada abertura. O truque de `splitNames` (4.0.48) só estava em `getPackageInfo`; a Play Store agora lê o PackageInfo dela na lista de `getInstalledPackages`, que não tinha `splitNames`.
- Diagnóstico (4.0.66/67) mostrou `sig=null` para TODOS os apps na lista (flags 0x40200000 não pedem assinatura) — então a assinatura (4.0.62) não era o critério. O Soul Huntress (instalado) aparece como "Install Queue: ... Queue only: 1" e `PSR: Package ... not installed`: um pedido de instalação duplicado/preso na fila da loja (duas sessões foram criadas para ele), que a loja trata como "baixando" e não como instalado.
- Mudança: `marcarPartesDaLoja` também na lista de `getInstalledPackages`.
- Não verificado no aparelho.

## 4.0.69
- Usuário: a Central de diagnóstico não acompanha o que acontece ao segurar o ícone do app, e não aparece log disso.
- Causa: nada era registrado no toque longo/menu (`AppsFragment`), e a Central só lia o histórico ao abrir ou em "Atualizar".
- Mudança: eventos `MENU toque longo no app <pacote>`, `MENU soltou o dedo: durou=..ms rolando=.. moveu=.. -> mostra/DESCARTA o menu`, `MENU dedo se moveu (rolagem)`, `MENU escolheu Desinstalar app` (`AppsFragment`); e a Central atualiza sozinha a cada 1,5 s enquanto aberta quando o histórico muda (`DiagnosticoActivity`, sem mudança visual).

## 4.0.70 — logs mais sensíveis
- Pedido do usuário: logs mais sensíveis.
- Mudanças só de registro: `ABRE tela (<app>)` para a Play Store e apps do Google (antes só a Play Store, a cada 5 s; agora a cada 1,5 s por tela) e `CICLO de telas` quando a mesma tela abre 4x em 4 s (ex.: Play Protect entre a loja e o gms); `TOQUE na lista de apps` (dedo desceu), `TOQUE curto abriu o app`, `MENU escolheu o item '<nome>'`, `DESINSTALAR confirmado/terminou/FALHOU` na tela do Vcloud; instalador e getInstalledPackages com limites menores (5 s e 10 s).
- Não muda comportamento.

## 4.0.71 — menu de segurar o app
- Relatório 06:50 (4.0.69): ZERO linhas `MENU ...` ao segurar o ícone → o listener de toque longo do item (`setItemLongClickListener`) nunca é chamado (a primeira linha dele é o log). O usuário sente uma vibração.
- Causa provável (código): `ItemTouchHelper` com `isLongPressDragEnabled()` padrão (true) começa o arrastar no toque longo (vibra) e intercepta o toque; o item recebe CANCEL e o toque longo dele não dispara → menu não abre; só às vezes (corrida entre os dois temporizadores).
- Mudança: `AppsTouchCallBack.isLongPressDragEnabled() = false`; o arrastar para reordenar começa só quando o dedo se mexe depois do toque longo (`AppsFragment` → `startDrag`). Sem mudança visual.
- Não verificado no aparelho.

## 4.0.72
- Relatório 08:28 (4.0.71): **o ciclo do Play Protect está confirmado pelo novo log**: `CICLO de telas: com.android.vending abriu 4x em 4 s -> VERIFY_APPS_SETTINGS` e `com.google.android.gms abriu 4x em 4 s -> com.google.android.finsky.PLAY_PROTECT` (loja e gms se chamando).
- Loja: `Installed: 0 + Install Queue: 2, Queue only: 2 [policewar, soulhuntress]` e `PSR: Package ... not installed`. Lido no dex: a mensagem "PSR: Package %s not installed" sai de `anuk.s(PackageInfo)` quando o PackageInfo da CONSULTA INDIVIDUAL vem nulo. No Vcloud, `PackageManagerCompat.generatePackageInfo(ps,...)` engolia qualquer exceção e devolvia null (ex.: ao montar `signingInfo`, `p.mSigningDetails`), o que a loja lê como "não instalado".
- Mudança: (1) exceção ao montar o PackageInfo agora é registrada (`PACOTE getPackageInfo falhou ...`); (2) falha só da assinatura não apaga mais o PackageInfo (try/catch isolado); (3) `VENDING getPackageInfo SEM resultado para <pacote>` quando a loja pergunta de um app instalado dentro e não recebe nada.
- Também visto: `SigningDetails.UNKNOWN` (singleton) é alterado por pacote em `generatePackageInfo` — risco de misturar assinaturas; não mexido.
- Não verificado no aparelho.

## 4.0.73 — partes (splits) de verdade
- Relatório 09:13 (4.0.72): Soul Huntress (instalado pela Play Store) abre e roda (Unity), mas `Unity: RemoteProviderException : TextDataProvider : unable to load from url jar:file:///.../base.apk!/ass...` → faltam arquivos que estão nas partes (UnityDataAssetPack, config.*), que o instalador só usava para copiar `.so`.
- Mudança: `InstaladorVirtual.copiarParte` guarda cada parte (APK) em `data/app/<pacote>/splits/<nome>.apk`; `PackageManagerCompat.adicionarPartes` põe `splitSourceDirs/splitPublicSourceDirs/splitNames` no `ApplicationInfo` (reflexão). O Android monta classes/recursos/assets com elas.
- Só vale para instalações NOVAS (as partes antigas já foram apagadas); reinstalar o app pela Play Store.
- Risco: apps com partes que dependem de `splitDependencies`/`isolatedSplits` não são tratados. Não verificado no aparelho.

## 4.0.74 — monitor detalhado
- Pedido do usuário: monitorar dentro do Vcloud tudo que acontece e como, e tudo que acontece internamente com os apps.
- Novo `utils/Monitor` (arquivo próprio `blackbox/monitor.log`, gira em 4 MB; o relatório traz o final, 1,5 MB, na seção "MONITOR DETALHADO"). Categorias: CICLO-DE-VIDA (processo iniciado, `Application.onCreate` em N ms), TELA (toda tela aberta por qualquer app de dentro), SERVICO-INICIA / BIND (serviços), AVISO (broadcasts enviados), PROVEDOR, PERMISSAO, SERVICO / ERRO-SERVICO / LENTO (toda chamada de um app de dentro aos serviços do Android passa por `ClassInvocationStub.invoke`: 1ª chamada, todo erro, chamadas > 500 ms), RESUMO-60s (contagem, erros e tempo médio por chamada, a cada 60 s por processo), SAUDE (memória, threads, arquivos abertos a cada 60 s).
- Chamadas muito frequentes não geram uma linha cada: viram contagem no RESUMO. Métodos com token/senha/credencial só têm o nome gravado.
- Não muda comportamento. Sem mudança de tela (a Central não mostra o monitor ainda; vai no relatório). Não verificado no aparelho.

## 4.0.75
- Relatório 09:31 (4.0.74): o Soul Huntress abre (Unity) mas `RemoteProviderException : TextDataProvider : unable to load from url jar:file:///.../base.apk!/assets/aa/settings.json (404)` → é a instalação ANTIGA (sem as partes; não há `INSTALADOR parte guardada` no relatório). Precisa desinstalar e instalar de novo pela Play Store com a 4.0.73+.
- Monitor: o relatório mostrou ruído de `OsStub.*` (toda leitura/escrita de arquivo, e o próprio monitor se registrando). Mudança: trava contra o monitor registrar a si mesmo; `OsStub` só entra na contagem do RESUMO (e em "LENTO" se > 500 ms), não gera linha por chamada nem por erro.

## 4.0.76
- Relatório 09:44 (4.0.75): `CICLO de telas` do Play Protect continua (vending → gms `VERIFY_APPS_SETTINGS` 4x/4 s; gms → `com.google.android.finsky.PLAY_PROTECT`). A filtragem por `android:enabled` (4.0.64/65) não adiantou: a própria loja liga o alias em tempo de execução (estado 1 em `ComponenteHabilitado`) ou o estado não chega ao filtro.
- Mudança: `ComponenteHabilitado.ativo` devolve falso sempre para `com.android.vending/.../ExternalPlayProtectHomeEntryPoint` (nome fixo da versão 53.8.11), para o gms não achar a loja e mostrar o Play Protect dele.
- Soul Huntress: esta cópia ainda é a ANTIGA (sem `INSTALADOR parte guardada`; erro 404 de `assets/aa/settings.json`); precisa reinstalar pela loja com 4.0.73+.
- Visto no monitor (erros de serviços, para depois): `IStorageManager.getCacheQuotaBytes` (STORAGE_INTERNAL), `IStorageStatsManager.queryStatsForPackage` (PACKAGE_USAGE_STATS), `IAppSearch.initialize` (uid do gms ≠ origem), `IPersistentDataBlock.getFlashLockState`, `IPackageManager.getPackageInfo("com.android.vending", 0x400000)` → InvocationTargetException.

## 4.0.77 — por que a loja nunca marcava "instalado"
- Relatório 10:11 (4.0.76): Play Protect funciona. Soul Huntress reinstalado: `INSTALADOR parte guardada` (config.arm64_v8a 43 MB, UnityDataAssetPack 886 MB) e `resultado enviado por broadcast direto`. Mesmo assim: `PSR: Package ... not installed`, `IV2::PSV: Package ... was not installed`, e o NOVO log da 4.0.72 mostrou a causa: `PACOTE getPackageInfo falhou para <pacote> (flags=4980736): SecurityException: MATCH_ANY_USER flag requires INTERACT_ACROSS_USERS permission: UID 10454`.
- Causa: a loja consulta com MATCH_ANY_USER (0x00400000); `PackageManagerCompat.generatePackageInfo` repassa as flags à consulta REAL do celular (`getPackageInfo` do pacote, para pegar assinaturas) e `generateApplicationInfo` também (`getApplicationInfo` do Vcloud) → SecurityException → PackageInfo nulo → "não instalado". Explica a loja nunca reconhecer apps (e o `Queue only`).
- Mudança: `semParaTodosUsuarios(flags)` tira MATCH_ANY_USER nas duas consultas reais; qualquer outra falha da consulta real ao celular agora só é registrada (`PACOTE consulta ao celular de ... falhou`), sem apagar o PackageInfo.
- Não verificado no aparelho.

## 4.0.78
- Usuário (12:00): app instala e abre normalmente; a loja continua mostrando o Soul Huntress em "Pausar/X" (instalando) e o Police Chief como "Não foi possível instalar".
- Relatório 11:59 (4.0.77): `PACOTE ...` sem erros (4.0.77 resolveu o MATCH_ANY_USER). Novo erro no monitor: `IPackageManagerProxy.getApplicationEnabledSetting("com.pantheraplay.soulhuntress", 0) -> IllegalArgumentException: Unknown package` (chamada ao PackageManager REAL, que só conhece apps do celular) logo antes de `PSR: ... not installed` / `IV2::PSV: ... was not installed` (status 6252).
- Mudança: hook `getApplicationEnabledSetting` devolve `COMPONENT_ENABLED_STATE_DEFAULT` para app instalado dentro do Vcloud.
- Não verificado no aparelho. Se persistir, outras perguntas ao PackageManager real (ver ERRO-SERVICO IPackageManagerProxy.* no monitor).

## 4.0.79 — desinstalar e fim da instalação pela loja
- Relatório 13:07 (4.0.78) + prints: Soul Huntress "Desinstalando..." sem fim na loja (já saiu do Vcloud pelo menu) e Police Chief aparece como "Instalar" mesmo instalado.
- Monitor: (1) `IPackageManagerProxy.deletePackageAsUser("com.pantheraplay.soulhuntress", -1, <observer>, 0, 0) -> SecurityException: ... DELETE_PACKAGES` — o botão "Desinstalar" da loja usa a API antiga; (2) `IPackageManagerProxy.setApplicationCategoryHint("com.slg.policewar", 0, "com.android.vending") -> SecurityException: Package com.android.vending does not belong to 10454` logo antes de `state:INSTALL_ERROR status_code=6200` (policewar instalou em 13:06:36 e a loja marcou erro 13:06:37).
- Mudança: hook `deletePackageAsUser` (app de dentro → `uninstallPackageAsUser` + `packageDeleted(pkg, 1)` no observador; Google de dentro → falha 1; nunca mexe em app do celular) e hook `setApplicationCategoryHint` (resposta vazia para app de dentro).
- Não verificado no aparelho.

## 4.0.80 — "Desinstalando..." sem fim
- Relatório 13:31 (4.0.79): `DESINSTALAR pedido da loja (deletePackageAsUser): com.slg.policewar`, depois (3 s) `deletePackageAsUser -> InvocationTargetException` (loja repetiu o pedido; o pacote já saíra do Vcloud, então a chamada foi ao PackageManager REAL e falhou).
- Causa provável: o observador da loja é `IPackageDeleteObserver2`, cujo método é `onPackageDeleted(String,int,String)`; o hook só tentava `packageDeleted(String,int)` e engolia o erro → a loja nunca recebia resposta.
- Mudança: o hook chama `onPackageDeleted` (com fallback `packageDeleted`); pacote que não está no Vcloud e falha no celular também responde (-1) em vez de estourar.
- Não verificado no aparelho (nome do método do observador é deduzido do AIDL do Android, não visto no log).
