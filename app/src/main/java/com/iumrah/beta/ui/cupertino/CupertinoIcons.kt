package com.iumrah.beta.ui.cupertino

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Local Cupertino/SF-style semantic icon set for Android Compose.
 *
 * These glyphs intentionally avoid Material Icons.  The API mirrors the semantic
 * names used by the iOS `Image(systemName:)` call sites, while the geometry is
 * rendered locally and can be tuned centrally for pixel-parity work.
 */
enum class CupertinoSymbol {
    Home, Hotel, Suitcase, Heart, HeartFill, PersonCircle,
    Menu, ChevronRight, ChevronLeft, ArrowRight,
    ChatBubble, Phone, Send, Lock, LockShield, ShieldCheck,
    CreditCard, UndoCircle, HandRaised, IdentityCard,
    Gear, Globe, HalfCircle, BellSignal, BellBadge,
    Persons, Wallet, Airplane, AirplaneTakeoff, AirplaneLand, Location, Building,
    CheckCircle, ExclamationCircle, Passport, Key,
    SignOut, Sun, Moon, Device, Mail, Apple,
    InfoCircle, Message, TrashSlash, Document, NumberSquare,
    CalendarClock, Close, Copy, Eye, EyeSlash, Sparkles,
    Route, Car, Refresh, PlusPerson, Lightbulb, Checkmark, SignalWave,
    Star, Sliders, Speaker, SpeakerSlash, ChevronDown, Plus, Minus,
    ArrowUpRight, ArrowDown, Paperclip, Bed, Play, ForkKnife, Share,
}

@Composable
fun CupertinoIcon(
    symbol: CupertinoSymbol,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val color = if (tint == Color.Unspecified) Color.Black else tint
    Canvas(modifier = modifier) { drawCupertinoSymbol(symbol, color) }
}

/** Material3-compatible call surface backed entirely by the local Cupertino renderer.
 * This lets legacy Compose call-sites migrate from CupertinoSymbol to CupertinoSymbol
 * without retaining Material icon geometry.
 */
@Composable
fun Icon(
    imageVector: CupertinoSymbol,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    CupertinoIcon(imageVector, contentDescription, modifier, tint)
}

private fun DrawScope.drawCupertinoSymbol(symbol: CupertinoSymbol, color: Color) {
    val w = size.width
    val h = size.height
    val s = minOf(w, h)
    val ox = (w - s) / 2f
    val oy = (h - s) / 2f
    val c = Offset(ox + s / 2f, oy + s / 2f)
    val sw = (s * .075f).coerceAtLeast(1.5f)
    val stroke = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun line(a: Offset, b: Offset, width: Float = sw) = drawLine(color, a, b, width, StrokeCap.Round)
    fun circle(center: Offset, radius: Float, fill: Boolean = false) = drawCircle(color, radius, center, style = if (fill) androidx.compose.ui.graphics.drawscope.Fill else Stroke(sw))
    fun rr(left: Float, top: Float, width: Float, height: Float, radius: Float, fill: Boolean = false) =
        drawRoundRect(color, Offset(left, top), Size(width, height), CornerRadius(radius, radius), style = if (fill) androidx.compose.ui.graphics.drawscope.Fill else Stroke(sw))
    fun arc(rect: Rect, start: Float, sweep: Float) = drawArc(color, start, sweep, false, topLeft = rect.topLeft, size = rect.size, style = stroke)
    fun oval(rect: Rect) = drawOval(color, topLeft = rect.topLeft, size = rect.size, style = stroke)

    when (symbol) {
        CupertinoSymbol.Home -> {
            val p = Path().apply {
                moveTo(ox + s*.15f, oy + s*.48f); lineTo(c.x, oy + s*.16f); lineTo(ox+s*.85f, oy+s*.48f)
                moveTo(ox+s*.25f, oy+s*.43f); lineTo(ox+s*.25f, oy+s*.82f); lineTo(ox+s*.75f, oy+s*.82f); lineTo(ox+s*.75f, oy+s*.43f)
            }; drawPath(p,color,style=stroke)
        }
        CupertinoSymbol.Hotel, CupertinoSymbol.Building -> {
            rr(ox+s*.2f, oy+s*.18f, s*.6f, s*.64f, s*.06f)
            for (x in listOf(.33f,.5f,.67f)) for (y in listOf(.34f,.5f,.66f)) circle(Offset(ox+s*x,oy+s*y),s*.025f,true)
            line(Offset(c.x,oy+s*.7f),Offset(c.x,oy+s*.82f))
        }
        CupertinoSymbol.Suitcase -> {
            rr(ox+s*.18f, oy+s*.32f, s*.64f, s*.5f, s*.09f)
            rr(ox+s*.38f, oy+s*.19f, s*.24f, s*.16f, s*.06f)
            line(Offset(ox+s*.34f,oy+s*.36f),Offset(ox+s*.34f,oy+s*.78f),sw*.8f)
            line(Offset(ox+s*.66f,oy+s*.36f),Offset(ox+s*.66f,oy+s*.78f),sw*.8f)
        }
        CupertinoSymbol.Heart, CupertinoSymbol.HeartFill -> {
            val p=Path().apply{moveTo(c.x,oy+s*.82f); cubicTo(ox+s*.1f,oy+s*.58f,ox+s*.12f,oy+s*.24f,ox+s*.34f,oy+s*.24f); cubicTo(ox+s*.44f,oy+s*.24f,c.x,oy+s*.34f,c.x,oy+s*.39f); cubicTo(c.x,oy+s*.34f,ox+s*.56f,oy+s*.24f,ox+s*.66f,oy+s*.24f); cubicTo(ox+s*.88f,oy+s*.24f,ox+s*.9f,oy+s*.58f,c.x,oy+s*.82f)}
            drawPath(p,color,style=if(symbol==CupertinoSymbol.HeartFill) androidx.compose.ui.graphics.drawscope.Fill else stroke)
        }
        CupertinoSymbol.PersonCircle -> {
            circle(c,s*.36f); circle(Offset(c.x,oy+s*.4f),s*.105f); val p=Path().apply{moveTo(ox+s*.31f,oy+s*.68f); quadraticBezierTo(c.x,oy+s*.55f,ox+s*.69f,oy+s*.68f)}; drawPath(p,color,style=stroke)
        }
        CupertinoSymbol.Menu -> { line(Offset(ox+s*.2f,oy+s*.32f),Offset(ox+s*.8f,oy+s*.32f)); line(Offset(ox+s*.2f,c.y),Offset(ox+s*.8f,c.y)); line(Offset(ox+s*.2f,oy+s*.68f),Offset(ox+s*.8f,oy+s*.68f)) }
        CupertinoSymbol.ChevronRight, CupertinoSymbol.ArrowRight -> {
            if(symbol==CupertinoSymbol.ArrowRight) line(Offset(ox+s*.18f,c.y),Offset(ox+s*.78f,c.y))
            line(Offset(ox+s*.55f,oy+s*.28f),Offset(ox+s*.78f,c.y)); line(Offset(ox+s*.78f,c.y),Offset(ox+s*.55f,oy+s*.72f))
        }
        CupertinoSymbol.ChevronLeft -> { line(Offset(ox+s*.45f,oy+s*.28f),Offset(ox+s*.22f,c.y)); line(Offset(ox+s*.22f,c.y),Offset(ox+s*.45f,oy+s*.72f)) }
        CupertinoSymbol.ChatBubble, CupertinoSymbol.Message -> {
            rr(ox+s*.16f,oy+s*.19f,s*.68f,s*.52f,s*.14f); val p=Path().apply{moveTo(ox+s*.32f,oy+s*.7f); lineTo(ox+s*.25f,oy+s*.84f); lineTo(ox+s*.47f,oy+s*.71f)}; drawPath(p,color,style=stroke)
        }
        CupertinoSymbol.Phone -> {
            val p=Path().apply{moveTo(ox+s*.29f,oy+s*.2f); cubicTo(ox+s*.2f,oy+s*.25f,ox+s*.22f,oy+s*.45f,ox+s*.4f,oy+s*.63f); cubicTo(ox+s*.58f,oy+s*.81f,ox+s*.77f,oy+s*.82f,ox+s*.82f,oy+s*.72f); lineTo(ox+s*.69f,oy+s*.59f); cubicTo(ox+s*.65f,oy+s*.55f,ox+s*.58f,oy+s*.62f,ox+s*.52f,oy+s*.57f); lineTo(ox+s*.43f,oy+s*.48f); cubicTo(ox+s*.38f,oy+s*.42f,ox+s*.45f,oy+s*.35f,ox+s*.41f,oy+s*.3f); close()}; drawPath(p,color,style=stroke)
        }
        CupertinoSymbol.Send -> { val p=Path().apply{moveTo(ox+s*.15f,oy+s*.22f); lineTo(ox+s*.85f,c.y); lineTo(ox+s*.15f,oy+s*.78f); lineTo(ox+s*.31f,c.y); close()}; drawPath(p,color,style=stroke); line(Offset(ox+s*.31f,c.y),Offset(ox+s*.72f,c.y),sw*.8f) }
        CupertinoSymbol.Lock, CupertinoSymbol.LockShield, CupertinoSymbol.ShieldCheck -> {
            if(symbol==CupertinoSymbol.Lock){ rr(ox+s*.23f,oy+s*.43f,s*.54f,s*.38f,s*.09f); drawArc(color,180f,180f,false,Offset(ox+s*.34f,oy+s*.17f),Size(s*.32f,s*.42f),style=stroke) }
            else { val p=Path().apply{moveTo(c.x,oy+s*.13f); lineTo(ox+s*.77f,oy+s*.25f); lineTo(ox+s*.73f,oy+s*.58f); quadraticBezierTo(c.x,oy+s*.86f,ox+s*.27f,oy+s*.58f); lineTo(ox+s*.23f,oy+s*.25f); close()}; drawPath(p,color,style=stroke); if(symbol==CupertinoSymbol.ShieldCheck){ line(Offset(ox+s*.36f,oy+s*.49f),Offset(ox+s*.46f,oy+s*.6f)); line(Offset(ox+s*.46f,oy+s*.6f),Offset(ox+s*.65f,oy+s*.39f)) } else { rr(ox+s*.4f,oy+s*.42f,s*.2f,s*.18f,s*.04f); drawArc(color,180f,180f,false,Offset(ox+s*.44f,oy+s*.31f),Size(s*.12f,s*.18f),style=stroke) } }
        }
        CupertinoSymbol.CreditCard -> { rr(ox+s*.13f,oy+s*.25f,s*.74f,s*.5f,s*.08f); line(Offset(ox+s*.15f,oy+s*.4f),Offset(ox+s*.85f,oy+s*.4f),sw*.9f); line(Offset(ox+s*.23f,oy+s*.61f),Offset(ox+s*.42f,oy+s*.61f),sw*.9f) }
        CupertinoSymbol.UndoCircle, CupertinoSymbol.Refresh -> { circle(c,s*.34f); arc(Rect(ox+s*.28f,oy+s*.28f,ox+s*.72f,oy+s*.72f),200f,250f); line(Offset(ox+s*.31f,oy+s*.38f),Offset(ox+s*.22f,oy+s*.48f)); line(Offset(ox+s*.22f,oy+s*.48f),Offset(ox+s*.35f,oy+s*.51f)) }
        CupertinoSymbol.HandRaised -> { rr(ox+s*.29f,oy+s*.31f,s*.42f,s*.49f,s*.14f); line(Offset(ox+s*.35f,oy+s*.43f),Offset(ox+s*.35f,oy+s*.2f)); line(Offset(ox+s*.45f,oy+s*.39f),Offset(ox+s*.45f,oy+s*.16f)); line(Offset(ox+s*.55f,oy+s*.39f),Offset(ox+s*.55f,oy+s*.18f)); line(Offset(ox+s*.65f,oy+s*.43f),Offset(ox+s*.65f,oy+s*.24f)); line(Offset(ox+s*.3f,oy+s*.56f),Offset(ox+s*.2f,oy+s*.47f)) }
        CupertinoSymbol.IdentityCard, CupertinoSymbol.Passport -> { rr(ox+s*.15f,oy+s*.2f,s*.7f,s*.6f,s*.08f); circle(Offset(ox+s*.35f,oy+s*.43f),s*.08f); arc(Rect(ox+s*.24f,oy+s*.46f,ox+s*.46f,oy+s*.69f),200f,140f); line(Offset(ox+s*.53f,oy+s*.38f),Offset(ox+s*.74f,oy+s*.38f)); line(Offset(ox+s*.53f,oy+s*.51f),Offset(ox+s*.74f,oy+s*.51f)); line(Offset(ox+s*.53f,oy+s*.64f),Offset(ox+s*.67f,oy+s*.64f)) }
        CupertinoSymbol.Gear -> { circle(c,s*.19f); circle(c,s*.08f); for(i in 0 until 8){ val a=Math.PI*2*i/8; val p1=Offset(c.x+(kotlin.math.cos(a)*s*.25).toFloat(),c.y+(kotlin.math.sin(a)*s*.25).toFloat()); val p2=Offset(c.x+(kotlin.math.cos(a)*s*.36).toFloat(),c.y+(kotlin.math.sin(a)*s*.36).toFloat()); line(p1,p2,sw*1.15f)} }
        CupertinoSymbol.Globe -> { circle(c,s*.34f); oval(Rect(ox+s*.38f,oy+s*.16f,ox+s*.62f,oy+s*.84f)); line(Offset(ox+s*.17f,c.y),Offset(ox+s*.83f,c.y)); arc(Rect(ox+s*.18f,oy+s*.28f,ox+s*.82f,oy+s*.62f),0f,180f); arc(Rect(ox+s*.18f,oy+s*.38f,ox+s*.82f,oy+s*.72f),180f,180f) }
        CupertinoSymbol.HalfCircle -> { circle(c,s*.34f); val p=Path().apply{moveTo(c.x,oy+s*.16f); arcTo(Rect(ox+s*.16f,oy+s*.16f,ox+s*.84f,oy+s*.84f),-90f,-180f,false); close()}; drawPath(p,color) }
        CupertinoSymbol.BellSignal, CupertinoSymbol.BellBadge -> { val p=Path().apply{moveTo(ox+s*.28f,oy+s*.64f); quadraticBezierTo(ox+s*.33f,oy+s*.56f,ox+s*.34f,oy+s*.39f); quadraticBezierTo(ox+s*.36f,oy+s*.22f,c.x,oy+s*.2f); quadraticBezierTo(ox+s*.64f,oy+s*.22f,ox+s*.66f,oy+s*.39f); quadraticBezierTo(ox+s*.67f,oy+s*.56f,ox+s*.72f,oy+s*.64f); close()}; drawPath(p,color,style=stroke); line(Offset(ox+s*.25f,oy+s*.66f),Offset(ox+s*.75f,oy+s*.66f)); arc(Rect(ox+s*.42f,oy+s*.64f,ox+s*.58f,oy+s*.79f),0f,180f); if(symbol==CupertinoSymbol.BellBadge) circle(Offset(ox+s*.74f,oy+s*.25f),s*.09f,true) else { arc(Rect(ox+s*.7f,oy+s*.34f,ox+s*.92f,oy+s*.66f),-60f,120f) } }
        CupertinoSymbol.Persons, CupertinoSymbol.PlusPerson -> { circle(Offset(ox+s*.4f,oy+s*.38f),s*.105f); circle(Offset(ox+s*.67f,oy+s*.42f),s*.085f); arc(Rect(ox+s*.2f,oy+s*.48f,ox+s*.59f,oy+s*.82f),205f,130f); arc(Rect(ox+s*.52f,oy+s*.52f,ox+s*.82f,oy+s*.78f),205f,130f); if(symbol==CupertinoSymbol.PlusPerson){ line(Offset(ox+s*.72f,oy+s*.2f),Offset(ox+s*.9f,oy+s*.2f)); line(Offset(ox+s*.81f,oy+s*.11f),Offset(ox+s*.81f,oy+s*.29f)) } }
        CupertinoSymbol.Wallet -> { rr(ox+s*.16f,oy+s*.28f,s*.68f,s*.48f,s*.09f); rr(ox+s*.54f,oy+s*.4f,s*.3f,s*.22f,s*.06f); circle(Offset(ox+s*.63f,oy+s*.51f),s*.025f,true) }
        CupertinoSymbol.Airplane, CupertinoSymbol.AirplaneTakeoff, CupertinoSymbol.AirplaneLand -> { val p=Path().apply{moveTo(ox+s*.14f,oy+s*.56f); lineTo(ox+s*.43f,oy+s*.49f); lineTo(ox+s*.68f,oy+s*.2f); lineTo(ox+s*.77f,oy+s*.22f); lineTo(ox+s*.65f,oy+s*.5f); lineTo(ox+s*.84f,oy+s*.55f); lineTo(ox+s*.82f,oy+s*.63f); lineTo(ox+s*.6f,oy+s*.61f); lineTo(ox+s*.45f,oy+s*.82f); lineTo(ox+s*.37f,oy+s*.8f); lineTo(ox+s*.44f,oy+s*.59f); lineTo(ox+s*.16f,oy+s*.64f); close()}; drawPath(p,color,style=stroke) }
        CupertinoSymbol.Location -> { val p=Path().apply{moveTo(c.x,oy+s*.86f); cubicTo(ox+s*.22f,oy+s*.56f,ox+s*.27f,oy+s*.18f,c.x,oy+s*.18f); cubicTo(ox+s*.73f,oy+s*.18f,ox+s*.78f,oy+s*.56f,c.x,oy+s*.86f); close()}; drawPath(p,color,style=stroke); circle(Offset(c.x,oy+s*.42f),s*.09f) }
        CupertinoSymbol.CheckCircle, CupertinoSymbol.ExclamationCircle, CupertinoSymbol.InfoCircle -> { circle(c,s*.34f); when(symbol){ CupertinoSymbol.CheckCircle->{ line(Offset(ox+s*.32f,c.y),Offset(ox+s*.45f,oy+s*.62f)); line(Offset(ox+s*.45f,oy+s*.62f),Offset(ox+s*.69f,oy+s*.37f))}; CupertinoSymbol.ExclamationCircle->{line(Offset(c.x,oy+s*.31f),Offset(c.x,oy+s*.54f)); circle(Offset(c.x,oy+s*.67f),s*.025f,true)}; else->{circle(Offset(c.x,oy+s*.34f),s*.024f,true); line(Offset(c.x,oy+s*.45f),Offset(c.x,oy+s*.68f))} } }
        CupertinoSymbol.Key -> { circle(Offset(ox+s*.34f,oy+s*.47f),s*.15f); line(Offset(ox+s*.48f,oy+s*.5f),Offset(ox+s*.82f,oy+s*.5f)); line(Offset(ox+s*.68f,oy+s*.5f),Offset(ox+s*.68f,oy+s*.63f)); line(Offset(ox+s*.78f,oy+s*.5f),Offset(ox+s*.78f,oy+s*.59f)) }
        CupertinoSymbol.SignOut -> { rr(ox+s*.17f,oy+s*.18f,s*.44f,s*.64f,s*.06f); line(Offset(ox+s*.48f,c.y),Offset(ox+s*.87f,c.y)); line(Offset(ox+s*.75f,oy+s*.39f),Offset(ox+s*.87f,c.y)); line(Offset(ox+s*.87f,c.y),Offset(ox+s*.75f,oy+s*.61f)) }
        CupertinoSymbol.Sun -> { circle(c,s*.15f); for(i in 0 until 8){ val a=Math.PI*2*i/8; line(Offset(c.x+(kotlin.math.cos(a)*s*.26).toFloat(),c.y+(kotlin.math.sin(a)*s*.26).toFloat()),Offset(c.x+(kotlin.math.cos(a)*s*.36).toFloat(),c.y+(kotlin.math.sin(a)*s*.36).toFloat())) } }
        CupertinoSymbol.Moon -> { val p=Path().apply{moveTo(ox+s*.67f,oy+s*.2f); cubicTo(ox+s*.37f,oy+s*.23f,ox+s*.3f,oy+s*.65f,ox+s*.58f,oy+s*.79f); cubicTo(ox+s*.34f,oy+s*.86f,ox+s*.17f,oy+s*.66f,ox+s*.19f,oy+s*.45f); cubicTo(ox+s*.21f,oy+s*.25f,ox+s*.4f,oy+s*.13f,ox+s*.67f,oy+s*.2f); close()}; drawPath(p,color,style=stroke) }
        CupertinoSymbol.Device -> { rr(ox+s*.24f,oy+s*.12f,s*.52f,s*.76f,s*.1f); line(Offset(ox+s*.42f,oy+s*.78f),Offset(ox+s*.58f,oy+s*.78f),sw*.8f) }
        CupertinoSymbol.Mail -> { rr(ox+s*.13f,oy+s*.24f,s*.74f,s*.52f,s*.07f); line(Offset(ox+s*.15f,oy+s*.29f),Offset(c.x,oy+s*.55f)); line(Offset(c.x,oy+s*.55f),Offset(ox+s*.85f,oy+s*.29f)) }
        CupertinoSymbol.Apple -> { circle(c,s*.29f); line(Offset(c.x,oy+s*.2f),Offset(ox+s*.58f,oy+s*.12f),sw*.9f) }
        CupertinoSymbol.TrashSlash -> { rr(ox+s*.31f,oy+s*.29f,s*.38f,s*.48f,s*.05f); line(Offset(ox+s*.25f,oy+s*.25f),Offset(ox+s*.75f,oy+s*.25f)); line(Offset(ox+s*.41f,oy+s*.17f),Offset(ox+s*.59f,oy+s*.17f)); line(Offset(ox+s*.18f,oy+s*.18f),Offset(ox+s*.82f,oy+s*.82f)) }
        CupertinoSymbol.Document -> { rr(ox+s*.24f,oy+s*.13f,s*.52f,s*.74f,s*.05f); line(Offset(ox+s*.34f,oy+s*.36f),Offset(ox+s*.66f,oy+s*.36f)); line(Offset(ox+s*.34f,oy+s*.5f),Offset(ox+s*.66f,oy+s*.5f)); line(Offset(ox+s*.34f,oy+s*.64f),Offset(ox+s*.58f,oy+s*.64f)) }
        CupertinoSymbol.NumberSquare -> { rr(ox+s*.18f,oy+s*.18f,s*.64f,s*.64f,s*.12f); line(Offset(ox+s*.42f,oy+s*.34f),Offset(ox+s*.36f,oy+s*.66f)); line(Offset(ox+s*.61f,oy+s*.34f),Offset(ox+s*.55f,oy+s*.66f)); line(Offset(ox+s*.3f,oy+s*.45f),Offset(ox+s*.68f,oy+s*.45f)); line(Offset(ox+s*.28f,oy+s*.57f),Offset(ox+s*.66f,oy+s*.57f)) }
        CupertinoSymbol.CalendarClock -> { rr(ox+s*.16f,oy+s*.2f,s*.68f,s*.62f,s*.08f); line(Offset(ox+s*.16f,oy+s*.36f),Offset(ox+s*.84f,oy+s*.36f)); line(Offset(ox+s*.32f,oy+s*.13f),Offset(ox+s*.32f,oy+s*.27f)); line(Offset(ox+s*.68f,oy+s*.13f),Offset(ox+s*.68f,oy+s*.27f)); circle(Offset(ox+s*.64f,oy+s*.61f),s*.12f); line(Offset(ox+s*.64f,oy+s*.61f),Offset(ox+s*.64f,oy+s*.54f),sw*.75f); line(Offset(ox+s*.64f,oy+s*.61f),Offset(ox+s*.7f,oy+s*.65f),sw*.75f) }
        CupertinoSymbol.Close -> { line(Offset(ox+s*.25f,oy+s*.25f),Offset(ox+s*.75f,oy+s*.75f)); line(Offset(ox+s*.75f,oy+s*.25f),Offset(ox+s*.25f,oy+s*.75f)) }
        CupertinoSymbol.Copy -> { rr(ox+s*.2f,oy+s*.28f,s*.48f,s*.5f,s*.07f); rr(ox+s*.34f,oy+s*.16f,s*.46f,s*.5f,s*.07f) }
        CupertinoSymbol.Eye, CupertinoSymbol.EyeSlash -> { val p=Path().apply{moveTo(ox+s*.14f,c.y); quadraticBezierTo(c.x,oy+s*.2f,ox+s*.86f,c.y); quadraticBezierTo(c.x,oy+s*.8f,ox+s*.14f,c.y)}; drawPath(p,color,style=stroke); circle(c,s*.1f); if(symbol==CupertinoSymbol.EyeSlash) line(Offset(ox+s*.18f,oy+s*.18f),Offset(ox+s*.82f,oy+s*.82f)) }
        CupertinoSymbol.Sparkles -> { line(Offset(c.x,oy+s*.14f),Offset(c.x,oy+s*.54f)); line(Offset(ox+s*.3f,oy+s*.34f),Offset(ox+s*.7f,oy+s*.34f)); line(Offset(ox+s*.7f,oy+s*.54f),Offset(ox+s*.7f,oy+s*.82f),sw*.8f); line(Offset(ox+s*.56f,oy+s*.68f),Offset(ox+s*.84f,oy+s*.68f),sw*.8f) }
        CupertinoSymbol.Route -> { circle(Offset(ox+s*.25f,oy+s*.72f),s*.06f); circle(Offset(ox+s*.75f,oy+s*.27f),s*.06f); val p=Path().apply{moveTo(ox+s*.31f,oy+s*.72f); cubicTo(ox+s*.48f,oy+s*.73f,ox+s*.43f,oy+s*.29f,ox+s*.69f,oy+s*.28f)}; drawPath(p,color,style=stroke) }
        CupertinoSymbol.Car -> { rr(ox+s*.17f,oy+s*.42f,s*.66f,s*.28f,s*.07f); val p=Path().apply{moveTo(ox+s*.28f,oy+s*.42f); lineTo(ox+s*.38f,oy+s*.27f); lineTo(ox+s*.66f,oy+s*.27f); lineTo(ox+s*.75f,oy+s*.42f)}; drawPath(p,color,style=stroke); circle(Offset(ox+s*.3f,oy+s*.71f),s*.06f); circle(Offset(ox+s*.7f,oy+s*.71f),s*.06f) }
        CupertinoSymbol.Lightbulb -> { circle(Offset(c.x,oy+s*.4f),s*.2f); line(Offset(ox+s*.41f,oy+s*.6f),Offset(ox+s*.43f,oy+s*.7f)); line(Offset(ox+s*.59f,oy+s*.6f),Offset(ox+s*.57f,oy+s*.7f)); line(Offset(ox+s*.43f,oy+s*.7f),Offset(ox+s*.57f,oy+s*.7f)); line(Offset(ox+s*.45f,oy+s*.78f),Offset(ox+s*.55f,oy+s*.78f)) }
        CupertinoSymbol.Checkmark -> { line(Offset(ox+s*.25f,c.y),Offset(ox+s*.43f,oy+s*.68f)); line(Offset(ox+s*.43f,oy+s*.68f),Offset(ox+s*.76f,oy+s*.33f)) }
        CupertinoSymbol.Star -> { val p=Path(); for(i in 0 until 10){ val a=(-Math.PI/2 + i*Math.PI/5); val r=if(i%2==0)s*.34f else s*.15f; val q=Offset(c.x+(kotlin.math.cos(a)*r).toFloat(),c.y+(kotlin.math.sin(a)*r).toFloat()); if(i==0)p.moveTo(q.x,q.y) else p.lineTo(q.x,q.y)}; p.close(); drawPath(p,color,style=stroke) }
        CupertinoSymbol.Sliders -> { line(Offset(ox+s*.18f,oy+s*.3f),Offset(ox+s*.82f,oy+s*.3f)); line(Offset(ox+s*.18f,c.y),Offset(ox+s*.82f,c.y)); line(Offset(ox+s*.18f,oy+s*.7f),Offset(ox+s*.82f,oy+s*.7f)); circle(Offset(ox+s*.36f,oy+s*.3f),s*.055f,true); circle(Offset(ox+s*.65f,c.y),s*.055f,true); circle(Offset(ox+s*.47f,oy+s*.7f),s*.055f,true) }
        CupertinoSymbol.Speaker, CupertinoSymbol.SpeakerSlash -> { val p=Path().apply{moveTo(ox+s*.18f,oy+s*.43f); lineTo(ox+s*.34f,oy+s*.43f); lineTo(ox+s*.52f,oy+s*.27f); lineTo(ox+s*.52f,oy+s*.73f); lineTo(ox+s*.34f,oy+s*.57f); lineTo(ox+s*.18f,oy+s*.57f); close()}; drawPath(p,color,style=stroke); arc(Rect(ox+s*.48f,oy+s*.34f,ox+s*.72f,oy+s*.66f),-50f,100f); arc(Rect(ox+s*.46f,oy+s*.23f,ox+s*.86f,oy+s*.77f),-50f,100f); if(symbol==CupertinoSymbol.SpeakerSlash) line(Offset(ox+s*.18f,oy+s*.2f),Offset(ox+s*.82f,oy+s*.8f)) }
        CupertinoSymbol.ChevronDown -> { line(Offset(ox+s*.27f,oy+s*.4f),Offset(c.x,oy+s*.63f)); line(Offset(c.x,oy+s*.63f),Offset(ox+s*.73f,oy+s*.4f)) }
        CupertinoSymbol.Plus -> { line(Offset(ox+s*.2f,c.y),Offset(ox+s*.8f,c.y)); line(Offset(c.x,oy+s*.2f),Offset(c.x,oy+s*.8f)) }
        CupertinoSymbol.Minus -> { line(Offset(ox+s*.2f,c.y),Offset(ox+s*.8f,c.y)) }
        CupertinoSymbol.ArrowUpRight -> { line(Offset(ox+s*.22f,oy+s*.78f),Offset(ox+s*.78f,oy+s*.22f)); line(Offset(ox+s*.48f,oy+s*.22f),Offset(ox+s*.78f,oy+s*.22f)); line(Offset(ox+s*.78f,oy+s*.22f),Offset(ox+s*.78f,oy+s*.52f)) }
        CupertinoSymbol.ArrowDown -> { line(Offset(c.x,oy+s*.18f),Offset(c.x,oy+s*.78f)); line(Offset(ox+s*.3f,oy+s*.58f),Offset(c.x,oy+s*.78f)); line(Offset(c.x,oy+s*.78f),Offset(ox+s*.7f,oy+s*.58f)) }
        CupertinoSymbol.Paperclip -> { val p=Path().apply{moveTo(ox+s*.66f,oy+s*.25f); cubicTo(ox+s*.82f,oy+s*.36f,ox+s*.75f,oy+s*.53f,ox+s*.63f,oy+s*.65f); lineTo(ox+s*.42f,oy+s*.79f); cubicTo(ox+s*.24f,oy+s*.9f,ox+s*.09f,oy+s*.7f,ox+s*.25f,oy+s*.55f); lineTo(ox+s*.53f,oy+s*.31f); cubicTo(ox+s*.65f,oy+s*.2f,ox+s*.76f,oy+s*.34f,ox+s*.65f,oy+s*.45f); lineTo(ox+s*.39f,oy+s*.66f)}; drawPath(p,color,style=stroke) }
        CupertinoSymbol.Bed -> { rr(ox+s*.15f,oy+s*.42f,s*.7f,s*.3f,s*.05f); rr(ox+s*.2f,oy+s*.3f,s*.22f,s*.14f,s*.04f); line(Offset(ox+s*.15f,oy+s*.72f),Offset(ox+s*.15f,oy+s*.83f)); line(Offset(ox+s*.85f,oy+s*.72f),Offset(ox+s*.85f,oy+s*.83f)) }
        CupertinoSymbol.Play -> { val p=Path().apply{moveTo(ox+s*.35f,oy+s*.22f); lineTo(ox+s*.78f,c.y); lineTo(ox+s*.35f,oy+s*.78f); close()}; drawPath(p,color,style=stroke) }
        CupertinoSymbol.ForkKnife -> { line(Offset(ox+s*.3f,oy+s*.18f),Offset(ox+s*.3f,oy+s*.82f)); line(Offset(ox+s*.2f,oy+s*.18f),Offset(ox+s*.2f,oy+s*.39f)); line(Offset(ox+s*.4f,oy+s*.18f),Offset(ox+s*.4f,oy+s*.39f)); arc(Rect(ox+s*.2f,oy+s*.29f,ox+s*.4f,oy+s*.48f),0f,180f); val p=Path().apply{moveTo(ox+s*.67f,oy+s*.18f); quadraticBezierTo(ox+s*.82f,oy+s*.34f,ox+s*.68f,oy+s*.5f); lineTo(ox+s*.68f,oy+s*.82f)}; drawPath(p,color,style=stroke) }
        CupertinoSymbol.Share -> { rr(ox+s*.2f,oy+s*.35f,s*.6f,s*.48f,s*.07f); line(Offset(c.x,oy+s*.58f),Offset(c.x,oy+s*.13f)); line(Offset(ox+s*.36f,oy+s*.27f),Offset(c.x,oy+s*.13f)); line(Offset(c.x,oy+s*.13f),Offset(ox+s*.64f,oy+s*.27f)) }
        CupertinoSymbol.SignalWave -> { circle(Offset(ox+s*.27f,c.y),s*.035f,true); arc(Rect(ox+s*.28f,oy+s*.34f,ox+s*.58f,oy+s*.66f),-58f,116f); arc(Rect(ox+s*.27f,oy+s*.22f,ox+s*.76f,oy+s*.78f),-58f,116f); arc(Rect(ox+s*.26f,oy+s*.1f,ox+s*.94f,oy+s*.9f),-58f,116f) }
    }
}
