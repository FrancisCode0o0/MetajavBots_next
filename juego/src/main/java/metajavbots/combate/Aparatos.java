package metajavbots.combate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import metajavbots.api.Cacharro;

/**
 * APARATOS — Los cacharros que aparecen en la arena de ronda en ronda (motor
 * a chorro, antigravedad, descarga, lanzallamas, aceite, pegamento, imán,
 * martillo, sierra). Se anuncia el de la ronda siguiente al acabar una (para
 * programar entre rondas cómo ir a por él); sale a los 3 s de empezar. Se
 * coge al tocarlo, se conecta con conectar(hueco) en el hueco que dice su
 * código (si no es ese, no engancha), tarda un segundo en montarse y se usa
 * con usarAparato().
 */
public final class Aparatos {

    /** Los tipos, su nombre de marca, su hueco y su recarga (en turnos). */
    public enum Tipo {
        CHORRO("Jetzazo", "espalda", 120), ANTIGRAVEDAD("Flotón", "piernas", 210), DESCARGA("Chispazo", "cabeza", 180),
        LANZALLAMAS("Chamuscator", "brazo", 150), ACEITE("Resbalín", "espalda", 240), PEGAMENTO("Pegotón", "brazo", 150),
        IMAN("Imanator", "cabeza", 180), MARTILLO("Mazazo", "brazo", 150), SIERRA("Sierrón", "brazo", 135);

        public final String nombre, hueco;
        public final int recarga;

        Tipo(String nombre, String hueco, int recarga) {
            this.nombre = nombre;
            this.hueco = hueco;
            this.recarga = recarga;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Un aparato: en el suelo (dueno -1) o llevado por un robot. */
    public static final class Uno {
        public final Tipo tipo;
        public double x, y;
        public int dueno = -1;
        /** Turnos que le quedan para quedar montado (al conectarlo); 0 si ya lo está o no se ha conectado. */
        public int montando;
        public boolean conectado;
        public int recarga;
        /** Turnos que le queda haciendo lo suyo (el chorro, el vuelo, la llama); para dibujarlo. */
        public int activo;

        Uno(Tipo tipo, double x, double y) {
            this.tipo = tipo;
            this.x = x;
            this.y = y;
        }
    }

    /** Lo que ve un robot de un aparato. */
    private static final class Vista implements Cacharro {
        private final Uno a;
        private final double x, y, distancia, angulo;

        Vista(Cuerpo yo, Uno a) {
            this.a = a;
            x = a.dueno >= 0 ? yo.x : a.x;
            y = a.dueno >= 0 ? yo.y : a.y;
            distancia = Math.hypot(x - yo.x, y - yo.y);
            angulo = Math.toDegrees(Math.atan2(y - yo.y, x - yo.x));
        }

        @Override
        public String tipo() {
            return a.tipo.id();
        }

        @Override
        public String nombre() {
            return a.tipo.nombre;
        }

        @Override
        public double x() {
            return x;
        }

        @Override
        public double y() {
            return y;
        }

        @Override
        public double distancia() {
            return distancia;
        }

        @Override
        public double angulo() {
            return angulo;
        }

        @Override
        public boolean conectado() {
            return a.conectado;
        }

        @Override
        public boolean listo() {
            return a.conectado && a.recarga <= 0;
        }
    }

    /** Si hay aparatos en esta pelea (lo pone la pantalla, según Ajustes; las pruebas y las peleas de antes, sin). */
    public boolean activos;
    private final List<Uno> todos = new ArrayList<>();
    /** El que saldrá en la ronda siguiente (para enseñar su código entre rondas). */
    private Tipo siguiente;
    private Random azar = new Random(5);
    /** El turno de la ronda en que sale el aparato. */
    public static final int SALE = 90, MONTAR = 30;

    public List<Uno> todos() {
        return todos;
    }

    /** El que saldrá en esta ronda o en la siguiente (null si no hay aparatos). */
    public Tipo siguiente() {
        return activos ? siguiente : null;
    }

    /** Fija el aparato que saldrá en la ronda (útil para pruebas o escenarios dirigidos). */
    public void fijarSiguiente(Tipo tipo) {
        this.siguiente = tipo;
    }

    void empezarPelea(long semilla) {
        azar = new Random(semilla * 13 + 1);
        siguiente = Tipo.values()[azar.nextInt(Tipo.values().length)];
    }

    void empezarRonda() {
        todos.clear();
    }

    void acabarRonda() {
        todos.clear();
        if (activos) siguiente = Tipo.values()[azar.nextInt(Tipo.values().length)];
    }

    /** Lo que ve un robot: los del suelo (o, con propio, solo el suyo). */
    List<Cacharro> vistas(Cuerpo c, boolean propio) {
        List<Cacharro> l = new ArrayList<>();
        for (Uno a : todos) if (propio ? a.dueno == c.indice : a.dueno < 0) l.add(new Vista(c, a));
        return l;
    }

    /** El que lleva ese robot (o null). */
    public Uno delCuerpo(int indice) {
        for (Uno a : todos) if (a.dueno == indice) return a;
        return null;
    }

    Uno de(Cuerpo c) {
        for (Uno a : todos) if (a.dueno == c.indice) return a;
        return null;
    }

    /** Lo conecta en ese hueco (en cualquiera de los tres idiomas): si no es el suyo, no engancha. */
    boolean conectar(Cuerpo c, String hueco) {
        Uno a = de(c);
        if (a == null || a.conectado || a.montando > 0) return false;
        if (!hueco(hueco).equals(a.tipo.hueco)) {
            c.pelea.suceso(Suceso.Tipo.APARATO, c.indice, c.x, c.y, 0, 0, "hueco");
            return false;
        }
        a.montando = MONTAR;
        c.pelea.suceso(Suceso.Tipo.APARATO, c.indice, c.x, c.y, 0, 0, "montar");
        return true;
    }

    /** «espalda», «Rücken», «back»… → el hueco en español. */
    static String hueco(String h) {
        String s = h == null ? "" : h.strip().toLowerCase(Locale.ROOT).replace("ü", "ue");
        switch (s) {
            case "espalda": case "ruecken": case "back": return "espalda";
            case "piernas": case "beine": case "legs": return "piernas";
            case "cabeza": case "kopf": case "head": return "cabeza";
            case "brazo": case "arm": return "brazo";
            default: return s;
        }
    }

    /** Lo usa (con el ángulo, para lo que apunta; sin él, hacia donde mira). */
    boolean usar(Cuerpo c, double angulo) {
        Uno a = de(c);
        if (a == null || !a.conectado || a.recarga > 0 || c.fuera) return false;
        Pelea p = c.pelea;
        a.recarga = a.tipo.recarga;
        double rad = Math.toRadians(angulo);
        switch (a.tipo) {
            case CHORRO:
                c.turbo = Math.max(c.turbo, 20);
                c.factorTurbo = Math.max(c.factorTurbo, 2.6);
                a.activo = 20;
                break;
            case ANTIGRAVEDAD:
                c.flotando = 50;
                a.activo = 50;
                break;
            case DESCARGA:
                for (int i = 0; i < p.combatientes.length; i++) {
                    Cuerpo e = p.cuerpo(i);
                    if (e == null || e == c || e.fuera || p.equipo[i] == p.equipo[c.indice]) continue;
                    if (Math.hypot(e.x - c.x, e.y - c.y) < 150) {
                        p.danar(e, c, 6, "descarga", Math.toDegrees(Math.atan2(c.y - e.y, c.x - e.x)), false);
                        e.aturdido = Math.max(e.aturdido, 25);
                    }
                }
                a.activo = 12;
                break;
            case LANZALLAMAS:
                c.llamaAparato = 30;
                c.miraDeLaLlama = angulo;
                a.activo = 30;
                break;
            case ACEITE:
                p.arenaPropia().charco("aceite", c.x - Math.cos(Math.toRadians(c.rumbo)) * 40, c.y - Math.sin(Math.toRadians(c.rumbo)) * 40, 80, 300);
                a.activo = 10;
                break;
            case PEGAMENTO: {
                // Una pegota que cae a 220 de distancia (o en el primer enemigo de la línea).
                double[] fin = new double[2];
                Cuerpo e = p.primeroEnLaLinea(c, c.x, c.y, Math.cos(rad), Math.sin(rad), 220, fin);
                double px = e != null ? e.x : c.x + Math.cos(rad) * 220, py = e != null ? e.y : c.y + Math.sin(rad) * 220;
                p.arenaPropia().charco("pegamento", px, py, 60, 240);
                a.activo = 10;
                break;
            }
            case IMAN:
                iman(p, c, angulo);
                a.activo = 18;
                break;
            case MARTILLO:
                martillo(p, c, angulo);
                a.activo = 15;
                break;
            case SIERRA:
                c.sierraAparato = 24;
                c.miraDeLaSierra = angulo;
                a.activo = 24;
                break;
            default:
                break;
        }
        p.suceso(Suceso.Tipo.APARATO, c.indice, c.x, c.y, angulo, 0, "aparato_" + a.tipo.id());
        return true;
    }

    /** Un turno: sale el aparato, se coge al tocarlo, se monta y se recarga; la llama del lanzallamas quema. */
    void turno(Pelea p) {
        if (!activos) return;
        if (p.turno() == SALE - 1 && siguiente != null) salir(p);
        for (Uno a : todos) {
            if (a.recarga > 0) a.recarga--;
            if (a.activo > 0) a.activo--;
            if (a.montando > 0 && --a.montando == 0) {
                a.conectado = true;
                Cuerpo c = p.cuerpo(a.dueno);
                if (c != null) p.suceso(Suceso.Tipo.APARATO, c.indice, c.x, c.y, 0, 0, "conectado");
            }
            if (a.dueno >= 0) {
                Cuerpo c = p.cuerpo(a.dueno);
                if (c != null) {
                    a.x = c.x;
                    a.y = c.y;
                }
                continue;
            }
            for (int i = 0; i < p.combatientes.length; i++) {
                Cuerpo c = p.cuerpo(i);
                if (c == null || c.fuera || de(c) != null || Math.hypot(c.x - a.x, c.y - a.y) > c.radio + 26) continue;
                a.dueno = i;
                p.suceso(Suceso.Tipo.APARATO, i, a.x, a.y, 0, 0, "coger");
                break;
            }
        }
        for (int i = 0; i < p.combatientes.length; i++) {
            llama(p, p.cuerpo(i));
            sierra(p, p.cuerpo(i));
        }
    }

    /** Sale en un sitio libre: lejos de las trampas y de los dos robots a la vez. */
    private void salir(Pelea p) {
        double mejor = -1, bx = p.ancho / 2, by = p.alto / 2;
        for (int k = 0; k < 20; k++) {
            double x = p.ancho * (0.15 + 0.7 * azar.nextDouble()), y = p.alto * (0.15 + 0.7 * azar.nextDouble());
            double libre = Double.MAX_VALUE;
            for (Arena.Trampa t : p.arena.trampas()) {
                if (!t.destruida()) libre = Math.min(libre, Math.hypot(t.x - x, t.y - y) - t.radio);
            }
            if (libre < 40) continue;
            // Una carrera justa: a la misma distancia de todos y lejos de cada uno (que haya que programar cómo ir).
            double cerca = Double.MAX_VALUE, lejos = 0;
            for (int i = 0; i < p.combatientes.length; i++) {
                Cuerpo c = p.cuerpo(i);
                if (c == null) continue;
                double d = Math.hypot(c.x - x, c.y - y);
                cerca = Math.min(cerca, d);
                lejos = Math.max(lejos, d);
            }
            double nota = -(lejos - cerca) - (cerca < 220 ? 1000 : 0) + Math.min(libre, 100) * 0.2;
            if (mejor < 0 || nota > mejor) {
                mejor = nota;
                bx = x;
                by = y;
            }
        }
        todos.add(new Uno(siguiente, bx, by));
        p.suceso(Suceso.Tipo.APARATO, -1, bx, by, 0, 0, "aparato_sale");
    }

    /** El lanzallamas: un chorro de fuego en cono que quema a los enemigos que pilla. */
    private static void llama(Pelea p, Cuerpo c) {
        if (c == null || c.llamaAparato <= 0) return;
        c.llamaAparato--;
        for (int i = 0; i < p.combatientes.length; i++) {
            Cuerpo e = p.cuerpo(i);
            if (e == null || e == c || e.fuera || p.equipo[i] == p.equipo[c.indice]) continue;
            double d = Math.hypot(e.x - c.x, e.y - c.y);
            double ang = Math.abs(Pelea.normal(Math.toDegrees(Math.atan2(e.y - c.y, e.x - c.x)) - c.miraDeLaLlama));
            if (d < 180 && ang < 18 && c.llamaAparato % 3 == 0) p.danar(e, c, 1.2, "llama", Math.toDegrees(Math.atan2(c.y - e.y, c.x - e.x)), false);
        }
    }

    /** El imán: atrae hacia el robot a los enemigos del cono y desvía proyectiles enemigos hacia el ángulo dado. */
    private static void iman(Pelea p, Cuerpo c, double angulo) {
        double rad = Math.toRadians(angulo);
        for (int i = 0; i < p.combatientes.length; i++) {
            Cuerpo e = p.cuerpo(i);
            if (e == null || e == c || e.fuera || p.equipo[i] == p.equipo[c.indice]) continue;
            double dx = e.x - c.x, dy = e.y - c.y;
            double d = Math.hypot(dx, dy);
            double ang = Math.toDegrees(Math.atan2(dy, dx));
            if (d > 0.001 && d < 260 && Math.abs(Pelea.normal(ang - angulo)) <= 45) {
                double tiron = Math.min(65, Math.max(0, d - (c.radio + e.radio + 4)));
                e.x = Math.max(e.radio, Math.min(p.ancho - e.radio, e.x - (dx / d) * tiron));
                e.y = Math.max(e.radio, Math.min(p.alto - e.radio, e.y - (dy / d) * tiron));
            }
        }
        for (Proyectil pr : p.proyectiles) {
            if (!pr.vivo || p.equipo[pr.dueno] == p.equipo[c.indice]) continue;
            if (Math.hypot(pr.x - c.x, pr.y - c.y) < 260) {
                pr.vx = Math.cos(rad) * pr.velocidad;
                pr.vy = Math.sin(rad) * pr.velocidad;
            }
        }
    }

    /** El martillo acoplable: golpe de corto alcance en el ángulo indicado con daño, empuje y aturdimiento. */
    private static void martillo(Pelea p, Cuerpo c, double angulo) {
        for (int i = 0; i < p.combatientes.length; i++) {
            Cuerpo e = p.cuerpo(i);
            if (e == null || e == c || e.fuera || p.equipo[i] == p.equipo[c.indice]) continue;
            double dx = e.x - c.x, dy = e.y - c.y;
            double d = Math.hypot(dx, dy);
            double ang = Math.toDegrees(Math.atan2(dy, dx));
            if (d <= c.radio + e.radio + 80 && Math.abs(Pelea.normal(ang - angulo)) <= 50) {
                p.danar(e, c, 10, "martillo", Pelea.normal(ang + 180), true);
                double radEmpuje = Math.toRadians(ang);
                e.x = Math.max(e.radio, Math.min(p.ancho - e.radio, e.x + Math.cos(radEmpuje) * 65));
                e.y = Math.max(e.radio, Math.min(p.alto - e.radio, e.y + Math.sin(radEmpuje) * 65));
                e.aturdido = Math.max(e.aturdido, 20);
                e.avance = 0;
            }
        }
    }

    /** La sierra circular acoplable: corta a ráfagas rápidas a los rivales cercanos delante del ángulo elegido. */
    private static void sierra(Pelea p, Cuerpo c) {
        if (c == null || c.sierraAparato <= 0) return;
        c.sierraAparato--;
        if (c.sierraAparato % 4 != 0) return;
        for (int i = 0; i < p.combatientes.length; i++) {
            Cuerpo e = p.cuerpo(i);
            if (e == null || e == c || e.fuera || p.equipo[i] == p.equipo[c.indice]) continue;
            double dx = e.x - c.x, dy = e.y - c.y;
            double d = Math.hypot(dx, dy);
            double ang = Math.toDegrees(Math.atan2(dy, dx));
            if (d <= c.radio + e.radio + 70 && Math.abs(Pelea.normal(ang - c.miraDeLaSierra)) <= 42) {
                p.danar(e, c, 2.2, "sierra", Pelea.normal(ang + 180), true);
                double radEmpuje = Math.toRadians(ang);
                e.x = Math.max(e.radio, Math.min(p.ancho - e.radio, e.x + Math.cos(radEmpuje) * 8));
                e.y = Math.max(e.radio, Math.min(p.alto - e.radio, e.y + Math.sin(radEmpuje) * 8));
            }
        }
    }
}
