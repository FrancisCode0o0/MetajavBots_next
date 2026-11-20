package metajavbots.rivales;

import metajavbots.api.Aparato;
import metajavbots.api.Enemigo;
import metajavbots.api.Golpe;
import metajavbots.api.Metajavbot;
import metajavbots.api.Obstaculo;

/**
 * RIVAL — El robot de la computadora. Es un Metajavbot como el tuyo, escrito
 * con la misma API: puedes leerlo entero en el juego (Taller › Ver el código
 * del rival) y copiarle lo que te guste.
 *
 * Lo que lo hace fácil o difícil está en su Ajuste (cuánto falla al apuntar,
 * si adivina hacia dónde vas, si esquiva, si usa lo especial…). El Ajuste no
 * cambia en toda la pelea; lo que sí cambia, de ronda en ronda y solo desde
 * la dificultad «difícil», es su táctica: mira con qué le has hecho daño y
 * se defiende de eso.
 */
public class Rival extends Metajavbot {

    private final Ajuste ajuste;
    private Estilo estilo;

    // Lo último que se sabe del enemigo.
    private double enemigoX, enemigoY, enemigoRumbo, enemigoVelocidad, enemigoDistancia = 9999;
    private int turnoDelUltimoAvistamiento = -1000;
    private boolean enemigoVuela;

    // Para moverse en zigzag y en círculos.
    private int sentido = 1;
    private int turnosHastaCambiar;

    public Rival(Ajuste ajuste, Estilo estilo) {
        this.ajuste = ajuste;
        this.estilo = estilo;
    }

    public Estilo estilo() {
        return estilo;
    }

    @Override
    public void alEmpezar() {
        // De una ronda a otra, se acuerda de lo que le hizo daño (memoria()).
        Object guardado = memoria().get("estilo");
        if (guardado instanceof Estilo) estilo = (Estilo) guardado;
        turnoDelUltimoAvistamiento = -1000;
        sentido = azar().nextBoolean() ? 1 : -1;
        if (ajuste.saludo != null && ronda() == 1) decir(ajuste.saludo);
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        // Con poca reacción, no se entera de cada movimiento: solo de vez en cuando.
        if (turnoActual() - turnoDelUltimoAvistamiento < ajuste.reaccion && turnoActual() - turnoDelUltimoAvistamiento >= 0) return;
        enemigoX = e.x();
        enemigoY = e.y();
        enemigoRumbo = e.rumbo();
        enemigoVelocidad = e.velocidad();
        enemigoDistancia = e.distancia();
        enemigoVuela = e.vuela();
        turnoDelUltimoAvistamiento = turnoActual();
        // En equipo: de vez en cuando, avisa a los compañeros de dónde está el enemigo.
        if (!companeros().isEmpty() && turnoActual() % 45 == 0) {
            avisarAlEquipo("objetivo " + (int) e.x() + " " + (int) e.y());
        }
    }

    /** Un compañero dice dónde hay un enemigo: si yo no veo ninguno, voy para allá. */
    @Override
    public void alOirAlEquipo(String de, String mensaje) {
        String[] partes = mensaje.split(" ");
        if (partes.length != 3 || !partes[0].equals("objetivo")) return;
        if (turnoActual() - turnoDelUltimoAvistamiento < 25) return;
        try {
            double x = Double.parseDouble(partes[1]), y = Double.parseDouble(partes[2]);
            girarHacia(anguloHacia(x, y));
            apuntarA(x, y);
            avanzar(Math.min(200, distanciaA(x, y) - 150));
        } catch (NumberFormatException ex) {
            // un aviso que no entiendo: lo ignoro
        }
    }

    @Override
    public void cadaTurno() {
        boolean loVeo = turnoActual() - turnoDelUltimoAvistamiento < 25;
        if (!loVeo) {
            buscar();
            return;
        }
        apuntarAlEnemigo();
        moverseSegunEstilo();
        usarElAparato();
        apartarseDeLasTrampas();
        usarArmas();
        usarEspecial();
    }

    /**
     * Los aparatos de la arena: si hay uno cerca, va a por él; si lo lleva, lo conecta en su hueco (el que dice su
     * código) y lo usa cuando le conviene. En las peleas sin aparatos, no hace nada.
     */
    private void usarElAparato() {
        Aparato mio = miAparato();
        if (mio == null) {
            for (Aparato a : aparatos()) {
                if (a.distancia() < 320) {
                    girarHacia(a.angulo());
                    avanzar(a.distancia());
                    return;
                }
            }
            return;
        }
        if (!mio.conectado()) {
            switch (mio.tipo()) {
                case "antigravedad": conectar("piernas"); break;
                case "descarga": case "iman": conectar("cabeza"); break;
                case "lanzallamas": case "pegamento": case "martillo": conectar("brazo"); break;
                default: conectar("espalda"); break;
            }
            return;
        }
        if (!mio.listo()) return;
        double d = distanciaA(enemigoX, enemigoY);
        switch (mio.tipo()) {
            case "descarga": if (d < 140) usarAparato(); break;
            case "lanzallamas": if (d < 170) usarAparato(anguloHacia(enemigoX, enemigoY)); break;
            case "pegamento": if (d < 240) usarAparato(anguloHacia(enemigoX, enemigoY)); break;
            case "iman": if (d < 250) usarAparato(anguloHacia(enemigoX, enemigoY)); break;
            case "martillo": if (d < 120) usarAparato(anguloHacia(enemigoX, enemigoY)); break;
            case "chorro": if (d > 250) usarAparato(); break;
            default: usarAparato(); break;
        }
    }

    /**
     * Si va derecho a una trampa que hace daño (foso, pinchos fuera, sierra…), gira para rodearla. En las arenas sin
     * trampas, obstaculos() está vacía y no hace nada.
     */
    private void apartarseDeLasTrampas() {
        for (Obstaculo o : obstaculos()) {
            if (!o.peligroso() && !o.solido()) continue;
            double delante = Math.abs(normalizar(o.angulo() - rumbo()));
            if (o.distancia() < o.radio() + 70 && delante < 50) {
                girar(normalizar(o.angulo() - rumbo()) > 0 ? -60 : 60);
                avanzar(90);
                return;
            }
        }
    }

    /** Sin el enemigo a la vista: gira la mira y el cuerpo hasta encontrarlo. */
    private void buscar() {
        apuntar(25 * sentido);
        if (restanteAvance() == 0) {
            girar(30 * sentido);
            avanzar(80);
        }
        // Si se ha quedado contra una pared, sale hacia el centro.
        if (cercaDeLaPared(60)) {
            girarHacia(anguloHacia(anchoArena() / 2, altoArena() / 2));
            avanzar(120);
        }
    }

    /** Apunta adonde estará el enemigo cuando llegue el ataque (si sabe predecir) y falla un poco (según su ajuste). */
    private void apuntarAlEnemigo() {
        double x = enemigoX, y = enemigoY;
        if (ajuste.prediccion > 0) {
            double velocidadDelAtaque = 14;
            double turnos = enemigoDistancia / velocidadDelAtaque;
            double rad = Math.toRadians(enemigoRumbo);
            x += Math.cos(rad) * enemigoVelocidad * turnos * ajuste.prediccion;
            y += Math.sin(rad) * enemigoVelocidad * turnos * ajuste.prediccion;
        }
        double error = (azar().nextDouble() * 2 - 1) * ajuste.punteria;
        apuntarHacia(anguloHacia(x, y) + error);
    }

    private void moverseSegunEstilo() {
        double hacia = anguloHacia(enemigoX, enemigoY);
        turnosHastaCambiar--;
        if (turnosHastaCambiar <= 0) {
            turnosHastaCambiar = 30 + azar().nextInt(60);
            if (azar().nextDouble() < ajuste.esquiva) sentido = -sentido;
        }
        switch (estilo) {
            case FRANCOTIRADOR:
                // Lejos y de lado.
                if (enemigoDistancia < 380) girarHacia(hacia + 180 + 40 * sentido);
                else girarHacia(hacia + 90 * sentido);
                avanzar(100);
                break;
            case ACOSADOR:
                // En círculos a media distancia.
                girarHacia(hacia + (enemigoDistancia > 260 ? 60 : 100) * sentido);
                avanzar(100);
                break;
            case TANQUE:
                // De frente, sin prisa.
                girarHacia(hacia + 10 * sentido);
                avanzar(enemigoDistancia > 70 ? 100 : 0);
                break;
            case EMBESTIDOR:
                girarHacia(hacia);
                avanzar(200);
                break;
            case DANZARIN:
            default:
                girarHacia(hacia + 90 * sentido + (azar().nextDouble() * 60 - 30));
                avanzar(enemigoDistancia > 450 ? 140 : 70);
                break;
        }
        if (cercaDeLaPared(50)) {
            girarHacia(anguloHacia(anchoArena() / 2, altoArena() / 2));
            avanzar(100);
        }
    }

    private void usarArmas() {
        if (!apuntaBien()) return;
        // Cada brazo, si el enemigo está a su alcance.
        if (listoDerecha() && sirve(armaDerecha(), alcanceDerecha())) atacarDerecha();
        if (listoIzquierda() && sirve(armaIzquierda(), alcanceIzquierda())) atacarIzquierda();
    }

    /** ¿Merece la pena usar esta arma ahora? */
    private boolean sirve(String arma, double alcance) {
        switch (arma) {
            case "":
                return false;
            case "ESCUDO":
                return enemigoDistancia < 300 && azar().nextDouble() < ajuste.usoEspecial;
            case "REPARAR":
                return blindaje() < 70;
            case "CUERPO":
                return enemigoDistancia <= alcance && !enemigoVuela;
            default:
                return enemigoDistancia <= alcance * 0.95;
        }
    }

    private boolean apuntaBien() {
        return Math.abs(restanteMira()) < 12 + ajuste.punteria;
    }

    private void usarEspecial() {
        if (!listoEspecial() || azar().nextDouble() > ajuste.usoEspecial * 0.1) return;
        String especial = especialDeLaCabeza();
        boolean conviene;
        switch (especial) {
            case "REPARAR":
                conviene = blindaje() < 60;
                break;
            case "PULSO":
                conviene = enemigoDistancia < 220;
                break;
            case "TURBO":
                conviene = estilo == Estilo.EMBESTIDOR || enemigoDistancia > 500;
                break;
            default:
                conviene = enemigoDistancia < 500;
                break;
        }
        if (conviene) especial();
    }

    private String especialDeLaCabeza() {
        return especialCabeza();
    }

    private boolean cercaDeLaPared(double margen) {
        return x() < margen || y() < margen || x() > anchoArena() - margen || y() > altoArena() - margen;
    }

    // ----------------------------------------------------------- se adapta de ronda en ronda

    @Override
    public void alRecibirGolpe(Golpe g) {
        String clave = g.arma().equals("cuerpo") || g.arma().equals("choque") || g.arma().equals("llama") ? "cerca" : "lejos";
        memoria().merge("dano_" + clave, g.dano(), (a, b) -> (Double) a + (Double) b);
        if (ajuste.esquiva > 0.5 && azar().nextDouble() < ajuste.esquiva * 0.3) {
            // Esquiva de lado.
            girarHacia(g.angulo() + 90 * sentido);
            avanzar(90);
        }
        if (ajuste.burlas && blindaje() < 30 && azar().nextDouble() < 0.02) decir("…");
    }

    @Override
    public void alAcabarRonda() {
        if (!ajuste.adapta) return;
        double cerca = numero(memoria().get("dano_cerca")), lejos = numero(memoria().get("dano_lejos"));
        Estilo nuevo = estilo;
        if (cerca > lejos * 1.5) nuevo = Estilo.FRANCOTIRADOR;          // me pegan de cerca: me alejo
        else if (lejos > cerca * 1.5 && estilo == Estilo.FRANCOTIRADOR) nuevo = Estilo.ACOSADOR;
        else if (lejos > cerca * 1.5) nuevo = azar().nextBoolean() ? Estilo.DANZARIN : Estilo.TANQUE;
        memoria().put("estilo", nuevo);
        memoria().put("dano_cerca", 0.0);
        memoria().put("dano_lejos", 0.0);
    }

    private static double numero(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }
}
