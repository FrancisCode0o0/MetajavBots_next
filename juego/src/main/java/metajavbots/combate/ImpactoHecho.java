package metajavbots.combate;

import metajavbots.api.Impacto;

/** IMPACTO HECHO — Un golpe dado o recibido, en los tres idiomas. */
final class ImpactoHecho implements Impacto {

    /** El arma: «laser», «bala», «misil», «cuerpo», «llama», «choque», «pared», «pulso». */
    private final String arma;
    private final double dano, angulo;
    private final int parte;

    private static final String[][] ARMAS = {
            {"laser", "bala", "misil", "cuerpo", "llama", "choque", "pared", "pulso"},
            {"laser", "kugel", "rakete", "nahkampf", "flamme", "zusammenstoss", "wand", "puls"},
            {"laser", "bullet", "missile", "melee", "flame", "ram", "wall", "pulse"}};
    private static final String[][] PARTES = {
            {"cabeza", "izquierda", "derecha", "piernas"},
            {"kopf", "links", "rechts", "beine"},
            {"head", "left", "right", "legs"}};

    ImpactoHecho(String arma, double dano, double angulo, int parte) {
        this.arma = arma;
        this.dano = dano;
        this.angulo = angulo;
        this.parte = Math.max(0, Math.min(3, parte));
    }

    private String armaEn(int idioma) {
        for (int i = 0; i < ARMAS[0].length; i++) if (ARMAS[0][i].equals(arma)) return ARMAS[idioma][i];
        return arma;
    }

    @Override public double dano() { return dano; }
    @Override public String arma() { return arma; }
    @Override public double angulo() { return angulo; }
    @Override public String pieza() { return PARTES[0][parte]; }
    @Override public double schaden() { return dano; }
    @Override public String waffe() { return armaEn(1); }
    @Override public double winkel() { return angulo; }
    @Override public String teil() { return PARTES[1][parte]; }
    @Override public double damage() { return dano; }
    @Override public String weapon() { return armaEn(2); }
    @Override public double angle() { return angulo; }
    @Override public String part() { return PARTES[2][parte]; }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s %.1f en %s", arma, dano, PARTES[0][parte]);
    }
}
