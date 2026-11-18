package metajavbots.pantalla;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

/**
 * ENTRADA — Lo que hace el jugador en este fotograma: el ratón (en
 * coordenadas lógicas), los clics (simples, dobles y triples), la rueda, las
 * letras escritas y las teclas pulsadas con sus modificadores. Las pantallas
 * lo leen; quien usa algo lo «consume» para que no lo use otro.
 *
 * Con un teclado español, las llaves y los corchetes se escriben con AltGr,
 * que el sistema da como Ctrl+Alt: esas letras se escriben, no se toman por
 * atajos de Ctrl.
 */
public final class Entrada extends InputAdapter {

    /** Una tecla pulsada, con los modificadores de ese momento. */
    public static final class Tecla {
        public final int codigo;
        public final boolean ctrl, mayus, alt;
        public boolean usada;

        Tecla(int codigo, boolean ctrl, boolean mayus, boolean alt) {
            this.codigo = codigo;
            this.ctrl = ctrl;
            this.mayus = mayus;
            this.alt = alt;
        }

        public boolean es(int c, boolean conCtrl) {
            return !usada && codigo == c && ctrl == conCtrl;
        }
    }

    private final Pincel pincel;
    public float ratonX, ratonY;
    public boolean pulsado, recienPulsado, recienSoltado, derechoPulsado;
    /** Clics seguidos en el mismo sitio: 1, 2 (doble) o 3 (triple). */
    public int clics;
    public float rueda;
    public final List<Character> letras = new ArrayList<>();
    public final List<Tecla> teclas = new ArrayList<>();
    private final List<Character> letrasNuevas = new ArrayList<>();
    private final List<Tecla> teclasNuevas = new ArrayList<>();
    private boolean pulsadoNuevo, soltadoNuevo, derechoNuevo;
    private float ruedaNueva;
    private long ultimoClic;
    private float xUltimoClic, yUltimoClic;
    private int clicsSeguidos;
    /** Lo usó alguien este fotograma (el clic, la rueda). */
    public boolean clicUsado, ruedaUsada;
    /** Cuánto lleva quieto el ratón (para los avisos que salen al dejarlo encima). */
    public float quieto;
    private float ultimoX, ultimoY;

    public Entrada(Pincel pincel) {
        this.pincel = pincel;
    }

    /** Al empezar cada fotograma: pasa lo recogido a lo visible. */
    public synchronized void nuevoFotograma(float dt) {
        letras.clear();
        letras.addAll(letrasNuevas);
        letrasNuevas.clear();
        teclas.clear();
        teclas.addAll(teclasNuevas);
        teclasNuevas.clear();
        recienPulsado = pulsadoNuevo;
        recienSoltado = soltadoNuevo;
        derechoPulsado = derechoNuevo;
        pulsadoNuevo = soltadoNuevo = derechoNuevo = false;
        rueda = ruedaNueva;
        ruedaNueva = 0;
        clicUsado = false;
        ruedaUsada = false;
        zona = null;
        bloqueada = false;
        if (Gdx.input != null) {
            ratonX = Gdx.input.getX() / pincel.escala;
            ratonY = Gdx.input.getY() / pincel.escala;
            pulsado = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        }
        repetirTeclas();
        if (Math.abs(ratonX - ultimoX) > 1 || Math.abs(ratonY - ultimoY) > 1) quieto = 0;
        else quieto += dt;
        ultimoX = ratonX;
        ultimoY = ratonY;
    }

    /** Las teclas que se repiten al dejarlas pulsadas (el sistema solo repite las letras). */
    private static final int[] REPETIBLES = {Input.Keys.LEFT, Input.Keys.RIGHT, Input.Keys.UP, Input.Keys.DOWN, Input.Keys.BACKSPACE,
            Input.Keys.FORWARD_DEL, Input.Keys.PAGE_UP, Input.Keys.PAGE_DOWN, Input.Keys.ENTER, Input.Keys.TAB, Input.Keys.Z, Input.Keys.Y};
    private final long[] desde = new long[REPETIBLES.length], ultimaRepeticion = new long[REPETIBLES.length];

    private void repetirTeclas() {
        if (Gdx.input == null) return;
        long ahora = System.currentTimeMillis();
        for (int i = 0; i < REPETIBLES.length; i++) {
            int k = REPETIBLES[i];
            if (!Gdx.input.isKeyPressed(k)) {
                desde[i] = 0;
                continue;
            }
            // Z e Y solo se repiten con Ctrl (deshacer y rehacer seguidos).
            if ((k == Input.Keys.Z || k == Input.Keys.Y) && !ctrl()) continue;
            if (desde[i] == 0) {
                desde[i] = ahora;
                ultimaRepeticion[i] = ahora;
                continue;
            }
            if (ahora - desde[i] > 420 && ahora - ultimaRepeticion[i] >= 33) {
                ultimaRepeticion[i] = ahora;
                teclas.add(new Tecla(k, ctrl(), mayus(), alt()));
            }
        }
    }

    public boolean ctrl() {
        return Gdx.input != null && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT));
    }

    public boolean mayus() {
        return Gdx.input != null && (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));
    }

    public boolean alt() {
        return Gdx.input != null && (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT));
    }

    /** Hay un diálogo abierto encima: lo de debajo no recibe ni el ratón ni las teclas (ver Dialogo). */
    public boolean bloqueada;
    /** La zona visible de lo que se desplaza: fuera de ella, lo de dentro no se toca (null: toda la pantalla). */
    private float[] zona;

    /** Desde aquí, el ratón solo cuenta dentro de este rectángulo (lo que se desplaza y queda tapado no se pulsa). */
    public void limitar(float x, float y, float w, float h) {
        zona = new float[]{x, y, w, h};
    }

    public void sinLimite() {
        zona = null;
    }

    public boolean dentro(float x, float y, float w, float h) {
        if (bloqueada) return false;
        if (zona != null && !(ratonX >= zona[0] && ratonX < zona[0] + zona[2] && ratonY >= zona[1] && ratonY < zona[1] + zona[3])) return false;
        return ratonX >= x && ratonX < x + w && ratonY >= y && ratonY < y + h;
    }

    /** ¿Se ha hecho clic dentro de este rectángulo (y nadie lo ha usado aún)? Lo consume. */
    public boolean clic(float x, float y, float w, float h) {
        if (recienPulsado && !clicUsado && dentro(x, y, w, h)) {
            clicUsado = true;
            return true;
        }
        return false;
    }

    /** ¿Se ha pulsado esta tecla (con o sin Ctrl)? La consume. */
    public boolean tecla(int codigo, boolean conCtrl) {
        if (bloqueada) return false;
        for (Tecla t : teclas) {
            if (t.es(codigo, conCtrl) && !t.alt && !t.usada) {
                t.usada = true;
                return true;
            }
        }
        return false;
    }

    public boolean tecla(int codigo) {
        return tecla(codigo, false);
    }

    @Override
    public synchronized boolean keyDown(int codigo) {
        teclasNuevas.add(new Tecla(codigo, ctrl(), mayus(), alt()));
        return true;
    }

    @Override
    public synchronized boolean keyTyped(char c) {
        // Las letras de control (Ctrl+A da 1, Retroceso da 8…) no se escriben; AltGr+7 da «{» y sí.
        if (c >= ' ' && c != 127) letrasNuevas.add(c);
        return true;
    }

    @Override
    public synchronized boolean touchDown(int x, int y, int puntero, int boton) {
        if (boton == Input.Buttons.RIGHT) {
            derechoNuevo = true;
            return true;
        }
        if (boton != Input.Buttons.LEFT) return false;
        pulsadoNuevo = true;
        long ahora = System.currentTimeMillis();
        float lx = x / pincel.escala, ly = y / pincel.escala;
        if (ahora - ultimoClic < 450 && Math.abs(lx - xUltimoClic) < 6 && Math.abs(ly - yUltimoClic) < 6) clicsSeguidos = Math.min(3, clicsSeguidos + 1);
        else clicsSeguidos = 1;
        clics = clicsSeguidos;
        ultimoClic = ahora;
        xUltimoClic = lx;
        yUltimoClic = ly;
        return true;
    }

    @Override
    public synchronized boolean touchUp(int x, int y, int puntero, int boton) {
        if (boton == Input.Buttons.LEFT) soltadoNuevo = true;
        return true;
    }

    @Override
    public synchronized boolean scrolled(float dx, float dy) {
        ruedaNueva += dy;
        return true;
    }
}
