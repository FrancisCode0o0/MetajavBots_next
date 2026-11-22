package metajavbots.campanas;

import java.util.Collections;
import java.util.List;

import metajavbots.dialogos.Concordancia;
import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * ESCENA DE CAMPAÑA — Secuencia ilustrada de viñetas dibujadas por código para
 * un capítulo de campaña (inicio, giro o final). Cada viñeta describe su
 * decorado, animación, robots, entrenador, público, narración y bocadillos
 * trilingües sin depender de imágenes externas ni de lwjgl3.
 */
public final class EscenaDeCampana {

    /** Decorado procedimental de la viñeta. */
    public enum Decorado {
        GIMNASIO, PLAYA, ESTADIO, HOSPITAL, MILAGRO,
        ISLA_PUERTO, TEMPLO_ISLA, COCINA_TORNEO, VOLCAN_HIELO,
        TORMENTA_AZOTEA, RING_NEON, DESIERTO_SOL
    }

    /** Efecto de animación sencillo de la viñeta. */
    public enum Animacion {
        ENTRADA, ZOOM, SACUDIDA, DESTELLO, LLUVIA, CONFETI
    }

    /** Momento narrativo dentro del capítulo. */
    public enum Momento {
        INICIO, GIRO, FINAL
    }

    /** Una viñeta ilustrada con decorado, personajes, narración y bocadillos. */
    public static final class Vineta {
        public final Decorado decorado;
        public final Animacion animacion;
        public final String modeloIzq;
        public final String modeloDer;
        public final boolean mostrarEntrenador;
        public final boolean entrenadorEnCamilla;
        public final boolean mostrarPublico;
        public final Trilingue narracion;
        public final Trilingue bocadilloIzq;
        public final Trilingue bocadilloDer;

        /** Crea una viñeta ilustrada completa. */
        public Vineta(Decorado decorado, Animacion animacion, String modeloIzq, String modeloDer,
                      boolean mostrarEntrenador, boolean entrenadorEnCamilla, boolean mostrarPublico,
                      Trilingue narracion, Trilingue bocadilloIzq, Trilingue bocadilloDer) {
            this.decorado = decorado == null ? Decorado.RING_NEON : decorado;
            this.animacion = animacion == null ? Animacion.ENTRADA : animacion;
            this.modeloIzq = modeloIzq == null ? "cobalto" : modeloIzq;
            this.modeloDer = modeloDer == null ? "chatarra" : modeloDer;
            this.mostrarEntrenador = mostrarEntrenador;
            this.entrenadorEnCamilla = entrenadorEnCamilla;
            this.mostrarPublico = mostrarPublico;
            this.narracion = narracion == null ? new Trilingue("", "", "") : narracion;
            this.bocadilloIzq = bocadilloIzq == null ? new Trilingue("", "", "") : bocadilloIzq;
            this.bocadilloDer = bocadilloDer == null ? new Trilingue("", "", "") : bocadilloDer;
        }

        /** Texto de narración resuelto en el idioma dado, con marcadores sustituidos y declinados. */
        public String textoNarracion(Idioma idioma, String entrenador, String rival, String pieza) {
            return resolver(narracion.en(idioma), idioma, entrenador, rival, pieza);
        }

        /** Texto del bocadillo izquierdo resuelto en el idioma dado. */
        public String textoBocadilloIzq(Idioma idioma, String entrenador, String rival, String pieza) {
            return resolver(bocadilloIzq.en(idioma), idioma, entrenador, rival, pieza);
        }

        /** Texto del bocadillo derecho resuelto en el idioma dado. */
        public String textoBocadilloDer(Idioma idioma, String entrenador, String rival, String pieza) {
            return resolver(bocadilloDer.en(idioma), idioma, entrenador, rival, pieza);
        }

        /** Aplica sustitución de marcadores {entrenador}, {rival} y {pieza} con concordancia gramatical. */
        public static String resolver(String plantilla, Idioma idioma, String entrenador, String rival, String pieza) {
            if (plantilla == null || plantilla.isEmpty()) return "";
            String s = plantilla;
            if (s.contains("{pieza}")) {
                String pz = pieza == null || pieza.isBlank() ? piezaPorDefecto(idioma) : pieza;
                s = Concordancia.ajustar(s, pz, idioma).replace("{pieza}", pz);
            }
            if (entrenador != null) s = s.replace("{entrenador}", entrenador);
            if (rival != null) s = s.replace("{rival}", rival);
            return s;
        }

        private static String piezaPorDefecto(Idioma idioma) {
            if (idioma == Idioma.DE) return "linker Arm";
            return idioma == Idioma.EN ? "left arm" : "brazo izquierdo";
        }
    }

    /** Controlador ligero y determinista para avanzar o saltar una escena sin hilos ni recursos gráficos. */
    public static final class Controlador {
        private final EscenaDeCampana escena;
        private int indice;
        private float tiempoVineta;
        private boolean terminada;

        /** Inicia el controlador en la primera viñeta. */
        public Controlador(EscenaDeCampana escena) {
            this.escena = escena;
            this.terminada = escena == null || escena.vinetas.isEmpty();
        }

        /** Avanza el reloj interno de la viñeta actual. */
        public void avanzarTiempo(float dt) {
            if (!terminada && dt > 0) tiempoVineta += dt;
        }

        /** Pasa a la siguiente viñeta o termina la escena si era la última. */
        public void siguiente() {
            if (terminada) return;
            if (indice + 1 < escena.vinetas.size()) {
                indice++;
                tiempoVineta = 0f;
            } else {
                terminada = true;
            }
        }

        /** Salta inmediatamente toda la escena. */
        public void saltar() {
            terminada = true;
        }

        /** Índice actual de viñeta (0-based). */
        public int indice() {
            return indice;
        }

        /** Total de viñetas de la escena. */
        public int total() {
            return escena == null ? 0 : escena.vinetas.size();
        }

        /** Segundos transcurridos en la viñeta actual. */
        public float tiempoVineta() {
            return tiempoVineta;
        }

        /** Si la escena ha terminado o fue saltada. */
        public boolean terminada() {
            return terminada;
        }

        /** Viñeta actual, o null si la escena está vacía. */
        public Vineta actual() {
            if (escena == null || escena.vinetas.isEmpty()) return null;
            return escena.vinetas.get(Math.min(indice, escena.vinetas.size() - 1));
        }
    }

    public final String id;
    public final Momento momento;
    public final Trilingue titulo;
    public final List<Vineta> vinetas;

    /** Crea una escena ilustrada con su lista inmutable de viñetas. */
    public EscenaDeCampana(String id, Momento momento, Trilingue titulo, List<Vineta> vinetas) {
        this.id = id == null ? "" : id;
        this.momento = momento == null ? Momento.INICIO : momento;
        this.titulo = titulo == null ? new Trilingue("", "", "") : titulo;
        this.vinetas = vinetas == null ? List.of() : Collections.unmodifiableList(vinetas);
    }

    /** Crea un controlador nuevo para reproducir o saltar esta escena. */
    public Controlador controlador() {
        return new Controlador(this);
    }
}
