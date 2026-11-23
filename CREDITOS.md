# Créditos y licencias de lo que viene de fuera

Para que Fran lo revise antes de publicar. Todo lo demás (código, textos, ejercicios, tutoriales, diálogos, robots, escenarios, sonidos y la voz) es de **Francis García Reyes**: el código con licencia Apache-2.0 (LICENSE) y lo demás con CC BY 4.0 (docs/LICENCIA_CONTENIDO.md).

## Bibliotecas (las baja Gradle; no están en el repositorio)

| Qué | Autor | Licencia | Para qué |
|---|---|---|---|
| libGDX 1.14.2 (gdx, freetype, backend LWJGL3, headless) | Mario Zechner, Nathan Sweet y colaboradores | Apache-2.0 | Ventana, dibujo, letras, sonido |
| LWJGL 3 | Lightweight Java Game Library | BSD-3-Clause | Por debajo de libGDX (OpenGL, OpenAL, GLFW) |
| shapedrawer 2.5.0 | earlygrey | MIT | Formas (los robots, la arena) |
| blade-ink 1.3.2 | bladecoder | Apache-2.0 | La usa el motor (CastillaFantasX) para los diálogos de Ink; MetajavBots no la usa directamente |
| Apache PDFBox 3.0.4 (pdfbox, fontbox, pdfbox-io, commons-logging) | The Apache Software Foundation | Apache-2.0 | Extracción de texto de archivos PDF en «Mi temario» (streams comprimidos, fuentes CID/Type0, mapas ToUnicode y codificaciones) |
| jlayer 1.0.1 (gdx) y jorbis | JavaZOOM / JCraft | **LGPL** | Llegan con el backend de escritorio de libGDX (MP3 y OGG). El juego solo usa WAV, pero sin ellas libGDX no arranca el sonido. **A revisar por Fran** (docs/ESTADO.md). |
| Gradle Wrapper 9.8 | Gradle Inc. | Apache-2.0 | Compilar |
| Checkstyle 10.21.1 | Checkstyle | LGPL-2.1 | Solo para revisar el estilo del código (`gradlew estilo`); no va dentro del juego |

## Motor

CastillaFantasX (carpeta `motor/`), del mismo autor, copiado de la Reliquia del Alba (rama `orden_2`, commit `948211b`).

## Fuentes

| Qué | Autor | Licencia | Uso |
|---|---|---|---|
| Atkinson Hyperlegible (Regular y Bold) | Braille Institute of America | SIL Open Font License 1.1 (libre, redistribuible). Texto en `res/fuentes/LICENCIA_AtkinsonHyperlegible_OFL.txt`. | Interfaz, lectura (textos, botones, descripciones, tutoriales) y títulos/marcadores (variante Bold) |
| JetBrains Mono (Regular y Bold) | JetBrains s.r.o. | SIL Open Font License 1.1 (libre, redistribuible). Texto en `res/fuentes/LICENCIA_JetBrainsMono_OFL.txt`. | Editor de código, visor del rival y depurador (`VentanaDeCodigo`, `PanelDepurador`, `CodigoDelRival`), renderizada glifo a glifo sin ligaduras |
| Noto Sans Symbols y Noto Sans Symbols 2 (Regular) | Google (Proyecto Noto Fonts) | SIL Open Font License 1.1 (libre, redistribuible). Texto en `res/fuentes/LICENCIA_NotoSansSymbols2_OFL.txt`. | Respaldo automático de símbolos técnicos e iconos del juego (`▶ ✎ ✕ ⚔ ⚙ ⚠ …`) |

## Opcional, fuera del repositorio (el jugador lo pone si quiere)

| Qué | Licencia | Notas |
|---|---|---|
| KoboldCpp (cerebro local) | AGPL-3.0 | Programa aparte que el juego arranca si está en la carpeta de modelos. No se distribuye con el juego. |
| Gemma 3 4B IT (GGUF de bartowski) | Gemma Terms of Use (Google) | Modelo local opcional. No se distribuye. |
| Qwen3.5-9B (GGUF de bartowski) | Apache-2.0 | Modelo local opcional. No se distribuye. |
| Qwen2.5-Coder-7B-Instruct (el de Jarvis) | Apache-2.0 | Modelo local opcional. No se distribuye. |
| Piper 2023.11.14-2 (voz clara del locutor) | MIT (trae espeak-ng, GPL-3.0, dentro del programa) | Programa aparte en 000KI_Local/piper que el juego llama si está. No se distribuye con el juego. |
| Voces de Piper: es_ES-davefx-medium, de_DE-thorsten-medium, en_US-joe-medium | CC0 (datos de voz en dominio público) | rhasspy/piper-voices. No se distribuyen. |

## Inspiración (sin copiar nada)

- **Robocode** (la idea de robots programados en Java que pelean).
- **La serie Medabots** (robots con partes, combates con láser, cuerpo a cuerpo y proyectiles): solo de inspiración. Ni nombres, ni diseños, ni imágenes: los 16 modelos son diseños propios.
- **CodingBat** y **CodeStepByStep** (el estilo de ejercicios cortos que se comprueban solos).
- **«Java Challenge»** de Michael Inden y **«Captain CiaoCiao»** de Christian Ullenboom (https://github.com/ullenboom/captain-ciaociao): el espíritu de los ejercicios temáticos. Ningún ejercicio, nombre ni texto copiado.
- Las convenciones de estilo de Oracle y la guía de Google para Java (el formateo del editor).

## Música

Todos los temas de `res/musica/` (47 piezas MIDI: menú, combates, último asalto, Carrera de obstáculos y recta final, Destrucción y cuenta atrás, minijuegos, jefes, peleas Senior, campañas Creed e Isla Clandestina, escenas ilustradas de hospital/milagro/victoria/derrota, Tienda, Taller y fanfarrias de victoria y derrota) son **100 % originales**, compuestos para MetajavBots en `herramientas/componer_musica.py` (código Apache-2.0, piezas bajo CC BY 4.0 como el resto del contenido del repositorio). Suenan con el SoundFont opcional `GeneralUser-GS.sf2` en `~/.metajavbots/` o, si no está presente, con el sintetizador MIDI que trae Java. Ninguna melodía ni riff proviene de bandas sonoras reales.
