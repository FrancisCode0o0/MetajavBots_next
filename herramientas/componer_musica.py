"""COMPONER MÚSICA — Escribe los temas MIDI originales del juego en res/musica/.

Cada tema es un homenaje de género a las películas de las campañas (mismo tipo de
instrumentos y de energía), con melodías y motivos propios: ninguno copia una
melodía conocida. Sin librerías: el archivo MIDI se escribe a mano.

    python herramientas/componer_musica.py

Notación de las melodías: «C5/1 E5/.5 r/.5» = do de la 5.ª octava, una negra;
mi, una corchea; silencio, una corchea. Los acordes, «C4+E4+G4/2».
"""
import os
import struct

TPB = 480  # ticks por negra
NOTAS = {'C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11}
SALIDA = os.path.join(os.path.dirname(__file__), '..', 'res', 'musica')


def altura(n):
    """«C#5» → número MIDI (do central, C4 = 60)."""
    base = NOTAS[n[0]]
    resto = n[1:]
    while resto and resto[0] in '#b':
        base += 1 if resto[0] == '#' else -1
        resto = resto[1:]
    return 12 * (int(resto) + 1) + base


class Pista:
    def __init__(self, canal, programa, nombre):
        self.canal, self.programa, self.nombre = canal, programa, nombre
        self.notas = []  # (inicio en negras, duración, altura, fuerza)

    def nota(self, inicio, dur, alt, fuerza=90):
        self.notas.append((inicio, dur, alt, fuerza))

    def melodia(self, texto, inicio=0.0, fuerza=90, legato=0.92, transponer=0):
        """Pone una melodía en la notación del archivo; devuelve dónde acaba."""
        t = inicio
        for trozo in texto.split():
            notas, dur = trozo.split('/')
            dur = float(dur)
            if notas != 'r':
                for n in notas.split('+'):
                    self.nota(t, dur * legato, altura(n) + transponer, fuerza)
            t += dur
        return t

    def bytes(self):
        ev = []
        if self.canal != 9:
            ev.append((0, 1, bytes([0xC0 | self.canal, self.programa])))
        for ini, dur, alt, f in self.notas:
            a = max(0, min(127, alt))
            ev.append((int(ini * TPB), 2, bytes([0x90 | self.canal, a, max(1, min(127, f))])))
            ev.append((int((ini + dur) * TPB), 0, bytes([0x80 | self.canal, a, 0])))
        ev.sort(key=lambda e: (e[0], e[1]))
        datos = bytearray(b'\x00\xff\x03' + vlq(len(self.nombre)) + self.nombre.encode('ascii'))
        ahora = 0
        for tick, _, msg in ev:
            datos += vlq(tick - ahora) + msg
            ahora = tick
        datos += b'\x00\xff\x2f\x00'
        return b'MTrk' + struct.pack('>I', len(datos)) + bytes(datos)


def vlq(n):
    salida = [n & 0x7F]
    n >>= 7
    while n:
        salida.insert(0, (n & 0x7F) | 0x80)
        n >>= 7
    return bytes(salida)


def guardar(nombre, bpm, pistas):
    tempo = int(60_000_000 / bpm)
    cab = b'\x00\xff\x51\x03' + struct.pack('>I', tempo)[1:] + b'\x00\xff\x2f\x00'
    pista0 = b'MTrk' + struct.pack('>I', len(cab)) + cab
    cuerpo = pista0 + b''.join(p.bytes() for p in pistas)
    os.makedirs(SALIDA, exist_ok=True)
    with open(os.path.join(SALIDA, nombre + '.mid'), 'wb') as f:
        f.write(b'MThd' + struct.pack('>IHHH', 6, 1, len(pistas) + 1, TPB) + cuerpo)
    print(nombre, len(cuerpo), 'bytes')


# ---------------------------------------------------------------- ayudas

BOMBO, CAJA, CHARLES, PLATO, TOM_GRAVE, TOM_ALTO, TAIKO = 36, 38, 42, 49, 41, 48, 45


def bateria(p, compases, patron, inicio=0.0, fuerza=80):
    """patron: dict instrumento → lista de tiempos dentro del compás de 4 negras."""
    for c in range(compases):
        for inst, tiempos in patron.items():
            for t in tiempos:
                p.nota(inicio + c * 4 + t, 0.2, inst, fuerza if inst != CHARLES else fuerza - 25)


def acordes(p, cifrado, dur=4, inicio=0.0, fuerza=60, octava=4):
    """Acordes tenidos: «C Am F G»; m = menor, 5 = quinta sola."""
    t = inicio
    for a in cifrado.split():
        raiz = altura(a[0] + (a[1] if len(a) > 1 and a[1] in '#b' else '') + str(octava))
        menor = 'm' in a[1:]
        quinta = a.endswith('5')
        notas = [raiz, raiz + 7] if quinta else [raiz, raiz + (3 if menor else 4), raiz + 7]
        for n in notas:
            p.nota(t, dur * 0.98, n, fuerza)
        t += dur
    return t


def bajo(p, cifrado, figura, dur=4, inicio=0.0, fuerza=85, octava=2):
    """Bajo según el acorde: figura = lista de (tiempo, intervalo en semitonos)."""
    t = inicio
    for a in cifrado.split():
        raiz = altura(a[0] + (a[1] if len(a) > 1 and a[1] in '#b' else '') + str(octava))
        for tt, iv in figura:
            p.nota(t + tt, 0.45, raiz + iv, fuerza)
        t += dur
    return t


# ---------------------------------------------------------------- los temas

def entrenamiento():
    """Montaje de entrenamiento: metales triunfales que suben, bajo que empuja y batería de marcha."""
    met, cue, baj, bat = Pista(0, 56, 'trompetas'), Pista(1, 48, 'cuerdas'), Pista(2, 33, 'bajo'), Pista(9, 0, 'bateria')
    prog = 'C C F G Am F G G'
    for vuelta in range(2):
        t0 = vuelta * 32
        acordes(cue, prog, inicio=t0, fuerza=55)
        bajo(baj, prog, [(0, 0), (1, 7), (2, 12), (3, 7)], inicio=t0)
        bateria(bat, 8, {BOMBO: [0, 2, 2.5], CAJA: [1, 3], CHARLES: [0, .5, 1, 1.5, 2, 2.5, 3, 3.5]}, inicio=t0)
        # Motivo propio: salta una sexta, baja por grados y vuelve a subir más alto.
        met.melodia('E5/1 C6/1.5 B5/.5 A5/1 G5/2 r/.5 G5/.5 A5/1 C6/1 D6/1.5 C6/.5 B5/1 G5/2 r/1 '
                    'A5/1 E6/1.5 D6/.5 C6/1 A5/1.5 G5/.5 F5/1 A5/1 C6/1 F6/1.5 E6/.5 D6/2 D6/1 E6/1 F6/1 G6/4',
                    inicio=t0, fuerza=100 if vuelta else 88)
    bateria(bat, 1, {PLATO: [0], BOMBO: [0]}, inicio=63)
    return [met, cue, baj, bat]


def oriental():
    """Aire oriental del maestro: koto en arpegios, flauta de bambú y taiko lejano (escala in-sen)."""
    koto, flauta, cue, bat = Pista(0, 107, 'koto'), Pista(1, 77, 'flauta'), Pista(2, 49, 'cuerdas'), Pista(9, 0, 'taiko')
    figuras = ['E4 F4 A4 B4', 'D4 E4 A4 B4', 'E4 F4 A4 D5', 'B3 E4 F4 A4']
    for c in range(16):
        f = figuras[c % 4].split()
        for i, n in enumerate(f + f[::-1][1:3] + [f[0]]):
            koto.nota(c * 4 + i * 0.5, 0.45, altura(n), 70)
    acordes(cue, 'Am5 Dm5 Am5 E5 ' * 4, inicio=0, fuerza=40, octava=3)
    flauta.melodia('r/8 E5/2 F5/1 E5/1 B4/3 r/1 A4/1.5 B4/.5 D5/2 F5/1 E5/3 r/4 '
                   'B5/2 A5/1 F5/1 E5/2 D5/1 B4/1 A4/4 r/2 B4/1 D5/1 E5/6 r/2', inicio=0, fuerza=82)
    for c in range(16):
        bat.nota(c * 4, 0.3, TAIKO, 75)
        if c % 2:
            bat.nota(c * 4 + 2.5, 0.3, TAIKO, 55)
            bat.nota(c * 4 + 3, 0.3, TOM_GRAVE, 60)
    return [koto, flauta, cue, bat]


def final():
    """Fanfarria de la pelea final: trompetas y trombones, timbales y cuerdas en trémolo."""
    trp, trb, cue, tim, bat = (Pista(0, 56, 'trompetas'), Pista(1, 57, 'trombones'), Pista(2, 44, 'tremolo'),
                               Pista(3, 47, 'timbales'), Pista(9, 0, 'platos'))
    prog = 'D Bb C D D G A D'
    acordes(cue, prog + ' ' + prog, fuerza=55)
    acordes(trb, prog + ' ' + prog, fuerza=72, octava=3)
    # Llamada de cuarta y quinta, en tresillos y con nota larga al final de cada frase.
    frase = ('A4/.333 A4/.333 A4/.333 D5/1.5 A4/.5 F#5/2 E5/.333 F#5/.333 G5/.333 A5/3 '
             'F5/1 D5/1 Bb4/2 C5/1 E5/1 G5/2 A5/.333 A5/.333 A5/.333 D6/1.5 C#6/.5 D6/4 ')
    trp.melodia(frase + frase.replace('/4', '/3 r/1'), fuerza=105)
    for c in range(16):
        raiz = altura(prog.split()[c % 8][0] + ('b' if 'b' in prog.split()[c % 8] else '') + '2')
        tim.nota(c * 4, .5, raiz, 95)
        for k in range(4):
            tim.nota(c * 4 + 3 + k * .25, .2, raiz + 7, 60 + k * 8)
        if c % 4 == 0:
            bat.nota(c * 4, 1, PLATO, 70)
    return [trp, trb, cue, tim, bat]


def rock():
    """Rock: guitarra distorsionada con quintas, bajo y batería rápida (mi menor)."""
    gui, baj, sol, bat = Pista(0, 30, 'guitarra'), Pista(1, 34, 'bajo'), Pista(2, 81, 'solo'), Pista(9, 0, 'bateria')
    riff = 'E5 E5 G5 A5 E5 E5 C5 D5'
    for vuelta in range(2):
        t0 = vuelta * 32
        for i, a in enumerate(riff.split()):
            raiz = altura(a[0] + '3')
            for k, d in enumerate([.5, .5, .25, .25, .5, 1, .5, .5]):
                t = t0 + i * 4 + sum([.5, .5, .25, .25, .5, 1, .5, .5][:k])
                gui.nota(t, d * .8, raiz, 95)
                gui.nota(t, d * .8, raiz + 7, 85)
        bajo(baj, riff, [(0, 0), (.5, 0), (1, 0), (1.5, 12), (2, 0), (2.5, 0), (3, 7), (3.5, 10)], inicio=t0)
        bateria(bat, 8, {BOMBO: [0, .75, 2, 2.5], CAJA: [1, 3], CHARLES: [0, .5, 1, 1.5, 2, 2.5, 3, 3.5]}, inicio=t0, fuerza=90)
        if vuelta:
            sol.melodia('B4/1 D5/.5 E5/1.5 G5/1 F#5/.5 E5/.5 D5/1 B4/2 r/2 E5/1 G5/.5 A5/1.5 B5/1 '
                        'A5/.5 G5/.5 E5/1 D5/2 r/2 B5/1 A5/1 G5/1 E5/1 D5/1 E5/1 G5/2 E5/4', inicio=t0, fuerza=92)
    return [gui, baj, sol, bat]


def combate():
    """Himno de combate ochentero: sintes de metal, bajo de sintetizador a corcheas, guitarra en quintas,
    caja con mucha pegada y un estribillo que sube (la mayor). Energía de película de acción de los 80, melodía propia."""
    lead, metal, gui, baj, pad, bat = (Pista(0, 81, 'sinte_solo'), Pista(1, 62, 'sinte_metal'), Pista(2, 30, 'guitarra'),
                                       Pista(3, 39, 'sinte_bajo'), Pista(4, 90, 'colchon'), Pista(9, 0, 'bateria'))
    estrofa, estribillo = 'A E F#m D A E F#m D', 'D E A F#m D E A A'
    t = 0
    for vuelta in range(2):
        for parte, prog in (('estrofa', estrofa), ('estribillo', estribillo)):
            fuerte = parte == 'estribillo'
            acordes(pad, prog, inicio=t, fuerza=50 if fuerte else 42)
            bajo(baj, prog, [(k * .5, 0 if k % 4 != 3 else 12) for k in range(8)], inicio=t, fuerza=100)
            # Guitarra: quintas a corcheas, apagadas en la estrofa y abiertas en el estribillo.
            for c, a in enumerate(prog.split()):
                raiz = altura(a[0] + ('#' if '#' in a else '') + '3')
                for k in range(8):
                    gui.nota(t + c * 4 + k * .5, .4 if fuerte else .22, raiz, 100 if fuerte else 70)
                    gui.nota(t + c * 4 + k * .5, .4 if fuerte else .22, raiz + 7, 92 if fuerte else 64)
                # Golpes de metal en el contratiempo, como en los 80.
                if not fuerte:
                    for tt in (1.5, 3.5):
                        for iv in (0, 3 if 'm' in a else 4, 7):
                            metal.nota(t + c * 4 + tt, .35, raiz + 12 + iv, 82)
            bateria(bat, 8, {BOMBO: [0, 1.5, 2, 3.5], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t, fuerza=118)
            bat.nota(t, 1.5, PLATO, 110)
            for k in range(8):   # redoble de toms al final de cada parte
                bat.nota(t + 30 + k * .25, .2, TOM_ALTO if k < 4 else TOM_GRAVE, 100 + k * 3)
            if fuerte:
                coro = ('F#5/1 A5/1 B5/1.5 A5/.5 G#5/1 B5/1 E6/2 C#6/1.5 B5/.5 A5/1 E5/1 F#5/2 A5/2 '
                        'F#5/1 A5/1 D6/1.5 C#6/.5 B5/1 C#6/.5 B5/.5 G#5/2 A5/1 C#6/1 E6/2 E6/4')
                lead.melodia(coro, inicio=t, fuerza=112)
                metal.melodia(coro, inicio=t, fuerza=88, transponer=-12)
            else:
                lead.melodia('r/2 E5/.5 F#5/.5 A5/1 B5/1 A5/.5 E5/.5 C#5/2 r/2 C#5/.5 E5/.5 F#5/1 A5/1 '
                             'F#5/.5 E5/.5 D5/3 r/2 E5/.5 F#5/.5 A5/1 B5/1 C#6/.5 B5/.5 A5/2 r/2 '
                             'A5/.5 B5/.5 C#6/1 E6/1 D6/1 C#6/1 B5/2', inicio=t, fuerza=92)
            t += 32
    return [lead, metal, gui, baj, pad, bat]


def artes_marciales():
    """Acción de artes marciales de los 80: bajo de sintetizador en semicorcheas, batería con pegada, taikos,
    cuerdas de sintetizador y una melodía pentatónica (re menor) que se dobla en el estribillo. Melodía propia."""
    lead, cue, baj, gong, bat = (Pista(0, 84, 'sinte_solo'), Pista(1, 50, 'cuerdas_sinte'), Pista(2, 38, 'sinte_bajo'),
                                 Pista(3, 116, 'taiko'), Pista(9, 0, 'bateria'))
    prog = 'Dm Dm Bb C Dm Dm F C'
    t = 0
    for vuelta in range(2):
        acordes(cue, prog, inicio=t, fuerza=52 + vuelta * 10)
        # Bajo en semicorcheas: la raíz con un salto de octava y una quinta, la secuencia típica de los 80.
        bajo(baj, prog, [(k * .25, (0, 0, 12, 0, 7, 0, 12, 10)[k % 8]) for k in range(16)], inicio=t, fuerza=96)
        bateria(bat, 8, {BOMBO: [0, .75, 2, 2.75], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t, fuerza=115)
        for c in range(8):
            gong.nota(t + c * 4, .5, 48, 100 if c % 2 == 0 else 80)
            if c % 4 == 3:
                for k in range(4):
                    gong.nota(t + c * 4 + 2 + k * .5, .3, 45, 90 + k * 8)
        bat.nota(t, 1.5, PLATO, 112)
        frase = ('D5/.5 F5/.5 G5/1 A5/1.5 C6/.5 A5/1 G5/.5 F5/.5 D5/2 r/1 C5/.5 D5/.5 F5/1 G5/1 A5/2 '
                 'C6/.5 D6/.5 C6/.5 A5/.5 G5/1 A5/1 F5/1.5 G5/.5 A5/2 D6/1 C6/1 A5/1 G5/1 A5/4')
        lead.melodia(frase, inicio=t, fuerza=100 + vuelta * 10)
        if vuelta:
            cue.melodia(frase, inicio=t, fuerza=70, transponer=-12)
        t += 32
    return [lead, cue, baj, gong, bat]


def drama():
    """Drama: piano en arpegios lentos, violonchelo y cuerdas (re menor)."""
    pia, che, cue = Pista(0, 0, 'piano'), Pista(1, 42, 'chelo'), Pista(2, 48, 'cuerdas')
    prog = 'Dm Bb F C Dm Gm A A'
    for c, a in enumerate((prog + ' ' + prog).split()):
        raiz = altura(a[0] + ('b' if 'b' in a else '') + '3')
        tri = [0, 3 if 'm' in a else 4, 7, 12, 7, 3 if 'm' in a else 4]
        for i in range(8):
            pia.nota(c * 4 + i * .5, .9, raiz + tri[i % 6], 58)
    acordes(cue, prog + ' ' + prog, fuerza=38)
    che.melodia('r/4 A3/3 G3/1 F3/2 A3/2 G3/4 F3/2 E3/1 D3/1 Bb3/3 A3/1 A3/4 C#4/4 '
                'D4/3 C4/1 Bb3/2 A3/2 G3/3 F3/1 E3/2 F3/1 G3/1 Bb3/2 A3/2 A3/8', fuerza=80)
    return [pia, che, cue]


def menu():
    """Menú: piano eléctrico, vibráfono melódico, bajo de sintetizador y batería suave (fa mayor), forma A-B."""
    epi, vib, pad, baj, bat = (Pista(0, 4, 'piano_electrico'), Pista(1, 11, 'vibrafono'), Pista(2, 89, 'colchon'),
                               Pista(3, 38, 'bajo'), Pista(9, 0, 'bateria'))
    prog_a = 'F Am Bb C F Dm Bb C'
    prog_b = 'Dm Am Bb F Gm Am Bb C'
    for vuelta, prog in enumerate((prog_a, prog_b)):
        t0 = vuelta * 32
        acordes(pad, prog, inicio=t0, fuerza=36, octava=4)
        for c, a in enumerate(prog.split()):
            raiz = altura(a[0] + ('b' if 'b' in a else '') + '4')
            for tt in (0, 1.5, 2.5):
                for iv in (0, 3 if 'm' in a else 4, 7):
                    epi.nota(t0 + c * 4 + tt, .75, raiz + iv, 56)
        bajo(baj, prog, [(0, 0), (1.5, 0), (2.5, 7), (3.5, 12)], inicio=t0, fuerza=68)
        bateria(bat, 8, {BOMBO: [0, 2.5], CAJA: [1, 3], CHARLES: [0, .5, 1, 1.5, 2, 2.5, 3, 3.5]}, inicio=t0, fuerza=54)
    vib.melodia('A5/1.5 G5/.5 F5/1 C5/1 E5/2 G5/2 F5/1.5 G5/.5 A5/1 C6/1 G5/3 r/1 '
                'A5/1.5 C6/.5 D6/1 C6/1 A5/2 F5/2 G5/1 A5/1 Bb5/1 A5/1 G5/3 r/1 '
                'F5/1.5 A5/.5 C6/1 A5/1 E5/2 G5/2 D5/1.5 F5/.5 A5/1 G5/1 F5/3 r/1 '
                'G5/1 Bb5/1 A5/1 G5/1 A5/1 C6/1 B5/1 A5/1 Bb5/1.5 A5/.5 G5/1 F5/1 G5/3 r/1', inicio=0, fuerza=72)
    return [epi, vib, pad, baj, bat]


# ---------------------------------------------------------------- las épicas de las peleas (bloque 6)
# Homenaje de género al boxeo y las artes marciales de los 70-80 y al rock de estadio: metales triunfales,
# crescendos, coros, guitarras y tambores que empujan. Melodías propias, nada copiado.

CLAP, CRASH2, BOMBO_GRANDE = 39, 57, 35
TIMBAL = 47


def redoble_timbal(p, inicio, dur, nota=38, desde=50, hasta=110):
    """Redoble de timbales que crece (el subidón antes del tema)."""
    n = int(dur * 8)
    for k in range(n):
        p.nota(inicio + k / 8, .12, nota, int(desde + (hasta - desde) * k / max(1, n - 1)))


def campeon():
    """El himno del campeón: timbales, fanfarria de metales que sube, cuerdas a corcheas y un coro en el estribillo.
    Primero en re mayor; la última vuelta, un tono más arriba (el subidón final)."""
    tro, tbn, cue, coro, baj, tim, bat = (Pista(0, 56, 'trompetas'), Pista(1, 61, 'metales'), Pista(2, 48, 'cuerdas'), Pista(3, 52, 'coro'),
                                          Pista(4, 33, 'bajo'), Pista(5, 47, 'timbales'), Pista(9, 0, 'bateria'))
    tema_a = ('D5/1.5 A4/.5 D5/1 E5/1 F#5/2 E5/1 D5/1 G5/1.5 F#5/.5 E5/1 D5/1 E5/3 r/1 '
              'F#5/1.5 E5/.5 F#5/1 A5/1 B5/2 A5/1 F#5/1 G5/1 F#5/1 E5/1 C#5/1 D5/4')
    tema_b = ('A5/2 B5/1 A5/1 G5/2 F#5/1 E5/1 F#5/1.5 G5/.5 A5/1 D6/1 D6/3 r/1 '
              'B5/2 C#6/1 D6/1 E6/2 D6/1 B5/1 A5/1 F#5/1 G5/1 E5/1 D5/4')
    prog_a, prog_b = 'D D G A D Bm A D', 'D Em D D Bm Em A D'
    # Intro: redoble y acordes de metal a golpes.
    redoble_timbal(tim, 0, 4, 38)
    for k, a in enumerate('D G D A'.split()):
        acordes(tbn, a, dur=1, inicio=4 + k * 2, fuerza=95, octava=4)
    t = 12
    for vuelta, subir in ((0, 0), (1, 0), (2, 2)):
        fuerte = vuelta > 0
        for prog, tema, voz in ((prog_a, tema_a, tro), (prog_b, tema_b, coro)):
            acordes(cue, prog, inicio=t, fuerza=55 + 10 * vuelta, octava=4)
            for c, a in enumerate(prog.split()):   # cuerdas a corcheas: el motor
                raiz = altura(a[0] + ('#' if '#' in a else '') + '4') + subir
                for k in range(8):
                    cue.nota(t + c * 4 + k * .5, .4, raiz + (7 if k % 2 else 0), 60 + 8 * vuelta)
            bajo(baj, prog, [(0, 0), (1.5, 0), (2, 7), (3, 12)], inicio=t, fuerza=95)
            voz.melodia(tema, inicio=t, fuerza=100 + 8 * vuelta, transponer=subir)
            if voz is coro:
                tro.melodia(tema, inicio=t, fuerza=85, transponer=subir - 12)
            if fuerte:
                bateria(bat, 8, {BOMBO: [0, 2], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t, fuerza=100)
                bat.nota(t, 2, PLATO, 110)
            for k in range(0, 32, 8):
                tim.nota(t + k, .5, 38 + subir, 100)
                tim.nota(t + k + 3.5, .25, 45 + subir, 90)
            t += 32
    redoble_timbal(tim, t, 2, 40, 70, 127)
    acordes(tbn, 'E', dur=6, inicio=t + 2, fuerza=120, octava=4)
    acordes(coro, 'E', dur=6, inicio=t + 2, fuerza=110, octava=4)
    bat.nota(t + 2, 4, PLATO, 127)
    return [tro, tbn, cue, coro, baj, tim, bat]


def estadio():
    """Rock de estadio: pisotón-pisotón-palmada de todo el público, coro que canta el tema, y luego entran la guitarra
    con quintas, el bajo y un solo. En mi menor, a medio tiempo."""
    coro, gui, solo, baj, pia, bat = (Pista(0, 52, 'coro'), Pista(1, 30, 'guitarra'), Pista(2, 29, 'solo'), Pista(3, 34, 'bajo'),
                                      Pista(4, 0, 'piano'), Pista(9, 0, 'bateria'))
    canto = ('E5/1 E5/1 G5/1 E5/1 D5/1 D5/1 B4/2 E5/1 G5/1 A5/1 B5/1 A5/1 G5/1 E5/2 '
             'G5/1 G5/1 A5/1 G5/1 F#5/1 E5/1 D5/2 E5/1 B4/1 D5/1 E5/1 E5/4')
    prog = 'Em C D Em G D C B'
    t = 0
    for parte in range(4):
        pisoton = {BOMBO_GRANDE: [0, .5, 2, 2.5], CLAP: [1, 3]}
        if parte >= 1:
            pisoton[CHARLES] = [k * .5 for k in range(8)]
        bateria(bat, 8, pisoton, inicio=t, fuerza=105 + 5 * parte)
        if parte == 0:
            coro.melodia(canto, inicio=t, fuerza=95)
        elif parte in (1, 3):
            coro.melodia(canto, inicio=t, fuerza=105 + 6 * parte)
            coro.melodia(canto, inicio=t, fuerza=85, transponer=-5)   # el público, a la cuarta
            for c, a in enumerate(prog.split()):
                raiz = altura(a[0] + ('#' if '#' in a else '') + '3')
                for tt in (0, 1.5, 2, 3):
                    gui.nota(t + c * 4 + tt, .45, raiz, 110)
                    gui.nota(t + c * 4 + tt, .45, raiz + 7, 100)
            bajo(baj, prog, [(0, 0), (1.5, 0), (2, 0), (3, 7)], inicio=t, fuerza=100)
            acordes(pia, prog, dur=4, inicio=t, fuerza=60)
            bat.nota(t, 2, PLATO, 115)
        else:
            # El solo de guitarra, por encima del pisotón: pentatónica que sube y sube.
            solo.melodia('E5/.5 G5/.5 A5/.5 B5/.5 D6/1 B5/1 A5/.5 B5/.5 A5/.5 G5/.5 E5/2 '
                         'G5/.5 A5/.5 B5/.5 D6/.5 E6/1 D6/1 B5/.5 D6/.5 B5/.5 A5/.5 B5/2 '
                         'E6/.5 D6/.5 B5/.5 A5/.5 G5/.5 A5/.5 B5/.5 D6/.5 E6/2 G6/2 '
                         'F#6/1 E6/1 D6/1 B5/1 E6/4', inicio=t, fuerza=118)
            bajo(baj, prog, [(0, 0), (2, 0)], inicio=t, fuerza=95)
        t += 32
    for k in range(4):
        bat.nota(t + k * .5, .3, BOMBO_GRANDE, 120)
    acordes(gui, 'E5', dur=4, inicio=t + 2, fuerza=120, octava=3)
    acordes(coro, 'E', dur=4, inicio=t + 2, fuerza=115, octava=4)
    bat.nota(t + 2, 4, PLATO, 127)
    return [coro, gui, solo, baj, pia, bat]


def subidon():
    """El montaje de entrenamiento: bajo de sinte a corcheas, cuerdas que no paran, golpes de metal y una melodía de
    trompas; cada vuelta sube un tono (subir escaleras, correr por la playa, levantar chatarra)."""
    tro, cue, met, baj, bat = (Pista(0, 60, 'trompas'), Pista(1, 48, 'cuerdas'), Pista(2, 61, 'metales'), Pista(3, 38, 'sinte_bajo'),
                               Pista(9, 0, 'bateria'))
    tema = ('C5/1 E5/.5 G5/1.5 C6/1 B5/1 G5/1 E5/1 F5/1.5 A5/.5 G5/2 r/2 E5/2 r/1 '
            'F5/1 A5/.5 C6/1.5 D6/1 C6/1 A5/1 F5/1 G5/1 B5/1 D6/1 C6/4 r/2')
    prog = 'C Am F G C Am F G'
    t = 0
    for vuelta in range(4):
        subir = 2 * vuelta
        for c, a in enumerate(prog.split()):
            raiz = altura(a[0] + '2') + subir
            for k in range(8):
                baj.nota(t + c * 4 + k * .5, .4, raiz + (12 if k % 2 else 0), 100)
            r4 = altura(a[0] + '4') + subir
            menor = 'm' in a
            for k in range(16):   # cuerdas a semicorcheas: el corazón a mil
                cue.nota(t + c * 4 + k * .25, .2, r4 + (0, 4 - menor, 7, 12)[k % 4], 55 + 8 * vuelta)
            for tt in (0, 2.5):
                for iv in (0, 4 - menor, 7):
                    met.nota(t + c * 4 + tt, .4, r4 + 12 + iv - 12, 90 + 6 * vuelta)
        if vuelta > 0:
            tro.melodia(tema, inicio=t, fuerza=95 + 8 * vuelta, transponer=subir)
        bateria(bat, 8, {BOMBO: [0, 1, 2, 3], CAJA: [1, 3, 3.75], CHARLES: [k * .5 for k in range(8)]}, inicio=t, fuerza=95 + 8 * vuelta)
        bat.nota(t, 2, PLATO, 100 + 7 * vuelta)
        t += 32
    acordes(met, 'D', dur=4, inicio=t, fuerza=120, octava=4)
    acordes(tro, 'D', dur=4, inicio=t, fuerza=115, octava=5)
    bat.nota(t, 4, PLATO, 127)
    return [tro, cue, met, baj, bat]


def ultimo_asalto():
    """El último asalto: empieza en re menor, despacio y con timbales; cada vuelta más fuerte (crescendo), entra el
    coro, los golpes de orquesta, y al final se abre a re mayor."""
    coro, met, cue, hit, baj, tim, bat = (Pista(0, 52, 'coro'), Pista(1, 61, 'metales'), Pista(2, 48, 'cuerdas'), Pista(3, 55, 'golpe'),
                                          Pista(4, 33, 'bajo'), Pista(5, 47, 'timbales'), Pista(9, 0, 'bateria'))
    tema = ('D5/1 F5/1 A5/2 G5/1 F5/1 E5/2 D5/1 E5/1 F5/1 G5/1 A5/3 r/1 '
            'Bb5/1 A5/1 G5/1 F5/1 E5/1.5 F5/.5 G5/2 A5/1 G5/1 F5/1 E5/1 D5/4')
    prog = 'Dm Dm Gm A Bb Gm A Dm'
    t = 0
    for vuelta in range(4):
        fuerza = 50 + 22 * vuelta
        acordes(cue, prog, inicio=t, fuerza=fuerza, octava=4)
        bajo(baj, prog, [(0, 0), (2, 0), (3, 7)], inicio=t, fuerza=fuerza + 10)
        for k in range(0, 32, 4):
            tim.nota(t + k, .4, 38, min(127, fuerza + 20))
            tim.nota(t + k + 2.5, .3, 38, min(127, fuerza + 5))
        if vuelta >= 1:
            met.melodia(tema, inicio=t, fuerza=min(127, fuerza + 20))
        if vuelta >= 2:
            coro.melodia(tema, inicio=t, fuerza=min(127, fuerza + 25), transponer=12 if vuelta == 3 else 0)
            bateria(bat, 8, {BOMBO: [0, 1.5, 2], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t, fuerza=min(127, fuerza + 15))
            for k in range(0, 32, 8):
                for iv in (0, 7, 12):
                    hit.nota(t + k, .5, 62 + iv, 115)
        t += 32
    # Se abre a mayor: la victoria.
    redoble_timbal(tim, t, 2, 38, 80, 127)
    for a, d in (('Bb', 2), ('C', 2), ('D', 6)):
        acordes(met, a, dur=d, inicio=t + 2, fuerza=124, octava=4)
        acordes(coro, a, dur=d, inicio=t + 2, fuerza=120, octava=4)
        t += d
    bat.nota(t - 4, 4, PLATO, 127)
    return [coro, met, cue, hit, baj, tim, bat]


# ---------------------------------------------------------------- música épica para cada modo (encargo música épica)
# Todas las piezas son 100 % originales: recursos de género aplicados a cada pantalla y fase del juego.

CENCERRO, CAMPANA_RIDE, CHARLES_ABIERTO = 56, 51, 46


def _raiz_acorde(a, octava, transponer=0):
    return altura(a[0] + (a[1] if len(a) > 1 and a[1] in '#b' else '') + str(octava)) + transponer


def arpegio(p, cifrado, patron, paso=0.25, dur=4, inicio=0.0, fuerza=72, octava=4, transponer=0):
    """Arpegio rítmico por acordes; patron usa índices 0=raíz, 1=tercera, 2=quinta, 3=octava."""
    t = inicio
    pasos = int(round(dur / paso))
    for a in cifrado.split():
        raiz = _raiz_acorde(a, octava, transponer)
        terc = 3 if 'm' in a[1:] else 4
        notas = (raiz, raiz + terc, raiz + 7, raiz + 12)
        for k in range(pasos):
            p.nota(t + k * paso, paso * 0.85, notas[patron[k % len(patron)]], fuerza)
        t += dur
    return t


def quintas(p, cifrado, tiempos, dur_nota=0.35, dur=4, inicio=0.0, fuerza=95, octava=3, transponer=0):
    """Quintas eléctricas o de metales sobre una progresión."""
    t = inicio
    for a in cifrado.split():
        raiz = _raiz_acorde(a, octava, transponer)
        for tt in tiempos:
            p.nota(t + tt, dur_nota, raiz, fuerza)
            p.nota(t + tt, dur_nota, raiz + 7, max(1, fuerza - 8))
        t += dur
    return t


def acordes_t(p, cifrado, dur=4, inicio=0.0, fuerza=60, octava=4, transponer=0):
    """Acordes tenidos con soporte de transposición en semitonos."""
    t = inicio
    for a in cifrado.split():
        raiz = _raiz_acorde(a, octava, transponer)
        menor = 'm' in a[1:]
        quinta = a.endswith('5')
        notas = [raiz, raiz + 7] if quinta else [raiz, raiz + (3 if menor else 4), raiz + 7]
        for n in notas:
            p.nota(t, dur * 0.96, n, fuerza)
        t += dur
    return t


def bajo_t(p, cifrado, figura, dur=4, inicio=0.0, fuerza=85, octava=2, transponer=0):
    """Bajo por acorde con transposición en semitonos."""
    t = inicio
    for a in cifrado.split():
        raiz = _raiz_acorde(a, octava, transponer)
        for tt, iv in figura:
            p.nota(t + tt, 0.42, raiz + iv, fuerza)
        t += dur
    return t


def menu_2():
    """Menú alternativo: sol mayor cálido, piano eléctrico, vibráfono, pad y pulso suave (100 BPM)."""
    epi, vib, pad, baj, bat = (Pista(0, 4, 'piano_electrico'), Pista(1, 11, 'vibrafono'), Pista(2, 89, 'colchon'),
                               Pista(3, 38, 'bajo'), Pista(9, 0, 'bateria'))
    prog_a = 'G Em C D G Bm C D'
    prog_b = 'Em C G D Am Em C D'
    for vuelta, prog in enumerate((prog_a, prog_b)):
        t0 = vuelta * 32
        acordes(pad, prog, inicio=t0, fuerza=34, octava=4)
        arpegio(epi, prog, (0, 1, 2, 3, 2, 1), paso=0.5, inicio=t0, fuerza=54, octava=4)
        bajo(baj, prog, [(0, 0), (1.5, 7), (2.5, 0), (3.5, 12)], inicio=t0, fuerza=66)
        bateria(bat, 8, {BOMBO: [0, 2.5], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=52)
    vib.melodia('B5/1.5 A5/.5 G5/1 D5/1 E5/2 G5/2 A5/1.5 B5/.5 C6/1 B5/1 A5/3 r/1 '
                'D6/1.5 B5/.5 G5/1 B5/1 F#5/2 A5/2 G5/1 A5/1 B5/1 A5/1 A5/3 r/1 '
                'G5/1.5 A5/.5 B5/1 G5/1 E5/2 G5/2 B5/1.5 A5/.5 G5/1 D5/1 F#5/3 r/1 '
                'E5/1 G5/1 A5/1 B5/1 G5/2 B5/2 C6/1.5 B5/.5 A5/1 G5/1 A5/3 r/1', inicio=0, fuerza=70)
    return [epi, vib, pad, baj, bat]


def carrera_turbo():
    """Carrera de obstáculos 1: arcade de carreras veloz en mi menor, bajo octavado y arpegios (148 BPM)."""
    lead, arp, met, baj, bat = (Pista(0, 81, 'sinte_solo'), Pista(1, 80, 'arpegio'), Pista(2, 62, 'sinte_metal'),
                                Pista(3, 39, 'sinte_bajo'), Pista(9, 0, 'bateria'))
    prog_a = 'Em C G D Em C D Em'
    prog_b = 'C D Em G C D Bm Em'
    tema_a = ('E5/.5 G5/.5 B5/1 E6/1 D6/.5 B5/.5 C6/1.5 B5/.5 A5/1 G5/1 B5/1.5 A5/.5 G5/1 E5/1 F#5/2 A5/2 '
              'E5/.5 G5/.5 B5/1 E6/1 G6/.5 F#6/.5 E6/1.5 D6/.5 C6/1 B5/1 A5/1 B5/1 D6/1 F#6/1 E6/3 r/1')
    tema_b = ('C6/1 G5/1 E6/1.5 D6/.5 D6/1 A5/1 F#6/1.5 E6/.5 E6/1 B5/1 G6/1.5 F#6/.5 G6/2 D6/2 '
              'C6/1 E6/1 G6/1 E6/1 F#6/1 D6/1 A5/1 D6/1 B5/1 D6/1 F#6/1 D6/1 E6/3 r/1')
    for idx, (prog, tema) in enumerate(((prog_a, tema_a), (prog_b, tema_b), (prog_a, tema_a))):
        t0 = idx * 32
        arpegio(arp, prog, (0, 1, 2, 3, 2, 1, 0, 2), paso=0.25, inicio=t0, fuerza=62 + idx * 5, octava=4)
        bajo(baj, prog, [(k * .5, 12 if k % 2 else 0) for k in range(8)], inicio=t0, fuerza=100)
        bateria(bat, 8, {BOMBO: [0, 1, 2, 3], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)], CLAP: [1, 3]}, inicio=t0, fuerza=105)
        bat.nota(t0, 1.0, PLATO, 110)
        lead.melodia(tema, inicio=t0, fuerza=104 if idx < 2 else 114)
        if idx >= 1:
            quintas(met, prog, (0, 1.5, 2.5), dur_nota=0.32, inicio=t0, fuerza=84, octava=4)
        for k in range(4):
            bat.nota(t0 + 31 + k * .25, .18, TOM_ALTO if k < 2 else TOM_GRAVE, 96 + k * 6)
    return [lead, arp, met, baj, bat]


def carrera_neon():
    """Carrera de obstáculos 2: neón veloz en la menor, sinte brillante y batería arcade (154 BPM)."""
    lead, arp, met, baj, bat = (Pista(0, 84, 'sinte_solo'), Pista(1, 80, 'arpegio'), Pista(2, 62, 'sinte_metal'),
                                Pista(3, 38, 'sinte_bajo'), Pista(9, 0, 'bateria'))
    prog_a = 'Am F C G Am F G Am'
    prog_b = 'F G Am C Dm F G E'
    tema_a = ('A5/.5 C6/.5 E6/1 A6/1 G6/.5 E6/.5 F6/1.5 E6/.5 D6/1 C6/1 E6/1.5 D6/.5 C6/1 G5/1 B5/2 D6/2 '
              'A5/.5 C6/.5 E6/1 G6/1 E6/.5 D6/.5 C6/1.5 D6/.5 E6/1 C6/1 B5/1 D6/1 G6/1 E6/1 A6/3 r/1')
    tema_b = ('F6/1 C6/1 A5/1 C6/1 G6/1 D6/1 B5/1 D6/1 A6/1.5 G6/.5 E6/1 C6/1 G6/2 E6/2 '
              'D6/1 F6/1 A6/1 F6/1 C6/1 F6/1 A6/1 G6/1 G6/1 D6/1 B5/1 G5/1 E6/3 r/1')
    for idx, (prog, tema) in enumerate(((prog_a, tema_a), (prog_b, tema_b), (prog_a, tema_a))):
        t0 = idx * 32
        arpegio(arp, prog, (0, 2, 1, 3, 2, 3, 1, 2), paso=0.25, inicio=t0, fuerza=64 + idx * 5, octava=4)
        bajo(baj, prog, [(k * .5, 12 if k % 2 else 0) for k in range(8)], inicio=t0, fuerza=102)
        bateria(bat, 8, {BOMBO: [0, 1, 2, 2.5, 3], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=106)
        bat.nota(t0, 1.0, PLATO, 112)
        lead.melodia(tema, inicio=t0, fuerza=106 if idx < 2 else 116)
        if idx >= 1:
            quintas(met, prog, (0, 2, 3.5), dur_nota=0.3, inicio=t0, fuerza=86, octava=4)
        for k in range(4):
            bat.nota(t0 + 31 + k * .25, .18, TOM_ALTO if k < 2 else TOM_GRAVE, 98 + k * 6)
    return [lead, arp, met, baj, bat]


def carrera_recta_1():
    """Recta final de carrera 1: sprint a 162 BPM en fa# menor que modula un tono arriba (+2 st)."""
    lead, met, arp, baj, bat = (Pista(0, 81, 'sinte_solo'), Pista(1, 62, 'sinte_metal'), Pista(2, 80, 'arpegio'),
                                Pista(3, 39, 'sinte_bajo'), Pista(9, 0, 'bateria'))
    prog = 'F#m D A E F#m D E F#m'
    tema = ('F#5/.5 A5/.5 C#6/1 F#6/1 E6/.5 C#6/.5 D6/1.5 C#6/.5 B5/1 A5/1 C#6/1.5 B5/.5 A5/1 E5/1 G#5/2 B5/2 '
            'F#5/.5 A5/.5 C#6/1 E6/1 C#6/.5 B5/.5 A5/1 B5/1 D6/1 F#6/1 E6/1 G#6/1 B6/1 G#6/1 F#6/3 r/1')
    for idx, subir in enumerate((0, 0, 2)):
        t0 = idx * 32
        arpegio(arp, prog, (0, 1, 2, 3), paso=0.25, inicio=t0, fuerza=70 + idx * 6, octava=4, transponer=subir)
        bajo_t(baj, prog, [(k * .5, 12 if k % 2 else 0) for k in range(8)], inicio=t0, fuerza=108, transponer=subir)
        quintas(met, prog, (0, 1.5, 2, 3.5), dur_nota=0.28, inicio=t0, fuerza=92 + idx * 6, octava=4, transponer=subir)
        bateria(bat, 8, {BOMBO: [0, .5, 1, 2, 2.5, 3], CAJA: [1, 3], CHARLES: [k * .25 for k in range(16)]}, inicio=t0, fuerza=112)
        bat.nota(t0, 1.0, PLATO, 118)
        lead.melodia(tema, inicio=t0, fuerza=112 + idx * 5, transponer=subir)
        if idx == 2:
            met.melodia(tema, inicio=t0, fuerza=90, transponer=subir - 12)
        for k in range(4):
            bat.nota(t0 + 31 + k * .25, .18, TOM_ALTO if k < 2 else TOM_GRAVE, 106 + k * 5)
    return [lead, met, arp, baj, bat]


def carrera_recta_2():
    """Recta final de carrera 2: sprint frenético a 166 BPM en si menor con modulación a do# menor."""
    lead, met, arp, baj, bat = (Pista(0, 84, 'sinte_solo'), Pista(1, 61, 'metales'), Pista(2, 80, 'arpegio'),
                                Pista(3, 38, 'sinte_bajo'), Pista(9, 0, 'bateria'))
    prog = 'Bm G D A Bm G A Bm'
    tema = ('B5/.5 D6/.5 F#6/1 B6/1 A6/.5 F#6/.5 G6/1.5 F#6/.5 E6/1 D6/1 F#6/1.5 E6/.5 D6/1 A5/1 C#6/2 E6/2 '
            'B5/.5 D6/.5 F#6/1 A6/1 F#6/.5 E6/.5 D6/1 E6/1 G6/1 B6/1 A6/1 C#7/1 E7/1 C#7/1 B6/3 r/1')
    for idx, subir in enumerate((0, 0, 2)):
        t0 = idx * 32
        arpegio(arp, prog, (0, 2, 1, 3), paso=0.25, inicio=t0, fuerza=72 + idx * 5, octava=4, transponer=subir)
        bajo_t(baj, prog, [(k * .5, 12 if k % 2 else 0) for k in range(8)], inicio=t0, fuerza=108, transponer=subir)
        quintas(met, prog, (0, 1.5, 3), dur_nota=0.3, inicio=t0, fuerza=94 + idx * 5, octava=4, transponer=subir)
        bateria(bat, 8, {BOMBO: [0, 1, 1.5, 2, 3, 3.5], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=114)
        bat.nota(t0, 1.0, PLATO, 120)
        lead.melodia(tema, inicio=t0, fuerza=110 + idx * 5, transponer=subir)
        for k in range(4):
            bat.nota(t0 + 31 + k * .25, .18, TOM_ALTO if k < 2 else TOM_GRAVE, 108 + k * 4)
    return [lead, met, arp, baj, bat]


def destruccion_pulso():
    """Destrucción y supervivencia 1: pulso grave industrial, golpes de orquesta y timbales (134 BPM, do menor)."""
    met, gui, cue, hit, baj, tim, bat = (Pista(0, 61, 'metales'), Pista(1, 30, 'guitarra'), Pista(2, 48, 'cuerdas'),
                                         Pista(3, 55, 'golpe'), Pista(4, 38, 'bajo'), Pista(5, 47, 'timbales'),
                                         Pista(9, 0, 'bateria'))
    prog = 'Cm Cm Ab G Cm Ab Bb G'
    tema = ('C5/1.5 Eb5/.5 G5/1 F#5/1 G5/2 C5/2 Ab5/1.5 G5/.5 F5/1 Eb5/1 D5/3 r/1 '
            'C5/1 G5/1 Bb5/1 Ab5/1 G5/1.5 F5/.5 Eb5/1 D5/1 Eb5/1 G5/1 F5/1 D5/1 C5/3 r/1')
    for vuelta in range(2):
        t0 = vuelta * 32
        acordes(cue, prog, inicio=t0, fuerza=56 + vuelta * 10, octava=4)
        quintas(gui, prog, (0, .5, 1.5, 2, 3), dur_nota=0.32, inicio=t0, fuerza=96 + vuelta * 8, octava=3)
        bajo(baj, prog, [(k * .5, 0 if k % 4 != 3 else 7) for k in range(8)], inicio=t0, fuerza=104)
        bateria(bat, 8, {BOMBO_GRANDE: [0, .75, 2, 2.75], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=112)
        for c in range(8):
            tim.nota(t0 + c * 4, .4, 36, 108)
            tim.nota(t0 + c * 4 + 2.5, .3, 43, 92)
            if c % 2 == 0:
                for iv in (0, 7, 12):
                    hit.nota(t0 + c * 4, .45, 48 + iv, 114)
        met.melodia(tema, inicio=t0, fuerza=102 + vuelta * 12)
        bat.nota(t0, 1.2, PLATO, 116)
    return [met, gui, cue, hit, baj, tim, bat]


def destruccion_asalto():
    """Destrucción y supervivencia 2: asalto pesado en re menor con cuerdas en ostinato y golpes (138 BPM)."""
    met, gui, cue, hit, baj, tim, bat = (Pista(0, 57, 'trombones'), Pista(1, 30, 'guitarra'), Pista(2, 44, 'tremolo'),
                                         Pista(3, 55, 'golpe'), Pista(4, 39, 'bajo'), Pista(5, 47, 'timbales'),
                                         Pista(9, 0, 'bateria'))
    prog = 'Dm Bb Gm A Dm F C A'
    tema = ('D5/1 F5/1 A5/1.5 Bb5/.5 A5/1 G5/1 F5/2 G5/1 Bb5/1 A5/1 G5/1 E5/3 r/1 '
            'D5/1 A5/1 C6/1 Bb5/1 A5/1.5 G5/.5 F5/1 E5/1 F5/1 A5/1 G5/1 E5/1 D5/3 r/1')
    for vuelta in range(2):
        t0 = vuelta * 32
        arpegio(cue, prog, (0, 2, 1, 2), paso=0.25, inicio=t0, fuerza=62 + vuelta * 8, octava=4)
        quintas(gui, prog, (0, 1, 1.5, 2.5, 3), dur_nota=0.3, inicio=t0, fuerza=98 + vuelta * 8, octava=3)
        bajo(baj, prog, [(0, 0), (.5, 0), (1.5, 7), (2, 0), (2.5, 0), (3.5, 10)], inicio=t0, fuerza=106)
        bateria(bat, 8, {BOMBO_GRANDE: [0, 1.5, 2, 3.25], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=114)
        for c in range(8):
            tim.nota(t0 + c * 4, .4, 38, 110)
            if c % 2 == 1:
                for iv in (0, 7, 12):
                    hit.nota(t0 + c * 4, .45, 50 + iv, 116)
        met.melodia(tema, inicio=t0, fuerza=104 + vuelta * 10)
        bat.nota(t0, 1.2, PLATO, 118)
    return [met, gui, cue, hit, baj, tim, bat]


def destruccion_cuenta_1():
    """Destrucción cuenta atrás 1: últimos segundos a 152 BPM en re menor, pulso frenético y urgencia."""
    met, sinte, hit, baj, tim, bat = (Pista(0, 61, 'metales'), Pista(1, 81, 'sinte_pulso'), Pista(2, 55, 'golpe'),
                                      Pista(3, 39, 'sinte_bajo'), Pista(4, 47, 'timbales'), Pista(9, 0, 'bateria'))
    prog = 'Dm Dm Bb C Dm Bb C A'
    tema = ('D5/.5 D5/.5 F5/.5 A5/.5 G5/1 F5/1 E5/1 F5/1 G5/1 A5/1 Bb5/1 A5/1 G5/1 F5/1 E5/2 A5/2 '
            'D6/.5 C6/.5 Bb5/.5 A5/.5 G5/1 F5/1 E5/1 G5/1 A5/1 C6/1 Bb5/1 A5/1 G5/1 E5/1 D5/3 r/1')
    for vuelta in range(2):
        t0 = vuelta * 32
        arpegio(sinte, prog, (0, 0, 2, 3), paso=0.25, inicio=t0, fuerza=76 + vuelta * 8, octava=4)
        bajo(baj, prog, [(k * .25, 0 if k % 2 == 0 else 12) for k in range(16)], inicio=t0, fuerza=106)
        bateria(bat, 8, {BOMBO: [0, 1, 2, 3], CAJA: [1, 3, 3.5, 3.75], CENCERRO: [0, 1, 2, 3],
                         CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=116)
        for c in range(8):
            tim.nota(t0 + c * 4, .35, 38, 114)
            for iv in (0, 7, 12):
                hit.nota(t0 + c * 4, .35, 50 + iv, 116)
        met.melodia(tema, inicio=t0, fuerza=112 + vuelta * 10)
    return [met, sinte, hit, baj, tim, bat]


def destruccion_cuenta_2():
    """Destrucción cuenta atrás 2: últimos segundos a 158 BPM en mi menor, tensión máxima."""
    met, sinte, hit, baj, tim, bat = (Pista(0, 56, 'trompetas'), Pista(1, 80, 'sinte_pulso'), Pista(2, 55, 'golpe'),
                                      Pista(3, 38, 'sinte_bajo'), Pista(4, 47, 'timbales'), Pista(9, 0, 'bateria'))
    prog = 'Em Em C D Em C D B'
    tema = ('E5/.5 E5/.5 G5/.5 B5/.5 A5/1 G5/1 F#5/1 G5/1 A5/1 B5/1 C6/1 B5/1 A5/1 G5/1 F#5/2 B5/2 '
            'E6/.5 D6/.5 C6/.5 B5/.5 A5/1 G5/1 F#5/1 A5/1 B5/1 D6/1 C6/1 B5/1 A5/1 F#5/1 E5/3 r/1')
    for vuelta in range(2):
        t0 = vuelta * 32
        arpegio(sinte, prog, (0, 2, 3, 2), paso=0.25, inicio=t0, fuerza=78 + vuelta * 8, octava=4)
        bajo(baj, prog, [(k * .25, 0 if k % 2 == 0 else 12) for k in range(16)], inicio=t0, fuerza=108)
        bateria(bat, 8, {BOMBO: [0, .5, 1, 2, 2.5, 3], CAJA: [1, 3, 3.75], CENCERRO: [0, .5, 2, 2.5],
                         CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=118)
        for c in range(8):
            tim.nota(t0 + c * 4, .35, 40, 116)
            for iv in (0, 7, 12):
                hit.nota(t0 + c * 4, .35, 52 + iv, 118)
        met.melodia(tema, inicio=t0, fuerza=114 + vuelta * 10)
    return [met, sinte, hit, baj, tim, bat]


def minijuegos_agil():
    """Minijuegos 1: deportiva, ligera y motivadora en sol mayor (126 BPM), marimba, metales y palmas."""
    mar, met, baj, bat = Pista(0, 12, 'marimba'), Pista(1, 61, 'metales'), Pista(2, 33, 'bajo'), Pista(9, 0, 'bateria')
    prog = 'G F C G G F C D'
    tema_a = ('G5/.5 A5/.5 B5/1 D6/1 B5/1 F5/1 A5/1 C6/1 A5/1 G5/.5 A5/.5 B5/1 G5/2 r/2 '
              'G5/.5 A5/.5 B5/1 D6/1 B5/1 A5/1 C6/1 E6/1 C6/1 D6/1.5 C6/.5 B5/1 A5/1 G5/3 r/1')
    tema_b = ('D6/1 B5/1 G5/1 B5/1 C6/1 A5/1 F5/1 A5/1 G5/1 C6/1 E6/1 C6/1 D6/3 r/1 '
              'B5/1 D6/1 G6/1 D6/1 C6/1 A5/1 F5/1 A5/1 B5/1 C6/1 D6/1 E6/1 D6/3 r/1')
    for idx, tema in enumerate((tema_a, tema_b)):
        t0 = idx * 32
        arpegio(mar, prog, (0, 1, 2, 3), paso=0.5, inicio=t0, fuerza=62, octava=4)
        bajo(baj, prog, [(0, 0), (1, 7), (1.5, 12), (2.5, 0), (3, 7)], inicio=t0, fuerza=86)
        bateria(bat, 8, {BOMBO: [0, 2, 2.5], CAJA: [1, 3], CLAP: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=82)
        met.melodia(tema, inicio=t0, fuerza=94 if idx == 0 else 102)
    return [mar, met, baj, bat]


def minijuegos_pista():
    """Minijuegos 2: pista de práctica alegre en do mayor (130 BPM), sinte ligero, vibráfono y bajo saltarín."""
    vib, lead, baj, bat = Pista(0, 11, 'vibrafono'), Pista(1, 81, 'sinte_solo'), Pista(2, 34, 'bajo'), Pista(9, 0, 'bateria')
    prog = 'C Bb F G C Bb F G'
    tema_a = ('C5/.5 E5/.5 G5/1 C6/1 G5/1 Bb5/1 F5/1 D5/1 F5/1 A5/1 C6/1 A5/1 G5/3 r/1 '
              'E5/.5 G5/.5 C6/1 E6/1 C6/1 D6/1 Bb5/1 F5/1 A5/1 C6/1 B5/1 A5/1 G5/3 r/1')
    tema_b = ('G5/1 C6/1 E6/1 C6/1 F6/1 D6/1 Bb5/1 D6/1 C6/1 A5/1 F5/1 A5/1 G5/3 r/1 '
              'E6/1 D6/1 C6/1 G5/1 F5/1 Bb5/1 D6/1 Bb5/1 A5/1 C6/1 D6/1 B5/1 C6/3 r/1')
    for idx, tema in enumerate((tema_a, tema_b)):
        t0 = idx * 32
        arpegio(vib, prog, (0, 2, 1, 3), paso=0.5, inicio=t0, fuerza=66, octava=4)
        bajo(baj, prog, [(0, 0), (1, 7), (2, 0), (2.5, 12), (3.5, 7)], inicio=t0, fuerza=88)
        bateria(bat, 8, {BOMBO: [0, 1.5, 2], CAJA: [1, 3], CLAP: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=84)
        lead.melodia(tema, inicio=t0, fuerza=92 + idx * 8)
    return [vib, lead, baj, bat]


def jefe_coloso():
    """Jefe 1: tema del rival temible en re menor (118 BPM), trombones, coro grave, trémolo y timbales."""
    trb, coro, cue, hit, baj, tim, bat = (Pista(0, 57, 'trombones'), Pista(1, 52, 'coro'), Pista(2, 44, 'tremolo'),
                                          Pista(3, 55, 'golpe'), Pista(4, 33, 'bajo'), Pista(5, 47, 'timbales'),
                                          Pista(9, 0, 'bateria'))
    prog = 'Dm Dm Eb Dm Gm Bb A Dm'
    tema = ('D4/1.5 F4/.5 E4/1 D4/1 Eb4/2 D4/2 G4/1.5 F4/.5 E4/1 D4/1 C#4/3 r/1 '
            'A4/1.5 Bb4/.5 A4/1 G4/1 F4/2 D4/2 Eb4/1 D4/1 C#4/1 E4/1 D4/3 r/1')
    for vuelta in range(2):
        t0 = vuelta * 32
        acordes(cue, prog, inicio=t0, fuerza=58 + vuelta * 12, octava=4)
        # Pedal grave en D2 que sostiene la amenaza del coloso.
        for c in range(8):
            baj.nota(t0 + c * 4, 1.8, 38, 108)
            baj.nota(t0 + c * 4 + 2, 1.8, 38, 102)
            tim.nota(t0 + c * 4, .45, 38, 114)
            tim.nota(t0 + c * 4 + 1.5, .3, 38, 96)
            tim.nota(t0 + c * 4 + 3, .35, 45, 102)
            if c in (0, 2, 4, 6):
                for iv in (0, 7, 12):
                    hit.nota(t0 + c * 4, .5, 50 + iv, 116)
        bateria(bat, 8, {BOMBO_GRANDE: [0, 1.5, 2], CAJA: [1, 3], TOM_GRAVE: [2.5, 3.5]}, inicio=t0, fuerza=110)
        trb.melodia(tema, inicio=t0, fuerza=108 + vuelta * 10)
        coro.melodia(tema, inicio=t0, fuerza=86 + vuelta * 16, transponer=12)
        bat.nota(t0, 1.5, PLATO, 118)
    return [trb, coro, cue, hit, baj, tim, bat]


def jefe_amenaza():
    """Jefe 2: coloso oscuro en do menor (124 BPM), trompas, coro, golpes de orquesta y bajo pesado."""
    trp, coro, cue, hit, baj, tim, bat = (Pista(0, 60, 'trompas'), Pista(1, 52, 'coro'), Pista(2, 48, 'cuerdas'),
                                          Pista(3, 55, 'golpe'), Pista(4, 34, 'bajo'), Pista(5, 47, 'timbales'),
                                          Pista(9, 0, 'bateria'))
    prog = 'Cm Ab Fm G Cm Db Ab G'
    tema = ('C5/1.5 Eb5/.5 D5/1 C5/1 Ab4/2 G4/2 F4/1.5 Ab4/.5 G4/1 F4/1 G4/3 r/1 '
            'C5/1.5 Db5/.5 C5/1 Bb4/1 Ab4/2 G4/2 F4/1 Eb4/1 D4/1 B4/1 C5/3 r/1')
    for vuelta in range(2):
        t0 = vuelta * 32
        acordes(cue, prog, inicio=t0, fuerza=60 + vuelta * 12, octava=4)
        bajo(baj, prog, [(0, 0), (.5, 0), (2, 0), (3, 7), (3.5, 6)], inicio=t0, fuerza=108)
        bateria(bat, 8, {BOMBO_GRANDE: [0, .5, 2, 2.5], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=112)
        for c in range(8):
            tim.nota(t0 + c * 4, .45, 36, 112)
            tim.nota(t0 + c * 4 + 2.5, .3, 43, 98)
            if c % 2 == 0:
                for iv in (0, 7, 12):
                    hit.nota(t0 + c * 4, .5, 48 + iv, 115)
        trp.melodia(tema, inicio=t0, fuerza=106 + vuelta * 10)
        if vuelta == 1:
            coro.melodia(tema, inicio=t0, fuerza=102)
        bat.nota(t0, 1.5, PLATO, 118)
    return [trp, coro, cue, hit, baj, tim, bat]


def senior_maquina():
    """Pelea Senior 1: duelo técnico de alta precisión en fa# menor (136 BPM), bajo sinte y cuerdas rápidas."""
    lead, cue, gui, baj, bat = (Pista(0, 61, 'metales'), Pista(1, 48, 'cuerdas'), Pista(2, 30, 'guitarra'),
                                Pista(3, 39, 'sinte_bajo'), Pista(9, 0, 'bateria'))
    prog = 'F#m F#m D E F#m Bm C# F#m'
    tema = ('F#5/1 A5/.5 G#5/.5 F#5/1 C#5/1 D5/1.5 E5/.5 F#5/2 G#5/1 A5/1 B5/1 A5/1 G#5/3 r/1 '
            'F#5/1 C#6/1 B5/1 A5/1 B5/1.5 A5/.5 G#5/1 F#5/1 G#5/1 A5/1 G#5/1 F#5/1 F#5/3 r/1')
    for vuelta in range(2):
        t0 = vuelta * 32
        arpegio(cue, prog, (0, 1, 2, 1, 3, 2, 1, 2), paso=0.25, inicio=t0, fuerza=64 + vuelta * 8, octava=4)
        quintas(gui, prog, (0, .75, 1.5, 2, 3), dur_nota=0.28, inicio=t0, fuerza=94 + vuelta * 8, octava=3)
        bajo(baj, prog, [(k * .25, (0, 0, 12, 0, 7, 0, 10, 0)[k % 8]) for k in range(16)], inicio=t0, fuerza=102)
        bateria(bat, 8, {BOMBO: [0, .75, 2, 2.75], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=110)
        lead.melodia(tema, inicio=t0, fuerza=100 + vuelta * 12)
        bat.nota(t0, 1.0, PLATO, 112)
    return [lead, cue, gui, baj, bat]


def senior_precision():
    """Pelea Senior 2: presión implacable en mi menor (140 BPM), ostinato mecánico y metales oscuros."""
    met, cue, gui, baj, bat = (Pista(0, 62, 'sinte_metal'), Pista(1, 44, 'tremolo'), Pista(2, 30, 'guitarra'),
                               Pista(3, 38, 'sinte_bajo'), Pista(9, 0, 'bateria'))
    prog = 'Em C Am B Em G C B'
    tema = ('E5/1 G5/.5 F#5/.5 E5/1 B4/1 C5/1.5 D5/.5 E5/2 F#5/1 G5/1 A5/1 G5/1 F#5/3 r/1 '
            'E5/1 B5/1 A5/1 G5/1 A5/1.5 G5/.5 F#5/1 E5/1 G5/1 F#5/1 E5/1 D#5/1 E5/3 r/1')
    for vuelta in range(2):
        t0 = vuelta * 32
        arpegio(cue, prog, (0, 2, 1, 3), paso=0.25, inicio=t0, fuerza=62 + vuelta * 8, octava=4)
        quintas(gui, prog, (0, .5, 1.5, 2.5, 3), dur_nota=0.28, inicio=t0, fuerza=96 + vuelta * 8, octava=3)
        bajo(baj, prog, [(k * .5, 0 if k % 2 == 0 else 12) for k in range(8)], inicio=t0, fuerza=104)
        bateria(bat, 8, {BOMBO: [0, 1, 1.5, 2, 3], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=112)
        met.melodia(tema, inicio=t0, fuerza=102 + vuelta * 10)
        bat.nota(t0, 1.0, PLATO, 114)
    return [met, cue, gui, baj, bat]


def campana_creed_1():
    """Campaña estilo Creed 1: legado y superación en la menor → do mayor (112 BPM), crece de íntimo a estadio."""
    pia, trp, cue, coro, baj, tim, bat = (Pista(0, 0, 'piano'), Pista(1, 60, 'trompas'), Pista(2, 48, 'cuerdas'),
                                          Pista(3, 52, 'coro'), Pista(4, 33, 'bajo'), Pista(5, 47, 'timbales'),
                                          Pista(9, 0, 'bateria'))
    prog_a = 'Am F C G Am F Dm E'
    prog_b = 'F G Am C F G C C'
    tema_a = ('A4/1.5 C5/.5 E5/1 A5/1 G5/2 E5/2 F5/1.5 E5/.5 D5/1 C5/1 D5/3 r/1 '
              'A4/1.5 C5/.5 E5/1 G5/1 F5/2 D5/2 E5/1 F5/1 G5/1 A5/1 E5/3 r/1')
    tema_b = ('F5/1 A5/1 C6/1.5 B5/.5 G5/1 B5/1 D6/2 E6/1.5 D6/.5 C6/1 A5/1 G5/3 r/1 '
              'F5/1 A5/1 C6/1 A5/1 G5/1 B5/1 D6/1 B5/1 C6/2 E6/2 C6/3 r/1')
    for idx, (prog, tema) in enumerate(((prog_a, tema_a), (prog_b, tema_b), (prog_b, tema_b))):
        t0 = idx * 32
        arpegio(pia, prog, (0, 1, 2, 3, 2, 1), paso=0.5, inicio=t0, fuerza=62 + idx * 6, octava=4)
        acordes(cue, prog, inicio=t0, fuerza=52 + idx * 14, octava=4)
        bajo(baj, prog, [(0, 0), (1.5, 0), (2, 7), (3, 12)], inicio=t0, fuerza=84 + idx * 10)
        trp.melodia(tema, inicio=t0, fuerza=94 + idx * 10)
        if idx >= 1:
            bateria(bat, 8, {BOMBO: [0, 2, 2.5], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=96 + idx * 8)
            for c in range(0, 8, 2):
                tim.nota(t0 + c * 4, .45, 45, 98 + idx * 8)
            bat.nota(t0, 1.5, PLATO, 108)
        if idx == 2:
            coro.melodia(tema, inicio=t0, fuerza=106)
    return [pia, trp, cue, coro, baj, tim, bat]


def campana_creed_2():
    """Campaña estilo Creed 2: determinación moderna en re menor → fa mayor (120 BPM), metales, cuerdas y coro."""
    trp, met, cue, coro, baj, tim, bat = (Pista(0, 56, 'trompetas'), Pista(1, 61, 'metales'), Pista(2, 48, 'cuerdas'),
                                          Pista(3, 52, 'coro'), Pista(4, 38, 'bajo'), Pista(5, 47, 'timbales'),
                                          Pista(9, 0, 'bateria'))
    prog_a = 'Dm Bb F C Dm Bb Gm A'
    prog_b = 'Bb C Dm F Bb C F F'
    tema_a = ('D5/1.5 F5/.5 A5/1 D6/1 C6/2 A5/2 Bb5/1.5 A5/.5 G5/1 F5/1 G5/3 r/1 '
              'D5/1.5 F5/.5 A5/1 C6/1 Bb5/2 G5/2 A5/1 Bb5/1 C6/1 D6/1 A5/3 r/1')
    tema_b = ('Bb5/1 D6/1 F6/1.5 E6/.5 C6/1 E6/1 G6/2 A6/1.5 G6/.5 F6/1 D6/1 C6/3 r/1 '
              'Bb5/1 D6/1 F6/1 D6/1 C6/1 E6/1 G6/1 E6/1 F6/2 A6/2 F6/3 r/1')
    for idx, (prog, tema) in enumerate(((prog_a, tema_a), (prog_b, tema_b), (prog_b, tema_b))):
        t0 = idx * 32
        arpegio(cue, prog, (0, 1, 2, 3), paso=0.5, inicio=t0, fuerza=58 + idx * 10, octava=4)
        bajo(baj, prog, [(k * .5, 0 if k % 4 != 3 else 12) for k in range(8)], inicio=t0, fuerza=92 + idx * 6)
        trp.melodia(tema, inicio=t0, fuerza=98 + idx * 8)
        bateria(bat, 8, {BOMBO: [0, 1.5, 2], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=94 + idx * 10)
        for c in range(0, 8, 2):
            tim.nota(t0 + c * 4, .45, 38, 100 + idx * 8)
        if idx >= 1:
            quintas(met, prog, (0, 2.5), dur_nota=0.4, inicio=t0, fuerza=88 + idx * 8, octava=4)
            bat.nota(t0, 1.5, PLATO, 112)
        if idx == 2:
            coro.melodia(tema, inicio=t0, fuerza=108)
    return [trp, met, cue, coro, baj, tim, bat]


def campana_isla_1():
    """Campaña Isla Clandestina 1: torneo eléctrico en mi menor (128 BPM), koto, flauta, taiko y guitarra."""
    flauta, koto, gui, baj, taiko, bat = (Pista(0, 77, 'flauta'), Pista(1, 107, 'koto'), Pista(2, 30, 'guitarra'),
                                          Pista(3, 38, 'sinte_bajo'), Pista(4, 116, 'taiko'), Pista(9, 0, 'bateria'))
    prog = 'Em Em C D Em G D Em'
    tema = ('E5/1 G5/1 A5/1.5 B5/.5 A5/1 G5/1 E5/2 D5/1 E5/1 G5/1 A5/1 B5/3 r/1 '
            'E6/1 D6/1 B5/1 A5/1 G5/1.5 A5/.5 B5/1 G5/1 A5/1 G5/1 E5/1 D5/1 E5/3 r/1')
    for vuelta in range(2):
        t0 = vuelta * 32
        arpegio(koto, prog, (0, 2, 1, 3, 2, 1), paso=0.5, inicio=t0, fuerza=72, octava=4)
        bajo(baj, prog, [(k * .5, 0 if k % 2 == 0 else 7) for k in range(8)], inicio=t0, fuerza=96)
        bateria(bat, 8, {BOMBO: [0, 1.5, 2], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=102)
        for c in range(8):
            taiko.nota(t0 + c * 4, .45, 45, 108)
            taiko.nota(t0 + c * 4 + 2.5, .35, 48, 90)
        flauta.melodia(tema, inicio=t0, fuerza=98 + vuelta * 10)
        if vuelta == 1:
            quintas(gui, prog, (0, 1.5, 2, 3), dur_nota=0.35, inicio=t0, fuerza=94, octava=3)
            bat.nota(t0, 1.2, PLATO, 112)
    return [flauta, koto, gui, baj, taiko, bat]


def campana_isla_2():
    """Campaña Isla Clandestina 2: duelo en la isla en re menor (134 BPM), koto rápido, taiko y metales."""
    met, koto, gui, baj, taiko, bat = (Pista(0, 61, 'metales'), Pista(1, 107, 'koto'), Pista(2, 30, 'guitarra'),
                                       Pista(3, 39, 'sinte_bajo'), Pista(4, 116, 'taiko'), Pista(9, 0, 'bateria'))
    prog = 'Dm Dm Bb C Dm F C Dm'
    tema = ('D5/.5 F5/.5 A5/1 C6/1 A5/1 G5/1 F5/1 D5/2 C5/1 D5/1 F5/1 G5/1 A5/3 r/1 '
            'D6/1 C6/1 A5/1 G5/1 F5/1.5 G5/.5 A5/1 F5/1 G5/1 F5/1 D5/1 C5/1 D5/3 r/1')
    for vuelta in range(2):
        t0 = vuelta * 32
        arpegio(koto, prog, (0, 2, 3, 2, 1, 2), paso=0.25, inicio=t0, fuerza=68 + vuelta * 6, octava=4)
        quintas(gui, prog, (0, 1, 2, 3), dur_nota=0.32, inicio=t0, fuerza=92 + vuelta * 8, octava=3)
        bajo(baj, prog, [(k * .5, 0 if k % 4 != 3 else 12) for k in range(8)], inicio=t0, fuerza=100)
        bateria(bat, 8, {BOMBO: [0, .75, 2, 2.75], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=106)
        for c in range(8):
            taiko.nota(t0 + c * 4, .45, 43, 110)
            if c % 2 == 1:
                taiko.nota(t0 + c * 4 + 3, .3, 45, 96)
        met.melodia(tema, inicio=t0, fuerza=100 + vuelta * 10)
        bat.nota(t0, 1.2, PLATO, 114)
    return [met, koto, gui, baj, taiko, bat]


def escena_hospital_1():
    """Escena hospital 1: emotiva y tensa en re menor (72 BPM), piano, violonchelo y suave pitido de monitor."""
    pia, che, cue, mon = Pista(0, 0, 'piano'), Pista(1, 42, 'chelo'), Pista(2, 48, 'cuerdas'), Pista(3, 8, 'monitor')
    prog = 'Dm Bb Gm A'
    arpegio(pia, prog, (0, 1, 2, 3), paso=0.5, inicio=0, fuerza=52, octava=3)
    acordes(cue, prog, inicio=0, fuerza=38, octava=4)
    che.melodia('A3/2 F3/1 G3/1 Bb3/2 A3/2 G3/1.5 F3/.5 E3/1 D3/1 E3/3 r/1', inicio=0, fuerza=78)
    for k in range(8):
        mon.nota(k * 2, 0.15, altura('A5'), 46)
    return [pia, che, cue, mon]


def escena_hospital_2():
    """Escena hospital 2: calma melancólica en la menor (76 BPM), piano, violonchelo y monitor suave."""
    pia, che, cue, mon = Pista(0, 0, 'piano'), Pista(1, 42, 'chelo'), Pista(2, 48, 'cuerdas'), Pista(3, 8, 'monitor')
    prog = 'Am F Dm E'
    arpegio(pia, prog, (0, 2, 1, 3), paso=0.5, inicio=0, fuerza=50, octava=3)
    acordes(cue, prog, inicio=0, fuerza=36, octava=4)
    che.melodia('E4/2 C4/1 D4/1 F4/2 E4/2 D4/1.5 C4/.5 B3/1 A3/1 B3/3 r/1', inicio=0, fuerza=76)
    for k in range(8):
        mon.nota(k * 2, 0.15, altura('E5'), 44)
    return [pia, che, cue, mon]


def escena_milagro_1():
    """Escena reaparición milagrosa 1: destello triunfal en re mayor (120 BPM, sin bucle)."""
    trp, coro, cam, cue, tim, bat = (Pista(0, 56, 'trompetas'), Pista(1, 52, 'coro'), Pista(2, 14, 'campanas'),
                                     Pista(3, 48, 'cuerdas'), Pista(4, 47, 'timbales'), Pista(9, 0, 'platos'))
    redoble_timbal(tim, 0, 2, 38, 55, 118)
    cam.melodia('D5/.5 F#5/.5 A5/.5 D6/.5 F#6/2 A6/2', inicio=0, fuerza=92)
    acordes(cue, 'Bb C D D', dur=3, inicio=4, fuerza=72, octava=4)
    acordes(coro, 'Bb C D', dur=3, inicio=4, fuerza=105, octava=4)
    trp.melodia('r/4 Bb5/1.5 A5/.5 G5/1 Bb5/1 C6/1.5 B5/.5 A5/1 C6/1 D6/2 F#6/2 D6/3.5', inicio=0, fuerza=114)
    tim.nota(4, .4, 34, 108)
    tim.nota(7, .4, 36, 112)
    tim.nota(10, .8, 38, 122)
    bat.nota(10, 3.0, PLATO, 120)
    return [trp, coro, cam, cue, tim, bat]


def escena_milagro_2():
    """Escena reaparición milagrosa 2: entrada luminosa en do mayor (126 BPM, sin bucle)."""
    trp, coro, cam, cue, tim, bat = (Pista(0, 56, 'trompetas'), Pista(1, 52, 'coro'), Pista(2, 14, 'campanas'),
                                     Pista(3, 48, 'cuerdas'), Pista(4, 47, 'timbales'), Pista(9, 0, 'platos'))
    redoble_timbal(tim, 0, 2, 36, 55, 118)
    cam.melodia('C5/.5 E5/.5 G5/.5 C6/.5 E6/2 G6/2', inicio=0, fuerza=92)
    acordes(cue, 'Ab Bb C C', dur=3, inicio=4, fuerza=72, octava=4)
    acordes(coro, 'Ab Bb C', dur=3, inicio=4, fuerza=106, octava=4)
    trp.melodia('r/4 Ab5/1.5 G5/.5 F5/1 Ab5/1 Bb5/1.5 A5/.5 G5/1 Bb5/1 C6/2 E6/2 C6/3.5', inicio=0, fuerza=114)
    tim.nota(4, .4, 32, 108)
    tim.nota(7, .4, 34, 112)
    tim.nota(10, .8, 36, 122)
    bat.nota(10, 3.0, PLATO, 120)
    return [trp, coro, cam, cue, tim, bat]


def escena_victoria_1():
    """Escena cierre de victoria 1: celebración narrativa en sol mayor (116 BPM, sin bucle)."""
    trp, trm, cue, coro, tim, bat = (Pista(0, 56, 'trompetas'), Pista(1, 60, 'trompas'), Pista(2, 48, 'cuerdas'),
                                     Pista(3, 52, 'coro'), Pista(4, 47, 'timbales'), Pista(9, 0, 'platos'))
    acordes(cue, 'Eb F G G', dur=4, inicio=0, fuerza=68, octava=4)
    acordes(coro, 'Eb F G', dur=4, inicio=0, fuerza=96, octava=4)
    trp.melodia('G5/1 Bb5/1 Eb6/2 F5/1 A5/1 C6/2 B5/1.5 A5/.5 G5/1 B5/1 D6/2 G6/1.8', inicio=0, fuerza=108)
    trm.melodia('Eb5/2 Bb4/2 F5/2 C5/2 G5/2 D5/2 G5/3.8', inicio=0, fuerza=88)
    tim.nota(0, .5, 39, 100)
    tim.nota(4, .5, 41, 106)
    tim.nota(8, .8, 43, 118)
    bat.nota(8, 3.5, PLATO, 116)
    return [trp, trm, cue, coro, tim, bat]


def escena_victoria_2():
    """Escena cierre de victoria 2: broche heroico en fa mayor (122 BPM, sin bucle)."""
    trp, trm, cue, coro, tim, bat = (Pista(0, 56, 'trompetas'), Pista(1, 60, 'trompas'), Pista(2, 48, 'cuerdas'),
                                     Pista(3, 52, 'coro'), Pista(4, 47, 'timbales'), Pista(9, 0, 'platos'))
    acordes(cue, 'Db Eb F F', dur=4, inicio=0, fuerza=68, octava=4)
    acordes(coro, 'Db Eb F', dur=4, inicio=0, fuerza=96, octava=4)
    trp.melodia('F5/1 Ab5/1 Db6/2 Eb5/1 G5/1 Bb5/2 A5/1.5 G5/.5 F5/1 A5/1 C6/2 F6/1.8', inicio=0, fuerza=108)
    trm.melodia('Db5/2 Ab4/2 Eb5/2 Bb4/2 F5/2 C5/2 F5/3.8', inicio=0, fuerza=88)
    tim.nota(0, .5, 37, 100)
    tim.nota(4, .5, 39, 106)
    tim.nota(8, .8, 41, 118)
    bat.nota(8, 3.5, PLATO, 116)
    return [trp, trm, cue, coro, tim, bat]


def escena_derrota_1():
    """Escena cierre de derrota 1: reflexiva y seria en mi menor (74 BPM, sin bucle)."""
    che, trm, pia, cue = Pista(0, 42, 'chelo'), Pista(1, 60, 'trompas'), Pista(2, 0, 'piano'), Pista(3, 48, 'cuerdas')
    acordes(cue, 'Em C B Em', dur=3, inicio=0, fuerza=44, octava=4)
    arpegio(pia, 'Em C B', (0, 1, 2, 1), paso=0.5, dur=3, inicio=0, fuerza=48, octava=3)
    acordes(pia, 'Em', dur=2.8, inicio=9, fuerza=52, octava=3)
    che.melodia('B3/1.5 A3/.5 G3/1 E3/1.5 F#3/.5 G3/1 F#3/2 D#3/1 E3/2.8', inicio=0, fuerza=80)
    trm.melodia('r/3 G4/2 F#4/1 F#4/2 B4/1 E4/2.8', inicio=0, fuerza=68)
    return [che, trm, pia, cue]


def escena_derrota_2():
    """Escena cierre de derrota 2: caída contenida en re menor (78 BPM, sin bucle)."""
    che, trm, pia, cue = Pista(0, 42, 'chelo'), Pista(1, 60, 'trompas'), Pista(2, 0, 'piano'), Pista(3, 48, 'cuerdas')
    acordes(cue, 'Dm Bb A Dm', dur=3, inicio=0, fuerza=44, octava=4)
    arpegio(pia, 'Dm Bb A', (0, 1, 2, 1), paso=0.5, dur=3, inicio=0, fuerza=48, octava=3)
    acordes(pia, 'Dm', dur=2.8, inicio=9, fuerza=52, octava=3)
    che.melodia('A3/1.5 G3/.5 F3/1 D3/1.5 E3/.5 F3/1 E3/2 C#3/1 D3/2.8', inicio=0, fuerza=80)
    trm.melodia('r/3 F4/2 E4/1 E4/2 A4/1 D4/2.8', inicio=0, fuerza=68)
    return [che, trm, pia, cue]


def tienda_bazar():
    """Tienda 1: tranquila pero con ritmo agradable en do mayor (102 BPM), piano eléctrico y vibráfono."""
    epi, vib, baj, bat = Pista(0, 4, 'piano_electrico'), Pista(1, 11, 'vibrafono'), Pista(2, 33, 'bajo'), Pista(9, 0, 'bateria')
    prog_a = 'C Am F G C Em F G'
    prog_b = 'F G Em Am Dm G C G'
    for idx, prog in enumerate((prog_a, prog_b)):
        t0 = idx * 32
        for c, a in enumerate(prog.split()):
            raiz = _raiz_acorde(a, 4)
            terc = 3 if 'm' in a[1:] else 4
            for tt in (0, 1.5, 2.5):
                for iv in (0, terc, 7):
                    epi.nota(t0 + c * 4 + tt, .65, raiz + iv, 52)
        bajo(baj, prog, [(0, 0), (1.5, 7), (2.5, 0), (3.5, 7)], inicio=t0, fuerza=66)
        bateria(bat, 8, {BOMBO: [0, 2.5], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=50)
    vib.melodia('E5/1.5 G5/.5 A5/1 G5/1 E5/2 C5/2 F5/1.5 E5/.5 D5/1 C5/1 D5/3 r/1 '
                'E5/1.5 G5/.5 C6/1 B5/1 G5/2 E5/2 F5/1 A5/1 G5/1 F5/1 G5/3 r/1 '
                'A5/1.5 G5/.5 F5/1 A5/1 B5/2 G5/2 G5/1.5 F5/.5 E5/1 G5/1 A5/3 r/1 '
                'F5/1 A5/1 G5/1 F5/1 G5/1 B5/1 A5/1 G5/1 E5/2 G5/2 D5/3 r/1', inicio=0, fuerza=68)
    return [epi, vib, baj, bat]


def tienda_escaparate():
    """Tienda 2: groove relajado en sol mayor (106 BPM), piano eléctrico, marimba y bajo suave."""
    epi, mar, baj, bat = Pista(0, 4, 'piano_electrico'), Pista(1, 12, 'marimba'), Pista(2, 33, 'bajo'), Pista(9, 0, 'bateria')
    prog_a = 'G Em C D G Bm C D'
    prog_b = 'C D Bm Em Am D G D'
    for idx, prog in enumerate((prog_a, prog_b)):
        t0 = idx * 32
        arpegio(epi, prog, (0, 1, 2, 1), paso=0.5, inicio=t0, fuerza=50, octava=4)
        bajo(baj, prog, [(0, 0), (1.5, 0), (2.5, 7), (3.5, 12)], inicio=t0, fuerza=66)
        bateria(bat, 8, {BOMBO: [0, 2, 3.5], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=48)
    mar.melodia('B5/1.5 A5/.5 G5/1 B5/1 G5/2 E5/2 E5/1.5 G5/.5 A5/1 G5/1 F#5/3 r/1 '
                'B5/1.5 D6/.5 C6/1 B5/1 F#5/2 D5/2 E5/1 G5/1 A5/1 G5/1 F#5/3 r/1 '
                'E5/1.5 G5/.5 C6/1 B5/1 A5/2 F#5/2 F#5/1.5 G5/.5 A5/1 F#5/1 G5/3 r/1 '
                'E5/1 A5/1 G5/1 E5/1 F#5/1 A5/1 D6/1 C6/1 B5/2 G5/2 A5/3 r/1', inicio=0, fuerza=66)
    return [epi, mar, baj, bat]


def taller_codigo():
    """Taller 1: concentración fluida en re menor dórico (98 BPM), piano eléctrico, pad y pulso constante."""
    epi, pad, vib, baj, bat = (Pista(0, 4, 'piano_electrico'), Pista(1, 89, 'colchon'), Pista(2, 11, 'vibrafono'),
                               Pista(3, 33, 'bajo'), Pista(9, 0, 'bateria'))
    prog_a = 'Dm G Dm C Dm G Am C'
    prog_b = 'F G Dm Am Bb C Dm C'
    for idx, prog in enumerate((prog_a, prog_b)):
        t0 = idx * 32
        acordes(pad, prog, inicio=t0, fuerza=32, octava=4)
        arpegio(epi, prog, (0, 1, 2, 3, 2, 1), paso=0.5, inicio=t0, fuerza=48, octava=4)
        bajo(baj, prog, [(0, 0), (1.5, 7), (2, 0), (3.5, 7)], inicio=t0, fuerza=64)
        bateria(bat, 8, {BOMBO: [0, 2.5], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=46)
    vib.melodia('r/2 A4/1 D5/1 F5/1.5 E5/.5 D5/2 r/2 F5/1 G5/1 A5/2 G5/2 '
                'r/2 A4/1 D5/1 E5/1.5 F5/.5 G5/2 r/2 E5/1 D5/1 C5/3 r/1 '
                'F5/1.5 A5/.5 G5/1 F5/1 G5/2 B5/2 A5/1.5 G5/.5 F5/1 D5/1 E5/3 r/1 '
                'D5/1 F5/1 E5/1 G5/1 F5/2 D5/2 E5/1 G5/1 F5/1 E5/1 D5/3 r/1', inicio=0, fuerza=62)
    return [epi, pad, vib, baj, bat]


def taller_chispa():
    """Taller 2: enfoque técnico cálido en la menor dórico (104 BPM), arpegio suave y groove discreto."""
    epi, pad, vib, baj, bat = (Pista(0, 4, 'piano_electrico'), Pista(1, 90, 'colchon'), Pista(2, 11, 'vibrafono'),
                               Pista(3, 38, 'bajo'), Pista(9, 0, 'bateria'))
    prog_a = 'Am D Am G Am D Em G'
    prog_b = 'C D Am Em F G Am G'
    for idx, prog in enumerate((prog_a, prog_b)):
        t0 = idx * 32
        acordes(pad, prog, inicio=t0, fuerza=32, octava=4)
        arpegio(epi, prog, (0, 2, 1, 3, 1, 2), paso=0.5, inicio=t0, fuerza=48, octava=4)
        bajo(baj, prog, [(0, 0), (1.5, 0), (2.5, 7), (3.5, 12)], inicio=t0, fuerza=64)
        bateria(bat, 8, {BOMBO: [0, 2, 2.5], CAJA: [1, 3], CHARLES: [k * .5 for k in range(8)]}, inicio=t0, fuerza=46)
    vib.melodia('r/2 E5/1 A5/1 C6/1.5 B5/.5 A5/2 r/2 C6/1 D6/1 E6/2 D6/2 '
                'r/2 E5/1 A5/1 B5/1.5 C6/.5 D6/2 r/2 B5/1 A5/1 G5/3 r/1 '
                'C6/1.5 E6/.5 D6/1 C6/1 D6/2 F#6/2 E6/1.5 D6/.5 C6/1 A5/1 B5/3 r/1 '
                'A5/1 C6/1 B5/1 D6/1 C6/2 A5/2 B5/1 D6/1 C6/1 B5/1 A5/3 r/1', inicio=0, fuerza=62)
    return [epi, pad, vib, baj, bat]


def victoria_gloria():
    """Fanfarria de victoria 1: breve, rotunda y en re mayor (124 BPM, sin bucle)."""
    trp, met, coro, tim, bat = (Pista(0, 56, 'trompetas'), Pista(1, 61, 'metales'), Pista(2, 52, 'coro'),
                                Pista(3, 47, 'timbales'), Pista(9, 0, 'platos'))
    trp.melodia('A4/.333 A4/.333 A4/.333 D5/1 F#5/1 A5/1.5 G5/.5 F#5/1 A5/1 Bb5/1 C6/1 D6/2 F#6/2 D6/3.8', inicio=0, fuerza=116)
    acordes(met, 'D G Bb C', dur=2, inicio=0, fuerza=100, octava=4)
    acordes(met, 'D', dur=7.8, inicio=8, fuerza=118, octava=4)
    acordes(coro, 'Bb C', dur=2, inicio=4, fuerza=102, octava=4)
    acordes(coro, 'D', dur=7.8, inicio=8, fuerza=116, octava=4)
    tim.nota(0, .35, 38, 106)
    tim.nota(4, .35, 34, 108)
    tim.nota(6, .35, 36, 112)
    redoble_timbal(tim, 6.5, 1.5, 38, 75, 122)
    tim.nota(8, 1.0, 38, 124)
    bat.nota(8, 4.0, PLATO, 124)
    return [trp, met, coro, tim, bat]


def victoria_campeon():
    """Fanfarria de victoria 2: paso triunfal bVI-bVII-I en do mayor (128 BPM, sin bucle)."""
    trp, met, coro, tim, bat = (Pista(0, 56, 'trompetas'), Pista(1, 61, 'metales'), Pista(2, 52, 'coro'),
                                Pista(3, 47, 'timbales'), Pista(9, 0, 'platos'))
    trp.melodia('G4/.333 G4/.333 G4/.333 C5/1 E5/1 G5/1.5 F5/.5 E5/1 G5/1 Ab5/1 Bb5/1 C6/2 E6/2 C6/3.8', inicio=0, fuerza=116)
    acordes(met, 'C F Ab Bb', dur=2, inicio=0, fuerza=100, octava=4)
    acordes(met, 'C', dur=7.8, inicio=8, fuerza=118, octava=4)
    acordes(coro, 'Ab Bb', dur=2, inicio=4, fuerza=102, octava=4)
    acordes(coro, 'C', dur=7.8, inicio=8, fuerza=116, octava=4)
    tim.nota(0, .35, 36, 106)
    tim.nota(4, .35, 32, 108)
    tim.nota(6, .35, 34, 112)
    redoble_timbal(tim, 6.5, 1.5, 36, 75, 122)
    tim.nota(8, 1.0, 36, 124)
    bat.nota(8, 4.0, PLATO, 124)
    return [trp, met, coro, tim, bat]


def derrota_lona():
    """Fanfarria de derrota 1: caída digna y breve en re menor (72 BPM, sin bucle)."""
    trb, che, cue, tim = Pista(0, 57, 'trombones'), Pista(1, 42, 'chelo'), Pista(2, 48, 'cuerdas'), Pista(3, 47, 'timbales')
    acordes(cue, 'Dm Bb A', dur=2, inicio=0, fuerza=58, octava=4)
    acordes(cue, 'Dm', dur=3.8, inicio=6, fuerza=52, octava=4)
    trb.melodia('A4/1.5 G4/.5 F4/1.5 E4/.5 D4/1.5 C#4/.5 D4/3.8', inicio=0, fuerza=92)
    che.melodia('D3/2 Bb2/2 A2/2 D2/3.8', inicio=0, fuerza=86)
    tim.nota(0, .5, 38, 92)
    tim.nota(4, .5, 45, 84)
    tim.nota(6, 1.0, 38, 96)
    return [trb, che, cue, tim]


def derrota_ceniza():
    """Fanfarria de derrota 2: descenso solemne en do menor (76 BPM, sin bucle)."""
    trb, che, cue, tim = Pista(0, 57, 'trombones'), Pista(1, 42, 'chelo'), Pista(2, 48, 'cuerdas'), Pista(3, 47, 'timbales')
    acordes(cue, 'Cm Ab G', dur=2, inicio=0, fuerza=58, octava=4)
    acordes(cue, 'Cm', dur=3.8, inicio=6, fuerza=52, octava=4)
    trb.melodia('G4/1.5 F4/.5 Eb4/1.5 D4/.5 C4/1.5 B3/.5 C4/3.8', inicio=0, fuerza=92)
    che.melodia('C3/2 Ab2/2 G2/2 C2/3.8', inicio=0, fuerza=86)
    tim.nota(0, .5, 36, 92)
    tim.nota(4, .5, 43, 84)
    tim.nota(6, 1.0, 36, 96)
    return [trb, che, cue, tim]


if __name__ == '__main__':
    guardar('entrenamiento', 116, entrenamiento())
    guardar('oriental', 76, oriental())
    guardar('final', 100, final())
    guardar('combate', 144, combate())
    guardar('rock', 138, rock())
    guardar('artes_marciales', 132, artes_marciales())
    guardar('drama', 66, drama())
    guardar('menu', 96, menu())
    guardar('menu_2', 100, menu_2())
    guardar('campeon', 100, campeon())
    guardar('estadio', 82, estadio())
    guardar('subidon', 124, subidon())
    guardar('ultimo_asalto', 120, ultimo_asalto())
    guardar('carrera_turbo', 148, carrera_turbo())
    guardar('carrera_neon', 154, carrera_neon())
    guardar('carrera_recta_1', 162, carrera_recta_1())
    guardar('carrera_recta_2', 166, carrera_recta_2())
    guardar('destruccion_pulso', 134, destruccion_pulso())
    guardar('destruccion_asalto', 138, destruccion_asalto())
    guardar('destruccion_cuenta_1', 152, destruccion_cuenta_1())
    guardar('destruccion_cuenta_2', 158, destruccion_cuenta_2())
    guardar('minijuegos_agil', 126, minijuegos_agil())
    guardar('minijuegos_pista', 130, minijuegos_pista())
    guardar('jefe_coloso', 118, jefe_coloso())
    guardar('jefe_amenaza', 124, jefe_amenaza())
    guardar('senior_maquina', 136, senior_maquina())
    guardar('senior_precision', 140, senior_precision())
    guardar('campana_creed_1', 112, campana_creed_1())
    guardar('campana_creed_2', 120, campana_creed_2())
    guardar('campana_isla_1', 128, campana_isla_1())
    guardar('campana_isla_2', 134, campana_isla_2())
    guardar('escena_hospital_1', 72, escena_hospital_1())
    guardar('escena_hospital_2', 76, escena_hospital_2())
    guardar('escena_milagro_1', 120, escena_milagro_1())
    guardar('escena_milagro_2', 126, escena_milagro_2())
    guardar('escena_victoria_1', 116, escena_victoria_1())
    guardar('escena_victoria_2', 122, escena_victoria_2())
    guardar('escena_derrota_1', 74, escena_derrota_1())
    guardar('escena_derrota_2', 78, escena_derrota_2())
    guardar('tienda_bazar', 102, tienda_bazar())
    guardar('tienda_escaparate', 106, tienda_escaparate())
    guardar('taller_codigo', 98, taller_codigo())
    guardar('taller_chispa', 104, taller_chispa())
    guardar('victoria_gloria', 124, victoria_gloria())
    guardar('victoria_campeon', 128, victoria_campeon())
    guardar('derrota_lona', 72, derrota_lona())
    guardar('derrota_ceniza', 76, derrota_ceniza())
