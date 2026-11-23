package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.combate.Combatiente;
import metajavbots.combate.ModificadoresPoo;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.T;

/**
 * INFORME DE LA RONDA — La pestaña «Informe» de la pausa entre rondas: las
 * cifras de cada robot (daño recibido y hecho, puntería, tiempo viendo al
 * enemigo, errores, topes de cálculo), los consejos del revisor y, para que
 * enseñe a mejorar, de cada cifra qué significa (al pasar el ratón) y, si va
 * mal, una pista concreta de qué cambiar en el código. Un clic en una cifra
 * abre la ficha de su mecánica (pantallas/Mecanicas).
 */
public final class InformeDeRonda {

    /** Una cifra del informe: el texto, qué significa, la pista si va mal (o null) y la ficha de su mecánica. */
    public record Cifra(String texto, String que, String pista, String ficha) {
    }

    private final Ui.Desplazable[] despl = {new Ui.Desplazable(), new Ui.Desplazable(), new Ui.Desplazable()};

    /** Las cifras de ese robot en la ronda; las pistas, solo para el jugador (i = 0) y solo las que van mal. */
    public static List<Cifra> cifras(Pelea pelea, int i) {
        Combatiente.Informe inf = pelea.informe(i), otro = pelea.informe(i == 0 ? 1 : 0);
        boolean yo = i == 0;
        int visto = (int) Math.round(100.0 * inf.turnosVistos / Math.max(1, inf.turnos));
        String pistaPunteria = inf.ataques == 0 ? "informe.pista.sin_ataques" : inf.punteria() < 0.35 ? "informe.pista.punteria" : null;
        List<Cifra> l = new ArrayList<>();
        l.add(cifra(T.f("informe.dano_recibido", Math.round(inf.danoRecibido)), "informe.que.dano_recibido",
                yo && inf.danoRecibido > otro.danoRecibido + 0.5 ? "informe.pista.dano_recibido" : null, "mec_como_se_gana"));
        l.add(cifra(T.f("informe.dano_hecho", Math.round(inf.danoHecho)), "informe.que.dano_hecho",
                yo && inf.danoHecho + 0.5 < otro.danoHecho ? "informe.pista.dano_hecho" : null, "mec_armas"));
        l.add(cifra(T.f("informe.punteria", inf.aciertos, inf.ataques, Math.round(inf.punteria() * 100)), "informe.que.punteria",
                yo ? pistaPunteria : null, "mec_turno"));
        l.add(cifra(T.f("informe.visto", visto), "informe.que.visto", yo && visto < 30 ? "informe.pista.visto" : null, "mec_radar"));
        l.add(cifra(T.f("informe.errores", inf.errores), "informe.que.errores", yo && inf.errores > 0 ? "informe.pista.errores" : null,
                "mec_tope"));
        l.add(cifra(T.f("informe.topes", inf.topes, Math.round(inf.pasosMedios())), "informe.que.topes",
                yo && inf.topes > 0 ? "informe.pista.topes" : null, "mec_tope"));
        l.add(cifra(T.f("informe.total", Math.round(pelea.combatientes[i].danoTotalRecibido)), "informe.que.total", null, "mec_como_se_gana"));
        return l;
    }

    private static Cifra cifra(String texto, String que, String pista, String ficha) {
        return new Cifra(texto, T.t(que), pista == null ? null : T.t(pista), ficha);
    }

    void dibujar(Juego j, Pelea pelea, Revisor.Informe revision, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float col = (w - 40) / 3;
        for (int i = 0; i < 2; i++) {
            float cx = x + i * (col + 20);
            int color = i == 0 ? 0x3FE0FF : 0xFF4FD8;
            Combatiente co = pelea.combatientes[i];
            String nombre = co.nombre + (co.montaje != null && co.montaje.modelo != null
                    ? "  ·  " + T.f("pelea.modelo", co.montaje.modelo.nombre(T.idioma)) : "");
            p.texto(nombre, cx, y, ui.cabe(nombre, col - 10, ui.letra * 1.1f, true), true, color, 1);
            float d = ui.desplazable(despl[i], cx, y + 40, col, h - 40);
            float inicio = y + 40 - d, yy = inicio;
            for (Cifra c : cifras(pelea, i)) yy = fila(j, c, cx, yy, col - 22);
            yy += 10;
            Combatiente.Informe inf = pelea.informe(i);
            for (java.util.Map.Entry<String, int[]> e : inf.porArma.entrySet()) {
                String t = "· " + e.getKey() + ": " + e.getValue()[1] + "/" + e.getValue()[0];
                p.texto(t, cx + 10, yy, ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
                if (ui.e.dentro(cx, yy, col - 22, ui.letra * 1.3f)) ui.avisar(T.t("informe.que.arma"));
                yy += ui.letra * 1.3f;
            }
            if (i == 0 && co.efectosPoo != null) yy = poo(j, co.efectosPoo.resumen(), cx, yy, col - 22);
            ui.finDesplazable(despl[i], Math.max(1, yy - inicio) + 8);
        }
        // Consejos del revisor (sin IA, o con ella si está puesta).
        float rx = x + 2 * (col + 20);
        p.texto(T.t("informe.consejos"), rx, y, ui.letra * 1.1f, true, ui.tema.texto, 1);
        float d = ui.desplazable(despl[2], rx, y + 40, col, h - 40);
        float yy = y + 40 - d;
        if (revision != null) {
            for (String c : revision.consejos) {
                yy += p.parrafo("• " + c, rx, yy, col - 20, ui.letra * 0.85f, ui.tema.texto, 1) + 10;
            }
        }
        ui.finDesplazable(despl[2], yy - (y + 40 - d));
    }

    /** Una cifra: su línea, su pista debajo si va mal; al pasar el ratón, qué significa; con un clic, su ficha. */
    private static float fila(Juego j, Cifra c, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float alto = p.altoDeParrafo(c.texto(), w, ui.letra * 0.9f);
        boolean encima = ui.e.dentro(x - 4, y - 2, w + 8, alto + 4);
        if (encima) p.caja(x - 4, y - 2, w + 8, alto + 4, 6, ui.tema.acento, 0.12f);
        p.parrafo(c.texto(), x, y, w, ui.letra * 0.9f, ui.tema.texto, 1);
        if (encima) {
            ui.avisar(c.que() + (c.pista() != null ? "\n" + c.pista() : "") + "\n" + T.t("informe.ficha"));
            if (ui.e.clic(x - 4, y - 2, w + 8, alto + 4)) {
                j.sonidos.tocar("clic", 0.5f);
                j.ir(new Mecanicas(c.ficha()));
            }
        }
        y += alto + 6;
        if (c.pista() != null) {
            String pista = T.t("informe.pista_titulo") + " " + c.pista();
            y += p.parrafo(pista, x + 10, y, w - 10, ui.letra * 0.76f, ui.tema.aviso, 1) + 8;
        }
        return y;
    }

    private static float poo(Juego j, List<ModificadoresPoo.Entrada> poo, float x, float y, float w) {
        if (poo.isEmpty()) return y;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        y += 8;
        p.texto(T.t("poo.titulo"), x, y, ui.letra * 0.9f, true, ui.tema.texto, 1);
        if (ui.e.dentro(x, y, w, ui.letra * 1.3f)) ui.avisar(T.t("informe.que.poo"));
        y += ui.letra * 1.3f;
        for (ModificadoresPoo.Entrada e : poo) {
            int colPoo = e.nivel == ModificadoresPoo.Nivel.PENALIZACION ? ui.tema.mal
                    : e.nivel == ModificadoresPoo.Nivel.BONO ? ui.tema.bien : ui.tema.aviso;
            y += p.parrafo("• " + e.texto, x + 6, y, w - 6, ui.letra * 0.78f, colPoo, 1) + 4;
        }
        return y;
    }
}
