package org.sengmean.tests.home;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.home.HomePage;
import org.sengmean.report.CaseLog;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The landing page's header furniture and signed-in account chip, on the New UI, as a visitor and
 * as a signed-in member - the New UI half of the split mirroring
 * {@link org.sengmean.tests.layout.ResponsiveLayoutNewUiTest} /
 * {@code ResponsiveLayoutClassicUiTest}: every case opens the home page as a fresh visitor,
 * clears the overlays ({@link org.sengmean.base.BasePage#dismissOverlays()}), answers the
 * UI-version picker with New Interface ({@link org.sengmean.base.BasePage#chooseNewUi()}) and
 * confirms the choice stuck ({@link org.sengmean.base.BasePage#isNewUiChosen()}) before running
 * its own check. The member cases additionally sign the shared QA account in, return to the home
 * page, and confirm the header still shows the signed-in state and that the UI choice survived
 * the round trip - the same walk {@code ResponsiveLayoutNewUiTest} already uses.
 *
 * <h2>Where these cases come from</h2>
 * Ported from the sibling NewWeibo_automation project's own {@code tests.home.HomePageTest} (a
 * different client site, 33 cases). This class and {@link HomePageClassicUiTest} between them
 * carry the seven of those 33 judged portable without inventing a Quark-side guess:
 * <ul>
 *   <li>the five header-furniture cases (its HOME-001 through HOME-009, minus one - see below)
 *       are UI-version-independent header/auth furniture and are ported, generalised away from
 *       that project's exact literals where this project has no confirmed equivalent;</li>
 *   <li>the two signed-in-header cases (its HOME-0015 and HOME-0061) are ported directly, since
 *       Quark's own confirmed wallet currencies ({@code CurrencySwitcher}'s USDT/TRX/THB/HKD/USD)
 *       are the same set NewWeibo_empire's own case asserted on its site.</li>
 * </ul>
 *
 * <h2>What was skipped, and why</h2>
 * Twenty-six of the 33 are not here at all:
 * <ul>
 *   <li><b>Account Activation</b> (its HOME-009) - that project's header carries a third auth
 *       control for an unactivated account; this project's registration flow folds activation
 *       into the Register form itself as an SMS step ({@code RegisterPage}), not a separate
 *       header-level account state, so there is no equivalent control to test.</li>
 *   <li><b>Game navigation</b> (its HOME-9001-HOME-9010 and HOME-0046, 11 cases) - every one
 *       hovers a <i>named</i> game category (Horse Race, Live, Lives Casino, ...) and checks the
 *       fly-out against a live catalogue API ({@code GameCatalogue}) specific to that site. Quark
 *       Dynasty's own game categories, and whether its home page even exposes a hover fly-out at
 *       all, are unconfirmed - see {@code org.sengmean.page.casino.WinWinCityGamePage}'s Javadoc
 *       for how unconfirmed this project's whole games area still is. Porting these would mean
 *       inventing category names and a catalogue-reconciliation mechanism from nothing.</li>
 *   <li><b>Language switching</b> (its HOME-0013, HOME-0014 and twelve per-language cases,
 *       HOME-0027-HOME-0038, 14 cases in all) - nothing in this project's README, its "Known
 *       defects" section or any existing page object suggests Quark's site offers more than
 *       English at all; every selector that project's switcher uses (a specific globe icon class,
 *       a specific language-row class) is particular to that site's markup. Rather than guess
 *       both that the feature exists and what it looks like, this is left out entirely.</li>
 * </ul>
 * If either feature turns out to exist on Quark, this is where it belongs once someone has
 * confirmed it against the live DOM.
 *
 * <h2>Case ids</h2>
 * {@code HOME-001} through {@code HOME-007} here; {@link HomePageClassicUiTest} continues at
 * {@code HOME-008}. Invented for this class, as there is no client test-case workbook for this
 * project to take ids from - the same situation {@code ResponsiveLayoutNewUiTest}'s own
 * {@code UI-} prefix is in.
 */
public class HomePageNewUiTest extends BaseTest {

    /** Opens the home page as a fresh visitor with New Interface chosen and overlays cleared. */
    private HomePage openAsVisitor() {
        Page page = page();
        HomePage home = new HomePage(page);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();

        assertTrue(home.isNewUiChosen(),
                "Expected New Interface to be the chosen UI version - ui-version is \""
                        + home.uiVersion() + "\"");
        return home;
    }

    /** As {@link #openAsVisitor()}, then signs the shared QA account in and returns to the home
     *  page, confirming the signed-in state and the UI choice both survived. */
    private HomePage openAsMember() {
        HomePage home = openAsVisitor();
        signIn(page());

        home.navigateTo();
        home.dismissOverlays();

        boolean signedIn = new LoginPage(page()).isSignedIn();
        CaseLog.data("Signed in", String.valueOf(signedIn));
        assertTrue(signedIn, "Expected the header to still show the signed-in state after "
                + "returning to the home page.");
        assertTrue(home.isNewUiChosen(), "Expected New Interface to still be the chosen UI "
                + "version after signing in - ui-version is \"" + home.uiVersion() + "\"");
        return home;
    }

    @Test
    @DisplayName("HOME-001: the landing page renders its header furniture, as a visitor")
    void landingPageRendersHeaderFurniture() {
        HomePage home = openAsVisitor();

        assertTrue(home.logoIsRendered(), "The header logo did not decode; src=" + home.logoSrc());
        // Generalised from NewWeibo_automation's own HOME-001, which also pins the exact header
        // button labels and a game-category count - both specific to that project's own header
        // and nav, neither confirmed here.
        assertTrue(home.isHeaderButtonVisible(HomePage.LOG_IN)
                        || home.isHeaderButtonVisible(HomePage.REGISTER),
                "Expected at least one of \"Log In\" / \"Register\" visible in the header.");
    }

    @Test
    @DisplayName("HOME-002: the logo sits at the top left, as a visitor")
    void logoIsTopLeft() {
        HomePage home = openAsVisitor();

        assertTrue(home.logoIsRendered(), "The logo image did not decode; src=" + home.logoSrc());
        assertTrue(home.logoIsTopLeft(), "The logo is not at the top left; box=" + home.logoBox());
    }

    @Test
    @DisplayName("HOME-003: Log In sits at the top right, as a visitor")
    void logInIsTopRight() {
        HomePage home = openAsVisitor();

        assertTrue(home.isHeaderButtonVisible(HomePage.LOG_IN), "No visible Log In control in the header");
        assertTrue(home.isHeaderButtonOnTheRight(HomePage.LOG_IN),
                "Log In is not on the right; box=" + home.headerButtonBox(HomePage.LOG_IN));
        // NewWeibo_automation's own case also asserts the text is rendered white
        // (rgb(255, 255, 255)) - unconfirmed here, so this is logged as evidence for a reviewer
        // rather than asserted and risking a false defect on the very first real run.
        CaseLog.data("Log In text colour", home.headerButtonColour(HomePage.LOG_IN));
    }

    @Test
    @DisplayName("HOME-004: clicking the header's Log In control reaches the login form, as a visitor")
    void logInReachesTheLoginForm() {
        HomePage home = openAsVisitor();

        home.openModalFrom(HomePage.LOG_IN);

        assertTrue(home.loginFormIsReachable(),
                "Clicking the header's Log In control did not reach the login form's own fields.");
    }

    @Test
    @DisplayName("HOME-005: clicking the header's Register control reaches the registration form, as a visitor")
    void registerReachesTheRegistrationForm() {
        HomePage home = openAsVisitor();

        home.openModalFrom(HomePage.REGISTER);

        assertTrue(home.registerFormIsReachable(),
                "Clicking the header's Register control did not reach the registration form's own fields.");
    }

    @Test
    @DisplayName("HOME-006: the wallet panel lists every one of the account's confirmed currencies, signed in")
    void walletPanelListsEveryCurrency() {
        HomePage home = openAsMember();

        // No dropdown needs opening first: confirmed live 2026-10-25 that the header's own
        // "Account / Balance / Remain T.O" currency panel already lists every wallet the account
        // holds without the small "▼ HKD ..." badge ever being clicked - see
        // CurrencySwitcher's own class Javadoc.
        List<String> currencies = home.currencyCodesOnScreen();

        // The same five CurrencySwitcher's own Javadoc confirms for qa00002 on 2026-10-16 - this
        // is Quark's own confirmed set, not a literal carried over from the other site.
        assertTrue(currencies.containsAll(List.of("USD", "HKD", "THB", "USDT", "TRX")),
                "The wallet panel is missing a currency. It showed: " + currencies);
    }

    @Test
    @DisplayName("HOME-007: the page shows the signed-in account's own username, signed in")
    void pageShowsTheSignedInUsername() {
        HomePage home = openAsMember();

        assertTrue(home.showsText(USERNAME),
                "Expected the signed-in account's username (" + USERNAME + ") to appear "
                        + "somewhere on the page after signing in.");
    }
}
