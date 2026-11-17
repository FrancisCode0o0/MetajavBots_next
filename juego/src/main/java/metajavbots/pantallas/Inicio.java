package metajavbots.pantallas;

import java.util.List;
import java.util.Random;

import metajavbots.Juego;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.pantalla.Pantalla;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * INICIO — La portada: el título, dos robots que se miran, el idioma y las
 * partidas guardadas (o una nueva). Una partida nueva pasa por la Bienvenida
 * (la prueba de nivel y el primer robot).
 */
public final class Inicio implements Pantalla {

    private float t;
    private final CampoDeTexto nombre = new CampoDeTexto();
    private List<String> partidas;
    private Montaje izquierdo, derecho;
    private VistaDeArena vista;
    private String aviso = "";
    private final Ui.Desplazable lista = new Ui.Desplazable();

    @Override
    public void alEntrar(Juego j) {
        partidas = Guardado.partidas();
        Random r = new Random();
        List<Modelo> m = Modelo.todos();
        izquierdo = new Montaje(m.get(r.nextInt(m.size())));
        derecho = new Montaje(m.get(r.nextInt(m.size())));
        if (vista == null) vista = new VistaDeArena(j.pincel);
        nombre.enfocado = partidas.isEmpty();
        nombre.maximo = 30;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, t);
        // El título, con brillo.
        float cx = p.ancho / 2;
        p.brillo(cx, 140, 260, ui.tema.acento, 0.12f);
        p.textoCentrado("METAJAVBOTS", cx + 3, 84, 86, true, 0x000000, 0.4f);
        p.textoCentrado("METAJAVBOTS", cx, 80, 86, true, ui.tema.texto, 1);
        p.textoCentrado(T.t("inicio.lema"), cx, 192, ui.letra * 1.1f, false, ui.tema.textoSuave, 1);
        // Los dos robots, a los lados.
        vista.robotSuelto(p, izquierdo, p.ancho * 0.17f, p.alto * 0.78f, 3.2f, t, 0, true);
        vista.robotSuelto(p, derecho, p.ancho * 0.83f, p.alto * 0.78f, 3.2f, t + 1.3f, 0, false);
        // El idioma.
        Idioma[] idiomas = Idioma.values();
        for (int i = 0; i < idiomas.length; i++) {
            Idioma id = idiomas[i];
            if (ui.boton(id.nombre, p.ancho - 3 * 150 - 20 + i * 150, 20, 140, 44, T.idioma == id ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                T.idioma = id;
            }
        }
        // Las partidas y la nueva.
        float w = 560, x = cx - w / 2, y = 250;
        float cy = ui.panel(x, y, w, p.alto - y - 70, T.t("inicio.partidas"));
        float alto = 54;
        float zona = p.alto - y - 70 - (cy - y) - 150;
        float desp = ui.desplazable(lista, x + 12, cy, w - 24, zona);
        float yy = cy - desp;
        for (String nombreDePartida : partidas) {
            if (ui.boton("▶  " + nombreDePartida.replace('_', ' '), x + 16, yy, w - 44, alto - 8)) abrir(j, nombreDePartida);
            yy += alto;
        }
        if (partidas.isEmpty()) p.texto(T.t("inicio.sin_partidas"), x + 20, yy + 10, ui.letra, false, ui.tema.textoSuave, 1);
        ui.finDesplazable(lista, Math.max(alto, partidas.size() * alto));
        float ny = cy + zona + 16;
        p.texto(T.t("inicio.nueva"), x + 16, ny, ui.letra, true, ui.tema.texto, 1);
        boolean intro = nombre.dibujar(ui, x + 16, ny + 32, w - 230, 50, T.t("inicio.tu_nombre"));
        if (ui.boton(T.t("inicio.crear"), x + w - 200, ny + 32, 184, 50, Ui.Estilo.PRINCIPAL) || intro) crear(j);
        if (!aviso.isEmpty()) p.texto(aviso, x + 16, ny + 92, ui.letra * 0.85f, false, ui.tema.mal, 1);
        Comun.pie(j, T.t("inicio.pie"));
    }

    private void abrir(Juego j, String carpeta) {
        try {
            Jugador jug = Guardado.leer(carpeta);
            j.ponerJugador(jug);
            if (!jug.pruebaHecha || jug.robots.isEmpty()) j.irSolo(new Bienvenida());
            else j.irSolo(new Menu());
        } catch (Exception e) {
            aviso = T.t("inicio.no_se_abre") + " " + e.getMessage();
        }
    }

    private void crear(Juego j) {
        String n = nombre.texto().strip();
        if (n.isEmpty()) {
            aviso = T.t("inicio.falta_nombre");
            return;
        }
        if (partidas.contains(Guardado.carpetaDe(n))) {
            aviso = T.t("inicio.ya_existe");
            return;
        }
        Jugador jug = new Jugador(n);
        jug.idioma = T.idioma;
        j.ponerJugador(jug);
        j.guardar();
        j.irSolo(new Bienvenida());
    }
}
