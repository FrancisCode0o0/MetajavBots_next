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

    public String texto() {
        return doc.texto();
    }

    public void ponerTexto(String s) {
        doc.ponerTexto(s == null ? "" : s.replace("\n", " "));
        doc.finalDelTexto(false);
    }

    /** Dibuja el campo y atiende las teclas si está enfocado. Devuelve true si se pulsó Intro. */
    public boolean dibujar(Ui ui, float x, float y, float w, float h, String pista) {
        Pincel p = ui.p;
        Entrada e = ui.e;
        if (e.clic(x, y, w, h)) enfocado = true;
        else if (e.recienPulsado && !e.dentro(x, y, w, h)) enfocado = false;
        boolean intro = false;
        if (enfocado && !e.bloqueada) {
            for (Entrada.Tecla k : e.teclas) {
                if (k.usada || k.alt && k.ctrl) continue;
                k.usada = true;
                switch (k.codigo) {
                    case Keys.BACKSPACE: if (k.ctrl) doc.borrarPalabraAtras(); else doc.borrarAtras(); break;
                    case Keys.FORWARD_DEL: doc.borrarDelante(); break;
                    case Keys.LEFT: doc.izquierda(k.mayus, k.ctrl); break;
                    case Keys.RIGHT: doc.derecha(k.mayus, k.ctrl); break;
                    case Keys.HOME: doc.principio(k.mayus); break;
                    case Keys.END: doc.finalDelTexto(k.mayus); break;
                    case Keys.ENTER: case Keys.NUMPAD_ENTER: intro = true; break;
                    case Keys.ESCAPE: enfocado = false; break;   // el primer Esc suelta el campo; el segundo vuelve atrás
                    default:
                        if (k.ctrl && k.codigo == Keys.A) doc.seleccionarTodo();
                        else if (k.ctrl && k.codigo == Keys.V) {
                            try {
                                String s = Gdx.app.getClipboard().getContents();
                                if (s != null) doc.insertar(s.replace("\n", " ").strip());
                            } catch (RuntimeException ex) {
                                // sin portapapeles
                            }
                        } else if (k.ctrl && k.codigo == Keys.Z) doc.deshacer();
                        else k.usada = false;
                        break;
                }
            }
            for (char c : e.letras) if (doc.largo() < maximo) doc.insertar(String.valueOf(c));
            e.letras.clear();
            parpadeo += Gdx.graphics == null ? 0.016f : Gdx.graphics.getDeltaTime();
        }
        Tema t = ui.tema;
        p.caja(x, y, w, h, 7, t.fondo, 1);
        p.bordeRedondeado(x, y, w, h, 7, enfocado ? t.acento : t.borde, 1, enfocado ? 2 : 1.2f);
        float tam = Math.min(ui.letra, h * 0.55f);
        String s = oculto ? "•".repeat(doc.largo()) : doc.texto();
        float ty = y + (h - tam * 1.2f) / 2;
        p.recortar(x + 4, y, w - 8, h);
        if (s.isEmpty() && !enfocado && pista != null) p.texto(pista, x + 12, ty, tam, false, t.textoApagado, 1);
        else {
            if (doc.haySeleccion()) {
                String antes = s.substring(0, Math.min(s.length(), doc.inicioDeSeleccion()));
                String sel = s.substring(Math.min(s.length(), doc.inicioDeSeleccion()), Math.min(s.length(), doc.finDeSeleccion()));
                p.rect(x + 12 + p.ancho(antes, tam, false), ty, p.ancho(sel, tam, false), tam * 1.2f, t.seleccion, 1);
            }
            p.texto(s, x + 12, ty, tam, false, t.texto, 1);
        }
        if (enfocado && parpadeo % 1 < 0.6f) {
            float cx = x + 12 + p.ancho(s.substring(0, Math.min(s.length(), doc.cursor())), tam, false);
            p.rect(cx, ty, 2, tam * 1.2f, t.acento, 1);
        }
        p.sinRecorte();
        if (enfocado) ui.reservarEscape();
        return intro;
    }
}
