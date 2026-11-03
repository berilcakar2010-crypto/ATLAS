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

## Durum (Phase 3)

**Çalışıyor**

* Kalıcı sol gezinme: geniş ekranda kenar çubuğu, dar (portre) ekranda 76 dp'lik ray
* Tasarım sistemi: palet, Source Serif 4 / Inter / JetBrains Mono (uygulamaya gömülü), çerçeveli paneller
* Yerel SQLite (Room) veritabanı
* Soru Motoru: soru ekleme, 6 durum arasında geçiş, silme
* Kütüphane: not ekleme, favori, silme
* Command Center: saat/tarih ve veritabanından canlı "son kaydedilenler" ve "çözülmemiş sorular"
* Ayarlar: tüm yerel verileri silme (onaylı), uygulama durumu

**Henüz yok (ekranlar bunu açıkça "Uygulanmadı" diye belirtir)**

* Daily Intelligence, RSS, Research Radar, arXiv, PubMed, Sürdürülebilirlik (Phase 4)
* Çevrimdışı arşiv ve senkronizasyon durumları (Phase 4)
* S Pen defteri, el yazısı, PDF dışa aktarma (Phase 5). El yazısı **tanıma** planlanmıyor.
* Deep Dive ve yapay zekâ sağlayıcıları (Phase 6)
* Bilgi grafiği, içe/dışa aktarma, dashboard modüllerini taşıma/gizleme/boyutlandırma

## Dürüst notlar

* Bu kod, geliştirme ortamında **derlenemedi** (Android SDK yok). İlk derleme GitHub Actions'ta yapılacak.
  Hata çıkarsa Actions günlüğünü paylaş, düzeltilir.
* Tablet üzerinde **test edilmedi.** Yerleşim, dokunma hedefleri ve ileride S Pen davranışı cihazda doğrulanmalı.
* Bu sürümde uygulamanın **INTERNET izni yok**; hiçbir veri dışarı gönderilmez. İzin, Phase 4'te RSS için eklenecek.
* Gradle wrapper dosyası repoda yok; derleme GitHub'ın kurduğu Gradle 8.10.2 ile yapılır. Android Studio ile
  açarsan wrapper'ı kendisi oluşturur.

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

## Veritabanı (Room, `atlas.db`, sürüm 1)

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
