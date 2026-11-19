# Informe de Auditoría de Bugs y Optimización — MetajavBots

**Fecha:** 19 de noviembre de 2026  
**Proyecto:** MetajavBots (`motor`, `juego`, `lwjgl3`, `pruebas`)  
**Estado:** Todos los parches aplicados, compilación limpia (`-Xlint:all`), **0 avisos de Checkstyle** y batería de pruebas verificada.

---

## 0. Lista de puntos solicitados para revisar en la auditoría

1. **Fuga de foco / cursor entre pantallas:**
   - Un cursor de entrada de texto se queda activo o visible al cambiar a otra pantalla.
   - Revisar la limpieza de estados de UI, listeners de teclado/ratón y foco en las transiciones de pantalla (`Juego.java`, `Ui.java`, `Entrada.java`, `VentanaDeCodigo.java`, `CampoDeTexto.java` y pantallas afectadas).
2. **Recorte visual (clipping) — Cabeza del entrenador en el margen/esquina derecha:**
   - La cabeza de un personaje aparece medio cortada (aclarado específicamente como la cabeza del entrenador en la esquina/margen derecho).
   - Revisar `EntrenadorFlotante.java`, `CabezaDeEntrenador.java` y cualquier clase que posicione o recorte al entrenador en ese margen (comprobando margen superior y derecho, coordenadas X/Y, radio, adornos y límites de ventana).
   - Mantener intacto el efecto intencional de asomar media cabeza en reposo en `EntrenadorFlotante.java`, corrigiendo solo desbordamientos no intencionales reales (como iconos o adornos pisando bordes) y descartando cambios en los robots (`Campanas.java`, `AnimacionDelCaso.java`, `FinalesDelRobotito.java`, `Menu.java`, `Taller.java`).
3. **Ventana que desaparece con el juego/audio corriendo en segundo plano:**
   - A veces la ventana se cierra o desaparece, pero el audio y el proceso siguen activos hasta matar la terminal.
   - Revisar el ciclo de vida de la ventana, hilos y recursos que no se cierran (`dispose`, `System.exit`, hilos de audio/bucle principal, apagado limpio de MIDI en `Musica.java` y subprocesos en `Voz.java` y `CerebroLocal.java`) y posibles excepciones no capturadas en el hilo gráfico (`Juego.java`, `Main.java`).
4. **Latencia en diálogos de personajes:**
   - Auditar `Voz.java`, `VozPiper.java` y el flujo de reproducción de diálogos para detectar y corregir la causa del retraso en las voces.
5. **Auditoría abierta (bajo la regla innegociable de no romper lo que ya funciona ni alterar comportamientos intencionales):**
   - Detectar y corregir otros bugs reales de estabilidad, concurrencia, fugas de recursos o rendimiento evidente.

---

## 1. Resumen Ejecutivo

Se realizó una auditoría completa del código fuente de **MetajavBots** centrada en cinco áreas:
1. **Fuga de foco y cursor de texto** entre pantallas, pestañas y paneles laterales.
2. **Desbordamiento visual de la cabeza del entrenador** en el margen y esquina derecha (preservando intacto el comportamiento intencional de asomar media cabeza en reposo).
3. **Desaparición de la ventana con audio y proceso activos en segundo plano** por excepciones fuera del bloque protegido de renderizado y recursos MIDI/subprocesos no cerrados de forma síncrona.
4. **Latencia y pérdida de diálogos de personajes y locutor** al inicio de las rondas y durante la síntesis de voz con `Voz` / `VozPiper`.
5. **Auditoría abierta de estabilidad, concurrencia y fugas de recursos** (`PantallaLaboratorio`, `ScissorStack` en `Pincel`, e instancias de `HttpClient` en `CerebroHttp` y `CerebroLocal`).

---

## 2. Problema 1: Fuga de foco y cursor entre pantallas y paneles

### Causas raíz detectadas

1. **Ausencia de limpieza de foco y estado de entrada en transiciones de pantalla (`Juego.java` y `Entrada.java`):**
   - **Ubicación original:** `juego/src/main/java/metajavbots/Juego.java` (métodos `ir`, `irSolo`, `volver`, líneas `71-95`) y `juego/src/main/java/metajavbots/pantalla/Entrada.java` (líneas `66-121`).
   - **Diagnóstico:** `Juego` almacena las pantallas salientes en la pila `anteriores` (`Deque<Pantalla>`) y las recupera al llamar a `volver()`. Al cambiar de pantalla no se desenfocaban los `CampoDeTexto` activos ni se limpiaban las colas de eventos (`letras`, `teclas`, `desde[]`, `ultimaRepeticion[]`). Al volver a una pantalla apilada (como `Ajustes`/`PanelDeCerebros`, `Taller`, `PantallaEjercicio`, `PantallaLeccion` o `Temario`), el `CampoDeTexto` conservaba `enfocado = true` con el cursor parpadeando y capturando el teclado.

2. **`VentanaDeCodigo` nunca perdía el foco al hacer clic fuera (doble cursor activo simultáneo):**
   - **Ubicación original:** `juego/src/main/java/metajavbots/pantalla/VentanaDeCodigo.java` (líneas `54`, `167-169`, `213-217`, `506-508`).
   - **Diagnóstico:** `VentanaDeCodigo` nacía con `public boolean enfocada = true;` y al procesar clics solo activaba `if (e.dentro(x, y, w, h) && e.recienPulsado) enfocada = true;`, sin perder jamás el foco cuando el usuario hacía clic en un `CampoDeTexto` lateral (`tutor.pregunta`, `api.busqueda`, `Temario.ruta`) o pulsaba `F1` (`PanelDeApi.buscar`). Como consecuencia:
     - Ambos cursores (el del editor y el del `CampoDeTexto`) parpadeaban simultáneamente.
     - En pantallas donde `editor.dibujar(...)` se invocaba antes que el campo de texto (`Taller.java:194-195`, `PantallaEjercicio.java:199-202`), `editor.teclas(...)` consumía las teclas y ejecutaba `e.letras.clear()`, impidiendo escribir en el campo lateral.

3. **Bloqueo total de escritura en `PantallaLaboratorio.java` por visor de solo lectura enfocado:**
   - **Ubicación original:** `juego/src/main/java/metajavbots/pantallas/PantallaLaboratorio.java` (líneas `26`, `47-51`, `106-107`) y `VentanaDeCodigo.java` (líneas `506-508`).
   - **Diagnóstico:** `PantallaLaboratorio` declara dos instancias de `VentanaDeCodigo`: `clase` (`soloLectura = true`, dibujada primero en línea `106`) y `pruebas` (`soloLectura = false`, dibujada después en línea `107`). Como `clase.enfocada` era `true` por defecto y nunca pasaba a `false`, `clase.teclas(...)` se ejecutaba siempre antes que `pruebas.teclas(...)`, consumía las teclas de navegación y llamaba a `e.letras.clear()` cuando `soloLectura == true`, bloqueando la escritura en el editor de pruebas del jugador.

4. **Campos de texto que dejaban de dibujarse al cambiar de pestaña sin limpiar `enfocado`:**
   - **Ubicación original:** `juego/src/main/java/metajavbots/pantallas/PanelDeCerebros.java` (líneas `30-31`, `87-112`) y `Taller.java` (líneas `140-163`).
   - **Diagnóstico:** En una interfaz *immediate-mode*, `CampoDeTexto` solo actualizaba `enfocado = false` dentro de su propio método `dibujar(...)`. Si el jugador hacía clic en otra pestaña (cuya botonera se dibuja antes que el campo), el campo dejaba de dibujarse en ese mismo fotograma sin llegar a procesar el clic fuera, manteniendo `enfocado = true`.

### Solución aplicada
- **`Ui.java`:** Se implementó un registro centralizado de foco (`registrarFoco(Object, Runnable)`, `desenfocar(Object)`, `tieneOtroFoco(Object)`, `quitarFoco()`). Cuando un componente toma el foco, `Ui` ejecuta automáticamente el callback de desenfoque del componente anterior. Además, en `acabarFotograma()`, si el componente enfocado dejó de dibujarse en el fotograma actual (`!focoVisto`), se desenfoca automáticamente.
- **`Entrada.java` y `Juego.java`:** Se añadió `Entrada.limpiar()` y el método `Juego.limpiarAlCambiar()` invocado en `ir`, `irSolo` y `volver` (además de cerrar `entrenadorFlotante` en `irSolo`).
- **`CampoDeTexto.java`, `VentanaDeCodigo.java`, `PanelDeApi.java` y `PantallaLaboratorio.java`:** Se integraron ambos componentes con el registro de foco de `Ui`, se eliminó el `e.letras.clear()` destructivo de los visores `soloLectura`, y se inicializó `clase.enfocada = false` y `pruebas.enfocada = true` en `PantallaLaboratorio`.

---

## 3. Problema 2: Cabeza del entrenador en el margen/esquina derecha

### Causas raíz detectadas

1. **Pestaña lateral del entrenador (`EntrenadorFlotante.java` y `CabezaDeEntrenador.java`):**
   - **Ubicación original:** `juego/src/main/java/metajavbots/pantallas/EntrenadorFlotante.java` (método `pestana`, líneas `187-207`) y `juego/src/main/java/metajavbots/tutor/CabezaDeEntrenador.java` (líneas `18-79`).
   - **Diagnóstico:**
     - El efecto horizontal en reposo (`asomada = 0`, donde `cx = p.ancho - 26 + (1 - asomada) * 24` hace que asome media cabeza por el borde derecho y salga entera al pasar el ratón o mostrar un tip) **es un diseño intencional** y se ha mantenido intacto.
     - Verticalmente, la caja de la pestaña tenía `y = cy - r - 10` y altura `r * 2 + 20` (de `cy - 30` a `cy + 30` para `r = 20`), mientras que en `CabezaDeEntrenador.java` los adornos superiores (`gorra` en `y - 1.2f * r`, `estrellas` y `antena_rota` en `y - 1.25f * r` más la oscilación `0.08f * r`, llegando a `cy - 26.6` px) y el icono `?` / `+` en `cy - r - 8` (`cy - 28`) pisaban el borde superior (`cy - 30`), y la sombra (`cy + 29.8`) y `barba` de cables (`cy + 32` px) rebasaban el borde inferior (`cy + 30`).

2. **Recuadro del entrenador en la esquina inferior derecha durante la pelea (`EntrenadorEnLaPelea.java`):**
   - **Ubicación original:** `juego/src/main/java/metajavbots/pantallas/EntrenadorEnLaPelea.java` (líneas `160-163`).
   - **Diagnóstico:** El contenedor tenía una altura mínima `h = Math.max(70, ...)` y dibujaba la cabeza con centro `cy = y + 38` y radio `r = 24`. Cuando el texto ocupaba una sola línea (`h = 70`), la sombra y la barba del entrenador llegaban hasta `y + 38 + 1.60 * 24 = y + 76.4` px, sobresaliendo 6.4 px por debajo del borde inferior del panel (`y + 70`).

### Solución aplicada
- **`EntrenadorFlotante.java`:** Sin tocar la posición horizontal ni la animación `asomada`, se amplió el margen vertical de la caja de la pestaña en 4 px por arriba y por abajo (`cy - r - 14`, alto `r * 2 + 28`) y se ajustó la coordenada vertical del icono `?` / `+` a `cy - r - 11`.
- **`EntrenadorEnLaPelea.java`:** Se incrementó el alto mínimo del panel de `70` a `78` px.

---

## 4. Problema 3: Ventana que desaparece con el juego/audio corriendo en segundo plano

### Causas raíz detectadas

1. **Excepciones fuera del `try/catch` de `Juego.render()` y `Juego.resize()` que saltaban `cleanupWindows()` en `Lwjgl3Application`:**
   - **Ubicación original:** `juego/src/main/java/metajavbots/Juego.java` (líneas `66-68` y `134-172`).
   - **Diagnóstico:** En `Lwjgl3Application` (`gdx-backend-lwjgl3-1.14.2`, líneas `152-162`), el bucle principal ejecuta:
     ```java
     try {
         loop();
         cleanupWindows(); // Llama a Juego.dispose()
     } catch (Throwable t) {
         throw ...;
     } finally {
         cleanup();        // Cierra OpenAL y llama a GLFW.glfwTerminate()
     }
     ```
     En `Juego.java`, el bloque `try/catch (Throwable e)` solo cubría las líneas `146-150`. Quedaban fuera `Juego.resize(w, h)` (`pincel.redimensionar`), la transición de recuperación dentro del propio `catch` (`irSolo(...)`) y las llamadas de audio al final de `render()` (`voz.fotograma`, `sonidos.fotograma`, `musica.tocar(pantalla.musica())`, `musica.fotograma()`). Si cualquier excepción escapaba de esos puntos, `Lwjgl3Application` omitía `cleanupWindows()` (**no llamaba a `Juego.dispose()`**) y ejecutaba `GLFW.glfwTerminate()`, haciendo desaparecer la ventana inmediatamente mientras el proceso Java y el sintetizador MIDI seguían vivos.

2. **Subsistema MIDI de Java Sound (`javax.sound.midi`) independiente de OpenAL y cierre asíncrono en `Musica.liberar()`:**
   - **Ubicación original:** `juego/src/main/java/metajavbots/sonido/Musica.java` (líneas `67-73`, `108-143`, `176-185`).
   - **Diagnóstico:** `Musica` abre `MidiSystem.getSynthesizer()` y `MidiSystem.getSequencer(false)` en modo `LOOP_CONTINUOUSLY`. Estos dispositivos son de `javax.sound.midi` (no de OpenAL), por lo que `audio.dispose()` de libGDX no los cierra. Además, `Musica.liberar()` encolaba `hilo.submit(this::cerrarMidi); hilo.shutdown();` en un hilo daemon sin esperar (`awaitTermination`) y sin limpiar `sonando = ""`, pudiendo quedar el secuenciador abierto o reabrirse si `empezarMidi` estaba cargando un SoundFont en paralelo.

3. **Falta de cierre garantizado en `Main.java`, `Voz.java` y `CerebroLocal.java`:**
   - **Ubicación original:** `lwjgl3/src/main/java/metajavbots/escritorio/Main.java` (línea `33`), `Voz.java` (líneas `18-22`), `CerebroLocal.java` (líneas `246-282`, `313-319`) y `CerebroLocalPerezoso.java` (líneas `85-97`).
   - **Diagnóstico:**
     - `Main.main` no tenía un bloque `try / catch / finally` que garantizase `juego.dispose()` y `System.exit(0)`.
     - `Voz` no disponía de un método `liberar()` para cancelar procesos `piper.exe` en curso y cerrar su `ExecutorService`.
     - `CerebroLocal` no comprobaba `cerrando` dentro de los bucles de `arrancarElQueFuncione` y `esperarACargar`, y usaba `destroy()` en lugar de `destroyForcibly()` para `koboldcpp.exe`.

### Solución aplicada
- **`Main.java` y `Juego.java`:** Se envolvió `new Lwjgl3Application(juego, c)` en `try / catch / finally` con `juego.dispose()` idempotente (`liberado`) y `System.exit(0)`. Se protegieron `Juego.resize`, la recuperación en `render` y el bloque de audio de `render` con `try / catch (Throwable)`.
- **`Musica.java`:** `liberar()` limpia `activa`, `pedido` y `sonando`, espera la terminación del hilo MIDI (`awaitTermination(500 ms)`) y ejecuta `cerrarMidi()` de forma sincronizada deteniendo `secuenciador` y cerrando `sintetizador`.
- **`Voz.java`, `CerebroLocal.java` y `CerebroLocalPerezoso.java`:** Se añadió `Voz.liberar()` (llamado desde `Juego.dispose()`), comprobación de `cerrando` en los bucles de carga de `CerebroLocal`, `destroyForcibly()` sobre procesos hijos y parada del hilo vigilante de `CerebroLocalPerezoso`.

---

## 5. Problema 4: Latencia y pérdida de diálogos de personajes y voces

### Causas raíz detectadas

1. **Borrado de los sucesos de inicio de ronda (`RONDA_EMPIEZA` y `DECIR` de `alEmpezar()`) en el primer turno de `Pelea.avanzar()`:**
   - **Ubicación original:** `juego/src/main/java/metajavbots/combate/Pelea.java` (líneas `202-209` y `236-239`).
   - **Diagnóstico:** En `Pelea.empezarRonda()` (con `turno = 0`), se limpia `sucesosDelTurno`, se emite `Suceso.Tipo.RONDA_EMPIEZA` y se llama a `prepararRobot(...)`, que ejecuta `alEmpezar()` del robot del jugador (donde la plantilla por defecto ejecuta `decir("¡A pelear!")`, emitiendo `Suceso.Tipo.DECIR`). Sin embargo, al comenzar a correr los turnos en `PantallaPelea` o `MaquinaContraMaquina`, la primera línea de `Pelea.avanzar()` ejecutaba incondicionalmente `sucesosDelTurno.clear();` **antes** de que `vista.sucesos(...)` y `narrador.turno(...)` leyeran los sucesos del turno 0. Como resultado, se perdían tanto el bocadillo inicial de `alEmpezar()` como los saludos iniciales de los robots y del locutor (`Narrador.ronda`), quedando los personajes en silencio hasta el primer impacto o tras 7 segundos sin ataques.

2. **Encolado sin cancelación de procesos `piper.exe` obsoletos y lecturas repetidas de disco en `Voz.java` y `VozPiper.java`:**
   - **Ubicación original:** `juego/src/main/java/metajavbots/sonido/Voz.java` (líneas `49-69`) y `juego/src/main/java/metajavbots/sonido/VozPiper.java` (líneas `38-96`).
   - **Diagnóstico:**
     - `Voz` utiliza un `SingleThreadExecutor`. Cuando se pedía una frase nueva mientras `VozPiper` aún estaba sintetizando una anterior (arrancar `piper.exe` y cargar el modelo `.onnx` tarda ~1-3 s), el proceso `piper.exe` antiguo **no se cancelaba** y bloqueaba la cola hasta terminar de sintetizar toda la frase obsoleta.
     - `VozPiper.decir(...)` listaba el directorio `voces/` (`Files.list`) y leía el archivo `.onnx.json` (`Files.readString`) en cada frase sin cachearlos.
     - `Voz` volvía a sintetizar frases repetidas en lugar de reutilizar las muestras en memoria, y la clave de caché `"voz:" + textoListo` no distinguía por `idioma` ni `timbre`.

3. **Solapamiento de voces en `Sonidos.java`:**
   - **Ubicación original:** `juego/src/main/java/metajavbots/sonido/Sonidos.java` (líneas `70-84`).
   - **Diagnóstico:** `tocarMuestras` reproducía la nueva frase de voz sin detener la voz anterior (`ultimaVoz.stop()`), provocando que dos locuciones sonaran superpuestas.

### Solución aplicada
- **`Pelea.java`:** En `avanzar()`, se cambió `sucesosDelTurno.clear();` por `if (turno > 0) sucesosDelTurno.clear();`, conservando `RONDA_EMPIEZA` y los `DECIR` de `alEmpezar()` en el primer turno de la ronda.
- **`VozPiper.java` y `Voz.java`:** Se añadió seguimiento del proceso `enCurso` y `VozPiper.cancelar()` para abortar inmediatamente con `destroyForcibly()` cualquier síntesis obsoleta al llegar una frase nueva; se cachearon en memoria el ejecutable, el modelo `.onnx` por idioma y su frecuencia de muestreo; y se añadió en `Voz` una caché LRU de 32 frases sintetizadas indexada por `idioma + motor + timbre + texto`.
- **`Sonidos.java`:** Se añadió `ultimaVoz` para detener la locución previa antes de iniciar la siguiente.

---

## 6. Hallazgos Adicionales (Auditoría Abierta)

1. **Desbalance de `ScissorStack` ante excepciones a mitad de fotograma (`Pincel.java`):**
   - **Ubicación:** `juego/src/main/java/metajavbots/pantalla/Pincel.java` (líneas `81-84` y `375-393`).
   - **Causa y solución:** Si ocurría una excepción dentro de `pantalla.fotograma` mientras había un recorte activo (`p.recortar` / `ui.desplazable`), `p.sinRecorte()` no llegaba a ejecutarse y la pila `ScissorStack` de libGDX quedaba sucia para los fotogramas siguientes. Ahora `Pincel.acabar()` vacía siempre todos los recortes pendientes (`while (recortes > 0) sinRecorte();`) antes de `lote.end()`.

2. **Creación ansiosa y fuga de instancias `HttpClient` / hilos `SelectorManager` (`CerebroHttp.java` y `CerebroLocal.java`):**
   - **Ubicación:** `juego/src/main/java/metajavbots/cerebro/CerebroHttp.java` (líneas `28` y `163-170`) y `juego/src/main/java/metajavbots/cerebro/CerebroLocal.java` (líneas `77-92`).
   - **Causa y solución:** `CerebroHttp` instanciaba `HttpClient.newBuilder()...build()` en su constructor (abriendo un `Selector` NIO, un par de sockets loopback y un hilo `SelectorManager` incluso cuando no se enviaba ninguna petición, y lanzando `UncheckedIOException` si el loopback estaba restringido), y `CerebroLocal.escucha` creaba un `HttpClient` nuevo cada 1,5 s al sondear puertos. Ahora `CerebroHttp` crea su `HttpClient` de forma perezosa al enviar la primera petición y `CerebroLocal.escucha` reutiliza un único cliente compartido.

---

## 7. Archivos Modificados

| Archivo | Resumen del cambio |
| :--- | :--- |
| `juego/src/main/java/metajavbots/Juego.java` | Limpieza de foco/entrada en transiciones (`ir`, `irSolo`, `volver`), protección de excepciones en `resize` y `render`, y `dispose()` idempotente con liberación de `voz`. |
| `juego/src/main/java/metajavbots/pantalla/Ui.java` | Registro centralizado de foco (`registrarFoco`, `desenfocar`, `tieneOtroFoco`, `quitarFoco`) y desenfoque automático de componentes que dejan de dibujarse. |
| `juego/src/main/java/metajavbots/pantalla/Entrada.java` | Método `limpiar()` para vaciar colas de teclado, ratón y repetición de teclas al cambiar de pantalla. |
| `juego/src/main/java/metajavbots/pantalla/CampoDeTexto.java` | Sincronización de foco con `Ui` al pulsar dentro/fuera o `Esc`. |
| `juego/src/main/java/metajavbots/pantalla/VentanaDeCodigo.java` | Sincronización de foco con `Ui`, desenfoque al ceder foco a otro control y eliminación de `e.letras.clear()` en modo `soloLectura`. |
| `juego/src/main/java/metajavbots/pantalla/Pincel.java` | Vaciado garantizado de `recortes` (`ScissorStack`) en `acabar()`. |
| `juego/src/main/java/metajavbots/pantallas/PanelDeApi.java` | Devolución limpia del foco al editor al pulsar «Insertar». |
| `juego/src/main/java/metajavbots/pantallas/PantallaLaboratorio.java` | Foco inicial en el editor `pruebas` y desenfoque del visor `clase` de solo lectura. |
| `juego/src/main/java/metajavbots/pantallas/EntrenadorFlotante.java` | Margen vertical (+4 px arriba/abajo) y ajuste del icono `?`/`+` en la pestaña lateral, conservando la media cabeza en reposo. |
| `juego/src/main/java/metajavbots/pantallas/EntrenadorEnLaPelea.java` | Altura mínima de 78 px en el panel inferior derecho para que la cabeza/barba del entrenador no rebase el borde. |
| `juego/src/main/java/metajavbots/combate/Pelea.java` | Conservación de los sucesos de `empezarRonda()` (`RONDA_EMPIEZA` y `DECIR` de `alEmpezar()`) durante el primer turno de `avanzar()`. |
| `juego/src/main/java/metajavbots/sonido/Voz.java` | Cancelación inmediata de síntesis anteriores, caché LRU de frases sintetizadas y método `liberar()`. |
| `juego/src/main/java/metajavbots/sonido/VozPiper.java` | Cancelación de proceso `piper.exe` en curso (`cancelar()`) y caché en memoria de ejecutable, modelos `.onnx` y frecuencias. |
| `juego/src/main/java/metajavbots/sonido/Sonidos.java` | Parada de la locución anterior (`ultimaVoz.stop()`) antes de reproducir la nueva. |
| `juego/src/main/java/metajavbots/sonido/Musica.java` | Cierre síncrono y determinista de `Sequencer` y `Synthesizer` en `liberar()` y `cerrarMidi()`. |
| `juego/src/main/java/metajavbots/cerebro/CerebroLocal.java` | Comprobación de `cerrando` en bucles de arranque/espera, `destroyForcibly()` de procesos y `HttpClient` compartido en `escucha()`. |
| `juego/src/main/java/metajavbots/cerebro/CerebroLocalPerezoso.java` | Interrupción limpia del hilo vigilante en `cerrar()`. |
| `juego/src/main/java/metajavbots/cerebro/CerebroHttp.java` | Inicialización perezosa de `HttpClient` al enviar peticiones. |
| `lwjgl3/src/main/java/metajavbots/escritorio/Main.java` | Bloque `try / catch / finally` con registro de errores en `Cierre`, `juego.dispose()` y `System.exit(0)`. |
