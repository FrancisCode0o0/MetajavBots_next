# El cerebro de IA (opcional)

El juego funciona entero sin IA. Con ella (Ajustes › Cerebro de IA):

| Para qué | Qué cerebro usa con «Los dos» |
|---|---|
| Pullas nuevas de la computadora en la pelea (moderadas) | local (rápido) |
| El estilo del rival entre rondas (la IA no lleva cada movimiento: elige la táctica a la vista del informe) | local |
| El locutor resume cada ronda con una frase propia | local |
| Charlar con tu robot en el taller | local |
| El tutor de los ejercicios y de los tutoriales (guía sin dar la solución) | en línea (si falla, local) |
| La revisión del código tras la pelea | en línea (si falla, local) |
| Ejercicios a medida de tu temario (se validan antes de dártelos) | en línea (si falla, local) |

## Opciones

- **Ninguno**: todo funciona con lo escrito a mano (diálogos, revisor, temario por palabras).
- **Local**: KoboldCpp en tu ordenador, sin internet. El juego lo **arranca solo la primera vez que hace falta** (con el modelo elegido de la carpeta, puerto 5002) y lo **apaga tras 8 minutos sin uso** y al cerrar el juego: no gasta memoria mientras no se usa. Si el cerebro de Jarvis ya está en marcha (puerto 5001), se puede compartir («Usar el de Jarvis»).
- **En línea**: Claude (Anthropic), OpenAI o cualquier otro compatible con la API de OpenAI, con tu propia clave. La clave se guarda en `~/.metajavbots/clave_<proveedor>.txt` (en tu carpeta de usuario), **nunca en la partida ni en el repositorio**.
- **Los dos**: el local para lo rápido y constante, el de en línea para lo pesado; si falla internet o la clave, el local se encarga de todo.

## Qué modelo local (en el portátil de Fran)

i7-1065G7 (4 núcleos), gráfica integrada Intel Iris Plus, 20 GB de RAM.

| Modelo | Tamaño | En este portátil |
|---|---|---|
| **Gemma 3 4B IT Q4_K_M** (`google_gemma-3-4b-it-Q4_K_M.gguf`) | 2,5 GB | **El elegido**: ~50 s la primera vez (cargar) y ~11 s por respuesta corta; deja memoria de sobra para Windows y el juego. |
| Qwen3.5-9B Q4_K_M (`Qwen_Qwen3.5-9B-Q4_K_M.gguf`) | 6,2 GB | Bajado como pidió Fran, pero KoboldCpp 1.112.2 no lo carga aún (falta un tensor de su arquitectura nueva). Con un KoboldCpp más nuevo funcionará, aunque será más lento. |
| Qwen2.5-Coder-7B (el de Jarvis) | 6,3 GB | Funciona; ~40 s por respuesta corta. De respaldo. |

Todos en `C:\Users\Francis\OneDrive\Documentos\000KI_Local`; ninguno va en el repositorio (`.gitignore` los deja fuera). Quien baje el juego de GitHub pone KoboldCpp y el modelo que quiera en una carpeta y la elige en Ajustes.

## Seguridad de las pullas

Las peticiones le dicen al modelo que solo haga pullas de ring (sobre robots, metal, código, chispas) y nunca toque origen, religión, sexo, cuerpo, discapacidad ni violencia contra personas. Además, todo lo que dice pasa por `cerebro/Moderacion`, que tira la frase si toca cualquiera de esos temas; entonces se usa una escrita a mano.

## La voz

El locutor habla con `sonido/VozRobot`, un sintetizador por formantes propio (sin internet ni bibliotecas). Para una voz más clara quedaría SVOX Pico TTS (Apache-2.0, español, alemán e inglés) compilado con Zig: no se pudo bajar el compilador esta noche (docs/ESTADO.md).
