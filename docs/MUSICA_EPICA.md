# Estudio de música épica MIDI para MetajavBots

Este estudio recoge los recursos musicales que hacen emocionante la música de combate, boxeo, artes marciales, carreras, supervivencia y deporte, aplicados de forma **100 % original** a cada pantalla y modo de MetajavBots. Todo se plantea como recursos de estilo (ritmo, armonía, orquestación y forma): ninguna pieza copia melodías, riffs, progresiones características ni cita títulos de bandas sonoras reales.

---

## 1. Tempo y pulso

El tempo y la subdivisión rítmica definen la sensación física de velocidad, peso o urgencia:

- **Calma activa y concentración (96–108 BPM)**: menús, Tienda y Taller. Un pulso moderado con síncopa suave mantiene la sensación de avance sin fatigar en sesiones largas de programación o compra.
- **Tensión pesada y rival temible (110–124 BPM)**: jefes, duelos decisivos y clímax dramáticos. Un tempo contenido permite que cada golpe de timbal, trombón y coro resuene con peso.
- **Montaje de entrenamiento y minijuegos (124–134 BPM)**: energía deportiva, zancada constante, sensación de progreso y superación.
- **Combate intenso, peleas Senior y destrucción (134–144 BPM)**: pulso implacable, batería agresiva y subdivisión rápida que exige reflejos.
- **Carreras arcade y recta final / cuenta atrás (148–166 BPM)**: tempo alto que empuja hacia delante sin descanso; en los últimos segundos o metros, subir el tempo y doblar la subdivisión dispara la adrenalina.

### Recursos rítmicos clave
- **Ostinato de corcheas o semicorcheas**: un patrón repetido en cuerdas, bajo de sintetizador o guitarra apagada actúa como un «motor» continuo que mantiene la tensión incluso cuando la melodía descansa en notas largas.
- **Percusión marcial y timbales**: redobles crecientes de caja o timbal anuncian la entrada del tema principal o el cambio de sección; los acentos en el tiempo 1 y 3 dan firmeza, mientras que los contratiempos (tiempos 1.5, 2.5, 3.5) generan empuje.
- **Pulso del público («pisotón-pisotón-palmada»)**: dos golpes graves seguidos de una palmada seca en tiempo fuerte evocan un estadio lleno participando en el combate.
- **Cuenta atrás rítmica**: subdivisión seca y mecánica (cencerro, charles cerrado, caja en semicorcheas o golpes cortos de metal) que acelera la percepción del tiempo cuando queda poco margen.

---

## 2. Armonía

- **Progresiones heroicas en menor (`i – VI – III – VII` y `i – iv – VI – V`)**: son la columna vertebral de la épica moderna. Equilibran gravedad, determinación y esperanza; permiten que la melodía salte hacia arriba en el grado VI o VII y resuelva con fuerza en la tónica.
- **Progresiones triunfales en mayor (`I – bVII – IV – I`, `I – V – vi – IV` y `I – IV – V – I`)**: ideales para entrenamientos, carreras luminosas y victorias. El acorde `bVII` (séptimo grado rebajado) aporta carácter épico y deportivo, menos «doméstico» que una cadencia clásica.
- **Paso de menor a mayor (tercera de Picardía o apertura triunfal)**: arrancar en menor durante la lucha y abrir a un acorde mayor luminoso (a menudo tras `bVI – bVII – I`) simboliza la victoria y el alivio tras el esfuerzo.
- **Modulación un tono arriba (+2 semitonos) en la última vuelta**: subir toda la tonalidad en la repetición final produce un «subidón» inmediato sin necesidad de añadir más instrumentos; funciona especialmente bien en montajes de entrenamiento, himnos de campeón y la recta final de una carrera.
- **Pedales graves**: mantener la nota tónica o dominante en el bajo o los timbales mientras los acordes superiores cambian genera tensión acumulada, muy eficaz en jefes, peleas Senior y supervivencia.
- **Escalas modales y pentatónicas**: la pentatónica menor o la escala *in-sen* (semitono inicial, cuarta, quinta) evocan disciplina oriental y artes marciales sin recurrir a clichés; la frigia o menor armónica endurecen el carácter de los jefes.

---

## 3. Orquestación

En MIDI General (con el SoundFont `GeneralUser-GS.sf2` o el sintetizador de Java), la claridad se logra separando registros (grave, medio, agudo) y evitando saturar el mismo rango:

- **Fanfarrias de metales (trompetas GM 56, trombones GM 57, trompas GM 60, sección de metales GM 61)**: llaman a la acción con intervalos abiertos (cuartas, quintas y octavas) y ataques marcados.
- **Cuerdas en ostinato (conjunto de cuerdas GM 48, trémolo GM 44, violonchelo GM 42)**: sostienen la armonía y dan motor continuo; el violonchelo solo aporta dramatismo íntimo en escenas de hospital o derrota.
- **Coro en los clímax (GM 52 / 53)**: reservado para el estribillo o la última vuelta; duplica el tema principal o sostiene acordes abiertos para dar escala de estadio.
- **Guitarra eléctrica y bajo de sintetizador (GM 29/30, GM 33/34, GM 38/39)**: aportan pegada moderna, quintas distorsionadas y tracción mecánica para combates urbanos, duelos Senior y carreras.
- **Instrumentos de identidad por modo**:
  - *Artes marciales / Isla Clandestina*: taiko (GM 116 y percusión), koto (GM 107), flauta de bambú / shakuhachi (GM 77) combinados con bajo eléctrico o de sinte en el torneo.
  - *Carreras de obstáculos*: arpegios de sintetizador (GM 80/81/84), bajo de sinte octavado (GM 38/39), palmas y batería arcade.
  - *Tienda y Taller*: piano eléctrico (GM 4), vibráfono (GM 11), marimba (GM 12), bajo suave (GM 33) y percusión ligera para no fatigar el oído.
  - *Destrucción y jefes*: golpes de orquesta (*Orchestra Hit* GM 55), timbales (GM 47), metales graves y percusión pesada.

---

## 4. Forma y bucles limpios

- **Arquitectura emocional**: incluso en piezas de 25–45 segundos, conviene construir un arco claro: *introducción o arranque directo → tema A (exposición) → puente o sección B (contraste/tensión) → estribillo o vuelta reforzada (clímax) → cierre que enlaza con el compás 1*.
- **Bucles sin corte ni notas colgadas**:
  - Todas las piezas en bucle terminan en un límite exacto de compás (múltiplo de 4 negras) y ninguna nota sobrepasa el final del compás final.
  - La armonía del último compás (dominante o subdominante de empuje) resuelve de forma natural en el primer compás al reiniciarse el secuenciador.
  - Cada `Note On` tiene su `Note Off` correspondiente antes del cierre de pista: cero notas colgadas al repetir o al cambiar de pantalla.
- **Piezas sin bucle (fanfarrias y escenas breves)**: las fanfarrias de victoria, derrota y los golpes dramáticos de escena (milagro, cierre de victoria/derrota) resuelven en un acorde final limpio con caída natural y se reproducen una sola vez (`LOOP = 0`).
- **Transiciones suaves y respeto a la voz**: al cambiar de pantalla o de fase (recta final, últimos segundos, fin de combate) se aplica un fundido corto (~0,25 s) para evitar cortes secos y garantizar que nunca suenen dos músicas a la vez. Mientras habla el locutor o el entrenador (Piper o voz sintetizada), el volumen de la música desciende automáticamente (atenuación suave hasta ~38 %) y se recupera al terminar la frase.

---

## 5. Claves propias de cada género

- **Montaje de entrenamiento (boxeo / deporte)**: empieza con pulso firme y armonía ascendente; cada vuelta suma capas (cuerdas, metales, coro) y sube un tono al final para transmitir esfuerzo y progreso.
- **Marcha triunfal del campeón**: fanfarria de metales en ritmo con puntillo, timbales que marcan el paso y apertura final a modo mayor.
- **Tema del rival temible (jefes y Senior)**: registro grave, pedal amenazante, intervalos tensos (segunda menor, tritono o quinta vacía), golpes de orquesta y sensación de peso inamovible.
- **Persecución y destrucción**: pulso obstinado en menor, acentos sincopados de metales/cuerdas y sensación de peligro constante; en la cuenta atrás, el patrón rítmico se comprime a semicorcheas y sube de registro.
- **Carrera de coches / arcade de carreras**: tempo alto (148–166 BPM), bajo de sintetizador en corcheas octavadas, arpegios luminosos de sintetizador, redobles de toms en las transiciones y modulación ascendente en la recta final.
- **Escenas narrativas cortas**:
  - *Hospital*: pulso pausado que evoca un monitor (nota aguda suave y periódica) sobre violonchelo y piano en menor, con tono entre cómico-dramático y emotivo.
  - *Reaparición milagrosa*: redoble breve, destello de campanas/arpegio ascendente y explosión de metales y coro en mayor.

---

## 6. Tabla de piezas por momento del juego

| Momento del juego | Tema (`temas.txt`) | Piezas MIDI | Bucle | Carácter | Tempo (BPM) | Tonalidad | Instrumentos principales | Recursos aplicados |
|---|---|---|---|---|---:|---|---|---|
| Menú principal y pantallas generales | `menu` | `menu.mid`, `menu_2.mid` | Sí | Acogedor, moderno, con pulso suave | 96 / 100 | Fa mayor / Sol mayor | Piano eléctrico, vibráfono, bajo sinte, pad, batería ligera | Forma A–B, síncopa suave, timbre cálido que no cansa |
| Pelea estándar | `combate` | `combate.mid`, `campeon.mid`, `estadio.mid`, `subidon.mid` | Sí | Heroico, estadio, acción y montaje | 144 / 100 / 82 / 124 | La M / Re M / Mi m / Do M | Sintes de metal, guitarra, coro, trompetas, timbales, batería | Ostinatos, pisotón-palmada, modulación +2 st |
| Pelea estilo rock | `rock` | `rock.mid` | Sí | Enérgico, agresivo | 138 | Mi menor | Guitarra distorsionada, bajo púa, sinte solo, batería | Riff de quintas, solo pentatónico en segunda vuelta |
| Pelea artes marciales | `artes_marciales` | `artes_marciales.mid` | Sí | Marcial, veloz, ochentero | 132 | Re menor | Sinte solo, cuerdas sinte, bajo sinte, taiko, batería | Bajo en semicorcheas, pentatónica doblada |
| Último asalto | `final` | `final.mid`, `ultimo_asalto.mid` | Sí | Clímax decisivo y épico | 100 / 120 | Re mayor / Re menor→mayor | Trompetas, trombones, coro, golpes orquesta, timbales | Tresillos de fanfarria, crescendo en 4 vueltas, apertura a mayor |
| Carrera de obstáculos | `carrera` | `carrera_turbo.mid`, `carrera_neon.mid` | Sí | Arcade de carreras, veloz y vibrante | 148 / 154 | Mi menor / La menor | Lead sinte, arpegio sinte, bajo sinte octavado, metales sinte, batería | Bajo a corcheas octavadas, arpegios en semicorcheas, redobles |
| Carrera: recta final (≥ 72 %) | `carrera_recta` | `carrera_recta_1.mid`, `carrera_recta_2.mid` | Sí | Adrenalina máxima, sprint final | 162 / 166 | Fa# m→Sol# m / Si m→Do# m | Lead doblado, arpegios rápidos, bajo sinte, metales, batería intensa | Modulación +2 st en 2.ª vuelta, semicorcheas constantes |
| Destrucción y supervivencia | `destruccion` | `destruccion_pulso.mid`, `destruccion_asalto.mid` | Sí | Tensión industrial, peligro y choque | 134 / 138 | Do menor / Re menor | Metales graves, guitarra quintas, cuerdas ostinato, golpes orquesta, timbales | Pedal grave, *Orchestra Hits*, batería pesada |
| Destrucción: últimos segundos (≤ 15 s) | `destruccion_final` | `destruccion_cuenta_1.mid`, `destruccion_cuenta_2.mid` | Sí | Cuenta atrás frenética, urgencia | 152 / 158 | Re menor / Mi menor | Sinte pulso, golpes orquesta, timbales, metales, caja/cencerro | Ostinato en semicorcheas, pulso de cuenta atrás, subida de tensión |
| Minijuegos y entrenamiento ligero | `minijuegos` | `minijuegos_agil.mid`, `minijuegos_pista.mid` | Sí | Deportivo, ágil, motivador | 126 / 130 | Sol mayor / Do mayor | Marimba/sinte brillante, metales ligeros, bajo saltarín, palmas, batería | Progresión `I–bVII–IV–I`, melodía rítmica y fresca |
| Jefes (Modo especial y jefes de campaña) | `jefe` | `jefe_coloso.mid`, `jefe_amenaza.mid` | Sí | Oscuro, colosal, intimidante | 118 / 124 | Re menor / Do menor | Trombones, trompas, coro grave, cuerdas trémolo, golpes orquesta, timbales | Pedal de tónica, intervalos tensos (b2/b6), coros dramáticos |
| Peleas Senior (presupuesto, paralelo, rivales Senior) | `senior` | `senior_maquina.mid`, `senior_precision.mid` | Sí | Tenso, técnico, implacable | 136 / 140 | Fa# menor / Mi menor | Bajo sinte rápido, cuerdas ostinato, guitarra, metales oscuros, batería | Pulso mecánico de precisión, armonía menor tensa |
| Campaña estilo Creed (Credo I–III) | `campana_creed` | `campana_creed_1.mid`, `campana_creed_2.mid` | Sí | Legado, superación, épica del ring | 112 / 120 | La menor→Do M / Re menor→Fa M | Piano, cuerdas, trompas, metales, coro, bajo moderno, batería | Crecimiento progresivo de íntimo a estadio, progresión `i–VI–III–VII` |
| Campaña Isla Clandestina | `campana_isla` | `campana_isla_1.mid`, `campana_isla_2.mid` | Sí | Torneo secreto, místico y eléctrico | 128 / 134 | Mi menor / Re menor | Koto, flauta, taiko, guitarra, bajo sinte, metales | Fusión de pentatónica oriental y tracción eléctrica de torneo |
| Escena: llegada al hospital | `escena_hospital` | `escena_hospital_1.mid`, `escena_hospital_2.mid` | Sí | Emotiva y tensa, monitor de hospital | 72 / 76 | Re menor / La menor | Piano, violonchelo, cuerdas suaves, celesta/pitido suave | Pulso espaciado de monitor, arpegio contenido en menor |
| Escena: reaparición milagrosa | `escena_milagro` | `escena_milagro_1.mid`, `escena_milagro_2.mid` | No | Destello triunfal y emoción súbita | 120 / 126 | Re mayor / Do mayor | Trompetas, coro, campanas tubulares, cuerdas, timbales | Redoble breve + arpegio ascendente + acorde heroico final |
| Escena: cierre de victoria | `escena_victoria` | `escena_victoria_1.mid`, `escena_victoria_2.mid` | No | Celebración narrativa cálida y épica | 116 / 122 | Sol mayor / Fa mayor | Trompas, trompetas, cuerdas, coro, timbales | Cadencia `bVI–bVII–I`, melodía ascendente resolutiva |
| Escena: cierre de derrota | `escena_derrota` | `escena_derrota_1.mid`, `escena_derrota_2.mid` | No | Reflexiva, seria, invitación a volver | 74 / 78 | Mi menor / Re menor | Violonchelo, trompa sola, piano, cuerdas suaves | Descenso melódico controlado, acorde final abierto |
| Tienda | `tienda` | `tienda_bazar.mid`, `tienda_escaparate.mid` | Sí | Relajada pero con ritmo, agradable | 102 / 106 | Do mayor / Sol mayor | Piano eléctrico, vibráfono, bajo suave, batería ligera | Groove contenido, melodía discreta que no fatiga |
| Taller (programar y montar) | `taller` | `taller_codigo.mid`, `taller_chispa.mid` | Sí | Concentración fluida, pulso técnico | 98 / 104 | Re menor dórico / La menor dórico | Piano eléctrico, arpegio suave, pad cálido, bajo redondo, batería suave | Armonía dórica limpia, sin estridencias en agudos |
| Fanfarria de victoria | `victoria` | `victoria_gloria.mid`, `victoria_campeon.mid` | No | Triunfo inmediato, breve y rotundo | 124 / 128 | Re mayor / Do mayor | Trompetas, metales, coro, timbales, plato | Paso `bVI–bVII–I`, tresillos de fanfarria, cierre limpio sin bucle |
| Fanfarria de derrota | `derrota` | `derrota_lona.mid`, `derrota_ceniza.mid` | No | Caída digna y breve, sin bucle | 72 / 76 | Re menor / Do menor | Trombones, violonchelo, cuerdas, timbal grave | Descenso por semitonos/grados a tónica grave |
| Entrenamiento clásico (campañas) | `entrenamiento` | `entrenamiento.mid`, `subidon.mid` | Sí | Montaje de esfuerzo y superación | 116 / 124 | Do mayor | Trompetas, cuerdas, bajo, batería | Subida melódica por sextas y modulación |
| Oriental clásico (campañas) | `oriental` | `oriental.mid` | Sí | Calma marcial, dojo tradicional | 76 | Mi in-sen | Koto, flauta de bambú, cuerdas, taiko | Arpegios modales y percusión espaciada |
| Drama clásico (campañas) | `drama` | `drama.mid` | Sí | Emotivo, narrativo | 66 | Re menor | Piano, violonchelo, cuerdas | Arpegios lentos y canto grave del chelo |
