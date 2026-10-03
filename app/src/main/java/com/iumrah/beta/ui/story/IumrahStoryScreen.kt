package com.iumrah.beta.ui.story

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iumrah.beta.R
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.settings.AppLanguage
import com.iumrah.beta.ui.cupertino.CupertinoIcon
import com.iumrah.beta.ui.cupertino.CupertinoSymbol
import com.iumrah.beta.ui.homeflow.InternalNavBar
import com.iumrah.beta.ui.homeflow.tr

@Composable
fun IumrahStoryScreen(language: AppLanguage, chrome: AppChromeStore) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        InternalNavBar(tr(language, "О проекте iumrah", "About iumrah", "iumrah haqida", "iumrah ҳақида"), chrome)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            StoryHero(language)
            Intro(language)
            ExperienceStrip(language)
            StoryImage(R.drawable.about_iumrah_pilgrims, 270)
            TextCard(
                tr(language, "ПОЧЕМУ IUMRAH", "WHY IUMRAH", "NEGA IUMRAH", "НЕГА IUMRAH"),
                tr(language, "Самостоятельность без потери поддержки", "Freedom without losing support", "Yordamni yo‘qotmasdan mustaqillik", "Ёрдамни йўқотмасдан мустақиллик"),
                tr(
                    language,
                    "Вы выбираете даты, людей, уровень отеля и темп поездки. iumrah связывает все части и остаётся рядом, когда нужна помощь.",
                    "You choose dates, people, hotel level and the pace of the journey. iumrah connects the operational pieces and stays with you when you need help.",
                    "Sanalar, hamrohlar, mehmonxona darajasi va safar tempini siz tanlaysiz. iumrah qolgan qismlarni bog‘laydi.",
                    "Саналар, ҳамроҳлар, меҳмонхона даражаси ва сафар темпини сиз танлайсиз. iumrah қолган қисмларни боғлайди.",
                ),
            )
            StoryImage(R.drawable.about_iumrah_kaaba_touch, 320)
            Principles(language)
            StoryImage(R.drawable.about_iumrah_map_dark, 205)
            TextCard(
                tr(language, "КУДА МЫ ИДЁМ", "THE DIRECTION", "YO‘NALISH", "ЙЎНАЛИШ"),
                tr(language, "Nusuk — для разрешений. iumrah — для всей остальной поездки.", "Nusuk for permits. iumrah for the rest of the journey.", "Nusuk — ruxsatlar uchun. iumrah — safarning qolgan qismi uchun.", "Nusuk — рухсатлар учун. iumrah — сафарнинг қолган қисми учун."),
                tr(
                    language,
                    "Цель — спокойный цифровой спутник до, во время и после Umrah: планирование, бронирование, статус, сопровождение и живая Care-поддержка в одной системе.",
                    "The goal is a calm digital companion before, during and after Umrah: planning, booking, status, guidance and human Care in one system.",
                    "Maqsad — Umradan oldin, davomida va keyin rejalashtirish, bron, holat va Care’ni bir tizimda birlashtirish.",
                    "Мақсад — Умрадан олдин, давомида ва кейин режалаштириш, брон, ҳолат ва Care’ни бир тизимда бирлаштириш.",
                ),
            )
            StoryImage(R.drawable.about_iumrah_kaaba_corner, 300)
            Closing(language)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StoryHero(language: AppLanguage) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier.fillMaxWidth().height(326.dp).clip(shape)
            .border(.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .06f), shape),
    ) {
        Image(painterResource(R.drawable.about_iumrah_map_light), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = .72f)))))
        Column(
            Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                tr(language, "3 ГОДА ОПЫТА", "3 YEARS OF EXPERIENCE", "3 YILLIK TAJRIBA", "3 ЙИЛЛИК ТАЖРИБА"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.15.sp,
                color = Color.White.copy(alpha = .78f),
            )
            Text(
                tr(language, "Персональная Umrah, построенная вокруг Вас", "A personal Umrah, built around you", "Sizga mos shaxsiy Umra", "Сизга мос шахсий Умра"),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.55).sp,
                color = Color.White,
                lineHeight = 31.sp,
            )
            Text(
                tr(
                    language,
                    "iumrah вырос из реального опыта паломников: человек должен понимать и контролировать поездку, не оставаясь без поддержки.",
                    "iumrah grew from real pilgrimage experience: the pilgrim should understand and control the journey without losing support.",
                    "iumrah haqiqiy ziyorat tajribasidan tug‘ilgan: ziyoratchi yordamni yo‘qotmasdan safarini tushunishi va boshqarishi kerak.",
                    "iumrah ҳақиқий зиёрат тажрибасидан туғилган: зиёратчи ёрдамни йўқотмасдан сафарини тушуниши ва бошқариши керак.",
                ),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = .84f),
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun Intro(language: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("iumrah", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f))
        Text(
            tr(language, "Одна поездка. Одна понятная система.", "One journey. One clear system.", "Bitta safar. Bitta tushunarli tizim.", "Битта сафар. Битта тушунарли тизим."),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-.5).sp,
            lineHeight = 31.sp,
        )
        Text(
            tr(
                language,
                "Перелёт, отель, трансфер, сопровождение, Care и статус поездки соединены в одном месте.",
                "Flights, hotel, transfer, guidance, Care and trip status are connected in one place.",
                "Parvoz, mehmonxona, transfer, yo‘l-yo‘riq, Care va safar holati bir joyda bog‘langan.",
                "Парвоз, меҳмонхона, трансфер, йўл-йўриқ, Care ва сафар ҳолати бир жойда боғланган.",
            ),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun ExperienceStrip(language: AppLanguage) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Metric("3", tr(language, "года в этой нише", "years in the niche", "yil tajriba", "йил тажриба"), CupertinoSymbol.CalendarClock, Modifier.weight(1f))
        Metric("1", tr(language, "связанная поездка", "connected journey", "yagona safar", "ягона сафар"), CupertinoSymbol.Route, Modifier.weight(1f))
    }
}

@Composable
private fun Metric(value: String, label: String, icon: CupertinoSymbol, modifier: Modifier) {
    Column(
        modifier.heightIn(min = 126.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .055f), RoundedCornerShape(24.dp)).padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CupertinoIcon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f))
        Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f))
    }
}

@Composable
private fun StoryImage(res: Int, height: Int) {
    val shape = RoundedCornerShape(28.dp)
    Image(
        painterResource(res),
        null,
        Modifier.fillMaxWidth().height(height.dp).clip(shape)
            .border(.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .055f), shape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun TextCard(eyebrow: String, title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .border(.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .055f), RoundedCornerShape(28.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (eyebrow.isNotBlank()) Text(eyebrow, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f))
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp, lineHeight = 29.sp)
        Text(body, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), lineHeight = 19.sp)
    }
}

@Composable
private fun Principles(language: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr(language, "Что для нас важно", "What matters to us", "Biz uchun muhim", "Биз учун муҳим"), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Principle(
            CupertinoSymbol.Eye,
            tr(language, "Прозрачность", "Transparency", "Shaffoflik", "Шаффофлик"),
            tr(language, "Понятный статус и следующий шаг.", "Clear status and clear next actions.", "Aniq holat va keyingi qadam.", "Аниқ ҳолат ва кейинги қадам."),
        )
        Principle(
            CupertinoSymbol.Persons,
            tr(language, "Персональный формат", "Personal format", "Shaxsiy format", "Шахсий формат"),
            tr(language, "Поездка для Вас, семьи или друзей.", "A journey for you, your family or friends.", "Siz, oila yoki do‘stlar uchun safar.", "Сиз, оила ёки дўстлар учун сафар."),
        )
        Principle(
            CupertinoSymbol.HeartFill,
            "iumrah Care",
            tr(language, "Живая поддержка, когда одного приложения недостаточно.", "Human support when an app alone is not enough.", "Ilovaning o‘zi yetarli bo‘lmaganda inson yordami.", "Илованинг ўзи етарли бўлмаганда инсон ёрдами."),
        )
    }
}

@Composable
private fun Principle(icon: CupertinoSymbol, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .border(.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .055f), RoundedCornerShape(22.dp)).padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            CupertinoIcon(icon, null, Modifier.size(17.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(body, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f), lineHeight = 19.sp)
        }
    }
}

@Composable
private fun Closing(language: AppLanguage) {
    TextCard(
        "",
        tr(language, "Создано вокруг паломника", "Built around the pilgrim", "Ziyoratchi uchun yaratilgan", "Зиёратчи учун яратилган"),
        tr(
            language,
            "Технология имеет смысл только тогда, когда делает поездку понятнее, спокойнее и персональнее.",
            "Technology matters only when it makes the journey clearer, calmer and more personal.",
            "Texnologiya safarni tushunarliroq, xotirjamroq va shaxsiyroq qilgandagina foydali.",
            "Технология сафарни тушунарлироқ, хотиржамроқ ва шахсийроқ қилгандагина фойдали.",
        ),
    )
}
