package metajavbots.escritorio;

import java.util.Map;
import java.util.function.Supplier;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantallas.PantallaCarreraDeObstaculos;
import metajavbots.pantallas.PantallaMinijuegoDestruccion;

/**
 * CAPTURAS DE LA CARRERA — El botón de la API junto al editor en la Carrera de
 * obstáculos y en Destrucción y supervivencia, y la sombra de un robot que cae
 * del cielo en la carrera. Aparte, para no hacer crecer Capturas.
 */
final class CapturasDeCarrera {

    private CapturasDeCarrera() {
    }

    static void registrar(Map<String, Supplier<Pantalla>> pantallas) {
        pantallas.put("31a_carrera_api", PantallaCarreraDeObstaculos::conLaApiAbierta);
        pantallas.put("31b_destruccion_api", PantallaMinijuegoDestruccion::conLaApiAbierta);
        pantallas.put("31c_carrera_caida_del_cielo", CaidaEnLaCarrera::new);
    }

    /** La carrera ya en marcha, parada con la sombra de un robot del cielo en la pista. */
    static final class CaidaEnLaCarrera implements Pantalla {
        private final PantallaCarreraDeObstaculos carrera = new PantallaCarreraDeObstaculos();
        private boolean empezada;

        @Override
        public void alEntrar(Juego j) {
            carrera.alEntrar(j);
        }

        @Override
        public void fotograma(Juego j, float dt) {
            if (!empezada) {
                empezada = true;
                carrera.empezarConCaidaParaCapturas(j);
            }
            carrera.fotograma(j, dt);
        }
    }
}
