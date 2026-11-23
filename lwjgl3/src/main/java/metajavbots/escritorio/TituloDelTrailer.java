package metajavbots.escritorio;

import java.util.Random;

import metajavbots.Juego;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.rivales.Rivales;

/**
 * TÍTULO DEL TRÁILER — El cierre: una pelea que sigue detrás, en penumbra, el título MetajavBots, «Aprende Java programando
 * robots» en los tres idiomas, el enlace del repositorio y, abajo, los modelos de robot en fila, que van apareciendo.
 */
final class TituloDelTrailer implements Pantalla {

    static final String REPOSITORIO = "github.com/FrancisCode0o0/MetajavBots_next";
    private static final String[] LEMAS = {
        "Aprende Java programando robots", "Learn Java by programming robots", "Lerne Java, indem du Roboter programmierst"};
    private VistaDeArena vista;
    private Pelea pelea;
    private float t;

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        if (vista == null) {
            vista = new VistaDeArena(p);
            vista.escenario = VistaDeArena.Escenario.NEON;
            Random r = new Random(12);
            pelea = new Pelea(Rivales.crear(Modelo.de("filo"), 0.7, 0.7, r), Rivales.crear(Modelo.de("cobalto"), 0.7, 0.7, r), 12);
            pelea.empezarRonda();
            for (int i = 0; i < 60; i++) pelea.avanzar();
        }
        t += dt;
        if ((int) (t * Pelea.TURNOS_POR_SEGUNDO) > (int) ((t - dt) * Pelea.TURNOS_POR_SEGUNDO)) pelea.avanzar();
        vista.sucesos(pelea, null);
        vista.dibujar(p, pelea, 0, 0, p.ancho, p.alto, dt);
        p.rect(0, 0, p.ancho, p.alto, 0x05070C, 0.55f + 0.25f * aparece(0, 1.2f));
        float cx = p.ancho / 2;
        // Abajo, en fila, los modelos de robot, que van entrando de uno en uno.
        java.util.List<Modelo> ms = Modelo.todos();
        int n = Math.min(10, ms.size());
        for (int i = 0; i < n; i++) {
            float b = aparece(0.25f + 0.09f * i, 0.4f);
            if (b <= 0) continue;
            float x = cx + (i - (n - 1) / 2f) * 150;
            vista.robotSuelto(p, new Montaje(ms.get(i)), x, 880 + 40 * (1 - b), 1.25f, t + i, 0.6f, i < n / 2);
        }
        float a = aparece(0.1f, 0.7f);
        float tam = 120 + 14 * (1 - a);
        p.textoCentradoConBordeNegro("MetajavBots", cx, 300 - tam * 0.62f, tam, true, 0xFFC857, a);
        for (int i = 0; i < LEMAS.length; i++) {
            float b = aparece(1.0f + 0.55f * i, 0.5f);
            p.textoCentradoConBordeNegro(LEMAS[i], cx, 430 + 62 * i + 12 * (1 - b), 40, i == 0, i == 0 ? 0xFFFFFF : 0xC9D6E3, b);
        }
        float c = aparece(2.9f, 0.6f);
        p.caja(cx - 400, 660, 800, 64, 14, 0x0E1A2A, 0.85f * c);
        p.bordeRedondeado(cx - 400, 660, 800, 64, 14, 0x4FD1FF, 0.9f * c, 2);
        p.textoCentradoConBordeNegro(REPOSITORIO, cx, 672, 32, true, 0x8FE3FF, c);
    }

    /** De 0 a 1 entre «desde» y «desde + dura» segundos, suavizado. */
    private float aparece(float desde, float dura) {
        float x = Math.max(0, Math.min(1, (t - desde) / dura));
        return x * x * (3 - 2 * x);
    }
}
