"""Gera amostras (HTML+PNG) das telas do Vcloud nos temas claro e escuro, lendo as cores REAIS de
android/app/src/main/res/values/colors.xml e values-night/colors.xml. Não são capturas de tela do app.
Uso: python3 gerar_amostras.py   (precisa do Node + playwright para o PNG: ver comando no fim)"""
import re,os,subprocess,sys
RES=os.path.join(os.path.dirname(os.path.abspath(__file__)),'../../android/app/src/main/res/')
def cores(p): return dict(re.findall(r'<color name="(\w+)">(#[0-9A-Fa-f]{6,8})</color>',open(RES+p,encoding='utf-8').read()))
claro=cores('values/colors.xml'); escuro={**claro,**cores('values-night/colors.xml')}
def css(v):
    v=v.lstrip('#')
    if len(v)==6: return '#'+v
    a=int(v[:2],16)/255; return f'rgba({int(v[2:4],16)},{int(v[4:6],16)},{int(v[6:8],16)},{a:.2f})'
V=lambda d:';'.join(f'--{k}:{css(v)}' for k,v in d.items())
CSS='''*{box-sizing:border-box;margin:0;font-family:Roboto,"Segoe UI",system-ui,sans-serif}
body{background:#8d96ab;display:flex;flex-direction:column;gap:18px;padding:22px;width:max-content}
.lin{display:flex;gap:18px;align-items:center}.lab{color:#fff;font-weight:700;width:58px;writing-mode:vertical-rl;transform:rotate(180deg);text-align:center}
.ph{position:relative;width:360px;height:760px;overflow:hidden;border-radius:26px;background:var(--surface);color:var(--on_surface)}
.sb{position:absolute;top:0;left:0;right:0;height:26px;display:flex;justify-content:space-between;align-items:center;padding:0 18px;font-size:11px;font-weight:600;z-index:3}
.nav{position:absolute;left:0;right:0;bottom:0;height:40px;display:flex;justify-content:center;align-items:center;gap:60px;opacity:.7;z-index:3}
.tb{position:absolute;top:26px;left:0;right:0;height:52px;display:flex;align-items:center;padding:0 16px;font-size:19px;font-weight:700;border-bottom:1px solid var(--surface_line)}
.body{position:absolute;top:78px;left:0;right:0;bottom:40px;padding:6px 16px;overflow:hidden}
.cat{font-size:13px;font-weight:700;color:var(--primary);margin:14px 0 4px}
.pf{padding:11px 0;border-bottom:1px solid var(--surface_line)}.pf b{display:block;font-size:15px;font-weight:500}.pf span{display:block;font-size:12.5px;color:var(--secondary_text);margin-top:2px;line-height:1.35}
.pf.sw{display:flex;align-items:center;gap:10px}.pf.sw>div:first-child{flex:1}
.sw1{flex:none;width:40px;height:22px;border-radius:11px;background:var(--surface_line);border:1px solid var(--secondary_text);position:relative}.sw1::after{content:"";position:absolute;top:3px;left:3px;width:14px;height:14px;border-radius:50%;background:var(--secondary_text)}
.sw1.on{background:var(--primary);border-color:var(--primary)}.sw1.on::after{left:21px;background:var(--on_primary)}
.btn{flex:1;text-align:center;padding:9px 4px;border-radius:20px;border:1px solid var(--secondary_text);font-size:13px;font-weight:600;color:var(--primary)}
.btn.f{background:var(--primary);color:var(--on_primary);border-color:var(--primary)}
.rowb{display:flex;gap:8px;margin:6px 0 10px}.mono{font-family:monospace;font-size:11px;line-height:1.5;white-space:pre-wrap}.ev{margin-bottom:10px}.dim{color:var(--secondary_text)}
.rd{display:flex;align-items:center;gap:12px;padding:6px 0;font-size:14px}.rd i{width:18px;height:18px;border-radius:50%;border:2px solid var(--secondary_text);display:inline-block}.rd.s i{border-color:var(--primary);background:radial-gradient(circle,var(--primary) 45%,transparent 50%)}
.sec{font-size:15px;font-weight:700;margin:14px 0 2px}
.app2{display:flex;align-items:center;gap:12px;padding:6px 0}.ai{width:38px;height:38px;border-radius:10px;background:linear-gradient(150deg,#7aa0ff,#3a5bd0);flex:none}.ai.b{background:linear-gradient(150deg,#ffb86b,#d9632b)}.ai.c{background:linear-gradient(150deg,#6be0b0,#1f9a6c)}
.app2 .g{flex:1;font-size:14px}.app2 small{display:block;color:var(--secondary_text);font-size:10.5px}.app2 em{font-style:normal;font-size:11.5px;font-weight:700;margin-right:4px}
.home{background:linear-gradient(165deg,var(--wave_bg_start),var(--wave_bg_end))}
.wave{position:absolute;inset:0;background:radial-gradient(70% 60% at 8% 70%,var(--wave_a_end),transparent 65%),radial-gradient(60% 50% at 100% 85%,var(--wave_c_end),transparent 65%);opacity:.8}
.panel{position:absolute;top:60px;left:10px;right:10px;bottom:56px;border-radius:24px;background:linear-gradient(160deg,var(--glass_start),var(--glass_end));border:1px solid var(--glass_stroke);padding:12px 12px 8px;display:flex;flex-direction:column;gap:9px;color:var(--glass_text)}
.panel .h{display:flex;justify-content:space-between;align-items:flex-start}.panel .h b{font-size:20px}.panel .h div{font-size:15px;color:var(--glass_text_dim)}
.tile{background:var(--tile_bg);border:1px solid var(--tile_stroke);border-radius:16px;padding:10px;color:var(--glass_text);font-size:12px;line-height:1.5}.tile .l{font-size:11px;color:var(--glass_text_dim)}.tile .v{font-size:15px;font-weight:700}
.ic{width:46px;height:46px;border-radius:14px;flex:none}.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px 2px;margin-top:6px}.app{display:flex;flex-direction:column;align-items:center;gap:4px;font-size:10px;color:var(--glass_text);text-align:center}
.bot{display:flex;align-items:center;justify-content:center;position:relative;height:40px;margin-top:6px}.dots{display:flex;gap:5px}.dots i{width:8px;height:8px;border-radius:50%;background:var(--glass_dot);opacity:.45}.dots i.on{width:18px;border-radius:5px;opacity:1}
.fab{position:absolute;right:0;top:0;width:40px;height:40px;border-radius:50%;background:var(--primary);color:var(--on_primary);display:flex;align-items:center;justify-content:center;font-size:22px}
.scrim{position:absolute;inset:0;background:rgba(0,0,0,.45);z-index:2}
.dlg{position:absolute;left:20px;right:20px;top:230px;background:var(--surface);color:var(--on_surface);border-radius:28px;padding:22px 20px 12px;z-index:3;box-shadow:0 20px 60px rgba(0,0,0,.4)}
.dlg h3{font-size:21px;margin-bottom:10px}.dlg p{font-size:14px;line-height:1.45;margin-bottom:8px}.cmd{font-family:monospace;font-size:12px;background:var(--surface_line);padding:6px 8px;border-radius:8px;margin-bottom:10px}
.ck{display:flex;gap:10px;align-items:center;font-size:14px;margin:6px 0 10px}.ck i{width:18px;height:18px;border-radius:4px;border:2px solid var(--secondary_text);display:inline-block}
.act{display:flex;justify-content:flex-end;gap:2px;font-size:14px;font-weight:600;color:var(--primary)}.act div{padding:10px 12px}
.game{position:absolute;inset:0;background:linear-gradient(170deg,#2c4d2c,#18301f 45%,#3a2a4f)}.game i{position:absolute;display:block}
.handle{position:absolute;right:0;top:330px;width:11px;height:64px;border-radius:8px 0 0 8px;background:rgba(220,228,245,.65);border:1px solid rgba(255,255,255,.4);border-right:none;z-index:3}
.side{position:absolute;right:0;top:140px;width:210px;padding:12px;border-radius:22px 0 0 22px;background:linear-gradient(160deg,var(--glass_start),var(--glass_end));border:1px solid var(--glass_stroke);border-right:none;color:var(--glass_text);z-index:3}
.side .hd{font-size:12px;color:var(--glass_text_dim);padding:2px 6px 8px;display:flex;justify-content:space-between}.side .hd b{color:var(--glass_text);font-size:14px}
.it{display:flex;align-items:center;gap:12px;padding:7px 8px;border-radius:14px;font-size:14px}.it:nth-child(odd){background:rgba(255,255,255,.06)}
.it .q{width:32px;height:32px;border-radius:10px;background:var(--tile_bg);border:1px solid var(--tile_stroke);display:flex;align-items:center;justify-content:center;font-weight:700}.sep{height:1px;background:var(--glass_stroke);margin:6px 4px}
'''
SB='<div class="sb"><span>15:59</span><span>● ▮▮▮ 34%</span></div>'
NAV='<div class="nav"><span>|||</span><span>○</span><span>‹</span></div>'
def home():
    a=''.join(f'<div class="app"><div class="ic" style="background:{c}"></div>{n}</div>' for n,c in [("Google Play Store","linear-gradient(135deg,#fff,#e8eefc)"),("Unit","linear-gradient(150deg,#16408f,#0a2a66)"),("Orecraft","linear-gradient(150deg,#d86a3a,#7a3a2a)"),("DYSMANTLE","linear-gradient(150deg,#d9a0e8,#5a6ac0)")])
    return f'<div class="ph home"><div class="wave"></div>{SB}<div class="panel"><div class="h"><div><b style="display:block;font-size:22px">Vcloud</b>Usuário 0</div><b style="font-size:22px">⋮</b></div><div style="display:grid;grid-template-columns:1.15fr 1fr;gap:9px"><div class="tile"><div class="l">Espaço Vcloud</div><div class="v">● Ligado</div>Saves: Download/Vcloud<br>Celular: sem root</div><div class="tile"><div class="l">Celular</div><div class="v">SM-S938B</div>Android 17</div></div><div style="font-weight:700;font-size:16px;margin:2px 4px">Todos os apps</div><div class="tile" style="flex:1"><div class="grid">{a}</div></div><div class="bot"><div class="dots"><i class="on"></i><i></i></div><div class="fab">+</div></div></div>{NAV}</div>'
def config():
    return f'<div class="ph">{SB}<div class="tb">Configurações</div><div class="body"><div class="cat">Outros</div><div class="pf"><b>Serviços do Google</b><span>Ir para os serviços do Google</span></div><div class="pf sw"><div><b>Mostrar a Play Store na tela inicial</b></div><div class="sw1"></div></div><div class="pf sw"><div><b>Manter ativo em segundo plano</b></div><div class="sw1 on"></div></div><div class="pf"><b>Sobrepor a outros apps</b><span>Libera balões e janelas flutuantes dos apps de dentro (ex.: Unit)</span></div><div class="pf"><b>Gerenciador de root</b><span>Chave geral do root do Vcloud, quem pode usar e o registro dos pedidos</span></div><div class="pf"><b>Central de diagnóstico</b><span>Erros, histórico dos apps (abriu, não abriu, fechou e por quê) e relatório para enviar</span></div></div>{NAV}</div>'
def central():
    return f'<div class="ph">{SB}<div class="body" style="top:34px"><div style="font-size:20px;font-weight:700">Central de diagnóstico</div><div style="font-size:13px;margin:4px 0 8px;color:var(--secondary_text)">42 registros, 6 fechamentos ou falhas. Relatórios ficam em Download/Vcloud.</div><div class="rowb"><div class="btn">Atualizar</div><div class="btn">Copiar</div></div><div class="rowb"><div class="btn">Gerar relatório</div><div class="btn">Limpar histórico</div></div><div class="mono"><div class="ev"><b>01/11 15:59:24  ROOT dev.unit.app: ALLOW</b></div><div class="ev"><b>01/11 15:59:17  ABRIU  dev.unit.app</b></div><div class="ev"><b>01/11 15:37:59  FECHOU  com.jake.touchmacro.pro</b> depois de 4 s\n<span class="dim">    Causa provável: O app se encerrou sozinho (System.exit)</span></div><div class="ev"><b>01/11 15:37:58  PAUSADO  com.android.vending</b>\n<span class="dim">    caiu 4 vezes em 30 s; novos inícios ficam parados</span></div><div class="ev"><b>01/11 14:28:15  FECHOU  dev.unit.app</b> depois de 2 s\n<span class="dim">    Causa provável: Notificação do serviço recusada pelo Android</span></div></div></div>{NAV}</div>'
def root():
    return f'<div class="ph">{SB}<div class="tb">Gerenciador de root</div><div class="body"><div class="pf sw" style="border:0"><div><b>Root do Vcloud (chave geral)</b></div><div class="sw1 on"></div></div><div class="sec">Notificações</div><div class="rd s"><i></i>Nenhuma</div><div class="rd"><i></i>Mensagem na tela</div><div class="rd"><i></i>Notificação</div><div class="sec">Responder todos da mesma maneira</div><div class="rd"><i></i>Perguntar a cada app</div><div class="rd s"><i></i>Permitir todos</div><div class="rd"><i></i>Negar todos</div><div class="rowb"><div class="btn">Permitir todos</div><div class="btn">Negar todos</div><div class="btn">Perguntar</div></div><div class="sec" style="margin-top:4px">Apps</div><div class="app2"><div class="ai"></div><div class="g">Unit<small>dev.unit.app</small></div><em>Permitido</em><div class="sw1 on"></div></div><div class="app2"><div class="ai b"></div><div class="g">Tinker<small>dev.tinker.app</small></div><em>Automático</em><div class="sw1 on"></div></div><div class="app2"><div class="ai c"></div><div class="g">MT Manager<small>bin.mt.plus</small></div><em style="color:var(--secondary_text)">Negado</em><div class="sw1"></div></div></div>{NAV}</div>'
def janela():
    return f'<div class="ph">{SB}<div class="tb">Unit</div><div class="scrim"></div><div class="dlg"><h3>Permitir root?</h3><p>Unit quer usar root dentro do Vcloud.</p><div class="cmd">su -c "id"</div><p class="dim" style="font-size:12.5px">Vale só dentro do Vcloud, não no celular. Você muda isso depois em Configurações > Gerenciador de root.</p><div class="ck"><i></i>Usar a mesma resposta para todos os apps</div><div class="act"><div>Negar</div><div>Só desta vez</div><div>Permitir sempre</div></div></div>{NAV}</div>'
def lateral():
    return f'<div class="ph"><div class="game"><i style="left:16px;top:40px;width:120px;height:26px;border-radius:13px;background:rgba(255,255,255,.25)"></i><i style="left:105px;top:230px;width:150px;height:150px;border-radius:50%;background:radial-gradient(circle,#ffd36b,#a86a1c);opacity:.85"></i><i style="left:12px;right:12px;bottom:60px;height:140px;border-radius:18px;background:rgba(120,70,30,.55)"></i></div>{SB}<div class="scrim" style="background:rgba(10,14,28,.3)"></div><div class="side"><div class="hd"><b>Vcloud</b><span>Orecraft</span></div><div class="it"><div class="q">—</div>Minimizar</div><div class="it"><div class="q">▢</div>Capturar tela</div><div class="it"><div class="q">⇄</div>Trocar de app</div><div class="it"><div class="q">⚙</div>Configurações</div><div class="it"><div class="q">#</div>Gerenciador de root</div><div class="it"><div class="q">!</div>Diagnóstico</div><div class="sep"></div><div class="it"><div class="q">✕</div>Fechar app</div></div>{NAV}</div>'
def par(nome,a,b):
    h=f'<!doctype html><html lang="pt-BR"><meta charset="utf-8"><style>{CSS}</style><body>'
    for txt,d in (("CLARO",claro),("ESCURO",escuro)):
        h+=f'<div class="lin"><div class="lab">{txt}</div><div style="{V(d)}">{a()}</div><div style="{V(d)}">{b()}</div></div>'
    open(nome,'w',encoding='utf-8').write(h)
aqui=os.path.dirname(os.path.abspath(__file__))
os.chdir(aqui)
par('painel-1.html',home,config); par('painel-2.html',central,root); par('painel-3.html',janela,lateral)
print("HTML gerado: painel-1/2/3.html")
