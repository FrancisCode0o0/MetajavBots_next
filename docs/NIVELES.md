# Los niveles (etiquetas) de MetajavBots

## Cuántos y cuáles

Tres categorías, como pidió Fran (Noob, Junior, Senior), con **cuatro escalones cada una** (12 en total, «diez o más») y una secreta, **Master**.

| Etiqueta | Temas que se aprenden en ella (res/datos/temas.txt) |
|---|---|
| Noob I | 01 Primeros pasos · 02 Variables y tipos |
| Noob II | 03 Operadores · 04 Condicionales |
| Noob III | 05 Bucles |
| Noob IV | 06 Métodos · 07 Arrays |
| Junior I | 08 Textos · 09 Clases y objetos |
| Junior II | 10 Encapsulación · 11 Herencia |
| Junior III | 12 Polimorfismo y abstractas · 13 Interfaces |
| Junior IV | 14 Excepciones · 15 Colecciones |
| Senior I | 16 Genéricos · 17 Lambdas |
| Senior II | 18 Streams · 19 Java moderno (enum, record, sealed, pattern matching) |
| Senior III | 20 Hilos y concurrencia · 21 Recursión y algoritmos |
| Senior IV | 22 Pruebas, estilo y diseño (y repasar todo: el 80 % de los ejercicios) |
| **Master** | El límite: 25 victorias contra la computadora en extrema |

**Por qué así** (razonado, no copiado de ningún sitio):
- Las tres categorías siguen la escala habitual del oficio (junior, senior) con un «noob» antes, que es quien no ha programado nunca.
- Cuatro escalones por categoría siguen los grandes bloques con que se enseña Java en un curso universitario de Programación I y II y en los temarios de las certificaciones de Java (fundamentos → control → datos → orientación a objetos → avanzado → profesional): cada escalón es un bloque que se puede practicar en unas semanas.
- Master es aparte porque no es un tema más: es dominar la arena (ganar muchas veces a la extrema), como pidió Fran.

## Cuánto cuesta (el ritmo)

Para pasar de una etiqueta a la siguiente hacen falta **victorias en ella** (de 4 en Noob I a 12 en Senior IV; `Progreso.victoriasParaSubir`) y **el 70 % de los ejercicios de sus temas** (el 80 % de todos en Senior IV). Así no se sube solo peleando: hay que estudiar.

Cálculo aproximado hasta Senior IV con el contenido de ahora: 262 ejercicios × 10-25 minutos (con sus errores y repasos) ≈ 45-110 h; 70 lecciones × 10-20 minutos ≈ 12-25 h; unas 95 peleas ganadas (más las perdidas) de unos 4 minutos cada una, más programar entre rondas ≈ 25-50 h; y el tiempo de entender lo que no sale. En total, **unas 100-200 horas de juego**, que es del orden de lo que lleva un curso serio de Java hasta lo avanzado (un semestre o dos de estudio constante). Como la etiqueta exige resolver los ejercicios de cada tema, quien llega a Senior ha practicado de verdad cada bloque.

**Bajar**: con 5 derrotas seguidas y muchas más derrotas que victorias en la etiqueta, se baja un escalón (nunca por debajo de Noob I). La computadora, además, se ablanda tras varias derrotas, para no frustrar.

**Robots por partida**: 1 en Noob; hasta 2 en Junior, 3 en Senior y 4 en Master, pero cada robot nuevo se gana además con victorias (30, 80 y 150 en total).

## La prueba de nivel

18 preguntas (res/datos/prueba_de_nivel.txt), tres por escalón (Noob II, Noob IV, Junior II, Junior IV, Senior I, Senior II). Se empieza en el último escalón del que se aciertan dos de tres, sin saltarse ninguno; como mucho, en Senior II (lo demás se demuestra jugando). Quien no ha programado nunca empieza en Noob I sin prueba.

## La computadora según la etiqueta

`Etiqueta.nivelDeLaIA()` va de 0,05 (Noob I) a 0,875 (Senior IV) y 1 (Master). En cada pelea, `Progreso.nivelParaLaPelea` le suma un poco de azar (unas peleas más fáciles que otras), la ablanda tras dos o más derrotas seguidas y la endurece un poco tras tres victorias. **No cambia durante la pelea.** El desafío usa la de tres escalones más arriba.

`TODO-FRAN`: todos estos números están en `progreso/Progreso.java` y `progreso/Etiqueta.java`, en un solo sitio, para afinarlos jugando.
