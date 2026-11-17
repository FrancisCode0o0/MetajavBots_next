package metajavbots.pantallas;

import java.util.Map;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.ejercicios.Laboratorio;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * UN RETO DEL LABORATORIO — A la izquierda, la clase que hay que probar (solo
 * lectura); a la derecha, las pruebas del jugador. «Ejecutar las pruebas»
 * dice cuáles pasan y cuántos mutantes caza; si pasan todas y caza a todos,
 * gana la pieza.
 */
public final class PantallaLaboratorio implements Pantalla {

    private final Laboratorio l;
    private final VentanaDeCodigo clase = new VentanaDeCodigo(), pruebas = new VentanaDeCodigo();
    private volatile Laboratorio.Resultado resultado;
    private volatile boolean probando;
    private boolean premiado;

    public PantallaLaboratorio(Laboratorio l) {
        this.l = l;
    }

    @Override
    public void alEntrar(Juego j) {
        if (pruebas.doc.largo() > 0) return;
        clase.ponerTexto(l.clase);
        clase.soloLectura = true;
        pruebas.ponerTexto(j.jugador.codigoDeEjercicios.getOrDefault("lab_" + l.id, l.plantilla));
        clase.tamano = pruebas.tamano = j.jugador.tamanoDeLetra;
        pruebas.alCompilar = () -> probar(j);
    }

    @Override
    public void alSalir(Juego j) {
        j.jugador.codigoDeEjercicios.put("lab_" + l.id, pruebas.doc.texto());
        j.guardar();
    }

    private void probar(Juego j) {
        if (probando || !Compilador.hayCompilador()) return;
        probando = true;
        String codigo = pruebas.doc.texto();
        j.sonidos.tocar("compilar", 0.5f);
        Thread h = new Thread(() -> {
            resultado = l.comprobar(codigo, j.jugador.idioma.locale());
            probando = false;
        }, "laboratorio");
        h.setDaemon(true);
        h.start();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Laboratorio.Resultado r = resultado;
        if (r != null && !probando && !premiado && r.todoBien()) {
            premiado = true;
            if (jug.laboratoriosHechos.add(l.id)) {
                j.sonidos.tocar("subida", 0.9f);
                j.sacudir(5, 0.5f);
            }
            j.guardar();
        }
        if (r != null && !probando && pruebas.marcas.isEmpty()) {
            for (Compilador.Fallo f : r.errores) {
                if (f.archivo.equals(Compilador.nombreDeClase(pruebas.doc.texto()))) {
                    pruebas.marcas.add(new VentanaDeCodigo.Marca((int) f.linea, (int) f.columna, Explicador.explicar(f, jug.etiqueta, jug.idioma), f.grave));
                }
            }
        }
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, l.titulo(T.idioma), true);
        float x = 20, y = arriba + 12, w = p.ancho - 40;
        float th = p.parrafo(l.enunciado.en(T.idioma), x, y, w, ui.letra * 0.85f, ui.tema.texto, 1);
        y += th + 12;
        float h = p.alto - y - 190;
        clase.dibujar(ui, x, y, w * 0.45f - 8, h);
        pruebas.dibujar(ui, x + w * 0.45f + 8, y, w * 0.55f - 8, h);
        float by = y + h + 12;
        if (ui.boton(probando ? T.t("ejercicio.comprobando") : T.t("laboratorio.ejecutar") + "  (Ctrl+S)", x, by, 330, 50, Ui.Estilo.PRINCIPAL,
                !probando && Compilador.hayCompilador(), null)) probar(j);
        if (r != null && !probando) {
            float ry = by + 62, rx = x;
            if (!r.errores.isEmpty() && r.pruebas.isEmpty()) {
                p.texto(T.f("ejercicio.no_compila", r.errores.size()), rx, ry, ui.letra, true, ui.tema.mal, 1);
            } else {
                for (Map.Entry<String, String> e : r.pruebas.entrySet()) {
                    boolean bien = e.getValue().isEmpty();
                    String s = (bien ? "✔ " : "✘ ") + e.getKey() + (bien ? "" : " — " + e.getValue());
                    p.texto(ui.recortarTexto(s, w * 0.5f, ui.letra * 0.8f, false), rx, ry, ui.letra * 0.8f, false, bien ? ui.tema.bien : ui.tema.mal, 1);
                    ry += ui.letra * 1.25f;
                }
                if (r.pruebas.size() < 3) p.texto(T.t("laboratorio.minimo"), x + w * 0.52f, by + 62, ui.letra * 0.85f, false, ui.tema.aviso, 1);
                int cazados = 0;
                for (boolean c : r.cazados) if (c) cazados++;
                if (r.pasanTodas()) {
                    p.texto(T.f("laboratorio.mutantes", cazados, l.mutantes.size()), x + w * 0.52f, by + 92, ui.letra, true,
                            cazados == l.mutantes.size() ? ui.tema.bien : ui.tema.aviso, 1);
                } else {
                    p.texto(T.t("laboratorio.no_pasan"), x + w * 0.52f, by + 92, ui.letra * 0.9f, false, ui.tema.mal, 1);
                }
                if (r.todoBien()) {
                    for (Pieza pz : CatalogoDePiezas.delJuego().todas()) {
                        if (pz.desbloqueo.equals("pruebas:" + l.id)) {
                            p.texto(T.f("final.botin", pz.nombre(T.idioma)), x + w * 0.52f, by + 128, ui.letra, true, 0xFFC857, 1);
                        }
                    }
                }
            }
        }
    }
}
