package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import metajavbots.api.Metajavbot;
import metajavbots.api.MetajavbotDe;
import metajavbots.api.MetajavbotEn;
import metajavbots.api.RobotBase;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.Vigilante;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Fabrica;
import metajavbots.combate.Pelea;
import metajavbots.pantallas.Taller;
import metajavbots.piezas.Arquetipo;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/** Pruebas de arquetipos: modelos, habilidades programables trilingües, bloqueos/avisos y plantilla del Taller. */
final class PruebasDeArquetipos {

    private PruebasDeArquetipos() {
    }

    private static Combatiente con(String nombre, Modelo m, java.util.function.Supplier<RobotBase> s) {
        Fabrica f = s::get;
        return new Combatiente(nombre, new Montaje(m), f);
    }

    private static boolean hayAvisoEnConsola(Combatiente c, String metodo) {
        for (String l : c.consola) {
            if (l.contains("⚠") && l.contains(metodo)) return true;
        }
        return false;
    }

    /** Robot pasivo para servir de rival en pruebas de arquetipo. */
    public static class Quieto extends Metajavbot {
    }

    /** Prueba embestir() en el primer turno. */
    public static class PruebaAsalto extends Metajavbot {
        @Override
        public void cadaTurno() {
            if (turnoActual() == 0) {
                memoria().put("arq", arquetipo());
                memoria().put("ok1", embestir());
                memoria().put("ok2", embestir());
            }
        }
    }

    /** Prueba pulsoRadar() mirando al lado contrario del rival. */
    public static class PruebaTactico extends Metajavbot {
        @Override
        public void cadaTurno() {
            if (turnoActual() == 0) {
                memoria().put("arq", arquetipo());
                memoria().put("antes", enemigosVistos().size());
                boolean ok = pulsoRadar();
                memoria().put("ok", ok);
                memoria().put("despues", enemigosVistos().size());
            }
        }
    }

    /** Prueba fortificar() en el primer turno. */
    public static class PruebaDefensor extends Metajavbot {
        @Override
        public void cadaTurno() {
            if (turnoActual() == 0) {
                avanzar(120);
                memoria().put("arq", arquetipo());
                memoria().put("ok", fortificar());
                memoria().put("restante", restanteAvance());
            }
        }
    }

    /** Fachada alemana: ansturm(), radarImpuls(), befestigen(). */
    public static class PruebaDe extends MetajavbotDe {
        @Override
        public void jedenZug() {
            if (zug() == 0) {
                gedaechtnis().put("arq", archetyp());
                gedaechtnis().put("asalto", ansturm());
                gedaechtnis().put("tactico", radarImpuls());
                gedaechtnis().put("defensor", befestigen());
            }
        }
    }

    /** Fachada inglesa: charge(), radarPulse(), fortify(). */
    public static class PruebaEn extends MetajavbotEn {
        @Override
        public void onTick() {
            if (tick() == 0) {
                memory().put("arq", archetype());
                memory().put("asalto", charge());
                memory().put("tactico", radarPulse());
                memory().put("defensor", fortify());
            }
        }
    }

    static void apuntar() {
        apuntarHabilidades();
        apuntarIdiomasYPlantilla();
    }

    private static void apuntarHabilidades() {
        caso("arquetipos: todos los modelos tienen arquetipo y los tres iniciales cubren Asalto, Táctico y Defensor", () -> {
            Set<Arquetipo> vistos = EnumSet.noneOf(Arquetipo.class);
            for (Modelo m : Modelo.todos()) {
                cierto(m.arquetipo != null, m.id + " sin arquetipo");
                vistos.add(m.arquetipo);
            }
            igual(3, vistos.size(), "los tres arquetipos están presentes");
            igual(Arquetipo.ASALTO, Modelo.de("chispa").arquetipo, "chispa es ASALTO");
            igual(Arquetipo.TACTICO, Modelo.de("vigia").arquetipo, "vigia es TACTICO");
            igual(Arquetipo.DEFENSOR, Modelo.de("cobalto").arquetipo, "cobalto es DEFENSOR");
        });

        caso("arquetipos: embestir() funciona en Asalto y se bloquea con aviso en consola en otros", () -> {
            T.idioma = Idioma.ES;
            Pelea ok = new Pelea(con("a", Modelo.de("chispa"), PruebaAsalto::new), con("b", Modelo.de("cobalto"), Quieto::new), 1);
            ok.empezarRonda();
            ok.avanzar();
            igual("ASALTO", ok.combatientes[0].memoria.get("arq"));
            igual(Boolean.TRUE, ok.combatientes[0].memoria.get("ok1"), "embestir() activa en chispa");
            igual(Boolean.FALSE, ok.combatientes[0].memoria.get("ok2"), "respeta el enfriamiento");
            cierto(ok.cuerpo(0).turbo > 0, "embestir() da turbo");
            falso(hayAvisoEnConsola(ok.combatientes[0], "embestir()"), "sin aviso en modelo correcto");

            Pelea mal = new Pelea(con("a", Modelo.de("cobalto"), PruebaAsalto::new), con("b", Modelo.de("chispa"), Quieto::new), 2);
            mal.empezarRonda();
            mal.avanzar();
            igual("DEFENSOR", mal.combatientes[0].memoria.get("arq"));
            igual(Boolean.FALSE, mal.combatientes[0].memoria.get("ok1"), "embestir() bloqueado en cobalto");
            igual(0, mal.cuerpo(0).turbo, "no activa turbo en otro arquetipo");
            cierto(hayAvisoEnConsola(mal.combatientes[0], "embestir()"), "avisa en consola: " + mal.combatientes[0].consola);
        });

        caso("arquetipos: pulsoRadar() revela en 360° en Táctico y se bloquea con aviso en consola en otros", () -> {
            T.idioma = Idioma.ES;
            Pelea ok = new Pelea(con("a", Modelo.de("vigia"), PruebaTactico::new), con("b", Modelo.de("cobalto"), Quieto::new), 3);
            ok.empezarRonda();
            Cuerpo a = ok.cuerpo(0), b = ok.cuerpo(1);
            a.x = 500;
            a.y = 350;
            a.mira = 0;
            b.x = 220;
            b.y = 350;
            ok.avanzar();
            igual("TACTICO", ok.combatientes[0].memoria.get("arq"));
            igual(0, ok.combatientes[0].memoria.get("antes"), "sin pulso no ve al enemigo detrás");
            igual(Boolean.TRUE, ok.combatientes[0].memoria.get("ok"), "pulsoRadar() activa en vigia");
            igual(1, ok.combatientes[0].memoria.get("despues"), "pulsoRadar() revela al enemigo detrás");
            cierto(a.pulsoTactico > 0, "mantiene el barrido táctico");
            falso(hayAvisoEnConsola(ok.combatientes[0], "pulsoRadar()"), "sin aviso en modelo correcto");

            Pelea mal = new Pelea(con("a", Modelo.de("chispa"), PruebaTactico::new), con("b", Modelo.de("cobalto"), Quieto::new), 4);
            mal.empezarRonda();
            Cuerpo a2 = mal.cuerpo(0), b2 = mal.cuerpo(1);
            a2.x = 500;
            a2.y = 350;
            a2.mira = 0;
            b2.x = 220;
            b2.y = 350;
            mal.avanzar();
            igual(Boolean.FALSE, mal.combatientes[0].memoria.get("ok"), "pulsoRadar() bloqueado en chispa");
            igual(0, mal.combatientes[0].memoria.get("despues"), "no revela al enemigo en otro arquetipo");
            igual(0, a2.pulsoTactico, "sin pulsoTactico");
            cierto(hayAvisoEnConsola(mal.combatientes[0], "pulsoRadar()"), "avisa en consola: " + mal.combatientes[0].consola);
        });

        caso("arquetipos: fortificar() blinda en Defensor y se bloquea con aviso en consola en otros", () -> {
            T.idioma = Idioma.ES;
            Pelea ok = new Pelea(con("a", Modelo.de("cobalto"), PruebaDefensor::new), con("b", Modelo.de("chispa"), Quieto::new), 5);
            ok.empezarRonda();
            ok.avanzar();
            igual("DEFENSOR", ok.combatientes[0].memoria.get("arq"));
            igual(Boolean.TRUE, ok.combatientes[0].memoria.get("ok"), "fortificar() activa en cobalto");
            igual(0.0, ok.combatientes[0].memoria.get("restante"), "fortificar() ancla el avance");
            cierto(ok.cuerpo(0).escudoCabeza > 0, "fortificar() sube el escudo");
            falso(hayAvisoEnConsola(ok.combatientes[0], "fortificar()"), "sin aviso en modelo correcto");

            Pelea mal = new Pelea(con("a", Modelo.de("vigia"), PruebaDefensor::new), con("b", Modelo.de("chispa"), Quieto::new), 6);
            mal.empezarRonda();
            mal.avanzar();
            igual(Boolean.FALSE, mal.combatientes[0].memoria.get("ok"), "fortificar() bloqueado en vigia");
            igual(0, mal.cuerpo(0).escudoCabeza, "no sube escudo en otro arquetipo");
            cierto(hayAvisoEnConsola(mal.combatientes[0], "fortificar()"), "avisa en consola: " + mal.combatientes[0].consola);
        });
    }

    private static void apuntarIdiomasYPlantilla() {
        caso("arquetipos: fachadas DE y EN habilitan solo su arquetipo y avisan en los otros", () -> {
            T.idioma = Idioma.DE;
            Pelea de = new Pelea(con("de", Modelo.de("chispa"), PruebaDe::new), con("b", Modelo.de("cobalto"), Quieto::new), 7);
            de.empezarRonda();
            de.avanzar();
            igual("ASALTO", de.combatientes[0].memoria.get("arq"));
            igual(Boolean.TRUE, de.combatientes[0].memoria.get("asalto"), "DE ansturm() en chispa");
            igual(Boolean.FALSE, de.combatientes[0].memoria.get("tactico"), "DE radarImpuls() bloqueado en chispa");
            igual(Boolean.FALSE, de.combatientes[0].memoria.get("defensor"), "DE befestigen() bloqueado en chispa");
            cierto(hayAvisoEnConsola(de.combatientes[0], "radarImpuls()"), "DE avisa en consola: " + de.combatientes[0].consola);

            T.idioma = Idioma.EN;
            Pelea en = new Pelea(con("en", Modelo.de("cobalto"), PruebaEn::new), con("b", Modelo.de("chispa"), Quieto::new), 8);
            en.empezarRonda();
            en.avanzar();
            igual("DEFENSOR", en.combatientes[0].memoria.get("arq"));
            igual(Boolean.FALSE, en.combatientes[0].memoria.get("asalto"), "EN charge() bloqueado en cobalto");
            igual(Boolean.FALSE, en.combatientes[0].memoria.get("tactico"), "EN radarPulse() bloqueado en cobalto");
            igual(Boolean.TRUE, en.combatientes[0].memoria.get("defensor"), "EN fortify() en cobalto");
            cierto(hayAvisoEnConsola(en.combatientes[0], "charge()"), "EN avisa en consola: " + en.combatientes[0].consola);
            T.idioma = Idioma.ES;
        });

        caso("arquetipos: la plantilla del Taller muestra el arquetipo y habilidad del modelo en es, de y en y compila", () -> {
            Modelo[] muestras = {Modelo.de("chispa"), Modelo.de("vigia"), Modelo.de("cobalto")};
            for (Idioma id : Idioma.values()) {
                for (Modelo m : muestras) {
                    String codigo = Taller.plantillaInicial(id, m);
                    cierto(codigo.contains(m.arquetipo.nombre(id)), id + "/" + m.id + " incluye nombre de arquetipo");
                    cierto(codigo.contains(m.arquetipo.habilidad(id)), id + "/" + m.id + " incluye habilidad propia");
                    if (Compilador.hayCompilador()) {
                        Compilador.Resultado r = Compilador.compilar(codigo, false, Vigilante.Modo.ROBOT, Locale.ROOT);
                        cierto(r.bien, "plantilla compila en " + id + " para " + m.id + ": " + r.errores);
                    }
                }
            }
        });
    }
}
