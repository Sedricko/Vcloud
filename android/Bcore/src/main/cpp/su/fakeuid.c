// Vcloud: faz os programas que o "su" abre verem uid/gid 0 (root de mentira, só dentro do Vcloud).
#include <sys/types.h>
#include <unistd.h>

__attribute__((visibility("default"))) uid_t getuid(void) { return 0; }
__attribute__((visibility("default"))) uid_t geteuid(void) { return 0; }
__attribute__((visibility("default"))) gid_t getgid(void) { return 0; }
__attribute__((visibility("default"))) gid_t getegid(void) { return 0; }
__attribute__((visibility("default"))) int getresuid(uid_t *r, uid_t *e, uid_t *s) { *r = *e = *s = 0; return 0; }
__attribute__((visibility("default"))) int getresgid(gid_t *r, gid_t *e, gid_t *s) { *r = *e = *s = 0; return 0; }
