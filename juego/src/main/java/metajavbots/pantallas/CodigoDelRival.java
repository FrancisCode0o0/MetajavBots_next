package metajavbots.pantallas;

import java.util.Random;

import metajavbots.Juego;
import metajavbots.codigo.TraductorDeApi;
import metajavbots.pantalla.Pantalla;
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
        if (jugador != null && jugador.haPeleado()) {
            String nombre = jugador.ultimoRivalNombre;
            if ((nombre == null || nombre.isBlank()) && !jugador.historial.isEmpty()) {
                nombre = jugador.historial.get(jugador.historial.size() - 1).rival;
            }
            return nombre != null && !nombre.isBlank() ? T.f("rival.ayuda_ultimo", nombre) : T.t("rival.ayuda");
        }
        return T.t("rival.ayuda_sparring");
    }

    @Override
    public void alEntrar(Juego j) {
        Idioma idioma = j.jugador == null || j.jugador.idioma == null ? Idioma.ES : j.jugador.idioma;
        ayuda = ayudaPara(j.jugador);
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
        float arriba = Comun.cabecera(j, T.t("rival.titulo"), true);
        float w = j.pincel.ancho - 40;
        String texto = ayuda.isEmpty() ? T.t("rival.ayuda") : ayuda;
        j.pincel.parrafo(texto, 20, arriba + 12, w, j.ui.letra * 0.85f, j.ui.tema.textoSuave, 1);
        editor.dibujar(j.ui, 20, arriba + 60, w - 440, j.pincel.alto - arriba - 80);
        api.dibujar(j.ui, j.jugador.idioma, null, w - 400, arriba + 60, 420, j.pincel.alto - arriba - 80);
    }
}
