package org.sengmean.tests.home;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.home.HomePage;
import org.sengmean.page.wallet.CurrencySwitcher;
import org.sengmean.report.CaseLog;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Classic UI half of the split described in {@link HomePageNewUiTest} - see that class for
 * where these cases come from, what was generalised, and the full list of what NewWeibo_empire's
 * own 33-case {@code HomePageTest} was skipped as either not applicable to this project's auth
 * flow (Account Activation) or an unconfirmed feature on Quark's site (game navigation, language
 * switching).
 *
 * <h2>What every case checks</h2>
 * As {@link HomePageNewUiTest}, but answers the UI-version picker with Classic Interface
 * ({@link org.sengmean.base.BasePage#chooseClassicUi()}) and confirms that choice stuck
 * ({@link org.sengmean.base.BasePage#isClassicUiChosen()}) instead - the same split
 * {@code ResponsiveLayoutClassicUiTest} already uses against {@code ResponsiveLayoutNewUiTest}.
 *
 * <h2>Assumption carried over from {@code HomePageNewUiTest}</h2>
 * Every one of these seven checks is assumed UI-version-independent: header furniture, the
 * login/register hand-off and the signed-in wallet list are not the kind of content a "New
 * Interface" / "Classic Interface" picker would plausibly change, unlike the hero banner
 * {@code ResponsiveLayoutClassicUiTest} itself declines to assert on for Classic UI. If Classic
 * UI turns out to render its header or auth controls differently, that difference is exactly what
 * a red case here would be reporting.
 *
 * <h2>Case ids</h2>
 * {@code HOME-008} through {@code HOME-014}, continuing {@link HomePageNewUiTest}'s
 * {@code HOME-001}-{@code HOME-007} under the same invented {@code HOME-} prefix.
 */
public class HomePageClassicUiTest extends BaseTest {

    /** Opens the home page as a fresh visitor with Classic Interface chosen and overlays cleared. */
    private HomePage openAsVisitor() {
        Page page = page();
        HomePage home = new HomePage(page);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseClassicUi();
        home.dismissOverlays();

        assertTrue(home.isClassicUiChosen(),
                "Expected Classic Interface to be the chosen UI version - ui-version is \""
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
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to still be the chosen "
                + "UI version after signing in - ui-version is \"" + home.uiVersion() + "\"");
        return home;
    }

    @Test
    @DisplayName("HOME-008: the landing page renders its header furniture, as a visitor (Classic UI)")
    void landingPageRendersHeaderFurniture() {
        HomePage home = openAsVisitor();

        assertTrue(home.logoIsRendered(), "The header logo did not decode; src=" + home.logoSrc());
        assertTrue(home.isHeaderButtonVisible(HomePage.LOG_IN)
                        || home.isHeaderButtonVisible(HomePage.REGISTER),
                "Expected at least one of \"Log In\" / \"Register\" visible in the header.");
    }

    @Test
    @DisplayName("HOME-009: the logo sits at the top left, as a visitor (Classic UI)")
    void logoIsTopLeft() {
        HomePage home = openAsVisitor();

        assertTrue(home.logoIsRendered(), "The logo image did not decode; src=" + home.logoSrc());
        assertTrue(home.logoIsTopLeft(), "The logo is not at the top left; box=" + home.logoBox());
    }

    @Test
    @DisplayName("HOME-010: Log In sits at the top right, as a visitor (Classic UI)")
    void logInIsTopRight() {
        HomePage home = openAsVisitor();

        assertTrue(home.isHeaderButtonVisible(HomePage.LOG_IN), "No visible Log In control in the header");
        assertTrue(home.isHeaderButtonOnTheRight(HomePage.LOG_IN),
                "Log In is not on the right; box=" + home.headerButtonBox(HomePage.LOG_IN));
        CaseLog.data("Log In text colour", home.headerButtonColour(HomePage.LOG_IN));
    }

    @Test
    @DisplayName("HOME-011: clicking the header's Log In control reaches the login form, as a visitor (Classic UI)")
    void logInReachesTheLoginForm() {
        HomePage home = openAsVisitor();

        home.openModalFrom(HomePage.LOG_IN);

        assertTrue(home.loginFormIsReachable(),
                "Clicking the header's Log In control did not reach the login form's own fields.");
    }

    @Test
    @DisplayName("HOME-012: clicking the header's Register control reaches the registration form, as a visitor (Classic UI)")
    void registerReachesTheRegistrationForm() {
        HomePage home = openAsVisitor();

        home.openModalFrom(HomePage.REGISTER);

        assertTrue(home.registerFormIsReachable(),
                "Clicking the header's Register control did not reach the registration form's own fields.");
    }

    @Test
    @DisplayName("HOME-013: the wallet panel lists every one of the account's confirmed currencies, signed in (Classic UI)")
    void walletPanelListsEveryCurrency() {
        HomePage home = openAsMember();

        new CurrencySwitcher(page()).open();
        List<String> currencies = home.currencyCodesOnScreen();

        assertTrue(currencies.containsAll(List.of("USD", "HKD", "THB", "USDT", "TRX")),
                "The wallet panel is missing a currency. It showed: " + currencies);
    }

    @Test
    @DisplayName("HOME-014: the page shows the signed-in account's own username, signed in (Classic UI)")
    void pageShowsTheSignedInUsername() {
        HomePage home = openAsMember();

        assertTrue(home.showsText(USERNAME),
                "Expected the signed-in account's username (" + USERNAME + ") to appear "
                        + "somewhere on the page after signing in.");
    }
}
