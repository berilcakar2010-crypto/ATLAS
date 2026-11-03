# ATLAS Mimari (Phase 1'in güncel hali)

## Neden tek modül?

Phase 1'de çok modüllü Gradle yapısı önerilmişti. Geliştirme ortamında Android SDK olmadığı için kod derlenemiyor;
çok modüllü yapı, derleme hatasını bulmayı ve düzeltmeyi zorlaştırır. Bu yüzden Phase 3 tek `app` modülüyle ve
**paket düzeyinde katmanlarla** başladı. Paket sınırları, ileride gerçek modüllere bölmeyi kolaylaştıracak biçimde
çizildi (`core`, `data`, `feature.*`). Bölme, kod derlenip tablette doğrulandıktan sonra yapılacak.

## Katmanlar

| Paket | Sorumluluk |
|---|---|
| `core.design` | Renkler, tipografi, tema, ortak Compose bileşenleri |
| `core.navigation` | 10 modülün rotaları ve sıralaması |
| `core.util` | Biçimlendirme yardımcıları |
| `data.db` | Room varlıkları, DAO'lar, veritabanı |
| `ui.shell` | Kalıcı kenar çubuğu, NavHost |
| `feature.*` | Her modülün ekranı ve ViewModel'i |

Bağımlılık enjeksiyonu elle yapılır (`AtlasApp.database`); ViewModel'ler `viewModelFactory` ile üretilir.

## Gezinme

Navigation Compose, string rotalar. Kenar çubuğu 900 dp ve üstünde genişler (232 dp), daha dar ekranda 76 dp'lik
ray olur. Tab S10 Lite yatayda yaklaşık 1050 dp, portrede yaklaşık 660 dp genişliğindedir (yaklaşık değerler,
cihazda doğrulanacak).

## Teknik riskler (güncel)

* **Derleme doğrulanmadı:** ilk derleme GitHub Actions'ta. Sürüm uyumluluğu (AGP 8.7.3, Kotlin 2.0.21,
  KSP 2.0.21-1.0.28, Compose BOM 2024.12.01, Room 2.6.1) bilinen uyumlu kombinasyon olarak seçildi.
* **Çizim performansı (Phase 5):** düşük gecikme için `androidx.ink` ve `MotionEvent` + `Path` önbelleği
  karşılaştırılacak.
* **Çift kayıt önleme (Phase 4):** URL normalizasyonu + içerik özeti.
* **Dashboard düzenleme:** modül taşıma/boyutlandırma özel bir ızgara düzeni gerektirir; henüz yapılmadı.
* **Hız sınırları (Phase 4):** arXiv ve PubMed için WorkManager ile nazik aralıklar.
* **Yapay zekâ anahtarı (Phase 6):** anahtar kaynak koda gömülmeyecek; istemci tarafı anahtarın riski
  Ayarlar'da açıkça yazılacak.
