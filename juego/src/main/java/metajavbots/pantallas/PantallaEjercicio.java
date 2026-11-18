package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.codigo.Compilador;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Comprobador;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Tema;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Efectos;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.textos.T;

/**
 * UN EJERCICIO — El enunciado, el editor y «Comprobar»: cada caso sale en
 * verde o en rojo con lo que se esperaba y lo que salió. Hay pista, y la
 * solución tras tres intentos. Con cerebro de IA, se le puede preguntar al
 * tutor (que guía sin dar la solución). Al resolverlo: experiencia, quizá
 * una pieza (los retos) y quizá subir de etiqueta, con fuegos artificiales.
 */
public final class PantallaEjercicio implements Pantalla {

    private final Ejercicio e;
    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final PanelDeApi api = new PanelDeApi();
    private volatile Comprobador.Informe informe;
    private volatile boolean comprobando;
    private int intentos;
    private boolean pista, solucion, conApi;
    private final Efectos fiesta = new Efectos();
    private float t, tiempoFiesta;
    private Progreso.Cambio cambio;
    private final Ui.Desplazable despl = new Ui.Desplazable(), desplChat = new Ui.Desplazable();
    // El tutor (con cerebro).
    private final CampoDeTexto pregunta = new CampoDeTexto();
    private final List<Cerebro.Mensaje> chat = new ArrayList<>();
    private CerebroDelJuego.Peticion peticion;
    private boolean conTutor;
    /** El cerebro del tutor (se mira al entrar: preguntarlo en cada fotograma leería las claves del disco). */
    private Cerebro cerebro;

    public PantallaEjercicio(Ejercicio e) {
        this.e = e;
    }

    @Override
    public void alEntrar(Juego j) {
        cerebro = CerebroDelJuego.tutor(j.jugador);
        if (editor.doc.largo() > 0) return;
        Jugador jug = j.jugador;
        editor.ponerTexto(jug.codigoDeEjercicios.getOrDefault(e.id, e.codigoInicial()));
        editor.tamano = jug.tamanoDeLetra;
        editor.alCompilar = () -> comprobar(j);
        editor.palabras = Palabras.api(jug.idioma);
        editor.alPedirAyuda = w -> {
            conApi = true;
            api.buscar(w);
        };
        fiesta.alEstallar = () -> j.sonidos.tocar("estallido", 0.35f);
    }

    @Override
    public void alSalir(Juego j) {
        j.jugador.codigoDeEjercicios.put(e.id, editor.doc.texto());
        j.guardar();
    }

    private void comprobar(Juego j) {
        if (comprobando) return;
        comprobando = true;
        intentos++;
        j.jugador.codigoDeEjercicios.put(e.id, editor.doc.texto());
        j.sonidos.tocar("compilar", 0.5f);
        String codigo = editor.doc.texto();
        Thread h = new Thread(() -> {
            informe = Comprobador.comprobar(e, codigo, j.jugador.idioma.locale());
            comprobando = false;
        }, "comprobar");
        h.setDaemon(true);
        h.start();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comprobador.Informe inf = informe;
        if (inf != null && !comprobando && editor.marcas.isEmpty() && !inf.compila()) {
            for (Compilador.Fallo f : inf.errores) {
                editor.marcas.add(new VentanaDeCodigo.Marca((int) f.linea, (int) f.columna, Explicador.explicar(f, jug.etiqueta, jug.idioma), f.grave));
            }
        }
        if (inf != null && !comprobando && cambio == null && inf.todoBien()) {
            cambio = Progreso.despuesDelEjercicio(jug, e);
            tiempoFiesta = 3;
            j.sonidos.tocar(cambio.subio ? "subida" : "bien", 0.9f);
            if (cambio.subio) j.sacudir(8, 0.8f);
            j.guardar();
        }
        Comun.fondo(j, t);
        Tema tm = Tema.de(e.tema);
        float arriba = Comun.cabecera(j, e.titulo(T.idioma), true);
        float x = 20, y = arriba + 14, w = 520;
        // El enunciado, la pista y los casos.
        float cy = ui.panel(x, y, w, p.alto - y - 20, (tm == null ? "" : tm.id + " · ") + "★".repeat(e.dificultad));
        float d = ui.desplazable(despl, x + 8, cy, w - 16, p.alto - cy - 100);
        float yy = cy - d;
        yy += p.parrafo(e.enunciado.en(T.idioma), x + 20, yy, w - 50, ui.letra * 0.95f, ui.tema.texto, 1) + 14;
        if (e.java > 8) {
            p.texto(T.f("ejercicio.java", e.java), x + 20, yy, ui.letra * 0.75f, false, ui.tema.textoApagado, 1);
            yy += ui.letra * 1.4f;
        }
        if (pista && !e.pista.es.isEmpty()) {
            yy += p.parrafo("★ " + e.pista.en(T.idioma), x + 20, yy, w - 50, ui.letra * 0.85f, ui.tema.aviso, 1) + 10;
        }
        if (solucion) {
            p.texto(T.t("ejercicio.solucion"), x + 20, yy, ui.letra * 0.85f, true, ui.tema.textoSuave, 1);
            yy += ui.letra * 1.4f;
            for (String l : e.codigoSolucion().split("\n")) {
                p.texto(l, x + 24, yy, ui.letra * 0.75f, false, ui.tema.cadena, 1);
                yy += ui.letra * 1.05f;
            }
            yy += 10;
        }
        // Los casos: antes de comprobar, solo cómo se llaman; después, en verde o en rojo.
        p.texto(T.t("ejercicio.casos"), x + 20, yy, ui.letra * 0.9f, true, ui.tema.texto, 1);
        yy += ui.letra * 1.6f;
        for (int i = 0; i < e.casos.size(); i++) {
            Ejercicio.Caso c = e.casos.get(i);
            Comprobador.Resultado r = inf != null && i < inf.casos.size() ? inf.casos.get(i) : null;
            int color = r == null ? ui.tema.textoSuave : r.bien ? ui.tema.bien : ui.tema.mal;
            p.texto((r == null ? "·" : r.bien ? "✔" : "✘") + " " + c.llamada, x + 20, yy, ui.letra * 0.78f, false, color, 1);
            yy += ui.letra * 1.15f;
            p.texto("   → " + c.esperado + (r != null && !r.bien ? "   (" + T.t("ejercicio.salio") + " " + r.obtenido + ")" : ""), x + 20, yy,
                    ui.letra * 0.72f, false, ui.tema.textoApagado, 1);
            yy += ui.letra * 1.35f;
        }
        if (inf != null && !inf.salida.isEmpty() && e.tipo != Ejercicio.Tipo.SALIDA) {
            p.texto(T.t("ejercicio.salida"), x + 20, yy, ui.letra * 0.8f, true, ui.tema.textoSuave, 1);
            yy += ui.letra * 1.3f;
            yy += p.parrafo(inf.salida.length() > 600 ? inf.salida.substring(0, 600) + "…" : inf.salida, x + 24, yy, w - 50, ui.letra * 0.72f, ui.tema.texto,
                    1);
        }
        ui.finDesplazable(despl, yy + d - cy);
        float by = p.alto - 84;
        String[] abajo = {T.t("ejercicio.pista"), T.t("ejercicio.ver_solucion"), T.t("ejercicio.tutor")};
        Ui.Estilo[] estilosAbajo = {Ui.Estilo.PLANO, Ui.Estilo.PLANO, conTutor ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO};
        boolean[] activosAbajo = {!e.pista.es.isEmpty(), intentos >= 3, true};
        String[] ayudasAbajo = {null, T.t("ejercicio.solucion_ayuda"), cerebro == null ? T.t("cerebro.sin") : T.t("ejercicio.tutor_ayuda")};
        switch (ui.botonera(abajo, estilosAbajo, activosAbajo, ayudasAbajo, x + 16, by, w - 32, 46)) {
            case 0: pista = !pista; break;
            case 1: solucion = !solucion; break;
            case 2: conTutor = !conTutor; break;
            default: break;
        }
        // El editor (y la API o el tutor al lado).
        float ex = x + w + 16, ew = p.ancho - ex - 20;
        float lado = conApi || conTutor ? 430 : 0;
        String[] barra = {comprobando ? T.t("ejercicio.comprobando") : T.t("ejercicio.comprobar") + "  (Ctrl+S)", T.t("taller.formatear"),
            T.t("ejercicio.reiniciar"), T.t("taller.api")};
        Ui.Estilo[] estilosArriba = {Ui.Estilo.PRINCIPAL, Ui.Estilo.PLANO, Ui.Estilo.PLANO, conApi ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO};
        boolean[] activosArriba = {!comprobando && Compilador.hayCompilador(), true, true, true};
        String[] ayudasArriba = {Compilador.hayCompilador() ? null : T.t("taller.sin_jdk"), null, T.t("ejercicio.reiniciar_ayuda"), null};
        float anchoSiguiente = cambio != null ? p.ancho(T.t("ejercicio.siguiente") + " →", ui.letra, true) + 40 : 0;
        switch (ui.botonera(barra, estilosArriba, activosArriba, ayudasArriba, ex, y, ew - anchoSiguiente - 10, 46)) {
            case 0: comprobar(j); break;
            case 1: editor.doc.formatear(); break;
            case 2:
                editor.doc.cambiarTodo(e.codigoInicial());
                editor.marcas.clear();
                break;
            case 3: conApi = !conApi; break;
            default: break;
        }
        float altoBarra = ui.altoDeBotonera;
        if (cambio != null && ui.boton(T.t("ejercicio.siguiente") + " →", ex + ew - anchoSiguiente, y, anchoSiguiente, 46, Ui.Estilo.PRINCIPAL)) {
            Ejercicio sig = siguiente(jug);
            if (sig != null) {
                j.volver();
                j.ir(new PantallaEjercicio(sig));
            }
        }
        float ey = y + altoBarra + 12, eh = p.alto - ey - 70;
        if (!editor.doc.texto().equals(jug.codigoDeEjercicios.getOrDefault(e.id, "")) && inf != null) editor.marcas.removeIf(m -> false);
        editor.dibujar(ui, ex, ey, ew - lado - (lado > 0 ? 12 : 0), eh);
        if (conTutor) tutor(j, ex + ew - lado, ey, lado, eh);
        else if (conApi) api.dibujar(ui, jug.idioma, editor, ex + ew - lado, ey, lado, eh);
        // Cómo ha ido.
        String estado;
        int color;
        if (comprobando) {
            estado = T.t("ejercicio.comprobando");
            color = ui.tema.textoSuave;
        } else if (inf == null) {
            estado = T.t("ejercicio.empieza");
            color = ui.tema.textoApagado;
        } else if (!inf.compila()) {
            estado = T.f("ejercicio.no_compila", inf.errores.size());
            color = ui.tema.mal;
        } else if (inf.todoBien()) {
            estado = T.f("ejercicio.bien", cambio == null ? 0 : cambio.experiencia);
            color = ui.tema.bien;
        } else {
            estado = T.f("ejercicio.casi", inf.bien(), inf.casos.size());
            color = ui.tema.aviso;
        }
        p.texto(estado, ex, p.alto - 54, ui.letra, true, color, 1);
        if (cambio != null && cambio.subio) p.textoDerecha(T.f("final.subes", cambio.despues.nombre), ex + ew, p.alto - 54, ui.letra, true, 0xFFC857, 1);
        for (metajavbots.piezas.Pieza pz : cambio == null ? List.<metajavbots.piezas.Pieza>of() : cambio.botin) {
            p.textoDerecha(T.f("final.botin", pz.nombre(T.idioma)), ex + ew, p.alto - 84, ui.letra * 0.9f, false, ui.tema.bien, 1);
        }
        if (tiempoFiesta > 0) {
            tiempoFiesta -= dt;
            if ((int) (tiempoFiesta * 4) != (int) ((tiempoFiesta + dt) * 4)) {
                fiesta.cohete((float) (p.ancho * (0.3 + Math.random() * 0.6)), p.alto, p.alto * 0.5f, 0x5BD685);
            }
            if (tiempoFiesta > 2.8f) fiesta.confeti(ex + ew / 2, ey + 40, 8);
        }
        fiesta.avanzar(dt);
        fiesta.dibujar(p);
    }

    private Ejercicio siguiente(Jugador jug) {
        List<Ejercicio> l = Banco.delJuego().deTema(e.tema);
        int i = l.indexOf(e);
        for (int k = i + 1; k < l.size(); k++) if (!jug.ejerciciosResueltos.contains(l.get(k).id)) return l.get(k);
        return i + 1 < l.size() ? l.get(i + 1) : null;
    }

    /** El tutor de IA: una conversación al lado del editor. */
    private void tutor(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        float cy = ui.panel(x, y, w, h, T.t("ejercicio.tutor"));
        Cerebro c = cerebro;
        if (c == null) {
            p.parrafo(T.t("cerebro.sin"), x + 16, cy, w - 32, ui.letra * 0.85f, ui.tema.textoSuave, 1);
            return;
        }
        float zona = h - (cy - y) - 70;
        float d = ui.desplazable(desplChat, x + 8, cy, w - 16, zona);
        float yy = cy - d;
        for (Cerebro.Mensaje m : chat) {
            boolean mio = m.rol.equals("user");
            String texto = mio ? m.texto.substring(m.texto.lastIndexOf("\n") + 1) : m.texto;
            yy += p.parrafo((mio ? "» " : "") + texto, x + (mio ? 40 : 16), yy, w - 70, ui.letra * 0.82f, mio ? ui.tema.acento : ui.tema.texto, 1) + 10;
        }
        if (peticion != null && !peticion.lista) {
            yy += p.parrafo(T.t("cerebro.pensando"), x + 16, yy, w - 32, ui.letra * 0.8f, ui.tema.textoApagado, 1);
        } else if (peticion != null) {
            chat.add(new Cerebro.Mensaje("assistant", peticion.respuesta != null ? peticion.respuesta : T.f("cerebro.error", peticion.error)));
            peticion = null;
            desplChat.desplazamiento = 1e6f;
        }
        ui.finDesplazable(desplChat, yy + d - cy + 10);
        boolean intro = pregunta.dibujar(ui, x + 12, y + h - 58, w - 24, 46, T.t("ejercicio.pregunta_al_tutor"));
        if (intro && !pregunta.texto().isBlank() && peticion == null) {
            String contexto = "Exercise: " + e.titulo.en + " — " + e.enunciado.en + "\nMy code:\n" + editor.doc.texto()
                    + (informe != null ? "\nLast check: " + informe.bien() + "/" + informe.casos.size() + " cases OK" : "") + "\nMy question:\n"
                            + pregunta.texto();
            chat.add(new Cerebro.Mensaje("user", contexto));
            Tema tm = Tema.de(e.tema);
            peticion = CerebroDelJuego.Peticion.lanzar(c, CerebroDelJuego.sistemaTutor(jug.idioma, jug.etiqueta.nombre, tm == null ? "" : tm.nombre.en),
                    new ArrayList<>(chat), 140);
            pregunta.ponerTexto("");
        }
    }
}
