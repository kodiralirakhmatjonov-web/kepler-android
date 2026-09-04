package com.iumrah.beta.ui.hotels

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.iumrah.beta.core.config.AppConfig
import com.iumrah.beta.core.design.IumrahMotion
import com.iumrah.beta.core.localization.L10n
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.data.hotel.HotelCatalogService
import com.iumrah.beta.data.hotel.RemotePackageEngineClient
import com.iumrah.beta.models.hotel.HotelDetail
import com.iumrah.beta.models.hotel.HotelImage
import com.iumrah.beta.models.hotel.HotelRoom
import com.iumrah.beta.models.hotel.IumrahRoomCategory
import com.iumrah.beta.models.hotel.IumrahRoomCategoryOption
import com.iumrah.beta.ui.components.IumrahPill
import com.iumrah.beta.ui.components.IumrahPressable
import com.iumrah.beta.ui.components.IumrahSectionHeader

@Composable
fun HotelDetailScreen(
    hotelId: String,
    language: AppLanguage,
    catalog: HotelCatalogService,
    packageEngine: RemotePackageEngineClient,
    onBack: () -> Unit,
) {
    var detail by remember(hotelId) { mutableStateOf<HotelDetail?>(null) }
    var categories by remember(hotelId) { mutableStateOf<List<IumrahRoomCategoryOption>>(emptyList()) }
    var error by remember(hotelId) { mutableStateOf<String?>(null) }
    var selectedCategory by remember(hotelId) { mutableStateOf<String?>(null) }

    LaunchedEffect(hotelId) {
        detail = runCatching { catalog.hotelDetail(hotelId) }.onFailure { error = L10n.text("hotels_load_error", language) }.getOrNull()
        categories = runCatching { packageEngine.roomCategories(hotelId) }.getOrElse { emptyList() }
    }

    val hotel = detail
    if (hotel == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            if (error == null) CircularProgressIndicator() else Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(error.orEmpty()); Spacer(Modifier.height(12.dp)); BackButton(onBack) }
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 54.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { HotelGallery(hotel, onBack) }
        item {
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(hotel.name, style = MaterialTheme.typography.headlineLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    hotel.stars?.let { IumrahPill("$it★") }
                    hotel.rating?.let { IumrahPill(String.format("%.1f", it)) }
                    IumrahPill(L10n.city(hotel.city, language))
                }
                Text(hotel.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))
                if (hotel.description.isNotBlank()) Text(hotel.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.72f))
            }
        }
        if (hotel.amenities.isNotEmpty()) {
            item { IumrahSectionHeader("Amenities", modifier = Modifier.padding(horizontal = 18.dp)) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hotel.amenities.size) { IumrahPill(hotel.amenities[it]) }
                }
            }
        }
        if (categories.isNotEmpty()) {
            item { IumrahSectionHeader("iumrah rooms", modifier = Modifier.padding(horizontal = 18.dp)) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(categories.size, key = { categories[it].id }) { index ->
                        val option = categories[index]
                        RoomCategoryCard(option, selectedCategory == option.id, language) { selectedCategory = option.id }
                    }
                }
            }
        }
        if (hotel.rooms.isNotEmpty()) {
            item { IumrahSectionHeader("Hotel rooms", modifier = Modifier.padding(horizontal = 18.dp)) }
            items(hotel.rooms.size, key = { hotel.rooms[it].id }) { RoomInventoryCard(hotel.rooms[it]) }
        }
    }
}

@Composable
private fun HotelGallery(hotel: HotelDetail, onBack: () -> Unit) {
    val images = hotel.images.sortedWith(compareByDescending<HotelImage> { it.isCover }.thenBy { it.position })
    val pager = rememberPagerState(pageCount = { maxOf(1, images.size) })
    Box(Modifier.fillMaxWidth().height(330.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (images.isNotEmpty()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { index ->
                AsyncImage(
                    model = AppConfig.absoluteUrl(images[index].url),
                    contentDescription = hotel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.26f), Color.Transparent, Color.Black.copy(alpha=.24f)))))
        }
        BackButton(onBack, Modifier.padding(start = 16.dp, top = 46.dp).align(Alignment.TopStart))
        if (images.size > 1) IumrahPill("${pager.currentPage + 1} / ${images.size}", modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), background = Color.Black.copy(alpha=.38f), foreground = Color.White)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IumrahPressable(onClick = onBack, modifier = modifier.size(44.dp), cornerRadius = 99.dp, background = MaterialTheme.colorScheme.surface.copy(alpha=.86f), pressedScale = .92f) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
    }
}

@Composable
private fun RoomCategoryCard(option: IumrahRoomCategoryOption, selected: Boolean, language: AppLanguage, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1f else .985f, IumrahMotion.selection, label = "room-category-scale")
    val colors = when (option.category) {
        IumrahRoomCategory.DOUBLE -> listOf(Color(0xFF233E5A), Color(0xFF467798))
        IumrahRoomCategory.TRIPLE -> listOf(Color(0xFF4C345A), Color(0xFF816490))
        IumrahRoomCategory.QUADRUPLE -> listOf(Color(0xFF3B4431), Color(0xFF6D7C58))
    }
    IumrahPressable(
        onClick = onClick,
        modifier = Modifier.width(320.dp).height(270.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        cornerRadius = 30.dp,
        background = colors.first(),
        shadowElevation = 7.dp,
    ) {
        Column(Modifier.fillMaxSize().background(Brush.linearGradient(colors)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row { Icon(Icons.Rounded.Bed, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp)); Spacer(Modifier.weight(1f)); if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White) }
            Text(option.displayName, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(L10n.text(option.category.bodyKey, language), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha=.80f))
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IumrahPill("${option.maxGuests}", background = Color.White.copy(alpha=.14f), foreground = Color.White)
                IumrahPill(option.bedConfiguration, background = Color.White.copy(alpha=.14f), foreground = Color.White)
            }
        }
    }
}

@Composable
private fun RoomInventoryCard(room: HotelRoom) {
    Column(
        Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(room.name, style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            room.maxGuests?.let { IumrahPill("$it guests") }
            room.beds?.let { IumrahPill(it) }
            room.sizeM2?.let { IumrahPill(String.format("%.0f m²", it)) }
        }
        room.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.58f)) }
    }
}
