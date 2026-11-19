package metajavbots.pantalla;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

import metajavbots.codigo.Documento;

/** CAMPO DE TEXTO — Una línea para escribir (un nombre, una búsqueda, una clave). Usa el Documento del editor. */
public final class CampoDeTexto {

    public final Documento doc = new Documento();
    public boolean enfocado;
    public boolean oculto;
    public int maximo = 200;
    private float parpadeo;
    private float desplazamientoX;
    private boolean arrastrando, recienPuesto;

    public String texto() {
        return doc.texto();
    }

    public void ponerTexto(String s) {
        doc.ponerTexto(s == null ? "" : s.replace("\n", " "));
        doc.finalDelTexto(false);
        recienPuesto = true;
    }

    /** Dibuja el campo y atiende las teclas si está enfocado. Devuelve true si se pulsó Intro. */
    public boolean dibujar(Ui ui, float x, float y, float w, float h, String pista) {
        Pincel p = ui.p;
        Entrada e = ui.e;
        boolean conservar = recienPuesto;
        recienPuesto = false;
        float tam = Math.min(ui.letra, h * 0.55f);
        String s = oculto ? "•".repeat(doc.largo()) : doc.texto();
        float tx = x + 12 - desplazamientoX;
        if (e.clic(x, y, w, h)) {
            enfocado = true;
            ui.registrarFoco(this, () -> enfocado = false);
            parpadeo = 0;
            int idx = Math.min(doc.largo(), indiceEn(p, e.ratonX - tx, tam, s));
            if (e.clics == 2) doc.seleccionarPalabra(idx);
            else if (e.clics >= 3) doc.seleccionarTodo();
            else doc.moverA(idx, e.mayus());
            arrastrando = e.clics == 1;
        } else if (!conservar && !e.bloqueada && e.recienPulsado && !e.dentro(x, y, w, h) && enfocado) {
            enfocado = false;
            arrastrando = false;
            ui.desenfocar(this);
        }
        if (!e.pulsado) arrastrando = false;
        if (arrastrando && !e.bloqueada) {
            doc.moverA(Math.min(doc.largo(), indiceEn(p, e.ratonX - tx, tam, s)), true);
            parpadeo = 0;
        }
        if (enfocado) ui.registrarFoco(this, () -> enfocado = false);
        boolean intro = enfocado && !e.bloqueada && teclas(e);
        if (!enfocado) ui.desenfocar(this);
        if (enfocado) {
            s = oculto ? "•".repeat(doc.largo()) : doc.texto();
            ajustarDesplazamiento(p, s, tam, w);
        } else {
            desplazamientoX = 0;
        }
        dibujarCampo(p, ui.tema, x, y, w, h, tam, s, pista);
        if (enfocado) ui.reservarEscape();
        return intro;
    }

    private boolean teclas(Entrada e) {
        boolean intro = false;
        for (Entrada.Tecla k : e.teclas) {
            if (k.usada || k.alt && k.ctrl) continue;
            k.usada = true;
            parpadeo = 0;
            switch (k.codigo) {
                case Keys.BACKSPACE: if (k.ctrl) doc.borrarPalabraAtras(); else doc.borrarAtras(); break;
                case Keys.FORWARD_DEL: if (k.ctrl) doc.borrarPalabraDelante(); else doc.borrarDelante(); break;
                case Keys.LEFT: doc.izquierda(k.mayus, k.ctrl); break;
                case Keys.RIGHT: doc.derecha(k.mayus, k.ctrl); break;
                case Keys.HOME: doc.principio(k.mayus); break;
                case Keys.END: doc.finalDelTexto(k.mayus); break;
                case Keys.ENTER: case Keys.NUMPAD_ENTER: intro = true; break;
                case Keys.ESCAPE: enfocado = false; break;   // el primer Esc suelta el campo; el segundo vuelve atrás
                default:
                    if (!atajo(k)) k.usada = false;
                    break;
            }
        }
        for (char c : e.letras) {
            if (doc.largo() < maximo) {
                doc.insertar(String.valueOf(c));
                parpadeo = 0;
            }
        }
        e.letras.clear();
        parpadeo += Gdx.graphics == null ? 0.016f : Gdx.graphics.getDeltaTime();
        return intro;
    }

    private boolean atajo(Entrada.Tecla k) {
        if (!k.ctrl) return false;
        switch (k.codigo) {
            case Keys.A: doc.seleccionarTodo(); return true;
            case Keys.C: if (!oculto) portapapeles(doc.haySeleccion() ? doc.seleccion() : doc.texto()); return true;
            case Keys.X:
                if (!oculto) {
                    portapapeles(doc.haySeleccion() ? doc.seleccion() : doc.texto());
                    if (doc.haySeleccion()) doc.borrarAtras();
                    else doc.cambiarTodo("");
                }
                return true;
            case Keys.V:
                try {
                    String s = Gdx.app == null ? null : Gdx.app.getClipboard().getContents();
                    if (s != null) doc.insertar(s.replace("\n", " ").strip());
                } catch (RuntimeException ex) {
                    // sin portapapeles
                }
                return true;
            case Keys.Z: if (k.mayus) doc.rehacer(); else doc.deshacer(); return true;
            case Keys.Y: doc.rehacer(); return true;
            default: return false;
        }
    }

    private static void portapapeles(String s) {
        try {
            if (Gdx.app != null && s != null) Gdx.app.getClipboard().setContents(s);
        } catch (RuntimeException ex) {
            // sin portapapeles
        }
    }

    private static int indiceEn(Pincel p, float rx, float tam, String s) {
        if (rx <= 0 || s.isEmpty()) return 0;
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (p.ancho(s.substring(0, mid), tam, false) < rx) lo = mid + 1;
            else hi = mid;
        }
        if (lo > 0) {
            float izq = p.ancho(s.substring(0, lo - 1), tam, false);
            float der = p.ancho(s.substring(0, lo), tam, false);
            if (rx - izq < der - rx) return lo - 1;
        }
        return lo;
    }

    private void ajustarDesplazamiento(Pincel p, String s, float tam, float w) {
        float util = Math.max(20, w - 24);
        float total = p.ancho(s, tam, false);
        if (total <= util) {
            desplazamientoX = 0;
            return;
        }
        float cx = p.ancho(s.substring(0, Math.min(s.length(), doc.cursor())), tam, false);
        if (cx - desplazamientoX < 0) desplazamientoX = cx;
        if (cx - desplazamientoX > util - 4) desplazamientoX = cx - (util - 4);
        desplazamientoX = Math.max(0, Math.min(total - util + 4, desplazamientoX));
    }

    private void dibujarCampo(Pincel p, Tema t, float x, float y, float w, float h, float tam, String s, String pista) {
        p.caja(x, y, w, h, 7, t.fondo, 1);
        p.bordeRedondeado(x, y, w, h, 7, enfocado ? t.acento : t.borde, 1, enfocado ? 2 : 1.2f);
        float ty = y + (h - tam * 1.2f) / 2;
        float tx = x + 12 - desplazamientoX;
        p.recortar(x + 4, y, w - 8, h);
        if (s.isEmpty() && !enfocado && pista != null) p.texto(pista, x + 12, ty, tam, false, t.textoApagado, 1);
        else {
            if (doc.haySeleccion()) {
                String antes = s.substring(0, Math.min(s.length(), doc.inicioDeSeleccion()));
                String sel = s.substring(Math.min(s.length(), doc.inicioDeSeleccion()), Math.min(s.length(), doc.finDeSeleccion()));
                p.rect(tx + p.ancho(antes, tam, false), ty, p.ancho(sel, tam, false), tam * 1.2f, t.seleccion, 1);
            }
            p.texto(s, tx, ty, tam, false, t.texto, 1);
        }
        if (enfocado && parpadeo % 1 < 0.6f) {
            float cx = tx + p.ancho(s.substring(0, Math.min(s.length(), doc.cursor())), tam, false);
            p.rect(cx, ty, 2, tam * 1.2f, t.acento, 1);
        }
        p.sinRecorte();
    }
}
