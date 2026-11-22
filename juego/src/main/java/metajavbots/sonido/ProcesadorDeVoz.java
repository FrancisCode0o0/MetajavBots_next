package metajavbots.sonido;

/**
 * PROCESADOR DE VOZ — Aplica el {@link Timbre} de cada entrenador o del
 * locutor de forma incremental, bloque a bloque, manteniendo la continuidad
 * del remuestreo, la fase de la modulación en anillo y el eco metálico entre
 * fragmentos PCM sin esperar a tener la frase entera.
 */
public final class ProcesadorDeVoz {

    private static final float[] VACIO_F = new float[0];
    private static final byte[] VACIO_B = new byte[0];

    private final int frecuenciaSalida;
    private final double paso;
    private final double robot;
    private final int d1;
    private final int d2;
    private final float[] historial;
    private int posHistorial;
    private long muestraSalida;
    private double posEntrada;
    private float ultimaEntrada;
    private boolean tieneAnterior;
    private boolean tieneBytePendiente;
    private byte bytePendiente;
    private float pico = 0.65f;

    /** Prepara un procesador incremental desde la frecuencia de Piper a la del juego con ese timbre. */
    public ProcesadorDeVoz(int frecuenciaOrigen, Timbre timbre) {
        this(frecuenciaOrigen, Sintetizador.FRECUENCIA, timbre);
    }

    /** Prepara un procesador incremental con frecuencia de origen y de salida explícitas. */
    public ProcesadorDeVoz(int frecuenciaOrigen, int frecuenciaSalida, Timbre timbre) {
        Timbre t = timbre == null ? Timbre.LOCUTOR : timbre;
        this.frecuenciaSalida = Math.max(8000, frecuenciaSalida);
        int origen = Math.max(8000, frecuenciaOrigen);
        this.paso = ((double) origen / this.frecuenciaSalida) * t.tono();
        this.robot = t.robot();
        this.d1 = Math.max(1, (int) (this.frecuenciaSalida * 0.006));
        this.d2 = Math.max(this.d1 + 1, (int) (this.frecuenciaSalida * 0.013));
        this.historial = new float[this.d2];
    }

    /** Procesa un bloque PCM crudo (16 bits LE mono) y devuelve el bloque procesado al volumen indicado. */
    public byte[] procesar(byte[] pcmEntrada, int inicio, int longitud, float volumen) {
        if (pcmEntrada == null || longitud <= 0) return VACIO_B;
        float[] muestras = aMuestras(pcmEntrada, inicio, longitud);
        if (muestras.length == 0) return VACIO_B;
        float[] remuestreado = remuestrear(muestras);
        if (remuestreado.length == 0) return VACIO_B;
        float vol = Math.max(0f, Math.min(1f, volumen));
        byte[] salida = new byte[remuestreado.length * 2];
        for (int i = 0; i < remuestreado.length; i++) {
            float m = aplicarEfecto(remuestreado[i]) * vol;
            int v = Math.round((float) Math.tanh(m * 1.2) * 32000f);
            if (v > 32767) v = 32767;
            else if (v < -32768) v = -32768;
            salida[2 * i] = (byte) (v & 0xFF);
            salida[2 * i + 1] = (byte) ((v >> 8) & 0xFF);
        }
        return salida;
    }

    private float[] aMuestras(byte[] b, int inicio, int longitud) {
        int totalBytes = longitud + (tieneBytePendiente ? 1 : 0);
        int cantidad = totalBytes / 2;
        if (cantidad == 0) {
            bytePendiente = b[inicio];
            tieneBytePendiente = true;
            return VACIO_F;
        }
        float[] s = new float[cantidad];
        int pos = inicio;
        int idx = 0;
        if (tieneBytePendiente) {
            int lo = bytePendiente & 0xFF;
            int hi = b[pos++];
            s[idx++] = (short) (lo | (hi << 8)) / 32768f;
            tieneBytePendiente = false;
        }
        while (idx < cantidad) {
            int lo = b[pos++] & 0xFF;
            int hi = b[pos++];
            s[idx++] = (short) (lo | (hi << 8)) / 32768f;
        }
        int fin = inicio + longitud;
        if (pos < fin) {
            bytePendiente = b[pos];
            tieneBytePendiente = true;
        }
        return s;
    }

    private float[] remuestrear(float[] entrada) {
        if (Math.abs(paso - 1.0) < 1e-4) {
            ultimaEntrada = entrada[entrada.length - 1];
            tieneAnterior = true;
            return entrada;
        }
        if (!tieneAnterior) {
            ultimaEntrada = entrada[0];
            tieneAnterior = true;
        }
        int estimadas = Math.max(1, (int) Math.ceil((entrada.length - posEntrada) / paso) + 1);
        float[] salida = new float[estimadas];
        int escritos = 0;
        while (posEntrada < entrada.length - 1) {
            int k = (int) Math.floor(posEntrada);
            double f = posEntrada - k;
            float s0 = k < 0 ? ultimaEntrada : entrada[k];
            float s1 = entrada[k + 1];
            if (escritos == salida.length) salida = java.util.Arrays.copyOf(salida, salida.length * 2);
            salida[escritos++] = (float) (s0 * (1.0 - f) + s1 * f);
            posEntrada += paso;
        }
        ultimaEntrada = entrada[entrada.length - 1];
        posEntrada -= entrada.length;
        return escritos == salida.length ? salida : java.util.Arrays.copyOf(salida, escritos);
    }

    private float aplicarEfecto(float muestra) {
        float abs = Math.abs(muestra);
        pico = Math.max(0.35f, Math.max(abs, pico * 0.99992f));
        double x = muestra / pico;
        double fase = 2.0 * Math.PI * 55.0 * muestraSalida / frecuenciaSalida;
        double y = x * (1.0 - 0.16 * robot + 0.16 * robot * Math.sin(fase));
        if (muestraSalida >= d1) y += historial[(posHistorial - d1 + d2) % d2] * 0.22 * robot;
        if (muestraSalida >= d2) y += historial[posHistorial] * 0.1 * robot;
        float out = (float) Math.tanh(y * 1.1) * 0.85f;
        historial[posHistorial] = out;
        posHistorial = (posHistorial + 1) % d2;
        muestraSalida++;
        return out;
    }
}
