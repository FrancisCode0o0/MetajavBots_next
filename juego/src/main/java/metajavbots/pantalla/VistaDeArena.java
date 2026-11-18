package metajavbots.pantalla;

import java.util.Random;

import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.combate.Proyectil;
import metajavbots.combate.Suceso;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.sonido.Sonidos;

/**
 * VISTA DE LA ARENA — Dibuja una pelea: el escenario (hangar, desierto,
 * ciudad de neón, laboratorio o luna), el suelo en perspectiva, los robots
 * ordenados por profundidad, los láseres, las balas, los misiles con su
 * estela, las llamas, las chispas, el humo de los robots dañados y los
 * bocadillos. Convierte cada Suceso de la pelea en efectos y sonidos.
 */
public final class VistaDeArena {

    /** Los escenarios: los cinco de siempre y, detrás, los de las arenas con trampas (combate.Arena). */
    public enum Escenario { HANGAR, DESIERTO, NEON, LABORATORIO, LUNA, CHATARRERIA, COCINA, HIELO, TEJADO }

    public Escenario escenario = Escenario.HANGAR;
    /** Lo averiado que está el robot del jugador sin contar los golpes (un robot roto a medio reparar): humo y chispas. */
    public float averiaDelJugador;
    /** A qué altura (en unidades de la arena) salen los disparos. */
    private static final float TIRO = 58;
    public final Efectos efectos = new Efectos();
    private final Trastos trastos = new Trastos();
    private final DibujoRobot dibujo;
    private DibujoRobot.Estado[] estados = {new DibujoRobot.Estado(), new DibujoRobot.Estado()};
    private final Random azar = new Random(11);
    private float tiempo;
    /** Lo que dice cada robot (el bocadillo) y cuánto le queda. */
    private String[] bocadillo = {"", ""};
    private float[] tiempoBocadillo = new float[2];
    // El rectángulo de pantalla donde está la arena.
    private float ax, ay, aw, ah, escalaX;
    private double ancho = 1000, alto = 700;
    public float sacudida;
    /** Una pausa cortísima tras un golpe fuerte (los segundos que quedan): hace que el golpe se sienta. */
    public float parada;
    /** La alegría de los robots (al subir de nivel saltan). */
    public float alegria;

    public VistaDeArena(Pincel p) {
        dibujo = new DibujoRobot(p);
    }

    /** Para una pelea de varios: un estado y un bocadillo para cada uno. */
    private void cuantos(int n) {
        if (estados.length == n) return;
        estados = new DibujoRobot.Estado[n];
        for (int i = 0; i < n; i++) estados[i] = new DibujoRobot.Estado();
        bocadillo = new String[n];
        java.util.Arrays.fill(bocadillo, "");
        tiempoBocadillo = new float[n];
    }

    /** Los colores de los equipos (los láseres y los nombres). */
    public static final int[] COLORES = {0x3FE0FF, 0xFF4FD8, 0x9CFF57, 0xFFC857, 0xB07CFF, 0xFF7A45};

    public void decir(int quien, String texto) {
        if (quien < 0 || quien >= bocadillo.length || texto == null) return;
        bocadillo[quien] = texto;
        tiempoBocadillo[quien] = 2.5f + texto.length() * 0.04f;
    }

    // ================================================================ coordenadas

    private float f(double y) {
        return (float) (0.8 + 0.2 * (1 - y / alto));
    }

    public float sx(double x, double y) {
        return (float) (ax + aw / 2 + (x - ancho / 2) * escalaX * f(y));
    }

    /** Píxeles por unidad de la arena, a lo ancho, a esa profundidad. */
    public float escala(double y) {
        return escalaX * f(y);
    }

    /** Píxeles por unidad de la arena, en profundidad (el suelo se ve aplastado). */
    public float escalaY() {
        return (float) (ah * 0.62f / alto);
    }

    public float sy(double y) {
        float suelo = ah * 0.62f;
        return (float) (ay + ah * 0.3f + (1 - y / alto) * suelo);
    }

    // ================================================================ sucesos → efectos y sonidos

    /** Lo que se ve y se oye de un aparato: sale, se coge, se monta, se usa o no engancha. */
    private void aparato(Suceso s, float px, Sonidos sonidos) {
        float suelo = sy(s.y);
        switch (s.texto) {
            case "aparato_sale": efectos.anadir(Efectos.Tipo.ONDA, px, suelo, 0, 0, 0.6f, 90 * escalaX, 0xFFE38A); break;
            case "coger": case "conectado": efectos.chispas(px, suelo - 50 * escalaX, 10, 0xFFE38A); break;
            case "aparato_descarga": efectos.anadir(Efectos.Tipo.ONDA, px, suelo - 30 * escalaX, 0, 0, 0.5f, 150 * escalaX, 0xFFF4A0); break;
            case "aparato_chorro": efectos.humo(px, suelo - 20 * escalaX, 0.8f); break;
            default: break;
        }
        if (sonidos == null) return;
        String que;
        switch (s.texto) {
            case "hueco": que = "error"; break;
            case "aparato_chorro": que = "turbo"; break;
            case "aparato_descarga": que = "pulso"; break;
            case "aparato_lanzallamas": que = "llama"; break;
            case "aparato_antigravedad": que = "escudo"; break;
            case "conectado": case "aparato_sale": que = "reparar"; break;
            default: que = "servo"; break;
        }
        sonidos.tocar(que, 0.7f);
    }

    public void sucesos(Pelea pelea, Sonidos sonidos) {
        for (Suceso s : pelea.sucesos()) {
            float px = sx(s.x, s.y), py = sy(s.y) - TIRO * escalaX;
            switch (s.tipo) {
                case LASER: if (sonidos != null) sonidos.tocar(s.valor > 10 ? "laser_pesado" : "laser", 0.6f, 0.9f + azar.nextFloat() * 0.2f); break;
                case DISPARO: if (sonidos != null) sonidos.tocar(s.valor > 1 ? "disparo" : "ametralladora", 0.5f); break;
                case MISIL: if (sonidos != null) sonidos.tocar("misil", 0.5f); break;
                case LLAMA: if (sonidos != null) sonidos.tocar("llama", 0.5f); break;
                case GOLPE_CUERPO:
                    efectos.chispas(px, py, 18, 0xFFE38A);
                    sacudida = Math.max(sacudida, 6);
                    if (sonidos != null) sonidos.tocar(s.valor > 20 ? "golpe_fuerte" : "golpe_metal", 0.8f);
                    break;
                case FALLO_CUERPO: if (sonidos != null) sonidos.tocar("fallo", 0.4f); break;
                case IMPACTO: {
                    float fuerza = (float) Math.min(1.5, s.valor / 10);
                    efectos.chispas(px, py, 4 + (int) (fuerza * 10), s.texto.equals("laser") ? 0x9FF7FF : 0xFFD27A);
                    if (s.valor > 8) sacudida = Math.max(sacudida, 3 + fuerza * 4);
                    if (s.valor > 12) parada = Math.max(parada, 0.05f + Math.min(0.05f, (float) s.valor / 600));
                    if (sonidos != null && !s.texto.equals("llama") && !s.texto.equals("pared")) sonidos.tocar("chispa", 0.4f + fuerza * 0.3f);
                    break;
                }
                case EXPLOSION:
                    efectos.explosion(px, sy(s.y) - 10 * escalaX, 0.6f);
                    if (sonidos != null) sonidos.tocar("explosion", 0.7f);
                    sacudida = Math.max(sacudida, 5);
                    break;
                case ESQUIVA: if (sonidos != null) sonidos.tocar("fallo", 0.3f, 1.4f); break;
                case BLOQUEO:
                    efectos.chispas(px, py, 10, 0x9FE7FF);
                    if (sonidos != null) sonidos.tocar("escudo", 0.5f);
                    break;
                case APARATO: aparato(s, px, sonidos); break;
                case TRAMPA:
                    efectos.chispas(px, sy(s.y) - 20, s.texto.equals("sierra") ? 10 : 6, s.texto.equals("rayo") ? 0xBFE6FF : 0xFFE38A);
                    if (s.texto.equals("foso")) efectos.anadir(Efectos.Tipo.ONDA, sx(s.x, s.y), sy(s.y), 0, 0, 0.6f, 120 * escalaX, 0x101722);
                    if (sonidos != null) sonidos.tocar(s.texto.equals("foso") ? "pared" : "choque", 0.7f);
                    break;
                case PARED: case CHOQUE:
                    efectos.chispas(px, sy(s.y) - 10, 6, 0xFFFFFF);
                    if (sonidos != null) sonidos.tocar(s.tipo == Suceso.Tipo.PARED ? "pared" : "choque", 0.6f);
                    break;
                case PARTE_ROTA:
                    efectos.explosion(px, py, 0.5f);
                    if (sonidos != null) sonidos.tocar("parte_rota", 0.8f);
                    sacudida = Math.max(sacudida, 8);
                    break;
                case FUERA_DE_COMBATE:
                    efectos.explosion(px, py, 1f);
                    if (sonidos != null) sonidos.tocar("explosion", 1f);
                    sacudida = Math.max(sacudida, 12);
                    break;
                case ESPECIAL:
                    if (sonidos != null) {
                        String que = s.texto.equals("escudo") || s.texto.equals("escudo_brazo") ? "escudo" : s.texto.equals("turbo") ? "turbo"
                                : s.texto.equals("pulso") ? "pulso" : s.texto.equals("camuflaje") ? "camuflaje" : "servo";
                        sonidos.tocar(que, 0.7f);
                    }
                    if (s.texto.equals("pulso")) efectos.anadir(Efectos.Tipo.ONDA, px, py, 0, 0, 0.5f, 240 * escalaX, 0x9BE7FF);
                    break;
                case REPARACION: if (sonidos != null) sonidos.tocar("reparar", 0.6f); break;
                case ATURDIDO: if (sonidos != null) sonidos.tocar("pulso", 0.4f, 1.5f); break;
                case DECIR: decir(s.quien, s.texto); break;
                case ERROR_CODIGO: if (sonidos != null) sonidos.tocar("error", 0.5f); break;
                case TOPE: if (sonidos != null) sonidos.tocar("tope", 0.3f); break;
                case RONDA_EMPIEZA: case RONDA_ACABA: if (sonidos != null) sonidos.tocar("campana", 0.8f); break;
                default: break;
            }
        }
    }

    // ================================================================ dibujar

    public void dibujar(Pincel p, Pelea pelea, float x, float y, float w, float h, float dt) {
        tiempo += dt;
        ancho = pelea.ancho;
        alto = pelea.alto;
        float temblor = sacudida > 0.1f ? (azar.nextFloat() * 2 - 1) * sacudida : 0;
        sacudida *= 0.88f;
        ax = x + temblor;
        ay = y + temblor * 0.6f;
        aw = w;
        ah = h;
        escalaX = (float) (aw * 0.94 / ancho);
        p.recortar(x, y, w, h);
        fondo(p);
        suelo(p);
        DibujoDeArenas.delSuelo(p, this, pelea.arena, tiempo);
        for (metajavbots.combate.Aparatos.Uno a : pelea.aparatos.todos()) {
            if (a.dueno < 0) DibujoDeAparatos.enElSuelo(p, this, a, tiempo, letra);
        }
        // Lo que está más lejos (y mayor), antes.
        cuantos(pelea.combatientes.length);
        // Lo que está más lejos (y mayor), antes.
        java.util.List<Cuerpo> orden = new java.util.ArrayList<>();
        for (int i = 0; i < pelea.combatientes.length; i++) if (pelea.cuerpo(i) != null) orden.add(pelea.cuerpo(i));
        orden.sort((u, v) -> Double.compare(v.y, u.y));
        // Los objetos sólidos (barriles, chimeneas, el carrito) van entre los robots, por profundidad.
        java.util.List<metajavbots.combate.Arena.Trampa> solidos = new java.util.ArrayList<>();
        for (metajavbots.combate.Arena.Trampa t : pelea.arena.trampas()) if (t.solido) solidos.add(t);
        solidos.sort((u, v) -> Double.compare(v.y, u.y));
        int s = 0;
        for (Cuerpo c : orden) {
            while (s < solidos.size() && solidos.get(s).y >= c.y) DibujoDeArenas.solido(p, this, solidos.get(s++), tiempo);
            sombraYRobot(p, c, pelea, dt);
        }
        while (s < solidos.size()) DibujoDeArenas.solido(p, this, solidos.get(s++), tiempo);
        if (!orden.isEmpty()) {
            proyectiles(p, pelea);
            rayos(p, pelea);
            llamas(p, pelea);
        }
        efectos.avanzar(dt);
        efectos.dibujar(p);
        trastos.avanzar(dt);
        trastos.dibujar(p);
        DibujoDeArenas.lluvia(p, escenario, ax, ay, aw, ah, tiempo);
        rotulos(p, orden, pelea, dt);
        p.sinRecorte();
    }

    private void fondo(Pincel p) {
        if (DibujoDeArenas.esNueva(escenario)) {
            DibujoDeArenas.fondo(p, escenario, ax, ay, aw, ah, tiempo);
            return;
        }
        float cielo = ay + ah * 0.32f;
        switch (escenario) {
            case DESIERTO: {
                p.degradado(ax - 20, ay - 20, aw + 40, cielo - ay + 30, 0xF6A85E, 0xFCD9A0, 1);
                p.circulo(ax + aw * 0.75f, ay + ah * 0.14f, ah * 0.08f, 0xFFF4D6, 1);
                p.brillo(ax + aw * 0.75f, ay + ah * 0.14f, ah * 0.1f, 0xFFE3A3, 0.6f);
                for (int i = 0; i < 4; i++) {
                    float dx = ax + i * aw / 3f - aw * 0.1f;
                    p.elipse(dx, cielo, aw * 0.28f, ah * 0.08f, 0, Pincel.mezcla(0xC97B3D, 0xE9A86A, i / 4f), 1);
                }
                break;
            }
            case NEON: {
                p.degradado(ax - 20, ay - 20, aw + 40, cielo - ay + 30, 0x0B0624, 0x3A0F5C, 1);
                Random r = new Random(5);
                for (int i = 0; i < 26; i++) {
                    float bw = 30 + r.nextFloat() * 70, bh = 50 + r.nextFloat() * ah * 0.22f;
                    float bx = ax + r.nextFloat() * aw;
                    p.rect(bx, cielo - bh, bw, bh + 4, 0x150B2E, 1);
                    for (int k = 0; k < 6; k++) {
                        if (r.nextFloat() < 0.5f) p.rect(bx + 6 + r.nextFloat() * (bw - 14), cielo - bh + 8 + r.nextFloat() * (bh - 16), 5, 3,
                                r.nextBoolean() ? 0xFF3CAC : 0x2DE2E6, 0.6f + 0.4f * (float) Math.sin(tiempo * 2 + i + k));
                    }
                }
                p.rect(ax - 20, cielo - 3, aw + 40, 3, 0xFF3CAC, 0.8f);
                break;
            }
            case LABORATORIO: {
                p.degradado(ax - 20, ay - 20, aw + 40, cielo - ay + 30, 0xD8E2EA, 0xB9C7D3, 1);
                for (int i = 0; i < 7; i++) {
                    float px = ax + aw * (0.06f + i * 0.14f);
                    p.caja(px, ay + ah * 0.06f, aw * 0.1f, ah * 0.15f, 6, 0x2A3644, 1);
                    p.rect(px + 6, ay + ah * 0.06f + 6, aw * 0.1f - 12, ah * 0.15f - 12, 0x0E2A1E, 1);
                    for (int k = 0; k < 4; k++) {
                        float lw = (aw * 0.1f - 20) * (0.3f + 0.7f * (float) Math.abs(Math.sin(tiempo * 0.7 + i * 1.3 + k)));
                        p.rect(px + 10, ay + ah * 0.06f + 12 + k * 9, lw, 3, 0x5BD685, 0.9f);
                    }
                }
                break;
            }
            case LUNA: {
                p.degradado(ax - 20, ay - 20, aw + 40, cielo - ay + 30, 0x03040A, 0x0D1124, 1);
                Random r = new Random(9);
                for (int i = 0; i < 120; i++) {
                    float sxx = ax + r.nextFloat() * aw, syy = ay + r.nextFloat() * (cielo - ay);
                    p.circulo(sxx, syy, r.nextFloat() * 1.6f + 0.3f, 0xFFFFFF, 0.4f + 0.6f * (float) Math.abs(Math.sin(tiempo + i)));
                }
                p.circulo(ax + aw * 0.2f, ay + ah * 0.13f, ah * 0.09f, 0x3D7BD9, 1);
                p.circulo(ax + aw * 0.2f - ah * 0.02f, ay + ah * 0.12f, ah * 0.05f, 0x4CAF50, 0.8f);
                break;
            }
            case HANGAR:
            default: {
                p.degradado(ax - 20, ay - 20, aw + 40, cielo - ay + 30, 0x1B2230, 0x2C3646, 1);
                for (int i = 0; i < 9; i++) {
                    float px = ax + i * aw / 8f;
                    p.rect(px - 5, ay, 10, cielo - ay, 0x141A24, 1);
                    p.linea(px, ay + 10, px + aw / 8f, cielo - 20, 4, 0x141A24, 1);
                }
                for (int i = 0; i < 5; i++) {
                    float lx = ax + aw * (0.12f + i * 0.19f);
                    p.brillo(lx, ay + ah * 0.05f, 30, 0xFFF1C1, 0.5f);
                    p.circulo(lx, ay + ah * 0.05f, 6, 0xFFF8E1, 1);
                }
                break;
            }
        }
    }

    private void suelo(Pincel p) {
        int suelo1, suelo2, linea;
        int[] nuevo = DibujoDeArenas.suelo(escenario);
        if (nuevo != null) {
            suelo1 = nuevo[0];
            suelo2 = nuevo[1];
            linea = nuevo[2];
        } else switch (escenario) {
            case DESIERTO: suelo1 = 0xD8A066; suelo2 = 0xC4884E; linea = 0xB27740; break;
            case NEON: suelo1 = 0x150B2E; suelo2 = 0x0B0618; linea = 0x2DE2E6; break;
            case LABORATORIO: suelo1 = 0xE9EEF2; suelo2 = 0xCDD6DE; linea = 0xA5B3C0; break;
            case LUNA: suelo1 = 0x6E6F75; suelo2 = 0x4A4B50; linea = 0x5C5D62; break;
            default: suelo1 = 0x3C4656; suelo2 = 0x262E3A; linea = 0x4E5A6E; break;
        }
        // Fuera de la arena: el suelo de alrededor, más oscuro, hasta abajo del todo.
        float horizonte = ay + ah * 0.3f;
        p.degradado(ax - 20, horizonte, aw + 40, ah - (horizonte - ay) + 20, Pincel.oscurecer(suelo2, 0.35f), Pincel.oscurecer(suelo2, 0.6f), 1);
        float[] v = {sx(0, alto), sy(alto), sx(ancho, alto), sy(alto), sx(ancho, 0), sy(0), sx(0, 0), sy(0)};
        p.poligono(v, suelo2, 1);
        // Baldosas a cuadros, con la perspectiva.
        int nx = 10, ny = 7;
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                if ((i + j) % 2 != 0) continue;
                double x0 = ancho * i / nx, x1 = ancho * (i + 1) / nx, y0 = alto * j / ny, y1 = alto * (j + 1) / ny;
                p.poligono(new float[]{sx(x0, y1), sy(y1), sx(x1, y1), sy(y1), sx(x1, y0), sy(y0), sx(x0, y0), sy(y0)}, suelo1, 1);
            }
        }
        float grosor = escenario == Escenario.NEON ? 2 : 1;
        float brilloNeon = escenario == Escenario.NEON ? 0.35f + 0.25f * (float) Math.sin(tiempo * 2) : 0.5f;
        for (int i = 0; i <= nx; i++) p.linea(sx(ancho * i / nx, alto), sy(alto), sx(ancho * i / nx, 0), sy(0), grosor, linea, brilloNeon);
        for (int j = 0; j <= ny; j++) p.linea(sx(0, alto * j / ny), sy(alto * j / ny), sx(ancho, alto * j / ny), sy(alto * j / ny), grosor, linea, brilloNeon);
        // El borde de la arena: una banda con luces.
        int borde = escenario == Escenario.NEON ? 0xFF3CAC : escenario == Escenario.LABORATORIO ? 0xFFB000 : 0xFFC857;
        p.linea(sx(0, 0), sy(0), sx(ancho, 0), sy(0), 6, 0x111111, 0.8f);
        p.linea(sx(0, 0), sy(0) - 3, sx(ancho, 0), sy(0) - 3, 3, borde, 0.9f);
        p.linea(sx(0, alto), sy(alto), sx(ancho, alto), sy(alto), 3, borde, 0.6f);
        p.linea(sx(0, 0), sy(0), sx(0, alto), sy(alto), 3, borde, 0.7f);
        p.linea(sx(ancho, 0), sy(0), sx(ancho, alto), sy(alto), 3, borde, 0.7f);
        // El muro de delante: una banda de metal bajo el borde.
        float muro = 22;
        p.poligono(new float[]{sx(0, 0), sy(0), sx(ancho, 0), sy(0), sx(ancho, 0), sy(0) + muro, sx(0, 0), sy(0) + muro},
                Pincel.oscurecer(suelo2, 0.5f), 1);
        for (int i = 0; i < 20; i++) {
            float lx = sx(ancho * (i + 0.5) / 20, 0);
            p.linea(lx - 10, sy(0) + 4, lx + 10, sy(0) + muro - 4, 5, borde, 0.25f);
        }
        for (int i = 0; i <= 10; i++) {
            float lx = sx(ancho * i / 10, 0);
            p.circulo(lx, sy(0) - 3, 3, borde, 0.5f + 0.5f * (float) Math.abs(Math.sin(tiempo * 3 + i)));
        }
    }

    private void sombraYRobot(Pincel p, Cuerpo c, Pelea pelea, float dt) {
        DibujoRobot.Estado e = estados[c.indice];
        Combatiente co = c.combatiente;
        double mira = Math.toRadians(c.mira);
        e.lado = Math.cos(mira) >= 0 ? 1 : -1;
        e.alzado = (float) Math.sin(mira) * 0.55f;
        e.paso = (float) c.paso;
        e.tiempo = tiempo + c.indice * 1.7f;
        for (int i = 0; i < 4; i++) e.partes[i] = (float) (c.parte[i] / Math.max(1, c.parteMax[i]));
        e.abolladuras = co.abolladuras;
        e.retroceso = (float) c.retroceso;
        e.sacudida = (float) c.sacudida;
        e.escudo = c.escudoCabeza > 0 || c.escudoBrazo > 0;
        e.turbo = c.turbo > 0;
        e.camuflaje = c.camuflaje > 0;
        e.aturdido = c.aturdido > 0;
        e.sobrecarga = c.sobrecarga > 0;
        e.fuera = c.fuera;
        e.brazoQueAtaca = pelea.turno() - c.ultimoAtaque < 24 ? c.brazoDelUltimoAtaque : -1;
        e.faseDelAtaque = Math.min(1, (pelea.turno() - c.ultimoAtaque) / 24f);
        e.volteo = c.volteado / 24f;
        e.alegria = alegria;
        e.cara = c.fuera ? 3 : pelea.turno() - c.ultimoGolpe < 20 ? 3 : pelea.turno() - c.ultimoAtaque < 20 ? 2 : 0;
        float px = sx(c.x, c.y), py = sy(c.y);
        float talla = escalaX * f(c.y) * 1.45f;
        if (c.flotando > 0) py -= Math.min(1f, Math.min(c.flotando / 8f, (50 - c.flotando) / 8f)) * 26 * talla;
        dibujo.dibujar(co.montaje.modelo, co.montaje, px, py, talla, e);
        metajavbots.combate.Aparatos.Uno lleva = pelea.aparatos.delCuerpo(c.indice);
        if (lleva != null) DibujoDeAparatos.montado(p, lleva, px, py, (float) (62 * talla * co.montaje.modelo.tamano / 30), e.lado, tiempo);
        // El humo y las chispas de lo dañado: más cuanto más daño lleva en toda la pelea.
        double dano = Math.min(1.2, Math.max(co.estropeado(), c.indice == 0 ? averiaDelJugador : 0));
        float alto = (float) (62 * talla * co.montaje.modelo.tamano / 30);
        // El humo, menos y por encima de la cabeza: que no tape al robot.
        if (dano > 0.15 && azar.nextFloat() < dano * 0.22f) efectos.humo(px + azar.nextFloat() * 20 - 10, py - alto * 1.1f, (float) dano * 0.8f);
        if (c.indice == 0 && averiaDelJugador > 0.3f && azar.nextFloat() < averiaDelJugador * 0.1f) {
            efectos.chispas(px + azar.nextFloat() * 30 - 15, py - alto * 0.6f, 3, 0xFFE38A);
        }
        for (int i = 0; i < 4; i++) {
            if (c.parte[i] <= 0 && azar.nextFloat() < 0.08f) efectos.chispas(px + azar.nextFloat() * 30 - 15, py - alto * (0.3f + 0.15f * i), 3, 0xFFE38A);
        }
        if (c.fuera && azar.nextFloat() < 0.5f) efectos.humo(px, py - alto * 0.6f, 1.2f);
    }

    private void proyectiles(Pincel p, Pelea pelea) {
        for (Proyectil pr : pelea.proyectiles) {
            float px = sx(pr.x, pr.y), py = sy(pr.y) - TIRO * escalaX;
            if (pr.misil) {
                float tx = sx(pr.x - pr.vx * 3, pr.y - pr.vy * 3), ty = sy(pr.y - pr.vy * 3) - TIRO * escalaX;
                p.linea(tx, ty, px, py, 6 * escalaX, 0xE63946, 1);
                p.circulo(px, py, 3.5f * escalaX, 0xFFFFFF, 1);
                p.brillo(tx, ty, 6 * escalaX, 0xFF9F1C, 0.7f);
                if (azar.nextFloat() < 0.7f) efectos.humo(tx, ty, 0.2f);
            } else {
                float tx = sx(pr.x - pr.vx * 1.5, pr.y - pr.vy * 1.5), ty = sy(pr.y - pr.vy * 1.5) - TIRO * escalaX;
                p.linea(tx, ty, px, py, 3.2f * escalaX, 0xFFD27A, 0.8f);
                p.circulo(px, py, 2.6f * escalaX, 0xFFF6D5, 1);
            }
        }
    }

    private void rayos(Pincel p, Pelea pelea) {
        for (Pelea.Rayo r : pelea.rayos) {
            float a = r.vida / 8f;
            float x1 = sx(r.x1, r.y1), y1 = sy(r.y1) - TIRO * escalaX, x2 = sx(r.x2, r.y2), y2 = sy(r.y2) - TIRO * escalaX;
            int color = COLORES[pelea.equipo[r.dueno] % COLORES.length];
            p.linea(x1, y1, x2, y2, 12 * escalaX * a, color, 0.25f * a);
            p.linea(x1, y1, x2, y2, 5 * escalaX * a, color, 0.7f * a);
            p.linea(x1, y1, x2, y2, 2 * escalaX, 0xFFFFFF, a);
            if (r.acerto) p.brillo(x2, y2, 10 * escalaX, color, a);
        }
    }

    private void llamas(Pincel p, Pelea pelea) {
        for (int i = 0; i < pelea.combatientes.length; i++) {
            Cuerpo c = pelea.cuerpo(i);
            if (c == null || c.llama <= 0 && c.llamaAparato <= 0) continue;
            double ang = Math.toRadians(c.llama > 0 ? c.mira : c.miraDeLaLlama);
            for (int k = 0; k < 4; k++) {
                double d = 30 + azar.nextDouble() * 150;
                double ex = c.x + Math.cos(ang + (azar.nextDouble() - 0.5) * 0.4) * d, ey = c.y + Math.sin(ang + (azar.nextDouble() - 0.5) * 0.4) * d;
                efectos.anadir(Efectos.Tipo.FUEGO, sx(ex, ey), sy(ey) - TIRO * escalaX, 0, -20, 0.25f, (float) (5 + d * 0.06) * escalaX,
                        azar.nextBoolean() ? 0xFF6B1A : 0xFFC145);
            }
        }
    }

    /** Lo más alto (en pantalla) a lo que pueden subir los rótulos: debajo de la banda del locutor, si la hay. */
    public float techo;
    /** El tamaño de la letra de los bocadillos y los nombres (la del jugador, para que se lea bien). */
    public float letra = 18;
    private final java.util.List<float[]> ocupado = new java.util.ArrayList<>();

    /**
     * Los nombres (con más de dos robots) y los bocadillos, sin pisarse: cada uno busca un sitio libre encima de su
     * robot (y si no cabe, a un lado); un bocadillo que se ha tenido que apartar se une a su robot con una línea.
     */
    private void rotulos(Pincel p, java.util.List<Cuerpo> orden, Pelea pelea, float dt) {
        ocupado.clear();
        float arriba = Math.max(ay + 6, techo);
        // Los robots también ocupan: los nombres y los bocadillos no se ponen encima de ninguno.
        for (Cuerpo c : orden) {
            float[] cab = cabezaDe(c);
            float ancho = 60 * escalaX * f(c.y) * (float) (c.combatiente.montaje.modelo.tamano / 30);
            ocupado.add(new float[]{cab[0] - ancho / 2, cab[1], ancho, sy(c.y) - cab[1]});
        }
        // Primero se buscan los sitios; luego se dibujan las líneas y encima los rótulos (las líneas no tachan ningún texto).
        java.util.List<Runnable> lineas = new java.util.ArrayList<>(), cajas = new java.util.ArrayList<>();
        if (pelea.combatientes.length > 2) {
            for (Cuerpo c : orden) etiqueta(p, c, pelea, arriba, cajas);
        }
        for (Cuerpo c : orden) bocadillo(p, c, c.indice, dt, arriba, lineas, cajas);
        for (Runnable r : lineas) r.run();
        for (Runnable r : cajas) r.run();
    }

    private void etiqueta(Pincel p, Cuerpo c, Pelea pelea, float arriba, java.util.List<Runnable> cajas) {
        Combatiente co = c.combatiente;
        float t = Math.max(15, letra * 0.85f);
        float w = Math.max(110, p.ancho(co.nombre, t, true) + 20), h = t * 1.3f + 12;
        float[] cab = cabezaDe(c);
        float[] r = sitioLibre(cab[0] - w / 2, cab[1] - h, w, h, arriba);
        int color = COLORES[pelea.equipo[c.indice] % COLORES.length];
        float parte = (float) (c.blindaje() / 100);
        if (Math.abs(r[1] + h - cab[1]) > 6 || Math.abs(r[0] + w / 2 - cab[0]) > w / 2) {
            // Apartado para no pisar nada: una línea fina hasta su robot.
            cajas.add(0, () -> p.linea(r[0] + w / 2, r[1] + h, cab[0], cab[1], 1.5f, color, 0.6f));
        }
        cajas.add(() -> {
            p.caja(r[0], r[1], w, h, 8, 0x0B0F18, 0.92f);
            p.textoCentrado(co.nombre, r[0] + w / 2, r[1] + 3, t, true, c.fuera ? 0x888888 : color, 1);
            p.rect(r[0] + 8, r[1] + h - 7, w - 16, 4, 0x222222, 0.9f);
            p.rect(r[0] + 8, r[1] + h - 7, (w - 16) * parte, 4, parte > 0.5f ? 0x5BD685 : parte > 0.2f ? 0xFFC857 : 0xFF6B6B, 1);
        });
    }

    private void bocadillo(Pincel p, Cuerpo c, int i, float dt, float arriba, java.util.List<Runnable> lineas, java.util.List<Runnable> cajas) {
        if (tiempoBocadillo[i] <= 0 || bocadillo[i].isEmpty()) return;
        tiempoBocadillo[i] -= dt;
        float a = Math.min(1, tiempoBocadillo[i] * 2);
        String texto = bocadillo[i];
        float t = Math.max(16, letra * 0.9f);
        float w = Math.min(380, p.ancho(texto, t, false) + 28);
        float h = p.altoDeParrafo(texto, w - 24, t) + 16;
        float[] cab = cabezaDe(c);
        float px = cab[0], py = cab[1] - 10;
        float[] r = sitioLibre(px - w / 2, py - h - 10, w, h, arriba);
        boolean enSuSitio = Math.abs(r[1] + h + 10 - py) < 4 && px > r[0] + 10 && px < r[0] + w - 10;
        if (!enSuSitio) {
            // Apartado para no pisar a otro: una línea hasta su robot.
            float bx = Math.max(r[0] + 10, Math.min(r[0] + w - 10, px)), by = r[1] + h / 2 < py ? r[1] + h : r[1];
            lineas.add(() -> p.linea(bx, by, px, py, 2, 0xFFFFFF, 0.7f * a));
        }
        cajas.add(() -> {
            p.caja(r[0], r[1], w, h, 10, 0xFFFFFF, 0.95f * a);
            if (enSuSitio) p.triangulo(px - 8, r[1] + h - 1, px + 8, r[1] + h - 1, px, r[1] + h + 10, 0xFFFFFF, 0.95f * a);
            p.parrafo(texto, r[0] + 12, r[1] + 8, w - 24, t, 0x14161A, a);
        });
    }

    /** Un sitio para un rótulo de w × h que no pise a los ya puestos: subiendo, y si no, a los lados. Lo apunta como ocupado. */
    private float[] sitioLibre(float x, float y, float w, float h, float arriba) {
        float x0 = Math.max(ax + 6, Math.min(ax + aw - w - 6, x));
        float[] r = {x0, Math.max(arriba, y)};
        for (int intento = 0; intento < 24 && pisa(r, w, h); intento++) {
            float[] o = quienPisa(r, w, h);
            float subir = o[1] - h - 6;
            if (subir >= arriba) {
                r[1] = subir;
            } else {
                // Arriba no cabe: al lado del que estorba (a la derecha o a la izquierda, donde haya sitio).
                float derecha = o[0] + o[2] + 6, izquierda = o[0] - w - 6;
                r[0] = derecha + w <= ax + aw - 6 ? derecha : Math.max(ax + 6, izquierda);
                r[1] = Math.max(arriba, y);
            }
        }
        ocupado.add(new float[]{r[0], r[1], w, h});
        return r;
    }

    private boolean pisa(float[] r, float w, float h) {
        return quienPisa(r, w, h) != null;
    }

    private float[] quienPisa(float[] r, float w, float h) {
        for (float[] o : ocupado) {
            if (r[0] < o[0] + o[2] + 4 && r[0] + w + 4 > o[0] && r[1] < o[1] + o[3] + 4 && r[1] + h + 4 > o[1]) return o;
        }
        return null;
    }

    /**
     * Cómo acaba un robot que pierde (una de cinco, «variante»): la explosión grande, una lluvia de tornillos y
     * engranajes que rebotan, confeti, la cabeza que sale rodando o mucho humo y un último «bip» triste. Devuelve el
     * sonido que toca.
     */
    public String explotar(Cuerpo c, int variante) {
        float[] cab = cabezaDe(c);
        float suelo = sy(c.y), e = escala(c.y);
        switch (variante % 5) {
            case 1:
                efectos.explosion(cab[0], cab[1] + 40, 1.0f);
                trastos.piezas(cab[0], cab[1] + 40, suelo, 18, e);
                return "estallido";
            case 2:
                efectos.confeti(cab[0], cab[1] + 30, 90);
                efectos.anadir(Efectos.Tipo.ONDA, cab[0], cab[1] + 40, 0, 0, 0.4f, 80 * e, 0xFFD23F);
                return "fuegos";
            case 3:
                efectos.explosion(cab[0], cab[1] + 60, 0.8f);
                trastos.cabeza(cab[0], cab[1] + 20, suelo, e, 0x8A929C);
                c.parte[metajavbots.piezas.Montaje.CABEZA] = 0;
                return "parte_rota";
            case 4:
                for (int k = 0; k < 6; k++) efectos.humo(cab[0] + (k - 3) * 6 * e, cab[1] + 40 - k * 10 * e, 1.6f);
                return "bip_triste";
            default:
                efectos.explosion(cab[0], cab[1] + 40, 2.2f);
                return "explosion_grande";
        }
    }

    /** Para quien dibuja encima (las barras de vida): dónde queda la cabeza de un robot en pantalla. */
    public float[] cabezaDe(Cuerpo c) {
        // Lo que mide el robot dibujado (con la perspectiva, como en sombraYRobot); los que vuelan, más arriba.
        float alto = (c.vuela() ? 140 : 110) * escalaX * f(c.y) * (float) (c.combatiente.montaje.modelo.tamano / 30);
        float px = sx(c.x, c.y), py = sy(c.y) - alto;
        return new float[]{px, py};
    }

    /** Un robot suelto con un estado dado (para enseñar un arma a medio golpe). */
    public void robotConEstado(Pincel p, Montaje m, float x, float y, float talla, DibujoRobot.Estado e) {
        dibujo.dibujar(m.modelo, m, x, y, talla, e);
    }

    /** Un robot suelto (para el taller y los menús), sin pelea. */
    public void robotSuelto(Pincel p, Montaje m, float x, float y, float talla, float t, float alegriaDelRobot, boolean mirarDerecha) {
        DibujoRobot.Estado e = new DibujoRobot.Estado();
        e.lado = mirarDerecha ? 1 : -1;
        e.tiempo = t;
        e.paso = 0;
        e.alzado = 0.05f * (float) Math.sin(t * 1.5);
        e.alegria = alegriaDelRobot;
        e.cara = alegriaDelRobot > 0 ? 1 : 0;
        dibujo.dibujar(m.modelo, m, x, y, talla, e);
    }

    /** Las piezas que más se ven, para que la pelea suene y se vea según el arma. */
    static boolean esCuerpo(Pieza p) {
        return p != null && p.arma() == Pieza.Arma.CUERPO;
    }
}
