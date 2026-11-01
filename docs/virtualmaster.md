# Análise do Virtual Master 3.2.86 (01/11)

Arquivo: `com.clone.android.dual.7z` → `com.clone.android.dual.apk` (162 MB). Pacote real: `com.clone.android.dual.space`.
Como olhei: igual ao vPhoneOS (`docs/vphone.md`): li os arquivos como dados, sem rodar nada. Também contei, no código
nativo de 64 bits, quais chamadas de sistema aparecem escritas diretamente (script `svcscan`, só lê bytes).
O APK não foi para o repositório.

Legenda: **certeza** = está escrito nos arquivos; **palpite** = dedução.

## 1. Identidade (certeza)
- Código próprio em `com.android.vmapp` (telas) e `com.android.vmcore` (motor). Nomes de propósito parecidos com os do
  Android, mas não são do Google.
- **Muito embaralhado** (ofuscado): a maioria das classes e métodos virou letras sem sentido; as bibliotecas nativas têm
  os textos cifrados e os símbolos trocados por `x.123`, `y.456`.
- Os "plugins" `play.zip` (Google Play, 102 MB), `magisk.zip`, `xposed.zip` e `superuser.zip` estão **cifrados**
  (não são zip de verdade).
- Tem anúncios, assinatura (billing) e login.
- Fechado.

## 2. Tipo
Também **Android inteiro dentro do app** (como o vPhoneOS e o Twoyi). A ROM não vem no APK: o código fala em `rom.zip`
e `rom_asset` (palpite: vem como pacote extra da Play Store ou download).

## 3. As peças
| Peça | Evidência | O que faz |
|---|---|---|
| `VMCoreService`, `VMStartActivity0..3`, `VMDisplayActivity` | manifesto e classes | liga até 4 celulares virtuais e mostra a tela deles (inteira, janela pequena, bolinha flutuante) |
| `NativeHelper` | `startOS`, `getProcessList`, `clearZombieProcess`, `buildVMProcessReport` | liga o Android de dentro e cuida dos processos dele |
| `BinderService` | `setupBinder`, `getBinderVersion` | prepara o binder (o "correio" entre apps) do Android de dentro. Como ele faz isso não deu para ver |
| `hal/*` | `DisplayService`, `InputService`, `AudioService`, `HALManager` (câmera, GPS, sensores, vibração), `NetlinkManager` | o "hardware de mentira": a tela, o toque, o som, a câmera etc. do Android de dentro são entregues pelo app |
| `vpn/*` | `VpnManager`, `VpnTunService` | a internet do Android de dentro passa por uma VPN local do app |
| `libvm.so` | usa `vfork`, `execve`, `waitpid`, `syscall`, `ANativeWindow_fromSurface`; caminho `/data/data/com.clone.android.dual.space/vm/vm%d/dev/input/touch` | o motor: inicia os processos e desenha a tela. Cada celular virtual tem uma pasta `vm/vmN` com o seu próprio `/dev` de mentira |
| `libkrloader64.so` | é um programa sozinho (`_start`, sem depender de nenhuma biblioteca); tem `/dev/__properties__`, `/dev/socket/property_service` | um **carregador próprio** que abre os programas do Android de dentro |
| `libkr64.so`, `libkr64.11.so`, `libkr64.12.so` (e 32 bits) | usam `clone`, `fork`, `prctl`, `android_dlopen_ext`; símbolos `AndroidRuntime::startVm` e do linker | peças que entram **dentro** dos processos do Android de dentro. O `.11` e o `.12` são uma versão para cada Android de dentro (palpite: Android 11 e 12) |
| `com.phantom.killer.disabler` | `isAdbPaired`, `directUnlock`, `ShizukuActivity`, avisos de "depuração sem fio" | **desliga o matador de processos do Android 12+**, pedindo para o usuário parear a depuração sem fio (ADB) ou usar o Shizuku |

## 4. Como funciona (palpite)
1. O app cria uma pasta por celular virtual (`vm/vm0`, `vm/vm1`...) com os arquivos do Android de dentro e um `/dev` falso.
2. O `libvm` inicia o carregador `libkrloader64`, que abre o Android de dentro **como processos comuns do app**.
3. As peças `libkr` entram nesses processos e trocam o que o Android de dentro precisa do sistema (propriedades, binder,
   caminhos) pelo que o app oferece.
4. Tela, toque, som, câmera e internet passam pelo app (`hal`, `vpn`).
5. Diferente do vPhoneOS, **não achei sinal de "kernel de mentira" completo**, nem de `ptrace` ou `seccomp` escritos
   diretamente no código. Ele parece usar um caminho mais leve: troca peças dentro dos processos. Mas como está
   ofuscado, posso estar errado.

## 5. Comparação com o vPhoneOS
| | vPhoneOS | Virtual Master |
|---|---|---|
| Tipo | Android inteiro | Android inteiro |
| Truque principal | kernel de mentira + seccomp/ptrace, binder próprio | carregador próprio + peças dentro dos processos (palpite) |
| Android de dentro | baixado (Android 10/14 citados) | baixado (palpite: 11 e 12) |
| Processos do jogo | processos reais do app, mesmo usuário | processos reais do app, mesmo usuário (palpite forte: `getProcessList`, `clearZombieProcess`, matador de processos) |
| Matador de processos do Android 12+ | só avisa | desliga via ADB sem fio / Shizuku |
| Ofuscação | pouca | muita |

## 6. O que aprendemos para o Vcloud
1. **Os dois confirmam o princípio da F2:** o jogo vira um processo comum do app hospedeiro, com o mesmo usuário Android.
2. **O matador de processos do Android 12+ é problema real:** os dois tratam dele. O Virtual Master mostra um jeito sem
   root (ADB sem fio / Shizuku). Vale anotar para o nosso hospedeiro (um espaço virtual como o BlackBox também cria
   processos, mas são processos "de verdade" do Android, então o risco deve ser menor — confirmar na F0/F1).
3. **Não serve de base:** fechado, ofuscado e com plugins cifrados.

## 7. Conclusão
Continua o **BlackBox** como base (F0). Dois emuladores que funcionam com o Unit, feitos de jeitos diferentes, chegam
ao mesmo ponto: o jogo roda como processo do app. É exatamente o que o nosso hospedeiro precisa.

## 8. O que não sei / não conferi
- Por causa da ofuscação, não sei como ele monta o binder nem se usa ptrace/seccomp por caminhos indiretos.
- Não abri os plugins (cifrados) nem a ROM (não vem no APK).
- Não sei a versão exata do Android de dentro; o `.11`/`.12` é palpite pelo nome.
