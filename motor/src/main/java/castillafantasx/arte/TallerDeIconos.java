package castillafantasx.arte;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TALLER DE ICONOS — Los iconos pequeños de F1 ("f1/<nombre>"), de 12×12,
 * dibujados con las letras de la Paleta: oscuros y de la época, como lo demás.
 * Se fabrican una vez al cargar (Almacen.cargar). La lista de los que hacen
 * falta está en f1.CatalogoF1.iconos(); las pruebas miran que no falte ninguno.
 */
public final class TallerDeIconos {

    private TallerDeIconos() { }

    /** El lado de cada icono, en píxeles. */
    public static final int LADO = 12;

    /** Fabrica todos los iconos de F1 y los deja en el mapa del Almacen. */
    public static void fabricar(Map<String, Imagen> imagenes) {
        for (Map.Entry<String, String[]> e : DIBUJOS.entrySet()) {
            imagenes.put("f1/" + e.getKey(), Imagen.deMatriz(e.getValue()));
        }
    }

    /** Los dibujos, por nombre. '.' es transparente; cada letra, su color de la Paleta. */
    public static final Map<String, String[]> DIBUJOS = new LinkedHashMap<>();

    private static void dibujo(String nombre, String... filas) {
        DIBUJOS.put(nombre, filas);
    }

    static {
        // --- El buscador, las favoritas y los recientes ---
        dibujo("cerrar",
            "............", ".ww......ww.", ".wwv....vww.", "..wwv..vww..", "...wwvvww...", "....wwww....",
            "....wwww....", "...wwvvww...", "..wwv..vww..", ".wwv....vww.", ".ww......ww.", "............");
        dibujo("lupa",
            "............", "...yyyy.....", "..y.ttgy....", ".y.tttt.y...", ".ytgttt.y...", ".yttttt.y...",
            "..y.tt.y....", "...yyyyj....", ".......jj...", "........jj..", ".........jj.", "............");
        dibujo("favorita",
            "............", ".....kk.....", ".....kk.....", "....kzzk....", "kkkkkzzkkkkk", ".kkzzzzzzkk.",
            "..kkzzzzkk..", "...kzzzzk...", "..kzzkkzzk..", "..kzk..kzk..", ".kk......kk.", "............");
        dibujo("recientes",
            "............", "....zzzz....", "..zz....zz..", ".z........z.", ".z...kk...z.", "z....kk....z",
            "z....kkkk..z", "z..........z", ".z........z.", ".zz......z..", "zzz..zzzz...", "............");

        // --- Los temas ---
        dibujo("tema_trucos",
            "............", "..zzzzzzzz..", ".zzzzzzzzzg.", ".zaazzzzaag.", ".zaazzzzaag.", ".zzzzaazzzg.",
            ".zzzzaazzzg.", ".zaazzzzaag.", ".zaazzzzaag.", ".zzzzzzzzzg.", "..gggggggg..", "............");
        dibujo("tema_galerias",
            "............", ".kkkkkkkkkk.", ".kjjjjjjjjk.", ".kjtttttzjk.", ".kjttttttjk.", ".kjtttgttjk.",
            ".kjttgggtjk.", ".kjqgggggjk.", ".kjqqqqqqjk.", ".kjjjjjjjjk.", ".kkkkkkkkkk.", "............");
        dibujo("tema_arena",
            "y..........y", ".yy......yy.", ".yyy....yyy.", "..yyy..yyy..", "...yyyyyy...", "....yyyy....",
            "....yyyy....", "...kyyyyk...", "..kk.yy.kk..", ".jj......jj.", "jj........jj", "............");
        dibujo("tema_depuracion",
            "............", "............", "....zzzz....", "..zz....zz..", ".z...kk...z.", "z...kaak...z",
            "z...kaak...z", ".z...kk...z.", "..zz....zz..", "....zzzz....", "............", "............");
        dibujo("tema_horas",
            ".jjjjjjjjjj.", "..j......j..", "..g.kkkk.g..", "...g.kk.g...", "....g..g....", ".....gk.....",
            ".....kg.....", "....g..g....", "...g.kk.g...", "..g.kkkk.g..", "..j......j..", ".jjjjjjjjjj.");
        dibujo("tema_heroe",
            "............", "....yyyy....", "...yggggy...", "..ygyyyyyy..", "..yyyyyyyy..", "..yaaaaaay..",
            "..yyyayyyy..", "..yyyayyyy..", "..yyyyyyyy..", "...yyyyyy...", "..kk....kk..", "............");
        dibujo("tema_males",
            "............", "...zzzzzz...", "..zzzzzzzz..", ".zzzzzzzzzz.", ".zzaazzaazz.", ".zzaazzaazz.",
            ".zzzzaazzzz.", "..zzzzzzzz..", "...zazazz...", "...zzzzzz...", "............", "............");
        dibujo("tema_lleva",
            "...hhhhhh...", "..h......h..", "..h......h..", ".jjjjjjjjjj.", ".jiiiiiiiij.", ".jiiikkiiij.",
            ".jiiikkiiij.", ".jiiiiiiiij.", ".jiiiiiiiij.", ".jiiiiiiiij.", "..jjjjjjjj..", "............");
        dibujo("tema_dialogos",
            "............", ".wwwwwwwwww.", "wwwwwwwwwwww", "wwwwwwwwwwww", "wwkwwwkwwwkw", "wwwwwwwwwwww",
            ".wwwwwwwwww.", "..ww........", ".ww.........", "ww..........", "............", "............");
        dibujo("tema_fama",
            "............", "..r......r..", ".rq......qr.", ".rq......qr.", "rq........qr", "rq........qr",
            ".rq......qr.", ".rq......qr.", "..rq....qr..", "...rrkkrr...", ".....kk.....", "............");
        dibujo("tema_saltos",
            "............", "...iiiii....", "...iiiii....", "...iiiii....", "...iiiii....", "...iiiii....",
            "...iiiiii...", "...iiiiiiii.", "..iiiiiiiiii", "..iiiiiiiiii", "..hhhhhhhhhh", "............");
        dibujo("tema_eventos",
            "............", ".k.kk.kk.kk.", ".kwkkwkkwkk.", ".kkkkkkkkkk.", ".wwwwwwwwww.", ".wkkkkkkkkw.",
            ".wkkzkkkkkw.", ".wkkzzkkkkw.", ".wkkzkkkkkw.", ".wkkkkkkkkw.", ".wwwwwwwwww.", "............");
        dibujo("eventos",
            "............", "..wwwwwwww..", ".wkkkkkkkkw.", ".wkzzkkzzkw.", ".wkkkkkkkkw.", ".wkzzzzzzkw.",
            ".wkkkkkkkkw.", ".wkzzzzkkkw.", ".wkkkkkkkkw.", "..wwwwwwww..", "............", "............");
        dibujo("tema_otras",
            "............", "...zzzzz....", "..zz...zz...", ".......zz...", "......zz....", ".....zz.....",
            ".....zz.....", "............", ".....zz.....", ".....zz.....", "............", "............");
        DIBUJOS.put("otras", DIBUJOS.get("tema_otras"));

        // --- Trucos ---
        dibujo("invencible",
            ".yyyyyyyyyy.", ".yggggggggy.", ".ygggwwgggy.", ".ygwwwwwwgy.", ".ygggwwgggy.", ".ygggwwgggy.",
            ".ygggwwgggy.", "..yggwwggy..", "..ygggggy...", "...yggggy...", "....yggy....", ".....yy.....");
        dibujo("tregua",
            ".j..........", ".jzzzzzzz...", ".jzzzzzzzz..", ".jzzzzzzzg..", ".jzzzzzzgg..", ".jzzzzzzz...",
            ".j..........", ".j..........", ".j..........", ".j..........", ".j..........", "jjj.........");
        dibujo("quieto",
            "....kkk.....", "...kkkkk....", "....kkk.....", ".kkkkjkkkkk.", ".....j......", "....kkk.....",
            "....kkk.....", "....kkk.....", ".....j......", ".....j......", ".....j......", "...jjjjj....");

        // --- Galerías ---
        dibujo("galeria",
            ".kkkkkkkkkk.", ".kddddddddk.", ".kdddnndddk.", ".kdddnndddk.", ".kddvvvvddk.", ".kdvvvvvvdk.",
            ".kddvvvvddk.", ".kddvddvddk.", ".kddvddvddk.", ".kddddddddk.", ".kkkkkkkkkk.", "............");
        dibujo("retratos",
            "....kkkk....", "..kk....kk..", ".k..mmmm..k.", ".k.mnnnnm.k.", "k..mamman..k", "k..mnnnnm..k",
            "k..mmllmm..k", ".k..mmmm..k.", ".k.vvvvvv.k.", "..kvvvvvvk..", "....kkkk....", "............");
        dibujo("maniquies",
            ".....mm.....", "....mnnm....", ".....mm.....", "..mmmmmmmm..", ".m..mnnm..m.", ".m..mnnm..m.",
            ".k..mmmm..k.", "....m..m....", "....m..m....", "....m..m....", "...kk..kk...", "............");
        // --- El editor de figuras (encargo EDITOR_FIGURAS): esculpir, poses, retratos y seres ---
        dibujo("esculpir",
            ".........jj.", "........jjk.", ".......yyj..", "......yyy...", ".....zyy....", "..mm.zz.....",
            ".mnnm.......", ".mnnnm......", ".mnnnnm.....", "..mnnnm.....", "...mmmm.....", "............");
        dibujo("poses",
            ".....mm...E.", "....mnnm.m..", ".....mm.m...", "..mmmmmm....", ".m..mnn.....", "E...mnn.....",
            "....mmm.....", "....m.m.....", "...m...m....", "..m....m....", ".kk....kk...", "............");
        dibujo("retocar_retratos",
            ".kkkkkkk....", ".k.mmm.k....", ".kmnnnmk..j.", ".kmananmjj..", ".kmnnnjj....", ".k.mmyy.....",
            ".kvvvzvk....", ".kvvvvvk....", ".kkkkkkk....", "............", "............", "............");
        dibujo("seres",
            "..C......C..", ".C..CCCC..C.", ".C.CCCCCC.C.", "..CCDFFDCC..", "..CCFaaFCC..", "..CCDFFDCC..",
            ".C.CCCCCC.C.", "C..CzCCzC..C", "C...CvvC...C", ".C.C....C.C.", "..C......C..", "............");
        dibujo("galeria_armas",
            ".....yy.....", ".....gy.....", ".....gy.....", ".....gy.....", ".....gy.....", ".....gy.....",
            ".....gy.....", "..kkkkkkkk..", ".....jj.....", ".....jj.....", ".....kk.....", "............");

        // --- La arena ---
        dibujo("arena",
            "............", "...ffffff...", ".ffffffffff.", "fgfgfgfgfgff", "fafafafafaff", "fafafafafaff",
            "ffffffffffff", "fgfgfgfgfgff", "fafafafafaff", "fafafafafaff", "ffffffffffff", "kkkkkkkkkkkk");
        dibujo("traer",
            "............", "..kk....kk..", "..kk....kk..", "............", "kk..kkkk..kk", "kk.kkkkkk.kk",
            "...kkkkkk...", "..kkkkkkkk..", "..kkkkkkkk..", "...kkkkkk...", "............", "............");
        dibujo("sin_firmeza",
            ".yyyyayyyyy.", ".yggggagggy.", ".ygggagggy..", ".ygggaggggy.", ".yggggagggy.", ".ygggaggggy.",
            "..ygagggyy..", "..ygagggy...", "...yaggy....", "....aggy....", ".....yy.....", "............");

        // --- Depuración ---
        dibujo("ver_firmeza",
            "............", "....zzzz....", "..zz.kk.zz..", ".z..kaak..z.", "..zz.kk.zz..", "....zzzz....",
            "............", ".aaaaaaaaaa.", ".akkkkkkdda.", ".akkkkkkdda.", ".aaaaaaaaaa.", "............");
        dibujo("ver_choques",
            "w.w.w.w.w.w.", "............", "w...nn.....w", "....nn......", "w..vvvv....w", "...vvvv.....",
            "w..vvvv....w", "...v..v.....", "w..v..v....w", "............", "w.w.w.w.w.w.", "............");
        dibujo("minimapa",
            "............", ".zzzzzzzzzz.", ".zggzzzzzzg.", ".zzzqqzzzzz.", ".zzqqqzttzz.", ".zzzqzzttzz.",
            ".zzzzzttzzz.", ".zwzzzzzzzz.", ".zzzzzzzzgz.", ".zzzzzzzzzz.", "..gggggggg..", "............");

        // --- Las horas ---
        dibujo("hora",
            ".....kk.....", ".k...kk...k.", "..k......k..", "....kkkk....", "...kzzzzk...", "kk.kzzzzk.kk",
            "kk.kzzzzk.kk", "...kzzzzk...", "....kkkk....", "..k......k..", ".k...kk...k.", ".....kk.....");

        // --- el héroe ---
        dibujo("sanar",
            "............", "..ww....ww..", ".wwww..wwww.", "wwzwwwwwwwwv", "wwwwwwwwwwwv", "wwwwwwwwwwwv",
            ".wwwwwwwwwv.", "..wwwwwwwv..", "...wwwwwv...", "....wwwv....", ".....wv.....", "............");
        dibujo("herir",
            ".....v......", ".....v......", "....vvv.....", "....vvv.....", "...vvvvv....", "..vvwvvvv...",
            "..vwwvvvv...", "..vwvvvvv...", "..vvvvvvv...", "...vvvvv....", "....uuu.....", "............");
        dibujo("agotar",
            "............", "............", "............", ".....k......", "....kzk.....", ".....a......",
            "....zzz.....", "....zzz.....", "....zgz.....", "..jjjjjjj...", "...jjjjj....", "............");
        dibujo("tambalear",
            "............", "...kkkkkk...", "..k......k..", ".k..kkkk..k.", ".k.k....k.k.", ".k.k.kk.k.k.",
            ".k.k..k.k.k.", ".k..kk..k.k.", "..k....k..k.", "...kkkk..k..", "........k...", "............");
        dibujo("critico",
            "............", "..vv....vv..", ".vvvv..vvvv.", "vvvvva.vvvvv", "vvvvvva.vvvv", "vvvvva.vvvvv",
            ".vvvvva.vvv.", "..vvva.vvv..", "...vvva.v...", "....vvav....", ".....va.....", "............");
        dibujo("mojar",
            ".....t......", ".....t......", "....ttt.....", "....ttt.....", "...ttttt....", "..ttgtttt...",
            "..tggtttt...", "..tgttttt...", "..ttttttt...", "...ttttt....", "....sss.....", "............");

        // --- Males ---
        dibujo("maldicion",
            "............", "....kkkk....", "..kk....kk..", ".k..BBBB..k.", ".k.BBBBBB.k.", "k.BBBBBBBB.k",
            "k.BBBBBBBB.k", ".k.BBBBBB.k.", ".k..BBBB..k.", "..kk....kk..", "....kkkk....", "............");
        dibujo("enfermar",
            "............", "...DDDDDD...", "..DDrDDDDD..", ".DDDDDDrDDD.", ".DrDDDDDDDD.", "............",
            "....zzzz....", "....zzzz....", "....zzzz....", "....zzzz....", "...gggggg...", "............");
        dibujo("envenenar",
            "....hhhh....", "....yggy....", ".....gg.....", ".....gg.....", "....g..g....", "...g....g...",
            "..gDDDDDDg..", "..gDDrDDDg..", "..gDDDDDDg..", "..gDDDDDDg..", "...gggggg...", "............");
        dibujo("curar_males",
            "............", ".......rr...", "......rrq...", ".....rrqq...", "....rrqq....", "...rrqq.....",
            "...rqq......", "..rqq.......", "..q.........", ".q..........", "q...........", "............");

        // --- Lo que lleva ---
        dibujo("armas",
            "y.........k.", ".y......kkk.", "..y....kyyk.", "...y...kyyk.", "....y..kyyk.", ".....y.kyyk.",
            "....kkkkyk..", ".....jj.kk..", "....j..j....", "...j....j...", "..j......j..", "............");
        dibujo("curas",
            "............", "............", "...jjjjjj...", "..jkkkkkkj..", ".jkzkkkzkkj.", ".jkkkzkkkkj.",
            ".jkkkkkzkkj.", ".jkkkkkkkkj.", "..jjjjjjjj..", "............", "............", "............");
        dibujo("dar_cosas",
            "....gggg....", ".....gg.....", "....gggg....", "...gvvvvg...", "..gvvvvvvg..", "..gvvwvvvg..",
            "..gvvvvvvg..", "..gvvvvvvg..", "...gvvvvg...", "....gggg....", "............", "............");
        dibujo("antorchas",
            ".....k......", "....kwk.....", "....wkw.....", "...wkkkw....", "....www.....", ".....j......",
            ".....j......", ".....j......", ".....j......", ".....j......", ".....h......", "............");
        dibujo("bolsa",
            "............", "....kkkk....", "...kzzzzk...", "..kzkkkkzk..", "..kkzzzzkk..", "...kkkkkk...",
            ".yyyy.......", "yggggy.kkkk.", "yyyyyykzzzzk", ".yyyy.kkkkk.", ".......kkk..", "............");
        dibujo("acompanantes",
            "............", "..nn....mm..", ".nnnn..mmmm.", ".nnnn..mmmm.", "..nn....mm..", "............",
            ".iiii..dddd.", "iiiiiidddddd", "iiiiiidddddd", "iiiiiidddddd", "iiiiiidddddd", "............");

        // --- Diálogos ---
        dibujo("probar_dialogo",
            "............", ".wwwwwwwwww.", "wwwwwkwwwwww", "wwwwwkkwwwww", "wwwwwkkkwwww", "wwwwwkkwwwww",
            ".wwwwkwwwww.", "..ww........", ".ww.........", "ww..........", "............", "............");
        dibujo("saltar_escena",
            "............", ".wwwwwwwwww.", "wwwwwwwkwwww", "wwwwwwwkkwww", "wkkkkkkkkkww", "wwwwwwwkkwww",
            ".wwwwwwkwww.", "..ww........", ".ww.........", "ww..........", "............", "............");
        dibujo("ink",
            "........kk..", ".......kyk..", "......kyk...", ".....kyk....", "....kyk.....", "...kyk......",
            "..kyk.......", "..kk........", ".k..........", "kk..........", "kkkkk.......", "............");

        // --- Fama ---
        dibujo("fama",
            ".....kk.....", "....kkkk....", "...kzzkkk...", "...kzkkkk...", "...kzkkkk...", "..kzkkkkkk..",
            "..kzkkkkkk..", ".kzkkkkkkkk.", ".kkkkkkkkkk.", "............", ".....jj.....", "............");
        dibujo("cambios_fama",
            "............", "..r.....w...", ".rrr....w...", "rrrrr...w...", "..r.....w...", "..r.....w...",
            "..r.....w...", "..r...wwwww.", "..r....www..", "..r.....w...", "............", "............");

        // --- Saltos de sitio ---
        dibujo("orilla",
            "............", "............", "............", "..g...g...g.", ".ttg.ttg.ttg", "tttttttttttt",
            "............", "...g...g...g", "..ttg.ttg.tt", "tttttttttttt", "ssssssssssss", "............");
        dibujo("pajaro",
            "............", "............", "ff........ff", ".ff......ff.", "..fff..fff..", "...ffggfff..",
            "....fggf....", ".....gg.....", ".....ff.....", "............", "............", "............");
        dibujo("bajar",
            "ffff........", "eeef........", "...ffff.....", "...eeef.....", "......ffff..", "......eeef..",
            ".k.......fff", ".k.......eef", ".k..........", "kkk.........", ".k..........", "............");
        dibujo("ir",
            ".....j......", ".kkkkkkkk...", ".kkkkkkkkk..", ".kkkkkkkk...", ".....j......", "...kkkkkkkk.",
            "..kkkkkkkkk.", "...kkkkkkkk.", ".....j......", ".....j......", ".....j......", "...iiiii....");

        // --- Las herramientas del mundo y de la fama (ENCARGO_EDITOR_MUNDO) ---
        dibujo("construir",
            "............", "..yyyyyy....", ".yyyyyyyy...", ".yyyyyyyy...", "..yyjjyy....", "....jj......",
            "....jj......", "....jj..gg..", "....jj.gffg.", "....jj.gffg.", "....jj..gg..", "............");
        dibujo("pincel",
            "..........jj", ".........jkj", "........jkj.", ".......jkj..", "......yyj...", ".....zzy....",
            "....zzz.....", "...vzz......", "..vvv.......", ".vvv........", ".vv...v.....", "............");
        dibujo("dibujar",
            ".........zz.", "........zzg.", ".......zzg..", "......zzg...", ".....zzg....", "....zzg.....",
            "...jzg......", "..jj........", ".aj.........", ".a..aa.aa...", "...a..a..a..", "............");
        dibujo("pegar",
            "............", ".kkkkkkkkkk.", ".kzzzzzzzzk.", ".kzttttwzzk.", ".kzttttttzk.", ".kztqqtttzk.",
            ".kzqqqqqtzk.", ".kzqqqqqqzk.", ".kzzzzzzzzk.", ".kkkkkkkkkk.", ".....kk.....", "............");
        dibujo("duenos",
            "............", "...jjjjjj...", "..jj....jj..", ".jj......jj.", ".j........j.", ".j........j.",
            ".jj......jj.", "..jj....jj..", "...jjkkjj...", "....kzzk....", "....kzzk....", ".....kk.....");
        dibujo("olvido",
            "............", "..jjjjjjjj..", "..gzzzzzzg..", "...gzzzzg...", "....gzzg....", ".....gg.....",
            ".....gg.....", "....g.zg....", "...g.zz.g...", "..gzzzzzzg..", "..jjjjjjjj..", "............");
        dibujo("caso",
            "............", "....nnnn....", "...nmmmmn...", "..nm....mn..", "..nm.nn.mn..", "..nm.nmnmn..",
            "..nm.nmmn...", "...nm.mn....", "....nmn.....", "....nmn.....", ".....n......", "............");
        dibujo("eventos_mapa",
            "............", "...j........", "...jwww.....", "...jwwwww...", "...jwww.....", "...j........",
            ".zzjzzzzzzz.", ".zgjgzzgzzz.", ".zzzzgzzzgz.", ".zgzzzzgzzz.", ".zzzzzzzzzz.", "............");

        // --- El editor (docs/EDITOR.md): las categorías de la rueda, sus herramientas y sus botones; y los temas del panel de la arena ---
        dibujo("cat_mundo",
            "............", ".....rr.....", "....rqqr....", "...rqqqqr...", "....qqqq....", ".....jj.....",
            ".....jj.....", "..rr.jj.rr..", ".rqqrjjrqqr.", "rqqqqqqqqqqr", "qqqqqqqqqqqq", "............");
        dibujo("cat_figuras",
            "............", ".....kk.....", "....kkkk....", ".....kk.....", "...kkkkkk...", "..k.kkkk.k..",
            "..k.kkkk.k..", "....kkkk....", "....k..k....", "....k..k....", "...kk..kk...", "............");
        dibujo("cat_personajes",
            "............", "....mmmm....", "...mnnnnm...", "...nannan...", "...nnnnnn...", "....nllm....",
            ".....nn.....", "..vvvnnvvv..", ".vvvvvvvvvv.", ".vvvvvvvvvv.", ".vvvvvvvvvv.", "............");
        dibujo("cat_luz",
            ".....k......", ".k...k...k..", "..k.....k...", "....kkk.....", "...kzzzk....", "kk.kzzzk.kk.",
            "...kzzzk....", "....kkk.....", "..k.....k...", ".k...k...k..", ".....k......", "............");
        dibujo("cat_historia",
            "............", "............", ".zzzz..zzzz.", "zzggzzzzggzz", "zzzzzzzzzzzz", "zzggzzzzggzz",
            "zzzzzzzzzzzz", "zzggzzzzggzz", "zzzzzzzzzzzz", ".jjjjzzjjjj.", "............", "............");
        dibujo("cat_combate",
            "............", ".wwwwwwwwww.", ".wvvvzzvvvw.", ".wvvvzzvvvw.", ".wzzzzzzzzw.", ".wzzzzzzzzw.",
            ".wvvvzzvvvw.", "..wvvzzvvw..", "..wvvzzvvw..", "...wvzzvw...", "....wwww....", "............");
        dibujo("cat_codigo",
            "....k..k....", "...k..k.....", "....k..k....", "............", ".kkkkkkkk...", ".kzzzzzzkkk.",
            ".kzzzzzzk.k.", ".kzzzzzzkkk.", "..kzzzzk....", "...kkkk.....", ".kkkkkkkk...", "............");
        dibujo("cat_sonido",
            "............", ".....yyyyyy.", ".....yyyyyy.", ".....y....y.", ".....y....y.", ".....y....y.",
            ".....y....y.", "..yyyy.yyyy.", ".yyyyyyyyyy.", ".yyyy..yyyy.", "..yy....yy..", "............");
        dibujo("cat_taller",
            "............", "..yyyyyy....", ".yyyyyyyy...", ".yyyyyyyy...", "..yyjjyy....", "....jj......",
            "....jj......", "....jj......", "....jj......", "....jj......", "....jj......", "............");
        dibujo("codigo",
            "....yyyy....", ".yy.yyyy.yy.", ".yyyyyyyyyy.", "..yykkkkyy..", "yyyk....kyyy", "yyyk.kk.kyyy",
            "yyyk.kk.kyyy", "yyyk....kyyy", "..yykkkkyy..", ".yyyyyyyyyy.", ".yy.yyyy.yy.", "....yyyy....");
        dibujo("visor",
            "............", "..zzzzzzz...", "..zggggzzz..", "..zzzzzzzz..", "..zkkzgggz..", "..zzzzzzzz..",
            "..zgEEEEgz..", "..zzzzzzzz..", "..zkkggzzz..", "..zzzzzzzz..", "..zzzzzzzz..", "............");
        // El buscador del código (EDITOR_CODIGO): una hoja de código con la lupa encima.
        dibujo("buscar_codigo",
            "............", ".zzzzzzz....", ".zggggzz....", ".zzzzzzz....", ".zkkz.kkk...", ".zzz.k...k..",
            ".zgg.k...k..", ".zzz.k...k..", "......kkk...", "........kk..", ".........kk.", "............");
        // La ayuda de una parte: un «?», discreto, en la esquina del visor.
        dibujo("ayuda",
            "............", "....zzzz....", "...zz..zz...", "...z....z...", "........z...", ".......zz...",
            "......zz....", ".....zz.....", ".....zz.....", "............", ".....zz.....", "............");
        dibujo("mando",
            "............", ".zzzzzzzzzz.", ".zeeeeeeeez.", ".zzzzzzzzzz.", ".z........z.", ".z..kkkk..z.",
            ".z....kk..z.", ".z...k.k..z.", ".z..k.....z.", ".z........z.", ".zzzzzzzzzz.", "............");
        dibujo("rueda",
            "............", "....kkkk....", "..kkzzzzkk..", ".kzz....zzk.", ".kz..kk..zk.", "kz..kkkk..zk",
            "kz..kkkk..zk", ".kz..kk..zk.", ".kzz....zzk.", "..kkzzzzkk..", "....kkkk....", "............");
        dibujo("deshacer",
            "............", "............", "...z........", "..zz........", ".zzzzzzz....", "..zz....zz..",
            "...z.....zz.", "..........z.", ".........zz.", ".....zzzzz..", "............", "............");
        // Volver a lo de fábrica (ORDEN_ERGONOMIA): la flecha que da la vuelta hacia atrás.
        dibujo("fabrica",
            "............", "...zzzzz....", "..z.....zz..", "zzzz......z.", ".zz.......z.", "..z.......z.",
            "..........z.", "..........z.", "..z......z..", "...zzzzzz...", "............", "............");
        dibujo("plegar",
            "............", "............", "............", "............", "............", "............",
            "............", "............", "..zzzzzzzz..", "..zzzzzzzz..", "............", "............");
        dibujo("agrandar",
            "............", "............", "..zzzzzzzz..", "..zzzzzzzz..", "..z......z..", "..z......z..",
            "..z......z..", "..z......z..", "..z......z..", "..zzzzzzzz..", "............", "............");
        // --- Encargo ORDEN_ERGONOMIA_2: la lupa de la mesa, la mano, varios a la vez, el menú del clic derecho y la simetría ---
        dibujo("acercar",
            "............", "...yyyy.....", "..y.zz.y....", ".y..zz..y...", ".yzzzzzzy...", ".y..zz..y...",
            "..y.zz.y....", "...yyyyj....", ".......jj...", "........jj..", ".........jj.", "............");
        dibujo("alejar",
            "............", "...yyyy.....", "..y....y....", ".y......y...", ".yzzzzzzy...", ".y......y...",
            "..y....y....", "...yyyyj....", ".......jj...", "........jj..", ".........jj.", "............");
        dibujo("encajar",
            "............", ".zzz....zzz.", ".z........z.", ".z........z.", "............", "............",
            "............", "............", ".z........z.", ".z........z.", ".zzz....zzz.", "............");
        dibujo("mano",
            "............", "....z.z.....", "...zkzkz.z..", "...zkzkzzkz.", ".z.zkzkzzkz.", "zkzzkkkkkkz.",
            "zkkzkkkkkkz.", ".zkkkkkkkkz.", "..zkkkkkkz..", "...zkkkkz...", "....zkkkz...", "............");
        dibujo("varios",
            "............", ".zzzzzz.....", ".z....z.....", ".z..yyyyyy..", ".z..y....y..", ".zzzy....y..",
            "....y....y..", "....yyyyyy..", "............", ".......kk...", "......kkkk..", ".......kk...");
        dibujo("menu",
            "............", ".zzzzzzzzzz.", ".z........z.", ".z.kkkkkk.z.", ".z........z.", ".z.kkkkkk.z.",
            ".z........z.", ".z.kkkk...z.", ".z........z.", ".zzzzzzzzzz.", "............", "............");
        dibujo("simetria",
            "............", ".....yy.....", "..zz.yy.zz..", ".zzz.yy.zzz.", "zzzz.yy.zzzz", ".zzz.yy.zzz.",
            "..zz.yy.zz..", "...z.yy.z...", ".....yy.....", ".....yy.....", ".....yy.....", "............");
        dibujo("arena_rival",
            "............", "....yyyy....", "...yyyyyy...", "...yaayaa...", "...yyyyyy...", "....yyyy....",
            "..vvvyyvvv..", ".vvvvvvvvvv.", ".vvvvvvvvvv.", ".vv.vvvv.vv.", ".yy.vvvv.yy.", "............");
        dibujo("arena_mezcla",
            "............", "..kk....yy..", ".kkkk..yyyy.", "..kk....yy..", ".kkkk..yyyy.", ".kkkk..yyyy.",
            "............", ".....nn.....", "....nnnn....", ".....nn.....", "....nnnn....", "....nnnn....");
        dibujo("arena_porte",
            "............", "..ww....ww..", ".wwww..wwww.", ".wwwwwwwwww.", ".wwwwwwwwww.", "..wwwwwwww..",
            "...wwwwww...", "....wwww....", ".....ww.....", "............", "............", "............");
        dibujo("arena_cifras",
            "............", ".zzzzzzzzzz.", ".z........z.", ".z.k.kk.k.z.", ".z........z.", ".z.kk.k.kkz.",
            ".z........z.", ".z.k.kkk..z.", ".z........z.", ".zzzzzzzzzz.", "............", "............");
        dibujo("rehacer", espejo(DIBUJOS.get("deshacer")));
        dibujo("editor", DIBUJOS.get("rueda"));
        dibujo("tema_editor", DIBUJOS.get("cat_taller"));
    }

    /** El mismo dibujo, visto en un espejo (de izquierda a derecha). */
    private static String[] espejo(String[] filas) {
        String[] otro = new String[filas.length];
        for (int i = 0; i < filas.length; i++) otro[i] = new StringBuilder(filas[i]).reverse().toString();
        return otro;
    }
}
