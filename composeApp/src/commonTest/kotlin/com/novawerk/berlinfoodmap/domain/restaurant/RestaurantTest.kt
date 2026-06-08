package com.novawerk.berlinfoodmap.domain.restaurant

import com.novawerk.berlinfoodmap.testutil.restaurant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RestaurantTest {

    @Test
    fun previewImage_prefersAdminGalleryFirst() {
        val r = restaurant(
            galleries = listOf("gallery.jpg"),
            coverPhotoUrl = "cover.jpg",
            photoUrls = listOf("photo.jpg"),
            logoUrl = "logo.jpg",
        )
        assertEquals("gallery.jpg", r.previewImageUrl())
    }

    @Test
    fun previewImage_fallsBackToGoogleCover() {
        val r = restaurant(coverPhotoUrl = "cover.jpg", photoUrls = listOf("photo.jpg"), logoUrl = "logo.jpg")
        assertEquals("cover.jpg", r.previewImageUrl())
    }

    @Test
    fun previewImage_fallsBackToFirstGooglePhoto() {
        val r = restaurant(photoUrls = listOf("photo1.jpg", "photo2.jpg"), logoUrl = "logo.jpg")
        assertEquals("photo1.jpg", r.previewImageUrl())
    }

    @Test
    fun previewImage_fallsBackToLogo() {
        val r = restaurant(logoUrl = "logo.jpg")
        assertEquals("logo.jpg", r.previewImageUrl())
    }

    @Test
    fun previewImage_nullWhenNothingAvailable() {
        assertNull(restaurant().previewImageUrl())
    }

    @Test
    fun primaryTag_isFirstTagOrNull() {
        assertEquals(Tag.SICHUAN, restaurant(tags = listOf(Tag.SICHUAN, Tag.HOTPOT)).primaryTag())
        assertNull(restaurant(tags = emptyList()).primaryTag())
    }
}
