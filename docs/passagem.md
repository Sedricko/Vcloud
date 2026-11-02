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
- A cada APK entregue ao usuário, subir `versionCode`/`versionName` em `android/build.gradle` (o instalador mostra "já está instalada" se for igual). Atual: 4.0.23 (423).

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
