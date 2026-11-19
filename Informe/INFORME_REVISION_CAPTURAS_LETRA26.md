# Informe de revisión visual de capturas (letra normal y `-Pletra=26`)

**Fecha:** 2026-11-19  
**Alcance:** Revisión visual completa de las 64 pantallas generadas por `gradlew.bat lwjgl3:capturas` en tamaño estándar (`letra=18`) y letra grande (`-Pletra=26`), más verificación en alemán (`-Pidioma=de`) e inglés (`-Pidioma=en`) con `-Pletra=26` sobre las 30 pantallas críticas con más texto.

---

## 1. Pantallas revisadas (64 en total)

- **Inicio y menú:** `01_inicio`, `02_menu`, `18_bienvenida`
- **Catálogos y arenas:** `03_robots`, `03b_armas`, `03c_escenas`, `03d_entrenadores`, `04_arena_hangar`, `04_arena_desierto`, `04_arena_neon`, `04_arena_laboratorio`, `04_arena_luna`, `04_arena_chatarreria`, `04_arena_cocina`, `04_arena_hielo`, `04_arena_tejado`
- **Taller, código y combates:** `05_taller`, `07_pelear`, `08_pelea`, `08b_aparato_suelo`, `08c_aparato_montado`, `08d_aparato_codigo`, `08e_arma_especial`, `08f_derrotas`, `08g_entrada`, `08h_repeticion`, `19_rival`, `20_maquinas`, `21_royale`, `22_equipos`, `23_campanas`, `25_sesion_de_hoy`, `26_minijuegos`
- **Ejercicios, tutoriales, laboratorio y modo especial:** `06_ejercicios`, `09_ejercicio`, `09b_robot_cajas`, `09c_robot_puertas`, `09c2_arena_giro`, `09c3_arena_avance`, `09d_robot_estanteria`, `09e_robot_papel`, `09f_finales`, `09g_robot_pizarra`, `09h_robot_bien`, `09i_robot_puerta`, `10_tutoriales`, `11_leccion`, `12_especial`, `12b_rotos`, `12c_roto_pelea`, `13_laboratorio`, `13b_inspeccion`, `14_temario`
- **Historial, IA, ajustes y entrenador:** `15_historial`, `16_panel_ia`, `17_ajustes`, `24_entrenador`, `24b_entrenador_error`, `24c_entrenador_tip`, `24d_guia_local`, `24d_guia_clave`, `24d_guia_despertar`, `24e_hospital`, `24f_consejo_pelea`

*(Las 30 pantallas críticas verificadas además en alemán e inglés con `-Pletra=26`: `01_inicio`, `02_menu`, `05_taller`, `06_ejercicios`, `07_pelear`, `08_pelea`, `09_ejercicio`, `10_tutoriales`, `11_leccion`, `12_especial`, `12b_rotos`, `13_laboratorio`, `14_temario`, `15_historial`, `16_panel_ia`, `17_ajustes`, `18_bienvenida`, `19_rival`, `20_maquinas`, `21_royale`, `23_campanas`, `24_entrenador`, `24b_entrenador_error`, `24c_entrenador_tip`, `24d_guia_local`, `24d_guia_despertar`, `24e_hospital`, `24f_consejo_pelea`, `25_sesion_de_hoy`, `26_minijuegos`.)*

---

## 2. Fallos visuales reales detectados y corregidos

| # | Pantalla(s) | Problema detectado | Archivo y líneas | Corrección aplicada |
|---|---|---|---|---|
| 1 | `05_taller`, `24c_entrenador_tip` (ES/DE/EN, `-Pletra=26`) | En el panel izquierdo del tutor, el bocadillo del entrenador recortaba la última línea (`abajo.` / `Wähle...`) y el cuadro `Pregúntame (Intro)` bajaba hasta montarse sobre la vista previa del robot. | `juego/src/main/java/metajavbots/pantallas/PanelDelTutor.java` (L96–L104) | Se limita `bhMax = Math.max(92, Math.min(h * 0.45f, h - 256))` reservando espacio exacto para metas, botones y caja de pregunta, y se ajusta `tl` hacia `Pincel.LETRA_MINIMA` solo si el texto no cabe en `bhMax`. |
| 2 | `08_pelea` (ES, `-Pletra=26`) | En el marcador superior derecho, `Chatarra · computadora` chocaba con `Daño recibido: 0`. | `juego/src/main/java/metajavbots/pantallas/PantallaPelea.java` (L413–L420) | Se calcula `danoW` y se aplica `ui.cabe(nombre, 588 - danoW - 14, ui.letra, true)` al nombre del combatiente. |
| 3 | `12c_roto_pelea` (ES, `-Pletra=26`) | El objetivo central (`Objetivo: acierta 3 veces en la ronda`) se montaba sobre las barras amarillas de energía a izquierda y derecha. | `juego/src/main/java/metajavbots/pantallas/PantallaPelea.java` (L436–L446) | Se aplica `ui.cabe(..., Math.max(180, p.ancho - 1250), ui.letra * 0.7f, false)` tanto al objetivo como al estado del jefe para mantenerlos centrados entre ambas barras. |
| 4 | `24f_consejo_pelea` (informe entre rondas, ES, `-Pletra=26`) | Las líneas largas de la columna izquierda (`Tiempo con el enemigo a la vista`, `Topes de cálculo`) se solapaban con la columna del rival. | `juego/src/main/java/metajavbots/pantallas/PantallaPelea.java` (L618–L633) | Se reemplaza `p.texto` por `p.parrafo(l, cx, yy, col - 10, ui.letra * 0.9f, ...)` y se protege el nombre del robot con `ui.cabe`. |
| 5 | `21_royale`, `22_equipos` (ES/DE/EN, `letra=18` y `26`) | El aviso inferior `Clic, Espacio o Esc para saltar la presentación` quedaba tapado parcialmente por el botón del aparato (`Pegotón: su código`, `Resbalín: sein Code`). | `juego/src/main/java/metajavbots/pantallas/EntradaEpica.java` (L111) | Se sube la coordenada `y` del aviso a `y + h - 76 - ui.letra * 0.9f` para dejarlo siempre por encima de la botonera inferior. |
| 6 | `24f_consejo_pelea` (consejo en combate, DE, `-Pletra=26`) | En el recuadro de pseudocódigo del entrenador (`Probier so etwas:`), líneas largas en alemán se salían por la derecha del recuadro y de la pantalla. | `juego/src/main/java/metajavbots/pantallas/EntrenadorEnLaPelea.java` (L157–L174) | Se mide y dibuja `trozo` con `p.altoDeParrafo` y `p.parrafo` dentro de `w - 96`, ajustando la altura del recuadro dinámicamente. |
| 7 | `24b_entrenador_error` (ES/DE, `letra=18` y `26`) | La lista desplazable de conceptos de la columna izquierda se extendía por detrás de la etiqueta `Ayuda 6/10` / `Hilfe 6/10`, solapándose con ella. | `juego/src/main/java/metajavbots/pantallas/EntrenadorFlotante.java` (L437–L439) | Se calcula `altoAyuda = ui.letra * 1.1f + 68` para recortar la lista limpiamente por encima de `Ayuda X/10`. |
| 8 | `24d_guia_local`, `24d_guia_clave`, `24d_guia_despertar` (ES/DE/EN, `-Pletra=26`) | Cuando el título azul de la página envolvía a 2 líneas con letra grande, pisaba el inicio del paso `1.`. | `juego/src/main/java/metajavbots/pantallas/GuiaDeCerebros.java` (L49–L50, L66–L76) | Se usa el alto real devuelto por `p.parrafo(...)` (`inicioPasos = y + Math.max(80, 12 + tituloH + 14)`) para iniciar la zona desplazable sin solapamiento, y `ui.cabe` en el título superior. |
| 9 | `15_historial` (ES/EN, `-Pletra=26`) | En la fila superior de tarjetas de estadísticas, `daño medio recibido` / `avg. damage taken` desbordaba su caja invadiendo las tarjetas vecinas. | `juego/src/main/java/metajavbots/pantallas/Historial.java` (L140–L143) | Se aplica `ui.cabe(..., cw - 16, ...)` al valor y a la etiqueta de cada tarjeta. |
| 10 | `26_minijuegos` (ES/DE/EN, `-Pletra=26`) | En las 4 tarjetas de minijuegos, las últimas 1–2 líneas del recuadro de pseudocódigo quedaban recortadas contra los botones `Jugar`/`Taller`. | `juego/src/main/java/metajavbots/pantallas/MiniJuegos.java` (L45–L68) | Se ajusta el espaciado vertical a la altura real de línea (`p.altoDeLinea`) y se calcula `tPseudo` para que todas las líneas del pseudocódigo quepan vertical y horizontalmente dentro del hueco disponible. |
| 11 | `25_sesion_de_hoy` (DE, `-Pletra=26`) | En la tarjeta 1, el título `Schwaches wiederholen` se solapaba con `20 min`. | `juego/src/main/java/metajavbots/pantallas/SesionDeHoy.java` (L91–L94) | Se mide el ancho de `tiempo` y se ajusta el título con `ui.cabe(titulo, w - 64 - tiempoW - 26, ui.letra * 1.1f, true)`. |

Todas las correcciones se verificaron visualmente tras regenerar capturas en `letra=18` (sin regresiones) y `-Pletra=26` (ES y DE).

---

## 3. Medición de milisegundos por fotograma (ms/fotograma)

### 3.1 Pantallas modificadas (antes y después)

| Pantalla | Normal antes (ms) | Normal final (ms) | Letra 26 ES antes (ms) | Letra 26 ES final (ms) | Letra 26 DE final (ms) |
|---|---:|---:|---:|---:|---:|
| `05_taller` | 2.9 | 3.5 | 2.7 | 3.3 | 3.4 |
| `08_pelea` | 3.2 | 2.2 | 3.7 | 2.6 | 2.5 |
| `08g_entrada` | 0.7 | 0.9 | 0.8 | 0.8 | — |
| `12c_roto_pelea` | 1.8 | 5.1 | 1.8 | 2.1 | — |
| `15_historial` | 2.7 | 2.9 | 2.9 | 2.3 | 2.5 |
| `21_royale` | 2.0 | 3.1 | 2.1 | 3.0 | 2.0 |
| `22_equipos` | 1.7 | 1.8 | 1.9 | 1.7 | — |
| `24b_entrenador_error` | 3.8 | 3.8 | 3.3 | 3.9 | 5.7 |
| `24c_entrenador_tip` | 1.9 | 4.5 | 2.1 | 3.4 | — |
| `24d_guia_local` | 0.9 | 1.4 | 1.3 | 1.4 | 1.2 |
| `24d_guia_clave` | 1.0 | 1.3 | 1.2 | 1.4 | — |
| `24d_guia_despertar` | 0.9 | 1.2 | 0.9 | 1.2 | — |
| `24f_consejo_pelea` | 2.7 | 3.1 | 2.6 | 3.1 | 3.3 |
| `25_sesion_de_hoy` | 1.0 | 1.1 | 1.0 | 1.0 | 1.7 |
| `26_minijuegos` | 1.1 | 1.2 | 1.1 | 1.3 | 1.4 |

### 3.2 Resto de pantallas (sin cambios de código, medición final)

| Pantalla | Normal (ms) | Letra 26 ES (ms) | Pantalla | Normal (ms) | Letra 26 ES (ms) |
|---|---:|---:|---|---:|---:|
| `01_inicio` | 1.6 | 1.3 | `09e_robot_papel` | 1.2 | 1.5 |
| `02_menu` | 2.5 | 2.1 | `09f_finales` | 0.4 | 0.5 |
| `03_robots` | 2.6 | 2.4 | `09g_robot_pizarra` | 1.1 | 1.3 |
| `03b_armas` | 1.7 | 1.5 | `09h_robot_bien` | 1.6 | 1.8 |
| `03c_escenas` | 2.7 | 3.1 | `09i_robot_puerta` | 1.2 | 1.3 |
| `03d_entrenadores` | 1.0 | 0.8 | `10_tutoriales` | 2.9 | 3.8 |
| `04_arena_hangar` | 1.5 | 1.6 | `11_leccion` | 0.7 | 0.7 |
| `04_arena_desierto` | 0.9 | 0.9 | `12_especial` | 2.3 | 2.3 |
| `04_arena_neon` | 0.9 | 0.9 | `12b_rotos` | 1.9 | 2.0 |
| `04_arena_laboratorio` | 0.8 | 1.0 | `13_laboratorio` | 1.1 | 1.3 |
| `04_arena_luna` | 0.9 | 0.8 | `13b_inspeccion` | 1.5 | 1.6 |
| `04_arena_chatarreria` | 0.9 | 0.8 | `14_temario` | 0.9 | 0.9 |
| `04_arena_cocina` | 1.0 | 0.8 | `16_panel_ia` | 0.7 | 0.8 |
| `04_arena_hielo` | 0.8 | 0.7 | `17_ajustes` | 2.4 | 2.6 |
| `04_arena_tejado` | 1.0 | 0.8 | `18_bienvenida` | 0.5 | 0.5 |
| `06_ejercicios` | 2.9 | 2.7 | `19_rival` | 1.4 | 1.6 |
| `08b_aparato_suelo` | 0.6 | 0.5 | `20_maquinas` | 2.2 | 2.4 |
| `08c_aparato_montado` | 0.9 | 0.7 | `23_campanas` | 1.8 | 2.0 |
| `08d_aparato_codigo` | 1.1 | 1.0 | `24_entrenador` | 3.6 | 2.5 |
| `08e_arma_especial` | 1.0 | 1.1 | `24e_hospital` | 1.7 | 1.8 |
| `08f_derrotas` | 0.6 | 0.7 | | | |
| `08h_repeticion` | 1.4 | 1.3 | | | |
| `09_ejercicio` | 1.2 | 1.3 | | | |
| `09b_robot_cajas` | 4.2 | 3.4 | | | |
| `09c_robot_puertas` | 1.5 | 1.8 | | | |
| `09c2_arena_giro` | 1.5 | 1.8 | | | |
| `09c3_arena_avance` | 1.3 | 1.5 | | | |
| `09d_robot_estanteria` | 1.4 | 1.7 | | | |

Todas las pantallas se mantienen muy por debajo del presupuesto de 16.6 ms por fotograma (60 FPS), oscilando entre **0.4 ms y 5.7 ms**.

---

## 4. Casos dudosos dejados sin tocar

1. **Descendentes bajo `PistaDeCarrera` (`09b_robot_cajas`, `09h_robot_bien`, `-Pletra=26`):** Se aprecian unos pocos píxeles de descendentes de texto asomando justo debajo del borde inferior de la barra verde `PistaDeCarrera`, procedentes del texto interno de `AnimacionDelCaso`. Se dejó intacto para respetar estrictamente la restricción de no tocar las animaciones/dibujos de los robots.
2. **Placeholders en `CampoDeTexto` (`11_leccion`, `14_temario`, `24_entrenador`, `-Pletra=26`):** Los textos de ejemplo vacíos (`Pregúntale al tutor e Intro`, `Ruta del archivo, o suéltalo en la...`) se recortan por el recorte horizontal interno del campo sin desbordar la caja. Se dejó sin tocar al ser comportamiento estándar de un campo de texto con texto guía largo.
3. **Abreviaciones con puntos suspensivos en controles estrechos (`05_taller` `– Dame meno...`, `07_pelear` `Cerebros de los rivales:...`, `24b_entrenador_error` pestañas de 5 niveles):** `Ui.boton` y `Ui.pestanas` aplican puntos suspensivos controlados dentro del control sin solapar ningún otro elemento; no se alteró el diseño ni los textos.

---

## 5. Verificación final de compilación, estilo y pruebas

Comando ejecutado:
```powershell
.\gradlew.bat compileJava estilo pruebas:prueba
```

- **Compilación (`compileJava`):** Correcta (`BUILD SUCCESSFUL`).
- **Checkstyle (`estilo`):** 0 avisos en todos los módulos (`juego`, `lwjgl3`, `pruebas`).
- **Pruebas (`pruebas:prueba`):** **117 casos bien, 0 mal** (54 s).
- **Áreas intactas:** No se ha modificado música, efectos de sonido, capa de voz, compilador/caja de arena ni dibujos/animaciones de los robots.
