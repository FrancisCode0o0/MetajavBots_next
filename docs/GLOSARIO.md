# Glosario de MetajavBots

Este glosario fija **un término para cada cosa** en los tres idiomas del juego. Si el taller se llama «Taller», «Werkstatt» y «Workshop» en el menú, se llama igual en las ayudas, en el entrenador, en la historia y en las fichas. Así el jugador nunca duda de si dos palabras distintas son dos cosas distintas.

Cómo se usa:

- Antes de escribir o traducir un texto, busca aquí el término. Si no está, añádelo.
- La forma elegida es casi siempre la que más se usa hoy en res/textos/ui.txt (cuento hecho con búsquedas por palabra completa sobre sus 1253 claves). Cuando no, la nota dice por qué.
- Las variantes que figuran en «Nota» y en «Incoherencias encontradas» son las que hay que cambiar en la revisión de estilo (REGLAS.md). Este archivo no cambia ningún texto: solo dice qué forma vale.
- Los nombres de la API (cadaTurno, alVerEnemigo, jedenZug, onTick…), las claves y los huecos {0} no se tocan nunca, aunque usen otra palabra (p. ej. «enemigo» en alVerEnemigo).
- Para nombrar un botón o una sección en un texto, se copia **letra a letra** su etiqueta, con [[…]] donde el texto ya lo usa o entre comillas del idioma: «Taller», „Werkstatt“, “Workshop”.

> **Decidido en la revisión de estilo (24-nov-2026).** Las formas elegidas aquí, y la resolución de cada incoherencia de la última sección, se han aplicado en todo el juego. Son decisiones prudentes (la forma más usada, o la que no choca con otro término); Fran puede cambiarlas: las más discutibles están con sus opciones en docs/REVISION_ESTILO.md.

## Normas ortotipográficas de cada idioma

Resumen de REGLAS.md (la versión completa manda).

| Norma | Español (es) | Deutsch (de) | English (en) |
|---|---|---|---|
| Comillas | «…»; dentro, “…” y luego ‘…’. Nunca " " en prosa | „…“; dentro, ‚…‘. Nunca «» ni " " en prosa | “…”; dentro, ‘…’. Nunca «» ni „“. Apóstrofo recto ' como en el texto actual |
| Signos de apertura | ¿…? ¡…! siempre, también a mitad de frase | — | — |
| Títulos y botones | Estilo frase: «Partida nueva», no «Partida Nueva» | Mayúscula solo en sustantivos (gramática alemana) | Sentence case: “Saved games”, “How do I play?”. Solo los nombres propios llevan mayúscula |
| Tratamiento | Tú (tuteo) | du, dein, dich… siempre en **minúscula** (Duden) | you |
| Puntos suspensivos | Un solo carácter «…», pegado a la palabra | «…» como un carácter; con espacio delante si faltan palabras enteras («Runde 2 … wie viele noch?»), sin espacio si falta parte de una palabra | «…» como un carácter, sin espacio delante |
| Decimales | Coma: 2,5 GB (salvo un literal de Java) | Coma: 2,5 GB | Punto: 2.5 GB |
| Cifra y unidad | Con espacio: 50 ms, 2,5 GB | Con espacio: 50 ms | Con espacio: 50 ms |
| Porcentaje | 50 % (con espacio) | 50 % (con espacio) | 50% (sin espacio) |
| Miles | 1000; desde 5 cifras, 10 000 | — | — |
| Otras | Raya de diálogo sin espacio con el inciso: «—Hola —dijo—.». Sin tilde: solo, este, guion. Sin calcos (cliquear) | ß y Umlaute de verdad (salvo en identificadores como vorwaerts()). Comas ante subordinadas. Tras dos puntos, mayúscula si sigue una frase completa. Compuestos con guion: «Java-Klasse», «While-Schleife» | **Inglés americano** en todo: armor, color, center, -ize, centimeter. Coma de Oxford: no añadirla ni quitarla |
| Los tres | Un espacio entre palabras; ninguno antes de , . ; : ? ! (salvo los casos de arriba); nada de signos duplicados (??, !!, ..) | | |

## Términos del juego

Las filas en negrita agrupan los términos. «cambiar: …» marca la variante que hay que quitar (con sus claves en la sección de incoherencias cuando son muchas).

| Español | Deutsch | English | Nota |
|---|---|---|---|
| **Pelea** | | | |
| pelea | Kampf | fight | La forma general (es 90 claves, de 83, en 92). es: «combate» solo en frases hechas («fuera de combate», «películas de combate»); «batalla» y «lucha» → pelea. en: «battle» y «combat» → fight, salvo «Battle royale» y «close combat». Ver incoherencias 1 |
| pelear | kämpfen | fight | Verbo. es «luchar» (especial.luchar) → pelear |
| ronda | Runde | round | Coherente en las 28 claves. Una pelea tiene varias rondas |
| turno | Zug | tick | Coherente en las 57 claves. 30 turnos por segundo. en: nunca «turn» para el turno («turn» = girar, giro) |
| Un turno (botón) | Ein Zug | One tick | Avanza la pelea un turno (pelea.paso) |
| paso a paso | Schritt für Schritt | step by step | Botón «Paso a paso» y modo del depurador |
| paso (de avance) | Schritt | step | Unidad de distancia: «avanza 50 pasos» |
| pasos de cálculo | Rechenschritte | computation steps | Lo que puede ejecutar el código en un turno. Siempre con «de cálculo» para no confundirlo con los pasos de avance |
| tope de cálculo | Rechenlimit | step limit | especial.tope. en: mejor «computation limit» si se quiere ligar a «computation steps» |
| victoria | Sieg | win | en: «win» en contadores y resultados; «Victory!» solo en el rótulo final (final.victoria) |
| derrota | Niederlage | loss | en: «loss» en contadores; «Defeat» solo en el rótulo final (final.derrota) |
| empate | Unentschieden | draw | en: «tie» (carrera_obs.empate) → draw |
| revancha | Revanche | rematch | Coherente |
| rendirse | aufgeben | surrender | en: 8 claves dicen «surrender» y 2 «give up» (pelea.rendirse, pelea.salir_texto) → surrender. es «Rendirme» (carrera_obs.rendirme) → Rendirse. de: el sustantivo «Aufgabe» (carrera_obs.rendido_sin_premio) se lee como «tarea» → «Aufgegeben» |
| rival | Gegner | rival | El robot contrario, en prosa. en «enemy» donde es dice «rival» (poo.*) → rival; «opponent» solo en «sparring opponent» |
| enemigo | Gegner | enemy | Solo en pseudocódigo y junto a métodos de la API que lo llevan (alVerEnemigo, enemigosVistos). En prosa, «rival» |
| la computadora | der Computer | the computer | El rival que maneja el juego (es 25 claves). es «la máquina» (especial.carrera_ayuda, carrera_obs.vs_maquina) → la computadora; «Máquina contra máquina» es nombre de modo y se queda |
| el ordenador | der Rechner | the computer | El PC del jugador (cerebro local, carpetas). Es otra cosa que «la computadora»; de ya lo distingue (Rechner/Computer) |
| sparring | Sparring-Gegner | sparring opponent | El rival de práctica del taller |
| jefe | Boss | boss | jefe final = Endboss = final boss |
| compañero (de equipo) | Kamerad | teammate | Coherente. de: «Kamerad» suena militar; si se cambia, a «Teamkollege» en las 3 claves (pelear.companero, pelear.companero_ayuda, pelear.equipos_ayuda) |
| equipo | Team | team | |
| jugador | Spieler | player | es «usuario» (especial.carrera_ayuda, carrera_obs.vs_usuario) → jugador. «usuario» solo en «carpeta de usuario» (Benutzerordner, user folder) |
| robots de otros jugadores | Roboter anderer Spieler | other players' robots | Los de la carpeta robots_ajenos. Variantes: es «Robots de otros» (especial.otros), «robot ajeno»; de «fremde Roboter» (7 claves); en «Other robots» (especial.otros). Elegir una; propuesta: la de la tabla |
| daño | Schaden | damage | en «dmg» solo donde no cabe (poo.*) |
| daño recibido | erlittener Schaden | damage taken | de «eingesteckter Schaden» (guia.10.pasos) → erlittener Schaden |
| daño hecho | verursachter Schaden | damage dealt | de «ausgeteilter Schaden» (guia.10.pasos) → verursachter Schaden |
| aciertos | Treffer | hits | |
| puntería | Trefferquote | accuracy | |
| locutor | Ansager | announcer | de «Sprecher» (guia.7.pasos) → Ansager |
| repetición | Wiederholung | replay | a cámara lenta = in Zeitlupe = in slow motion |
| **Robot y piezas** | | | |
| robot | Roboter | robot | en «Bots» (destruccion.hud_bajas, carrera_obs.desglose_bajas) → robots |
| metajavbot | Metajavbot | metajavbot | El robot del jugador como nombre común (de con mayúscula por ser sustantivo). El juego es «MetajavBots» |
| modelo | Modell | model | El modelo de robot (Cobalto, Chispa…). Ojo: también se dice «modelo» del archivo .gguf del cerebro de IA; en esas pantallas, «modelo de IA» si hay duda |
| naturaleza | Natur | nature | Acero, Rayo, Foco, Furia, Ingenio, Éter (ver nombres propios) |
| núcleo de naturaleza | Naturkern | nature core | La pieza de la ranura de naturaleza |
| arquetipo | Archetyp | archetype | Asalto, Táctico, Defensor (ver nombres propios). Solo sale en las fichas y en la API (arquetipo()) |
| habilidad | Fähigkeit | ability | La acción propia del arquetipo (embestir, pulso de radar, fortificar). en «skills» (pelear.azar_ayuda) → abilities |
| pieza | Teil | part | Coherente (29 claves). de nunca «Bauteil»; en nunca «piece» para una pieza |
| piezas de fábrica | Werksteile | factory parts | Las que trae el modelo |
| pieza legendaria | legendäres Teil | legendary part | |
| ranura | Platz | slot | Hueco del robot donde va una pieza: cabeza, brazo, piernas, núcleo, naturaleza |
| cabeza | Kopf | head | Lleva el radar y la especial |
| brazo izquierdo / derecho | linker / rechter Arm | left / right arm | Abreviado: brazo izq. / brazo der. |
| piernas | Beine | legs | Cómo se mueve y cuánto carga |
| núcleo | Kern | core | Pasos de cálculo y energía |
| cuerpo | Körper | body | Uno de los tres mandos |
| ruedas | Räder | wheels | Uno de los tres mandos |
| mandos | Regler | controls | Ruedas, cuerpo y cabeza |
| mira | Visier | sight | Hacia donde apunta la cabeza. en: también sale «head» y «aim» para la mira; usar «sight» (como en las fichas). Ver visor |
| visor (táctico) | Taktikanzeige (propuesta) | visor | La capa de F8 (pelea.visor). de: hoy «Visier», la misma palabra que la mira → cambiar. Ver incoherencias 9 |
| radar | Radar | radar | cono del radar = Radarkegel = radar cone |
| arma | Waffe | weapon | |
| arma propia | eigene Waffe | signature weapon | La que da la sinergia con el modelo (★ Propia / ★ Eigen / ★ Signature) |
| arma especial | Spezialwaffe | special weapon | Las de cuenta atrás de la arena |
| escudo | Schild | shield | |
| blindaje | Panzerung | armor | Coherente (7 claves). Nunca armadura, Rüstung ni armour |
| energía | Energie | energy | Coherente. «batería» solo en el chiste de animacion.corto |
| alcance | Reichweite | range | en «arm reaches» (pelea.visor_ayuda) → ranges |
| enfriamiento | Abkühlzeit | cooldown | Espera del arma entre ataques. es: valor.enfriamiento dice «recarga», igual que valor.recarga → enfriamiento. de: «Abkühlung» (ui) y «Abkühlzeit» (fichas): elegir «Abkühlzeit» |
| recarga | Aufladung | recharge | Lo que recupera un escudo o la energía. No usar para el enfriamiento |
| ráfaga | Salve | burst | Golpes seguidos antes de descansar (valor.cargas). Las fichas dicen «cargas / Ladungen / charges»: cambiar allí, porque «carga» ya es otra cosa |
| peso | Gewicht | weight | |
| carga | Traglast | load limit | Lo que aguantan las piernas. Fichas: «Last / load» → Traglast / load limit |
| sobrecarga | Überladen | overloaded | Tramo de peso. Los otros: ligero / normal / no puede pelear = Leicht / Normal / Darf nicht kämpfen = Light / Normal / Cannot fight |
| velocidad | Tempo | speed | de «Geschwindigkeit» en consejos del entrenador (entrenador.rival.rapido…) → Tempo |
| giro | Drehung | turn | |
| aceleración | Beschleunigung | acceleration | |
| cifras del robot | Werte des Roboters | robot stats | En las fichas, el párrafo «Las cifras» = «Die Zahlen» = «The numbers» (es otra cosa: los números de la regla) |
| **Arena** | | | |
| arena | Arena | arena | |
| terreno | Gelände | terrain | |
| trampa | Falle | trap | |
| obstáculo | Hindernis | obstacle | |
| aparato | Gerät | gadget | Los objetos programables de la arena |
| muñeco | Puppe | dummy | El blanco de práctica. de «Testpuppe» (taller.probar_aqui_ayuda) → Puppe |
| **Progreso** | | | |
| etiqueta | Stufe | rank | Noob I… Senior IV (y Master). es «rango» (5 claves) → etiqueta. de «Abzeichen», «Rang» → Stufe. en «level» para la etiqueta → rank. Ver incoherencias 4 |
| subir / bajar de etiqueta | aufsteigen / absteigen | rank up / drop a rank | es «subir de nivel» se admite en frase hecha. en «level up» → rank up |
| nivel (de dificultad) | Schwierigkeitsstufe | level | Niveles de una carrera, de Máquina contra máquina, de una demo. No confundir con la etiqueta del jugador |
| dificultad | Schwierigkeit | difficulty | |
| experiencia | Erfahrung | experience | puntos de experiencia = Erfahrungspunkte = experience points. Nunca «XP» |
| monedas | Münzen | coins | saldo = Guthaben = balance |
| premio | Belohnung | prize | de «Preis» (historia.premio) y «Beute» (carrera_obs.multiplicador) → Belohnung. en «reward» (menu.carrera_ayuda, carrera_obs.multiplicador) → prize |
| botín | Beute | loot | Lo que se cobra en Destrucción y en la carrera |
| medalla | Medaille | medal | oro / plata / bronce = Gold / Silber / Bronze = gold / silver / bronze |
| racha | Serie | streak | |
| desbloquear | freischalten | unlock | bloqueado = gesperrt = locked |
| historial | Verlauf | history | Las peleas pasadas de la partida. de «Geschichte» (inicio.borrar_texto) → Verlauf (Geschichte es el modo Historia) |
| progreso | Fortschritt | progress | |
| **Partidas** | | | |
| partida | Spielstand | save | El perfil guardado del jugador. de «Spiel» (5 claves) → Spielstand; en «game» (5) y «profile» (4) → save. Ver incoherencias 2 |
| Partidas guardadas | Spielstände | Saves | inicio.partidas |
| Partida nueva | Neuer Spielstand | New save | inicio.nueva (hoy «Neues Spiel» / «New game») |
| guardar | speichern | save | |
| borrar | löschen | delete | es «eliminada» (pieza_propia.borrada) → borrada |
| cambiar el nombre | umbenennen | rename | |
| crear | erstellen | create | de: «Anlegen» (inicio.crear) y «Erstellen» (taller.crear): elegir «Erstellen» |
| **Modos y menús** | | | |
| Historia (modo) | Geschichte | Story | menu.historia, pelear.campanas, campanas.titulo |
| historia | Geschichte | story | La historia guiada del viejo, por episodios |
| episodio | Folge | episode | De la historia guiada |
| campaña | Kampagne | campaign | Historias de varias peleas, por capítulos (Credo, Rocko…). es: varias claves las llaman «historias» (campanas.lista, campanas.cerrada, pelear.campanas_ayuda): aceptable, pero «Seguir la campaña» y «Seguir la historia» no deben traducirse como «Back to…» |
| capítulo | Kapitel | chapter | De una campaña |
| escena | Szene | scene | |
| viñeta | Bild | panel | |
| prólogo / epílogo | Prolog / Epilog | prologue / epilogue | |
| ¡A pelear! | Auf in den Kampf! | Let's fight! | Botón del menú (menu.pelear_ya). en «Fight!» en campanas.pelear → Let's fight!. El grito del locutor (entrada.a_pelear) puede ser corto |
| Desafíos / desafío | Herausforderungen / Herausforderung | Challenges / challenge | El modo de pelear contra una IA más fuerte, con premio |
| reto | Aufgabe | task | Prueba de programar con objetivo (reto de activación, retos de programar, reto del día, reto de Destrucción). Hoy de y en no lo distinguen del desafío. Ver incoherencias 3 |
| reto del día | Tagesaufgabe | daily task | Hoy en «Daily challenge» |
| Taller | Werkstatt | Workshop | Con mayúscula solo como nombre de la sección |
| Banco de pruebas | Teststand | Test bench | Probar el robot junto al editor |
| Tienda | Laden | Shop | Tienda de piezas = Teile-Laden = Parts shop |
| Ejercicios / ejercicio | Übungen / Übung | Exercises / exercise | Coherente (29 claves). de: «Übung» no debe usarse para otra cosa (ver práctica) |
| Tutoriales / tutorial | Tutorials / Tutorial | Tutorials / tutorial | |
| lección | Lektion | lesson | Cada tutorial tiene lecciones |
| tarea (de una lección) | Aufgabe | task | leccion.tareas |
| caso (de prueba) | Testfall | case | ejercicio.casos |
| Minijuegos | Minispiele | Minigames | Contra el muñeco |
| Demostraciones | Demos | Demos | El botón dice «Demos» en de y en; el «¿Cómo se juega?» y dos consejos dicen «Demonstrationen / Demonstrations» → Demos |
| La hora de hoy | Die Stunde heute | Today's hour | Sesión guiada |
| Modo especial | Spezialmodus | Special mode | |
| Carrera de obstáculos | Hindernislauf | Obstacle race | de «Hindernisrennen» (como_se_juega.txt) y «Rennen» (carrera.ganas) → Lauf. en «run» (especial.carrera_ayuda) → race |
| pista (de carrera) | Strecke | track | No confundir con la pista de ayuda |
| Destrucción y supervivencia | Zerstörung und Überleben | Destruction and survival | En ui.txt va con «&» y en en con mayúsculas (especial.destruccion, destruccion.titulo…) → «und» / «and» y sentence case, como en como_se_juega.txt |
| Peleas Senior | Senior-Kämpfe | Senior fights | en «Senior Fights» (especial.senior) → Senior fights |
| Máquina contra máquina | Maschine gegen Maschine | Machine vs machine | |
| Battle royale | Battle Royale | Battle royale | |
| Por equipos | Im Team | Teams | |
| Modo azar | Zufallsmodus | Random mode | |
| Contra la computadora | Gegen den Computer | Against the computer | en «Against computer» (carrera_obs.vs_maquina) → Against the computer |
| Práctica | Training | Practice | La pelea de práctica. de: en el temario dice «Übung vorbereiten», «Übungsplan» (temario.preparar, temario.plan, temario.vacio) y choca con ejercicio → Training |
| Depuración y pruebas | Debugging und Tests | Debugging and testing | Propuesta. Hoy «Depuración y Testing» / «Debugging & Testing» (menu.depuracion, entrenador.tip.juego_depuracion): «Testing» es un anglicismo en es y «&» no es prosa |
| depurador | Debugger | debugger | |
| Robots rotos | Kaputte Roboter | Broken robots | Modo de arreglar código |
| Laboratorio de pruebas | Test-Labor | Testing lab | Corto: laboratorio = Labor = lab |
| prueba (test) | Test | test | |
| mutante | Mutant | mutant | |
| Jefes de tema | Themen-Bosse | Topic bosses | |
| Gimnasios | Gyms | Gyms | de «Arenen» (especial.gimnasios) choca con arena → Gyms (como en entrenadores.txt: «aus dem Gym») |
| Retos de programar | Programmier-Aufgaben | Programming tasks | Si se adopta «reto = Aufgabe / task» |
| Mi temario / temario | Mein Lehrplan / Lehrplan | My syllabus / syllabus | |
| tema (de Java) | Thema | topic | No confundir con «tema claro» = helles Design = light theme |
| Mecánicas / mecánica | Mechaniken / Mechanik | Mechanics / mechanic | |
| ficha | Karte | card | Cada ficha de mecánica o de concepto |
| ¿Cómo se juega? | Wie spielt man das? | How do I play? | La guía con dibujos dice «Cómo se juega / So wird gespielt / How to play» (guia.9.*): vale como título de página, no como botón |
| Estadísticas | Statistiken | Statistics | |
| Ajustes | Einstellungen | Settings | |
| Panel de la IA | KI-Steuerung | AI panel | |
| **Entrenador e IA** | | | |
| entrenador | Trainer | coach | Coherente en ui.txt. en: entrenadores.txt dice «trainer» en los papeles (p. ej. «The grumpy old gym trainer») → coach |
| tutor (IA) | KI-Tutor | AI tutor | El que ayuda en ejercicios y lecciones |
| IA | KI | AI | |
| cerebro de IA | KI-Gehirn | AI brain | local / en línea = lokal / online = local / online |
| casilla (de cerebro) | Platz | slot | Las tres casillas de cerebros |
| clave (de API) | Schlüssel | key | |
| consejo | Tipp | tip | es «tip» (ajustes.entrenador, ajustes.entrenador_ayuda, entrenador.tip.otro) → consejo. de «Rat / Ratschläge» y en «advice» (entrenador.pelea.otro, ajustes.entrenador_ayuda) → Tipp / tip |
| pista (ayuda) | Hinweis | hint | de: hoy «Tipp», igual que consejo (8 claves) → Hinweis. Ver incoherencias 6 |
| aviso | Hinweis | notice | Avisos de órdenes = Befehlshinweise = order tips (ajustes.avisos_ordenes): se queda |
| informe | Bericht | report | El informe de la ronda |
| consola | Konsole | console | |
| **Código** | | | |
| código | Code | code | |
| compilar | kompilieren | compile | Botón «Compilar» = „Kompilieren“ = “Compile” |
| error de compilación | Kompilierfehler | compile error | |
| orden | Befehl | order | Una línea que el robot obedece. en «commands» (pelea.codigo_no_compila) → orders |
| pseudocódigo | Pseudocode | pseudocode | |
| plantilla | Vorlage | template | |
| Comprobar | Prüfen | Check | Botón de los ejercicios |
| Revisar el código / revisión del código | Code-Review / Code-Review | Review the code / code review | de: hoy «Code prüfen» y «Code-Prüfung», que se confunde con el botón «Prüfen» (Comprobar). Propuesta: «Code-Review». en: guia.10.pasos cita «[[Review code]]» pero el botón (final.revisar) es «Review the code» |
| Documentación | Dokumentation | Documentation | De paquetes (Javadoc); paquete = Paket = package |
| caja de arena | Sandbox | sandbox | |
| Buscar | Suchen | Find / Search | en: «Find» en el editor, «Search» en la API y la documentación. Se admite: son cajas distintas |
| Formatear | Formatieren | Format | |
| **Botones comunes** | | | |
| Volver | Zurück | Back | es «Atrás» (paquetes.atras) → Volver |
| Seguir | Weiter | Continue | Pasar a lo siguiente |
| Seguir (tras una pausa) | Weiter | Resume | es «Reanudar» (carrera_obs.reanudar) → Seguir; de «Fortsetzen» (carrera_obs.reanudar) → Weiter. es «Continuar» (demo.continuar) → Seguir |
| Pausa | Pause | Pause | es «Pausar» (carrera_obs.pausar) → Pausa. En pausa = Pausiert = Paused |
| Siguiente / Anterior | Weiter / Zurück | Next / Previous | |
| Cancelar | Abbrechen | Cancel | |
| Cerrar | Schließen | Close | |
| Salir (de un modo) | verlassen | leave | «¿Salir de la pelea?» = „Kampf verlassen?“ = “Leave the fight?”. Botón suelto: Salir = Beenden = Exit |
| Entendido | Verstanden | Got it | |
| Empezar | Loslegen | Start | |
| Probar | Testen | Test | en: «Try» en panel.probar y leccion.probar_robot. Se admite «Try» cuando es «probar a ver qué pasa»; «Test» cuando prueba código o un cerebro |
| Reiniciar | Neustart / Zurücksetzen | Restart / Reset | Neustart/Restart reinicia la simulación (banco.reiniciar); Zurücksetzen/Reset devuelve el código de un ejercicio al inicio (ejercicio.reiniciar) |
| Comprar | Kaufen | Buy | |
| Activar | Aktivieren | Activate | |
| Jugar | Spielen | Play | |
| Vista previa | Vorschau | Preview | |
| Otra vez | Noch einmal | Again | Pelear otra vez = Noch einmal kämpfen = Fight again |

## Nombres propios

Se escriben tal cual están en sus archivos; no se traducen si no lo están ya. Si un texto cita uno, lo copia exactamente (mayúsculas incluidas).

### Modelos de robot (res/datos/modelos.txt)

| id | Español | Deutsch | English | Naturaleza | Arquetipo |
|---|---|---|---|---|---|
| cobalto | Cobalto | Kobalt | Cobalt | Acero | Defensor |
| chispa | Chispa | Funke | Sparky | Rayo | Asalto |
| vigia | Vigía | Späher | Lookout | Foco | Táctico |
| mazo | Mazo | Hammerkopf | Sledge | Furia | Asalto |
| grulla | Grulla | Kranich | Crane | Éter | Táctico |
| abaco | Ábaco | Abakus | Abacus | Ingenio | Táctico |
| bastion | Bastión | Bollwerk | Bastion | Acero | Defensor |
| avispa | Avispa | Wespe | Wasp | Rayo | Asalto |
| tejedora | Tejedora | Weberin | Weaver | Ingenio | Táctico |
| filo | Filo | Schneide | Edge | Furia | Asalto |
| faro | Faro | Leuchtturm | Beacon | Foco | Táctico |
| pixel | Píxel | Pixel | Pixel | Ingenio | Táctico |
| nimbo | Nimbo | Nimbus | Nimbus | Éter | Defensor |
| chatarra | Chatarra | Schrotti | Scrapper | Acero | Defensor |
| voltio | Voltio | Volti | Volt | Rayo | Asalto |
| eclipse | Eclipse | Finsternis | Eclipse | Éter | Táctico |
| serrucho | Serrucho | Sägezahn | Sawtooth | Furia | Asalto |
| galapago | Galápago | Panzerkröte | Shellback | Acero | Defensor |
| volteo | Volteo | Kipper | Flipside | Ingenio | Táctico |
| bolon | Bolón | Kettenhund | Wrecker | Furia | Asalto |
| barrena | Barrena | Bohrkäfer | Driller | Furia | Asalto |
| remolino | Remolino | Wirbel | Whirl | Rayo | Asalto |

### Naturalezas, arquetipos y habilidades

| Español | Deutsch | English | Nota |
|---|---|---|---|
| Acero | Stahl | Steel | Núcleo de acero = Stahlkern = Steel core |
| Rayo | Blitz | Bolt | Núcleo de rayo = Blitzkern = Bolt core |
| Foco | Fokus | Focus | |
| Furia | Wut | Fury | |
| Ingenio | Geist | Wit | |
| Éter | Äther | Aether | Nombre propio: se queda «Aether», aunque el inglés americano diría «ether» |
| asalto | Angriff | assault | de: la ficha dice «Angreifer» en la prosa y «Angriff» en las cifras y el pseudocódigo; elegir «Angriff» para el arquetipo |
| táctico | Taktik | tactical | de: «Taktiker» en la prosa → Taktik |
| defensor | Verteidigung | defender | de: «Verteidiger» en la prosa → Verteidigung |
| embestir | Ansturm | charge | Habilidad de asalto (método embestir / ansturm / charge) |
| pulso de radar | Radarimpuls | radar pulse | Habilidad táctica |
| fortificar | befestigen | fortify | Habilidad defensora |

### Etiquetas del jugador

| Español | Deutsch | English | Nota |
|---|---|---|---|
| Noob I–IV | Noob I–IV | Noob I–IV | Iguales en los tres idiomas, con mayúscula |
| Junior I–IV | Junior I–IV | Junior I–IV | |
| Senior I–IV | Senior I–IV | Senior I–IV | En prosa en también «Senior» con mayúscula («from zero to senior» en guia.9.pasos → Senior) |
| Master | Master | Master | |

### Entrenadores (res/datos/entrenadores.txt)

| id | Español | Deutsch | English | Papel (es) |
|---|---|---|---|---|
| tuercas | Don Tuercas | Meister Mutter | Old Man Nuts | El viejo gruñón del gimnasio (también es «el viejo» de la historia) |
| aceite | Maestro Aceite | Meister Öl | Master Oil | El maestro sereno de las tareas absurdas |
| voltaje | Capitán Voltaje | Kapitän Volt | Captain Voltage | El excampeón rival que ahora te entrena |
| remache | Sargento Remache | Feldwebel Niete | Sergeant Rivet | El duro que no quería entrenarte |
| cobra | Sensei Cobra-7 | Sensei Kobra-7 | Sensei Cobra-7 | El sensei estricto |
| yanobit | Maestro Yanobit | Meister Yanobit | Master Yanobit | El anciano maestro de judo |
| pelobit | Gran Maestro Pelobit | Großmeister Pelobit | Grandmaster Pelobit | El maestro excéntrico y exigente |

### Personajes de la historia (res/historia/historia_*.ink)

| Variable | Español | Deutsch | English | Quién es |
|---|---|---|---|---|
| jugador | Tú | Du | You | El jugador |
| viejo | Don Tuercas | Meister Mutter | Old Man Nuts | El viejo, el entrenador del jugador. En prosa: el viejo = der Alte = the old man |
| nino | Iván Dragobit | Iwan Dragobit | Ivan Dragobit | El niño repelente = der Besserwisser-Bengel = the insufferable kid |
| friki | Bartolo Megabyte | Bartolo Megabyte | Bartolo Megabyte | El coleccionista |
| prisas | Trini Turbo | Trini Turbo | Trini Turbo | La chica que se lo salta todo |
| licenciado | el Licenciado Celdas | Herr Doktor Tabelle | Mr. Spreadsheet | El de la reunión de equipo |
| diva | Madame Purpurina | Madame Glitzer | Madame Glitter | La diva de la purpurina (jefe) |
| hijo | Pinochip | Pinochip | Pinochip | El campeón misterioso (jefe final) |
| trasto | Trasto | Klapperkiste | Rustbucket | El trasto que encuentra trabajo |
| rival | Tu rival | Dein Gegner | Your rival | El rival del episodio |

### Arenas y campañas

| Español | Deutsch | English | Nota |
|---|---|---|---|
| Hangar | Hangar | Hangar | Arena |
| Desierto | Wüste | Desert | Arena |
| Neón | Neon | Neon | Arena |
| Laboratorio | Labor | Lab | Arena |
| Luna | Mond | Moon | Arena |
| Chatarrería | Schrottplatz | Junkyard | Arena (y decorado del prólogo) |
| Cocina gigante | Riesenküche | Giant kitchen | Arena |
| Pista de hielo | Eisbahn | Ice rink | Arena |
| Tejado con lluvia | Regendach | Rainy rooftop | Arena |
| Credo | Credo | Credo | Campaña (parodia) |
| Isla Clandestina | Geheime Insel | Secret Island | Campaña (parodia) |
| Rocko | Rocko | Rocko | Campaña (parodia) |
| Kárate-bit | Karate-Bit | Karate-Bit | Campaña (parodia) |

### Piezas

Los nombres de las piezas están en res/datos/piezas.txt (columnas 3-5: es | de | en) y valen tal cual; no se copian aquí para no tener dos listas. Dos avisos:

- La ficha mec_armas (08_mecanicas.txt) dice «el sable fotónico» / „Der Photonensäbel“, pero la pieza se llama «Sable fotónico ligero» / „Leichte Photonklinge“ / “Light photon saber”. de debe decir «Photonklinge».
- REGLAS.md pone de ejemplo «Titan Hammer» con mayúsculas, pero piezas.txt lo escribe «Titan hammer» (y «Aurora laser», «Missile storm»…). Hay que decidir una sola forma para los nombres de piezas en en; mientras tanto, manda piezas.txt.

## Incoherencias encontradas

Cada punto: la forma elegida y las claves de ui.txt que llevan la otra. Las de otros archivos se dicen con su archivo.

1. **pelea / combate / batalla / lucha (es) y fight / battle / combat (en).** Se elige «pelea» y «fight».
   - es «combate» → pelea: temario.combate («Combate temático»), temario.ayuda, temario.sin_ia, temario.guardado_ayuda, entrenador.tip.juego_arena, entrenador.milagro.frases, ajustes.musica_ayuda, especial.carrera_ayuda («combate al rival»), senior.paralelo_boton, campanas.a_pelear_escena («¡Al combate!»), menu.historia_ayuda («historias de combate»). Se quedan pelea.estado_fuera («fuera de combate») y pelear.campanas_ayuda («películas de combate»).
   - es «batalla» → pelea: entrenador.aviso_sin_compilar_menu. es «lucha / Luchar» → pelea / Pelear: guia.9.pasos, especial.luchar.
   - en «battle» → fight: menu.pelear_ayuda, entrenador.aviso_sin_compilar_menu, guia.9.pasos, especial.carrera_ayuda. en «combat» → fight: menu.historia_ayuda, carrera_obs.mapa_ayuda (ahí «combat» traduce «disparos»: «shooting»). Se quedan pelear.royale, entrenador.rival.se_acerca («close combat») y pelear.campanas_ayuda («combat films»).
2. **partida = Spielstand / Spiel (de) y save / game / profile (en).** Se elige «Spielstand» y «save».
   - de «Spiel» → Spielstand: inicio.partidas («Gespeicherte Spiele» → «Spielstände»), inicio.nueva («Neues Spiel» → «Neuer Spielstand»), inicio.falta_nombre, inicio.ya_existe, inicio.no_se_abre.
   - en «game» → save: inicio.partidas («Saved games» → «Saves»), inicio.nueva («New game» → «New save»), inicio.falta_nombre, inicio.ya_existe, inicio.no_se_abre.
   - en «profile» → save: campanas.inventar_ayuda, biografias.editar_ayuda, biografias.guardar_ayuda, biografias.guardada. en «saved game» (pelear.companero_ayuda) y «save file» (guia.7.pasos) → save.
3. **desafío y reto se traducen igual en de y en.** Son dos cosas: el desafío es el modo de pelea con premio (Herausforderung / challenge, 13 claves, coherente); el reto es una prueba de programar con objetivo. Se elige reto = «Aufgabe» / «task».
   - de ya dice «Aufgabe» en: tienda.ayuda, tienda.activar_ayuda, tienda.ok, activar.empieza, pelea.salir_reto_titulo, entrenador.tip.juego_tienda, ajustes.especiales_ayuda, mini.boton_ayuda. Dice «Herausforderung» (→ Aufgabe) en: menu.especial_ayuda, taller.piezas_ayuda (la primera vez: «Programmier-Herausforderungen»), desbloqueo.reto, especial.laboratorio_ayuda, especial.retos, destruccion.panel_titulo, destruccion.iniciar, destruccion.iniciar_ayuda, destruccion.victoria_titulo, destruccion.salir_titulo, destruccion.salir_texto, especial.senior_ayuda.
   - en dice «challenge» en todas las de reto: mini.boton_ayuda, menu.especial_ayuda, tienda.ayuda, tienda.activar_ayuda, tienda.ok, activar.titulo, activar.empieza, pelea.salir_reto_titulo, taller.piezas_ayuda, desbloqueo.reto, especial.laboratorio_ayuda, especial.retos, entrenador.tip.juego_tienda, ejercicios.reto_dia, ajustes.especiales_ayuda, destruccion.* (panel_titulo, iniciar, iniciar_ayuda, victoria_titulo, salir_titulo, salir_texto), especial.senior_ayuda → task.
   - Si se prefiere dejar «challenge» para los dos en en, al menos el menú «Challenges» (menu.pelear) no debe compartir nombre con «Programming challenges» (especial.retos).
4. **etiqueta = rango / nivel (es), Stufe / Abzeichen / Rang (de), rank / level (en).** Se elige «etiqueta», «Stufe», «rank».
   - es «rango» → etiqueta: menu.tienda_ayuda, tienda.rango_bloqueado, tienda.necesita_rango, carrera_obs.dif_especial_bloqueada, carrera_obs.dif_bloqueada_rango.
   - de «Abzeichen» → Stufe: guia.10.pasos. de «Rang» → Stufe: carrera_obs.dif_especial_bloqueada, carrera_obs.dif_bloqueada_rango.
   - en «level» por la etiqueta → rank: comun.progreso_ayuda, bienvenida.algo_ayuda, sesion.sin_nuevo, final.bajas_titulo («Level drop!» → «Rank drop!»), final.bajas_aviso («dropped a level» → «dropped a rank»), especial.errores_ayuda, guia.10.pasos («level up» → «rank up»).
   - de usa «Stufe» también para niveles de dificultad (mcm.nivel, carrera_obs.dif_bloqueada_rango «Gesperrte Stufe», carrera_obs.dif_especial_bloqueada): → «Schwierigkeitsstufe» o «Level» para no confundirlo con la etiqueta.
5. **rendirse = Give up / Surrender (en) y Rendirse / Rendirme (es).** Se elige «Rendirse» y «Surrender».
   - en «Give up / giving up» → Surrender / surrendering: pelea.rendirse, pelea.salir_texto.
   - es «Rendirme» → Rendirse: carrera_obs.rendirme (además repite carrera_obs.rendirse).
   - de «Aufgabe» como sustantivo de rendirse (carrera_obs.rendido_sin_premio) → «Aufgegeben»: «Aufgabe» es «tarea» en el resto del juego.
6. **pista y consejo = Tipp (de).** en distingue hint / tip; de no. Se elige pista = «Hinweis», consejo = «Tipp».
   - de «Tipp» por pista → Hinweis: informe.pista_titulo, ejercicio.pista, revisor.multiples_objetivos, entrenador.error.sin_pista, tutor.otra_pista, tutor.ayuda, ajustes.ayuda_tutor_ayuda, especial.pista.
   - es «tip» (anglicismo) → consejo: ajustes.entrenador («tips y consejos» → «consejos»), ajustes.entrenador_ayuda («da tips»), entrenador.tip.otro («Otro tip»).
   - de «Rat / Ratschläge» → Tipp y en «advice» → tip: entrenador.pelea.otro, ajustes.entrenador_ayuda.
7. **enfriamiento = recarga (es) y cargas = ráfaga.**
   - es valor.enfriamiento dice «recarga», igual que valor.recarga (que es otra cosa: Aufladung / recharge) → «enfriamiento», como la ficha mec_armas.
   - de «Abkühlung» (valor.enfriamiento) y «Abkühlen / Abkühlzeit» (mec_armas) → «Abkühlzeit».
   - valor.cargas dice «ráfaga / Salve / burst» y mec_armas «cargas / Ladungen / charges» (título y texto) → «ráfaga / Salve / burst» en la ficha: «carga» ya es el límite de peso.
   - carga: mec_peso dice «Last / load» y ui.txt «Traglast / load limit» (valor.carga, peso.titulo, pelear.tu_peso) → Traglast / load limit.
8. **rival / enemigo / opponent.** Se elige «rival / Gegner / rival» en prosa.
   - en «enemy» donde es dice «rival»: poo.private, poo.protected, poo.paquete, poo.public, poo.consejo_public → rival.
   - es «enemigo» (25 claves) y en «enemy» (29): se aceptan en pseudocódigo y junto a la API (mini.*.pseudo, dibujo.orden.mirar_enemigo, entrenador.rival.no_apuntas.trozo…). En prosa → rival: informe.visto, informe.que.visto, informe.pista.dano_hecho, informe.pista.visto, revisor.no_ataca, revisor.radar, revisor.multiples_objetivos, entrenador.tip.sin_radar, entrenador.tip.multiples_objetivos, entrenador.tip.sin_apuntar, entrenador.pelea.marca, ajustes.avisos_ordenes_ayuda, especial.retos_ayuda.
9. **Visier = mira y visor (de); sight / head / aim = mira (en).**
   - de «Visier» se usa para la mira (13 claves: mini.girar.texto, pelea.mando_cabeza, aviso.mando.4…) y para el visor táctico (pelea.visor, pelea.visor_ayuda «Taktisches Visier»). Se deja «Visier» para la mira y el visor pasa a «Taktikanzeige» (pelea.visor, pelea.visor_ayuda).
   - en: la mira es «sight» en pelea.mando_cabeza, aviso.mando.4, pelea.paso_a_paso_ayuda, revisor.radar, ajustes.avisos_ordenes_ayuda; «head» en pelea.mando_disparo, aviso.ordenes.lejos, aviso.ordenes.lejos_mira, dibujo.turno.paso1; «aim» en mini.disparar.texto, entrenador.rival.no_apuntas. Cuando se nombra la mira como cosa → «sight»; «aim» solo como verbo.
10. **la computadora / la máquina (es).** Se elige «la computadora» para el rival del juego: especial.carrera_ayuda («contra la máquina»), carrera_obs.vs_maquina («Contra la máquina» → «Contra la computadora»; en «Against computer» → «Against the computer»). «el ordenador» queda para el PC del jugador (cerebro.local_ayuda, pelear.companero_ayuda, guia.*), que de ya llama «Rechner».
11. **jugador / usuario (es).** Se elige «jugador»: especial.carrera_ayuda («robots de otros usuarios»), carrera_obs.vs_usuario («Contra otro usuario»).
12. **robots de otros jugadores.** es «Robots de otros jugadores» (pelear.ajenos) frente a «Robots de otros» (especial.otros) y «robot ajeno» (pelear.companero_ayuda); de «Roboter anderer Spieler» (pelear.ajenos) frente a «fremde Roboter» (menu.pelear_ayuda, menu.especial_ayuda, especial.otros, pelear.royale_ayuda, pelear.companero_ayuda, especial.carrera_ayuda); en «Other players' robots» frente a «Other robots» (especial.otros). Se elige «robots de otros jugadores / Roboter anderer Spieler / other players' robots».
13. **premio = Belohnung / Preis / Beute (de) y prize / reward (en).** Se elige «Belohnung» y «prize»: de historia.premio («Preis»), carrera_obs.multiplicador («Beute ×{0}»), menu.carrera_ayuda («Stufenbelohnungen», vale); en menu.carrera_ayuda («tiered rewards»), carrera_obs.multiplicador («Reward ×{0}»).
14. **historial = Verlauf / Geschichte (de).** Se elige «Verlauf»: inicio.borrar_texto («seiner Geschichte» → «seinem Verlauf»). «Geschichte» es el modo Historia.
15. **Carrera = Lauf / Rennen (de) y race / run (en).** Se elige «Lauf» y «race»: de carrera.ganas («Rennen»), como_se_juega.txt «Hindernisrennen» (=== carrera) → «Hindernislauf»; en especial.carrera_ayuda («each run» → «each race»).
16. **seguir = Seguir / Continuar / Reanudar (es).** Se elige «Seguir»: demo.continuar («Continuar»), carrera_obs.reanudar («Reanudar»; de «Fortsetzen» → «Weiter»). en: «Continue» para avanzar, «Resume» tras una pausa (banco.seguir y historia.seguir ya lo hacen). En final.seguir_campana y historia.seguir_la_historia, en traduce «Seguir» por «Back to…»: → «Continue the campaign», «Continue the story».
17. **Pausa / Pausar (es).** carrera_obs.pausar «Pausar» → «Pausa», como banco.pausa e historia.pausa.
18. **Volver / Atrás (es).** paquetes.atras «Atrás» → «Volver».
19. **borrar / eliminar (es).** pieza_propia.borrada «eliminada» → «borrada».
20. **crear = Anlegen / Erstellen (de).** inicio.crear «Anlegen» → «Erstellen», como taller.crear.
21. **locutor = Ansager / Sprecher (de).** guia.7.pasos «Sprecher» → «Ansager».
22. **muñeco = Puppe / Testpuppe (de).** taller.probar_aqui_ayuda «Testpuppe» → «Puppe».
23. **gimnasio = Arenen (de).** especial.gimnasios «Arenen: die Grundlagen» choca con arena → «Gyms».
24. **práctica = Training / Übung (de).** temario.preparar («Übung vorbereiten»), temario.plan («Übungsplan»), temario.vacio → «Training», como pelear.practica; «Übung» es ejercicio.
25. **velocidad = Tempo / Geschwindigkeit (de).** entrenador.rival.rapido, entrenador.rival.rapido.trozo, entrenador.rival.rodea_izquierda.trozo, entrenador.rival.rodea_derecha.trozo → «Tempo», como valor.velocidad y cifras.velocidad.
26. **habilidad = skill (en).** pelear.azar_ayuda «skills» → «abilities», como la ficha mec_habilidades.
27. **orden = command (en).** pelea.codigo_no_compila «commands» → «orders».
28. **robot = bot (en).** destruccion.hud_bajas («Bots: {1}»), carrera_obs.desglose_bajas («Broken bots») → robots.
29. **empate = tie (en).** carrera_obs.empate «Technical tie» → «Technical draw» (o «Draw at the finish line»).
30. **alcance = reach (en).** pelea.visor_ayuda «arm reaches» → «ranges».
31. **Revisar el código.** de «Code prüfen» (final.revisar, guia.10.pasos) y «Code-Prüfung» (revision.titulo, tarea.pesado) chocan con «Prüfen» (ejercicio.comprobar) → propuesta «Code-Review». en guia.10.pasos cita «[[Review code]]», pero el botón final.revisar dice «Review the code»: igualarlos.
32. **Referencias a botones que no coinciden con el botón.**
    - temario.cargado: «Pulsa [[Preparar]]» / „[[Vorbereiten]]“ / “[[Prepare]]”, pero el botón (temario.preparar) es «Preparar la práctica» / „Übung vorbereiten“ / “Prepare the practice”.
    - entrenador.tip.juego_empezar_tutoriales y entrenador.tip.juego_demostraciones: de «[[Demonstrationen]]», en «[[Demonstrations]]», pero el botón (demo.boton) es «Demos» en los dos; también el título de como_se_juega.txt (=== demostraciones).
    - especial.carrera_entrar: en «Open Obstacle race» y especial.destruccion_entrar «Open Destruction & Survival»: nombres de modo dentro de un botón; sentence case y «and».
33. **Títulos con mayúsculas o «&» (en y de).** Pasar a sentence case y a «and» / «und»: menu.bloque_robot («Your Robot»; es «Tu Robot» → «Tu robot»), menu.depuracion, entrenador.tip.juego_depuracion, especial.destruccion, especial.destruccion_entrar, destruccion.titulo, carrera_obs.leyenda_ocultos, senior.presupuesto_titulo, pieza_propia.panel, pieza_propia.validar, pieza_propia.guardar_montar, pieza_propia.estado_inicial, pieza_propia.lista_para_montar, especial.senior («Senior Fights»), senior.rivales_titulo («Senior Rivals»), carrera_obs.maquina_nombre («AI Racer»), entrenador.ir_al_taller («Go to Workshop» → «Go to the workshop»), campanas.terminar_escena («Back to Story» → «Back to the story»), campanas.destacada («FEATURED STORY», en mayúsculas en los tres idiomas: si es un rótulo de estilo, vale; si no, estilo frase).
34. **Fuera de ui.txt.**
    - res/historia/historia.txt: los títulos de episodio en en van en Title Case («The Insufferable Kid», «The Old Man's Workshop», «Wax On, Polish Nut»…): → sentence case («The insufferable kid»). El de [senior] dice «Two Centimetres Taller»: inglés británico → «centimeters».
    - res/datos/como_se_juega.txt: es «El Taller» y «La Tienda» (=== taller, === tienda) → «El taller», «La tienda» (o «Taller», «Tienda», como el botón); de «Debugging und Testen» y en «Debugging and Testing» frente al botón «Debugging & Testing»; de «Demonstrationen» / en «Demonstrations» frente al botón «Demos»; de «Hindernisrennen» frente a «Hindernislauf».
    - res/datos/entrenadores.txt: los papeles en en dicen «trainer» («The grumpy old gym trainer») y el juego dice «coach» → coach.
    - res/conceptos/08_mecanicas.txt: «cargas / Ladungen / charges», «Last / load», «Abkühlen», «Photonensäbel», «Angreifer / Taktiker / Verteidiger» (ver puntos 7 y la tabla de arquetipos).
