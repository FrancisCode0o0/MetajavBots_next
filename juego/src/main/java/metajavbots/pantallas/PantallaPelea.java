package metajavbots.pantallas;

import java.util.Random;

import com.badlogic.gdx.Input.Keys;

import metajavbots.Juego;
import metajavbots.api.RobotBase;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.DetectorDeTemas;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.ModificadoresPoo;
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
import metajavbots.textos.Idioma;
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
    private EntradaEpica entrada;
    private final Repeticion repeticion = new Repeticion();
    private Combatiente yo;
    private final RobotDelJugador robot = new RobotDelJugador();
    private VistaDeArena vista;
    private Narrador narrador;
    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private float velocidad = 1, acumulado, t;
    private boolean pausa, depurador, entreRondas, terminada, compilando;
    private String locutor = "";
    private float tiempoLocutor;
    private volatile Compilador.Resultado resultadoDeCompilar;
    private String avisoDeCodigo = "";
    private boolean sinJdk;
    private float tiempoFinal;
    private Progreso.Cambio cambio;
    private boolean explotado;
    private final Ui.Desplazable desplInforme = new Ui.Desplazable();
    private final PanelDepurador panelDepurador = new PanelDepurador();
    private int pestanaEntreRondas;
    private boolean ganoElJugador, objetivoCumplido, premioElegido;
    private volatile String resumenDelLocutor;
    private Revisor.Informe revision;
    private final Dialogo dialogo = new Dialogo();
    /** El entrenador: consejos contra el enemigo marcado; contra un jefe, en el hospital (y al ganar, el milagro). */
    private final EntrenadorEnLaPelea entrenador = new EntrenadorEnLaPelea();

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
        vista.conPublico = true;
        // Un robot roto trae su propio código (el robot del jugador no se toca).
        String codigo = encuentro.codigo != null ? encuentro.codigo : r.codigo;
        editor.ponerTexto(codigo);
        editor.tamano = jug.tamanoDeLetra;
        editor.alCompilar = () -> compilar(j);
        editor.palabras = Palabras.api(jug.idioma);
        editor.documentacion = w -> Referencia.de(w, jug.idioma);
        yo = new Combatiente(encuentro.codigo != null ? encuentro.titulo : r.nombre,
                encuentro.montaje != null ? encuentro.montaje : j.jugador.montajeDe(r), robot);
        sinJdk = !Compilador.hayCompilador();
        if (sinJdk) usarPilotoAutomatico();
        else {
            Compilador.Resultado res = robot.compilar(codigo, j.jugador.idioma.locale(), true);
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
        if (encuentro.codigo == null && encuentro.objetivo == null) entrada = new EntradaEpica(pelea);
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

    /** Marca ese enemigo para el entrenador, como si el jugador le hiciera clic (para las capturas). */
    public void marcarEnemigo(int i) {
        entrenador.marcarDesdeFuera(i);
    }

    /** Abre el depurador en la pestaña indicada (0: variables, 1: código/turno; para las capturas). */
    public void abrirDepurador(int pestana) {
        depurador = true;
        panelDepurador.pestana(pestana);
    }

    /** Pausa o reanuda la pelea (para las capturas). */
    public void pausar(boolean si) {
        pausa = si;
    }

    /** Si el robot del jugador tiene ahora mismo una orden pisada en el último turno. */
    public boolean tieneConflictoDeOrdenes() {
        Cuerpo c = pelea == null ? null : pelea.cuerpo(0);
        return c != null && c.tieneConflictoDeOrdenes;
    }

    @Override
    public boolean entrenadorEnElHospital() {
        return encuentro.jefe != null && !(terminada && ganoElJugador);
    }

    @Override
    public String contextoParaElEntrenador() {
        return pelea == null ? null : entrenador.contexto(pelea, robot.listo() ? robot.fuente() : editor.doc.texto());
    }

    private void empezarRonda(Juego j) {
        entrenador.nuevaRonda();
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
            resultadoDeCompilar = robot.compilar(codigo, j.jugador.idioma.locale(), true);
            compilando = false;
        }, "compilar");
        h.setDaemon(true);
        h.start();
    }

    private void marcarErrores(Compilador.Resultado res, Jugador jug) {
        editor.marcas.clear();
        for (Compilador.Fallo f : res.errores) {
            editor.marcas.add(Explicador.marca(f, jug));
        }
    }

    // ================================================================ cada fotograma

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel; Ui ui = j.ui;
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
        } else if (entrada != null && entrada.activa()) {
            entrada.avanzar(j, pelea, vista, narrador, dt);
        } else if (especial != null && especial.activa()) {
            especial.avanzar(j, pelea, vista, dt);
        } else if (!pausa && !entreRondas && !terminada) {
            acumulado += dt * Pelea.TURNOS_POR_SEGUNDO * velocidad;
            int n = 0;
            while (acumulado >= 1 && n < 40) {
                acumulado -= 1;
                n++;
                pelea.avanzar();
                entrenador.observar(pelea);
                repeticion.grabar(pelea);
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
                revision = Revisor.revisar(editor.doc.texto(), pelea, j.jugador.idioma);
                ajustarRivalConIA(j);
            } else if (pelea.estado() == Pelea.Estado.TERMINADA) {
                acabar(j);
            }
        }
        float ancho = depurador && !entreRondas ? p.ancho - 460 : p.ancho;
        if (especial != null && especial.activa()) ancho = ArmaEspecial.anchoDeLaArena(p.ancho);
        vista.sacudida = Math.max(vista.sacudida, j.sacudida);
        boolean conBanda = tiempoLocutor > 0 && !locutor.isEmpty() && !entreRondas && !panelDelAparato.estaAbierto()
                && !(terminada && tiempoFinal >= 2.2f);
        float letraBanda = Math.min(26f, ui.letra * 1.05f);
        Marcadores.GeometriaBanda banda = calcularGeometriaBanda(p, ancho, conBanda, encuentro.codigo != null, locutor, letraBanda);
        vista.letra = ui.letra;
        vista.techo = banda.visible() ? banda.techo() : 0;
        vista.visor(j.visorTactico && (entrada == null || !entrada.activa()) && (especial == null || !especial.activa()), entrenador.marcado());
        vista.dibujar(p, pelea, 0, 88, ancho, p.alto - 88, pausa || entreRondas ? 0 : dt);
        if (entrada != null && entrada.activa()) entrada.dibujar(j, pelea, vista, 0, 88, ancho, p.alto - 88);
        if (especial != null) especial.dibujar(j, vista, pelea);
        if (encuentro.codigo != null) reparacion(j, dt);
        marcador(j);
        if (depurador && !entreRondas) panelDepurador.dibujar(j, pelea, yo, robot.listo() ? robot.fuente() : "", robot.listo() && robot.conTraza(), ancho);
        bandaDelLocutor(j, banda, letraBanda, dt);
        // El entrenador aconseja contra el enemigo marcado (clic en él); contra un jefe no: está en el hospital.
        if (!terminada && !entreRondas && encuentro.jefe == null && j.jugador.entrenadorActivo && (especial == null || !especial.activa())
                && (entrada == null || !entrada.activa())) {
            entrenador.dibujar(j, pelea, vista, ancho, dt, !pausa);
            if (!panelDelAparato.estaAbierto()) entrenador.clic(j, pelea, vista);
        }
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
        }, () -> pausa = estabaEnPausa);
    }

    private void teclas(Juego j) {
        if (terminada || (entrada != null && entrada.activa()) || panelDelAparato.estaAbierto()) return;
        if (j.ui.atras()) preguntarSiSalir(j);
        if (entreRondas || (especial != null && especial.activa())) return;
        if (j.entrada.tecla(Keys.SPACE)) pausa = !pausa;
        if (j.entrada.tecla(Keys.F8)) j.visorTactico = !j.visorTactico;
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

    /** La etiqueta del marcador: nombre, modelo explícito y, si es de la computadora, su marca. */
    public static String etiquetaMarcador(Combatiente co, Idioma idioma) {
        Idioma id = idioma == null ? Idioma.ES : idioma;
        String modelo = co.montaje != null && co.montaje.modelo != null
                ? T.en("pelea.modelo", id).replace("{0}", co.montaje.modelo.nombre(id)) : "";
        StringBuilder texto = new StringBuilder(co.nombre);
        if (!modelo.isEmpty()) texto.append("  ·  ").append(modelo);
        if (co.computadora) texto.append("  ·  ").append(T.en("pelea.computadora", id));
        return texto.toString();
    }

    /** En una pelea de varios: el jugador arriba a la izquierda y los demás en una fila, con el color de su equipo. */
    private void marcador(Juego j) {
        if (pelea.combatientes.length > 2) Marcadores.deVarios(j, pelea);
        else Marcadores.deDos(j, pelea, encuentro);
    }

    private static Marcadores.GeometriaBanda calcularGeometriaBanda(Pincel p, float anchoZona, boolean visible, boolean conReparacion,
            String texto, float letra) {
        if (!visible) return Marcadores.geometriaBanda(anchoZona, false, conReparacion, 0f);
        float minX = conReparacion ? 282f : 30f;
        float maxAncho = Math.max(220f, anchoZona - minX - 30f);
        float w = Math.min(1000f, maxAncho);
        float altoParrafo = p.altoDeParrafo(texto, Math.max(80f, w - 110f), letra);
        return Marcadores.geometriaBanda(anchoZona, true, conReparacion, altoParrafo);
    }

    private void controles(Juego j) {
        Pincel p = j.pincel; Ui ui = j.ui;
        float der = p.ancho - (depurador ? 460 : 0);
        float tBtn = Math.min(ui.letra, PanelDelAparato.ALTO_BOTON * 0.5f);
        String txtPaso = T.t("pelea.paso"), txtVisor = T.t("pelea.visor"), txtDep = T.t("pelea.depurador"), txtRend = T.t("pelea.rendirse");
        metajavbots.combate.Aparatos.Tipo sig = pelea.aparatos.siguiente();
        Marcadores.BarraInferior b = Marcadores.calcularBarraInferior(p.alto, der, pausa, p.ancho(txtPaso, tBtn, true) + 28f,
                Math.min(160f, p.ancho(txtVisor, tBtn, true) + 32f), Math.min(220f, p.ancho(txtDep, tBtn, true) + 32f),
                Math.min(190f, p.ancho(txtRend, tBtn, true) + 32f), sig != null, PanelDelAparato.anchoBotonDeseado(p, ui, sig));
        float x = 20f, y = b.y();
        if (ui.boton(pausa ? "▶" : "❚❚", x, y, 56, 46, Ui.Estilo.NORMAL, true, T.t("pelea.pausa_ayuda"))) pausa = !pausa;
        x += 64;
        for (float v : new float[]{1, 2, 4}) {
            if (ui.boton("×" + (int) v, x, y, 56, 46, velocidad == v ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) velocidad = v;
            x += 62;
        }
        if (pausa && ui.boton(txtPaso, x, y, b.wPaso(), 46, Ui.Estilo.PLANO, true, T.t("pelea.paso_ayuda"))) pasoAPaso(j);
        if (ui.boton(txtVisor, b.xVisor(), y, b.wVisor(), 46, j.visorTactico ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, true,
                T.t("pelea.visor_ayuda"))) j.visorTactico = !j.visorTactico;
        if (ui.boton(txtDep, b.xDepurador(), y, b.wDepurador(), 46, depurador ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, true,
                T.t("pelea.depurador_ayuda"))) depurador = !depurador;
        if (ui.boton(txtRend, b.xRendirse(), y, b.wRendirse(), 46, Ui.Estilo.PELIGRO)) preguntarSiSalir(j);
        if (!avisoDeCodigo.isEmpty()) dibujarAvisoDeCodigo(j, der, b);
        if (panelDelAparato.dibujar(j, sig, b.xAparatoIzq(), b.xAparatoDer(), b.yAparato())) pausa = true;
    }

    private void dibujarAvisoDeCodigo(Juego j, float der, Marcadores.BarraInferior b) {
        Pincel p = j.pincel; Ui ui = j.ui;
        boolean conEntrenador = encuentro.jefe == null && j.jugador.entrenadorActivo && (especial == null || !especial.activa())
                && (entrada == null || !entrada.activa());
        float reservaDer = conEntrenador ? Math.min(470f, der * 0.4f) + 28f : 20f;
        float anchoAviso = Math.max(220f, der - 20f - reservaDer);
        float tamAviso = Math.min(18f, ui.letra * 0.78f);
        float altoTexto = p.altoDeParrafo(avisoDeCodigo, anchoAviso - 16f, tamAviso, true);
        float yBase = b.aparatoArriba() ? b.yAparato() : b.y();
        float yAviso = yBase - altoTexto - 18f;
        p.caja(20f, yAviso, anchoAviso, altoTexto + 10f, 8f, 0x0B0F18, 0.82f);
        p.parrafo(avisoDeCodigo, 28f, yAviso + 5f, anchoAviso - 16f, tamAviso, true, ui.tema.aviso, 1f);
    }

    private void bandaDelLocutor(Juego j, Marcadores.GeometriaBanda b, float letra, float dt) {
        if (tiempoLocutor <= 0 || locutor.isEmpty()) return;
        tiempoLocutor -= dt;
        if (!b.visible()) return;
        Pincel p = j.pincel;
        float x = b.x(), y = b.y(), w = b.w(), alto = b.h();
        float a = Math.min(1, tiempoLocutor * 2);
        p.caja(x, y, w, alto, 32, 0x0B0F18, 0.88f * a);
        p.bordeRedondeado(x, y, w, alto, 32, 0xFFC857, a, 2);
        // La cabeza del locutor: un robot loco con antena y boca que se mueve.
        float hx = x + 36, hy = Math.max(y + 32f, y + Math.min(alto / 2f, 44f));
        p.caja(hx - 20, hy - 18, 40, 36, 8, 0x8A92A3, a);
        p.circulo(hx - 8, hy - 5, 6, 0xFFFFFF, a);
        p.circulo(hx + 9, hy - 6, 4, 0xFFFFFF, a);
        p.circulo(hx - 7 + (float) Math.sin(t * 13) * 2, hy - 5, 3, 0x111111, a);
        p.circulo(hx + 9, hy - 6 + (float) Math.cos(t * 11) * 1.5f, 2, 0x111111, a);
        float boca = 2 + Math.abs((float) Math.sin(t * 22)) * 6;
        p.rect(hx - 9, hy + 6, 18, boca, 0x111111, a);
        p.linea(hx, hy - 18, hx + 6, hy - 30, 2, 0x8A92A3, a);
        p.circulo(hx + 6, hy - 31, 4, (t % 0.4f) < 0.2f ? 0xFF4F4F : 0xFFC857, a);
        p.parrafo(locutor, x + 76, y + 13, Math.max(80f, w - 110f), letra, 0xFFF1C1, a);
    }

    // ================================================================ entre rondas

    private void entreRondas(Juego j) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.rect(0, 88, p.ancho, p.alto - 88, 0x000000, 0.55f);
        float x = 30, y = 110, w = p.ancho - 60, h = p.alto - 140;
        p.caja(x, y, w, h, 14, ui.tema.panel, 0.98f);
        String titRonda = T.f("pelea.entre_rondas", pelea.ronda(), pelea.ronda() + 1);
        float tamTit = ui.cabe(titRonda, w - 48, Math.min(32f, ui.letra * 1.3f), true);
        p.texto(ui.recortarTexto(titRonda, w - 48, tamTit, true), x + 24, y + 14, tamTit, true, ui.tema.texto, 1);
        String[] pest = {T.t("pelea.informe"), T.t("pelea.codigo"), T.t("pelea.piezas")};
        pestanaEntreRondas = ui.pestanas(pest, pestanaEntreRondas, x + 24, y + 60, Math.min(600, w - 48), 44);
        float cy = y + 116, ch = h - 116 - 80;
        switch (pestanaEntreRondas) {
            case 1:
                editor.dibujar(ui, x + 24, cy, w - 48, ch);
                if (ui.boton(compilando ? T.t("taller.compilando") : T.t("taller.compilar") + "  (Ctrl+S)", x + 24, y + h - 66, 260, 50,
                        Ui.Estilo.NORMAL, !compilando && !sinJdk, sinJdk ? T.t("taller.sin_jdk") : null)) compilar(j);
                if (!avisoDeCodigo.isEmpty()) {
                    float maxAvi = Math.max(120f, w - 660f);
                    float tamAvi = ui.cabe(avisoDeCodigo, maxAvi, ui.letra * 0.85f, false);
                    p.texto(ui.recortarTexto(avisoDeCodigo, maxAvi, tamAvi, false), x + 300, y + h - 52, tamAvi, false, ui.tema.textoSuave, 1);
                }
                break;
            case 2:
                Taller.piezas(j, yo, x + 24, cy, w - 48, ch);
                break;
            default:
                informe(j, x + 24, cy, w - 48, ch);
                break;
        }
        if (ui.boton(T.f("pelea.siguiente_ronda", pelea.ronda() + 1), x + w - 340, y + h - 66, 316, 52, Ui.Estilo.PRINCIPAL)) {
            if (encuentro.montaje == null && encuentro.codigo == null) j.jugador.robot().ponerPiezas(yo.montaje);
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
            Combatiente co = pelea.combatientes[i];
            String nombre = co.nombre + (co.montaje != null && co.montaje.modelo != null
                    ? "  ·  " + T.f("pelea.modelo", co.montaje.modelo.nombre(T.idioma)) : "");
            p.texto(nombre, cx, y, ui.cabe(nombre, col - 10, ui.letra * 1.1f, true), true, color, 1);
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
                yy += p.parrafo(l, cx, yy, col - 10, ui.letra * 0.9f, ui.tema.texto, 1) + 6;
            }
            yy += 10;
            for (java.util.Map.Entry<String, int[]> e : inf.porArma.entrySet()) {
                p.texto("· " + e.getKey() + ": " + e.getValue()[1] + "/" + e.getValue()[0], cx + 10, yy, ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
                yy += ui.letra * 1.3f;
            }
            if (i == 0 && pelea.combatientes[0].efectosPoo != null) {
                java.util.List<ModificadoresPoo.Entrada> poo = pelea.combatientes[0].efectosPoo.resumen();
                if (!poo.isEmpty()) {
                    yy += 8;
                    p.texto(T.t("poo.titulo"), cx, yy, ui.letra * 0.9f, true, ui.tema.texto, 1);
                    yy += ui.letra * 1.3f;
                    for (ModificadoresPoo.Entrada e : poo) {
                        int colPoo = e.nivel == ModificadoresPoo.Nivel.PENALIZACION ? ui.tema.mal
                                : e.nivel == ModificadoresPoo.Nivel.BONO ? ui.tema.bien : ui.tema.aviso;
                        yy += p.parrafo("• " + e.texto, cx + 6, yy, col - 16, ui.letra * 0.78f, colPoo, 1) + 4;
                    }
                }
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
        if (encuentro.montaje == null && encuentro.codigo == null) jug.robot().ponerPiezas(yo.montaje);
        Registro.Resultado res = g < 0 ? Registro.Resultado.EMPATE : ganoElJugador ? Registro.Resultado.VICTORIA : Registro.Resultado.DERROTA;
        if (encuentro.cuenta) {
            Registro r = new Registro(System.currentTimeMillis(), encuentro.modo, encuentro.rival.nombre, encuentro.rival.montaje.modelo.id,
                    yo.montaje.modelo.id, encuentro.nivelRival, res, yo.danoTotalRecibido, yo.danoTotalHecho, pelea.ronda(), jug.etiqueta.name());
            cambio = Progreso.despuesDeLaPelea(jug, r, encuentro.rival.montaje.modelo.naturaleza.name(), new Random());
            if (cambio.bajo) j.alarmaBajada.activar(cambio.antes, cambio.despues);
        }
        if (encuentro.rival != null) jug.registrarUltimoRival(encuentro.rival.nombre, metajavbots.rivales.Rivales.codigoDe(encuentro.rival));
        objetivoCumplido = encuentro.objetivo == null ? ganoElJugador
                : Objetivo.cumple(encuentro.objetivo, yo, robot.listo() ? robot.fuente() : editor.doc.texto());
        // Un robot roto solo cuenta arreglado si al final compila y se ha cambiado su código.
        if (encuentro.codigo != null) objetivoCumplido &= robot.listo() && !robot.fuente().equals(encuentro.codigo);
        if (encuentro.alAcabar != null) encuentro.alAcabar.accept(objetivoCumplido);
        j.guardar();
        narrador.locutor("explosion_final", true, null);
    }

    /** Si ha subido de nivel, fuegos artificiales, los robots saltan de alegría y la pantalla vibra. */
    private void celebrar(Juego j, float dt) {
        Pincel p = j.pincel;
        if (cambio != null && cambio.subio) {
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
    }

    private void pantallaFinal(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        tiempoFinal += dt;
        int g = pelea.ganador();
        if (!explotado && tiempoFinal > 0.6f && g >= 0) explotar(j, g);
        celebrar(j, dt);
        boolean bajoNivel = cambio != null && cambio.bajo;
        if (bajoNivel && tiempoFinal > 0.6f) j.alarmaBajada.dibujarBordeLuminoso(p, dt, true);
        if (tiempoFinal < 2.2f) return;
        if (repeticion.activa()) {
            repeticion.avanzar(j, pelea, vista, dt);
            repeticion.rotulo(j, 0, 88, p.ancho, p.alto - 88);
            return;
        }
        float w = Math.min(p.ancho - 60, bajoNivel ? 820 : 720);
        float h = Math.min(p.alto - 60, bajoNivel ? 580 : 470);
        float x = (p.ancho - w) / 2, y = (p.alto - h) / 2;
        p.caja(x, y, w, h, 16, ui.tema.panel, 0.97f);
        p.bordeRedondeado(x, y, w, h, 16, ganoElJugador ? ui.tema.bien : g == -1 ? ui.tema.aviso : ui.tema.mal, 1, bajoNivel ? 4 : 3);
        String titulo = g == -1 ? T.t("final.empate") : ganoElJugador ? T.t("final.victoria") : T.t("final.derrota");
        p.textoCentrado(titulo, x + w / 2, y + 24, ui.letra * 2.2f, true, ganoElJugador ? ui.tema.bien : ui.tema.texto, 1);
        p.textoCentrado(T.f("final.danos", Math.round(yo.danoTotalRecibido),
                Math.round(pelea.tieneMultiplesObjetivos() ? pelea.danoDelEquipo(1) : encuentro.rival.danoTotalRecibido)), x + w / 2, y + 90,
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
                yy += j.alarmaBajada.dibujarAvisoEnTarjeta(p, ui, x + 32, yy, w - 64) + 12;
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
        if (encuentro.jefe != null && ganoElJugador) entrenador.milagro(j, w, dt);
        botonesFinales(j, x, y, w, h);
    }

    private void botonesFinales(Juego j, float x, float y, float w, float h) {
        Ui ui = j.ui;
        boolean premio = encuentro.modo.equals("DESAFIO") && ganoElJugador && !premioElegido;
        if (premio && ui.boton(T.t("final.premio"), x + w / 2 - 170, y + h - 140, 340, 50, Ui.Estilo.PRINCIPAL)) {
            j.ir(new PremioDelDesafio(() -> premioElegido = true));
        }
        if (repeticion.hay() && ui.boton("▶ " + T.t("repeticion.ver"), x + w - 330, y + h - (premio ? 200 : 140), 300, 50, Ui.Estilo.PLANO)) {
            repeticion.empezar(pelea);
        }
        float bwFinal = (w - 72f) / 3f;
        if (ui.boton(T.t("final.revancha"), x + 24f, y + h - 74, bwFinal, 54, Ui.Estilo.NORMAL)) {
            j.volver();
            j.ir(PrepararPelea.repetir(j, encuentro));
        }
        if (ui.boton(T.t("final.revisar"), x + 36f + bwFinal, y + h - 74, bwFinal, 54, Ui.Estilo.NORMAL, true, T.t("final.revisar_ayuda"))) {
            j.ir(new Revision(editor.doc.texto(), pelea, yo));
        }
        if (encuentro.modo.equals("CAMPANA")) {
            if (ui.boton(T.t("final.seguir_campana"), x + 48f + 2f * bwFinal, y + h - 74, bwFinal, 54, Ui.Estilo.PRINCIPAL) || ui.atras()) j.volver();
        } else if (ui.boton(T.t("final.menu"), x + 48f + 2f * bwFinal, y + h - 74, bwFinal, 54, Ui.Estilo.PRINCIPAL) || ui.atras()) {
            j.irSolo(new Menu());
        }
    }

    private void reparacion(Juego j, float dt) {
        if (reparando == null) reparando = new RobotEnReparacion(() -> j.sonidos.tocar("compilar", 0.7f));
        reparando.panel(j, vista, yo, robot.listo(), robot.listo() ? robot.fuente() : "", encuentro.codigo, encuentro.objetivo, dt);
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
            j.sonidos.tocar("alerta_bajada", 0.9f);
            narrador.locutor("bajada_nivel", true, null);
        }
    }
}
