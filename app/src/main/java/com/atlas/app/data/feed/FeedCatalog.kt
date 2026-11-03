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
    NEURO("Nörobilim"),
    PHYSICS("Fizik"),
    COSMOLOGY("Uzay ve kozmoloji"),
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
    HEALTH("Sağlık ve tıp"),
    PSYCH("Psikoloji ve zihin"),
    CHEM("Kimya ve malzeme"),
    EARTH("Yer bilimleri"),
    ENERGY("Enerji"),
    ENGINEERING("Mühendislik ve üretim"),
    SECURITY("Güvenlik ve savunma"),
    TURKEY("Türkiye gündemi"),
    HISTORY("Tarih"),
    ARTS("Sanat ve kültür"),
    EDUCATION("Eğitim ve akademi"),
    LAW("Hukuk ve haklar"),
    LANGUAGE("Dil ve sözcükler"),
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

        // --- Genişletilmiş kaynaklar (canlı test sonrası elenenler çıkarılır) ---
        FeedSpec("sciencedaily-top", "ScienceDaily · Bilim", url = "https://www.sciencedaily.com/rss/top/science.xml", category = Category.SCIENCE, kind = SourceKind.PRIMARY),
        FeedSpec("sciencedaily-mind", "ScienceDaily · Zihin ve beyin", url = "https://www.sciencedaily.com/rss/mind_brain.xml", category = Category.NEURO, kind = SourceKind.PRIMARY),
        FeedSpec("sciencedaily-space", "ScienceDaily · Uzay", url = "https://www.sciencedaily.com/rss/space_time.xml", category = Category.COSMOLOGY, kind = SourceKind.PRIMARY),
        FeedSpec("sciencedaily-matter", "ScienceDaily · Madde ve enerji", url = "https://www.sciencedaily.com/rss/matter_energy.xml", category = Category.PHYSICS, kind = SourceKind.PRIMARY),
        FeedSpec("sciencedaily-health", "ScienceDaily · Sağlık", url = "https://www.sciencedaily.com/rss/health_medicine.xml", category = Category.HEALTH, kind = SourceKind.PRIMARY),
        FeedSpec("sciencedaily-earth", "ScienceDaily · Dünya ve iklim", url = "https://www.sciencedaily.com/rss/earth_climate.xml", category = Category.EARTH, kind = SourceKind.PRIMARY),
        FeedSpec("sciencedaily-cs", "ScienceDaily · Bilgisayar ve matematik", url = "https://www.sciencedaily.com/rss/computers_math.xml", category = Category.MATH, kind = SourceKind.PRIMARY),
        FeedSpec("sciencedaily-fossils", "ScienceDaily · Fosiller ve tarih", url = "https://www.sciencedaily.com/rss/fossils_ruins.xml", category = Category.HISTORY, kind = SourceKind.PRIMARY),
        FeedSpec("science-news-current", "Science (AAAS) · Haberler", url = "https://www.science.org/rss/news_current.xml", category = Category.SCIENCE, kind = SourceKind.JOURNALISM),
        FeedSpec("knowable", "Knowable Magazine", url = "https://knowablemagazine.org/rss", category = Category.SCIENCE, kind = SourceKind.JOURNALISM),
        FeedSpec("undark", "Undark", url = "https://undark.org/feed/", category = Category.SCIENCE, kind = SourceKind.JOURNALISM),
        FeedSpec("nautilus", "Nautilus", url = "https://nautil.us/feed/", category = Category.SCIENCE, kind = SourceKind.OPINION),
        FeedSpec("bigthink", "Big Think", url = "https://bigthink.com/feed/", category = Category.SCIENCE, kind = SourceKind.OPINION),
        FeedSpec("nature-neuro", "Nature Neuroscience", url = "https://www.nature.com/neuro.rss", category = Category.NEURO, kind = SourceKind.PEER),
        FeedSpec("nature-physics", "Nature Physics", url = "https://www.nature.com/nphys.rss", category = Category.PHYSICS, kind = SourceKind.PEER),
        FeedSpec("nature-nmi", "Nature Machine Intelligence", url = "https://www.nature.com/natmachintell.rss", category = Category.AI, kind = SourceKind.PEER),
        FeedSpec("nature-chem", "Nature Chemistry", url = "https://www.nature.com/nchem.rss", category = Category.CHEM, kind = SourceKind.PEER),
        FeedSpec("nature-med", "Nature Medicine", url = "https://www.nature.com/nm.rss", category = Category.HEALTH, kind = SourceKind.PEER),
        FeedSpec("nature-climate", "Nature Climate Change", url = "https://www.nature.com/nclimate.rss", category = Category.CLIMATE, kind = SourceKind.PEER),
        FeedSpec("nature-human", "Nature Human Behaviour", url = "https://www.nature.com/nathumbehav.rss", category = Category.PSYCH, kind = SourceKind.PEER),
        FeedSpec("pnas", "PNAS", url = "https://www.pnas.org/action/showFeed?type=etoc&feed=rss&jc=pnas", category = Category.SCIENCE, kind = SourceKind.PEER),
        FeedSpec("sciadv", "Science Advances", url = "https://www.science.org/action/showFeed?type=etoc&feed=rss&jc=sciadv", category = Category.SCIENCE, kind = SourceKind.PEER),
        FeedSpec("arxiv-qbio-nc", "arXiv · q-bio.NC (nöronlar ve biliş)", url = "https://rss.arxiv.org/rss/q-bio.NC", category = Category.NEURO, kind = SourceKind.PREPRINT),
        FeedSpec("arxiv-bio-ph", "arXiv · physics.bio-ph", url = "https://rss.arxiv.org/rss/physics.bio-ph", category = Category.BIOLOGY, kind = SourceKind.PREPRINT),
        FeedSpec("arxiv-cs-ne", "arXiv · cs.NE (sinirsel ve evrimsel)", url = "https://rss.arxiv.org/rss/cs.NE", category = Category.AI, kind = SourceKind.PREPRINT),
        FeedSpec("arxiv-astro-co", "arXiv · astro-ph.CO (kozmoloji)", url = "https://rss.arxiv.org/rss/astro-ph.CO", category = Category.COSMOLOGY, kind = SourceKind.PREPRINT),
        FeedSpec("arxiv-quant-ph", "arXiv · quant-ph", url = "https://rss.arxiv.org/rss/quant-ph", category = Category.PHYSICS, kind = SourceKind.PREPRINT),
        FeedSpec("arxiv-math-ds", "arXiv · math.DS (dinamik sistemler)", url = "https://rss.arxiv.org/rss/math.DS", category = Category.MATH, kind = SourceKind.PREPRINT),
        FeedSpec("arxiv-nlin-ao", "arXiv · nlin.AO (uyarlanır sistemler)", url = "https://rss.arxiv.org/rss/nlin.AO", category = Category.SCIENCE, kind = SourceKind.PREPRINT),
        FeedSpec("biorxiv-bioinf", "bioRxiv · Hesaplamalı biyoloji", url = "https://connect.biorxiv.org/biorxiv_xml.php?subject=bioinformatics", category = Category.BIOLOGY, kind = SourceKind.PREPRINT),
        FeedSpec("phys-physics", "Phys.org · Fizik", url = "https://phys.org/rss-feed/physics-news/", category = Category.PHYSICS, kind = SourceKind.JOURNALISM),
        FeedSpec("phys-space", "Phys.org · Uzay", url = "https://phys.org/rss-feed/space-news/", category = Category.COSMOLOGY, kind = SourceKind.JOURNALISM),
        FeedSpec("phys-bio", "Phys.org · Biyoloji", url = "https://phys.org/rss-feed/biology-news/", category = Category.BIOLOGY, kind = SourceKind.JOURNALISM),
        FeedSpec("phys-chem", "Phys.org · Kimya", url = "https://phys.org/rss-feed/chemistry-news/", category = Category.CHEM, kind = SourceKind.JOURNALISM),
        FeedSpec("phys-earth", "Phys.org · Yer bilimleri", url = "https://phys.org/rss-feed/earth-news/", category = Category.EARTH, kind = SourceKind.JOURNALISM),
        FeedSpec("phys-tech", "Phys.org · Teknoloji", url = "https://phys.org/rss-feed/technology-news/", category = Category.TECH, kind = SourceKind.JOURNALISM),
        FeedSpec("phys-nano", "Phys.org · Nanoteknoloji", url = "https://phys.org/rss-feed/nanotech-news/", category = Category.CHEM, kind = SourceKind.JOURNALISM),
        FeedSpec("apod", "NASA · Günün Astronomi Görüntüsü", url = "https://apod.nasa.gov/apod.rss", category = Category.COSMOLOGY, kind = SourceKind.PRIMARY),
        FeedSpec("universetoday", "Universe Today", url = "https://www.universetoday.com/feed", category = Category.COSMOLOGY, kind = SourceKind.JOURNALISM),
        FeedSpec("skytelescope", "Sky & Telescope", url = "https://skyandtelescope.org/feed/", category = Category.COSMOLOGY, kind = SourceKind.JOURNALISM),
        FeedSpec("astrobites", "Astrobites", url = "https://astrobites.org/feed/", category = Category.COSMOLOGY, kind = SourceKind.OPINION),
        FeedSpec("spacenews", "SpaceNews", url = "https://spacenews.com/feed/", category = Category.COSMOLOGY, kind = SourceKind.JOURNALISM),
        FeedSpec("jpl", "NASA JPL", url = "https://www.jpl.nasa.gov/feeds/news", category = Category.COSMOLOGY, kind = SourceKind.PRIMARY),
        FeedSpec("cern", "CERN", url = "https://home.cern/api/news/news/feed.rss", category = Category.PHYSICS, kind = SourceKind.PRIMARY),
        FeedSpec("symmetry", "Symmetry Magazine", url = "https://www.symmetrymagazine.org/feed", category = Category.PHYSICS, kind = SourceKind.PRIMARY),
        FeedSpec("terrytao", "Terence Tao (blog)", url = "https://terrytao.wordpress.com/feed/", category = Category.MATH, kind = SourceKind.OPINION),
        FeedSpec("aaronson", "Shtetl-Optimized", url = "https://scottaaronson.blog/?feed=rss2", category = Category.MATH, kind = SourceKind.OPINION),
        FeedSpec("quanta-math", "Quanta · Matematik", url = "https://www.quantamagazine.org/mathematics/feed/", category = Category.MATH, kind = SourceKind.JOURNALISM),
        FeedSpec("gradient", "The Gradient", url = "https://thegradient.pub/rss/", category = Category.AI, kind = SourceKind.OPINION),
        FeedSpec("bair", "Berkeley AI Research Blog", url = "https://bair.berkeley.edu/blog/feed.xml", category = Category.AI, kind = SourceKind.PRIMARY),
        FeedSpec("googleai", "Google · Yapay zekâ", url = "https://blog.google/technology/ai/rss/", category = Category.AI, kind = SourceKind.PRIMARY),
        FeedSpec("lilweng", "Lil'Log (Lilian Weng)", url = "https://lilianweng.github.io/index.xml", category = Category.AI, kind = SourceKind.OPINION),
        FeedSpec("simonw", "Simon Willison", url = "https://simonwillison.net/atom/everything/", category = Category.AI, kind = SourceKind.OPINION),
        FeedSpec("statnews", "STAT News", url = "https://www.statnews.com/feed/", category = Category.HEALTH, kind = SourceKind.JOURNALISM),
        FeedSpec("lancet", "The Lancet", url = "https://www.thelancet.com/rssfeed/lancet_current.xml", category = Category.HEALTH, kind = SourceKind.PEER),
        FeedSpec("nejm", "NEJM", url = "https://www.nejm.org/action/showFeed?type=etoc&feed=rss&jc=nejm", category = Category.HEALTH, kind = SourceKind.PEER),
        FeedSpec("psyche", "Psyche", url = "https://psyche.co/feed", category = Category.PSYCH, kind = SourceKind.OPINION),
        FeedSpec("bps-digest", "BPS Research Digest", url = "https://digest.bps.org.uk/feed/", category = Category.PSYCH, kind = SourceKind.JOURNALISM),
        FeedSpec("eos", "Eos (AGU)", url = "https://eos.org/feed", category = Category.EARTH, kind = SourceKind.JOURNALISM),
        FeedSpec("earthobs", "NASA Earth Observatory", url = "https://earthobservatory.nasa.gov/feeds/earth-observatory.rss", category = Category.EARTH, kind = SourceKind.PRIMARY),
        FeedSpec("grist", "Grist", url = "https://grist.org/feed/", category = Category.CLIMATE, kind = SourceKind.JOURNALISM),
        FeedSpec("cleantechnica", "CleanTechnica", url = "https://cleantechnica.com/feed/", category = Category.ENERGY, kind = SourceKind.JOURNALISM),
        FeedSpec("canary", "Canary Media", url = "https://www.canarymedia.com/rss.rss", category = Category.ENERGY, kind = SourceKind.JOURNALISM),
        FeedSpec("iea", "IEA · Haberler", url = "https://www.iea.org/rss/news.xml", category = Category.ENERGY, kind = SourceKind.PRIMARY),
        FeedSpec("hackaday", "Hackaday", url = "https://hackaday.com/blog/feed/", category = Category.ENGINEERING, kind = SourceKind.JOURNALISM),
        FeedSpec("interesting-eng", "Interesting Engineering", url = "https://interestingengineering.com/feed", category = Category.ENGINEERING, kind = SourceKind.JOURNALISM),
        FeedSpec("krebs", "Krebs on Security", url = "https://krebsonsecurity.com/feed/", category = Category.SECURITY, kind = SourceKind.JOURNALISM),
        FeedSpec("schneier", "Schneier on Security", url = "https://www.schneier.com/feed/atom/", category = Category.SECURITY, kind = SourceKind.OPINION),
        FeedSpec("warontherocks", "War on the Rocks", url = "https://warontherocks.com/feed/", category = Category.SECURITY, kind = SourceKind.OPINION),
        FeedSpec("bellingcat", "Bellingcat", url = "https://www.bellingcat.com/feed/", category = Category.SECURITY, kind = SourceKind.JOURNALISM),
        FeedSpec("thediplomat", "The Diplomat", url = "https://thediplomat.com/feed/", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("dw-en", "DW · English", url = "https://rss.dw.com/xml/rss-en-all", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("france24", "France 24", url = "https://www.france24.com/en/rss", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("npr-world", "NPR · World", url = "https://feeds.npr.org/1004/rss.xml", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("guardian-world", "The Guardian · World", url = "https://www.theguardian.com/world/rss", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("nyt-world", "NYT · World", url = "https://rss.nytimes.com/services/xml/rss/nyt/World.xml", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("eurasianet", "Eurasianet", url = "https://eurasianet.org/rss.xml", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("mee", "Middle East Eye", url = "https://www.middleeasteye.net/rss", category = Category.POLITICS, kind = SourceKind.JOURNALISM),
        FeedSpec("restofworld", "Rest of World", url = "https://restofworld.org/feed/", category = Category.TECH, kind = SourceKind.JOURNALISM),
        FeedSpec("cfr", "Council on Foreign Relations", url = "https://www.cfr.org/rss.xml", category = Category.POLITICS, kind = SourceKind.OPINION),
        FeedSpec("nber", "NBER · Yeni çalışma kâğıtları", url = "https://www.nber.org/rss/new.xml", category = Category.ECONOMICS, kind = SourceKind.PEER),
        FeedSpec("marginalrev", "Marginal Revolution", url = "https://marginalrevolution.com/feed", category = Category.ECONOMICS, kind = SourceKind.OPINION),
        FeedSpec("noahpinion", "Noahpinion", url = "https://noahpinion.blog/feed", category = Category.ECONOMICS, kind = SourceKind.OPINION),
        FeedSpec("economist-fin", "The Economist · Finans ve ekonomi", url = "https://www.economist.com/finance-and-economics/rss.xml", category = Category.ECONOMICS, kind = SourceKind.JOURNALISM),
        FeedSpec("3quarks", "3 Quarks Daily", url = "https://3quarksdaily.com/feed", category = Category.PHILOSOPHY, kind = SourceKind.OPINION),
        FeedSpec("philnow", "Philosophy Now", url = "https://philosophynow.org/rss", category = Category.PHILOSOPHY, kind = SourceKind.OPINION),
        FeedSpec("millions", "The Millions", url = "https://themillions.com/feed", category = Category.LITERATURE, kind = SourceKind.OPINION),
        FeedSpec("nyrb", "The New York Review of Books", url = "https://www.nybooks.com/feed/", category = Category.LITERATURE, kind = SourceKind.OPINION),
        FeedSpec("lrb", "London Review of Books", url = "https://www.lrb.co.uk/feeds/rss", category = Category.LITERATURE, kind = SourceKind.OPINION),
        FeedSpec("wwb", "Words Without Borders", url = "https://wordswithoutborders.org/feed", category = Category.LITERATURE, kind = SourceKind.OPINION),
        FeedSpec("sapiens", "SAPIENS (antropoloji)", url = "https://www.sapiens.org/feed/", category = Category.SOCIOLOGY, kind = SourceKind.OPINION),
        FeedSpec("jstor-daily", "JSTOR Daily", url = "https://daily.jstor.org/feed/", category = Category.HISTORY, kind = SourceKind.OPINION),
        FeedSpec("smithsonian", "Smithsonian Magazine", url = "https://www.smithsonianmag.com/rss/latest_articles/", category = Category.HISTORY, kind = SourceKind.JOURNALISM),
        FeedSpec("historytoday", "History Today", url = "https://www.historytoday.com/feed/rss.xml", category = Category.HISTORY, kind = SourceKind.OPINION),
        FeedSpec("medievalists", "Medievalists.net", url = "https://www.medievalists.net/feed/", category = Category.HISTORY, kind = SourceKind.JOURNALISM),
        FeedSpec("hyperallergic", "Hyperallergic", url = "https://hyperallergic.com/feed/", category = Category.ARTS, kind = SourceKind.OPINION),
        FeedSpec("openculture", "Open Culture", url = "https://www.openculture.com/feed", category = Category.ARTS, kind = SourceKind.OPINION),
        FeedSpec("colossal", "Colossal", url = "https://www.thisiscolossal.com/feed/", category = Category.ARTS, kind = SourceKind.JOURNALISM),
        FeedSpec("guardian-culture", "The Guardian · Kültür", url = "https://www.theguardian.com/culture/rss", category = Category.ARTS, kind = SourceKind.JOURNALISM),
        FeedSpec("edutopia", "Edutopia", url = "https://www.edutopia.org/rss.xml", category = Category.EDUCATION, kind = SourceKind.JOURNALISM),
        FeedSpec("insidehighered", "Inside Higher Ed", url = "https://www.insidehighered.com/rss.xml", category = Category.EDUCATION, kind = SourceKind.JOURNALISM),
        FeedSpec("khan-blog", "Khan Academy Blog", url = "https://blog.khanacademy.org/feed/", category = Category.EDUCATION, kind = SourceKind.PRIMARY),
        FeedSpec("mit-news", "MIT News", url = "https://news.mit.edu/rss/feed", category = Category.EDUCATION, kind = SourceKind.PRIMARY),
        FeedSpec("stanford-news", "Stanford Report", url = "https://news.stanford.edu/feed/", category = Category.EDUCATION, kind = SourceKind.PRIMARY),
        FeedSpec("harvard-gazette", "Harvard Gazette", url = "https://news.harvard.edu/gazette/feed/", category = Category.EDUCATION, kind = SourceKind.PRIMARY),
        FeedSpec("uwc", "UWC · Haberler", url = "https://www.uwc.org/news/feed", category = Category.EDUCATION, kind = SourceKind.PRIMARY),
        FeedSpec("justsecurity", "Just Security", url = "https://www.justsecurity.org/feed/", category = Category.LAW, kind = SourceKind.OPINION),
        FeedSpec("lawfare", "Lawfare", url = "https://www.lawfaremedia.org/feed", category = Category.LAW, kind = SourceKind.OPINION),
        FeedSpec("eff", "EFF · Güncellemeler", url = "https://www.eff.org/rss/updates.xml", category = Category.LAW, kind = SourceKind.PRIMARY),
        FeedSpec("freedomhouse", "Freedom House", url = "https://freedomhouse.org/rss.xml", category = Category.LAW, kind = SourceKind.PRIMARY),
        FeedSpec("rsf", "Sınır Tanımayan Gazeteciler (RSF)", url = "https://rsf.org/en/rss.xml", category = Category.LAW, kind = SourceKind.PRIMARY),
        FeedSpec("languagelog", "Language Log", url = "https://languagelog.ldc.upenn.edu/nll/?feed=rss2", category = Category.LANGUAGE, kind = SourceKind.OPINION),
        FeedSpec("mw-wotd", "Merriam-Webster · Günün Sözcüğü", url = "https://www.merriam-webster.com/wotd/feed/rss2", category = Category.LANGUAGE, kind = SourceKind.PRIMARY),
        FeedSpec("duvar-en", "Duvar English", url = "https://www.duvarenglish.com/rss", category = Category.TURKEY, kind = SourceKind.JOURNALISM),
        FeedSpec("turkishminute", "Turkish Minute", url = "https://www.turkishminute.com/feed/", category = Category.TURKEY, kind = SourceKind.JOURNALISM),
        FeedSpec("aa-en", "Anadolu Ajansı · English", url = "https://www.aa.com.tr/en/rss/default?cat=turkey", category = Category.TURKEY, kind = SourceKind.JOURNALISM),
        FeedSpec("dw-tr", "DW Türkçe", url = "https://rss.dw.com/xml/rss-tr-all", category = Category.TURKEY, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("euronews-tr", "Euronews Türkçe", url = "https://tr.euronews.com/rss", category = Category.TURKEY, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("t24", "T24", url = "https://t24.com.tr/rss", category = Category.TURKEY, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("gazeteduvar", "Gazete Duvar", url = "https://www.gazeteduvar.com.tr/export/rss", category = Category.TURKEY, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("indyturk", "Independent Türkçe", url = "https://www.indyturk.com/rss.xml", category = Category.TURKEY, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("diken", "Diken", url = "https://www.diken.com.tr/feed/", category = Category.TURKEY, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("webrazzi", "Webrazzi", url = "https://webrazzi.com/feed/", category = Category.TECH, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("shiftdelete", "ShiftDelete.Net", url = "https://shiftdelete.net/feed", category = Category.TECH, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("evrimagaci", "Evrim Ağacı", url = "https://evrimagaci.org/rss.xml", category = Category.SCIENCE, kind = SourceKind.JOURNALISM, language = "tr"),
        FeedSpec("bilimgenc", "TÜBİTAK Bilim Genç", url = "https://bilimgenc.tubitak.gov.tr/rss.xml", category = Category.SCIENCE, kind = SourceKind.PRIMARY, language = "tr"),
    )
}
