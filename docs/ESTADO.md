# MetajavBots · Estado

Encargo METAJAVBOTS (17-nov-2026, Opus, de noche). Repositorio propio en `OneDrive\Hochschule\Desarrollo de juegos\MetajavBots_next`, rama `main`.

## Cómo se juega (y se prueba)

- **Jugar**: `jugar.bat` (Windows) o `./jugar.sh`. Hace falta un **JDK 21 o más nuevo** (el juego compila el código Java del jugador con javac; con solo un JRE se pelea en «piloto automático» y se avisa).
- **Pruebas**: `jugar.bat prueba` (o `gradlew pruebas:prueba`): **69 casos**, entre ellos: el editor machacado con miles de pulsaciones al azar, los 262 ejercicios (cada solución pasa sus casos y cada plantilla no), las 262 soluciones compiladas con la traza, peleas de 2, de 6 (battle royale) y de 2 contra 2, el equilibrio de los 16 modelos, el compilador y su caja de arena, el laboratorio de pruebas, la voz, el progreso y el guardado.
- **Capturas**: `jugar.bat capturas` (o `gradlew lwjgl3:capturas -Psalida=…`): fotos de todas las pantallas con ventana invisible. Con `-Psonido`, con el audio encendido.
- **Cerebro de IA con arranque real** (opcional, tarda un minuto): `set METAJAVBOTS_PROBAR_CEREBRO=1` y `gradlew pruebas:prueba -Psolo=arranca`.

Primera partida: nombre → «¿has programado?» (Nunca → Noob I; si no, la prueba de nivel de 18 preguntas te pone la etiqueta, con fuegos artificiales) → eliges tu primer robot → menú. En el **Taller** está el código del robot (plantilla en tu idioma); **Ctrl+S** compila. «¡A pelear!» lanza una pelea de 4 rondas a tu nivel. Entre rondas: informe, código y piezas.

## Qué está hecho

**Motor**: copia de CastillaFantasX (el módulo `motor/` de `C:\Users\Francis\git\Reliquia_editor`, **rama `orden_2`, commit `948211b`**, tal cual, sin cambios). El juego usa de él la compilación base (libGDX, shapedrawer, FreeType), las letras DejaVu y el registro de cierres (`Cierre`); el dibujo, la interfaz y el editor del juego son propios (el motor es de pixel art; aquí hacía falta otra cosa).

**El editor de código** (lo más importante, `pantalla/VentanaDeCodigo` + `codigo/Documento`): números de línea, coloreado de Java (por trozos del Léxico, cada línea dibujada de una vez: ninguna letra se monta), línea actual, pareja de llaves, palabra bajo el cursor resaltada, guías de sangría, errores subrayados en rojo con su explicación según el nivel al pasar el ratón (y marcas en la barra), deshacer/rehacer por palabras (1000 cambios), cierres automáticos de ( [ " ', Intro con sangría y «}» debajo, Tab/Mayús+Tab por bloques, Ctrl+/ comentar, Ctrl+D duplicar, Ctrl+Mayús+K borrar línea, Alt+↑↓ mover líneas, Ctrl+←→ por palabras, Inicio inteligente, buscar y reemplazar (Ctrl+F/H, F3), autocompletado (Ctrl+Espacio y al escribir), formateo profesional (Ctrl+Mayús+F: guía de Google/Oracle, 4 espacios, case dentro del switch, +8 en continuaciones; no cambia ninguna letra que no sea espacio: probado), zoom (Ctrl+rueda, Ctrl+±), repetición de teclas, AltGr del teclado español, portapapeles. El **panel de la API** al lado (F1 o Ctrl+clic sobre una palabra) busca sin tildes en la API de los robots (generada de las fachadas) y en 239 entradas de referencia de Java, por los 22 temas, con «Insertar».

**La API de los robots en tres idiomas**: `Metajavbot` (es), `MetajavbotDe`, `MetajavbotEn`, con `Enemigo/Gegner/Enemy` y `Golpe/Treffer/Hit` como interfaces (lo implementa una sola clase). Cada idioma ve solo sus nombres. 54 métodos: moverse, girar, apuntar, atacar con cada brazo, lo especial de la cabeza, saber, memoria entre rondas (`memoria()`), compañeros y avisos de equipo. `herramientas/fachadas.py` rehace las fachadas alemana e inglesa y `herramientas/api_doc.py` la documentación del buscador.

**El compilador y su caja de arena** (`codigo/`): javac en memoria; el **Inyector** mete un paso (`Tope.paso()`) en cada bucle y método sin cambiar las líneas, así un bucle infinito o una recursión enorme se cortan y el código lento cuesta turnos; el **Vigilante** lee las clases compiladas y no deja usar archivos, red, procesos, reflexión, `System.exit`, `System.in` ni (en robots) hilos; el **Cargador** solo carga lo permitido. `System.out` va a la consola del robot. Los errores salen con la línea y la columna del jugador.

**La pelea** (`combate/`): 4 rondas de 40 s; partes que se rompen (cabeza, brazos, piernas; sin cabeza, fuera de combate); láser, balas (con perdigones), misiles que persiguen, cuerpo a cuerpo (puños, espada, martillo que aturde, taladro, látigo), lanzallamas, escudo de brazo, brazo reparador; especiales de cabeza (escudo, turbo, reparar, camuflaje, pulso, sobrecarga); radar con cono; naturalezas; choques; paredes. **Gana quien menos daño recibe**; al final el perdedor (o todos los que no ganan) explota. Pelea de 2, **battle royale** (todos contra todos) y **por equipos** (sin daño entre compañeros; se avisan con `avisarAlEquipo` / `alOirAlEquipo`). La misma semilla da la misma pelea.

**La pausa entre rondas**: informe (daño, puntería, tiempo viendo al enemigo, errores, topes, por arma) y consejos del Revisor; se cambia el código (se compila) y las piezas; el robot nuevo entra en la ronda siguiente y conserva su `memoria()`. Sin parar el ritmo: rondas cortas y vivas, pausa para programar (decisión pedida en el encargo).

**Daños a la vista**: abolladuras y quemaduras que se acumulan toda la pelea en su parte, humo que crece con el daño, chispas de las partes rotas, cables sueltos, cara de la pantalla que cambia, sacudidas.

**Gráficos** (`pantalla/DibujoRobot`, `VistaDeArena`, `Efectos`): robots dibujados con formas en vista de tres cuartos, 8 torsos, 10 cabezas, brazos según el arma, 6 formas de moverse (piernas, orugas, ruedas, flotador, alas, patas de araña), detalles (capa, mochila, panel, alerón, cola, hombreras, remaches, rayas, pinchos, aro…); 5 escenarios (hangar, desierto, ciudad de neón, laboratorio, luna); partículas, láseres con brillo, estelas, explosiones, fuegos artificiales. Diseños propios (nada de Medabots).

**16 modelos** (`res/datos/modelos.txt`), cada uno con silueta, colores, naturaleza y personalidad propias. **236 piezas** (`res/datos/piezas.txt`, con series Mk I-V: el sistema aguanta cientos añadiendo líneas), entre ellas legendarias (desafío), épicas (jefes), raras (laboratorio de pruebas y retos de programar).

**Personalidades y diálogos**: 16 personalidades × 22 situaciones × 4-5 frases × 3 idiomas (≈1430 frases) y el **locutor robot loco** (24 situaciones, 125 frases). El **Narrador** decide quién dice qué sin atropellarse; la computadora se burla según la agresividad (pullas de ring, nunca de odio; filtro `Moderacion`).

**Voz del locutor**: sintetizador de voz por formantes propio (`sonido/VozRobot`), sin internet ni bibliotecas: reglas de lectura de español, alemán e inglés, números en letras, entonación «loca» y efecto robótico (modulación en anillo y eco metálico). Suena a máquina a propósito. **Sonidos** sintetizados (`Sintetizador`, 33 sonidos: láseres, disparos, misiles, metal, explosiones, servos, campana, fuegos, interfaz).

**Contenido para aprender** (lo que más importa del juego):
- **262 ejercicios** que se comprueban solos (estilo CodingBat/CodeStepByStep; tipos método, salida y clases), en 22 temas de los primeros pasos al diseño, dificultad 1-5, marcando la versión de Java que hace falta (8, 10, 11, 14-17, 21, 22, 24, 25) y explicando qué versión trajo cada novedad. Inspirados en CodingBat, CodeStepByStep, Inden y Ullenboom, sin copiar ninguno.
- **70 lecciones** de tutorial en los 22 temas, que mejoran el robot y enseñan Java desde cero, con tareas (un ejercicio o una prueba en la arena contra el muñeco: aciertos, daño, sin errores, «usa:for»…).
- **Explicaciones de errores** de javac por nivel (113 códigos × Noob/Junior/Senior × 3 idiomas) y **referencia de Java** (239 entradas).
- **Prueba de nivel** (18 preguntas).

**Progreso** (`progreso/`, docs/NIVELES.md): 12 etiquetas (Noob, Junior y Senior I-IV) y **Master** secreta (25 victorias en extrema desde Senior IV). Para subir: victorias en tu etiqueta + 70 % de los ejercicios de sus temas; se baja tras 5 derrotas seguidas con muchas más derrotas que victorias. Al subir: fuegos artificiales, la pantalla y los robots saltan de alegría, y el locutor lo celebra. Robots por partida: 1 en Noob, 2 en Junior, 3 en Senior, 4 en Master, y además cada uno se gana con victorias (30, 80, 150). **Botín** de naturaleza al ganar. **Desafío** (una IA tres escalones arriba: subir o legendaria). **Historial** con estadísticas. Partidas guardadas en `partidas/<nombre>/` (el código de cada robot en su .java; escritura segura).

**La computadora** (`rivales/Rival`, legible en el juego): 5 estilos, 5 dificultades (o **adaptada** a la etiqueta: fija en toda la pelea, unas más fáciles que otras y más fácil tras perder varias). Desde «difícil» cambia de táctica entre rondas según lo que le hizo daño. **Modo azar**: modelo, piezas y estilo al azar.

**Modo especial** (todas las ideas): **jefes de tema** (9: herencia, interfaces, colecciones, excepciones, genéricos, lambdas, streams, recursión, hilos; casi inmunes salvo que el código use su tema de verdad, lo mira `DetectorDeTemas`); **tope de cálculo estricto** (150/400/1000 pasos por turno); **depurador en vivo** (pausa, turno a turno, variables del robot por reflexión, pasos por turno, consola); **errores explicados según el nivel**; **laboratorio de pruebas** (3 retos: pruebas al estilo JUnit 5 que deben pasar con la clase buena y cazar a los mutantes → piezas); **revisión del código** tras la pelea (sin IA, el Revisor; con IA, además, la del cerebro); **robots exportables** como .java (con su modelo y piezas en una cabecera) para pelear contra los de otros (`robots_ajenos/`); **retos de programar** que dan piezas (predicción, radar, esquiva).

**Máquina contra máquina**: dos robots de la computadora pelean con su código a la vista; la **traza** (el Inyector apunta cada sentencia que se ejecuta) ilumina las líneas del turno y ▶ marca la última; pausa y turno a turno.

**Modo temario**: el jugador pega su temario; el juego reconoce los temas (por palabras, en tres idiomas) y prepara ejercicios y **combates temáticos** (el rival solo recibe daño si el código usa ese tema); con IA, además, ejercicios nuevos a medida que el comprobador valida antes de darlos.

**Cerebro de IA** (opcional; docs/CEREBRO.md): ninguno, local, en línea o **los dos a la vez** (local para lo rápido: pullas, locutor, charla y estilo del rival entre rondas; en línea para lo pesado: revisión, tutores, temario; si falla internet o la clave, el local lo hace todo). El local se **carga solo al usarlo** y se apaga tras 8 minutos sin uso y al cerrar el juego; el jugador elige el modelo de la carpeta. Claves en `~/.metajavbots/`, nunca en el repositorio. La IA no decide cada movimiento: elige el estilo del rival entre rondas y habla.

**Tres idiomas** en todo: interfaz (≈380 textos), API, ejercicios, tutoriales, diálogos, errores, referencia, README.

**Público en GitHub** (el push lo hace Fran): LICENSE (Apache-2.0, con el autor), NOTICE, docs/LICENCIA_CONTENIDO.md (CC BY 4.0), README en tres idiomas con cómo citarlo, CREDITOS.md, .gitignore sin claves, modelos ni compilados.

## Lo que decidí yo (para que Fran lo revise)

- **Rama**: `main` del repositorio nuevo (lo dice METAJAVBOTS.txt; COLA.md hablaba de `metajavbots`).
- **Modelo local del juego**: bajé **Qwen3.5-9B Q4_K_M** (bartowski, 6,2 GB) como pediste, pero el **KoboldCpp 1.112.2** que hay **no lo carga** («missing tensor blk.32.ssm_conv1d.weight»; parece que su soporte de Qwen3.5 aún no está completo). No actualicé KoboldCpp porque es el mismo exe de Jarvis. Como el portátil (i7-1065G7, 4 núcleos, gráfica integrada, 20 GB) va justo con modelos de 7-9B (el de Jarvis tardó unos 40 s por respuesta corta), bajé también **Gemma 3 4B Q4_K_M** (2,5 GB): arranca en ~50 s la primera vez y contesta en ~11 s. Es el que el juego prueba primero; luego Qwen3.5 (cuando KoboldCpp lo cargue) y el de Jarvis. Todos en `000KI_Local`; ninguno en el repositorio. **Para ti**: si actualizas KoboldCpp a una versión que cargue Qwen3.5, ponlo el primero en Ajustes › Cerebro (◄ ►), aunque será más lento que Gemma en este portátil.
- **Voz del locutor**: quise compilar **SVOX Pico TTS** (Apache-2.0, buena voz en es/de/en) bajando Zig para compilarlo, pero el permiso de bajar y ejecutar ese compilador se denegó. Escribí un sintetizador propio por formantes: se entiende y suena a robot (lo pedido), pero no es tan claro como Pico. **Para ti**: si quieres más claridad, autoriza bajar Zig (MIT) y compilar Pico (queda la idea en docs/CEREBRO.md); el juego solo tendría que llamar al .exe.
- **Fuentes**: DejaVu Sans Mono (licencia Bitstream Vera + dominio público), la que ya trae el motor. No es CC0 ni CC BY, como pide el encargo para lo de fuera; es libre y permite redistribuir. **Para ti**: dejarla (recomendado) o cambiarla por una CC0.
- **jlayer y jorbis** (decodificadores MP3/OGG, licencia LGPL) llegan con el lanzador de escritorio de libGDX. Probé a quitarlos (el juego solo usa WAV), pero sin ellos libGDX no arranca el audio. Los dejo; no son GPL. **Para ti**: si quieres quitarlos, haría falta un audio propio.
- **JUnit**: el laboratorio usa un mini-JUnit propio (`metajavbots.junit`: @Test, assertEquals…, mismos nombres que JUnit 5) en vez de JUnit 5 (EPL-2.0, no estaba en la lista de licencias). Cambiando el import, las pruebas son JUnit 5 de verdad.
- **Robots ajenos**: la caja de arena (Vigilante + Cargador + Tope) evita que un robot toque el ordenador sin querer, pero no es una caja fuerte contra alguien que lo intente a propósito; el juego avisa de abrir solo robots de gente de confianza.
- **Niveles y ritmo**: 12 escalones + Master (docs/NIVELES.md explica el porqué y el cálculo de horas). Los números finos (victorias por escalón, 70 % de ejercicios, robots por victorias, botín) son `TODO-FRAN` en `Progreso`.
- **Equilibrio**: en 82 peleas al azar entre modelos, Nimbo, Vigía, Bastión y Chatarra ganan poco y Eclipse mucho (ver la prueba «equilibrio»). Ninguno pasa del 25 %; los números de piezas y modelos son `TODO-FRAN`.
- **Modelo en línea por defecto**: `claude-haiku-5-5` (Anthropic) o `gpt-4o-mini`; se cambia en Ajustes.
- **{pieza} en los diálogos**: algunas frases en español ponen «{pieza} rota»; con «brazo derecho» queda «brazo derecho rota». Pendiente de pulir (TODO-FRAN si molesta).

## Qué falta (y en qué más puede ayudar la IA)

- **Probarlo jugando**: lo he visto en capturas de todas las pantallas y en muchas peleas sin ventana, pero no lo he jugado con ratón y teclado. Lo primero que miraría Fran: el tacto del editor, el ritmo de la pelea y lo legibles que son los textos.
- **Ventana de API separada**: el buscador va en un panel al lado del editor (más cómodo en un portátil); si se prefiere una ventana del sistema aparte, libGDX lo permite.
- **Más contenido**: lista de modelos, piezas y diálogos (TODO-FRAN), más ejercicios del laboratorio, más jefes.
- **Ideas de IA que no metí** (apuntadas para Fran): que el tutor proponga el siguiente ejercicio según los fallos; que la IA genere diálogos nuevos de cada personalidad entre peleas (y se guarden, moderados); un «modo profesor» que comente en voz los movimientos del jugador; que la IA lea el historial y proponga un plan de estudio semanal; resúmenes de cada tema en lenguaje sencillo bajo demanda; que la IA cree jefes nuevos a partir de un tema del temario.
- **Rendimiento**: no medido a fondo (la norma de fotogramas es de la Reliquia). Las peleas de 6 robots con muchas partículas van bien en las capturas.

## Detalles para quien siga

- Los formatos de todo el contenido: docs/FORMATOS.md. El comprobador de ejercicios sin Gradle: `pruebas/…/ComprobarEjercicios`.
- `gradlew` a veces dice «Failed to clean up stale outputs» justo después de cambiar algo de `res/` (OneDrive); al repetir, va.
- Cuando se cambie `Metajavbot.java`: `python herramientas/fachadas.py juego/src/main/java/metajavbots/api` y `python herramientas/api_doc.py`.
