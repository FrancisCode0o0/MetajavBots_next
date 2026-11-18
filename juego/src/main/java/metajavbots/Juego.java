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
        if (pincel != null) pincel.redimensionar(w, h);
    }

    /** Va a otra pantalla (y recuerda esta para volver). */
    public void ir(Pantalla nueva) {
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
        if (pantalla != null) pantalla.alSalir(this);
        anteriores.clear();
        pantalla = nueva;
        nueva.alEntrar(this);
    }

    /** Vuelve a la pantalla de antes. */
    public void volver() {
        if (anteriores.isEmpty()) return;
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
        if (j != null) {
            T.idioma = j.idioma;
            ui.tema = Tema.de(j.temaClaro);
            ui.letra = Math.max(14, Math.min(28, j.tamanoDeLetra));
            sonidos.volumen = j.volumen;
            voz.activa = j.vozDelLocutor;
            voz.clara = j.vozClara;
            voz.usarPiper(metajavbots.cerebro.CerebroLocal.carpeta(j.carpetaModelos).resolve("piper"));
            voz.volumen = j.volumenVoz;
            musica.activa = j.musicaActiva;
            musica.volumen = j.volumenMusica;
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
            pantalla.fotograma(this, dt);
            ui.acabarFotograma();
            fallosSeguidos = 0;
        } catch (Throwable e) {
            fallosSeguidos++;
            Cierre.apuntar(e, null);
            if (fallosSeguidos > 3) {
                // Algo falla una y otra vez en esta pantalla: se vuelve al menú (o al inicio).
                fallosSeguidos = 0;
                irSolo(jugador != null ? new metajavbots.pantallas.Menu() : new Inicio());
            }
        } finally {
            try {
                pincel.acabar();
            } catch (RuntimeException e) {
                // el lote ya estaba cerrado
            }
        }
        sonidos.fotograma(dt);
        musica.tocar(pantalla.musica());
        musica.fotograma();
    }

    @Override
    public void dispose() {
        guardar();
        // El cerebro local que arrancó el juego no se queda gastando memoria.
        metajavbots.cerebro.CerebroLocal.cerrar();
        if (sonidos != null) sonidos.liberar();
        musica.liberar();
        if (pincel != null) pincel.liberar();
    }
}
