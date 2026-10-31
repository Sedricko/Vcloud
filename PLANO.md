# Vcloud: plano

## Objetivo (uma frase)
Rodar um jogo offline dentro do nosso app hospedeiro, para o Unit ler e mudar a memória dele **sem root**.

## Onde estamos (31/10)
- Pesquisa feita (`docs/pesquisa.md`): candidato principal **BlackBox**; Twoyi e VirtualApp descartados.
- **Próximo:** F0, o teste sem programar.

## Fases
| Fase | O que | Pronto quando |
|---|---|---|
| F0 | Testar o BlackBox pronto (APK das releases) com o Orecraft dentro | O Orecraft abre, roda liso e salva dentro dele |
| F1 | Compilar o BlackBox do código (conferir licença e Android 16) | O nosso build roda o Orecraft |
| F2 | O hospedeiro lê a memória do jogo sem root (process_vm_readv, mesmo usuário) | Lê o Cobre do Orecraft certo |
| F3 | Juntar o motor/tela do Unit ao hospedeiro (ou o Unit falar com ele) | Achar, editar e congelar o Cobre sem root |
| F4 | Prova em jogo real e, se o usuário autorizar, no celular de verdade | Orecraft e DYSMANTLE funcionando |

## Regras do projeto
As mesmas do Unit (estão na skill): só offline single-player; nada pago com dinheiro real; nada de anúncios com
recompensa; nada para jogos online nem contra anti-trapaça; não mexer no celular de verdade sem pedir; prévia antes de
mudar tela; ideia fora de ordem vai para um ramo de laboratório.
