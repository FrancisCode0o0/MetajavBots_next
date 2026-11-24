package metajavbots.pantallas;

import java.util.List;
import java.util.Random;

import metajavbots.Juego;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Entrenador;
import metajavbots.tutor.LecturaDelRival;

/**
 * EL ENTRENADOR EN LA PELEA — Consejos sobre la marcha: el jugador hace clic
 * en un enemigo para marcarlo (le sale una diana encima) y el entrenador mira
 * cómo pelea (tutor/LecturaDelRival) y propone qué probar, con su pista y un
 * trocito de pseudocódigo según la barra de ayuda: «veo que te rodea por la
 * izquierda; apunta a donde va a estar…». El cambio se hace en la pausa
 * entre rondas. Contra los jefes no sale (está en el hospital).
 */
final class EntrenadorEnLaPelea {

    private int marcado = -1;
    private final LecturaDelRival lectura = new LecturaDelRival();
    /** El consejo que enseña (id de LecturaDelRival, o «nada»), cuánto lleva y cuántos ha descartado el jugador con «Otro». */
    private String consejo;
    private float tiempo, desdeElCambio, t;
    private int saltados;

    /** Los avisos de órdenes (como mucho una vez por pelea cada uno: este objeto vive lo que la pelea). */
    final AvisoDeOrdenes avisos = new AvisoDeOrdenes();
    /** A quién se le cuentan los avisos (null: el entrenador no está en esta pelea, o el jugador los ha apagado). */
    private Jugador paraAvisos;
    private final Random azar = new Random();
    /** Cuánto asoma el panel del aviso al borde (0: nada; 1: entero), para que entre y salga sin saltos. */
    private float asomaAviso;
    /** El ancho del panel del aviso, a la derecha de la arena (el mismo hueco que el depurador). */
    static final float ANCHO_AVISO = 460;

    private float tiempoDelMilagro = -1;
    private String fraseDelMilagro;

    int marcado() {
        return marcado;
    }

    /** Para la IA: cómo va la pelea (cada robot, su daño, dónde está, su último error; cuál está marcado) y el código del jugador. */
    String contexto(Pelea pelea, String codigo) {
        StringBuilder sb = new StringBuilder("The player is in a robot fight (round ").append(pelea.ronda()).append(" of ").append(pelea.rondas)
                .append("; whoever takes less damage wins).\n");
        for (int i = 0; i < pelea.combatientes.length; i++) {
            metajavbots.combate.Combatiente c = pelea.combatientes[i];
            Cuerpo cu = pelea.cuerpo(i);
            sb.append(i == 0 ? "PLAYER " : i == marcado ? "MARKED ENEMY " : "ENEMY ").append(c.nombre).append(" (")
                    .append(c.montaje.modelo.id).append("): damage taken ").append(Math.round(c.danoTotalRecibido)).append(", dealt ")
                    .append(Math.round(c.danoTotalHecho));
            if (cu != null) sb.append(", at (").append(Math.round(cu.x)).append(", ").append(Math.round(cu.y)).append("), energy ")
                    .append(Math.round(cu.energia));
            if (!c.ultimoError.isEmpty()) sb.append(", last error: ").append(c.ultimoError);
            sb.append('\n');
        }
        return sb.append("The player's robot code:\n").append(EntrenadorFlotante.conNumeros(codigo, 160)).toString();
    }

    /** Tras ganar a un jefe, el entrenador «sale del hospital» milagrosamente a felicitarte (al final sí fue al combate). */
    void milagro(Juego j, float anchoDelCuadro, float dt) {
        Entrenador en = Entrenador.de(j.jugador.entrenador);
        if (tiempoDelMilagro < 0) {
            tiempoDelMilagro = 0;
            fraseDelMilagro = Hospital.milagro(System.nanoTime());
            j.voz.decir(fraseDelMilagro, j.jugador.idioma, en.timbre);
        }
        tiempoDelMilagro += dt;
        Pincel p = j.pincel;
        float libre = (p.ancho - anchoDelCuadro) / 2;
        if (libre >= 300) Hospital.milagro(p, en, fraseDelMilagro, 10, p.alto * 0.3f, libre - 20, tiempoDelMilagro);
        else Hospital.milagro(p, en, fraseDelMilagro, 0, 150, p.ancho, tiempoDelMilagro);
    }

    /** Con el entrenador en esta pelea (ni jefe ni hospital, encendido y con los avisos puestos): a ese jugador. */
    void avisarA(Jugador jug) {
        paraAvisos = jug;
    }

    /** Cada turno de la pelea: los avisos de órdenes (al ritmo del jugador) y, si hay un enemigo marcado, el entrenador lo mira. */
    void observar(Pelea pelea) {
        if (paraAvisos != null) avisos.observar(pelea.cuerpo(0), paraAvisos, azar);
        if (marcado < 0) return;
        Cuerpo yo = pelea.cuerpo(0), el = pelea.cuerpo(marcado);
        if (yo == null || el == null || el.fuera) return;
        lectura.observar(yo.x, yo.y, yo.mira, el.x, el.y, el.velocidad, el.vuela(), pelea.combatientes[0].danoTotalRecibido,
                pelea.combatientes[0].danoTotalHecho, pelea.ancho, pelea.alto);
    }

    /** Una ronda nueva: lo visto en la anterior ya no vale (el rival puede haber cambiado de táctica). */
    void nuevaRonda() {
        lectura.olvidar();
        consejo = null;
    }

    /** El clic en la arena: si cae sobre un enemigo, lo marca (y si es el marcado, lo desmarca). Antes de dibujar los botones. */
    void clic(Juego j, Pelea pelea, VistaDeArena vista) {
        for (int i = 1; i < pelea.combatientes.length; i++) {
            Cuerpo c = pelea.cuerpo(i);
            if (c == null || c.fuera || pelea.equipo[i] == pelea.equipo[0]) continue;
            float sx = vista.sx(c.x, c.y), sy = vista.sy(c.y), r = Math.max(30, vista.escala(c.y) * (float) c.radio * 1.6f);
            if (j.entrada.clic(sx - r, sy - r * 2.2f, r * 2, r * 2.6f)) {
                marcar(j, i == marcado ? -1 : i, pelea);
                return;
            }
        }
    }

    void marcarDesdeFuera(int i) {
        marcado = i;
        lectura.olvidar();
        consejo = null;
    }

    private void marcar(Juego j, int i, Pelea pelea) {
        marcado = i;
        lectura.olvidar();
        consejo = null;
        saltados = 0;
        j.sonidos.tocar("clic", 0.5f);
    }

    /** La diana sobre el marcado y, abajo a la derecha, el entrenador con su consejo. */
    void dibujar(Juego j, Pelea pelea, VistaDeArena vista, float ancho, float dt, boolean enMarcha) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        t += dt;
        Entrenador en = Entrenador.de(j.jugador.entrenador);
        if (marcado >= 0) {
            Cuerpo c = pelea.cuerpo(marcado);
            if (c == null || c.fuera) marcado = -1;
            else {
                float sx = vista.sx(c.x, c.y), sy = vista.sy(c.y) - vista.escala(c.y) * (float) c.radio * 2.6f;
                float r = 16 + (float) Math.sin(t * 5) * 3;
                p.bordeRedondeado(sx - r, sy - r, 2 * r, 2 * r, r, 0xFFD23F, 0.95f, 2.5f);
                p.linea(sx - r - 6, sy, sx - r + 6, sy, 2.5f, 0xFFD23F, 1);
                p.linea(sx + r - 6, sy, sx + r + 6, sy, 2.5f, 0xFFD23F, 1);
                p.linea(sx, sy - r - 6, sx, sy - r + 6, 2.5f, 0xFFD23F, 1);
            }
        }
        // El consejo cambia como mucho cada 6 s, y solo si lo que ve ha cambiado (TODO-FRAN: el ritmo).
        String texto, trozo = null;
        if (marcado < 0) {
            texto = T.t("entrenador.pelea.marca");
        } else {
            List<String> ls = lectura.lecturas();
            desdeElCambio += enMarcha ? dt : 0;
            if (lectura.bastante() && (consejo == null || (desdeElCambio > 6 && !ls.contains(consejo)))) {
                String nuevo = ls.isEmpty() ? "nada" : ls.get(Math.min(saltados, ls.size() - 1) % Math.max(1, ls.size()));
                if (!nuevo.equals(consejo)) {
                    consejo = nuevo;
                    tiempo = 0;
                    desdeElCambio = 0;
                    j.voz.decir(T.t("entrenador.rival." + consejo), j.jugador.idioma, en.timbre);
                }
            }
            if (consejo == null) texto = T.f("entrenador.pelea.mirando", pelea.combatientes[marcado].nombre);
            else {
                texto = T.t("entrenador.rival." + consejo);
                // El trocito de pseudocódigo, solo si la barra de ayuda lo permite (con poca, la observación y nada más).
                if (PanelDelTutor.ayuda(j.jugador, 0) >= 0.45f) trozo = T.t("entrenador.rival." + consejo + ".trozo");
            }
        }
        // El aviso de órdenes no va aquí (esta caja está sobre la arena): sale en su panel, al borde (avisoAlBorde).
        boolean conAviso = false;
        tiempo += dt;
        float w = Math.min(470, ancho * 0.4f), tl = ui.letra * 0.78f, x = ancho - w - 16;
        float th = p.altoDeParrafo(texto, w - 90, tl);
        float trozoAncho = w - 96, tTrozo = tl * 0.95f;
        float trozoCuerpo = trozo == null ? 0 : p.altoDeParrafoDeCodigo(trozo, trozoAncho, tTrozo, false);
        float trozoAlto = trozo == null ? 0 : trozoCuerpo + tl * 1.6f + 10;
        float h = Math.max(78, th + trozoAlto + (marcado >= 0 ? 58 : 20)), y = p.alto - 80 - h;
        p.caja(x, y, w, h, 12, ui.tema.panelAlto, 0.9f);
        p.bordeRedondeado(x, y, w, h, 12, conAviso ? ui.tema.aviso : marcado >= 0 ? 0xFFD23F : ui.tema.borde, conAviso ? 1f : 0.8f, conAviso ? 2.5f : 1.5f);
        CabezaDeEntrenador.dibujar(p, en, x + 38, y + 38, 24, tiempo < 3, t);
        p.parrafo(texto, x + 76, y + 10, w - 90, tl, ui.tema.texto, 1);
        float yy = y + 14 + th;
        if (trozo != null) {
            p.texto(T.t("entrenador.pelea.trozo"), x + 76, yy, tl * 0.9f, true, ui.tema.textoSuave, 1);
            yy += tl * 1.4f;
            p.caja(x + 70, yy - 4, w - 80, trozoCuerpo + 8, 6, 0x1B2430, 1);
            p.parrafoDeCodigo(trozo, x + 78, yy, trozoAncho, tTrozo, false, 0xE8E2C8, 1);
            yy += trozoCuerpo + 10;
        }
        if (marcado >= 0) {
            float bw = (w - 90 - 8) / 2;
            if (ui.boton(T.t("entrenador.pelea.otro"), x + 76, y + h - 48, bw, 38, Ui.Estilo.PLANO, consejo != null, T.t("entrenador.pelea.pausa"))) {
                saltados++;
                List<String> ls = lectura.lecturas();
                String nuevo = ls.isEmpty() ? "nada" : ls.get(saltados % ls.size());
                consejo = nuevo;
                tiempo = 0;
                desdeElCambio = 0;
                j.voz.decir(T.t("entrenador.rival." + consejo), j.jugador.idioma, en.timbre);
            }
            if (ui.boton(T.t("entrenador.pelea.quitar"), x + 84 + bw, y + h - 48, bw, 38, Ui.Estilo.PLANO)) marcar(j, -1, pelea);
        }
    }

    /**
     * Cuánto ancho le quita a la arena el panel del aviso ahora (0 si no hay aviso): entra y sale despacio, sin saltos.
     * El aviso nunca tapa la arena: la arena se encoge y el panel va a su derecha.
     */
    float anchoDelAviso(float dt, boolean puede) {
        boolean quiere = puede && paraAvisos != null && avisos.activo();
        asomaAviso = Math.max(0, Math.min(1, asomaAviso + (quiere ? dt : -dt) * 4f));
        return ANCHO_AVISO * asomaAviso * asomaAviso * (3 - 2 * asomaAviso);
    }

    /** Si hay un aviso que se está enseñando o recogiendo (para no dibujar el panel en balde). */
    boolean conAviso() {
        return asomaAviso > 0;
    }

    /**
     * El panel del aviso al borde derecho de la arena: la cabeza del entrenador, el aviso en llano y el botón
     * «Entendido». Lo dice en voz alta una vez.
     */
    void avisoAlBorde(Juego j, float x, float dt, boolean enMarcha) {
        if (asomaAviso <= 0) return;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Entrenador en = Entrenador.de(j.jugador.entrenador);
        if (avisos.recienSalido()) j.voz.decir(avisos.texto(), j.jugador.idioma, en.timbre);
        if (enMarcha) avisos.pasar(dt);
        float y = 88, w = p.ancho - x, h = p.alto - y;
        p.rect(x, y, w, h, ui.tema.panel, 0.98f);
        p.rect(x, y, 3, h, ui.tema.aviso, 1);
        float tx = x + 24, tw = Math.max(120, w - 48), cy = y + 18;
        CabezaDeEntrenador.dibujar(p, en, tx + 30, cy + 30, 30, avisos.tiempo() < 3, t);
        String titulo = T.t("aviso.ordenes.titulo");
        float tTit = ui.cabe(titulo, tw - 76, Math.min(24, ui.letra * 1.05f), true);
        p.texto(titulo, tx + 76, cy + 6, tTit, true, ui.tema.aviso, 1);
        p.texto(en.nombre.en(j.jugador.idioma), tx + 76, cy + 10 + p.altoDeLinea(tTit), Math.min(18, ui.letra * 0.8f), false,
                ui.tema.textoSuave, 1);
        cy += 84;
        String texto = avisos.activo() ? avisos.texto() : "";
        float tl = Math.min(22, ui.letra * 0.95f);
        cy += p.parrafo(texto, tx, cy, tw, tl, ui.tema.texto, 1) + 14;
        cy += p.parrafo(T.t("aviso.ordenes.f9"), tx, cy, tw, Math.min(19, ui.letra * 0.8f), ui.tema.textoSuave, 1) + 18;
        if (ui.boton(T.t("comun.entendido"), tx, cy, tw, 46, Ui.Estilo.PRINCIPAL, avisos.activo(), null)) avisos.cerrar();
    }
}
