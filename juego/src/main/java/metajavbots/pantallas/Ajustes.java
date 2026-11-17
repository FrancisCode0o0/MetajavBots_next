package metajavbots.pantallas;

import java.nio.file.Path;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.cerebro.CerebroHttp;
import metajavbots.cerebro.CerebroLocal;
import metajavbots.cerebro.Claves;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Tema;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * AJUSTES — El idioma, el tamaño de la letra, el tema claro u oscuro, el
 * volumen, la voz del locutor y el cerebro de IA: ninguno, el local
 * (KoboldCpp, se puede arrancar desde aquí) o uno en línea con su clave
 * (que se guarda fuera del juego, en la carpeta del usuario).
 */
public final class Ajustes implements Pantalla {

    private final CampoDeTexto clave = new CampoDeTexto(), modelo = new CampoDeTexto(), direccion = new CampoDeTexto(), carpeta = new CampoDeTexto();
    private volatile String estadoCerebro = "";
    private boolean cargado;

    @Override
    public void alEntrar(Juego j) {
        if (cargado) return;
        cargado = true;
        Jugador jug = j.jugador;
        clave.oculto = true;
        clave.ponerTexto(Claves.leer(jug.proveedor));
        modelo.ponerTexto(jug.modeloEnLinea);
        direccion.ponerTexto(jug.direccionLocal);
        carpeta.ponerTexto(CerebroLocal.carpeta(null).toString());
    }

    @Override
    public void alSalir(Juego j) {
        Jugador jug = j.jugador;
        jug.modeloEnLinea = modelo.texto().strip();
        jug.direccionLocal = direccion.texto().strip();
        j.ponerJugador(jug);
        j.guardar();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("menu.ajustes"), true);
        float x = 60, y = arriba + 24, w = (p.ancho - 160) / 2;
        // A la izquierda: idioma, letra, tema, sonido, voz.
        p.texto(T.t("ajustes.idioma"), x, y, ui.letra, true, ui.tema.texto, 1);
        y += 34;
        for (int i = 0; i < Idioma.values().length; i++) {
            Idioma id = Idioma.values()[i];
            if (ui.boton(id.nombre, x + i * (w / 3), y, w / 3 - 10, 50, jug.idioma == id ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                jug.idioma = id;
                T.idioma = id;
            }
        }
        y += 74;
        float letra = ui.deslizador(T.f("ajustes.letra", Math.round(jug.tamanoDeLetra)), (jug.tamanoDeLetra - 14) / 14f, x, y, w, 50, T.t("ajustes.letra_ayuda"));
        jug.tamanoDeLetra = Math.round(14 + letra * 14);
        y += 76;
        boolean claro = ui.casilla(T.t("ajustes.tema_claro"), jug.temaClaro, x, y, null);
        if (claro != jug.temaClaro) {
            jug.temaClaro = claro;
            ui.tema = Tema.de(claro);
        }
        y += 50;
        jug.volumen = ui.deslizador(T.t("ajustes.volumen"), jug.volumen, x, y, w, 50, null);
        j.sonidos.volumen = jug.volumen;
        y += 76;
        jug.vozDelLocutor = ui.casilla(T.t("ajustes.voz"), jug.vozDelLocutor, x, y, T.t("ajustes.voz_ayuda"));
        j.voz.activa = jug.vozDelLocutor;
        y += 50;
        jug.volumenVoz = ui.deslizador(T.t("ajustes.volumen_voz"), jug.volumenVoz, x, y, w, 50, null);
        j.voz.volumen = jug.volumenVoz;
        y += 76;
        if (ui.boton(T.t("ajustes.probar_voz"), x, y, 260, 50, Ui.Estilo.NORMAL)) {
            j.voz.decir(metajavbots.dialogos.Dialogos.de("locutor").frase("inicio_pelea", jug.idioma, java.util.Map.of("jugador", jug.nombre)), jug.idioma);
        }
        j.voz.fotograma(j.sonidos);
        // A la derecha: el cerebro de IA.
        float cx = x + w + 40;
        float cy = ui.panel(cx, arriba + 16, w, p.alto - arriba - 40, T.t("ajustes.cerebro"));
        p.parrafo(T.t("ajustes.cerebro_ayuda"), cx + 20, cy, w - 40, ui.letra * 0.8f, ui.tema.textoSuave, 1);
        cy += 70;
        String[] opciones = {"NINGUNO", "LOCAL", "EN_LINEA"};
        for (int i = 0; i < 3; i++) {
            if (ui.boton(T.t("cerebro." + opciones[i].toLowerCase()), cx + 20 + i * ((w - 40) / 3), cy, (w - 40) / 3 - 10, 50,
                    jug.cerebro.equals(opciones[i]) ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) jug.cerebro = opciones[i];
        }
        cy += 70;
        if (jug.cerebro.equals("LOCAL")) {
            p.texto(T.t("ajustes.direccion"), cx + 20, cy, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
            direccion.dibujar(ui, cx + 20, cy + 26, w - 40, 46, "http://localhost:5002");
            jug.direccionLocal = direccion.texto().strip();
            cy += 90;
            p.texto(T.t("ajustes.carpeta_modelos"), cx + 20, cy, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
            carpeta.dibujar(ui, cx + 20, cy + 26, w - 40, 46, "");
            cy += 90;
            if (ui.boton(T.t("ajustes.arrancar_local"), cx + 20, cy, 300, 50, Ui.Estilo.NORMAL, true, T.t("ajustes.arrancar_ayuda"))) {
                Path c = CerebroLocal.carpeta(carpeta.texto());
                estadoCerebro = T.t("ajustes.arrancando");
                Thread h = new Thread(() -> {
                    String r = CerebroLocal.arrancarElQueFuncione(c, m -> estadoCerebro = T.f("ajustes.cargando", m));
                    estadoCerebro = r.equals("ya") ? T.t("ajustes.local_listo") : T.f("ajustes.local_error", r);
                    if (r.equals("ya")) {
                        jug.direccionLocal = "http://localhost:" + CerebroLocal.PUERTO_DEL_JUEGO;
                        direccion.ponerTexto(jug.direccionLocal);
                    }
                }, "arrancar-cerebro");
                h.setDaemon(true);
                h.start();
            }
            if (ui.boton(T.t("ajustes.usar_jarvis"), cx + 330, cy, w - 350, 50, Ui.Estilo.PLANO, true, T.t("ajustes.usar_jarvis_ayuda"))) {
                jug.direccionLocal = "http://localhost:" + CerebroLocal.PUERTO_DE_JARVIS;
                direccion.ponerTexto(jug.direccionLocal);
            }
            cy += 66;
        } else if (jug.cerebro.equals("EN_LINEA")) {
            String[] provs = {"anthropic", "openai", "otro"};
            for (int i = 0; i < provs.length; i++) {
                if (ui.boton(provs[i], cx + 20 + i * ((w - 40) / 3), cy, (w - 40) / 3 - 10, 46,
                        jug.proveedor.equals(provs[i]) ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                    jug.proveedor = provs[i];
                    clave.ponerTexto(Claves.leer(provs[i]));
                }
            }
            cy += 62;
            p.texto(T.t("ajustes.clave"), cx + 20, cy, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
            clave.dibujar(ui, cx + 20, cy + 26, w - 200, 46, "sk-…");
            if (ui.boton(T.t("ajustes.guardar_clave"), cx + w - 170, cy + 26, 150, 46, Ui.Estilo.NORMAL)) {
                try {
                    Claves.guardar(jug.proveedor, clave.texto());
                    estadoCerebro = T.t("ajustes.clave_guardada");
                } catch (Exception e) {
                    estadoCerebro = e.toString();
                }
            }
            cy += 84;
            p.parrafo(T.t("ajustes.clave_ayuda"), cx + 20, cy, w - 40, ui.letra * 0.72f, ui.tema.textoApagado, 1);
            cy += 50;
            p.texto(T.t("ajustes.modelo"), cx + 20, cy, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
            modelo.dibujar(ui, cx + 20, cy + 26, w - 40, 46, jug.proveedor.equals("anthropic") ? "claude-haiku-5-5" : "gpt-4o-mini");
            jug.modeloEnLinea = modelo.texto().strip();
            cy += 84;
            if (jug.proveedor.equals("otro")) {
                p.texto(T.t("ajustes.direccion"), cx + 20, cy, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
                direccion.dibujar(ui, cx + 20, cy + 26, w - 40, 46, "https://…");
                jug.direccionLocal = direccion.texto().strip();
                cy += 84;
            }
        }
        if (!jug.cerebro.equals("NINGUNO") && ui.boton(T.t("ajustes.probar"), cx + 20, cy, 200, 50, Ui.Estilo.PRINCIPAL)) {
            Cerebro c = CerebroDelJuego.de(jug);
            if (c == null) estadoCerebro = T.t("ajustes.falta_clave");
            else {
                estadoCerebro = T.t("cerebro.pensando");
                Thread h = new Thread(() -> estadoCerebro = ((CerebroHttp) c).responde() ? T.f("ajustes.responde", c.nombre()) : T.t("ajustes.no_responde"));
                h.setDaemon(true);
                h.start();
            }
        }
        p.parrafo(estadoCerebro, cx + 20, cy + 64, w - 40, ui.letra * 0.85f, ui.tema.texto, 1);
    }
}
