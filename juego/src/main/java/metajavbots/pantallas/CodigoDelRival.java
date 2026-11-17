package metajavbots.pantallas;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.textos.T;

/** CÓDIGO DEL RIVAL — El código de la computadora, para leerlo (y copiarle lo que guste): está escrito con la misma API. */
public final class CodigoDelRival implements Pantalla {

    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final PanelDeApi api = new PanelDeApi();

    @Override
    public void alEntrar(Juego j) {
        String codigo = "// Rival.java";
        try (InputStream in = CodigoDelRival.class.getResourceAsStream("/codigo_rival/Rival.java")) {
            if (in != null) codigo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            codigo = "// " + e;
        }
        editor.ponerTexto(codigo);
        editor.soloLectura = true;
        editor.tamano = j.jugador == null ? 18 : j.jugador.tamanoDeLetra;
        editor.alPedirAyuda = api::buscar;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("rival.titulo"), true);
        float w = j.pincel.ancho - 40;
        j.pincel.parrafo(T.t("rival.ayuda"), 20, arriba + 12, w, j.ui.letra * 0.85f, j.ui.tema.textoSuave, 1);
        editor.dibujar(j.ui, 20, arriba + 60, w - 440, j.pincel.alto - arriba - 80);
        api.dibujar(j.ui, j.jugador.idioma, null, w - 400, arriba + 60, 420, j.pincel.alto - arriba - 80);
    }
}
