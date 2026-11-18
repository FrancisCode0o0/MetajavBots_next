# Los cerebros de IA (opcionales)

El juego funciona entero sin IA. Con ella (Ajustes › Cerebros de IA) se pueden poner **hasta tres cerebros**, locales o en línea (o mezclados), y repartir entre ellos las tareas:

| Tarea (Ajustes › Qué hace cada cerebro) | Qué incluye | Por defecto |
|---|---|---|
| Diálogos, pullas y locutor | Pullas nuevas de la computadora (moderadas), el estilo del rival entre rondas (la IA no lleva cada movimiento: elige la táctica), la frase del locutor de cada ronda, la charla con tu robot en el taller | cerebro 1 |
| Revisión del código y temario | La revisión del código tras la pelea, los ejercicios a medida de tu temario (se validan antes de dártelos) | cerebro 2 |
| Tutores | El tutor de los tutoriales, de las lecciones y de los ejercicios (guía sin dar la solución) | cerebro 1 |

Si el cerebro de una tarea está vacío o falla (sin internet, sin clave, sin memoria), contesta otro de los puestos. El botón **«Recomendado»** deja el reparto del portátil de Fran: cerebro 1 = Gemma 3 4B local, cerebro 2 = Qwen3.5-9B local.

## Cada cerebro

- **Ninguno**: esa ranura no se usa.
- **Local**: KoboldCpp en tu ordenador, sin internet, con el modelo elegido de la carpeta (◄ ►). Cada cerebro local tiene **su propio KoboldCpp** (puertos 5002, 5003 y 5004; si dos usan el mismo modelo, lo comparten). Se **arranca solo la primera vez que hace falta** y se **apaga tras 8 minutos sin uso** y al cerrar el juego. Si el cerebro de Jarvis ya está en marcha (puerto 5001), se puede compartir («Usar el de Jarvis»).
- **En línea**: Claude (Anthropic), OpenAI o cualquier otro compatible con la API de OpenAI, con tu clave. La clave se guarda en `~/.metajavbots/clave_<proveedor>.txt` (en tu carpeta de usuario), **nunca en la partida ni en el repositorio**. Se pueden poner los tres en línea.

Las partidas de antes (un solo cerebro, «Local», «En línea» o «Los dos») pasan solas a las ranuras: el local a la 1 y el de en línea a la 1 o a la 2.

## La memoria (con los tres a la vez)

Los modelos locales que arranca el juego **nunca pasan, juntos, del presupuesto**: la memoria del ordenador menos 6 GB que se dejan a Windows, al juego y a lo demás (en el portátil de Fran, 20 GB → 14 GB para modelos). Si el siguiente no cabe, se apaga antes el que lleve más rato sin usarse. Ajustes enseña la memoria en uso.

Además, la **gráfica integrada** del portátil no tiene memoria para dos modelos a la vez: el primero va en la gráfica (Vulkan) y, si el segundo no cabe, KoboldCpp se arranca **solo con el procesador** (en ese portátil, Qwen3.5-9B contesta casi igual de rápido así: 25 s frente a 30 s).

## Qué modelo local (en el portátil de Fran)

i7-1065G7 (4 núcleos), gráfica integrada Intel Iris Plus, 20 GB de RAM. **KoboldCpp 1.124.2** (versión con Vulkan).

| Modelo | Tamaño | En este portátil (medido el 18-nov) |
|---|---|---|
| **Gemma 3 4B IT Q4_K_M** (`google_gemma-3-4b-it-Q4_K_M.gguf`) | 2,5 GB (3,2 GB cargado) | Lo rápido: carga en ~20-50 s y contesta en ~11-16 s. |
| **Qwen3.5-9B Q4_K_M** (`Qwen_Qwen3.5-9B-Q4_K_M.gguf`) | 6,2 GB (6,9 GB cargado) | Lo pesado: **ya carga** con KoboldCpp 1.124 (35 s); ~30 s por respuesta. Es un modelo que «piensa» antes de contestar: el juego le pide que no (`enable_thinking: false`), si no tarda minutos y se queda sin palabras. |
| Qwen2.5-Coder-7B (el de Jarvis) | 6,3 GB | Funciona con la 1.124 (30 s en cargar, ~27 s por respuesta corta). De respaldo. |

Todos en `C:\Users\Francis\OneDrive\Documentos\000KI_Local`; ninguno va en el repositorio (`.gitignore` los deja fuera). Quien baje el juego de GitHub pone KoboldCpp y los modelos que quiera en una carpeta y la elige en Ajustes.

La prueba con arranque real (tarda unos minutos): `set METAJAVBOTS_PROBAR_CEREBRO=1` y `gradlew pruebas:prueba -Psolo=cerebro`. Arranca Gemma y Qwen3.5 a la vez, les pregunta, añade el tercero (el de Jarvis) y comprueba que la memoria no pasa del presupuesto (apaga el más viejo).

## Seguridad de las pullas

Las peticiones le dicen al modelo que solo haga pullas de ring (sobre robots, metal, código, chispas) y nunca toque origen, religión, sexo, cuerpo, discapacidad ni violencia contra personas. Además, todo lo que dice pasa por `cerebro/Moderacion`, que tira la frase si toca cualquiera de esos temas; entonces se usa una escrita a mano.

## La voz

- **Voz clara (Ajustes › Voz clara, por defecto)**: el locutor lee con **Piper** (programa libre de voz, MIT, versión 2023.11.14-2), que va aparte como KoboldCpp, en `000KI_Local/piper/` (`piper.exe` y `voces/`). Voces **CC0** (dominio público): `es_ES-davefx-medium`, `de_DE-thorsten-medium` y `en_US-joe-medium` (rhasspy/piper-voices en Hugging Face; sus fichas están junto a cada voz). Encima va un efecto robótico **suave** (un poco de modulación en anillo a 55 Hz y un eco metálico de 6 y 13 ms) que no tapa las consonantes. Para otra voz: soltar su `.onnx` y su `.onnx.json` en `voces/` con el prefijo del idioma (`es_`, `de_`, `en_`).
- **Si Piper no está o falla**, la voz propia (`sonido/VozRobot`, por formantes, sin bibliotecas), ahora con menos saltos de tono y menos efecto para que se entienda mejor.
