# Rehace res/datos/api.txt (la API de los robots para el buscador del juego) a partir de las tres fachadas
# (Metajavbot.java, MetajavbotDe.java, MetajavbotEn.java) y de las interfaces Enemigo/Gegner/Enemy y Golpe/Treffer/Hit.
# Uso, desde la raíz: python herramientas/api_doc.py
import re, os

API = 'juego/src/main/java/metajavbots/api/'

# El tema de cada método (res/datos/temas.txt): dónde se aprende lo que hace falta para usarlo.
TEMAS = {
    'alEmpezar': '11', 'cadaTurno': '11', 'alVerEnemigo': '11', 'alRecibirGolpe': '11', 'alAcertar': '11',
    'alChocarConPared': '11', 'alChocarConEnemigo': '11', 'alAcabarRonda': '11',
    'avanzar': '01', 'retroceder': '01', 'girar': '01', 'girarIzquierda': '01', 'girarDerecha': '01', 'detener': '01',
    'girarHacia': '03', 'apuntar': '01', 'apuntarHacia': '03', 'apuntarA': '03',
    'atacarIzquierda': '04', 'atacarDerecha': '04', 'especial': '04', 'atacar': '01', 'listoIzquierda': '04', 'listoDerecha': '04',
    'listoEspecial': '04', 'especialCabeza': '08', 'armaIzquierda': '08', 'armaDerecha': '08', 'alcanceIzquierda': '02', 'alcanceDerecha': '02',
    'x': '02', 'y': '02', 'rumbo': '02', 'mira': '02', 'velocidad': '02', 'energia': '02', 'blindaje': '02', 'turnoActual': '02',
    'ronda': '02', 'anchoArena': '02', 'altoArena': '02', 'restanteAvance': '04', 'restanteGiro': '04', 'restanteMira': '04',
    'enemigosVistos': '15', 'anguloHacia': '06', 'distanciaA': '06', 'normalizar': '06', 'azar': '09', 'decir': '01', 'escribir': '01',
    'memoria': '15', 'alOirAlEquipo': '13', 'companeros': '15', 'avisarAlEquipo': '08',
}

def metodos(archivo):
    s = open(API + archivo, encoding='utf-8').read()
    out = []
    for m in re.finditer(r'(?m)(^    /\*\*((?:(?!\*/).)*)\*/\n)?^    public (?:final )?([\w<>]+) (\w+)\(([^)]*)\) \{', s, re.S):
        doc = re.sub(r'\s*\n\s*\*\s*', ' ', (m.group(2) or '')).strip()
        out.append((m.group(4), m.group(3) + ' ' + m.group(4) + '(' + m.group(5) + ')', doc))
    return out

def interfaz(archivo):
    s = open(API + archivo, encoding='utf-8').read()
    out = []
    for m in re.finditer(r'(?m)(^    /\*\*((?:(?!\*/).)*)\*/\n(?:\n)?)?^    ([\w<>]+) (\w+)\(\);', s, re.S):
        doc = re.sub(r'\s*\n\s*\*\s*', ' ', (m.group(2) or '')).strip()
        out.append((m.group(4), m.group(3) + ' ' + m.group(4) + '()', doc))
    return out

es, de, en = metodos('Metajavbot.java'), metodos('MetajavbotDe.java'), metodos('MetajavbotEn.java')
assert len(es) == len(de) == len(en), (len(es), len(de), len(en))
b = ['# La API de los robots (la genera herramientas/api_doc.py; no se toca a mano).', '']
for (n1, f1, d1), (n2, f2, d2), (n3, f3, d3) in zip(es, de, en):
    b += ['=== api.' + n1, 'tema: ' + TEMAS.get(n1, '06'), 'firma: ' + f1, 'firma.de: ' + f2, 'firma.en: ' + f3,
          'es: ' + (d1 or f1), 'de: ' + (d2 or d1 or f2), 'en: ' + (d3 or d1 or f3), '']
for a, bb, c, tema in (('Enemigo.java', 'Gegner.java', 'Enemy.java', '13'), ('Golpe.java', 'Treffer.java', 'Hit.java', '13')):
    for (n1, f1, d1), (n2, f2, d2), (n3, f3, d3) in zip(interfaz(a), interfaz(bb), interfaz(c)):
        clase = a[:-5]
        b += ['=== ' + clase + '.' + n1, 'tema: ' + tema, 'firma: ' + clase + ': ' + f1, 'firma.de: ' + bb[:-5] + ': ' + f2,
              'firma.en: ' + c[:-5] + ': ' + f3, 'es: ' + (d1 or f1), 'de: ' + (d2 or d1), 'en: ' + (d3 or d1), '']
open('res/datos/api.txt', 'w', encoding='utf-8', newline='\n').write('\n'.join(b))
print(len(es), 'métodos')
