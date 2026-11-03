package com.atlas.app.network

import android.util.Xml
import java.io.StringReader
import java.net.URLEncoder
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

/** Bir makalenin ağdan gelen, henüz veritabanına yazılmamış hali. Hiçbir alan uydurulmaz. */
data class PaperDraft(
    val source: String,
    val externalId: String,
    val title: String,
    val authors: String,
    val abstractText: String,
    val publishedAt: Long,
    val url: String,
    val doi: String?,
    val journal: String,
)

object ArxivClient {
    private const val BASE = "https://export.arxiv.org/api/query"

    /** Tırnaklı ifade tam eşleşme arar; aksi halde her sözcük için AND kullanılır. */
    private fun buildQuery(raw: String): String {
        val q = raw.trim()
        val phrase = q.length > 2 && q.startsWith("\"") && q.endsWith("\"")
        return if (phrase || !q.contains(' ')) {
            "all:\"" + q.trim('"') + "\""
        } else {
            q.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" AND ") { "all:$it" }
        }
    }

    suspend fun search(query: String, max: Int): List<PaperDraft> {
        val encoded = URLEncoder.encode(buildQuery(query), "UTF-8")
        val url = "$BASE?search_query=$encoded&start=0&max_results=$max&sortBy=submittedDate&sortOrder=descending"
        return parse(Http.get(url))
    }

    fun parse(xml: String): List<PaperDraft> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val out = mutableListOf<PaperDraft>()
        var inEntry = false
        var id = ""
        var title = ""
        var summary = ""
        var published = ""
        var doi: String? = null
        var link = ""
        val authors = mutableListOf<String>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (name == "entry") {
                        inEntry = true
                        id = ""
                        title = ""
                        summary = ""
                        published = ""
                        doi = null
                        link = ""
                        authors.clear()
                    } else if (inEntry) {
                        when (name) {
                            "id" -> id = FeedParser.readText(parser)
                            "title" -> title = FeedParser.readText(parser)
                            "summary" -> summary = FeedParser.readText(parser)
                            "published" -> published = FeedParser.readText(parser)
                            "name" -> authors += FeedParser.readText(parser)
                            "arxiv:doi" -> doi = FeedParser.readText(parser).ifBlank { null }
                            "link" -> {
                                val rel = parser.getAttributeValue(null, "rel")
                                val type = parser.getAttributeValue(null, "type")
                                val href = parser.getAttributeValue(null, "href")
                                if (href != null && rel == "alternate" && type == "text/html") link = href
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "entry" && inEntry) {
                        inEntry = false
                        val externalId = id.substringAfter("/abs/").replace(Regex("v\\d+$"), "")
                        val cleanTitle = title.replace(Regex("\\s+"), " ").trim()
                        val date = TextUtil.parseDate(published)
                        if (externalId.isNotBlank() && cleanTitle.isNotBlank() && date != null) {
                            out += PaperDraft(
                                source = "ARXIV",
                                externalId = externalId,
                                title = cleanTitle,
                                authors = authors.joinToString(", "),
                                abstractText = summary.replace(Regex("\\s+"), " ").trim(),
                                publishedAt = date,
                                url = link.ifBlank { "https://arxiv.org/abs/$externalId" },
                                doi = doi,
                                journal = "arXiv",
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }
        return out
    }
}

object PubMedClient {
    private const val BASE = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"

    suspend fun search(query: String, max: Int): List<PaperDraft> {
        val term = URLEncoder.encode(query.trim(), "UTF-8")
        val searchJson = Http.get("$BASE/esearch.fcgi?db=pubmed&retmode=json&sort=date&retmax=$max&term=$term")
        val idArray = JSONObject(searchJson).getJSONObject("esearchresult").getJSONArray("idlist")
        val ids = (0 until idArray.length()).map { idArray.getString(it) }
        if (ids.isEmpty()) return emptyList()
        val idParam = ids.joinToString(",")

        val summaryJson = JSONObject(Http.get("$BASE/esummary.fcgi?db=pubmed&retmode=json&id=$idParam"))
        val result = summaryJson.getJSONObject("result")
        val abstracts = try {
            parseAbstracts(Http.get("$BASE/efetch.fcgi?db=pubmed&retmode=xml&rettype=abstract&id=$idParam"))
        } catch (_: Exception) {
            emptyMap()
        }

        return ids.mapNotNull { id ->
            val item = result.optJSONObject(id) ?: return@mapNotNull null
            val title = item.optString("title").trim()
            if (title.isBlank()) return@mapNotNull null
            val authorsArray = item.optJSONArray("authors")
            val authors = if (authorsArray == null) "" else
                (0 until authorsArray.length()).joinToString(", ") { authorsArray.getJSONObject(it).optString("name") }
            var doi: String? = null
            val articleIds = item.optJSONArray("articleids")
            if (articleIds != null) {
                for (i in 0 until articleIds.length()) {
                    val a = articleIds.getJSONObject(i)
                    if (a.optString("idtype") == "doi") doi = a.optString("value").ifBlank { null }
                }
            }
            PaperDraft(
                source = "PUBMED",
                externalId = id,
                title = title.trimEnd('.'),
                authors = authors,
                abstractText = abstracts[id].orEmpty(),
                publishedAt = parsePubDate(item.optString("sortpubdate").ifBlank { item.optString("pubdate") }),
                url = "https://pubmed.ncbi.nlm.nih.gov/$id/",
                doi = doi,
                journal = item.optString("fulljournalname").ifBlank { item.optString("source") },
            )
        }
    }

    /** "2024/11/05 00:00" veya "2024 Nov 5" gibi PubMed tarihlerini ayrıştırır; yıl yoksa şimdiyi döner. */
    fun parsePubDate(raw: String): Long {
        val year = Regex("(19|20)\\d{2}").find(raw)?.value?.toIntOrNull()
            ?: return System.currentTimeMillis()
        val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
        val lower = raw.lowercase(Locale.ROOT)
        var month = months.indexOfFirst { lower.contains(it) } + 1
        var day = 1
        val numeric = Regex("((?:19|20)\\d{2})/(\\d{1,2})/(\\d{1,2})").find(raw)
        if (numeric != null) {
            month = numeric.groupValues[2].toInt()
            day = numeric.groupValues[3].toInt()
        } else {
            if (month == 0) month = 1
            Regex("\\b(\\d{1,2})\\b(?!\\s*\\d{3})").findAll(raw).firstOrNull { it.value.toInt() in 1..31 }
                ?.let { day = it.value.toInt() }
        }
        return try {
            LocalDate.of(year, month.coerceIn(1, 12), day.coerceIn(1, 28))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    /** efetch XML'inden PMID → özet metni eşlemesi çıkarır. */
    fun parseAbstracts(xml: String): Map<String, String> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))
        val map = mutableMapOf<String, StringBuilder>()
        var pmid: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "PubmedArticle" -> pmid = null
                    "PMID" -> if (pmid == null) pmid = FeedParser.readText(parser)
                    "AbstractText" -> {
                        val label = parser.getAttributeValue(null, "Label")
                        val text = FeedParser.readText(parser)
                        val key = pmid
                        if (key != null && text.isNotBlank()) {
                            val sb = map.getOrPut(key) { StringBuilder() }
                            if (sb.isNotEmpty()) sb.append(' ')
                            if (!label.isNullOrBlank()) sb.append(label).append(": ")
                            sb.append(text)
                        }
                    }
                }
            }
            event = parser.next()
        }
        return map.mapValues { it.value.toString() }
    }
}
