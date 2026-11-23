package metajavbots;

import java.util.ArrayDeque;
import java.util.Deque;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

import castillafantasx.Cierre;
import metajavbots.codigo.Salida;
import metajavbots.pantalla.Entrada;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Tema;
import metajavbots.pantalla.Ui;
import metajavbots.pantallas.Inicio;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.sonido.Sonidos;
import metajavbots.textos.T;

/**
 * JUEGO — MetajavBots. Lleva la pantalla que toca, el jugador de la partida,
 * el pincel, la entrada y el sonido. Cada fotograma va protegido: si una
 * pantalla falla, se apunta el error en registro/ (con el Cierre del motor)
 * y se vuelve al menú, sin perder la partida.
 */
public class Juego extends ApplicationAdapter {

    public Pincel pincel;
    public Entrada entrada;
    public Ui ui;
    public Sonidos sonidos;
    public metajavbots.sonido.Voz voz = new metajavbots.sonido.Voz();
    public final metajavbots.sonido.Musica musica = new metajavbots.sonido.Musica();
    public Jugador jugador;
    private Pantalla pantalla;
    private final Deque<Pantalla> anteriores = new ArrayDeque<>();
    /** La sacudida de la pantalla (al subir de nivel, en las explosiones). */
    public float sacudida;
    private float tiempoSacudida;
    private int fallosSeguidos;
    private boolean cerrado;
    /** El entrenador flotante: asoma en todas las pantallas y explica cualquier concepto (F2). */
    public final metajavbots.pantallas.EntrenadorFlotante entrenadorFlotante = new metajavbots.pantallas.EntrenadorFlotante();
    /** Alarma visual luminosa en rojo cuando el jugador baja de nivel tras varias derrotas seguidas. */
    public final metajavbots.pantallas.AlarmaBajada alarmaBajada = new metajavbots.pantallas.AlarmaBajada();
    /** Aviso obligatorio del entrenador activo cuando el robot del jugador aún no se ha compilado nunca. */
    public final metajavbots.pantallas.AvisoDeCompilacion avisoCompilar = new metajavbots.pantallas.AvisoDeCompilacion();
    /** Si el visor táctico de la arena (F8) está activo durante esta sesión (desactivado por defecto). */
    public boolean visorTactico;
    /** Los archivos que se acaban de soltar en la ventana (el escritorio los pone; los recoge quien los quiera, como el temario). */
    public volatile String[] archivosSoltados;
    /** El selector nativo de archivos del sistema (el escritorio lo pone; null sin ventana). */
    public volatile metajavbots.pantalla.SelectorDeArchivos selectorDeArchivos;
    /** La primera pantalla (Capturas pone otras). */
    protected Pantalla primera;

    @Override
    public void create() {
        Salida.instalar();
        Cierre.enConsola = true;
        pincel = new Pincel();
        pincel.redimensionar(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        entrada = new Entrada(pincel);
        Gdx.input.setInputProcessor(entrada);
        ui = new Ui(pincel, entrada);
        sonidos = new Sonidos();
        ui.sonidoDeClic = () -> sonidos.tocar("clic", 0.5f);
        ir(primera != null ? primera : new Inicio());
    }

    @Override
    public void resize(int w, int h) {
        if (w <= 0 || h <= 0 || pincel == null) return;
        try {
            pincel.redimensionar(w, h);
        } catch (Throwable t) {
            Cierre.apuntar(t, null);
        }
    }

    /** Va a otra pantalla (y recuerda esta para volver). */
    public void ir(Pantalla nueva) {
        // Un robot que pasa del 120 % de la carga de sus piernas no sale a pelear: al Taller, que explica qué quitar.
        if (nueva instanceof metajavbots.pantallas.PantallaPelea pelea && pelea.usaRobotDelJugador() && jugador != null
                && jugador.robot() != null && !jugador.montajeDe(jugador.robot()).puedePelear()) {
            nueva = metajavbots.pantallas.Taller.porSobrepeso();
        }
        if (nueva instanceof metajavbots.pantallas.PantallaPelea pelea && pelea.usaRobotDelJugador()
                && !avisoCompilar.consumirPasePelea()
                && metajavbots.pantallas.AvisoDeCompilacion.debeAvisarAlPelear(jugador)) {
            avisoCompilar.mostrarAntesDePelear(this, pelea);
            return;
        }
        if (voz != null) voz.detener();
        if (ui != null) ui.quitarFoco();
        if (entrada != null) entrada.limpiar();
        if (pantalla != null) {
            pantalla.alSalir(this);
            anteriores.push(pantalla);
            while (anteriores.size() > 20) anteriores.removeLast();
        }
        pantalla = nueva;
        nueva.alEntrar(this);
    }

    /** Va a otra pantalla olvidando las anteriores (al menú, por ejemplo). */
    public void irSolo(Pantalla nueva) {
        if (voz != null) voz.detener();
        if (ui != null) ui.quitarFoco();
        if (entrada != null) entrada.limpiar();
        if (entrenadorFlotante != null) entrenadorFlotante.cerrar();
        if (pantalla != null) pantalla.alSalir(this);
        anteriores.clear();
        pantalla = nueva;
        nueva.alEntrar(this);
    }

    /** Vuelve a la pantalla de antes. */
    public void volver() {
        if (anteriores.isEmpty()) return;
        if (voz != null) voz.detener();
        if (ui != null) ui.quitarFoco();
        if (entrada != null) entrada.limpiar();
        if (pantalla != null) pantalla.alSalir(this);
        pantalla = anteriores.pop();
        pantalla.alEntrar(this);
    }

    public Pantalla pantalla() {
        return pantalla;
    }

    public void sacudir(float fuerza, float segundos) {
        sacudida = Math.max(sacudida, fuerza);
        tiempoSacudida = Math.max(tiempoSacudida, segundos);
    }

    /** El jugador elegido: idioma, tema y letra de toda la interfaz. */
    public void ponerJugador(Jugador j) {
        jugador = j;
        alarmaBajada.cancelar();
        avisoCompilar.cerrar();
        if (j != null) {
            metajavbots.tutor.Biografias.usarPartida(j);
            T.idioma = j.idioma;
            ui.tema = Tema.de(j.temaClaro);
            ui.letra = Math.max(14, Math.min(28, j.tamanoDeLetra));
            sonidos.volumen = j.volumen;
            voz.activa = j.vozDelLocutor;
            voz.clara = j.vozClara;
            voz.usarPiper(metajavbots.cerebro.CerebroLocal.carpeta(j.carpetaModelos).resolve("piper"));
            voz.volumen = j.volumenVoz;
            if (j.vozDelLocutor && j.vozClara && Gdx.audio != null) {
                voz.precargar(j.idioma, metajavbots.tutor.Entrenador.de(j.entrenador).timbre);
            }
            musica.activa = j.musicaActiva;
            musica.volumen = j.volumenMusica;
            metajavbots.pantallas.PrepararPelea.conTrampas = j.arenasConTrampas;
            entrenadorFlotante.sincronizarEntrenador(j);
        }
    }

    public void guardar() {
        if (jugador == null) return;
        try {
            Guardado.guardar(jugador);
        } catch (Exception e) {
            Cierre.apuntar(e, null);
        }
    }

    @Override
    public void render() {
        float dt = Math.min(0.1f, Gdx.graphics.getDeltaTime());
        entrada.nuevoFotograma(dt);
        Gdx.gl.glClearColor(0.05f, 0.06f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (tiempoSacudida > 0) {
            tiempoSacudida -= dt;
            if (tiempoSacudida <= 0) sacudida = 0;
        }
        pincel.empezar();
        ui.empezarFotograma();
        try {
            entrenadorFlotante.antes(this);
            avisoCompilar.antes(this);
            pantalla.fotograma(this, dt);
            entrenadorFlotante.dibujar(this, dt);
            avisoCompilar.dibujar(this, dt);
            ui.acabarFotograma();
            fallosSeguidos = 0;
        } catch (Throwable e) {
            entrada.bloqueada = false;
            fallosSeguidos++;
            Cierre.apuntar(e, null);
            if (fallosSeguidos > 3) {
                // Algo falla una y otra vez en esta pantalla: se vuelve al menú (o al inicio).
                fallosSeguidos = 0;
                try {
                    irSolo(jugador != null ? new metajavbots.pantallas.Menu() : new Inicio());
                } catch (Throwable secundario) {
                    Cierre.apuntar(secundario, null);
                }
            }
        } finally {
            try {
                pincel.acabar();
            } catch (RuntimeException e) {
                // el lote ya estaba cerrado
            }
        }
        // La voz suena en cualquier pantalla (el locutor, el tutor del taller, el entrenador flotante).
        try {
            voz.fotograma(sonidos);
            sonidos.fotograma(dt);
            musica.tocar(pantalla.musica());
            musica.fotograma();
        } catch (Throwable t) {
            Cierre.apuntar(t, null);
        }
    }

    @Override
    public void dispose() {
        if (cerrado) return;
        cerrado = true;
        if (pantalla != null) {
            try {
                pantalla.alSalir(this);
            } catch (Throwable t) {
                Cierre.apuntar(t, null);
            }
        }
        try {
            guardar();
        } catch (Throwable t) {
            // no impedir el cierre de los recursos si falla el guardado
        }
        // El cerebro local que arrancó el juego no se queda gastando memoria.
        metajavbots.cerebro.CerebroLocal.cerrar();
        voz.liberar();
        if (sonidos != null) sonidos.liberar();
        musica.liberar();
        if (pincel != null) pincel.liberar();
    }
}
