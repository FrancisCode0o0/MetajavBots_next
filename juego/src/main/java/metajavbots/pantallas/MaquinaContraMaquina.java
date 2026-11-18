package metajavbots.pantallas;

import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Input.Keys;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.dialogos.Narrador;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.rivales.Rivales;
import metajavbots.rivales.RivalConTraza;
import metajavbots.textos.T;

/**
 * MÁQUINA CONTRA MÁQUINA — Dos robots de la computadora pelean y el jugador
 * mira, con el código de cada uno a la vista: las líneas que se acaban de
 * ejecutar se iluminan (la última, con ▶). Se puede pausar, avanzar turno a
 * turno y ver exactamente qué línea decidió cada movimiento. No cuenta para
 * el progreso: es para aprender del código de la máquina.
 */
public final class MaquinaContraMaquina implements Pantalla {

    private final int[] modelo = new int[2];
    private final float[] nivel = {0.4f, 0.7f};
    private Pelea pelea;
    private VistaDeArena vista;
    private Narrador narrador;
    private final VentanaDeCodigo[] codigo = {new VentanaDeCodigo(), new VentanaDeCodigo()};
    private boolean pausa;
    private float velocidad = 1, acumulado, esperaEntreRondas;
    private String locutor = "";
    private float altoDeLaBanda = 44;
    private float tiempoLocutor;
    private boolean seguir = true;

    @Override
    public void alEntrar(Juego j) {
        if (vista != null) return;
        vista = new VistaDeArena(j.pincel);
        Random r = new Random();
        modelo[0] = r.nextInt(Modelo.todos().size());
        modelo[1] = (modelo[0] + 1 + r.nextInt(Modelo.todos().size() - 1)) % Modelo.todos().size();
        for (VentanaDeCodigo v : codigo) {
            v.ponerTexto(RivalConTraza.fuente());
            v.soloLectura = true;
            v.tamano = Math.max(12, j.jugador.tamanoDeLetra * 0.8f);
            v.seguirActiva = true;
        }
    }

    /** Para las capturas: empieza sin pulsar el botón. */
    public void empezarParaCapturas(Juego j) {
        empezar(j);
    }

    private void empezar(Juego j) {
        List<Modelo> ms = Modelo.todos();
        Random r = new Random();
        Combatiente a = Rivales.crear(ms.get(modelo[0]), nivel[0], j.jugador.agresividad, r, true);
        Combatiente b = Rivales.crear(ms.get(modelo[1]), nivel[1], j.jugador.agresividad, r, true);
        pelea = new Pelea(a, b, r.nextLong());
        vista.escenario = PrepararPelea.escenarioAlAzar();
        narrador = new Narrador(a, b);
        narrador.idioma = j.jugador.idioma;
        narrador.jugador = j.jugador.nombre;
        narrador.alHablarLocutor = f -> {
            locutor = f;
            tiempoLocutor = 4;
            j.voz.decir(f, j.jugador.idioma);
        };
        narrador.alHablarRobot = (q, f) -> vista.decir(q, f);
        pelea.empezarRonda();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("mcm.titulo"), true);
        if (pelea == null) {
            configurar(j, arriba);
            return;
        }
        if (j.entrada.tecla(Keys.SPACE)) pausa = !pausa;
        boolean paso = pausa && j.entrada.tecla(Keys.RIGHT);
        if ((!pausa || paso) && pelea.estado() != Pelea.Estado.TERMINADA) {
            if (pelea.estado() == Pelea.Estado.ENTRE_RONDAS) {
                esperaEntreRondas += dt;
                if (esperaEntreRondas > 2.5f) {
                    esperaEntreRondas = 0;
                    pelea.empezarRonda();
                }
            } else {
                acumulado += paso ? 1 : dt * Pelea.TURNOS_POR_SEGUNDO * velocidad;
                int n = 0;
                while (acumulado >= 1 && n++ < 20 && pelea.estado() == Pelea.Estado.LUCHANDO) {
                    acumulado -= 1;
                    pelea.avanzar();
                    vista.sucesos(pelea, j.sonidos);
                    narrador.turno(pelea, 1f / Pelea.TURNOS_POR_SEGUNDO);
                }
            }
        }
        j.voz.fotograma(j.sonidos);
        float ah = (p.alto - arriba) * 0.5f;
        vista.letra = ui.letra;
        vista.techo = tiempoLocutor > 0 ? arriba + 12 + altoDeLaBanda + 8 : 0;
        vista.dibujar(p, pelea, 0, arriba, p.ancho, ah, pausa ? 0 : dt);
        // Las cabeceras de cada código y el código con la línea que se ejecuta.
        float cy = arriba + ah + 6, ch = p.alto - cy - 70, cw = (p.ancho - 30) / 2;
        for (int i = 0; i < 2; i++) {
            Cuerpo c = pelea.cuerpo(i);
            float x = 10 + i * (cw + 10);
            int color = i == 0 ? 0x3FE0FF : 0xFF4FD8;
            Combatiente co = pelea.combatientes[i];
            String estado = c == null ? "" : String.format(java.util.Locale.ROOT, " · %s %.0f · %s %d", T.t("pelea.blindaje"), c.blindaje(),
                    T.t("mcm.linea"), c.ultimaLinea);
            p.texto(co.nombre + " (" + Math.round(nivel[i] * 100) + " %)" + estado, x, cy, ui.letra * 0.85f, true, color, 1);
            if (c != null) {
                codigo[i].lineasActivas = c.lineasDelTurno;
                codigo[i].lineaActiva = c.ultimaLinea;
            }
            codigo[i].seguirActiva = seguir;
            codigo[i].dibujar(ui, x, cy + 26, cw, ch - 26);
        }
        // Los mandos.
        float by = p.alto - 58;
        if (ui.boton(pausa ? "▶" : "❚❚", 10, by, 56, 46, Ui.Estilo.NORMAL, true, T.t("pelea.pausa_ayuda"))) pausa = !pausa;
        if (pausa && ui.boton(T.t("pelea.paso"), 74, by, 120, 46, Ui.Estilo.PLANO, true, T.t("pelea.paso_ayuda"))) {
            if (pelea.estado() == Pelea.Estado.LUCHANDO) {
                pelea.avanzar();
                vista.sucesos(pelea, j.sonidos);
            }
        }
        float bx = 210;
        for (float v : new float[]{0.25f, 1, 2, 4}) {
            if (ui.boton((v < 1 ? "×¼" : "×" + (int) v), bx, by, 62, 46, velocidad == v ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) velocidad = v;
            bx += 68;
        }
        seguir = ui.casilla(T.t("mcm.seguir"), seguir, bx + 20, by + 10, T.t("mcm.seguir_ayuda"));
        p.textoCentrado(T.f("pelea.ronda", pelea.ronda(), pelea.rondas), p.ancho / 2 + 200, by + 12, ui.letra, true, ui.tema.texto, 1);
        if (pelea.estado() == Pelea.Estado.TERMINADA) {
            int g = pelea.ganador();
            p.textoCentrado(g < 0 ? T.t("final.empate") : T.f("mcm.gana", pelea.combatientes[g].nombre), p.ancho / 2, arriba + 20, ui.letra * 1.6f, true,
                    0xFFC857, 1);
            if (ui.boton(T.t("mcm.otra"), p.ancho - 230, by, 210, 46, Ui.Estilo.PRINCIPAL)) pelea = null;
        } else if (ui.boton(T.t("mcm.otra"), p.ancho - 230, by, 210, 46, Ui.Estilo.PLANO)) {
            pelea = null;
        }
        if (tiempoLocutor > 0) {
            tiempoLocutor -= dt;
            // La frase entera, en las líneas que haga falta (los bocadillos se quedan debajo: vista.techo).
            float bw = Math.min(1000, p.ancho - 200), alto = p.altoDeParrafo(locutor, bw - 40, ui.letra, true) + 16;
            p.caja(p.ancho / 2 - bw / 2, arriba + 12, bw, alto, 22, 0x0B0F18, 0.88f);
            float yy = arriba + 20;
            for (String l : p.partir(locutor, bw - 40, ui.letra, true)) {
                p.textoCentrado(l, p.ancho / 2, yy, ui.letra, true, 0xFFF1C1, 1);
                yy += p.altoDeLinea(ui.letra);
            }
            altoDeLaBanda = alto;
        }
    }

    private void configurar(Juego j, float arriba) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.parrafo(T.t("mcm.ayuda"), 60, arriba + 20, p.ancho - 120, ui.letra, ui.tema.textoSuave, 1);
        List<Modelo> ms = Modelo.todos();
        for (int i = 0; i < 2; i++) {
            float x = 120 + i * (p.ancho / 2), y = arriba + 120, w = p.ancho / 2 - 240;
            Modelo m = ms.get(modelo[i]);
            vista.robotSuelto(p, new metajavbots.piezas.Montaje(m), x + w / 2, y + 300, 3, (float) (System.nanoTime() / 1e9), 0, i == 0);
            if (ui.boton("◄", x, y + 320, 56, 50, Ui.Estilo.PLANO)) modelo[i] = (modelo[i] - 1 + ms.size()) % ms.size();
            if (ui.boton("►", x + w - 56, y + 320, 56, 50, Ui.Estilo.PLANO)) modelo[i] = (modelo[i] + 1) % ms.size();
            p.textoCentrado(m.nombre(T.idioma), x + w / 2, y + 330, ui.letra * 1.2f, true, ui.tema.texto, 1);
            nivel[i] = ui.deslizador(T.t("mcm.nivel"), nivel[i], x, y + 400, w, 50, null);
        }
        if (!Compilador.hayCompilador()) p.textoCentrado(T.t("mcm.sin_jdk"), p.ancho / 2, p.alto - 150, ui.letra * 0.85f, false, ui.tema.aviso, 1);
        if (ui.boton(T.t("mcm.empezar"), p.ancho / 2 - 160, p.alto - 100, 320, 60, Ui.Estilo.PRINCIPAL)) empezar(j);
    }
}
