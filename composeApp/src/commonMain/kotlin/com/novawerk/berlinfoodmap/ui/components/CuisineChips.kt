package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.restaurant.CuisineType
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.all
import berlinfoodmap.composeapp.generated.resources.cuisine_sichuan
import berlinfoodmap.composeapp.generated.resources.cuisine_cantonese
import berlinfoodmap.composeapp.generated.resources.cuisine_hotpot
import berlinfoodmap.composeapp.generated.resources.cuisine_bbq
import berlinfoodmap.composeapp.generated.resources.cuisine_dim_sum
import berlinfoodmap.composeapp.generated.resources.cuisine_noodles
import berlinfoodmap.composeapp.generated.resources.cuisine_general
import berlinfoodmap.composeapp.generated.resources.cuisine_other

@Composable
fun CuisineChips(
    selected: CuisineType?,
    onSelected: (CuisineType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text(stringResource(Res.string.all)) },
        )
        CuisineType.entries.forEach { cuisine ->
            FilterChip(
                selected = selected == cuisine,
                onClick = { onSelected(cuisine) },
                label = { Text(cuisineDisplayName(cuisine)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(cuisineIconResource(cuisine)),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }
    }
}

fun cuisineIconResource(cuisine: CuisineType): DrawableResource = when (cuisine) {
    CuisineType.SICHUAN -> Res.drawable.cuisine_sichuan
    CuisineType.CANTONESE -> Res.drawable.cuisine_cantonese
    CuisineType.HOTPOT -> Res.drawable.cuisine_hotpot
    CuisineType.BBQ -> Res.drawable.cuisine_bbq
    CuisineType.DIM_SUM -> Res.drawable.cuisine_dim_sum
    CuisineType.NOODLES -> Res.drawable.cuisine_noodles
    CuisineType.GENERAL -> Res.drawable.cuisine_general
    CuisineType.OTHER -> Res.drawable.cuisine_other
}
