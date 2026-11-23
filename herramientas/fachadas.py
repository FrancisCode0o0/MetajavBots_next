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
    'embestir': ('ansturm', 'charge'), 'pulsoRadar': ('radarImpuls', 'radarPulse'), 'fortificar': ('befestigen', 'fortify'),
    'arquetipo': ('archetyp', 'archetype'),
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
    'aparatos': ('geraete', 'gadgets'), 'miAparato': ('meinGeraet', 'myGadget'), 'conectar': ('anschliessen', 'plugIn'),
    'usarAparato': ('geraetBenutzen', 'useGadget'),
    'companeros': ('kameraden', 'teammates'), 'avisarAlEquipo': ('teamMelden', 'tellTeam'),
    'anguloHacia': ('winkelZu', 'angleTo'), 'distanciaA': ('abstandZu', 'distanceTo'),
    'normalizar': ('normalisieren', 'normalize'),
    'azar': ('zufall', 'random'), 'decir': ('sagen', 'say'), 'escribir': ('ausgeben', 'print'),
    'memoria': ('gedaechtnis', 'memory'),
    'pesoTotal': ('gesamtgewicht', 'totalWeight'), 'cargaMaxima': ('traglast', 'loadLimit'),
    'proporcionDeCarga': ('lastanteil', 'loadRatio'), 'sobrecargado': ('ueberladen', 'overloaded'),
    'velocidadMaxima': ('hoechstgeschwindigkeit', 'topSpeed'), 'giroMaximo': ('maxDrehung', 'maxTurnRate'),
    'aceleracionMaxima': ('maxBeschleunigung', 'maxAcceleration'), 'locomocion': ('fortbewegung', 'locomotion'),
    'terreno': ('gelaende', 'terrain'), 'regularPotencia': ('leistungRegeln', 'setPower'),
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
 * Der Roboter hat drei Regler: die Räder (vorwaerts, rueckwaerts), den Körper
 * (drehen, drehenNach) und das Visier (zielen, zielenNach, zielenAuf). Jeder
 * Regler merkt sich nur den letzten Befehl des Zugs: Der spätere löscht den
 * früheren. Das Visier dreht langsam, und angreifen() schießt dorthin, wohin es
 * JETZT zeigt.
 *
 * Winkel in Grad: 0 ist rechts (Osten), 90 oben; positive Drehungen gehen nach
 * links. Die Arena reicht von (0, 0) unten links bis (arenaBreite(), arenaHoehe()).
 */""",
        'beimStart': 'Zu Beginn jeder Runde (vor dem ersten Zug). Thema 06: Methoden.',
        'jedenZug': 'In jedem Zug (etwa 30 pro Sekunde). Hier steht das Wichtigste. Kommt als Letztes: vorher laufen die Meldungen (beiTreffer, beiWand …) und beiSicht. Jeder Regler (Fahren, Drehen, Visier) merkt sich nur den letzten Befehl des Zugs, also löscht ein Befehl von hier den für denselben Regler aus einer Meldung.',
        'beiSicht': 'In jedem Zug, in dem dein Radar einen Gegner sieht, vor jedenZug. Achtung: Gibt jedenZug danach einen anderen Visierbefehl (zum Beispiel zielen), löscht er den von hier, und das Visier kommt nie beim Gegner an.',
        'beiTreffer': 'Wenn du getroffen wirst.',
        'beiEigenemTreffer': 'Wenn dein Angriff trifft.',
        'beiWand': 'Wenn du gegen eine Wand fährst (kostet etwas Panzerung).',
        'beiZusammenstoss': 'Wenn du mit einem Gegner zusammenstößt.',
        'beiRundenende': 'Am Ende der Runde.',
        'beiTeamNachricht': 'Wenn dir ein Teamkamerad etwas meldet (in Teamkämpfen).',
        'kameraden': 'Deine Teamkameraden (in Teamkämpfen; leer, wenn du allein kämpfst).',
        'teamMelden': 'Meldet deinen Kameraden etwas (es kommt im nächsten Zug in beiTeamNachricht an).',
        'vorwaerts': 'Fährt diese Strecke vorwärts (negativ: rückwärts). Ersetzt den vorigen Fahrbefehl: Nur der letzte des Zugs zählt. Er wartet nicht: Der Roboter erledigt ihn nach und nach, über mehrere Züge.',
        'rueckwaerts': 'Fährt diese Strecke rückwärts. Ist ein Fahrbefehl: Nur der letzte des Zugs zählt, und er wird nach und nach erledigt.',
        'drehen': 'Dreht den Körper um diese Grad (positiv: nach links), nach und nach über mehrere Züge. Nur der letzte Drehbefehl des Zugs zählt (drehen, drehenNach, anhalten …): Der später aufgerufene löscht den vorigen.',
        'drehenNach': 'Dreht den Körper auf diesen absoluten Winkel, auf dem kürzesten Weg. Nicht auf einen Schlag: Er kommt nach mehreren Zügen an. Nur der letzte Drehbefehl des Zugs zählt.',
        'anhalten': 'Hält an: weder fahren noch drehen.',
        'zielen': 'Dreht das Visier (Arme und Radar) um diese Grad, ohne den Körper zu drehen. Das Visier dreht langsam, etwa 14° pro Zug je nach Waffe. Nur der letzte Visierbefehl des Zugs zählt: Ein zielen in jedenZug löscht das zielenNach aus beiSicht.',
        'zielenNach': 'Will das Visier auf diesen absoluten Winkel drehen. Nicht sofort: Das Visier dreht langsam, etwa 14° pro Zug je nach Waffe, und restZielen() sagt, wie viel noch fehlt. Nur der letzte Visierbefehl des Zugs zählt: Ruft jedenZug danach zielen auf, ist dieser gelöscht.',
        'zielenAuf': 'Will das Visier auf diesen Punkt der Arena drehen. Wie zielenNach: Es braucht mehrere Züge, und nur der letzte Visierbefehl zählt.',
        'angriffLinks': 'Greift mit dem linken Arm in Blickrichtung an. Gibt false zurück, wenn er noch nicht bereit war.',
        'spezial': 'Benutzt das Spezielle des Kopfes (Schild, Turbo, Reparatur…, je nach Teil).',
        'ansturm': 'Eigene Fähigkeit des Archetyps Sturm: Vorwärts-Temposchub (warnt in der Konsole bei anderen Archetypen).',
        'radarImpuls': 'Eigene Fähigkeit des Archetyps Taktiker: 360°-Radarimpuls mit größerer Reichweite (warnt bei anderen Archetypen).',
        'befestigen': 'Eigene Fähigkeit des Archetyps Verteidiger: Abwehrhaltung, verringert Schaden (warnt bei anderen Archetypen).',
        'archetyp': 'Der Archetyp deines Robotermodells: "ASALTO", "TACTICO" oder "DEFENSOR".',
        'angreifen': 'Greift mit dem Arm an, der bereit ist (zuerst der rechte). Schießt sofort, dorthin, wohin das Visier JETZT zeigt, nicht dorthin, wohin es sich gerade drehen soll. Zweimal im selben Zug benutzt beide Arme; öfter bringt nichts, weil die Arme abkühlen müssen.',
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
        'geraete': 'Die Geräte auf dem Boden der Arena: Fahr zu einem hin, um es aufzuheben (leer, wenn es keine gibt; Thema 15).',
        'meinGeraet': 'Das Gerät, das du trägst (null, wenn keins).',
        'anschliessen': 'Schließt dein Gerät an diesem Platz an: "ruecken", "beine", "kopf" oder "arm" (lies seinen Code, um zu wissen, welcher).',
        'geraetBenutzen': 'Benutzt das angeschlossene Gerät (in Blickrichtung oder zu diesem Winkel). Gibt zurück, ob es ging.',
        'hindernisse': 'Die Fallen und Gegenstände der Arena: Stacheln, Sägen, Grube, Fässer … (leer, wenn es keine gibt; Thema 15).',
        'winkelZu': 'Der absolute Winkel von dir zu diesem Punkt.',
        'abstandZu': 'Der Abstand von dir zu diesem Punkt.',
        'normalisieren': 'Bringt einen Winkel nach (-180, 180]: normalisieren(350) ergibt -10.',
        'zufall': 'Zufallszahlen (die deines Roboters: derselbe Kampf ergibt dasselbe).',
        'sagen': 'Sagt etwas in einer Sprechblase über dem Roboter.',
        'ausgeben': 'Schreibt in die Konsole deines Roboters (unter dem Code).',
        'gedaechtnis': 'Was du dir von Runde zu Runde merken willst (Thema 15: Map). Bleibt, auch wenn du den Code änderst.',
        'gesamtgewicht': 'Was deine Beine tragen: das Chassis deines Modells und alle Teile außer den Beinen.',
        'traglast': 'Was deine Beine aushalten (ihre Traglast).',
        'lastanteil': 'gesamtgewicht() / traglast(): bis 0,70 bist du leicht, bis 1 normal, bis 1,20 überladen.',
        'ueberladen': 'Bist du überladen? Dann fährst und drehst du schlechter, verbrauchst mehr beim Fahren und springst und weichst nicht schnell aus.',
        'hoechstgeschwindigkeit': 'Deine Höchstgeschwindigkeit gerade jetzt, mit deinem Gewicht und dem Gelände unter dir.',
        'maxDrehung': 'Wie weit du in diesem Zug drehen kannst: Je schneller du fährst, desto schlechter drehst du.',
        'maxBeschleunigung': 'Deine Beschleunigung gerade jetzt (Bremsen geht doppelt so schnell).',
        'fortbewegung': 'Deine Beine: "BIPEDO", "ORUGAS" (Ketten), "RUEDAS" (Räder), "FLOTANTE" (Schweber), "VUELO" (Flug) oder "PATAS" (Spinnenbeine).',
        'gelaende': 'Das Gelände unter dir: "LLANO" (eben), "ACEITE" (Öl), "AGUA" (Wasser), "RAMPA", "ESCOMBROS" (Trümmer) oder "TRAMPA" (Bodenfalle).',
        'leistungRegeln': 'Die Leistung der regelbaren Waffen (0,1 bis 3): mehr Leistung, mehr Schaden, aber langsamere Kugel, mehr Hitze und mehr Energie.',
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
 * The robot has three controls: the wheels (ahead, back), the body (turn,
 * turnTo) and the sight (aim, aimTo, aimAt). Each control remembers only the
 * last order of the tick: the later one erases the earlier one. The sight
 * turns slowly, and attack() fires where it points NOW.
 *
 * Angles in degrees: 0 is right (east), 90 is up; positive turns go left.
 * The arena goes from (0, 0), bottom left, to (arenaWidth(), arenaHeight()).
 */""",
        'onStart': 'At the start of every round (before the first tick). Topic 06: methods.',
        'onTick': 'Every tick (about 30 per second). The main logic goes here. It is called last: the events (onHit, onHitWall…) and onSeeEnemy come first. Each control (move, turn and sight) remembers only the last order of the tick, so an order here erases the one for the same control you gave in an event.',
        'onSeeEnemy': 'Every tick in which your radar sees an enemy, before onTick. Careful: if onTick then gives another sight order (for example aim), it erases the one you give here, and the sight never reaches the enemy.',
        'onHit': 'When you get hit.',
        'onHitEnemy': 'When your attack lands.',
        'onHitWall': 'When you crash into a wall (it costs a little armor).',
        'onBump': 'When you bump into an enemy.',
        'onRoundEnd': 'At the end of the round.',
        'onTeamMessage': 'When a teammate tells you something (in team fights).',
        'teammates': 'Your teammates (in team fights; empty when you fight alone).',
        'tellTeam': 'Tells your teammates something (it arrives next tick in onTeamMessage).',
        'ahead': 'Moves forward this distance (negative: backward). Replaces the previous move order: only the last one of the tick counts. It does not wait: the robot carries it out little by little, over several ticks.',
        'back': 'Moves backward this distance. It is a move order: only the last one of the tick counts, and it is carried out little by little.',
        'turn': 'Turns the body this many degrees (positive: left), little by little over several ticks. Only the last turn order of the tick counts (turn, turnTo, stop…): the one called later erases the earlier one.',
        'turnTo': 'Turns the body to face this absolute angle, the short way. Not all at once: it gets there over several ticks. Only the last turn order of the tick counts.',
        'stop': 'Stops: no moving, no turning.',
        'aim': 'Turns the sight (arms and radar) this many degrees without turning the body. The sight turns slowly, about 14° per tick depending on the weapon. Only the last sight order of the tick counts: an aim in onTick erases the aimTo you gave in onSeeEnemy.',
        'aimTo': 'Asks the sight to turn to this absolute angle. Not instant: the sight turns slowly, about 14° per tick depending on the weapon, and remainingAim() tells you how much is left. Only the last sight order of the tick counts: if onTick calls aim afterwards, this one is erased.',
        'aimAt': 'Asks the sight to turn toward this point of the arena. Like aimTo: it takes several ticks and only the last sight order counts.',
        'attackLeft': 'Attacks with the left arm where the sight points. Returns false if it was not ready yet.',
        'special': "Uses the head's special (a shield, a turbo, repair…, depending on the part).",
        'charge': 'Signature ability of the Assault archetype: forward speed burst (warns in the console on other archetypes).',
        'radarPulse': 'Signature ability of the Tactical archetype: 360° extended-range radar sweep (warns on other archetypes).',
        'fortify': 'Signature ability of the Defender archetype: fortified stance that reduces incoming damage (warns on other archetypes).',
        'archetype': 'The archetype of your robot model: "ASALTO", "TACTICO" or "DEFENSOR".',
        'attack': 'Attacks with whichever arm is ready (right first). Fires at once, where the sight points NOW, not where you just asked it to turn. Twice in the same tick uses both arms; more calls do nothing, because the arms need to cool down.',
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
        'gadgets': 'The gadgets on the arena floor: drive to one to pick it up (empty if there are none; topic 15).',
        'myGadget': 'The gadget you carry (null if none).',
        'plugIn': 'Plugs the gadget you carry into that slot: "back", "legs", "head" or "arm" (read its code to know which).',
        'useGadget': 'Uses the plugged-in gadget (where you aim, or at that angle). Returns whether it worked.',
        'obstacles': 'The traps and objects in the arena: spikes, saws, pit, barrels… (empty if there are none; topic 15).',
        'angleTo': 'The absolute angle from you to that point.',
        'distanceTo': 'The distance from you to that point.',
        'normalize': 'Brings an angle into (-180, 180]: normalize(350) gives -10.',
        'random': "Random numbers (your robot's own: the same fight replayed gives the same).",
        'say': 'Says something in a speech bubble above the robot.',
        'print': "Writes to your robot's console (below the code).",
        'memory': 'What you want to remember from round to round (topic 15: Map). Survives even if you change the code.',
        'totalWeight': 'What your legs carry: the chassis of your model and all your parts except the legs.',
        'loadLimit': 'What your legs can carry (their load limit).',
        'loadRatio': 'totalWeight() / loadLimit(): up to 0.70 you are light, up to 1 normal, up to 1.20 overloaded.',
        'overloaded': 'Are you overloaded? Then you drive and turn worse, spend more energy moving, and cannot jump or dodge quickly.',
        'topSpeed': 'Your top speed right now, with your weight and the terrain under you.',
        'maxTurnRate': 'How far you can turn this tick: the faster you go, the worse you turn.',
        'maxAcceleration': 'Your acceleration right now (braking is twice as fast).',
        'locomotion': 'Your legs: "BIPEDO" (biped), "ORUGAS" (treads), "RUEDAS" (wheels), "FLOTANTE" (hover), "VUELO" (flight), or "PATAS" (spider legs).',
        'terrain': 'The terrain under you: "LLANO" (flat), "ACEITE" (oil), "AGUA" (water), "RAMPA" (ramp), "ESCOMBROS" (rubble), or "TRAMPA" (floor trap).',
        'setPower': 'The power of adjustable-power weapons (0.1 to 3): more power, more damage, but a slower bullet, more heat, and more energy.',
    }}
tipos = {'de': {'Enemigo': 'Gegner', 'Golpe': 'Treffer', 'Obstaculo': 'Hindernis', 'Aparato': 'Geraet'}, 'en': {'Enemigo': 'Enemy', 'Golpe': 'Hit', 'Obstaculo': 'Obstacle', 'Aparato': 'Gadget'}}
params = {'de': {'distancia': 'strecke', 'grados': 'grad', 'angulo': 'winkel', 'texto': 'text', 'mensaje': 'nachricht', 'de': 'von', 'g': 't', 'e': 'g', 'hueco': 'platz', 'potencia': 'leistung'},
          'en': {'distancia': 'distance', 'grados': 'degrees', 'angulo': 'angle', 'texto': 'text', 'mensaje': 'message', 'de': 'from', 'e': 'e', 'g': 'h', 'hueco': 'slot', 'potencia': 'power'}}
secciones = [('lo que sobrescribes', ('was du überschreibst', 'what you override')), ('moverse', ('fahren', 'moving')),
             ('peso, carga y movilidad', ('Gewicht, Traglast und Beweglichkeit', 'weight, load, and mobility')),
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
    s = re.sub(r'this::(\w+)', lambda mo: 'this::' + allmap.get(mo.group(1), mo.group(1)), s)
    s = re.sub(r'ejecutarEn\("(\w+)"', lambda mo: 'ejecutarEn("' + allmap.get(mo.group(1), mo.group(1)) + '"', s)
    s = s.replace('§', '')
    # javadocs
    def redoc(mo):
        decl, name = mo.group(2), mo.group(3)
        t = doc[lang].get(name)
        if not t:
            return decl
        if len(t) <= 150:
            return '    /** %s */\n' % t + decl
        import textwrap
        return '    /**\n' + ''.join('     * %s\n' % l for l in textwrap.wrap(t, 110)) + '     */\n' + decl
    s = re.sub(r'(^    /\*\*(?:(?!\*/).)*\*/\n)?(^    public (?:final )?[\w<>]+(?:, [\w<>]+)* (\w+)\([^)]*\) \{)', redoc, s, flags=re.S | re.M)
    for a, b in tipos[lang].items():
        s = re.sub(r'\b%s\b' % a, b, s)
    # parameter names inside each method
    def fix(mo):
        bloque = mo.group(0)
        for a, b in params[lang].items():
            bloque = re.sub(r'\b%s\b' % a, b, bloque)
        return bloque
    s = re.sub(r'^    public (?:final )?[\w<>]+(?:, [\w<>]+)* \w+\([^)]*\) \{\n(?:        .*\n)*?    \}', fix, s, flags=re.M)
    open(cls + '.java', 'w', encoding='utf-8', newline='\n').write(s)
print('ok')
