package com.mateyou.duedate.data

import java.util.regex.Pattern

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String
)

object CurrencyConfig {
    val currencies = listOf(
        CurrencyInfo("AED", "د.إ", "United Arab Emirates Dirham"),
        CurrencyInfo("AFN", "؋", "Afghan Afghani"),
        CurrencyInfo("ALL", "L", "Albanian Lek"),
        CurrencyInfo("AMD", "֏", "Armenian Dram"),
        CurrencyInfo("ANG", "ƒ", "Netherlands Antillean Guilder"),
        CurrencyInfo("AOA", "Kz", "Angolan Kwanza"),
        CurrencyInfo("ARS", "$", "Argentine Peso"),
        CurrencyInfo("AUD", "A$", "Australian Dollar"),
        CurrencyInfo("AWG", "ƒ", "Aruban Florin"),
        CurrencyInfo("AZN", "₼", "Azerbaijani Manat"),
        CurrencyInfo("BAM", "KM", "Bosnia-Herzegovina Convertible Mark"),
        CurrencyInfo("BBD", "$", "Barbadian Dollar"),
        CurrencyInfo("BDT", "৳", "Bangladeshi Taka"),
        CurrencyInfo("BGN", "лв", "Bulgarian Lev"),
        CurrencyInfo("BHD", ".د.ب", "Bahraini Dinar"),
        CurrencyInfo("BIF", "FBu", "Burundian Franc"),
        CurrencyInfo("BMD", "$", "Bermudan Dollar"),
        CurrencyInfo("BND", "$", "Brunei Dollar"),
        CurrencyInfo("BOB", "Bs.", "Bolivian Boliviano"),
        CurrencyInfo("BRL", "R$", "Brazilian Real"),
        CurrencyInfo("BSD", "$", "Bahamian Dollar"),
        CurrencyInfo("BTN", "Nu.", "Bhutanese Ngultrum"),
        CurrencyInfo("BWP", "P", "Botswanan Pula"),
        CurrencyInfo("BYN", "Br", "Belarusian Ruble"),
        CurrencyInfo("BZD", "BZ$", "Belize Dollar"),
        CurrencyInfo("CAD", "CA$", "Canadian Dollar"),
        CurrencyInfo("CDF", "FC", "Congolese Franc"),
        CurrencyInfo("CHF", "CHF", "Swiss Franc"),
        CurrencyInfo("CLP", "$", "Chilean Peso"),
        CurrencyInfo("CNY", "¥", "Chinese Yuan"),
        CurrencyInfo("COP", "$", "Colombian Peso"),
        CurrencyInfo("CRC", "₡", "Costa Rican Colón"),
        CurrencyInfo("CUP", "$", "Cuban Peso"),
        CurrencyInfo("CVE", "Esc", "Cape Verdean Escudo"),
        CurrencyInfo("CZK", "Kč", "Czech Koruna"),
        CurrencyInfo("DJF", "Fdj", "Djiboutian Franc"),
        CurrencyInfo("DKK", "kr", "Danish Krone"),
        CurrencyInfo("DOP", "RD$", "Dominican Peso"),
        CurrencyInfo("DZD", "د.ج", "Algerian Dinar"),
        CurrencyInfo("EGP", "E£", "Egyptian Pound"),
        CurrencyInfo("ERN", "Nkf", "Eritrean Nakfa"),
        CurrencyInfo("ETB", "Br", "Ethiopian Birr"),
        CurrencyInfo("EUR", "€", "Euro"),
        CurrencyInfo("FJD", "$", "Fijian Dollar"),
        CurrencyInfo("FKP", "£", "Falkland Islands Pound"),
        CurrencyInfo("GBP", "£", "British Pound"),
        CurrencyInfo("GEL", "₾", "Georgian Lari"),
        CurrencyInfo("GHS", "GH₵", "Ghanaian Cedi"),
        CurrencyInfo("GIP", "£", "Gibraltar Pound"),
        CurrencyInfo("GMD", "D", "Gambian Dalasi"),
        CurrencyInfo("GNF", "FG", "Guinean Franc"),
        CurrencyInfo("GTQ", "Q", "Guatemalan Quetzal"),
        CurrencyInfo("GYD", "$", "Guyanese Dollar"),
        CurrencyInfo("HKD", "HK$", "Hong Kong Dollar"),
        CurrencyInfo("HNL", "L", "Honduran Lempira"),
        CurrencyInfo("HRK", "kn", "Croatian Kuna"),
        CurrencyInfo("HTG", "G", "Haitian Gourde"),
        CurrencyInfo("HUF", "Ft", "Hungarian Forint"),
        CurrencyInfo("IDR", "Rp", "Indonesian Rupiah"),
        CurrencyInfo("ILS", "₪", "Israeli New Shekel"),
        CurrencyInfo("INR", "₹", "Indian Rupee"),
        CurrencyInfo("IQD", "ع.د", "Iraqi Dinar"),
        CurrencyInfo("IRR", "﷼", "Iranian Rial"),
        CurrencyInfo("ISK", "kr", "Icelandic Króna"),
        CurrencyInfo("JMD", "J$", "Jamaican Dollar"),
        CurrencyInfo("JOD", "د.ا", "Jordanian Dinar"),
        CurrencyInfo("JPY", "¥", "Japanese Yen"),
        CurrencyInfo("KES", "KSh", "Kenyan Shilling"),
        CurrencyInfo("KGS", "с", "Kyrgystani Som"),
        CurrencyInfo("KHR", "៛", "Cambodian Riel"),
        CurrencyInfo("KMF", "CF", "Comorian Franc"),
        CurrencyInfo("KPW", "₩", "North Korean Won"),
        CurrencyInfo("KRW", "₩", "South Korean Won"),
        CurrencyInfo("KWD", "د.ك", "Kuwaiti Dinar"),
        CurrencyInfo("KYD", "$", "Cayman Islands Dollar"),
        CurrencyInfo("KZT", "₸", "Kazakhstani Tenge"),
        CurrencyInfo("LAK", "₭", "Laotian Kip"),
        CurrencyInfo("LBP", "ل.ل", "Lebanese Pound"),
        CurrencyInfo("LKR", "Rs", "Sri Lankan Rupee"),
        CurrencyInfo("LRD", "$", "Liberian Dollar"),
        CurrencyInfo("LSL", "L", "Lesotho Loti"),
        CurrencyInfo("LYD", "ل.د", "Libyan Dinar"),
        CurrencyInfo("MAD", "د.م.", "Moroccan Dirham"),
        CurrencyInfo("MDL", "L", "Moldovan Leu"),
        CurrencyInfo("MGA", "Ar", "Malagasy Ariary"),
        CurrencyInfo("MKD", "ден", "Macedonian Denar"),
        CurrencyInfo("MMK", "K", "Myanmar Kyat"),
        CurrencyInfo("MNT", "₮", "Mongolian Tugrik"),
        CurrencyInfo("MOP", "MOP$", "Macanese Pataca"),
        CurrencyInfo("MRU", "UM", "Mauritanian Ouguiya"),
        CurrencyInfo("MUR", "₨", "Mauritian Rupee"),
        CurrencyInfo("MVR", "Rf", "Maldivian Rufiyaa"),
        CurrencyInfo("MWK", "MK", "Malawian Kwacha"),
        CurrencyInfo("MXN", "$", "Mexican Peso"),
        CurrencyInfo("MYR", "RM", "Malaysian Ringgit"),
        CurrencyInfo("MZN", "MT", "Mozambican Metical"),
        CurrencyInfo("NAD", "$", "Namibian Dollar"),
        CurrencyInfo("NGN", "₦", "Nigerian Naira"),
        CurrencyInfo("NIO", "C$", "Nicaraguan Córdoba"),
        CurrencyInfo("NOK", "kr", "Norwegian Krone"),
        CurrencyInfo("NPR", "₨", "Nepalese Rupee"),
        CurrencyInfo("NZD", "$", "New Zealand Dollar"),
        CurrencyInfo("OMR", "ر.ع.", "Omani Rial"),
        CurrencyInfo("PAB", "B/.", "Panamanian Balboa"),
        CurrencyInfo("PEN", "S/.", "Peruvian Sol"),
        CurrencyInfo("PGK", "K", "Papua New Guinean Kina"),
        CurrencyInfo("PHP", "₱", "Philippine Peso"),
        CurrencyInfo("PKR", "₨", "Pakistani Rupee"),
        CurrencyInfo("PLN", "zł", "Polish Zloty"),
        CurrencyInfo("PYG", "Gs", "Paraguayan Guarani"),
        CurrencyInfo("QAR", "ر.ق", "Qatari Rial"),
        CurrencyInfo("RON", "lei", "Romanian Leu"),
        CurrencyInfo("RSD", "дн.", "Serbian Dinar"),
        CurrencyInfo("RUB", "₽", "Russian Ruble"),
        CurrencyInfo("RWF", "RF", "Rwandan Franc"),
        CurrencyInfo("SAR", "SR", "Saudi Riyal"),
        CurrencyInfo("SBD", "$", "Solomon Islands Dollar"),
        CurrencyInfo("SCR", "₨", "Seychellois Rupee"),
        CurrencyInfo("SDG", "ج.س.", "Sudanese Pound"),
        CurrencyInfo("SEK", "kr", "Swedish Krona"),
        CurrencyInfo("SGD", "S$", "Singapore Dollar"),
        CurrencyInfo("SHP", "£", "St. Helena Pound"),
        CurrencyInfo("SLL", "Le", "Sierra Leonean Leone"),
        CurrencyInfo("SOS", "S", "Somali Shilling"),
        CurrencyInfo("SRD", "$", "Surinamese Dollar"),
        CurrencyInfo("STN", "Db", "São Tomé \u0026 Príncipe Dobra"),
        CurrencyInfo("SYP", "£", "Syrian Pound"),
        CurrencyInfo("SZL", "L", "Eswatini Lilangeni"),
        CurrencyInfo("THB", "฿", "Thai Baht"),
        CurrencyInfo("TJS", "SM", "Tajikistani Somoni"),
        CurrencyInfo("TMT", "T", "Turkmenistani Manat"),
        CurrencyInfo("TND", "د.ت", "Tunisian Dinar"),
        CurrencyInfo("TOP", "T$", "Tongan Paʻanga"),
        CurrencyInfo("TRY", "₺", "Turkish Lira"),
        CurrencyInfo("TTD", "TT$", "Trinidad \u0026 Tobago Dollar"),
        CurrencyInfo("TWD", "NT$", "New Taiwan Dollar"),
        CurrencyInfo("TZS", "TSh", "Tanzanian Shilling"),
        CurrencyInfo("UAH", "₴", "Ukrainian Hryvnia"),
        CurrencyInfo("UGX", "USh", "Ugandan Shilling"),
        CurrencyInfo("USD", "$", "US Dollar"),
        CurrencyInfo("UYU", "\$U", "Uruguayan Peso"),
        CurrencyInfo("UZS", "soʻm", "Uzbekistani Som"),
        CurrencyInfo("VED", "Bs.P", "Venezuelan Bolívar"),
        CurrencyInfo("VND", "₫", "Vietnamese Đồng"),
        CurrencyInfo("VUV", "VT", "Vanuatu Vatu"),
        CurrencyInfo("WST", "WS$", "Samoan Tala"),
        CurrencyInfo("XAF", "FCFA", "Central African CFA Franc"),
        CurrencyInfo("XCD", "$", "East Caribbean Dollar"),
        CurrencyInfo("XOF", "CFA", "West African CFA Franc"),
        CurrencyInfo("XPF", "₣", "CFP Franc"),
        CurrencyInfo("YER", "﷼", "Yemeni Rial"),
        CurrencyInfo("ZAR", "R", "South African Rand"),
        CurrencyInfo("ZMW", "ZK", "Zambian Kwacha")
    )

    val allCodes: List<String> = currencies.map { it.code }
    val allSymbols: List<String> = currencies.map { it.symbol }.distinct()

    fun getSymbol(codeOrSymbol: String): String {
        val upper = codeOrSymbol.uppercase()
        // Try match by code
        val matchByCode = currencies.find { it.code.uppercase() == upper }
        if (matchByCode != null) return matchByCode.symbol
        
        // Handle specific variations not in the standard list
        return when (upper) {
            "RS", "RS." -> "₹"
            else -> codeOrSymbol // If it's already a symbol or unknown, return as is
        }
    }

    fun getCurrencyRegex(): String {
        val quotedSymbols = allSymbols.map { Pattern.quote(it) }.toMutableList()
        // Add manual common variations that might not be in the standard list
        quotedSymbols.add("Rs\\.")
        quotedSymbols.add("Rs")

        val combined = (allCodes + quotedSymbols).joinToString("|")
        return "(?:$combined)"
    }
}
