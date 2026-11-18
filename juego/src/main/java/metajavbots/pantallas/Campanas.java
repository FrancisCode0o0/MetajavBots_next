package metajavbots.pantallas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import metajavbots.Juego;
import metajavbots.campanas.Campana;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.combate.Combatiente;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * CAMPAÑAS — Las historias de varias peleas (Campana): a la izquierda, la
 * lista (las de serie y las que inventó la IA); a la derecha, la elegida: su
 * entrenador la cuenta con su cabecita y su viñeta, y debajo, sus capítulos,
 * cada uno con su rival dibujado. Al ganar un capítulo se abre el siguiente.
 */
public final class Campanas implements Pantalla {

    private int elegida, capitulo = -1;
    private final Ui.Desplazable lista = new Ui.Desplazable(), capitulos = new Ui.Desplazable();
    private VistaDeArena vista;
    private final List<Campana> inventadas = new ArrayList<>();
    private CerebroDelJuego.Peticion peticion;
    private String aviso = "";
    private float t;

    @Override
    public void alEntrar(Juego j) {
        vista = new VistaDeArena(j.pincel);
        inventadas.clear();
        inventadas.addAll(leerInventadas(j.jugador));
    }

    private List<Campana> todas() {
        List<Campana> l = new ArrayList<>(Campana.deSerie());
        l.addAll(inventadas);
        return l;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        t += dt;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("campanas.titulo"), true) + 16, abajo = p.alto - 20;
        mirarLaIA(j);
        List<Campana> cs = todas();
        elegida = Math.max(0, Math.min(cs.size() - 1, elegida));
        float lw = Math.min(460, p.ancho * 0.3f);
        // A la izquierda, la lista; debajo, el botón para que la IA invente una.
        float botones = 64 + (aviso.isEmpty() ? 0 : p.altoDeParrafo(aviso, lw - 16, ui.letra * 0.8f) + 8);
        float cy = ui.panel(20, arriba, lw, abajo - arriba - botones, T.t("campanas.lista"));
        float d = ui.desplazable(lista, 28, cy, lw - 16, abajo - botones - cy - 8);
        float yy = cy - d;
        for (int i = 0; i < cs.size(); i++) {
            Campana c = cs.get(i);
            boolean abierta = c.abierta(jug.capitulosGanados);
            String marca = c.terminada(jug.capitulosGanados) ? "★ " : abierta ? "" : "□ ";
            String cuenta = c.ganados(jug.capitulosGanados) + "/" + c.capitulos.size();
            float tl = ui.letra * 0.9f, cw = p.ancho(cuenta, tl, false) + 16;
            float alto = p.altoDeParrafo(marca + c.titulo.en(jug.idioma), lw - 64 - cw, tl, true) + 20;
            boolean encima = ui.e.dentro(28, yy, lw - 32, alto);
            p.caja(28, yy, lw - 44, alto - 6, 10, i == elegida ? Pincel.oscurecer(ui.tema.acento, 0.55f) : encima ? ui.tema.panelAlto : ui.tema.fondo, 1);
            p.parrafo(marca + c.titulo.en(jug.idioma), 40, yy + 8, lw - 64 - cw, tl, true, abierta ? ui.tema.texto : ui.tema.textoApagado, 1);
            p.textoDerecha(cuenta, 28 + lw - 56, yy + 8, tl, false, ui.tema.textoSuave, 1);
            if (ui.e.clic(28, yy, lw - 44, alto - 6)) {
                j.sonidos.tocar("clic", 0.5f);
                elegida = i;
                capitulo = -1;
            }
            yy += alto;
        }
        ui.finDesplazable(lista, yy + d - cy);
        float by = abajo - botones + 10;
        boolean pensando = peticion != null;
        if (ui.boton(pensando ? T.t("campanas.inventando") : T.t("campanas.inventar"), 20, by, lw, 50, Ui.Estilo.NORMAL, !pensando,
                T.t("campanas.inventar_ayuda"))) {
            inventar(j);
        }
        if (!aviso.isEmpty()) p.parrafo(aviso, 28, by + 58, lw - 16, ui.letra * 0.8f, ui.tema.textoSuave, 1);
        if (!cs.isEmpty()) campana(j, cs.get(elegida), 20 + lw + 20, arriba, p.ancho - lw - 60, abajo - arriba);
    }

    @Override
    public void alSalir(Juego j) {
        j.guardar();
    }

    /** La campaña elegida: título, entrenador que cuenta el capítulo y la lista de capítulos. */
    private void campana(Juego j, Campana c, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        float cy = ui.panel(x, y, w, h, null);
        float tt = ui.cabe(c.titulo.en(jug.idioma), w - 40, ui.letra * 1.6f, true);
        p.texto(c.titulo.en(jug.idioma), x + 20, cy, tt, true, 0xFFC857, 1);
        cy += tt * 1.3f;
        if (!c.homenaje.isEmpty()) {
            p.texto(T.f("campanas.homenaje", c.homenaje) + (c.inventada ? "  · " + T.t("campanas.de_la_ia") : ""), x + 20, cy,
                    ui.letra * 0.75f, false, ui.tema.textoApagado, 1);
            cy += ui.letra * 1.1f;
        }
        int ganados = c.ganados(jug.capitulosGanados);
        boolean abierta = c.abierta(jug.capitulosGanados);
        if (capitulo < 0) capitulo = Math.min(ganados, c.capitulos.size() - 1);
        Campana.Capitulo cap = c.capitulos.get(capitulo);
        // El entrenador cuenta el capítulo elegido (o la victoria, si ya se ganó).
        String cuenta = !abierta ? T.f("campanas.cerrada", tituloDe(c.requiere, jug))
                : jug.capitulosGanados.contains(c.clave(cap.numero)) ? cap.texto.en(jug.idioma) + "\n\n" + cap.victoria.en(jug.idioma)
                : cap.texto.en(jug.idioma);
        Entrenador e = Entrenador.de(c.entrenador);
        float r = 46;
        CabezaDeEntrenador.dibujar(p, e, x + 20 + r, cy + r + 10, r, true, t);
        float tn = ui.cabe(e.nombre.en(jug.idioma), 2 * r + 30, ui.letra * 0.75f, true);
        p.textoCentrado(e.nombre.en(jug.idioma), x + 20 + r, cy + 2 * r + 30, tn, true, ui.tema.textoSuave, 1);
        float vx = x + 40 + 2 * r, vw = w - (vx - x) - 20;
        float vh = Math.max(2 * r + 40, p.altoDeParrafo(cuenta, vw - 28, ui.letra * 0.95f) + 24);
        vh = Math.min(vh, h * 0.42f);
        p.caja(vx, cy, vw, vh, 12, 0xF4F1E8, 1);
        p.parrafo(cuenta, vx + 14, cy + 12, vw - 28, ui.letra * 0.95f, 0x1C1F26, 1);
        cy += vh + 16;
        // Los capítulos, cada uno con su rival dibujado y su botón.
        float zona = y + h - cy - 12;
        float d = ui.desplazable(capitulos, x + 8, cy, w - 16, zona);
        float yy = cy - d, fila = 96;
        for (int i = 0; i < c.capitulos.size(); i++) {
            Campana.Capitulo k = c.capitulos.get(i);
            boolean ganado = jug.capitulosGanados.contains(c.clave(k.numero));
            boolean puede = abierta && i <= ganados;
            boolean visible = yy + fila > cy && yy < cy + zona;
            if (visible) {
                p.caja(x + 16, yy, w - 32, fila - 10, 12, i == capitulo ? ui.tema.panelAlto : ui.tema.fondo, 0.9f);
                Modelo m = Modelo.de(k.modelo);
                if (puede) vista.robotSuelto(p, new Montaje(m), x + 62, yy + fila - 20, 0.85f, t + i, 0, true);
                float tx = x + 124;
                String nombre = puede ? k.rival.en(jug.idioma) : "???";
                p.texto(T.f("campanas.capitulo", k.numero) + "  " + nombre, tx, yy + 12, ui.letra * 1.05f, true,
                        ganado ? ui.tema.bien : puede ? ui.tema.texto : ui.tema.textoApagado, 1);
                String detalle = puede ? T.f("campanas.detalle", m.nombre(jug.idioma), k.rondas) : T.t("campanas.bloqueado");
                p.texto(detalle, tx, yy + 18 + ui.letra * 1.2f, ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
                float bw = 200;
                if (ui.e.clic(x + 16, yy, w - 32 - bw - 20, fila - 10) && puede) capitulo = i;
                String boton = ganado ? T.t("campanas.otra_vez") : T.t("campanas.pelear");
                if (ui.boton(boton, x + w - 32 - bw, yy + (fila - 10 - 50) / 2, bw, 50, ganado ? Ui.Estilo.NORMAL : Ui.Estilo.PRINCIPAL, puede, null)) {
                    capitulo = i;
                    pelear(j, c, k);
                }
            }
            yy += fila;
        }
        ui.finDesplazable(capitulos, yy + d - cy);
    }

    private static String tituloDe(String id, Jugador jug) {
        Campana c = Campana.de(id);
        return c == null ? id : c.titulo.en(jug.idioma);
    }

    /** La pelea de un capítulo: su rival con su nombre, su nivel, su escenario y sus rondas; al ganar, queda apuntado. */
    static void pelear(Juego j, Campana c, Campana.Capitulo k) {
        j.ir(new PantallaPelea(encuentro(j, c, k)));
    }

    static Encuentro encuentro(Juego j, Campana c, Campana.Capitulo k) {
        Jugador jug = j.jugador;
        Encuentro e = new Encuentro();
        e.modo = "CAMPANA";
        e.nivelRival = k.nivel;
        Modelo m = Modelo.de(k.modelo);
        Combatiente r = Rivales.crear(m, k.nivel, Math.max(0.5, jug.agresividad), new Random());
        e.rival = new Combatiente(k.rival.en(jug.idioma), r.montaje, r.fabrica);
        e.rival.computadora = true;
        e.rondas = k.rondas;
        e.escenario = escenario(k.escenario);
        e.titulo = c.titulo.en(jug.idioma);
        e.alGanar = k.victoria.en(jug.idioma);
        e.alAcabar = gano -> {
            if (gano) jug.capitulosGanados.add(c.clave(k.numero));
        };
        e.otraVez = () -> encuentro(j, c, k);
        return e;
    }

    static VistaDeArena.Escenario escenario(String nombre) {
        try {
            return VistaDeArena.Escenario.valueOf(nombre);
        } catch (IllegalArgumentException ex) {
            return VistaDeArena.Escenario.HANGAR;
        }
    }

    // ================================================================ las que inventa la IA

    private static Path archivo(Jugador jug) {
        return Guardado.carpeta.resolve(Guardado.carpetaDe(jug.nombre)).resolve("campanas_inventadas.txt");
    }

    private static List<Campana> leerInventadas(Jugador jug) {
        try {
            Path a = archivo(jug);
            return Files.exists(a) ? Campana.leer(Files.readString(a, StandardCharsets.UTF_8), true) : List.of();
        } catch (IOException ex) {
            return List.of();
        }
    }

    /** Le pide a la IA una campaña nueva, en parodia y en el formato del archivo; se guarda en la partida. */
    private void inventar(Juego j) {
        Cerebro c = CerebroDelJuego.pesado(j.jugador);
        if (c == null) {
            aviso = T.t("campanas.sin_ia");
            return;
        }
        aviso = "";
        String modelos = String.join(", ", Modelo.todos().stream().map(m -> m.id).toList());
        peticion = CerebroDelJuego.Peticion.lanzar(c, CerebroDelJuego.sistemaCampana(modelos),
                CerebroDelJuego.uno("Invent a new parody campaign. Its id must start with ia_ and be unique: ia_" + System.currentTimeMillis() % 100000),
                900);
    }

    private void mirarLaIA(Juego j) {
        if (peticion == null || !peticion.lista) return;
        CerebroDelJuego.Peticion p = peticion;
        peticion = null;
        List<Campana> nuevas = p.respuesta == null ? List.of() : Campana.leer(p.respuesta, true);
        if (nuevas.isEmpty()) {
            aviso = T.t("campanas.ia_fallo");
            return;
        }
        try {
            Path a = archivo(j.jugador);
            Files.createDirectories(a.getParent());
            String antes = Files.exists(a) ? Files.readString(a, StandardCharsets.UTF_8) + "\n" : "";
            Files.writeString(a, antes + p.respuesta.strip() + "\n", StandardCharsets.UTF_8);
        } catch (IOException ex) {
            aviso = T.t("campanas.ia_fallo");
            return;
        }
        inventadas.addAll(nuevas);
        elegida = todas().size() - nuevas.size();
        capitulo = -1;
        aviso = T.t("campanas.ia_lista");
    }
}
