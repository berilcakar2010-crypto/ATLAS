package com.atlas.app.network

import android.util.Xml
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser

data class ParsedItem(
    val title: String,
    val link: String,
    val summary: String,
    val publishedAt: Long?,
)

/** RSS 2.0, RSS 1.0 (RDF) ve Atom için küçük, bağımlılıksız ayrıştırıcı. */
object FeedParser {
    fun parse(xml: String, limit: Int = 40): List<ParsedItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val items = mutableListOf<ParsedItem>()
        var inItem = false
        var title = ""
        var link = ""
        var summary = ""
        var content = ""
        var date = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && items.size < limit) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (!inItem) {
                        if (name == "item" || name == "entry") {
                            inItem = true
                            title = ""
                            link = ""
                            summary = ""
                            content = ""
                            date = ""
                        }
                    } else {
                        when (name) {
                            "title" -> if (title.isEmpty()) title = readText(parser)
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                if (href != null) {
                                    val rel = parser.getAttributeValue(null, "rel")
                                    if (link.isEmpty() && (rel == null || rel == "alternate")) link = href
                                } else if (link.isEmpty()) {
                                    link = readText(parser)
                                }
                            }
                            "guid" -> {
                                val permalink = parser.getAttributeValue(null, "isPermaLink")
                                val text = readText(parser)
                                if (link.isEmpty() && permalink != "false" && TextUtil.isHttpUrl(text)) link = text
                            }
                            "description", "summary", "dc:description" ->
                                if (summary.isEmpty()) summary = readText(parser)
                            "content:encoded", "content" ->
                                if (content.isEmpty()) content = readText(parser)
                            "pubDate", "published", "dc:date", "updated" ->
                                if (date.isEmpty() || name == "pubDate" || name == "published") date = readText(parser)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (inItem && (name == "item" || name == "entry")) {
                        inItem = false
                        val cleanTitle = TextUtil.cleanHtml(title, 300)
                        val cleanLink = link.trim()
                        if (cleanTitle.isNotEmpty() && TextUtil.isHttpUrl(cleanLink)) {
                            val body = summary.ifBlank { content }
                            items += ParsedItem(
                                title = cleanTitle,
                                link = cleanLink,
                                summary = TextUtil.cleanHtml(body),
                                publishedAt = TextUtil.parseDate(date),
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }
        return items
    }

    /** Geçerli öğenin bitiş etiketine kadar tüm metni toplar (iç içe etiketler dahil). */
    fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.TEXT -> sb.append(parser.text)
                XmlPullParser.END_DOCUMENT -> return sb.toString().trim()
            }
        }
        return sb.toString().trim()
    }
}
