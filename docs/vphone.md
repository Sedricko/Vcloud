# Análise do vPhoneOS 5.2.1 (31/10)

Arquivo: `VPhoneOS_Emulator_5.2.1_APKPure` (XAPK, versionCode 6816, minSdk 29 = Android 10, targetSdk 36 = Android 16).
Como olhei: li os arquivos como dados (lista de classes e métodos do Java, textos e símbolos das bibliotecas nativas,
manifesto). **Não rodei nada** e não descompilei o corpo dos métodos (o descompilador de fora não foi autorizado).
O APK não foi para o repositório.

Legenda: **certeza** = está escrito nos arquivos; **palpite** = dedução a partir do que está escrito.

## 1. Identidade (certeza)
- Nome real: **VPhoneGaGa**. Código em `com.vphonegaga.titan`; pasta do autor `/Users/ychuang/Documents/VPhoneGaGa_master`.
- O pacote `com.yoyo.snake.rush` (cobra) é só disfarce de nome na loja.
- Fechado. Servidores: `vphoneos.com`, `user.vphoneos.com`, `web.vphoneosapp.com`.
- Tem anúncios, compras (Google Play, AliPay, WeChat) e VIP; Magisk dentro exige VIP (`popMagiskInstallRequireVIPDialog`).

## 2. Tipo
**Android inteiro dentro do app** (como o Twoyi), não espaço virtual de apps (como o BlackBox).

## 3. As peças e o que cada uma faz
| Peça | Evidência | O que faz |
|---|---|---|
| App Java (`com.vphonegaga.titan`, ~970 classes próprias) | `VPhoneManager`, `VPhoneInstance1..N`, `MyNativeActivity1..N` | tela, menus, download da ROM, teclado virtual/mapa de teclas, transferência de arquivos, VPN, Steam |
| `libVPhoneGaGaLib.so` | métodos `StartGaGaOs`, `StopGaGaOs`, `flashBootImage`, `importApk`, `updateRootSwitch`, `mapSharedMemory` | a ponte entre o Java e o motor: liga/desliga o Android de dentro, instala APKs nele, liga o root |
| `libuserkernel.so` | exporta `main`, `init_kernel_api`, `inject_input_event`; textos `binder_proc`, `binder_node`, `binder_transaction`, `ashmem_create_region`, `selinuxfs`, `/dev/__properties__`, `block.img` | o **kernel de mentira**: um programa que faz o papel do kernel do Linux. Tem **sua própria cópia do binder** (o "correio" entre apps do Android), do ashmem (memória compartilhada), do SELinux e dos discos |
| `libloader.so` | `/system/bin/linker`, `fork`, `execve`, `waitpid stopped pid=%d`, `/proc/%d/maps`, `/proc/%d/mem`, `seccomp SECCOMP_SET_MODE_FILTER` | um **carregador de programas próprio**: abre os programas do Android de dentro, cria os processos e liga o filtro que desvia as chamadas de sistema para o kernel de mentira |
| `libOpenglRender.so` | `emugl`, `FrameBuffer`, `ANDROID_EMU_gles_max_version` | desenho na tela: renderizador do **emulador oficial do Android** (aberto, AOSP) |
| `libsdk29helper.so` | `IPackageManagerNative`, `libbinder_ndk` | fala com o Android **de verdade** (lista de apps etc.) |
| ROM | `user.vphoneos.com/get_android.php?rom=%d_%s`, `android10_rom`, `Android14`, `popMissingAndroid32BitRomDialog` | o Android de dentro **não vem no APK**: é baixado como disco (`block.img`, `boot.img`) |
| Extras | `wine_data` (DXVK, vkd3d, vcredist), `libXlorie.so` (Termux:X11), `in.dragonbra.javasteam` | jogos de **Windows** e da **Steam** (estilo Winlator). Não interessa para nós |

## 4. Como funciona, passo a passo (palpite forte)
1. O app abre um serviço por "celular virtual" (`VPhoneInstance1`, `2`, ... — até 256 no manifesto), cada um no seu processo.
2. Esse serviço inicia o `libuserkernel.so` como um programa (ele tem `main`).
3. O `libloader.so` carrega o `init` do Android de dentro, cria os processos com `fork` e liga o **seccomp** (filtro do
   kernel de verdade). Toda chamada de sistema que o Android de dentro faz ("abre o binder", "quem sou eu?") é desviada
   para o kernel de mentira responder. Também usa `ptrace` (o mesmo mecanismo de depurador) para vigiar os processos.
4. Os processos do Android de dentro (inclusive o jogo e o Unit) são **processos reais do celular**, com o **mesmo
   usuário Android do app vPhone**. Prova: o app avisa quando o Android 12+ mata processos-filho demais
   (`maybeKilledByPhantomProcessKiller`, `popKilledByPhantomProcessKillerDialog`, tela `RmProcesslimitActivity`).
5. O "root" lá dentro é **de mentira**: quando um processo pergunta "quem sou eu?", o kernel de mentira responde "root".
   O celular continua sem root (`updateRootSwitch`, `magisk_patched-`, `u:r:magisk:s0`).
6. A imagem sai pelo `libOpenglRender` (o Android de dentro manda os comandos de desenho, o app desenha com a placa de
   vídeo de verdade).

## 5. Por que o Unit funciona lá dentro (palpite forte)
O kernel de mentira conhece `process_vm_readv` / `process_vm_writev` e `/proc/%d/mem` (estão nos textos). Quando o
`unitd` (com root de mentira) pede para ler a memória do Orecraft, o kernel de mentira repassa o pedido ao kernel de
verdade, e o kernel de verdade aceita **porque os dois processos são do mesmo usuário** (o app vPhone).

## 6. O que aprendemos para o Vcloud
1. **Confirma a ideia da F2:** "mesmo usuário Android = pode ler a memória" funciona no Android 16. É o mesmo princípio
   que o BlackBox usa, só que o BlackBox não precisa de um Android inteiro nem de um kernel de mentira.
2. **Cuidado já conhecido:** o Android 12+ mata processos-filho em excesso (phantom process killer). O vPhone tem tela
   própria para isso; o nosso hospedeiro pode precisar do mesmo aviso.
3. **Não dá para usar como base:** é fechado (copiar é proibido) e o kernel de mentira, com binder próprio, é trabalho
   de anos. As partes abertas que ele usa (emugl do AOSP) são as mesmas do Twoyi, já descartado.

## 7. Conclusão
Continuar com o **BlackBox** como base (F0). O vPhone é a prova de que o princípio funciona.

## 8. O que não sei / não conferi
- O APKPure mandou só a parte de **32 bits** (armeabi-v7a); a de 64 bits (a do S25) não veio. Os nomes das peças devem
  ser os mesmos, mas não conferi.
- Não li o corpo dos métodos Java nem desmontei o código nativo; só nomes, textos e símbolos.
- Não sei se o desvio das chamadas usa só seccomp, só ptrace ou os dois juntos (há sinais dos dois).
- Não sei qual é a versão exata do Android de dentro: ela vem do servidor, e não baixei.
