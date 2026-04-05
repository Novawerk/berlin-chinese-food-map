package com.novawerk.berlinfoodmap.ui.pages.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.auth.AuthService
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.back
import berlinfoodmap.composeapp.generated.resources.detail_coming_soon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    restaurantId: String,
    repository: RestaurantRepository,
    authService: AuthService,
    onBack: () -> Unit,
) {
    var restaurant by remember { mutableStateOf<Restaurant?>(null) }
    var loading by remember { mutableStateOf(true) }
    var hasVisited by remember { mutableStateOf(false) }

    LaunchedEffect(restaurantId) {
        restaurant = repository.getById(restaurantId)
        loading = false

        // Track view
        val uid = authService.getCurrentUid()
        if (uid != null) {
            repository.incrementViewCount(restaurantId, uid)
            hasVisited = repository.hasVisited(restaurantId, uid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    restaurant?.let {
                        Text(it.name.zh)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val r = restaurant
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.detail_coming_soon))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Name
            Text(r.name.zh, style = MaterialTheme.typography.headlineMedium)
            Text(r.name.en, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Address
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(r.address.addressLine1, style = MaterialTheme.typography.bodyMedium)
                    r.address.addressLine2?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("${r.address.postalCode} ${r.address.city}", style = MaterialTheme.typography.bodySmall)
                    Text(r.address.district, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Description
            r.description?.let { desc ->
                HorizontalDivider()
                Text(desc.zh, style = MaterialTheme.typography.bodyMedium)
                Text(desc.en, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Visibility, contentDescription = null)
                    Text("${r.viewCount}", style = MaterialTheme.typography.titleMedium)
                    Text("views", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Text("${r.visitCount}", style = MaterialTheme.typography.titleMedium)
                    Text("visited", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.weight(1f))

            // Visit button
            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    val uid = authService.getCurrentUid() ?: return@Button
                    scope.launch {
                        try {
                            repository.markVisited(restaurantId, uid)
                            hasVisited = true
                            restaurant = repository.getById(restaurantId)
                        } catch (_: Exception) {}
                    }
                },
                enabled = !hasVisited,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    if (hasVisited) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (hasVisited) "Already Visited" else "I've Been Here")
            }
        }
    }
}

