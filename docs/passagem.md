# Passagem de bastão — Vcloud (01/11/2026, 08:15 BRT)

Leia também: `PLANO.md`, `android/NOTICE-VCLOUD.md`, `docs/analise-app.md`, `docs/pesquisa.md`.

## O que é
App Android "espaço virtual" (sem Android inteiro): os apps/jogos rodam dentro do Vcloud, no Android do próprio celular.
Motor = fork do NewBlackbox (LamPPKK, Apache 2.0) renomeado: `android/Bcore` (`com.vcloud.motor`, Java + C++) e
`android/app` (telas, `com.vcloud.app`, Kotlin). Usuário fala português; testa no **Samsung S25 (SM-S938B), Android 17
(API 37)**.

## Como compilar e entregar
- Repositório `Sedricko/Vcloud`, ramo **`claude/lucid-euler-fwkwoc`**.
- Cada push em `android/**` dispara `.github/workflows/android.yml` (GitHub Actions) → artefato `vcloud-debug-apk`
  (`Vcloud_4.0.0_arm64-v8a-debug.apk`). Compilar localmente costuma ser bloqueado na sessão; usar o Actions.
- **Chave fixa** de teste: `android/vcloud-debug.keystore` (senha `vcloud-debug`, alias `vcloud`). Todo APK novo
  **instala por cima** (não mudar a chave, senão o usuário perde os dados).
- Relatório de erros: no app, menu ⋮ ou Configurações > "Gerar relatório de erros" → `Download/Vcloud/relatorio-*.txt`
  (logcat do Vcloud e dos apps de dentro; nada sai do celular). O usuário manda o arquivo na conversa.

## Última versão entregue (commit 8de7db2, build run 34 OK)
Funciona no S25: abre, tela inicial Windows 12, ícone novo, Serviços do Google (ativar em Configurações), Orecraft,
DYSMANTLE passa da checagem do Google.

### Correções feitas nesta conversa (todas no ramo)
| Problema | Causa | Correção |
|---|---|---|
| Apps fechavam ao abrir no Android 17 | `IServiceConnection.connected` ganhou `IBinderSession` | `ServiceConnectionDelegate` + stub `android/app/IBinderSession.java` |
| **Tela preta na abertura** (várias versões) | `res/drawable/splash.xml` usava `<bitmap src="@mipmap/ic_launcher">` e o ícone novo é adaptável | `<item android:drawable=...>` |
| Tela inicial podia travar esperando o motor | `MainActivity.initViewPager` pedia usuários ao motor na linha da UI | pede em segundo plano (`montarViewPager`) |
| DYSMANTLE "Something went wrong / assinatura inválida" | motor devolvia uma **Play Store falsa** (`createFakeGooglePlayServicesPackageInfo`) | removida; entrega a Play Store verdadeira |
| Queda do Play Services | broadcast com usuário -1 (todos) recusado no Android 17 | último inteiro <0 vira o usuário do Vcloud (`IActivityManagerProxy.BroadcastIntent`) |
| Serviço caía (Tinker) | `ForegroundServiceDidNotStartInTime` | `ProxyService` tenta `startForeground` sem tipo antes de parar |
| Abrir outro app de dentro (Play Store) | proteção Intent Redirect (Android 16/17) | `removeLaunchSecurityProtection` em `ActivityManagerCommonProxy` |
| Peças do Google na tela | — | escondidas; Play Store só com a chave "Mostrar a Play Store" |
| Permissões dos apps de dentro | — | segurar o app > **Permissões** (pede as perigosas que o app usa; abre "Acesso a todos os arquivos" e "Sobrepor a outros apps" quando precisa) |

Também: nome Vcloud em tudo (pacotes `com.vcloud.*`), português, opções antigas removidas (VPN, ocultar root,
localização falsa, Xposed, captura de tela), telemetria do autor original removida, falsificação de aparelho removida.

## Problemas atuais (para continuar)
1. **DYSMANTLE (Unity) abre em pé/cortado** — deveria ser deitado. A tela `com.the10tons.MainActivity` abre em janela
   1080x2340 e o log "Orientação:" do `AppInstrumentation.checkActivity` **não aparece** para ela. A última versão
   (8de7db2) reaplica a orientação lida do APK em `callActivityOnResume` e registra, com a tag `VcloudOrientacao`,
   os pedidos `setRequestedOrientation` (hook em `IActivityClientProxy`). **Próximo passo:** usuário abre o DYSMANTLE,
   espera ~10 s, gera relatório → ver `VcloudOrientacao` e "Orientação (retomada)".
2. **Play Store de dentro cai em segundo plano** (`ClassCastException` interno, `com.android.vending:background`) e
   "Unable to bind to service ProxyService$P1"; Play Services `CronetInit`. Não impede jogos; investigar depois.
3. **Compra/login do jogo pago** dentro do Vcloud ainda não verificado (depende da conta Google nos Serviços do Google).
4. `docs/analise-app.md`: código faltando (aviso de permissão de arquivos sem receptor; `requestInstallPackage` sempre
   false) e 138 `catch` que escondem erros.

## Pendências pedidas pelo usuário e ainda não feitas
- Tela inicial no conceito Windows 12 **aproveitando a tela toda** (prévia aprovada base: `docs/design/inicio-w12.png`;
  pedidos: busca no topo, Fixados em linha larga, blocos Espaço/Celular menores, Todos os apps ocupando o resto, barra embaixo).
- Painel lateral escondido na borda para voltar/fechar jogo/relatório durante o jogo (alternativa sugerida: notificação
  com botões; ou Painel Edge da Samsung com atalho do Vcloud).
- Ferramenta de diagnóstico (resumo no topo do relatório + aviso quando app de dentro fecha) — ver `PLANO.md`.
- Pedidos de notificações/bateria ao abrir (passo 5): o código anterior está no commit 27aa9d3; reaplicar com cuidado
  (sem rodar nada pesado na abertura).
- Root interno: **não implementado** (apps de root mostram "Cannot run program su").

## Cuidados aprendidos
- Nunca rodar trabalho pesado do motor na abertura (ativação automática dos Serviços do Google foi removida).
- Ícone adaptável: qualquer `<bitmap>` apontando para `@mipmap/ic_launcher` quebra.
- Mudanças uma por vez, com APK testado pelo usuário entre elas; se quebrar, voltar ao último que abriu.
