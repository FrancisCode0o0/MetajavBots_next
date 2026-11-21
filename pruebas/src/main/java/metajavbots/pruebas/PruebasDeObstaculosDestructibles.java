package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.List;

import metajavbots.api.Cosa;
import metajavbots.api.Metajavbot;
import metajavbots.api.MetajavbotDe;
import metajavbots.api.MetajavbotEn;
import metajavbots.combate.Arena;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Fabrica;
import metajavbots.combate.Pelea;
import metajavbots.combate.Suceso;
import metajavbots.pantalla.Efectos;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.sonido.Sintetizador;

/** Pruebas de los obstáculos destructibles de la arena (barriles y chimeneas) frente a trampas fijas. */
final class PruebasDeObstaculosDestructibles {

    private PruebasDeObstaculosDestructibles() {
    }

    /** Robot pasivo para controlar posiciones y disparos desde las pruebas. */
    public static class Quieto extends Metajavbot {
        @Override
        public void cadaTurno() {
            // No hace nada por sí solo.
        }
    }

    /** Robot que dispara con el brazo izquierdo cada turno. */
    public static class TiradorIzquierdo extends Metajavbot {
        @Override
        public void cadaTurno() {
            atacarIzquierda();
        }
    }

    /** Robot en alemán que registra cuántos obstáculos ve en hindernisse(). */
    public static class ObservadorDe extends MetajavbotDe {
        int vistos = -1;

        @Override
        public void jedenZug() {
            vistos = hindernisse().size();
        }
    }

    /** Robot en inglés que registra cuántos obstáculos ve en obstacles(). */
    public static class ObservadorEn extends MetajavbotEn {
        int vistos = -1;

        @Override
        public void onTick() {
            vistos = obstacles().size();
        }
    }

    static void apuntar() {
        apuntarClasificacion();
        apuntarProyectilYLaser();
        apuntarGolpeYChoque();
        apuntarEfectosYSonido();
    }

    private static Combatiente combatienteConArma(String nombre, Pieza.Arma arma, Fabrica fabrica) {
        Montaje m = new Montaje(Modelo.de("cobalto"));
        Pieza encontrada = null;
        for (Pieza p : CatalogoDePiezas.delJuego().de(Pieza.Ranura.BRAZO)) {
            if (p.arma() == arma && p.valor("dano") > 0) {
                encontrada = p;
                break;
            }
        }
        cierto(encontrada != null, "existe brazo con arma " + arma);
        cierto(m.poner(Montaje.Hueco.IZQUIERDA, encontrada), "se monta el arma " + arma);
        return new Combatiente(nombre, m, fabrica);
    }

    private static Arena.Trampa primeraDeTipo(Arena arena, String tipo) {
        for (Arena.Trampa t : arena.trampas()) {
            if (t.tipo.equals(tipo)) return t;
        }
        throw new AssertionError("no se encontró trampa de tipo " + tipo);
    }

    private static boolean contiene(List<? extends Cosa> lista, Arena.Trampa trampa) {
        for (Cosa c : lista) {
            if (c.tipo().equals(trampa.tipo) && Math.hypot(c.x() - trampa.x, c.y() - trampa.y) < 1e-3) return true;
        }
        return false;
    }

    private static boolean huboEventoRoto(Pelea pelea, String tipo) {
        for (Suceso s : pelea.historia()) {
            if (s.tipo == Suceso.Tipo.OBSTACULO_ROTO && s.texto.equals(tipo)) return true;
        }
        return false;
    }

    private static void apuntarClasificacion() {
        caso("arena destructible: solo barriles y chimeneas son destructibles; trampas fijas siguen intactas", () -> {
            for (String escenario : new String[]{"CHATARRERIA", "COCINA", "HIELO", "TEJADO"}) {
                Arena arena = Arena.de(escenario, 1000, 700);
                for (Arena.Trampa t : arena.trampas()) {
                    boolean esperado = t.tipo.equals("barril") || t.tipo.equals("chimenea");
                    igual(esperado, t.destructible, escenario + " -> " + t.tipo + " destructible");
                    cierto(t.activa() && !t.destruida(), escenario + " -> " + t.tipo + " empieza activo");
                    if (esperado) {
                        cierto(t.resistenciaMax > 0 && t.resistencia() == t.resistenciaMax, "resistencia inicial positiva");
                    } else {
                        igual(0.0, t.resistenciaMax, "trampa fija sin resistencia destructible");
                    }
                }
            }
        });
    }

    private static void apuntarProyectilYLaser() {
        caso("arena destructible: proyectiles y láser destruyen obstáculos y desaparecen de obstaculos()/hindernisse()/obstacles()", () -> {
            ObservadorDe[] obsDe = new ObservadorDe[1];
            ObservadorEn[] obsEn = new ObservadorEn[1];
            Pelea pBala = new Pelea(
                    new Combatiente[]{
                            combatienteConArma("tirador", Pieza.Arma.BALA, PruebasDeLaPelea.de(TiradorIzquierdo::new)),
                            PruebasDeLaPelea.con("de", Modelo.de("cobalto"), PruebasDeLaPelea.de(() -> {
                                obsDe[0] = new ObservadorDe();
                                return obsDe[0];
                            })),
                            PruebasDeLaPelea.con("en", Modelo.de("cobalto"), PruebasDeLaPelea.de(() -> {
                                obsEn[0] = new ObservadorEn();
                                return obsEn[0];
                            }))
                    },
                    new int[]{0, 1, 1}, 101);
            pBala.arena = Arena.de("CHATARRERIA", pBala.ancho, pBala.alto);
            pBala.empezarRonda();
            Arena.Trampa barril = primeraDeTipo(pBala.arena, "barril");
            Arena.Trampa pinchos = primeraDeTipo(pBala.arena, "pinchos");
            Arena.Trampa iman = primeraDeTipo(pBala.arena, "iman");

            Cuerpo yo = pBala.cuerpo(0), cDe = pBala.cuerpo(1), cEn = pBala.cuerpo(2);
            yo.x = barril.x - 140;
            yo.y = barril.y;
            yo.mira = 0;
            cDe.x = 900;
            cDe.y = 600;
            cEn.x = 900;
            cEn.y = 100;
            int totalAntes = yo.obstaculos().size();
            cierto(contiene(yo.obstaculos(), barril), "el barril aparece inicialmente en obstaculos()");

            for (int t = 0; t < 200 && !barril.destruida(); t++) {
                yo.x = barril.x - 140;
                yo.y = barril.y;
                yo.mira = 0;
                pBala.avanzar();
            }
            cierto(barril.destruida() && !barril.activa(), "el barril se destruye por proyectiles");
            igual(0.0, barril.resistencia(), "resistencia a 0 tras destruirse");
            cierto(huboEventoRoto(pBala, "barril"), "se emite OBSTACULO_ROTO para barril");
            falso(contiene(yo.obstaculos(), barril), "el barril destruido desaparece de obstaculos()");
            pBala.avanzar();
            igual(totalAntes - 1, obsDe[0].vistos, "hindernisse() ya no devuelve el barril destruido");
            igual(totalAntes - 1, obsEn[0].vistos, "obstacles() ya no devuelve el barril destruido");
            cierto(pinchos.activa() && iman.activa(), "pinchos e imán siguen intactos");

            // Láser contra chimenea en TEJADO.
            Pelea pLaser = new Pelea(
                    combatienteConArma("laser", Pieza.Arma.LASER, PruebasDeLaPelea.de(TiradorIzquierdo::new)),
                    PruebasDeLaPelea.con("quieto", Modelo.de("cobalto"), PruebasDeLaPelea.de(Quieto::new)),
                    102);
            pLaser.arena = Arena.de("TEJADO", pLaser.ancho, pLaser.alto);
            pLaser.empezarRonda();
            Arena.Trampa chimenea = primeraDeTipo(pLaser.arena, "chimenea");
            Arena.Trampa rayo = primeraDeTipo(pLaser.arena, "rayo");
            Cuerpo tirador = pLaser.cuerpo(0), rival = pLaser.cuerpo(1);
            rival.x = 900;
            rival.y = 600;
            cierto(contiene(tirador.obstaculos(), chimenea), "la chimenea aparece inicialmente en obstaculos()");

            for (int t = 0; t < 220 && !chimenea.destruida(); t++) {
                tirador.x = chimenea.x - 180;
                tirador.y = chimenea.y;
                tirador.mira = 0;
                pLaser.avanzar();
            }
            cierto(chimenea.destruida(), "la chimenea se destruye por láser");
            cierto(huboEventoRoto(pLaser, "chimenea"), "se emite OBSTACULO_ROTO para chimenea");
            falso(contiene(tirador.obstaculos(), chimenea), "la chimenea destruida desaparece de obstaculos()");
            cierto(rayo.activa() && !rayo.destruida(), "el rayo sigue intacto");
        });
    }

    private static void apuntarGolpeYChoque() {
        caso("arena destructible: golpe cuerpo a cuerpo y choque destruyen obstáculos y dejan de colisionar", () -> {
            // Destrucción por golpe cuerpo a cuerpo en HIELO.
            Pelea pGolpe = new Pelea(
                    combatienteConArma("pegador", Pieza.Arma.CUERPO, PruebasDeLaPelea.de(TiradorIzquierdo::new)),
                    PruebasDeLaPelea.con("quieto", Modelo.de("cobalto"), PruebasDeLaPelea.de(Quieto::new)),
                    103);
            pGolpe.arena = Arena.de("HIELO", pGolpe.ancho, pGolpe.alto);
            pGolpe.empezarRonda();
            Arena.Trampa barril = primeraDeTipo(pGolpe.arena, "barril");
            Arena.Trampa foso = primeraDeTipo(pGolpe.arena, "foso");
            Cuerpo yo = pGolpe.cuerpo(0), rival = pGolpe.cuerpo(1);
            rival.x = 900;
            rival.y = 600;

            for (int t = 0; t < 220 && !barril.destruida(); t++) {
                yo.x = barril.x - barril.radio - yo.radio - 10;
                yo.y = barril.y;
                yo.mira = 0;
                pGolpe.avanzar();
            }
            cierto(barril.destruida(), "el barril se destruye con golpes cuerpo a cuerpo");
            falso(contiene(yo.obstaculos(), barril), "el barril golpeado desaparece de obstaculos()");
            cierto(foso.activa() && !foso.destruida(), "el foso sigue intacto");

            // Destrucción por choque de robot en HIELO y verificación de que deja de colisionar.
            Pelea pChoque = new Pelea(
                    PruebasDeLaPelea.con("embestidor", Modelo.de("mazo"), PruebasDeLaPelea.de(Quieto::new)),
                    PruebasDeLaPelea.con("quieto", Modelo.de("cobalto"), PruebasDeLaPelea.de(Quieto::new)),
                    104);
            pChoque.arena = Arena.de("HIELO", pChoque.ancho, pChoque.alto);
            pChoque.empezarRonda();
            Arena.Trampa barrilChoque = primeraDeTipo(pChoque.arena, "barril");
            Cuerpo ariete = pChoque.cuerpo(0);
            pChoque.cuerpo(1).x = 900;
            pChoque.cuerpo(1).y = 600;

            for (int t = 0; t < 200 && !barrilChoque.destruida(); t++) {
                if (t % 22 == 0) {
                    ariete.x = barrilChoque.x - barrilChoque.radio - ariete.radio + 4;
                    ariete.y = barrilChoque.y;
                    ariete.rumbo = 0;
                    ariete.velocidad = 5.0;
                    ariete.avanzar(40);
                }
                pChoque.avanzar();
            }
            cierto(barrilChoque.destruida(), "el barril se destruye tras choques de robot");
            falso(contiene(ariete.obstaculos(), barrilChoque), "el barril chocado desaparece de obstaculos()");

            // Al colocarse justo en el centro del barril destruido, ya no lo empuja fuera.
            ariete.x = barrilChoque.x;
            ariete.y = barrilChoque.y;
            ariete.velocidad = 0;
            ariete.detener();
            pChoque.avanzar();
            cierto(Math.hypot(ariete.x - barrilChoque.x, ariete.y - barrilChoque.y) < 1.0, "el obstáculo destruido ya no colisiona");
        });
    }

    private static void apuntarEfectosYSonido() {
        caso("arena destructible: efectos de escombros y sonido sintetizado obstaculo_roto", () -> {
            Efectos efectos = new Efectos();
            efectos.escombros(200, 200, 0x9A4A22, 0xFFC857);
            cierto(efectos.vivas() >= 20, "escombros genera partículas de rotura");

            boolean enCatalogo = false;
            for (String n : Sintetizador.NOMBRES) {
                if (n.equals("obstaculo_roto")) enCatalogo = true;
            }
            cierto(enCatalogo, "obstaculo_roto figura en Sintetizador.NOMBRES");
            float[] muestras = Sintetizador.hacer("obstaculo_roto", 7);
            cierto(muestras != null && muestras.length > 1000, "Sintetizador genera muestras para obstaculo_roto");
        });
    }
}
