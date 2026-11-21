package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.pantallas.CodigoDelRival;
import metajavbots.pantallas.RivalesAbsurdos;
import metajavbots.piezas.Modelo;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.progreso.Registro;
import metajavbots.rivales.Rival;
import metajavbots.rivales.Rivales;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/** Pruebas de «Código del rival»: sparring al azar sin peleas previas y último rival real persistido. */
final class PruebasDelCodigoDelRival {

    private PruebasDelCodigoDelRival() {
    }

    static void apuntar() {
        caso("codigo del rival: partida nueva muestra un rival al azar con comentario de sparring en ES, DE y EN", () -> {
            comprobarSparring(Idioma.ES, "// Este es el código de un sparring", "extends Metajavbot");
            comprobarSparring(Idioma.DE, "// Dies ist der Code eines Sparring-Gegners", "extends MetajavbotDe");
            comprobarSparring(Idioma.EN, "// This is the code of a sparring opponent", "extends MetajavbotEn");
            Set<String> vistos = new HashSet<>();
            Jugador j = new Jugador("Nuevo");
            Random azar = new Random(1);
            for (int i = 0; i < 40; i++) {
                String codigo = CodigoDelRival.codigoPara(j, azar);
                if (codigo.contains("class Rival ")) vistos.add("Rival");
                if (codigo.contains("class Muneco ")) vistos.add("Muneco");
            }
            igual(2, vistos.size(), "elige entre los rivales válidos del juego: " + vistos);
            for (String clave : new String[]{"rival.sparring", "rival.ayuda_ultimo", "rival.ayuda_sparring"}) {
                cierto(T.existe(clave), "existe clave " + clave);
                for (Idioma id : Idioma.values()) {
                    cierto(!T.en(clave, id).isBlank(), clave + " no vacía en " + id);
                }
            }
        });

        caso("codigo del rival: muestra el último rival peleado (nunca uno futuro) y persiste al guardar y cargar", () -> {
            Path antes = Guardado.carpeta;
            Path tmp = Files.createTempDirectory("metajavbots-rival");
            Guardado.carpeta = tmp;
            try {
                Jugador j = new Jugador("Luchador");
                j.idioma = Idioma.ES;
                String codigoA = "public class RivalAlfa extends Metajavbot {\n    public void cadaTurno() { avanzar(42); }\n}\n";
                j.registrarUltimoRival("Alfa", codigoA);
                cierto(j.haPeleado(), "ya ha peleado");
                // Preparar otro rival futuro no cambia el último rival peleado.
                Combatiente futuro = Rivales.crear(Modelo.de("cobalto"), 0.5, 0.5, new Random(3));
                cierto(futuro != null, "rival futuro creado");
                String mostrado = CodigoDelRival.codigoPara(j, new Random(1));
                igual(codigoA, mostrado, "muestra el último rival peleado");
                falso(mostrado.startsWith(CodigoDelRival.comentarioDeSparring(Idioma.ES)), "sin comentario de sparring");

                // Persiste al guardar y cargar, y se traduce al idioma de la API activo.
                Guardado.guardar(j);
                Jugador cargado = Guardado.leer(Guardado.carpetaDe("Luchador"));
                igual("Alfa", cargado.ultimoRivalNombre, "nombre persistido");
                igual(codigoA, cargado.ultimoRivalCodigo, "código persistido");
                cargado.idioma = Idioma.EN;
                String enIngles = CodigoDelRival.codigoPara(cargado, new Random(2));
                cierto(enIngles.contains("extends MetajavbotEn"), "traducido a EN: " + enIngles);
                cierto(enIngles.contains("onTick()") && enIngles.contains("ahead(42)"), "métodos en EN: " + enIngles);
                falso(enIngles.startsWith(CodigoDelRival.comentarioDeSparring(Idioma.EN)), "sin sparring tras cargar");

                // Al registrar una pelea nueva contra otro rival, pasa a mostrar ese nuevo último rival.
                Registro reg = new Registro(System.currentTimeMillis(), "TUTORIAL", "Muñeco", "chatarra", "cobalto", 0.1,
                        Registro.Resultado.VICTORIA, 10, 80, 1, "NOOB_1");
                Progreso.despuesDeLaPelea(cargado, reg, "ACERO", new Random(4));
                cargado.idioma = Idioma.DE;
                String trasSegunda = CodigoDelRival.codigoPara(cargado, new Random(5));
                cierto(trasSegunda.contains("class Muneco extends MetajavbotDe"), "actualizado al nuevo último rival en DE");
                falso(trasSegunda.contains("RivalAlfa"), "ya no muestra el rival anterior");
            } finally {
                Guardado.carpeta = antes;
            }
        });

        caso("codigo del rival: compatible con partidas antiguas con historial y sin archivo ultimo_rival.java", () -> {
            Path antes = Guardado.carpeta;
            Path tmp = Files.createTempDirectory("metajavbots-rival-antiguo");
            Guardado.carpeta = tmp;
            try {
                Jugador j = new Jugador("Veterano");
                j.idioma = Idioma.EN;
                j.historial.add(new Registro(System.currentTimeMillis(), "ADAPTADA", "Chispa", "chispa", "cobalto", 0.3,
                        Registro.Resultado.VICTORIA, 20, 90, 4, "NOOB_1"));
                Guardado.guardar(j);
                falso(Files.exists(tmp.resolve(Guardado.carpetaDe("Veterano")).resolve("ultimo_rival.java")), "sin archivo nuevo");
                Jugador cargado = Guardado.leer(Guardado.carpetaDe("Veterano"));
                cierto(cargado.haPeleado(), "reconoce el historial antiguo");
                String codigo = CodigoDelRival.codigoPara(cargado, new Random(1));
                falso(codigo.startsWith(CodigoDelRival.comentarioDeSparring(Idioma.EN)), "no trata a un veterano como sparring");
                cierto(codigo.contains("class Rival extends MetajavbotEn"), "devuelve el código del rival en EN");
            } finally {
                Guardado.carpeta = antes;
            }
        });
        apuntarAvisosDelEquipo();
        apuntarDescripcionAbsurda();
    }

    private static void apuntarAvisosDelEquipo() {
        caso("rival: un aviso de objetivo cercano no le ordena avanzar en negativo", () -> {
            Pelea p = peleaConRival();
            Cuerpo cuerpo = p.cuerpo(0);
            Rival rival = (Rival) p.combatientes[0].robot();
            cuerpo.x = 300;
            cuerpo.y = 300;
            rival.alOirAlEquipo("compañero", "objetivo 310 310");
            igual(0.0, cuerpo.restanteAvance(), "a menos de 150 no avanza (ni hacia atrás)");
        });

        caso("rival: un aviso mal formado se ignora y el rival sigue operativo", () -> {
            Pelea p = peleaConRival();
            Cuerpo cuerpo = p.cuerpo(0);
            Rival rival = (Rival) p.combatientes[0].robot();
            cuerpo.x = 300;
            cuerpo.y = 300;
            rival.alOirAlEquipo("compañero", "objetivo abc 200");
            rival.alOirAlEquipo("compañero", "objetivo 100 xyz");
            rival.alOirAlEquipo("compañero", "objetivo NaN 200");
            igual(0.0, cuerpo.restanteAvance(), "los avisos rotos no mueven al rival");
            rival.alOirAlEquipo("compañero", "objetivo 1300 300");
            cierto(cuerpo.restanteAvance() > 0, "sigue operativo tras los avisos rotos: " + cuerpo.restanteAvance());
        });
    }

    private static Pelea peleaConRival() {
        Combatiente a = Rivales.crear(Modelo.de("cobalto"), 0.5, 0.5, new Random(11));
        Combatiente b = Rivales.crear(Modelo.de("chispa"), 0.5, 0.5, new Random(12));
        Pelea p = new Pelea(new Combatiente[]{a, b}, new int[]{0, 1}, 11);
        p.empezarRonda();
        return p;
    }

    private static void apuntarDescripcionAbsurda() {
        caso("codigo del rival: rival combatido recibe nombre y minibiografía absurda en pasado (ES/DE/EN); sparring queda exento", () -> {
            cierto(RivalesAbsurdos.todos().size() >= 15, "al menos 15 rivales absurdos: " + RivalesAbsurdos.todos().size());
            Set<String> nombres = new HashSet<>();
            for (RivalesAbsurdos.Perfil p : RivalesAbsurdos.todos()) {
                cierto(p.nombre != null && !p.nombre.isBlank(), "nombre no vacío");
                cierto(nombres.add(p.nombre), "nombre repetido: " + p.nombre);
                for (Idioma id : Idioma.values()) {
                    String bio = p.biografia(id);
                    cierto(bio != null && !bio.isBlank(), "biografía vacía para " + p.nombre + " en " + id);
                    cierto(!bio.contains("\n"), "biografía de 1-2 líneas sin saltos forzados: " + p.nombre);
                }
                cierto(!p.biografia(Idioma.ES).equals(p.biografia(Idioma.DE)), "DE traducido: " + p.nombre);
                cierto(!p.biografia(Idioma.ES).equals(p.biografia(Idioma.EN)), "EN traducido: " + p.nombre);
            }

            Jugador combatido = new Jugador("Guerrero");
            combatido.registrarUltimoRival("Cobalto", "public class Rival extends Metajavbot {}");
            RivalesAbsurdos.Perfil esperado = RivalesAbsurdos.para(combatido);
            String ayudaEs = CodigoDelRival.ayudaPara(combatido, Idioma.ES);
            String ayudaDe = CodigoDelRival.ayudaPara(combatido, Idioma.DE);
            String ayudaEn = CodigoDelRival.ayudaPara(combatido, Idioma.EN);
            cierto(ayudaEs.contains(esperado.nombre) && ayudaEs.contains(esperado.biografia(Idioma.ES)), "ES incluye nombre y bio: " + ayudaEs);
            cierto(ayudaDe.contains(esperado.nombre) && ayudaDe.contains(esperado.biografia(Idioma.DE)), "DE incluye nombre y bio: " + ayudaDe);
            cierto(ayudaEn.contains(esperado.nombre) && ayudaEn.contains(esperado.biografia(Idioma.EN)), "EN incluye nombre y bio: " + ayudaEn);
            cierto(ayudaEs.contains("era un robot") && ayudaEs.contains("estaba programado"), "ES en pasado: " + ayudaEs);
            cierto(ayudaDe.contains("war ein Roboter") && ayudaDe.contains("wurde"), "DE en pasado: " + ayudaDe);
            cierto(ayudaEn.contains("was a robot") && ayudaEn.contains("was programmed"), "EN en pasado: " + ayudaEn);

            Jugador sparring = new Jugador("SinPeleas");
            for (Idioma id : Idioma.values()) {
                String texto = CodigoDelRival.ayudaPara(sparring, id);
                igual(T.en("rival.ayuda_sparring", id), texto, "sparring conserva aviso simple en " + id);
                for (RivalesAbsurdos.Perfil p : RivalesAbsurdos.todos()) {
                    falso(texto.contains(p.nombre), "sparring sin nombre absurdo " + p.nombre + " en " + id);
                    falso(texto.contains(p.biografia(id)), "sparring sin minibiografía en " + id);
                }
            }
        });
    }

    private static void comprobarSparring(Idioma idioma, String comentarioEsperado, String herenciaEsperada) {
        Jugador j = new Jugador("Nuevo_" + idioma.name());
        j.idioma = idioma;
        falso(j.haPeleado(), "partida nueva sin peleas");
        String codigo = CodigoDelRival.codigoPara(j, new Random(7));
        String primeraLinea = codigo.split("\n", 2)[0];
        igual(comentarioEsperado, primeraLinea, "comentario de sparring en " + idioma);
        cierto(codigo.contains(herenciaEsperada), "código válido en idioma " + idioma);
        cierto(codigo.contains("class Rival ") || codigo.contains("class Muneco "), "rival válido del juego");
    }
}
