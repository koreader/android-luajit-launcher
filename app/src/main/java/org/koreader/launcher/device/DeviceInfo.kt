/* Device info using Android build properties,
 * based on https://github.com/unwmun/refreshU
 *
 * Note: devices don't need to be declared here unless
 * they have known e-ink update routines, custom light settings  and/or bug workarounds. */

package org.koreader.launcher.device

import android.os.Build
import android.util.Log
import java.util.Locale

@Suppress("detekt:all")
object DeviceInfo {
    private const val TAG = "DeviceInfo"

    private const val STR_KOBO = "rakutenkobo"
    private const val STR_NTX = "ntx_6sl"
    private const val STR_ROCKCHIP = "rockchip"
    private const val STR_TOLINO = "tolino"

    val MANUFACTURER: String
    val BRAND: String
    val MODEL: String
    val DEVICE: String
    val PRODUCT: String
    val HARDWARE: String

    // known quirks
    val QUIRK_BROKEN_LIFECYCLE: Boolean
    val QUIRK_NEEDS_WAKELOCKS: Boolean
    val QUIRK_NO_LIGHTS: Boolean

    val HAS_COLOR_SCREEN: Boolean

    enum class Id {
        NONE,
        BOYUE_C64P,
        BOYUE_K78W,
        BOYUE_K103,
        BOYUE_P6,
        BOYUE_P61,
        BOYUE_P78,
        BOYUE_P101,
        BOYUE_S62,
        BOYUE_T61,
        BOYUE_T62,
        BOYUE_T65S,
        BOYUE_T78D,
        BOYUE_T80D,
        BOYUE_T80S,
        BOYUE_T103D,
        CREMA,
        CREMA_0650L,
        CREMA_0710C,
        CREMA_CARTA_G,
        ENERGY,
        FIDIBOOK,
        HANVON_960,
        HYREAD_MINI6,
        INKBOOK,
        INKBOOKFOCUS,
        INKBOOKFOCUS_PLUS,
        INKPALM_PLUS,
        JDREAD,
        LINFINY_ENOTE,
        MEEBOOK_M6,
        MEEBOOK_M6C,
        MEEBOOK_M7,
        MEEBOOK_P6,
        MOAAN_MIX7,
        MOOINKPLUS2C,
        NABUK,
        NOOK,
        NOOK_GL4,
        OBOOK_P78D,
        ONYX_C67,
        ONYX_DARWIN7,
        ONYX_DARWIN9,
        ONYX_EDISON,
        ONYX_FAUST3,
        ONYX_GALILEO2,
        ONYX_GO_103,
        ONYX_GO_COLOR7,
        ONYX_JDREAD,
        ONYX_KON_TIKI2,
        ONYX_LEAF,
        ONYX_LEAF2,
        ONYX_LIVINGSTONE3,
        ONYX_LOMONOSOV,
        ONYX_MAGICBOOK,
        ONYX_MAX,
        ONYX_MONTECRISTO3,
        ONYX_NOTE,
        ONYX_NOTE3,
        ONYX_NOTE4,
        ONYX_NOTE5,
        ONYX_NOTE_AIR,
        ONYX_NOTE_AIR2,
        ONYX_NOTE_AIR_3C,
        ONYX_NOTE_MAX,
        ONYX_NOTE_PRO,
        ONYX_NOTE_X2,
        ONYX_NOVA,
        ONYX_NOVA2,
        ONYX_NOVA3,
        ONYX_NOVA3_COLOR,
        ONYX_NOVA_AIR,
        ONYX_NOVA_AIR_2,
        ONYX_NOVA_AIR_C,
        ONYX_NOVA_PRO,
        ONYX_PAGE,
        ONYX_PALMA,
        ONYX_PALMA2,
        ONYX_POKE2,
        ONYX_POKE3,
        ONYX_POKE4,
        ONYX_POKE5,
        ONYX_POKE4LITE,
        ONYX_POKE_PRO,
        ONYX_TAB_ULTRA,
        ONYX_TAB_ULTRA_C,
        ONYX_TAB_ULTRA_C_PRO,
        PUBU_PUBOOK,
        RIDI_PAPER_3,
        SONY_CP1,
        SONY_RP1,
        STORYTEL_READER2,
        TAGUS_GEA,
        TOLINO,
        TOLINO_EPOS1,
        TOLINO_EPOS2,
        TOLINO_EPOS3,
        TOLINO_PAGE2,
        TOLINO_SHINE3,
        TOLINO_VISION4,
        TOLINO_VISION5,
        TOLINO_VISION6,
        XIAOMI_READER,
    }

    internal val ID: Id

    init {
        MANUFACTURER = lowerCase(getBuildField("MANUFACTURER"))
        BRAND = lowerCase(getBuildField("BRAND"))
        MODEL = lowerCase(getBuildField("MODEL"))
        DEVICE = lowerCase(getBuildField("DEVICE"))
        PRODUCT = lowerCase(getBuildField("PRODUCT"))
        HARDWARE = lowerCase(getBuildField("HARDWARE"))

        Log.i(TAG, String.format(Locale.US,
            """
            MANUFACTURER: %s
            BRAND       : %s
            MODEL       : %s
            DEVICE      : %s
            PRODUCT     : %s
            HARDWARE    : %s
            """.trimIndent(),
            MANUFACTURER,
            BRAND,
            MODEL,
            DEVICE,
            PRODUCT,
            HARDWARE,
        ))

        val BOYUE = MANUFACTURER == "boeye" || MANUFACTURER == "boyue"
        val CREMA = BRAND == "crema"

        ID = when {

            // Boyue C64P (Boyue P6 Clone)
            BRAND == "c64p" && PRODUCT == "c64p"
            -> Id.BOYUE_C64P

            // Boyue Likebook Ares
            BOYUE && (PRODUCT == "k78w" || PRODUCT == "ares")
            -> Id.BOYUE_K78W

            // Boyue Likebook Alita
            BOYUE && (PRODUCT == "k103" || PRODUCT == "alita")
            -> Id.BOYUE_K103

            // Boyue Likebook P6
            BOYUE && PRODUCT == "p6"
            -> Id.BOYUE_P6

            // Boyue Lemon
            BOYUE && PRODUCT == "p61-k12-l"
            -> Id.BOYUE_P61

            // Boyue Likebook P78
            BOYUE && PRODUCT == "p78"
            -> Id.BOYUE_P78

            // Boyue Likebook P101
            BOYUE && PRODUCT == "p101"
            -> Id.BOYUE_P101

            // Boyue Likebook LemonRead S62A
            BOYUE && PRODUCT == "s62"
            -> Id.BOYUE_S62

            // Boyue T61
            BOYUE
            && (PRODUCT.startsWith("t61") || MODEL == "rk30sdk")
            && DEVICE.startsWith("t61")
            -> Id.BOYUE_T61

            // Boyue T62
            BOYUE
            && PRODUCT.startsWith("t62") || MODEL == "rk30sdk"
            && DEVICE.startsWith("t62")
            -> Id.BOYUE_T62

            // Boyue/JDRead T65S
            BOYUE && PRODUCT == "t65s"
            -> Id.BOYUE_T65S

            // Boyue Likebook Muses
            BOYUE && (PRODUCT == "t78d" || PRODUCT == "muses")
            -> Id.BOYUE_T78D

            // Boyue Likebook Mars
            BOYUE && (PRODUCT == "t80d" || PRODUCT == "mars")
            -> Id.BOYUE_T80D

            // Boyue Likebook Plus
            BOYUE && PRODUCT == "t80s"
            -> Id.BOYUE_T80S

            // Boyue Likebook Mimas
            BOYUE && (PRODUCT == "t103d" || PRODUCT == "mimas")
            -> Id.BOYUE_T103D

            // Crema Note (1010P)
            CREMA && PRODUCT == "note"
            -> Id.CREMA

            // Crema Carta+
            CREMA && PRODUCT == "keplerb"
            -> Id.CREMA_0650L

            // Crema Grande
            CREMA && MODEL == "crema-0710c"
            -> Id.CREMA_0710C

            // Crema Carta G
            CREMA && MODEL == "crema-0670c"
            -> Id.CREMA_CARTA_G

            // Energy Sistem eReaders. Tested on Energy Ereader Pro 4
            (BRAND == "energysistem" || BRAND == "energy_sistem") && MODEL.startsWith("ereader")
            -> Id.ENERGY

            // Fidibook
            MANUFACTURER == "fidibo" && MODEL == "fidibook"
            -> Id.FIDIBOOK

            // Hanvon 960
            BRAND == "freescale" && PRODUCT == "evk_6sl_eink"
            -> Id.HANVON_960

            // Hyread Mini 6
            MANUFACTURER == "hyread" && MODEL == "k06nu"
            -> Id.HYREAD_MINI6

            // Artatech Inkbook Prime/Prime HD.
            MANUFACTURER == "artatech" && BRAND == "inkbook" && MODEL.startsWith("prime")
            -> Id.INKBOOK

            // InkBook Focus
            DEVICE == "px30_eink" && MODEL == "focus"
            -> Id.INKBOOKFOCUS

            // InkBook Focus Plus
            DEVICE == "rk3566_eink" && MODEL == "focus plus"
            -> Id.INKBOOKFOCUS_PLUS

            // InkPalm Plus
            MANUFACTURER == STR_ROCKCHIP && MODEL == "inkpalmplus"
            -> Id.INKPALM_PLUS

            // JDRead1
            MANUFACTURER == "onyx" && MODEL == "jdread"
            -> Id.JDREAD

            // Linfiny A4 (13.3") eNote / Avalue ENT-13T1 / QuirkLogic Papyr
            MANUFACTURER == "linfiny" && MODEL == "ent-13t1"
            -> Id.LINFINY_ENOTE

            // Meebook M6
            MANUFACTURER == "haoqing" && MODEL == "m6"
            -> Id.MEEBOOK_M6

            // Meebook M6C
            MANUFACTURER == "haoqing" && MODEL == "m6c"
            -> Id.MEEBOOK_M6C

            // Meebook M7
            MANUFACTURER == "haoqing" && MODEL == "m7"
            -> Id.MEEBOOK_M7

            // Meebook P6
            MANUFACTURER == "haoqing" && MODEL == "p6"
            -> Id.MEEBOOK_P6

            // Moaan Mix7
            MANUFACTURER == STR_ROCKCHIP && MODEL == "moaanmix7"
            -> Id.MOAAN_MIX7

            // Mooink Plus 2c
            BRAND == "allwinner" && MODEL == "mooink plus 2c"
            -> Id.MOOINKPLUS2C

            // Nabuk Regal HD
            MANUFACTURER == "onyx" && MODEL == "nabukreg_hd"
            -> Id.NABUK

            // Nook Glowlight 4 (4/4e/4plus)
            (MANUFACTURER == "barnesandnoble")
            && (MODEL == "bnrv1000" || MODEL == "bnrv1100" || MODEL == "bnrv1300")
            -> Id.NOOK_GL4

            // Nook (catch them all fallback for all other models)
            (MANUFACTURER == "barnesandnoble" || MANUFACTURER == "freescale")
            && (MODEL == "bnrv510" || MODEL == "bnrv520" || MODEL == "bnrv700"
                || MODEL == "evk_mx6sl" || MODEL.startsWith("ereader"))
            -> Id.NOOK

            // OBOOK P78D
            MANUFACTURER == STR_ROCKCHIP && PRODUCT == "rk3566_78d" && MODEL == "p78d"
            -> Id.OBOOK_P78D

            // Onyx C67
            MANUFACTURER == "onyx"
            && (PRODUCT.startsWith("c67") || MODEL == "rk30sdk")
            && DEVICE.startsWith("c67")
            -> Id.ONYX_C67

            // ONYX DARWIN 7
            MANUFACTURER == "onyx"
            && (PRODUCT == "mc_darwin7" || PRODUCT == "darwin7")
            && (DEVICE == "mc_darwin7" || DEVICE == "darwin7")
            -> Id.ONYX_DARWIN7

            // ONYX DARWIN 9
            MANUFACTURER == "onyx"
            && (PRODUCT == "mc_darwin9" || PRODUCT == "darwin9")
            && (DEVICE == "mc_darwin9" || DEVICE == "darwin9")
            -> Id.ONYX_DARWIN9

            // Onyx Edison
            MANUFACTURER == "onyx" && PRODUCT == "edison" && DEVICE == "edison"
            -> Id.ONYX_EDISON

            // Onyx Faust 3
            MANUFACTURER == "onyx" && PRODUCT == "mc_faust3" && DEVICE == "mc_faust3"
            -> Id.ONYX_FAUST3

            // Onyx Boox Galileo 2
            BRAND == "onyx" && MODEL == "galileo2"
            -> Id.ONYX_GALILEO2

            // Onyx Boox Go 10.3
            BRAND == "onyx" && MODEL == "go103"
            -> Id.ONYX_GO_103

            // Onyx Boox Go Color 7
            BRAND == "onyx" && MODEL == "gocolor7"
            -> Id.ONYX_GO_COLOR7

            // Onyx JDRead
            BRAND == "onyx" && MODEL == "jdread"
            -> Id.ONYX_JDREAD

            // Onyx Kon-Tiki 2
            MANUFACTURER == "onyx" && PRODUCT == "kon_tiki2" && DEVICE == "kon_tiki2"
            -> Id.ONYX_KON_TIKI2

            // Onyx Leaf
            MANUFACTURER == "onyx" && PRODUCT == "leaf" && DEVICE == "leaf"
            -> Id.ONYX_LEAF

            // Onyx Leaf 2 && Onyx Leaf 2 Plus
            MANUFACTURER == "onyx"
            && (PRODUCT == "leaf2" || PRODUCT == "leaf2_p")
            && (DEVICE == "leaf2" || DEVICE == "leaf2_p")
            -> Id.ONYX_LEAF2

            // Onyx Boox Livingstone3
            MANUFACTURER == "onyx" && DEVICE == "livingstone3"
            -> Id.ONYX_LIVINGSTONE3

            // Onyx Lomonosov
            MANUFACTURER == "onyx" && DEVICE == "lomonosov"
            -> Id.ONYX_LOMONOSOV

            // Onyx MagicBook
            MANUFACTURER == "onyx" && BRAND == "magicbook"
            -> Id.ONYX_MAGICBOOK

            // Onyx Max
            MANUFACTURER == "onyx" && PRODUCT == "max" && DEVICE == "max"
            -> Id.ONYX_MAX

            // Onyx Montecristo 3
            MANUFACTURER == "onyx" && PRODUCT == "mc_kepler_c" && DEVICE == "mc_kepler_c"
            -> Id.ONYX_MONTECRISTO3

            // Onyx Note
            MANUFACTURER == "onyx" && PRODUCT == "note" && DEVICE == "note"
            -> Id.ONYX_NOTE

            // Onyx Note 3
            MANUFACTURER == "onyx" && PRODUCT == "note3" && DEVICE == "note3"
            -> Id.ONYX_NOTE3

            // Onyx Note 4
            MANUFACTURER == "onyx" && MODEL == "mc_note4"
            -> Id.ONYX_NOTE4

            // Onyx Note 5
            BRAND == "onyx" && PRODUCT == "note5" && DEVICE == "note5"
            -> Id.ONYX_NOTE5

            // Onyx Note Air
            MANUFACTURER == "onyx" && PRODUCT == "noteair" && DEVICE == "noteair"
            -> Id.ONYX_NOTE_AIR

            // Onyx Note Air 2 && Note Air 2 Plus
            BRAND == "onyx" && (MODEL == "noteair2" || MODEL == "noteair2p")
            -> Id.ONYX_NOTE_AIR2

            // Onyx Note Air 3C
            BRAND == "onyx" && MODEL == "noteair3c"
            -> Id.ONYX_NOTE_AIR_3C

            // Onyx Boox Note Max
            BRAND == "onyx" && PRODUCT == "notemax" && DEVICE == "notemax"
            -> Id.ONYX_NOTE_MAX

            // Onyx Note Pro
            MANUFACTURER == "onyx" && PRODUCT == "notepro" && DEVICE == "notepro"
            -> Id.ONYX_NOTE_PRO

            // Onyx Note X2
            MANUFACTURER == "onyx" && MODEL == "notex2"
            -> Id.ONYX_NOTE_X2

            // Onyx Nova
            MANUFACTURER == "onyx" && PRODUCT == "nova" && DEVICE == "nova"
            -> Id.ONYX_NOVA

            // Onyx Nova 2
            MANUFACTURER == "onyx" && PRODUCT == "nova2" && DEVICE == "nova2"
            -> Id.ONYX_NOVA2

            // Onyx Nova 3
            MANUFACTURER == "onyx" && PRODUCT == "nova3" && DEVICE == "nova3"
            -> Id.ONYX_NOVA3

            // Onyx Nova 3 Color
            MANUFACTURER == "onyx" && MODEL == "nova3color"
            -> Id.ONYX_NOVA3_COLOR

            // Onyx Nova Air
            MANUFACTURER == "onyx" && MODEL == "novaair"
            -> Id.ONYX_NOVA_AIR

            // Onyx Nova Air 2
            MANUFACTURER == "onyx" && MODEL == "novaair2"
            -> Id.ONYX_NOVA_AIR_2

            // Onyx Nova Air C
            BRAND == "onyx" && MODEL == "novaairc"
            -> Id.ONYX_NOVA_AIR_C

            // Onyx Nova Pro
            BRAND == "onyx" && MODEL == "novapro"
            -> Id.ONYX_NOVA_PRO

            // Onyx Page
            BRAND == "onyx" && MODEL == "page"
            -> Id.ONYX_PAGE

            // Onyx Palma
            BRAND == "onyx" && MODEL == "palma"
            -> Id.ONYX_PALMA

            // Onyx Palma2
            BRAND == "onyx" && MODEL == "palma2"
            -> Id.ONYX_PALMA2

            // Onyx Poke 2
            MANUFACTURER == "onyx" && PRODUCT == "poke2"
            -> Id.ONYX_POKE2

            // Onyx Poke 3
            MANUFACTURER == "onyx" && PRODUCT == "poke3" && DEVICE == "poke3"
            -> Id.ONYX_POKE3

            // Onyx Poke 4
            BRAND == "onyx" && MODEL == "poke4"
            -> Id.ONYX_POKE4

            // Onyx Poke 4 lite
            BRAND == "onyx" && MODEL == "poke4lite"
            -> Id.ONYX_POKE4LITE

            // Onyx Poke 5
            BRAND == "onyx" && MODEL == "poke5p"
            -> Id.ONYX_POKE5

            // Onyx Poke Pro
            MANUFACTURER == "onyx" && PRODUCT == "poke_pro"
            -> Id.ONYX_POKE_PRO

            // Onyx Tab Ultra
            MANUFACTURER == "onyx" && MODEL == "tabultra"
            -> Id.ONYX_TAB_ULTRA

            // Onyx Tab Ultra C
            MANUFACTURER == "onyx" && MODEL == "tabultrac"
            -> Id.ONYX_TAB_ULTRA_C

            // Onyx Tab Ultra C Pro
            BRAND == "onyx" && PRODUCT == "tabultracpro"
            -> Id.ONYX_TAB_ULTRA_C_PRO

            // Pubu Pubook
            MANUFACTURER == STR_ROCKCHIP && BRAND == STR_ROCKCHIP && MODEL == "pubook" && DEVICE == "pubook" && HARDWARE == "rk30board"
            -> Id.PUBU_PUBOOK

            // Ridi Paper 3
            BRAND == "ridi" && MODEL == "ridipaper" && PRODUCT == "rp1"
            -> Id.RIDI_PAPER_3

            // Sony DPT-CP1
            MANUFACTURER == "sony" && MODEL == "dpt-cp1"
            -> Id.SONY_CP1

            // Sony DPT-RP1
            MANUFACTURER == "sony" && MODEL == "dpt-rp1"
            -> Id.SONY_RP1

            // Storytel Reader 2
            MANUFACTURER == "storytel" && MODEL == "reader 2"
            -> Id.STORYTEL_READER2

            // Tagus Gea
            MANUFACTURER == "onyx" && MODEL == "tagus_pokep"
            -> Id.TAGUS_GEA

            // Tolino Epos 1
            BRAND == STR_KOBO && MODEL == STR_TOLINO && DEVICE == STR_NTX && HARDWARE == "e70q20"
            -> Id.TOLINO_EPOS1

            // Tolino Epos 2
            BRAND == STR_KOBO && MODEL == STR_TOLINO && DEVICE == STR_NTX && HARDWARE == "e80k00"
            -> Id.TOLINO_EPOS2

            // Tolino Epos 3
            BRAND == STR_KOBO && MODEL == "tolino epos 3"
            -> Id.TOLINO_EPOS3

            // Tolino Page 2 has no warmth lights
            BRAND == STR_KOBO && MODEL == STR_TOLINO && DEVICE == STR_NTX && HARDWARE == "e60qv0"
            -> Id.TOLINO_PAGE2

            // Tolino Shine 3 also has warmth lights, but with ntx_io file
            BRAND == STR_KOBO && MODEL == STR_TOLINO && DEVICE == STR_NTX && HARDWARE == "e60k00"
            -> Id.TOLINO_SHINE3

            // Tolino Vision 4 also has warmth lights, but with ntx_io file
            BRAND == STR_KOBO && MODEL == STR_TOLINO && DEVICE == STR_NTX && HARDWARE == "e60q50"
            -> Id.TOLINO_VISION4

            // Tolino Vision 5 also has warmth lights, but with ntx_io file
            BRAND == STR_KOBO && MODEL == STR_TOLINO && DEVICE == STR_NTX && HARDWARE == "e70k00"
            -> Id.TOLINO_VISION5

            // Tolino Vision 6
            BRAND == STR_KOBO && MODEL == "tolino vision 6" && DEVICE == STR_TOLINO && HARDWARE == "sun8iw15p1"
            -> Id.TOLINO_VISION6

            // Tolino (catch them all fallback for all other models)
            BRAND == STR_TOLINO && MODEL == "imx50_rdp"
            || MODEL == STR_TOLINO && (DEVICE == "tolino_vision2" || DEVICE == STR_NTX)
            -> Id.TOLINO

            // Xiaomi
            MANUFACTURER == "xiaomi" && BRAND == "xiaomi" && MODEL == "xiaomi_reader" && DEVICE == "rk3566_eink" && HARDWARE == "rk30board"
            -> Id.XIAOMI_READER

            // ???
            else -> Id.NONE
        }

        // has broken lifecycle
        QUIRK_BROKEN_LIFECYCLE = when (ID) {
            Id.ONYX_POKE2,
            -> true else -> false
        }

        // need wakelocks
        QUIRK_NEEDS_WAKELOCKS = when (ID) {
            Id.SONY_RP1,
            -> true else -> false
        }

        // Android devices without lights
        QUIRK_NO_LIGHTS = when (ID) {
            Id.LINFINY_ENOTE,
            Id.ONYX_MAX,
            Id.ONYX_NOTE,
            Id.SONY_CP1,
            Id.SONY_RP1,
            -> true else -> false
        }

        HAS_COLOR_SCREEN = when (ID) {
            Id.MEEBOOK_M6C,
            Id.MOOINKPLUS2C,
            Id.NONE,
            Id.ONYX_GO_COLOR7,
            Id.ONYX_NOVA3_COLOR,
            Id.ONYX_NOVA_AIR_C,
            Id.ONYX_NOTE_AIR_3C,
            Id.ONYX_TAB_ULTRA_C,
            Id.ONYX_TAB_ULTRA_C_PRO,
            -> true else -> false
        }
    }

    private fun getBuildField(fieldName: String): String {
        return try {
            Build::class.java.getField(fieldName).get(null) as? String ?: ""
        } catch (e: Exception) {
             ""
        }
    }

    private fun lowerCase(text: String): String {
        return text.lowercase(Locale.US)
    }
}
