package com.mateyou.duedate

import com.mateyou.duedate.data.BankEntity

object BankConfig {
    private val SENDER_TO_BANK = mapOf(
        "HDFCBK" to "HDFC",
        "IDFCFB" to "IDFC",
        "UTKSPR" to "Utkarsh",
        "BOBONE" to "BOB",
        "BOBCRD" to "BOB",
        "SSFBNK" to "Suryoday",
        "ICICIB" to "ICICI",
        "SBICRD" to "SBI",
        "AXISBK" to "Axis",
        "KOTAKB" to "Kotak",
        "RBLBNK" to "RBL",
        "CITIBK" to "Citi",
        "Citi" to "Citi",
        "Citibank" to "Citi",
        "SLCEIT" to "Slice",
        "JKBANK" to "J&K Bank",
        "UCOBNK" to "UCO",
        "SIBSMS" to "SIB",
        "FEDBNK" to "Federal",
        "CANBNK" to "Canara",
        "HSBCBK" to "HSBC",
        "HSBC" to "HSBC",
        "INDUSB" to "IndusInd",
        "AUBANK" to "AU Bank",
        "PNBSMS" to "PNB",
        "UBISMS" to "UBI",
        "YESBNK" to "Yes Bank",
        "CUBBK" to "CUB",
        "KBLBK" to "Karnataka Bank",
        "APGBNK" to "Rural Bank",
        "AMEXIN" to "AMEX",
        "American Express" to "AMEX",
        "JUPITR" to "Jupiter",
        "ABBANK" to "AB Bank",
        "AB Bank" to "AB Bank",
        "ANZBNK" to "ANZ",
        "ANZ" to "ANZ",
        "BOAMER" to "Bank of America",
        "Bank of America" to "Bank of America",
        "SCOTIA" to "Scotia",
        "BKCHNA" to "Bank of China",
        "Bank of China" to "Bank of China",
        "BARCBK" to "Barclays",
        "Barclays" to "Barclays",
        "RABOBK" to "Rabobank",
        "Rabobank" to "Rabobank",
        "CRAGRI" to "Credit Agricole",
        "Credit Agricole" to "Credit Agricole",
        "DBSBNK" to "DBS",
        "DBS" to "DBS",
        "DBANKI" to "Deutsche Bank",
        "Deutsche Bank" to "Deutsche Bank",
        "FIRSTR" to "FirstRand",
        "FirstRand" to "FirstRand",
        "ICBCBK" to "ICBC",
        "ICBC" to "ICBC",
        "CHASEB" to "Chase",
        "Chase" to "Chase",
        "VTBBANK" to "VTB",
        "VTB" to "VTB",
        "MIZUHO" to "Mizuho",
        "MUFGBK" to "MUFG",
        "MUFG" to "MUFG",
        "NTWSTB" to "NatWest",
        "NATWEST" to "NatWest",
        "NatWest" to "NatWest",
        "MAYBNK" to "Maybank",
        "Maybank" to "Maybank",
        "QNBANK" to "QNB",
        "QNB" to "QNB",
        "SBERBK" to "Sberbank",
        "Sberbank" to "Sberbank",
        "STCHRT" to "StanChart",
        "Standard Chartered" to "StanChart",
        "SMBCBK" to "SMBC",
        "SMBC" to "SMBC",
        "UOBANK" to "UOB",
        "UOB" to "UOB",
        "UBSBNK" to "UBS",
        "UBS" to "UBS",
        "WOORIB" to "Woori",
        "Woori" to "Woori",
        "LLOYDS" to "Lloyds",
        "Lloyds" to "Lloyds",
        "NWIDE" to "Nationwide",
        "Nationwide" to "Nationwide",
        "HALIFX" to "Halifax",
        "Halifax" to "Halifax",
        "RBS" to "RBS",
        "SANTAN" to "Santander",
        "Santander" to "Santander",
        "REVOLT" to "Revolut",
        "Revolut" to "Revolut",
        "MONZO" to "Monzo",
        "STALIN" to "Starling",
        "Starling" to "Starling",
        "WISE" to "Wise",
        "WELLSF" to "Wells Fargo",
        "Wells Fargo" to "Wells Fargo",
        "PNCBK" to "PNC",
        "PNC" to "PNC",
        "TRUIST" to "Truist",
        "CAPONE" to "Capital One",
        "Capital One" to "Capital One",
        "TDBANK" to "TD Bank",
        "TD Bank" to "TD Bank",
        "KLARNA" to "Klarna",
        "CHIME" to "Chime",
        "SOFIBK" to "SoFi",
        "SoFi" to "SoFi",
        "CURVE" to "Curve"
    )

    fun getDefaultBankEntities(): List<BankEntity> {
        val banks = SENDER_TO_BANK.entries
            .filter { it.value != "Trial" && it.value != "TrialTwo" }
            .groupBy({ it.value }, { it.key })
            .map { (name, senders) ->
                BankEntity(
                    name = name,
                    builtInName = name,
                    isBrandedCard = false,
                    senderIds = senders.joinToString(", "),
                    aliases = "",
                    isBuiltIn = true,
                    svgLogo = null
                )
            }

        val cards = listOf(
            "Kiwi" to "Kiwi",
            "OneCard" to "One card, OneCard",
            "Scapia" to "Scapia",
            "SuperCard" to "Super card, Supercard",
            "Stable Money" to "Stable Money, StableMoney",
            "PhonePe" to "PhonePe"
        ).map { (name, aliases) ->
            BankEntity(
                name = name,
                builtInName = name,
                isBrandedCard = true,
                senderIds = "",
                aliases = aliases,
                isBuiltIn = true,
                svgLogo = null
            )
        }

        return (banks + cards).sortedBy { it.name.lowercase() }
    }

    fun getAllBanks(): List<String> {
        return SENDER_TO_BANK.values.distinct()
            .filter { it != "Trial" && it != "TrialTwo" }
            .sortedWith(compareBy { it.lowercase() })
    }

    fun getSpecialCards(): List<String> {
        return listOf("Kiwi", "OneCard", "Scapia", "SuperCard", "Stable Money", "PhonePe")
            .sortedWith(compareBy { it.lowercase() })
    }

    fun getBankName(sender: String): String {
        val upperSender = sender.uppercase()
        for ((key, name) in SENDER_TO_BANK) {
            if (upperSender.contains(key.uppercase())) return name
        }
        if (upperSender.contains("GRAMIN") || upperSender.contains("GRAMEENA") || upperSender.contains("GRAMEEN") || upperSender.contains("GRAMA") || upperSender.contains("RURAL")) {
            return "Rural Bank"
        }
        return "Bank"
    }

    fun isBankSender(sender: String): Boolean {
        val upperSender = sender.uppercase()
        if (upperSender.contains("IDFC") || upperSender.contains("HDFC")) return true
        if (upperSender.contains("GRAMIN") || upperSender.contains("GRAMEEN") || upperSender.contains("GRAMA") || upperSender.contains("RURAL")) return true
        return SENDER_TO_BANK.keys.any { upperSender.contains(it.uppercase()) } || sender.contains("07125")
    }

    fun getBankLogo(bankName: String, cardName: String? = null): Int? {
        val lowerCard = cardName?.lowercase() ?: ""
        if (lowerCard.contains("phonepe")) return R.drawable.ic_bank_phonepe
        if (lowerCard.contains("kiwi")) return R.drawable.ic_bank_kiwi
        if (lowerCard.contains("scapia")) return R.drawable.ic_bank_scapia
        if (lowerCard.contains("supercard") || lowerCard.contains("super card")) return R.drawable.ic_bank_supercard
        if (lowerCard.contains("onecard") || lowerCard.contains("one card")) return R.drawable.ic_bank_onecard
        if (lowerCard.contains("stable money") || lowerCard.contains("stablemoney")) return R.drawable.ic_bank_stablemoney

        val lowerBank = bankName.lowercase()
        return when {
            lowerBank.contains("phonepe") -> R.drawable.ic_bank_phonepe
            lowerBank.contains("hdfc") -> R.drawable.ic_bank_hdfc
            lowerBank.contains("idfc") -> R.drawable.ic_bank_idfc
            lowerBank.contains("kotak") -> R.drawable.ic_bank_kotak
            lowerBank.contains("sbi") -> R.drawable.ic_bank_sbi
            lowerBank.contains("slice") -> R.drawable.ic_bank_slice
            lowerBank.contains("jupiter") -> R.drawable.ic_bank_jupiter
            lowerBank.contains("axis") -> R.drawable.ic_bank_axis
            lowerBank.contains("icici") -> R.drawable.ic_bank_icici
            lowerBank.contains("rabobank") -> R.drawable.ic_bank_rabobank
            lowerBank.contains("bob") || lowerBank.contains("baroda") -> R.drawable.ic_bank_bob
            lowerBank.contains("canara") -> R.drawable.ic_bank_canara
            lowerBank.contains("amex") || lowerBank.contains("american") -> R.drawable.ic_bank_amex
            lowerBank.contains("citi") -> R.drawable.ic_bank_citi
            lowerBank.contains("hsbc") -> R.drawable.ic_bank_hsbc
            lowerBank.contains("union") || lowerBank.contains("ubi") -> R.drawable.ic_bank_ubi
            lowerBank.contains("pnb") || lowerBank.contains("punjab") -> R.drawable.ic_bank_pnb
            lowerBank.contains("indus") -> R.drawable.ic_bank_indusind
            lowerBank.contains("yes") -> R.drawable.ic_bank_yes
            lowerBank.contains("rbl") -> R.drawable.ic_bank_rbl
            lowerBank.contains("au ") || lowerBank.contains("aubank") -> R.drawable.ic_bank_aubank
            lowerBank.contains("j&k") || lowerBank.contains("jammu") -> R.drawable.ic_bank_jk
            lowerBank.contains("uco") -> R.drawable.ic_bank_uco
            lowerBank.contains("sib") || lowerBank.contains("south indian") -> R.drawable.ic_bank_sib
            lowerBank.contains("federal") -> R.drawable.ic_bank_federal
            lowerBank.contains("cub") || lowerBank.contains("city union") -> R.drawable.ic_bank_cub
            lowerBank.contains("karnataka") -> R.drawable.ic_bank_kbl
            lowerBank.contains("rural") || lowerBank.contains("gramin") || lowerBank.contains("grameen") || lowerBank.contains("grama") -> R.drawable.ic_bank_rural
            lowerBank.contains("utkarsh") -> R.drawable.ic_bank_utkarsh
            lowerBank.contains("suryoday") -> R.drawable.ic_bank_suryoday
            lowerBank.contains("ab bank") -> R.drawable.ic_bank_ab
            lowerBank.contains("anz") -> R.drawable.ic_bank_anz
            lowerBank.contains("america") -> R.drawable.ic_bank_bofa
            lowerBank.contains("scotia") -> R.drawable.ic_bank_scotia
            lowerBank.contains("china") -> R.drawable.ic_bank_china
            lowerBank.contains("barclays") -> R.drawable.ic_bank_barclays
            lowerBank.contains("credit agricole") -> R.drawable.ic_bank_credit_agricole
            lowerBank.contains("natwest") -> R.drawable.ic_bank_natwest
            lowerBank.contains("maybank") -> R.drawable.ic_bank_maybank
            lowerBank.contains("qnb") -> R.drawable.ic_bank_qnb
            lowerBank.contains("sberbank") -> R.drawable.ic_bank_sberbank
            lowerBank.contains("stanchart") -> R.drawable.ic_bank_sc
            lowerBank.contains("smbc") -> R.drawable.ic_bank_smbc
            lowerBank.contains("uob") -> R.drawable.ic_bank_uob
            lowerBank.contains("ubs") -> R.drawable.ic_bank_ubs
            lowerBank.contains("woori") -> R.drawable.ic_bank_woori
            lowerBank.contains("dbs") -> R.drawable.ic_bank_dbs
            lowerBank.contains("deutsche") -> R.drawable.ic_bank_deutsche
            lowerBank.contains("firstrand") -> R.drawable.ic_bank_firstrand
            lowerBank.contains("icbc") -> R.drawable.ic_bank_icbc
            lowerBank.contains("chase") -> R.drawable.ic_bank_chase
            lowerBank.contains("vtb") -> R.drawable.ic_bank_vtb
            lowerBank.contains("mizuho") -> R.drawable.ic_bank_mizuho
            lowerBank.contains("mufg") -> R.drawable.ic_bank_mufg
            lowerBank.contains("lloyds") -> R.drawable.ic_bank_lloyds
            lowerBank.contains("nationwide") -> R.drawable.ic_bank_nationwide
            lowerBank.contains("halifax") -> R.drawable.ic_bank_halifax
            lowerBank.contains("rbs") -> R.drawable.ic_bank_rbs
            lowerBank.contains("santander") -> R.drawable.ic_bank_santander
            lowerBank.contains("revolut") -> R.drawable.ic_bank_revolut
            lowerBank.contains("monzo") -> R.drawable.ic_bank_monzo
            lowerBank.contains("starling") -> R.drawable.ic_bank_starling
            lowerBank.contains("wise") -> R.drawable.ic_bank_wise
            lowerBank.contains("wells fargo") -> R.drawable.ic_bank_wellsfargo
            lowerBank.contains("pnc") -> R.drawable.ic_bank_pnc
            lowerBank.contains("truist") -> R.drawable.ic_bank_truist
            lowerBank.contains("capital one") -> R.drawable.ic_bank_capitalone
            lowerBank.contains("td bank") -> R.drawable.ic_bank_tdbank
            lowerBank.contains("klarna") -> R.drawable.ic_bank_klarna
            lowerBank.contains("chime") -> R.drawable.ic_bank_chime
            lowerBank.contains("sofi") -> R.drawable.ic_bank_sofi
            lowerBank.contains("curve") -> R.drawable.ic_bank_curve
            else -> null
        }
    }
}
