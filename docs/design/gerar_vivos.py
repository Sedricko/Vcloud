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
def comandos():
    return f'''<div class="ph">{SB}<div class="tb">Manter apps vivos</div><div class="body">
<div class="cat">Estado</div>
<div class="pf"><b>Limite de processos fantasma</b><span>Toque para ver como desativar.</span></div>
</div><div class="scrim"></div>
<div class="dlg" style="top:190px"><h3>Desativar o limite</h3><p>Rode uma vez no computador, com o celular ligado por depuração USB ou sem fio:</p><div class="cmd">adb shell settings put global settings_enable_monitor_phantom_procs false</div><div class="cmd">adb shell device_config put activity_manager max_phantom_processes 2147483647</div><p class="dim" style="font-size:12.5px">O Vcloud não roda isso por você.</p><div class="act"><div>Fechar</div><div>Copiar os dois</div></div></div>{NAV}</div>'''
h=f'<!doctype html><html lang="pt-BR"><meta charset="utf-8"><style>{CSS}</style><body>'
for txt,d in (("CLARO",claro),("ESCURO",escuro)):
    h+=f'<div class="lin"><div class="lab">{txt}</div>'+''.join(f'<div style="{V(d)}">{f()}</div>' for f in (tela,bateria,comandos))+'</div>'
open('vivos.html','w',encoding='utf-8').write(h); print("ok")
