# Rehace res/datos/api.txt (la API de los robots para el buscador del juego) a partir de las tres fachadas
# (Metajavbot.java, MetajavbotDe.java, MetajavbotEn.java) y de las interfaces Enemigo/Gegner/Enemy y Golpe/Treffer/Hit.
# Conserva las explicaciones pedagógicas, pistas «donde.*» y ejemplos existentes en res/datos/api.txt.
# Uso, desde la raíz: python herramientas/api_doc.py
import re, os

API = 'juego/src/main/java/metajavbots/api/'

# El tema de cada método (res/datos/temas.txt): dónde se aprende lo que hace falta para usarlo.
TEMAS = {
    'alEmpezar': '11', 'cadaTurno': '11', 'alVerEnemigo': '11', 'alRecibirGolpe': '11', 'alAcertar': '11',
    'alChocarConPared': '11', 'alChocarConEnemigo': '11', 'alAcabarRonda': '11',
    'avanzar': '01', 'retroceder': '01', 'girar': '01', 'girarIzquierda': '01', 'girarDerecha': '01', 'detener': '01',
    'girarHacia': '03', 'apuntar': '01', 'apuntarHacia': '03', 'apuntarA': '03',
    'atacarIzquierda': '04', 'atacarDerecha': '04', 'especial': '04', 'atacar': '01',
    'embestir': '04', 'pulsoRadar': '04', 'fortificar': '04', 'arquetipo': '08',
    'listoIzquierda': '04', 'listoDerecha': '04',
    'listoEspecial': '04', 'especialCabeza': '08', 'armaIzquierda': '08', 'armaDerecha': '08', 'alcanceIzquierda': '02', 'alcanceDerecha': '02',
    'x': '02', 'y': '02', 'rumbo': '02', 'mira': '02', 'velocidad': '02', 'energia': '02', 'blindaje': '02', 'turnoActual': '02',
    'ronda': '02', 'anchoArena': '02', 'altoArena': '02', 'restanteAvance': '04', 'restanteGiro': '04', 'restanteMira': '04',
    'enemigosVistos': '15', 'anguloHacia': '06', 'distanciaA': '06', 'normalizar': '06', 'azar': '09', 'decir': '01', 'escribir': '01',
    'pesoTotal': '02', 'cargaMaxima': '02', 'proporcionDeCarga': '03', 'sobrecargado': '04', 'velocidadMaxima': '02',
    'giroMaximo': '02', 'aceleracionMaxima': '02', 'locomocion': '08', 'terreno': '08', 'regularPotencia': '06',
    'memoria': '15', 'obstaculos': '15', 'aparatos': '15', 'miAparato': '13', 'conectar': '13', 'usarAparato': '13', 'alOirAlEquipo': '13', 'companeros': '15', 'avisarAlEquipo': '08',
}

EXTRAS = {
    'api.pesoTotal': {
        'donde.es': "Consúltalo en alEmpezar() o cadaTurno() para saber lo que cargan tus piernas; compáralo con cargaMaxima().",
        'donde.de': "In beimStart() oder jedenZug() abfragen, um zu wissen, was deine Beine tragen; vergleiche es mit traglast().",
        'donde.en': "Check it in onStart() or onTick() to know what your legs carry; compare it with loadLimit().",
    },
    'api.cargaMaxima': {
        'donde.es': "Consúltalo en alEmpezar(): depende de tus piernas (las orugas aguantan lo que más; las alas, lo que menos).",
        'donde.de': "In beimStart() abfragen: Es hängt von deinen Beinen ab (Ketten tragen am meisten, Flügel am wenigsten).",
        'donde.en': "Check it in onStart(): it depends on your legs (treads carry the most; wings, the least).",
    },
    'api.proporcionDeCarga': {
        'donde.es': "Úsalo en un if dentro de alEmpezar() o cadaTurno(): por encima de 1 vas sobrecargado y conviene no correr.",
        'donde.de': "In einem if in beimStart() oder jedenZug() verwenden: Über 1 bist du überladen und solltest nicht rasen.",
        'donde.en': "Use it in an if inside onStart() or onTick(): above 1 you are overloaded and should not rush around.",
    },
    'api.velocidadMaxima': {
        'donde.es': "Consúltalo en cadaTurno(): cambia con el terreno que pisas (en escombros o aceite, a las ruedas les baja mucho).",
        'donde.de': "In jedenZug() abfragen: Sie ändert sich mit dem Gelände (auf Trümmern oder Öl sinkt sie bei Rädern stark).",
        'donde.en': "Check it in onTick(): it changes with the terrain under you (on rubble or oil it drops a lot for wheels).",
    },
    'api.giroMaximo': {
        'donde.es': "Consúltalo en cadaTurno() antes de girar: si vas muy deprisa, frena primero para poder girar más.",
        'donde.de': "In jedenZug() vor dem Drehen abfragen: Fährst du sehr schnell, bremse zuerst, um stärker drehen zu können.",
        'donde.en': "Check it in onTick() before turning: if you are going very fast, brake first so you can turn more.",
    },
    'api.aceleracionMaxima': {
        'donde.es': "Consúltalo en cadaTurno(): sirve para calcular cuántos turnos tardas en llegar a tu velocidad máxima.",
        'donde.de': "In jedenZug() abfragen: Damit berechnest du, wie viele Züge du bis zur Höchstgeschwindigkeit brauchst.",
        'donde.en': "Check it in onTick(): use it to work out how many ticks you need to reach your top speed.",
    },
    'api.locomocion': {
        'donde.es': "Consúltalo en alEmpezar() para adaptar tu táctica a tus piernas; compáralo con equals().",
        'donde.de': "In beimStart() abfragen, um deine Taktik an deine Beine anzupassen; mit equals() vergleichen.",
        'donde.en': "Check it in onStart() to fit your tactics to your legs; compare it with equals().",
    },
    'api.terreno': {
        'donde.es': "Consúltalo en cadaTurno(): si pisas \"AGUA\" o \"TRAMPA\", sal de ahí; compáralo con equals().",
        'donde.de': "In jedenZug() abfragen: Stehst du auf \"AGUA\" (Wasser) oder \"TRAMPA\" (Falle), geh weg; mit equals() vergleichen.",
        'donde.en': "Check it in onTick(): if you are on \"AGUA\" (water) or \"TRAMPA\" (trap), get off it; compare it with equals().",
    },
    'Enemigo.locomocion': {
        'donde.es': "Llámalo sobre el parámetro e dentro de alVerEnemigo(e) para elegir arma: contra \"VUELO\", misiles; contra \"RUEDAS\", la cuña.",
        'donde.de': "Auf dem Parameter g in beiSicht(g) aufrufen, um die Waffe zu wählen: gegen \"VUELO\" Raketen, gegen \"RUEDAS\" den Keil.",
        'donde.en': "Call it on the parameter e inside onSeeEnemy(e) to pick a weapon: missiles against \"VUELO\", the wedge against \"RUEDAS\".",
    },
    'api.regularPotencia': {
        'donde.es': 'Llámalo antes de atacar (en cadaTurno() o alVerEnemigo(e)); solo cambia las armas de potencia regulable (cañón ligero, bláster de iones, cañón predictor).',
        'donde.de': 'Vor dem Angriff aufrufen (in jedenZug() oder beiSicht(g)); es ändert nur die Waffen mit regelbarer Leistung (leichte Kanone, Ionenblaster, Vorhersagekanone).',
        'donde.en': 'Call it before attacking (in onTick() or onSeeEnemy(e)); it only changes adjustable-power weapons (light cannon, ion blaster, predictor cannon).',
        'ejemplo.es': '    AL VER enemigo e:\n        SI e.distancia() < 250:\n            regularPotencia(3)\n        SI NO:\n            regularPotencia(1)\n        atacar()',
        'ejemplo.de': '    BEI SICHT Gegner g:\n        WENN g.entfernung() < 250:\n            leistungRegeln(3)\n        SONST:\n            leistungRegeln(1)\n        angreifen()',
        'ejemplo.en': '    ON SEE ENEMY e:\n        IF e.distance() < 250:\n            setPower(3)\n        ELSE:\n            setPower(1)\n        attack()',
    },
    'api.sobrecargado': {
        'donde.es': 'Consúltalo en alEmpezar() o cadaTurno(): si vas sobrecargado, no esperes esquivar a la carrera; mejor pelear de cara.',
        'donde.de': 'In beimStart() oder jedenZug() abfragen: Bist du überladen, kannst du nicht schnell ausweichen; kämpfe lieber frontal.',
        'donde.en': 'Check it in onStart() or onTick(): if you are overloaded, do not count on quick dodges; fight head-on instead.',
        'ejemplo.es': '    AL EMPEZAR:\n        SI sobrecargado():\n            decir("Despacio, pero seguro")',
        'ejemplo.de': '    BEIM START:\n        WENN ueberladen():\n            sagen("Langsam, aber sicher")',
        'ejemplo.en': '    ON START:\n        IF overloaded():\n            say("Slow but sure")',
    },
    'api.embestir': {
        'donde.es': 'Llámalo dentro de cadaTurno() o alVerEnemigo(e) solo en robots de arquetipo Asalto (en otros arquetipos devuelve false y avisa en consola).',
        'donde.de': 'In jedenZug() oder beiSicht(g) nur bei Sturm-Robotern aufrufen (bei anderen Archetypen liefert es false und warnt in der Konsole).',
        'donde.en': 'Call inside onTick() or onSeeEnemy(e) only on Assault-archetype robots (on other archetypes it returns false and warns in the console).',
        'ejemplo.es': '    AL VER enemigo e:\n        girarHacia(e.angulo())\n        SI e.distancia() > 120:\n            embestir()',
        'ejemplo.de': '    BEI SICHT von Gegner g:\n        drehenNach(g.winkel())\n        WENN g.entfernung() > 120:\n            ansturm()',
        'ejemplo.en': '    ON SEEING enemy e:\n        turnTo(e.angle())\n        IF e.distance() > 120:\n            charge()',
    },
    'api.pulsoRadar': {
        'donde.es': 'Llámalo dentro de cadaTurno() o alRecibirGolpe(g) solo en robots de arquetipo Táctico para revelar rivales en 360° (en otros avisa en consola).',
        'donde.de': 'In jedenZug() oder beiTreffer(t) nur bei Taktiker-Robotern aufrufen, um Gegner rundum in 360° zu erfassen (sonst Konsolenwarnung).',
        'donde.en': 'Call inside onTick() or onHit(h) only on Tactical-archetype robots to reveal enemies in 360° (otherwise logs a console warning).',
        'ejemplo.es': '    CADA TURNO:\n        SI enemigosVistos().isEmpty():\n            pulsoRadar()',
        'ejemplo.de': '    JEDEN ZUG:\n        WENN gesehen().isEmpty():\n            radarImpuls()',
        'ejemplo.en': '    EVERY TICK:\n        IF seenEnemies().isEmpty():\n            radarPulse()',
    },
    'api.fortificar': {
        'donde.es': 'Llámalo dentro de alRecibirGolpe(g) o cadaTurno() solo en robots de arquetipo Defensor para activar la postura blindada (en otros avisa en consola).',
        'donde.de': 'In beiTreffer(t) oder jedenZug() nur bei Verteidiger-Robotern aufrufen, um die Abwehrhaltung zu aktivieren (sonst Konsolenwarnung).',
        'donde.en': 'Call inside onHit(h) or onTick() only on Defender-archetype robots to raise the fortified stance (otherwise logs a console warning).',
        'ejemplo.es': '    AL RECIBIR GOLPE g:\n        fortificar()\n        apuntarHacia(g.angulo())',
        'ejemplo.de': '    BEI TREFFER t:\n        befestigen()\n        zielenNach(t.winkel())',
        'ejemplo.en': '    ON HIT h:\n        fortify()\n        aimTo(h.angle())',
    },
    'api.arquetipo': {
        'donde.es': 'Úsalo en condiciones dentro de alEmpezar() o cadaTurno() comparando con .equals("ASALTO"), .equals("TACTICO") o .equals("DEFENSOR").',
        'donde.de': 'In Bedingungen in beimStart() oder jedenZug() mit .equals("ASALTO"), .equals("TACTICO") oder .equals("DEFENSOR") vergleichen.',
        'donde.en': 'Use in conditions inside onStart() or onTick() comparing with .equals("ASALTO"), .equals("TACTICO") or .equals("DEFENSOR").',
        'ejemplo.es': '    AL EMPEZAR:\n        SI arquetipo().equals("ASALTO"):\n            decir("¡Al ataque!")',
        'ejemplo.de': '    BEIM START:\n        WENN archetyp().equals("ASALTO"):\n            sagen("Angriff!")',
        'ejemplo.en': '    ON START:\n        IF archetype().equals("ASALTO"):\n            say("Charge!")',
    },
}

def cargar_previo(ruta):
    if not os.path.exists(ruta):
        return {}
    datos = {}
    actual = None
    ultima = None
    for linea in open(ruta, encoding='utf-8').read().splitlines():
        if linea.startswith('#'):
            continue
        if linea.startswith('==='):
            actual = {}
            datos.setdefault(linea[3:].strip(), []).append(actual)
            ultima = None
            continue
        if actual is None or not linea.strip():
            continue
        if ':' in linea and not linea.startswith(' '):
            k, v = linea.split(':', 1)
            ultima = k.strip()
            actual[ultima] = v.strip()
        elif ultima is not None:
            actual[ultima] = (actual[ultima] + '\n' + linea) if actual[ultima] else linea
    return datos

def escribir_bloque(b, eid, tema, f1, f2, f3, d1, d2, d3, prev):
    cola = prev.get(eid)
    p = cola.pop(0) if cola else {}
    ex = EXTRAS.get(eid, {})
    b += [
        '=== ' + eid,
        'tema: ' + p.get('tema', tema),
        'firma: ' + f1,
        'firma.de: ' + f2,
        'firma.en: ' + f3,
        'es: ' + (p.get('es') or d1 or f1),
        'de: ' + (p.get('de') or d2 or d1 or f2),
        'en: ' + (p.get('en') or d3 or d1 or f3),
    ]
    for campo in ('donde.es', 'donde.de', 'donde.en'):
        val = p.get(campo) or ex.get(campo)
        if val:
            b.append(campo + ': ' + val)
    for campo in ('ejemplo', 'ejemplo.es', 'ejemplo.de', 'ejemplo.en'):
        val = p.get(campo) or ex.get(campo)
        if val:
            if '\n' in val or val.startswith('    '):
                b.append(campo + ':')
                b.append(val)
            else:
                b.append(campo + ': ' + val)
    b.append('')

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
    for m in re.finditer(r'(?m)(^    /\*\*((?:(?!\*/).)*)\*/\n(?:\n)?)?^    (?:default )?([\w<>]+) (\w+)\(\)(?:;| \{)', s, re.S):
        doc = re.sub(r'\s*\n\s*\*\s*', ' ', (m.group(2) or '')).strip()
        out.append((m.group(4), m.group(3) + ' ' + m.group(4) + '()', doc))
    return out

previo = cargar_previo('res/datos/api.txt')
es, de, en = metodos('Metajavbot.java'), metodos('MetajavbotDe.java'), metodos('MetajavbotEn.java')
assert len(es) == len(de) == len(en), (len(es), len(de), len(en))
b = ['# La API de los robots (con explicaciones pedagógicas y pseudocódigo por idioma).', '']
for (n1, f1, d1), (n2, f2, d2), (n3, f3, d3) in zip(es, de, en):
    eid = 'api.usarAparato.angulo' if (n1 == 'usarAparato' and 'angulo' in f1) else 'api.' + n1
    escribir_bloque(b, eid, TEMAS.get(n1, '06'), f1, f2, f3, d1, d2, d3, previo)
for a, bb, c, tema in (('Enemigo.java', 'Gegner.java', 'Enemy.java', '13'), ('Golpe.java', 'Treffer.java', 'Hit.java', '13'),
                       ('Obstaculo.java', 'Hindernis.java', 'Obstacle.java', '13'),
                       ('Aparato.java', 'Geraet.java', 'Gadget.java', '13')):
    for (n1, f1, d1), (n2, f2, d2), (n3, f3, d3) in zip(interfaz(a), interfaz(bb), interfaz(c)):
        clase = a[:-5]
        escribir_bloque(b, clase + '.' + n1, tema, clase + ': ' + f1, bb[:-5] + ': ' + f2, c[:-5] + ': ' + f3, d1, d2, d3, previo)
open('res/datos/api.txt', 'w', encoding='utf-8', newline='\n').write('\n'.join(b))
print(len(es), 'métodos')
