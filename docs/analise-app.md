# Análise do app Vcloud (telas e abertura) — 01/11

Escopo: módulo `android/app` (telas, ~3.600 linhas) e o caminho de abertura do motor. Motor inteiro (`Bcore`, ~70 mil
linhas) não foi revisado linha por linha.

## Causa real da tela preta (achada em 01/11, 06:57)
`res/drawable/splash.xml` (fundo da tela de entrada) usava `<bitmap android:src="@mipmap/ic_launcher">`. Com o ícone
novo adaptável (`mipmap-anydpi-v26`), `@mipmap/ic_launcher` deixou de ser imagem simples e a tela de entrada não se
desenhava: tela preta em todas as versões a partir do ícone novo. Corrigido usando `<item android:drawable=...>`.

## Falhas encontradas

| # | Onde | O que acontece | Gravidade |
|---|---|---|---|
| 1 | `view/main/MainActivity.kt` `initViewPager()` e `scanUser()` | Pedem a lista de usuários ao motor (`VcloudCore.get().users`) **na linha principal da tela**. Se o motor estiver ocupado, a tela inicial fica esperando: é o mecanismo do **travamento na abertura** visto no S25 (somado à ativação automática dos Serviços do Google, que deixava o motor ocupado). | Alta |
| 2 | `view/main/VcloudLoader.kt` `onStoragePermissionNeeded` | Envia o aviso `com.vcloud.app.REQUEST_STORAGE_PERMISSION`, mas **nenhuma parte do app escuta esse aviso**: quando um app de dentro precisa de permissão de arquivos, nada acontece. (código faltando) | Média |
| 3 | `view/main/VcloudLoader.kt` `requestInstallPackage` | Lê o APK e sempre devolve `false` (o resultado da leitura não é usado). Instalar APK pedido por um app de dentro nunca é tratado pelo Vcloud. (código faltando) | Baixa |
| 4 | `app/App.kt`, `VcloudLoader.kt`, `MainActivity.kt` | 138 blocos `catch (e: Exception)` que só registram e seguem. Erros de abertura do motor ficam escondidos e o app continua em estado quebrado (tela preta) em vez de avisar. | Média |
| 5 | `util/MemoryManager.kt`, `data/AppsRepository.kt` | Força coleta de lixo (`System.gc()`) e dorme (`Thread.sleep`) durante o carregamento da lista; tentativas com espera fixa. Deixa a lista mais lenta sem resolver memória. | Baixa |
| 6 | Opções removidas | Preferências antigas (VPN, captura de tela) ainda guardadas no loader, só ignoradas. | Baixa |

## Plano (junto com a reaplicação das melhorias, uma por vez)
1. Corrigir o item 1: pedir os usuários ao motor fora da linha principal e só então montar a lista.
2. Item 2: ao precisar de permissão de arquivos, abrir direto a tela "Acesso a todos os arquivos" do Vcloud.
3. Item 4: na abertura, se o motor não ligar, mostrar uma mensagem clara em vez de tela preta.
4. Itens 3, 5 e 6: limpeza, depois.
