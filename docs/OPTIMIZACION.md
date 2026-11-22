# MetajavBots · Optimización y unificación de código

Informe técnico de la optimización de recursos (CPU, memoria, basura por fotograma, tiempo de arranque y simulación) y refactorización de código duplicado en MetajavBots (versión de escritorio LWJGL3), conservando intacto el comportamiento, aspecto visual, sonido, API trilingüe y formatos de archivo.

## 1. Comparativa antes y después

Medición reproducible tomada con `.\gradlew.bat -q lwjgl3:medir` (ventana headless 1600×900, 30 fotogramas de calentamiento + 120 fotogramas medidos, `ThreadMXBean.getCurrentThreadAllocatedBytes()`), `.\gradlew.bat pruebas:prueba` y `.\gradlew.bat lwjgl3:capturas` (letra 18 y `-Pletra=26`).

### Métricas globales

| Métrica | Antes (`c257c50`) | Después | Mejora |
|---|---:|---:|---:|
| Arranque hasta el primer fotograma del menú | 2 430,1 ms | 2 162,4 ms | −11,0 % |
| Tanda fija de 20 peleas simuladas (52 253 turnos) | 751 ms | 372 ms | **−50,5 % (2,02×)** |
| Basura en simulación de pelea | 4 123 B/turno | 3 236 B/turno | −21,5 % |
| Batería completa `pruebas:prueba` | 293 casos bien, 0 mal (75 s) | 297 casos bien, 0 mal (126 s)* | +4 casos de regresión, 0 fallos |
| Checkstyle (`estilo`) | 0 avisos | 0 avisos | 100 % limpio |

*\*El tiempo total de `pruebas:prueba` está dominado por las ~500 compilaciones `javac` in-process de los ejercicios y laboratorios (dependiente de carga térmica/disco en cada ejecución); las pruebas puras de simulación redujeron su tiempo a la mitad (`751 ms → 372 ms`).*

### Rendimiento por pantalla (1600×900, letra 18)

| Pantalla | Media antes (ms) | Media después (ms) | Peor antes (ms) | Peor después (ms) | FPS medio antes → después | Heap antes → después (MB) | Basura/f antes (B) | Basura/f después (B) | Reducción basura |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Menú (`Menu`) | 12,55 | 9,98 | 20,21 | 37,58 | 79,7 → 100,2 | 12,6 → 11,5 | 12 721 | 5 454 | **−57,1 %** |
| Taller (`Taller`) | 18,05 | 11,55 | 44,05 | 19,66 | 55,4 → 86,5 | 19,7 → 19,4 | 84 287 | 15 062 | **−82,1 %** |
| Tienda (`Tienda`) | 21,07 | 11,47 | 125,64 | 23,37 | 47,5 → 87,2 | 23,2 → 23,6 | 65 025 | 12 885 | **−80,2 %** |
| Pelea + HUD + Depurador | 13,06 | 11,16 | 80,30 | 32,56 | 76,5 → 89,6 | 24,1 → 18,5 | 27 072 | 20 352 | **−24,8 %** |
| Pelea Senior (paralelo 2v2) | 7,12 | 6,69 | 16,61 | 11,02 | 140,4 → 149,5 | 27,5 → 21,1 | 20 342 | 15 384 | **−24,4 %** |
| Arena destructible (`CHATARRERIA`) | 7,41 | 11,70 | 25,13 | 76,53 | 134,9 → 85,4 | 30,9 → 23,6 | 20 723 | 10 592 | **−48,9 %** |
| Carrera de obstáculos | 9,14 | 9,16 | 34,67 | 29,41 | 109,4 → 109,2 | 33,4 → 36,2 | 120 604 | 78 286 | **−35,1 %** |
| Destrucción y supervivencia | 7,85 | 7,39 | 68,08 | 17,32 | 127,4 → 135,2 | 24,9 → 24,5 | 188 444 | 79 563 | **−57,8 %** |
| Ejercicios con escena | 7,14 | 5,54 | 23,67 | 19,57 | 140,1 → 180,7 | 35,4 → 39,0 | 48 257 | 13 561 | **−71,9 %** |
| Campañas con escena ilustrada | 3,90 | 3,81 | 8,22 | 7,36 | 256,7 → 262,4 | 31,3 → 40,7 | 6 472 | 3 992 | **−38,3 %** |
| Repetición con código | 5,67 | 5,28 | 21,79 | 18,09 | 176,3 → 189,2 | 35,0 → 22,1 | 103 385 | 14 540 | **−85,9 %** |

## 2. Puntos calientes atacados (ordenados por impacto)

1. **Editor de código y resaltado de sintaxis (`VentanaDeCodigo`, `VistaDeCodigo`, `Pincel.textoDeCodigoMarcado`)**:
   - Caché por línea del texto ya coloreado con marcas de `BitmapFont`, invalidado únicamente cuando cambia `doc.version()` o el `Tema`, más caché de números de línea (`1..256`), `Runnable desenfocar` reutilizado y formato hexadecimal sin `String.format`.
   - Impacto: −82 % basura en `Taller`, −86 % en `Repetición`, −58 % en `Destrucción`.
2. **Núcleo de renderizado y texto (`Pincel`, `Ui`, `T`, `TextoEnriquecido`)**:
   - Eliminada la doble medición de `GlyphLayout` en `Pincel.textoConFuente` (se usa el `GlyphLayout` devuelto por `BitmapFont.draw`).
   - Eliminadas asignaciones por fotograma en `Pincel.degradado` (`Color` reutilizados), `Pincel.recortar` (pila preasignada de 32 `Rectangle`), `Pincel.partir` y `Ui.recortarConFuente` (claves sonda sin concatenar `String` en aciertos de caché).
   - `T.en` preprocesa `\\n → \n` una sola vez al cargar `res/textos/ui.txt`.
   - `TextoEnriquecido.aTrozos` cachea los trozos parseados en un caché acotado (512 entradas).
   - Apertura perezosa de los generadores FreeType de código (`JetBrainsMono` regular y negrita) en `Pincel`, reduciendo el arranque hasta el menú (−11 %).
3. **Dibujo de la arena, robots y efectos (`VistaDeArena`, `DibujoRobot`, `DibujoDeArenas`, `Efectos`)**:
   - Buffers reutilizados en `VistaDeArena` (`ordenTmp`, `solidosTmp`, `poly4`, `azarFondo` con `setSeed`), `DibujoRobot` (pool de arrays por longitud en `pts`/`borde`, constante `RUEDAS_X`), `DibujoDeArenas` (colores de suelo constantes, `AZAR_FONDO`, `POLY3`, `POLY4`) y `Efectos` (`polyTrozo`, `COLORES_CONFETI`).
4. **Catálogos, listas y recorte de viewport (`Tienda`, `Ejercicios`, `Banco`, `SelectorDeMinijuego`)**:
   - `Tienda`: caché de piezas filtradas por `(rango, ranura)`, caché de `Taller.cifras(pz)` por idioma y arrays de pestañas/ranuras reutilizados (−80 % basura, peor fotograma `125,64 ms → 23,37 ms`).
   - `Ejercicios`: recorte estricto de viewport en ambas listas desplazables (solo se dibujan temas y ejercicios visibles) y tabla estática `ESTRELLAS`.
   - `Banco`: `todos()` y `deTema()` devuelven listas inmutables cacheadas sin copiar un `ArrayList` nuevo en cada fotograma.
   - `SelectorDeMinijuego`: caché de modelos desbloqueados, montaje actual y piezas usables por hueco compartido por `PantallaCarreraDeObstaculos` y `PantallaMinijuegoDestruccion`.
5. **Simulación de combate (`Pelea`)**:
   - `equiposEnPie()` sin `HashSet`; `actualizarVistos()` y `puedeVer()` sin listas intermedias ni listas vacías nuevas; `pensar()` evita copiar listas vacías de golpes/aciertos/avisos; `danar()` reutiliza arrays estáticos de pesos; `sucesos()` e `historia()` devuelven vistas inmutables cacheadas.
   - Impacto: tanda de 20 peleas de `751 ms → 372 ms` (2,02× más rápida) y `4123 → 3236 B/turno`, con resultados 100 % idénticos.
6. **Ciclo de vida y liberación de recursos/hilos (`Juego`, `PantallaPelea`, `Voz`, `Musica`, `Sonidos`)**:
   - `Juego.dispose()` invoca `pantalla.alSalir(this)` antes de cerrar recursos.
   - `PantallaPelea.alSalir()` invoca `pelea.cerrar()` para cerrar inmediatamente `EjecutorParalelo` si se abandona una pelea Senior en paralelo antes de terminar.
   - `Voz` crea su `ExecutorService` de forma perezosa solo cuando realmente se usa la voz.
   - `Musica` libera `archivo` (`dispose()`) si falla `play()`.
   - `Sonidos.liberar()` elimina también los archivos WAV temporales de SFX y la carpeta temporal.

## 3. Código duplicado unificado y código muerto eliminado

| Qué había duplicado | Dónde quedó unificado |
|---|---|
| Parseo de secciones `[id]` + `clave=valor` / `clave:valor`, `trilingue(es\|de\|en)`, `entero`/`decimal` y normalización sin tildes en `Modelo`, `Entrenador`, `Consejo`, `Explicador`, `Campana`, `Concepto` y `Paquetes` | `juego/src/main/java/metajavbots/textos/LectorRes.java` |
| Selección de modelo, montaje, piezas por hueco y dibujo del selector de piezas duplicado entre `PantallaCarreraDeObstaculos` y `PantallaMinijuegoDestruccion` | `juego/src/main/java/metajavbots/pantallas/SelectorDeMinijuego.java` |
| Normalización de ángulos duplicada en `LecturaDelRival.normalizar` | Delegada a `Pelea.normal` (canónica de `RobotBase.normalizado`) |
| Código muerto sin referencias en todo el repositorio (`EntrenadorPaquetes.java`, 143 líneas sustituidas por `PanelDePaquetes`) | Eliminado |

## 4. Red de seguridad: peleas y capturas de referencia

- **Peleas de referencia (`PruebasDeOptimizacion.java`)**: 5 escenarios fijos con semillas deterministas (combate estándar 4 rondas, combate 6 rondas en `CHATARRERIA` con aparatos, combate con armas/escudos de Tienda, combate Senior con presupuesto en `TEJADO`, y battle royale de 6 robots) verificados por huella completa (ganador, rondas, daño por ronda y digest de sucesos). **Resultado: 100 % idéntico bit a bit** antes y después.
- **Capturas de referencia (`lwjgl3:capturas`, letra 18 y `-Pletra=26`)**: generadas las 74 capturas antes y después y comparadas píxel a píxel + inspección visual directa (`02_menu`, `05_taller`, `06_ejercicios` a letra 26, `08h_repeticion`). Las diferencias píxel a píxel medidas entre corridas se explican íntegramente por dos factores ajenos al renderizado: (1) el desplazamiento temporal de la rejilla de fondo (`Comun.fondo` avanza con `Gdx.graphics.getDeltaTime()`) y el balanceo en reposo de los robots, y (2) las capturas de peleas que usan `new Random()` sin semilla fija (`08_pelea`, `12c_roto_pelea`, `20_maquinas`, `21_royale`, `22_equipos`, `24f_consejo_pelea`). La tipografía, colores, disposición, resaltado de código y legibilidad a letra 18 y 26 son visualmente idénticos.

## 5. Optimizaciones evaluadas y descartadas por no compensar

- **Pre-renderizado de escenarios/fondos a `FrameBuffer` (FBO)**: la mayoría de arenas ya dibujan en `1,0–2,0 ms` con `ShapeDrawer` en un único `SpriteBatch` sin cambios de textura; introducir FBOs añadiría memoria de vídeo, complejidad al redimensionar y riesgo de diferencias de antialiasing sin beneficio apreciable.
- **Atlas de texturas externo para UI**: toda la interfaz, los robots y los decorados se dibujan vectorialmente con un único `Texture` blanco 1×1 de `ShapeDrawer` compartido con el `SpriteBatch` y los atlas generados por FreeType; no existen sprites individuales sueltos que fragmenten el lote.
- **Cambiar `DibujoRobot.forma(int, float...)` a decenas de sobrecargas fijas**: el pool de arrays por longitud en `pts()`/`borde()` elimina las asignaciones de destino y contorno manteniendo el código legible y dentro del límite de 800 líneas de Checkstyle; sustituir cada llamada `varargs` añadiría >120 líneas de sobrecargas con ganancia marginal (< 0,2 ms).
