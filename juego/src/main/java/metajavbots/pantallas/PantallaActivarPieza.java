package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.ejercicios.Comprobador;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.RetoDePieza;
import metajavbots.pantalla.Efectos;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.textos.T;

/**
 * ACTIVAR PIEZA — Reto breve de programación ligado a la mecánica de una pieza
 * comprada en la Tienda, con ajuste de parámetros básicos dentro de límites
 * anti-abuso. Al pasar todos los casos de prueba, la pieza queda desbloqueada,
 * personalizada y equipada en el robot activo.
 */
public final class PantallaActivarPieza implements Pantalla {

    private final Pieza pieza;
    private final Ejercicio reto;
    private final List<RetoDePieza.Parametro> parametros;
    private final Map<String, Double> ajustes = new LinkedHashMap<>();
    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final Ui.Desplazable despl = new Ui.Desplazable();
    private final Efectos fiesta = new Efectos();
    private volatile Comprobador.Informe informe;
    private volatile boolean comprobando;
    private int intentos;
    private boolean pista, solucion, activada;
    private float t, tiempoFiesta;

    public PantallaActivarPieza(Pieza pieza) {
        this.pieza = pieza;
        this.reto = RetoDePieza.para(pieza);
        this.parametros = RetoDePieza.parametros(pieza);
        for (RetoDePieza.Parametro par : parametros) ajustes.put(par.clave, par.base);
    }

    @Override
    public void alEntrar(Juego j) {
        if (editor.doc.largo() > 0) return;
        Jugador jug = j.jugador;
        Map<String, Double> previos = jug.ajustesDePiezas.get(pieza.id);
        if (previos != null) {
            for (RetoDePieza.Parametro par : parametros) {
                Double v = previos.get(par.clave);
                if (v != null) ajustes.put(par.clave, par.acotar(v));
            }
        }
        String guardado = jug.codigoDeEjercicios.get(reto.id);
        editor.ponerTexto(guardado == null || guardado.isBlank() ? reto.codigoInicial(jug.idioma) : guardado);
        editor.tamano = jug.tamanoDeLetra;
        editor.alCompilar = () -> comprobar(j);
        editor.palabras = Palabras.api(jug.idioma);
        editor.documentacion = w -> Referencia.de(w, jug.idioma);
        editor.revisarAlEscribir = codigo -> {
            if (!Compilador.hayCompilador()) return null;
            List<VentanaDeCodigo.Marca> l = new ArrayList<>();
            Comprobador.Informe inf = Comprobador.soloCompilar(reto, codigo, jug.idioma.locale());
            if (!inf.compila()) for (Compilador.Fallo f : inf.errores) l.add(Explicador.marca(f, jug));
            return l;
        };
        fiesta.alEstallar = () -> j.sonidos.tocar("estallido", 0.35f);
    }

    @Override
    public void alSalir(Juego j) {
        j.jugador.codigoDeEjercicios.put(reto.id, editor.doc.texto());
        j.guardar();
    }

    private void comprobar(Juego j) {
        if (comprobando) return;
        comprobando = true;
        intentos++;
        j.jugador.codigoDeEjercicios.put(reto.id, editor.doc.texto());
        j.sonidos.tocar("compilar", 0.5f);
        String codigo = editor.doc.texto();
        Thread h = new Thread(() -> {
            try {
                informe = Comprobador.comprobar(reto, codigo, j.jugador.idioma.locale());
            } finally {
                comprobando = false;
            }
        }, "activar-pieza");
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
        atenderInforme(j, inf);
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, reto.titulo(T.idioma), true);
        float x = 20, y = arriba + 14, w = 520;
        panelIzquierdo(j, inf, x, y, w);

        float ex = x + w + 16, ew = p.ancho - ex - 20;
        String[] barra = {comprobando ? T.t("ejercicio.comprobando") : T.t("ejercicio.comprobar") + "  (Ctrl+S)",
            T.t("taller.formatear"), T.t("ejercicio.reiniciar")};
        Ui.Estilo[] estilos = {Ui.Estilo.PRINCIPAL, Ui.Estilo.PLANO, Ui.Estilo.PLANO};
        boolean[] activos = {!comprobando && Compilador.hayCompilador(), true, true};
        String[] ayudas = {Compilador.hayCompilador() ? null : T.t("taller.sin_jdk"), null, T.t("ejercicio.reiniciar_ayuda")};
        switch (ui.botonera(barra, estilos, activos, ayudas, ex, y, ew, 46)) {
            case 0: comprobar(j); break;
            case 1: editor.doc.formatear(); break;
            case 2:
                editor.doc.cambiarTodo(reto.codigoInicial(jug.idioma));
                editor.marcas.clear();
                informe = null;
                break;
            default: break;
        }
        float ey = y + ui.altoDeBotonera + 12, eh = p.alto - ey - 70;
        editor.dibujar(ui, ex, ey, ew, eh);
        dibujarEstado(j, inf, ex, ew);
        if (tiempoFiesta > 0) {
            tiempoFiesta -= dt;
            if ((int) (tiempoFiesta * 4) != (int) ((tiempoFiesta + dt) * 4)) {
                fiesta.cohete((float) (p.ancho * (0.3 + Math.random() * 0.6)), p.alto, p.alto * 0.5f, 0x5BD685);
            }
            if (tiempoFiesta > 2.6f) fiesta.confeti(ex + ew / 2, ey + 40, 8);
        }
        fiesta.avanzar(dt);
        fiesta.dibujar(p);
    }

    private void atenderInforme(Juego j, Comprobador.Informe inf) {
        Jugador jug = j.jugador;
        if (inf != null && !comprobando && editor.marcas.isEmpty() && !inf.compila()) {
            for (Compilador.Fallo f : inf.errores) editor.marcas.add(Explicador.marca(f, jug));
        }
        if (inf != null && !comprobando && !activada && inf.todoBien()) {
            activada = Progreso.activarPieza(jug, pieza, ajustes);
            tiempoFiesta = 2.8f;
            j.sonidos.tocar("subida", 0.85f);
            j.guardar();
        }
    }

    private void panelIzquierdo(Juego j, Comprobador.Informe inf, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float cy = ui.panel(x, y, w, p.alto - y - 20, T.t("activar.titulo"));
        float d = ui.desplazable(despl, x + 8, cy, w - 16, p.alto - cy - 100);
        float yy = cy - d;
        yy += p.parrafo(reto.enunciado.en(T.idioma), x + 20, yy, w - 50, ui.letra * 0.92f, ui.tema.texto, 1) + 12;
        if (pista && !reto.pista.es.isEmpty()) {
            yy += p.parrafo("★ " + reto.pista.en(T.idioma), x + 20, yy, w - 50, ui.letra * 0.82f, ui.tema.aviso, 1) + 10;
        }
        if (solucion) {
            p.texto(T.t("ejercicio.solucion"), x + 20, yy, ui.letra * 0.82f, true, ui.tema.textoSuave, 1);
            yy += ui.letra * 1.3f;
            for (String l : reto.codigoSolucion(T.idioma).split("\n")) {
                p.texto(l, x + 24, yy, ui.letra * 0.72f, false, ui.tema.cadena, 1);
                yy += ui.letra * 1.05f;
            }
            yy += 8;
        }
        yy = dibujarAjustes(j, x + 20, yy, w - 50);
        p.texto(T.t("ejercicio.casos"), x + 20, yy, ui.letra * 0.88f, true, ui.tema.texto, 1);
        yy += ui.letra * 1.5f;
        List<Ejercicio.Caso> casos = reto.casos(T.idioma);
        for (int i = 0; i < casos.size(); i++) {
            Ejercicio.Caso c = casos.get(i);
            Comprobador.Resultado r = inf != null && i < inf.casos.size() ? inf.casos.get(i) : null;
            int color = r == null ? ui.tema.textoSuave : r.bien ? ui.tema.bien : ui.tema.mal;
            p.texto((r == null ? "·" : r.bien ? "✔" : "✘") + " " + c.llamada, x + 20, yy, ui.letra * 0.76f, false, color, 1);
            yy += ui.letra * 1.15f;
            p.texto("   → " + c.esperado + (r != null && !r.bien ? "   (" + T.t("ejercicio.salio") + " " + r.obtenido + ")" : ""),
                    x + 20, yy, ui.letra * 0.7f, false, ui.tema.textoApagado, 1);
            yy += ui.letra * 1.3f;
        }
        ui.finDesplazable(despl, yy + d - cy);
        String[] abajo = {T.t("ejercicio.pista"), T.t("ejercicio.ver_solucion")};
        Ui.Estilo[] estAbajo = {Ui.Estilo.PLANO, Ui.Estilo.PLANO};
        boolean[] actAbajo = {!reto.pista.es.isEmpty(), intentos >= 3};
        String[] ayuAbajo = {null, T.t("ejercicio.solucion_ayuda")};
        switch (ui.botonera(abajo, estAbajo, actAbajo, ayuAbajo, x + 16, p.alto - 84, w - 32, 46)) {
            case 0: pista = !pista; break;
            case 1: solucion = !solucion; break;
            default: break;
        }
    }

    private float dibujarAjustes(Juego j, float x, float yy, float w) {
        if (parametros.isEmpty()) return yy;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.texto(T.t("activar.parametros"), x, yy, ui.letra * 0.85f, true, ui.tema.acento, 1);
        yy += ui.letra * 1.35f;
        for (RetoDePieza.Parametro par : parametros) {
            double val = ajustes.getOrDefault(par.clave, par.base);
            String etiq = T.t("valor." + par.clave) + ": " + fmt(val) + "  [" + fmt(par.min) + ".." + fmt(par.max) + "]";
            p.texto(etiq, x, yy + 6, ui.letra * 0.76f, false, ui.tema.texto, 1);
            if (ui.boton("-", x + w - 92, yy, 40, 34, Ui.Estilo.PLANO, val > par.min + 1e-6, null)) {
                ajustes.put(par.clave, par.acotar(val - par.paso));
                if (activada) {
                    Progreso.activarPieza(j.jugador, pieza, ajustes);
                    j.guardar();
                }
            }
            if (ui.boton("+", x + w - 46, yy, 40, 34, Ui.Estilo.PLANO, val < par.max - 1e-6, null)) {
                ajustes.put(par.clave, par.acotar(val + par.paso));
                if (activada) {
                    Progreso.activarPieza(j.jugador, pieza, ajustes);
                    j.guardar();
                }
            }
            yy += 42;
        }
        return yy + 8;
    }

    private void dibujarEstado(Juego j, Comprobador.Informe inf, float ex, float ew) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String estado;
        int color;
        if (comprobando) {
            estado = T.t("ejercicio.comprobando");
            color = ui.tema.textoSuave;
        } else if (activada) {
            estado = T.f("activar.ok", pieza.nombre(T.idioma));
            color = ui.tema.bien;
        } else if (inf == null) {
            estado = T.t("activar.empieza");
            color = ui.tema.textoApagado;
        } else if (!inf.compila()) {
            estado = T.f("ejercicio.no_compila", inf.errores.size());
            color = ui.tema.mal;
        } else {
            estado = T.f("ejercicio.casi", inf.bien(), inf.casos.size());
            color = ui.tema.aviso;
        }
        p.texto(estado, ex, p.alto - 54, ui.letra * 0.95f, true, color, 1);
        if (activada && ui.boton(T.t("comun.volver"), ex + ew - 160, p.alto - 60, 160, 44, Ui.Estilo.PRINCIPAL)) {
            j.volver();
        }
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? Long.toString(Math.round(v)) : String.format(Locale.ROOT, "%.2f", v);
    }
}
