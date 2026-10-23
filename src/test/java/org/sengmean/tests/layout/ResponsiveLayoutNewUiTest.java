package org.sengmean.tests.layout;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.wallet.GameCategoryPage;
import org.sengmean.page.wallet.HomePage;
import org.sengmean.page.wallet.MemberAreaPage;
import org.sengmean.page.wallet.StaticContentPage;
import org.sengmean.report.CaseLog;
import org.sengmean.support.Device;
import org.sengmean.support.GameCategory;
import org.sengmean.support.MemberPage;
import org.sengmean.support.StaticPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every page this project could confirm a URL for, on the New UI, across the five {@link Device}
 * profiles this project tests against: a Windows PC and an Android phone in Chromium, a Mac, an
 * iPad and an iPhone in WebKit, since Safari is WebKit and a Chromium window pretending to be an
 * iPhone exercises the wrong engine. One class rather than four, so every New UI responsive case
 * lives and reports in one place - {@link ResponsiveLayoutClassicUiTest} is its Classic UI twin.
 *
 * <h2>Four page groups, one shape</h2>
 * <ul>
 *   <li><b>The home page</b> ({@code onPc}..{@code memberOnAndroid}, not nested) - as a visitor and
 *       as a signed-in member. Clears the VIP promo popup
 *       ({@link org.sengmean.base.BasePage#dismissOverlays()}), answers the UI-version picker with
 *       New Interface ({@link org.sengmean.base.BasePage#chooseNewUi()}), clears the chat panel
 *       that opens afterwards, then confirms New Interface is actually the chosen option
 *       ({@link org.sengmean.base.BasePage#isNewUiChosen()} - the picker is down and
 *       {@code ui-version} reads {@code "new"}, not just that the picker was answered) before
 *       asserting nothing runs off the side of the screen and the hero banner renders
 *       ({@link HomePage#hasHeroBanner()}). The member rows sign in first
 *       ({@link org.sengmean.base.BaseTest#signIn(Page)}), return to the home page, and re-run the
 *       same checks plus confirm the header still shows the signed-in state.</li>
 *   <li><b>The nine game-category pages</b> ({@link GameCategory} - Slots, Horse Race, Lives
 *       Casino, E-Sport, Fish Game, Lotteries, Sport, Card Game, 真人直播), one {@code @Nested}
 *       class per category. Classic UI's nav bar (and its footer "Site Maps") carries a tenth
 *       entry, Promotions, that New UI's nav does not expose at all; left out here so the two UI
 *       suites stay a fair side-by-side comparison of the same pages.</li>
 *   <li><b>The eight static content pages</b> ({@link StaticPage} - About Us, Responsible Gaming,
 *       Banking Options, Terms of Use, Privacy Policy, the three How-to guides), one
 *       {@code @Nested} class per page. None of these are linked from New UI's own home page -
 *       it renders no footer at all - but every path was confirmed live on 2026-10-22 to still
 *       render correctly under New UI when opened directly, which is why this group exists here
 *       rather than being skipped as "New UI doesn't have these pages".</li>
 *   <li><b>The ten signed-in member-area pages</b> ({@link MemberPage} - Personal Information,
 *       Password, Wallet, Deposit, Withdrawal, Rebate claim, Referral link, Referral bonus,
 *       Membership program, Records), one {@code @Nested} class per page. The header's own
 *       "Transfer" shortcut opens {@link MemberPage#WALLET} directly, not an eleventh page - see
 *       that enum's Javadoc. Every case signs in as the shared QA account, so this class should
 *       not be run alongside another run that is also signed in.</li>
 * </ul>
 *
 * <h2>Why a direct {@code page.navigate()} for the last three groups</h2>
 * Every category/static/member case opens its page with a direct
 * {@code page.navigate()} ({@link GameCategoryPage}, {@link StaticContentPage},
 * {@link MemberAreaPage}) rather than clicking the nav bar or a footer link, which on a narrow
 * viewport may not even be the same control (collapsed behind a menu, scrolled off-screen, or in
 * New UI's case simply not rendered at all for the static pages). Confirmed live: landing on any
 * of these paths cold, with no {@code ui-version} in {@code localStorage} yet, renders Classic UI
 * silently with no picker at all, unlike the home page - so every case still has to visit the home
 * page and answer the picker first (and, for the member group, sign in) before navigating on.
 *
 * <h2>What every case checks</h2>
 * The same locator-free pair throughout: the UI choice has survived the navigation
 * ({@code ui-version} still reads "new"), and nothing runs off the side of the screen at the
 * device's own viewport ({@code document.body.scrollWidth}). The home page additionally asserts
 * the hero banner renders, confirmed against New UI specifically; the other three groups make no
 * content-specific assertion, per this project's "Selector confidence" caution in the README.
 *
 * <h2>Case ids</h2>
 * {@code UI-001}-{@code UI-010} (home page: visitor {@code 001}-{@code 005}, member
 * {@code 006}-{@code 010}), {@code CATEGORY-001}-{@code CATEGORY-045}, {@code STATIC-001}-
 * {@code STATIC-040}, {@code MEMBER-001}-{@code MEMBER-050}, five per page (PC, Mac, iPad, iPhone,
 * Android). Classic UI continues each range in {@link ResponsiveLayoutClassicUiTest}. All invented,
 * as there is no client test-case workbook for this project to take ids from.
 *
 * <h2>WebKit</h2>
 * The Mac, iPad and iPhone cases need WebKit installed ({@code npx playwright install webkit});
 * see {@link org.sengmean.base.BaseTest#webkitOrAbort()}. A case whose engine cannot launch
 * aborts (Not run) naming the install it needs, rather than failing the whole class.
 */
public class ResponsiveLayoutNewUiTest extends BaseTest {

    // =====================================================================
    // Home page
    // =====================================================================

    @Test
    @DisplayName("UI-001: New UI home page layout is responsive on PC, as a visitor (Windows, Chrome, 1920x1080)")
    void onPc() {
        layoutHolds(Device.DESKTOP_PC);
    }

    @Test
    @DisplayName("UI-002: New UI home page layout is responsive on Mac, as a visitor (macOS, Safari/WebKit, 1440x900)")
    void onMac() {
        layoutHolds(Device.DESKTOP_MAC);
    }

    @Test
    @DisplayName("UI-003: New UI home page layout is responsive on iPad, as a visitor (iPadOS, Safari/WebKit, 820x1180, touch)")
    void onIpad() {
        layoutHolds(Device.IPAD);
    }

    @Test
    @DisplayName("UI-004: New UI home page layout is responsive on iPhone, as a visitor (iOS, Safari/WebKit, 390x844, touch)")
    void onIphone() {
        layoutHolds(Device.IPHONE);
    }

    @Test
    @DisplayName("UI-005: New UI home page layout is responsive on Android, as a visitor (Pixel 7, Chrome, 412x915, touch)")
    void onAndroid() {
        layoutHolds(Device.ANDROID);
    }

    @Test
    @DisplayName("UI-006: New UI home page layout is responsive on PC, signed in (Windows, Chrome, 1920x1080)")
    void memberOnPc() {
        memberLayoutHolds(Device.DESKTOP_PC);
    }

    @Test
    @DisplayName("UI-007: New UI home page layout is responsive on Mac, signed in (macOS, Safari/WebKit, 1440x900)")
    void memberOnMac() {
        memberLayoutHolds(Device.DESKTOP_MAC);
    }

    @Test
    @DisplayName("UI-008: New UI home page layout is responsive on iPad, signed in (iPadOS, Safari/WebKit, 820x1180, touch)")
    void memberOnIpad() {
        memberLayoutHolds(Device.IPAD);
    }

    @Test
    @DisplayName("UI-009: New UI home page layout is responsive on iPhone, signed in (iOS, Safari/WebKit, 390x844, touch)")
    void memberOnIphone() {
        memberLayoutHolds(Device.IPHONE);
    }

    @Test
    @DisplayName("UI-010: New UI home page layout is responsive on Android, signed in (Pixel 7, Chrome, 412x915, touch)")
    void memberOnAndroid() {
        memberLayoutHolds(Device.ANDROID);
    }

    // =====================================================================
    // Game-category pages
    // =====================================================================

    @Nested
    @DisplayName("真人直播")
    class LiveStream {
        @Test
        @DisplayName("CATEGORY-001: New UI 真人直播 category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.LIVE_STREAM, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-002: New UI 真人直播 category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.LIVE_STREAM, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-003: New UI 真人直播 category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.LIVE_STREAM, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-004: New UI 真人直播 category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.LIVE_STREAM, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-005: New UI 真人直播 category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.LIVE_STREAM, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Slots Game")
    class SlotsGame {
        @Test
        @DisplayName("CATEGORY-006: New UI Slots Game category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.SLOTS, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-007: New UI Slots Game category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.SLOTS, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-008: New UI Slots Game category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.SLOTS, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-009: New UI Slots Game category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.SLOTS, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-010: New UI Slots Game category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.SLOTS, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Horse Race")
    class HorseRace {
        @Test
        @DisplayName("CATEGORY-011: New UI Horse Race category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.HORSE_RACE, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-012: New UI Horse Race category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.HORSE_RACE, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-013: New UI Horse Race category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.HORSE_RACE, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-014: New UI Horse Race category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.HORSE_RACE, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-015: New UI Horse Race category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.HORSE_RACE, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Lives Casino")
    class LivesCasino {
        @Test
        @DisplayName("CATEGORY-016: New UI Lives Casino category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.LIVE_CASINO, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-017: New UI Lives Casino category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.LIVE_CASINO, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-018: New UI Lives Casino category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.LIVE_CASINO, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-019: New UI Lives Casino category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.LIVE_CASINO, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-020: New UI Lives Casino category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.LIVE_CASINO, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("E-Sport")
    class ESport {
        @Test
        @DisplayName("CATEGORY-021: New UI E-Sport category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.E_SPORT, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-022: New UI E-Sport category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.E_SPORT, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-023: New UI E-Sport category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.E_SPORT, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-024: New UI E-Sport category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.E_SPORT, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-025: New UI E-Sport category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.E_SPORT, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Fish Game")
    class FishGame {
        @Test
        @DisplayName("CATEGORY-026: New UI Fish Game category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.FISH_GAME, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-027: New UI Fish Game category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.FISH_GAME, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-028: New UI Fish Game category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.FISH_GAME, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-029: New UI Fish Game category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.FISH_GAME, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-030: New UI Fish Game category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.FISH_GAME, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Lotteries")
    class Lotteries {
        @Test
        @DisplayName("CATEGORY-031: New UI Lotteries category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.LOTTERIES, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-032: New UI Lotteries category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.LOTTERIES, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-033: New UI Lotteries category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.LOTTERIES, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-034: New UI Lotteries category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.LOTTERIES, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-035: New UI Lotteries category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.LOTTERIES, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Sport")
    class Sport {
        @Test
        @DisplayName("CATEGORY-036: New UI Sport category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.SPORT, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-037: New UI Sport category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.SPORT, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-038: New UI Sport category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.SPORT, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-039: New UI Sport category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.SPORT, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-040: New UI Sport category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.SPORT, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Card Game")
    class CardGame {
        @Test
        @DisplayName("CATEGORY-041: New UI Card Game category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.CARD_GAME, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-042: New UI Card Game category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.CARD_GAME, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-043: New UI Card Game category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.CARD_GAME, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-044: New UI Card Game category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.CARD_GAME, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-045: New UI Card Game category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.CARD_GAME, Device.ANDROID);
        }
    }

    // =====================================================================
    // Static content pages
    // =====================================================================

    @Nested
    @DisplayName("About Us")
    class AboutUs {
        @Test
        @DisplayName("STATIC-001: New UI About Us page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.ABOUT_US, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-002: New UI About Us page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.ABOUT_US, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-003: New UI About Us page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.ABOUT_US, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-004: New UI About Us page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.ABOUT_US, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-005: New UI About Us page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.ABOUT_US, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Responsible Gaming")
    class ResponsibleGaming {
        @Test
        @DisplayName("STATIC-006: New UI Responsible Gaming page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.RESPONSIBLE_GAMING, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-007: New UI Responsible Gaming page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.RESPONSIBLE_GAMING, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-008: New UI Responsible Gaming page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.RESPONSIBLE_GAMING, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-009: New UI Responsible Gaming page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.RESPONSIBLE_GAMING, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-010: New UI Responsible Gaming page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.RESPONSIBLE_GAMING, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Banking Options")
    class Banking {
        @Test
        @DisplayName("STATIC-011: New UI Banking Options page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.BANKING, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-012: New UI Banking Options page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.BANKING, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-013: New UI Banking Options page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.BANKING, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-014: New UI Banking Options page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.BANKING, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-015: New UI Banking Options page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.BANKING, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Terms of Use")
    class TermsOfUse {
        @Test
        @DisplayName("STATIC-016: New UI Terms of Use page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.TERMS_OF_USE, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-017: New UI Terms of Use page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.TERMS_OF_USE, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-018: New UI Terms of Use page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.TERMS_OF_USE, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-019: New UI Terms of Use page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.TERMS_OF_USE, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-020: New UI Terms of Use page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.TERMS_OF_USE, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Privacy Policy")
    class PrivacyPolicy {
        @Test
        @DisplayName("STATIC-021: New UI Privacy Policy page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.PRIVACY_POLICY, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-022: New UI Privacy Policy page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.PRIVACY_POLICY, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-023: New UI Privacy Policy page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.PRIVACY_POLICY, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-024: New UI Privacy Policy page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.PRIVACY_POLICY, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-025: New UI Privacy Policy page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.PRIVACY_POLICY, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("How to Register")
    class HowToRegister {
        @Test
        @DisplayName("STATIC-026: New UI How to Register page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.HOW_TO_REGISTER, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-027: New UI How to Register page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.HOW_TO_REGISTER, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-028: New UI How to Register page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.HOW_TO_REGISTER, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-029: New UI How to Register page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.HOW_TO_REGISTER, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-030: New UI How to Register page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.HOW_TO_REGISTER, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("How to Deposit")
    class HowToDeposit {
        @Test
        @DisplayName("STATIC-031: New UI How to Deposit page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.HOW_TO_DEPOSIT, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-032: New UI How to Deposit page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.HOW_TO_DEPOSIT, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-033: New UI How to Deposit page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.HOW_TO_DEPOSIT, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-034: New UI How to Deposit page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.HOW_TO_DEPOSIT, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-035: New UI How to Deposit page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.HOW_TO_DEPOSIT, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("How to Withdrawal")
    class HowToWithdraw {
        @Test
        @DisplayName("STATIC-036: New UI How to Withdrawal page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.HOW_TO_WITHDRAW, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-037: New UI How to Withdrawal page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.HOW_TO_WITHDRAW, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-038: New UI How to Withdrawal page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.HOW_TO_WITHDRAW, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-039: New UI How to Withdrawal page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.HOW_TO_WITHDRAW, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-040: New UI How to Withdrawal page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.HOW_TO_WITHDRAW, Device.ANDROID);
        }
    }

    // =====================================================================
    // Signed-in member-area pages
    // =====================================================================

    @Nested
    @DisplayName("Personal Information")
    class PersonalInformation {
        @Test
        @DisplayName("MEMBER-001: New UI Personal Information page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.PERSONAL_INFORMATION, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-002: New UI Personal Information page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.PERSONAL_INFORMATION, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-003: New UI Personal Information page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.PERSONAL_INFORMATION, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-004: New UI Personal Information page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.PERSONAL_INFORMATION, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-005: New UI Personal Information page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.PERSONAL_INFORMATION, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Password")
    class Password {
        @Test
        @DisplayName("MEMBER-006: New UI Password page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.PASSWORD, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-007: New UI Password page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.PASSWORD, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-008: New UI Password page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.PASSWORD, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-009: New UI Password page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.PASSWORD, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-010: New UI Password page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.PASSWORD, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Wallet")
    class Wallet {
        @Test
        @DisplayName("MEMBER-011: New UI Wallet page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.WALLET, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-012: New UI Wallet page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.WALLET, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-013: New UI Wallet page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.WALLET, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-014: New UI Wallet page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.WALLET, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-015: New UI Wallet page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.WALLET, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Deposit")
    class Deposit {
        @Test
        @DisplayName("MEMBER-016: New UI Deposit page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.DEPOSIT, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-017: New UI Deposit page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.DEPOSIT, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-018: New UI Deposit page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.DEPOSIT, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-019: New UI Deposit page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.DEPOSIT, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-020: New UI Deposit page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.DEPOSIT, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Withdrawal")
    class Withdrawal {
        @Test
        @DisplayName("MEMBER-021: New UI Withdrawal page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.WITHDRAWAL, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-022: New UI Withdrawal page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.WITHDRAWAL, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-023: New UI Withdrawal page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.WITHDRAWAL, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-024: New UI Withdrawal page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.WITHDRAWAL, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-025: New UI Withdrawal page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.WITHDRAWAL, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Rebate claim")
    class RebateClaim {
        @Test
        @DisplayName("MEMBER-026: New UI Rebate claim page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.REBATE_CLAIM, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-027: New UI Rebate claim page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.REBATE_CLAIM, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-028: New UI Rebate claim page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.REBATE_CLAIM, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-029: New UI Rebate claim page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.REBATE_CLAIM, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-030: New UI Rebate claim page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.REBATE_CLAIM, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Referral link")
    class ReferralLink {
        @Test
        @DisplayName("MEMBER-031: New UI Referral link page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.REFERRAL_LINK, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-032: New UI Referral link page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.REFERRAL_LINK, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-033: New UI Referral link page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.REFERRAL_LINK, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-034: New UI Referral link page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.REFERRAL_LINK, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-035: New UI Referral link page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.REFERRAL_LINK, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Referral bonus")
    class ReferralBonus {
        @Test
        @DisplayName("MEMBER-036: New UI Referral bonus page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.REFERRAL_BONUS, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-037: New UI Referral bonus page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.REFERRAL_BONUS, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-038: New UI Referral bonus page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.REFERRAL_BONUS, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-039: New UI Referral bonus page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.REFERRAL_BONUS, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-040: New UI Referral bonus page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.REFERRAL_BONUS, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Membership program")
    class MembershipProgram {
        @Test
        @DisplayName("MEMBER-041: New UI Membership program page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.MEMBERSHIP_PROGRAM, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-042: New UI Membership program page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.MEMBERSHIP_PROGRAM, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-043: New UI Membership program page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.MEMBERSHIP_PROGRAM, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-044: New UI Membership program page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.MEMBERSHIP_PROGRAM, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-045: New UI Membership program page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.MEMBERSHIP_PROGRAM, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Records")
    class Records {
        @Test
        @DisplayName("MEMBER-046: New UI Records page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.RECORDS, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-047: New UI Records page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.RECORDS, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-048: New UI Records page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.RECORDS, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-049: New UI Records page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.RECORDS, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-050: New UI Records page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.RECORDS, Device.ANDROID);
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Opens the home page on the device's own engine and viewport, as a fresh visitor, with
     *  New Interface chosen and the overlays cleared. */
    private Page openAsVisitor(Device device) {
        Page on = openOn(device);
        CaseLog.data("Device", device + " in " + device.engine);
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();

        assertTrue(home.isNewUiChosen(), "Expected New Interface to be the chosen UI version on "
                + device.label + " - ui-version is \"" + home.uiVersion() + "\"");

        return on;
    }

    /** The visitor walk: open as a visitor, then check the home page layout. */
    private void layoutHolds(Device device) {
        Page on = openAsVisitor(device);
        assertLayoutHolds(on, device, "New UI");
        capture(on, device, "home");
    }

    /** The member walk: open as a visitor (answering the picker), sign in, return to the home
     *  page, then check the home page layout the same way. */
    private void memberLayoutHolds(Device device) {
        Page on = openAsVisitor(device);
        signIn(on);

        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();

        boolean signedIn = new LoginPage(on).isSignedIn();
        CaseLog.data("Signed in", String.valueOf(signedIn));
        assertTrue(signedIn, "Expected the header to still show the signed-in state on "
                + device.label + " after returning to the home page.");

        assertLayoutHolds(on, device, "New UI, signed in");
        capture(on, device, "member-home");
    }

    /** The three checks every home-page row runs: the chosen UI version held, nothing overflows,
     *  and the hero banner renders. */
    private void assertLayoutHolds(Page on, Device device, String state) {
        HomePage home = new HomePage(on);
        CaseLog.data("UI version", home.uiVersion());
        assertTrue(home.isNewUiChosen(), "Expected New Interface to still be the chosen UI "
                + "version on " + device.label + " (" + state + ") - ui-version is \""
                + home.uiVersion() + "\"");

        Number scrollWidth = (Number) on.evaluate("document.body.scrollWidth");
        CaseLog.data("document.body.scrollWidth at " + device.width + "px viewport",
                String.valueOf(scrollWidth));
        assertTrue(scrollWidth.intValue() <= device.width,
                "Nothing should run off the side of the screen on " + device.label + " (" + state
                        + ") - document.body.scrollWidth is " + scrollWidth
                        + " but the viewport is " + device.width + "px wide");

        assertTrue(home.hasHeroBanner(),
                "The hero banner should render under the nav on " + device.label + " (" + state + ")");
    }

    /** Opens the home page as a fresh visitor, answers the UI-version picker with New Interface,
     *  then opens the given category page and confirms the layout holds - see this class's Javadoc
     *  for why the home page has to come first. */
    private void layoutHolds(GameCategory category, Device device) {
        Page on = openOn(device);
        CaseLog.data("Device", device + " in " + device.engine);
        CaseLog.data("Category", category.label);

        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();
        assertTrue(home.isNewUiChosen(), "Expected New Interface to be the chosen UI version on "
                + device.label + " before opening the " + category.label + " category - "
                + "ui-version is \"" + home.uiVersion() + "\"");

        new GameCategoryPage(on).navigateTo(category);
        home.dismissOverlays();

        assertTrue(home.isNewUiChosen(), "Expected New Interface to still be the chosen UI version "
                + "on " + device.label + " after opening the " + category.label + " category - "
                + "ui-version is \"" + home.uiVersion() + "\"");

        Number scrollWidth = (Number) on.evaluate("document.body.scrollWidth");
        CaseLog.data("document.body.scrollWidth at " + device.width + "px viewport",
                String.valueOf(scrollWidth));
        assertTrue(scrollWidth.intValue() <= device.width,
                "Nothing should run off the side of the screen on " + device.label + " on the "
                        + category.label + " category page (New UI) - document.body.scrollWidth is "
                        + scrollWidth + " but the viewport is " + device.width + "px wide");

        capture(on, device, "category-" + category.name().toLowerCase());
    }

    /** As {@link #layoutHolds(GameCategory, Device)}, for a static content page. */
    private void layoutHolds(StaticPage staticPage, Device device) {
        Page on = openOn(device);
        CaseLog.data("Device", device + " in " + device.engine);
        CaseLog.data("Page", staticPage.label);

        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();
        assertTrue(home.isNewUiChosen(), "Expected New Interface to be the chosen UI version on "
                + device.label + " before opening the " + staticPage.label + " page - "
                + "ui-version is \"" + home.uiVersion() + "\"");

        new StaticContentPage(on).navigateTo(staticPage);
        home.dismissOverlays();

        assertTrue(home.isNewUiChosen(), "Expected New Interface to still be the chosen UI version "
                + "on " + device.label + " after opening the " + staticPage.label + " page - "
                + "ui-version is \"" + home.uiVersion() + "\"");

        Number scrollWidth = (Number) on.evaluate("document.body.scrollWidth");
        CaseLog.data("document.body.scrollWidth at " + device.width + "px viewport",
                String.valueOf(scrollWidth));
        assertTrue(scrollWidth.intValue() <= device.width,
                "Nothing should run off the side of the screen on " + device.label + " on the "
                        + staticPage.label + " page (New UI) - document.body.scrollWidth is "
                        + scrollWidth + " but the viewport is " + device.width + "px wide");

        capture(on, device, "static-" + staticPage.name().toLowerCase());
    }

    /** As {@link #layoutHolds(GameCategory, Device)}, for a signed-in member-area page - signs in
     *  and re-confirms the session before navigating on, since a member page is unreachable - and
     *  the picker itself does not appear - without one. */
    private void layoutHolds(MemberPage memberPage, Device device) {
        Page on = openOn(device);
        CaseLog.data("Device", device + " in " + device.engine);
        CaseLog.data("Page", memberPage.label);

        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();
        assertTrue(home.isNewUiChosen(), "Expected New Interface to be the chosen UI version on "
                + device.label + " before signing in - ui-version is \"" + home.uiVersion() + "\"");

        signIn(on);

        home.navigateTo();
        home.dismissOverlays();
        boolean signedIn = new LoginPage(on).isSignedIn();
        CaseLog.data("Signed in", String.valueOf(signedIn));
        assertTrue(signedIn, "Expected the header to still show the signed-in state on "
                + device.label + " after returning to the home page under New UI.");
        assertTrue(home.isNewUiChosen(), "Expected New Interface to still be the chosen UI version "
                + "on " + device.label + " after signing in - ui-version is \""
                + home.uiVersion() + "\"");

        new MemberAreaPage(on).navigateTo(memberPage);
        home.dismissOverlays();

        assertTrue(home.isNewUiChosen(), "Expected New Interface to still be the chosen UI version "
                + "on " + device.label + " after opening the " + memberPage.label + " page - "
                + "ui-version is \"" + home.uiVersion() + "\"");

        Number scrollWidth = (Number) on.evaluate("document.body.scrollWidth");
        CaseLog.data("document.body.scrollWidth at " + device.width + "px viewport",
                String.valueOf(scrollWidth));
        assertTrue(scrollWidth.intValue() <= device.width,
                "Nothing should run off the side of the screen on " + device.label + " on the "
                        + memberPage.label + " page (New UI) - document.body.scrollWidth is "
                        + scrollWidth + " but the viewport is " + device.width + "px wide");

        capture(on, device, "member-" + memberPage.name().toLowerCase());
    }
}
