package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.combate.Combatiente;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.RivalSenior;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;

/**
 * PANEL SENIOR — Pestaña de peleas de nivel Senior dentro de {@link ModoEspecial}:
 * 1) Presupuesto estricto de tiempo (ms) y memoria (KB) por turno.
 * 2) Robots en paralelo (2 contra 2 ejecutándose a la vez con hilos demonio acotados).
 * 3) Los 5 rivales Senior que castigan fallos típicos de programación con estilo absurdo.
 */
public final class PanelSenior {

    private PanelSenior() {
    }

    /** Dibuja el contenido de la pestaña Senior y devuelve la coordenada Y final. */
    static float dibujar(Juego j, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        yy += p.parrafo(T.t("especial.senior_ayuda"), x + 24, yy, w - 48, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 18;

        // 1) Presupuesto de tiempo y memoria por turno.
        p.textoConBordeNegro(T.t("senior.presupuesto_titulo"), x + 24, yy, ui.letra * 1.15f, true, 0xFFC857, 1);
        yy += ui.letra * 1.8f;
        long[][] perfiles = {{2_500_000L, 8_192L, 350L}, {1_500_000L, 4_096L, 220L}};
        for (long[] pr : perfiles) {
            String txt = T.f("senior.presupuesto_boton", String.format(java.util.Locale.ROOT, "%.1f", pr[0] / 1_000_000.0), pr[1] / 1024L, pr[2]);
            float bt = Math.min(w - 48, Math.max(460, p.ancho(txt, ui.letra, true) + 48));
            if (ui.boton(txt, x + 24, yy, bt, 54, Ui.Estilo.NORMAL)) {
                j.ir(new PantallaPelea(encuentroPresupuesto(j, pr[0], pr[1], pr[2])));
            }
            yy += 66;
        }
        yy += 10;

        // 2) Robots en paralelo.
        p.textoConBordeNegro(T.t("senior.paralelo_titulo"), x + 24, yy, ui.letra * 1.15f, true, 0x3FE0FF, 1);
        yy += ui.letra * 1.8f;
        yy += p.parrafo(T.t("senior.paralelo_ayuda"), x + 24, yy, w - 48, ui.letra * 0.85f, ui.tema.textoSuave, 1) + 12;
        String txtPar = T.t("senior.paralelo_boton");
        float btPar = Math.min(w - 48, Math.max(400, p.ancho(txtPar, ui.letra, true) + 48));
        if (ui.boton(txtPar, x + 24, yy, btPar, 54, Ui.Estilo.PRINCIPAL)) {
            j.ir(new PantallaPelea(encuentroParalelo(j)));
        }
        yy += 74;

        // 3) Rivales Senior que castigan fallos típicos.
        p.textoConBordeNegro(T.t("senior.rivales_titulo"), x + 24, yy, ui.letra * 1.15f, true, 0xFF8FB1, 1);
        yy += ui.letra * 1.9f;
        for (RivalSenior.Tipo tipo : RivalSenior.todos()) {
            float hBio = p.altoDeParrafo(tipo.biografiaAbsurda.en(T.idioma), w - 400, ui.letra * 0.78f);
            float hDesc = p.altoDeParrafo(tipo.descripcion.en(T.idioma), w - 400, ui.letra * 0.82f);
            float altoTarjeta = Math.max(114, 46 + hBio + hDesc + 18);
            p.caja(x + 20, yy, w - 40, altoTarjeta, 12, ui.tema.fondo, 0.92f);
            p.bordeRedondeado(x + 20, yy, w - 40, altoTarjeta, 12, 0xFF8FB1, 0.45f, 1.3f);
            p.caja(x + 20, yy + 10, 5, altoTarjeta - 20, 2, 0xFF8FB1, 0.9f);
            float tNom = ui.cabeEntero(tipo.nombre.en(T.idioma), w - 400, ui.letra * 1.05f, true);
            p.textoConBordeNegro(tipo.nombre.en(T.idioma), x + 38, yy + 12, tNom, true, ui.tema.texto, 1);
            p.parrafo(tipo.biografiaAbsurda.en(T.idioma), x + 38, yy + 18 + tNom * 1.25f, w - 400, ui.letra * 0.78f, ui.tema.textoApagado, 1);
            p.parrafo(tipo.descripcion.en(T.idioma), x + 38, yy + 22 + tNom * 1.25f + hBio, w - 400, ui.letra * 0.82f, ui.tema.textoSuave, 1);
            if (ui.boton(T.t("especial.luchar"), x + w - 320, yy + (altoTarjeta - 54) / 2, 280, 54, Ui.Estilo.NORMAL)) {
                j.ir(new PantallaPelea(encuentroRivalSenior(j, tipo)));
            }
            yy += altoTarjeta + 12;
        }
        return yy;
    }

    /** Construye un encuentro Senior con presupuesto estricto de tiempo, memoria y pasos por turno. */
    public static Encuentro encuentroPresupuesto(Juego j, long nanos, long bytes, long pasos) {
        Encuentro e = PrepararPelea.encuentro(j, "ADAPTADA");
        e.modo = "SENIOR";
        e.musica = "senior";
        e.topeDePasos = pasos;
        e.presupuestoNanosPorTurno = nanos;
        e.presupuestoBytesPorTurno = bytes;
        e.otraVez = () -> encuentroPresupuesto(j, nanos, bytes, pasos);
        return e;
    }

    /** Construye un encuentro Senior 2 contra 2 con robots pensando en paralelo. */
    public static Encuentro encuentroParalelo(Juego j) {
        Encuentro e = encuentroRivalSenior(j, RivalSenior.Tipo.CONCURRENCIA);
        e.otraVez = () -> encuentroParalelo(j);
        return e;
    }

    /** Construye un encuentro contra uno de los 5 rivales Senior que castigan fallos típicos. */
    public static Encuentro encuentroRivalSenior(Juego j, RivalSenior.Tipo tipo) {
        Jugador jug = j.jugador;
        Encuentro e = new Encuentro();
        e.modo = "SENIOR";
        e.musica = "senior";
        e.rondas = jug.rondas;
        e.nivelRival = tipo.nivel;
        e.rival = RivalSenior.crear(tipo, T.idioma);
        e.topeDePasos = tipo.topeDePasos;
        e.presupuestoNanosPorTurno = tipo.presupuestoNanos;
        e.presupuestoBytesPorTurno = tipo.presupuestoBytes;
        e.pensarEnParalelo = tipo.enParalelo;
        e.rivalSeniorId = tipo.id;
        e.escenario = PrepararPelea.escenarioAlAzar();
        if (tipo.enParalelo) {
            Jugador.Robot r = jug.robot();
            Combatiente aliado = new Combatiente(r.nombre + " II", jug.montajeDe(r), null);
            e.demas.add(aliado);
            e.equiposDeLosDemas.add(0);
            Combatiente rival2 = RivalSenior.crear(tipo, T.idioma);
            Combatiente gemelo = new Combatiente(rival2.nombre + " II", rival2.montaje, rival2.fabrica);
            gemelo.computadora = true;
            e.demas.add(gemelo);
            e.equiposDeLosDemas.add(1);
        }
        e.otraVez = () -> encuentroRivalSenior(j, tipo);
        return e;
    }

    /** Prepara un robot aliado paralelo con la misma fábrica y memoria compartida que el jugador. */
    static void vincularAliadoParalelo(Encuentro encuentro, Combatiente yo) {
        if (encuentro == null || !encuentro.pensarEnParalelo || yo == null) return;
        for (int i = 0; i < encuentro.demas.size() && i < encuentro.equiposDeLosDemas.size(); i++) {
            if (encuentro.equiposDeLosDemas.get(i) == 0) {
                Combatiente aliado = encuentro.demas.get(i);
                aliado.fabrica = yo.fabrica;
            }
        }
    }

    /** Crea un rival auxiliar para pruebas sin necesidad de ventana. */
    public static Combatiente rivalDePrueba(RivalSenior.Tipo tipo) {
        return tipo == null ? Rivales.muneco() : RivalSenior.crear(tipo, T.idioma);
    }
}
