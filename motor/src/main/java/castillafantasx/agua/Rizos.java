package castillafantasx.agua;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RIZOS — Lo que se mueve el agua cuando alguien entra o se mueve en ella:
 * ondas que se abren y se apagan, salpicaduras (gotas que saltan y caen) y
 * la estela (ondas pequeñas que va dejando detrás). Solo lleva la cuenta:
 * cada mundo tiene los suyos y los dibuja a su manera. Vale igual en la
 * superficie (medidas en píxeles) y en la mazmorra (en casillas).
 * Las cifras están en res/datos/agua.txt, sección [rizos].
 */
public final class Rizos {

    /** Una onda o una gota. */
    public static final class Marca {
        public boolean gota;        // false: una onda
        public float x, y;          // dónde está, en medidas del mundo
        public double edad, dura;   // segundos que lleva y los que dura
        public float radio;         // onda: hasta dónde se abre, en medidas del mundo
        public float vx, vy;        // gota: lo que se desplaza por segundo
        public float salto;         // gota: lo alto que llega, en píxeles de dibujo

        /** De 0 (recién nacida) a 1 (se acaba). */
        public double avance() {
            return Math.min(1, edad / dura);
        }

        /** El radio que tiene ahora la onda. */
        public float radioAhora() {
            return (float) (radio * (0.25 + 0.75 * avance()));
        }

        /** Lo alta que va ahora la gota, en píxeles de dibujo: sube y cae. */
        public float alturaAhora() {
            return (float) (salto * Math.sin(Math.PI * avance()));
        }
    }

    /** Lo que se recuerda de cada uno para saber cuándo entra en el agua y cuánto ha andado por ella. */
    public static final class Rastro {
        private boolean dentro;
        private float x, y;
        private double andado;
        private int lado = 1;       // a qué lado de su camino deja la próxima onda de la estela
    }

    /** Píxeles que se aparta de su camino cada onda de la estela, una a cada lado: así se abre en uve. */
    private static final double ANCHO_DE_LA_ESTELA = 5;

    /** Los dibujos de onda que hay (efectos/rizo_0…): cuántos tamaños y cuántos píxeles de radio crece cada uno. */
    public static final int TAMANOS = 9, PASO_DE_TAMANO = 4;

    public final List<Marca> marcas = new ArrayList<>();
    private final double porPixel;          // medidas del mundo que mide un píxel de dibujo
    private final Random azar = new Random(7);

    /** 'porPixel': lo que mide un píxel de dibujo en las medidas del mundo (1 en la superficie). */
    public Rizos(double porPixel) {
        this.porPixel = porPixel;
    }

    /** Pasan esos segundos: las marcas envejecen y las acabadas se quitan. */
    public void avanzar(double dt) {
        for (int i = marcas.size() - 1; i >= 0; i--) {
            Marca m = marcas.get(i);
            m.edad += dt;
            m.x += m.vx * dt;
            m.y += m.vy * dt;
            if (m.edad >= m.dura) marcas.remove(i);
        }
    }

    /**
     * Sigue a alguien que está en (x, y) con 'hondo' píxeles de agua debajo:
     * al entrar salpica y, mientras se mueve por ella, va dejando su estela.
     * 'talla': de 0 a 1 y pico, lo grande que es (un animal pequeño, menos que uno grande).
     */
    public void seguir(Rastro r, float x, float y, float hondo, float talla) {
        boolean dentro = hondo >= CifrasDeAgua.TOBILLOS;
        if (dentro && !r.dentro) {
            onda(x, y, talla);
            salpicar(x, y, Math.round(CifrasDeAgua.GOTAS_AL_ENTRAR * talla), talla);
            r.andado = 0;
        } else if (dentro) {
            double paso = Math.hypot(x - r.x, y - r.y);
            r.andado += paso / porPixel;
            if (r.andado >= CifrasDeAgua.PASO_DE_ESTELA && paso > 0) {
                r.andado = 0;
                // La estela: ondas pequeñas que quedan detrás, una a cada lado de su camino.
                double aparta = ANCHO_DE_LA_ESTELA * talla * porPixel * r.lado / paso;
                onda((float) (x - (y - r.y) * aparta), (float) (y + (x - r.x) * aparta), talla * 0.5f);
                r.lado = -r.lado;
            }
        }
        r.dentro = dentro;
        r.x = x;
        r.y = y;
    }

    /** Una onda que se abre desde ahí. 'talla': 1, la de una persona. */
    public void onda(float x, float y, float talla) {
        Marca m = nueva();
        m.x = x;
        m.y = y;
        m.dura = CifrasDeAgua.SEGUNDOS_DE_ONDA;
        m.radio = (float) (CifrasDeAgua.RADIO_DE_ONDA * talla * porPixel);
    }

    /** Saltan esas gotas desde ahí, cada una hacia un lado. 'fuerza': 1, la de entrar andando. */
    public void salpicar(float x, float y, int gotas, float fuerza) {
        for (int k = 0; k < gotas; k++) {
            Marca m = nueva();
            m.gota = true;
            m.x = x;
            m.y = y;
            m.dura = CifrasDeAgua.SEGUNDOS_DE_SALPICADURA * (0.7 + 0.6 * azar.nextDouble());
            double hacia = azar.nextDouble() * 2 * Math.PI, corre = (8 + 22 * azar.nextDouble()) * fuerza * porPixel;
            m.vx = (float) (Math.cos(hacia) * corre);
            m.vy = (float) (Math.sin(hacia) * corre * 0.5);
            m.salto = (float) ((5 + 9 * azar.nextDouble()) * fuerza);
        }
    }

    private Marca nueva() {
        if (marcas.size() >= CifrasDeAgua.MAXIMO_DE_RIZOS) marcas.remove(0);
        Marca m = new Marca();
        marcas.add(m);
        return m;
    }

    /** Ni una marca (al cambiar de sitio de golpe). */
    public void limpiar() {
        marcas.clear();
    }

    /**
     * De 0 a 1: cuánto brilla el agua en ese punto por las ondas que pasan
     * por él (para pintar el suelo de agua de la mazmorra). 'grueso': el
     * ancho de la cresta, en medidas del mundo.
     */
    public float brilloEn(double x, double y, double grueso) {
        float brillo = 0;
        for (int i = 0; i < marcas.size(); i++) {
            Marca m = marcas.get(i);
            if (m.gota) continue;
            double r = m.radioAhora(), dx = x - m.x, dy = y - m.y;
            if (Math.abs(dx) > r + grueso || Math.abs(dy) > r + grueso) continue;
            double d = Math.abs(Math.sqrt(dx * dx + dy * dy) - r);
            if (d < grueso) brillo = Math.max(brillo, (float) ((1 - d / grueso) * (1 - m.avance())));
        }
        return brillo;
    }
}
