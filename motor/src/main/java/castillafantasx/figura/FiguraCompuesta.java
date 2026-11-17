package castillafantasx.figura;

import java.util.ArrayList;
import java.util.List;

/**
 * FIGURA COMPUESTA — Un ser nuevo hecho de injertos: trozos de otras figuras
 * (una cabeza de persona, un torso de fiera, seis tentáculos, un gancho…)
 * puestos cada uno en su sitio, girados, deformados y repintados. Cada trozo
 * lo esculpe su propia figura, con sus gestos y su manera de moverse: aquí
 * solo se le dice al Escultor dónde cae cada punto (un Retoque por injerto).
 * Los seres se hacen en el editor (Figuras › Crear seres) y se guardan en res/seres/.
 */
public final class FiguraCompuesta implements Figura {

    public final List<Injerto> injertos = new ArrayList<>();
    /** Su tamaño respecto a un adulto. */
    public double talla = 1;

    private final List<Colocador> colocadores = new ArrayList<>();
    private Instante propio;     // de la misma clase que el que llega (el juego lo hereda con lo suyo)
    private static final double ENTRE_COPIAS = 0.37;       // segundos que va cada copia por detrás de la anterior, para que no se muevan a la vez
    private static final int TRONCO = 1;                    // el número que las figuras dan a su tronco (personas y cuadrúpedos)

    /** Lo que coloca un injerto: el Retoque que el Escultor consulta mientras su figura se esculpe. */
    private static final class Colocador implements Retoque {
        Injerto in;
        P3 ancla = P3.CERO;
        double vuelta;                              // el giro de esta copia alrededor del eje vertical (radianes)
        private P3 eje;                             // desde dónde se mide el trozo: su primer punto, si es solo una parte
        private Material pintura;
        final P3[] ejeFinal = new P3[64];           // dónde ha quedado el primer punto de cada parte, ya colocado
        P3 primero;                                 // el primer punto del trozo, ya colocado (de ahí sale el puente de «fundir»)
        Material visto;                             // el primer material que se ha pintado (el del puente)

        @Override
        public void empezar() {
            // Lo llama el Escultor al poner el injerto; lo de cada figura se limpia en preparar().
        }

        void preparar(Injerto injerto, P3 donde, double giroDeLaCopia) {
            in = injerto;
            ancla = donde;
            vuelta = giroDeLaCopia;
            eje = null;
            primero = null;
            visto = null;
            pintura = in.material == null ? null : Materiales.de(in.material);
            java.util.Arrays.fill(ejeFinal, null);
        }

        @Override
        public P3 punto(P3 p, int parte, int pieza) {
            // Un trozo suelto (una cabeza, un brazo) se mide desde su arranque; una figura entera, o lo que conserva su tronco, desde sus pies.
            if (eje == null) eje = in.partes.isEmpty() || in.partes.contains(TRONCO) ? P3.CERO : p;
            P3 v = p.menos(eje);
            if (in.estira.x != 1 || in.estira.y != 1 || in.estira.z != 1) v = new P3(v.x * in.estira.x, v.y * in.estira.y, v.z * in.estira.z);
            if (in.abre != 0) v = v.mas(Math.signum(v.x) * in.abre, 0, 0);
            if (in.retuerce != 0) v = v.girado(Math.toRadians(in.retuerce) * v.z / 10);
            v = v.por(in.tam);
            if (in.alza != 0) v = v.inclinado(Math.toRadians(-in.alza));
            if (in.ladea != 0) v = v.ladeado(Math.toRadians(in.ladea));
            v = v.girado(Math.toRadians(in.gira) + vuelta);
            P3 q = ancla.mas(in.en.girado(vuelta)).mas(v);
            if (ejeFinal[parte & 63] == null) ejeFinal[parte & 63] = q;
            if (primero == null) primero = q;
            return q;
        }

        @Override
        public double grosor(P3 p, int parte, int pieza) {
            return in.tam * in.grosor;
        }

        @Override
        public Material material(Material m, int parte, int pieza) {
            if (!in.partes.isEmpty() && !in.partes.contains(parte)) return null;
            Material sale = pintura != null && !m.brilla ? pintura : m;
            if (visto == null && !sale.brilla) visto = sale;
            return sale;
        }
    }

    @Override
    public double talla() {
        return talla;
    }

    @Override
    public double alto() {
        double alto = 12;
        for (Injerto in : injertos) {
            Figura f = castillafantasx.Contenido.delJuego().figuraDe(in.de);
            // Lo que se apoya en el suelo da la medida (de una figura a la que se le quita la cabeza se cuenta el cuerpo entero: se acerca bastante).
            if (f != null && in.sujetoA < 0) alto = Math.max(alto, in.en.z + (in.partes.isEmpty() || in.en.z < 1 ? f.alto() * in.tam * Math.abs(in.estira.z) : 8 * in.tam));
        }
        return alto;
    }

    @Override
    public P3 centroTendido(Instante instante) {
        Instante m = instante;
        for (Injerto in : injertos) {
            Figura f = castillafantasx.Contenido.delJuego().figuraDe(in.de);
            if (f != null) return f.centroTendido(m);
        }
        return P3.CERO;
    }

    @Override
    public void esculpir(Escultor e, Instante instante) {
        Instante m = instante;
        int copia = 0;
        if (primeraCopia.length < injertos.size()) primeraCopia = new int[injertos.size()];
        java.util.Arrays.fill(primeraCopia, -1);
        for (int k = 0; k < injertos.size(); k++) {
            Injerto in = injertos.get(k);
            Figura f = castillafantasx.Contenido.delJuego().figuraDe(in.de);
            if (f == null || f == this) continue;
            // De dónde cuelga: de una parte de una pieza anterior (ya colocada en este mismo dibujo) o del suelo.
            Colocador sosten = in.sujetoA >= 0 && in.sujetoA < k && primeraCopia[in.sujetoA] >= 0 ? colocadores.get(primeraCopia[in.sujetoA]) : null;
            P3 ancla = sosten == null ? P3.CERO : sosten.ejeFinal[in.parteDeSujecion & 63] != null ? sosten.ejeFinal[in.parteDeSujecion & 63]
                    : sosten.primero != null ? sosten.primero : P3.CERO;
            for (int c = 0; c < Math.max(1, in.veces); c++) {
                while (colocadores.size() <= copia) colocadores.add(new Colocador());
                Colocador col = colocadores.get(copia);
                col.preparar(in, ancla, in.veces <= 1 ? 0 : Math.toRadians(in.arco) * c / (in.arco >= 360 ? in.veces : Math.max(1, in.veces - 1)));
                if (c == 0) primeraCopia[k] = copia;
                copia++;
                e.injertar(col, k + 1);
                Instante suyo = m;
                if (in.desfase != 0 || c > 0) {
                    if (propio == null || propio.getClass() != m.getClass()) propio = m.nuevoIgual();
                    propio.copiar(m);
                    propio.tiempo = m.tiempo + in.desfase + c * ENTRE_COPIAS;
                    propio.fase = m.fase + (in.desfase + c * ENTRE_COPIAS) * 0.5;
                    suyo = propio;
                }
                f.esculpir(e, suyo);
                e.injertar(null, k + 1);
                e.figura((Figura) null, null);
                if (in.funde && sosten != null && col.primero != null) puente(e, ancla, col);
            }
        }
        e.injertar(null, 0);
        e.parte(0);
    }

    // De cada injerto, cuál es el colocador de su primera copia en este dibujo (-1: no se ha esculpido): de él cuelgan los siguientes.
    private int[] primeraCopia = new int[0];

    /** «Fundir»: un puente de carne, de bultos cada vez más finos, entre la pieza que sostiene y la que cuelga de ella. */
    private static void puente(Escultor e, P3 desde, Colocador col) {
        Material carne = col.visto != null ? col.visto : Material.CARNE_VIVA;
        double largo = col.primero.menos(desde).largo();
        if (largo < 0.5) return;
        int bultos = (int) Math.max(2, Math.min(8, largo / 1.8));
        double grueso = Math.max(1.2, 2.4 * col.in.tam * col.in.grosor);
        P3 antes = desde;
        for (int i = 1; i <= bultos; i++) {
            double t = i / (double) bultos;
            // Un poco caído por el medio, como algo que cuelga y se estira.
            P3 ahora = desde.hacia(col.primero, t).mas(0, 0, -Math.sin(Math.PI * t) * largo * 0.08);
            e.hueso(antes, ahora, grueso * (1 - 0.35 * Math.sin(Math.PI * (t - 0.5 / bultos))), grueso * (1 - 0.35 * Math.sin(Math.PI * t)), carne);
            antes = ahora;
        }
    }
}
