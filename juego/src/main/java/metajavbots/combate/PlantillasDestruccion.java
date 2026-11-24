package metajavbots.combate;

import metajavbots.textos.Idioma;

/**
 * Plantillas iniciales compilables en ES, DE y EN para el reto especial
 * «Destrucción y supervivencia», sin fugas de API entre idiomas.
 */
final class PlantillasDestruccion {

    private PlantillasDestruccion() {
    }

    static String plantillaInicial(Idioma idioma) {
        Idioma id = idioma == null ? Idioma.ES : idioma;
        switch (id) {
            case DE:
                return "import metajavbots.api.*;\n\n"
                        + "// Spezialaufgabe: 3x-Arena mit Countdown.\n"
                        + "// Nutze hindernisse() und gesehen(), um herabfallende defekte Roboter\n"
                        + "// sowie Kisten, Fässer und Schornsteine zu zerstören und zu überleben.\n"
                        + "public class UeberlebensBot extends MetajavbotDe {\n"
                        + "    @Override\n"
                        + "    public void jedenZug() {\n"
                        + "        if (!gesehen().isEmpty()) {\n"
                        + "            Gegner ziel = gesehen().get(0);\n"
                        + "            zielenNach(ziel.winkel());\n"
                        + "            angreifen();\n"
                        + "        } else {\n"
                        + "            for (Hindernis h : hindernisse()) {\n"
                        + "                if (h.typ().equals(\"caja\") || h.typ().equals(\"barril\") || h.typ().equals(\"chimenea\")) {\n"
                        + "                    zielenNach(h.winkel());\n"
                        + "                    angreifen();\n"
                        + "                    break;\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "        drehen(8);\n"
                        + "        vorwaerts(30);\n"
                        + "    }\n"
                        + "}\n";
            case EN:
                return "import metajavbots.api.*;\n\n"
                        + "// Special task: 3x arena with countdown.\n"
                        + "// Use obstacles() and seenEnemies() to destroy falling broken robots\n"
                        + "// plus crates, barrels, and chimneys while surviving until zero.\n"
                        + "public class SurvivalBot extends MetajavbotEn {\n"
                        + "    @Override\n"
                        + "    public void onTick() {\n"
                        + "        if (!seenEnemies().isEmpty()) {\n"
                        + "            Enemy target = seenEnemies().get(0);\n"
                        + "            aimTo(target.angle());\n"
                        + "            attack();\n"
                        + "        } else {\n"
                        + "            for (Obstacle o : obstacles()) {\n"
                        + "                if (o.type().equals(\"caja\") || o.type().equals(\"barril\") || o.type().equals(\"chimenea\")) {\n"
                        + "                    aimTo(o.angle());\n"
                        + "                    attack();\n"
                        + "                    break;\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "        turn(8);\n"
                        + "        ahead(30);\n"
                        + "    }\n"
                        + "}\n";
            default:
                return "import metajavbots.api.*;\n\n"
                        + "// Reto especial: arena 3x con cuenta atrás.\n"
                        + "// Usa obstaculos() y enemigosVistos() para destruir robots averiados\n"
                        + "// caídos del cielo y cajas, barriles o chimeneas mientras sobrevives.\n"
                        + "public class BotSupervivencia extends Metajavbot {\n"
                        + "    @Override\n"
                        + "    public void cadaTurno() {\n"
                        + "        if (!enemigosVistos().isEmpty()) {\n"
                        + "            Enemigo blanco = enemigosVistos().get(0);\n"
                        + "            apuntarHacia(blanco.angulo());\n"
                        + "            atacar();\n"
                        + "        } else {\n"
                        + "            for (Obstaculo o : obstaculos()) {\n"
                        + "                if (o.tipo().equals(\"caja\") || o.tipo().equals(\"barril\") || o.tipo().equals(\"chimenea\")) {\n"
                        + "                    apuntarHacia(o.angulo());\n"
                        + "                    atacar();\n"
                        + "                    break;\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "        girar(8);\n"
                        + "        avanzar(30);\n"
                        + "    }\n"
                        + "}\n";
        }
    }
}
