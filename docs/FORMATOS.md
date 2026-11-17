# Formatos del contenido de MetajavBots

Todo el contenido vive en `res/` como texto UTF-8, en los tres idiomas (es, de, en). Las pruebas
(`jugar.bat prueba`) comprueban que cada texto esté en los tres idiomas y que cada ejercicio se
resuelva con su solución. Licencia del contenido: CC BY 4.0 (Francis García Reyes).

## Diálogos — `res/dialogos/<personalidad>.txt` y `res/dialogos/locutor.txt`

Un archivo por personalidad (las de `res/datos/modelos.txt`: estoico, hiperactivo, calmado, bravucon,
poetico, cerebrito, protector, sarcastico, intrigante, honorable, teatral, jugueton, sonador, grunon,
euforico, misterioso) y uno para el locutor.

```
# comentario
[evento]
es: Frase en español.
de: Satz auf Deutsch.
en: Sentence in English.

es: Otra variante (siempre las tres líneas seguidas, en este orden).
de: ...
en: ...
```

Huecos que se rellenan solos: `{yo}` (el nombre del robot que habla), `{rival}` (el otro),
`{jugador}` (el nombre del jugador), `{ronda}` (1-4), `{pieza}` (la parte rota: cabeza, brazo…),
`{arma}`. No hace falta usarlos todos.

Eventos de cada personalidad (de 3 a 6 variantes cada uno):
`saludo` (antes de pelear), `inicio_ronda`, `ataque` (al atacar, de vez en cuando), `acierto`,
`fallo`, `golpe` (recibe un golpe), `golpe_fuerte`, `poco_blindaje`, `parte_rota`, `esquiva`,
`victoria_ronda`, `derrota_ronda`, `victoria` (gana la pelea), `derrota` (pierde y va a explotar),
`burla` (una pulla suave, de pelea amistosa), `pulla` (más dura, solo con agresividad alta: se ríe
del rival o le suelta una fanfarronada; nunca insultos de odio, nada sobre origen, cuerpo, sexo,
religión ni nada parecido: pullas de ring, como en la lucha libre o en los dibujos animados),
`subida` (el jugador sube de nivel: alegría), `error_codigo` (su propio código ha fallado: se queja
con gracia), `espera` (en el taller, mientras el jugador programa), `elogio` (le gusta el código
nuevo del jugador), `reparar`, `especial`.

Eventos del locutor (de 4 a 8 variantes cada uno; es un robot locutor LOCO, de voz mecánica,
exagerado, con chistes de programación y de robots, como un comentarista de lucha libre
chiflado): `inicio_pelea`, `inicio_ronda`, `primer_golpe`, `golpe_fuerte`, `laser`, `misil`,
`cuerpo`, `esquiva`, `bloqueo`, `parte_rota`, `fuera_de_combate`, `error_codigo` (al robot se le
ha caído una excepción), `tope` (el código del robot es tan lento que se queda pensando), `pared`,
`choque`, `aburrido` (nadie ataca), `fin_ronda`, `remontada`, `empate`, `explosion_final`,
`victoria_jugador`, `derrota_jugador`, `subida_nivel`, `bajada_nivel`.

Frases cortas (que quepan en un bocadillo: menos de 90 letras) y con mucha personalidad. El alemán
y el inglés, naturales (no traducción literal), con el mismo carácter.

## Ejercicios — `res/ejercicios/<tema>_<nombre>.txt`

Al estilo CodingBat y CodeStepByStep: ejercicios cortos que se comprueban solos. Un archivo por
tema (los temas, en `res/datos/temas.txt`), varios ejercicios por archivo.

```
=== id_unico_en_minusculas
tema: 05
dificultad: 2                 # 1 (muy fácil) a 5 (difícil) dentro de su tema
java: 8                       # versión mínima de Java que hace falta (8, 11, 17, 21, 25)
tipo: metodo                  # metodo | salida | clase
titulo.es: Contar pares
titulo.de: Gerade Zahlen zählen
titulo.en: Count evens
enunciado.es: Devuelve cuántos números pares hay en el array.
enunciado.de: Gib zurück, wie viele gerade Zahlen im Array sind.
enunciado.en: Return how many even numbers there are in the array.
pista.es: Recorre el array con un for y usa % 2.
pista.de: ...
pista.en: ...
firma: static int contarPares(int[] numeros)
plantilla:
    return 0;
solucion:
    int cuenta = 0;
    for (int n : numeros) {
        if (n % 2 == 0) cuenta++;
    }
    return cuenta;
casos:
    contarPares(new int[]{1, 2, 3, 4}) => 2
    contarPares(new int[]{}) => 0
    contarPares(new int[]{-2, 7}) => 1
```

- `tipo: metodo`: el jugador escribe el cuerpo del método de `firma` (siempre `static`). Los casos
  son `expresión => valor esperado` en Java. Para esperar una excepción: `=> !ArithmeticException`.
  Para dobles se compara con tolerancia (1e-9). Los arrays y las listas se comparan por contenido.
- `tipo: salida`: el jugador escribe el cuerpo de `firma` (normalmente `static void ...`), que
  imprime con `System.out.println`. Cada caso es `llamada; => "texto esperado"` (con `\n` para los
  saltos de línea), por ejemplo `escalera(3); => "*\n**\n***\n"`.
- `tipo: clase`: el jugador escribe clases enteras (sin `firma`); la `plantilla` y la `solucion`
  son el archivo entero. Los casos son expresiones que usan sus clases:
  `new Punto(3, 4).distanciaAlOrigen() => 5.0`.
- Las secciones `plantilla:` y `solucion:` (y `casos:`) siguen hasta la siguiente clave sin sangría;
  sus líneas van con 4 espacios de sangría (que se quitan al leer).
- La plantilla debe compilar y NO pasar todos los casos; la solución debe pasarlos todos.
- Ejercicios propios: inspirados en el espíritu de CodingBat, CodeStepByStep, «Java Challenge»
  (Inden) y «Captain CiaoCiao» (Ullenboom), sin copiar ninguno. Mejor si tienen que ver con robots,
  combates, piezas y talleres.

## Tutoriales — `res/tutoriales/<nn>_<id>.txt`

Lecciones cortas, por temas, de cero absoluto a avanzado. Cada lección mejora el robot y enseña Java.

```
=== id_de_la_leccion
tema: 05
orden: 3
titulo.es: ...
titulo.de: ...
titulo.en: ...
--- pagina
es: Texto de la página (puede tener varias líneas es: seguidas).
de: ...
en: ...
codigo:
    // código de ejemplo (en español; el juego lo muestra tal cual)
    for (int i = 0; i < 3; i++) {
        avanzar(50);
    }
--- tarea
ejercicio: id_de_un_ejercicio          # un ejercicio del banco, o bien:
robot: aciertos>=3                     # una prueba en la arena con el muñeco: aciertos>=N, dano<=N, ronda_sin_errores, usa:<palabra>
es: Lo que hay que hacer.
de: ...
en: ...
```
