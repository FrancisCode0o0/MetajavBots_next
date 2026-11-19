# ENCARGO METAJAVBOTS_3
Para qué: que jugando una hora al día se aprenda Java a fondo, del lego al senior, y que un senior también aprenda de verdad.

**Lo primero: cada cosa pequeña que termines queda guardada y jugable.** Trabaja por bloques, en este orden, y termina un bloque entero antes de empezar el siguiente. Todos los bloques se terminan, también los pendientes de antes (bloque 5). Divide cada bloque en pasos pequeños; al acabar cada paso, el juego arranca, `jugar.bat prueba` pasa entero, haces commit y `git push origin main`. Así, si la sesión se corta, todo lo hecho está guardado y se puede jugar.

Lee docs/ESTADO.md con grep y por trozos. Trabaja en la rama `main`. Al cerrar cada paso, apunta en ESTADO qué bloques van hechos y por dónde sigue, para continuar justo ahí en otra sesión. Las pruebas (`jugar.bat prueba`) siguen todas bien.

**Ya hecho** (no rehacer): el robot hace de verdad lo que dice el código y se ve (cajas, pizarra, estantería, puerta, palanca; al fallar, falla haciendo la acción, con gracia) y la pista de carrera contra el fantasma de la solución.

## Los bloques, en este orden

### Lo primero en la sesión siguiente: las voces sin retraso (Fran, 18-nov: «las voces están buenísimas, pero van con mucho retraso»)
Las mismas voces de Piper, que empiecen a sonar casi al instante, como un lector de pantalla; sobre todo cuando lee el entrenador. Hoy `sonido/VozPiper` arranca un Piper nuevo por frase (carga la voz cada vez) y espera a tenerla entera antes de sonar. Mide el retraso antes y después (del texto al primer sonido) y apúntalo en ESTADO. Después sigue con lo que falta, apuntado al principio de ESTADO.

### Bloque 1. El entrenador
El entrenador flotante enseña, ayuda y acompaña en todo el juego. Lo que hay sin commit (EntrenadorFlotante, Concepto, res/conceptos, PruebasDelEntrenador) es el comienzo de este bloque: termínalo.
- **Explica cualquier concepto** de Java y de la orientación a objetos, desde lo más básico (qué es una variable, una clase, un objeto, un método, cómo se ejecuta un programa) hasta lo avanzado, claro y con ejemplos del propio juego, para que lo entienda alguien que no sabe nada. Se le puede preguntar en cualquier momento; sin IA, con explicaciones escritas de serie.
- **Contesta cualquier pregunta, con sus palabras.** Con IA, se le pregunta libremente sobre Java y sobre el juego («¿qué hace mi código en esta línea?», «¿por qué pierdo?») y contesta mirando el código y la situación del jugador. Sin IA, con lo escrito de serie.
- **Cómo explica**: mucho pseudocódigo, y también código Java, sobre todo al explicar cosas de Java; metáforas de peleas y ejemplos de funciones de Java. Regla de siempre: pistas y trocitos, nunca la solución entera. Cuánto ayuda lo regula la barra que ya existe («Dame menos / Dame más»).
- **Errores del editor**: cuando el editor marca algo, el jugador le pregunta al entrenador qué pasa en esa línea y él lo explica de forma interactiva, con esas metáforas, pseudocódigo y ejemplos.
- **En el taller** da tips mientras se programa.
- **En la pelea** da consejos sobre la marcha. El jugador marca al enemigo y el entrenador le da estrategias contra él: mira cómo pelea y propone qué probar («veo que te rodea por la izquierda; prueba esto…»).
- **Contra jefes no ayuda**: está en el hospital agonizando y necesita que ganes, por causas absurdas que cambian (tomó aceite en mal estado, fue a la playa y el agua salada lo oxidó…); que se vea y se diga, con gracia. Cuando ganas, aparece milagrosamente a felicitarte: al final sí fue al combate.
- **Se desactiva** cuando el jugador quiera.
- **Cada entrenador con su voz distinta** (Piper tiene una por idioma; variar el tono u otra voz).
- **Enseña a instalar y despertar los cerebros**, para alguien que no sabe nada: paso a paso, con dibujos o capturas, cómo poner un cerebro local (KoboldCpp y un modelo) y uno en línea (sacar la clave de Google, Claude u OpenAI y ponerla), cómo llenar las tres casillas y cómo despertar los cerebros cuando hace falta. También enseña a jugar y a ver las notas.

### Bloque 2. Tres cerebros a la vez (Fran: «súper importante»)
- Las tres casillas admiten cualquier cerebro, local o en línea, mezclados como quiera el jugador: tres locales, tres en línea o cualquier mezcla, y los tres trabajan a la vez.
- En línea, además de Claude y OpenAI, **Gemini de Google** como opción propia (clave en `~/.metajavbots/clave_gemini.txt`).
- Pruébalo de verdad con las tres casillas llenas a la vez.

### Bloque 3. El editor de código
- **Ayuda como los editores de verdad**: señala los errores al escribir, autocompleta y enseña la documentación de cada función (al pasar el ratón o al pedirla). Lo que ya hay, que se note y funcione igual de bien con las clases del jugador. (Las explicaciones las da el entrenador: bloque 1.)
- **Barra de desplazamiento**: se arrastra con el ratón (clic en la barra y bajar o subir) y un clic en el carril salta a ese sitio, en vertical y en horizontal.

### Bloque 4. Todo es juego, se ve y se aprende a fondo, hasta senior
Cada ejercicio, nivel y pelea es juego animado y enseña de verdad, del lego al senior.
- **Cada ejercicio, del más corto al más alto, tiene su escena animada**, y lo que escribe el jugador se ve en lo que hace el robot: una pista de carrera que cruzar, una carrera que ganar, obstáculos que sortear, y las demás escenas que haga falta (también en el laboratorio de pruebas y en todo lo que aún sea solo texto). Cada número, condición o bucle que cambia se nota en el movimiento. Un ejercicio que solo da texto no vale.
- **Absurdo hasta el límite y más allá.** Es el estilo del juego: animaciones divertidas y parodias en todas las escenas, en los aciertos y sobre todo en los fallos; cuanto más disparatado, mejor.
- **Ejercicios que enseñan el juego.** En los ejercicios cortos (tipo CodingBat), además de los que hay, una tanda que introduce el juego: moverse, girar, apuntar, usar los brazos, leer al enemigo, memoria entre rondas, aparatos… cada uno con su escena. Además, minijuegos aparte para aprender a moverse, girar, apuntar y disparar.
- **Ejercicios que se corrigen solos** con pruebas y, al fallar, explican el porqué con una pista o una pregunta, nunca la solución.

- **La orientación a objetos pesa tanto como Java, en todo el juego**: en los ejercicios, los niveles, las peleas y las explicaciones del entrenador.
- **Niveles hasta senior.** Cada nivel, un tema de Java con su ejercicio corto y su pelea. Temas de arriba: genéricos y comodines, colecciones y su coste, lambdas y streams, records, sealed y pattern matching, excepciones bien diseñadas, inmutabilidad, concurrencia (hilos, hilos virtuales, java.util.concurrent, condiciones de carrera), modelo de memoria, recolector de basura y rendimiento, patrones de diseño, pruebas con JUnit, refactorizar código heredado, reflexión y anotaciones, módulos, bytecode.
- **Peleas que exigen el tema.** Presupuesto de tiempo y de memoria por turno medido de verdad, robots que piensan en paralelo, un robot heredado que hay que arreglar sin romper sus pruebas, rivales que castigan los fallos típicos de cada tema.
- **La prueba de nivel con IA**: que el cerebro invente preguntas nuevas (comprobadas antes, como las plantillas de ahora).
- **La sesión de una hora** la propone el juego: repaso de lo flojo, tema nuevo y pelea.
- **Sparring con tu temario.** En el modo IA, el jugador da su temario (PDF, Word u otro formato) y recibe una propuesta de ejercicios completos que repasa todo lo que contiene, al nivel de dificultad que muestra el temario. De cada tema sale todo lo necesario para dominarlo: combates de sparring y ejercicios cortos animados, cada uno con sus pruebas y comprobado antes de dárselo al jugador. La propuesta se guarda como campaña propia y se juega después sin IA.

### Bloque 5. Peleas, robots y arena (lo que quedó pendiente)
- **Diseña tu propia pieza**: una clase que implementa la interfaz Pieza.
- **Más aparatos estilo Medabots**: martillo, sierra, imanes…
- **Repetición con el código al lado** (la línea que se ejecutaba) y guardarla como GIF.
- **Personalidades propias para los seis robots nuevos** (Fran dice que ya está hecho; compruébalo y completa solo lo que falte).
- **Editar las biografías** de robots y entrenadores desde el juego.
- **Casos alemanes de declinación** en las frases.
- **Campañas de Creed.**
- **Equilibrio revisado jugando** (no solo en capturas): armas especiales, aparatos y desgaste de 10-12 rondas.

### Bloque 6. Música épica de las peleas, en MIDI (Fran: «muy importante»)
Con `herramientas/componer_musica.py`, como la de ahora: que levante el ánimo como las bandas sonoras de las películas de boxeo y artes marciales de los 70-80 (Rocky, Karate Kid) y el rock de estadio tipo Queen: metales triunfales, crescendos, coros, guitarras y tambores que empujan, el subidón del entrenamiento y del último asalto. Música original de homenaje, con melodías propias (nada copiado de ninguna canción). Varias piezas, para que no se repita. Un SoundFont (.sf2) libre para que el MIDI suene mejor.

### Bloque 7. Déjalo instalado para Fran, listo para probar
En su portátil: el juego arranca con `jugar.bat`, las tres casillas configuradas (dos locales de `C:\Users\Francis\OneDrive\Documentos\000KI_Local` y Gemini en línea si está su clave) y comprobado que los tres contestan a la vez. Apunta en ESTADO qué quedó instalado y qué falta.

Dificultad, orden fino y números: `// TODO-FRAN:`.

Mide los fotogramas antes y después y apúntalos en ESTADO. Al acabar, ESTADO al día y la palabra ENCARGO TERMINADO.
