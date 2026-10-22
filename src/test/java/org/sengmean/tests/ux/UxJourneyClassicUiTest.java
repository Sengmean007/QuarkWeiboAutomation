package org.sengmean.tests.ux;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.wallet.CurrencySwitcher;
import org.sengmean.page.wallet.HomePage;
import org.sengmean.report.CaseLog;
import org.sengmean.support.Device;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The other half of {@link UxJourneyNewUiTest}'s split: the same end-to-end member journey, run
 * on Classic UI instead - see that class's Javadoc for the full journey description, why this
 * class is shaped as one-{@code @Test}-per-device rather than NewWeibo_automation's one-class-
 * per-device/one-{@code @Test}-per-step, the unconfirmed "follow a link" addition, and the
 * NewWeibo journey steps this project skips and why.
 *
 * <h2>The one difference from the New UI class</h2>
 * {@link HomePage#hasHeroBanner()} was written and confirmed against the New UI home page only
 * ({@code ResponsiveLayoutClassicUiTest}'s own Javadoc says so); whether Classic UI's own home
 * page carries an equivalent banner is unconfirmed. So, exactly as {@code ResponsiveLayoutClassicUiTest}
 * does for its own layout cases, this class logs what {@code hasHeroBanner()} finds to the case's
 * evidence trail instead of asserting on it, both as a guest and signed in - every other step in
 * the journey is identical to {@link UxJourneyNewUiTest}, with {@code chooseClassicUi()} /
 * {@code isClassicUiChosen()} in place of the New UI equivalents.
 *
 * <h2>Case ids</h2>
 * {@code UX-006} through {@code UX-010}, one per device, continuing immediately after
 * {@link UxJourneyNewUiTest}'s {@code UX-001}-{@code UX-005} under the same invented {@code UX-}
 * prefix.
 *
 * <h2>WebKit</h2>
 * The Mac, iPad and iPhone cases need WebKit installed ({@code npx playwright install webkit});
 * see {@link org.sengmean.base.BaseTest#webkitOrAbort()}.
 */
public class UxJourneyClassicUiTest extends BaseTest {

    @Test
    @DisplayName("UX-006: Classic UI end-to-end member journey on PC (Windows, Chrome, 1920x1080)")
    void onPc() {
        journey(Device.DESKTOP_PC);
    }

    @Test
    @DisplayName("UX-007: Classic UI end-to-end member journey on Mac (macOS, Safari/WebKit, 1440x900)")
    void onMac() {
        journey(Device.DESKTOP_MAC);
    }

    @Test
    @DisplayName("UX-008: Classic UI end-to-end member journey on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
    void onIpad() {
        journey(Device.IPAD);
    }

    @Test
    @DisplayName("UX-009: Classic UI end-to-end member journey on iPhone (iOS, Safari/WebKit, 390x844, touch)")
    void onIphone() {
        journey(Device.IPHONE);
    }

    @Test
    @DisplayName("UX-010: Classic UI end-to-end member journey on Android (Pixel 7, Chrome, 412x915, touch)")
    void onAndroid() {
        journey(Device.ANDROID);
    }

    // =====================================================================

    /** The same five-step journey as {@link UxJourneyNewUiTest#journey(Device)}, under Classic UI. */
    private void journey(Device device) {
        Page on = openOn(device);
        CaseLog.data("Device", device + " in " + device.engine);
        HomePage home = new HomePage(on);
        LoginPage login = new LoginPage(on);

        // Step 1: arrive as a guest.
        CaseLog.step("Arriving as a guest on " + device.label);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseClassicUi();
        home.dismissOverlays();
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to be the chosen UI version on "
                + device.label + " - ui-version is \"" + home.uiVersion() + "\"");
        assertFalse(login.isSignedIn(), "A fresh visit on " + device.label
                + " should not already show the signed-in header");
        assertNoHorizontalOverflow(on, device, "The landing page");
        CaseLog.data("Hero banner as a guest, " + device.label,
                home.hasHeroBanner() ? "rendered" : "not found - unconfirmed whether Classic UI "
                        + "carries one at all; see this class's Javadoc before treating this as a defect");
        capture(on, device, "landing");

        // Step 2: browse - follow a link away from home (unconfirmed addition, see UxJourneyNewUiTest's Javadoc).
        CaseLog.step("Following a link away from the home page on " + device.label);
        String before = on.url();
        if (followAGenericLink(on)) {
            assertFalse(on.url().equals(before), "Following a link on " + device.label
                    + " left the browser at the same address, " + before);
            int shown = visibleTextLength(on);
            CaseLog.data("Visible text length on the followed page", String.valueOf(shown));
            assertTrue(shown > 100, "The page followed to from the home page on " + device.label
                    + " shows almost no text (" + shown + " characters) - an empty shell rather than a page");
            CaseLog.evidence("Followed a link to " + on.url());
        } else {
            CaseLog.evidence("No followable internal link was found on the Classic UI home page on "
                    + device.label + " - this project has no confirmed navigation locator, so the "
                    + "browse step is logged rather than asserted this run.");
        }
        home.navigateTo();
        home.dismissOverlays();

        // Step 3: sign in.
        CaseLog.step("Signing in as " + USERNAME + " on " + device.label);
        signIn(on);
        assertTrue(login.isSignedIn(), "Signing in on " + device.label + " did not leave a signed-in header");
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to still be the chosen UI "
                + "version on " + device.label + " after signing in - ui-version is \"" + home.uiVersion() + "\"");
        assertNoHorizontalOverflow(on, device, "The signed-in landing page");
        CaseLog.data("Hero banner signed in, " + device.label,
                home.hasHeroBanner() ? "rendered" : "not found - see this class's Javadoc");
        capture(on, device, "signed-in");

        // Step 4: the wallet, read-only.
        String currency = new CurrencySwitcher(on).currentCurrency();
        CaseLog.data("Wallet currency shown after sign-in", currency);
        assertFalse(currency == null || currency.isBlank(),
                "The signed-in header on " + device.label + " should show a wallet currency code");

        // Step 5: sign out.
        CaseLog.step("Signing out on " + device.label);
        login.signOut();
        assertFalse(login.isSignedIn(), "Signing out on " + device.label + " should return the header to the guest state");
        home.navigateTo();
        assertFalse(login.isSignedIn(), "After signing out on " + device.label
                + ", a fresh visit to the home page should still show the guest state");
    }

    /**
     * Confirms nothing runs off the side of the screen at this device's viewport, the same
     * locator-free check {@code ResponsiveLayoutClassicUiTest} uses.
     */
    private void assertNoHorizontalOverflow(Page on, Device device, String screenName) {
        Number scrollWidth = (Number) on.evaluate("document.body.scrollWidth");
        CaseLog.data("document.body.scrollWidth at " + device.width + "px viewport", String.valueOf(scrollWidth));
        assertTrue(scrollWidth.intValue() <= device.width, screenName + " on " + device.label
                + " is wider than its " + device.width + "px viewport (document.body.scrollWidth is "
                + scrollWidth + ")");
    }

    /** How much visible text {@code document.body} carries right now. */
    private int visibleTextLength(Page on) {
        Object length = on.evaluate("() => document.body.innerText ? document.body.innerText.length : 0");
        return length == null ? 0 : ((Number) length).intValue();
    }

    private static final Pattern EXCLUDED_LINK_TEXT =
            Pattern.compile("(log in|log out|register|choose new|choose classic)", Pattern.CASE_INSENSITIVE);

    /**
     * Finds and follows the first visible, same-site link on the page that is not one of the
     * sign-in/sign-out/UI-picker controls, and is not a new-tab, javascript:, mailto: or tel:
     * link. Returns false, having clicked nothing, if no such link is found.
     */
    private boolean followAGenericLink(Page on) {
        Locator links = on.locator("a[href^='/']:visible")
                .filter(new Locator.FilterOptions().setHasNotText(EXCLUDED_LINK_TEXT));
        int count = links.count();
        for (int i = 0; i < count; i++) {
            Locator link = links.nth(i);
            String href = link.getAttribute("href");
            if (href == null || href.isBlank() || href.equals("/")
                    || href.contains("log-in") || href.contains("register")) {
                continue;
            }
            String target = link.getAttribute("target");
            if ("_blank".equalsIgnoreCase(target)) {
                continue;
            }
            try {
                link.click(new Locator.ClickOptions().setTimeout(5_000));
                on.waitForLoadState();
                on.waitForTimeout(1_000);
                return true;
            } catch (PlaywrightException cannotClick) {
                // Try the next candidate - this one was not actually clickable (hidden under an
                // overlay, detached by a re-render, etc.).
            }
        }
        return false;
    }
}
