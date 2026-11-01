"""Prévia da tela 'Manter apps vivos' NO PADRÃO DAS CONFIGURAÇÕES (linhas nativas, sem ícones coloridos).
Reaproveita CSS/cores de gerar_amostras.py (cores reais do app)."""
import os
aqui=os.path.dirname(os.path.abspath(__file__)); os.chdir(aqui)
exec(open('gerar_amostras.py',encoding='utf-8').read().split("def par(")[0])
def tela():
    return f'''<div class="ph">{SB}<div class="tb">Manter apps vivos</div><div class="body">
<div class="pf" style="border:0"><span>O Android encerra apps em segundo plano para poupar memória e bateria. Aqui você vê o que pode estar fechando os seus jogos. Nada muda no celular sozinho.</span></div>
<div class="cat">Estado</div>
<div class="pf"><b>Restrição de bateria</b><span>Sem restrição: o Android não pausa o Vcloud para poupar bateria.</span></div>
<div class="pf"><b>Limite de processos fantasma</b><span>Não consegui verificar neste Android. Toque para ver como desativar.</span></div>
<div class="pf"><b>Encerrados pelo Android</b><span>9 no histórico (pouca memória ou limite de processos). Toque para ver na Central.</span></div>
<div class="cat">Relacionado</div>
<div class="pf"><b>Manter ativo em segundo plano</b><span>Fica em Configurações.</span></div>
</div>{NAV}</div>'''
def bateria():
    return f'''<div class="ph">{SB}<div class="tb">Manter apps vivos</div><div class="body">
<div class="cat">Estado</div>
<div class="pf"><b>Restrição de bateria</b><span>O Android pode pausar o Vcloud para poupar bateria. Toque para liberar.</span></div>
<div class="pf"><b>Limite de processos fantasma</b><span>Não consegui verificar neste Android. Toque para ver como desativar.</span></div>
</div><div class="scrim"></div>
<div class="dlg" style="top:260px"><h3>Liberar a bateria?</h3><p>O Android vai perguntar se o Vcloud pode ficar sem restrição de bateria. Isso ajuda os jogos a continuarem abertos.</p><div class="act"><div>Cancelar</div><div>Continuar</div></div></div>{NAV}</div>'''
def dev():
    return f'''<div class="ph">{SB}<div class="tb">Manter apps vivos</div><div class="body">
<div class="cat">Estado</div>
<div class="pf"><b>Limite de processos fantasma</b><span>Toque para ver como desativar.</span></div>
</div><div class="scrim"></div>
<div class="dlg" style="top:230px"><h3>Limite de processos fantasma</h3><p>Nas Opções do desenvolvedor, procure "Desativar restrições de processos secundários" (o nome varia) e ligue. Se o seu celular não tiver essa chave, este ajuste não está disponível nele.</p><div class="act"><div>Fechar</div><div>Abrir Opções do desenvolvedor</div></div></div>{NAV}</div>'''
h=f'<!doctype html><html lang="pt-BR"><meta charset="utf-8"><style>{CSS}</style><body>'
for txt,d in (("CLARO",claro),("ESCURO",escuro)):
    h+=f'<div class="lin"><div class="lab">{txt}</div>'+''.join(f'<div style="{V(d)}">{f()}</div>' for f in (tela,bateria,dev))+'</div>'
open('vivos.html','w',encoding='utf-8').write(h); print("ok")
