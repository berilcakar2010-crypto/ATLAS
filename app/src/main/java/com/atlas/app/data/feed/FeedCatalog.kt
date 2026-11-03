package com.atlas.app.data.feed

/** Kaynak türü: okuyucuya "bu bilgiye ne kadar ve nasıl güvenmeliyim" ipucu verir. */
enum class SourceKind(val label: String) {
    PRIMARY("Birincil / kurumsal"),
    PEER("Hakemli"),
    PREPRINT("Ön baskı · hakemsiz"),
    JOURNALISM("Gazetecilik"),
    OPINION("Yorum / analiz"),
    ;

    companion object {
        fun fromName(name: String): SourceKind = entries.firstOrNull { it.name == name } ?: JOURNALISM
    }
}

enum class Category(val label: String) {
    SCIENCE("Bilim"),
    NEURO("Hesaplamalı nörobilim"),
    PHYSICS("Fizik"),
    COSMOLOGY("Kozmoloji"),
    BIOLOGY("Biyoloji"),
    MATH("Matematik"),
    TECH("Teknoloji"),
    AI("Yapay zekâ"),
    SUSTAINABILITY("Sürdürülebilirlik"),
    CLIMATE("İklim ve çevre"),
    POLITICS("Siyaset ve uluslararası ilişkiler"),
    ECONOMICS("Ekonomi"),
    PHILOSOPHY("Felsefe"),
    LITERATURE("Edebiyat"),
    SOCIOLOGY("Sosyoloji ve toplum"),
    ;

    companion object {
        fun fromName(name: String): Category = entries.firstOrNull { it.name == name } ?: SCIENCE
    }
}

/**
 * Bir RSS/Atom kaynağı.
 * [mixed] = true ise kaynak birden çok konuyu kapsar; her haber anahtar kelimelerle yeniden sınıflandırılır.
 */
data class FeedSpec(
    val id: String,
    val name: String,
    val url: String,
    val category: Category,
    val kind: SourceKind,
    val language: String = "en",
    val mixed: Boolean = false,
)

/**
 * Başlangıç kaynak listesi. Adresler zamanla değişebilir; her kaynağın son durumu uygulamada
 * (Çevrimdışı Arşiv) görünür ve kaynaklar Ayarlar'dan kapatılabilir.
 */
object FeedCatalog {
    val all: List<FeedSpec> = listOf(
        // Genel bilim
        FeedSpec("nature", "Nature", url = "https://www.nature.com/nature.rss", category = Category.SCIENCE, kind = SourceKind.PEER, mixed = true),
        FeedSpec("sciencenews", "Science News", url = "https://www.sciencenews.org/feed", category = Category.SCIENCE, kind = SourceKind.JOURNALISM, mixed = true),
        FeedSpec("physorg", "Phys.org", url = "https://phys.org/rss-feed/", category = Category.SCIENCE, kind = SourceKind.JOURNALISM, mixed = true),
        FeedSpec("newscientist", "New Scientist", url = "https://www.newscientist.com/feed/home/", category = Category.SCIENCE, kind = SourceKind.JOURNALISM, mixed = true),
        FeedSpec("sciam", "Scientific American", url = "https://www.scientificamerican.com/platform/syndication/rss/", category = Category.SCIENCE, kind = SourceKind.JOURNALISM, mixed = true),
        FeedSpec("quanta", "Quanta Magazine", url = "https://www.quantamagazine.org/feed/", category = Category.MATH, kind = SourceKind.JOURNALISM, mixed = true),
        FeedSpec("retractionwatch", "Retraction Watch", url = "https://retractionwatch.com/feed/", category = Category.SCIENCE, kind = SourceKind.JOURNALISM),

        // Nörobilim
        FeedSpec("transmitter", "The Transmitter", url = "https://www.thetransmitter.org/feed/", category = Category.NEURO, kind = SourceKind.JOURNALISM),
        FeedSpec("elife", "eLife", url = "https://elifesciences.org/rss/recent.xml", category = Category.NEURO, kind = SourceKind.PEER, mixed = true),
        FeedSpec("ploscb", "PLOS Computational Biology", url = "https://journals.plos.org/ploscompbiol/feed/atom", category = Category.NEURO, kind = SourceKind.PEER, mixed = true),
        FeedSpec("biorxiv-neuro", "bioRxiv · Neuroscience", url = "https://connect.biorxiv.org/biorxiv_xml.php?subject=neuroscience", category = Category.NEURO, kind = SourceKind.PREPRINT),

        // Fizik, kozmoloji
        FeedSpec("physicsworld", "Physics World", url = "https://physicsworld.com/feed/", category = Category.PHYSICS, kind = SourceKind.JOURNALISM),
        FeedSpec("apsphysics", "APS Physics", url = "https://feeds.aps.org/rss/recent/physics.xml", category = Category.PHYSICS, kind = SourceKind.PEER),
        FeedSpec("nasa", "NASA", url = "https://www.nasa.gov/feed/", category = Category.COSMOLOGY, kind = SourceKind.PRIMARY),
        FeedSpec("spacecom", "Space.com", url = "https://www.space.com/feeds/all", category = Category.COSMOLOGY, kind = SourceKind.JOURNALISM),
        FeedSpec("esa", "ESA · Space Science", url = "https://www.esa.int/rssfeed/Our_Activities/Space_Science", category = Category.COSMOLOGY, kind = SourceKind.PRIMARY),

        // Biyoloji ve sağlık
        FeedSpec("who", "WHO", url = "https://www.who.int/rss-feeds/news-english.xml", category = Category.BIOLOGY, kind = SourceKind.PRIMARY),

        // Teknoloji ve yapay zekâ
        FeedSpec("techreview", "MIT Technology Review", url = "https://www.technologyreview.com/feed/", category = Category.TECH, kind = SourceKind.JOURNALISM, mixed = true),
        FeedSpec("ieeespectrum", "IEEE Spectrum", url = "https://spectrum.ieee.org/feeds/feed.rss", category = Category.TECH, kind = SourceKind.JOURNALISM, mixed = true),
        FeedSpec("arstechnica", "Ars Technica", url = "https://feeds.arstechnica.com/arstechnica/index", category = Category.TECH, kind = SourceKind.JOURNALISM, mixed = true),
        FeedSpec("wired", "WIRED", url = "https://www.wired.com/feed/rss", category = Category.TECH, kind = SourceKind.JOURNALISM, mixed = true),
        FeedSpec("deepmind", "Google DeepMind", url = "https://deepmind.google/blog/rss.xml", category = Category.AI, kind = SourceKind.PRIMARY),
        FeedSpec("openai", "OpenAI", url = "https://openai.com/news/rss.xml", category = Category.AI, kind = SourceKind.PRIMARY),
        FeedSpec("huggingface", "Hugging Face Blog", url = "https://huggingface.co/blog/feed.xml", category = Category.AI, kind = SourceKind.PRIMARY),

        // İklim, çevre, sürdürülebilirlik
        FeedSpec("carbonbrief", "Carbon Brief", url = "https://www.carbonbrief.org/feed/", category = Category.CLIMATE, kind = SourceKind.JOURNALISM),
        FeedSpec("yale360", "Yale Environment 360", url = "https://e360.yale.edu/feed.xml", category = Category.CLIMATE, kind = SourceKind.JOURNALISM),
        FeedSpec("mongabay", "Mongabay", url = "https://news.mongabay.com/feed/", category = Category.CLIMATE, kind = SourceKind.JOURNALISM),
        FeedSpec("insideclimate", "Inside Climate News", url = "https://insideclimatenews.org/feed/", category = Category.CLIMATE, kind = SourceKind.JOURNALISM),
        FeedSpec("guardian-env", "The Guardian · Environment", url = "https://www.theguardian.com/environment/rss", category = Category.CLIMATE, kind = SourceKind.JOURNALISM),
        FeedSpec("owid", "Our World in Data", url = "https://ourworldindata.org/atom.xml", category = Category.SUSTAINABILITY, kind = SourceKind.PRIMARY),

        // Siyaset ve uluslararası ilişkiler
        FeedSpec("bbc-world", "BBC News · World", url = "https://feeds.bbci.co.uk/news/world/rss.xml", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("aljazeera", "Al Jazeera", url = "https://www.aljazeera.com/xml/rss/all.xml", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("unnews", "UN News", url = "https://news.un.org/feed/subscribe/en/news/all/rss.xml", category = Category.POLITICS, kind = SourceKind.PRIMARY),
        FeedSpec("foreignpolicy", "Foreign Policy", url = "https://foreignpolicy.com/feed/", category = Category.POLITICS, kind = SourceKind.OPINION),
        FeedSpec("foreignaffairs", "Foreign Affairs", url = "https://www.foreignaffairs.com/rss.xml", category = Category.POLITICS, kind = SourceKind.OPINION),
        FeedSpec("crisisgroup", "International Crisis Group", url = "https://www.crisisgroup.org/rss.xml", category = Category.POLITICS, kind = SourceKind.PRIMARY),
        FeedSpec("globalvoices", "Global Voices", url = "https://globalvoices.org/feed/", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("bbc-turkce", "BBC Türkçe", url = "https://feeds.bbci.co.uk/turkce/rss.xml", category = Category.POLITICS, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("medyascope", "Medyascope", url = "https://medyascope.tv/feed/", category = Category.POLITICS, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("dailysabah", "Daily Sabah", url = "https://www.dailysabah.com/rssFeed/turkey", category = Category.POLITICS, kind = SourceKind.JOURNALISM),

        // Ekonomi
        FeedSpec("projectsyndicate", "Project Syndicate", url = "https://www.project-syndicate.org/rss", category = Category.ECONOMICS, kind = SourceKind.OPINION, mixed = true),
        FeedSpec("voxeu", "VoxEU / CEPR", url = "https://cepr.org/rss/vox-content", category = Category.ECONOMICS, kind = SourceKind.OPINION),

        // Felsefe
        FeedSpec("aeon", "Aeon", url = "https://aeon.co/feed.rss", category = Category.PHILOSOPHY, kind = SourceKind.OPINION, mixed = true),
        FeedSpec("dailynous", "Daily Nous", url = "https://dailynous.com/feed/", category = Category.PHILOSOPHY, kind = SourceKind.OPINION),
        FeedSpec("noema", "Noema", url = "https://www.noemamag.com/feed/", category = Category.PHILOSOPHY, kind = SourceKind.OPINION, mixed = true),
        FeedSpec("sep", "Stanford Encyclopedia of Philosophy", url = "https://plato.stanford.edu/rss/sep.xml", category = Category.PHILOSOPHY, kind = SourceKind.PRIMARY),

        // Edebiyat
        FeedSpec("lithub", "Literary Hub", url = "https://lithub.com/feed/", category = Category.LITERATURE, kind = SourceKind.OPINION),
        FeedSpec("marginalian", "The Marginalian", url = "https://www.themarginalian.org/feed/", category = Category.LITERATURE, kind = SourceKind.OPINION),
        FeedSpec("parisreview", "The Paris Review", url = "https://www.theparisreview.org/blog/feed/", category = Category.LITERATURE, kind = SourceKind.OPINION),

        // Sosyoloji ve toplum
        FeedSpec("pew", "Pew Research Center", url = "https://www.pewresearch.org/feed/", category = Category.SOCIOLOGY, kind = SourceKind.PRIMARY),
        FeedSpec("conversation", "The Conversation", url = "https://theconversation.com/global/articles.atom", category = Category.SOCIOLOGY, kind = SourceKind.OPINION, mixed = true),
        FeedSpec("hrw", "Human Rights Watch", url = "https://www.hrw.org/rss/news", category = Category.SOCIOLOGY, kind = SourceKind.PRIMARY),
        FeedSpec("amnesty", "Amnesty International", url = "https://www.amnesty.org/en/latest/news/feed/", category = Category.SOCIOLOGY, kind = SourceKind.PRIMARY),
    )
}
