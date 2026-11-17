package castillafantasx.tiempo;

import castillafantasx.arte.Colores;

/**
 * RELOJ — La hora del mundo. Las horas pasan mientras se juega, del alba a la
 * noche y vuelta, igual en la superficie que en la mazmorra; lo que dura un
 * día está en res/datos/dia_noche.txt. De la hora salen la parte del día, la
 * claridad del cielo y su color.
 */
public class Reloj {

    private static final double HORAS = 24;

    /** La hora, de 0 a 24 (19.5 son las siete y media de la tarde). */
    public double hora = DiaYNoche.HORA_INICIAL;
    /** Los días que van de partida (el primero es el 1). */
    public int dia = 1;

    /** Pasan esos segundos de juego. */
    public void avanzar(double segundos) {
        hora += segundos * HORAS / DiaYNoche.SEGUNDOS_POR_DIA;
        while (hora >= HORAS) {
            hora -= HORAS;
            dia++;
        }
    }

    /** Duerme hasta esa hora (7.5 son las siete y media): la de hoy, si aún no ha llegado, o la de mañana. */
    public void dormirHasta(double despierta) {
        if (hora >= despierta) dia++;
        hora = despierta;
    }

    /** En qué parte del día se está. */
    public ParteDelDia parte() {
        if (hora >= DiaYNoche.empieza(ParteDelDia.NOCHE) || hora < DiaYNoche.empieza(ParteDelDia.ALBA)) return ParteDelDia.NOCHE;
        if (hora >= DiaYNoche.empieza(ParteDelDia.CREPUSCULO)) return ParteDelDia.CREPUSCULO;
        if (hora >= DiaYNoche.empieza(ParteDelDia.DIA)) return ParteDelDia.DIA;
        return ParteDelDia.ALBA;
    }

    /** Por dónde va la parte del día en curso, de 0 (acaba de empezar) a 1 (se acaba). */
    public double avanceDeLaParte() {
        ParteDelDia parte = parte();
        ParteDelDia siguiente = ParteDelDia.values()[(parte.ordinal() + 1) % ParteDelDia.values().length];
        double desde = DiaYNoche.empieza(parte), hasta = DiaYNoche.empieza(siguiente);
        double dura = (hasta - desde + HORAS) % HORAS, lleva = (hora - desde + HORAS) % HORAS;
        return dura <= 0 ? 0 : Math.min(1, lleva / dura);
    }

    /** Depuración (F1): pone la hora en mitad de esa parte del día (la noche, en su primera mitad). */
    public void saltarA(ParteDelDia parte) {
        ParteDelDia siguiente = ParteDelDia.values()[(parte.ordinal() + 1) % ParteDelDia.values().length];
        double desde = DiaYNoche.empieza(parte), dura = (DiaYNoche.empieza(siguiente) - desde + HORAS) % HORAS;
        hora = (desde + dura * (parte == ParteDelDia.NOCHE ? 0.3 : 0.5)) % HORAS;
    }

    /**
     * La claridad del cielo: 0 de noche, 1 de día. Al alba clarea pronto y
     * en el crepúsculo aguanta: la luz se va casi toda al final.
     */
    public float claridad() {
        float t = suave((float) avanceDeLaParte());
        switch (parte()) {
            case ALBA:       return 1 - (1 - t) * (1 - t);
            case DIA:        return 1;
            case CREPUSCULO: return 1 - t * t;
            default:         return 0;
        }
    }

    /** El color de la luz del cielo ahora (RRGGBB): del alba al día y del día al crepúsculo. */
    public int colorDelCielo() {
        float t = suave((float) avanceDeLaParte());
        switch (parte()) {
            // El color del alba dura casi toda el alba, y el del crepúsculo llega pronto.
            case ALBA:       return Colores.mezcla(DiaYNoche.COLOR_DEL_ALBA, DiaYNoche.COLOR_DEL_DIA, t * t) & 0xFFFFFF;
            case CREPUSCULO: return Colores.mezcla(DiaYNoche.COLOR_DEL_DIA, DiaYNoche.COLOR_DEL_CREPUSCULO, (float) Math.sqrt(t)) & 0xFFFFFF;
            case NOCHE:      return DiaYNoche.COLOR_DEL_CREPUSCULO;
            default:         return DiaYNoche.COLOR_DEL_DIA;
        }
    }

    /** Cuánta oscuridad hay: lo contrario de la claridad (1 = noche cerrada). */
    public float oscuridad() {
        return 1 - claridad();
    }

    /** "07:30 · alba": la hora como se lee en pantalla. */
    public String texto() {
        int minutos = (int) (hora * 60);
        return String.format("%02d:%02d · %s", minutos / 60, minutos % 60, parte().nombre);
    }

    /** Un paso suave de 0 a 1: arranca y frena despacio. */
    private static float suave(float t) {
        return t * t * (3 - 2 * t);
    }
}
