package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.AtaqueLejos;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Leccion;
import metajavbots.pantallas.AvisoDeOrdenes;
import metajavbots.pantallas.Comun;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Muneco;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.textos.Trilingue;
import metajavbots.tutor.Concepto;

/**
 * Pruebas de ORDENES_CLARAS: que se entienda cómo obedece el robot. La plantilla con sus comentarios nuevos
 * compila en los tres idiomas; el aviso de orden borrada señala las dos líneas exactas y sale una vez por
 * pelea; el aviso de ataque con la mira lejos sale con la plantilla y no con un robot que espera a la mira; y
 * el tutorial 1, el concepto, la API, el ejercicio y el ajuste están en los tres idiomas.
 */
final class PruebasDeOrdenesClaras {

    private PruebasDeOrdenesClaras() {
    }

    static void apuntar() {
        apuntarPlantilla();
        apuntarAvisos();
        apuntarMiraLejos();
        apuntarAjuste();
        apuntarContenido();
    }

    private static void apuntarPlantilla() {
        caso("órdenes claras: la plantilla con sus comentarios nuevos compila en es, de y en y avisa de las tres reglas", () -> {
            String[][] palabras = {{"borra", "14°", "AHORA"}, {"löscht", "14°", "JETZT"}, {"erases", "14°", "NOW"}};
            for (Idioma i : Idioma.values()) {
                String codigo = Comun.plantilla(i);
                Compilador.Resultado r = new RobotDelJugador().compilar(codigo, Locale.ROOT, true);
                cierto(r.bien, i + ": la plantilla compila: " + r.errores);
                for (String p : palabras[i.ordinal()]) cierto(comentarios(codigo).contains(p), i + ": un comentario dice «" + p + "»");
            }
        });
    }

    private static void apuntarAvisos() {
        caso("órdenes claras: el aviso de orden borrada dice en llano las dos líneas de la plantilla, una vez por pelea", () -> {
            Idioma antes = T.idioma;
            try {
                String codigo = Comun.plantilla(Idioma.ES);
                int lApuntar = linea(codigo, "apuntar(12);"), lHacia = linea(codigo, "apuntarHacia(e.angulo());");
                Pelea p = peleaCon(codigo, 11);
                Cuerpo c = p.cuerpo(0);
                AvisoDeOrdenes aviso = new AvisoDeOrdenes();
                boolean salio = false;
                for (int i = 0; i < 600 && p.estado() == Pelea.Estado.LUCHANDO && !salio; i++) {
                    p.avanzar();
                    if (c.tieneConflictoDeOrdenes) salio = true;
                }
                cierto(salio, "la plantilla pisa la orden de alVerEnemigo en algún turno");
                for (Idioma i : Idioma.values()) {
                    T.idioma = i;
                    String t = AvisoDeOrdenes.textoPisada(AvisoDeOrdenes.primeraPisada(c));
                    cierto(t.contains(Integer.toString(lApuntar)) && t.contains(Integer.toString(lHacia)), i + ": nombra las dos líneas: " + t);
                    falso(t.contains("{"), i + ": sin huecos sin rellenar: " + t);
                }
                T.idioma = Idioma.ES;
                cierto(aviso.observar(c), "sale el aviso");
                cierto(aviso.texto().startsWith("La línea " + lApuntar + " borra la orden de la línea " + lHacia + ": la cabeza nunca llega al enemigo."),
                        "texto llano: " + aviso.texto());
                cierto(aviso.texto().contains("cadaTurno() se llama después de alVerEnemigo()"), "dice el orden de los métodos");
                // Una vez por pelea: aunque vuelva a pasar, no vuelve a salir (como mucho sale el otro aviso, una vez).
                int mas = 0;
                for (int i = 0; i < 300 && p.estado() == Pelea.Estado.LUCHANDO; i++) {
                    p.avanzar();
                    if (aviso.observar(c)) mas++;
                }
                cierto(mas <= 1, "cada aviso sale una sola vez por pelea (" + mas + " más)");
            } finally {
                T.idioma = antes;
            }
        });
    }

    private static void apuntarMiraLejos() {
        caso("órdenes claras: atacar con la mira lejos se avisa con la plantilla y nunca si el robot espera a la mira", () -> {
            Idioma antes = T.idioma;
            try {
                T.idioma = Idioma.ES;
                String codigo = Comun.plantilla(Idioma.ES);
                Pelea p = peleaCon(codigo, 11);
                Cuerpo c = p.cuerpo(0);
                AtaqueLejos lejos = null;
                for (int i = 0; i < 900 && p.estado() == Pelea.Estado.LUCHANDO && lejos == null; i++) {
                    p.avanzar();
                    lejos = c.ataqueLejos;
                }
                cierto(lejos != null, "la plantilla ataca alguna vez con la mira lejos");
                igual(linea(codigo, "atacar();"), lejos.linea, "línea de atacar()");
                igual(linea(codigo, "apuntarHacia(e.angulo());"), lejos.lineaDeLaMira, "línea de la orden de mira");
                cierto(lejos.faltaban > AtaqueLejos.UMBRAL, "le faltaban grados de verdad: " + lejos.faltaban);
                String t = AvisoDeOrdenes.textoLejos(lejos);
                cierto(t.contains("pide girar la cabeza, pero la línea " + lejos.linea + " dispara antes de que llegue"), "texto llano: " + t);
                String paciente = "import metajavbots.api.*;\n"
                        + "public class Paciente extends Metajavbot {\n"
                        + "    @Override\n"
                        + "    public void alVerEnemigo(Enemigo e) {\n"
                        + "        apuntarHacia(e.angulo());\n"
                        + "        if (Math.abs(restanteMira()) < 3) atacar();\n"
                        + "    }\n"
                        + "    @Override\n"
                        + "    public void cadaTurno() {\n"
                        + "        if (enemigosVistos().isEmpty()) apuntar(12);\n"
                        + "    }\n"
                        + "}\n";
                Pelea q = peleaCon(paciente, 11);
                Cuerpo d = q.cuerpo(0);
                AvisoDeOrdenes aviso = new AvisoDeOrdenes();
                int ataques = 0;
                for (int i = 0; i < 900 && q.estado() == Pelea.Estado.LUCHANDO; i++) {
                    q.avanzar();
                    cierto(d.ataqueLejos == null, "sin aviso de mira lejos en el turno " + i);
                    falso(d.tieneConflictoDeOrdenes, "sin órdenes borradas en el turno " + i);
                    falso(aviso.observar(d), "ningún aviso en el turno " + i);
                    for (metajavbots.combate.OrdenDelTurno o : d.ordenesDelTurno) if (o.accion.startsWith("atacar")) ataques++;
                }
                cierto(ataques > 0, "el robot paciente sí llega a atacar");
            } finally {
                T.idioma = antes;
            }
        });
    }

    private static void apuntarAjuste() {
        caso("órdenes claras: el ajuste de los avisos se guarda y se lee", () -> {
            Path anterior = Guardado.carpeta;
            Path temp = Files.createTempDirectory("metajavbots_ordenes_");
            try {
                Guardado.carpeta = temp;
                Jugador j = new Jugador("AvisosTest");
                j.robots.add(new Jugador.Robot("Bot", "cobalto"));
                cierto(j.avisosDeOrdenes, "encendido por defecto");
                j.avisosDeOrdenes = false;
                Guardado.guardar(j);
                falso(Guardado.leer(Guardado.carpetaDe(j.nombre)).avisosDeOrdenes, "apagado tras guardar y leer");
            } finally {
                Guardado.carpeta = anterior;
            }
        });
    }

    private static void apuntarContenido() {
        caso("órdenes claras: textos nuevos en es, de y en", () -> {
            String[] claves = {"aviso.mando.1", "aviso.mando.2", "aviso.mando.4", "aviso.ordenes.pisada", "aviso.ordenes.pisada_mira",
                "aviso.ordenes.por_que", "aviso.ordenes.por_que_mismo", "aviso.ordenes.lejos", "aviso.ordenes.lejos_mira", "aviso.ordenes.f9",
                "ajustes.avisos_ordenes", "ajustes.avisos_ordenes_ayuda", "pelea.mira_quiere",
                "pelea.mira_ahora", "dibujo.turno.titulo", "dibujo.turno.paso1", "dibujo.turno.paso2", "dibujo.turno.paso3",
                "dibujo.mando.ruedas", "dibujo.mando.cuerpo", "dibujo.mando.cabeza", "dibujo.orden.avanzar", "dibujo.orden.ninguna",
                "dibujo.orden.mirar_enemigo", "dibujo.orden.girar12"};
            for (String k : claves) {
                Trilingue t = T.todos().get(k);
                cierto(t != null, "existe " + k);
                for (Idioma i : Idioma.values()) cierto(!t.en(i).isBlank(), k + " en " + i);
            }
        });

        caso("órdenes claras: el tutorial 1 cuenta las tres reglas y enseña el turno antes del primer código", () -> {
            Leccion l = null;
            for (Leccion x : Leccion.todas()) if (x.id.equals("l01_hola_arena")) l = x;
            cierto(l != null, "existe l01_hola_arena");
            int primerCodigo = -1, reglas = -1, dibujo = -1;
            for (int i = 0; i < l.paginas.size(); i++) {
                Leccion.Pagina pg = l.paginas.get(i);
                if (primerCodigo < 0 && !pg.codigo.isBlank()) primerCodigo = i;
                if (reglas < 0 && pg.texto.es.contains("ÚLTIMA") && pg.texto.de.contains("LETZTEN") && pg.texto.en.contains("LAST")) reglas = i;
                if (dibujo < 0 && pg.dibujo.equals("turno")) dibujo = i;
            }
            cierto(reglas >= 0 && reglas < primerCodigo, "las reglas van antes del primer código (" + reglas + " < " + primerCodigo + ")");
            cierto(dibujo >= 0 && dibujo < primerCodigo, "el dibujo del turno va antes del primer código");
            for (Idioma i : Idioma.values()) {
                String t = l.paginas.get(reglas).texto.en(i);
                falso(t.contains("public ") || t.contains("();"), i + ": las reglas se explican con palabras, sin Java para copiar");
            }
        });

        caso("órdenes claras: el concepto «Así obedece tu robot», la API y el ejercicio del banco", () -> {
            Concepto c = Concepto.de("como_obedece");
            cierto(c != null, "existe el concepto como_obedece");
            for (Idioma i : Idioma.values()) {
                cierto(c.parrafos(i).size() >= 4, i + ": párrafos del concepto");
                cierto(!c.pseudo(i).isBlank(), i + ": pseudocódigo del concepto");
            }
            cierto(Concepto.de("turno").ver.contains("como_obedece"), "el concepto del turno lleva al nuevo");
            String api = recurso("/datos/api.txt");
            for (String trozo : new String[]{"No es instantáneo: la mira gira despacio", "Nicht sofort: Das Visier dreht langsam",
                "Not instant: the sight turns slowly", "hacia donde apunta la mira AHORA", "Orden de cada turno: primero los avisos"}) {
                cierto(api.contains(trozo), "la API del Taller dice: " + trozo);
            }
            Ejercicio e = Banco.delJuego().ejercicio("j_dos_ordenes_cabeza");
            cierto(e != null, "existe el ejercicio j_dos_ordenes_cabeza");
            igual("giro", e.escena, "con su escena en la arena");
        });
    }

    private static String comentarios(String codigo) {
        StringBuilder b = new StringBuilder();
        for (String l : codigo.split("\n")) if (l.strip().startsWith("//")) b.append(l).append('\n');
        return b.toString();
    }

    /** La línea (desde 1) donde aparece por primera vez ese trozo de código fuera de un comentario. */
    private static int linea(String codigo, String trozo) {
        String[] ls = codigo.split("\n", -1);
        for (int i = 0; i < ls.length; i++) if (ls[i].contains(trozo) && !ls[i].strip().startsWith("//")) return i + 1;
        return -1;
    }

    private static String recurso(String ruta) throws IOException {
        try (InputStream in = PruebasDeOrdenesClaras.class.getResourceAsStream(ruta)) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Pelea peleaCon(String codigo, long semilla) {
        RobotDelJugador rj = new RobotDelJugador();
        Compilador.Resultado res = rj.compilar(codigo, Locale.ROOT, true);
        cierto(res.bien, "compila con traza: " + res.errores);
        List<Modelo> ms = Modelo.todos();
        Combatiente a = new Combatiente("Jugador", new Montaje(ms.get(0)), rj);
        Combatiente b = new Combatiente("Muneco", new Montaje(ms.get(1)), Muneco::new);
        Pelea p = new Pelea(a, b, semilla);
        p.rondas = 1;
        p.empezarRonda();
        return p;
    }
}
