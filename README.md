# EuroJackpot AI (Phase 1–4)

A EuroJackpot number-analysis and personal-tracking app for Android — Kotlin, Jetpack
Compose, Material 3, Room, Coroutines/StateFlow, Navigation Compose, MVVM.

This build covers **Phase 1–4** of the product spec: project skeleton and 5-tab
navigation, the local database and draw-history sync pipeline, the Random / Balanced /
Hot / Cold / Hot+Cold / Statistical Mix generators plus the full statistics engine,
**Smart Coverage** (the cross-game diversity optimizer), per-country winner counts with
configurable-currency prize amounts (now including the actual PLN amount won when a
saved number matches a tier), and **OCR ticket scanning** (photograph a purchased
ticket to pre-fill My Numbers). **AI Mix, AdMob, Play Billing, onboarding,
notifications, and the 6 non-English/non-Korean locales are not built yet** — see
"What's deferred to a later phase" below for why and what's already scaffolded for them.

## Opening the project

1. Install **Android Studio** (a recent stable release — the "Upgrade Assistant" will
   offer to update the Android Gradle Plugin/Gradle wrapper automatically if a newer
   one is available by the time you read this).
2. Open this folder as a project. Android Studio will download the Android SDK
   components and Gradle dependencies on first sync (this machine that generated the
   project has no network access to Google's Maven repo or the Android SDK, so none of
   this could be pre-verified by actually running Gradle — see "Known limitation" below).
3. Copy `local.properties.example` to `local.properties` if Android Studio doesn't
   create it for you, and set `sdk.dir` to your SDK path (Android Studio usually does
   this automatically).

The app runs immediately with **zero configuration** — it seeds a clearly-labeled
sample draw history on first launch so Generate/Statistics/Results all have data to
work with offline.

## Connecting real EuroJackpot data (optional)

Draw results come from Poland's official [LOTTO OpenAPI](https://developers.lotto.pl)
(`gameType=EuroJackpot`), run by Totalizator Sportowy. An earlier version of this app
used the magayo Lottery Data API instead, but its free tier's 10-requests/month cap
turned out to be impractical for real use, so the data source was switched.

1. Request an API key by emailing openapi@totalizator.pl (see the instructions on
   [developers.lotto.pl](https://developers.lotto.pl)'s own Swagger page), or via
   https://www.lotto.pl/kontakt. There's no self-service signup — access is provisioned
   manually per requester.
2. In `local.properties`, add:
   ```
   LOTTO_API_KEY=your_key_here
   ```
3. Rebuild. `AppContainer` reads this into `BuildConfig.LOTTO_API_KEY` at build time —
   the key never gets committed (`local.properties` is git-ignored).

Without a key, the app works fully offline on the bundled sample data and local
Statistics/Generate features — it just never overwrites the "SAMPLE DATA" badge.

### Winners by Country, and currency conversion

The Results screen has a "Winners by Country" section: it fetches
`draw-prizes/eurojackpot/{drawSystemId}` for the latest draw and shows, per prize
tier, how many winners each country had (grouping LOTTO OpenAPI's per-ticket
`countriesPrizes` entries by country — see the doc comment on
`LottoOpenApiRemoteDataSource.fetchPrizeBreakdown`). Only the tiers LOTTO OpenAPI
actually reports a breakdown for are shown (in practice just the top couple of
tiers) — no other tiers are padded in.

**City-level winner locations are not shown, on purpose.** LOTTO OpenAPI's full
spec was checked for this: the only endpoint with city/place data
(`/lotteries/highest-wins`) explicitly excludes EuroJackpot from its supported
`games` filter, and the EuroJackpot prize endpoint's own schema has no place/city
field at all. Rather than fabricate or omit this silently, the app says so directly
in the breakdown UI (`results_country_no_city_data` in strings.xml).

Jackpot and most prize amounts come from LOTTO OpenAPI in **PLN** — but the
"Winners by Country" per-country breakdown amount (`CountryPrize.prizeValueEur`)
is the one exception, and it's in **EUR**, not PLN (see below for why, and
`CountryPrize`'s doc comment for the full reasoning). A currency picker
(EUR/USD/KRW/GBP/JPY/PLN — see `SupportedCurrency`) on both Home and Results lets
the user see amounts converted into their preferred currency, using the free,
keyless [Frankfurter](https://api.frankfurter.dev) exchange-rate API (ECB daily
reference rates). The chosen currency is shared across screens and persisted
(`CurrencyRepository`/`CurrencyPreferences`), and rates are cached for 6 hours
(`CurrencyPreferences.COOLDOWN_MILLIS`) since they only actually change once a
banking day. No Settings screen exists yet, so this picker is inline rather than
in a menu — see "What's deferred to a later phase" below.

**The "Winners by Country" amount is in EUR, not PLN (fixed 2026-10-13).** A user
noticed the tier 2/3 amounts shown in that section looked like EUR figures
mislabeled as PLN. Checking it: `countriesPrizes` is a *cross-border* list — one
shared value meant to apply to winners in Poland, Germany, Denmark, etc. all at
once, each with their own native currency, and the API gives no per-entry
currency field. A single number meant to be read the same way regardless of
country only makes sense in EuroJackpot's common settlement currency, EUR. This
was confirmed directly against the real numbers: for the 9 Oct 2026 draw,
euro-jackpot.net's own EUR prize breakdown reported €11,558,578.60 for the 5+1
tier and €180,126.60 for the 5+0 tier — the same figures (and the same
uniform-across-countries pattern) `countriesPrizes` returns, which would be an
implausibly small PLN amount for a jackpot-adjacent tier but lines up exactly as
EUR. Fixed by renaming `CountryPrize.prizeValuePln` → `prizeValueEur`, formatting
it as EUR natively (`CurrencyFormat.formatNative(..., "EUR")`), and adding
`CurrencyFormat.convertAndFormatFromEur` (converts EUR → PLN via the reciprocal
of the EUR/PLN rate already in `ratesPerPln`, then PLN → any other selected
currency as before) so the currency picker still works correctly for this field.
This does **not** affect `TierPrizeAmount` (the "Prize amount when a saved number
wins" figure below) — that one is still genuinely PLN, since it's Poland's own
national per-tier payout table, not a cross-border figure.

### Prize amount when a saved number wins

Results' "Check My Numbers" section (My Numbers vs. the latest draw, via
`CompareNumbersUseCase`) shows the actual PLN prize amount — plus the converted
amount in whichever currency is selected — next to any saved number that landed on
a prize tier, not just the tier number.

This reuses the exact same `draw-prizes/eurojackpot/{drawSystemId}` call "Winners by
Country" already makes, just reading a different part of the same response: that
endpoint also returns a `prizes` object keyed `"1"`..`"12"`, which (live-verified
across two separate draws) is Poland/LOTTO's own official per-tier winner count and
PLN payout for that specific draw — see `TierPrizeAmount` and the doc comment on
`PrizeBreakdown` for the full detail. It's fetched automatically the moment a
comparison actually has a prize tier (no need to also open "Winners by Country"
first), and cached the same way.

**One documented open caveat, stated plainly rather than hidden**: for the two tiers
that also appear in `countriesPrizes` (the "Winners by Country" breakdown), the two
figures were checked against each other on a real draw and didn't numerically agree
even after converting units — the `countriesPrizes` amount is uniform across every
country as expected for a pan-European pooled EUR tier, but the `prizes` table's PLN
amount for the same tier/draw doesn't come out to a plausible EUR→PLN conversion of
it either. Now that the currency mismatch itself is understood (see above —
`countriesPrizes` is EUR, `prizes` is PLN), part of the original gap is explained, but
a residual difference remains unreconciled (possibly Poland's "Multiplikator" add-on,
tax handling, or a timing/rounding difference between the two sub-objects of the same
API response). Rather than silently pick one as "correct" without being able to
confirm the residual gap, this app treats `prizes` as Poland's own domestic-payout
figure on its own terms (the right answer to "what would LOTTO Poland actually pay a
Polish ticket for this tier"), separately from the cross-border reference amount
`countriesPrizes` shows.

If LOTTO Poland recorded zero winners for a tier on that specific draw (which can
happen even when a saved number matches it, since a saved number is the user's own
combination, not necessarily one that was actually purchased), the app says so
instead of showing a fabricated "₩0" — there's no real pari-mutuel amount to report
when nobody won that tier that week.

### Ticket scanning (OCR)

My Numbers has a camera/gallery "Scan Ticket" entry point (the `DocumentScanner`
FAB, plus a smaller gallery button in the header and empty state) alongside the
existing manual "+" add. It runs the photo through Google ML Kit's on-device Latin
text recognizer (`TicketTextRecognizer`) — free, no API key, the photo never leaves
the phone — then `ParseTicketTextUseCase` regex-matches the recognized text for the
5 main numbers, 2 Euro numbers, and draw date.

**Camera capture is full-resolution, not a preview thumbnail.** The camera path uses
`ActivityResultContracts.TakePicture()` with a real `content://` output Uri (via
`TicketPhotoStore.createScanCaptureUri` + the `FileProvider` declared in
`AndroidManifest.xml`/`res/xml/file_paths.xml`), not `TakePicturePreview()` — reported
directly as OCR "not working well" from camera photos, and `TakePicturePreview()` is
the reason why: it only ever hands back a tiny preview-quality Bitmap regardless of the
phone's actual camera resolution, nowhere near enough detail for ML Kit to reliably
read a ticket's small printed digits. `TicketPhotoStore.decodeForRecognition` then
downsamples that full-resolution photo to a size still generous enough for OCR
(2048px on the long side) without risking an OutOfMemoryError on a 12+ MP source image,
while a separate, smaller copy (1280px) is kept for the saved number's reference-photo
thumbnail. Gallery picks go through the same two functions for consistency, though they
were never the reported problem (a photo picked from the gallery was already
full-resolution).

**Scoped to one physical ticket format**: a Poland/Totalizator Sportowy retail-terminal
EuroJackpot slip (the format verified against a real purchased ticket and its owner-
confirmed numbers — see the doc comment on `ParseTicketTextUseCase`). An online-purchase
screenshot or a different country's ticket layout will simply fail to match some or all
fields rather than guess.

**Every scanned field is shown back as an editable draft, never auto-saved.** The add
dialog reused for manual entry gets a scan-review mode (`TicketScanStatusBanner` in
`AddNumberDialog`) that shows per-field recognized/not-recognized status and a warning
if a second board looks present (this parser only ever reads the first board). This
isn't a hedge — the reference ticket used to build this feature was itself first
misread by hand (a "30" first looked like "50" at a glance before the ticket's owner
corrected it), which is exactly the failure mode OCR is also prone to. Saved entries
from this flow are tagged `NumberSource.OCR_SCAN` so they're distinguishable from
manual entries in the list, but otherwise go through the same validation
(`NumberValidation`) and storage as any other saved number.

The ticket doesn't print an official EuroJackpot round number at all (it prints a draw
date/time) — this app has no verified source for that numbering anyway (see
`SavedNumber.drawRoundLabel`'s doc comment), so the scanner extracts the draw date into
`targetDrawDate` and surfaces the terminal reference number as an editable note, never
as a fabricated round label.

### Why sample data instead of scraped real history

Building an accurate 2012-onward EuroJackpot archive requires either an official feed
or careful scraping/verification that was out of scope to do reliably in this pass —
and for an app whose entire premise is trustworthy lottery numbers, shipping
subtly-wrong "real" historical numbers would be worse than clearly labelling
placeholder data as a placeholder. `SampleDrawSeeder` generates a deterministic
(fixed-seed) fake draw history instead, tagged `isSampleData = true` end to end (Room
column → domain model → UI badge in Home/Results, see `common_sample_data_notice` in
`strings.xml`). The first successful real sync automatically clears the sample rows
(`LotteryRepositoryImpl.syncLatestResults`).

`data/remote/RemoteLotteryDataSource` is a plain interface specifically so a second
provider (an official feed, once you have access to one) can be added or swapped in
without touching the DAO, use cases, or UI.

## Architecture

```
app/src/main/java/com/eurojackpotai/app/
  core/           theme, Constants-equivalent (AppContainer, GenericViewModelFactory), util
  data/
    local/        Room entities, DAOs, AppDatabase, sample data seeder
    remote/       magayo Retrofit service + DTOs + RemoteLotteryDataSource
    ocr/          TicketTextRecognizer (ML Kit wrapper, see "Ticket scanning (OCR)")
    repository/   LotteryRepositoryImpl, SavedNumberRepositoryImpl
  domain/
    model/        DrawResult, SavedNumber, GenerationMode, statistics/prize models
    repository/   repository interfaces (what domain/presentation depend on)
    usecase/      GenerateNumbersUseCase, SmartCoverageGenerator, ComputeStatisticsUseCase,
                  CompareNumbersUseCase
  presentation/
    navigation/   bottom nav + NavHost
    home/ generate/ statistics/ mynumbers/ results/   one package per screen, each
                  with a ViewModel (StateFlow) + UiState + Composable screen
    components/   NumberBall, LotteryCombinationRow, SectionCard, HorizontalBarList
```

**No DI framework.** `core/AppContainer.kt` is a small manual dependency container
(plain `by lazy` singletons). At this size — one module, five screens — Hilt/Dagger's
annotation processing adds real build complexity without a matching payoff; see the
doc comment on `AppContainer` for the fuller reasoning. Each screen gets its
ViewModel via `GenericViewModelFactory { SomeViewModel(app.container.xyz) }`.

**No image-loading/DI/analytics libraries** beyond what each phase actually needs, for
the same reason — the spec explicitly asks not to add unnecessary complexity.

## Versions used

- Kotlin 2.3.20, Compose compiler via the Kotlin Compose Gradle plugin (no manual
  `kotlinCompilerExtensionVersion` pinning needed)
- Compose BOM 2025.12.01 (compose-ui 1.11.4), Material 3 — pinned below the newest BOM
  on purpose: BOMs from 2026 onward ship compose-ui 1.12.x, which requires compileSdk 37
  and Android Gradle Plugin 9.1.0+, both ahead of this project's AGP 8.13.0/compileSdk 36
- Android Gradle Plugin 8.13.0 / Gradle 8.13
- `compileSdk` / `targetSdk` = 36 (Android 16), matching Google Play's August 2026
  target-API requirement; `minSdk` = 26 (Android 8.0)
- Room 2.7.0, Retrofit 2.11.0 + kotlinx.serialization (no Moshi/Gson — one less codegen
  step), OkHttp 4.12.0
- ML Kit Text Recognition 16.0.1 (on-device, Latin script) — ticket-photo OCR, see
  "Ticket scanning (OCR)" above

Because these were current as of September 2026 but nothing here could be verified
against a live Gradle sync (see below), let Android Studio's Upgrade Assistant bump
anything that's since moved past its stable channel.

## Known limitation: this build was never compiled

The environment this project was generated in has no Android SDK and its network
egress blocks Google's Maven repo (`dl.google.com`) and Maven Central, so **no Gradle
sync or build ever actually ran against this code** — everything here was written and
manually cross-checked (types, imports, resource references, brace/paren balance)
rather than compiler-verified. Everything that *could* be checked without an Android
toolchain was: this repo's Kotlin files balance every brace/paren, and there are no
stray characters or naming collisions that a search could catch.

Practically, expect Android Studio's first sync/build to be the actual first compile —
most likely outcome is it builds cleanly, but treat any error Android Studio shows you
as more authoritative than this document, and don't hesitate to point specific errors
back for a fix.

## Tests

- `app/src/test/.../domain/usecase/`: `LotteryNumberGeneratorTest`, `StatisticsTest`,
  `ResultCheckerTest`, `SmartCoverageTest` — plain JUnit4, run with `./gradlew test`, no
  emulator needed.
- `app/src/androidTest/.../data/local/DatabaseTest`: Room CRUD test, needs a device or
  emulator — `./gradlew connectedAndroidTest`.

## Smart Coverage (Phase 4)

`domain/usecase/SmartCoverageGenerator.kt` implements product spec section 8. It does
**not** loop plain Random N times. Instead, for a request of N games it:

1. Builds a pool of up to `max(N * 15, 200)` candidate combinations, keeping only
   "well-formed" ones — odd/even split not 5/0 or 0/5, sum within the theoretical
   range's middle band (70–190 out of 15–240), main numbers spread across at least 3 of
   the five 1-10/…/41-50 buckets, and no run of 3+ consecutive numbers.
2. Greedily selects N of those candidates so that, as a set, they reuse the same
   main/Euro numbers as little as possible (tracked via per-number usage counts),
   maximizing how many distinct numbers the whole batch covers.

This changes only which *equally-likely* combinations get suggested together — it is
never described as, and must never be changed to imply, a higher chance of winning for
any individual game (see `generation_disclaimer` / `mode_smart_coverage_desc` in
`strings.xml`, and the doc comment on `SmartCoverageGenerator`). Tapping a number in a
Smart Coverage combination doesn't open the "why this number" explanation sheet the
other modes have, since coverage is a property of the whole batch, not of any one
number picked for its own stats.

## What's deferred to a later phase

Kept out of this pass by the "core first" scope, with scaffolding already in place
where it made the eventual add-on cheaper:

- **AI Mix** — `GenerationMode.AI_MIX.isAvailable = false`; it shows in the Generate
  screen's mode list with a "coming soon" badge (spec section 6's full 8-mode menu is
  visible from day one) but throws if invoked. Statistical Mix already implements the
  same weighted-scoring idea (frequency + recency + gap) the spec describes for AI Mix
  in section 22 — the two are effectively the same algorithm under different names in
  the spec — so AI Mix was left alone rather than duplicating it under a second name;
  worth clarifying with product whether it should stay a distinct mode before building it.
- **AdMob / Play Billing** — not wired up; `ads/` and `billing/` packages weren't created
  since there's nothing real to put in them yet without your AdMob/Play Console IDs.
- **Onboarding, Settings actions, notifications** — string resources and the Settings
  menu labels exist; the screens/behaviors don't yet.
- **Locales beyond English/Korean** — every user-facing string is already in
  `strings.xml`/`values-ko/strings.xml` (never hardcoded in a Composable), so adding
  `values-pl`, `values-de`, etc. is a translation pass, not a code change.
- **Real per-draw prize amounts** — `PrizeTier` models the 12 official match tiers
  (which combination wins which tier) but deliberately carries no currency amounts,
  since real prize money varies per draw/country and needs an official data source.

## EuroJackpot rule/tier sources

- Prize tier table (12 standard tiers, match combinations): [Eurojackpot – Wikipedia](https://en.wikipedia.org/wiki/Eurojackpot)
- Draw data API: [magayo Lottery Data API](https://www.magayo.com/lottery-feeds/lottery-data-api/), [Supported Jackpots](https://www.magayo.com/lottery-docs/api/supported-jackpots/)
- Android Gradle Plugin / target API requirement: [Meet Google Play's target API level requirement](https://developer.android.com/google/play/requirements/target-sdk)
