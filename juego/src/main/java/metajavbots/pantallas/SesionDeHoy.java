package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.SesionDeUnaHora;
import metajavbots.ejercicios.Tema;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * SESIÓN DE HOY — La hora de entrenamiento que propone el juego
 * (ejercicios/SesionDeUnaHora), en tres paradas con su reloj: repaso de lo
 * flojo, tema nuevo y pelea. Cada parada se marca hecha al volver; el
 * entrenador lo cuenta. Se recalcula al entrar (lo resuelto cambia el plan).
 */
public final class SesionDeHoy implements Pantalla {

    private SesionDeUnaHora.Propuesta propuesta;
    private float t;
    private boolean peleado;

    @Override
    public void alEntrar(Juego j) {
        propuesta = SesionDeUnaHora.proponer(j.jugador, Banco.delJuego());
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("sesion.titulo"), true);
        Entrenador en = Entrenador.de(jug.entrenador);
        CabezaDeEntrenador.dibujar(p, en, 70, arriba + 60, 34, (t % 4) < 2, t);
        p.parrafo(T.t("sesion.intro"), 120, arriba + 30, p.ancho - 160, ui.letra, ui.tema.texto, 1);
        float x = 40, y = arriba + 120, w = (p.ancho - 80 - 40) / 3, h = p.alto - y - 40;
        // 1. Repaso de lo flojo.
        float cy = parada(j, 1, x, y, w, h, "sesion.repaso", "20 min");
        if (propuesta.temaFlojo() == null || propuesta.repaso().isEmpty()) {
            p.parrafo(T.t("sesion.sin_flojo"), x + 20, cy, w - 40, ui.letra * 0.9f, ui.tema.textoSuave, 1);
        } else {
            cy += p.parrafo(T.f("sesion.flojo", nombre(propuesta.temaFlojo())), x + 20, cy, w - 40, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 12;
            for (Ejercicio e : propuesta.repaso()) cy = ejercicio(j, e, x + 20, cy, w - 40);
        }
        // 2. Tema nuevo: su lección y su primer ejercicio.
        x += w + 20;
        cy = parada(j, 2, x, y, w, h, "sesion.nuevo", "25 min");
        if (propuesta.temaNuevo() == null) {
            p.parrafo(T.t("sesion.sin_nuevo"), x + 20, cy, w - 40, ui.letra * 0.9f, ui.tema.textoSuave, 1);
        } else {
            cy += p.parrafo(nombre(propuesta.temaNuevo()), x + 20, cy, w - 40, ui.letra, true, ui.tema.texto, 1) + 12;
            if (propuesta.leccion() != null) {
                boolean hecha = jug.tutorialesHechos.contains(propuesta.leccion().id);
                if (ui.boton((hecha ? "✔ " : "") + T.f("sesion.leccion", propuesta.leccion().titulo.en(T.idioma)), x + 20, cy, w - 40, 46,
                        hecha ? Ui.Estilo.PLANO : Ui.Estilo.NORMAL)) j.ir(new PantallaLeccion(propuesta.leccion()));
                cy += 56;
            }
            if (propuesta.ejercicioNuevo() != null) ejercicio(j, propuesta.ejercicioNuevo(), x + 20, cy, w - 40);
        }
        // 3. La pelea, para usarlo.
        x += w + 20;
        cy = parada(j, 3, x, y, w, h, "sesion.pelea", "15 min");
        cy += p.parrafo(T.t("sesion.pelea_ayuda"), x + 20, cy, w - 40, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 12;
        if (ui.boton((peleado ? "✔ " : "") + T.t("menu.pelear_ya"), x + 20, cy, w - 40, 56, peleado ? Ui.Estilo.PLANO : Ui.Estilo.PRINCIPAL)) {
            peleado = true;
            j.ir(PrepararPelea.adaptada(j));
        }
    }

    private static String nombre(String tema) {
        Tema t = Tema.de(tema);
        return t == null ? tema : t.id + " · " + t.nombre(T.idioma);
    }

    /** El recuadro de una parada: número, título y su tiempo. Devuelve dónde empieza lo de dentro. */
    private float parada(Juego j, int n, float x, float y, float w, float h, String clave, String tiempo) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.caja(x, y, w, h, 14, ui.tema.panel, 1);
        p.bordeRedondeado(x, y, w, h, 14, ui.tema.borde, 1, 1);
        p.circulo(x + 34, y + 34, 20, ui.tema.acento, 1);
        p.textoCentrado(Integer.toString(n), x + 34, y + 34 - p.altoDeLinea(ui.letra) / 2, ui.letra, true, 0x10161C, 1);
        float tiempoW = p.ancho(tiempo, ui.letra * 0.85f, false);
        String titulo = T.t(clave);
        p.texto(titulo, x + 64, y + 22, ui.cabe(titulo, w - 64 - tiempoW - 26, ui.letra * 1.1f, true), true, ui.tema.texto, 1);
        p.textoDerecha(tiempo, x + w - 16, y + 24, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
        return y + 72;
    }

    private float ejercicio(Juego j, Ejercicio e, float x, float y, float w) {
        boolean hecho = j.jugador.ejerciciosResueltos.contains(e.id);
        if (j.ui.boton((hecho ? "✔ " : "") + e.titulo(T.idioma), x, y, w, 46, hecho ? Ui.Estilo.PLANO : Ui.Estilo.NORMAL)) j.ir(new PantallaEjercicio(e));
        return y + 56;
    }
}
