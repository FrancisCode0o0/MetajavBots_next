package metajavbots.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * METAJAVBOT — Tu robot, en español. Escribe una clase que herede de esta
 * (tema 11, herencia) y sobrescribe (@Override) los métodos «al…» y
 * «cadaTurno» para darle comportamiento:
 *
 * <pre>
 * public class MiRobot extends Metajavbot {
 *     &#64;Override
 *     public void cadaTurno() {
 *         avanzar(50);
 *         girar(10);
 *     }
 *
 *     &#64;Override
 *     public void alVerEnemigo(Enemigo e) {
 *         apuntarHacia(e.angulo());
 *         atacar();
 *     }
 * }
 * </pre>
 *
 * Cada turno la pelea llama primero a los avisos (alRecibirGolpe,
 * alChocarConPared…), luego a alVerEnemigo por cada enemigo que ve y al final
 * a cadaTurno. Las órdenes (avanzar, girar, apuntar) no esperan: dicen lo que
 * quieres y el robot lo va haciendo turno a turno, a su velocidad.
 *
 * Ángulos en grados: 0 es la derecha (el este), 90 arriba; los giros
 * positivos van a la izquierda. La arena va de (0, 0), abajo a la izquierda,
 * a (anchoArena(), altoArena()).
 */
public abstract class Metajavbot extends RobotBase {

    // ================================================================ lo que sobrescribes

    /** Al empezar cada ronda (antes del primer turno). Tema 06: métodos. */
    public void alEmpezar() {
    }

    /** Cada turno (unos 30 por segundo). Aquí va lo principal. */
    public void cadaTurno() {
    }

    /** Cada turno en que tu radar ve a un enemigo. */
    public void alVerEnemigo(Enemigo e) {
    }

    /** Cuando te dan. */
    public void alRecibirGolpe(Golpe g) {
    }

    /** Cuando tu ataque da en el blanco. */
    public void alAcertar(Golpe g) {
    }

    /** Cuando chocas con una pared (te hace un poco de daño). */
    public void alChocarConPared() {
    }

    /** Cuando chocas con un enemigo. */
    public void alChocarConEnemigo(Enemigo e) {
    }

    /** Al acabar la ronda. */
    public void alAcabarRonda() {
    }

    /** Cuando un compañero de equipo te avisa (en las peleas por equipos). */
    public void alOirAlEquipo(String de, String mensaje) {
    }

    @Override
    final void empezar() {
        ejecutarEn("alEmpezar", this::alEmpezar);
    }

    @Override
    final void turno() {
        ejecutarEn("cadaTurno", this::cadaTurno);
    }

    @Override
    final void ver(Avistado e) {
        ejecutarEn("alVerEnemigo", () -> alVerEnemigo(e));
    }

    @Override
    final void golpeado(Impacto g) {
        ejecutarEn("alRecibirGolpe", () -> alRecibirGolpe(g));
    }

    @Override
    final void acierto(Impacto g) {
        ejecutarEn("alAcertar", () -> alAcertar(g));
    }

    @Override
    final void pared() {
        ejecutarEn("alChocarConPared", this::alChocarConPared);
    }

    @Override
    final void choque(Avistado e) {
        ejecutarEn("alChocarConEnemigo", () -> alChocarConEnemigo(e));
    }

    @Override
    final void finDeRonda() {
        ejecutarEn("alAcabarRonda", this::alAcabarRonda);
    }

    @Override
    final void equipo(String de, String mensaje) {
        ejecutarEn("alOirAlEquipo", () -> alOirAlEquipo(de, mensaje));
    }

    // ================================================================ moverse

    /** Avanza esa distancia (negativa: marcha atrás). Sustituye a la orden de avance anterior. */
    public final void avanzar(double distancia) {
        m().avanzar(distancia);
        anotarOrden("avanzar(" + redondear(distancia) + ")", Mando.CANAL_AVANCE);
    }

    /** Marcha atrás esa distancia. */
    public final void retroceder(double distancia) {
        m().avanzar(-distancia);
        anotarOrden("retroceder(" + redondear(distancia) + ")", Mando.CANAL_AVANCE);
    }

    /** Gira el cuerpo esos grados (positivos: a la izquierda). */
    public final void girar(double grados) {
        m().girar(grados);
        anotarOrden("girar(" + redondear(grados) + "°)", Mando.CANAL_GIRO);
    }

    public final void girarIzquierda(double grados) {
        m().girar(grados);
        anotarOrden("girarIzquierda(" + redondear(grados) + "°)", Mando.CANAL_GIRO);
    }

    public final void girarDerecha(double grados) {
        m().girar(-grados);
        anotarOrden("girarDerecha(" + redondear(grados) + "°)", Mando.CANAL_GIRO);
    }

    /** Gira el cuerpo hasta mirar hacia ese ángulo absoluto, por el camino más corto. */
    public final void girarHacia(double angulo) {
        m().girar(normalizar(angulo - m().rumbo()));
        anotarOrden("girarHacia(" + redondear(angulo) + "°)", Mando.CANAL_GIRO);
    }

    /** Para: sin avanzar ni girar. */
    public final void detener() {
        m().detener();
        anotarOrden("detener()", Mando.CANAL_AVANCE | Mando.CANAL_GIRO);
    }

    // ================================================================ apuntar y atacar

    /** Gira la mira (los brazos y el radar) esos grados, sin girar el cuerpo. */
    public final void apuntar(double grados) {
        m().apuntar(grados);
        anotarOrden("apuntar(" + redondear(grados) + "°)", Mando.CANAL_MIRA);
    }

    /** Gira la mira hasta ese ángulo absoluto. */
    public final void apuntarHacia(double angulo) {
        m().apuntar(normalizar(angulo - m().mira()));
        anotarOrden("apuntarHacia(" + redondear(angulo) + "°)", Mando.CANAL_MIRA);
    }

    /** Gira la mira hacia ese punto de la arena. */
    public final void apuntarA(double x, double y) {
        m().apuntar(normalizar(anguloHasta(x, y) - m().mira()));
        anotarOrden("apuntarA(" + redondear(x) + ", " + redondear(y) + ")", Mando.CANAL_MIRA);
    }

    /** Ataca con el brazo izquierdo hacia donde mira. Devuelve false si aún no estaba listo. */
    public final boolean atacarIzquierda() {
        boolean ok = m().atacar(Mando.IZQUIERDA);
        if (ok) anotarOrden("atacarIzquierda()", 0);
        return ok;
    }

    public final boolean atacarDerecha() {
        boolean ok = m().atacar(Mando.DERECHA);
        if (ok) anotarOrden("atacarDerecha()", 0);
        return ok;
    }

    /** Usa lo especial de la cabeza (un escudo, un turbo, reparar…, según la pieza). */
    public final boolean especial() {
        boolean ok = m().atacar(Mando.CABEZA);
        if (ok) anotarOrden("especial()", 0);
        return ok;
    }

    /** Ataca con el brazo que esté listo (primero el derecho). */
    public final boolean atacar() {
        boolean ok = m().atacar(Mando.DERECHA) || m().atacar(Mando.IZQUIERDA);
        if (ok) anotarOrden("atacar()", 0);
        return ok;
    }

    public final boolean listoIzquierda() {
        return m().listo(Mando.IZQUIERDA);
    }

    public final boolean listoDerecha() {
        return m().listo(Mando.DERECHA);
    }

    public final boolean listoEspecial() {
        return m().listo(Mando.CABEZA);
    }

    /** El arma del brazo izquierdo: "LASER", "BALA", "MISIL", "CUERPO", "ESCUDO", "REPARAR"… ("" si no hay). */
    public final String armaIzquierda() {
        return m().arma(Mando.IZQUIERDA);
    }

    public final String armaDerecha() {
        return m().arma(Mando.DERECHA);
    }

    /** Lo especial de la cabeza: "ESCUDO", "TURBO", "REPARAR", "CAMUFLAJE", "PULSO", "SOBRECARGA" ("" si no tiene). */
    public final String especialCabeza() {
        return m().arma(Mando.CABEZA);
    }

    /** Hasta dónde llega el arma del brazo izquierdo. */
    public final double alcanceIzquierda() {
        return m().alcance(Mando.IZQUIERDA);
    }

    public final double alcanceDerecha() {
        return m().alcance(Mando.DERECHA);
    }

    // ================================================================ saber

    public final double x() {
        return m().x();
    }

    public final double y() {
        return m().y();
    }

    /** Hacia dónde mira el cuerpo (grados). */
    public final double rumbo() {
        return m().rumbo();
    }

    /** Hacia dónde mira la mira (grados). */
    public final double mira() {
        return m().mira();
    }

    public final double velocidad() {
        return m().velocidad();
    }

    /** La energía (los láseres y lo especial la gastan; se recarga sola). */
    public final double energia() {
        return m().energia();
    }

    /** Lo que te queda de blindaje, de 0 a 100. */
    public final double blindaje() {
        return m().blindaje();
    }

    /** El número de turno de esta ronda (desde 0). */
    public final int turnoActual() {
        return m().turno();
    }

    /** La ronda (de 1 a 4). */
    public final int ronda() {
        return m().ronda();
    }

    public final double anchoArena() {
        return m().anchoArena();
    }

    public final double altoArena() {
        return m().altoArena();
    }

    /** Lo que le falta por avanzar de la última orden. */
    public final double restanteAvance() {
        return m().restanteAvance();
    }

    /** Lo que le falta por girar de la última orden. */
    public final double restanteGiro() {
        return m().restanteGiro();
    }

    public final double restanteMira() {
        return m().restanteMira();
    }

    /** Los enemigos que ves ahora mismo (tema 15: colecciones). */
    public final List<Enemigo> enemigosVistos() {
        return new ArrayList<>(m().vistos());
    }

    /** Las trampas y los objetos de la arena: pinchos, sierras, foso, barriles… (vacía si no hay; tema 15). */
    public final List<Obstaculo> obstaculos() {
        return new ArrayList<>(m().obstaculos());
    }

    /** Los aparatos que hay en el suelo de la arena: ve hasta uno para cogerlo (vacía si no hay; tema 15). */
    public final List<Aparato> aparatos() {
        return new ArrayList<>(m().aparatos());
    }

    /** El aparato que llevas (null si no llevas ninguno). */
    public final Aparato miAparato() {
        return m().miAparato();
    }

    /** Conecta el aparato que llevas en ese hueco: "espalda", "piernas", "cabeza" o "brazo" (lee su código para saber cuál). */
    public final boolean conectar(String hueco) {
        return m().conectar(hueco);
    }

    /** Usa el aparato conectado, hacia donde miras. Devuelve si se pudo (conectado y recargado). */
    public final boolean usarAparato() {
        return m().usarAparato(false, 0);
    }

    /** Usa el aparato conectado hacia ese ángulo (para los que apuntan: lanzallamas, pegamento). */
    public final boolean usarAparato(double angulo) {
        return m().usarAparato(true, angulo);
    }

    /** Tus compañeros de equipo (en las peleas por equipos; vacía si peleas solo). */
    public final List<Enemigo> companeros() {
        return new ArrayList<>(m().companeros());
    }

    /** Avisa a tus compañeros (les llega el turno siguiente en alOirAlEquipo): «voy a por el de la izquierda». */
    public final void avisarAlEquipo(Object mensaje) {
        m().avisarAlEquipo(aTexto(mensaje));
    }

    // ================================================================ ayudas

    /** El ángulo absoluto desde ti hasta ese punto. */
    public final double anguloHacia(double x, double y) {
        return anguloHasta(x, y);
    }

    /** La distancia desde ti hasta ese punto. */
    public final double distanciaA(double x, double y) {
        return distanciaHasta(x, y);
    }

    /** Lleva un ángulo a (-180, 180]: normalizar(350) da -10. */
    public final double normalizar(double grados) {
        return normalizado(grados);
    }

    /** Números al azar (los de tu robot: la misma pelea repetida da lo mismo). */
    public final Random azar() {
        return m().azar();
    }

    /** Dice algo en un bocadillo, encima del robot. */
    public final void decir(Object texto) {
        m().decir(aTexto(texto));
    }

    /** Escribe en la consola de tu robot (debajo del código). */
    public final void escribir(Object texto) {
        m().escribir(aTexto(texto));
    }

    /** Lo que quieras recordar de una ronda a otra (tema 15: Map). Sobrevive aunque cambies el código. */
    public final Map<String, Object> memoria() {
        return m().memoria();
    }
}
