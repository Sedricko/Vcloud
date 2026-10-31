# Análise do vPhoneOS 5.2.1 (31/10)

Arquivo: `VPhoneOS_Emulator_5.2.1_APKPure` (XAPK, versionCode 6816, minSdk 29, targetSdk 36).
Só olhei os arquivos e os textos de dentro deles: não rodei, não descompilei o Java. O APK não foi para o repositório.

## O que é (certeza: aparece nos arquivos)
| Peça | O que achei | O que quer dizer |
|---|---|---|
| Nome real | código em `com.vphonegaga.titan`, pasta do autor `VPhoneGaGa_master` | é o **VPhoneGaGa**; o pacote `com.yoyo.snake.rush` é só disfarce de nome |
| `libuserkernel.so` | `/binder`, `/dev/block/...`, `block.img`, `boot.img`, `PTRACE_*`, `/proc/%d/mem` | um **"kernel de mentira"** feito em programa comum: imita o kernel do Linux para um Android inteiro rodar dentro do app |
| `libloader.so` | `seccomp SECCOMP_SET_MODE_FILTER`, `UserKernelLoader` | usa o filtro seccomp para **interceptar as chamadas de sistema** dos processos do Android de dentro |
| ROM | `user.vphoneos.com/get_android.php?rom=...`, `android10_rom`, `Android14` | o Android de dentro **não vem no APK**: é baixado depois, como um disco (`block.img`) |
| `libOpenglRender.so` | `emugl`, `FrameBuffer`, `ANDROID_EMU_gles_max_version` | o desenho na tela usa o renderizador do **emulador oficial do Android** (aberto, AOSP), o mesmo caminho do Twoyi |
| Extras | `wine_data` (DXVK, vkd3d), `libXlorie.so` (Termux:X11) | também roda jogos de **Windows** (estilo Winlator). Não interessa para nós |

## Tipo
**Android inteiro dentro do app** (como o Twoyi), não espaço virtual de apps (como o BlackBox).
O root que existe lá dentro é **de mentira**: quem dá é o kernel de mentira; o celular continua sem root.

## O que aprendemos para o Vcloud
1. **Confirma a ideia da F2 (palpite forte):** dentro do vPhone, todo processo do Android de dentro é, para o celular de
   verdade, um processo do próprio app vPhone (mesmo usuário Android). É por isso que dá para ler a memória dos jogos
   sem root real. O BlackBox usa o mesmo princípio, só que bem mais leve (sem Android inteiro).
2. **Não dá para usar como base:** é fechado, e o kernel de mentira é trabalho de anos. Copiar é proibido; refazer é
   grande demais.
3. As partes abertas que ele usa (emugl do AOSP) são as mesmas do Twoyi, que já descartamos.

## Conclusão
Continuar com o **BlackBox** como base (F0). O vPhone serve de prova de que o princípio "mesmo usuário = lê a memória"
funciona no Android 16.

## O que não sei / não conferi
- O APKPure mandou só a parte de **32 bits** (armeabi-v7a). A de 64 bits (a do S25) não veio; não olhei.
- Não descompilei o código Java (são 9 arquivos dex), só os textos de dentro.
- Não confirmei se o kernel de mentira usa ptrace, seccomp ou os dois em cada caso.
