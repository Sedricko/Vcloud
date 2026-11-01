// Vcloud su: pede ao Vcloud (socket abstrato "vcloud_su") se este app pode ter root e, se sim, abre o sh.
// Não é root de verdade: o Vcloud decide quem pode (painel) e, com a biblioteca libfakeuid.so, o app vê uid 0.
#define _GNU_SOURCE
#include <errno.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <unistd.h>

#ifndef VSU_SH
#define VSU_SH "/system/bin/sh"
#endif
#define SOCK_NAME "vcloud_su"
#define VSU_VERSION "1.0:VCLOUD"

static int conectar(void) {
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) return -1;
    struct sockaddr_un a;
    memset(&a, 0, sizeof a);
    a.sun_family = AF_UNIX;
    memcpy(a.sun_path + 1, SOCK_NAME, strlen(SOCK_NAME));
    socklen_t len = offsetof(struct sockaddr_un, sun_path) + 1 + strlen(SOCK_NAME);
    if (connect(fd, (struct sockaddr *) &a, len) < 0) { close(fd); return -1; }
    return fd;
}

// Troca quebras de linha por espaço: o protocolo é de uma linha por campo.
static void uma_linha(char *s) {
    for (; *s; s++) if (*s == '\n' || *s == '\r') *s = ' ';
}

static int escrever_tudo(int fd, const char *p, size_t n) {
    while (n > 0) {
        ssize_t w = write(fd, p, n);
        if (w < 0) { if (errno == EINTR) continue; return -1; }
        p += w; n -= (size_t) w;
    }
    return 0;
}

// Lê uma linha da resposta (até '\n'); devolve o tamanho ou -1.
static int ler_linha(int fd, char *buf, size_t max) {
    size_t i = 0;
    while (i + 1 < max) {
        char c;
        ssize_t r = read(fd, &c, 1);
        if (r < 0) { if (errno == EINTR) continue; return -1; }
        if (r == 0) break;
        if (c == '\n') break;
        buf[i++] = c;
    }
    buf[i] = 0;
    return (int) i;
}

// Caminho irmão do próprio executável (a pasta de bibliotecas nativas do Vcloud).
static int irmao(const char *nome, char *out, size_t max) {
    char exe[4096];
    ssize_t n = readlink("/proc/self/exe", exe, sizeof exe - 1);
    if (n <= 0) return -1;
    exe[n] = 0;
    char *barra = strrchr(exe, '/');
    if (!barra) return -1;
    barra[1] = 0;
    return snprintf(out, max, "%s%s", exe, nome) < (int) max ? 0 : -1;
}

// O Android pode recusar LD_PRELOAD de uma pasta do app para o sh do sistema (e o sh nem abriria).
// Só usa a biblioteca se um sh de teste rodar com ela.
static int preload_funciona(const char *lib) {
    pid_t pid = fork();
    if (pid < 0) return 0;
    if (pid == 0) {
        setenv("LD_PRELOAD", lib, 1);
        if (!freopen("/dev/null", "w", stdout) || !freopen("/dev/null", "w", stderr)) _exit(127);
        execl(VSU_SH, "sh", "-c", "exit 0", (char *) NULL);
        _exit(127);
    }
    int st = 0;
    if (waitpid(pid, &st, 0) < 0) return 0;
    return WIFEXITED(st) && WEXITSTATUS(st) == 0;
}

int main(int argc, char **argv) {
    char *cmd = NULL;
    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "-v") || !strcmp(argv[i], "--version")) { puts(VSU_VERSION); return 0; }
        if (!strcmp(argv[i], "-V")) { puts("1"); return 0; }
        if ((!strcmp(argv[i], "-c") || !strcmp(argv[i], "--command")) && i + 1 < argc) { cmd = argv[++i]; continue; }
        if (!strcmp(argv[i], "-s") && i + 1 < argc) { i++; continue; } // shell pedido: sempre o sh do sistema
        // "-", "-l", "-m", "-p" e o nome do usuário ("root", "0") não mudam nada aqui
    }

    int fd = conectar();
    if (fd < 0) { fputs("su: o Vcloud nao esta respondendo\n", stderr); return 1; }

    char linha[8192];
    const char *pkg = getenv("VCLOUD_PKG");
    char comando[4096];
    snprintf(comando, sizeof comando, "%s", cmd ? cmd : "(shell interativo)");
    uma_linha(comando);
    char dica[512];
    snprintf(dica, sizeof dica, "%s", pkg ? pkg : "");
    uma_linha(dica);
    int n = snprintf(linha, sizeof linha, "VSU1\npkg %s\ncmd %s\n", dica, comando);
    if (n < 0 || n >= (int) sizeof linha || escrever_tudo(fd, linha, (size_t) n) < 0) {
        fputs("su: falha ao falar com o Vcloud\n", stderr);
        return 1;
    }

    char resp[512];
    if (ler_linha(fd, resp, sizeof resp) < 0) { fputs("su: sem resposta do Vcloud\n", stderr); return 1; }
    close(fd);
    if (strncmp(resp, "ALLOW", 5) != 0) {
        if (strncmp(resp, "DENY ", 5) == 0) fprintf(stderr, "su: permissao negada: %s\n", resp + 5);
        else fputs("su: permissao negada\n", stderr);
        return 1;
    }

    char lib[4200];
    if (irmao("libfakeuid.so", lib, sizeof lib) == 0 && access(lib, R_OK) == 0 && preload_funciona(lib)) {
        const char *antigo = getenv("LD_PRELOAD");
        if (antigo && *antigo) {
            char junto[8400];
            snprintf(junto, sizeof junto, "%s:%s", lib, antigo);
            setenv("LD_PRELOAD", junto, 1);
        } else {
            setenv("LD_PRELOAD", lib, 1);
        }
    }

    if (cmd) execl(VSU_SH, "sh", "-c", cmd, (char *) NULL);
    else execl(VSU_SH, "sh", (char *) NULL);
    fprintf(stderr, "su: nao consegui abrir o shell: %s\n", strerror(errno));
    return 127;
}
