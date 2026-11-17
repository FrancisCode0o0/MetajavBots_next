# El editor de código

La base de todo (Fran: «impecable, que no se rompa nunca»). Dos piezas:

- `codigo/Documento`: todo el texto, el cursor, la selección y el deshacer, sin dibujo. Cada posición que llega de fuera se recorta; los saltos de Windows y los tabuladores se normalizan al entrar. La prueba «miles de pulsaciones al azar» lo machaca con 16 000 pulsaciones y comprueba que el cursor nunca sale del texto, que las líneas suman el texto y que deshacer todo y rehacer todo devuelven exactamente el principio y el final.
- `pantalla/VentanaDeCodigo`: el dibujo y las teclas. Cada línea se dibuja de una vez con sus colores (las letras las coloca la fuente), así ninguna se monta en otra.

## Estilo

El de los profesionales (convenciones de Oracle y guía de Google para Java): 4 espacios y nunca tabuladores, la llave que abre al final de la línea y la que cierra en la suya, `case` dentro del `switch`, +8 en las líneas que siguen una sentencia partida. Ctrl+Mayús+F lo ordena todo sin cambiar ninguna letra que no sea un espacio (probado con 300 códigos al azar y con lo ya formateado). Los bloques de texto (`"""`) no se tocan.

## Teclas

| Tecla | Qué hace |
|---|---|
| Ctrl+S, Ctrl+Intro | Compilar (o comprobar el ejercicio) |
| Ctrl+Z / Ctrl+Y (Ctrl+Mayús+Z) | Deshacer / rehacer (por palabras) |
| Ctrl+X / C / V | Cortar / copiar / pegar (sin selección: la línea entera); al pegar, la sangría se ajusta |
| Ctrl+A | Todo |
| Ctrl+D | Duplicar la línea (o lo elegido) |
| Ctrl+Mayús+K | Borrar la línea |
| Alt+↑ / Alt+↓ | Mover líneas |
| Ctrl+/ | Comentar o descomentar |
| Tab / Mayús+Tab | Meter o sacar sangría (con varias líneas, todas) |
| Ctrl+F / Ctrl+H / F3 | Buscar / reemplazar / siguiente |
| Ctrl+Espacio | Autocompletar (la API del idioma, palabras clave, nombres del código) |
| Ctrl+Mayús+F | Formatear |
| F1 o Ctrl+clic | Ayuda de la palabra en el panel de la API |
| Ctrl+← / → | Por palabras (con Mayús, eligiendo) |
| Inicio | Tras la sangría; otra vez, al principio |
| Ctrl+rueda, Ctrl++ / Ctrl+- / Ctrl+0 | Tamaño de la letra |
| Doble y triple clic | Palabra y línea |

Con el teclado español, AltGr (que el sistema da como Ctrl+Alt) escribe `{ } [ ]` y no se toma por atajo.
