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
 * Every page this project could confirm a URL for, on Classic UI, across the same five
 * {@link Device} profiles {@link ResponsiveLayoutNewUiTest} checks - see that class's Javadoc for
 * the full reasoning behind every group (the four page groups and their shared shape, why a direct
 * {@code page.navigate()} is used throughout, what every case checks). This is the Classic UI
 * twin of that class, one class rather than four for the same reason.
 *
 * <h2>What's different here</h2>
 * <ul>
 *   <li>The home page rows answer the picker with Classic Interface
 *       ({@link org.sengmean.base.BasePage#chooseClassicUi()}) and confirm
 *       {@link org.sengmean.base.BasePage#isClassicUiChosen()} instead. <b>No case here asserts a
 *       hero banner must render</b> - {@link HomePage#hasHeroBanner()} was written and confirmed
 *       against New UI's home page only; whether Classic UI's own home page carries an equivalent
 *       banner is unconfirmed, so the finding is logged to each case's evidence trail instead of
 *       asserted, the same restraint {@code ResponsiveLayoutClassicUiTest} already used before this
 *       merge.</li>
 *   <li>The nine game-category pages and the eight static pages are the same {@link GameCategory}
 *       / {@link StaticPage} sets {@link ResponsiveLayoutNewUiTest} uses - deliberately not
 *       Classic UI's own tenth nav entry, Promotions, which has no New UI counterpart to compare
 *       against; see {@link ResponsiveLayoutNewUiTest}'s Javadoc.</li>
 *   <li>The ten member-area pages are the same {@link MemberPage} set, signed in as the same
 *       shared QA account - so this class should not be run alongside another run that is also
 *       signed in, including {@link ResponsiveLayoutNewUiTest} itself.</li>
 * </ul>
 *
 * <h2>Case ids</h2>
 * {@code UI-011}-{@code UI-020} (home page: visitor {@code 011}-{@code 015}, member
 * {@code 016}-{@code 020}), continuing {@link ResponsiveLayoutNewUiTest}'s {@code UI-001}-
 * {@code UI-010}; {@code CATEGORY-046}-{@code CATEGORY-090}, continuing that class's
 * {@code CATEGORY-001}-{@code CATEGORY-045}; {@code STATIC-041}-{@code STATIC-080}, continuing
 * {@code STATIC-001}-{@code STATIC-040}; {@code MEMBER-051}-{@code MEMBER-100}, continuing
 * {@code MEMBER-001}-{@code MEMBER-050}. Five per page (PC, Mac, iPad, iPhone, Android).
 *
 * <h2>WebKit</h2>
 * The Mac, iPad and iPhone cases need WebKit installed ({@code npx playwright install webkit});
 * see {@link org.sengmean.base.BaseTest#webkitOrAbort()}.
 */
public class ResponsiveLayoutClassicUiTest extends BaseTest {

    // =====================================================================
    // Home page
    // =====================================================================

    @Test
    @DisplayName("UI-011: Classic UI home page layout is responsive on PC, as a visitor (Windows, Chrome, 1920x1080)")
    void onPc() {
        layoutHolds(Device.DESKTOP_PC);
    }

    @Test
    @DisplayName("UI-012: Classic UI home page layout is responsive on Mac, as a visitor (macOS, Safari/WebKit, 1440x900)")
    void onMac() {
        layoutHolds(Device.DESKTOP_MAC);
    }

    @Test
    @DisplayName("UI-013: Classic UI home page layout is responsive on iPad, as a visitor (iPadOS, Safari/WebKit, 820x1180, touch)")
    void onIpad() {
        layoutHolds(Device.IPAD);
    }

    @Test
    @DisplayName("UI-014: Classic UI home page layout is responsive on iPhone, as a visitor (iOS, Safari/WebKit, 390x844, touch)")
    void onIphone() {
        layoutHolds(Device.IPHONE);
    }

    @Test
    @DisplayName("UI-015: Classic UI home page layout is responsive on Android, as a visitor (Pixel 7, Chrome, 412x915, touch)")
    void onAndroid() {
        layoutHolds(Device.ANDROID);
    }

    @Test
    @DisplayName("UI-016: Classic UI home page layout is responsive on PC, signed in (Windows, Chrome, 1920x1080)")
    void memberOnPc() {
        memberLayoutHolds(Device.DESKTOP_PC);
    }

    @Test
    @DisplayName("UI-017: Classic UI home page layout is responsive on Mac, signed in (macOS, Safari/WebKit, 1440x900)")
    void memberOnMac() {
        memberLayoutHolds(Device.DESKTOP_MAC);
    }

    @Test
    @DisplayName("UI-018: Classic UI home page layout is responsive on iPad, signed in (iPadOS, Safari/WebKit, 820x1180, touch)")
    void memberOnIpad() {
        memberLayoutHolds(Device.IPAD);
    }

    @Test
    @DisplayName("UI-019: Classic UI home page layout is responsive on iPhone, signed in (iOS, Safari/WebKit, 390x844, touch)")
    void memberOnIphone() {
        memberLayoutHolds(Device.IPHONE);
    }

    @Test
    @DisplayName("UI-020: Classic UI home page layout is responsive on Android, signed in (Pixel 7, Chrome, 412x915, touch)")
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
        @DisplayName("CATEGORY-046: Classic UI 真人直播 category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.LIVE_STREAM, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-047: Classic UI 真人直播 category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.LIVE_STREAM, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-048: Classic UI 真人直播 category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.LIVE_STREAM, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-049: Classic UI 真人直播 category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.LIVE_STREAM, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-050: Classic UI 真人直播 category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.LIVE_STREAM, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Slots Game")
    class SlotsGame {
        @Test
        @DisplayName("CATEGORY-051: Classic UI Slots Game category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.SLOTS, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-052: Classic UI Slots Game category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.SLOTS, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-053: Classic UI Slots Game category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.SLOTS, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-054: Classic UI Slots Game category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.SLOTS, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-055: Classic UI Slots Game category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.SLOTS, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Horse Race")
    class HorseRace {
        @Test
        @DisplayName("CATEGORY-056: Classic UI Horse Race category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.HORSE_RACE, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-057: Classic UI Horse Race category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.HORSE_RACE, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-058: Classic UI Horse Race category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.HORSE_RACE, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-059: Classic UI Horse Race category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.HORSE_RACE, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-060: Classic UI Horse Race category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.HORSE_RACE, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Lives Casino")
    class LivesCasino {
        @Test
        @DisplayName("CATEGORY-061: Classic UI Lives Casino category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.LIVE_CASINO, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-062: Classic UI Lives Casino category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.LIVE_CASINO, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-063: Classic UI Lives Casino category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.LIVE_CASINO, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-064: Classic UI Lives Casino category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.LIVE_CASINO, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-065: Classic UI Lives Casino category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.LIVE_CASINO, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("E-Sport")
    class ESport {
        @Test
        @DisplayName("CATEGORY-066: Classic UI E-Sport category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.E_SPORT, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-067: Classic UI E-Sport category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.E_SPORT, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-068: Classic UI E-Sport category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.E_SPORT, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-069: Classic UI E-Sport category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.E_SPORT, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-070: Classic UI E-Sport category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.E_SPORT, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Fish Game")
    class FishGame {
        @Test
        @DisplayName("CATEGORY-071: Classic UI Fish Game category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.FISH_GAME, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-072: Classic UI Fish Game category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.FISH_GAME, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-073: Classic UI Fish Game category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.FISH_GAME, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-074: Classic UI Fish Game category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.FISH_GAME, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-075: Classic UI Fish Game category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.FISH_GAME, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Lotteries")
    class Lotteries {
        @Test
        @DisplayName("CATEGORY-076: Classic UI Lotteries category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.LOTTERIES, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-077: Classic UI Lotteries category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.LOTTERIES, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-078: Classic UI Lotteries category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.LOTTERIES, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-079: Classic UI Lotteries category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.LOTTERIES, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-080: Classic UI Lotteries category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.LOTTERIES, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Sport")
    class Sport {
        @Test
        @DisplayName("CATEGORY-081: Classic UI Sport category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.SPORT, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-082: Classic UI Sport category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.SPORT, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-083: Classic UI Sport category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.SPORT, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-084: Classic UI Sport category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.SPORT, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-085: Classic UI Sport category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(GameCategory.SPORT, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Card Game")
    class CardGame {
        @Test
        @DisplayName("CATEGORY-086: Classic UI Card Game category layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(GameCategory.CARD_GAME, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("CATEGORY-087: Classic UI Card Game category layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(GameCategory.CARD_GAME, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("CATEGORY-088: Classic UI Card Game category layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(GameCategory.CARD_GAME, Device.IPAD);
        }

        @Test
        @DisplayName("CATEGORY-089: Classic UI Card Game category layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(GameCategory.CARD_GAME, Device.IPHONE);
        }

        @Test
        @DisplayName("CATEGORY-090: Classic UI Card Game category layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
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
        @DisplayName("STATIC-041: Classic UI About Us page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.ABOUT_US, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-042: Classic UI About Us page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.ABOUT_US, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-043: Classic UI About Us page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.ABOUT_US, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-044: Classic UI About Us page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.ABOUT_US, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-045: Classic UI About Us page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.ABOUT_US, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Responsible Gaming")
    class ResponsibleGaming {
        @Test
        @DisplayName("STATIC-046: Classic UI Responsible Gaming page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.RESPONSIBLE_GAMING, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-047: Classic UI Responsible Gaming page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.RESPONSIBLE_GAMING, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-048: Classic UI Responsible Gaming page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.RESPONSIBLE_GAMING, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-049: Classic UI Responsible Gaming page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.RESPONSIBLE_GAMING, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-050: Classic UI Responsible Gaming page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.RESPONSIBLE_GAMING, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Banking Options")
    class Banking {
        @Test
        @DisplayName("STATIC-051: Classic UI Banking Options page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.BANKING, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-052: Classic UI Banking Options page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.BANKING, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-053: Classic UI Banking Options page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.BANKING, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-054: Classic UI Banking Options page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.BANKING, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-055: Classic UI Banking Options page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.BANKING, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Terms of Use")
    class TermsOfUse {
        @Test
        @DisplayName("STATIC-056: Classic UI Terms of Use page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.TERMS_OF_USE, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-057: Classic UI Terms of Use page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.TERMS_OF_USE, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-058: Classic UI Terms of Use page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.TERMS_OF_USE, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-059: Classic UI Terms of Use page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.TERMS_OF_USE, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-060: Classic UI Terms of Use page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.TERMS_OF_USE, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Privacy Policy")
    class PrivacyPolicy {
        @Test
        @DisplayName("STATIC-061: Classic UI Privacy Policy page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.PRIVACY_POLICY, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-062: Classic UI Privacy Policy page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.PRIVACY_POLICY, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-063: Classic UI Privacy Policy page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.PRIVACY_POLICY, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-064: Classic UI Privacy Policy page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.PRIVACY_POLICY, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-065: Classic UI Privacy Policy page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.PRIVACY_POLICY, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("How to Register")
    class HowToRegister {
        @Test
        @DisplayName("STATIC-066: Classic UI How to Register page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.HOW_TO_REGISTER, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-067: Classic UI How to Register page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.HOW_TO_REGISTER, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-068: Classic UI How to Register page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.HOW_TO_REGISTER, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-069: Classic UI How to Register page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.HOW_TO_REGISTER, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-070: Classic UI How to Register page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.HOW_TO_REGISTER, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("How to Deposit")
    class HowToDeposit {
        @Test
        @DisplayName("STATIC-071: Classic UI How to Deposit page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.HOW_TO_DEPOSIT, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-072: Classic UI How to Deposit page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.HOW_TO_DEPOSIT, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-073: Classic UI How to Deposit page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.HOW_TO_DEPOSIT, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-074: Classic UI How to Deposit page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.HOW_TO_DEPOSIT, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-075: Classic UI How to Deposit page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(StaticPage.HOW_TO_DEPOSIT, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("How to Withdrawal")
    class HowToWithdraw {
        @Test
        @DisplayName("STATIC-076: Classic UI How to Withdrawal page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(StaticPage.HOW_TO_WITHDRAW, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("STATIC-077: Classic UI How to Withdrawal page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(StaticPage.HOW_TO_WITHDRAW, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("STATIC-078: Classic UI How to Withdrawal page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(StaticPage.HOW_TO_WITHDRAW, Device.IPAD);
        }

        @Test
        @DisplayName("STATIC-079: Classic UI How to Withdrawal page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(StaticPage.HOW_TO_WITHDRAW, Device.IPHONE);
        }

        @Test
        @DisplayName("STATIC-080: Classic UI How to Withdrawal page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
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
        @DisplayName("MEMBER-051: Classic UI Personal Information page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.PERSONAL_INFORMATION, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-052: Classic UI Personal Information page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.PERSONAL_INFORMATION, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-053: Classic UI Personal Information page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.PERSONAL_INFORMATION, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-054: Classic UI Personal Information page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.PERSONAL_INFORMATION, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-055: Classic UI Personal Information page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.PERSONAL_INFORMATION, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Password")
    class Password {
        @Test
        @DisplayName("MEMBER-056: Classic UI Password page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.PASSWORD, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-057: Classic UI Password page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.PASSWORD, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-058: Classic UI Password page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.PASSWORD, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-059: Classic UI Password page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.PASSWORD, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-060: Classic UI Password page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.PASSWORD, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Wallet")
    class Wallet {
        @Test
        @DisplayName("MEMBER-061: Classic UI Wallet page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.WALLET, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-062: Classic UI Wallet page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.WALLET, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-063: Classic UI Wallet page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.WALLET, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-064: Classic UI Wallet page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.WALLET, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-065: Classic UI Wallet page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.WALLET, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Deposit")
    class Deposit {
        @Test
        @DisplayName("MEMBER-066: Classic UI Deposit page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.DEPOSIT, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-067: Classic UI Deposit page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.DEPOSIT, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-068: Classic UI Deposit page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.DEPOSIT, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-069: Classic UI Deposit page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.DEPOSIT, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-070: Classic UI Deposit page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.DEPOSIT, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Withdrawal")
    class Withdrawal {
        @Test
        @DisplayName("MEMBER-071: Classic UI Withdrawal page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.WITHDRAWAL, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-072: Classic UI Withdrawal page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.WITHDRAWAL, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-073: Classic UI Withdrawal page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.WITHDRAWAL, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-074: Classic UI Withdrawal page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.WITHDRAWAL, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-075: Classic UI Withdrawal page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.WITHDRAWAL, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Rebate claim")
    class RebateClaim {
        @Test
        @DisplayName("MEMBER-076: Classic UI Rebate claim page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.REBATE_CLAIM, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-077: Classic UI Rebate claim page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.REBATE_CLAIM, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-078: Classic UI Rebate claim page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.REBATE_CLAIM, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-079: Classic UI Rebate claim page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.REBATE_CLAIM, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-080: Classic UI Rebate claim page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.REBATE_CLAIM, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Referral link")
    class ReferralLink {
        @Test
        @DisplayName("MEMBER-081: Classic UI Referral link page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.REFERRAL_LINK, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-082: Classic UI Referral link page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.REFERRAL_LINK, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-083: Classic UI Referral link page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.REFERRAL_LINK, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-084: Classic UI Referral link page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.REFERRAL_LINK, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-085: Classic UI Referral link page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.REFERRAL_LINK, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Referral bonus")
    class ReferralBonus {
        @Test
        @DisplayName("MEMBER-086: Classic UI Referral bonus page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.REFERRAL_BONUS, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-087: Classic UI Referral bonus page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.REFERRAL_BONUS, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-088: Classic UI Referral bonus page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.REFERRAL_BONUS, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-089: Classic UI Referral bonus page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.REFERRAL_BONUS, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-090: Classic UI Referral bonus page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.REFERRAL_BONUS, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Membership program")
    class MembershipProgram {
        @Test
        @DisplayName("MEMBER-091: Classic UI Membership program page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.MEMBERSHIP_PROGRAM, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-092: Classic UI Membership program page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.MEMBERSHIP_PROGRAM, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-093: Classic UI Membership program page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.MEMBERSHIP_PROGRAM, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-094: Classic UI Membership program page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.MEMBERSHIP_PROGRAM, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-095: Classic UI Membership program page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.MEMBERSHIP_PROGRAM, Device.ANDROID);
        }
    }

    @Nested
    @DisplayName("Records")
    class Records {
        @Test
        @DisplayName("MEMBER-096: Classic UI Records page layout is responsive on PC (Windows, Chrome, 1920x1080)")
        void onPc() {
            layoutHolds(MemberPage.RECORDS, Device.DESKTOP_PC);
        }

        @Test
        @DisplayName("MEMBER-097: Classic UI Records page layout is responsive on Mac (macOS, Safari/WebKit, 1440x900)")
        void onMac() {
            layoutHolds(MemberPage.RECORDS, Device.DESKTOP_MAC);
        }

        @Test
        @DisplayName("MEMBER-098: Classic UI Records page layout is responsive on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
        void onIpad() {
            layoutHolds(MemberPage.RECORDS, Device.IPAD);
        }

        @Test
        @DisplayName("MEMBER-099: Classic UI Records page layout is responsive on iPhone (iOS, Safari/WebKit, 390x844, touch)")
        void onIphone() {
            layoutHolds(MemberPage.RECORDS, Device.IPHONE);
        }

        @Test
        @DisplayName("MEMBER-100: Classic UI Records page layout is responsive on Android (Pixel 7, Chrome, 412x915, touch)")
        void onAndroid() {
            layoutHolds(MemberPage.RECORDS, Device.ANDROID);
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Opens the home page on the device's own engine and viewport, as a fresh visitor, with
     *  Classic Interface chosen and the overlays cleared. */
    private Page openAsVisitor(Device device) {
        Page on = openOn(device);
        CaseLog.data("Device", device + " in " + device.engine);
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseClassicUi();
        home.dismissOverlays();

        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to be the chosen UI "
                + "version on " + device.label + " - ui-version is \"" + home.uiVersion() + "\"");

        return on;
    }

    /** The visitor walk: open as a visitor, then check the home page layout. */
    private void layoutHolds(Device device) {
        Page on = openAsVisitor(device);
        assertLayoutHolds(on, device, "Classic UI");
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

        assertLayoutHolds(on, device, "Classic UI, signed in");
        capture(on, device, "member-home");
    }

    /** The chosen-option check and no-overflow are hard assertions for every row; the hero
     *  banner is logged, not asserted - see this class's Javadoc for why. */
    private void assertLayoutHolds(Page on, Device device, String state) {
        HomePage home = new HomePage(on);
        CaseLog.data("UI version", home.uiVersion());
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to still be the chosen "
                + "UI version on " + device.label + " (" + state + ") - ui-version is \""
                + home.uiVersion() + "\"");

        Number scrollWidth = (Number) on.evaluate("document.body.scrollWidth");
        CaseLog.data("document.body.scrollWidth at " + device.width + "px viewport",
                String.valueOf(scrollWidth));
        assertTrue(scrollWidth.intValue() <= device.width,
                "Nothing should run off the side of the screen on " + device.label + " (" + state
                        + ") - document.body.scrollWidth is " + scrollWidth
                        + " but the viewport is " + device.width + "px wide");

        CaseLog.data("Hero banner on " + state + ", " + device.label,
                home.hasHeroBanner() ? "rendered" : "not found - unconfirmed whether Classic UI "
                        + "carries one at all; see this class's Javadoc before treating this as a defect");
    }

    /** Opens the home page as a fresh visitor, answers the UI-version picker with Classic
     *  Interface, then opens the given category page and confirms the layout holds - see
     *  {@link ResponsiveLayoutNewUiTest}'s Javadoc for why the home page has to come first. */
    private void layoutHolds(GameCategory category, Device device) {
        Page on = openOn(device);
        CaseLog.data("Device", device + " in " + device.engine);
        CaseLog.data("Category", category.label);

        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseClassicUi();
        home.dismissOverlays();
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to be the chosen UI "
                + "version on " + device.label + " before opening the " + category.label
                + " category - ui-version is \"" + home.uiVersion() + "\"");

        new GameCategoryPage(on).navigateTo(category);
        home.dismissOverlays();

        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to still be the chosen "
                + "UI version on " + device.label + " after opening the " + category.label
                + " category - ui-version is \"" + home.uiVersion() + "\"");

        Number scrollWidth = (Number) on.evaluate("document.body.scrollWidth");
        CaseLog.data("document.body.scrollWidth at " + device.width + "px viewport",
                String.valueOf(scrollWidth));
        assertTrue(scrollWidth.intValue() <= device.width,
                "Nothing should run off the side of the screen on " + device.label + " on the "
                        + category.label + " category page (Classic UI) - document.body."
                        + "scrollWidth is " + scrollWidth + " but the viewport is " + device.width
                        + "px wide");

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
        home.chooseClassicUi();
        home.dismissOverlays();
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to be the chosen UI "
                + "version on " + device.label + " before opening the " + staticPage.label
                + " page - ui-version is \"" + home.uiVersion() + "\"");

        new StaticContentPage(on).navigateTo(staticPage);
        home.dismissOverlays();

        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to still be the chosen "
                + "UI version on " + device.label + " after opening the " + staticPage.label
                + " page - ui-version is \"" + home.uiVersion() + "\"");

        Number scrollWidth = (Number) on.evaluate("document.body.scrollWidth");
        CaseLog.data("document.body.scrollWidth at " + device.width + "px viewport",
                String.valueOf(scrollWidth));
        assertTrue(scrollWidth.intValue() <= device.width,
                "Nothing should run off the side of the screen on " + device.label + " on the "
                        + staticPage.label + " page (Classic UI) - document.body.scrollWidth is "
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
        home.chooseClassicUi();
        home.dismissOverlays();
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to be the chosen UI "
                + "version on " + device.label + " before signing in - ui-version is \""
                + home.uiVersion() + "\"");

        signIn(on);

        home.navigateTo();
        home.dismissOverlays();
        boolean signedIn = new LoginPage(on).isSignedIn();
        CaseLog.data("Signed in", String.valueOf(signedIn));
        assertTrue(signedIn, "Expected the header to still show the signed-in state on "
                + device.label + " after returning to the home page under Classic UI.");
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to still be the chosen "
                + "UI version on " + device.label + " after signing in - ui-version is \""
                + home.uiVersion() + "\"");

        new MemberAreaPage(on).navigateTo(memberPage);
        home.dismissOverlays();

        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to still be the chosen "
                + "UI version on " + device.label + " after opening the " + memberPage.label
                + " page - ui-version is \"" + home.uiVersion() + "\"");

        Number scrollWidth = (Number) on.evaluate("document.body.scrollWidth");
        CaseLog.data("document.body.scrollWidth at " + device.width + "px viewport",
                String.valueOf(scrollWidth));
        assertTrue(scrollWidth.intValue() <= device.width,
                "Nothing should run off the side of the screen on " + device.label + " on the "
                        + memberPage.label + " page (Classic UI) - document.body.scrollWidth is "
                        + scrollWidth + " but the viewport is " + device.width + "px wide");

        capture(on, device, "member-" + memberPage.name().toLowerCase());
    }
}
