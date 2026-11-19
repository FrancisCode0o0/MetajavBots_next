package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Leccion;
import metajavbots.ejercicios.Tema;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;

/**
 * UNA LECCIÓN — Página a página, con su ejemplo; al final, las tareas: un
 * ejercicio (se abre) o una prueba con tu robot contra el muñeco. Con
 * cerebro de IA, se le puede preguntar al tutor sobre la página: así el
 * tutorial se vuelve una conversación.
 */
public final class PantallaLeccion implements Pantalla {

    private final Leccion l;
    private int pagina;
    private final boolean[] hechas;
    private final CampoDeTexto pregunta = new CampoDeTexto();
    private final List<Cerebro.Mensaje> chat = new ArrayList<>();
    private CerebroDelJuego.Peticion peticion;
    /** El cerebro del tutor (se mira al entrar: preguntarlo en cada fotograma leería las claves del disco). */
    private Cerebro cerebro;
    private final Ui.Desplazable despl = new Ui.Desplazable();

    public PantallaLeccion(Leccion l) {
        this.l = l;
        this.hechas = new boolean[l.tareas.size()];
    }

    @Override
    public void alEntrar(Juego j) {
        cerebro = CerebroDelJuego.tutor(j.jugador);
        Jugador jug = j.jugador;
        for (int i = 0; i < l.tareas.size(); i++) {
            Leccion.Tarea t = l.tareas.get(i);
            if (t.ejercicio != null && jug.ejerciciosResueltos.contains(t.ejercicio)) hechas[i] = true;
        }
        boolean todas = true;
        for (boolean h : hechas) todas &= h;
        if (todas && !l.tareas.isEmpty() && jug.tutorialesHechos.add(l.id)) {
            j.sonidos.tocar("subida", 0.6f);
            j.guardar();
        }
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, l.titulo(T.idioma), true);
        float lado = cerebro != null ? 440 : 0;
        float w = Math.min(1100, p.ancho - 80 - (lado > 0 ? lado + 20 : 0));
        // El texto, de un ancho cómodo de leer (y centrado si sobra sitio).
        float x = lado > 0 ? 40 : (p.ancho - w) / 2, y = arriba + 20, h = p.alto - y - 110;
        int total = l.paginas.size() + 1;
        ui.barra(x, y, w, 8, (pagina + 1f) / total, ui.tema.acento, null);
        float cy = y + 30;
        if (pagina < l.paginas.size()) {
            Leccion.Pagina pg = l.paginas.get(pagina);
            cy += p.parrafo(pg.texto.en(T.idioma), x, cy, w, ui.letra * 1.15f, ui.tema.texto, 1) + 24;
            if (!pg.codigo.isBlank()) {
                String[] ls = pg.codigo.split("\n");
                float ch = ls.length * ui.letra * 1.3f + 30;
                p.caja(x, cy, w, ch, 10, ui.tema.panel, 1);
                float yy = cy + 15;
                for (String s : ls) {
                    p.texto(s, x + 24, yy, ui.letra, false, ui.tema.cadena, 1);
                    yy += ui.letra * 1.3f;
                }
            }
        } else {
            p.texto(T.t("leccion.tareas"), x, cy, ui.letra * 1.4f, true, ui.tema.texto, 1);
            cy += 50;
            for (int i = 0; i < l.tareas.size(); i++) {
                Leccion.Tarea t = l.tareas.get(i);
                p.texto(hechas[i] ? "✔" : "○", x, cy + 4, ui.letra * 1.3f, true, hechas[i] ? ui.tema.bien : ui.tema.textoApagado, 1);
                float th = p.parrafo(t.texto.en(T.idioma), x + 40, cy, w - 360, ui.letra, ui.tema.texto, 1);
                if (t.ejercicio != null) {
                    Ejercicio e = Banco.delJuego().ejercicio(t.ejercicio);
                    if (e != null && ui.boton(T.t("leccion.abrir_ejercicio"), x + w - 300, cy, 300, 50, hechas[i] ? Ui.Estilo.PLANO : Ui.Estilo.PRINCIPAL)) {
                        j.ir(new PantallaEjercicio(e));
                    }
                } else if (t.robot != null) {
                    p.texto(Objetivo.texto(t.robot), x + 40, cy + th + 4, ui.letra * 0.85f, false, ui.tema.aviso, 1);
                    if (ui.boton(T.t("leccion.probar_robot"), x + w - 300, cy, 300, 50, hechas[i] ? Ui.Estilo.PLANO : Ui.Estilo.PRINCIPAL)) {
                        final int k = i;
                        Encuentro en = new Encuentro();
                        en.rival = Rivales.muneco();
                        en.modo = "TUTORIAL";
                        en.cuenta = false;
                        en.rondas = 1;
                        en.objetivo = t.robot;
                        en.escenario = PrepararPelea.escenarioAlAzar();
                        en.alAcabar = bien -> {
                            if (bien) hechas[k] = true;
                        };
                        j.ir(new PantallaPelea(en));
                    }
                    if (ui.boton(T.t("menu.taller"), x + w - 300, cy + 58, 300, 46, Ui.Estilo.PLANO)) j.ir(new Taller());
                    th += 60;
                }
                cy += Math.max(th, 60) + 30;
            }
        }
        float by = p.alto - 80;
        if (ui.boton("← " + T.t("leccion.anterior"), x, by, 280, 54, Ui.Estilo.PLANO, pagina > 0, null)) pagina--;
        if (pagina < l.paginas.size()) {
            if (ui.boton(T.t("leccion.siguiente") + " →", x + w - 280, by, 280, 54, Ui.Estilo.PRINCIPAL)) pagina++;
        } else if (ui.boton(T.t("leccion.volver"), x + w - 280, by, 280, 54, Ui.Estilo.PRINCIPAL)) {
            alEntrar(j);
            j.volver();
        }
        if (cerebro != null) tutor(j, cerebro, p.ancho - 40 - lado, y, lado, p.alto - y - 30);
        if (!pregunta.enfocado) {
            if (pagina < l.paginas.size() && j.entrada.tecla(com.badlogic.gdx.Input.Keys.RIGHT)) pagina++;
            if (pagina > 0 && j.entrada.tecla(com.badlogic.gdx.Input.Keys.LEFT)) pagina--;
        }
    }

    private void tutor(Juego j, Cerebro c, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float cy = ui.panel(x, y, w, h, T.t("leccion.tutor"));
        float zona = h - (cy - y) - 70;
        float d = ui.desplazable(despl, x + 8, cy, w - 16, zona);
        float yy = cy - d;
        if (chat.isEmpty()) yy += p.parrafo(T.t("leccion.tutor_ayuda"), x + 16, yy, w - 40, ui.letra * 0.8f, ui.tema.textoApagado, 1);
        for (Cerebro.Mensaje m : chat) {
            boolean mio = m.rol.equals("user");
            String texto = mio ? m.texto.substring(m.texto.lastIndexOf('\n') + 1) : m.texto;
            yy += p.parrafo((mio ? "» " : "") + texto, x + (mio ? 36 : 16), yy, w - 60, ui.letra * 0.82f, mio ? ui.tema.acento : ui.tema.texto, 1) + 10;
        }
        if (peticion != null && !peticion.lista) {
            yy += p.parrafo(T.t("cerebro.pensando"), x + 16, yy, w - 40, ui.letra * 0.8f, ui.tema.textoApagado, 1);
        } else if (peticion != null) {
            chat.add(new Cerebro.Mensaje("assistant", peticion.respuesta != null ? peticion.respuesta : T.f("cerebro.error", peticion.error)));
            peticion = null;
            despl.desplazamiento = 1e6f;
        }
        ui.finDesplazable(despl, yy + d - cy + 10);
        if (pregunta.dibujar(ui, x + 12, y + h - 58, w - 24, 46, T.t("ejercicio.pregunta_al_tutor")) && !pregunta.texto().isBlank() && peticion == null) {
            String contexto = pagina < l.paginas.size() ? l.paginas.get(pagina).texto.en + "\n" + l.paginas.get(pagina).codigo : "";
            chat.add(new Cerebro.Mensaje("user", "Lesson page:\n" + contexto + "\nMy question:\n" + pregunta.texto()));
            Tema tm = Tema.de(l.tema);
            peticion = CerebroDelJuego.Peticion.lanzar(c, CerebroDelJuego.sistemaTutor(j.jugador.idioma, j.jugador.etiqueta.nombre,
                    tm == null ? "" : tm.nombre.en), new ArrayList<>(chat), 140);
            pregunta.ponerTexto("");
        }
    }
}
