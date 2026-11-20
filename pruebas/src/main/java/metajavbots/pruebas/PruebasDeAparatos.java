package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import metajavbots.api.Metajavbot;
import metajavbots.combate.Aparatos;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.combate.Proyectil;
import metajavbots.piezas.Modelo;

/** Pruebas de los aparatos recogibles nuevos (imán y martillo) y sus fichas trilingües. */
final class PruebasDeAparatos {

    private PruebasDeAparatos() {
    }

    /** Robot pasivo para controlar posiciones y activación desde las pruebas. */
    public static class Quieto extends Metajavbot {
        @Override
        public void cadaTurno() {
            // No hace nada por sí solo.
        }
    }

    /** Robot que dispara con el brazo izquierdo para generar proyectiles reales en la prueba del imán. */
    public static class TiradorIzquierdo extends Metajavbot {
        @Override
        public void cadaTurno() {
            atacarIzquierda();
        }
    }

    static void apuntar() {
        apuntarIman();
        apuntarMartillo();
        apuntarFichasTrilingues();
    }

    private static void apuntarIman() {
        caso("aparatos: imán aparece, conecta solo en cabeza, atrae rival, desvía proyectiles y recarga", () -> {
            Modelo cobalto = Modelo.de("cobalto");
            Pelea p = new Pelea(
                    PruebasDeLaPelea.con("yo", cobalto, PruebasDeLaPelea.de(Quieto::new)),
                    PruebasDeLaPelea.con("el", cobalto, PruebasDeLaPelea.de(TiradorIzquierdo::new)),
                    71);
            p.aparatos.activos = true;
            p.aparatos.fijarSiguiente(Aparatos.Tipo.IMAN);
            igual(Aparatos.Tipo.IMAN, p.aparatos.siguiente(), "el siguiente es el imán");
            p.empezarRonda();
            while (p.turno() < Aparatos.SALE) p.avanzar();
            igual(1, p.aparatos.todos().size(), "aparece el imán en la arena");
            Aparatos.Uno a = p.aparatos.todos().get(0);
            igual(Aparatos.Tipo.IMAN, a.tipo, "tipo imán");

            Cuerpo yo = p.cuerpo(0), el = p.cuerpo(1);
            yo.x = a.x;
            yo.y = a.y;
            p.avanzar();
            igual(0, a.dueno, "el robot recoge el imán al tocarlo");
            igual("iman", yo.miAparato().tipo(), "tipo visible en miAparato()");
            falso(yo.conectar("brazo"), "el imán no engancha en brazo");
            falso(yo.conectar("espalda"), "el imán no engancha en espalda");
            cierto(yo.conectar("head"), "el imán engancha en cabeza (en inglés)");
            falso(yo.miAparato().conectado(), "mientras monta aún no está conectado");
            for (int k = 0; k < Aparatos.MONTAR; k++) p.avanzar();
            cierto(yo.miAparato().conectado() && yo.miAparato().listo(), "imán montado y listo");

            yo.x = 300;
            yo.y = 350;
            yo.mira = 0;
            el.x = 500;
            el.y = 350;
            el.mira = 180;
            // Avanza hasta que el rival dispare un proyectil hacia la izquierda (180°).
            for (int k = 0; k < 20 && p.proyectiles.isEmpty(); k++) p.avanzar();
            cierto(!p.proyectiles.isEmpty(), "hay proyectil enemigo en vuelo");
            Proyectil pr = p.proyectiles.get(0);
            pr.x = 420;
            pr.y = 350;
            pr.vx = -pr.velocidad;
            pr.vy = 0;
            double distAntes = Math.hypot(el.x - yo.x, el.y - yo.y);

            cierto(yo.usarAparato(true, 0), "el imán se activa hacia 0°");
            double distDespues = Math.hypot(el.x - yo.x, el.y - yo.y);
            cierto(distDespues < distAntes - 40, "el imán atrae al rival (" + distAntes + " -> " + distDespues + ")");
            cierto(pr.vx > 0 && Math.abs(Pelea.normal(pr.angulo())) < 1e-6, "el imán desvía el proyectil hacia 0°");
            falso(yo.miAparato().listo(), "entra en recarga");
            falso(yo.usarAparato(true, 0), "no puede usarse mientras recarga");

            for (int k = 0; k < Aparatos.Tipo.IMAN.recarga; k++) p.avanzar();
            cierto(yo.miAparato().listo(), "tras la recarga vuelve a estar listo");
            cierto(yo.usarAparato(true, 0), "se puede volver a activar");
        });
    }

    private static void apuntarMartillo() {
        caso("aparatos: martillo aparece, conecta solo en brazo, daña, empuja, aturde y recarga", () -> {
            Modelo cobalto = Modelo.de("cobalto");
            Pelea p = new Pelea(
                    PruebasDeLaPelea.con("yo", cobalto, PruebasDeLaPelea.de(Quieto::new)),
                    PruebasDeLaPelea.con("el", cobalto, PruebasDeLaPelea.de(Quieto::new)),
                    72);
            p.aparatos.activos = true;
            p.aparatos.fijarSiguiente(Aparatos.Tipo.MARTILLO);
            igual(Aparatos.Tipo.MARTILLO, p.aparatos.siguiente(), "el siguiente es el martillo");
            p.empezarRonda();
            while (p.turno() < Aparatos.SALE) p.avanzar();
            igual(1, p.aparatos.todos().size(), "aparece el martillo en la arena");
            Aparatos.Uno a = p.aparatos.todos().get(0);
            igual(Aparatos.Tipo.MARTILLO, a.tipo, "tipo martillo");

            Cuerpo yo = p.cuerpo(0), el = p.cuerpo(1);
            yo.x = a.x;
            yo.y = a.y;
            p.avanzar();
            igual(0, a.dueno, "el robot recoge el martillo al tocarlo");
            igual("martillo", yo.miAparato().tipo(), "tipo visible en miAparato()");
            falso(yo.conectar("cabeza"), "el martillo no engancha en cabeza");
            falso(yo.conectar("piernas"), "el martillo no engancha en piernas");
            cierto(yo.conectar("arm"), "el martillo engancha en brazo (en inglés/alemán)");
            falso(yo.miAparato().conectado(), "mientras monta aún no está conectado");
            for (int k = 0; k < Aparatos.MONTAR; k++) p.avanzar();
            cierto(yo.miAparato().conectado() && yo.miAparato().listo(), "martillo montado y listo");

            yo.x = 300;
            yo.y = 350;
            el.x = 390;
            el.y = 350;
            double blindajeAntes = el.blindaje();
            double distAntes = Math.hypot(el.x - yo.x, el.y - yo.y);

            cierto(yo.usarAparato(true, 0), "el martillo golpea hacia 0°");
            double distDespues = Math.hypot(el.x - yo.x, el.y - yo.y);
            cierto(el.blindaje() < blindajeAntes, "el martillo hace daño");
            cierto(distDespues > distAntes + 40, "el martillo empuja al rival (" + distAntes + " -> " + distDespues + ")");
            cierto(el.aturdido >= 20, "el martillo aturde al rival: " + el.aturdido);
            falso(yo.miAparato().listo(), "entra en recarga");
            falso(yo.usarAparato(true, 0), "no puede usarse mientras recarga");

            for (int k = 0; k < Aparatos.Tipo.MARTILLO.recarga; k++) p.avanzar();
            cierto(yo.miAparato().listo(), "tras la recarga vuelve a estar listo");
            cierto(yo.usarAparato(true, 0), "se puede volver a activar");
        });
    }

    private static void apuntarFichasTrilingues() {
        caso("aparatos: todos los tipos tienen ficha trilingüe (es, de, en) con hueco y uso", () -> {
            cierto(Aparatos.Tipo.values().length >= 8, "al menos 8 aparatos en el catálogo");
            for (Aparatos.Tipo t : Aparatos.Tipo.values()) {
                String ruta = "/aparatos/" + t.id() + ".txt";
                String contenido;
                try (InputStream in = PruebasDeAparatos.class.getResourceAsStream(ruta)) {
                    cierto(in != null, "existe la ficha " + ruta);
                    contenido = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                for (String idioma : new String[]{"es", "de", "en"}) {
                    String marca = "== " + idioma + "\n";
                    int ini = contenido.indexOf(marca);
                    cierto(ini >= 0, "sección " + idioma + " en " + ruta);
                    int fin = contenido.indexOf("\n== ", ini + marca.length());
                    String bloque = contenido.substring(ini + marca.length(), fin < 0 ? contenido.length() : fin + 1);
                    cierto(bloque.contains("return \"" + t.id() + "\";"), "id en " + ruta + " (" + idioma + ")");
                    String conectar = idioma.equals("es") ? "conectar(" : idioma.equals("de") ? "anschliessen(" : "plugIn(";
                    String usar = idioma.equals("es") ? "usarAparato(" : idioma.equals("de") ? "geraetBenutzen(" : "useGadget(";
                    cierto(bloque.contains(conectar), "indica hueco en " + ruta + " (" + idioma + ")");
                    cierto(bloque.contains(usar), "indica uso en " + ruta + " (" + idioma + ")");
                }
            }
        });
    }
}
