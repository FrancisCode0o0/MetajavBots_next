# Revisión de estilo: resumen y dudas para Fran

Rama `revision_estilo`. Este documento cierra el encargo de revisión de estilo. Solo se han corregido ortografía, puntuación, terminología y presentación; el sentido, el humor, los nombres propios y el código de los robots siguen igual.

## Lo hecho

- **Parte 1**: la rama compila y pasa comprobación de estilo y pruebas; se arregló lo que faltaba del modo historia sin traer ese modo a la rama.
- **Parte 2**: fuera el botón «Paso a paso» de la barra de las peleas normales; sigue en el depurador con F9, con los textos reescritos en los tres idiomas.
- **Parte 3**: aplicada la segunda pasada de los 16 trozos `p2_trozo_*.json` (384 correcciones), comprobando antes el texto «antes».
- **Parte 4**: repaso texto a texto de los 41 trozos restantes (3385 correcciones); casi todas ya estaban aplicadas. Se aplicaron las que seguían vigentes y se descartaron 13 falsos positivos, con el motivo de cada uno en las dudas N1-N8 de abajo.
- **Parte 5**: `modo.campana`, `modo.jefe` y `arma_de.*` en los tres idiomas, y todos los plurales `{n:singular|plural}` con su `{n}` delante, según `docs/GLOSARIO.md`.
- **Parte 5b**: presentación del narrador «nombre, modelo X» (y su equivalente natural en alemán e inglés), nombre del robot visible y biografía una sola vez dentro del bocadillo; los botones con icono llevan un solo signo.
- **Parte 6**: prueba nueva `PruebasDeMedicionDeTextos` con `MedidorDeTextos` (fuentes reales medidas sin ventana) sobre tarjetas de `Historial`, fila de `SesionDeHoy` y título y pasos de `GuiaDeCerebros`, en ES/DE/EN con letra 18 y 26. Arreglo real de layout: la etiqueta alemana de `historial.dano_medio` desbordaba 4 px y ahora usa `cw - 12`. Detalle en `docs/proceso/informes/INFORME_REVISION_CAPTURAS_LETRA26.md` y en el propio commit.
- **Parte 7**: herramientas sueltas movidas a `material_revision/herramientas/`, dudas recogidas aquí y estado actualizado.

## Verificación

`.\gradlew.bat compileJava estilo pruebas:prueba` → **344 casos bien, 0 mal**, Checkstyle sin avisos, `BUILD SUCCESSFUL`.
Capturas para que Fran las mire, generadas con `.\gradlew.bat lwjgl3:capturas`: `build/capturas/` (letra normal), `build/capturas/de26/` y `build/capturas/en26/` (alemán e inglés con `-Pletra=26`). El detalle de la revisión visual anterior está en el informe citado arriba.

## Dudas nuevas de este tramo

Son las que aparecieron al aplicar la parte 4 y al revisar los informes visuales. La opción A es la que se ha dejado puesta; la B es la alternativa.

### N1 — res/textos/ui.txt:431 (`valor.recarga = recarga | Aufladung | recharge`)

- **Problema**: El JSON de la primera pasada pedía cambiar «= recarga |» por «= enfriamiento |» en la línea 418, pero ahí ya estaba `valor.enfriamiento`; el texto que quedaba era la clave `valor.recarga`, que es otra cosa.
- **Opciones**:
  - A: dejar las dos claves como están: `recarga` es la energía que se llena y `enfriamiento` la espera del arma (lo que dice `docs/GLOSARIO.md`)
  - B: renombrar también `valor.recarga` a `enfriamiento` y quitar la distinción

### N2 — res/textos/ui.txt:201 (`aviso.ordenes.titulo = Un consejo sobre tus órdenes | Ein Tipp zu deinen Befehlen | A tip about your orders`)

- **Problema**: El JSON pedía «Una pista | Ein Hinweis | A hint», pero el español dice «Un consejo»: cambiarlo alteraría el sentido, no la forma.
- **Opciones**:
  - A: dejar «consejo/Tipp/tip», que concuerda con las tres lenguas
  - B: pasar las tres a «pista/Hinweis/hint» y aceptar el cambio de sentido

### N3 — res/textos/ui.txt:1255 (`campanas.terminar_escena = Volver a Historia | Zurück zur Geschichte | Back to the story`)

- **Problema**: El JSON pedía «Seguir historia», pero el español dice «Volver»; están las tres lenguas de acuerdo.
- **Opciones**:
  - A: dejar «Volver a Historia / Zurück zur Geschichte / Back to the story»
  - B: cambiar las tres a «Seguir historia / Weiter zur Geschichte / Continue the story»

### N4 — res/dialogos/sonador.txt:166-168 (`Fiuuu…` / `Fiuuu …` / `Whoosh…`)

- **Problema**: La corrección alemana decía «Fiuuu…», pero en la línea señalada ya estaba «Fiuuu …»; la regla alemana pide espacio antes de los puntos suspensivos cuando falta una palabra entera, no cuando es un sonido.
- **Opciones**:
  - A: dejar «Fiuuu …» en alemán y «Fiuuu…» en español e inglés
  - B: quitar el espacio en alemán para igualar las tres

### N5 — res/datos/paquetes.txt:191 (`... im JUnit-5-Stil für Unit-Tests im Labor und in Übungen.`)

- **Problema**: El JSON quería añadir «de concurrencia» en alemán, pero el español dice solo «los ejercicios»; añadirlo en un idioma rompería la paridad.
- **Opciones**:
  - A: dejar «in Übungen» como está
  - B: añadir «de concurrencia / für Nebenläufigkeits-Übungen / in concurrency exercises» en los tres

### N6 — res/ejercicios/24_juego.txt:14-16, 99-101, 306-308

- **Problema**: El JSON pedía comillas alemanas en unas líneas donde el texto señalado estaba en `enunciado.es` (con comillas españolas «»); las líneas `enunciado.de` ya tenían „“.
- **Opciones**:
  - A: no tocar nada: cada idioma ya usa sus comillas
  - B: pasar también las comillas del `enunciado.es` a „“, con lo que el español dejaría de usar las suyas

### N7 — res/conceptos/07_juego.txt:1122 (`... circular saw "Sierrón" for the arm slot ...`)

- **Problema**: En el resumen en inglés, «Design your part» y «Save GIF» ya tenían comillas curvas; la única que sigue con comillas rectas es el nombre de la sierra «Sierrón», que podría considerarse un nombre de clase o de pieza.
- **Opciones**:
  - A: dejar «"Sierrón"» con comillas rectas, como nombre de pieza/code-ish
  - B: convertirlas a “Sierrón”, como el resto de la prosa en inglés

### N8 — res/datos/paquetes.txt:844 (`... to keep fights fair and reproducible and fast.`)

- **Problema**: La corrección inglesa sustituyó «deterministic and fast» por «fair and reproducible and fast» para cuadrar con el español; la frase queda con dos «and» seguidos. En alemán no se pudo hacer el mismo cambio sin romper «damit Kämpfe ... bleiben».
- **Opciones**:
  - A: dejar la frase actual
  - B: reescribirla en los tres idiomas, p. ej. «blocked in combat robots so fights stay fair, reproducible and fast» / «... damit Kämpfe fair, reproduzierbar und schnell bleiben» / «... para mantener los combates justos, reproducibles y rápidos»

### N9 — Pista de carrera, pantallas `09b_robot_cajas` y `09h_robot_bien` con letra 26

- **Problema**: Según `docs/proceso/informes/INFORME_REVISION_CAPTURAS_LETRA26.md`, asoman unos píxeles de descendentes bajo la barra verde; vienen del texto interno de `AnimacionDelCaso`.
- **Opciones**:
  - A: dejarlo: tocar la animación del robot queda fuera de esta revisión
  - B: recortar también esos descendentes dentro de la animación

### N10 — `CampoDeTexto` en `11_leccion`, `14_temario` y `24_entrenador` con letra 26

- **Problema**: Los textos guía largos («Pregúntale al tutor e Intro», «Ruta del archivo, o suéltalo en la...») se recortan por el recorte horizontal del campo; no desbordan la caja.
- **Opciones**:
  - A: dejarlo: es el comportamiento normal de un campo de texto
  - B: acortar los textos guía en los tres idiomas para que se vean enteros

### N11 — Botones y pestañas estrechos: `05_taller` («- Dame meno...»), `07_pelear` («Cerebros de los rivales:...»), pestañas de cinco niveles de `24b_entrenador_error`

- **Problema**: `Ui.boton` y `Ui.pestanas` ponen puntos suspensivos controlados dentro del control; no tapan ni se salen.
- **Opciones**:
  - A: dejar los puntos suspensivos
  - B: cambiar los textos o la interfaz para que no haga falta recortar

### N12 — Menú principal, dos descripciones de casilla en alemán con letra 26

- **Problema**: `docs/ESTADO.md` (sección CLARIDAD) apunta que quedan pegadas al borde, igual que en el menú de siempre; no se han tocado en esta revisión.
- **Opciones**:
  - A: dejarlo, porque es el diseño anterior y no se corta la lectura
  - B: reducir la letra o dar más hueco a esas dos casillas solo en alemán

### N13 — Títulos largos y campos de biografía de una línea

- **Problema**: Se han dejado a propósito los títulos que acaban en «…» y las biografías de una línea; la prueba headless solo marca los textos que se salen de su recuadro o se pisan.
- **Opciones**:
  - A: dejarlos como están
  - B: acortar textos o ampliar recuadros para que no aparezcan los puntos suspensivos

### N14 — Etiquetas «Averiado-N» en el borde de la pista

- **Problema**: Están en el límite de la pista, pero dentro de ella; no tapan ni se cortan. Se han dejado sin cambiar.
- **Opciones**:
  - A: dejarlas
  - B: separarlas un poco del borde

## Dudas heredadas de la revisión anterior

Las 115 dudas de los JSON de `material_revision/correcciones/` no se han aplicado: cambiarían el sentido, los nombres propios o una decisión de estilo que debe tomar Fran. Se agrupan por archivo; dentro de cada una, la opción A es la que propuso la revisión y la B la alternativa. Los números D1-D115 sirven para citarlas en el correo.

### `res/aparatos/aceite.txt`

#### D1 — de, línea 37 (en `p2_trozo_00.json`)

- **Texto**: `OELKANNE „Resbalín“ (también DUESENTRIEBWERK en chorro.txt:36; en cambio sierra.txt:37 dice SÄGE)`
- **Problema**: Los rótulos en mayúsculas copian el nombre de la clase (Oelkanne, Duesentriebwerk, Saege), pero SÄGE sí lleva Umlaut: incoherente. Igual en es: IMAN (iman.txt:3) sin tilde, como la clase Iman.
- **Opciones**:
  - A: tratarlos como nombre de clase: SAEGE (sierra.txt:37), y dejar OELKANNE, DUESENTRIEBWERK, IMAN
  - B: tratarlos como prosa: ÖLKANNE, DÜSENTRIEBWERK, SÄGE, IMÁN

### `res/aparatos/descarga.txt`

#### D2 — es, línea 4 (en `p2_trozo_00.json`)

- **Texto**: `a los enemigos a menos de 150 (también iman.txt:4-5 «a los enemigos», «proyectiles enemigos»; pegamento.txt:4 «el primer enemigo»; y sus versiones en: enemies/enemy)`
- **Problema**: El glosario pide «rival» en prosa y «enemigo» solo junto a la API; estos Javadoc son prosa, pero el código de al lado usa Enemigo/cerca(). martillo y sierra ya dicen «rival».
- **Opciones**:
  - A: dejar «enemigo/enemy» en los aparatos (texto pegado al código)
  - B: pasar a «rival(es)» / «Gegner» / «rival(s)» en descarga, iman y pegamento

### `res/aparatos/pegamento.txt`

#### D3 — es, línea 3 (en `p2_trozo_00.json`)

- **Texto**: `Dispara una pegota que cae a 220 (o en el primer enemigo de la línea): quien la pisa`
- **Problema**: «pegota» no está en el DLE (sí «pegote», masculino), aunque el nombre del aparato es «Pegotón». Puede ser invención jocosa.
- **Opciones**:
  - A: «Dispara un pegote que cae a 220 (…): quien lo pisa»
  - B: dejar «pegota» como palabra inventada

### `res/laboratorio/blackbox.txt`

#### D4 — en, línea 5 (en `p2_trozo_00.json`)

- **Texto**: `Blackbox tests: equivalence classes and boundaries (y línea 8 «Blackbox strategy»; whitebox.txt:5 y 8 «Whitebox tests», «Whitebox strategy»)`
- **Problema**: En inglés la grafía normal es «black-box» / «white-box» (adjetivo con guion). En de «Blackbox-Tests» y en es «Blackbox» como nombre del modo pueden quedarse.
- **Opciones**:
  - A: «Black-box tests», «Black-box strategy», «White-box tests», «White-box strategy»
  - B: dejar «Blackbox/Whitebox» como nombre propio del modo en los tres idiomas

### `res/conceptos/01_basico.txt`

#### D5 — es, línea 475 (en `p2_trozo_01.json`)

- **Texto**: `Un double es la báscula del pesaje: 87.3 kilos de chatarra orgullosa.`
- **Problema**: Decimal con punto en prosa (lo mismo en de, línea 480: «87.3 Kilo», y en en, línea 485: «87.3 kilos», donde el punto ya es correcto). Puede leerse como el valor del double (literal de Java) o como prosa (báscula).
- **Opciones**:
  - A: dejar «87.3» en es y de porque es el valor de un double
  - B: «87,3 kilos» en es y «87,3 Kilo» en de, como prosa

#### D6 — es, línea 80 (en `p2_trozo_01.json`)

- **Texto**: `pulsas el botón rojo de Compilar (con letras amarillas) en la barra superior del Taller (pestaña Código, o Ctrl+S)`
- **Problema**: REGLAS y el glosario piden nombrar los botones entre comillas del idioma o con [[…]]; aquí el botón va sin marcar (lo mismo en de, línea 85: «Button Kompilieren», y en en, línea 90: «Compile button»). Las pestañas (Código, Piezas) van sin comillas en todas las fichas de conceptos.
- **Opciones**:
  - A: dejarlo así, como el resto de las fichas
  - B: «el botón rojo [[Compilar]]» en es, „Kompilieren“ en de y “Compile” en en

#### D7 — es, línea 244 (en `p2_trozo_01.json`)

- **Texto**: `lo que está dentro pelea en ese combate y lo de fuera mira desde la grada`
- **Problema**: El glosario quiere «pelea» en lugar de «combate» fuera de frases hechas, pero aquí cambiarlo daría «pelea en esa pelea».
- **Opciones**:
  - A: dejar «combate» (evita la repetición)
  - B: «lo que está dentro pelea y lo de fuera mira desde la grada»

#### D8 — de, línea 23 (en `p2_trozo_01.json`)

- **Texto**: `Im Spiel ist dein Programm die Klasse MiRobot.`
- **Problema**: La plantilla alemana se llama MeinRoboter (res/plantillas/robot_de.java) y la inglesa MyRobot, y la ficha bloque ya lo usa (líneas 250 y 255). Aquí (de 23, en 29) y en compilar (de 85, en 90: «MiRobot.java», «MiRobot.class») se cita el nombre español. Es un identificador, así que no lo he tocado.
- **Opciones**:
  - A: dejar MiRobot en todas las fichas (nombre del ejemplo en Java, que está en español)
  - B: de MeinRoboter / MeinRoboter.java y en MyRobot / MyRobot.java en las líneas 23, 29, 85 y 90

#### D9 — es, línea 475 (en `trozo_01.json`)

- **Texto**: `87.3 kilos de chatarra orgullosa`
- **Problema**: Decimal con punto en prosa; la norma pide coma salvo valor de Java. Aquí ilustra un double (el párrafo anterior dice que en Java se escribe con punto). Lo mismo en de (línea 480, «87.3 Kilo»).
- **Opciones**:
  - A: dejar 87.3 (es el valor del double)
  - B: 87,3 kilos / 87,3 Kilo (prosa)

#### D10 — en, línea 428 (en `trozo_01.json`)

- **Texto**: `“becomes”, not “is equal to”.`
- **Problema**: Puntuación americana: comas y puntos irían dentro de las comillas (“becomes,” … “is equal to.”). El texto inglés pone siempre la puntuación fuera (estilo lógico) en todo el archivo (también 716, 717, 778, 900, 1035…).
- **Opciones**:
  - A: mantener la puntuación fuera (estilo lógico, coherente con el resto)
  - B: pasar a estilo americano en todo el juego

### `res/conceptos/02_control.txt`

#### D11 — en, línea 24 (en `p2_trozo_02.json`)

- **Texto**: `Before every lap it asks … / one more lap / does 10 laps / on every lap …`
- **Problema**: REGLAS dice que un recorrido de bucle es “iteration” (no “round”). El inglés usa “lap” en toda la ficha (líneas 24, 51, 80, 83, 100, 103, 128, 188, 314, 336, 337, 769, 883), calcando «vuelta». No choca con “round”, pero no es el término técnico.
- **Opciones**:
  - A: dejar “lap” (imagen cercana a «vuelta», no se confunde con la ronda)
  - B: cambiar a “iteration” en todas

#### D12 — de, línea 244 (en `p2_trozo_02.json`)

- **Texto**: `Sie dreht Runde um Runde und hört nie auf.`
- **Problema**: Es una frase hecha («dar vueltas y más vueltas»), pero usa «Runde», que REGLAS reserva para la ronda de pelea.
- **Opciones**:
  - A: dejarlo (modismo)
  - B: «Sie läuft und läuft und hört nie auf.»

#### D13 — en, línea 993 (en `p2_trozo_02.json`)

- **Texto**: `a 3.7-ton robot covered in little gadgets … the little gadgets (the decimals)`
- **Problema**: «gadget» es el término del glosario para los aparatos programables de la arena; aquí traduce «detallitos» (de «Schnickschnack»).
- **Opciones**:
  - A: dejarlo
  - B: “covered in frills … the frills (the decimals)”

#### D14 — es, línea 285 (en `p2_trozo_02.json`)

- **Texto**: `// sin fin a propósito... || absichtlich endlos... || endless on purpose...  (y línea 287: ...pero con salida segura)`
- **Problema**: Comentarios del código que ve el jugador con «...» de tres puntos. En código es lo habitual, pero REGLAS pide «…» en la prosa.
- **Opciones**:
  - A: dejar «...» (es código)
  - B: cambiar a «…» en los tres idiomas (líneas 285 y 287)

#### D15 — en, línea 27 (en `trozo_02.json`)

- **Texto**: `For "bit by bit", use an if in cadaTurno (onTick)`
- **Problema**: Puntuación con comillas: el inglés americano pone coma y punto dentro de las comillas (“bit by bit,”). El texto usa en todo el archivo la puntuación «lógica» (fuera), p. ej. también líneas 25, 129, 422, 1358. Se ha dejado como está y solo se han cambiado las comillas.
- **Opciones**:
  - A: dejar la puntuación fuera de las comillas (como ahora, coherente en todo el juego)
  - B: estilo americano: coma y punto dentro (“bit by bit,” / “…again.”) en todos los textos

#### D16 — en, línea 937 (en `trozo_02.json`)

- **Texto**: `"Ceeeee… for Cobalt!"`
- **Problema**: «Ceeeee» imita cómo suena la C en español; en inglés la C se pronuncia «see». El chiste puede no funcionar igual.
- **Opciones**:
  - A: dejar «Ceeeee…»
  - B: «Seeeee… C for Cobalt!»

### `res/conceptos/03_objetos.txt`

#### D17 — es, línea 1072 (en `p2_trozo_04.json`)

- **Texto**: `si le das a tu compañera una copia del mando, las dos controláis el [[mismo]] robot`
- **Problema**: «las dos» da por hecho que el jugador es mujer (de «ihr beide» y en «you both» son neutros).
- **Opciones**:
  - A: dejarlo («las dos»)
  - B: «las dos» → «los dos» (masculino genérico)
  - C: «tu compañero … los dos» (como el glosario: compañero de equipo)

#### D18 — de, línea 638 (en `trozo_03.json`)

- **Texto**: `das Liskovsche Substitutionsprinzip`
- **Problema**: Según la norma vigente (§ 62), los adjetivos en -sch derivados de nombres propios van en minúscula o con apóstrofo; aun así, «Liskovsches Substitutionsprinzip» es la forma habitual (es el título en la Wikipedia alemana).
- **Opciones**:
  - A: dejar «das Liskovsche Substitutionsprinzip» (uso establecido)
  - B: «das liskovsche Substitutionsprinzip» (norma)
  - C: «das Liskov’sche Substitutionsprinzip»

#### D19 — es/de/en, línea 1072 (en `trozo_04.json`)

- **Texto**: `las dos controláis el [[mismo]] robot / … Java pasa [[siempre]] por valor (l. 1073)`
- **Problema**: El español resalta con la marca [[…]]; el alemán (DENSELBEN, IMMER, l. 1078–1079) y el inglés (SAME, ALWAYS, l. 1084–1085) usan mayúsculas, y el resto del archivo resalta con mayúsculas también en español (ES, TIENE…). El resaltado no coincide entre idiomas.
- **Opciones**:
  - A: Dejarlo como está.
  - B: Usar mayúsculas también en español: «el MISMO robot», «pasa SIEMPRE por valor».
  - C: Usar [[…]] también en de/en: [[denselben]], [[immer]], [[same]], [[always]].

### `res/conceptos/04_intermedio.txt`

#### D20 — de, línea 395 (en `p2_trozo_05.json`)

- **Texto**: `        eine WARTESCHLANGE      (die Öldusche)`
- **Problema**: En el pseudocódigo, las otras tres opciones llevan verbo («eine LISTE nehmen», «eine MENGE nehmen», «eine MAP nehmen»; es «usar una COLA», en «use a QUEUE»), pero esta no. Añadir «nehmen» desplaza 2 caracteres la alineación de la columna de comentarios.
- **Opciones**:
  - A: «eine WARTESCHLANGE nehmen (die Öldusche)» (con verbo, la columna se desplaza 2 caracteres)
  - B: dejarlo así para conservar la alineación

#### D21 — de/en, línea 370 (en `p2_trozo_05.json`)

- **Texto**: `„Cobalto“ → sein Steckbrief / “Cobalto” → his file (y líneas 497, 502, 551, 556)`
- **Problema**: El modelo Cobalto se llama «Kobalt» en de y «Cobalt» en en según el glosario, pero en la prosa de estas fichas (colecciones, set, map) de y en lo dejan en español. En la 551 cambiarlo obligaría también a cambiar «Reiter C» por «Reiter K». En el código (Map.of("Cobalto", 3)) es un literal y se queda.
- **Opciones**:
  - A: traducir en la prosa: „Kobalt“ (y «Reiter K» en la 551) / “Cobalt”
  - B: dejar «Cobalto» en los tres idiomas como nombre del rival, coherente con el literal del código

#### D22 — de, línea 1260 (en `p2_trozo_06.json`)

- **Texto**: `O(1) dauert bei 10 Daten so lange wie bei einer Million`
- **Problema**: «10 Daten» como plural contable suena poco natural (es «10 datos», en «10 items»)
- **Opciones**:
  - A: dejarlo
  - B: «bei 10 Werten so lange wie bei einer Million»

#### D23 — es, línea 970 (en `p2_trozo_06.json`)

- **Texto**: `el NullPointerException salta en ese momento`
- **Problema**: Género de las excepciones vacila en los conceptos: «el/un NullPointerException» frente a «la/una ClassCastException» (y 717 «la ClassCastException»)
- **Opciones**:
  - A: dejarlo (el error)
  - B: «la NullPointerException salta» (la excepción), como «la ClassCastException»

#### D24 — es, línea 695 (en `trozo_05.json`)

- **Texto**: `// el más cerca, primero || der Nächste zuerst || closest first`
- **Problema**: Comentario del ejemplo Java (el jugador lo ve en su idioma): «el más cerca» es agramatical; debería ser «el más cercano». No lo corrijo porque las instrucciones dicen no tocar comentarios //.
- **Opciones**:
  - A: Corregir a «// el más cercano, primero»
  - B: Dejarlo (los comentarios de código no se revisan)

#### D25 — en, línea 1096 (en `trozo_06.json`)

- **Texto**: `Optional is meant for RETURN values. … orElse(compute()) ALWAYS computes`
- **Problema**: es y de usan versalita [[retorno]]/[[Rückgabewerte]] y [[siempre]]/[[immer]]; en usa mayúsculas. Lo mismo en 1321/1327/1333: es [[solo]], de NUR, en ONLY.
- **Opciones**:
  - A: igualar con [[…]] en las tres (en: [[return]] values, [[always]] computes; de: [[nur]], en: [[only]])
  - B: dejarlo como está (mayúsculas como énfasis)

#### D26 — es, línea 1025 (en `trozo_06.json`)

- **Texto**: `Para cinco enemigos, un for normal… / 1258: Con n pequeño (cinco enemigos)`
- **Problema**: El glosario pide «rival» en prosa y «enemigo» solo junto a la API; aquí se habla de los elementos de enemigosVistos (en: five enemies, de: fünf Gegner).
- **Opciones**:
  - A: cinco rivales / five rivals
  - B: dejar «enemigos» (se refiere a la lista enemigosVistos)

### `res/conceptos/05_moderno.txt`

#### D27 — de, línea 200 (en `p2_trozo_07.json`)

- **Texto**: `titulo.de: Pattern Matching (y «Pattern Matching heißt» en 213; «Record Patterns» en 86 y 215)`
- **Problema**: Según el Duden, los compuestos de sustantivos ingleses se escriben juntos o con guion («Pattern-Matching», «Record-Patterns»); en la literatura técnica alemana es muy frecuente con espacio.
- **Opciones**:
  - A: Dejarlo como está («Pattern Matching», «Record Patterns»), uso técnico habitual
  - B: «Pattern-Matching» / «Record-Patterns» en todo el archivo, norma Duden

#### D28 — de, línea 217 (en `p2_trozo_07.json`)

- **Texto**: `und Muster mit primitiven Typen sind seit Java 23 noch Preview (también «waren in Java 21 und 22 Preview» en 350 y «Sie waren in Java 19 und 20 Preview» en 740)`
- **Problema**: «sind … Preview» es jerga técnica coloquial; más cuidado sería «im Preview-Status» o «als Preview verfügbar».
- **Opciones**:
  - A: Dejarlo (jerga técnica comprensible)
  - B: «sind seit Java 23 noch im Preview-Status» / «waren in Java 21 und 22 im Preview-Status» / «waren in Java 19 und 20 im Preview-Status»

#### D29 — en, línea 89 (en `trozo_07.json`)

- **Texto**: `“Cobalt, x 120, y 340, tick 312”. Nobody corrects it`
- **Problema**: Puntuación con comillas: el texto inglés deja el punto y la coma fuera de las comillas (estilo lógico/británico); la norma estadounidense, que sigue el resto del inglés, los mete dentro. Afecta también a las líneas 27, 152, 220, 486, 680, 681 y 809. No lo he cambiado.
- **Opciones**:
  - A: dejar la puntuación fuera (estilo lógico, como está ahora)
  - B: puntuación dentro al estilo estadounidense (“…312.” / “works,”)

### `res/conceptos/06_senior.txt`

#### D30 — es|de|en, línea 134 (en `p2_trozo_08.json`)

- **Texto**: `// primero el dato... || erst die Daten ... || data first...  (y en la 135: ...luego la bandera || ... dann das Flag || ...then the flag)`
- **Problema**: Puntos suspensivos de tres puntos en comentarios del código de ejemplo que ve el jugador; las reglas piden «…» en prosa, pero es código (y la fuente monoespaciada puede no tener el glifo).
- **Opciones**:
  - A: dejar «...» por ser código
  - B: cambiar a «…» en los tres idiomas (de con espacio: «erst die Daten …», «… dann das Flag»)

#### D31 — en, línea 166 (en `p2_trozo_08.json`)

- **Texto**: `with a few carriers tied up, the program looked deadlocked without being so`
- **Problema**: «a few carriers tied up» sugiere «unos cuantos portadores ocupados»; la idea (es: «con pocos portadores ocupados», de: «Mit wenigen belegten Trägern») es que, como hay pocos portadores, al quedar todos clavados el programa parece colgado. Las tres versiones son algo ambiguas.
- **Opciones**:
  - A: dejarlo
  - B: en «with its few carriers all tied up»; es «con los pocos portadores ocupados»; de «Wenn die wenigen Träger belegt waren»

#### D32 — de, línea 1127 (en `p2_trozo_09.json`)

- **Texto**: `alle Teile der Marke Cobalto zusammen`
- **Problema**: «Cobalto» parece el modelo Cobalto, que el glosario llama «Kobalt» en alemán y «Cobalt» en inglés (línea 1133: «all Cobalto-brand parts»). Si es el modelo, debe ir con su nombre en cada idioma; si es una marca inventada, puede quedarse igual.
- **Opciones**:
  - A: dejar «Cobalto» en los tres idiomas (marca)
  - B: de «der Marke Kobalt» (1127) y en «all Cobalt-brand parts» (1133), como el modelo

#### D33 — de, línea 24 (en `trozo_08.json`)

- **Texto**: `Für Senioren:`
- **Problema**: «Senioren» en alemán significa «personas mayores», no «desarrolladores sénior»; aparece 23 veces en este archivo (líneas 24, 92, 160, 227, 295, 360, 423, 485, 552, 613, 681, 745 en este tramo) y en otros archivos se usa «Für Seniors:» (03_objetos), «Für Profis:» (04_intermedio), «Für Fortgeschrittene:» (05_moderno). Puede ser un chiste intencionado.
- **Opciones**:
  - A: dejar «Für Senioren:» (juego de palabras)
  - B: «Für Senior-Entwickler:» en todo el archivo
  - C: «Für Profis:» como en 04_intermedio

#### D34 — de, línea 1405 (en `trozo_09.json`)

- **Texto**: `falsch ← ein Schein-Gegner, 50 entfernt, am Boden`
- **Problema**: En el pseudocódigo alemán la variable «falsch» (también en 1406-1410) traduce «falso» como «incorrecto»; aquí significa «de mentira, simulado». Es un nombre de variable del pseudocódigo, por eso no lo corrijo directamente.
- **Opciones**:
  - A: dejar «falsch»
  - B: cambiarlo a «attrappe» (o «fake») en las líneas 1405-1410

### `res/datos/api.txt`

#### D35 — es, línea 1133 (en `p2_trozo_13.json`)

- **Texto**: `(en peleas normales hay 30 turnos por segundo hasta 1200)`
- **Problema**: Se puede leer como «30 turnos por segundo hasta llegar a 1200 por segundo»; una coma lo aclara. Lo mismo en de (1134) y en (1135).
- **Opciones**:
  - A: dejarlo así
  - B: «30 turnos por segundo, hasta 1200» / «30 Züge pro Sekunde, bis 1200» / “30 ticks per second, up to 1200”

#### D36 — es, línea 2156 (en `p2_trozo_14.json`)

- **Texto**: `apuntarHacia(o.angulo() + 90);`
- **Problema**: El ejemplo es (Obstaculo.velocidad) apunta la mira a o.angulo() + 90, mientras que de y en giran el cuerpo con drehenNach(o.richtung() + 90) / turnTo(o.heading() + 90). Es código, no lo toco, pero las tres versiones no hacen lo mismo.
- **Opciones**:
  - A: igualar el es a girarHacia(o.rumbo() + 90); como de y en
  - B: dejarlo como está

#### D37 — es, línea 1769 (en `p2_trozo_14.json`)

- **Texto**: `blindaje() solo devuelve el tuyo.`
- **Problema**: «solo» (adjetivo, «sin objeto») puede leerse como adverbio («únicamente devuelve el tuyo»). Las fichas vecinas dicen «sin prefijo» o «sin objeto».
- **Opciones**:
  - A: «blindaje() sin objeto devuelve el tuyo.»
  - B: dejarlo como está

### `res/datos/campanas.txt`

#### D38 — de, línea 209 (en `p2_trozo_15.json`)

- **Texto**: `Der Kranich-Tritt!`
- **Problema**: En la línea 208 el alemán dice «der Kran» (máquina, como «la grúa» del español, que juega con «la grulla» de la película) y en la 209 «Kranich-Tritt» (el pájaro, como en la película). Hay que elegir uno para las dos líneas.
- **Opciones**:
  - A: «Der Kran-Tritt!» en la 209 (mantiene el chiste de la grúa del español)
  - B: «der Kranich» en la 208 (la referencia conocida de la película)

#### D39 — es|en, línea 413 (en `trozo_15.json`)

- **Texto**: `Millón de Bitios Baby | Millionen-Bit-Baby | Million Bit Baby`
- **Problema**: Título de campaña con mayúsculas de título. Si es un título de obra, va en estilo frase («Millón de bitios baby», “Million bit baby”); si «Baby» se lee como apodo de la boxeadora, las mayúsculas pueden quedarse. He corregido «Acero de Saldo» y «Patadas a la Palmera», que no son nombres; «Toro Oxidado» lo dejo como apodo del boxeador.
- **Opciones**:
  - A: dejarlo como está (nombre propio/apodo)
  - B: «Millón de bitios baby» / “Million bit baby”

#### D40 — en, línea 663 (en `trozo_15.json`)

- **Texto**: `Secret Island: The 1000-Volt Tournament`
- **Problema**: Sentence case: si «1000-Volt Tournament» es el nombre propio del torneo (el texto lo repite así en 8.victoria), se queda; si no, “The 1000-volt tournament”. El español «Torneo de los 1000 Voltios» va igual con mayúsculas.
- **Opciones**:
  - A: nombre propio, se queda
  - B: “Secret Island: The 1000-volt tournament” y «Isla Clandestina: torneo de los 1000 voltios»

### `res/datos/referencia.txt`

#### D41 — de, línea 1756 (en `p2_trozo_19.json`)

- **Texto**: `ohne Zieltyp kann eine Lambda nicht allein stehen (también 1764 «Jede passende Lambda»)`
- **Problema**: Duden: «das Lambda» (neutro); el archivo usa «die Lambda» de forma coherente (1756, 1764, 1830, 1833, 1841, 1844, 2624), como abreviatura de «Lambda-Funktion».
- **Opciones**:
  - A: dejar «die Lambda» (coherente y habitual entre programadores)
  - B: pasar a neutro en todo el archivo: «ein Lambda», «jedes passende Lambda», «eines Lambdas»…

#### D42 — es, línea 1785 (en `trozo_19.json`)

- **Texto**: `Una condición sobre un valor. Se combinan con and, or y negate.`
- **Problema**: Sujeto singular («Una condición») y verbo plural sin sujeto explícito («Se combinan»); el en dice «Combine them».
- **Opciones**:
  - A: dejarlo así
  - B: «Se pueden combinar varias con and, or y negate.»

#### D43 — es, línea 2678 (en `trozo_20.json`)

- **Texto**: `en lugar de heredar de varias clases.`
- **Problema**: es dice «heredar de varias clases» (Java no permite herencia múltiple de clases); de y en dicen «en vez de jerarquías de herencia profundas». Las versiones no coinciden.
- **Opciones**:
  - A: cambiar es a «en lugar de crear jerarquías de herencia profundas» (coincide con de/en y es correcto en Java)
  - B: dejar es y cambiar de/en para que digan «heredar de varias clases»

### `res/aparatos/iman.txt`

#### D44 — es/de, línea 3 (en `trozo_00.json`)

- **Texto**: `IMAN «Imanator» / OELKANNE «Resbalín» / DUESENTRIEBWERK «Jetzazo»`
- **Problema**: Los encabezados en mayúsculas copian el nombre de la clase (sin tilde ni Umlaut: IMAN, OELKANNE, DUESENTRIEBWERK, ANTIGRAVITATIONSANTRIEB…), pero en sierra.txt el alemán ya pone SÄGE con Umlaut. Si son texto y no identificadores, faltan tilde/Umlaute.
- **Opciones**:
  - A: Dejarlos como nombre de clase en mayúsculas (IMAN, OELKANNE, DUESENTRIEBWERK).
  - B: Escribirlos como palabras: IMÁN (iman.txt l.3), ÖLKANNE (aceite.txt l.37), DÜSENTRIEBWERK (chorro.txt l.36).

### `res/laboratorio/punteria.txt`

#### D45 — en, línea 5 (en `trozo_00.json`)

- **Texto**: `titulo.en: Tests for the accuracy`
- **Problema**: «Tests for the accuracy» suena a calco; en inglés natural sería «Accuracy tests». Lo mismo, en menor grado, con «Tests for the trajectory» / «Tests for the armor» (se refieren a la clase).
- **Opciones**:
  - A: Dejarlo como está (paralelo a los demás títulos).
  - B: «Accuracy tests», «Trajectory tests», «Armor tests».

### `res/conceptos/07_juego.txt`

#### D46 — es, línea 315 (en `trozo_10.json`)

- **Texto**: `Tienda, monedas y rangos`
- **Problema**: El glosario manda «etiqueta» en vez de «rango», pero aquí (y en la línea 333, «cuatro rangos») son los niveles de la tienda (Noob, Junior, Senior, Especial), y «Especial» no es una etiqueta. de «Stufen» y en «ranks» coinciden con la etiqueta.
- **Opciones**:
  - A: dejar «rangos» para los niveles de la tienda
  - B: «Tienda, monedas y etiquetas» y «cuatro niveles» en la 333 (de «Stufen» / en «tiers»)

#### D47 — es, línea 365 (en `trozo_10.json`)

- **Texto**: `Depuración y Testing`
- **Problema**: El glosario propone «Depuración y pruebas / Debugging und Tests / Debugging and testing» (marcado como propuesta) porque «Testing» es anglicismo en es; el botón menu.depuracion aún dice «Depuración y Testing». Afecta a las líneas 365, 366, 373, 390, 393 (y de 374, 395).
- **Opciones**:
  - A: dejar «Testing» (solo cambiar «&» por «y/und/and», ya corregido)
  - B: «Depuración y pruebas» / «Debugging und Tests» / «Debugging and testing» en todas, igual que el botón cuando se cambie

#### D48 — de, línea 614 (en `trozo_11.json`)

- **Texto**: `Nach dem Kampf: „Code prüfen“`
- **Problema**: El glosario (punto 31) propone «Code-Review» para no chocar con el botón «Prüfen», pero el botón real (final.revisar) sigue diciendo «Code prüfen». Lo he dejado igual que el botón.
- **Opciones**:
  - A: dejar „Code prüfen“ mientras el botón no cambie
  - B: „Code-Review“ si se cambia final.revisar

#### D49 — es, línea 1178 (en `trozo_11.json`)

- **Texto**: `([[Creed]] ampliada e Isla Clandestina: Torneo de los 1000 Voltios)`
- **Problema**: La campaña se llama «Credo» en el glosario y en campanas.txt («Credo: el hijo del cromo»); el consejo dice [[Creed]] en los tres idiomas (no lo toco por ser marca [[…]], quizá enlace).
- **Opciones**:
  - A: dejar [[Creed]] (nombre de la parodia)
  - B: [[Credo]] en es (y quizá en de/en, como el título de la campaña)

#### D50 — en, línea 1074 (en `trozo_11.json`)

- **Texto**: `“Senior fights”`
- **Problema**: He puesto sentence case según el glosario, pero la etiqueta real (especial.senior) aún dice «Senior Fights». Si el botón no se cambia, la cita no coincide letra a letra.
- **Opciones**:
  - A: cambiar también especial.senior a “Senior fights”
  - B: dejar “Senior Fights” en las citas

#### D51 — es, línea 897 (en `trozo_11.json`)

- **Texto**: `Modelos, armas características y equilibrio`
- **Problema**: He cambiado «armas características» / «charakteristische Waffen» por el término del glosario (armas propias / eigene Waffen) en títulos y texto; las claves.* siguen con las dos formas (no se tocan).
- **Opciones**:
  - A: aplicar el glosario (como en el JSON)
  - B: dejar «armas características» si Fran lo prefiere en esta ficha

### `res/conceptos/08_mecanicas.txt`

#### D52 — de, línea 374 (en `trozo_12.json`)

- **Texto**: `titulo.de: Gewicht, Last und Beine (y en: «Weight, load and legs»; prosa de 387 «Gemessen wird die Last» / 392 «What counts is the load»)`
- **Problema**: El glosario pide «Last / load» → «Traglast / load limit» en mec_peso, pero en esta ficha «carga» es la proporción (peso ÷ lo que aguantan las piernas, proporcionDeCarga = lastanteil = loadRatio), no el límite (cargaMaxima = traglast = loadLimit). Cambiar la prosa a Traglast/load limit cambiaría el sentido.
- **Opciones**:
  - A: Dejar «Last / load» en título y prosa (es la proporción).
  - B: Título «Gewicht, Traglast und Beine» / «Weight, load limit and legs» y dejar la prosa de 387/392 como está.
  - C: Cambiar todo a Traglast / load limit como dice el glosario.

#### D53 — de, línea 713 (en `trozo_12.json`)

- **Texto**: `Im Kampf: Mit dem Visier (F8) siehst du das Radar …`
- **Problema**: El glosario propone que el visor táctico pase a «Taktikanzeige» para no chocar con «Visier» (la mira), pero ui.txt (pelea.visor) sigue diciendo «Visier». La ficha debe copiar el botón.
- **Opciones**:
  - A: Dejar «Visier» mientras el botón diga «Visier».
  - B: Cambiar a «Mit der Taktikanzeige (F8)» si se cambia también pelea.visor.

### `res/datos/modelos.txt`

#### D54 — es, línea 358 (en `trozo_16.json`)

- **Texto**: `La cadena va y viene. Tú, solo vas.`
- **Problema**: Coma entre sujeto y verbo (la RAE la censura), aunque aquí es una pausa expresiva.
- **Opciones**:
  - A: dejar «Tú, solo vas.» como pausa deliberada
  - B: «Tú solo vas.»

### `res/datos/piezas.txt`

#### D55 — es, línea 83 (en `trozo_17.json`)

- **Texto**: `Núcleo batería`
- **Problema**: Las demás piezas de núcleo llevan «de» (Núcleo de acero, Núcleo de rayo…); «Núcleo batería» en aposición puede ser intencionado (como «Cabeza turbo») o una errata.
- **Opciones**:
  - A: Dejar «Núcleo batería»
  - B: «Núcleo de batería»

### `res/datos/temas.txt`

#### D56 — es, línea 23 (en `trozo_20.json`)

- **Texto**: `Java moderno: enum, record, sealed y pattern matching`
- **Problema**: Anglicismo «pattern matching» en el título español; en la referencia se habla de «patrones» (switch con patrones, patrón de record).
- **Opciones**:
  - A: dejar «pattern matching» (término técnico habitual)
  - B: «Java moderno: enum, record, sealed y patrones»
  - C: «… y coincidencia de patrones»

### `res/dialogos/bravucon.txt`

#### D57 — de/en, línea 272 (en `trozo_20.json`)

- **Texto**: `¡Tu código tiene más bugs que un nido de avispas! | … als ein Ameisenhaufen! | … than an anthill!`
- **Problema**: es dice «nido de avispas»; de y en dicen «hormiguero». El chiste funciona en los dos casos.
- **Opciones**:
  - A: dejarlo así (cada idioma con su imagen)
  - B: de «als ein Wespennest», en «than a wasp nest» (coincide con es)

### `res/datos/robots_rotos.txt`

#### D58 — en, línea 9 (en `trozo_20.json`)

- **Texto**: `The Spinning Top / The Cross-Eyed One / The Shy One / The Forgetful One / The Deaf One / The Stuck One`
- **Problema**: Títulos de los robots rotos en Title Case. Si son apodos (nombres propios, como «El Tímido» en es), están bien; si son títulos, la regla pide sentence case.
- **Opciones**:
  - A: dejarlos como apodos con mayúsculas (recomendado)
  - B: sentence case: «The spinning top», «The shy one»…

### `res/dialogos/hiperactivo.txt`

#### D59 — es, línea 268 (en `trozo_21.json`)

- **Texto**: `¡Tu código se compila con dial-up, {rival}!`
- **Problema**: anglicismo «dial-up» en la línea española (el alemán dice «per Modem»); si se mantiene, iría en cursiva o entre comillas
- **Opciones**:
  - A: dejar «dial-up» (el chiste se entiende)
  - B: «¡Tu código se compila por módem, {rival}!»

### `res/dialogos/cerebrito.txt`

#### D60 — de, línea 214 (en `trozo_21.json`)

- **Texto**: `Q. E. D.: Was zu beweisen war.`
- **Problema**: tras los dos puntos no sigue una oración principal completa («was zu beweisen war» es una relativa libre); según Duden iría en minúscula
- **Opciones**:
  - A: dejar «Was» (se lee como fórmula independiente)
  - B: «Q. E. D.: was zu beweisen war.»

### `res/dialogos/locutor.txt`

#### D61 — de, línea 15 (en `trozo_22.json`)

- **Texto**: `de: Piep piep! Meine Schaltkreise qualmen! Der Kampf des Jahrhunderts beginnt!`
- **Problema**: El español dice «el combate del siglo veintiuno y medio» (chiste); de y en (l. 15-16) usan solo la frase hecha «Kampf des Jahrhunderts» / «fight of the century» y pierden el «y medio».
- **Opciones**:
  - A: Dejarlo así (adaptación con la frase hecha)
  - B: de: «Der Kampf des einundzwanzigeinhalbten Jahrhunderts beginnt!» / en: «The fight of the twenty-first-and-a-half century begins!»
  - C: Simplificar el español a «¡Empieza el combate del siglo!»

### `res/dialogos/jugueton.txt`

#### D62 — de, línea 154 (en `trozo_22.json`)

- **Texto**: `de: Tschüss, {pieza}. Für dich ist Game Over.`
- **Problema**: «Game Over» (también l. 227): el Duden registra «Game-over» como sustantivo; en pantalla de videojuego es habitual «Game Over».
- **Opciones**:
  - A: Dejar «Game Over» (rótulo de juego)
  - B: Duden: «Für dich ist Game-over.» / «Game-over …»

### `res/dialogos/teatral.txt`

#### D63 — es, línea 77 (en `trozo_23.json`)

- **Texto**: `¡Quién ha movido el decorado!`
- **Problema**: de/en lo plantean como pregunta exclamada («?!»); en es es una exclamación sin signos de interrogación. Ambas son válidas.
- **Opciones**:
  - A: dejarlo («¡Quién ha movido el decorado!»)
  - B: «¡¿Quién ha movido el decorado?!», igual que de/en

### `res/ejercicios/07_arrays.txt`

#### D64 — es, línea 508 (en `trozo_24.json`)

- **Texto**: `genera el array {1, 1, 2, 1, 2, 3, ..., 1, 2, ..., n}`
- **Problema**: Puntos suspensivos «...» dentro de una notación de array (pseudocódigo). En prosa matemática el mismo tramo usa «…» (05_bucles:38 «1 + 2 + … + n»). Afecta igual a las líneas 509 (de) y 510 (en).
- **Opciones**:
  - A: Dejar «...» por ser notación tipo Java
  - B: Cambiar a «…» en las tres líneas (508, 509, 510) por coherencia con 05_bucles:38

#### D65 — de, línea 584 (en `trozo_24.json`)

- **Texto**: `Sammle dann links in einer Schleife bis length - 1 und prüfe, ob links == total - links gilt.`
- **Problema**: La variable de la solución se llama izquierda; de y en (línea 585: «left == total - left») la traducen a links/left, mientras que en el resto de ejercicios los nombres de variable se dejan en español. En alemán, además, «Sammle dann links» se lee como «recoge a la izquierda».
- **Opciones**:
  - A: Dejarlo así (nombre conceptual traducido)
  - B: de: «Summiere dann izquierda in einer Schleife bis length - 1 auf und prüfe, ob izquierda == total - izquierda gilt.»; en: «Then accumulate izquierda in a loop up to length - 1 and check whether izquierda == total - izquierda.»

### `res/ejercicios/08_textos.txt`

#### D66 — es, línea 609 (en `trozo_25.json`)

- **Texto**: `titulo.es: Sumar códigos ocultos en el informe`
- **Problema**: El título español dice «códigos» y el alemán y el inglés dicen «Zahlen» / «numbers»; el ejercicio suma números.
- **Opciones**:
  - A: Cambiar el español a «Sumar números ocultos en el informe» (como de/en y el enunciado)
  - B: Dejar «códigos» y cambiar de/en a «Versteckte Codes im Bericht summieren» / «Sum hidden codes in the report»

### `res/ejercicios/10_encapsulacion.txt`

#### D67 — de, línea 191 (en `trozo_25.json`)

- **Texto**: `titulo.de: Die Kreditbörse`
- **Problema**: «Kreditbörse» se lee como «bolsa/mercado de créditos (préstamos)»; «Kredit» en alemán es préstamo, no los créditos del juego.
- **Opciones**:
  - A: «Der Credit-Geldbeutel»
  - B: «Die Geldbörse für Credits»
  - C: Dejarlo

### `res/ejercicios/11_herencia.txt`

#### D68 — de, línea 986 (en `trozo_26.json`)

- **Texto**: `titulo.de: Prämienkonto mit Gebühr`
- **Problema**: El glosario fija premio = Belohnung; aquí «premios» es el dinero del club de un ejercicio, no el premio del juego. En inglés ya dice «Prize account».
- **Opciones**:
  - A: dejar «Prämienkonto mit Gebühr»
  - B: «Belohnungskonto mit Gebühr» (término del glosario)

#### D69 — de, línea 685 (en `trozo_26.json`)

- **Texto**: `vergleicht zusätzlich das Niveau`
- **Problema**: «Niveau» para el nivel (Mk) de una pieza suena raro; «Stufe» sería lo natural, pero el glosario reserva «Stufe» para la etiqueta del jugador.
- **Opciones**:
  - A: dejar «das Niveau»
  - B: «die Stufe»
  - C: «das Level»

### `res/ejercicios/14_excepciones.txt`

#### D70 — de/en, línea 228 (en `trozo_27.json`)

- **Texto**: `Mensajes de excepción de las soluciones sin entrada en terminos.de/terminos.en: "falta el nombre", "nombre vacio", "vida fuera de rango: " (t14_validar_robot); "hay que cargar al menos una bala", "no caben mas de ", "torreta vacia" (t14_torreta); "mensaje vacio" (t14_canal_recursos); "hace falta al menos un intento" (t14_reintentos); "lote nulo", "rango invalido" y el comentario "// se ignora la lectura corrupta" (t14_lote_de_baterias)`
- **Problema**: Si el jugador alemán o inglés ve la solución, estos textos le salen en español (los casos no los comprueban). En t14_validar_robot la pista.de/en ya traduce "falta el nombre" ("Name fehlt" / "missing name"), pero la solución no.
- **Opciones**:
  - A: Añadir sus líneas en terminos.de y terminos.en
  - B: Dejarlo así (no afectan a los casos)

### `res/ejercicios/16_genericos.txt`

#### D71 — de/en, línea 188 (en `trozo_27.json`)

- **Texto**: `"la lista está vacía" (t16_maximo_acotado), "tamaño no válido: " (t16_en_trozos), "la pila está vacía" (t16_pila_generica)`
- **Problema**: Mensajes de excepción de las soluciones sin terminos.de/terminos.en.
- **Opciones**:
  - A: Añadir sus líneas en terminos.de y terminos.en
  - B: Dejarlo así

### `res/ejercicios/20_hilos.txt`

#### D72 — de|en, línea 140 (en `trozo_28.json`)

- **Texto**: `/** Ya hecho: lanza «hilos» hilos que suman «veces» cada uno, espera a todos y devuelve el total. */`
- **Problema**: Comentario que ve el jugador en la plantilla y la solución (líneas 140, 174) sin entrada en terminos.de / terminos.en: en alemán e inglés se mostrará en español. No se corrige con un cambio de línea; hay que añadir terminos.
- **Opciones**:
  - A: Añadir a terminos.de «/** Schon fertig: Startet „hilos“ Threads, die je „veces“-mal addieren, wartet auf alle und gibt die Summe zurück. */» y a terminos.en «/** Already done: starts “hilos” threads that each add “veces” times, waits for all of them and returns the total. */»
  - B: Dejarlo en español

#### D73 — de|en, línea 397 (en `trozo_28.json`)

- **Texto**: `/** Ya hecho: el daño de un arma; si la potencia es negativa, el arma está averiada. */`
- **Problema**: Comentario que ve el jugador en la plantilla y la solución (líneas 397, 419) sin traducción en terminos.de / terminos.en (que sí traducen los literales de ese ejercicio).
- **Opciones**:
  - A: Añadir a terminos.de «/** Schon fertig: der Schaden einer Waffe; ist die Leistung negativ, ist die Waffe defekt. */» (con «Der» si se prefiere mayúscula tras los dos puntos) y a terminos.en «/** Already done: the damage of a weapon; if the power is negative, the weapon is broken. */»
  - B: Dejarlo en español

### `res/ejercicios/22_diseno.txt`

#### D74 — de, línea 12 (en `trozo_29.json`)

- **Texto**: `vida <= 0 "ausser gefecht"`
- **Problema**: El valor que devuelve el programa en alemán (terminos.de, línea 59: «fuera de combate ==> ausser gefecht»; también en pista.de, línea 15) está escrito con «ss» y en minúscula: en alemán es «außer Gefecht». Es un literal del programa, así que no lo corrijo yo: hay que cambiar a la vez terminos.de y las líneas 12 y 15.
- **Opciones**:
  - A: dejar "ausser gefecht" (valor del programa en ASCII, a propósito)
  - B: cambiarlo a "außer Gefecht" en terminos.de (l. 59) y en las líneas 12 y 15

### `res/ejercicios/23_retos.txt`

#### D75 — en, línea 118 (en `trozo_29.json`)

- **Texto**: `Challenge: dodge to the right side`
- **Problema**: «right side» se lee como «el lado derecho», justo en un ejercicio que elige entre girar a izquierda o a derecha; el español dice «el lado bueno». (Aparte, «Challenge» pasa a «Task» en mis correcciones.)
- **Opciones**:
  - A: dejarlo («right» = correcto, juego de palabras)
  - B: «Task: dodge to the better side»
  - C: «Task: dodge to the safe side»

### `res/ejercicios/24_juego.txt`

#### D76 — es, línea 99 (en `trozo_29.json`)

- **Texto**: `el enemigo está hacia «haciaEnemigo»`
- **Problema**: El glosario pide «rival / Gegner / rival» en la prosa y deja «enemigo / enemy» solo en el pseudocódigo y junto a la API que lo lleva. En los ejercicios, «enemigo / enemy» sale en la prosa al lado de variables que lo llevan (haciaEnemigo, rumboEnemigo, velocidadEnemigo, enemigos, el carácter 'E'): 21_recursion.txt 419-424 (título incluido); 23_retos.txt 13, 15, 64, 66, 69; 24_juego.txt 99, 101, 154, 156, 180, 182, 214, 216, 217, 219. Y en el mismo archivo 24_juego.txt el título de la línea 126 ya dice «rival». El alemán dice siempre «Gegner», así que no cambia.
- **Opciones**:
  - A: dejar «enemigo / enemy» en los ejercicios porque va junto a variables que lo llevan
  - B: pasar a «rival» (es y en) en la prosa y en los títulos, sin tocar los identificadores

### `res/especiales/retos.txt`

#### D77 — es, línea 113 (en `trozo_30.json`)

- **Texto**: `debes tener al menos coste de energia`
- **Problema**: «energia» sin tilde: si es el sustantivo, falta la tilde; si es la variable, la frase no se entiende como tal (de/en dicen «tu energia debe ser al menos coste»).
- **Opciones**:
  - A: «debes tener al menos coste de energía» (sustantivo, con tilde)
  - B: «tu energia debe ser al menos coste» (variable, como de/en)

### `res/historia/historia_en.ink`

#### D78 — en, línea 127 (en `trozo_30.json`)

- **Texto**: `Today it is wax on, bolt off. Right hand, wax. Left hand, bolt.`
- **Problema**: El título del episodio en historia.txt es «Wax on, polish nut» y el español habla de «pulir tuerca» y «tuerca»; aquí se usa «bolt» (otra pieza) y el juego de palabras «bolt off».
- **Opciones**:
  - A: dejarlo (el chiste «wax on, bolt off» funciona en inglés)
  - B: «Today it is wax on, polish nut. Right hand, wax. Left hand, nut.» (coherente con el título y el español)

#### D79 — en, línea 31 (en `trozo_30.json`)

- **Texto**: `You are riding… / I am {nino}… / That is because I never have. / Do not call me that!`
- **Problema**: Todo el guion inglés evita las contracciones (I am, do not, that is, it is…). En diálogo cómico suena rígido y poco natural; en algún personaje (el niño repelente, el licenciado) puede ser un rasgo buscado.
- **Opciones**:
  - A: dejarlo así (estilo uniforme, rasgo «robótico»)
  - B: usar contracciones en el diálogo (I'm, don't, that's, it's…) salvo en el niño y el licenciado, en una pasada aparte

### `res/historia/historia_es.ink`

#### D80 — es, línea 209 (en `trozo_30.json`)

- **Texto**: `plastificadas y ordenadas por orden de manía`
- **Problema**: Pleonasmo «ordenadas por orden» (salvo que sea un chiste buscado).
- **Opciones**:
  - A: dejarlo
  - B: «plastificadas y clasificadas por orden de manía»

#### D81 — es, línea 50 (en `trozo_30.json`)

- **Texto**: `pulsa Compilar / pulsas Comprobar / le doy a Jugar / pulsas Continuar / pulsas Iniciar`
- **Problema**: Los nombres de botones van a veces sin comillas (Compilar, Comprobar, Jugar, Continuar, Repetir, Iniciar) y otras con ellas («Compilar y validar», «Guardar y montar», «Jugar» en la línea 185). Las REGLAS piden comillas para nombrar botones. Lo mismo en de/en.
- **Opciones**:
  - A: dejar sin comillas los botones de una palabra (se distinguen por la mayúscula)
  - B: entrecomillarlos todos en los tres idiomas («Compilar», „Kompilieren“, “Compile”)

### `res/textos/errores.txt`

#### D82 — es, línea 221 (en `trozo_31.json`)

- **Texto**: `(el máximo es 2147483647)`
- **Problema**: Número de 10 cifras en prosa sin separador de miles (REGLAS: desde 5 cifras, con espacio). Pero es el valor de Integer.MAX_VALUE tal como se escribe en Java; lo mismo en de (l. 222) y en (l. 223).
- **Opciones**:
  - A: dejar 2147483647 en los tres (es el valor que se teclea en el código)
  - B: es «2 147 483 647», de «2 147 483 647», en «2,147,483,647»

#### D83 — en, línea 1235 (en `trozo_31.json`)

- **Texto**: `Blocked by the sandbox: {msg}`
- **Problema**: es dice «Prohibido por el vigilante» y de «Vom Wächter verboten» (el componente Vigilante); en habla de «sandbox», que en el glosario es «caja de arena».
- **Opciones**:
  - A: dejar “Blocked by the sandbox”
  - B: “Blocked by the guard: {msg}” para que coincida con es/de

#### D84 — es, línea 1263 (en `trozo_31.json`)

- **Texto**: `el juego corre sobre un JRE`
- **Problema**: «correr» por «ejecutarse» (calque de “runs”), aunque es habitual en informática.
- **Opciones**:
  - A: dejarlo
  - B: «el juego se ejecuta en un JRE»

### `res/textos/ui.txt`

#### D85 — es, línea 300 (en `trozo_32.json`)

- **Texto**: `[[¡Bajada de nivel!]] / has bajado de nivel / gana peleas en tu nivel / te ponen en tu nivel (líneas 9, 36, 300, 301)`
- **Problema**: El español usa «nivel» para la etiqueta del jugador; el glosario solo admite «subir de nivel» como frase hecha y he cambiado el inglés a «rank». ¿Se pasa también el español a «etiqueta»?
- **Opciones**:
  - A: dejar «nivel» en español (frase hecha)
  - B: «¡Bajada de etiqueta!», «has bajado de etiqueta», «en tu etiqueta»

#### D86 — es, línea 285 (en `trozo_32.json`)

- **Texto**: `final ({0}): +{1} % pasos de cálculo por turno`
- **Problema**: Al añadir «de cálculo» (glosario) la línea de los modificadores POO se alarga; si no cabe en el panel, habría que abreviar.
- **Opciones**:
  - A: dejar la forma larga
  - B: volver a «pasos por turno» solo en poo.final

#### D87 — de, línea 308 (en `trozo_32.json`)

- **Texto**: `Code-Review`
- **Problema**: El glosario da «Code-Review» como propuesta para no confundir con el botón «Prüfen»; hay que cambiar a la vez revision.titulo, tarea.pesado y guia.10.pasos (fuera de mi tramo).
- **Opciones**:
  - A: «Code-Review»
  - B: dejar «Code prüfen»

#### D88 — es, línea 134 (en `trozo_32.json`)

- **Texto**: `Catálogo Especial bloqueado`
- **Problema**: «Especial» con mayúscula como nombre de la pestaña del catálogo; en estilo frase sería «Catálogo especial».
- **Opciones**:
  - A: dejar «Catálogo Especial» (nombre de la pestaña)
  - B: «Catálogo especial»

#### D89 — es, línea 683 (en `trozo_33.json`)

- **Texto**: `Los errores de compilación se explican según tu nivel`
- **Problema**: Se refiere a la etiqueta del jugador (Noob, Senior); el glosario manda «etiqueta» (en ya corregido a «rank»), pero «según tu nivel» es natural en español.
- **Opciones**:
  - A: dejar «según tu nivel»
  - B: «según tu etiqueta»

#### D90 — de, línea 514 (en `trozo_33.json`)

- **Texto**: `Ya en plantilla | Schon in Vorlage | Already in template`
- **Problema**: Etiqueta telegráfica sin artículo en los tres idiomas; en de y en suena incompleta.
- **Opciones**:
  - A: dejarla así (etiqueta corta)
  - B: «Ya en la plantilla» / «Schon in der Vorlage» / «Already in the template»

#### D91 — es|de|en, línea 1064 (en `trozo_34.json`)

- **Texto**: `[[¡¡Sííí!!]] | [[Jaaaa!!]] | [[Yesss!!]]`
- **Problema**: Signos duplicados (¡¡ !!), que REGLAS prohíbe; aquí es el grito del público y parece intencionado para distinguirlo de publico.bien.
- **Opciones**:
  - A: dejarlo así (onomatopeya expresiva)
  - B: [[¡Sííí!]] | [[Jaaaa!]] | [[Yesss!]]

### `res/tutoriales/02_variables.txt`

#### D92 — en, línea 33 (en `trozo_34.json`)

- **Texto**: `That's why step = step + 10; makes perfect sense`
- **Problema**: El código de ejemplo (línea 19) declara «double paso», y es/de dicen «paso = paso + 10;», pero en traduce el nombre de la variable a «step», que no aparece en el código que ve el jugador.
- **Opciones**:
  - A: dejar «step = step + 10;» (si el código se muestra traducido en inglés)
  - B: «paso = paso + 10;», como el código

### `res/tutoriales/05_bucles.txt`

#### D93 — de, línea 124 (en `trozo_35.json`)

- **Texto**: `titulo.de: Mindestens einmal, und Notausgänge`
- **Problema**: Coma ante «und» que no pide la norma alemana; copia la pausa del título español («Al menos una vez, y salidas de emergencia»), que también es discutible.
- **Opciones**:
  - A: dejar la coma en los dos títulos (pausa de estilo)
  - B: quitarla en de («Mindestens einmal und Notausgänge»)
  - C: quitarla en de y en es («Al menos una vez y salidas de emergencia»)

### `res/tutoriales/12_polimorfismo.txt`

#### D94 — es, línea 117 (en `trozo_35.json`)

- **Texto**: `«veo al enemigo» pasa de BUSCAR a ATACAR`
- **Problema**: Glosario: en prosa «rival», pero aquí es la señal entre comillas de una máquina de estados, casi pseudocódigo (en: “I see the enemy”).
- **Opciones**:
  - A: dejar «enemigo» / “enemy” (es una señal, como en el pseudocódigo)
  - B: cambiar a «veo al rival» / “I see the rival”

### `res/tutoriales/13_interfaces.txt`

#### D95 — de, línea 64 (en `trozo_35.json`)

- **Texto**: `titulo.de: Das Zielen im Lauf wechseln`
- **Problema**: «im Lauf» calca «en marcha» y además choca con «Lauf» = carrera (Hindernislauf). He propuesto «im laufenden Kampf» (título) e «im laufenden Betrieb» (línea 111); Fran puede preferir otra fórmula.
- **Opciones**:
  - A: «im laufenden Kampf» / «im laufenden Betrieb» (como en las correcciones)
  - B: «unterwegs» en las dos
  - C: «mitten im Kampf» en las dos

### `res/tutoriales/15_colecciones.txt`

#### D96 — es, línea 127 (en `trozo_36.json`)

- **Texto**: `(las flechas :: las verás en el tema 17)`
- **Problema**: «::» no son flechas (la flecha es ->); de y en dicen solo «::».
- **Opciones**:
  - A: (los :: los verás en el tema 17)
  - B: (las referencias con :: las verás en el tema 17)
  - C: dejarlo

### `res/tutoriales/21_recursion.txt`

#### D97 — es, línea 104 (en `trozo_36.json`)

- **Texto**: `Geometría de combate: radar y esquiva`
- **Problema**: El glosario cambia «combate» por «pelea» y “combat” por “fight”, salvo frases hechas. «Geometría de combate» / “Combat geometry” suena a término fijo; «Geometría de pelea» suena raro.
- **Opciones**:
  - A: dejar «Geometría de combate» / „Kampfgeometrie“ / “Combat geometry”
  - B: «Geometría de la pelea» / „Kampfgeometrie“ / “Fight geometry”

### `README.md`

#### D98 — es, línea 67 (en `trozo_37.json`)

- **Texto**: `desafíos con modo azar, el desafío, battle royale, …`
- **Problema**: «el desafío» aparece como modo dentro de la lista de «desafíos»: redundante o confuso. ¿Es otro modo (p. ej. «el reto» o «Destrucción»)?
- **Opciones**:
  - A: dejarlo así
  - B: sustituir «el desafío» por el nombre real del modo (p. ej. «el reto»)
  - C: quitar «el desafío»

#### D99 — es, línea 89 (en `trozo_37.json`)

- **Texto**: `https://github.com/ (enlace del repositorio)`
- **Problema**: La cita tiene un hueco en lugar del enlace real; las citas en alemán (142) e inglés (195) no llevan enlace.
- **Opciones**:
  - A: poner https://github.com/FrancisCode0o0/MetajavBots_next en las tres citas
  - B: dejarlo como marcador hasta publicar

#### D100 — de, línea 116 (en `trozo_37.json`)

- **Texto**: `Secciones Deutsch y English (93-195)`
- **Problema**: Las versiones alemana e inglesa no traducen el párrafo «Qué hay» (67), el de «Cerebro de IA (opcional)» (69) ni el ejemplo de código; son resúmenes. No lo añado por ser párrafos enteros.
- **Opciones**:
  - A: dejar de y en como resumen
  - B: traducir los párrafos «Qué hay» y «Cerebro de IA» al alemán y al inglés

### `docs/LICENCIA_CONTENIDO.md`

#### D101 — de, línea 11 (en `trozo_37.json`)

- **Texto**: `stehen unter **CC BY 4.0** (…)`
- **Problema**: El español da el nombre completo («Creative Commons Atribución 4.0 Internacional») y un ejemplo de atribución (línea 7); de y en no.
- **Opciones**:
  - A: dejarlo así
  - B: añadir „Creative Commons Namensnennung 4.0 International“ / “Creative Commons Attribution 4.0 International” y el ejemplo traducido

### `juego/src/main/java/metajavbots/campanas/Campana.java`

#### D102 — es, línea 178 (en `trozo_38.json`)

- **Texto**: `«En esta isla cada eliminatoria es un bonsái eléctrico: poda lo que sobra y vencerás.» (y las demás frases de entrenador de Campana.java, líneas 178-312)`
- **Problema**: Las frases de esquina van enteras entre «» con el punto final dentro («…vencerás.»). La RAE pone el punto después de las comillas de cierre («…vencerás».); ? y ! sí van dentro. En alemán e inglés el punto dentro es correcto.
- **Opciones**:
  - A: dejarlo así (cita aislada, como en muchos juegos)
  - B: mover el punto fuera en español en todas: «…vencerás».

### `juego/src/main/java/metajavbots/campanas/CatalogoDeEscenas.java`

#### D103 — de, línea 59 (en `trozo_38.json`)

- **Texto**: `Vorsicht vor meinem {pieza}! / Heute mache ich Püree aus dem {pieza}! (línea 129) / Lass meinen {pieza} los (línea 228)`
- **Problema**: El artículo alemán (meinem/dem/meinen) es masculino o neutro, pero {pieza} puede ser un nombre femenino (p. ej. «Leichte Photonklinge»): saldría «meinem Leichte Photonklinge».
- **Opciones**:
  - A: dejarlo así
  - B: evitar el artículo con el nombre entre comillas: „Vorsicht vor meinem Teil „{pieza}“!“ → «Vorsicht vor meinem Teil {pieza}!», «Püree aus deinem Teil {pieza}», «Lass mein Teil {pieza} los»

### `juego/src/main/java/metajavbots/pantallas/Jefe.java`

#### D104 — es, línea 112 (en `trozo_38.json`)

- **Texto**: `"El Atrapa-errores"`
- **Problema**: Lo he corregido a «El Atrapaerrores» (compuesto que se escribe junto, como «atrapamoscas»), pero es nombre propio de un jefe: si el guion es a propósito, se deja.
- **Opciones**:
  - A: «El Atrapaerrores» (norma)
  - B: «El Atrapa-errores» (forma original)

### `juego/src/main/java/metajavbots/ejercicios/Demo4Arquitectura.java`

#### D105 — es/en, línea 26 (en `trozo_39.json`)

- **Texto**: `Demo 4: POO en combate, aparatos, energía y tope de cálculo / OOP in combat (también línea 60 y 62)`
- **Problema**: El glosario quiere «pelea / fight» y deja «combate» solo en frases hechas; «en combate» casi lo es.
- **Opciones**:
  - A: dejar «POO en combate» / «OOP in combat»
  - B: «POO en la pelea» / «OOP in the fight» (y «OOP im Kampf» se queda)

### `juego/src/main/java/metajavbots/ejercicios/Demostracion.java`

#### D106 — es/en, línea 311 (en `trozo_39.json`)

- **Texto**: `¡Enemigo a la vista! / Enemy spotted!`
- **Problema**: Título en prosa con «enemigo» (el glosario pide «rival»), pero es el paso que presenta alVerEnemigo y «enemigo a la vista» es frase hecha.
- **Opciones**:
  - A: dejarlo
  - B: «¡Rival a la vista!» / «Rival spotted!»

### `juego/src/main/java/metajavbots/ejercicios/Demo5PrimeraPelea.java`

#### D107 — es, línea 132 (en `trozo_39.json`)

- **Texto**: `Antes de pulsar «Continuar»`
- **Problema**: El glosario (punto 16) decide que demo.continuar pase de «Continuar» a «Seguir», pero ui.txt aún dice «Continuar». Si se cambia el botón, hay que cambiar también este texto.
- **Opciones**:
  - A: «Continuar» (como el botón actual)
  - B: «Seguir» (si se aplica el punto 16 del glosario)

### `juego/src/main/java/metajavbots/piezas/DisenadorDePieza.java`

#### D108 — es, línea 90 (en `trozo_40.json`)

- **Texto**: `Tu clase debe implementar PiezaPropia (public class MiPieza implements PiezaPropia).`
- **Problema**: El paréntesis dice cosas distintas: es da un ejemplo de cabecera de clase; de/en dicen «(oder PiezaPropia)» porque la interfaz tiene alias EigenesTeil/CustomPart.
- **Opciones**:
  - A: dejarlo así (cada idioma explica lo suyo)
  - B: igualar de/en al español: «(public class MeinTeil implements EigenesTeil)» / «(public class MyPart implements CustomPart)»

#### D109 — de, línea 371 (en `trozo_40.json`)

- **Texto**: `traglast() { return 34; }       // 20..46`
- **Problema**: El comentario del ejemplo alemán (y el inglés, línea 425: capacity() 34 // 20..46) da el rango 20..46, pero el español dice 50 // 20..80 y el validador (línea 222) acepta 20..80. Es código: no lo corrijo, pero el jugador ve un rango falso.
- **Opciones**:
  - A: igualar de/en a «return 50; // 20..80»
  - B: dejarlo

### `juego/src/main/java/metajavbots/combate/CarreraDeObstaculos.java`

#### D110 — es|de|en, línea 701 (en `trozo_40.json`)

- **Texto**: `"Piloto"`
- **Problema**: Nombre por defecto del corredor del jugador si no tiene nombre; es igual en los tres idiomas (si se ve en pantalla, en de/en no está traducido).
- **Opciones**:
  - A: dejarlo «Piloto» en los tres
  - B: traducirlo (Pilot / Pilot) con un id.elegir

### `juego/src/main/java/metajavbots/pantallas/PantallaCarreraDeObstaculos.java`

#### D111 — de|en, línea 534 (en `trozo_40.json`)

- **Texto**: `"VEL %.1f · BLIND %.0f"`
- **Problema**: Abreviaturas en español (VEL, BLIND) que se ven igual en alemán e inglés; no se pueden traducir sin tocar código (T.t o idioma.elegir). Igual en juego/src/main/java/metajavbots/pantallas/PantallaMinijuegoDestruccion.java, línea 357.
- **Opciones**:
  - A: dejarlas
  - B: traducirlas: de «TEMPO %.1f · PANZ %.0f», en «SPD %.1f · ARM %.0f»

### `juego/src/main/java/metajavbots/ejercicios/Comprobador.java`

#### D112 — de|en, línea 197 (en `trozo_40.json`)

- **Texto**: `"⏱ demasiados pasos (¿un bucle que no acaba?)" (y líneas 200, 227, 229, 231 y 351: «⚠ StackOverflowError (¿una recursión sin caso base?)», «⚠ el caso no se pudo evaluar: », «⏱ tarda demasiado»)`
- **Problema**: Mensajes del resultado de cada caso que el jugador ve en la tabla de casos; solo están en español, también con el juego en alemán o inglés. Traducirlos exige tocar código (idioma.elegir), por eso no van como corrección.
- **Opciones**:
  - A: traducirlos: de «⏱ zu viele Rechenschritte (eine Schleife, die nie endet?)», «⚠ StackOverflowError (eine Rekursion ohne Basisfall?)», «⚠ Der Testfall ließ sich nicht auswerten: », «⏱ dauert zu lange»; en «⏱ too many computation steps (a loop that never ends?)», «⚠ StackOverflowError (a recursion with no base case?)», «⚠ the case could not be evaluated: », «⏱ takes too long»
  - B: dejarlos en español

### `juego/src/main/java/metajavbots/pantallas/PantallaDemostracion.java`

#### D113 — es|de|en, línea 313 (en `trozo_40.json`)

- **Texto**: `"private/final/static: -6 % daño · +10 % pasos" / "+10 % Schritte" / "+10% steps"`
- **Problema**: El glosario pide decir siempre «pasos de cálculo / Rechenschritte / computation steps» para no confundirlos con los pasos de avance; es un rótulo corto sobre la demo y puede no caber.
- **Opciones**:
  - A: «+10 % pasos de cálculo» / «+10 % Rechenschritte» / «+10% computation steps»
  - B: dejarlo corto como está

### `juego/src/main/java/metajavbots/combate/PresupuestoSenior.java`

#### D114 — de|en, línea 150 (en `trozo_40.json`)

- **Texto**: `"⏱ Presupuesto de tiempo superado (%.2f ms / %.2f ms): -6 energía y +%d turnos de latencia." (y líneas 164, 192-193, 198, 200 y 241: mensajes de consola del robot en Peleas Senior)`
- **Problema**: Mensajes que el jugador ve en la consola del robot; solo están en español aunque el juego esté en alemán o inglés. Además, String.format con Locale.ROOT escribe los decimales con punto («1.25 ms») en la prosa española. Arreglarlo exige tocar código.
- **Opciones**:
  - A: traducirlos con T.t/idioma.elegir y formatear con el locale del idioma (coma en es/de)
  - B: dejarlos como están

### `juego/src/main/java/metajavbots/codigo/Compilador.java`

#### D115 — de|en, línea 147 (en `trozo_40.json`)

- **Texto**: `"No hay compilador de Java (hace falta un JDK)" (y línea 199 «El compilador ha fallado: »; también juego/src/main/java/metajavbots/codigo/RobotDelJugador.java, línea 92: «No hay ninguna clase pública que herede de Metajavbot (o MetajavbotDe, MetajavbotEn)»)`
- **Problema**: Mensajes de error de compilación que el jugador ve en la lista de errores; solo están en español, también con el juego en alemán o inglés. Traducirlos exige tocar código.
- **Opciones**:
  - A: traducirlos: de «Kein Java-Compiler gefunden (dafür braucht es ein JDK)», «Der Compiler ist abgestürzt: », «Keine öffentliche Klasse, die von Metajavbot (oder MetajavbotDe, MetajavbotEn) erbt»; en «No Java compiler found (a JDK is required)», «The compiler failed: », «No public class extends Metajavbot (or MetajavbotDe, MetajavbotEn)»
  - B: dejarlos en español
