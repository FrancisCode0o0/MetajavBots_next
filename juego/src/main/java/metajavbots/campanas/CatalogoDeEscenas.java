package metajavbots.campanas;

import java.util.List;

import metajavbots.campanas.EscenaDeCampana.Animacion;
import metajavbots.campanas.EscenaDeCampana.Decorado;
import metajavbots.campanas.EscenaDeCampana.Momento;
import metajavbots.campanas.EscenaDeCampana.Vineta;
import metajavbots.textos.Trilingue;

/**
 * CATÁLOGO DE ESCENAS — Escenas ilustradas (viñetas dibujadas por código) para
 * cada capítulo de la campaña Creed ampliada (8 capítulos), de la segunda
 * campaña «Isla Clandestina: Torneo de los 1000 Voltios» (8 capítulos) y un
 * generador determinista para el resto de campañas y las inventadas por la IA.
 */
public final class CatalogoDeEscenas {

    private CatalogoDeEscenas() {
    }

    /** Devuelve siempre una escena ilustrada válida y trilingüe para ese capítulo. */
    public static EscenaDeCampana paraCapitulo(Campana c, Campana.Capitulo k) {
        if (c == null || k == null) {
            return escenaGenerica("default:1", "cobalto", "chatarra", false,
                    tr("Escena de campaña", "Kampagnenszene", "Campaign scene"),
                    tr("El combate está a punto de empezar.", "Der Kampf beginnt gleich.", "The fight is about to begin."));
        }
        if ("creed1".equals(c.id)) return escenaCreed(k);
        if ("isla_torneo".equals(c.id)) return escenaIsla(k);
        return escenaDeOtras(c, k);
    }

    // ================================================================ CAMPAÑA 1: CREED AMPLIADA (8 capítulos)

    private static EscenaDeCampana escenaCreed(Campana.Capitulo k) {
        return k.numero <= 4 ? escenaCreedPrimeraMitad(k) : escenaCreedSegundaMitad(k);
    }

    private static EscenaDeCampana escenaCreedPrimeraMitad(Campana.Capitulo k) {
        String id = "creed1:" + k.numero;
        switch (k.numero) {
            case 1:
                return new EscenaDeCampana(id, Momento.INICIO,
                        tr("El sótano de las tuercas", "Der Keller der Muttern", "The Basement of Bolts"),
                        List.of(
                                new Vineta(Decorado.GIMNASIO, Animacion.ENTRADA, "cobalto", k.modelo, true, false, false,
                                        tr("Adonis Cromo cuelga la corbata de oficina y entra en el gimnasio de {entrenador}.",
                                                "Adonis Chrom hängt die Bürokrawatte an den Nagel und betritt das Gym von {entrenador}.",
                                                "Adonis Chrome hangs up his office tie and steps into {entrenador}'s gym."),
                                        tr("¡Vengo a compilar a puñetazos!", "Ich bin hier, um mit Fäusten zu kompilieren!",
                                                "I'm here to compile with my fists!"),
                                        tr("Primero aguanta tres segundos contra {rival}.", "Halt erst mal drei Sekunden gegen {rival} durch.",
                                                "First try lasting three seconds against {rival}.")),
                                new Vineta(Decorado.GIMNASIO, Animacion.SACUDIDA, "cobalto", k.modelo, false, false, true,
                                        tr("El saco de entrenamiento sale volando antes siquiera de sonar la campana.",
                                                "Der Boxsack fliegt davon, noch bevor der Gong ertönt.",
                                                "The punching bag flies off before the bell even rings."),
                                        tr("¡Cuidado con mi {pieza}!", "Vorsicht vor meinem {pieza}!", "Watch out for my {pieza}!"),
                                        tr("¡A ver si compilas bajo presión!", "Mal sehen, ob du unter Druck kompilierst!",
                                                "Let's see if you compile under pressure!"))));
            case 2:
                return new EscenaDeCampana(id, Momento.GIRO,
                        tr("Montaje absurdo en la chatarrería", "Absurde Montage auf dem Schrottplatz", "Absurd Scrapyard Montage"),
                        List.of(
                                new Vineta(Decorado.PLAYA, Animacion.ZOOM, "cobalto", k.modelo, true, false, false,
                                        tr("{entrenador} diseña un entrenamiento imposible: perseguir drones tostadores y cargar lavadoras.",
                                                "{entrenador} entwirft ein unmögliches Training: Toaster-Drohnen jagen und Waschmaschinen stemmen.",
                                                "{entrenador} designs an impossible drill: chasing toaster-drones and lifting washing machines."),
                                        tr("¡Sin pies ligeros no hay título!", "Ohne leichte Füße kein Titel!", "No light feet, no title!"),
                                        tr("¡Tus tuercas suenan a chatarra nueva!", "Deine Muttern klingen nach Frischmetall!",
                                                "Your bolts sound brand new!")),
                                new Vineta(Decorado.RING_NEON, Animacion.DESTELLO, "cobalto", k.modelo, true, false, true,
                                        tr("Las cámaras enfocan la primera pelea televisada contra {rival}.",
                                                "Die Kameras richten sich auf den ersten Fernsehkampf gegen {rival}.",
                                                "The cameras lock onto the first televised bout against {rival}."),
                                        tr("¡Salgo en directo y sin errores!", "Ich bin live und fehlerfrei!", "I'm live and bug-free!"),
                                        tr("¡Te voltearé antes de los anuncios!", "Ich kippe dich noch vor der Werbung!",
                                                "I'll flip you before the commercial break!"))));
            case 3:
                return new EscenaDeCampana(id, Momento.GIRO,
                        tr("Título regional y visita al hospital", "Regionaltitel und Krankenhausbesuch", "Regional Title and Hospital Visit"),
                        List.of(
                                new Vineta(Decorado.HOSPITAL, Animacion.SACUDIDA, "cobalto", k.modelo, true, true, false,
                                        tr("¡Catástrofe! {entrenador} bebió limpiaparabrisas con gas creyendo que era aceite energético.",
                                                "Katastrophe! {entrenador} trank Sprudel-Scheibenreiniger und hielt ihn für Energieöl.",
                                                "Disaster! {entrenador} drank sparkling windshield fluid thinking it was energy oil."),
                                        tr("¡Bip… bip… gana por mí, novato!", "Piep … piep … gewinn für mich, Neuling!",
                                                "Beep… beep… win it for me, rookie!"),
                                        tr("¡Sin entrenador no aguantas un asalto!", "Ohne Trainer hältst du keine Runde durch!",
                                                "Without a coach you won't last a round!")),
                                new Vineta(Decorado.ESTADIO, Animacion.DESTELLO, "cobalto", k.modelo, false, false, true,
                                        tr("El estadio ruge. {rival} entra entre focos mientras Adonis aprieta los puños.",
                                                "Das Stadion tobt. {rival} betritt im Scheinwerferlicht den Ring, während Adonis die Fäuste ballt.",
                                                "The stadium roars. {rival} enters under spotlights as Adonis clenches his fists."),
                                        tr("¡Esta pelea va por {entrenador}!", "Dieser Kampf ist für {entrenador}!",
                                                "This fight is for {entrenador}!"),
                                        tr("¡Muestra tu código, aspirante!", "Zeig deinen Code, Herausforderer!",
                                                "Show me your code, challenger!"))));
            default:
                return new EscenaDeCampana(id, Momento.INICIO,
                        tr("Taller perdido en el desierto", "Verlorene Werkstatt in der Wüste", "Lost Desert Garage"),
                        List.of(
                                new Vineta(Decorado.DESIERTO_SOL, Animacion.ZOOM, "cobalto", k.modelo, true, false, false,
                                        tr("Sin aire acondicionado y a 50 grados: {entrenador} manda golpear neumáticos gigantes.",
                                                "Ohne Klimaanlage bei 50 Grad: {entrenador} lässt dich riesige Reifen bearbeiten.",
                                                "No AC at 120 degrees: {entrenador} orders sledgehammer drills on giant tires."),
                                        tr("¡Si tu código no se derrite aquí, no cae nunca!",
                                                "Wenn dein Code hier nicht schmilzt, fällt er nie!",
                                                "If your code doesn't melt here, it never breaks!"),
                                        tr("¡El desierto solo acepta acero templado!", "Die Wüste duldet nur gehärteten Stahl!",
                                                "The desert only respects tempered steel!"))));
        }
    }

    private static EscenaDeCampana escenaCreedSegundaMitad(Campana.Capitulo k) {
        String id = "creed1:" + k.numero;
        switch (k.numero) {
            case 5:
                return new EscenaDeCampana(id, Momento.GIRO,
                        tr("Exhibición en la cocina gigante", "Schaukampf in der Riesenküche", "Giant Kitchen Exhibition"),
                        List.of(
                                new Vineta(Decorado.COCINA_TORNEO, Animacion.SACUDIDA, "cobalto", k.modelo, true, false, true,
                                        tr("Una exhibición benéfica acaba en caos cuando {rival} enciende las batidoras industriales.",
                                                "Ein Wohltätigkeitskampf endet im Chaos, als {rival} die Industriemixer startet.",
                                                "A charity exhibition turns chaotic when {rival} fires up the industrial blenders."),
                                        tr("¡No pienso acabar en batido de tornillos!", "Ich ende nicht als Schrauben-Smoothie!",
                                                "I'm not ending up as a bolt smoothie!"),
                                        tr("¡Hoy preparo puré de {pieza}!", "Heute mache ich Püree aus dem {pieza}!",
                                                "Tonight I'm blending your {pieza}!"))));
            case 6:
                return new EscenaDeCampana(id, Momento.GIRO,
                        tr("El coloso de hielo y el cubito traidor", "Der Eiskoloss und der tückische Würfel", "The Ice Colossus and the Rogue Cube"),
                        List.of(
                                new Vineta(Decorado.HOSPITAL, Animacion.LLUVIA, "cobalto", k.modelo, true, true, false,
                                        tr("En el vestuario helado, {entrenador} resbala con un cubito decorativo y vuelve a la camilla.",
                                                "In der eisigen Kabine rutscht {entrenador} auf einem Deko-Eiswürfel aus und landet auf der Trage.",
                                                "In the icy locker room, {entrenador} slips on a decorative ice cube and lands on the stretcher."),
                                        tr("¡No dejes que te congele el bucle!", "Lass dir nicht die Schleife einfrieren!",
                                                "Don't let it freeze your loop!"),
                                        tr("Temperatura óptima. Rival obsoleto.", "Optimale Temperatur. Gegner veraltet.",
                                                "Optimal temperature. Opponent obsolete.")),
                                new Vineta(Decorado.VOLCAN_HIELO, Animacion.ZOOM, "cobalto", k.modelo, false, false, true,
                                        tr("{rival} ni parpadea ni siente el frío. Toca demostrar temple de Senior.",
                                                "{rival} blinzelt nicht und spürt keine Kälte. Zeit für echte Senior-Nerven.",
                                                "{rival} never blinks and feels no cold. Time to show real Senior grit."),
                                        tr("¡Mi código arde incluso bajo cero!", "Mein Code glüht selbst unter Null!",
                                                "My code burns even below zero!"),
                                        tr("Calculando tu derrota en 3… 2… 1.", "Berechne deine Niederlage in 3 … 2 … 1.",
                                                "Computing your defeat in 3… 2… 1."))));
            case 7:
                return new EscenaDeCampana(id, Momento.GIRO,
                        tr("Duelo bajo la lluvia en la azotea", "Duell im Regen auf dem Dach", "Rooftop Duel in the Rain"),
                        List.of(
                                new Vineta(Decorado.TORMENTA_AZOTEA, Animacion.LLUVIA, "cobalto", k.modelo, true, false, false,
                                        tr("Bajo relámpagos y aguacero, {rival} confiesa que copió tus antiguos planos del patio.",
                                                "Unter Blitz und Platzregen gesteht {rival}, deine alten Pläne vom Hof kopiert zu haben.",
                                                "Under lightning and pouring rain, {rival} admits copying your old yard blueprints."),
                                        tr("¡Copiar código no te da mi ritmo!", "Code kopieren gibt dir nicht meinen Rhythmus!",
                                                "Copying code won't give you my rhythm!"),
                                        tr("¡Conozco cada giro antes de que lo escribas!", "Ich kenne jede Drehung, bevor du sie schreibst!",
                                                "I know every turn before you write it!"))));
            default:
                return new EscenaDeCampana(id, Momento.FINAL,
                        tr("Último asalto por el cinturón mundial", "Letzte Runde um den Weltmeistergürtel", "Final Round for the World Belt"),
                        List.of(
                                new Vineta(Decorado.HOSPITAL, Animacion.SACUDIDA, "cobalto", k.modelo, true, true, false,
                                        tr("Antes de la final mundial, {entrenador} se pegó la gorra al monitor cardíaco con pegamento industrial.",
                                                "Vor dem Weltfinale klebte sich {entrenador} mit Industriekleber die Mütze an den Herzmonitor.",
                                                "Before the world final, {entrenador} superglued their cap to the heart monitor."),
                                        tr("¡Cuando suene la última campana, recuerda quién eres!",
                                                "Wenn der letzte Gong ertönt, denk daran, wer du bist!",
                                                "When the final bell rings, remember who you are!"),
                                        tr("¡El cinturón mundial no cambiará de dueño!", "Der Weltgürtel wechselt nicht den Besitzer!",
                                                "The world belt isn't changing hands!")),
                                new Vineta(Decorado.MILAGRO, Animacion.CONFETI, "cobalto", k.modelo, true, false, true,
                                        tr("El estadio entero corea tu nombre. Si ganas a {rival}, ¡habrá milagro en el ring!",
                                                "Das ganze Stadion ruft deinen Namen. Besiegst du {rival}, geschieht im Ring ein Wunder!",
                                                "The entire stadium chants your name. Beat {rival} and a ringside miracle awaits!"),
                                        tr("¡A escribir nuestra propia leyenda!", "Schreiben wir unsere eigene Legende!",
                                                "Let's write our own legend!"),
                                        tr("¡Que suene la campana definitiva!", "Lasst den letzten Gong erklingen!",
                                                "Ring the final bell!"))));
        }
    }

    // ================================================================ CAMPAÑA 2: ISLA CLANDESTINA (8 capítulos)

    private static EscenaDeCampana escenaIsla(Campana.Capitulo k) {
        return k.numero <= 4 ? escenaIslaPrimeraMitad(k) : escenaIslaSegundaMitad(k);
    }

    private static EscenaDeCampana escenaIslaPrimeraMitad(Campana.Capitulo k) {
        String id = "isla_torneo:" + k.numero;
        switch (k.numero) {
            case 1:
                return new EscenaDeCampana(id, Momento.INICIO,
                        tr("Desembarco en Isla Cortocircuito", "Landung auf Insel Kurzschluss", "Landing on Short-Circuit Island"),
                        List.of(
                                new Vineta(Decorado.ISLA_PUERTO, Animacion.ENTRADA, "cobalto", k.modelo, true, false, true,
                                        tr("Un transbordador oxidado atraca en la isla secreta del Torneo de los 1000 Voltios.",
                                                "Eine rostige Fähre legt auf der geheimen Insel des 1000-Volt-Turniers an.",
                                                "A rusty ferry docks at the secret island of the 1000-Volt Tournament."),
                                        tr("¡Invitación sellada con cable dorado!", "Einladung mit Goldkabel versiegelt!",
                                                "Invitation sealed with golden wire!"),
                                        tr("¡Nadie desembarca sin pasar por {rival}!", "Niemand geht an Land ohne Kampf gegen {rival}!",
                                                "Nobody lands without getting past {rival}!"))));
            case 2:
                return new EscenaDeCampana(id, Momento.INICIO,
                        tr("La prueba del coco de cobre", "Die Prüfung der Kupferkokosnuss", "The Copper-Coconut Trial"),
                        List.of(
                                new Vineta(Decorado.TEMPLO_ISLA, Animacion.ZOOM, "cobalto", k.modelo, true, false, false,
                                        tr("Un ermitaño excéntrico te venda el radar: debes esquivar cocos de cobre y superar a {rival}.",
                                                "Ein exzentrischer Einsiedler verbindet dein Radar: Weiche Kupferkokosnüssen aus und besiege {rival}.",
                                                "An eccentric hermit blindfolds your radar: dodge copper coconuts and beat {rival}."),
                                        tr("¡Escucho el silbido del coco en el código!", "Ich höre das Pfeifen der Kokosnuss im Code!",
                                                "I can hear the coconut whistling in the code!"),
                                        tr("¡El radar sin calma siempre tropieza!", "Ein unruhiges Radar stolpert immer!",
                                                "A restless radar always trips!"))));
            case 3:
                return new EscenaDeCampana(id, Momento.GIRO,
                        tr("Octavos de final en el foso magnético", "Achtelfinale in der Magnetgrube", "Round of 16 in the Magnet Pit"),
                        List.of(
                                new Vineta(Decorado.DESIERTO_SOL, Animacion.SACUDIDA, "cobalto", k.modelo, true, false, true,
                                        tr("Empiezan las eliminatorias. {rival} activa un imán colosal para quedarse con tus tornillos.",
                                                "Die K.-o.-Runde beginnt. {rival} aktiviert einen Riesenmagneten, um deine Schrauben zu klauen.",
                                                "Knockout rounds begin. {rival} fires up a giant magnet to steal your bolts."),
                                        tr("¡Suelta mi {pieza} o te desconecto!", "Lass meinen {pieza} los oder ich zieh den Stecker!",
                                                "Let go of my {pieza} or I'll unplug you!"),
                                        tr("¡Atractivo magnético garantizado!", "Magnetische Anziehung garantiert!",
                                                "Magnetic attraction guaranteed!"))));
            default:
                return new EscenaDeCampana(id, Momento.GIRO,
                        tr("Cuartos en la cocina volcánica", "Viertelfinale in der Vulkanküche", "Quarterfinal in the Volcanic Kitchen"),
                        List.of(
                                new Vineta(Decorado.HOSPITAL, Animacion.SACUDIDA, "cobalto", k.modelo, true, true, false,
                                        tr("¡Urgencia en el bufé! {entrenador} probó sopa de litio extrapicante y acabó en enfermería.",
                                                "Notfall am Buffet! {entrenador} kostete extrascharfe Lithium-Suppe und landete auf der Krankenstation.",
                                                "Buffet emergency! {entrenador} tasted extra-spicy lithium soup and landed in the infirmary."),
                                        tr("¡Agua… no, aceite frío! ¡Pasa a semifinales!", "Wasser … nein, kaltes Öl! Ab ins Halbfinale!",
                                                "Water… no, cold oil! Reach the semifinals!"),
                                        tr("¡Te freiré en la sartén del torneo!", "Ich brate dich in der Turnierpfanne!",
                                                "I'll sear you on the tournament skillet!")),
                                new Vineta(Decorado.COCINA_TORNEO, Animacion.DESTELLO, "cobalto", k.modelo, false, false, true,
                                        tr("Las sierras de la cocina rugen mientras {rival} blande su sartén supersónica.",
                                                "Die Küchensägen heulen auf, während {rival} die Überschallpfanne schwingt.",
                                                "The kitchen saws roar as {rival} brandishes a supersonic skillet."),
                                        tr("¡Hoy el menú lleva código bien cocinado!", "Heute steht sauber gekochter Code auf dem Menü!",
                                                "Today's special is clean-cooked code!"),
                                        tr("¡A fuego máximo!", "Auf höchster Stufe!", "Turn up the heat!"))));
        }
    }

    private static EscenaDeCampana escenaIslaSegundaMitad(Campana.Capitulo k) {
        String id = "isla_torneo:" + k.numero;
        switch (k.numero) {
            case 5:
                return new EscenaDeCampana(id, Momento.GIRO,
                        tr("El laboratorio de chispas", "Das Funkenlabor", "The Spark Laboratory"),
                        List.of(
                                new Vineta(Decorado.RING_NEON, Animacion.DESTELLO, "cobalto", k.modelo, true, false, true,
                                        tr("En los túneles de la isla, {rival} recita monólogos teatrales mientras lanza arcos eléctricos.",
                                                "In den Inseltunneln hält {rival} dramatische Monologe und schleudert Lichtbögen.",
                                                "In the island tunnels, {rival} delivers dramatic monologues while hurling electric arcs."),
                                        tr("¡Menos discurso y más combate!", "Weniger Rede, mehr Kampf!", "Less monologue, more fighting!"),
                                        tr("¡Contempla mis mil voltios de genialidad!", "Bestaune meine tausend Volt Genialität!",
                                                "Behold my thousand volts of genius!"))));
            case 6:
                return new EscenaDeCampana(id, Momento.GIRO,
                        tr("Semifinal en el volcán helado", "Halbfinale auf dem Eisvulkan", "Semifinal on the Ice Volcano"),
                        List.of(
                                new Vineta(Decorado.VOLCAN_HIELO, Animacion.LLUVIA, "cobalto", k.modelo, true, false, true,
                                        tr("En la cumbre helada del Monte Voltio, {rival} se inclina con exquisita educación antes de embestir.",
                                                "Auf dem eisigen Gipfel des Berges Volt verbeugt sich {rival} höflich vor jedem Rammstoß.",
                                                "On the icy peak of Mount Volt, {rival} bows politely before every charge."),
                                        tr("¡Qué suelo tan resbaladizo!", "Was für ein rutschiger Boden!", "What a slippery floor!"),
                                        tr("Disculpe las molestias; voy a lanzarle muy lejos.",
                                                "Entschuldigen Sie die Umstände; ich werfe Sie gleich sehr weit.",
                                                "Pardon the inconvenience; I am about to launch you very far."))));
            case 7:
                return new EscenaDeCampana(id, Momento.GIRO,
                        tr("El campeón invicto de la isla", "Der ungeschlagene Insel-Champion", "The Unbeaten Island Champion"),
                        List.of(
                                new Vineta(Decorado.HOSPITAL, Animacion.ZOOM, "cobalto", k.modelo, true, true, false,
                                        tr("{entrenador} intentó meditar dentro de una nevera industrial para entender a {rival}… y se congeló.",
                                                "{entrenador} meditierte in einem Industriekühlschrank, um {rival} zu verstehen … und fror ein.",
                                                "{entrenador} tried meditating inside an industrial fridge to understand {rival}… and froze solid."),
                                        tr("¡Brrr… rompe su patrón perfecto!", "Brrr … brich sein perfektes Muster!",
                                                "Brrr… break its flawless pattern!"),
                                        tr("Probabilidad de victoria rival: 0,00 %.", "Siegchance des Gegners: 0,00 %.",
                                                "Opponent win probability: 0.00%.")),
                                new Vineta(Decorado.ESTADIO, Animacion.DESTELLO, "cobalto", k.modelo, false, false, true,
                                        tr("Silencio absoluto en las gradas. {rival} jamás ha cedido una sola ronda en la isla.",
                                                "Totenstille auf den Rängen. {rival} hat auf der Insel noch nie eine Runde abgegeben.",
                                                "Dead silence in the stands. {rival} has never dropped a single round on the island."),
                                        tr("¡Hoy toca estrenar tu casillero de derrotas!", "Heute eröffne ich dein Niederlagenkonto!",
                                                "Today I open your loss column!"),
                                        tr("Iniciando protocolo de victoria impecable.", "Starte Protokoll für makellosen Sieg.",
                                                "Initiating flawless victory protocol."))));
            default:
                return new EscenaDeCampana(id, Momento.FINAL,
                        tr("Final bajo la tormenta: el Enchufe Dorado", "Finale im Sturm: Der Goldene Stecker", "Stormy Final: The Golden Plug"),
                        List.of(
                                new Vineta(Decorado.HOSPITAL, Animacion.SACUDIDA, "cobalto", k.modelo, true, true, false,
                                        tr("{entrenador} se enredó en la pancarta de fuegos artificiales VIP antes de la gran final.",
                                                "{entrenador} verhedderte sich vor dem großen Finale im VIP-Feuerwerksbanner.",
                                                "{entrenador} got tangled in the VIP fireworks banner right before the grand final."),
                                        tr("¡En el último asalto, confía en tu código!", "Vertrau in der letzten Runde auf deinen Code!",
                                                "In the final round, trust your code!"),
                                        tr("¡El Enchufe Dorado es mío para siempre!", "Der Goldene Stecker gehört für immer mir!",
                                                "The Golden Plug is mine forever!")),
                                new Vineta(Decorado.TORMENTA_AZOTEA, Animacion.LLUVIA, "cobalto", k.modelo, false, false, true,
                                        tr("Truenos sobre el faro de la isla. {rival}, el anfitrión secreto, entra al ring bajo la lluvia.",
                                                "Donner über dem Inselleuchtturm. {rival}, der geheime Gastgeber, betritt im Regen den Ring.",
                                                "Thunder crashes over the island lighthouse. {rival}, the secret host, steps into the rain."),
                                        tr("¡Apagaré tu espectáculo con un golpe limpio!", "Ich beende deine Show mit einem sauberen Treffer!",
                                                "I'll shut down your show with one clean strike!"),
                                        tr("¡Que caiga un rayo sobre el mejor robot!", "Möge der Blitz den besten Roboter krönen!",
                                                "May lightning crown the greatest robot!")),
                                new Vineta(Decorado.MILAGRO, Animacion.CONFETI, "cobalto", k.modelo, true, false, true,
                                        tr("Si resistes la tormenta, el Enchufe Dorado y el milagro de {entrenador} serán tuyos.",
                                                "Wenn du dem Sturm trotzt, gehören dir der Goldene Stecker und das Wunder von {entrenador}.",
                                                "Survive the storm and both the Golden Plug and {entrenador}'s miracle are yours."),
                                        tr("¡Al último asalto sin mirar atrás!", "In die letzte Runde ohne Zurückblicken!",
                                                "Into the final round with no looking back!"),
                                        tr("¡A mil voltios!", "Mit tausend Volt!", "At a thousand volts!"))));
        }
    }

    // ================================================================ RESTO DE CAMPAÑAS (generador determinista)

    private static EscenaDeCampana escenaDeOtras(Campana c, Campana.Capitulo k) {
        String id = c.id + ":" + k.numero;
        Decorado dec = decoradoPara(k.escenario, k.esJefe);
        Animacion anim = k.esJefe ? Animacion.DESTELLO : animacionPara(k.escenario);
        if (k.esJefe) {
            return new EscenaDeCampana(id, Momento.FINAL, c.titulo, List.of(
                    new Vineta(Decorado.HOSPITAL, Animacion.SACUDIDA, "cobalto", k.modelo, true, true, false,
                            k.texto,
                            tr("¡Gana este combate por mí!", "Gewinn diesen Kampf für mich!", "Win this fight for me!"),
                            tr("¡Nadie me quita el título!", "Niemand nimmt mir den Titel!", "Nobody takes my title!")),
                    new Vineta(dec, Animacion.CONFETI, "cobalto", k.modelo, false, false, true,
                            tr("Todo está listo en la arena frente a {rival}.", "In der Arena ist alles bereit gegen {rival}.",
                                    "Everything is set in the arena against {rival}."),
                            tr("¡Vamos allá!", "Los geht's!", "Let's go!"),
                            tr("¡En guardia!", "Mach dich bereit!", "On guard!"))));
        }
        return new EscenaDeCampana(id, Momento.INICIO, c.titulo, List.of(
                new Vineta(dec, anim, "cobalto", k.modelo, true, false, k.numero >= c.capitulos.size(),
                        k.texto,
                        tr("¡Hora de demostrar lo entrenado!", "Zeit zu zeigen, was wir geübt haben!",
                                "Time to show what we trained for!"),
                        tr("¡No durarás ni un asalto!", "Du hältst keine Runde durch!",
                                "You won't last a single round!"))));
    }

    private static EscenaDeCampana escenaGenerica(String id, String izq, String der, boolean jefe, Trilingue titulo, Trilingue texto) {
        return new EscenaDeCampana(id, jefe ? Momento.FINAL : Momento.INICIO, titulo, List.of(
                new Vineta(Decorado.RING_NEON, Animacion.ENTRADA, izq, der, true, jefe, true, texto,
                        tr("¡Listos!", "Bereit!", "Ready!"),
                        tr("¡A pelear!", "Auf in den Kampf!", "Let's fight!"))));
    }

    private static Decorado decoradoPara(String escenario, boolean jefe) {
        if (escenario == null) return jefe ? Decorado.ESTADIO : Decorado.GIMNASIO;
        switch (escenario) {
            case "DESIERTO": return Decorado.DESIERTO_SOL;
            case "COCINA": return Decorado.COCINA_TORNEO;
            case "HIELO": return Decorado.VOLCAN_HIELO;
            case "TEJADO": return Decorado.TORMENTA_AZOTEA;
            case "NEON": return jefe ? Decorado.ESTADIO : Decorado.RING_NEON;
            case "LUNA": return Decorado.TEMPLO_ISLA;
            default: return Decorado.GIMNASIO;
        }
    }

    private static Animacion animacionPara(String escenario) {
        if ("TEJADO".equals(escenario)) return Animacion.LLUVIA;
        if ("NEON".equals(escenario)) return Animacion.DESTELLO;
        if ("COCINA".equals(escenario) || "CHATARRERIA".equals(escenario)) return Animacion.SACUDIDA;
        return Animacion.ENTRADA;
    }

    private static Trilingue tr(String es, String de, String en) {
        return new Trilingue(es, de, en);
    }
}
