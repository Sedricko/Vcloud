"""Prévia do painel lateral v2 (funções inspiradas na bolha do vPhoneOS). Reaproveita o CSS/cores de gerar_amostras.py."""
import os
aqui=os.path.dirname(os.path.abspath(__file__)); os.chdir(aqui)
src=open('gerar_amostras.py',encoding='utf-8').read().split("def par(")[0]
exec(src)
EXTRA='''
.game2{position:absolute;inset:0;background:linear-gradient(170deg,#2c4d2c,#18301f 45%,#3a2a4f)}.game2 i{position:absolute;display:block}
.bolha{position:absolute;right:0;top:330px;width:11px;height:64px;border-radius:8px 0 0 8px;background:rgba(220,228,245,.9);border:1px solid rgba(255,255,255,.4);border-right:none;opacity:.30;z-index:3}
.side2{position:absolute;right:0;top:70px;width:232px;padding:12px;border-radius:22px 0 0 22px;background:linear-gradient(160deg,var(--glass_start),var(--glass_end));border:1px solid var(--glass_stroke);border-right:none;color:var(--glass_text);z-index:3}
.side2 .hd{font-size:12px;color:var(--glass_text_dim);padding:2px 6px 8px;display:flex;justify-content:space-between}.side2 .hd b{color:var(--glass_text);font-size:14px}
.it2{display:flex;align-items:center;gap:11px;padding:6px 8px;border-radius:14px;font-size:13.5px}.it2:nth-child(odd){background:rgba(255,255,255,.06)}
.it2 .q{width:30px;height:30px;border-radius:10px;background:var(--tile_bg);border:1px solid var(--tile_stroke);display:flex;align-items:center;justify-content:center;font-weight:700;flex:none}
.it2 .t{flex:1}.it2 .m{font-size:11px;color:var(--glass_text_dim)}
.sw2{width:34px;height:19px;border-radius:10px;background:var(--tile_stroke);border:1px solid var(--glass_stroke);position:relative;flex:none}.sw2::after{content:"";position:absolute;top:2px;left:2px;width:13px;height:13px;border-radius:50%;background:var(--glass_text_dim)}
.sw2.on{background:var(--glass_dot);border-color:var(--glass_dot)}.sw2.on::after{left:18px;background:var(--glass_text)}
.sep2{height:1px;background:var(--glass_stroke);margin:5px 4px}
.cap2{position:absolute;left:14px;top:40px;right:14px;font-size:12px;color:#fff;background:rgba(0,0,0,.4);padding:6px 10px;border-radius:10px;z-index:3}
'''
def lat_fechada():
    return f'<div class="ph"><div class="game2"><i style="left:16px;top:40px;width:120px;height:26px;border-radius:13px;background:rgba(255,255,255,.25)"></i><i style="left:105px;top:230px;width:150px;height:150px;border-radius:50%;background:radial-gradient(circle,#ffd36b,#a86a1c);opacity:.85"></i><i style="left:12px;right:12px;bottom:60px;height:140px;border-radius:18px;background:rgba(120,70,30,.55)"></i></div>{SB}<div class="bolha"></div><div class="cap2">A aba fica discreta (opacidade ajustável) e se destaca ao tocar</div>{NAV}</div>'
def lat_aberta():
    def it(g,t,m='',sw=None):
        s='' if sw is None else f'<div class="sw2{" on" if sw else ""}"></div>'
        return f'<div class="it2"><div class="q">{g}</div><div class="t">{t}{("<div class=m>"+m+"</div>") if m else ""}</div>{s}</div>'
    return f'''<div class="ph"><div class="game2"><i style="left:105px;top:230px;width:150px;height:150px;border-radius:50%;background:radial-gradient(circle,#ffd36b,#a86a1c);opacity:.85"></i></div>{SB}<div class="scrim" style="background:rgba(10,14,28,.35)"></div>
<div class="side2"><div class="hd"><b>Vcloud</b><span>Orecraft</span></div>
{it("‹","Voltar","só dentro do app")}{it("⌂","Início do Vcloud","o app continua aberto")}{it("▤","Apps recentes")}{it("▢","Capturar tela")}
<div class="sep2"></div>
{it("#","Root do Vcloud","chave geral",True)}{it("⚙","Configurações")}{it("☰","Gerenciador de root")}{it("!","Diagnóstico")}
<div class="sep2"></div>
{it("◌","Ocultar painel","reative em Configurações")}{it("✕","Fechar app")}</div>{NAV}</div>'''
def cfg2():
    return f'''<div class="ph">{SB}<div class="tb">Configurações</div><div class="body">
<div class="cat">Painel lateral</div>
<div class="pf sw"><div><b>Mostrar o painel nos apps</b><span>Aba na borda com Voltar, Início, Apps recentes, captura e atalhos</span></div><div class="sw1 on"></div></div>
<div class="sec" style="margin-top:10px;font-size:14px">Opacidade da aba</div>
<div class="rd"><i></i>Normal</div><div class="rd s"><i></i>Discreta</div><div class="rd"><i></i>Quase invisível</div>
<div class="cat">Outros</div>
<div class="pf"><b>Serviços do Google</b><span>Ir para os serviços do Google</span></div>
<div class="pf sw"><div><b>Manter ativo em segundo plano</b></div><div class="sw1 on"></div></div>
<div class="pf"><b>Gerenciador de root</b><span>Chave geral do root do Vcloud, quem pode usar e o registro dos pedidos</span></div>
</div>{NAV}</div>'''
h=f'<!doctype html><html lang="pt-BR"><meta charset="utf-8"><style>{CSS}{EXTRA}</style><body>'
for txt,d in (("CLARO",claro),("ESCURO",escuro)):
    h+=f'<div class="lin"><div class="lab">{txt}</div>'+''.join(f'<div style="{V(d)}">{f()}</div>' for f in (lat_fechada,lat_aberta,cfg2))+'</div>'
open('painel-4.html','w',encoding='utf-8').write(h)
print("ok")
