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
Motivo: regras do projeto (não enganar verificações de segurança; nada de enviar dados para fora).
