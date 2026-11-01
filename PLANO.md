# Vcloud: plano

## Objetivo (uma frase)
Um espaço virtual leve para **apps que precisam de root e os jogos em que eles mexem**, com root que só vale lá
dentro, **sem root no celular**. O resto fica no celular normal.

## A ideia
- Sem Android inteiro (diferente do vPhoneOS e do Virtual Master): o jogo roda no Android do próprio celular,
  hospedado pelo nosso app, como processo do mesmo usuário. Por isso o Unit lê a memória sem root.
- **Dentro do Vcloud:** apps de root (Unit, GameGuardian...) e os jogos em que eles mexem (Orecraft, DYSMANTLE).
  Um app de root só enxerga o que está dentro.
- **No celular normal:** todo o resto (WhatsApp, banco, jogos online, Play Store). Fica fora do alcance dos apps de root.
- **Root do Vcloud:** `su` interno com janela "permitir root?", só vale lá dentro. Chave por app: ligada nos apps de
  root, desligada nos jogos.
- **Natural de usar:** atalho na tela inicial, mesmas pastas do celular, mesma internet, notificações e copiar e colar.
- Meta: Android 7 ao 16.

## Onde estamos (01/11)
- Analisados: vPhoneOS (`docs/vphone.md`) e Virtual Master (`docs/virtualmaster.md`). Os dois confirmam o princípio
  "mesmo usuário = lê a memória". Os dois são fechados: só servem para aprender.
- **Próximo:** F0, o teste sem programar.

## Fases
| Fase | O que | Pronto quando |
|---|---|---|
| F0 | Testar o BlackBox pronto com o Orecraft dentro | Abre, roda e salva |
| F1 | Compilar o BlackBox e cortar o que não usamos (espaço mínimo) | O nosso build roda o Orecraft |
| F2 | O hospedeiro lê a memória sem root | Lê o Cobre certo |
| F3 | Juntar com o Unit | Achar, editar e congelar o Cobre sem root |
| F3c | Root do Vcloud (`su` interno com janela de permissão) | Um app pede root e recebe, só lá dentro |
| F3d | Pastas: mesmas do celular + pasta dos saves visível; opção "isolado" | Save do Orecraft com backup pelo app Arquivos |
| F4 | Testar em Android antigo e novo (vPhone, outro emulador, S25 se autorizar) | Funciona no 7 e no 16 |
| Lab | Plano B: APK do jogo com uma pecinha nossa | Só se algum jogo não rodar no espaço |

## Cuidados já conhecidos
- Matador de processos do Android 12+ (os dois emuladores tratam dele). Testar na F0/F1.
- Apps que recusam espaço virtual: não escondemos, só ficam de fora.

## Regras do projeto
Só usar o Unit e o root em jogos offline single-player; nada pago com dinheiro real; nada de anúncios com
recompensa; nada contra anti-trapaça nem para esconder o Vcloud; não mexer no celular de verdade sem pedir; prévia
antes de mudar tela; ideia fora de ordem vai para um ramo de laboratório.
