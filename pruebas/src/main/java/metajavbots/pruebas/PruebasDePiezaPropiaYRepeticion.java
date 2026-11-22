package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.pantallas.Repeticion;
import metajavbots.piezas.DisenadorDePieza;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;

/**
 * Pruebas del Bloque 5A:
 * - «Diseña tu propia pieza» (compilación, límites, presupuesto, Vigilante, persistencia y combate real).
 * - Repetición con código al lado y exportación a GIF válido (cabecera GIF89a y tamaño > 0) sin pantalla.
 */
final class PruebasDePiezaPropiaYRepeticion {

    private PruebasDePiezaPropiaYRepeticion() {
    }

    static void apuntar() {
        apuntarPiezaPropiaValidaYTrilingue();
        apuntarPiezaPropiaLimitesYSeguridad();
        apuntarPiezaPropiaPersistenciaYCombate();
        apuntarRepeticionCodigoYGif();
    }

    private static void apuntarPiezaPropiaValidaYTrilingue() {
        caso("pieza propia: plantillas trilingües (ES, DE, EN) compilan y cumplen presupuesto", () -> {
            for (Idioma idm : Idioma.values()) {
                for (Pieza.Ranura r : new Pieza.Ranura[]{Pieza.Ranura.BRAZO, Pieza.Ranura.CABEZA, Pieza.Ranura.PIERNAS, Pieza.Ranura.NUCLEO}) {
                    String codigo = DisenadorDePieza.plantilla(idm, r);
                    DisenadorDePieza.Resultado res = DisenadorDePieza.compilarYValidar(codigo, idm);
                    cierto(res.valida, "plantilla " + idm + "/" + r + " debe ser válida: " + res.errores);
                    cierto(res.pieza != null && res.pieza.esPropia(), "crea pieza propia");
                    igual(r, res.pieza.ranura, "ranura coincide");
                    cierto(res.presupuesto > 0 && res.presupuesto <= DisenadorDePieza.MAX_PRESUPUESTO,
                            "presupuesto acotado: " + res.presupuesto);
                }
            }
        });
    }

    private static void apuntarPiezaPropiaLimitesYSeguridad() {
        caso("pieza propia: rechaza piezas rotas/exceso de presupuesto, bucles infinitos y API prohibida por Vigilante", () -> {
            String rota = "import metajavbots.api.PiezaPropia;\n"
                    + "public class Rota implements PiezaPropia {\n"
                    + "    public String nombre() { return \"SuperArma\"; }\n"
                    + "    public String ranura() { return \"BRAZO\"; }\n"
                    + "    public double dano() { return 16; }\n"
                    + "    public double alcance() { return 420; }\n"
                    + "    public double enfriamiento() { return 8; }\n"
                    + "    public double energia() { return 4; }\n"
                    + "    public double blindaje() { return 28; }\n"
                    + "    public double peso() { return 4; }\n"
                    + "}\n";
            DisenadorDePieza.Resultado resRota = DisenadorDePieza.compilarYValidar(rota, Idioma.ES);
            falso(resRota.valida, "pieza por encima de 100 pts debe rechazarse");
            cierto(resRota.presupuesto > DisenadorDePieza.MAX_PRESUPUESTO, "presupuesto supera 100: " + resRota.presupuesto);

            String insegura = "import metajavbots.api.PiezaPropia;\n"
                    + "public class Mala implements PiezaPropia {\n"
                    + "    public String nombre() { System.exit(0); return \"X\"; }\n"
                    + "    public String ranura() { return \"BRAZO\"; }\n"
                    + "}\n";
            DisenadorDePieza.Resultado resInsegura = DisenadorDePieza.compilarYValidar(insegura, Idioma.ES);
            falso(resInsegura.valida, "Vigilante debe bloquear System.exit en piezas propias");

            String bucle = "import metajavbots.api.PiezaPropia;\n"
                    + "public class Infinita implements PiezaPropia {\n"
                    + "    public String nombre() { while (true) {} }\n"
                    + "    public String ranura() { return \"BRAZO\"; }\n"
                    + "}\n";
            DisenadorDePieza.Resultado resBucle = DisenadorDePieza.compilarYValidar(bucle, Idioma.ES);
            falso(resBucle.valida, "Tope debe cortar bucles infinitos al leer la pieza");
        });
    }

    private static void apuntarPiezaPropiaPersistenciaYCombate() {
        caso("pieza propia: persiste en Guardado, se monta en el robot y pelea en combate real", () -> {
            Path temp = Files.createTempDirectory("mjb_pieza_propia");
            Path anterior = Guardado.carpeta;
            Guardado.carpeta = temp;
            try {
                String codigo = "import metajavbots.api.PiezaPropia;\n"
                        + "public class RayoPropio implements PiezaPropia {\n"
                        + "    @Override public String nombre() { return \"Rayo Test\"; }\n"
                        + "    @Override public String ranura() { return \"BRAZO\"; }\n"
                        + "    @Override public String estilo() { return \"LASER\"; }\n"
                        + "    @Override public double dano() { return 11; }\n"
                        + "    @Override public double alcance() { return 260; }\n"
                        + "    @Override public double enfriamiento() { return 20; }\n"
                        + "    @Override public double energia() { return 13; }\n"
                        + "    @Override public double blindaje() { return 8; }\n"
                        + "    @Override public double peso() { return 9; }\n"
                        + "}\n";
                DisenadorDePieza.Resultado res = DisenadorDePieza.compilarYValidar(codigo, Idioma.ES);
                cierto(res.valida && res.pieza != null, "pieza propia válida");

                Jugador j = new Jugador("TesterPieza");
                Jugador.Robot rob = new Jugador.Robot("MiBot", "cobalto");
                rob.piezas.put(Montaje.Hueco.IZQUIERDA, res.pieza.id);
                j.robots.add(rob);
                j.guardarPiezaPropia(res.pieza, codigo);
                Guardado.guardar(j);

                Jugador leido = Guardado.leer(Guardado.carpetaDe(j.nombre));
                cierto(leido.piezasPropiasCodigo.containsKey(res.pieza.id), "código persistido en disco");
                Montaje montaje = leido.montajeDe(leido.robot());
                Pieza equipada = montaje.pieza(Montaje.Hueco.IZQUIERDA);
                cierto(equipada != null && res.pieza.id.equals(equipada.id), "pieza propia montada tras recargar partida");
                igual(11.0, equipada.valor("dano"), "daño real de la pieza propia");

                Combatiente yo = new Combatiente("yo", montaje, PruebasDeLaPelea.de(PruebasDeAparatos.TiradorIzquierdo::new));
                Combatiente rival = PruebasDeLaPelea.con("el", Modelo.de("cobalto"), PruebasDeLaPelea.de(PruebasDeAparatos.Quieto::new));
                Pelea p = new Pelea(yo, rival, 91);
                p.empezarRonda();
                p.cuerpo(0).x = 300;
                p.cuerpo(0).y = 350;
                p.cuerpo(0).mira = 0;
                p.cuerpo(1).x = 450;
                p.cuerpo(1).y = 350;
                double blindajeAntes = p.cuerpo(1).blindaje();
                for (int k = 0; k < 25; k++) p.avanzar();
                cierto(p.cuerpo(1).blindaje() < blindajeAntes, "la pieza propia hace daño real en combate");
            } finally {
                Guardado.carpeta = anterior;
            }
        });
    }

    private static void apuntarRepeticionCodigoYGif() {
        caso("repetición: guarda código con traza de líneas y exporta GIF89a válido sin pantalla", () -> {
            String codigoRobot = "import metajavbots.api.Metajavbot;\n"
                    + "public class RepBot extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() {\n"
                    + "        apuntarHacia(0);\n"
                    + "        atacarIzquierda();\n"
                    + "        avanzar(10);\n"
                    + "    }\n"
                    + "}\n";
            RobotDelJugador rj = new RobotDelJugador();
            cierto(rj.compilar(codigoRobot, Locale.ROOT, true).bien, "compila robot con traza");
            Combatiente yo = new Combatiente("yo", new Montaje(Modelo.de("cobalto")), rj.fabricaSiListo());
            Combatiente rival = PruebasDeLaPelea.con("el", Modelo.de("cobalto"), PruebasDeLaPelea.de(PruebasDeAparatos.Quieto::new));
            Pelea p = new Pelea(yo, rival, 95);
            p.empezarRonda();
            p.cuerpo(0).x = 300;
            p.cuerpo(0).y = 350;
            p.cuerpo(1).x = 440;
            p.cuerpo(1).y = 350;

            Repeticion rep = new Repeticion();
            for (int k = 0; k < 45; k++) {
                p.avanzar();
                rep.grabar(p);
            }
            cierto(rep.hay(), "hay turnos grabados para repetición");
            rep.empezar(p, codigoRobot);
            cierto(rep.activa(), "repetición activa");
            cierto(rep.codigo().contains("atacarIzquierda"), "código asociado a la repetición");
            cierto(rep.turnosGrabados() > 0, "turnos grabados > 0");

            Path dir = Files.createTempDirectory("mjb_gif_test");
            File destino = dir.resolve("prueba_repeticion.gif").toFile();
            File creado = rep.exportarGif(p, destino);
            cierto(creado.exists() && creado.length() > 100, "archivo GIF creado y no vacío (" + creado.length() + " B)");
            byte[] bytes = Files.readAllBytes(creado.toPath());
            String cabecera = new String(bytes, 0, 6, StandardCharsets.US_ASCII);
            igual("GIF89a", cabecera, "cabecera GIF89a válida");
            rep.cerrar(p);
            falso(rep.activa(), "repetición cerrada limpiamente");
        });
    }
}
