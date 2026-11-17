# Créditos y licencias de lo que viene de fuera

Para que Fran lo revise antes de publicar. Todo lo demás (código, textos, ejercicios, tutoriales, diálogos, robots, escenarios, sonidos y la voz) es de **Francis García Reyes**: el código con licencia Apache-2.0 (LICENSE) y lo demás con CC BY 4.0 (docs/LICENCIA_CONTENIDO.md).

## Bibliotecas (las baja Gradle; no están en el repositorio)

| Qué | Autor | Licencia | Para qué |
|---|---|---|---|
| libGDX 1.14.2 (gdx, freetype, backend LWJGL3, headless) | Mario Zechner, Nathan Sweet y colaboradores | Apache-2.0 | Ventana, dibujo, letras, sonido |
| LWJGL 3 | Lightweight Java Game Library | BSD-3-Clause | Por debajo de libGDX (OpenGL, OpenAL, GLFW) |
| shapedrawer 2.5.0 | earlygrey | MIT | Formas (los robots, la arena) |
| blade-ink 1.3.2 | bladecoder | Apache-2.0 | La usa el motor (CastillaFantasX) para los diálogos de Ink; MetajavBots no la usa directamente |
| jlayer 1.0.1 (gdx) y jorbis | JavaZOOM / JCraft | **LGPL** | Llegan con el backend de escritorio de libGDX (MP3 y OGG). El juego solo usa WAV, pero sin ellas libGDX no arranca el sonido. **A revisar por Fran** (docs/ESTADO.md). |
| Gradle Wrapper 9.8 | Gradle Inc. | Apache-2.0 | Compilar |

## Motor

CastillaFantasX (carpeta `motor/`), del mismo autor, copiado de la Reliquia del Alba (rama `orden_2`, commit `948211b`).

## Fuentes

| Qué | Autor | Licencia |
|---|---|---|
| DejaVu Sans Mono (normal y negrita) | Proyecto DejaVu, sobre Bitstream Vera | Licencia de Bitstream Vera (libre, redistribuible) y cambios de DejaVu en dominio público. Texto en `res/fuentes/LICENCIA_DejaVu.txt`. **No es CC0 ni CC BY**: a revisar por Fran (docs/ESTADO.md). |

## Opcional, fuera del repositorio (el jugador lo pone si quiere)

| Qué | Licencia | Notas |
|---|---|---|
| KoboldCpp (cerebro local) | AGPL-3.0 | Programa aparte que el juego arranca si está en la carpeta de modelos. No se distribuye con el juego. |
| Gemma 3 4B IT (GGUF de bartowski) | Gemma Terms of Use (Google) | Modelo local opcional. No se distribuye. |
| Qwen3.5-9B (GGUF de bartowski) | Apache-2.0 | Modelo local opcional. No se distribuye. |
| Qwen2.5-Coder-7B-Instruct (el de Jarvis) | Apache-2.0 | Modelo local opcional. No se distribuye. |

## Inspiración (sin copiar nada)

- **Robocode** (la idea de robots programados en Java que pelean).
- **La serie Medabots** (robots con partes, combates con láser, cuerpo a cuerpo y proyectiles): solo de inspiración. Ni nombres, ni diseños, ni imágenes: los 16 modelos son diseños propios.
- **CodingBat** y **CodeStepByStep** (el estilo de ejercicios cortos que se comprueban solos).
- **«Java Challenge»** de Michael Inden y **«Captain CiaoCiao»** de Christian Ullenboom (https://github.com/ullenboom/captain-ciaociao): el espíritu de los ejercicios temáticos. Ningún ejercicio, nombre ni texto copiado.
- Las convenciones de estilo de Oracle y la guía de Google para Java (el formateo del editor).
