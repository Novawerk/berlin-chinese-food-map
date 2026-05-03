package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.district_desc_britz
import berlinfoodmap.composeapp.generated.resources.district_desc_charlottenburg
import berlinfoodmap.composeapp.generated.resources.district_desc_dahlem
import berlinfoodmap.composeapp.generated.resources.district_desc_franzoesisch_buchholz
import berlinfoodmap.composeapp.generated.resources.district_desc_friedenau
import berlinfoodmap.composeapp.generated.resources.district_desc_friedrichshain
import berlinfoodmap.composeapp.generated.resources.district_desc_halensee
import berlinfoodmap.composeapp.generated.resources.district_desc_hellersdorf
import berlinfoodmap.composeapp.generated.resources.district_desc_hermsdorf
import berlinfoodmap.composeapp.generated.resources.district_desc_johannisthal
import berlinfoodmap.composeapp.generated.resources.district_desc_kreuzberg
import berlinfoodmap.composeapp.generated.resources.district_desc_lankwitz
import berlinfoodmap.composeapp.generated.resources.district_desc_lichtenberg
import berlinfoodmap.composeapp.generated.resources.district_desc_lichtenrade
import berlinfoodmap.composeapp.generated.resources.district_desc_lichterfelde
import berlinfoodmap.composeapp.generated.resources.district_desc_mariendorf
import berlinfoodmap.composeapp.generated.resources.district_desc_marienfelde
import berlinfoodmap.composeapp.generated.resources.district_desc_marzahn
import berlinfoodmap.composeapp.generated.resources.district_desc_mitte
import berlinfoodmap.composeapp.generated.resources.district_desc_moabit
import berlinfoodmap.composeapp.generated.resources.district_desc_neukoelln
import berlinfoodmap.composeapp.generated.resources.district_desc_pankow
import berlinfoodmap.composeapp.generated.resources.district_desc_prenzlauer_berg
import berlinfoodmap.composeapp.generated.resources.district_desc_reinickendorf
import berlinfoodmap.composeapp.generated.resources.district_desc_rudow
import berlinfoodmap.composeapp.generated.resources.district_desc_schmargendorf
import berlinfoodmap.composeapp.generated.resources.district_desc_schoeneberg
import berlinfoodmap.composeapp.generated.resources.district_desc_spandau
import berlinfoodmap.composeapp.generated.resources.district_desc_steglitz
import berlinfoodmap.composeapp.generated.resources.district_desc_tegel
import berlinfoodmap.composeapp.generated.resources.district_desc_tempelhof
import berlinfoodmap.composeapp.generated.resources.district_desc_tiergarten
import berlinfoodmap.composeapp.generated.resources.district_desc_wedding
import berlinfoodmap.composeapp.generated.resources.district_desc_westend
import berlinfoodmap.composeapp.generated.resources.district_desc_wilmersdorf
import berlinfoodmap.composeapp.generated.resources.district_desc_wittenau

private val DESCRIPTIONS: Map<String, StringResource> = mapOf(
    "Britz" to Res.string.district_desc_britz,
    "Charlottenburg" to Res.string.district_desc_charlottenburg,
    "Dahlem" to Res.string.district_desc_dahlem,
    "Französisch Buchholz" to Res.string.district_desc_franzoesisch_buchholz,
    "Friedenau" to Res.string.district_desc_friedenau,
    "Friedrichshain" to Res.string.district_desc_friedrichshain,
    "Halensee" to Res.string.district_desc_halensee,
    "Hellersdorf" to Res.string.district_desc_hellersdorf,
    "Hermsdorf" to Res.string.district_desc_hermsdorf,
    "Johannisthal" to Res.string.district_desc_johannisthal,
    "Kreuzberg" to Res.string.district_desc_kreuzberg,
    "Lankwitz" to Res.string.district_desc_lankwitz,
    "Lichtenberg" to Res.string.district_desc_lichtenberg,
    "Lichtenrade" to Res.string.district_desc_lichtenrade,
    "Lichterfelde" to Res.string.district_desc_lichterfelde,
    "Mariendorf" to Res.string.district_desc_mariendorf,
    "Marienfelde" to Res.string.district_desc_marienfelde,
    "Marzahn" to Res.string.district_desc_marzahn,
    "Mitte" to Res.string.district_desc_mitte,
    "Moabit" to Res.string.district_desc_moabit,
    "Neukölln" to Res.string.district_desc_neukoelln,
    "Pankow" to Res.string.district_desc_pankow,
    "Prenzlauer Berg" to Res.string.district_desc_prenzlauer_berg,
    "Reinickendorf" to Res.string.district_desc_reinickendorf,
    "Rudow" to Res.string.district_desc_rudow,
    "Schmargendorf" to Res.string.district_desc_schmargendorf,
    "Schöneberg" to Res.string.district_desc_schoeneberg,
    "Spandau" to Res.string.district_desc_spandau,
    "Steglitz" to Res.string.district_desc_steglitz,
    "Tegel" to Res.string.district_desc_tegel,
    "Tempelhof" to Res.string.district_desc_tempelhof,
    "Tiergarten" to Res.string.district_desc_tiergarten,
    "Wedding" to Res.string.district_desc_wedding,
    "Westend" to Res.string.district_desc_westend,
    "Wilmersdorf" to Res.string.district_desc_wilmersdorf,
    // Best-effort: data file uses "Wittnau" but the actual Berlin Ortsteil is
    // Wittenau. Keep both keys so a future data fix doesn't lose the description.
    "Wittenau" to Res.string.district_desc_wittenau,
    "Wittnau" to Res.string.district_desc_wittenau,
)

/**
 * Description for a Berlin neighbourhood by display name. Returns null for
 * unknown districts so the caller can omit the subtitle line.
 */
@Composable
fun districtDescription(district: String): String? =
    DESCRIPTIONS[district]?.let { stringResource(it) }
