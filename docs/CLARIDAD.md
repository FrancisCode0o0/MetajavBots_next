# Claridad: cómo se juega cada parte de MetajavBots

Repaso del juego entero con ojos de jugador nuevo (ENCARGO_CLARIDAD_METAJAVBOTS, 23-nov-2026). La idea de Fran:
«La dificultad aquí no tiene que ser de comprensión, sino de utilizar todo lo que ya está claro para hacer una buena
partida.» Para cada modo: qué hay que hacer, cómo se gana, qué parte de la API se usa, dónde se atasca o se confunde
quien empieza y qué se ha cambiado. Al final, las mecánicas, lo que decidí yo y lo que queda para Fran.

## Lo nuevo, en una tabla

| Qué | Dónde |
|---|---|
| «¿Cómo se juega?» en cada modo (sale sola la primera vez; luego, botón en la cabecera o «?» en la barra de la pelea) | `pantallas/ComoSeJuega`, `res/datos/como_se_juega.txt` |
| 24 fichas de mecánicas (qué es, cifras, en la pelea, desde tu código, idea en pseudocódigo) e índice | `res/conceptos/08_mecanicas.txt`, `pantallas/Mecanicas` |
| Camino «Empezar» en el menú: un solo camino que va abriendo lo demás; «Ver todo el menú» y vuelta en Ajustes | `pantallas/Camino`, `pantallas/TarjetaDelCamino`, `Menu` |
| Informe de la ronda que enseña: qué significa cada cifra (ratón), pista si va mal (amarillo), clic a su ficha | `pantallas/InformeDeRonda` |
| Modo «Paso a paso» en la pelea: para y abre el depurador; «Así quedan los mandos» (lo que falta a ruedas, cuerpo y mira; hacia dónde sale el disparo) | `PantallaPelea`, `PanelDepurador.mandos` |
| Consejo de órdenes al ritmo de la etiqueta, al borde de la arena (nunca encima) | `progreso/RitmoDeAvisos`, `EntrenadorEnLaPelea` |
| Fuera las dos líneas de la mira (pelea y depurador) | `PanelDepurador` |
| Tutoriales nuevos: «Así obedece tu robot» (antes de «Hola, arena»), «Tu primera pelea completa», «La carrera de obstáculos», «Destrucción y supervivencia», «Pelear en equipo» | `res/tutoriales/01, 05, 15` |
| Demostración nueva «Tu primera pelea, en palabras» (el robot lo hace en la arena; la hoja, en pseudocódigo) | `ejercicios/Demo5PrimeraPelea` |
| Prueba que lo recorre todo en los tres idiomas | `pruebas/PruebasDeClaridad` (11 casos) |

Nada de lo que ya existía con código (tutoriales, conceptos, pistas, demostraciones, plantillas) se ha quitado
(Fran: «No quites eso. Está bien»). Todo lo nuevo va en palabras y pseudocódigo, y dice qué métodos de la API existen;
el Java lo escribe el jugador.

## Modo a modo

### Menú
- **Qué hay que hacer**: elegir adónde ir. **Cómo se gana**: no se gana; la barra de arriba a la derecha dice cuánto falta para subir de rango.
- **API**: ninguna.
- **Dónde se atascaba**: diecisiete casillas iguales de golpe y ninguna dice «empieza aquí»; «¡A pelear!» destaca, pero sin compilar el robot no hace nada; jerga en las casillas («caja negra», «battle royale», «tope de cálculo»).
- **Cambiado**: quien empieza ve solo el camino «Empezar» (paso N de 16, una frase de qué se hace y un botón grande) y las casillas que ya ha abierto. Quien ya sabe pulsa «Ver todo el menú» (o lo quita en Ajustes). La primera vez sale «MetajavBots en dos minutos». Nuevo botón «Mecánicas» en el panel izquierdo.

### Taller
- **Qué hay que hacer**: escribir el código del robot y compilarlo (Ctrl+S). **Cómo se gana**: no se gana; si compila en verde, el robot ya pelea con ese código.
- **API**: `cadaTurno`, `alVerEnemigo`, `avanzar`, `girar`, `apuntarHacia`, `atacar` (las de la plantilla).
- **Dónde se atascaba**: muchos botones a la vez (API, Exportar .java, Formatear, Documentación, Código del último rival); «compilar» no se explica aquí; sin JDK el mensaje es técnico; nada dice qué hacer después.
- **Cambiado**: «¿Cómo se juega?» del Taller (compilar = pasar el corrector; el robot usa la última versión buena; mira la barra de peso) con las fichas del turno, el peso, las armas y la energía.

### Historia (campañas)
- **Qué hay que hacer**: ver la escena y pelear capítulo a capítulo. **Cómo se gana**: recibir menos daño que el rival; entonces se abre el capítulo siguiente.
- **API**: la de la pelea normal.
- **Dónde se atascaba**: no recuerda que el robot pelea con el código del Taller (se espera poder manejarlo); «[rango]» y «Homenaje a…» no dicen nada a quien empieza; en los jefes, el entrenador está en el hospital sin explicación previa.
- **Cambiado**: «¿Cómo se juega?» de Historia (también al entrar en su primera pelea).

### ¡A pelear!
- **Qué hay que hacer**: mirar cómo pelea tu robot; entre rondas, leer el informe y mejorar. **Cómo se gana**: menos daño recibido sumando todas las rondas (1200 turnos = 40 s por ronda; KO de cabeza acaba la ronda; diferencia < 0,5 = empate).
- **API**: `cadaTurno`, `alVerEnemigo`, `apuntarHacia`, `atacar`, `alRecibirGolpe`.
- **Dónde se atascaba**: la regla de ganar no salía en ningún sitio antes de pelear; el informe usaba jerga («topes de cálculo», «pasos por turno», efectos POO) sin explicarla; nada enseñaba el turno paso a paso.
- **Cambiado**: «¿Cómo se juega?» al empezar la primera pelea (la pelea espera a que se cierre) y botón «?» en la barra; informe que explica cada cifra y da pistas; «Paso a paso»; el aviso de órdenes al borde y a su ritmo.

### Desafíos (preparar la pelea)
- **Qué hay que hacer**: elegir rondas, rival y tipo (adaptada, al azar, desafío, práctica, todos contra todos, equipos, máquina contra máquina). **Cómo se gana**: casi siempre, menos daño; la práctica no cuenta.
- **API**: además, `enemigosVistos` (varios enemigos) y `avisarAlEquipo` / `alOirAlEquipo` (equipos).
- **Dónde se atascaba**: la regla de ganar solo estaba en la tarjeta de battle royale; jerga («desgaste», «tramo de peso», «robots ajenos»); página larga.
- **Cambiado**: «¿Cómo se juega?» de Desafíos y otro para Equipos / todos contra todos (al entrar en su pelea); ficha de desgaste y de equipos.

### Carrera de obstáculos
- **Qué hay que hacer**: llegar a la meta (a la derecha) esquivando trampas y rivales. **Cómo se gana**: el primero en cruzar; si se acaba el tiempo, el que va más adelante (menos de 5 de diferencia, empate).
- **API**: `obstaculos()`, `avanzar`, `girarHacia`, `atacar`, `enemigosVistos`.
- **Dónde se atascaba**: tiene su propio editor y nada dice que el código del Taller no se usa aquí; «Iniciar» sin compilar deja el robot quieto; leyenda densa («6×», «subnivel», «repentinos»).
- **Cambiado**: «¿Cómo se juega?» que lo dice en la primera frase; tutorial «La carrera de obstáculos» (tema 05) con una predicción; paso del camino que abre la casilla.

### Destrucción y supervivencia
- **Qué hay que hacer**: aguantar 60 s en una arena enorme rompiendo cosas. **Cómo se gana**: seguir en pie al final (KO = derrota); cada cosa o robot roto da monedas, sobrevivir +0,5.
- **API**: `obstaculos()`, `enemigosVistos`, `atacar`, movimiento.
- **Dónde se atascaba**: está escondido (Modo especial → última pestaña); textos llenos de cifras («≥ 2,08× sobre Carrera…»); editor aparte.
- **Cambiado**: «¿Cómo se juega?» y tutorial «Destrucción y supervivencia» (dice dónde está).

### Minijuegos
- **Qué hay que hacer**: cinco retos contra el muñeco, una cosa nueva cada uno (moverse, girar el radar, apuntar, disparar con cabeza, esquivar). **Cómo se gana**: la meta de cada tarjeta en una ronda (p. ej. 3 aciertos).
- **API**: `avanzar`, `restanteAvance`, `girar`, `alChocarConPared`, `apuntar`, `apuntarHacia`, `alVerEnemigo`, `atacar`, `normalizar`, `obstaculos()`.
- **Dónde se atascaba**: «Recorre al menos 2500» sin unidad; «Jugar» usa el código del Taller y la tarjeta no lo dice.
- **Cambiado**: «¿Cómo se juega?» que dice que primero se escribe y compila en el Taller. (La unidad de «2500» queda para Fran: ver abajo.)

### Modo especial
- **Qué hay que hacer**: jefes de tema, tope de cálculo, peleas Senior, retos de programar, robots de otros, Destrucción. **Cómo se gana**: cada pestaña dice su meta; en las peleas, menos daño.
- **API**: la de pelea; en los jefes, el tema de Java del jefe.
- **Dónde se atascaba**: nada está cerrado por rango: quien empieza lee «Atomic*, ConcurrentHashMap» o «presupuesto ms/KB»; «tope de cálculo» y «pasos» no se explicaban.
- **Cambiado**: «¿Cómo se juega?» del modo (con «es para cuando ya te defiendes») y uno propio en la pestaña Senior; fichas de jefes, tope y presupuestos Senior; en el camino, la casilla se abre al final.

### Peleas Senior
- **Qué hay que hacer**: pelear con presupuesto de tiempo y memoria por turno (2,5 ms / 8 KB / 350 pasos o 1,5 ms / 4 KB / 220 pasos), o 2 contra 2 en paralelo. **Cómo se gana**: menos daño; pasarse cuesta 6 de energía y 2 turnos.
- **API**: la de pelea; el reto es cómo se escribe.
- **Dónde se atascaba**: jerga de concurrencia sin explicar.
- **Cambiado**: «¿Cómo se juega?» Senior y ficha «Presupuestos Senior».

### Ejercicios
- **Qué hay que hacer**: escribir la respuesta y comprobar (Ctrl+S). **Cómo se gana**: todos los casos en verde (la solución se abre tras 3 intentos; cada uno nuevo da 0,25 monedas).
- **API**: ninguna (Java puro), salvo los del bloque del juego (`24_juego`).
- **Dónde se atascaba**: temas con códigos («01 …»); ningún orden recomendado; los ejercicios que enseñan el juego estaban escondidos en el bloque 4.
- **Cambiado**: «¿Cómo se juega?»; los ejercicios del juego son el primer tramo del camino «Empezar», uno por paso y una mecánica nueva cada vez.

### Tutoriales
- **Qué hay que hacer**: leer páginas cortas y hacer la tarea (ejercicio o prueba en la arena). **Cómo se gana**: la lección queda hecha con todas sus tareas.
- **Dónde se atascaba**: las tareas de arena necesitan un robot compilado (requisito escondido); ningún tutorial enseñaba cómo obedece el robot antes del primer código.
- **Cambiado**: «Así obedece tu robot» antes de «Hola, arena» (sin código, con el dibujo del turno y una predicción); «Tu primera pelea completa» justo después (crear, compilar, pelear, leer el informe, mejorar una sola cosa, volver a pelear); «La carrera de obstáculos», «Destrucción y supervivencia» (tema 05) y «Pelear en equipo» (tema 15).

### Demostraciones
- **Qué hay que hacer**: mirar; Continuar / Repetir paso. **Cómo se gana**: no se gana.
- **Dónde se atascaba**: las cuatro enseñan Java desde el principio.
- **Cambiado**: «Tu primera pelea, en palabras» (siete pasos con el robot en la arena y la hoja en pseudocódigo, con las mismas líneas resaltadas); «¿Cómo se juega?» que pide adivinar antes de Continuar (PRIMM).

### Tienda
- **Qué hay que hacer**: cambiar monedas por piezas. **Cómo se gana**: al comprar, un reto de código corto deja la pieza lista para montarla.
- **API**: ninguna directa; cambia lo que dicen `armaIzquierda`, `alcanceDerecha`, `pesoTotal`…
- **Dónde se atascaba**: el reto de código tras comprar sorprende; el peso no se mira antes de comprar.
- **Cambiado**: «¿Cómo se juega?» de la Tienda y fichas de tienda, monedas y rangos, peso y armas.

### Editor de piezas (Diseña tu pieza)
- **Qué hay que hacer**: escribir una clase que reparte 100 puntos. **Cómo se gana**: «Compilar y validar» y, si cabe en el presupuesto, «Guardar y montar».
- **Dónde se atascaba**: «Vigilante/Cargador» sin explicar.
- **Cambiado**: «¿Cómo se juega?» de la pieza.

### Depuración y Testing
- **Qué hay que hacer**: arreglar robots rotos (con pista) y el laboratorio de pruebas. **Cómo se gana**: el robot roto compila, has cambiado su código y cumple su meta.
- **Dónde se atascaba**: casi todo es jerga (F9, caja negra/blanca, mutantes, @Test).
- **Cambiado**: «¿Cómo se juega?»; «Paso a paso» en la pelea.

## La prueba de los dos minutos

Con las capturas `38a` (menú del camino) y `38c` («MetajavBots en dos minutos»): en la primera pantalla hay una sola
tarjeta con una frase; la tarjeta de dos minutos dice qué es un robot («pelea solo»), qué es una orden («una línea de tu
código, como "avanza 50 pasos"») y cómo se gana («quien recibe MENOS daño») en tres frases, cada una de una o dos líneas.
Nada pide leer más de dos frases seguidas para entenderlo.

## Mecánicas: la lista y sus fichas

Veinticuatro fichas, mirando el código: el turno y los tres mandos · cómo se gana · daño por partes y abolladuras ·
energía · armas (enfriamiento y cargas) · potencia y calor · escudos y especiales de cabeza · la habilidad del modelo ·
peso, carga y piernas · terrenos · choques y embestidas · trampas y cosas que se rompen · aparatos · desgaste entre
rondas · tope de cálculo y errores · presupuestos Senior · el radar · la Tienda y las piezas · monedas y rangos · la
naturaleza del modelo · cómo escribes tu clase (POO) · equipos y varios enemigos · jefes · el arma especial.

Se llega a ellas desde el índice («Mecánicas» en el menú), desde cada «¿Cómo se juega?» («Las reglas que entran en
juego» y «Todas las mecánicas»), desde cada cifra del informe de la ronda (clic) y desde el entrenador (F2: son
conceptos del nivel «El juego» y se encuentran buscando).

**Mecánicas que no caben en pocas frases** (las fichas dan lo esencial y un ejemplo; el detalle está en el código):
- **Armas**: hay unas 40, cada una con daño, alcance, enfriamiento, energía, peso y a veces cargas, empuje o vuelco. La ficha da dos ejemplos; la Tienda enseña las cifras de cada una.
- **Efectos de tu clase (POO)**: ocho reglas (private, protected, de paquete, public, final, static en equipo) con sus topes. La ficha da las tres que más pesan.
- **Peso y locomoción**: cuatro tramos de carga y seis tipos de piernas con su tabla por terreno. La ficha da los tramos y una frase por pierna.
- **Naturaleza y series Mk**: +8 % propio, núcleos del 6 % por Mk (la mitad si no coinciden), +1,5 % por pieza; y la serie Mk cambia 17 cifras distintas. Solo va lo esencial.
- **Potencia**: la fórmula (daño ×(4p+2·máx(p−1,0))/4, velocidad, calor, energía) se da con el caso de potencia 3.

## Lo que decidí yo (Fran, cámbialo si quieres)

- **Ritmo del consejo de órdenes** (`progreso/RitmoDeAvisos`, con `// TODO-FRAN:`): se cuentan solo las peleas en las que el jugador comete ese fallo (por separado la orden borrada y el ataque con la mira lejos) y el entrenador está. Noob: la 1.ª, la 3.ª, la 5.ª… (una sí y otra no). **Junior: al azar con un 40 %, nunca dos seguidas** (propuesta). **Senior: la 1.ª y luego una de cada 4** (propuesta). **Master: la 1.ª y luego una de cada 6** (propuesta). La primera vez sale en todas las etiquetas (quien entra en Junior por la prueba de nivel nunca lo ha oído).
- **Dónde sale el consejo**: en un panel a la derecha de la arena, que entra despacio y la arena se encoge para dejarle sitio (el mismo hueco que el depurador). Nunca encima. Con dos botones: «Paso a paso» y «Entendido». Si el depurador está abierto, ya lo dice el depurador. Puede salir a mitad de ronda; contra jefes no.
- **Las dos líneas de la mira**: quitadas de la pelea y del depurador. El dibujo animado del tutorial 1 (no es la pelea) sigue igual.
- **«Paso a paso»**: el mismo hueco de la barra que «Un turno» (en marcha dice «Paso a paso»; en pausa, «Un turno»). En el depurador, encima de las órdenes del turno, «Así quedan los mandos».
- **El botón «?» de la barra de la pelea** solo sale si cabe (pantallas de 760 de ancho o más); el resto de pantallas lo tiene en la cabecera.
- **«¿Cómo se juega?» sale solo la primera vez** que se entra en cada modo, encima de todo y con un solo botón para seguir (Esc o Intro también). Si a la vez sale el aviso de compilar del menú, va primero el de «¿Cómo se juega?».
- **Camino «Empezar»**: 16 pasos (el tutorial sin código, tres ejercicios del juego suaves, «Hola, arena», la primera pelea completa, una pelea de verdad, el resto de ejercicios del juego y la carrera). Lo que abre cada paso está en `Camino.PASOS`. Las partidas guardadas de antes que ya habían jugado ven el menú entero; las nuevas, el camino.
- **Tareas de los tutoriales nuevos**: ejercicios que ya existían (`j_hasta_la_bandera`, `j_mas_cercano`, `j_elegir_brazo`, `t15_peso_total`) y dos pruebas en la arena en la primera pelea.

## Lo que queda (para Fran o para otro encargo)

- **Ayudas que se van retirando** (ejemplo completo → huecos → líneas desordenadas tipo Parsons → desde cero): el juego no tiene ejercicios de huecos ni de ordenar líneas. Los tutoriales nuevos ya van en ese orden (idea en palabras → predicción → tarea), pero un tipo de ejercicio «Parsons» sería código nuevo en el banco de ejercicios.
- **Minijuegos**: «Recorre al menos 2500» sigue sin unidad (son unidades de la arena: el ancho de la arena es 1000).
- **Menú, Historia**: el texto dice «dos campañas largas y dieciocho historias», pero `res/datos/campanas.txt` tiene 22 bloques. No lo toqué: quizá alguno es secreto o de pruebas.
- **Estadísticas**: la tabla por modo enseña los ids internos (ADAPTADA, TUTORIAL, CAMPANA…) sin traducir.
- **Carrera**: el contexto que se le pasa a la IA del entrenador está en inglés fijo (no se ve, pero cambia el idioma de la respuesta si el modelo se despista).
- **Modo especial**: nada se cierra por rango; el camino lo abre al final, pero con «Ver todo el menú» se ve desde el principio.
- **La caja de consejos del entrenador contra el enemigo marcado** (clic en un enemigo) sigue sobre la arena, abajo a la derecha, como antes: no es el aviso de órdenes y la abre el jugador.
