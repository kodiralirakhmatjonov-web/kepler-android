package com.iumrah.beta.data.flight

import com.iumrah.beta.models.flight.Airport

/**
 * Zero-network airport bootstrap catalog ported from iOS AirportMapBootstrapCatalog.
 * Keeps the core Uzbekistan / Saudi / hub network available instantly on MapLibre.
 */
object AirportMapBootstrapCatalog {
    val airports: List<Airport> = listOf(
        Airport("TAS", "UTTT", "Tashkent International Airport", "Tashkent", "Uzbekistan", "UZ", "", 41.2579, 69.2812, "airport", 100.0),
        Airport("SKD", "UTSS", "Samarkand International Airport", "Samarkand", "Uzbekistan", "UZ", "", 39.7005, 66.9838, "airport", 96.0),
        Airport("BHK", "UTSB", "Bukhara International Airport", "Bukhara", "Uzbekistan", "UZ", "", 39.7750, 64.4833, "airport", 92.0),
        Airport("UGC", "UTNU", "Urgench International Airport", "Urgench", "Uzbekistan", "UZ", "", 41.5843, 60.6417, "airport", 92.0),
        Airport("NMA", "UTKN", "Namangan International Airport", "Namangan", "Uzbekistan", "UZ", "", 40.9846, 71.5567, "airport", 91.0),
        Airport("FEG", "UTKF", "Fergana International Airport", "Fergana", "Uzbekistan", "UZ", "", 40.3588, 71.7450, "airport", 91.0),
        Airport("AZN", "UTKA", "Andijan Airport", "Andijan", "Uzbekistan", "UZ", "", 40.7277, 72.2940, "airport", 88.0),
        Airport("KSQ", "UTSK", "Karshi Airport", "Karshi", "Uzbekistan", "UZ", "", 38.8336, 65.9215, "airport", 88.0),
        Airport("TMJ", "UTST", "Termez International Airport", "Termez", "Uzbekistan", "UZ", "", 37.2867, 67.3100, "airport", 88.0),
        Airport("NCU", "UTNN", "Nukus Airport", "Nukus", "Uzbekistan", "UZ", "", 42.4884, 59.6233, "airport", 87.0),
        Airport("NAV", "UTSA", "Navoi International Airport", "Navoi", "Uzbekistan", "UZ", "", 40.1172, 65.1708, "airport", 87.0),
        Airport("JED", "OEJN", "King Abdulaziz International Airport", "Jeddah", "Saudi Arabia", "SA", "", 21.6796, 39.1565, "airport", 100.0),
        Airport("MED", "OEMA", "Prince Mohammad bin Abdulaziz International Airport", "Madinah", "Saudi Arabia", "SA", "", 24.5534, 39.7051, "airport", 100.0),
        Airport("RUH", "OERK", "King Khalid International Airport", "Riyadh", "Saudi Arabia", "SA", "", 24.9576, 46.6988, "airport", 96.0),
        Airport("DMM", "OEDF", "King Fahd International Airport", "Dammam", "Saudi Arabia", "SA", "", 26.4712, 49.7979, "airport", 91.0),
        Airport("TIF", "OETF", "Taif International Airport", "Taif", "Saudi Arabia", "SA", "", 21.4834, 40.5443, "airport", 90.0),
        Airport("AHB", "OEAB", "Abha International Airport", "Abha", "Saudi Arabia", "SA", "", 18.2404, 42.6566, "airport", 86.0),
        Airport("GIZ", "OEGN", "Jazan Regional Airport", "Jazan", "Saudi Arabia", "SA", "", 16.9011, 42.5858, "airport", 84.0),
        Airport("IST", "LTFM", "Istanbul Airport", "Istanbul", "Türkiye", "TR", "", 41.2753, 28.7519, "airport", 99.0),
        Airport("SAW", "LTFJ", "Sabiha Gökçen International Airport", "Istanbul", "Türkiye", "TR", "", 40.8986, 29.3092, "airport", 96.0),
        Airport("ESB", "LTAC", "Esenboğa Airport", "Ankara", "Türkiye", "TR", "", 40.1281, 32.9951, "airport", 90.0),
        Airport("DXB", "OMDB", "Dubai International Airport", "Dubai", "United Arab Emirates", "AE", "", 25.2532, 55.3657, "airport", 99.0),
        Airport("DWC", "OMDW", "Al Maktoum International Airport", "Dubai", "United Arab Emirates", "AE", "", 24.8964, 55.1614, "airport", 90.0),
        Airport("SHJ", "OMSJ", "Sharjah International Airport", "Sharjah", "United Arab Emirates", "AE", "", 25.3286, 55.5172, "airport", 92.0),
        Airport("AUH", "OMAA", "Zayed International Airport", "Abu Dhabi", "United Arab Emirates", "AE", "", 24.4330, 54.6511, "airport", 96.0),
        Airport("DOH", "OTHH", "Hamad International Airport", "Doha", "Qatar", "QA", "", 25.2731, 51.6081, "airport", 98.0),
        Airport("KWI", "OKKK", "Kuwait International Airport", "Kuwait City", "Kuwait", "KW", "", 29.2266, 47.9689, "airport", 93.0),
        Airport("BAH", "OBBI", "Bahrain International Airport", "Manama", "Bahrain", "BH", "", 26.2708, 50.6336, "airport", 92.0),
        Airport("MCT", "OOMS", "Muscat International Airport", "Muscat", "Oman", "OM", "", 23.5933, 58.2844, "airport", 93.0),
        Airport("AMM", "OJAI", "Queen Alia International Airport", "Amman", "Jordan", "JO", "", 31.7226, 35.9932, "airport", 91.0),
        Airport("CAI", "HECA", "Cairo International Airport", "Cairo", "Egypt", "EG", "", 30.1219, 31.4056, "airport", 94.0),
        Airport("GYD", "UBBB", "Heydar Aliyev International Airport", "Baku", "Azerbaijan", "AZ", "", 40.4675, 50.0467, "airport", 93.0),
        Airport("TBS", "UGTB", "Tbilisi International Airport", "Tbilisi", "Georgia", "GE", "", 41.6692, 44.9547, "airport", 90.0),
        Airport("ALA", "UAAA", "Almaty International Airport", "Almaty", "Kazakhstan", "KZ", "", 43.3521, 77.0405, "airport", 94.0),
        Airport("NQZ", "UACC", "Nursultan Nazarbayev International Airport", "Astana", "Kazakhstan", "KZ", "", 51.0222, 71.4669, "airport", 91.0),
        Airport("FRU", "UCFM", "Manas International Airport", "Bishkek", "Kyrgyzstan", "KG", "", 43.0613, 74.4776, "airport", 91.0),
        Airport("OSS", "UCFO", "Osh International Airport", "Osh", "Kyrgyzstan", "KG", "", 40.6090, 72.7933, "airport", 87.0),
        Airport("DYU", "UTDD", "Dushanbe International Airport", "Dushanbe", "Tajikistan", "TJ", "", 38.5433, 68.8250, "airport", 91.0),
        Airport("IKA", "OIIE", "Imam Khomeini International Airport", "Tehran", "Iran", "IR", "", 35.4161, 51.1522, "airport", 91.0),
        Airport("ISB", "OPIS", "Islamabad International Airport", "Islamabad", "Pakistan", "PK", "", 33.5490, 72.8257, "airport", 91.0),
        Airport("KHI", "OPKC", "Jinnah International Airport", "Karachi", "Pakistan", "PK", "", 24.9065, 67.1608, "airport", 91.0),
        Airport("DEL", "VIDP", "Indira Gandhi International Airport", "Delhi", "India", "IN", "", 28.5562, 77.1000, "airport", 97.0),
        Airport("BOM", "VABB", "Chhatrapati Shivaji Maharaj International Airport", "Mumbai", "India", "IN", "", 19.0896, 72.8656, "airport", 96.0),
        Airport("DAC", "VGHS", "Hazrat Shahjalal International Airport", "Dhaka", "Bangladesh", "BD", "", 23.8433, 90.3978, "airport", 92.0),
        Airport("KUL", "WMKK", "Kuala Lumpur International Airport", "Kuala Lumpur", "Malaysia", "MY", "", 2.7456, 101.7099, "airport", 96.0),
        Airport("CGK", "WIII", "Soekarno–Hatta International Airport", "Jakarta", "Indonesia", "ID", "", -6.1256, 106.6559, "airport", 96.0),
        Airport("SIN", "WSSS", "Singapore Changi Airport", "Singapore", "Singapore", "SG", "", 1.3644, 103.9915, "airport", 98.0),
        Airport("BKK", "VTBS", "Suvarnabhumi Airport", "Bangkok", "Thailand", "TH", "", 13.6900, 100.7501, "airport", 96.0),
        Airport("LHR", "EGLL", "Heathrow Airport", "London", "United Kingdom", "GB", "", 51.4700, -0.4543, "airport", 99.0),
        Airport("CDG", "LFPG", "Charles de Gaulle Airport", "Paris", "France", "FR", "", 49.0097, 2.5479, "airport", 98.0),
        Airport("FRA", "EDDF", "Frankfurt Airport", "Frankfurt", "Germany", "DE", "", 50.0379, 8.5622, "airport", 98.0),
        Airport("AMS", "EHAM", "Amsterdam Airport Schiphol", "Amsterdam", "Netherlands", "NL", "", 52.3105, 4.7683, "airport", 98.0),
        Airport("JFK", "KJFK", "John F. Kennedy International Airport", "New York", "United States", "US", "", 40.6413, -73.7781, "airport", 98.0),
        Airport("LAX", "KLAX", "Los Angeles International Airport", "Los Angeles", "United States", "US", "", 33.9416, -118.4085, "airport", 98.0),
    ).distinctBy { it.iata.uppercase() }

    fun airport(code: String): Airport? {
        val normalized = code.trim().uppercase()
        return airports.firstOrNull { it.iata.equals(normalized, ignoreCase = true) }
    }
}
