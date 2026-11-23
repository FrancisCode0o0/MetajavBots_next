# El modo especial

Todas las ideas de Fran, en Menú › Modo especial (y repartidas por el juego):

1. **Jefes de tema** (`pantallas/Jefe`, 9 jefes): cada uno es casi inmune (recibe el 10 % del daño) salvo que tu código use de verdad su tema: herencia propia (no basta heredar de Metajavbot), interfaz propia implementada, colecciones, excepciones lanzadas y atrapadas, genéricos, lambdas, streams, recursión o clases seguras para hilos. Lo decide `codigo/DetectorDeTemas` mirando el código (no los comentarios). Vencerlos da piezas épicas.
2. **Tope de cálculo por turno**: peleas con 150, 400 o 1000 pasos por turno. Cada vuelta de bucle y cada llamada a método es un paso (lo cuenta el Inyector); quien se pasa se queda «pensando» unos turnos. El código ineficiente hace lento al robot.
3. **Depurador en vivo**: en cualquier pelea, Espacio pausa, → avanza un turno y F9 abre el panel con las variables de tu robot (por reflexión), sus pasos por turno y su consola.
4. **Errores explicados según el nivel**: `res/textos/errores.txt` (113 códigos de javac, más los del juego), con un texto para Noob, otro para Junior y otro para Senior, en tres idiomas. Debajo, siempre, el mensaje original de javac para aprender a leerlo.
5. **Piezas por escribir pruebas** (el laboratorio, `ejercicios/Laboratorio`, `res/laboratorio/`): escribes pruebas al estilo JUnit 5 (`metajavbots.junit`: @Test, assertEquals, assertThrows…) para una clase; valen si pasan con la buena y cazan a todos los mutantes (copias con un fallo escondido). Tres retos: trayectoria, blindaje y puntería.
6. **Revisión del código por la IA tras el combate**: «Revisar el código» al acabar la pelea; sin IA, los consejos del Revisor (cifras de la pelea y estilo profesional: números mágicos, métodos largos, catch vacíos, == con textos…).
7. **Robots exportables como .java**: Taller › Exportar .java (con una cabecera `// @metajavbot modelo=… cabeza=…`). Los de otros se ponen en `robots_ajenos/` y se pelea contra ellos (o con ellos de compañeros) desde «Desafíos».

Y además: **retos de programar** que dan piezas (predicción, radar, esquiva), **máquina contra máquina** (ver qué línea ejecuta cada robot) y **combates temáticos** del modo temario.
