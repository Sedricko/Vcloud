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
Foco: **o root funcionar lá dentro** e **o que estiver lá dentro rodar bem**. O Unit é um dos apps de root.

| Fase | O que | Pronto quando |
|---|---|---|
| F0 | Testar o BlackBox pronto: o Orecraft e mais 2 ou 3 apps que o usuário quer usar dentro | Abrem, rodam e salvam |
| F1 | Compilar o BlackBox e cortar o que não usamos (espaço mínimo) | O nosso build roda os mesmos apps |
| F2 | Root do Vcloud (`su` interno com janela "permitir root?") | Um verificador de root dentro diz "tem root" |
| F3 | Apps de root funcionando: o Unit (e outro app de root) lendo e mudando o Orecraft lá dentro | Achar, editar e congelar o Cobre |
| F4 | Pastas: mesmas do celular + pasta dos saves visível; opção "isolado" | Backup do save pelo app Arquivos |
| F5 | Testar em Android antigo e novo (vPhone, outro emulador, S25 se autorizar) | Funciona no 7 e no 16 |
| Lab | Plano B: APK do jogo com uma pecinha nossa | Só se algum jogo não rodar no espaço |

## Cuidados já conhecidos
- Matador de processos do Android 12+ (os dois emuladores tratam dele). Testar na F0/F1.
- Apps que recusam espaço virtual: não escondemos, só ficam de fora.

## Regras do projeto
Só usar o Unit e o root em jogos offline single-player; nada pago com dinheiro real; nada de anúncios com
recompensa; nada contra anti-trapaça nem para esconder o Vcloud; não mexer no celular de verdade sem pedir; prévia
antes de mudar tela; ideia fora de ordem vai para um ramo de laboratório.
