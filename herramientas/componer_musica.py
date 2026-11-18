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
        bat.nota(c * 4, 1, PLATO, 70 if c % 4 == 0 else 0)
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
    """Menú: piano eléctrico, bajo de sintetizador y batería suave (fa mayor), para estar sin cansar."""
    epi, baj, bat = Pista(0, 4, 'piano_electrico'), Pista(1, 38, 'bajo'), Pista(9, 0, 'bateria')
    prog = 'F Am Bb C F Dm Bb C'
    for c, a in enumerate(prog.split()):
        raiz = altura(a[0] + ('b' if 'b' in a else '') + '4')
        for t in (0, 1.5, 2.5):
            for iv in (0, 3 if 'm' in a else 4, 7):
                epi.nota(c * 4 + t, .8, raiz + iv, 55)
    bajo(baj, prog, [(0, 0), (1.5, 0), (2.5, 7), (3.5, 12)], fuerza=70)
    bateria(bat, 8, {BOMBO: [0, 2.5], CAJA: [1, 3], CHARLES: [0, 1, 2, 3]}, fuerza=55)
    return [epi, baj, bat]


if __name__ == '__main__':
    guardar('entrenamiento', 116, entrenamiento())
    guardar('oriental', 76, oriental())
    guardar('final', 100, final())
    guardar('combate', 144, combate())
    guardar('rock', 138, rock())
    guardar('artes_marciales', 132, artes_marciales())
    guardar('drama', 66, drama())
    guardar('menu', 96, menu())
