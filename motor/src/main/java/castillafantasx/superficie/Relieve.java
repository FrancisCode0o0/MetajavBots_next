package castillafantasx.superficie;

/**
 * RELIEVE — Por dónde va el sol a cada hora: de eso depende hacia qué lado
 * caen las sombras del terreno en alto. (El terreno con sus alturas lo
 * pinta castillafantasx.paisaje, con un dibujo para el sol de la mañana y otro
 * para el de la tarde; el dibujo de la superficie los funde con este número.)
 */
public final class Relieve {

    private Relieve() { }

    /** Por dónde va el sol a esa hora: de -1 (acaba de salir: las sombras caen a poniente) a 1 (se pone: caen a levante). */
    public static float sol(double hora) {
        double sale = castillafantasx.tiempo.DiaYNoche.empieza(castillafantasx.tiempo.ParteDelDia.ALBA), sePone = castillafantasx.tiempo.DiaYNoche.empieza(castillafantasx.tiempo.ParteDelDia.NOCHE);
        return (float) Math.max(-1, Math.min(1, 2 * (hora - sale) / (sePone - sale) - 1));
    }
}
