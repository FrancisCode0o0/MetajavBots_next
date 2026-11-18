package metajavbots.combate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import metajavbots.api.Cosa;

/**
 * ARENA — Las trampas y los objetos de una arena, como en las luchas de
 * robots de la tele: pinchos que salen del suelo, sierras en las esquinas, un
 * imán, un foso, barriles, un carrito que va y viene… y lo que agarra el
 * suelo (en el hielo se resbala). Las arenas de antes no tienen nada
 * (NINGUNA): pelean igual que siempre. Los robots las ven con obstaculos().
 */
public final class Arena {

    /** Una trampa o un objeto: dónde está, lo que ocupa y lo que hace. */
    public static final class Trampa {
        public final String tipo;
        public double x, y;
        public final double radio;
        public final boolean solido;
        /** Para lo que se mueve: su velocidad (vx, vy); para lo que va a ratos, su ciclo y su fase (en turnos). */
        double vx, vy;
        final int ciclo, activo;
        int fase;

        Trampa(String tipo, double x, double y, double radio, boolean solido, int ciclo, int activo) {
            this.tipo = tipo;
            this.x = x;
            this.y = y;
            this.radio = radio;
            this.solido = solido;
            this.ciclo = ciclo;
            this.activo = activo;
        }

        /** ¿Hace daño ahora? Los pinchos y el rayo, solo en su rato; las sierras y el carrito, siempre. */
        public boolean peligroso() {
            switch (tipo) {
                case "pinchos": case "rayo": return fase % ciclo < activo;
                case "sierra": case "carrito": case "foso": return true;
                default: return false;
            }
        }

        /** De 0 a 1, lo que han salido los pinchos (para dibujarlos subiendo y bajando). */
        public float salida() {
            if (ciclo <= 0) return 1;
            int f = fase % ciclo;
            if (f < activo) return Math.min(1, Math.min(f, activo - f) / 6f);
            return 0;
        }

        public int fase() {
            return fase;
        }

        public double rumbo() {
            return vx == 0 && vy == 0 ? 0 : Math.toDegrees(Math.atan2(vy, vx));
        }

        public double velocidad() {
            return Math.hypot(vx, vy);
        }
    }

    /** Lo que ve un robot de una trampa: sus datos y dónde queda desde él. */
    private static final class Vista implements Cosa {
        private final Trampa t;
        private final double distancia, angulo;

        Vista(Cuerpo yo, Trampa t) {
            this.t = t;
            distancia = Math.hypot(t.x - yo.x, t.y - yo.y);
            angulo = Math.toDegrees(Math.atan2(t.y - yo.y, t.x - yo.x));
        }

        @Override
        public String tipo() {
            return t.tipo;
        }

        @Override
        public double x() {
            return t.x;
        }

        @Override
        public double y() {
            return t.y;
        }

        @Override
        public double radio() {
            return t.radio;
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
        public boolean peligroso() {
            return t.peligroso();
        }

        @Override
        public boolean solido() {
            return t.solido;
        }

        @Override
        public double rumbo() {
            return t.rumbo();
        }

        @Override
        public double velocidad() {
            return t.velocidad();
        }
    }

    /** La arena de siempre: sin trampas y con el suelo que agarra. */
    public static final Arena NINGUNA = new Arena(1);

    private final List<Trampa> trampas = new ArrayList<>();
    /** Lo que agarra el suelo: 1 normal; menos, se resbala (cuesta frenar y cambiar de dirección). */
    public final double agarre;
    /** Cuándo puede volver a hacer daño cada trampa a cada robot (para que no pegue en todos los turnos). */
    private final java.util.Map<Long, Integer> espera = new java.util.HashMap<>();

    private Arena(double agarre) {
        this.agarre = agarre;
    }

    public List<Trampa> trampas() {
        return Collections.unmodifiableList(trampas);
    }

    /** Lo que ve ese robot: una vista de cada trampa (vacía en las arenas de siempre). */
    List<Cosa> vistas(Cuerpo c) {
        if (trampas.isEmpty()) return List.of();
        List<Cosa> l = new ArrayList<>(trampas.size());
        for (Trampa t : trampas) l.add(new Vista(c, t));
        return l;
    }

    /** La arena de ese escenario (CHATARRERIA, COCINA, HIELO, TEJADO); los demás, NINGUNA. */
    public static Arena de(String escenario, double ancho, double alto) {
        double cx = ancho / 2, cy = alto / 2;
        switch (escenario) {
            case "CHATARRERIA": {
                Arena a = new Arena(1);
                a.poner("barril", ancho * 0.22, alto * 0.7, 30, true, 0, 0);
                a.poner("barril", ancho * 0.78, alto * 0.3, 30, true, 0, 0);
                a.poner("barril", ancho * 0.25, alto * 0.25, 26, true, 0, 0);
                a.poner("pinchos", cx - 170, cy, 55, false, 150, 60);
                a.poner("pinchos", cx + 170, cy, 55, false, 150, 60).fase = 75;
                a.poner("iman", cx, cy, 200, false, 0, 0);
                return a;
            }
            case "COCINA": {
                Arena a = new Arena(1);
                for (double[] e : new double[][]{{0, 0}, {ancho, 0}, {0, alto}, {ancho, alto}}) a.poner("sierra", e[0], e[1], 80, false, 0, 0);
                Trampa carrito = a.poner("carrito", 120, cy, 36, true, 0, 0);
                carrito.vx = 2.6;
                a.poner("pinchos", cx, alto * 0.22, 60, false, 120, 45);
                a.poner("pinchos", cx, alto * 0.78, 60, false, 120, 45).fase = 60;
                return a;
            }
            case "HIELO": {
                Arena a = new Arena(0.3);
                a.poner("foso", cx, cy, 70, false, 0, 0);
                a.poner("barril", ancho * 0.2, alto * 0.5, 28, true, 0, 0);
                a.poner("barril", ancho * 0.8, alto * 0.5, 28, true, 0, 0);
                return a;
            }
            case "TEJADO": {
                Arena a = new Arena(0.6);
                a.poner("chimenea", ancho * 0.3, alto * 0.65, 40, true, 0, 0);
                a.poner("chimenea", ancho * 0.7, alto * 0.35, 40, true, 0, 0);
                Trampa rayo = a.poner("rayo", cx, cy, 70, false, 200, 30);
                rayo.vx = 1.4;
                rayo.vy = 1.0;
                return a;
            }
            default:
                return NINGUNA;
        }
    }

    private Trampa poner(String tipo, double x, double y, double radio, boolean solido, int ciclo, int activo) {
        Trampa t = new Trampa(tipo, x, y, radio, solido, ciclo, activo);
        trampas.add(t);
        return t;
    }

    /** Un turno: se mueve lo que se mueve y cada trampa hace lo suyo a los robots que la tocan. */
    void turno(Pelea p) {
        if (trampas.isEmpty()) return;
        for (Trampa t : trampas) {
            t.fase++;
            mover(t, p);
        }
        for (int i = 0; i < p.combatientes.length; i++) {
            Cuerpo c = p.cuerpo(i);
            if (c == null || c.fuera) continue;
            for (int k = 0; k < trampas.size(); k++) efecto(p, c, k, trampas.get(k));
        }
    }

    /** Lo que se mueve rebota en las paredes (y el rayo va de un lado a otro por el tejado). */
    private static void mover(Trampa t, Pelea p) {
        if (t.vx == 0 && t.vy == 0) return;
        t.x += t.vx;
        t.y += t.vy;
        if (t.x < t.radio || t.x > p.ancho - t.radio) t.vx = -t.vx;
        if (t.y < t.radio || t.y > p.alto - t.radio) t.vy = -t.vy;
        t.x = Math.max(t.radio, Math.min(p.ancho - t.radio, t.x));
        t.y = Math.max(t.radio, Math.min(p.alto - t.radio, t.y));
    }

    private void efecto(Pelea p, Cuerpo c, int k, Trampa t) {
        double dx = c.x - t.x, dy = c.y - t.y, d = Math.hypot(dx, dy);
        boolean suelo = !c.vuela();
        if (t.tipo.equals("iman")) {
            // El imán tira de todos los que pisan el suelo, más cuanto más cerca.
            if (suelo && d < t.radio && d > 1) {
                double f = 0.9 * (1 - d / t.radio);
                c.x -= dx / d * f;
                c.y -= dy / d * f;
            }
            return;
        }
        if (t.solido && d < t.radio + c.radio && d > 0.01) {
            // Lo sólido no se atraviesa: se aparta al robot (y, si iba rápido, se da un golpe).
            double meter = t.radio + c.radio - d;
            c.x += dx / d * meter;
            c.y += dy / d * meter;
            if (Math.abs(c.velocidad) > 1.5 && listo(k, c, p, 20)) {
                p.danar(c, null, 0.5 + Math.abs(c.velocidad) * 0.3, t.tipo, Math.toDegrees(Math.atan2(-dy, -dx)), false);
                p.suceso(Suceso.Tipo.TRAMPA, c.indice, c.x, c.y, 0, 1, t.tipo);
            }
            c.velocidad *= 0.5;
        }
        if (!t.peligroso() || t.solido && !t.tipo.equals("carrito")) return;
        boolean toca = t.tipo.equals("foso") ? d < t.radio - c.radio * 0.3 : d < t.radio + c.radio * 0.6;
        if (!toca || !suelo && !t.tipo.equals("rayo")) return;
        switch (t.tipo) {
            case "foso":
                // Se cae: mucho daño, aturdido un rato y vuelve a salir por el borde más cercano.
                p.danar(c, null, 18, "foso", 0, false);
                p.suceso(Suceso.Tipo.TRAMPA, c.indice, c.x, c.y, 0, 3, "foso");
                c.aturdido = Math.max(c.aturdido, 45);
                double ang = Math.atan2(dy, dx);
                c.x = t.x + Math.cos(ang) * (t.radio + c.radio + 40);
                c.y = t.y + Math.sin(ang) * (t.radio + c.radio + 40);
                c.velocidad = 0;
                c.avance = 0;
                break;
            case "carrito":
                if (listo(k, c, p, 25)) {
                    p.danar(c, null, 4, "carrito", Math.toDegrees(Math.atan2(-dy, -dx)), false);
                    p.suceso(Suceso.Tipo.TRAMPA, c.indice, c.x, c.y, 0, 2, "carrito");
                }
                break;
            default:
                // Pinchos, sierras y rayo: daño a golpes, no en cada turno.
                if (listo(k, c, p, t.tipo.equals("sierra") ? 12 : 30)) {
                    double dano = t.tipo.equals("sierra") ? 2.5 : t.tipo.equals("rayo") ? 7 : 5;
                    p.danar(c, null, dano, t.tipo, Math.toDegrees(Math.atan2(-dy, -dx)), false);
                    p.suceso(Suceso.Tipo.TRAMPA, c.indice, c.x, c.y, 0, 2, t.tipo);
                }
                break;
        }
    }

    /** ¿Puede esa trampa volver a dañar a ese robot? (Y, si sí, apunta cuándo podrá la próxima vez.) */
    private boolean listo(int trampa, Cuerpo c, Pelea p, int turnos) {
        long clave = (long) trampa * 64 + c.indice;
        int ahora = p.turno() + p.ronda() * 100_000;
        Integer cuando = espera.get(clave);
        if (cuando != null && ahora < cuando) return false;
        espera.put(clave, ahora + turnos);
        return true;
    }
}
