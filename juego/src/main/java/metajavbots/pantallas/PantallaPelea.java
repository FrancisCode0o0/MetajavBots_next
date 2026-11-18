package metajavbots.pantallas;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Input.Keys;

import metajavbots.Juego;
import metajavbots.api.RobotBase;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.DetectorDeTemas;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.dialogos.Narrador;
import metajavbots.pantalla.Dialogo;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.progreso.Registro;
import metajavbots.rivales.Rival;
import metajavbots.textos.T;

/**
 * PANTALLA DE PELEA — La pelea en marcha: la arena, el marcador (el blindaje
 * de cada parte, la energía, la ronda y el tiempo), la velocidad, la pausa,
 * el depurador en vivo (las variables del robot, sus pasos por turno y su
 * consola) y el locutor. Entre ronda y ronda se para: el jugador lee el
 * informe, cambia su código (se compila) y sus piezas. Al final, el que
 * pierde explota; el que gana celebra, y el progreso se apunta.
 */
public final class PantallaPelea implements Pantalla {

    private final Encuentro encuentro;
    private Pelea pelea;
    private RobotEnReparacion reparando;
    private final PanelDelAparato panelDelAparato = new PanelDelAparato();
    private ArmaEspecial especial;
    private Combatiente yo;
    private final RobotDelJugador robot = new RobotDelJugador();
    private VistaDeArena vista;
    private Narrador narrador;
    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private float velocidad = 1, acumulado, t;
    private boolean pausa, depurador, entreRondas, terminada, compilando;
    private String locutor = "";
    private float tiempoLocutor, altoDeLaBanda = 64;
    private volatile Compilador.Resultado resultadoDeCompilar;
    private String avisoDeCodigo = "";
    private boolean sinJdk;
    private float tiempoFinal;
    private Progreso.Cambio cambio;
    private boolean explotado;
    private final Ui.Desplazable desplConsola = new Ui.Desplazable(), desplVariables = new Ui.Desplazable(), desplInforme = new Ui.Desplazable();
    private int pestanaEntreRondas;
    private boolean ganoElJugador, objetivoCumplido, premioElegido;
    private volatile String resumenDelLocutor;
    private Revisor.Informe revision;
    private final Dialogo dialogo = new Dialogo();

    public PantallaPelea(Encuentro e) {
        this.encuentro = e;
    }

    @Override
    public String musica() {
        // El último asalto, con fanfarria (si hay más de uno).
        boolean ultimo = pelea != null && pelea.rondas > 1 && pelea.ronda() >= pelea.rondas;
        return ultimo ? "final" : encuentro.musica;
    }

    @Override
    public void alEntrar(Juego j) {
        if (pelea != null) return;
        Jugador jug = j.jugador;
        Jugador.Robot r = jug.robot();
        vista = new VistaDeArena(j.pincel);
        vista.escenario = encuentro.escenario;
        // Un robot roto trae su propio código (el robot del jugador no se toca).
        String codigo = encuentro.codigo != null ? encuentro.codigo : r.codigo;
        editor.ponerTexto(codigo);
        editor.tamano = jug.tamanoDeLetra;
        editor.alCompilar = () -> compilar(j);
        editor.palabras = Palabras.api(jug.idioma);
        yo = new Combatiente(encuentro.codigo != null ? encuentro.titulo : r.nombre, encuentro.montaje != null ? encuentro.montaje : r.montaje(), robot);
        sinJdk = !Compilador.hayCompilador();
        if (sinJdk) usarPilotoAutomatico();
        else {
            Compilador.Resultado res = robot.compilar(codigo, j.jugador.idioma.locale());
            if (!res.bien) {
                marcarErrores(res, jug);
                usarPilotoAutomatico();
                avisoDeCodigo = T.t("pelea.codigo_no_compila");
            }
        }
        if (encuentro.demas.isEmpty()) {
            pelea = new Pelea(yo, encuentro.rival, new Random().nextLong());
        } else {
            int n = 2 + encuentro.demas.size();
            Combatiente[] cs = new Combatiente[n];
            int[] eq = new int[n];
            cs[0] = yo;
            cs[1] = encuentro.rival;
            eq[1] = 1;
            for (int i = 0; i < encuentro.demas.size(); i++) {
                cs[2 + i] = encuentro.demas.get(i);
                eq[2 + i] = encuentro.equiposDeLosDemas.get(i);
            }
            pelea = new Pelea(cs, eq, new Random().nextLong());
        }
        pelea.rondas = encuentro.rondas;
        pelea.arena = metajavbots.combate.Arena.de(encuentro.escenario.name(), pelea.ancho, pelea.alto);
        // Los aparatos que salen de ronda en ronda (no en los robots rotos ni en los tutoriales).
        pelea.aparatos.activos = jug.aparatosEnLaArena && encuentro.codigo == null && encuentro.objetivo == null;
        // El arma especial con cuenta atrás: en las peleas de uno contra uno de más de una ronda.
        if (jug.armasEspeciales && encuentro.demas.isEmpty() && encuentro.codigo == null && encuentro.objetivo == null && encuentro.rondas > 1
                && !sinJdk) especial = ArmaEspecial.para(jug);
        pelea.topeDePasos = encuentro.topeDePasos;
        narrador = new Narrador(pelea.combatientes, pelea.equipo);
        narrador.idioma = jug.idioma;
        narrador.jugador = jug.nombre;
        narrador.agresividad = jug.agresividad;
        narrador.humor = jug.humor;
        narrador.alHablarLocutor = f -> {
            locutor = f;
            tiempoLocutor = 4 + f.length() * 0.05f;
            j.voz.decir(f, jug.idioma);
        };
        narrador.alHablarRobot = (q, f) -> vista.decir(q, f);
        empezarRonda(j);
    }

    /** Sin JDK o con el código roto, el robot se mueve solo (como un rival fácil) para que se pueda pelear igual. */
    private void usarPilotoAutomatico() {
        yo.fabrica = new metajavbots.combate.Fabrica() {
            @Override
            public RobotBase crear() {
                return new Rival(new metajavbots.rivales.Ajuste(0.2, 0.3, null), metajavbots.rivales.Estilo.ACOSADOR);
            }
        };
    }

    private void empezarRonda(Juego j) {
        if (encuentro.jefe != null) {
            // El jefe solo recibe daño si el código del jugador usa su tema de verdad.
            boolean usa = robot.listo() && DetectorDeTemas.temas(robot.fuente()).contains(encuentro.jefe.tema);
            pelea.resistencia[1] = usa ? 1 : 0.1;
            narrador.locutor(usa ? "golpe_fuerte" : "bloqueo", true, null);
        }
        pelea.empezarRonda();
        entreRondas = false;
        pedirPullas(j);
        if (resumenDelLocutor != null) {
            // Lo que la IA del locutor dijo de la ronda anterior, al empezar esta.
            locutor = resumenDelLocutor;
            tiempoLocutor = 5;
            j.voz.decir(resumenDelLocutor, j.jugador.idioma);
            resumenDelLocutor = null;
        }
        j.sonidos.tocar("campana", 0.8f);
    }

    /**
     * Con cerebro de IA, entre rondas: la IA no decide cada movimiento, pero elige el estilo del rival para la ronda
     * siguiente (a la vista del informe) y el locutor resume la ronda con una frase propia. El nivel no cambia.
     */
    private void ajustarRivalConIA(Juego j) {
        // Los demás robots de la computadora (battle royale, equipos): cada uno con el cerebro que le toca.
        for (int i = 2; i < pelea.combatientes.length; i++) {
            if (!pelea.combatientes[i].computadora) continue;
            EstiloPorIA.pedir(EstiloPorIA.cerebroDe(j.jugador, encuentro.cerebros, i - 1), pelea.combatientes[i], EstiloPorIA.informe(pelea, i));
        }
        if (encuentro.cerebros != EstiloPorIA.EL_RAPIDO) {
            EstiloPorIA.pedir(EstiloPorIA.cerebroDe(j.jugador, encuentro.cerebros, 0), encuentro.rival, EstiloPorIA.informe(pelea, 1));
        }
        metajavbots.cerebro.Cerebro c = metajavbots.cerebro.CerebroDelJuego.rapido(j.jugador);
        if (c == null || !encuentro.rival.computadora || encuentro.jefe != null) return;
        Combatiente.Informe yoInf = pelea.informe(0), elInf = pelea.informe(1);
        String informe = String.format(java.util.Locale.ROOT,
                "Round %d. Player robot: took %.0f damage, dealt %.0f, accuracy %.0f%%, close-range damage taken by the computer %.0f, "
                        + "long-range damage taken by the computer %.0f.",
                pelea.ronda(), yoInf.danoRecibido, yoInf.danoHecho, yoInf.punteria() * 100,
                elInf.danoPorArmaRecibido.getOrDefault("cuerpo", 0.0) + elInf.danoPorArmaRecibido.getOrDefault("choque", 0.0),
                elInf.danoPorArmaRecibido.getOrDefault("laser", 0.0) + elInf.danoPorArmaRecibido.getOrDefault("bala", 0.0)
                        + elInf.danoPorArmaRecibido.getOrDefault("misil", 0.0));
        Thread h = new Thread(() -> {
            try {
                if (encuentro.cerebros == EstiloPorIA.EL_RAPIDO) {
                    String r = c.preguntar(metajavbots.cerebro.CerebroDelJuego.sistemaEstilo(), informe, 6).toUpperCase(java.util.Locale.ROOT);
                    for (metajavbots.rivales.Estilo e : metajavbots.rivales.Estilo.values()) {
                        if (r.contains(e.name())) {
                            encuentro.rival.memoria.put("estilo", e);
                            break;
                        }
                    }
                }
                String f = c.preguntar(metajavbots.cerebro.CerebroDelJuego.sistemaLocutor(j.jugador.idioma), informe, 25);
                if (metajavbots.cerebro.Moderacion.aceptable(f)) resumenDelLocutor = f.replace("\"", "").strip();
            } catch (Exception e) {
                // sin cerebro, la computadora se adapta sola (Rival.alAcabarRonda)
            }
        }, "ajustar-rival");
        h.setDaemon(true);
        h.start();
    }

    /** Con cerebro de IA y la computadora de rival: unas pullas nuevas para esta ronda (se moderan antes de decirlas). */
    private void pedirPullas(Juego j) {
        metajavbots.cerebro.Cerebro c = metajavbots.cerebro.CerebroDelJuego.rapido(j.jugador);
        if (c == null || !encuentro.rival.computadora || j.jugador.agresividad < 0.3) return;
        String sistema = metajavbots.cerebro.CerebroDelJuego.sistemaPullas(j.jugador.idioma, encuentro.rival.montaje.modelo.personalidad,
                j.jugador.agresividad);
        for (int i = 0; i < 3; i++) {
            final int n = i;
            Thread h = new Thread(() -> {
                try {
                    String f = c.preguntar(sistema, "Opponent robot: " + yo.nombre + ". Round " + pelea.ronda() + ". Line " + (n + 1) + ".", 20);
                    f = f.replace("\"", "").replace("«", "").replace("»", "").strip();
                    if (metajavbots.cerebro.Moderacion.aceptable(f)) narrador.pullasDeLaIA.add(f);
                } catch (Exception e) {
                    // sin cerebro, las de siempre
                }
            }, "pullas");
            h.setDaemon(true);
            h.start();
        }
    }

    private void compilar(Juego j) {
        if (compilando || sinJdk) return;
        compilando = true;
        String codigo = editor.doc.texto();
        if (encuentro.codigo == null) j.jugador.robot().codigo = codigo;
        j.sonidos.tocar("compilar", 0.5f);
        Thread h = new Thread(() -> {
            resultadoDeCompilar = robot.compilar(codigo, j.jugador.idioma.locale());
            compilando = false;
        }, "compilar");
        h.setDaemon(true);
        h.start();
    }

    private void marcarErrores(Compilador.Resultado res, Jugador jug) {
        editor.marcas.clear();
        for (Compilador.Fallo f : res.errores) {
            editor.marcas.add(new VentanaDeCodigo.Marca((int) f.linea, (int) f.columna,
                    Explicador.explicar(f, jug.etiqueta, jug.idioma), f.grave));
        }
    }

    // ================================================================ cada fotograma

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        dialogo.antes(ui);
        if (resultadoDeCompilar != null) {
            Compilador.Resultado res = resultadoDeCompilar;
            resultadoDeCompilar = null;
            marcarErrores(res, j.jugador);
            if (res.bien) {
                yo.fabrica = robot;
                avisoDeCodigo = T.t("pelea.compilado");
                j.sonidos.tocar("bien", 0.6f);
                narrador.robot(0, "elogio", true, null);
            } else {
                avisoDeCodigo = T.f("pelea.errores", res.graves().size());
                j.sonidos.tocar("mal", 0.6f);
            }
            j.guardar();
        }
        teclas(j);
        if (vista.parada > 0) {
            // Tras un golpe fuerte, la pelea se para un instante (como en los juegos de lucha).
            vista.parada -= dt;
        } else if (especial != null && especial.activa()) {
            especial.avanzar(j, pelea, vista, dt);
        } else if (!pausa && !entreRondas && !terminada) {
            acumulado += dt * Pelea.TURNOS_POR_SEGUNDO * velocidad;
            int n = 0;
            while (acumulado >= 1 && n < 40) {
                acumulado -= 1;
                n++;
                pelea.avanzar();
                vista.sucesos(pelea, j.sonidos);
                narrador.turno(pelea, 1f / Pelea.TURNOS_POR_SEGUNDO);
                if (pelea.estado() != Pelea.Estado.LUCHANDO) break;
                if (especial != null && especial.toca(pelea)) {
                    especial.empezar(j, pelea);
                    break;
                }
            }
            if (pelea.estado() == Pelea.Estado.ENTRE_RONDAS) {
                entreRondas = true;
                pestanaEntreRondas = 0;
                editor.ponerTexto(j.jugador.robot().codigo);
                revision = Revisor.revisar(editor.doc.texto(), pelea, j.jugador.idioma);
                ajustarRivalConIA(j);
            } else if (pelea.estado() == Pelea.Estado.TERMINADA) {
                acabar(j);
            }
        }
        j.voz.fotograma(j.sonidos);
        float ancho = depurador && !entreRondas ? p.ancho - 460 : p.ancho;
        if (especial != null && especial.activa()) ancho = ArmaEspecial.anchoDeLaArena(p.ancho);
        vista.sacudida = Math.max(vista.sacudida, j.sacudida);
        // Los bocadillos y los nombres, con la letra del jugador y por debajo de la banda del locutor.
        vista.letra = ui.letra;
        vista.techo = tiempoLocutor > 0 && !locutor.isEmpty() ? 100 + altoDeLaBanda + 8 : 0;
        vista.dibujar(p, pelea, 0, 88, ancho, p.alto - 88, pausa || entreRondas ? 0 : dt);
        if (especial != null) especial.dibujar(j, vista, pelea);
        if (encuentro.codigo != null) reparacion(j, dt);
        marcador(j);
        if (depurador && !entreRondas) depurador(j, ancho);
        bandaDelLocutor(j, dt);
        if (!terminada && !entreRondas) controles(j);
        if (entreRondas) entreRondas(j);
        if (terminada) pantallaFinal(j, dt);
        dialogo.dibujar(ui);
    }

    /** Salir de la pelea a medias (Esc o «Rendirse»): se para y se pregunta; salir cuenta como rendirse. */
    private void preguntarSiSalir(Juego j) {
        boolean estabaEnPausa = pausa;
        pausa = true;
        dialogo.confirmar(T.t("pelea.salir_titulo"), T.t("pelea.salir_texto"), T.t("pelea.rendirse"), T.t("pelea.seguir"), true, () -> {
            pausa = estabaEnPausa;
            entreRondas = false;
            pelea.rendirse(0);
            acabar(j);
        });
    }

    private void teclas(Juego j) {
        if (terminada) return;
        if (j.ui.atras()) preguntarSiSalir(j);
        if (entreRondas) return;
        if (j.entrada.tecla(Keys.SPACE)) pausa = !pausa;
        if (j.entrada.tecla(Keys.F9)) depurador = !depurador;
        if (j.entrada.tecla(Keys.NUM_1)) velocidad = 1;
        if (j.entrada.tecla(Keys.NUM_2)) velocidad = 2;
        if (j.entrada.tecla(Keys.NUM_4)) velocidad = 4;
        if (pausa && j.entrada.tecla(Keys.RIGHT)) pasoAPaso(j);
    }

    private void pasoAPaso(Juego j) {
        if (pelea.estado() != Pelea.Estado.LUCHANDO) return;
        pelea.avanzar();
        vista.sucesos(pelea, j.sonidos);
        narrador.turno(pelea, 1f / Pelea.TURNOS_POR_SEGUNDO);
    }

    // ================================================================ marcador

    /** En una pelea de varios: el jugador arriba a la izquierda y los demás en una fila, con el color de su equipo. */
    private void marcadorDeVarios(Juego j) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.rect(0, 0, p.ancho, 88, ui.tema.panel, 0.97f);
        p.rect(0, 87, p.ancho, 1, ui.tema.borde, 1);
        int n = pelea.combatientes.length;
        float w = Math.min(260, (p.ancho - 260) / n);
        for (int i = 0; i < n; i++) {
            Cuerpo c = pelea.cuerpo(i);
            Combatiente co = pelea.combatientes[i];
            float x = 16 + i * w;
            int color = VistaDeArena.COLORES[pelea.equipo[i] % VistaDeArena.COLORES.length];
            p.texto(ui.recortarTexto(co.nombre, w - 20, ui.letra * 0.8f, true), x, 8, ui.letra * 0.8f, true, color, 1);
            if (c != null) {
                float parte = (float) (c.blindaje() / 100);
                ui.barra(x, 34, w - 20, 14, parte, parte > 0.5f ? ui.tema.bien : parte > 0.2f ? ui.tema.aviso : ui.tema.mal, null);
            }
            p.texto(T.f("pelea.dano", Math.round(co.danoTotalRecibido)), x, 54, ui.letra * 0.65f, false, ui.tema.textoSuave, 1);
        }
        float cx = p.ancho - 130;
        p.textoCentrado(T.f("pelea.ronda", pelea.ronda(), pelea.rondas), cx, 8, ui.letra * 0.9f, true, ui.tema.texto, 1);
        int segundos = (int) Math.ceil((pelea.turnosPorRonda - pelea.turno()) / (double) Pelea.TURNOS_POR_SEGUNDO);
        p.textoCentrado(String.format("%d:%02d", Math.max(0, segundos) / 60, Math.max(0, segundos) % 60), cx, 36, ui.letra * 1.5f, true,
                segundos <= 10 ? ui.tema.mal : ui.tema.texto, 1);
    }

    private void marcador(Juego j) {
        if (pelea.combatientes.length > 2) {
            marcadorDeVarios(j);
            return;
        }
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.rect(0, 0, p.ancho, 88, ui.tema.panel, 0.97f);
        p.rect(0, 87, p.ancho, 1, ui.tema.borde, 1);
        for (int i = 0; i < 2; i++) {
            Cuerpo c = pelea.cuerpo(i);
            Combatiente co = pelea.combatientes[i];
            boolean derecha = i == 1;
            float x = derecha ? p.ancho - 620 : 20;
            int color = i == 0 ? 0x3FE0FF : 0xFF4FD8;
            p.texto(co.nombre + (co.computadora ? "  ·  " + T.t("pelea.computadora") : ""), x, 8, ui.letra, true, color, 1);
            if (c == null) continue;
            float bx = x, by = 34;
            p.textoDerecha(T.f("pelea.dano", Math.round(co.danoTotalRecibido)), x + 588, 10, ui.letra * 0.8f, false, ui.tema.texto, 1);
            String[] nombres = {T.t("parte.cabeza"), T.t("parte.izquierda"), T.t("parte.derecha"), T.t("parte.piernas")};
            for (int k = 0; k < 4; k++) {
                float parte = (float) (c.parte[k] / Math.max(1, c.parteMax[k]));
                int col = parte > 0.5f ? ui.tema.bien : parte > 0.2f ? ui.tema.aviso : ui.tema.mal;
                ui.barra(bx + k * 148, by, 140, 14, parte, col, null);
                p.texto(nombres[k], bx + k * 148 + 4, by + 17, ui.letra * 0.6f, false, ui.tema.textoSuave, 1);
                if (ui.e.dentro(bx + k * 148, by, 140, 30)) ui.avisar(nombres[k] + ": " + Math.round(c.parte[k]) + " / " + Math.round(c.parteMax[k]));
            }
            ui.barra(x, 72, 588, 7, (float) (c.energia / Math.max(1, c.cifras.energiaMax)), 0xFFD23F, null);
            if (ui.e.dentro(x, 68, 588, 14)) ui.avisar(T.t("pelea.energia") + ": " + Math.round(c.energia));
        }
        float cx = p.ancho / 2;
        p.textoCentrado(T.f("pelea.ronda", pelea.ronda(), pelea.rondas), cx, 8, ui.letra * 1.1f, true, ui.tema.texto, 1);
        int segundos = (int) Math.ceil((pelea.turnosPorRonda - pelea.turno()) / (double) Pelea.TURNOS_POR_SEGUNDO);
        p.textoCentrado(String.format("%d:%02d", Math.max(0, segundos) / 60, Math.max(0, segundos) % 60), cx, 38, ui.letra * 1.6f, true,
                segundos <= 10 ? ui.tema.mal : ui.tema.texto, 1);
        if (encuentro.objetivo != null) {
            p.textoCentrado(Objetivo.texto(encuentro.objetivo), cx, 70, ui.letra * 0.7f, false, ui.tema.aviso, 1);
        }
        if (encuentro.jefe != null) {
            boolean vulnerable = pelea.resistencia[1] >= 1;
            p.textoCentrado(vulnerable ? T.t("pelea.jefe_vulnerable") : T.t("pelea.jefe_inmune"), cx, 70, ui.letra * 0.7f, false,
                    vulnerable ? ui.tema.bien : ui.tema.mal, 1);
        }
    }

    private void controles(Juego j) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float y = p.alto - 64, x = 20;
        if (ui.boton(pausa ? "▶" : "❚❚", x, y, 56, 46, Ui.Estilo.NORMAL, true, T.t("pelea.pausa_ayuda"))) pausa = !pausa;
        x += 64;
        for (float v : new float[]{1, 2, 4}) {
            if (ui.boton("×" + (int) v, x, y, 56, 46, velocidad == v ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) velocidad = v;
            x += 62;
        }
        if (pausa && ui.boton(T.t("pelea.paso"), x, y, 120, 46, Ui.Estilo.PLANO, true, T.t("pelea.paso_ayuda"))) pasoAPaso(j);
        if (ui.boton(T.t("pelea.depurador"), p.ancho - (depurador ? 460 : 0) - 380, y, 200, 46, depurador ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO,
                true, T.t("pelea.depurador_ayuda"))) depurador = !depurador;
        if (ui.boton(T.t("pelea.rendirse"), p.ancho - (depurador ? 460 : 0) - 170, y, 150, 46, Ui.Estilo.PELIGRO)) preguntarSiSalir(j);
        if (!avisoDeCodigo.isEmpty()) p.texto(avisoDeCodigo, 20, y - 30, ui.letra * 0.8f, false, ui.tema.aviso, 1);
        if (panelDelAparato.dibujar(j, pelea.aparatos.siguiente(), p.ancho - (depurador ? 460 : 0) - 380 - 12, y)) pausa = true;
    }


    private void bandaDelLocutor(Juego j, float dt) {
        if (tiempoLocutor <= 0 || locutor.isEmpty()) return;
        tiempoLocutor -= dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float w = Math.min(1000, p.ancho - 200), x = (p.ancho - w) / 2, y = 100;
        float a = Math.min(1, tiempoLocutor * 2);
        // La frase entera, en una o dos líneas (nada cortado).
        float letra = ui.letra * 1.05f;
        float alto = Math.max(64, p.altoDeParrafo(locutor, w - 110, letra) + 26);
        altoDeLaBanda = alto;
        p.caja(x, y, w, alto, 32, 0x0B0F18, 0.88f * a);
        p.bordeRedondeado(x, y, w, alto, 32, 0xFFC857, a, 2);
        // La cabeza del locutor: un robot loco con antena y boca que se mueve.
        float hx = x + 36, hy = y + 32;
        p.caja(hx - 20, hy - 18, 40, 36, 8, 0x8A92A3, a);
        p.circulo(hx - 8, hy - 5, 6, 0xFFFFFF, a);
        p.circulo(hx + 9, hy - 6, 4, 0xFFFFFF, a);
        p.circulo(hx - 7 + (float) Math.sin(t * 13) * 2, hy - 5, 3, 0x111111, a);
        p.circulo(hx + 9, hy - 6 + (float) Math.cos(t * 11) * 1.5f, 2, 0x111111, a);
        float boca = 2 + Math.abs((float) Math.sin(t * 22)) * 6;
        p.rect(hx - 9, hy + 6, 18, boca, 0x111111, a);
        p.linea(hx, hy - 18, hx + 6, hy - 30, 2, 0x8A92A3, a);
        p.circulo(hx + 6, hy - 31, 4, (t % 0.4f) < 0.2f ? 0xFF4F4F : 0xFFC857, a);
        p.parrafo(locutor, x + 76, y + 13, w - 110, letra, 0xFFF1C1, a);
    }

    // ================================================================ depurador en vivo

    private void depurador(Juego j, float x) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float w = p.ancho - x, y = 88, h = p.alto - 88;
        p.rect(x, y, w, h, ui.tema.panel, 0.98f);
        p.rect(x, y, 1, h, ui.tema.borde, 1);
        float ty = y + 12;
        p.texto(T.t("pelea.depurador"), x + 16, ty, ui.letra * 1.1f, true, ui.tema.texto, 1);
        ty += 34;
        Cuerpo c = pelea.cuerpo(0);
        if (c != null) {
            long tope = pelea.topeDePasos > 0 ? pelea.topeDePasos : (long) c.cifras.pasos;
            float parte = Math.min(1, c.pasosDelTurno / (float) tope);
            ui.barra(x + 16, ty, w - 32, 18, parte, parte > 0.8f ? ui.tema.mal : ui.tema.acento, T.f("pelea.pasos", c.pasosDelTurno, tope));
            ty += 28;
            p.texto(String.format(java.util.Locale.ROOT, "x=%.0f  y=%.0f  rumbo=%.0f°  mira=%.0f°", c.x, c.y, c.rumbo(), c.mira()), x + 16, ty,
                    ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
            ty += 24;
            p.texto(String.format(java.util.Locale.ROOT, "%s=%.0f  %s=%.0f  %s=%d", T.t("pelea.blindaje"), c.blindaje(), T.t("pelea.energia"),
                    c.energia, T.t("pelea.turno"), pelea.turno()), x + 16, ty, ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
            ty += 30;
        }
        p.texto(T.t("pelea.variables"), x + 16, ty, ui.letra * 0.9f, true, ui.tema.texto, 1);
        ty += 26;
        List<String[]> vars = variables(yo.robot());
        float zona = (h - (ty - y)) * 0.5f;
        float d = ui.desplazable(desplVariables, x + 8, ty, w - 16, zona);
        float yy = ty - d;
        for (String[] v : vars) {
            p.texto(v[0], x + 16, yy, ui.letra * 0.8f, false, ui.tema.tipo, 1);
            p.texto(ui.recortarTexto(v[1], w - 200, ui.letra * 0.8f, false), x + 180, yy, ui.letra * 0.8f, false, ui.tema.texto, 1);
            yy += ui.letra * 1.15f;
        }
        if (vars.isEmpty()) p.texto(T.t("pelea.sin_variables"), x + 16, yy, ui.letra * 0.8f, false, ui.tema.textoApagado, 1);
        ui.finDesplazable(desplVariables, Math.max(1, vars.size()) * ui.letra * 1.15f);
        ty += zona + 10;
        p.texto(T.t("pelea.consola"), x + 16, ty, ui.letra * 0.9f, true, ui.tema.texto, 1);
        ty += 26;
        consola(j, x + 8, ty, w - 16, y + h - ty - 80);
    }

    private void consola(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.caja(x, y, w, h, 6, ui.tema.fondo, 1);
        float alto = ui.letra * 0.95f;
        desplConsola.desplazamiento = Math.max(0, yo.consola.size() * alto - h + 10);
        float d = ui.desplazable(desplConsola, x, y + 4, w, h - 8);
        float yy = y + 4 - d;
        for (String l : yo.consola) {
            int color = l.contains("⚠") ? ui.tema.mal : l.contains("⏱") ? ui.tema.aviso : ui.tema.texto;
            p.texto(l, x + 8, yy, ui.letra * 0.75f, false, color, 1);
            yy += alto;
        }
        ui.finDesplazable(desplConsola, yo.consola.size() * alto + 10);
    }

    /** Los campos del robot del jugador y lo que valen ahora (con reflexión: es el juego quien mira, no su código). */
    static List<String[]> variables(RobotBase r) {
        List<String[]> l = new ArrayList<>();
        if (r == null) return l;
        Class<?> c = r.getClass();
        while (c != null && !c.getName().startsWith("metajavbots.api.")) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(r);
                    String s = v == null ? "null" : v.getClass().isArray() ? java.util.Arrays.deepToString(new Object[]{v}) : String.valueOf(v);
                    if (v instanceof Double || v instanceof Float) s = String.format(java.util.Locale.ROOT, "%.2f", ((Number) v).doubleValue());
                    l.add(new String[]{f.getType().getSimpleName() + " " + f.getName(), s.length() > 200 ? s.substring(0, 200) + "…" : s});
                } catch (RuntimeException | IllegalAccessException e) {
                    l.add(new String[]{f.getName(), "?"});
                }
            }
            c = c.getSuperclass();
        }
        return l;
    }

    // ================================================================ entre rondas

    private void entreRondas(Juego j) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.rect(0, 88, p.ancho, p.alto - 88, 0x000000, 0.55f);
        float x = 30, y = 110, w = p.ancho - 60, h = p.alto - 140;
        p.caja(x, y, w, h, 14, ui.tema.panel, 0.98f);
        p.texto(T.f("pelea.entre_rondas", pelea.ronda(), pelea.ronda() + 1), x + 24, y + 16, ui.letra * 1.4f, true, ui.tema.texto, 1);
        String[] pest = {T.t("pelea.informe"), T.t("pelea.codigo"), T.t("pelea.piezas")};
        pestanaEntreRondas = ui.pestanas(pest, pestanaEntreRondas, x + 24, y + 60, 600, 44);
        float cy = y + 116, ch = h - 116 - 80;
        switch (pestanaEntreRondas) {
            case 1:
                editor.dibujar(ui, x + 24, cy, w - 48, ch);
                if (ui.boton(compilando ? T.t("taller.compilando") : T.t("taller.compilar") + "  (Ctrl+S)", x + 24, y + h - 66, 260, 50,
                        Ui.Estilo.NORMAL, !compilando && !sinJdk, sinJdk ? T.t("taller.sin_jdk") : null)) compilar(j);
                if (!avisoDeCodigo.isEmpty()) p.texto(avisoDeCodigo, x + 300, y + h - 52, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
                break;
            case 2:
                Taller.piezas(j, yo, x + 24, cy, w - 48, ch);
                break;
            default:
                informe(j, x + 24, cy, w - 48, ch);
                break;
        }
        if (ui.boton(T.f("pelea.siguiente_ronda", pelea.ronda() + 1), x + w - 340, y + h - 66, 316, 52, Ui.Estilo.PRINCIPAL)) {
            j.jugador.robot().ponerPiezas(yo.montaje);
            j.guardar();
            empezarRonda(j);
        }
    }

    private void informe(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float col = (w - 40) / 3;
        for (int i = 0; i < 2; i++) {
            Combatiente.Informe inf = pelea.informe(i);
            float cx = x + i * (col + 20);
            int color = i == 0 ? 0x3FE0FF : 0xFF4FD8;
            p.texto(pelea.combatientes[i].nombre, cx, y, ui.letra * 1.1f, true, color, 1);
            String[] lineas = {
                    T.f("informe.dano_recibido", Math.round(inf.danoRecibido)),
                    T.f("informe.dano_hecho", Math.round(inf.danoHecho)),
                    T.f("informe.punteria", inf.aciertos, inf.ataques, Math.round(inf.punteria() * 100)),
                    T.f("informe.visto", Math.round(100.0 * inf.turnosVistos / Math.max(1, inf.turnos))),
                    T.f("informe.errores", inf.errores),
                    T.f("informe.topes", inf.topes, Math.round(inf.pasosMedios())),
                    T.f("informe.total", Math.round(pelea.combatientes[i].danoTotalRecibido))};
            float yy = y + 40;
            for (String l : lineas) {
                p.texto(l, cx, yy, ui.letra * 0.9f, false, ui.tema.texto, 1);
                yy += ui.letra * 1.5f;
            }
            yy += 10;
            for (java.util.Map.Entry<String, int[]> e : inf.porArma.entrySet()) {
                p.texto("· " + e.getKey() + ": " + e.getValue()[1] + "/" + e.getValue()[0], cx + 10, yy, ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
                yy += ui.letra * 1.3f;
            }
        }
        // Consejos del revisor (sin IA, o con ella si está puesta).
        float rx = x + 2 * (col + 20);
        p.texto(T.t("informe.consejos"), rx, y, ui.letra * 1.1f, true, ui.tema.texto, 1);
        float d = ui.desplazable(desplInforme, rx, y + 40, col, h - 40);
        float yy = y + 40 - d;
        if (revision != null) {
            for (String c : revision.consejos) {
                yy += p.parrafo("• " + c, rx, yy, col - 20, ui.letra * 0.85f, ui.tema.texto, 1) + 10;
            }
        }
        ui.finDesplazable(desplInforme, yy - (y + 40 - d));
    }

    // ================================================================ el final

    private void acabar(Juego j) {
        if (terminada) return;
        terminada = true;
        tiempoFinal = 0;
        int g = pelea.ganador();
        ganoElJugador = g >= 0 && pelea.equipo[g] == pelea.equipo[0];
        Jugador jug = j.jugador;
        jug.robot().ponerPiezas(yo.montaje);
        Registro.Resultado res = g == 0 ? Registro.Resultado.VICTORIA : g == 1 ? Registro.Resultado.DERROTA : Registro.Resultado.EMPATE;
        if (encuentro.cuenta) {
            Registro r = new Registro(System.currentTimeMillis(), encuentro.modo, encuentro.rival.nombre, encuentro.rival.montaje.modelo.id,
                    yo.montaje.modelo.id, encuentro.nivelRival, res, yo.danoTotalRecibido, yo.danoTotalHecho, pelea.ronda(), jug.etiqueta.name());
            cambio = Progreso.despuesDeLaPelea(jug, r, encuentro.rival.montaje.modelo.naturaleza.name(), new Random());
        }
        objetivoCumplido = encuentro.objetivo == null ? ganoElJugador
                : Objetivo.cumple(encuentro.objetivo, yo, robot.listo() ? robot.fuente() : editor.doc.texto());
        // Un robot roto solo cuenta arreglado si al final compila y se ha cambiado su código.
        if (encuentro.codigo != null) objetivoCumplido &= robot.listo() && !robot.fuente().equals(encuentro.codigo);
        if (encuentro.alAcabar != null) encuentro.alAcabar.accept(objetivoCumplido);
        j.guardar();
        narrador.locutor("explosion_final", true, null);
    }

    private void pantallaFinal(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        tiempoFinal += dt;
        int g = pelea.ganador();
        if (!explotado && tiempoFinal > 0.6f && g >= 0) explotar(j, g);
        if (cambio != null && cambio.subio) {
            // ¡Fuegos artificiales! Y los robots saltan de alegría, y la pantalla vibra.
            vista.alegria = 1;
            if (tiempoFinal > 1 && (int) (tiempoFinal * 3) != (int) ((tiempoFinal - dt) * 3)) {
                vista.efectos.cohete((float) (p.ancho * (0.15 + Math.random() * 0.7)), p.alto, p.alto * 0.55f,
                        new int[]{0xFF5E5B, 0xFFD23F, 0x3FE0D0, 0x7B61FF, 0x5BD685}[(int) (Math.random() * 5)]);
                j.sonidos.tocar("fuegos", 0.4f);
                if (Math.random() < 0.4) j.sacudir(5, 0.25f);
            }
        } else if (ganoElJugador) {
            vista.alegria = 0.5f;
        }
        if (tiempoFinal < 2.2f) return;
        float w = 720, h = 470, x = (p.ancho - w) / 2, y = (p.alto - h) / 2;
        p.caja(x, y, w, h, 16, ui.tema.panel, 0.97f);
        p.bordeRedondeado(x, y, w, h, 16, ganoElJugador ? ui.tema.bien : g == -1 ? ui.tema.aviso : ui.tema.mal, 1, 3);
        String titulo = g == -1 ? T.t("final.empate") : ganoElJugador ? T.t("final.victoria") : T.t("final.derrota");
        p.textoCentrado(titulo, x + w / 2, y + 24, ui.letra * 2.2f, true, ganoElJugador ? ui.tema.bien : ui.tema.texto, 1);
        p.textoCentrado(T.f("final.danos", Math.round(yo.danoTotalRecibido), Math.round(encuentro.rival.danoTotalRecibido)), x + w / 2, y + 90,
                ui.letra, false, ui.tema.textoSuave, 1);
        float yy = y + 140;
        if (encuentro.objetivo != null) {
            p.textoCentrado(Objetivo.texto(encuentro.objetivo), x + w / 2, yy, ui.letra, false, ui.tema.texto, 1);
            yy += 36;
            p.textoCentrado(objetivoCumplido ? T.t("objetivo.cumplido") : T.t("objetivo.no_cumplido"), x + w / 2, yy, ui.letra * 1.3f, true,
                    objetivoCumplido ? ui.tema.bien : ui.tema.mal, 1);
            yy += 44;
        }
        if (encuentro.alGanar != null && ganoElJugador) {
            yy += p.parrafo(encuentro.alGanar, x + 40, yy, w - 80, ui.letra * 0.95f, ui.tema.texto, 1) + 16;
        }
        if (cambio != null) {
            p.textoCentrado(T.f("final.experiencia", cambio.experiencia), x + w / 2, yy, ui.letra, false, ui.tema.texto, 1);
            yy += 36;
            if (cambio.subio) {
                p.textoCentrado(T.f("final.subes", cambio.despues.nombre), x + w / 2, yy, ui.letra * 1.4f, true, 0xFFC857, 1);
                yy += 44;
            } else if (cambio.bajo) {
                p.textoCentrado(T.f("final.bajas", cambio.despues.nombre), x + w / 2, yy, ui.letra * 1.1f, true, ui.tema.mal, 1);
                yy += 40;
            }
            for (Pieza b : cambio.botin) {
                p.textoCentrado(T.f("final.botin", b.nombre(T.idioma)), x + w / 2, yy, ui.letra, false, ui.tema.bien, 1);
                yy += 32;
            }
            for (metajavbots.piezas.Modelo m : cambio.modelosNuevos) {
                p.textoCentrado(T.f("final.modelo_nuevo", m.nombre(T.idioma)), x + w / 2, yy, ui.letra, false, ui.tema.bien, 1);
                yy += 32;
            }
            if (cambio.robotsNuevos > 0) {
                p.textoCentrado(T.t("final.robot_nuevo"), x + w / 2, yy, ui.letra, true, ui.tema.bien, 1);
                yy += 32;
            }
        }
        if (encuentro.modo.equals("DESAFIO") && ganoElJugador && !premioElegido) {
            if (ui.boton(T.t("final.premio"), x + w / 2 - 170, y + h - 140, 340, 50,
                    Ui.Estilo.PRINCIPAL)) j.ir(new PremioDelDesafio(() -> premioElegido = true));
        }
        if (ui.boton(T.t("final.revancha"), x + 30, y + h - 74, 200, 54, Ui.Estilo.NORMAL)) {
            j.volver();
            j.ir(PrepararPelea.repetir(j, encuentro));
        }
        if (ui.boton(T.t("final.revisar"), x + w / 2 - 110, y + h - 74, 220, 54, Ui.Estilo.NORMAL, true, T.t("final.revisar_ayuda"))) {
            j.ir(new Revision(editor.doc.texto(), pelea, yo));
        }
        if (encuentro.modo.equals("CAMPANA")) {
            // Vuelve a la campaña (que sigue debajo): el siguiente capítulo ya está abierto.
            if (ui.boton(T.t("final.seguir_campana"), x + w - 290, y + h - 74, 260, 54, Ui.Estilo.PRINCIPAL) || ui.atras()) j.volver();
        } else if (ui.boton(T.t("final.menu"), x + w - 230, y + h - 74, 200, 54, Ui.Estilo.PRINCIPAL) || ui.atras()) j.irSolo(new Menu());
    }

    /**
     * Un robot roto: arriba a la izquierda se ve cómo se va reparando según lo que el jugador hace bien (compila,
     * ha cambiado el código y va cumpliendo el objetivo).
     */
    private void reparacion(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean compila = robot.listo(), cambiado = compila && !robot.fuente().equals(encuentro.codigo);
        Combatiente.Informe inf = yo.informes.isEmpty() ? new Combatiente.Informe() : yo.informes.get(yo.informes.size() - 1);
        int quiere = Objetivo.aciertosQuePide(encuentro.objetivo);
        float parte = quiere <= 0 ? (Objetivo.cumple(encuentro.objetivo, yo, robot.fuente()) ? 1 : 0) : Math.min(1, inf.aciertos / (float) quiere);
        float arreglo = 0.1f + (compila ? 0.2f : 0) + (cambiado ? 0.2f : 0) + (cambiado ? 0.5f * parte : 0);
        vista.averiaDelJugador = 1 - arreglo;
        float tl = ui.letra * 0.72f, w = 250, h = 240 + 3 * tl * 1.4f, x = 16, y = 100;
        p.caja(x, y, w, h, 12, ui.tema.panel, 0.88f);
        p.texto(T.f("roto.reparado", Math.round(arreglo * 100)), x + 12, y + 8, ui.letra * 0.85f, true, arreglo >= 1 ? ui.tema.bien : 0xFFC857, 1);
        if (reparando == null) {
            reparando = new RobotEnReparacion();
            reparando.alRecuperar = () -> j.sonidos.tocar("compilar", 0.7f);
        }
        reparando.dibujar(p, vista, yo.montaje, x + w / 2, y + 222, 2.1f, arreglo, dt);
        float ly = y + 234;
        String[][] pasos = {{compila ? "✔" : "□", T.t("roto.compila")}, {cambiado ? "✔" : "□", T.t("roto.cambiado")},
            {parte >= 1 ? "✔" : "□", quiere > 0 ? T.f("roto.aciertos", inf.aciertos, quiere) : Objetivo.texto(encuentro.objetivo)}};
        for (String[] paso : pasos) {
            boolean hecho = paso[0].equals("✔");
            p.texto(paso[0] + " " + ui.recortarTexto(paso[1], w - 40, tl, false), x + 12, ly, tl, false, hecho ? ui.tema.bien : ui.tema.texto, 1);
            ly += tl * 1.4f;
        }
    }

    /** A los 0,6 s del final: explotan todos los que no son del equipo ganador, y el locutor y los robots lo dicen. */
    private void explotar(Juego j, int g) {
        explotado = true;
        // Acaban todos los que no son del equipo ganador, cada uno a su manera (VistaDeArena.explotar).
        int variante = new Random().nextInt(5);
        for (int i = 0; i < pelea.combatientes.length; i++) {
            Cuerpo perdedor = pelea.cuerpo(i);
            if (perdedor == null || pelea.equipo[i] == pelea.equipo[g]) continue;
            j.sonidos.tocar(vista.explotar(perdedor, variante++), 1f);
            perdedor.fuera = true;
        }
        j.sacudir(14, 0.9f);
        narrador.locutor(ganoElJugador ? "victoria_jugador" : "derrota_jugador", true, null);
        narrador.robot(g, "victoria", true, null);
        for (int i = 0; i < pelea.combatientes.length; i++) if (pelea.equipo[i] != pelea.equipo[g]) {
            narrador.robot(i, "derrota", true, null);
            break;
        }
        if (cambio != null && cambio.subio) {
            j.sonidos.tocar("subida", 1f);
            narrador.locutor("subida_nivel", true, null);
        } else if (cambio != null && cambio.bajo) {
            j.sonidos.tocar("bajada", 0.8f);
            narrador.locutor("bajada_nivel", true, null);
        }
    }
}
