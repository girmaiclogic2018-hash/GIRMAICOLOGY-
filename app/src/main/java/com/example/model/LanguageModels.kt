package com.example.model

enum class LanguageCategory(val label: String) {
    ETHIOPIAN("Ethiopian"),
    AFRICAN("African"),
    GLOBAL("Worldwide")
}

enum class TargetLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val shortBadge: String,
    val sampleGreeting: String,
    val category: LanguageCategory,
    val regionHint: String
) {
    // Ethiopian Local Languages
    AMHARIC("am", "Amharic", "አማርኛ", "AM", "ሰላም! ዛሬ እንዴት ነህ?", LanguageCategory.ETHIOPIAN, "Ethiopia · Federal"),
    AFAAN_OROMO("om", "Afaan Oromo", "Afaan Oromoo", "OM", "Akkam! Nagaa keetii?", LanguageCategory.ETHIOPIAN, "Ethiopia · Oromia"),
    TIGRIGNA("ti", "Tigrigna", "ትግርኛ", "TI", "ሰላም! ከመይ ኣለኻ?", LanguageCategory.ETHIOPIAN, "Ethiopia · Tigray"),
    SOMALI("so", "Somali", "Af-Soomaali", "SO", "Salaan! Sidee tahay maanta?", LanguageCategory.ETHIOPIAN, "Ethiopia · Somali Region"),
    SIDAMA("sid", "Sidama", "Sidaamu Afoo", "SID", "Keere! Maassi keeri nooni?", LanguageCategory.ETHIOPIAN, "Ethiopia · Sidama"),
    WOLAYTTA("wal", "Wolaytta", "Wolayttatto Donaa", "WAL", "Saro! Waayda de'ay?", LanguageCategory.ETHIOPIAN, "Ethiopia · South Ethiopia"),
    AFAR("aa", "Afar", "Qafaraf", "AA", "Salaam! Nagaa maalin?", LanguageCategory.ETHIOPIAN, "Ethiopia · Afar"),
    HADIYYA("hdy", "Hadiyya", "Hadiyyisa", "HDY", "Xummo! Fayyaa he'ootto?", LanguageCategory.ETHIOPIAN, "Ethiopia · Central Ethiopia"),
    GURAGE("sgw", "Gurage", "ጉራጊኛ", "GUR", "ተናስተልኝ! እንዴት ናኸ?", LanguageCategory.ETHIOPIAN, "Ethiopia · Gurage"),
    GAMO("gmv", "Gamo", "Gamotstso", "GMV", "Saro! Lo'o de'ay?", LanguageCategory.ETHIOPIAN, "Ethiopia · Gamo"),
    KAFA("kbr", "Kafa", "Kafinoonoo", "KBR", "Shaahiyoo! Gaawaa beetaa?", LanguageCategory.ETHIOPIAN, "Ethiopia · South West"),
    HARARI("har", "Harari", "ሐረሪ / Gey Sinan", "HAR", "አማን ዋልቲ! ሚስኪን ከይፍ?", LanguageCategory.ETHIOPIAN, "Ethiopia · Harar"),
    SILTE("stv", "Silt'e", "ስልጥኛ", "STV", "ሰላም! ደህና አልክ?", LanguageCategory.ETHIOPIAN, "Ethiopia · Silt'e"),
    GEDEO("drs", "Gedeo", "Gedeuffa", "DRS", "Keere! Fayyaa he'eette?", LanguageCategory.ETHIOPIAN, "Ethiopia · Gedeo"),
    KAMBAATA("ktb", "Kambaata", "Kambaatissaa", "KTB", "Keerii! Fayyaa yoo?", LanguageCategory.ETHIOPIAN, "Ethiopia · Kambaata"),
    NUER("nus", "Nuer", "Thok Naath", "NUS", "Mɛɛlɛ! Yi ba rɔ̱?", LanguageCategory.ETHIOPIAN, "Ethiopia · Gambella"),
    ANUAK("anu", "Anuak", "Dha Anywaa", "ANU", "Mathi! I dii nywɔ?", LanguageCategory.ETHIOPIAN, "Ethiopia · Gambella"),
    BENCH("bcq", "Bench", "Benchon", "BCQ", "Wos! Niyam da?", LanguageCategory.ETHIOPIAN, "Ethiopia · Bench Sheko"),
    KONSO("kxc", "Konso", "Afa Xonso", "KXC", "Nagaaya! Fayyaa jirtaa?", LanguageCategory.ETHIOPIAN, "Ethiopia · Konso"),
    BERTA("wti", "Berta", "Beni Shangul", "WTI", "Salaam! Maari na?", LanguageCategory.ETHIOPIAN, "Ethiopia · Benishangul"),
    GUMUZ("guk", "Gumuz", "Bega", "GUK", "Kooma! Ayi ma?", LanguageCategory.ETHIOPIAN, "Ethiopia · Benishangul"),
    AWNGI("awn", "Awngi", "አውኛ", "AWN", "አማን! ድሓን ቲ?", LanguageCategory.ETHIOPIAN, "Ethiopia · Agew / Awi"),
    DAWRO("dwr", "Dawro", "Dawrotsuwaa", "DWR", "Saro! Lo'o de'ay?", LanguageCategory.ETHIOPIAN, "Ethiopia · Dawro"),
    GEEZ("gez", "Ge'ez", "ግዕዝ", "GEZ", "ሰላም ለከ! እፎ ሀሎከ?", LanguageCategory.ETHIOPIAN, "Ethiopia · Classical"),

    // Essential African Languages
    SWAHILI("sw", "Swahili", "Kiswahili", "SW", "Hujambo! Habari yako leo?", LanguageCategory.AFRICAN, "East & Central Africa"),
    YORUBA("yo", "Yoruba", "Èdè Yorùbá", "YO", "Ẹ káàárọ̀! Báwo ni lónìí?", LanguageCategory.AFRICAN, "West Africa · Nigeria"),
    HAUSA("ha", "Hausa", "Harshen Hausa", "HA", "Sannu! Yaya aiki da gida?", LanguageCategory.AFRICAN, "West & Sahel Africa"),
    IGBO("ig", "Igbo", "Asụsụ Igbo", "IG", "Nnọọ! Kedụ ka ị mere taa?", LanguageCategory.AFRICAN, "West Africa · Nigeria"),
    ZULU("zu", "Zulu", "isiZulu", "ZU", "Sawubona! Unjani namhlanje?", LanguageCategory.AFRICAN, "Southern Africa"),
    XHOSA("xh", "Xhosa", "isiXhosa", "XH", "Molo! Unjani namhlanje?", LanguageCategory.AFRICAN, "Southern Africa"),
    SHONA("sn", "Shona", "chiShona", "SN", "Mhoro! Makadii henyu nhasi?", LanguageCategory.AFRICAN, "Southern Africa · Zimbabwe"),
    KINYARWANDA("rw", "Kinyarwanda", "Ikinyarwanda", "RW", "Muraho! Amakuru yawe uyu munsi?", LanguageCategory.AFRICAN, "East Africa · Rwanda"),
    LINGALA("ln", "Lingala", "Lingála", "LN", "Mbote! Sango nini lelo?", LanguageCategory.AFRICAN, "Central Africa · DRC"),
    WOLOF("wo", "Wolof", "Wolof", "WO", "Salaamalekum! Nanga def tey?", LanguageCategory.AFRICAN, "West Africa · Senegal"),
    TWI("ak", "Twi (Akan)", "Twi / Akan", "TW", "Akwaaba! Ɛte sɛn nnɛ?", LanguageCategory.AFRICAN, "West Africa · Ghana"),
    LUGANDA("lg", "Luganda", "Oluganda", "LG", "Osiibye otya! Gyebale ko?", LanguageCategory.AFRICAN, "East Africa · Uganda"),
    CHICHEWA("ny", "Chichewa", "ChiCheŵa", "NY", "Moni! Muli bwanji lero?", LanguageCategory.AFRICAN, "Southern & East Africa"),
    SESOTHO("st", "Sesotho", "Sesotho", "ST", "Dumela! O phela jwang kajeno?", LanguageCategory.AFRICAN, "Southern Africa"),

    // Worldwide High-Volume Languages
    ENGLISH("en", "English", "English", "EN", "Hello! Ready to practice today?", LanguageCategory.GLOBAL, "Worldwide"),
    MANDARIN("zh", "Mandarin", "普通话", "ZH", "你好！你今天怎么样？", LanguageCategory.GLOBAL, "East Asia · Global"),
    SPANISH("es", "Spanish", "Español", "ES", "¡Hola! ¿Cómo estás hoy?", LanguageCategory.GLOBAL, "Americas & Europe"),
    HINDI("hi", "Hindi", "हिन्दी", "HI", "नमस्ते! आज आप कैसे हैं?", LanguageCategory.GLOBAL, "South Asia"),
    ARABIC("ar", "Arabic", "العربية", "AR", "مرحباً! كيف حالك اليوم؟", LanguageCategory.GLOBAL, "Middle East & North Africa"),
    FRENCH("fr", "French", "Français", "FR", "Bonjour ! Comment ça va ?", LanguageCategory.GLOBAL, "Europe, Africa & Global"),
    PORTUGUESE("pt", "Portuguese", "Português", "PT", "Olá! Tudo bem com você?", LanguageCategory.GLOBAL, "Americas, Europe & Africa"),
    BENGALI("bn", "Bengali", "বাংলা", "BN", "নমস্কার! আজ কেমন আছেন?", LanguageCategory.GLOBAL, "South Asia"),
    RUSSIAN("ru", "Russian", "Русский", "RU", "Привет! Как дела сегодня?", LanguageCategory.GLOBAL, "Eastern Europe & Eurasia"),
    JAPANESE("ja", "Japanese", "日本語", "JA", "こんにちは！元気ですか？", LanguageCategory.GLOBAL, "East Asia"),
    GERMAN("de", "German", "Deutsch", "DE", "Hallo! Wie geht es dir?", LanguageCategory.GLOBAL, "Central Europe"),
    KOREAN("ko", "Korean", "한국어", "KO", "안녕하세요! 오늘 어때요?", LanguageCategory.GLOBAL, "East Asia"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia", "ID", "Halo! Apa kabar hari ini?", LanguageCategory.GLOBAL, "Southeast Asia"),
    TURKISH("tr", "Turkish", "Türkçe", "TR", "Merhaba! Bugün nasılsın?", LanguageCategory.GLOBAL, "Eurasia"),
    ITALIAN("it", "Italian", "Italiano", "IT", "Ciao! Come stai oggi?", LanguageCategory.GLOBAL, "Europe"),
    URDU("ur", "Urdu", "اردو", "UR", "السلام علیکم! آج آپ کیسے ہیں؟", LanguageCategory.GLOBAL, "South Asia"),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt", "VI", "Xin chào! Hôm nay bạn thế nào?", LanguageCategory.GLOBAL, "Southeast Asia");

    fun matchesQuery(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return displayName.contains(q, ignoreCase = true) ||
            nativeName.contains(q, ignoreCase = true) ||
            regionHint.contains(q, ignoreCase = true) ||
            code.equals(q, ignoreCase = true)
    }
}

enum class ProficiencyLevel(
    val label: String,
    val cefrTag: String,
    val guidance: String
) {
    BEGINNER(
        label = "Beginner",
        cefrTag = "A1–A2",
        guidance = "Speak slowly with simple everyday words, short sentences, and gentle encouragement."
    ),
    INTERMEDIATE(
        label = "Intermediate",
        cefrTag = "B1–B2",
        guidance = "Speak at a natural conversational pace using common idioms and follow-up questions."
    ),
    ADVANCED(
        label = "Advanced",
        cefrTag = "C1+",
        guidance = "Speak fluently with nuanced vocabulary, cultural idioms, and subtle grammar refinements."
    )
}

enum class PartnerVoice(
    val apiName: String,
    val displayName: String,
    val persona: String
) {
    AOEDE("Aoede", "Aoede", "Warm & expressive"),
    PUCK("Puck", "Puck", "Upbeat & playful"),
    KORE("Kore", "Kore", "Calm & articulate"),
    CHARON("Charon", "Charon", "Deep & steady"),
    FENRIR("Fenrir", "Fenrir", "Energetic & crisp")
}

enum class ThemePreference(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Studio"),
    DARK("Dark Studio")
}

data class PracticeScenario(
    val id: String,
    val title: String,
    val subtitle: String,
    val roleplayPrompt: String,
    val iconKey: String
)

object PresetScenarios {
    val all: List<PracticeScenario> = listOf(
        PracticeScenario(
            id = "free_chat",
            title = "Free Conversation",
            subtitle = "Casual daily chat about your day, hobbies, and ideas",
            roleplayPrompt = "Act as a friendly native-speaking conversation partner having a relaxed everyday chat.",
            iconKey = "chat"
        ),
        PracticeScenario(
            id = "cafe_ordering",
            title = "Café & Dining",
            subtitle = "Order coffee, pastries, ask about specials, and pay the bill",
            roleplayPrompt = "Roleplay as a welcoming barista and server at a lively local neighborhood café.",
            iconKey = "coffee"
        ),
        PracticeScenario(
            id = "travel_transit",
            title = "Travel & Airport",
            subtitle = "Ask for directions, check in, or navigate train stations",
            roleplayPrompt = "Roleplay as a helpful local transit agent and hotel concierge assisting a traveler.",
            iconKey = "flight"
        ),
        PracticeScenario(
            id = "career_interview",
            title = "Job Interview",
            subtitle = "Introduce your experience and answer professional questions",
            roleplayPrompt = "Roleplay as a supportive hiring manager conducting a friendly introductory interview.",
            iconKey = "work"
        ),
        PracticeScenario(
            id = "market_shopping",
            title = "Local Market",
            subtitle = "Browse fresh produce, ask prices, and compare local goods",
            roleplayPrompt = "Roleplay as an enthusiastic artisan vendor at an open-air weekend market.",
            iconKey = "store"
        ),
        PracticeScenario(
            id = "making_friends",
            title = "Meet & Connect",
            subtitle = "Break the ice at a meetup, talk about music, food, and films",
            roleplayPrompt = "Roleplay as a curious new acquaintance met at a local cultural event.",
            iconKey = "people"
        )
    )
}

data class TranscriptEntry(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
)

sealed interface LiveSessionStatus {
    data object Idle : LiveSessionStatus
    data object Connecting : LiveSessionStatus
    data object Listening : LiveSessionStatus
    data object PartnerSpeaking : LiveSessionStatus
    data object Paused : LiveSessionStatus
    data class Error(val message: String) : LiveSessionStatus
}

data class SuggestedPhrase(
    val phrase: String,
    val translation: String,
    val note: String
)

data class SessionReview(
    val fluencySummary: String,
    val strengths: List<String>,
    val grammarTip: String,
    val suggestedPhrases: List<SuggestedPhrase>
)

data class PhraseInsight(
    val literalMeaning: String,
    val pronunciationGuide: String,
    val exampleSentence: String,
    val exampleTranslation: String,
    val culturalTip: String
)

enum class TranslationTone(val label: String, val instruction: String) {
    STANDARD("Balanced", "Natural, clear, everyday standard phrasing."),
    FORMAL("Formal", "Respectful, polite, official register suitable for elders, hospitality, and formal settings."),
    CASUAL("Casual", "Warm, conversational, friendly spoken phrasing."),
    BUSINESS("Professional", "Crisp, articulate professional and academic wording.")
}

data class TranslationCoachResult(
    val detectedSourceLanguage: String,
    val translatedText: String,
    val romanization: String,
    val partOfSpeech: String = "",
    val alternativeTranslations: List<String> = emptyList(),
    val phoneticGuide: String = "",
    val politeVariant: String = "",
    val casualVariant: String = "",
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val culturalNote: String = "",
    val extractedSourceText: String = ""
)

data class WritingPolishResult(
    val correctedText: String,
    val nativeTranslation: String,
    val romanization: String = "",
    val toneSummary: String = "",
    val improvements: List<String> = emptyList(),
    val alternativeVersion: String = ""
)

data class PracticeQuestion(
    val prompt: String,
    val targetExpression: String,
    val romanization: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class PracticeDrillSet(
    val languageName: String,
    val topic: String,
    val questions: List<PracticeQuestion>
)
