# Informe: Pistas de ubicación en el panel de API y referencia

## 1. Archivos modificados

- `juego/src/main/java/metajavbots/pantallas/Referencia.java`: añadido el método `Entrada.donde(Idioma)` (lee `donde.es`, `donde.de`, `donde.en`) e incluido el texto de la pista dentro de `Referencia.buscar(...)`.
- `juego/src/main/java/metajavbots/pantallas/PanelDeApi.java`: añadido el bloque visual «Dónde ponerlo» dentro de la zona desplazable (`ui.desplazable(ficha, ...)`) en `dibujarFicha(...)`, diferenciado de la explicación y del pseudocódigo/ejemplo mediante cabecera en `ui.tema.aviso`, caja con fondo `ui.tema.panelAlto` y barra lateral de acento, respetando márgenes horizontales y sin invadir el botón «Insertar».
- `res/textos/ui.txt`: añadida la clave trilingüe `api.donde = Dónde ponerlo | Wo einsetzen | Where to place it`.
- `res/datos/api.txt`: añadidas pistas únicas `donde.es`, `donde.de` y `donde.en` a las **93 entradas** de la API de robots (verificadas contra `Metajavbot.java`, `RobotBase.java`, `Pelea.java` y las interfaces `Enemigo`, `Golpe`, `Obstaculo`, `Aparato`).
- `res/datos/referencia.txt`: añadidas pistas únicas `donde.es`, `donde.de` y `donde.en` a las **239 entradas** de la referencia de Java (verificadas contra las restricciones reales de `Vigilante.java`, como el bloqueo de hilos en robots).
- `pruebas/src/main/java/metajavbots/pruebas/PruebasDelContenido.java`: añadido el caso de prueba `apuntarPistasReferencia()` que valida las 332 entradas (93 API + 239 referencia), exige pistas no vacías y traducidas en ES, DE y EN, y comprueba que no exista ninguna pista duplicada entre entradas distintas en ningún idioma.

## 2. Muestras variadas de pistas añadidas

### a) Evento (`api.alVerEnemigo`)
- **ES**: Sobrescríbelo en el cuerpo de tu clase Metajavbot, nunca dentro de cadaTurno(); no lo invoques a mano porque solo el motor te pasa el parámetro Enemigo e.
- **DE**: Direkt im Rumpf deiner MetajavbotDe-Klasse überschreiben, nie in jedenZug(); nicht selbst aufrufen, denn nur die Engine liefert das Gegner-Objekt g.
- **EN**: Override directly in your MetajavbotEn class body, never inside onTick(); do not invoke it yourself since only the engine supplies the Enemy parameter e.

### b) Acción / consulta del robot (`api.avanzar`)
- **ES**: Llámalo dentro de alEmpezar(), cadaTurno() o un evento, mejor tras comprobar restanteAvance() == 0; nunca en el constructor ni en el cuerpo de la clase.
- **DE**: In beimStart(), jedenZug() oder einem Ereignis aufrufen, idealerweise wenn restStrecke() == 0 gilt; nie im Konstruktor oder direkt im Klassenrumpf.
- **EN**: Call inside onStart(), onTick(), or an event handler, ideally when remainingDistance() == 0; never in the constructor or directly in the class body.

### c) Método de objeto (`Golpe.angulo`)
- **ES**: Úsalo como g.angulo() dentro de alRecibirGolpe(Golpe g) para esquivar de lado (g.angulo() + 90) o mirar hacia el atacante; nunca fuera de eventos de golpe.
- **DE**: Als t.winkel() in beiTreffer(Treffer t) nutzen, um seitlich auszuweichen (t.winkel() + 90) oder zurückzuzielen; nicht außerhalb von Treffer-Ereignissen.
- **EN**: Use h.angle() inside onHit(Hit h) to strafe perpendicular (h.angle() + 90) or turn toward the shooter; never outside hit event handlers.

### d) Elemento de referencia Java (`Thread.sleep`)
- **ES**: Úsalo solo dentro de try-catch en ejercicios de concurrencia o pruebas de pausas; NUNCA lo pongas en un robot ni para esperar turnos (en robots usa cadaTurno()).
- **DE**: Nur in try-catch in Nebenläufigkeits-Übungen für Pausen nutzen; NIEMALS im Roboter oder zum Warten auf Züge (dort läuft jedenZug() automatisch).
- **EN**: Use only inside try-catch in concurrency exercises; NEVER put it in a robot or to wait for ticks (robots use onTick() automatically).

## 3. Dudas pendientes

- Ninguna. Se respetó el alcance solicitado sin modificar música, efectos de sonido, voz, animaciones de robots ni el resto de pantallas.

## 4. Verificación (`./gradlew.bat compileJava estilo pruebas:prueba`)

- **Compilación (`compileJava`)**: correcta (`BUILD SUCCESSFUL`).
- **Estilo (`estilo` / Checkstyle)**: **0 avisos** en `juego`, `lwjgl3` y `pruebas`.
- **Pruebas (`pruebas:prueba`)**: **117 casos bien, 0 mal** (incluyendo la verificación completa de las 332 pistas en ES, DE y EN sin vacíos ni duplicados).
