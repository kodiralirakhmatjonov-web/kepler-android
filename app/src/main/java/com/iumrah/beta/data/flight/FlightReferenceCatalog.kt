package com.iumrah.beta.data.flight

import java.time.ZoneId

data class AirlineReference(val iata: String, val name: String, val websiteDomain: String? = null)
data class AirportReference(val iata: String, val city: String, val name: String, val country: String, val timeZoneIdentifier: String? = null)

/** Offline presentation/filter fallback ported from FlightReferenceCatalog.swift. */
object FlightReferenceCatalog {
    private val airlines = listOf(
        AirlineReference("HY", "Uzbekistan Airways", "uzairways.com"),
        AirlineReference("HH", "Qanot Sharq", "qanotsharq.com"),
        AirlineReference("C6", "Centrum Air", "centrum-air.com"),
        AirlineReference("US", "Silk Avia", "silk-avia.com"),
        AirlineReference("9S", "Air Samarkand", "airsamarkand.com"),
        AirlineReference("2U", "Fly Khiva", "flykhiva.uz"),
        AirlineReference("TK", "Turkish Airlines", "turkishairlines.com"),
        AirlineReference("VF", "AJet", "ajet.com"),
        AirlineReference("PC", "Pegasus Airlines", "flypgs.com"),
        AirlineReference("SV", "Saudia", "saudia.com"),
        AirlineReference("XY", "flynas", "flynas.com"),
        AirlineReference("FZ", "flydubai", "flydubai.com"),
        AirlineReference("G9", "Air Arabia", "airarabia.com"),
        AirlineReference("J9", "Jazeera Airways", "jazeeraairways.com"),
        AirlineReference("QR", "Qatar Airways", "qatarairways.com"),
        AirlineReference("J2", "Azerbaijan Airlines", "azal.az"),
        AirlineReference("KC", "Air Astana", "airastana.com"),
        AirlineReference("FS", "FlyArystan", "flyarystan.com"),
        AirlineReference("SZ", "Somon Air", "somonair.com"),
        AirlineReference("W4", "Wizz Air Malta", "wizzair.com"),
        AirlineReference("EK", "Emirates", "emirates.com"),
        AirlineReference("EY", "Etihad Airways", "etihad.com"),
        AirlineReference("WY", "Oman Air", "omanair.com"),
        AirlineReference("GF", "Gulf Air", "gulfair.com"),
        AirlineReference("KU", "Kuwait Airways", "kuwaitairways.com"),
        AirlineReference("MS", "EgyptAir", "egyptair.com"),
        AirlineReference("W6", "Wizz Air", "wizzair.com"),
        AirlineReference("5W", "Wizz Air Abu Dhabi", "wizzair.com"),
        AirlineReference("3L", "Air Arabia Abu Dhabi", "airarabia.com"),
        AirlineReference("F3", "flyadeal", "flyadeal.com"),
        AirlineReference("RJ", "Royal Jordanian", "rj.com"),
        AirlineReference("OV", "SalamAir", "salamair.com"),
        AirlineReference("6E", "IndiGo", "goindigo.in"),
        AirlineReference("PK", "Pakistan International Airlines", "piac.com.pk"),
    ).associateBy { it.iata }

    private val airports = listOf(
        AirportReference("TAS", "Tashkent", "Tashkent International Airport", "Uzbekistan", "Asia/Tashkent"),
        AirportReference("SKD", "Samarkand", "Samarkand International Airport", "Uzbekistan", "Asia/Samarkand"),
        AirportReference("BHK", "Bukhara", "Bukhara International Airport", "Uzbekistan", "Asia/Samarkand"),
        AirportReference("UGC", "Urgench", "Urgench International Airport", "Uzbekistan", "Asia/Samarkand"),
        AirportReference("NMA", "Namangan", "Namangan International Airport", "Uzbekistan", "Asia/Tashkent"),
        AirportReference("FEG", "Fergana", "Fergana International Airport", "Uzbekistan", "Asia/Tashkent"),
        AirportReference("NCU", "Nukus", "Nukus Airport", "Uzbekistan", "Asia/Samarkand"),
        AirportReference("TMJ", "Termez", "Termez International Airport", "Uzbekistan", "Asia/Samarkand"),
        AirportReference("KSQ", "Karshi", "Karshi Airport", "Uzbekistan", "Asia/Samarkand"),
        AirportReference("AZN", "Andijan", "Andijan Airport", "Uzbekistan", "Asia/Tashkent"),
        AirportReference("NAV", "Navoi", "Navoi International Airport", "Uzbekistan", "Asia/Samarkand"),
        AirportReference("JED", "Jeddah", "King Abdulaziz International Airport", "Saudi Arabia", "Asia/Riyadh"),
        AirportReference("MED", "Madinah", "Prince Mohammad bin Abdulaziz International Airport", "Saudi Arabia", "Asia/Riyadh"),
        AirportReference("RUH", "Riyadh", "King Khalid International Airport", "Saudi Arabia", "Asia/Riyadh"),
        AirportReference("IST", "Istanbul", "Istanbul Airport", "Türkiye", "Europe/Istanbul"),
        AirportReference("SAW", "Istanbul", "Sabiha Gökçen International Airport", "Türkiye", "Europe/Istanbul"),
        AirportReference("DXB", "Dubai", "Dubai International Airport", "United Arab Emirates", "Asia/Dubai"),
        AirportReference("SHJ", "Sharjah", "Sharjah International Airport", "United Arab Emirates", "Asia/Dubai"),
        AirportReference("AUH", "Abu Dhabi", "Zayed International Airport", "United Arab Emirates", "Asia/Dubai"),
        AirportReference("DOH", "Doha", "Hamad International Airport", "Qatar", "Asia/Qatar"),
        AirportReference("KWI", "Kuwait City", "Kuwait International Airport", "Kuwait", "Asia/Kuwait"),
        AirportReference("GYD", "Baku", "Heydar Aliyev International Airport", "Azerbaijan", "Asia/Baku"),
        AirportReference("ALA", "Almaty", "Almaty International Airport", "Kazakhstan", "Asia/Almaty"),
        AirportReference("NQZ", "Astana", "Nursultan Nazarbayev International Airport", "Kazakhstan", "Asia/Almaty"),
        AirportReference("DYU", "Dushanbe", "Dushanbe International Airport", "Tajikistan", "Asia/Dushanbe"),
        AirportReference("MCT", "Muscat", "Muscat International Airport", "Oman", "Asia/Muscat"),
        AirportReference("BAH", "Manama", "Bahrain International Airport", "Bahrain", "Asia/Bahrain"),
        AirportReference("CAI", "Cairo", "Cairo International Airport", "Egypt", "Africa/Cairo"),
        AirportReference("DWC", "Dubai", "Al Maktoum International Airport", "United Arab Emirates", "Asia/Dubai"),
        AirportReference("AMM", "Amman", "Queen Alia International Airport", "Jordan", "Asia/Amman"),
        AirportReference("TBS", "Tbilisi", "Tbilisi International Airport", "Georgia", "Asia/Tbilisi"),
        AirportReference("FRU", "Bishkek", "Manas International Airport", "Kyrgyzstan", "Asia/Bishkek"),
        AirportReference("OSS", "Osh", "Osh International Airport", "Kyrgyzstan", "Asia/Bishkek"),
        AirportReference("IKA", "Tehran", "Imam Khomeini International Airport", "Iran", "Asia/Tehran"),
        AirportReference("ISB", "Islamabad", "Islamabad International Airport", "Pakistan", "Asia/Karachi"),
        AirportReference("DEL", "Delhi", "Indira Gandhi International Airport", "India", "Asia/Kolkata"),
        AirportReference("BOM", "Mumbai", "Chhatrapati Shivaji Maharaj International Airport", "India", "Asia/Kolkata"),
        AirportReference("DMM", "Dammam", "King Fahd International Airport", "Saudi Arabia", "Asia/Riyadh"),
        AirportReference("TIF", "Taif", "Taif International Airport", "Saudi Arabia", "Asia/Riyadh"),
    ).associateBy { it.iata }

    val filterAirlines: List<AirlineReference> get() = airlines.values.sortedBy { it.name.lowercase() }
    fun airline(code: String?): AirlineReference? = code?.trim()?.uppercase()?.let(airlines::get)
    fun airlineName(code: String?, fallback: String): String = airline(code)?.name ?: fallback
    fun airport(code: String): AirportReference? = airports[code.trim().uppercase()]
    fun timeZone(code: String): ZoneId? = airport(code)?.timeZoneIdentifier?.let { runCatching { ZoneId.of(it) }.getOrNull() }
}
