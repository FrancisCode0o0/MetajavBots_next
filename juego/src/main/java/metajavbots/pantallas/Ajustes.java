package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Tema;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * AJUSTES — El idioma, el tamaño de la letra, el tema claro u oscuro, el
 * volumen, la voz del locutor y, a la derecha, los tres cerebros de IA
 * (PanelDeCerebros): locales (KoboldCpp) o en línea con su clave (que se
 * guarda fuera del juego, en la carpeta del usuario), y qué hace cada uno.
 */
public final class Ajustes implements Pantalla {

    private final PanelDeCerebros cerebros = new PanelDeCerebros();
    private boolean cargado;

    @Override
    public void alEntrar(Juego j) {
        if (cargado) return;
        cargado = true;
        cerebros.cargar(j.jugador);
    }

    @Override
    public void alSalir(Juego j) {
        Jugador jug = j.jugador;
        cerebros.guardar(jug);
        j.ponerJugador(jug);
        j.guardar();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("menu.ajustes"), true);
        float x = 60, y = arriba + 24, w = (p.ancho - 160) / 2;
        // A la izquierda: idioma, letra, tema, sonido, voz.
        p.texto(T.t("ajustes.idioma"), x, y, ui.letra, true, ui.tema.texto, 1);
        y += 34;
        for (int i = 0; i < Idioma.values().length; i++) {
            Idioma id = Idioma.values()[i];
            if (ui.boton(id.nombre, x + i * (w / 3), y, w / 3 - 10, 50, jug.idioma == id ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                jug.idioma = id;
                T.idioma = id;
            }
        }
        y += 74;
        float letra = ui.deslizador(T.f("ajustes.letra", Math.round(jug.tamanoDeLetra)), (jug.tamanoDeLetra - 14) / 14f, x, y, w, 50,
                T.t("ajustes.letra_ayuda"));
        jug.tamanoDeLetra = Math.round(14 + letra * 14);
        y += 76;
        boolean claro = ui.casilla(T.t("ajustes.tema_claro"), jug.temaClaro, x, y, null);
        if (claro != jug.temaClaro) {
            jug.temaClaro = claro;
            ui.tema = Tema.de(claro);
        }
        y += 50;
        jug.volumen = ui.deslizador(T.t("ajustes.volumen"), jug.volumen, x, y, w, 50, null);
        j.sonidos.volumen = jug.volumen;
        y += 76;
        jug.vozDelLocutor = ui.casilla(T.t("ajustes.voz"), jug.vozDelLocutor, x, y, T.t("ajustes.voz_ayuda"));
        j.voz.activa = jug.vozDelLocutor;
        y += 50;
        String ayudaClara = j.voz.hayVozClara(jug.idioma) ? T.t("ajustes.voz_clara_ayuda") : T.t("ajustes.voz_clara_falta");
        jug.vozClara = ui.casilla(T.t("ajustes.voz_clara"), jug.vozClara, x, y, ayudaClara);
        j.voz.clara = jug.vozClara;
        y += 50;
        jug.volumenVoz = ui.deslizador(T.t("ajustes.volumen_voz"), jug.volumenVoz, x, y, w, 50, null);
        j.voz.volumen = jug.volumenVoz;
        y += 76;
        jug.ayudaDelTutor = ui.deslizador(T.t("ajustes.ayuda_tutor"), jug.ayudaDelTutor, x, y, w, 50, T.t("ajustes.ayuda_tutor_ayuda"));
        y += 76;
        if (ui.boton(T.t("ajustes.probar_voz"), x, y, 260, 50, Ui.Estilo.NORMAL)) {
            j.voz.decir(metajavbots.dialogos.Dialogos.de("locutor").frase("inicio_pelea", jug.idioma, java.util.Map.of("jugador", jug.nombre)), jug.idioma);
        }
        j.voz.fotograma(j.sonidos);
        // A la derecha: los cerebros de IA.
        cerebros.dibujar(j, x + w + 40, arriba + 16, w, p.alto - arriba - 40);
    }
}
