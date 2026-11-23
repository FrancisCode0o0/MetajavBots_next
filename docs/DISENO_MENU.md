# Diseño visual del Menú Principal y sus pantallas

Este documento recoge los principios de diseño de interfaz aplicados al **Menú principal** (`Menu.java`) y a las pantallas que nacen de él (**Historia** `Campanas.java`, **Desafíos** `PrepararPelea.java`, **Modo especial** `ModoEspecial.java` / `PanelSenior.java` y **Estadísticas** `Historial.java`), así como la justificación de cada decisión.

---

## 1. Investigación de menús de juegos y buenas prácticas de UI

Al analizar menús de juegos tácticos, competitivos y de programación bien resueltos (*Into the Breach*, *Slay the Spire*, *Hades*, *Armored Core VI*, *Factorio*), destacan siete reglas constantes que evitan el aspecto improvisado y guían la vista del jugador sin esfuerzo:

1. **Rejilla simétrica y ritmo constante (Grid & Rhythm)**:
   - Un menú dividido en bloques apilados verticalmente debe compartir el **mismo número de columnas** en todos sus bloques. Cuando un bloque tiene 3 columnas y el de abajo tiene 5 tarjetas estrechas, el ritmo visual se rompe y los títulos largos se parten o se truncan con `…`.
   - Solución aplicada: los tres bloques (**Aprender**, **Tu Robot** y **Arena**) comparten una **rejilla uniforme de 3 columnas** (3×2 en Aprender, 3×1 en Tu Robot y 3×2 en Arena), con el mismo margen lateral (`14 px`) y la misma separación horizontal (`12 px`) y vertical (`10 px`).

2. **Tarjetas del mismo tamaño y alineación estricta**:
   - Dentro de cada fila o sección, todas las tarjetas tienen exactamente el mismo ancho y alto, con sus elementos internos anclados a coordenadas fijas (icono y título arriba, descripción en la zona central con interlineado limpio, y botón de acción anclado al borde inferior en las pantallas de selección).
   - En **Desafíos** (`PrepararPelea.java`), al salir las campañas hacia su botón propio **Historia**, quedan 7 modos de combate que se organizan en una rejilla equilibrada (4 tarjetas arriba y 3 tarjetas centradas del mismo ancho abajo, o 4+3 con alturas unificadas por fila), seguidas del panel limpio de *Robots de otros jugadores*.

3. **Jerarquía tipográfica de tres niveles**:
   - **Nivel 1 (Cabecera de pantalla y de bloque)**: negrita con alto contraste, acento cromático del bloque y barra indicadora vertical a la izquierda.
   - **Nivel 2 (Título de tarjeta / modo / capítulo)**: negrita clara (`tema.texto`), medida dinámicamente para que quepa en 1 línea o, si el idioma o `-Pletra=26` lo requiere, en 2 líneas completas **sin puntos suspensivos (`…`) ni cortes**.
   - **Nivel 3 (Texto de ayuda / descripción)**: peso regular (`tema.textoSuave`), con ajuste automático de cuerpo dentro del área disponible para que jamás desborde la tarjeta ni pise botones inferiores.

4. **Contraste, profundidad y color semántico por bloque**:
   - Se mantiene la identidad cromática de MetajavBots:
     - **Aprender**: azul/cian (`#38BDF8` en oscuro, `#0B66C3` en claro).
     - **Tu Robot**: ámbar/dorado (`#F5B742` en oscuro, `#9A6400` en claro).
     - **Arena**: rojo/coral (`#FF7070` en oscuro, `#C62828` en claro).
   - Cada tarjeta cuenta con sombra sutil desplazada (`+2 px`), fondo tintado según su bloque, franja lateral de acento (`4 px`) y borde redondeado nítido que aumenta de grosor e intensidad al pasar el ratón.

5. **Iconografía coherente y medallas del mismo estilo**:
   - Cada tarjeta del menú principal lleva una insignia circular con fondo translúcido del color del bloque, aro de contorno sutil y símbolo centrado a escala uniforme:
     - *Aprender*: `★` Tutoriales, `⚄` Minijuegos, `▶` Demostraciones, `✓` Ejercicios, `⏱` La hora de hoy, `▤` Mi temario.
     - *Tu Robot*: `⚙` Taller, `◆` Tienda, `!` Depuración y Testing.
     - *Arena*: `▤` Historia, `⚔` ¡A pelear!, `►` Desafíos, `»` Carrera de obstáculos, `◆` Modo especial, `▲` Estadísticas.

6. **Estados claros al pasar el ratón (Hover) y al seleccionar (Active/Focus)**:
   - Al pasar el ratón por cualquier tarjeta o fila interactiva (en Menú, Historia, Desafíos o Modo especial), el fondo se ilumina suavemente hacia el color de acento, el borde pasa a `2.4 px` en el color principal del bloque y la barra inferior muestra la ayuda contextual.
   - Las acciones primarias (**¡A pelear!** y **Minijuegos ★ Principiantes**) destacan con un tinte de fondo más cálido y borde permanente de `1.8 px`.

7. **Cero texto cortado en cualquier idioma y tamaño de letra**:
   - Ningún botón, título, pestaña ni etiqueta usa truncado con `…` en los menús.
   - Las barras de pestañas (`Ui.pestanas`, como las 6 pestañas de **Modo especial** en español, alemán e inglés con `-Pletra=26`) escalan el tamaño de fuente de cada pestaña con `cabe(...)` para que títulos largos como *«Destrucción y supervivencia»* o *«Programmier-Herausforderungen»* se lean enteros de principio a fin.

---

## 2. Aplicación pantalla por pantalla

### 2.1. Menú Principal (`Menu.java`)
- **Panel izquierdo (Escaparate del robot)**: cabecera con nombre del robot en grande y subtítulo de modelo/naturaleza, plataforma elíptica bajo el robot animado, tarjeta interna con el marcador de victorias/derrotas y experiencia, y botones inferiores de **Panel de la IA** y **Ajustes** con ancho completo y margen simétrico.
- **Bloque Arena en 2 filas × 3 columnas**:
  - **Fila 1**: `Historia` (abre `Campanas`), `¡A pelear!` (combate directo adaptado), `Desafíos` (abre `PrepararPelea`).
  - **Fila 2**: `Carrera de obstáculos`, `Modo especial`, `Estadísticas` (abre `Historial`).
- **Títulos de casilla sin cortes**: `dibujarTituloCasilla` prioriza una sola línea con reducción suave de fuente y, cuando la traducción o `-Pletra=26` es muy larga (como *«Carrera de obstáculos»* o *«Herausforderungen»*), parte limpiamente en dos líneas equilibradas sin recurrir a `…`.

### 2.2. Historia (`Campanas.java`)
- **Sin destripar la sorpresa del hospital**: en la vista previa de la campaña y en las tarjetas de capítulo antes de combatir no se menciona el hospital ni se dibuja la venda en la frente del entrenador; el jugador descubre la sorpresa dentro de la escena ilustrada y del propio combate contra el jefe.
- ** Tarjetas de capítulo pulidas**: fondo diferenciado con borde sutil, miniatura del robot rival a la izquierda, rango (`[Noob]`, `[Junior]`, `[Senior]`, `[Especial]`) y nombre del rival con escalado automático (`cabe`), y botones **Escena** y **¡A pelear!** dimensionados según el ancho real de su texto para que nunca se corten a `-Pletra=26`.

### 2.3. Desafíos (`PrepararPelea.java`)
- **Cada cosa en un solo sitio**: las campañas salen de aquí (ya tienen su botón **Historia** en el menú principal).
- **Rejilla equilibrada de 7 modos**: fila superior de 4 tarjetas (*Contra la computadora*, *Modo azar*, *Desafío*, *Práctica*) y fila inferior centrada de 3 tarjetas (*Battle royale*, *Por equipos*, *Máquina contra máquina*), todas con borde redondeado, sombra, título sin cortes y botón **¡Vamos!** alineado en la base.
- **Selector de rondas y vista previa de arena**: integrados en una barra superior compacta con botón **Otra arena** de ancho adaptable al idioma.

### 2.4. Modo especial (`ModoEspecial.java` y `PanelSenior.java`)
- **Pestañas sin puntos suspensivos**: las 6 pestañas (*Jefes de tema*, *Tope de cálculo*, *Peleas Senior*, *Retos de programar*, *Robots de otros*, *Destrucción y supervivencia*) ajustan su tamaño de letra para caber completas incluso a `-Pletra=26` en ES, DE y EN.
- **Tarjetas de jefes, retos y rivales Senior**: cada fila se presenta dentro de una tarjeta con borde, indicador visual de completado (`✔`), descripción con margen amplio respecto al botón derecho (**Luchar** / **Abrir**) y altura calculada dinámicamente según el párrafo.

### 2.5. Estadísticas (`Historial.java`)
- **Tarjetas KPI superiores**: 8 tarjetas con borde redondeado, acento superior de color semántico (victorias en verde, derrotas en rojo, porcentaje y racha en dorado, experiencia en cian) y etiquetas que escalan limpiamente sin truncarse en ningún idioma.
- **Paneles inferiores alineados**: gráfico de barras de las últimas 30 peleas con línea base clara, desglose por modelo y por modo con barras de proporción, progreso de ejercicios por tema en dos columnas holgadas y tabla de combates con columnas medidas dinámicamente.
