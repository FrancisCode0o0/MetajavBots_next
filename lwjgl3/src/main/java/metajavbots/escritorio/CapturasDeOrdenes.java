package metajavbots.escritorio;

import java.util.Map;
import java.util.function.Supplier;

import metajavbots.Juego;
import metajavbots.ejercicios.Leccion;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.pantallas.Comun;
import metajavbots.pantallas.Encuentro;
import metajavbots.pantallas.PantallaLeccion;
import metajavbots.pantallas.PantallaPelea;
import metajavbots.pantallas.PrepararPelea;
import metajavbots.pantallas.Taller;

/**
 * CAPTURAS DE ÓRDENES — Lo nuevo de ORDENES_CLARAS («-Psolo=37»): las tres
 * reglas del tutorial 1, el dibujo del turno en sus tres pasos, el concepto
 * «Así obedece tu robot» del entrenador, la pelea con la plantilla cuando una
 * orden borra otra (aviso del entrenador, depurador y las dos líneas de la mira)
 * y el ejercicio de las dos órdenes para la cabeza. Aparte, porque Capturas está
 * en el límite de líneas.
 */
final class CapturasDeOrdenes {

    private CapturasDeOrdenes() {
    }

    static void registrar(Map<String, Supplier<Pantalla>> pantallas) {
        pantallas.put("37a_leccion_reglas", () -> new Leccion1(1, 0));
        // El dibujo del turno, quieto en cada uno de sus tres pasos (el reloj del dibujo se fija en la primera llamada).
        pantallas.put("37b_turno_paso1", () -> new Leccion1(2, 0.72f * 3.4f));
        pantallas.put("37c_turno_paso2", () -> new Leccion1(2, 1.25f * 3.4f));
        pantallas.put("37d_turno_paso3", () -> new Leccion1(2, 2.6f * 3.4f));
        pantallas.put("37e_concepto_obedece", () -> new Pantalla() {
            private final Taller taller = new Taller();
            private boolean abierto;

            @Override
            public void alEntrar(Juego j) {
                taller.alEntrar(j);
            }

            @Override
            public void fotograma(Juego j, float dt) {
                if (!abierto) {
                    abierto = true;
                    j.entrenadorFlotante.abrir(j, "como_obedece");
                }
                taller.fotograma(j, dt);
            }
        });
        pantallas.put("37f_pelea_orden_borrada", PeleaConLaPlantilla::new);
        pantallas.put("37g_ejercicio_dos_ordenes", () -> new Capturas.ProbarEjercicio(
                metajavbots.ejercicios.Banco.delJuego().ejercicio("j_dos_ordenes_cabeza"), "return mira + primera;"));
    }

    /** La primera lección en esa página; el dibujo animado, fijo en el instante dado. */
    private static final class Leccion1 implements Pantalla {
        private final PantallaLeccion pantalla;
        private final float reloj;
        private boolean primera = true;

        Leccion1(int pagina, float reloj) {
            Leccion l = null;
            for (Leccion x : Leccion.todas()) if (x.id.equals("l01_hola_arena")) l = x;
            this.pantalla = new PantallaLeccion(l, pagina);
            this.reloj = reloj;
        }

        @Override
        public void alEntrar(Juego j) {
            pantalla.alEntrar(j);
        }

        @Override
        public void fotograma(Juego j, float dt) {
            pantalla.fotograma(j, primera ? reloj : 0f);
            primera = false;
        }
    }

    /** La pelea con la plantilla del idioma: va deprisa hasta que una orden borra otra, y ahí se para. */
    private static final class PeleaConLaPlantilla implements Pantalla {
        private PantallaPelea pp;
        private String codigoAntes;
        private boolean parada;

        @Override
        public void alEntrar(Juego j) {
            codigoAntes = j.jugador.robot().codigo;
            j.jugador.robot().codigo = Comun.plantilla(j.jugador.idioma);
            Encuentro e = PrepararPelea.encuentro(j, "ADAPTADA");
            e.escenario = VistaDeArena.Escenario.LABORATORIO;
            pp = new PantallaPelea(e);
            pp.alEntrar(j);
            pp.abrirDepurador(2);
        }

        @Override
        public void alSalir(Juego j) {
            if (codigoAntes != null) j.jugador.robot().codigo = codigoAntes;
        }

        @Override
        public void fotograma(Juego j, float dt) {
            for (int i = 0; i < 40 && !parada; i++) {
                pp.fotograma(j, 1 / 20f);
                if (pp.tieneConflictoDeOrdenes()) {
                    parada = true;
                    pp.pausar(true);
                }
            }
            pp.fotograma(j, dt);
        }
    }
}
