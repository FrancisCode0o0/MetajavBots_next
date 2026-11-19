package metajavbots.pantallas;

import java.util.Random;

import metajavbots.Juego;
import metajavbots.codigo.TraductorDeApi;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Registro;
import metajavbots.rivales.Rivales;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * CÓDIGO DEL RIVAL — Muestra el código real del último rival con el que se
 * peleó (en el idioma de la API activo). Si el jugador aún no ha peleado nunca,
 * enseña el código de un rival al azar con el comentario de sparring.
 */
public final class CodigoDelRival implements Pantalla {

    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final PanelDeApi api = new PanelDeApi();
    private String ayuda = "";
    private RivalesAbsurdos.Perfil perfil;
    private Idioma idioma = Idioma.ES;

    /** Primera línea que se antepone cuando el jugador todavía no ha disputado ninguna pelea. */
    public static String comentarioDeSparring(Idioma idioma) {
        return T.en("rival.sparring", idioma);
    }

    /**
     * El código que debe mostrarse al jugador: el real de su último rival (en el idioma de la API activo),
     * o el de un rival válido al azar precedido del comentario de sparring si todavía no ha peleado.
     */
    public static String codigoPara(Jugador jugador, Random azar) {
        Idioma idioma = jugador == null || jugador.idioma == null ? Idioma.ES : jugador.idioma;
        if (jugador != null && jugador.haPeleado()) {
            String codigo = jugador.ultimoRivalCodigo;
            if (codigo == null || codigo.isBlank()) {
                Registro ultimo = jugador.historial.get(jugador.historial.size() - 1);
                codigo = Rivales.codigoParaModo(ultimo.modo);
            }
            return TraductorDeApi.traducir(codigo, idioma);
        }
        return comentarioDeSparring(idioma) + "\n" + TraductorDeApi.traducir(Rivales.codigoAlAzar(azar), idioma);
    }

    /** El texto de ayuda superior según si ya ha peleado contra algún rival o está viendo un sparring. */
    public static String ayudaPara(Jugador jugador) {
        Idioma idioma = jugador != null && jugador.idioma != null ? jugador.idioma : T.idioma;
        return ayudaPara(jugador, idioma);
    }

    /** El texto de ayuda superior en el idioma indicado (pasado + nombre/minibiografía absurda si ya peleó; aviso simple si es sparring). */
    public static String ayudaPara(Jugador jugador, Idioma idioma) {
        Idioma id = idioma == null ? Idioma.ES : idioma;
        if (jugador != null && jugador.haPeleado()) {
            RivalesAbsurdos.Perfil perfil = RivalesAbsurdos.para(jugador);
            return T.en("rival.ayuda_ultimo", id)
                    .replace("{0}", perfil.nombre)
                    .replace("{1}", perfil.biografia(id));
        }
        return T.en("rival.ayuda_sparring", id);
    }

    @Override
    public void alEntrar(Juego j) {
        idioma = j.jugador == null || j.jugador.idioma == null ? Idioma.ES : j.jugador.idioma;
        perfil = j.jugador != null && j.jugador.haPeleado() ? RivalesAbsurdos.para(j.jugador) : null;
        ayuda = ayudaPara(j.jugador, idioma);
        editor.palabras = Palabras.api(idioma);
        editor.documentacion = w -> Referencia.de(w, idioma);
        editor.ponerTexto(codigoPara(j.jugador, new Random()));
        editor.soloLectura = true;
        editor.tamano = j.jugador == null ? 18 : j.jugador.tamanoDeLetra;
        editor.alPedirAyuda = api::buscar;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Comun.fondo(j, 0);
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float arriba = Comun.cabecera(j, T.t("rival.titulo"), true);
        float w = p.ancho - 40;
        float cuerpoY = dibujarFichaRival(p, ui, 20, arriba + 10, w) + 12;
        float cuerpoH = Math.max(120, p.alto - cuerpoY - 20);
        editor.dibujar(ui, 20, cuerpoY, w - 440, cuerpoH);
        api.dibujar(ui, j.jugador.idioma, null, w - 400, cuerpoY, 420, cuerpoH);
    }

    /** Dibuja la tarjeta superior con el nombre y la biografía del último rival (o el aviso de sparring) con alto contraste. */
    private float dibujarFichaRival(Pincel p, Ui ui, float x, float y, float w) {
        float padX = 20, padY = 12, anchoTexto = w - padX * 2 - 8;
        if (perfil != null) {
            float tNombre = ui.letra * 1.1f;
            float tIntro = ui.letra * 0.88f;
            float tBio = ui.letra * 1.0f;
            String intro = T.en("rival.ayuda", idioma);
            String bio = perfil.biografia(idioma);
            float hCab = p.altoDeLinea(tNombre);
            float hIntro = p.altoDeParrafo(intro, anchoTexto, tIntro);
            float hBio = p.altoDeParrafo(bio, anchoTexto, tBio);
            float altoCaja = padY * 2 + hCab + 4 + hIntro + 6 + hBio;
            p.caja(x, y, w, altoCaja, 10, ui.tema.panel, 1);
            p.bordeRedondeado(x, y, w, altoCaja, 10, ui.tema.acento, 0.65f, 1.5f);
            p.rect(x + 2, y + 8, 4, altoCaja - 16, ui.tema.aviso, 1);
            float yy = y + padY;
            p.texto(perfil.nombre, x + padX, yy, tNombre, true, ui.tema.aviso, 1);
            yy += hCab + 4;
            yy += p.parrafo(intro, x + padX, yy, anchoTexto, tIntro, ui.tema.textoSuave, 1) + 6;
            p.parrafo(bio, x + padX, yy, anchoTexto, tBio, false, ui.tema.texto, 1);
            return y + altoCaja;
        }
        String texto = ayuda.isEmpty() ? T.t("rival.ayuda") : ayuda;
        float tTexto = ui.letra * 0.95f;
        float altoCaja = padY * 2 + p.altoDeParrafo(texto, anchoTexto, tTexto);
        p.caja(x, y, w, altoCaja, 10, ui.tema.panel, 1);
        p.bordeRedondeado(x, y, w, altoCaja, 10, ui.tema.borde, 1, 1.2f);
        p.parrafo(texto, x + padX, y + padY, anchoTexto, tTexto, ui.tema.texto, 1);
        return y + altoCaja;
    }
}
