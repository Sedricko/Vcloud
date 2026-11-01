"""Prévia da tela 'Manter apps vivos'. Reaproveita CSS/cores de gerar_amostras.py (cores reais do app)."""
import os
aqui=os.path.dirname(os.path.abspath(__file__)); os.chdir(aqui)
exec(open('gerar_amostras.py',encoding='utf-8').read().split("def par(")[0])
EXTRA='''
.intro{font-size:13px;color:var(--secondary_text);line-height:1.4;margin:2px 0 10px}
.card{border:1px solid var(--surface_line);border-radius:16px;padding:12px;margin-bottom:10px}
.card .t{display:flex;align-items:center;gap:10px;font-size:15px;font-weight:600}
.card .t .bd{flex:none;width:22px;height:22px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:800;color:#fff}
.ok{background:#2e9b57}.av{background:#d98a1f}.in{background:var(--secondary_text)}
.card p{font-size:12.5px;color:var(--secondary_text);line-height:1.4;margin:6px 0 0 32px}
.card .ac{margin:8px 0 0 32px;display:flex;gap:8px}.card .ac div{padding:8px 14px;border-radius:18px;border:1px solid var(--secondary_text);font-size:12.5px;font-weight:700;color:var(--primary)}
.card .ac div.f{background:var(--primary);border-color:var(--primary);color:var(--on_primary)}
.sw1.right{margin-left:auto}
.sheet{position:absolute;left:0;right:0;bottom:40px;background:var(--surface);border-radius:24px 24px 0 0;padding:16px;box-shadow:0 -10px 40px rgba(0,0,0,.35);z-index:3;border-top:1px solid var(--surface_line)}
.sheet h3{font-size:17px;margin-bottom:6px}.sheet .cmd{font-family:monospace;font-size:10.8px;background:var(--surface_line);padding:8px;border-radius:10px;margin:6px 0;word-break:break-all;line-height:1.4}
.sheet small{display:block;color:var(--secondary_text);font-size:12px;line-height:1.4;margin:4px 0}
.dimbg{position:absolute;inset:0;background:rgba(0,0,0,.45);z-index:2}
'''
def tela():
    return f'''<div class="ph">{SB}<div class="tb">Manter apps vivos</div><div class="body">
<div class="intro">O Android encerra apps em segundo plano para poupar memória e bateria. Estas verificações ajudam os jogos a continuarem abertos. Nada muda no celular sozinho.</div>
<div class="card"><div class="t"><span class="bd ok">✓</span>Vcloud ativo em segundo plano<div class="sw1 on right"></div></div><p>Mantém o Vcloud rodando com a tela bloqueada.</p></div>
<div class="card"><div class="t"><span class="bd av">!</span>Sem restrição de bateria</div><p>O Android pode pausar o Vcloud para poupar bateria. Recomendado liberar.</p><div class="ac"><div class="f">Liberar</div></div></div>
<div class="card"><div class="t"><span class="bd av">!</span>Limite de processos fantasma</div><p>Ativo: o Android pode encerrar processos dos jogos (Android 12 ou mais novo).</p><div class="ac"><div>Ver como desativar</div></div></div>
<div class="card"><div class="t"><span class="bd in">9</span>Encerrados pelo Android</div><p>Nas últimas 24 h: Touch Macro, Spellbinders e outros (pouca memória ou limite de processos).</p><div class="ac"><div>Ver na Central</div></div></div>
</div>{NAV}</div>'''
def folha():
    return f'''<div class="ph">{SB}<div class="tb">Manter apps vivos</div><div class="dimbg"></div>
<div class="sheet"><h3>Desativar o limite de processos</h3><small>Rode uma vez no computador, com o celular ligado por depuração USB ou sem fio. Toque para copiar.</small>
<div class="cmd">adb shell settings put global settings_enable_monitor_phantom_procs false</div>
<div class="cmd">adb shell device_config put activity_manager max_phantom_processes 2147483647</div>
<small>Em algumas versões do Android o primeiro comando basta. O Vcloud não roda isso por você.</small>
<div class="rowb" style="margin-top:10px"><div class="btn">Copiar os dois</div><div class="btn f">Entendi</div></div></div>{NAV}</div>'''
h=f'<!doctype html><html lang="pt-BR"><meta charset="utf-8"><style>{CSS}{EXTRA}</style><body>'
for txt,d in (("CLARO",claro),("ESCURO",escuro)):
    h+=f'<div class="lin"><div class="lab">{txt}</div>'+''.join(f'<div style="{V(d)}">{f()}</div>' for f in (tela,folha))+'</div>'
open('vivos.html','w',encoding='utf-8').write(h); print("ok")
