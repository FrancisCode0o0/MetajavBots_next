package metajavbots.escritorio;

import java.util.List;
import java.util.Random;

import metajavbots.Juego;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.pantallas.Comun;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.rivales.Rivales;

/**
 * CAPTURAS DE ARENA: los escenarios de las capturas que no son una pantalla del juego, sino una foto preparada
 * (los modelos en fila, las armas montadas y una pelea a medias en cada escenario). Van aparte para que
 * Capturas.java no pase del límite de líneas.
 */
final class CapturasDeArena {

    private CapturasDeArena() {
    }

    /** Los dieciséis modelos de pie, en fila. */
    static final class Escaparate implements Pantalla {
        private VistaDeArena vista;
        private float t;

        @Override
        public void fotograma(Juego j, float dt) {
            if (vista == null) vista = new VistaDeArena(j.pincel);
            t += dt;
            Pincel p = j.pincel;
            Comun.fondo(j, t);
            List<Modelo> ms = Modelo.todos();
            int porFila = 8, filas = (ms.size() + porFila - 1) / porFila;
            float alto = (p.alto - 40) / filas;
            for (int i = 0; i < ms.size(); i++) {
                float x = 100 + (i % porFila) * ((p.ancho - 200) / (porFila - 1));
                float y = 20 + alto * (i / porFila) + alto - 40;
                vista.robotSuelto(p, new Montaje(ms.get(i)), x, y, 2.4f * Math.min(1, alto / 410) * 1.4f, t + i, 0, i % 2 == 0);
                p.textoCentrado(ms.get(i).nombre.es, x, y + 10, 18, true, 0xFFFFFF, 1);
            }
        }
    }

    /** Las armas de las luchas de robots de verdad, montadas: arriba en reposo, abajo a medio golpe. */
    static final class Armeria implements Pantalla {
        private static final String[] ARMAS = {"sierra", "pinza", "volteador", "cuna", "bola_cadena", "sierra_volteadora", "pinza_taladro", "martillo"};
        private VistaDeArena vista;
        private float t;

        @Override
        public void fotograma(Juego j, float dt) {
            if (vista == null) vista = new VistaDeArena(j.pincel);
            t += dt;
            Pincel p = j.pincel;
            Comun.fondo(j, t);
            metajavbots.piezas.CatalogoDePiezas cat = metajavbots.piezas.CatalogoDePiezas.delJuego();
            for (int i = 0; i < ARMAS.length; i++) {
                float x = 110 + i * ((p.ancho - 220) / (ARMAS.length - 1));
                for (int fila = 0; fila < 2; fila++) {
                    Montaje m = new Montaje(Modelo.todos().get(i * 2 % Modelo.todos().size()));
                    metajavbots.piezas.Pieza pz = cat.pieza(ARMAS[i]);
                    if (pz == null) pz = cat.pieza(ARMAS[i] + "_1");
                    if (pz != null) m.poner(Montaje.Hueco.DERECHA, pz);
                    metajavbots.pantalla.DibujoRobot.Estado e = new metajavbots.pantalla.DibujoRobot.Estado();
                    e.tiempo = t;
                    if (fila == 1) {
                        e.brazoQueAtaca = 1;
                        e.faseDelAtaque = 0.2f;
                    }
                    vista.robotConEstado(p, m, x, 380 + fila * 380, 2.4f, e);
                    if (fila == 0) p.textoCentrado(pz == null ? "?" + ARMAS[i] : pz.nombre(metajavbots.textos.Idioma.ES), x, 400, 18, true, 0xFFFFFF, 1);
                }
            }
        }
    }

    /** Una pelea a medias en un escenario (dos rivales de la computadora). */
    static final class Arena implements Pantalla {
        private final VistaDeArena.Escenario escenario;
        private VistaDeArena vista;
        private Pelea pelea;
        /** Con aparatos: 0 sin, 1 recién salido en el suelo, 2 montado y en marcha. */
        private int aparatos;

        Arena(VistaDeArena.Escenario e) {
            escenario = e;
        }

        Arena(VistaDeArena.Escenario e, int aparatos) {
            escenario = e;
            this.aparatos = aparatos;
        }

        /** Hasta que sale el aparato; con 2, el primero se lo pone y lo enciende. */
        private void prepararAparatos() {
            pelea.aparatos.activos = true;
            while (pelea.turno() <= metajavbots.combate.Aparatos.SALE) pelea.avanzar();
            if (aparatos < 2 || pelea.aparatos.todos().isEmpty()) return;
            metajavbots.combate.Aparatos.Uno a = pelea.aparatos.todos().get(0);
            metajavbots.combate.Cuerpo c = pelea.cuerpo(0);
            c.x = a.x;
            c.y = a.y;
            pelea.avanzar();
            c.conectar(a.tipo.hueco);
            for (int i = 0; i <= metajavbots.combate.Aparatos.MONTAR; i++) pelea.avanzar();
            c.usarAparato(false, 0);
            for (int i = 0; i < 6; i++) pelea.avanzar();
        }

        @Override
        public void fotograma(Juego j, float dt) {
            if (vista == null) {
                vista = new VistaDeArena(j.pincel);
                vista.escenario = escenario;
                Random r = new Random(escenario.ordinal());
                List<Modelo> ms = Modelo.todos();
                pelea = new Pelea(Rivales.crear(ms.get(r.nextInt(ms.size())), 0.6, 0.5, r), Rivales.crear(ms.get(r.nextInt(ms.size())), 0.6, 0.5, r),
                        r.nextLong());
                pelea.arena = metajavbots.combate.Terrenos.arenaDePelea(escenario.name(), pelea.ancho, pelea.alto);
                pelea.empezarRonda();
                if (aparatos > 0) prepararAparatos();
                else for (int i = 0; i < 160; i++) pelea.avanzar();
            }
            if (aparatos != 1) pelea.avanzar();
            vista.sucesos(pelea, null);
            vista.dibujar(j.pincel, pelea, 0, 0, j.pincel.ancho, j.pincel.alto, dt);
        }
    }
}
