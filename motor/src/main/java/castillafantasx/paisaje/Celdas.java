package castillafantasx.paisaje;



/**
 * CELDAS — Reparte el plano en celdas irregulares (cada punto es de la
 * «semilla» que tiene más cerca, como las grietas del barro seco). Con ellas
 * se pintan los bolos del granito, las lajas de la pizarra, los bloques de
 * la caliza y los cantos de un muro de piedra seca: cada celda, una piedra.
 */
public final class Celdas {

    /** Del último punto buscado: a cuántos píxeles queda el borde de su celda… */
    public float borde;
    /** …dónde cae dentro de ella, desde su centro (de -1 a 1, más o menos)… */
    public float dx, dy;
    /** …y dos números al azar, fijos para cada celda (de 0 a 1). */
    public float suerte, otraSuerte;

    /**
     * Busca la celda del punto (x, y). 'tamano': lo que mide una celda, en
     * píxeles. 'estirada': 1 = redonda; más = alargada (las lajas), en la
     * dirección cuyo coseno y seno son 'cos' y 'sin'.
     */
    public void buscar(float x, float y, float tamano, float estirada, float cos, float sin, int semilla) {
        // Se gira y se encoge el plano: en él las celdas son redondas; al deshacerlo, salen alargadas.
        float u = (x * cos + y * sin) / (tamano * estirada), v = (-x * sin + y * cos) / tamano;
        int cu = (int) Math.floor(u), cv = (int) Math.floor(v);
        float cerca = 99, segunda = 99, cercaU = 0, cercaV = 0;
        int suya = 0;
        for (int j = cv - 1; j <= cv + 1; j++) {
            for (int i = cu - 1; i <= cu + 1; i++) {
                // La semilla de cada casilla de la rejilla, movida al azar dentro de ella (de un solo número salen sus dos coordenadas).
                int azar = Ruido.entero(i, j, semilla);
                float du = (u - i - 0.1f - (azar & 255) / 320f) * estirada, dv = v - j - 0.1f - ((azar >> 8) & 255) / 320f;
                float d = du * du + dv * dv;
                if (d < cerca) {
                    segunda = cerca;
                    cerca = d;
                    cercaU = du;
                    cercaV = dv;
                    suya = azar;
                } else if (d < segunda) {
                    segunda = d;
                }
            }
        }
        borde = ((float) Math.sqrt(segunda) - (float) Math.sqrt(cerca)) * tamano / 2;
        dx = (cercaU * cos - cercaV * sin) * 2;
        dy = (cercaU * sin + cercaV * cos) * 2;
        suerte = ((suya >> 16) & 255) / 255f;
        otraSuerte = ((suya >> 24) & 255) / 255f;
    }
}
