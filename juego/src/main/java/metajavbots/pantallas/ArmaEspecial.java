package metajavbots.pantallas;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import metajavbots.Juego;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.ejercicios.Comprobador;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.textos.Trilingue;

/**
 * ARMA ESPECIAL — Una vez por pelea, en la segunda ronda, la pelea se congela
 * y aparece un arma especialmente fuerte con una cuenta atrás de números
 * grandes que parpadean. Para cogerla hay que resolver su reto de código
 * (res/especiales/retos.txt), que es justo lo que hace falta para apuntar:
 * la distancia (Martillo Titán), el ángulo (Cañón de Plasma) o el ángulo con
 * adelanto (Arpón Trueno); cuanto más difícil, más tiempo y más fuerte. En la
 * arena se dibuja una ayuda para apuntar (la línea al rival, dx, dy y el
 * ángulo). Si sale bien, el robot corre al arma, la coge y dispara (o carga);
 * a medias, falla el tiro; si no, puede cogerla la computadora o se desvanece.
 * Son aparte de los aparatos de la arena.
 */
public final class ArmaEspecial {

    /** Las armas: su prefijo de reto, rango (0 Noob, 1 Junior, 2 Senior), nombre, si dispara o carga, cuenta atrás (s), daño y colores. */
    enum Tipo {
        MARTILLO("especial_martillo", 0, new Trilingue("Martillo Titán", "Titanhammer", "Titan Hammer"), false, 45, 16, 0xB8C0C8, 0xFFC857),
        LANZA("especial_lanza", 0, new Trilingue("Lanza de Chispas", "Funkenlanze", "Spark Lance"), false, 45, 17, 0xD48A3B, 0xFFE27A),
        ESCUDO("especial_escudo", 0, new Trilingue("Pulso de Escudo", "Schildimpuls", "Shield Pulse"), false, 45, 15, 0x4E8C7A, 0x7CF2C6),
        CANON("especial_canon", 1, new Trilingue("Cañón de Plasma", "Plasmakanone", "Plasma Cannon"), true, 65, 26, 0x6B3FA0, 0xD08CFF),
        RAYO("especial_rayo", 1, new Trilingue("Rayo Giratorio", "Drehstrahl", "Gyro Beam"), true, 65, 25, 0x2F7E9E, 0x7CE6FF),
        SIERRA("especial_sierra", 1, new Trilingue("Brazo Sierra", "Sägearm", "Saw Arm"), false, 60, 27, 0xA84A3E, 0xFF9B6A),
        ARPON("especial_arpon", 2, new Trilingue("Arpón Trueno", "Donnerharpune", "Thunder Harpoon"), true, 90, 36, 0x3A6EA5, 0x9FD8FF),
        MISIL("especial_misil", 2, new Trilingue("Misil Rastreador", "Suchrakete", "Seeker Missile"), true, 85, 35, 0x8C3A52, 0xFF8FA3),
        TORRETA("especial_torreta", 2, new Trilingue("Torreta Enjambre", "Schwarmgeschütz", "Swarm Turret"), true, 90, 38, 0x4F5D95, 0xA5C4FF);

        final String reto;
        final int nivel;
        final Trilingue nombre;
        final boolean dispara;
        final int segundos, dano, color, acento;

        Tipo(String reto, int nivel, Trilingue nombre, boolean dispara, int segundos, int dano, int color, int acento) {
            this.reto = reto;
            this.nivel = nivel;
            this.nombre = nombre;
            this.dispara = dispara;
            this.segundos = segundos;
            this.dano = dano;
            this.color = color;
            this.acento = acento;
        }

        boolean casa(String idReto) {
            return idReto.equals(reto) || idReto.startsWith(reto + "_");
        }
    }

    private static final class Candidato {
        final Tipo tipo;
        final Ejercicio reto;

        Candidato(Tipo tipo, Ejercicio reto) {
            this.tipo = tipo;
            this.reto = reto;
        }
    }

    private enum Fase { ESPERA, CUENTA, ANIMACION, HECHA }

    private final Tipo tipo;
    private final Ejercicio reto;
    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private Fase fase = Fase.ESPERA;
    private float resta, t;
    private double ax, ay;
    /** Quién la usa (0 el jugador, 1 la computadora; -1 nadie) y si acierta. */
    private int quien = -1;
    private boolean acierta;
    private double desdeX, desdeY;
    private volatile Comprobador.Informe informe;
    private volatile boolean comprobando;
    private String mensaje = "";
    /** Lo último que dijo el comprobador (para saber si iba a medias al acabarse el tiempo). */
    private Comprobador.Informe ultimo;
    private boolean disparado;

    private static String ultimoReto = "";

    private ArmaEspecial(Tipo tipo, Ejercicio reto) {
        this.tipo = tipo;
        this.reto = reto;
    }

    /** Rango de arma especial para una etiqueta: 0 Noob, 1 Junior, 2 Senior/Master. */
    public static int nivelDe(Etiqueta etiqueta) {
        return Math.min(2, Math.max(0, etiqueta.ordinal() / 4));
    }

    /** La de esta pelea, según el nivel del jugador (filtra por rango y elige un reto al azar sin repetir el último). */
    public static ArmaEspecial para(Jugador jug) {
        int nivel = nivelDe(jug.etiqueta);
        try (InputStream in = ArmaEspecial.class.getResourceAsStream("/especiales/retos.txt")) {
            List<Ejercicio> todos = Ejercicio.leer(in, "retos.txt");
            List<Candidato> candidatos = new ArrayList<>();
            for (Ejercicio e : todos) {
                for (Tipo t : Tipo.values()) {
                    if (t.nivel == nivel && t.casa(e.id)) {
                        candidatos.add(new Candidato(t, e));
                        break;
                    }
                }
            }
            if (candidatos.isEmpty()) return null;
            List<Candidato> sinRepetir = new ArrayList<>();
            for (Candidato c : candidatos) if (!c.reto.id.equals(ultimoReto)) sinRepetir.add(c);
            List<Candidato> bolsa = sinRepetir.isEmpty() ? candidatos : sinRepetir;
            Candidato elegido = bolsa.get(ThreadLocalRandom.current().nextInt(bolsa.size()));
            ultimoReto = elegido.reto.id;
            return new ArmaEspecial(elegido.tipo, elegido.reto);
        } catch (Exception e) {
            // sin reto, sin arma especial
        }
        return null;
    }

    /** Identificador del reto elegido para esta arma especial. */
    public String idReto() {
        return reto.id;
    }

    /** Rango (0 Noob, 1 Junior, 2 Senior) del arma elegida. */
    public int nivel() {
        return tipo.nivel;
    }

    /** Nombre del arma en el idioma indicado. */
    public String nombreArma(Idioma idioma) {
        return tipo.nombre.en(idioma);
    }

    boolean activa() {
        return fase == Fase.CUENTA || fase == Fase.ANIMACION;
    }

    /** ¿Toca que salga ahora? (A los 10 s de la segunda ronda.) */
    boolean toca(Pelea p) {
        return fase == Fase.ESPERA && p.ronda() == 2 && p.turno() >= 300 && p.estado() == Pelea.Estado.LUCHANDO;
    }

    public void empezar(Juego j, Pelea p) {
        Cuerpo a = p.cuerpo(0), b = p.cuerpo(1);
        // A medio camino de los dos, un poco apartada.
        ax = Math.max(80, Math.min(p.ancho - 80, (a.x + b.x) / 2 + (a.y - b.y) * 0.25));
        ay = Math.max(80, Math.min(p.alto - 80, (a.y + b.y) / 2 + (b.x - a.x) * 0.25));
        resta = tipo.segundos;
        editor.ponerTexto(reto.codigoInicial(j.jugador.idioma));
        editor.tamano = j.jugador.tamanoDeLetra;
        editor.enfocada = true;
        editor.alCompilar = () -> comprobar(j);
        fase = Fase.CUENTA;
        j.sonidos.tocar("campana", 1);
    }

    private void comprobar(Juego j) {
        if (comprobando) return;
        comprobando = true;
        String codigo = editor.doc.texto();
        Thread h = new Thread(() -> {
            try {
                informe = Comprobador.comprobar(reto, codigo, j.jugador.idioma.locale());
            } finally {
                comprobando = false;
            }
        }, "arma-especial");
        h.setDaemon(true);
        h.start();
    }

    /** Un fotograma: la cuenta atrás (y la comprobación) o la animación. */
    public void avanzar(Juego j, Pelea p, VistaDeArena vista, float dt) {
        t += dt;
        if (fase == Fase.CUENTA) {
            Comprobador.Informe inf = informe;
            if (inf != null && !comprobando) {
                informe = null;
                if (inf.todoBien()) {
                    empezarAnimacion(p, 0, true);
                    return;
                }
                mensaje = inf.compila() ? T.f("especial.casi", inf.bien(), inf.casos.size()) : T.t("especial.no_compila");
                ultimo = inf;
                j.sonidos.tocar("error", 0.6f);
            }
            resta -= dt;
            if (resta <= 0) seAcaba(p);
            return;
        }
        if (fase == Fase.ANIMACION) animar(j, p, vista);
    }

    /** Se acabó el tiempo: a medias, la coge y falla; si no, quizá la computadora; si no, se desvanece. */
    private void seAcaba(Pelea p) {
        if (ultimo != null && ultimo.compila() && ultimo.bien() * 2 >= ultimo.casos.size()) empezarAnimacion(p, 0, false);
        else if (p.combatientes[1].computadora && Math.random() < 0.4) empezarAnimacion(p, 1, true);
        else {
            mensaje = T.t("especial.desvanece");
            fase = Fase.HECHA;
        }
    }

    private void empezarAnimacion(Pelea p, int quien, boolean acierta) {
        this.quien = quien;
        this.acierta = acierta;
        Cuerpo c = p.cuerpo(quien);
        desdeX = c.x;
        desdeY = c.y;
        t = 0;
        fase = Fase.ANIMACION;
    }

    /** Corre al arma, la coge y dispara (se aparta, se gira) o carga contra el rival. */
    private void animar(Juego j, Pelea p, VistaDeArena vista) {
        Cuerpo c = p.cuerpo(quien), e = p.cuerpo(1 - quien);
        if (t < 0.9f) {
            float f = t / 0.9f;
            c.x = desdeX + (ax - desdeX) * f;
            c.y = desdeY + (ay - desdeY) * f;
            c.rumbo = Math.toDegrees(Math.atan2(ay - desdeY, ax - desdeX));
            return;
        }
        double haciaEl = Math.toDegrees(Math.atan2(e.y - c.y, e.x - c.x));
        if (tipo.dispara) {
            if (t < 1.4f) {
                double atras = Math.toRadians(haciaEl + 180);
                c.x = ax + Math.cos(atras) * 50 * (t - 0.9f) / 0.5f;
                c.y = ay + Math.sin(atras) * 50 * (t - 0.9f) / 0.5f;
            }
            c.mira = haciaEl + (acierta ? 0 : 35);
            c.rumbo = c.mira;
            if (t >= 1.8f && !disparado) disparar(j, p, vista);
        } else {
            if (t < 1.6f) {
                float f = (t - 0.9f) / 0.7f;
                double llega = Math.max(0, Math.hypot(e.x - ax, e.y - ay) - c.radio - e.radio + (acierta ? 0 : 90));
                double rad = Math.toRadians(haciaEl + (acierta ? 0 : 40));
                c.x = ax + Math.cos(rad) * llega * f;
                c.y = ay + Math.sin(rad) * llega * f;
                c.rumbo = Math.toDegrees(rad);
            } else if (!disparado) disparar(j, p, vista);
        }
        if (t > 2.7f) fase = Fase.HECHA;
    }

    private void disparar(Juego j, Pelea p, VistaDeArena vista) {
        disparado = true;
        if (acierta) {
            p.golpeEspecial(quien, 1 - quien, quien == 0 ? tipo.dano : tipo.dano * 0.6, tipo.dispara ? "laser" : "cuerpo");
            vista.sacudida = 12;
            j.sonidos.tocar("explosion", 1);
            mensaje = quien == 0 ? T.t("especial.acierto") : T.t("especial.la_cogio");
        } else {
            j.sonidos.tocar("fallo", 0.8f);
            mensaje = T.t("especial.fallo");
        }
    }

    // ================================================================ lo que se ve

    /** Encima de la arena: el arma, la ayuda para apuntar, la cuenta atrás y el panel del reto. */
    public void dibujar(Juego j, VistaDeArena v, Pelea p) {
        Pincel pi = j.pincel;
        Ui ui = j.ui;
        if (fase == Fase.ESPERA) return;
        Cuerpo yo = p.cuerpo(0), el = p.cuerpo(1);
        if (fase == Fase.CUENTA) ayudaParaApuntar(pi, ui, v, yo, el);
        arma(pi, v, p);
        if (fase == Fase.ANIMACION && tipo.dispara && disparado && t < 2.3f) {
            Cuerpo c = p.cuerpo(quien), e = p.cuerpo(1 - quien);
            double rad = Math.toRadians(c.mira), largo = Math.hypot(e.x - c.x, e.y - c.y) + (acierta ? 0 : 200);
            float x1 = v.sx(c.x, c.y), y1 = v.sy(c.y) - 60 * v.escala(c.y);
            double fx = c.x + Math.cos(rad) * largo, fy = c.y + Math.sin(rad) * largo;
            float x2 = v.sx(fx, fy), y2 = v.sy(fy) - 60 * v.escala(fy);
            pi.linea(x1, y1, x2, y2, 14, tipo.acento, 0.5f);
            pi.linea(x1, y1, x2, y2, 5, 0xFFFFFF, 0.95f);
        }
        if (!mensaje.isEmpty() && fase != Fase.CUENTA) {
            pi.textoCentrado(mensaje, pi.ancho / 2, 160, ui.letra * 1.5f, true, 0xFFE27A, Math.max(0, Math.min(1, 3.2f - t)));
        }
        if (fase == Fase.CUENTA) panel(j);
    }

    /** El arma en el suelo (girando y con brillo), o en la mano de quien la ha cogido. */
    private void arma(Pincel pi, VistaDeArena v, Pelea p) {
        double x = ax, y = ay;
        boolean enLaMano = fase == Fase.ANIMACION && t >= 0.9f;
        if (enLaMano) {
            Cuerpo c = p.cuerpo(quien);
            x = c.x + Math.cos(Math.toRadians(c.mira)) * 30;
            y = c.y + Math.sin(Math.toRadians(c.mira)) * 30;
        }
        if (fase == Fase.HECHA) return;
        float cx = v.sx(x, y), cy = v.sy(y) - (enLaMano ? 55 : 40) * v.escala(y), s = 26 * v.escala(y);
        if (!enLaMano) {
            pi.brillo(cx, cy, s * 3, 0xFFE27A, 0.4f + 0.2f * (float) Math.sin(t * 8));
            pi.aro(v.sx(x, y), v.sy(y), s * 1.6f, 3, 0xFFE27A, 0.8f);
        }
        if (tipo.dispara) {
            pi.caja(cx - s, cy - s * 0.35f, s * 1.8f, s * 0.7f, s * 0.2f, tipo.color, 1);
            pi.rect(cx + s * 0.6f, cy - s * 0.15f, s * 0.9f, s * 0.3f, 0xD9DEE4, 1);
            pi.circulo(cx - s * 0.4f, cy, s * 0.22f, tipo.acento, 1);
        } else {
            pi.rect(cx - s * 0.12f, cy - s * 0.2f, s * 0.24f, s * 1.6f, 0x8A5A3A, 1);
            pi.caja(cx - s * 0.8f, cy - s * 0.75f, s * 1.6f, s * 0.7f, s * 0.15f, tipo.color, 1);
            pi.rect(cx - s * 0.8f, cy - s * 0.5f, s * 1.6f, s * 0.12f, tipo.acento, 1);
        }
    }

    /** La línea al rival, sus dos lados (dx y dy) y el arco del ángulo, con los números. */
    private void ayudaParaApuntar(Pincel pi, Ui ui, VistaDeArena v, Cuerpo yo, Cuerpo el) {
        float x1 = v.sx(yo.x, yo.y), y1 = v.sy(yo.y), x2 = v.sx(el.x, el.y), y2 = v.sy(el.y), xc = v.sx(el.x, yo.y);
        pi.linea(x1, y1, xc, y1, 3, 0x5BD685, 0.9f);
        pi.linea(xc, y1, x2, y2, 3, 0x3FA9F5, 0.9f);
        pi.linea(x1, y1, x2, y2, 3, 0xFFE27A, 0.9f);
        for (int k = 0; k < 10; k++) {
            double a = Math.atan2(y2 - y1, x2 - x1) * k / 10, b = Math.atan2(y2 - y1, x2 - x1) * (k + 1) / 10;
            pi.linea(x1 + (float) Math.cos(a) * 50, y1 + (float) Math.sin(a) * 50, x1 + (float) Math.cos(b) * 50, y1 + (float) Math.sin(b) * 50, 2,
                    0xFFE27A, 0.8f);
        }
        float tl = ui.letra * 0.85f;
        etiqueta(pi, "dx = " + Math.round(el.x - yo.x), (x1 + xc) / 2 - 40, y1 + 8, tl, 0x5BD685);
        etiqueta(pi, "dy = " + Math.round(el.y - yo.y), xc + 10, (y1 + y2) / 2, tl, 0x3FA9F5);
    }

    /** Un número de la ayuda, con su fondo oscuro (que se lea encima de lo que haya). */
    private static void etiqueta(Pincel pi, String s, float x, float y, float tl, int color) {
        pi.caja(x - 6, y - 3, pi.ancho(s, tl, true) + 12, tl * 1.4f, 6, 0x0B0F18, 0.85f);
        pi.texto(s, x, y, tl, true, color, 1);
    }

    /** Mientras dura el reto, la arena va a la izquierda y el panel a la derecha (que no tape nada). */
    static float anchoDeLaArena(float anchoDePantalla) {
        return Math.max(anchoDePantalla * 0.48f, anchoDePantalla - 840);
    }

    /** La cuenta atrás (grande, parpadea al final) y el panel con el reto, el editor y los botones. */
    private void panel(Juego j) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        int s = (int) Math.ceil(resta);
        boolean apura = resta < 10;
        float alfa = apura ? 0.5f + 0.5f * (float) Math.abs(Math.sin(t * 8)) : 0.75f + 0.25f * (float) Math.abs(Math.sin(t * 3));
        String reloj = s / 60 + ":" + String.format("%02d", s % 60);
        float centro = anchoDeLaArena(p.ancho) / 2;
        p.textoCentrado(reloj, centro, 96, ui.letra * 4, true, apura ? 0xFF4A4A : 0xFFE27A, alfa);
        p.textoCentrado(tipo.nombre.en(T.idioma) + " · " + T.f("especial.dano", tipo.dano), centro, 96 + ui.letra * 4.6f, ui.letra * 1.1f, true, 0xFFFFFF, 1);
        float x = anchoDeLaArena(p.ancho) + 10, w = p.ancho - x - 20, y = 100, h = p.alto - 190;
        float cy = ui.panel(x, y, w, h, reto.titulo(T.idioma));
        cy += p.parrafo(reto.enunciado.en(T.idioma), x + 16, cy, w - 32, ui.letra * 0.85f, ui.tema.texto, 1) + 10;
        float alto = y + h - cy - 80;
        editor.dibujar(ui, x + 12, cy, w - 24, alto);
        float by = y + h - 64;
        if (ui.boton(comprobando ? T.t("especial.comprobando") : T.t("especial.comprobar"), x + 16, by, 260, 50, Ui.Estilo.PRINCIPAL, !comprobando,
                null)) comprobar(j);
        if (ui.boton(T.t("especial.dejarla"), x + w - 236, by, 220, 50, Ui.Estilo.PLANO)) resta = 0;
        if (!mensaje.isEmpty()) p.parrafo(mensaje, x + 290, by + 4, w - 540, ui.letra * 0.8f, ui.tema.aviso, 1);
    }
}
