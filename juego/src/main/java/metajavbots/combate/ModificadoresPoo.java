package metajavbots.combate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import metajavbots.textos.T;

/**
 * MODIFICADORES POO — Analizador ligero por reflexión sobre la clase compilada
 * del robot del jugador que traduce el uso real de modificadores de Java
 * ({@code private}, {@code final}, {@code static}, {@code protected}, acceso
 * por defecto y {@code public} mutable) en efectos claros y equilibrados dentro
 * de la arena:
 * <ul>
 *   <li>{@code private} en campos de estado (y métodos auxiliares): encapsulación
 *       defensiva que reduce el daño recibido y acorta el radar enemigo.</li>
 *   <li>{@code final} en campos de configuración (y métodos): constantes
 *       inmutables que amplían el presupuesto de pasos por turno ({@code Tope}).</li>
 *   <li>{@code static} en campos compartidos: memoria persistente de clase; en
 *       peleas por equipos (2v2+) aporta además coordinación táctica (radar y
 *       recarga de energía).</li>
 *   <li>{@code protected}: extensibilidad controlada con una bonificación
 *       defensiva y de pasos moderada (menor que {@code private}).</li>
 *   <li>Acceso por defecto (paquete) en campos mutables: exposición leve al
 *       radar enemigo y aviso para encapsular con {@code private}.</li>
 *   <li>{@code public} en campos mutables sin encapsular: penalización visible
 *       (el rival detecta antes al robot, el daño recibido aumenta y aparece
 *       un aviso táctico en el informe).</li>
 * </ul>
 */
public final class ModificadoresPoo {

    /** Categoría visual de cada línea del resumen POO (depurador e informe). */
    public enum Nivel { BONO, INFO, PENALIZACION }

    /** Una línea breve del resumen de efectos POO activos. */
    public static final class Entrada {
        public final Nivel nivel;
        public final String texto;

        public Entrada(Nivel nivel, String texto) {
            this.nivel = nivel;
            this.texto = texto;
        }
    }

    /** Robot sin modificadores propios (o rival interno de la computadora). */
    public static final ModificadoresPoo NINGUNO = new ModificadoresPoo(new int[6], new int[5], false);

    public final int camposPrivados;
    public final int camposFinal;
    public final int camposStatic;
    public final int camposProtected;
    public final int camposPaquete;
    public final int camposPublicosMutables;
    public final int metodosPrivados;
    public final int metodosProtected;
    public final int metodosPaquete;
    public final int metodosFinal;
    public final int metodosStatic;
    public final boolean enEquipo;

    /** Multiplicador sobre el daño recibido (menor que 1 = más resistente). */
    public final double factorDanoRecibido;
    /** Multiplicador sobre el alcance del radar con el que el rival detecta a este robot. */
    public final double factorRadarRival;
    /** Multiplicador sobre el tope de pasos de cálculo por turno. */
    public final double factorPasos;
    /** Multiplicador sobre el alcance del radar propio (sinergia {@code static} en equipos). */
    public final double factorRadarPropio;
    /** Multiplicador sobre la recarga de energía por turno (sinergia {@code static} en equipos). */
    public final double factorRecarga;

    private final double bonoDanoPrivado;
    private final double bonoRadarPrivado;
    private final double bonoDanoProtected;
    private final double bonoRadarProtected;
    private final double penaRadarPaquete;
    private final double penaDanoPublico;
    private final double penaRadarPublico;

    private ModificadoresPoo(int[] campos, int[] metodos, boolean enEquipo) {
        this.camposPrivados = campos[0];
        this.camposFinal = campos[1];
        this.camposStatic = campos[2];
        this.camposProtected = campos[3];
        this.camposPaquete = campos[4];
        this.camposPublicosMutables = campos[5];
        this.metodosPrivados = metodos[0];
        this.metodosProtected = metodos[1];
        this.metodosPaquete = metodos[2];
        this.metodosFinal = metodos[3];
        this.metodosStatic = metodos[4];
        this.enEquipo = enEquipo;

        this.bonoDanoPrivado = Math.min(0.08, camposPrivados * 0.02) + (metodosPrivados > 0 ? 0.02 : 0.0);
        this.bonoRadarPrivado = Math.min(0.16, camposPrivados * 0.04);
        this.bonoDanoProtected = Math.min(0.03, camposProtected * 0.01);
        this.bonoRadarProtected = Math.min(0.06, camposProtected * 0.02);
        this.penaRadarPaquete = Math.min(0.09, camposPaquete * 0.03);
        this.penaDanoPublico = Math.min(0.12, camposPublicosMutables * 0.04);
        this.penaRadarPublico = Math.min(0.24, camposPublicosMutables * 0.08);

        double dano = 1.0 - bonoDanoPrivado - bonoDanoProtected + penaDanoPublico;
        this.factorDanoRecibido = limitar(dano, 0.90, 1.15);

        double radarRival = 1.0 - bonoRadarPrivado - bonoRadarProtected + penaRadarPaquete + penaRadarPublico;
        this.factorRadarRival = limitar(radarRival, 0.84, 1.25);

        double extraPasos = Math.min(0.20, camposFinal * 0.05)
                + Math.min(0.06, metodosFinal * 0.03)
                + Math.min(0.04, metodosProtected * 0.02)
                + Math.min(0.03, metodosStatic * 0.03);
        this.factorPasos = limitar(1.0 + extraPasos, 1.0, 1.25);

        boolean sinergiaEquipo = enEquipo && camposStatic > 0;
        this.factorRadarPropio = sinergiaEquipo ? 1.10 : 1.0;
        this.factorRecarga = sinergiaEquipo ? 1.08 : 1.0;
    }

    /**
     * Inspecciona por reflexión los campos y métodos declarados en la clase del
     * robot (y sus superclases propias, deteniéndose en {@code metajavbots.api.}).
     * Los rivales internos de {@code metajavbots.rivales.} devuelven {@link #NINGUNO}.
     */
    public static ModificadoresPoo analizar(Class<?> clase, boolean enEquipo) {
        if (clase == null) return NINGUNO;
        String raiz = clase.getName();
        if (raiz.startsWith("metajavbots.api.") || raiz.startsWith("metajavbots.rivales.")) return NINGUNO;

        int cPriv = 0, cFin = 0, cStat = 0, cProt = 0, cPaq = 0, cPubMut = 0;
        int mPriv = 0, mProt = 0, mPaq = 0, mFin = 0, mStat = 0;

        Class<?> c = clase;
        while (c != null && c != Object.class && !c.getName().startsWith("metajavbots.api.") && !c.getName().startsWith("metajavbots.rivales.")) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isSynthetic()) continue;
                int m = f.getModifiers();
                boolean esFinal = Modifier.isFinal(m);
                boolean esStatic = Modifier.isStatic(m);
                if (esFinal) cFin++;
                if (esStatic && !esFinal) cStat++;
                if (Modifier.isPublic(m)) {
                    if (!esFinal) cPubMut++;
                } else if (Modifier.isPrivate(m)) {
                    if (!esStatic && !esFinal) cPriv++;
                } else if (Modifier.isProtected(m)) {
                    if (!esStatic && !esFinal) cProt++;
                } else if (!esStatic && !esFinal) {
                    cPaq++;
                }
            }
            for (Method me : c.getDeclaredMethods()) {
                if (me.isSynthetic() || me.isBridge()) continue;
                int m = me.getModifiers();
                if (Modifier.isFinal(m)) mFin++;
                if (Modifier.isStatic(m)) mStat++;
                if (Modifier.isPrivate(m)) mPriv++;
                else if (Modifier.isProtected(m)) mProt++;
                else if (!Modifier.isPublic(m)) mPaq++;
            }
            c = c.getSuperclass();
        }
        if (cPriv == 0 && cFin == 0 && cStat == 0 && cProt == 0 && cPaq == 0 && cPubMut == 0
                && mPriv == 0 && mProt == 0 && mPaq == 0 && mFin == 0 && mStat == 0) {
            return NINGUNO;
        }
        return new ModificadoresPoo(
                new int[]{cPriv, cFin, cStat, cProt, cPaq, cPubMut},
                new int[]{mPriv, mProt, mPaq, mFin, mStat},
                enEquipo);
    }

    /** Si hay al menos un efecto o aviso POO activo para mostrar al jugador. */
    public boolean hayEfectos() {
        return !resumen().isEmpty();
    }

    /** Resumen breve y localizado (es/de/en) de los bonos y penalizaciones POO activos. */
    public List<Entrada> resumen() {
        List<Entrada> l = new ArrayList<>();
        if (camposPrivados > 0 || metodosPrivados > 0) {
            int pctDano = (int) Math.round(bonoDanoPrivado * 100);
            int pctRadar = (int) Math.round(bonoRadarPrivado * 100);
            l.add(new Entrada(Nivel.BONO, T.f("poo.private", camposPrivados + metodosPrivados, pctDano, pctRadar)));
        }
        if (factorPasos > 1.0001) {
            int pctPasos = (int) Math.round((factorPasos - 1.0) * 100);
            l.add(new Entrada(Nivel.BONO, T.f("poo.final", camposFinal + metodosFinal, pctPasos)));
        }
        if (camposStatic > 0) {
            if (enEquipo) {
                int pctRadar = (int) Math.round((factorRadarPropio - 1.0) * 100);
                int pctRecarga = (int) Math.round((factorRecarga - 1.0) * 100);
                l.add(new Entrada(Nivel.BONO, T.f("poo.static_equipo", camposStatic, pctRadar, pctRecarga)));
            } else {
                l.add(new Entrada(Nivel.INFO, T.f("poo.static_solo", camposStatic)));
            }
        }
        if (camposProtected > 0 || metodosProtected > 0) {
            int pctDano = (int) Math.round(bonoDanoProtected * 100);
            int pctRadar = (int) Math.round(bonoRadarProtected * 100);
            l.add(new Entrada(Nivel.BONO, T.f("poo.protected", camposProtected + metodosProtected, pctDano, pctRadar)));
        }
        if (camposPaquete > 0) {
            int pctRadar = (int) Math.round(penaRadarPaquete * 100);
            l.add(new Entrada(Nivel.INFO, T.f("poo.paquete", camposPaquete, pctRadar)));
        }
        if (camposPublicosMutables > 0) {
            int pctDano = (int) Math.round(penaDanoPublico * 100);
            int pctRadar = (int) Math.round(penaRadarPublico * 100);
            l.add(new Entrada(Nivel.PENALIZACION, T.f("poo.public", camposPublicosMutables, pctDano, pctRadar)));
        }
        return Collections.unmodifiableList(l);
    }

    private static double limitar(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
