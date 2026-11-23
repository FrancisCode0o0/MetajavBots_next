# Informe completo de funcionalidades actuales de MetajavBots

**Fecha:** 19 de noviembre de 2026  
**Estado técnico verificado:** compilación limpia con `-Xlint:all`, Checkstyle (`estilo`) con 0 avisos y **130/130** pruebas unitarias pasando (`gradlew compileJava estilo pruebas:prueba`).

---

## 1. Resumen ejecutivo

**MetajavBots** es un videojuego educativo en Java (con motor gráfico libGDX) orientado a aprender Java desde cero absoluto hasta nivel Senior programando robots de combate autónomos que pelean por rondas. Funciona íntegramente en **español, alemán e inglés** (interfaz, API del robot, tutoriales, ejercicios, diálogos, voz y documentación) y puede jugarse **100 % sin IA** o con hasta **3 cerebros de IA simultáneos** (locales y/o en línea).

### Cifras actuales del proyecto
- **Código del juego:** 200 clases Java en `juego/` (59 pantallas y paneles en `metajavbots.pantallas`).
- **Pruebas automatizadas:** 19 clases en `pruebas/` con **130 casos** sin ventana (`gradlew pruebas:prueba`), más sistema automatizado de capturas (`gradlew lwjgl3:capturas`) con soporte de idioma, tamaño de letra (`-Pletra=26`) y grabación GIF.
- **Contenido integrado:**
  - **22** modelos de robot con silueta, colores, naturaleza, biografía y personalidad.
  - **236+** piezas intercambiables (armas, cabezas, piernas/locomoción y núcleos, con series Mk I–V y rarezas).
  - **272** ejercicios autocorregibles (22 temas de Java + muestra + 10 ejercicios específicos de mecánicas del juego).
  - **70** lecciones interactivas organizadas en 22 temas.
  - **140** conceptos explicados en 3 idiomas en el entrenador flotante (`F2`).
  - **14** paquetes de Java documentados en el visor Javadoc de la caja de arena.
  - **18** campañas narradas (3 capítulos cada una) y **7** entrenadores con voz propia.
  - **9** arenas de combate (5 clásicas + 4 con trampas y física especial).
  - **6** aparatos conectables por código en la arena, **6** robots rotos para depurar y **3** laboratorios de pruebas unitarias.

---

## 2. Flujo de entrada, partidas y progresión

- **Gestión de partidas (`Inicio`):** crear, abrir, renombrar y borrar partidas (las borradas se archivan en `partidas/_borradas/` para evitar pérdidas accidentales).
- **Bienvenida y prueba de nivel (`Bienvenida`, `PruebaDeNivel`, `GeneradorDePreguntas`, `PreguntasDeLaIa`):**
  - Quien empieza de cero arranca en **Noob I**.
  - Quien ya sabe algo hace una prueba dinámica de nivel con preguntas generadas aleatoriamente (y opcionalmente creadas y verificadas ejecutando código real con IA).
- **Sistema de progresión (`Progreso`, `Etiqueta`, `Historial`):**
  - 12 etiquetas (**Noob I–IV**, **Junior I–IV**, **Senior I–IV**) más etiqueta secreta.
  - Experiencia, botín de piezas, desbloqueo de modelos y ranuras adicionales de robots propios.
  - Historial completo de combates con estadísticas (victorias, derrotas, daño recibido/infligido, modo y fecha).
- **La hora de hoy (`SesionDeHoy`, `SesionDeUnaHora`):**
  - Plan guiado de 60 minutos desde el menú principal: ~20 min de repaso del tema empezado más flojo (2 ejercicios pendientes), ~25 min de tema nuevo (lección + primer ejercicio) y ~15 min de combate práctico.

---

## 3. Taller, editor tipo IDE y banco de pruebas

- **Editor de código profesional (`VentanaDeCodigo`, `Documento`, `Formateo`, `RevisorAlEscribir`):**
  - Coloreado léxico de Java, números de línea, guías de sangría, emparejamiento de llaves y resaltado de palabra bajo el cursor.
  - **Revisión en vivo al escribir:** compila en segundo plano tras 1 s de pausa y subraya errores en rojo con explicaciones adaptadas al nivel del jugador.
  - **Documentación al pasar el ratón (`DocumentacionPropia`, `Referencia`):** muestra Javadoc tanto de la API y Java estándar como de las propias clases, métodos y campos del jugador.
  - **Autocompletado (`Ctrl+Espacio` y tras `.`)** incluyendo miembros de las clases del jugador.
  - Atajos completos: `Ctrl+S` compilar, `Ctrl+Z/Y` deshacer/rehacer (hasta 1000 pasos), `Ctrl+F/H` y `F3` buscar/reemplazar, `Ctrl+D` duplicar línea, `Ctrl+Mayús+K` borrar línea, `Alt+↑/↓` mover líneas, `Ctrl+/` comentar, `Tab/Mayús+Tab` sangrar bloques, `Ctrl+Mayús+F` formateo estilo Google/Oracle, `Ctrl+rueda/±` zoom y barras de desplazamiento vertical/horizontal.
- **Panel de API (`PanelDeApi`):** buscador rápido (`F1` o `Ctrl+clic`) en la API de robots y 239 entradas de referencia de Java, con botón **Insertar**.
- **Documentación de paquetes (`PanelDePaquetes`):** explorador Javadoc de los 14 paquetes permitidos en la caja de arena (clases, interfaces, enums, métodos clave, imports y restricciones de seguridad).
- **Banco de pruebas integrado (`BancoDePruebas`):**
  - Arena en vivo junto al editor para probar el robot al instante contra un muñeco (sin afectar historial ni progreso).
  - 4 modos: *Quieto*, *En movimiento*, *Con obstáculo* y *Con aparato*.
  - Controles de pausa, avance paso a paso, velocidad y reinicio instantáneo con `Ctrl+S`.
- **Código del último rival (`CodigoDelRival`):** permite leer el código real del último rival combatido (o de un sparring si aún no se ha peleado) con comentarios explicativos en ES/DE/EN.
- **Gestión de robots y piezas (`Taller`):** cambiar de modelo (con vista previa y biografía), equipar piezas por hueco (cabeza, brazos izquierdo y derecho, piernas, núcleo y naturaleza), consultar estadísticas y exportar `.java` a `robots_exportados/`.

---

## 4. Compilador, API trilingüe y caja de arena

- **API trilingüe (`Metajavbot`, `MetajavbotDe`, `MetajavbotEn`):**
  - 54 métodos de movimiento, giro, apuntado, ataque por brazo, especiales de cabeza, sensores (`enemigosVistos`, `obstaculos`, `aparatos`, `companeros`), memoria persistente entre rondas (`memoria()`), comunicación en equipo (`avisarAlEquipo` / `alOirAlEquipo`) y utilidades matemáticas.
  - Los robots de cada idioma ven exclusivamente los nombres de su idioma.
- **Compilación en memoria y seguridad (`Compilador`, `Inyector`, `Vigilante`, `Cargador`, `Tope`):**
  - Compila en memoria con `javac` del JDK (sin escribir en disco) y traduce errores a línea/columna originales del jugador. Sin JDK, el robot entra en modo piloto automático con aviso claro.
  - **Inyector de pasos y traza:** inserta `Tope.paso()` en bucles y métodos para cortar bucles infinitos o recursiones descontroladas (el exceso de cálculo cuesta turnos de deuda) y `Tope.linea(n)` para trazar la ejecución línea a línea sin alterar las líneas ni el coste de pasos.
  - **Vigilante de bytecode:** bloquea acceso a archivos, red, procesos, reflexión, `System.exit`, `System.in` e hilos no permitidos en robots. `System.out` se redirige a la consola del robot.

---

## 5. Sistema de combate, modos de pelea y depuración en vivo

### Mecánicas de combate (`Pelea`, `Cuerpo`, `Armas`, `Arena`, `Aparatos`)
- **Combate por rondas configurables:** 3, 4, 6 (por defecto), 10 o 12 rondas; con más de 4 rondas se activa el sistema de **desgaste** entre asaltos.
- **Daño localizado y visual:** cabeza (si se rompe, fuera de combate), brazo izquierdo, brazo derecho y piernas; abolladuras y quemaduras persistentes, humo progresivo, chispas, sacudidas y 5 estilos de explosión final.
- **Arsenal variado:** láseres (se detienen en el primer robot que cruzan), balas/perdigones, misiles teledirigidos, lanzallamas, escudos, brazos reparadores, y armas cuerpo a cuerpo de competición (puño, espada, martillo aturdidor, taladro, látigo, sierra circular, pinza aplastadora, volteador que lanza por los aires, cuña empujadora, bola con cadena y combinaciones).
- **9 escenarios de arena:** 5 clásicos (Hangar, Desierto, Neón, Laboratorio, Luna) y 4 con trampas y física interactiva (Chatarrería con pinchos e imán, Cocina con sierras y carrito, Hielo resbaladizo con foso, Tejado lluvioso con rayo).
- **6 aparatos conectables por código:** *Jetzazo*, *Flotón*, *Chispazo*, *Chamuscator*, *Resbalín* y *Pegotón*. Aparecen en la arena, se inspecciona su clase documentada desde la pelea, se recogen al tocarlos, se conectan al hueco correcto (`conectar(...)`) y se activan con `usarAparato(...)`.
- **Armas especiales con cuenta atrás (`ArmaEspecial`):** evento interactivo en peleas 1v1 donde se congela el combate y hay que resolver un reto breve de puntería (Pitágoras, `atan2` o disparo con adelanto) antes de que acabe el tiempo.
- **Ambientación:** entradas épicas estilo boxeo (`EntradaEpica`), público reactivo en grada (`Publico`), locutor robot con voz (Piper TTS local o sintetizador propio), música MIDI original multipista (con soporte para SoundFont `.sf2` y carpeta `musica_personal/`) y **repetición a cámara lenta del mejor golpe** (`Repeticion`).
- **Pausa entre rondas:** informe estadístico detallado, consejos del `Revisor`, edición y recompilación del código y cambio de piezas conservando `memoria()`.

### Herramientas en vivo durante la pelea
- **Visor táctico (`F8`, `VisorTactico`):** superpone en la arena el cono de radar, alcances de armas, ángulo relativo al rival marcado y zonas de trampas.
- **Depurador en vivo (`F9`, `PanelDepurador`):**
  - Barra de pasos de cálculo por turno, telemetría (`x`, `y`, `rumbo`, `mira`, `blindaje`, `energía`, `turno`) y dos pestañas compactas:
  - **Pestaña «Variables»:** inspección en vivo por reflexión de todos los campos propios del robot y su consola (`System.out`, avisos de error y topes).
  - **Pestaña «Código / Turno»:**
    1. Código del jugador en solo lectura con las líneas ejecutadas en el último turno iluminadas y `▶` en la última línea ejecutada (ideal con pausa `Espacio` y paso a paso `→`).
    2. Lista **Órdenes del turno** indicando qué método pidió qué acción y en qué línea (p. ej. `alVerEnemigo -> apuntarHacia(20°) (L6)`).
    3. **Detección de conflictos en ámbar:** cuando una llamada posterior en el mismo turno sobrescribe o anula el avance, giro o apuntado de un evento anterior, lo destaca en ámbar con el aviso exacto (p. ej. `⚠ ¡Pisado por cadaTurno() en línea 13!`), además de mostrar `⚠` en la pestaña para alertar desde cualquier vista.

### Modos de pelea (`PrepararPelea`, `Campanas`, `MaquinaContraMaquina`, `MiniJuegos`)
- **Contra la computadora (adaptada)**, **Modo azar**, **Desafío** (IA superior con premio de subida de nivel o pieza legendaria), **Práctica** (1 ronda sin contar), **Battle Royale** (todos contra todos), **Por equipos** (2v2 con comunicación entre compañeros), **Robots de otros jugadores** (`robots_ajenos/`), **18 Campañas** narradas, **Máquina contra máquina** (dos IA con su código trazado lado a lado) y **4 Minijuegos** (Moverse, Girar el radar, Apuntar, Disparar con cabeza).

---

## 6. Aprendizaje estructurado: ejercicios, tutoriales, modo especial y temario

- **Ejercicios (`Ejercicios`, `PantallaEjercicio`, `Comprobador`, `AccionesDelRobotito`, `PistaDeCarrera`):**
  - 272 ejercicios en 24 bloques (incluyendo 10 de mecánicas reales del juego con escenas de arena *giro* y *avance*).
  - Al comprobar, un **robotito animado** ejecuta físicamente lo que devuelve el código (cajas, pizarra, papel, estantería, puertas, alarma) sincronizado con la traza de líneas, junto a una **pista de carrera** contra el fantasma de la solución (vallas por bucle, carteles de ramas y desvíos) y 6 finales animados.
  - **Reto diario con racha** (experiencia extra creciente) y botón **Ejercicio nuevo** (generado por plantillas paramétricas o por IA comprobada).
- **Tutoriales (`Tutoriales`, `PantallaLeccion`):** 70 lecciones en 22 temas con tareas directas al ejercicio y a la arena.
- **Modo especial (`ModoEspecial`):**
  - **Gimnasios y Jefes de tema (`Jefe`, `DetectorDeTemas`):** 7 líderes de gimnasio + 9 jefes + **4 jefes Senior** (records, jerarquías selladas con pattern matching, inmutabilidad y patrón estrategia) con vitrina de medallas; solo reciben daño real si el código del jugador emplea su concepto.
  - **Tope de cálculo:** peleas con límite estricto de pasos por turno para practicar eficiencia algorítmica.
  - **Robots rotos (`RobotEnReparacion`):** 6 robots averiados con bugs reales que se reparan visualmente en vivo conforme el jugador corrige su código.
  - **Laboratorio de pruebas (`PantallaLaboratorio`, `InspeccionDelLaboratorio`):** 3 retos para escribir pruebas unitarias al estilo JUnit (`@Test`, `Assertions`, `@BeforeEach`, `@DisplayName`).
- **Mi temario (`Temario`, `LectorDeTemario`, `TemarioGuardado`):**
  - Carga apuntes o temarios propios desde archivo (explorador o arrastrar y soltar) en **PDF, DOCX, ODT, ODP, PPTX, HTML, RTF y TXT**.
  - Con IA genera ejercicios por tema, los comprueba y los guarda como **campañas propias** jugables después sin IA.

---

## 7. Entrenador flotante y sistema de cerebros de IA

- **Entrenador flotante (`EntrenadorFlotante`, `EntrenadorEnLaPelea`, `ConsejosDelTaller`, `LecturaDelRival`, `Hospital`):**
  - Accesible en cualquier pantalla (`F2` o pestaña lateral derecha), con 7 personajes y voces diferenciadas.
  - **Sin IA:** busca y explica los 140 conceptos integrados (metáfora de combate, error típico, pseudocódigo y ejemplo en el idioma del jugador), analiza los 113 códigos de error de `javac` desde el editor, da tips periódicos en el taller y analiza tácticamente al enemigo marcado con clic durante la pelea.
  - **Contra jefes:** aparece ingresado en el hospital con 10 partes médicos cómicos y protagoniza un «milagro» al vencer.
  - **Nivel de ayuda regulable:** automático según etiqueta/atasco y ajustable con *«− Dame menos / + Dame más»*.
- **Cerebros de IA opcionales (`PanelDeCerebros`, `CerebroLocal`, `CerebroHttp`, `GuiaDeCerebros`):**
  - Hasta **3 ranuras simultáneas** (locales vía KoboldCpp con gestión automática de RAM y arranque/apagado bajo demanda, o en línea con **Claude, Gemini, OpenAI** u otro endpoint compatible), repartibles por función (diálogos/locutor, revisión/temario, tutores) y con respaldo entre ranuras.
  - Incluye **Guía ilustrada de 10 páginas** para configurar modelos locales y claves API desde cero.

---

## 8. Accesibilidad, ajustes y audio

- **Ajustes (`Ajustes`):** idioma (`es`, `de`, `en`), tamaño de letra escalable (verificado hasta `letra=26`), tema oscuro y claro (contraste WCAG AAA), música y efectos de sonido, voz del locutor (Piper TTS / voz sintética), activar/desactivar entrenador, arenas con trampas, aparatos en la arena y armas especiales.
- **Panel de la IA (`PanelIA`):** configuración de dificultad fija o adaptada, agresividad y tono de humor del locutor y los rivales.
