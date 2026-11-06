# Auditoria do código escrito nesta conversa (4.0.36 → 4.0.88)

Escopo: só o que foi escrito aqui (motor `com.vcloud.motor` + telas do `app`). O projeto é Android nativo (Java/Kotlin); **não existe HTML/CSS** para entregar.

## Defeitos achados e corrigidos na 4.0.89
| # | Onde | Defeito | Correção |
|---|------|---------|----------|
| 1 | `InstaladorVirtual.copiarParte` | A cópia anterior da parte (split) é gravada como somente leitura; ao ATUALIZAR um app, abrir o arquivo para escrita falhava e o erro era engolido → app atualizado ficava com as partes velhas | apaga o arquivo antigo antes de gravar |
| 2 | `InstaladorVirtual.copiarParte` | Copiava centenas de MB (ex.: 886 MB) byte a byte numa thread só, atrasando o resultado à loja e gastando o dobro de disco | `renameTo` (mesma partição) com cópia só como reserva; bibliotecas nativas agora são extraídas ANTES de mover |
| 3 | `InstaladorVirtual.PENDENTES` | Mapa binder→Intent crescia a cada instalação (vazamento) | `remove` ao usar |
| 4 | `InnerReceiverDelegate` | `HashMap` estático lido/escrito de várias threads (binder, binderDied, entregar) sem sincronizar → corrida/ConcurrentModification | todos os acessos sincronizados |
| 5 | `ManifestFiltros` | Cache por caminho do APK: depois de atualizar o app o caminho é o mesmo e os filtros ficavam velhos | chave = caminho + data de modificação |
| 6 | `IBActivityThread.mudancaDePacote` | Chamada binder síncrona em fila: um app congelado pelo Android travava o aviso para todos os outros | `oneway` |
| 7 | `BPackageManagerService.queryIntentActivities` (fallback do manifesto) | Sem try/catch: qualquer falha derrubava a consulta inteira | try/catch com log |
| 8 | `Monitor.vazamento` | Fazia a pergunta cara (`isInstalled`, vai ao servidor) em toda consulta da loja antes de filtrar o método → dobrava o custo | filtro barato primeiro |

## Riscos conhecidos que NÃO foram corrigidos
- Erro 6252 (`PSV was not installed`) da Play Store: causa não encontrada (ver `passagem.md` 4.0.87).
- `ComponenteHabilitado` desliga o alias do Play Protect pelo NOME da classe da 53.8.11; a loja de dentro já está na 53.8.12.
- `foraDoVcloudParaALoja`/`PM-VAZA` dependem do nome do processo `com.android.vending`; outro app Google de dentro que consulte pacotes ainda enxerga o celular real.
- Monitor grava arquivo a cada evento; em relatório de 17 MB já pesa — falta limite por categoria.
- Layout de telas (Central de diagnóstico, menu do ícone): nenhuma mudança feita; pela regra do projeto precisa de imagem de prévia aprovada antes (proposta no chat).
