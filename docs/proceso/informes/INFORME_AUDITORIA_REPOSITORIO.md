# Informe de Auditoría del Repositorio — MetajavBots

Fecha: 2026-11-19  
Alcance: Inspección de código fuente real (`juego/`, `pruebas/`, `res/`) y estado técnico del repositorio.

---

## 1. Pendientes del encargo anterior (Voz, Bloque 4 y Bloque 5)

### 1.1. Voz sin retraso (Piper)
- **Estado:** `NO`
- **Evidencia:** `juego/src/main/java/metajavbots/sonido/VozPiper.java` (`decir`, líneas 84–115)
- **Resumen:** Cada llamada a `decir(...)` arranca un nuevo proceso `ProcessBuilder` de `piper`/`piper.exe`, lee todo el audio en memoria (`transferTo`) y espera a que termine (`waitFor`); no hay proceso persistente ni streaming.

### 1.2. Escenas en ejercicios
- **Estado:** `NO` *(no quedan ejercicios solo de texto)*
- **Evidencia:** `juego/src/main/java/metajavbots/pantallas/PantallaEjercicio.java` (`robotito`, líneas 212–216), `AnimacionDelCaso.java`, `EscenasDelRobotito.java`, `EscenaDeArena.java`, `res/ejercicios/*.txt`
- **Resumen:** Quedan 0 ejercicios sin escena animada (de los 272 totales): todos los que compilan y tienen casos renderizan al robotito en `AnimacionDelCaso` con 24 fondos temáticos (`01`–`22`, `COMPOSICION`, `ABSTRACTA` y respaldo `CLASIFICAR`), más 5 ejercicios en `res/ejercicios/24_juego.txt` con escena explícita `giro`/`avance` en `EscenaDeArena`.

### 1.3. Peleas de Senior
- **Estado:** `PARCIAL`
- **Evidencia:** `juego/src/main/java/metajavbots/combate/Pelea.java` (`nanosPorTurno`, `topeDePasos`, `avanzar`), `Tope.java`, `ModoEspecial.java` (líneas 78–90), `Jefe.java` (líneas 139–159), `PruebasDeSenior.java`
- **Resumen:** Hay 4 jefes senior por tema (`records`, `selladas`, `inmutabilidad`, `estrategia`) y tope de pasos/tiempo por turno (`nanosPorTurno`, `topeDePasos`, modo 150/400/1000 pasos), pero no hay presupuesto de memoria por turno, ni ejecución en paralelo de robots (`Pelea.avanzar` es secuencial), ni rivales que castiguen fallos típicos.

### 1.4. Bloque 5
- **Diseña tu propia pieza (interfaz `Pieza`):**
  - **Estado:** `NO`
  - **Evidencia:** `juego/src/main/java/metajavbots/piezas/Pieza.java`, `CatalogoDePiezas.java`
  - **Resumen:** `Pieza` es una clase `final` de datos leída de `res/datos/piezas.txt`, no una interfaz implementable por el jugador en `metajavbots.api`.
- **Aparatos extra estilo Medabots (martillo, sierra, imanes…):**
  - **Estado:** `PARCIAL`
  - **Evidencia:** `juego/src/main/java/metajavbots/combate/Aparatos.java` (`Tipo`, líneas 21–24), `res/aparatos/*.txt`, `res/datos/piezas.txt` (líneas 25–36)
  - **Resumen:** En `Aparatos` siguen solo los 6 aparatos de arena (`chorro`, `antigravedad`, `descarga`, `lanzallamas`, `aceite`, `pegamento`, sin imanes); martillo, sierra, pinza, volteador, cuña y bola con cadena son piezas de `BRAZO` en `piezas.txt`, no aparatos recogibles.
- **Repetición con el código ejecutado al lado y botón de guardar como GIF:**
  - **Estado:** `PARCIAL`
  - **Evidencia:** `juego/src/main/java/metajavbots/pantallas/Repeticion.java`, `PantallaPelea.java` (líneas 635–689), `GrabadorGif.java`, `lwjgl3/src/main/java/metajavbots/escritorio/Capturas.java`
  - **Resumen:** `Repeticion` reproduce el mejor golpe a cámara lenta y existe `GrabadorGif`, pero no muestra el código ejecutado al lado ni tiene botón de guardar GIF (`GrabadorGif` solo lo invoca `Capturas.java`).
- **6 personalidades propias nuevas para los últimos robots (`res/dialogos/`):**
  - **Estado:** `NO`
  - **Evidencia:** `res/dialogos/` (17 archivos: 16 personalidades + `locutor.txt`), `res/datos/modelos.txt` (líneas 268–362)
  - **Resumen:** Los 6 últimos modelos (`serrucho`, `galapago`, `volteo`, `bolon`, `barrena`, `remolino`) reutilizan personalidades ya existentes (`euforico`, `calmado`, `jugueton`, `grunon`, `intrigante`, `sarcastico`).
- **Biografías editables desde el juego:**
  - **Estado:** `NO`
  - **Evidencia:** `juego/src/main/java/metajavbots/tutor/Biografias.java`, `res/biografias/robots.txt`, `res/biografias/entrenadores.txt`
  - **Resumen:** `Biografias` solo lee recursos estáticos del classpath (`getResourceAsStream`) para mostrarlos en `Taller`, `EntradaEpica` y `PanelDelTutor`, sin edición ni guardado.
- **Declinación alemana en diálogos (`Concordancia`):**
  - **Estado:** `PARCIAL`
  - **Evidencia:** `juego/src/main/java/metajavbots/dialogos/Concordancia.java` (`ajustar`, `aleman`, líneas 27–32 y 62–66)
  - **Resumen:** En alemán solo sustituye formas de plural para `Beine` (`ist/sind`, `hat/haben`, `fällt/fallen`, `geht/gehen`, `war/waren`, `mein/meine`); no declina género ni casos (nominativo/acusativo/dativo) en singular.
- **Campañas de Creed:**
  - **Estado:** `NO`
  - **Evidencia:** `res/datos/campanas.txt`, `juego/src/main/java/metajavbots/campanas/Campana.java`
  - **Resumen:** Existen 17 campañas de serie (`rocko`–`rocko6`, `karatebit`–`karatebit4`, `kungbit`, `leyendasbit`, `torooxidado`, `granyudo`, `millonbitios`, `aceropuro`, `patadaspalmera`, `kumitebit`), ninguna de Creed.

### 1.5. Repaso visual (`-Pletra=26`)
- **Estado:** `SÍ`
- **Evidencia:** Commit `fd5ee5b` (`EntradaEpica.java`, `EntrenadorEnLaPelea.java`, `EntrenadorFlotante.java`, `GuiaDeCerebros.java`, `Historial.java`, `MiniJuegos.java`, `PanelDelTutor.java`, `PantallaPelea.java`, `SesionDeHoy.java`)
- **Resumen:** Consta un commit reciente de ajustes visuales para `-Pletra=26` que toca 9 pantallas/paneles más allá de `Menu.java`.

---

## 2. Inventario inicial, Economía y Tienda

### 2.1. Inicio de partida
- **Estado:** `SÍ`
- **Evidencia:** `Inicio.java` (`crear`, líneas 145–160), `Bienvenida.java` (`elegirRobot`, líneas 192–231), `Progreso.java` (`modelosDisponibles`, `piezaDisponible`, `piezaUsable`, `robotsQuePuedeTener`, líneas 183–243), `res/datos/modelos.txt`, `res/datos/piezas.txt`
- **Resumen:** En `NOOB_1` (0 victorias) empieza con **3 modelos** disponibles (`cobalto`, `chispa`, `vigia`, eligiendo 1 robot inicial) y **6 piezas globales** de `inicio` (`laser_basico`, `canon_ligero`, `punos`, `cabeza_radar`, `piernas_bipedo`, `nucleo_basico`), más las 5 piezas de fábrica de su modelo (`piezaUsable`); los 6 aparatos de arena no son inventario, sino recogibles en combate.

### 2.2. Monedas / puntos
- **Estado:** `NO`
- **Evidencia:** `Jugador.java` (línea 60), `Progreso.java` (líneas 108–168)
- **Resumen:** Solo existe `Jugador.experiencia` (XP acumulativa); no hay ninguna moneda gastable.

### 2.3. Tienda
- **Estado:** `NO`
- **Evidencia:** `Menu.java` (líneas 36–46), `Taller.java`
- **Resumen:** No existe ninguna pantalla ni clase de tienda; todo se desbloquea por progreso, botín, desafíos, jefes, laboratorio o retos.

### 2.4. Desbloqueo por código
- **Estado:** `PARCIAL`
- **Evidencia:** `res/ejercicios/23_retos.txt`, `res/datos/piezas.txt` (líneas 97–100), `Progreso.java` (líneas 169–173), `ModoEspecial.java` (líneas 109–119)
- **Resumen:** Resolver 3 ejercicios de `23_retos.txt` (`reto_prediccion`, `reto_radar`, `reto_esquiva`) desbloquea 3 piezas fijas del catálogo, pero no desbloquea métodos en la API ni permite modificar piezas con límites de equilibrio.

---

## 3. Programación diferenciada, POO jugable y Control de flujo

### 3.1. Robots distintos al programar
- **Estado:** `NO`
- **Evidencia:** `Modelo.java`, `Montaje.java`, `Metajavbot.java`, `RobotBase.java`
- **Resumen:** Todos los modelos comparten exactamente la misma API (`Metajavbot`/`MetajavbotDe`/`MetajavbotEn`); solo cambian estadísticas, naturaleza, estética, personalidad y piezas de fábrica.

### 3.2. Modificadores POO (`public`, `private`, `protected`, `final`, `static`, default)
- **Estado:** `NO`
- **Evidencia:** `DetectorDeTemas.java` (líneas 70–120), `Jefe.java`, `Pelea.java`
- **Resumen:** No tienen ningún efecto en mecánicas de combate/arena; solo se comprueban estáticamente en `DetectorDeTemas` (encapsulación e inmutabilidad para jefes) y por `javac` en ejercicios.

### 3.3. Bucle principal
- **Estado:** `NO`
- **Evidencia:** `Pelea.java` (`avanzar`, líneas 236–270; `pensar`, líneas 381–424), `Metajavbot.java` (líneas 42–121)
- **Resumen:** El motor ejecuta el bucle en `Pelea.avanzar()` e invoca los callbacks del robot en orden fijo (`alRecibirGolpe`, `alAcertar`, `alChocarConPared`, `alChocarConEnemigo`, `alOirAlEquipo`, `alVerEnemigo`, `cadaTurno`); el jugador solo sobrescribe callbacks y no controla el bucle principal.

---

## 4. Minijuegos, Depuración y Testing

### 4.1. Minijuegos actuales
- **Estado:** `NO` *(no existe ninguno de esquivar obstáculos/objetos)*
- **Evidencia:** `MiniJuegos.java` (líneas 21–26), `Objetivo.java` (líneas 27–42)
- **Resumen:** Hay 4 minijuegos contra el muñeco: `moverse` (`recorre>=2500`), `girar` (`vistos>=300`), `apuntar` (`aciertos>=3`) y `disparar` (`punteria>=60`); ninguno es de esquivar obstáculos u objetos.

### 4.2. Ubicación de Depuración y Testing
- **Estado:** `NO` *(siguen dentro de `ModoEspecial`)*
- **Evidencia:** `Menu.java` (líneas 36–46), `ModoEspecial.java` (líneas 61–108)
- **Resumen:** `Robots rotos` y la práctica del depurador siguen en la pestaña 1 (`especial.tope`) de `ModoEspecial`, y `Laboratorio` en la pestaña 2 (`especial.laboratorio`), sin sección propia en `Menu`.

### 4.3. Laboratorio de pruebas
- **Estado:** `NO` *(ninguno evalúa estrategias de Blackbox, Whitebox ni TDD)*
- **Evidencia:** `res/laboratorio/indice.txt`, `trayectoria.txt`, `blindaje.txt`, `punteria.txt`, `Laboratorio.java` (líneas 57–84)
- **Resumen:** Hay 3 retos (`trayectoria`, `blindaje`, `punteria`), todos evaluados únicamente exigiendo `>= 3` métodos `@Test` que pasen contra la clase buena y cacen sus 3 mutantes (*mutation testing*), sin cobertura de ramas, particiones formales ni flujo TDD.

---

## 5. Estado técnico del repositorio

### 5.1. `git log --oneline -5` y `git status --short`
- **Estado:** `PARCIAL` *(hay cambios locales sin confirmar en el árbol de trabajo)*
- **Evidencia:**

```text
5dafbc1 Mostrar codigo del ultimo rival o sparring y consolidar banco de pruebas y paquetes
ad2c706 Eliminar archivos huerfanos de paquetes y dejar pruebas en verde
7c252e7 Implementar visor tactico en vivo (F8)
68de0e0 Punto de control funcional antes de mejoras de jugabilidad
fd5ee5b Punto de control: ajustes visuales para letra grande (-Pletra=26)
```

```text
 M juego/src/main/java/metajavbots/api/Mando.java
 M juego/src/main/java/metajavbots/api/Metajavbot.java
 M juego/src/main/java/metajavbots/api/MetajavbotDe.java
 M juego/src/main/java/metajavbots/api/MetajavbotEn.java
 M juego/src/main/java/metajavbots/api/RobotBase.java
 M juego/src/main/java/metajavbots/combate/Cuerpo.java
 M juego/src/main/java/metajavbots/combate/Pelea.java
 M juego/src/main/java/metajavbots/pantallas/PantallaPelea.java
 M lwjgl3/src/main/java/metajavbots/escritorio/Capturas.java
 M pruebas/src/main/java/metajavbots/pruebas/Pruebas.java
 M res/textos/ui.txt
?? Informe/INFORME_FUNCIONALIDADES_ACTUALES.md
?? juego/src/main/java/metajavbots/combate/OrdenDelTurno.java
?? juego/src/main/java/metajavbots/pantallas/PanelDepurador.java
?? pruebas/src/main/java/metajavbots/pruebas/PruebasDelDepurador.java
```

- **Resumen:** El `HEAD` está en `5dafbc1` con 11 archivos modificados y 4 sin seguimiento (depurador en vivo e informe).

### 5.2. `gradlew compileJava estilo pruebas:prueba`
- **Estado:** `SÍ`
- **Evidencia:** Ejecución de `.\gradlew.bat compileJava estilo pruebas:prueba` (`BUILD SUCCESSFUL`).
- **Resumen:** Compilación, estilo y suite de pruebas pasan limpios: **130 casos bien, 0 mal** (65 s).
