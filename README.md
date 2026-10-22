# QuarkWeiboAutomation

Playwright + JUnit 5 UI automation for **Quark Dynasty**, the HK-brand member site at
https://test-v2.138hk.vip. Scaffolded from a manual exploratory testing session on 2026-10-16
(responsive layout pass, auth test cases, register test cases, wallet-currency regression).

## Setup

1. Install Playwright's browsers once: `./mvnw exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"`
   (or run any test once - Playwright downloads Chromium on first use if missing).
2. Nothing else to configure for a local run: `Config/config.properties` already points at the
   test environment and the qa00002 account.

## Running

Always through the committed wrapper, never a bare `mvn` (see `use-committed-maven-wrapper`):

```
./mvnw test                          # every *Test class
./mvnw test -Dsuite=AuthSuite         # login + register only
./mvnw test -Dsuite=WalletSuite       # the currency-banner regression only
./mvnw test -Dsuite=ResponsiveSuite   # the five-device home page layout cases, both UI versions
./mvnw test -Dsuite=HomeSuite         # home page header/wallet-panel checks, both UI versions
./mvnw test -Dsuite=DepositSuite      # deposit page checks, both UI versions (unconfirmed - see below)
./mvnw test -Dsuite=WithdrawalSuite   # withdrawal page checks, both UI versions (unconfirmed - see below)
./mvnw test -Dsuite=TransferSuite     # wallet-to-wallet transfer checks, both UI versions (unconfirmed - see below)
./mvnw test -Dsuite=WinWinCitySuite   # live-casino game checks, both UI versions (unconfirmed - see below)
./mvnw test -Dsuite=UxJourneySuite    # one end-to-end journey per device, both UI versions
./mvnw test -Dtest=LoginTest          # one class
```

Each of those seven area suites also comes split by UI version, for running just one side of an
area (e.g. only New UI broke, no need to also run Classic UI while fixing it):

```
./mvnw test -Dsuite=ResponsiveSuiteNewUI    / -Dsuite=ResponsiveSuiteClassicUI
./mvnw test -Dsuite=HomeSuiteNewUI          / -Dsuite=HomeSuiteClassicUI
./mvnw test -Dsuite=DepositSuiteNewUI       / -Dsuite=DepositSuiteClassicUI
./mvnw test -Dsuite=WithdrawalSuiteNewUI    / -Dsuite=WithdrawalSuiteClassicUI
./mvnw test -Dsuite=TransferSuiteNewUI      / -Dsuite=TransferSuiteClassicUI
./mvnw test -Dsuite=WinWinCitySuiteNewUI    / -Dsuite=WinWinCitySuiteClassicUI
./mvnw test -Dsuite=UxJourneySuiteNewUI     / -Dsuite=UxJourneySuiteClassicUI
```

And two suites cross every area by UI version instead of staying within one area - run "does
New UI hold up everywhere" (or Classic UI) in one go:

```
./mvnw test -Dsuite=NewUiSuite        # every area's New UI half, together
./mvnw test -Dsuite=ClassicUiSuite    # every area's Classic UI half, together
./mvnw test -Dcase=UI-001             # one case, by id, wherever it lives
./mvnw test -Dheadless=false          # watch the browser (also the config.properties default)
```

After a run, two reports can be built from its Surefire results:

```
./mvnw exec:java               # the Excel workbook: target/E2E-Test-Report.xlsx
./mvnw exec:java@html-report   # the HTML test case reports: docs/reports/index.html
```

Both are also written automatically at the end of every `./mvnw test` run (by
`HtmlReportListener` and `ExtentReportListener`, registered as JUnit Platform listeners), so
`docs/reports/index.html` and `docs/reports/extent/index.html` are always current for the run
that just finished - the `exec:java` goals above are only for rebuilding them by hand from
Surefire XML that is already on disk, without re-running anything.

## Layout

- `src/main/java/org/sengmean/base` - `BasePage`, shared by every page object: overlay/welcome-
  banner dismissal, and the New UI / Classic UI picker (see below).
- `src/main/java/org/sengmean/config` - `ConfigDataProvider`, the settings precedence chain.
- `src/main/java/org/sengmean/page/auth` - Login and Register page objects.
- `src/main/java/org/sengmean/page/wallet` - the currency switcher and the landing-page banner
  check the wallet regression suite drives. Deliberately scoped to just that; see
  `src/main/java/org/sengmean/page/home` below for the separate, broader home-page object.
- `src/main/java/org/sengmean/page/home` - `HomePage`, the header/wallet-panel checks
  `HomePageNewUiTest`/`ClassicUiTest` drive. Kept apart from `page/wallet/HomePage` on purpose -
  see "A note on two HomePage classes" below.
- `src/main/java/org/sengmean/page/payment` - `DepositPage`, `WithdrawalPage`, `TransferPage` -
  ported structurally from NewWeibo_automation; every locator and limit is an unconfirmed guess,
  see "Ported-but-unconfirmed areas" below.
- `src/main/java/org/sengmean/page/casino` - `WinWinCityGamePage` - ported structurally from
  NewWeibo_automation; whether this site has any live-casino game at all is unconfirmed, see
  "Ported-but-unconfirmed areas" below.
- `src/main/java/org/sengmean/report` - `CaseLog` (so page objects can narrate a step too),
  `ExcelReportGenerator`, `HtmlReportGenerator` and `ReportMailer` - ported from the sibling
  NewWeibo_automation / Admin_weibo projects' reporting infrastructure, trimmed to this project's
  layout and with the client-workbook join dropped entirely (this project has no workbook): every
  report column comes from the test sources, the Surefire XML, the run's screenshots and its
  `CaseLog` step trail.
- `src/test/java/org/sengmean/base` - `BaseTest`, the Playwright browser lifecycle (Chromium, and
  WebKit on demand for the `Device` profiles below).
- `src/test/java/org/sengmean/support` - `Device` (the five device profiles the responsive and UX
  journey cases run on) and `CaseIdFilter` (wires up `-Dcase=`).
- `src/test/java/org/sengmean/report` - `HtmlReportListener` and `ExtentReportListener`, the
  JUnit Platform listeners that write both reports at the end of every run.
- `src/test/java/org/sengmean/tests/<area>` - test classes, mirroring `page/<area>`. Every area
  added after the initial scaffold follows the New UI / Classic UI split below: `tests/layout`
  (`ResponsiveLayoutNewUiTest`/`ClassicUiTest`), `tests/home` (`HomePageNewUiTest`/`ClassicUiTest`),
  `tests/payment` (`DepositNewUiTest`/`ClassicUiTest`, `WithdrawalNewUiTest`/`ClassicUiTest`,
  `ManualTransferNewUiTest`/`ClassicUiTest`), `tests/casino` (`WinWinCityNewUiTest`/`ClassicUiTest`),
  `tests/ux` (`UxJourneyNewUiTest`/`ClassicUiTest`).
- `src/test/java/org/sengmean/suites/<area>` - one `@Suite` per area covering both UI versions
  (`ResponsiveSuite`, `HomeSuite`, `DepositSuite`, `WithdrawalSuite`, `TransferSuite`,
  `WinWinCitySuite`, `UxJourneySuite`, plus `AuthSuite`/`WalletSuite`), each in its own
  subpackage mirroring `page/<area>` and `tests/<area>` (`suites/layout`, `suites/home`,
  `suites/payment` for all three payment suites, `suites/casino`, `suites/ux`, `suites/auth`,
  `suites/wallet`); each of the seven also comes split in two (`<Area>SuiteNewUI` /
  `<Area>SuiteClassicUI`) for running just one UI version of one area, in the same subpackage.
  `NewUiSuite`/`ClassicUiSuite` stay at the `suites` root - they cross every area by UI version
  instead of belonging to one. `-Dsuite=` still finds any of these by simple name regardless of
  subpackage (Surefire's include is a depth-agnostic `**/${suite}.java`), and
  `HtmlReportGenerator`'s per-suite report pages still work the same way (it already walks the
  whole `suites` tree recursively, subpackages included).
- `docs/reports` - the generated HTML test case reports (`index.html` plus one page per suite)
  and the Extent Spark dashboard (`docs/reports/extent`), rewritten at the end of every run.

## Selector confidence - read this before trusting a red case

This project was scaffolded from a manual session driven by screenshots and coordinate clicks,
not from a captured accessibility tree. Locators for buttons and field labels are written from
what was visibly on screen and are reasonably safe (Playwright role/text queries), but a handful
of CSS-based locators for things that were never inspected in the DOM - the VIP promo popup, the
chat panel, the currency dropdown trigger, the hero banner container - are best-effort and called
out with a comment where they are. Confirm these against the live DOM on the first real run.
The one exception is the welcome banner (`BasePage.dismissWelcomeBanner()`) - its selector was
confirmed against this exact site by the sibling Admin_weibo project, not guessed here.

## Known defects this project exists to track

- **Hero banner breaks on every wallet currency except HKD** (`CurrencyBannerTest`). USDT, TRX
  and USD show no banner at all; THB shows a broken placeholder image unrelated to the site (a
  stock photo of a ladybug on desktop, a QR code on mobile). Reproducible after a full reload.
- **Stale inline validation messages** on both Login and Register: correcting a field to a valid
  value does not clear its error text until the next submit attempt.
- **Raw backend error code** (`RegisterTest.invalidContactNumber_showsRawBackendErrorCode`): an
  invalid contact number surfaces "Status error -201" verbatim instead of a readable message.
- Responsive layout on *this* site is now automated, split by UI version and by visitor vs.
  signed-in member (`ResponsiveLayoutNewUiTest`, `UI-001`-`UI-010`; `ResponsiveLayoutClassicUiTest`,
  `UI-011`-`UI-020` - see the note below). The earlier finding on the sibling site
  test-v2.1083int.co - a cropped WELIVE promo banner and a Lucky-Spin-over-footer overlap at
  tablet/phone widths - is unrelated and this project still does not cover that site.
- Classic UI's own hero banner is unconfirmed (`ResponsiveLayoutClassicUiTest` logs what it finds
  rather than asserting on it - see that class's Javadoc).

## A note on case ids

Every area has its own invented prefix - a letter prefix, a hyphen, a zero-padded number - which
is what the ported `HtmlReportGenerator`'s id pattern (lifted from the sibling NewWeibo_automation
/ Admin_weibo projects) expects: it needs a hyphen before the trailing digits to recognise an id
at all, so a bare `TC1` (what `LoginTest`/`RegisterTest` used before) is not read as one and such
a case's report row carries no id.

| Prefix | Area | New UI ids | Classic UI ids |
|---|---|---|---|
| `LOGIN-` | `LoginTest` (no UI split - see below) | `001`-`012` | - |
| `REGISTER-` | `RegisterTest` (no UI split) | `001`-`005` | - |
| `UI-` | `ResponsiveLayoutNewUiTest`/`ClassicUiTest` | `001`-`010` | `011`-`020` |
| `HOME-` | `HomePageNewUiTest`/`ClassicUiTest` | `001`-`007` | `008`-`014` |
| `DEPOSIT-` | `DepositNewUiTest`/`ClassicUiTest` | `001`-`010` | `011`-`020` |
| `WITHDRAWAL-` | `WithdrawalNewUiTest`/`ClassicUiTest` | `001`-`012` | `013`-`024` |
| `TRANSFER-` | `ManualTransferNewUiTest`/`ClassicUiTest` | `001`-`013` | `014`-`026` |
| `WINWIN-` | `WinWinCityNewUiTest`/`ClassicUiTest` | `001`-`006` | `007`-`012` |
| `UX-` | `UxJourneyNewUiTest`/`ClassicUiTest` | `001`-`005` | `006`-`010` |

All of these prefixes are invented: this project has no client test-case workbook to take an id
from, unlike those sibling projects, so there is no established convention to follow instead.
Login and Register are the two exceptions to the New UI / Classic UI split - the login/register
forms are reached independently of the home page's UI-version picker, with no evidence either
form differs by UI version.

## The New UI / Classic UI picker

A fresh visitor on this site is asked "Choose your preferred interface to get started.", with a
Classic Interface card and a New Interface card; the choice persists in `localStorage` as
`ui-version`. `BasePage.chooseNewUi()` / `chooseClassicUi()` answer it (ported from the sibling
Admin_weibo project's `ClientCategoryHomePage`, which only ever needed New Interface);
`isNewUiChosen()` / `isClassicUiChosen()` / `isUiVersionChosen()` confirm the choice actually took
effect, rather than just that the picker was clicked - every New UI / Classic UI test class
asserts this immediately after choosing, and again after any sign-in or navigation that could
have silently reverted it. Every device case opens a fresh, isolated browser context, so every
case answers the picker itself.

## A note on two `HomePage` classes

`org.sengmean.page.wallet.HomePage` and `org.sengmean.page.home.HomePage` are deliberately two
different classes, not a naming accident. The `wallet` one predates this expansion and is scoped
to exactly what `CurrencyBannerTest` and the `ResponsiveLayout*Test` classes need (`navigateTo()`,
`hasHeroBanner()`); the `home` one is scoped to the header/wallet-panel checks
`HomePageNewUiTest`/`ClassicUiTest` need. They were kept apart rather than merged so that growing
one area's home-page checks can't change what the other three already-passing areas depend on -
if you need something from both, import both; don't merge them without re-reading all four
callers first.

## Ported-but-unconfirmed areas

`DepositNewUiTest`/`ClassicUiTest`, `WithdrawalNewUiTest`/`ClassicUiTest`,
`ManualTransferNewUiTest`/`ClassicUiTest` and `WinWinCityNewUiTest`/`ClassicUiTest` were all
structurally ported from NewWeibo_automation's own suites, which test a different client site
(test-v2.1083.city, "Weibo Empire") with its own bank/payment-channel names, its own deposit and
withdrawal limits, its own wallet model, and its own game catalogue. **None of that business
detail is confirmed to exist on Quark Dynasty.** Each class was deliberately cut down from
NewWeibo's much larger case count to a small set of generic checks (an option exists, an amount
below the page's own *stated* minimum is refused, a destructive action is never actually
confirmed) with every locator flagged in a "Selector confidence" Javadoc section, and every
amount/limit read live from the page rather than carried over as a Weibo Empire literal. Treat a
red case in any of these four areas as "this locator or assumption needs correcting against the
live DOM" before treating it as a real site defect - see each class's own Javadoc for exactly
what was ported, what was generalized, and what was skipped outright.

`ManualTransferTest` and `WinWinCityBettingTest` go further: nothing in this project's own
history confirms a wallet-to-wallet transfer feature or any live-casino game exists on Quark's
site at all. Both classes say so prominently in their own Javadoc. No test in either payment area
(`Deposit`, `Withdrawal`, `Transfer`) ever presses a real confirm button - every one stops at a
confirmation prompt and cancels, so running these suites cannot move real money or balance.
