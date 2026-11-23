# MetajavBots · Equilibrio: peso, carga, locomoción, terrenos y contrarios

Encargo PESO_Y_ESTRATEGIA (23-nov-2026, Opus). Objetivo: que el peso, la carga y el tipo de locomoción de cada robot importen de verdad en la pelea y que ninguna combinación domine: cada elección tiene ventajas y costes claros («¿me llevo el cañón pesado aunque me vuelva lento?», «¿orugas para aguantar empujones o piernas para esquivar?»).

Todos los números están en archivos de datos, no en el código:

- `res/datos/equilibrio.txt`: tramos de peso, giro según la velocidad, mira, retroceso, potencia regulable, choques, armas y los rasgos y el rendimiento por terreno de cada locomoción (lo lee `piezas/Equilibrio`).
- `res/datos/terrenos.txt`: el suelo y las zonas de terreno de cada arena (lo lee `combate/Terrenos`).
- `res/datos/piezas.txt` (`carga=`, `peso=`, `antiaereo=`, `potencia=`, `esquiva=`) y `res/datos/modelos.txt` (`chasis =`).

Código: `piezas/Equilibrio` (reglas y tramos), `piezas/Montaje` (peso, masa, carga, tramo y cifras), `combate/Movimiento` (moverse y chocar, salido de `Pelea`), `combate/ReglasDePeso` (empujón, volteo, esquiva aérea, antiaéreo, retroceso, potencia), `combate/Terrenos` (arenas de pelea y reparto), `combate/Arena` (terreno bajo cada punto), `pantallas/PanelDePeso` (barra y cifras en Taller, pausa entre rondas, Tienda y preparación de la pelea). Pruebas: `PruebasDeEquilibrio` y `PruebasDeEquilibrioTorneo`.

## De dónde salen las reglas

La investigación venía hecha en el encargo; aquí, resumida con sus fuentes.

1. **Robocode** (robocode.sourceforge.io; RoboWiki, «Game Physics»). El robot acelera 1 unidad por turno y frena 2 (frenar es el doble de rápido que acelerar); velocidad máxima 8; el giro máximo del cuerpo es (10 − 0,75·|v|) grados por turno: cuanto más rápido va, peor gira (a toda velocidad, solo 4°); la torreta gira 20°/turno y el radar 45°/turno. Disparos: potencia de 0,1 a 3; velocidad de la bala = 20 − 3·potencia (las fuertes van más lentas y se esquivan mejor); daño = 4·potencia + 2·max(potencia − 1, 0); disparar cuesta energía igual a la potencia y acertar devuelve 3·potencia; cada disparo calienta el cañón 1 + potencia/5 y se enfría 0,1 por turno. Moverse también gasta energía. Lección: cada ventaja lleva su coste en la misma moneda (velocidad, giro, energía, tiempo).
2. **Armored Core VI**. Las piernas fijan el límite de carga; si lo que llevan encima lo supera, el robot no puede salir a combatir. Por debajo del límite, el peso afecta poco a la velocidad. Si el peso de las armas supera lo que aguantan los brazos, empeoran el seguimiento al apuntar y el control del retroceso. Bípedas, equilibradas; articulación invertida, ágiles, saltan alto, poca carga y defensa; cuadrúpedas, buena carga y disparan artillería sin retroceso, poco móviles; orugas, cargan lo más pesado y aguantan mucho, lentas y esquivan mal.
3. **Medabots/Medarot**. Siete tipos de piernas (dos piernas, multipata, ruedas, orugas, flotante, vuelo y acuáticas), cada uno mejor en su terreno y peor en otros; vuelo, rapidísimo y esquivo pero poca armadura y muy vulnerable a las armas antiaéreas; orugas, mucha armadura, no esquivan, sin bonificación ni penalización por terreno; flotante, sin terreno malo ni bueno; multipata, fuerte cuerpo a cuerpo; ruedas, buenas en carretera, potencian las embestidas. Los contrarios explícitos (antiaéreo contra vuelo) castigan lo más esquivo.
4. **BattleTech/MechWarrior** (conocimiento general del juego de mesa). La velocidad sale de la potencia del motor dividida por el tonelaje; las armas generan calor; más armas o blindaje = más toneladas = más lento.
5. **Combates de robots reales** (BattleBots, Robot Wars; guías de Make: y SparkFun; wiki de NHRL). Se compite por clases de peso y ningún tipo es el mejor: piedra-papel-tijera «cuña-giratorio-martillo». La cuña para al giratorio vertical; las cuñitas las arranca un giratorio horizontal; al horizontal lo frenan un vertical o un volteador. Los volteadores son muy eficaces contra robots que no pueden enderezarse; los de mucha altura al suelo son vulnerables a las cuñas; los giratorios tardan en coger velocidad.

## Las reglas aplicadas

### Peso frente a carga (Armored Core, con el giro de Robocode)

- **pesoTotal** = chasis del modelo + todas las piezas montadas menos las piernas (cabeza, brazos con su arma, escudos, espadas, núcleo, naturaleza, pieza propia). **carga** = la de las piernas. **r** = pesoTotal / carga. La **masa** (choques, empujones, retroceso) es pesoTotal + peso de las piernas.

| Tramo | r | Efecto |
|---|---|---|
| Ligero | ≤ 0,70 | Sin castigo; +0,04 de esquiva contra balas y misiles |
| Normal | 0,70–1,00 | Velocidad, aceleración y giro bajan en línea recta hasta −15 % en r = 1 |
| Sobrecarga | 1,00–1,20 | Velocidad y aceleración −40 %, giro −30 %, +40 % de energía al moverse, sin saltos de rampa ni esquivas rápidas (esquiva 0); barra roja en el Taller |
| Imposible | > 1,20 | No sale a pelear: `Juego.ir` lleva al Taller (pestaña Piezas), que dice qué pesa más; entre rondas no se puede montar; un robot guardado así carga sin romperse y se avisa |

- **Giro según la velocidad**: giroEfectivo = giro · (1 − 0,4 · |v| / vMax). El encargo proponía 0,6 (el de Robocode); con 0,6 los robots rápidos de este juego (que giran de 4 a 12°, no 10) se estrellaban sin remedio contra las paredes (el embestidor perdía contra ellas buena parte de su blindaje en cada pelea); con 0,4 se nota y se puede programar (frenar antes de girar). **Frenar es el doble de rápido que acelerar** (ya lo hacía el juego).
- **Moverse gasta energía**: 0,012 por unidad recorrida (×1,4 en sobrecarga).

### Peso de cada arma (brazos de Armored Core, potencia de Robocode)

- **Mira**: gira 14°/turno con un arma de peso 8; cada unidad de peso del arma más pesada la cambia un 3,5 % (topes 55 %–120 %). Un cañón de riel cuántico (17) deja la mira en ~9,6°/turno; un microhaz (4), en ~16°.
- **Retroceso**: las armas de distancia de más de 10 de peso echan atrás al robot 1,1 × (peso − 10) × (45 / masa) unidades y retrasan el siguiente disparo 1,5 × (peso − 10) turnos, **salvo en orugas y patas de araña**, que lo absorben.
- **Potencia regulable** (cañón ligero, bláster de iones, cañón predictor: `potencia=1` en piezas.txt; `regularPotencia(p)` / `leistungRegeln(p)` / `setPower(p)`, de 0,1 a 3): daño × (4p + 2·max(p − 1, 0)) / 4, velocidad × (20 − 3p) / 17, enfriamiento × (10 + 2p) / 12, energía extra 14 × (p − 1) solo por encima de 1 (a potencia 1, la de siempre, las balas siguen sin gastar energía) y acertar devuelve 1 × p de energía. El extra de energía se subió de 4 a 14 tras las simulaciones: con 4, el cañón a potencia 2,5 de cerca decidía casi todas las peleas.

### Masa en los choques (combates de robots reales)

- **Empujón** de un golpe: empuje × clamp(masa del que pega / masa del que recibe, 0,5, 1,8) / estabilidad de sus piernas (× vulnerabilidad a cuñas si el empuje es de 40 o más: cuña, volteador, ariete).
- **Volteo**: turnos × clamp(ratio de masas, 0,6, 1,5) × (1 − antivuelco) × vulnerabilidad a cuñas; menos de 4 turnos no levanta. Los ligeros se recolocan antes.
- **Embestida**: daño = 0,3 + 0,9 × v × (masa / 45) × embestida de las piernas (ruedas 1,6). Al chocar, cada uno se aparta en proporción a la masa × estabilidad del otro.
- **Imán** (aparato y arena): tirón × imán de las piernas × clamp(45 / masa, 0,6, 1,5).

### Voladores, antiaéreo y cuerpo a cuerpo

- Un robot que solo salta, flota con el aparato o ha sido volteado sigue sin recibir golpes cuerpo a cuerpo (como antes). El que **vuela con alas** puede recibirlos, pero esquiva el 45 % (no los de armas de alcance ≥ 120: látigos, bola con cadena), no choca con los de suelo, y a cambio: los **misiles que persiguen** no se le escapan (sin esquiva) y las armas con `antiaereo=X` le hacen ×X (misiles ×1,5; **cañón antiaéreo** nuevo, ×2,2); los imanes tiran de él ×1,7 y el agua lo desorienta.
- **Cuerpo a cuerpo** ×1,6 en las peleas normales (`[armas] cuerpo_a_cuerpo`): hay que llegar recibiendo disparos y las armas de distancia también valen de cerca. Sin este factor, ningún montaje de cuerpo a cuerpo ganaba a ninguno de distancia. No se aplica a las armas largas (látigos, bola), que pegan desde lejos.

### Minijuegos

En la **Carrera de obstáculos** y en **Destrucción y supervivencia** (`Pelea.reglasPropias`) se respetan sus reglas y su daño: de lo anterior solo se aplica el efecto del terreno sobre la locomoción (rendimiento por terreno, agarre de orugas y flotador, el flotador por encima de pinchos y charcos, el agua según las piernas). Las cifras del montaje (velocidad según el tramo de peso) son las del robot en todos los modos. Las trampas mortales de la carrera siguen igual para todos los que no vuelan.

## Valores finales

### Piernas (Mk I)

| Piernas | Locomoción | Velocidad | Giro | Aceleración | Carga | Peso | Blindaje |
|---|---|---:|---:|---:|---:|---:|---:|
| Piernas de paso | BIPEDO | 5 | 7 | 0.6 | 62 | 10 | 40 |
| Orugas | ORUGAS | 3.6 | 4 | 0.35 | 92 | 16 | 60 |
| Ruedas | RUEDAS | 7.5 | 6 | 0.5 | 52 | 8 | 32 |
| Flotador | FLOTANTE | 5.3 | 8 | 0.4 | 46 | 7 | 28 |
| Alas de rotor | VUELO | 5.6 | 8 | 0.45 | 42 | 6 | 22 |
| Patas de araña | PATAS | 5.5 | 12 | 0.7 | 56 | 11 | 38 |
| Piernas de muelle | BIPEDO | 6.2 | 8 | 0.9 | 46 | 9 | 34 |

Cada marca (Mk) suma un 5 % de carga y un 3 % de peso. Piernas de premio: alas de fénix 58, turbina paralela 54, patas de borde 56, muelles de esquiva 52 (y +0,04 de esquiva). Las piernas de muelle tienen +0,05 de esquiva. Las piezas propias del jugador aceptan una carga de 20 a 80 (antes, de 20 a 46; los diseños guardados siguen valiendo).

### Rasgos de cada locomoción (`equilibrio.txt`)

| | Estabilidad | Antivuelco | Tracción | Absorbe retroceso | Esquiva | Esquiva golpes | Imán | Embestida | Cuñas | Pisa el suelo | Agua desorienta |
|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---|---|
| Piernas | 1.0 | 0.2 | 0 | no | 0.02 | 0 | 1.0 | 1.0 | 1.0 | sí | sí |
| Orugas | 2.2 | 0.85 | 0.8 | sí | 0 | 0 | 0.45 | 1.15 | 0.5 | sí | sí |
| Ruedas | 0.85 | 0 | 0 | no | 0.02 | 0 | 1.1 | 1.6 | 1.5 | sí | sí |
| Flotador | 0.7 | 0.3 | 0 | no | 0.01 | 0 | 1.6 | 0.9 | 1.0 | no | no |
| Vuelo | 0.6 | 0 | 0 | no | 0.04 | 0.45 | 1.7 | 0.8 | 1.0 | no | sí |
| Patas de araña | 1.4 | 0.45 | 0.5 | sí | 0.03 | 0 | 0.9 | 0.9 | 0.8 | sí | sí |

Rendimiento (velocidad y aceleración) por terreno:

| | Llano | Aceite | Agua | Rampas | Escombros | Trampas |
|---|---:|---:|---:|---:|---:|---:|
| Piernas | 1.0 | 0.75 | 0.8 | 1.05 | 0.92 | 0.85 |
| Orugas | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 |
| Ruedas | 1.12 | 0.55 | 0.6 | 0.6 | 0.5 | 0.8 |
| Flotador | 1.0 | 1.0 | 1.0 | 0.95 | 0.95 | 1.0 |
| Vuelo | 1.0 | 1.0 | 0.5 | 1.0 | 1.0 | 1.0 |
| Patas de araña | 0.95 | 0.8 | 0.85 | 1.1 | 1.15 | 0.9 |

### Chasis y proporción de los modelos de fábrica (Mk I, sin naturaleza)

| Modelo | Chasis | Peso que cargan las piernas | Piernas (carga) | r | Tramo |
|---|---:|---:|---|---:|---|
| Cobalto | 18 | 45 | Orugas (92) | 0.49 | ligero |
| Chispa | 8 | 33 | Ruedas (52) | 0.63 | ligero |
| Vigía | 10 | 34 | Piernas de paso (62) | 0.55 | ligero |
| Mazo | 16 | 57 | Piernas de paso (62) | 0.92 | normal |
| Grulla | 8 | 35 | Alas de rotor (42) | 0.83 | normal |
| Ábaco | 11 | 42 | Patas de araña (56) | 0.75 | normal |
| Bastión | 22 | 57 | Orugas (92) | 0.62 | ligero |
| Avispa | 7 | 37 | Flotador (46) | 0.80 | normal |
| Tejedora | 10 | 37 | Patas de araña (56) | 0.66 | ligero |
| Filo | 10 | 37 | Piernas de muelle (46) | 0.80 | normal |
| Faro | 10 | 38 | Flotador (46) | 0.83 | normal |
| Píxel | 9 | 32 | Ruedas (52) | 0.62 | ligero |
| Nimbo | 11 | 37 | Flotador (46) | 0.80 | normal |
| Chatarra | 16 | 49 | Orugas (92) | 0.53 | ligero |
| Voltio | 9 | 35 | Piernas de muelle (46) | 0.76 | normal |
| Eclipse | 8 | 40 | Alas de rotor (42) | 0.95 | normal |
| Serrucho | 12 | 49 | Ruedas (52) | 0.94 | normal |
| Galápago | 20 | 60 | Orugas (92) | 0.65 | ligero |
| Volteo | 11 | 43 | Ruedas (52) | 0.83 | normal |
| Bolón | 16 | 61 | Piernas de paso (62) | 0.98 | normal |
| Barrena | 9 | 42 | Patas de araña (56) | 0.75 | normal |
| Remolino | 7 | 43 | Flotador (46) | 0.93 | normal |

Ningún modelo de fábrica va sobrecargado en ninguna marca (lo comprueba la prueba); los de orugas van sobrados: pueden cargar armas pesadas.

### Cambios en las piezas tras las simulaciones

| Pieza | Antes | Ahora | Por qué |
|---|---|---|---|
| Piernas (todas) | carga 26–60 | carga 42–92 | La carga ahora cuenta el chasis y no las piernas; los de fábrica quedan entre 0,49 y 0,98 |
| Ruedas | giro 5 | giro 6 | Con el giro según la velocidad no podían cambiar de rumbo |
| Flotador | velocidad 6, giro 9 | 5,3 y 8 | Sin terreno malo ni trampas, era el más rápido y dominaba |
| Alas de rotor | velocidad 6,5 | 5,6 | Igual: el volador ganaba el 74 % |
| Láser de precisión | daño 15, enfriamiento 70 | daño 12, enfriamiento 80 | El francotirador ganaba el 77 % |
| Lanzamisiles | daño 13, giro 4 | daño 10, giro 3,5 | Los misiles que persiguen apenas fallan |
| Ametralladora | daño 2 | daño 1,7 | El mejor daño sostenido a distancia |
| Látigo eléctrico | daño 8 | daño 6,5 | Largo alcance, aturde y llega a los voladores |
| Cabeza de pulso | aturde 30 | aturde 20 | Un segundo entero aturdido decidía el cuerpo a cuerpo |
| Misiles (lanzamisiles, enjambre, tormenta, colmena) | — | antiaereo=1,5 | Contrario del volador |
| Cañón ligero, bláster de iones, cañón predictor | — | potencia=1 (regulable) | Potencia de Robocode |
| **Cañón antiaéreo** (nuevo, Tienda Noob, 12 monedas) | — | BALA daño 4 ×2 perdigones, antiaereo=2,2, peso 10 | Contrario barato del volador |

**Precios**: no se ha cambiado ninguno; el cañón antiaéreo entra en la curva de Noob (4–12) en su tope, como el resto de armas especializadas de ese rango.

## Terrenos de las arenas

Cada arena de pelea (`Terrenos.arenaDePelea`, que usan `PantallaPelea` y Máquina contra máquina) es la de siempre más su suelo y sus zonas. `Arena.de` no cambia: las demostraciones, el banco de pruebas y las pruebas siguen igual. Antes de pelear, la preparación enseña la próxima arena, su reparto de terrenos y a quién favorece y perjudica, con un botón para cambiarla.

«Brilla» y «sufre»: rendimiento medio de la locomoción en la arena (pesado por lo que ocupa cada terreno) por encima o por debajo de la media de todas las locomociones en esa arena.

| Arena | Reparto | Brillan | Sufren |
|---|---|---|---|
| HANGAR | llano 100 % | ruedas | bipedo, orugas, flotante, vuelo, patas |
| DESIERTO | llano 56 %, rampa 8 %, escombros 37 % | bipedo, orugas, flotante, vuelo, patas | ruedas |
| NEON | llano 63 %, aceite 37 % | orugas, flotante, vuelo | bipedo, ruedas, patas |
| LABORATORIO | llano 64 %, agua 36 % | orugas, ruedas, flotante | bipedo, vuelo, patas |
| LUNA | llano 61 %, rampa 32 %, escombros 7 % | bipedo, orugas, vuelo, patas | ruedas, flotante |
| CHATARRERIA | llano 74 %, escombros 24 %, trampa 3 % | orugas, vuelo, patas | bipedo, ruedas |
| COCINA | llano 70 %, aceite 15 %, trampa 15 % | orugas, ruedas, flotante, vuelo | bipedo, patas |
| HIELO | aceite 97 %, escombros 1 %, trampa 2 % | orugas, flotante, vuelo | bipedo, ruedas, patas |
| TEJADO | llano 69 %, agua 16 %, rampa 14 %, escombros 1 % | bipedo, orugas, flotante | ruedas, vuelo, patas |

Cada locomoción tiene al menos una arena donde brilla y otra donde sufre, y la mejor cambia de una arena a otra (ruedas en el hangar, patas en el desierto y la luna, orugas o flotador en el resto). En las peleas del torneo, en todas las arenas ganan las seis locomociones alguna vez.

## Qué gana a qué y por qué

| Gana | A | Por qué |
|---|---|---|
| Tanque con orugas | Embestidor ligero | Pesa el doble y sus orugas apenas se dejan empujar ni voltear; su martillo aturde al que llega |
| Francotirador ligero | Tanque lento | Mantiene la distancia: el tanque, a 2,9 por turno, no llega |
| Embestidor con ruedas | Francotirador | Es el más rápido en llano y lo alcanza; de cerca, el volteador lo deja en el aire |
| Volador | Tanque, araña, bípedo | Esquiva casi la mitad de los golpes, no choca y pasa por encima de las trampas |
| Escudero | Volador, embestidor | El escudo para los misiles y las embestidas; su cañón castiga al que no esquiva |
| Araña de cuerpo a cuerpo | Escudero | La pinza aturde y gira mejor que nadie: rodea el escudo |
| Especialista en aparatos | Volador, araña | Va a por los aparatos (el imán tira del volador ×1,7) y su látigo llega arriba |
| Cuña | Sierra y ruedas | Empuja y voltea (las ruedas, ×1,5) y saca al giratorio de su alcance |
| Imán, misiles, antiaéreo | Volador | Tirón ×1,7, misiles sin esquiva y ×1,5, antiaéreo ×2,2 |
| Orugas | Cuñas y volteadores | Estabilidad 2,2 y antivuelco 0,85 |
| Patas de araña y orugas | Armas pesadas de retroceso | Absorben el retroceso: disparan sin moverse ni esperar |

## Torneo de arquetipos (PruebasDeEquilibrioTorneo)

Ocho arquetipos en Mk III, con el mismo código de la computadora (`Rival`, nivel 0,6) y aparatos en la arena, todos contra todos en las nueve arenas, con dos semillas fijas y cambiando de lado: 504 peleas.

| Arquetipo | Modelo | Piezas | Estilo |
|---|---|---|---|
| tanque | Bastión | cabeza de escudo, cañón de riel, martillo, orugas, núcleo blindado | TANQUE |
| francotirador | Vigía | ojo de halcón, láser de precisión, cañón ligero, piernas de paso, núcleo batería | FRANCOTIRADOR |
| embestidor | Volteo | cabeza turbo, volteador, cañón ligero, ruedas, núcleo batería | EMBESTIDOR |
| volador | Grulla | cabeza de sombra, lanzamisiles, láser básico, alas, núcleo básico | ACOSADOR |
| araña | Barrena | cabeza de furia, pinza con taladro, taladro, patas de araña, núcleo blindado | EMBESTIDOR |
| bípedo | Mazo | cabeza de radar, cañón ligero, espada de plasma, piernas de paso, núcleo básico | ACOSADOR |
| escudero | Cobalto | cabeza de escudo, escudo de brazo, cañón ligero, piernas de paso, núcleo blindado | TANQUE |
| aparatos | Avispa | cabeza de pulso, látigo, ametralladora, flotador, núcleo batería (va a por los aparatos) | DANZARÍN |

Porcentaje de victorias de la fila contra la columna:

```
                 tanquefrancotirembestido  volador    arana   bipedo escudero aparatos   total
tanque                —      17%      94%      28%      72%      61%      61%      94%   61.1%
francotirador       83%        —      17%      28%      56%      89%      61%      50%   54.8%
embestidor           6%      83%        —      39%      50%      72%      22%      50%   46.0%
volador             72%      72%      61%        —      78%      83%      22%       6%   56.3%
arana               28%      44%      50%      22%        —      33%      83%      17%   39.7%
bipedo              39%      11%      28%      17%      67%        —       6%      39%   29.4%
escudero            39%      39%      78%      78%      17%      94%        —      50%   56.3%
aparatos             6%      50%      50%      94%      83%      61%      50%        —   56.3%
```

Nadie pasa del 65 % (el que más, el tanque, 61 %); cada uno gana a algún estilo (más del 50 %) y pierde con otro; no hay victorias en el primer segundo ni peleas sin final. El bípedo equilibrado es el más flojo (29 %): gana a la araña, pero no destaca en nada; es el precio de no especializarse. Para llegar aquí hicieron falta, además de los cambios de piezas: que la IA del rival frene antes de girar cerca de las paredes, use la habilidad de su arquetipo (embestir, fortificar, pulso de radar), solo suba la potencia si le sobra energía y golpee a los voladores con alas.

## Cómo retocarlo

1. Cambia los números en `res/datos/equilibrio.txt`, `terrenos.txt`, `piezas.txt` o `modelos.txt`.
2. `.\gradlew.bat pruebas:prueba -Psolo=equilibrio` repite el torneo (unos 6 s), imprime la matriz y comprueba los tramos, las locomociones, los terrenos y la API.
3. `TODO-FRAN`: los números finos (chasis, cargas, factores de terreno, cuerpo a cuerpo ×1,6, energía de la potencia) son un primer ajuste por simulación; afínalos jugando.
