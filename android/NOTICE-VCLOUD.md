# Origem do motor

Esta pasta parte do **NewBlackbox** (licença Apache 2.0, ver `LICENSE`):
github.com/LamPPKK/NewBlackbox, commit `6e6d90137213148af5a19df3f3ffd60cbf2700f3` (continuação de ALEX5402/NewBlackbox, que vem do BlackBox/FBlackBox).

## Mudanças do Vcloud
- Desligado o envio automático de registros de erro (logcat + dados do aparelho) para um servidor de terceiros
  (`ClientConfiguration.getLogSenderChatId` devolve null).
- Removido o "esconder root" (`BlackBoxCore.isHideRoot` sempre false).
- Removido o que pula as verificações de ambiente virtual dos apps (`BActivityThread.isAntiDetectProvider`).
- Removidos do build `AntiDetection.cpp` (esconde Magisk/Xposed/su) e `VirtualSpoof.cpp` (fingia ser Pixel 6 /
  Android 12 com série falsa); apagado `AntiVirtualDetectProxy.java` (não era usado).
- Removida a automação do autor que mandava os builds para o Telegram (`.github`).
- Tirado o pedido de permissão de VPN ao abrir o app (a VPN do motor não encaminha tráfego; só liga no "modo VPN").
- Relatório de erros agora fica **no próprio celular** (`Download/Vcloud/relatorio-*.txt`): o envio automático foi trocado por gravação local (`BlackBoxCore.sendLogs/writeReport`), `LogSender.java` apagado, menu "Gerar relatório de erros" no lugar do link do Telegram.
- **Android 17 (API 37):** `ServiceConnectionDelegate` ganhou `connected(..., IBinderSession, boolean)` (o sistema mudou a assinatura; sem isso os apps fechavam com AbstractMethodError) e stub `android/app/IBinderSession.java` só para compilar.
- Opções removidas da tela: ocultar root, rede VPN, permitir captura de tela (agora sempre desligados no loader), localização falsa e link de código aberto no menu.
- Identidade própria: `applicationId "com.vcloud.app"` e APK `Vcloud_*.apk` (classes internas continuam em `top.niunaijun.*`).
- Limpeza do app (telas): apagados localização falsa (view/fake, RockerManager, RockerView, EnFloatView, osmdroid), telas de Xposed e 27 textos sem uso.
- **Nome Vcloud em tudo:** pacotes `top.niunaijun.blackbox` → `com.vcloud.motor`, `top.niunaijun.blackboxa` →
  `com.vcloud.app`, `top.niunaijun.blackreflection` → `com.vcloud.reflexao`, `top.niunaijun.jnihook` → `com.vcloud.jnihook`;
  `BlackBoxCore` → `VcloudCore`, `BlackBoxSystem` → `VcloudSystem`, `BlackBoxLoader` → `VcloudLoader`; textos "BlackBox" →
  "Vcloud"; projeto `Vcloud`; tema `Theme.Vcloud`. Apagados README/Docs/RELEASE_NOTES/docs/assets do projeto de origem.
  (Ficam só nomes internos de pastas de dados em minúsculas, invisíveis ao usuário.)
- Ícone próprio do Vcloud (nuvem com V); chave de assinatura fixa de teste (`vcloud-debug.keystore`) para os APKs instalarem por cima; Play Store escondida na tela a menos que a opção esteja ligada.
- Android 17: `ProxyService` sempre cumpre o startForeground (evita ForegroundServiceDidNotStartInTime); `startActivity` marca o intent repassado com `removeLaunchSecurityProtection` (proteção Intent Redirect do Android 16+/17).
Motivo: regras do projeto (não enganar verificações de segurança; nada de enviar dados para fora).
