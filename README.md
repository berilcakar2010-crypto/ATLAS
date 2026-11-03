# ATLAS — Personal Intelligence System

Samsung Galaxy Tab S10 Lite için yazılan, tablet ve S Pen odaklı, çevrimdışı çalışan kişisel araştırma terminali.
Yerel Android uygulaması: Kotlin + Jetpack Compose + Room. WebView değil.

> **Sürüm 0.1.0 · Phase 3 (çekirdek uygulama).** Aşağıdaki "Durum" bölümü neyin gerçekten çalıştığını,
> neyin henüz olmadığını açıkça listeler.

## APK'yi almak (bilgisayara Android Studio gerekmez)

APK'yi GitHub bulutta derler.

1. Bu repoda **Actions** sekmesine gir, en üstteki **Build APK** çalışmasını aç (yeşil tik olmalı).
2. Sayfanın altındaki **Artifacts** bölümünden **ATLAS-apk** dosyasını indir (zip).
3. Zip'in içinden **`app-release.apk`** dosyasını çıkar.
4. APK'yi tablete aktar (USB, Drive, e-posta). Tablette dosyaya dokun, istenirse
   *"Bu kaynaktan uygulama yüklemeye izin ver"* seçeneğini aç ve kur.
5. Uygulama simgesi bordo zemin üzerinde pirinç rengi pusula.

`app-debug.apk` ayrı bir uygulama olarak (`com.atlas.app.debug`) kurulur; hata ayıklamak için, normalde gerekmez.

Bir çalışmayı elle başlatmak için: **Actions → Build APK → Run workflow**.

### Güncelleme ve veri

* Her derleme aynı imza anahtarıyla (`app/atlas-sideload.keystore`) imzalanır, bu yüzden yeni sürüm
  eskisinin **üzerine kurulur ve verilerin korunur**.
* Uygulamayı **kaldırırsan yerel veriler (sorular, notlar) silinir**; veriler yalnızca cihazdadır.
* Bu anahtar kişisel yan yükleme içindir (Play Store için uygun değil). Repo herkese açıksa, anahtarı bilen biri
  aynı imzayla APK üretebilir; bu yüzden APK'yi yalnızca kendi reponun Actions sekmesinden indir.

## Durum (Phase 7 — derleme başarılı, cihazda test EDİLMEDİ)

Yazılan ve CI'da derlenen özellikler:
- Daily Intelligence: 53 RSS/Atom kaynağı (canlı doğrulanmış; ölü/engelli 8 kaynak çıkarıldı), konu ve kaynak türü etiketi, arama, kaydetme, not.
- Research Radar: arXiv + PubMed araması, 16 başlangıç konusu (düzenlenebilir), kaydetme, not.
- Sürdürülebilirlik izleyici, Çevrimdışı Arşiv durumu, bilgi grafiği (yalnızca etiket/başlık metin eşleşmesi).
- S Pen defteri: basınçlı çizim, kalem türleri, silgi, şekil araçları, kâğıt türleri, geri al/yinele, PDF dışa aktarma.
- Deep Dive: yapay zekâsız çalışma sayfası + gerçek makale araması; isteğe bağlı Gemini / OpenAI uyumlu sağlayıcı (varsayılan kapalı, uygunluk uyarısı var).
- Command Center: canlı bülten, modül gizleme/sıralama/boyutlandırma. Arka plan eşitleme: 6 saatte bir.

Yok / doğrulanmadı: el yazısı tanıma, defterde metin/görsel, PDF üstüne not, haber tam metni; gerçek cihazda (Tab S10 Lite + S Pen) hiçbir şey denenmedi; v1→v2 veritabanı geçişi cihazda sınanmadı.

## Proje yapısı

Tek Gradle modülü (`app`), katmanlar paketlerle ayrılmış. Ayrıntı: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

```
app/src/main/java/com/atlas/app/
  AtlasApp.kt, MainActivity.kt
  core/design/        renkler, tipografi, tema, ortak bileşenler
  core/navigation/    10 modülün tanımı
  core/util/          biçimlendirme
  data/db/            Room: varlıklar, DAO'lar, veritabanı
  ui/shell/           kenar çubuğu + gezinme
  feature/            command, questions, library, settings, placeholder
```

## Veritabanı (Room, `atlas.db`, sürüm 2)

| Tablo | Amaç |
|---|---|
| `questions` | Soru Motoru: başlık, ayrıntı, konu, durum, hipotez, tarihler |
| `question_links` | Sorular arası bağlantı (fromId → toId) |
| `library_items` | Kütüphane kaydı: tür, başlık, gövde, url, kaynak, favori |
| `tags`, `library_item_tags` | Etiketler ve kayıt–etiket ilişkisi |
| `notebooks`, `notebook_pages` | Defter ve sayfa üst verisi (çizim verisi Phase 5'te) |

`fallbackToDestructiveMigration()` bilerek kullanılmıyor: şema değişince veri sessizce silinmez.

## Yol haritası

Phase 4 araştırma sistemleri → Phase 5 S Pen defteri → Phase 6 yapay zekâ → Phase 7 test ve yayın imzası.
