package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.textos.T;

/**
 * TIENDA — Catálogo de piezas y complementos comprables con las monedas del
 * jugador, agrupado por rango (Noob, Junior y Senior). Las piezas de rangos
 * superiores aparecen bloqueadas hasta alcanzar ese rango; las compradas se
 * guardan en {@link Jugador#piezasPendientes} (partida.txt) y requieren
 * superar su breve reto de código para quedar activas y equipables en el Taller.
 */
public final class Tienda implements Pantalla {

    private static final Etiqueta.Categoria[] RANGOS = {
        Etiqueta.Categoria.NOOB,
        Etiqueta.Categoria.JUNIOR,
        Etiqueta.Categoria.SENIOR,
        Etiqueta.Categoria.MASTER
    };

    private static final Pieza.Ranura[] RANURAS = {
        null,
        Pieza.Ranura.BRAZO,
        Pieza.Ranura.CABEZA,
        Pieza.Ranura.PIERNAS,
        Pieza.Ranura.NUCLEO,
        Pieza.Ranura.NATURALEZA
    };

    private final Ui.Desplazable scroll = new Ui.Desplazable();
    private final String[] pestanasRangos = new String[RANGOS.length];
    private final String[] textosRanuras = new String[RANURAS.length];
    private final Ui.Estilo[] estilosRanuras = new Ui.Estilo[RANURAS.length];
    private final java.util.Map<String, String> cifrasCache = new java.util.HashMap<>();
    private metajavbots.textos.Idioma idiomaCache;
    private Etiqueta.Categoria rangoCache;
    private Pieza.Ranura ranuraCache;
    private List<Pieza> listaCache;
    private int rangoElegido;
    private int ranuraElegida;
    private String aviso = "";
    private float t;

    @Override
    public void alEntrar(Juego j) {
        if (j.jugador != null) {
            int ord = j.jugador.etiqueta.categoria().ordinal();
            rangoElegido = Math.min(RANGOS.length - 1, Math.max(0, ord));
        }
        scroll.alPrincipio();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("tienda.titulo"), true);
        dibujarMonedas(j, jug);

        float x = 32, w = p.ancho - 64;
        float y = arriba + 14;
        y = barraDeRangos(j, jug, x, y, w);
        y = barraDeRanuras(ui, x, y, w);

        Etiqueta.Categoria rango = RANGOS[rangoElegido];
        boolean desbloqueado = Progreso.rangoDesbloqueadoEnTienda(jug, rango);
        if (!desbloqueado) y = avisoRangoBloqueado(p, ui, rango, x, y, w);

        String pie = aviso.isEmpty() ? T.t("tienda.ayuda") : aviso;
        float tamPie = ui.cabe(pie, w, ui.letra * 0.8f, false);
        float altoPie = p.altoDeParrafo(pie, w, tamPie) + 12;
        float altoLista = Math.max(120, p.alto - altoPie - 12 - y);

        dibujarCatalogo(j, jug, rango, desbloqueado, x, y, w, altoLista);
        p.parrafo(pie, x, p.alto - altoPie - 6, w, tamPie, aviso.isEmpty() ? ui.tema.textoApagado : ui.tema.bien, 1);
    }

    private float barraDeRangos(Juego j, Jugador jug, float x, float y, float w) {
        Ui ui = j.ui;
        for (int i = 0; i < RANGOS.length; i++) {
            boolean libre = Progreso.rangoDesbloqueadoEnTienda(jug, RANGOS[i]);
            pestanasRangos[i] = libre ? nombreRango(RANGOS[i]) : "[" + nombreRango(RANGOS[i]) + "]";
        }
        int antes = rangoElegido;
        rangoElegido = ui.pestanas(pestanasRangos, rangoElegido, x, y, Math.min(w, 820), 48);
        if (rangoElegido != antes) scroll.alPrincipio();
        return y + 58;
    }

    private float barraDeRanuras(Ui ui, float x, float y, float w) {
        textosRanuras[0] = T.t("tienda.todas");
        textosRanuras[1] = T.t("tienda.ranura.brazo");
        textosRanuras[2] = T.t("tienda.ranura.cabeza");
        textosRanuras[3] = T.t("tienda.ranura.piernas");
        textosRanuras[4] = T.t("tienda.ranura.nucleo");
        textosRanuras[5] = T.t("tienda.ranura.naturaleza");
        for (int i = 0; i < textosRanuras.length; i++) {
            estilosRanuras[i] = i == ranuraElegida ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO;
        }
        int pulsado = ui.botonera(textosRanuras, estilosRanuras, null, null, x, y, w, 40);
        if (pulsado >= 0 && pulsado != ranuraElegida) {
            ranuraElegida = pulsado;
            scroll.alPrincipio();
        }
        return y + ui.altoDeBotonera + 12;
    }

    private float avisoRangoBloqueado(Pincel p, Ui ui, Etiqueta.Categoria rango, float x, float y, float w) {
        String msg = rango == Etiqueta.Categoria.MASTER
                ? T.t("tienda.rango_especial_bloqueado")
                : T.f("tienda.rango_bloqueado", nombreRango(rango));
        float tam = ui.cabe(msg, w - 56, ui.letra * 0.88f, true);
        float h = p.altoDeParrafo(msg, w - 56, tam) + 20;
        p.caja(x, y, w, h, 10, Pincel.oscurecer(ui.tema.aviso, 0.72f), 1);
        p.bordeRedondeado(x, y, w, h, 10, ui.tema.aviso, 1, 1.5f);
        ui.candado(x + 24, y + h / 2f, 18, ui.tema.aviso, 1);
        p.parrafo(msg, x + 42, y + 10, w - 56, tam, ui.tema.texto, 1);
        return y + h + 12;
    }

    private void dibujarCatalogo(Juego j, Jugador jug, Etiqueta.Categoria rango, boolean desbloqueado,
                                 float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Pieza.Ranura ranura = RANURAS[ranuraElegida];
        if (listaCache == null || rangoCache != rango || ranuraCache != ranura) {
            rangoCache = rango;
            ranuraCache = ranura;
            listaCache = piezasFiltradas(rango, ranura);
        }
        List<Pieza> lista = listaCache;
        float desp = ui.desplazable(scroll, x, y, w, h);
        if (lista.isEmpty()) {
            p.textoCentrado(T.t("tienda.vacio"), x + w / 2, y + 40, ui.letra, false, ui.tema.textoApagado, 1);
            ui.finDesplazable(scroll, 100);
            return;
        }
        int cols = w >= 1000 ? 2 : 1;
        float gap = 16;
        float cw = (w - 16 - (cols - 1) * gap) / cols;
        float ch = Math.max(134, ui.letra * 5.6f);
        for (int i = 0; i < lista.size(); i++) {
            float cx = x + (i % cols) * (cw + gap);
            float cy = y + (i / cols) * (ch + gap) - desp;
            if (cy + ch >= y - 20 && cy <= y + h + 20) {
                dibujarTarjeta(j, jug, lista.get(i), desbloqueado, cx, cy, cw, ch);
            }
        }
        int filas = (lista.size() + cols - 1) / cols;
        ui.finDesplazable(scroll, filas * (ch + gap));
    }

    private void dibujarTarjeta(Juego j, Jugador jug, Pieza pz, boolean desbloqueado,
                                float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean yaLaTiene = Progreso.piezaDisponible(jug, pz);
        boolean pendiente = Progreso.piezaPendiente(jug, pz);
        boolean alcanzaSaldo = jug.monedas + 1e-9 >= pz.precio;
        int borde = yaLaTiene ? ui.tema.bien : pendiente ? ui.tema.aviso : desbloqueado ? ui.tema.borde : ui.tema.textoApagado;
        p.caja(x, y, w, h, 12, ui.tema.panel, desbloqueado || yaLaTiene || pendiente ? 1f : 0.65f);
        p.bordeRedondeado(x, y, w, h, 12, borde, 1, yaLaTiene || pendiente ? 2f : 1f);

        float bw = Math.min(220, Math.max(144, w * 0.32f));
        float textoW = w - bw - 36;
        String nombre = pz.nombre(T.idioma);
        float tNom = ui.cabe(nombre, textoW, ui.letra * 1.02f, true);
        p.textoConBordeNegro(ui.recortarTexto(nombre, textoW, tNom, true), x + 16, y + 12, tNom, true,
                desbloqueado || yaLaTiene || pendiente ? ui.tema.texto : ui.tema.textoSuave, 1);

        float yMeta = y + 14 + p.altoDeLinea(tNom) + 2;
        p.circulo(x + 22, yMeta + ui.letra * 0.42f, 6, pz.naturaleza.color(), 1);
        String meta = pz.naturaleza.nombre(T.idioma) + " · " + nombreRanura(pz.ranura) + " · " + nombreRango(pz.rangoTienda)
                + (pendiente ? " · " + T.t("tienda.pendiente") : "");
        float tMeta = ui.cabe(meta, textoW - 20, ui.letra * 0.76f, false);
        p.texto(ui.recortarTexto(meta, textoW - 20, tMeta, false), x + 34, yMeta, tMeta, false,
                pendiente ? ui.tema.aviso : ui.tema.textoSuave, 1);

        float yCifras = yMeta + p.altoDeLinea(tMeta) + 4;
        if (idiomaCache != T.idioma) {
            idiomaCache = T.idioma;
            cifrasCache.clear();
        }
        String cifras = cifrasCache.computeIfAbsent(pz.id, id -> Taller.cifras(pz));
        float tCif = ui.letra * 0.72f;
        float dispCif = y + h - yCifras - 10;
        while (tCif > 10 && p.altoDeParrafo(cifras, textoW, tCif) > dispCif) tCif -= 1;
        p.parrafo(cifras, x + 16, yCifras, textoW, tCif, ui.tema.textoApagado, 1);

        float bx = x + w - bw - 14;
        String precioTxt = "◆ " + Jugador.formatearMonedas(pz.precio);
        float tPrecio = ui.cabe(precioTxt, bw, ui.letra * 0.95f, true);
        p.textoCentradoConBordeNegro(precioTxt, bx + bw / 2, y + 16, tPrecio, true,
                desbloqueado && alcanzaSaldo ? 0xFFC857 : ui.tema.textoSuave, 1);

        float bh = Math.min(48, h * 0.36f);
        float by = y + h - bh - 14;
        if (yaLaTiene) {
            ui.boton("✔ " + T.t("tienda.comprada"), bx, by, bw, bh, Ui.Estilo.PLANO, false, null);
        } else if (pendiente) {
            if (ui.boton("⚡ " + T.t("tienda.activar"), bx, by, bw, bh, Ui.Estilo.PRINCIPAL, true, T.t("tienda.activar_ayuda"))) {
                j.ir(new PantallaActivarPieza(pz));
            }
        } else if (!desbloqueado) {
            ui.boton(T.t("tienda.bloqueada"), bx, by, bw, bh, Ui.Estilo.PLANO, false,
                    T.f("tienda.necesita_rango", nombreRango(pz.rangoTienda)));
            ui.candado(bx + 18, by + bh / 2f, 14, ui.tema.textoApagado, 1);
        } else if (!alcanzaSaldo) {
            String falta = Jugador.formatearMonedas(pz.precio - jug.monedas);
            ui.boton(T.t("tienda.comprar"), bx, by, bw, bh, Ui.Estilo.NORMAL, false, T.f("tienda.sin_saldo", falta));
        } else if (ui.boton(T.t("tienda.comprar"), bx, by, bw, bh, Ui.Estilo.DESTACADO, true, null)) {
            if (Progreso.comprarPieza(jug, pz) == Progreso.CompraResultado.COMPRADA) {
                j.guardar();
                j.sonidos.tocar("bien", 0.6f);
                aviso = T.f("tienda.ok", pz.nombre(T.idioma), Jugador.formatearMonedas(pz.precio));
            }
        }
    }

    private void dibujarMonedas(Juego j, Jugador jug) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String texto = "◆ " + T.f("menu.monedas", Jugador.formatearMonedas(jug.monedas));
        float anchoMax = Math.max(120, p.ancho - 460 - 420);
        float tam = ui.cabe(texto, anchoMax - 32, Math.min(ui.letra, 22f), true);
        float mw = Math.min(anchoMax, Math.max(130, p.ancho(texto, tam, true) + 32));
        float mx = p.ancho - 460 - mw - 16, my = 14, mh = 50;
        int dorado = 0xFFC857;
        p.caja(mx, my, mw, mh, 10, Pincel.oscurecer(dorado, 0.65f), 1);
        p.bordeRedondeado(mx, my, mw, mh, 10, dorado, 1, 2);
        p.textoCentradoConBordeNegro(texto, mx + mw / 2, my + (mh - p.altoDeLinea(tam)) / 2, tam, true, 0xFFFFFF, 1);
    }

    /** Piezas comprables de la tienda para un rango y ranura (null = todas las ranuras). */
    public static List<Pieza> piezasFiltradas(Etiqueta.Categoria rango, Pieza.Ranura ranura) {
        List<Pieza> l = new ArrayList<>();
        for (Pieza p : CatalogoDePiezas.delJuego().comprables(rango)) {
            if (ranura == null || p.ranura == ranura) l.add(p);
        }
        return l;
    }

    private static String nombreRango(Etiqueta.Categoria c) {
        if (c == null) return "Noob";
        switch (c) {
            case JUNIOR: return "Junior";
            case SENIOR: return "Senior";
            case MASTER: return T.t("tienda.rango.especial");
            default: return "Noob";
        }
    }

    private static String nombreRanura(Pieza.Ranura r) {
        switch (r) {
            case CABEZA: return T.t("tienda.ranura.cabeza");
            case BRAZO: return T.t("tienda.ranura.brazo");
            case PIERNAS: return T.t("tienda.ranura.piernas");
            case NUCLEO: return T.t("tienda.ranura.nucleo");
            case NATURALEZA: return T.t("tienda.ranura.naturaleza");
            default: return "";
        }
    }
}
