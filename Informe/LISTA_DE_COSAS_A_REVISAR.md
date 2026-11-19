# Lista de cosas pedidas para revisar en la auditoría

1. **Fuga de foco / cursor entre pantallas:**
   - Un cursor de entrada de texto se queda activo o visible al cambiar a otra pantalla.
   - Revisar la limpieza de estados de UI, listeners de teclado/ratón y foco en las transiciones de pantalla (`Juego.java`, `Ui.java`, `Entrada.java`, `VentanaDeCodigo.java`, `CampoDeTexto.java` y pantallas afectadas).

2. **Recorte visual (clipping) — Cabeza del entrenador en el margen/esquina derecha:**
   - La cabeza de un personaje aparece medio cortada (aclarado específicamente como la cabeza del entrenador en la esquina/margen derecho).
   - Revisar `EntrenadorFlotante.java`, `CabezaDeEntrenador.java` y cualquier clase que posicione o recorte al entrenador en ese margen:
     - Comprobar tanto el margen superior como el margen derecho (coordenadas X/Y, radio, adornos y cualquier `p.recortar` o límite de ventana).
     - Mantener intacto el efecto intencional de asomar media cabeza en reposo en `EntrenadorFlotante.java`, corrigiendo únicamente desbordamientos no intencionales reales (como iconos o adornos pisando bordes).
     - Descartar cambios en los robots (`Campanas.java`, `AnimacionDelCaso.java`, `FinalesDelRobotito.java`, `Menu.java` y `Taller.java`).

3. **Ventana que desaparece con el juego/audio corriendo en segundo plano:**
   - A veces la ventana se cierra o desaparece, pero el audio y el proceso siguen activos hasta matar la terminal.
   - Revisar el ciclo de vida de la ventana, hilos que no se cierran (`dispose`, `System.exit`, hilos de audio/bucle principal, apagado limpio de MIDI en `Musica.java` y subprocesos en `Voz.java` y `CerebroLocal.java`) y posibles excepciones no capturadas en el hilo gráfico (`Juego.java`, `Main.java`).

4. **Latencia en diálogos de personajes:**
   - Auditar `Voz.java`, `VozPiper.java` y el flujo de reproducción de diálogos para detectar y corregir la causa del retraso en las voces.

5. **Auditoría abierta (bajo la regla innegociable de no romper nada de lo que ya funciona ni alterar comportamientos intencionales):**
   - Detectar y corregir otros bugs reales de estabilidad, concurrencia, fugas de recursos o rendimiento evidente.
