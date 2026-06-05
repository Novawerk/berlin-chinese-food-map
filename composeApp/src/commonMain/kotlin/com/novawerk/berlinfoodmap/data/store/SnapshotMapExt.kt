package com.novawerk.berlinfoodmap.data.store

import androidx.compose.runtime.snapshots.SnapshotStateMap

/**
 * Drop every entry whose key is not in [keep]. Used to evict per-restaurant
 * caches (cover bitmaps, marker descriptors) when a restaurant leaves the
 * dataset, so the cache ceiling tracks the live id set.
 */
internal fun <K, V> SnapshotStateMap<K, V>.retainKeys(keep: Set<K>) {
    keys.toList().forEach { key ->
        if (key !in keep) remove(key)
    }
}
