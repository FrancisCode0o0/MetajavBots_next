package metajavbots.pantallas;

import java.util.List;

import com.badlogic.gdx.Input.Keys;

import metajavbots.codigo.Paquetes;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Entrada;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * PANEL DE PAQUETES — Nueva pestaña del Taller al estilo de la documentación
 * oficial de Java (Javadoc): índice de paquetes permitidos, vista de paquete
 * agrupada por tipo (interfaces, clases, enums, anotaciones, excepciones y
 * errores), vista detallada de cada componente y buscador en tiempo real con
 * autocompletado por teclado y ratón.
 */
public final class PanelDePaquetes {

    /** Ficha práctica de un método o campo en el idioma activo. */
    public static final class FichaMiembro {
        public final String firma;
        public final String descripcion;
        public final String donde;
        public final String pseudocodigo;

        public FichaMiembro(String firma, String descripcion, String donde, String pseudocodigo) {
            this.firma = firma;
            this.descripcion = descripcion;
            this.donde = donde;
            this.pseudocodigo = pseudocodigo;
        }
    }

    private final CampoDeTexto busqueda = new CampoDeTexto();
    private final Ui.Desplazable desp = new Ui.Desplazable();
    private Paquetes.Paquete paqueteActual;
    private Paquetes.Componente componenteActual;
    private int miembroActual = -1;
    private int miembroEnfocado = -1;
    private boolean enfocadoPorTeclado;
    private String ultimaConsulta = "";
    private Idioma ultimoIdioma;
    private List<Paquetes.Sugerencia> sugerencias = List.of();
    private int sugerenciaElegida;
    private boolean desplegado;

    /** Resuelve la ficha práctica (descripción, dónde se usa y pseudocódigo) del miembro en el índice dado. */
    public static FichaMiembro fichaDeMiembro(Paquetes.Componente componente, int indice, Idioma idioma) {
        if (componente == null) return FichasDePaquetes.resolver(null, "", idioma);
        List<String> lista = componente.miembros(idioma);
        String linea = indice >= 0 && indice < lista.size() ? lista.get(indice) : "";
        return FichasDePaquetes.resolver(componente, linea, idioma);
    }

    /** Resuelve la ficha práctica (descripción, dónde se usa y pseudocódigo) de una línea de miembro. */
    public static FichaMiembro fichaDeMiembro(Paquetes.Componente componente, String lineaMiembro, Idioma idioma) {
        return FichasDePaquetes.resolver(componente, lineaMiembro, idioma);
    }

    /** Suelta el foco del buscador y cierra el desplegable (al cambiar de pestaña). */
    public void desenfocar(Ui ui) {
        busqueda.enfocado = false;
        desplegado = false;
        if (ui != null) ui.desenfocar(busqueda);
    }

    /** Abre directamente un paquete por nombre (útil para capturas y pruebas). */
    public void abrirPaquete(String nombre) {
        Paquetes.Paquete p = Paquetes.de(nombre);
        paqueteActual = p;
        componenteActual = null;
        miembroActual = -1;
        miembroEnfocado = -1;
        desplegado = false;
        busqueda.enfocado = false;
        desp.alPrincipio();
    }

    /** Abre directamente un componente dentro de su paquete (útil para capturas y pruebas). */
    public void abrirComponente(String nombrePaquete, String idComponente) {
        Paquetes.Paquete p = Paquetes.de(nombrePaquete);
        paqueteActual = p;
        componenteActual = p == null ? null : p.componente(idComponente);
        miembroActual = -1;
        miembroEnfocado = -1;
        desplegado = false;
        busqueda.enfocado = false;
        desp.alPrincipio();
    }

    /** Abre directamente la ficha de un método o campo concreto dentro de un componente (útil para capturas y pruebas). */
    public void abrirMiembro(String nombrePaquete, String idComponente, int indice) {
        abrirComponente(nombrePaquete, idComponente);
        if (componenteActual != null && indice >= 0 && indice < componenteActual.miembros(Idioma.ES).size()) {
            miembroActual = indice;
            miembroEnfocado = indice;
        }
    }

    /** Índice del miembro abierto actualmente (-1 si no se está viendo el detalle de un miembro). */
    public int miembroActual() {
        return miembroActual;
    }

    /** Escribe una búsqueda y abre el autocompletado (útil para capturas y pruebas). */
    public void escribirBusqueda(String texto, Idioma idioma) {
        busqueda.ponerTexto(texto == null ? "" : texto);
        busqueda.enfocado = true;
        actualizarSugerencias(idioma);
        desplegado = !busqueda.texto().isBlank();
    }

    /** Qué paquete o componente está consultando el jugador, para el contexto del entrenador con IA. */
    public String contextoParaIa() {
        if (componenteActual != null && paqueteActual != null && miembroActual >= 0) {
            FichaMiembro f = fichaDeMiembro(componenteActual, miembroActual, ultimoIdioma == null ? Idioma.ES : ultimoIdioma);
            return "The player has the Workshop Packages (Javadoc) tab open, viewing member "
                    + f.firma + " of " + paqueteActual.nombre + "." + componenteActual.id + ".";
        }
        if (componenteActual != null && paqueteActual != null) {
            return "The player has the Workshop Packages (Javadoc) tab open, viewing component "
                    + paqueteActual.nombre + "." + componenteActual.id + " (" + componenteActual.tipo.clave + ").";
        }
        if (paqueteActual != null) {
            return "The player has the Workshop Packages (Javadoc) tab open, viewing package " + paqueteActual.nombre + ".";
        }
        return "The player has the Workshop Packages (Javadoc) tab open on the package index.";
    }

    public void dibujar(Ui ui, Idioma idioma, float x, float y, float w, float h) {
        Pincel p = ui.p;
        p.caja(x, y, w, h, 10, ui.tema.panel, 1);
        p.bordeRedondeado(x, y, w, h, 10, ui.tema.borde, 1, 1);

        float sx = x + 16, sy = y + 14;
        float sh = Math.max(44, ui.letra * 1.9f);
        float sw = w - 32;
        boolean conTexto = !busqueda.texto().isBlank();
        float limpiarW = conTexto ? sh : 0;
        float campoW = sw - (conTexto ? limpiarW + 8 : 0);

        float filaH = Math.max(40, ui.letra * 1.9f);
        int filas = Math.max(1, Math.min(8, sugerencias.size()));
        float dx = sx, dy = sy + sh + 4, dw = sw, dh = filas * filaH + 10;
        boolean mostrarDesplegable = desplegado && conTexto;
        boolean enDesplegable = mostrarDesplegable && ui.e.dentro(dx, dy, dw, dh);

        if (enDesplegable) atenderRatonDesplegable(ui, dx, dy, dw, filaH);
        if (busqueda.enfocado && mostrarDesplegable) atenderTeclasDesplegable(ui);

        boolean intro = busqueda.dibujar(ui, sx, sy, campoW, sh, T.t("paquetes.buscar"));
        if (conTexto && ui.boton("✕", sx + campoW + 8, sy, limpiarW, sh, Ui.Estilo.PLANO)) {
            busqueda.ponerTexto("");
            sugerencias = List.of();
            desplegado = false;
            ultimaConsulta = "";
        }
        if (!busqueda.texto().equals(ultimaConsulta) || idioma != ultimoIdioma) {
            actualizarSugerencias(idioma);
            desplegado = !busqueda.texto().isBlank() && busqueda.enfocado;
        }
        if (intro && !sugerencias.isEmpty()) {
            elegirSugerencia(ui, sugerencias.get(Math.min(sugerenciaElegida, sugerencias.size() - 1)));
        }
        if (!busqueda.enfocado && !enDesplegable) desplegado = false;

        float navY = sy + sh + 10;
        float contenidoY = barraDeNavegacion(ui, idioma, x + 16, navY, w - 32);
        p.rect(x + 16, contenidoY - 6, w - 32, 1, ui.tema.borde, 1);

        float contenidoH = Math.max(60, y + h - contenidoY - 10);
        boolean bloquearDebajo = mostrarDesplegable && enDesplegable;
        if (paqueteActual == null) {
            dibujarIndice(ui, idioma, x + 12, contenidoY, w - 24, contenidoH, bloquearDebajo);
        } else if (componenteActual == null) {
            dibujarPaquete(ui, idioma, x + 12, contenidoY, w - 24, contenidoH, bloquearDebajo);
        } else if (miembroActual < 0) {
            dibujarComponente(ui, idioma, x + 12, contenidoY, w - 24, contenidoH, bloquearDebajo);
        } else {
            dibujarMiembro(ui, idioma, x + 12, contenidoY, w - 24, contenidoH);
        }

        if (desplegado && !busqueda.texto().isBlank()) {
            dibujarDesplegable(ui, idioma, dx, dy, dw, dh, filaH);
        }
    }

    private void actualizarSugerencias(Idioma idioma) {
        ultimaConsulta = busqueda.texto();
        ultimoIdioma = idioma;
        sugerencias = Paquetes.buscar(ultimaConsulta, idioma, 8);
        sugerenciaElegida = 0;
    }

    private void atenderTeclasDesplegable(Ui ui) {
        for (Entrada.Tecla k : ui.e.teclas) {
            if (k.usada) continue;
            if (k.codigo == Keys.DOWN && !sugerencias.isEmpty()) {
                sugerenciaElegida = (sugerenciaElegida + 1) % sugerencias.size();
                k.usada = true;
            } else if (k.codigo == Keys.UP && !sugerencias.isEmpty()) {
                sugerenciaElegida = (sugerenciaElegida - 1 + sugerencias.size()) % sugerencias.size();
                k.usada = true;
            } else if (k.codigo == Keys.ENTER || k.codigo == Keys.NUMPAD_ENTER) {
                if (!sugerencias.isEmpty()) {
                    elegirSugerencia(ui, sugerencias.get(Math.min(sugerenciaElegida, sugerencias.size() - 1)));
                    k.usada = true;
                }
            } else if (k.codigo == Keys.ESCAPE) {
                desenfocar(ui);
                ui.reservarEscape();
                k.usada = true;
            }
        }
    }

    private void atenderRatonDesplegable(Ui ui, float dx, float dy, float dw, float filaH) {
        if (sugerencias.isEmpty()) {
            ui.e.clic(dx, dy, dw, filaH + 10);
            return;
        }
        for (int i = 0; i < sugerencias.size(); i++) {
            float ry = dy + 5 + i * filaH;
            if (ui.e.dentro(dx + 4, ry, dw - 8, filaH)) {
                sugerenciaElegida = i;
                if (ui.e.clic(dx + 4, ry, dw - 8, filaH)) {
                    elegirSugerencia(ui, sugerencias.get(i));
                    return;
                }
            }
        }
    }

    private void elegirSugerencia(Ui ui, Paquetes.Sugerencia s) {
        paqueteActual = s.paquete;
        componenteActual = s.componente;
        miembroActual = -1;
        miembroEnfocado = -1;
        desenfocar(ui);
        desp.alPrincipio();
    }

    private float barraDeNavegacion(Ui ui, Idioma idioma, float x, float y, float w) {
        Pincel p = ui.p;
        if (paqueteActual == null) {
            float tTit = ui.letra * 1.05f;
            p.texto(ui.recortarTexto(T.t("paquetes.titulo"), w, tTit, true), x, y, tTit, true, ui.tema.texto, 1);
            float yy = y + p.altoDeLinea(tTit) + 4;
            yy += p.parrafo(T.t("paquetes.subtitulo"), x, yy, w, ui.letra * 0.8f, ui.tema.textoSuave, 1) + 10;
            return yy;
        }
        float bh = Math.max(38, ui.letra * 1.7f);
        if (componenteActual == null) {
            String[] txt = {T.t("paquetes.volver_indice")};
            Ui.Estilo[] est = {Ui.Estilo.PLANO};
            if (ui.botonera(txt, est, null, null, x, y, w, bh) == 0) {
                paqueteActual = null;
                miembroActual = -1;
                miembroEnfocado = -1;
                desp.alPrincipio();
                return y + ui.altoDeBotonera + 12;
            }
            float bw = Math.min(w, p.ancho(txt[0], Math.min(ui.letra, bh * 0.5f), true) + 34);
            float bx = x + bw + 12;
            float tCrumb = ui.letra * 0.85f;
            if (x + w - bx > 120) {
                p.texto(ui.recortarTexto(paqueteActual.nombre, x + w - bx, tCrumb, true), bx, y + (bh - tCrumb * 1.15f) / 2, tCrumb, true,
                        ui.tema.acento, 1);
            }
            return y + ui.altoDeBotonera + 12;
        }
        if (miembroActual >= 0) {
            String[] txt = {"← " + T.t("comun.volver"), T.t("paquetes.volver_indice")};
            Ui.Estilo[] est = {Ui.Estilo.NORMAL, Ui.Estilo.PLANO};
            int pulsado = ui.botonera(txt, est, null, null, x, y, w, bh);
            if (pulsado == 0) {
                miembroActual = -1;
                desp.alPrincipio();
            } else if (pulsado == 1) {
                paqueteActual = null;
                componenteActual = null;
                miembroActual = -1;
                miembroEnfocado = -1;
                desp.alPrincipio();
            }
            return y + ui.altoDeBotonera + 12;
        }
        String[] txt = {T.t("paquetes.volver_indice"), "← " + paqueteActual.nombre};
        Ui.Estilo[] est = {Ui.Estilo.PLANO, Ui.Estilo.NORMAL};
        int pulsado = ui.botonera(txt, est, null, null, x, y, w, bh);
        if (pulsado == 0) {
            paqueteActual = null;
            componenteActual = null;
            miembroActual = -1;
            miembroEnfocado = -1;
            desp.alPrincipio();
        } else if (pulsado == 1) {
            componenteActual = null;
            miembroActual = -1;
            miembroEnfocado = -1;
            desp.alPrincipio();
        }
        return y + ui.altoDeBotonera + 12;
    }

    // ================================================================ 1. Índice de paquetes

    private void dibujarIndice(Ui ui, Idioma idioma, float x, float y, float w, float h, boolean bloquearClic) {
        Pincel p = ui.p;
        float d = ui.desplazable(desp, x, y, w, h);
        float yy = y - d + 4;
        float ancho = w - 24;
        float tNom = ui.letra * 1.0f;
        float tRes = ui.letra * 0.82f;
        float tTag = ui.letra * 0.72f;
        for (Paquetes.Paquete pq : Paquetes.todos()) {
            String tag = etiquetaPaquete(pq);
            float tagW = p.ancho(tag, tTag, true) + 20;
            float resH = p.altoDeParrafo(pq.resumen(idioma), ancho - 28, tRes);
            float cardH = p.altoDeLinea(tNom) + resH + 26;
            boolean encima = !bloquearClic && ui.e.dentro(x + 4, yy, ancho, cardH) && yy + cardH > y && yy < y + h;
            p.caja(x + 4, yy, ancho, cardH, 8, encima ? ui.tema.panelAlto : ui.tema.fondo, 1);
            p.bordeRedondeado(x + 4, yy, ancho, cardH, 8, encima ? ui.tema.acento : ui.tema.borde, 1, encima ? 1.5f : 1);
            float maxNomW = Math.max(80, ancho - tagW - 36);
            p.texto(ui.recortarTexto(pq.nombre, maxNomW, tNom, true), x + 18, yy + 10, tNom, true, ui.tema.acento, 1);
            int colorTag = pq.automatico ? ui.tema.bien : pq.enPlantilla ? ui.tema.acento : pq.soloEjercicio ? ui.tema.aviso : ui.tema.textoSuave;
            p.caja(x + 4 + ancho - tagW - 12, yy + 9, tagW, tTag * 1.45f, 5, ui.tema.panel, 1);
            p.textoCentrado(tag, x + 4 + ancho - tagW / 2 - 12, yy + 11, tTag, true, colorTag, 1);
            p.parrafo(pq.resumen(idioma), x + 18, yy + 14 + p.altoDeLinea(tNom), ancho - 28, tRes, ui.tema.texto, 1);
            if (encima && ui.e.clic(x + 4, yy, ancho, cardH)) {
                paqueteActual = pq;
                componenteActual = null;
                desp.alPrincipio();
            }
            yy += cardH + 10;
        }
        yy += cajaAviso(p, ui, T.t("paquetes.bloqueado_titulo"), T.t("paquetes.bloqueados_resumen"), x + 4, yy, ancho) + 12;
        ui.finDesplazable(desp, yy + d - y);
    }

    // ================================================================ 2. Vista de paquete

    private void dibujarPaquete(Ui ui, Idioma idioma, float x, float y, float w, float h, boolean bloquearClic) {
        Pincel p = ui.p;
        Paquetes.Paquete pq = paqueteActual;
        float d = ui.desplazable(desp, x, y, w, h);
        float yy = y - d + 4;
        float ancho = w - 24;
        float tTit = ui.letra * 1.25f;
        p.texto(ui.recortarTexto(pq.nombre, ancho - 16, tTit, true), x + 8, yy, tTit, true, ui.tema.acento, 1);
        yy += p.altoDeLinea(tTit) + 6;
        String elem = T.f("paquetes.elementos", pq.componentes.size());
        String etq = etiquetaPaquete(pq);
        String meta = etq.equals(elem) ? elem : etq + " · " + elem;
        yy += p.parrafo(meta, x + 8, yy, ancho - 16, ui.letra * 0.8f, ui.tema.textoSuave, 1) + 8;
        String impTxt = pq.automatico ? T.t("paquetes.auto") : pq.sentenciaImport;
        yy += cajaCodigo(p, ui, T.t("paquetes.import_etiqueta"), impTxt, x + 4, yy, ancho) + 10;
        for (String par : pq.parrafos(idioma)) {
            yy += p.parrafo(par, x + 8, yy, ancho - 16, ui.letra * 0.88f, ui.tema.texto, 1) + 8;
        }
        String bloq = pq.bloqueado(idioma);
        if (!bloq.isEmpty()) {
            yy += cajaAviso(p, ui, T.t("paquetes.bloqueado_titulo"), bloq, x + 4, yy, ancho) + 12;
        }
        for (Paquetes.Tipo tipo : Paquetes.Tipo.values()) {
            List<Paquetes.Componente> grupo = pq.porTipo(tipo);
            if (grupo.isEmpty()) continue;
            yy += 6;
            p.texto(T.t("paquetes.grupo." + tipo.clave), x + 8, yy, ui.letra * 0.95f, true, colorTipo(ui, tipo), 1);
            yy += p.altoDeLinea(ui.letra * 0.95f) + 6;
            for (Paquetes.Componente c : grupo) {
                yy = filaComponente(ui, idioma, c, x + 4, yy, ancho, y, h, bloquearClic);
            }
        }
        ui.finDesplazable(desp, yy + d - y + 10);
    }

    private float filaComponente(Ui ui, Idioma idioma, Paquetes.Componente c, float x, float yy, float ancho, float visY, float visH, boolean bloq) {
        Pincel p = ui.p;
        float tNom = ui.letra * 0.92f;
        float tDesc = ui.letra * 0.8f;
        float descH = p.altoDeParrafo(c.descripcion(idioma), ancho - 28, tDesc);
        float cardH = p.altoDeLinea(tNom) + descH + 20;
        boolean encima = !bloq && ui.e.dentro(x, yy, ancho, cardH) && yy + cardH > visY && yy < visY + visH;
        p.caja(x, yy, ancho, cardH, 7, encima ? ui.tema.panelAlto : ui.tema.fondo, 1);
        p.bordeRedondeado(x, yy, ancho, cardH, 7, encima ? ui.tema.acento : ui.tema.borde, 1, encima ? 1.5f : 1);
        String nom = c.nombre(idioma);
        float tTag = ui.letra * 0.7f;
        float tagW = c.soloEjercicio ? p.ancho(T.t("paquetes.solo_ejercicio"), tTag, true) + 18 : 0;
        p.texto(ui.recortarTexto(nom, Math.max(80, ancho - tagW - 32), tNom, true), x + 14, yy + 8, tNom, true, colorTipo(ui, c.tipo), 1);
        if (c.soloEjercicio) {
            p.caja(x + ancho - tagW - 10, yy + 7, tagW, tTag * 1.45f, 5, ui.tema.panel, 1);
            p.textoCentrado(T.t("paquetes.solo_ejercicio"), x + ancho - tagW / 2 - 10, yy + 9, tTag, true, ui.tema.aviso, 1);
        }
        p.parrafo(c.descripcion(idioma), x + 14, yy + 11 + p.altoDeLinea(tNom), ancho - 28, tDesc, ui.tema.texto, 1);
        if (encima && ui.e.clic(x, yy, ancho, cardH)) {
            componenteActual = c;
            miembroActual = -1;
            miembroEnfocado = -1;
            desp.alPrincipio();
        }
        return yy + cardH + 8;
    }

    // ================================================================ 3. Vista de componente

    private void dibujarComponente(Ui ui, Idioma idioma, float x, float y, float w, float h, boolean bloquearClic) {
        Pincel p = ui.p;
        Paquetes.Paquete pq = paqueteActual;
        Paquetes.Componente c = componenteActual;
        List<String> miembros = c.miembros(idioma);
        if (!busqueda.enfocado && !desplegado) {
            atenderTeclasComponente(ui, miembros.size(), h);
            if (componenteActual == null || miembroActual >= 0) return;
        }
        float d = ui.desplazable(desp, x, y, w, h);
        float yy = y - d + 4;
        float ancho = w - 24;

        String tipoTxt = T.t("paquetes.tipo." + c.tipo.clave).toUpperCase(idioma.locale());
        String sub = tipoTxt + " · " + pq.nombre + (c.soloEjercicio ? " · " + T.t("paquetes.solo_ejercicio") : "");
        yy += p.parrafo(sub, x + 8, yy, ancho - 16, ui.letra * 0.78f, true, colorTipo(ui, c.tipo), 1) + 4;

        float tTit = ui.letra * 1.25f;
        yy += p.parrafo(c.nombre(idioma), x + 8, yy, ancho - 16, tTit, true, ui.tema.acento, 1) + 8;

        String imp = pq.automatico ? T.t("paquetes.auto") : "import " + c.nombreCompleto(idioma) + ";";
        yy += cajaCodigo(p, ui, T.t("paquetes.import_etiqueta"), imp, x + 4, yy, ancho) + 10;

        yy += p.parrafo(c.descripcion(idioma), x + 8, yy, ancho - 16, ui.letra * 0.9f, ui.tema.texto, 1) + 14;

        if (!miembros.isEmpty()) {
            p.texto(T.t("paquetes.miembros"), x + 8, yy, ui.letra * 0.92f, true, ui.tema.textoSuave, 1);
            yy += p.altoDeLinea(ui.letra * 0.92f) + 6;
            for (int i = 0; i < miembros.size(); i++) {
                yy = filaMiembro(p, ui, miembros.get(i), i, x + 4, yy, ancho, y, h, d, bloquearClic);
            }
            yy += 8;
        }
        enfocadoPorTeclado = false;
        String ej = c.ejemplo(idioma);
        if (!ej.isEmpty()) {
            yy += cajaCodigo(p, ui, T.t("paquetes.ejemplo"), ej, x + 4, yy, ancho) + 12;
        }
        ui.finDesplazable(desp, yy + d - y + 10);
    }

    private void atenderTeclasComponente(Ui ui, int totalMiembros, float h) {
        for (Entrada.Tecla k : ui.e.teclas) {
            if (k.usada) continue;
            if (k.codigo == Keys.DOWN && totalMiembros > 0) {
                miembroEnfocado = miembroEnfocado < 0 ? 0 : Math.min(totalMiembros - 1, miembroEnfocado + 1);
                enfocadoPorTeclado = true;
                k.usada = true;
            } else if (k.codigo == Keys.UP && totalMiembros > 0) {
                miembroEnfocado = miembroEnfocado <= 0 ? 0 : miembroEnfocado - 1;
                enfocadoPorTeclado = true;
                k.usada = true;
            } else if ((k.codigo == Keys.ENTER || k.codigo == Keys.NUMPAD_ENTER) && miembroEnfocado >= 0 && miembroEnfocado < totalMiembros) {
                miembroActual = miembroEnfocado;
                desp.alPrincipio();
                k.usada = true;
            } else if (k.codigo == Keys.PAGE_DOWN) {
                desp.desplazamiento += h * 0.8f;
                k.usada = true;
            } else if (k.codigo == Keys.PAGE_UP) {
                desp.desplazamiento = Math.max(0, desp.desplazamiento - h * 0.8f);
                k.usada = true;
            } else if (k.codigo == Keys.ESCAPE || k.codigo == Keys.BACKSPACE) {
                componenteActual = null;
                miembroEnfocado = -1;
                desp.alPrincipio();
                ui.reservarEscape();
                k.usada = true;
            }
        }
    }

    private float filaMiembro(Pincel p, Ui ui, String miembro, int indice, float x, float yy, float ancho,
            float visY, float visH, float d, boolean bloquearClic) {
        int sep = miembro.indexOf(" — ");
        float tF = ui.letra * 0.82f;
        float tD = ui.letra * 0.8f;
        float textoW = Math.max(80, ancho - 42);
        float bh;
        String firma = sep > 0 ? miembro.substring(0, sep) : miembro;
        String desc = sep > 0 ? miembro.substring(sep + 3) : "";
        float fh = sep > 0 ? p.altoDeParrafo(firma, textoW, tF, true) : p.altoDeParrafo(firma, textoW, tD);
        float dh = sep > 0 ? p.altoDeParrafo(desc, textoW, tD) : 0;
        bh = sep > 0 ? fh + dh + 18 : fh + 14;

        if (enfocadoPorTeclado && indice == miembroEnfocado) {
            desp.mostrar(yy + d - visY, bh, visH);
        }
        boolean encima = !bloquearClic && ui.e.dentro(x, yy, ancho, bh) && yy + bh > visY && yy < visY + visH;
        boolean enfocado = encima || indice == miembroEnfocado;
        p.caja(x, yy, ancho, bh, 6, enfocado ? ui.tema.panelAlto : ui.tema.fondo, 1);
        p.bordeRedondeado(x, yy, ancho, bh, 6, enfocado ? ui.tema.acento : ui.tema.borde, enfocado ? 1f : 0.6f, enfocado ? 1.5f : 1f);
        if (sep > 0) {
            p.parrafo(firma, x + 12, yy + 6, textoW, tF, true, ui.tema.metodo, 1);
            p.parrafo(desc, x + 12, yy + 10 + fh, textoW, tD, ui.tema.texto, 1);
        } else {
            p.parrafo(firma, x + 12, yy + 6, textoW, tD, ui.tema.texto, 1);
        }
        p.texto("›", x + ancho - 20, yy + (bh - tF * 1.15f) / 2, tF, true, enfocado ? ui.tema.acento : ui.tema.textoSuave, 1);
        if (encima && ui.e.clic(x, yy, ancho, bh)) {
            if (ui.sonidoDeClic != null) ui.sonidoDeClic.run();
            miembroActual = indice;
            miembroEnfocado = indice;
            desp.alPrincipio();
        }
        return yy + bh + 6;
    }

    // ================================================================ 4. Vista de detalle de miembro

    private void dibujarMiembro(Ui ui, Idioma idioma, float x, float y, float w, float h) {
        if (!busqueda.enfocado && !desplegado) {
            atenderTeclasMiembro(ui, h);
            if (miembroActual < 0) return;
        }
        Pincel p = ui.p;
        Paquetes.Paquete pq = paqueteActual;
        Paquetes.Componente c = componenteActual;
        FichaMiembro ficha = fichaDeMiembro(c, miembroActual, idioma);
        float d = ui.desplazable(desp, x, y, w, h);
        float yy = y - d + 4;
        float ancho = w - 24;

        String sub = c.nombre(idioma) + " · " + pq.nombre;
        yy += p.parrafo(sub, x + 8, yy, ancho - 16, ui.letra * 0.78f, true, colorTipo(ui, c.tipo), 1) + 4;
        yy += p.parrafo(ficha.firma, x + 8, yy, ancho - 16, ui.letra * 1.08f, true, ui.tema.metodo, 1) + 12;

        yy += cajaBloque(p, ui, T.t("paquetes.miembro.descripcion"), ficha.descripcion, x + 4, yy, ancho, ui.tema.acento) + 12;
        yy += cajaBloque(p, ui, T.t("paquetes.miembro.donde"), ficha.donde, x + 4, yy, ancho, ui.tema.aviso) + 12;
        yy += cajaCodigo(p, ui, T.t("paquetes.miembro.ejemplo"), ficha.pseudocodigo, x + 4, yy, ancho) + 14;
        ui.finDesplazable(desp, yy + d - y + 10);
    }

    private void atenderTeclasMiembro(Ui ui, float h) {
        for (Entrada.Tecla k : ui.e.teclas) {
            if (k.usada) continue;
            if (k.codigo == Keys.DOWN) {
                desp.desplazamiento += ui.letra * 3;
                k.usada = true;
            } else if (k.codigo == Keys.UP) {
                desp.desplazamiento = Math.max(0, desp.desplazamiento - ui.letra * 3);
                k.usada = true;
            } else if (k.codigo == Keys.PAGE_DOWN) {
                desp.desplazamiento += h * 0.8f;
                k.usada = true;
            } else if (k.codigo == Keys.PAGE_UP) {
                desp.desplazamiento = Math.max(0, desp.desplazamiento - h * 0.8f);
                k.usada = true;
            } else if (k.codigo == Keys.ESCAPE || k.codigo == Keys.BACKSPACE) {
                miembroActual = -1;
                desp.alPrincipio();
                ui.reservarEscape();
                k.usada = true;
            }
        }
    }

    private static float cajaBloque(Pincel p, Ui ui, String titulo, String texto, float x, float y, float w, int colorBorde) {
        float tTit = ui.letra * 0.82f;
        float tTxt = ui.letra * 0.84f;
        float titH = p.altoDeParrafo(titulo, w - 28, tTit, true);
        float txtH = p.altoDeParrafo(texto, w - 28, tTxt);
        float ch = titH + txtH + 22;
        p.caja(x, y, w, ch, 6, ui.tema.panelAlto, 1);
        p.rect(x, y + 4, 4, Math.max(0, ch - 8), colorBorde, 0.95f);
        p.bordeRedondeado(x, y, w, ch, 6, ui.tema.borde, 0.8f, 1);
        p.parrafo(titulo, x + 14, y + 7, w - 28, tTit, true, colorBorde, 1);
        p.parrafo(texto, x + 14, y + 12 + titH, w - 28, tTxt, ui.tema.texto, 1);
        return ch;
    }

    // ================================================================ Desplegable de autocompletado

    private void dibujarDesplegable(Ui ui, Idioma idioma, float dx, float dy, float dw, float dh, float filaH) {
        Pincel p = ui.p;
        p.caja(dx + 3, dy + 4, dw, dh, 8, 0x000000, 0.4f);
        p.caja(dx, dy, dw, dh, 8, ui.tema.panelAlto, 0.99f);
        p.bordeRedondeado(dx, dy, dw, dh, 8, ui.tema.acento, 1, 1.5f);
        if (sugerencias.isEmpty()) {
            float t = ui.letra * 0.82f;
            p.texto(ui.recortarTexto(T.f("paquetes.sin_resultados", busqueda.texto().strip()), dw - 24, t, false),
                    dx + 12, dy + (dh - t * 1.15f) / 2, t, false, ui.tema.textoApagado, 1);
            return;
        }
        float tTag = ui.letra * 0.68f;
        float tNom = ui.letra * 0.85f;
        float tSub = ui.letra * 0.74f;
        for (int i = 0; i < sugerencias.size(); i++) {
            Paquetes.Sugerencia s = sugerencias.get(i);
            float ry = dy + 5 + i * filaH;
            if (i == sugerenciaElegida) p.caja(dx + 4, ry, dw - 8, filaH - 2, 6, ui.tema.seleccion, 1);
            String tag = s.esPaquete() ? T.t("paquetes.tipo.paquete") : T.t("paquetes.tipo." + s.componente.tipo.clave);
            int colorTag = s.esPaquete() ? ui.tema.acento : colorTipo(ui, s.componente.tipo);
            float tagW = Math.min(dw * 0.24f, p.ancho(tag, tTag, true) + 14);
            p.caja(dx + 10, ry + (filaH - tTag * 1.5f) / 2 - 1, tagW, tTag * 1.5f, 4, ui.tema.panel, 1);
            p.textoCentrado(ui.recortarTexto(tag, tagW - 6, tTag, true), dx + 10 + tagW / 2, ry + (filaH - tTag * 1.2f) / 2, tTag, true, colorTag, 1);
            float nomX = dx + 18 + tagW;
            String tit = s.titulo(idioma);
            String sub = s.subtitulo(idioma);
            float maxNomW = Math.max(80, dw * 0.38f);
            String titRec = ui.recortarTexto(tit, maxNomW, tNom, true);
            p.texto(titRec, nomX, ry + (filaH - tNom * 1.15f) / 2, tNom, true, ui.tema.texto, 1);
            float subX = nomX + p.ancho(titRec, tNom, true) + 12;
            float subW = dx + dw - 12 - subX;
            if (subW > 60) {
                p.texto(ui.recortarTexto(sub, subW, tSub, false), subX, ry + (filaH - tSub * 1.15f) / 2, tSub, false, ui.tema.textoSuave, 1);
            }
        }
    }

    // ================================================================ Auxiliares visuales

    private static float cajaCodigo(Pincel p, Ui ui, String rotulo, String codigo, float x, float y, float w) {
        float tRot = ui.letra * 0.78f;
        float tCod = ui.letra * 0.8f;
        float rotH = p.parrafo(rotulo, x + 4, y, Math.max(60, w - 8), tRot, true, ui.tema.textoSuave, 1);
        float cy = y + rotH + 4;
        float ch = p.altoDeParrafoDeCodigo(codigo, Math.max(60, w - 24), tCod, false) + 14;
        p.caja(x, cy, w, ch, 6, ui.tema.fondo, 1);
        p.bordeRedondeado(x, cy, w, ch, 6, ui.tema.borde, 0.8f, 1);
        p.parrafoDeCodigo(codigo, x + 12, cy + 7, Math.max(60, w - 24), tCod, false, ui.tema.cadena, 1);
        return (cy + ch) - y;
    }

    private static float cajaAviso(Pincel p, Ui ui, String titulo, String texto, float x, float y, float w) {
        float tTit = ui.letra * 0.78f;
        float tTxt = ui.letra * 0.78f;
        float th = p.altoDeParrafo(texto, w - 32, tTxt);
        float ch = p.altoDeLinea(tTit) + th + 18;
        p.caja(x, y, w, ch, 6, ui.tema.panelAlto, 1);
        p.rect(x, y + 4, 4, Math.max(0, ch - 8), ui.tema.aviso, 0.95f);
        p.bordeRedondeado(x, y, w, ch, 6, ui.tema.borde, 0.8f, 1);
        p.texto(titulo, x + 14, y + 6, tTit, true, ui.tema.aviso, 1);
        p.parrafo(texto, x + 14, y + 10 + p.altoDeLinea(tTit), w - 26, tTxt, ui.tema.texto, 1);
        return ch;
    }

    private static String etiquetaPaquete(Paquetes.Paquete pq) {
        if (pq.automatico) return T.t("paquetes.auto");
        if (pq.enPlantilla) return T.t("paquetes.plantilla");
        if (pq.soloEjercicio) return T.t("paquetes.solo_ejercicio");
        return T.f("paquetes.elementos", pq.componentes.size());
    }

    private static int colorTipo(Ui ui, Paquetes.Tipo tipo) {
        switch (tipo) {
            case INTERFAZ: return ui.tema.metodo;
            case CLASE: return ui.tema.tipo;
            case ENUM: return ui.tema.anotacion;
            case ANOTACION: return ui.tema.clave;
            case EXCEPCION: return ui.tema.aviso;
            case ERROR: return ui.tema.mal;
            default: return ui.tema.texto;
        }
    }
}
