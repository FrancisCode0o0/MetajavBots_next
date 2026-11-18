# Rehace MetajavbotDe.java y MetajavbotEn.java a partir de Metajavbot.java (la API en español).
# Uso, desde la raíz: python herramientas/fachadas.py juego/src/main/java/metajavbots/api
# Si cambias Metajavbot.java, pásalo y añade aquí la traducción de lo nuevo.
import re, sys, os
os.chdir(sys.argv[1])
src = open('Metajavbot.java', encoding='utf-8').read()
hooks = {
    'alEmpezar': ('beimStart', 'onStart'),
    'cadaTurno': ('jedenZug', 'onTick'),
    'alVerEnemigo': ('beiSicht', 'onSeeEnemy'),
    'alRecibirGolpe': ('beiTreffer', 'onHit'),
    'alAcertar': ('beiEigenemTreffer', 'onHitEnemy'),
    'alChocarConPared': ('beiWand', 'onHitWall'),
    'alChocarConEnemigo': ('beiZusammenstoss', 'onBump'),
    'alAcabarRonda': ('beiRundenende', 'onRoundEnd'),
    'alOirAlEquipo': ('beiTeamNachricht', 'onTeamMessage'),
}
metodos = {
    'avanzar': ('vorwaerts', 'ahead'), 'retroceder': ('rueckwaerts', 'back'), 'girar': ('drehen', 'turn'),
    'girarIzquierda': ('linksDrehen', 'turnLeft'), 'girarDerecha': ('rechtsDrehen', 'turnRight'),
    'girarHacia': ('drehenNach', 'turnTo'), 'detener': ('anhalten', 'stop'), 'apuntar': ('zielen', 'aim'),
    'apuntarHacia': ('zielenNach', 'aimTo'), 'apuntarA': ('zielenAuf', 'aimAt'),
    'atacarIzquierda': ('angriffLinks', 'attackLeft'), 'atacarDerecha': ('angriffRechts', 'attackRight'),
    'especial': ('spezial', 'special'), 'atacar': ('angreifen', 'attack'),
    'listoIzquierda': ('bereitLinks', 'readyLeft'), 'listoDerecha': ('bereitRechts', 'readyRight'),
    'listoEspecial': ('bereitSpezial', 'readySpecial'),
    'especialCabeza': ('spezialKopf', 'headSpecial'),
    'armaIzquierda': ('waffeLinks', 'weaponLeft'), 'armaDerecha': ('waffeRechts', 'weaponRight'),
    'alcanceIzquierda': ('reichweiteLinks', 'rangeLeft'), 'alcanceDerecha': ('reichweiteRechts', 'rangeRight'),
    'x': ('x', 'x'), 'y': ('y', 'y'), 'rumbo': ('richtung', 'heading'), 'mira': ('blick', 'aimDirection'),
    'velocidad': ('geschwindigkeit', 'speed'), 'energia': ('energie', 'energy'), 'blindaje': ('panzerung', 'armor'),
    'turnoActual': ('zug', 'tick'), 'ronda': ('runde', 'round'), 'anchoArena': ('arenaBreite', 'arenaWidth'),
    'altoArena': ('arenaHoehe', 'arenaHeight'),
    'restanteAvance': ('restStrecke', 'remainingDistance'), 'restanteGiro': ('restDrehung', 'remainingTurn'),
    'restanteMira': ('restZielen', 'remainingAim'),
    'enemigosVistos': ('gesehen', 'seenEnemies'), 'obstaculos': ('hindernisse', 'obstacles'),
    'companeros': ('kameraden', 'teammates'), 'avisarAlEquipo': ('teamMelden', 'tellTeam'),
    'anguloHacia': ('winkelZu', 'angleTo'), 'distanciaA': ('abstandZu', 'distanceTo'),
    'normalizar': ('normalisieren', 'normalize'),
    'azar': ('zufall', 'random'), 'decir': ('sagen', 'say'), 'escribir': ('ausgeben', 'print'),
    'memoria': ('gedaechtnis', 'memory'),
}
doc = {
    'de': {
        'class': """/**
 * METAJAVBOT (DEUTSCH) — Dein Roboter auf Deutsch. Schreibe eine Klasse, die
 * von dieser erbt (Thema 11, Vererbung), und überschreibe (@Override) die
 * Methoden «bei…» und «jedenZug»:
 *
 * <pre>
 * public class MeinRoboter extends MetajavbotDe {
 *     &#64;Override
 *     public void jedenZug() {
 *         vorwaerts(50);
 *         drehen(10);
 *     }
 *
 *     &#64;Override
 *     public void beiSicht(Gegner g) {
 *         zielenNach(g.winkel());
 *         angreifen();
 *     }
 * }
 * </pre>
 *
 * In jedem Zug ruft der Kampf zuerst die Meldungen auf (beiTreffer, beiWand…),
 * dann beiSicht für jeden sichtbaren Gegner und zuletzt jedenZug. Befehle
 * (vorwaerts, drehen, zielen) warten nicht: Sie sagen, was du willst, und der
 * Roboter erledigt es Zug um Zug in seinem Tempo.
 *
 * Winkel in Grad: 0 ist rechts (Osten), 90 oben; positive Drehungen gehen nach
 * links. Die Arena reicht von (0, 0) unten links bis (arenaBreite(), arenaHoehe()).
 */""",
        'beimStart': 'Zu Beginn jeder Runde (vor dem ersten Zug). Thema 06: Methoden.',
        'jedenZug': 'In jedem Zug (etwa 30 pro Sekunde). Hier steht das Wichtigste.',
        'beiSicht': 'In jedem Zug, in dem dein Radar einen Gegner sieht.',
        'beiTreffer': 'Wenn du getroffen wirst.',
        'beiEigenemTreffer': 'Wenn dein Angriff trifft.',
        'beiWand': 'Wenn du gegen eine Wand fährst (kostet etwas Panzerung).',
        'beiZusammenstoss': 'Wenn du mit einem Gegner zusammenstößt.',
        'beiRundenende': 'Am Ende der Runde.',
        'beiTeamNachricht': 'Wenn dir ein Teamkamerad etwas meldet (in Teamkämpfen).',
        'kameraden': 'Deine Teamkameraden (in Teamkämpfen; leer, wenn du allein kämpfst).',
        'teamMelden': 'Meldet deinen Kameraden etwas (es kommt im nächsten Zug in beiTeamNachricht an).',
        'vorwaerts': 'Fährt diese Strecke vorwärts (negativ: rückwärts). Ersetzt den vorigen Fahrbefehl.',
        'rueckwaerts': 'Fährt diese Strecke rückwärts.',
        'drehen': 'Dreht den Körper um diese Grad (positiv: nach links).',
        'drehenNach': 'Dreht den Körper auf diesen absoluten Winkel, auf dem kürzesten Weg.',
        'anhalten': 'Hält an: weder fahren noch drehen.',
        'zielen': 'Dreht das Visier (Arme und Radar) um diese Grad, ohne den Körper zu drehen.',
        'zielenNach': 'Dreht das Visier auf diesen absoluten Winkel.',
        'zielenAuf': 'Dreht das Visier auf diesen Punkt der Arena.',
        'angriffLinks': 'Greift mit dem linken Arm in Blickrichtung an. Gibt false zurück, wenn er noch nicht bereit war.',
        'spezial': 'Benutzt das Spezielle des Kopfes (Schild, Turbo, Reparatur…, je nach Teil).',
        'angreifen': 'Greift mit dem Arm an, der bereit ist (zuerst der rechte).',
        'waffeLinks': 'Die Waffe des linken Arms: "LASER", "BALA" (Kugel), "MISIL" (Rakete), "CUERPO" (Nahkampf), "ESCUDO" (Schild), "REPARAR"… ("" wenn keine).',
        'spezialKopf': 'Das Spezielle des Kopfes: "ESCUDO" (Schild), "TURBO", "REPARAR" (Reparatur), "CAMUFLAJE" (Tarnung), "PULSO", "SOBRECARGA" ("" wenn keins).',
        'reichweiteLinks': 'Wie weit die Waffe des linken Arms reicht.',
        'richtung': 'Wohin der Körper schaut (Grad).',
        'blick': 'Wohin das Visier schaut (Grad).',
        'energie': 'Die Energie (Laser und Spezielles verbrauchen sie; sie lädt sich von selbst).',
        'panzerung': 'Deine restliche Panzerung, von 0 bis 100.',
        'zug': 'Die Nummer des Zuges in dieser Runde (ab 0).',
        'runde': 'Die Runde (1 bis 4).',
        'restStrecke': 'Was vom letzten Fahrbefehl noch fehlt.',
        'restDrehung': 'Was vom letzten Drehbefehl noch fehlt.',
        'gesehen': 'Die Gegner, die du gerade siehst (Thema 15: Collections).',
        'hindernisse': 'Die Fallen und Gegenstände der Arena: Stacheln, Sägen, Grube, Fässer … (leer, wenn es keine gibt; Thema 15).',
        'winkelZu': 'Der absolute Winkel von dir zu diesem Punkt.',
        'abstandZu': 'Der Abstand von dir zu diesem Punkt.',
        'normalisieren': 'Bringt einen Winkel nach (-180, 180]: normalisieren(350) ergibt -10.',
        'zufall': 'Zufallszahlen (die deines Roboters: derselbe Kampf ergibt dasselbe).',
        'sagen': 'Sagt etwas in einer Sprechblase über dem Roboter.',
        'ausgeben': 'Schreibt in die Konsole deines Roboters (unter dem Code).',
        'gedaechtnis': 'Was du dir von Runde zu Runde merken willst (Thema 15: Map). Bleibt, auch wenn du den Code änderst.',
    },
    'en': {
        'class': """/**
 * METAJAVBOT (ENGLISH) — Your robot, in English. Write a class that extends
 * this one (topic 11, inheritance) and override (@Override) the «on…»
 * methods to give it behavior:
 *
 * <pre>
 * public class MyRobot extends MetajavbotEn {
 *     &#64;Override
 *     public void onTick() {
 *         ahead(50);
 *         turn(10);
 *     }
 *
 *     &#64;Override
 *     public void onSeeEnemy(Enemy e) {
 *         aimTo(e.angle());
 *         attack();
 *     }
 * }
 * </pre>
 *
 * Every tick the fight first calls the events (onHit, onHitWall…), then
 * onSeeEnemy for each enemy in sight and finally onTick. Orders (ahead, turn,
 * aim) don't wait: they say what you want and the robot does it tick by tick,
 * at its own speed.
 *
 * Angles in degrees: 0 is right (east), 90 is up; positive turns go left.
 * The arena goes from (0, 0), bottom left, to (arenaWidth(), arenaHeight()).
 */""",
        'onStart': 'At the start of every round (before the first tick). Topic 06: methods.',
        'onTick': 'Every tick (about 30 per second). The main logic goes here.',
        'onSeeEnemy': 'Every tick in which your radar sees an enemy.',
        'onHit': 'When you get hit.',
        'onHitEnemy': 'When your attack lands.',
        'onHitWall': 'When you crash into a wall (it costs a little armor).',
        'onBump': 'When you bump into an enemy.',
        'onRoundEnd': 'At the end of the round.',
        'onTeamMessage': 'When a teammate tells you something (in team fights).',
        'teammates': 'Your teammates (in team fights; empty when you fight alone).',
        'tellTeam': 'Tells your teammates something (it arrives next tick in onTeamMessage).',
        'ahead': 'Moves forward this distance (negative: backwards). Replaces the previous move order.',
        'back': 'Moves backwards this distance.',
        'turn': 'Turns the body this many degrees (positive: left).',
        'turnTo': 'Turns the body to face this absolute angle, the short way.',
        'stop': 'Stops: no moving, no turning.',
        'aim': 'Turns the sight (arms and radar) this many degrees without turning the body.',
        'aimTo': 'Turns the sight to this absolute angle.',
        'aimAt': 'Turns the sight towards this point of the arena.',
        'attackLeft': 'Attacks with the left arm where the sight points. Returns false if it was not ready yet.',
        'special': "Uses the head's special (a shield, a turbo, repair…, depending on the part).",
        'attack': 'Attacks with whichever arm is ready (right first).',
        'weaponLeft': 'The left arm weapon: "LASER", "BALA" (bullet), "MISIL" (missile), "CUERPO" (melee), "ESCUDO" (shield), "REPARAR" (repair)… ("" if none).',
        'headSpecial': 'The head special: "ESCUDO" (shield), "TURBO", "REPARAR" (repair), "CAMUFLAJE" (cloak), "PULSO" (pulse), "SOBRECARGA" (overload) ("" if none).',
        'rangeLeft': "How far the left arm's weapon reaches.",
        'heading': 'Where the body faces (degrees).',
        'aimDirection': 'Where the sight points (degrees).',
        'energy': 'Energy (lasers and specials use it; it recharges by itself).',
        'armor': 'Your remaining armor, from 0 to 100.',
        'tick': 'The tick number in this round (from 0).',
        'round': 'The round (1 to 4).',
        'remainingDistance': 'What is left of the last move order.',
        'remainingTurn': 'What is left of the last turn order.',
        'seenEnemies': 'The enemies you can see right now (topic 15: collections).',
        'obstacles': 'The traps and objects in the arena: spikes, saws, pit, barrels… (empty if there are none; topic 15).',
        'angleTo': 'The absolute angle from you to that point.',
        'distanceTo': 'The distance from you to that point.',
        'normalize': 'Brings an angle into (-180, 180]: normalize(350) gives -10.',
        'random': "Random numbers (your robot's own: the same fight replayed gives the same).",
        'say': 'Says something in a speech bubble above the robot.',
        'print': "Writes to your robot's console (below the code).",
        'memory': 'Whatever you want to remember from round to round (topic 15: Map). It survives code changes.',
    }}
tipos = {'de': {'Enemigo': 'Gegner', 'Golpe': 'Treffer', 'Obstaculo': 'Hindernis'}, 'en': {'Enemigo': 'Enemy', 'Golpe': 'Hit', 'Obstaculo': 'Obstacle'}}
params = {'de': {'distancia': 'strecke', 'grados': 'grad', 'angulo': 'winkel', 'texto': 'text', 'mensaje': 'nachricht', 'de': 'von', 'g': 't', 'e': 'g'},
          'en': {'distancia': 'distance', 'grados': 'degrees', 'angulo': 'angle', 'texto': 'text', 'mensaje': 'message', 'de': 'from', 'e': 'e', 'g': 'h'}}
secciones = [('lo que sobrescribes', ('was du überschreibst', 'what you override')), ('moverse', ('fahren', 'moving')),
             ('apuntar y atacar', ('zielen und angreifen', 'aiming and attacking')), ('saber', ('wissen', 'knowing')),
             ('ayudas', ('Hilfen', 'helpers'))]

for lang, idx, cls in (('de', 0, 'MetajavbotDe'), ('en', 1, 'MetajavbotEn')):
    s = src
    s = re.sub(r'/\*\*\n \* METAJAVBOT — .*?\*/', lambda m: doc[lang]['class'], s, count=1, flags=re.S)
    s = s.replace('public abstract class Metajavbot extends', 'public abstract class %s extends' % cls)
    for a, b in secciones:
        s = s.replace('// ================================================================ ' + a + '\n',
                      '// ================================================================ ' + b[idx] + '\n')
    allmap = {**{k: v[idx] for k, v in hooks.items()}, **{k: v[idx] for k, v in metodos.items()}}
    s = re.sub(r'm\(\)\.(\w+)\(', lambda mo: 'm().§' + mo.group(1) + '(', s)
    s = re.sub(r'(?<![\w§.])(\w+)\(', lambda mo: allmap.get(mo.group(1), mo.group(1)) + '(', s)
    s = s.replace('§', '')
    # javadocs
    def redoc(mo):
        decl, name = mo.group(2), mo.group(3)
        t = doc[lang].get(name)
        return ('    /** %s */\n' % t if t else '') + decl
    s = re.sub(r'(^    /\*\*(?:(?!\*/).)*\*/\n)?(^    public (?:final )?[\w<>]+ (\w+)\([^)]*\) \{)', redoc, s, flags=re.S | re.M)
    for a, b in tipos[lang].items():
        s = re.sub(r'\b%s\b' % a, b, s)
    # parameter names inside each method
    def fix(mo):
        bloque = mo.group(0)
        for a, b in params[lang].items():
            bloque = re.sub(r'\b%s\b' % a, b, bloque)
        return bloque
    s = re.sub(r'^    public (?:final )?[\w<>]+ \w+\([^)]*\) \{\n(?:        .*\n)*?    \}', fix, s, flags=re.M)
    open(cls + '.java', 'w', encoding='utf-8', newline='\n').write(s)
print('ok')
