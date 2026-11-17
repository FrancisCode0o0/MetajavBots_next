package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.textos.T;

/**
 * REVISIÓN — Después de la pelea: el código del robot y los consejos. Sin
 * cerebro, los del Revisor (las cifras y el estilo); con cerebro de IA,
 * además, una revisión escrita por él, adaptada al nivel del jugador.
 */
public final class Revision implements Pantalla {

    private final String codigo;
    private final Pelea pelea;
    private final Combatiente yo;
    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private Revisor.Informe local;
    private CerebroDelJuego.Peticion peticion;
    private final Ui.Desplazable despl = new Ui.Desplazable();

    public Revision(String codigo, Pelea pelea, Combatiente yo) {
        this.codigo = codigo;
        this.pelea = pelea;
        this.yo = yo;
    }

    @Override
    public void alEntrar(Juego j) {
        editor.ponerTexto(codigo);
        editor.soloLectura = true;
        editor.tamano = j.jugador.tamanoDeLetra;
        local = Revisor.revisar(codigo, pelea, j.jugador.idioma);
        Cerebro c = CerebroDelJuego.de(j.jugador);
        if (c != null && peticion == null) {
            StringBuilder informe = new StringBuilder();
            for (int i = 0; i < yo.informes.size(); i++) {
                Combatiente.Informe inf = yo.informes.get(i);
                informe.append("Round ").append(i + 1).append(": damage taken ").append(Math.round(inf.danoRecibido)).append(", dealt ")
                        .append(Math.round(inf.danoHecho)).append(", hits ").append(inf.aciertos).append('/').append(inf.ataques)
                        .append(", errors ").append(inf.errores).append(", step-limit hits ").append(inf.topes).append('\n');
            }
            String numerado = numerar(codigo);
            peticion = CerebroDelJuego.Peticion.lanzar(c, CerebroDelJuego.sistemaRevisor(j.jugador.idioma, j.jugador.etiqueta.nombre),
                    CerebroDelJuego.uno("Fight report:\n" + informe + "\nCode:\n" + numerado), 200);
        }
    }

    static String numerar(String codigo) {
        StringBuilder b = new StringBuilder();
        String[] l = codigo.split("\n");
        for (int i = 0; i < l.length; i++) b.append(i + 1).append(": ").append(l[i]).append('\n');
        return b.toString();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("revision.titulo"), true);
        float w = p.ancho - 40, ew = w * 0.55f;
        editor.dibujar(ui, 20, arriba + 20, ew, p.alto - arriba - 40);
        float x = 40 + ew, cw = w - ew - 20;
        float cy = ui.panel(x, arriba + 20, cw, p.alto - arriba - 40, T.t("revision.consejos"));
        float d = ui.desplazable(despl, x + 10, cy, cw - 20, p.alto - cy - 30);
        float yy = cy - d;
        for (String c : local.consejos) yy += p.parrafo("• " + c, x + 20, yy, cw - 50, ui.letra * 0.88f, ui.tema.texto, 1) + 12;
        yy += 10;
        if (peticion != null) {
            p.texto(T.t("revision.ia"), x + 20, yy, ui.letra, true, ui.tema.acento, 1);
            yy += ui.letra * 1.6f;
            String texto = !peticion.lista ? T.t("cerebro.pensando") + " " + ".".repeat((int) (System.currentTimeMillis() / 400 % 4))
                    : peticion.respuesta != null ? peticion.respuesta : T.f("cerebro.error", peticion.error);
            yy += p.parrafo(texto, x + 20, yy, cw - 50, ui.letra * 0.88f, ui.tema.texto, 1);
        } else {
            yy += p.parrafo(T.t("revision.sin_ia"), x + 20, yy, cw - 50, ui.letra * 0.8f, ui.tema.textoApagado, 1);
        }
        ui.finDesplazable(despl, yy + d - cy + 20);
    }
}
