"""Aplica los trozos de la parte 4 que no tienen segunda pasada (sin p2_trozo_XX.json).

Solo cambia una línea si el texto «antes» sigue en ella (o, si no, si aparece
una sola vez en el archivo). Con --probe solo cuenta, no escribe nada.
"""
import io
import json
import os
import sys

RAIZ = os.path.dirname(os.path.abspath(__file__))
MATERIAL = os.path.join(os.path.dirname(RAIZ), "material_revision", "correcciones")
CON_P2 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 13, 14, 15, 18, 19, 26}
TROZOS = [i for i in range(41) if i not in CON_P2]

# Revisadas texto a texto en la parte 4: el «antes» que queda ya no es el trozo que describía la corrección.
RECHAZADAS = {
    (32, "res/textos/ui.txt", 418, "= recarga |"):
        "ya aplicada en valor.enfriamiento; lo que queda es valor.recarga (otra clave)",
    (34, "res/textos/ui.txt", 1025, "Ein Tipp"):
        "el español dice «Un consejo», no «Una pista»: Tipp es correcto",
    (34, "res/textos/ui.txt", 1272, "Back to the story"):
        "el español dice «Volver a Historia», no «Seguir historia»",
    (23, "res/dialogos/sonador.txt", 167, "Fiuuu…"):
        "el alemán ya dice «Fiuuu …»; lo que queda es la línea española",
    (17, "res/datos/paquetes.txt", 1850, "in Übungen."):
        "el español dice «los ejercicios», no «ejercicios de concurrencia»: «in Übungen» es correcto",
    (29, "res/ejercicios/24_juego.txt", 15, "«bandera»"):
        "el texto «antes» está en enunciado.es; enunciado.de ya tiene „bandera“",
    (29, "res/ejercicios/24_juego.txt", 100, "«rumbo»"):
        "el texto «antes» está en enunciado.es; enunciado.de ya tiene „rumbo“",
    (29, "res/ejercicios/24_juego.txt", 100, "«haciaEnemigo»"):
        "el texto «antes» está en enunciado.es; enunciado.de ya tiene „haciaEnemigo“",
    (29, "res/ejercicios/24_juego.txt", 307, "«mira»"):
        "el texto «antes» está en enunciado.es; enunciado.de ya tiene „mira“",
    (11, "res/conceptos/07_juego.txt", 1126, '"Design your part"'):
        "en resumen.ia ya están las dos primeras comillas convertidas; falta esta segunda para no dejar una abierta",
    (11, "res/conceptos/07_juego.txt", 1137, '"Design your part"'):
        "en resumen.ia solo queda una comilla inglesa suelta; la otra ya está convertida",
    (17, "res/datos/paquetes.txt", 1776, "to keep fights deterministic"):
        "el alemán ya dice «damit Kämpfe … bleiben» y añadir la frase rompería la concordancia; lo revisaré aparte",
    (11, "res/conceptos/07_juego.txt", 1139, '"Save GIF"'):
        "«Save GIF» ya tiene comillas inglesas en resumen.ia; convertirla dejaría comillas desparejadas",
}


def trozos_a_hacer():
    if len(sys.argv) > 1 and sys.argv[1].isdigit():
        return [int(sys.argv[1])]
    return TROZOS


def carga(trozo):
    with io.open(os.path.join(MATERIAL, "trozo_%02d.json" % trozo), encoding="utf-8") as f:
        return json.load(f)


def aplica(archivo, linea, antes, despues, escribir):
    ruta = os.path.join(RAIZ, archivo.replace("/", os.sep))
    if not os.path.exists(ruta):
        return "falta el archivo"
    with io.open(ruta, "rb") as f:
        crudo = f.read()
    texto = crudo.decode("utf-8")
    lineas = texto.split("\n")
    if 1 <= linea <= len(lineas) and antes in lineas[linea - 1]:
        destino = lineas[linea - 1]
        if despues in destino:
            return "ya aplicada (el resultado ya está en la línea %d)" % linea
        lineas[linea - 1] = destino.replace(antes, despues, 1)
        nuevo = "\n".join(lineas)
        modo = "linea"
    elif texto.count(antes) == 1:
        nl = texto[:texto.index(antes)].count("\n")
        if despues in lineas[nl]:
            return "ya aplicada (el resultado ya está en la línea %d)" % (nl + 1)
        nuevo = texto.replace(antes, despues, 1)
        modo = "unica en el archivo (linea %d)" % (nl + 1)
    else:
        return "no encaja (veces en el archivo: %d)" % texto.count(antes)
    if nuevo == texto:
        return "sin cambio"
    if escribir:
        with io.open(ruta, "wb") as f:
            f.write(nuevo.encode("utf-8"))
    return "bien|" + modo


def main():
    escribir = "--probe" not in sys.argv
    total = bien = 0
    por_archivo = {}
    for n in trozos_a_hacer():
        datos = carga(n)
        for c in datos["correcciones"]:
            clave = (n, c["archivo"], c["linea"], c["antes"])
            if clave in RECHAZADAS:
                print("RECHAZADA trozo %02d %s:%s %r -> %s"
                      % (n, c["archivo"], c["linea"], c["antes"][:60], RECHAZADAS[clave]))
                continue
            total += 1
            por_archivo[c["archivo"]] = por_archivo.get(c["archivo"], 0) + 1
            r = aplica(c["archivo"], c["linea"], c["antes"], c["despues"], escribir)
            if r.startswith("bien|"):
                bien += 1
                print("APLICAR trozo %02d %s:%s [%s] %r -> %r  (%s) [%s]"
                      % (n, c["archivo"], c["linea"], c.get("idioma", "?"), c["antes"], c["despues"],
                         c.get("motivo", ""), r[5:]))
            elif not escribir or r != "sin cambio":
                print("OMITIDA trozo %02d %s:%s %r -> %s" % (n, c["archivo"], c["linea"], c["antes"][:60], r))
        print("trozo %02d: %d de %d aplicadas" % (n, bien, total))
    print("TOTAL: %d de %d aplicadas" % (bien, total))
    if not escribir:
        for k, v in sorted(por_archivo.items(), key=lambda x: -x[1]):
            print(v, k)


main()
