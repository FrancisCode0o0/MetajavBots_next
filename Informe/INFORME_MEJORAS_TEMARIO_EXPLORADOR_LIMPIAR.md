# Informe: Explorador de archivos y botón «Limpiar» en «Mi temario»

**Fecha:** 2026-11-19  
**Alcance:** Dos mejoras de usabilidad en la pantalla «Mi temario» (`Temario.java`), manteniendo intactos el resto de pantallas, los dibujos/animaciones de los robots y la lógica de extracción de `LectorDeTemario`.

---

## 1. Archivos creados y modificados

- `juego/src/main/java/metajavbots/pantalla/SelectorDeArchivos.java` *(nuevo)* — Interfaz funcional independiente de ventana para abrir el selector nativo de archivos de forma asíncrona.
- `juego/src/main/java/metajavbots/Juego.java` — Añadido el campo `public volatile SelectorDeArchivos selectorDeArchivos` (siguiendo el mismo patrón de inyección que `archivosSoltados`).
- `lwjgl3/src/main/java/metajavbots/escritorio/SelectorDeArchivosEscritorio.java` *(nuevo)* — Implementación nativa de escritorio para LWJGL3.
- `lwjgl3/src/main/java/metajavbots/escritorio/Main.java` — Instalación de `SelectorDeArchivosEscritorio::abrir` en `juego.selectorDeArchivos`.
- `juego/src/main/java/metajavbots/pantallas/Temario.java` — Integración del selector al pulsar «Cargar», conservación del flujo por ruta manual/arrastrar y soltar, y botón «Limpiar» con reseteo completo del estado.
- `res/textos/ui.txt` — Nuevas claves trilingües (`ES | DE | EN`): `temario.elegir_titulo`, `temario.limpiar`, `temario.limpiar_ayuda`, y actualización de `temario.cargar_ayuda`.
- `pruebas/src/main/java/metajavbots/pruebas/PruebasDelTemario.java` — Añadidos dos casos de prueba unitaria para el flujo del selector de archivos/ruta manual y para la opción «Limpiar».

---

## 2. Cómo se implementó el selector de archivos

- **Comprobación previa del proyecto:** Se revisó el proyecto completo (incluida la exportación de robots en `Exportar.java` y `Taller.java`); ninguna pantalla usaba todavía un selector nativo de archivos (`Exportar` escribe directamente en `robots_exportados/`, y `Main.java` solo recibía archivos arrastrados mediante `filesDropped` → `juego.archivosSoltados`).
- **Arquitectura desacoplada y segura con LWJGL3:**
  - `juego` define la interfaz `SelectorDeArchivos` (sin depender de LWJGL3 ni de AWT) y `lwjgl3` inyecta `SelectorDeArchivosEscritorio::abrir` desde `Main.java`.
  - El diálogo se ejecuta en un hilo demonio propio (`selector-archivos`) para no bloquear el bucle de renderizado OpenGL/LWJGL3.
  - En Windows y Linux usa `java.awt.FileDialog` (`FileDialog.LOAD`) sobre un `Frame` oculto con `alwaysOnTop(true)` (liberado siempre en `finally`), filtrando por `LectorDeTemario.FORMATOS` mediante `FilenameFilter` y patrón `*.ext;...` en Windows. En macOS usa `osascript` (`choose file of type ...`) en proceso separado para evitar conflictos entre AWT/AppKit y el hilo principal de GLFW. En entornos headless devuelve `null` de forma segura.
  - Al confirmar un archivo, la ruta se deposita en un `AtomicReference<Path>` y se procesa en el siguiente fotograma dentro del hilo gráfico (`procesarArchivoPendiente`), rellenando el campo de ruta y llamando automáticamente a `cargar(...)`. Mientras el diálogo está abierto, el botón «Cargar» se desactiva para evitar aperturas duplicadas.
- **Flujo intuitivo con ruta manual:**
  - Pulsar **«Cargar»** con el campo vacío o con la última ruta ya cargada abre el explorador nativo (situado en la carpeta del archivo actual si existe).
  - Escribir una ruta nueva manualmente y pulsar **Intro** o **«Cargar»** carga esa ruta directamente sin perder la vía manual.
  - Arrastrar y soltar archivos en la ventana sigue funcionando igual que antes.

---

## 3. Cómo se implementó la opción «Limpiar»

- **Ubicación y diseño:** Se añadió el botón **«Limpiar»** (`Limpiar` / `Leeren` / `Clear`) junto a «Preparar la práctica» en la fila inferior izquierda, con anchos calculados dinámicamente (`limpiarW` + hueco + `prepararW <= w`) para no desbordar ni pisar el panel derecho en ningún tamaño de ventana (desde el mínimo `960×600` hasta `1600×900`) ni tamaño de fuente. Se activa solo cuando hay algo que limpiar (`hayContenidoParaLimpiar()`).
- **Reseteo completo (`limpiar()`):** Vacía la ruta (`ruta`), el historial de última ruta intentada, el editor (`texto`), el nombre guardado temporal (`nombre`), el mensaje de estado (`estado`), los temas detectados (`temas`), los ejercicios nuevos generados (`nuevos`), la petición de IA en curso (`peticion`), cualquier archivo pendiente del selector y reinicia el desplazamiento del panel derecho (`despl.alPrincipio()`). Además incrementa un contador de generación (`generacion`) para descartar resultados tardíos si había un hilo de validación de IA en marcha.
- Los temarios ya guardados en disco (`Mis temarios`) no se borran.

---

## 4. Dudas y puntos pendientes de revisión

1. **Verificación visual en ejecución real (Windows / macOS / Linux):** Las pruebas unitarias corren en modo headless (`java.awt.headless=true`) usando un selector inyectado. Conviene probar manualmente en escritorio:
   - Que en Windows el diálogo `FileDialog` aparezca por delante de la ventana de LWJGL3 y filtre correctamente las extensiones de `LectorDeTemario.FORMATOS` (en Win32 `FileDialog.setFile("*.txt;...")` muestra la máscara de extensiones en el cuadro de nombre de archivo; si se prefiriera un desplegable de tipos de archivo más limpio, podría valorarse añadir `lwjgl-tinyfd` en una iteración futura).
   - El comportamiento en macOS (`osascript`) y Linux (GTK), ya que esta sesión se ejecutó en Windows.
2. **Comportamiento al salir y volver tras limpiar:** Si el usuario pulsa «Limpiar», navega a otra pantalla (p. ej. un ejercicio guardado) y vuelve con «Volver», `alEntrar()` vuelve a cargar `temario.ejemplo` si el editor está vacío (comportamiento original dejado intacto por prudencia).
3. **Avisos preexistentes del compilador fuera del alcance:** Permanecen 2 avisos previos de cast redundante en `juego/src/main/java/metajavbots/pantallas/EscenaDeArena.java` (líneas 58 y 95), dejados sin tocar por estar fuera del alcance.

---

## 5. Resultado de compilación, estilo y pruebas

Comando ejecutado:

```powershell
.\gradlew.bat compileJava estilo pruebas:prueba
```

- **Compilación (`compileJava`):** `BUILD SUCCESSFUL` (0 avisos en los archivos creados o modificados).
- **Estilo (`estilo` — Checkstyle en `juego`, `lwjgl3` y `pruebas`):** `PASSED` (0 errores, 0 avisos).
- **Pruebas unitarias (`pruebas:prueba`):** **`114 casos bien, 0 mal`** (56 s), incluyendo:
  - `temario: pulsar Cargar abre el explorador con los formatos soportados y la ruta manual sigue funcionando` — **bien**
  - `temario: Limpiar vacía ruta, editor, estado y plan para cargar otro temario al instante` — **bien**
  - `textos: cada texto de la interfaz que usa el código está en los tres idiomas` — **bien**
