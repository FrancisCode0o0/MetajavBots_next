package metajavbots.pantallas;

import java.util.List;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Tema;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Biografias;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * AJUSTES — El idioma, el tamaño de la letra, el tema claro u oscuro, el
 * volumen, la voz del locutor y, a la derecha, los tres cerebros de IA
 * (PanelDeCerebros): locales (KoboldCpp) o en línea con su clave (que se
 * guarda fuera del juego, en la carpeta del usuario), y qué hace cada uno.
 */
public final class Ajustes implements Pantalla {

    private final PanelDeCerebros cerebros = new PanelDeCerebros();
    private boolean cargado;
    private float t;

    @Override
    public void alEntrar(Juego j) {
        if (cargado) return;
        cargado = true;
        cerebros.cargar(j.jugador);
    }

    private final Ui.Desplazable izquierda = new Ui.Desplazable();

    @Override
    public void alSalir(Juego j) {
        Jugador jug = j.jugador;
        cerebros.guardar(jug);
        j.ponerJugador(jug);
        j.guardar();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("menu.ajustes"), true);
        float x = 60, w = (p.ancho - 160) / 2;
        // A la izquierda: idioma, letra, tema, sonido, voz… (se desplaza si con letra grande no cabe).
        float zona = arriba + 12, d = ui.desplazable(izquierda, 40, zona, w + 40, p.alto - zona - 12);
        float y = zona + 12 - d;
        p.textoConBordeNegro(T.t("ajustes.idioma"), x, y, ui.letra, true, ui.tema.texto, 1);
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
        jug.arenasConTrampas = ui.casilla(T.t("ajustes.trampas"), jug.arenasConTrampas, x, y, T.t("ajustes.trampas_ayuda"));
        PrepararPelea.conTrampas = jug.arenasConTrampas;
        y += 50;
        jug.aparatosEnLaArena = ui.casilla(T.t("ajustes.aparatos"), jug.aparatosEnLaArena, x, y, T.t("ajustes.aparatos_ayuda"));
        y += 50;
        jug.armasEspeciales = ui.casilla(T.t("ajustes.especiales"), jug.armasEspeciales, x, y, T.t("ajustes.especiales_ayuda"));
        y += 50;
        jug.avisosDeOrdenes = ui.casilla(T.t("ajustes.avisos_ordenes"), jug.avisosDeOrdenes, x, y, T.t("ajustes.avisos_ordenes_ayuda"));
        y += 50;
        boolean claro = ui.casilla(T.t("ajustes.tema_claro"), jug.temaClaro, x, y, null);
        if (claro != jug.temaClaro) {
            jug.temaClaro = claro;
            ui.tema = Tema.de(claro);
        }
        y += 50;
        jug.volumen = ui.deslizador(T.t("ajustes.volumen"), jug.volumen, x, y, w, 50, null);
        j.sonidos.volumen = jug.volumen;
        y += 76;
        jug.musicaActiva = ui.casilla(T.t("ajustes.musica"), jug.musicaActiva, x, y, T.t("ajustes.musica_ayuda"));
        j.musica.activa = jug.musicaActiva;
        y += 50;
        if (jug.musicaActiva) {
            jug.volumenMusica = ui.deslizador(T.t("ajustes.volumen_musica"), jug.volumenMusica, x, y, w, 50, null);
            j.musica.volumen = jug.volumenMusica;
            y += 76;
        }
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
        jug.entrenadorActivo = ui.casilla(T.t("ajustes.entrenador"), jug.entrenadorActivo, x, y, T.t("ajustes.entrenador_ayuda"));
        y += 50;
        y = selectorDeEntrenador(j, jug, x, y, w);
        if (ui.boton(T.t("ajustes.probar_voz"), x, y, 260, 50, Ui.Estilo.NORMAL)) {
            j.voz.decir(metajavbots.dialogos.Dialogos.de("locutor").frase("inicio_pelea", jug.idioma, java.util.Map.of("jugador", jug.nombre)), jug.idioma);
        }
        ui.finDesplazable(izquierda, y + 70 + d - zona);
        // A la derecha: los cerebros de IA.
        cerebros.dibujar(j, x + w + 40, arriba + 16, w, p.alto - arriba - 40);
    }

    private float selectorDeEntrenador(Juego j, Jugador jug, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        List<Entrenador> lista = Entrenador.todos();
        Entrenador actual = Entrenador.de(jug.entrenador);
        int indice = Math.max(0, lista.indexOf(actual));
        p.texto(T.t("ajustes.entrenador_tipo"), x, y, ui.letra, true, ui.tema.texto, 1);
        y += ui.letra * 1.45f;
        float alto = 56f;
        if (ui.boton("◄", x, y, 44, alto, Ui.Estilo.PLANO)) cambiarEntrenador(j, jug, lista, indice - 1);
        if (ui.boton("►", x + w - 44, y, 44, alto, Ui.Estilo.PLANO)) cambiarEntrenador(j, jug, lista, indice + 1);
        float centroX = x + 52, centroW = Math.max(120, w - 104);
        p.caja(centroX, y, centroW, alto, 10, ui.tema.panel, 1);
        CabezaDeEntrenador.dibujar(p, actual, centroX + 30, y + alto / 2f, 18f, false, t);
        String nombre = actual.nombre.en(jug.idioma);
        float tn = ui.cabe(nombre, centroW - 68, ui.letra * 0.95f, true);
        p.texto(nombre, centroX + 58, y + (alto - tn * 1.2f) / 2f, tn, true, ui.tema.texto, 1);
        if (ui.e.dentro(centroX, y, centroW, alto)) {
            ui.avisar(actual.papel.en(jug.idioma) + "\n\n" + Biografias.deEntrenador(actual.id, jug.idioma));
        }
        y += alto + 10f;
        if (ui.boton("✎ " + T.t("biografias.editar"), x, y, Math.min(w, 280), 42, Ui.Estilo.PLANO, true, T.t("biografias.editar_ayuda"))) {
            j.ir(new PantallaBiografias(true, actual.id));
        }
        return y + 54f;
    }

    private void cambiarEntrenador(Juego j, Jugador jug, List<Entrenador> lista, int indice) {
        Entrenador nuevo = lista.get(Math.floorMod(indice, lista.size()));
        jug.entrenador = nuevo.id;
        j.entrenadorFlotante.sincronizarEntrenador(jug);
        j.voz.decir(nuevo.frase(jug.idioma, 0), jug.idioma, nuevo.timbre);
    }
}
