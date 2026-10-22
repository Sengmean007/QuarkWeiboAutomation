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
 * One member's end-to-end journey through the New UI home page, on each of this project's five
 * {@link Device} profiles - ported from NewWeibo_automation's {@code UxJourneyBase} /
 * {@code DesktopUxJourneyTest} / {@code IpadUxJourneyTest} / {@code IphoneUxJourneyTest} /
 * {@code AndroidUxJourneyTest}, which ran that site's own 34-screen crawl once per device across
 * four device-named classes (no Mac).
 *
 * <h2>Why this class is shaped differently from what it was ported from</h2>
 * NewWeibo_automation splits one device per class and one journey step per {@code @Test}, because
 * it has a confirmed {@code SiteScreen}/{@code SiteTab} crawl and dedicated page objects for every
 * cashier screen, the member sidebar and the game lobby. Quark has none of that - no client
 * workbook, no site crawl, and only {@link LoginPage}, {@code RegisterPage}, {@link HomePage} and
 * {@link CurrencySwitcher} as confirmed page objects (see this project's README, "Selector
 * confidence"). Rather than invent a wishlist of Deposit/Withdrawal/Transfer/Records/game-lobby
 * page objects against unconfirmed locators, this class follows this project's own established
 * shape instead - {@code ResponsiveLayoutNewUiTest}'s one-class-per-UI-version, one-{@code @Test}-
 * per-device layout - and keeps every step to what is realistic to actually verify today.
 *
 * <h2>The journey each device method runs</h2>
 * <ol>
 *   <li><b>Arrive as a guest</b>: the home page opens, the welcome banner/VIP popup/chat panel are
 *       cleared, the New UI picker is answered and confirmed to have stuck, the header shows the
 *       guest state, nothing runs off the side of the screen, and the hero banner renders
 *       ({@link HomePage#hasHeroBanner()} - confirmed against New UI, see {@code ResponsiveLayoutNewUiTest});</li>
 *   <li><b>browse</b>: a link found in the page is followed and the destination is confirmed to
 *       show real content rather than a blank or broken page - see "Unconfirmed addition" below;</li>
 *   <li><b>sign in</b> as the shared QA account, and the New UI choice is confirmed to have
 *       survived the round trip;</li>
 *   <li><b>the wallet</b>: the signed-in header's currency code is read back
 *       ({@link CurrencySwitcher#currentCurrency()}) and confirmed non-blank - a read-only touch of
 *       the money area, see "What was skipped" below for why nothing further is checked there;</li>
 *   <li><b>sign out</b>, confirming the header returns to the guest state and stays that way after
 *       a fresh navigation to the home page.</li>
 * </ol>
 * Nothing is deposited, withdrawn, transferred or bet, and no currency is actually switched - safe
 * to run unattended, like every other suite in this project.
 *
 * <h2>Unconfirmed addition: following a link</h2>
 * {@link #followAGenericLink(Page)} is new for this class, since Quark has no confirmed
 * navigation/category locators the way the sibling project's {@code UxAudit.openCategory} does.
 * It looks for any visible, same-site link that is not the sign-in/sign-out/UI-picker controls,
 * follows the first one that is clickable, and asserts only the generic contract every page on a
 * site owes a visitor: it is not a blank shell. If no such link is found, the step logs that and
 * moves on without failing the case - the site's actual navigation structure was never captured
 * (unlike the login/register paths, which {@code LoginPage}/{@code RegisterPage} already use and
 * this class reuses as-is).
 *
 * <h2>What was skipped, and why</h2>
 * NewWeibo's own journey additionally covers: ten game categories plus Promotions plus eight
 * footer pages plus four auth forms plus seven member pages (its {@code SiteScreen} crawl);
 * Deposit, Withdrawal, Transfer and Records as named cashier screens; a game's start-confirmation
 * dialog; the header's '&gt;'/'&lt;' category-scroll arrows; and the Referral link/bonus tabs.
 * Every one of those depends on either a confirmed screen crawl this project does not have, or a
 * page object this project does not have ({@code DepositPage}, {@code WithdrawalPage},
 * {@code TransferPage}, {@code RecordPage}, a game-lobby page) - all Weibo-Empire-specific
 * infrastructure, not anything generic enough to approximate safely without a manual session to
 * confirm the real markup first. Writing locators for any of them now would risk manufacturing
 * false defects rather than real coverage, which is why this journey stays to the five steps above.
 *
 * <h2>Case ids</h2>
 * {@code UX-001} through {@code UX-005}, one per device (PC, Mac, iPad, iPhone, Android) - this
 * project's own invented {@code UX-} prefix, there being no client workbook to take one from (see
 * {@code UI-} in {@code ResponsiveLayoutNewUiTest} for the same reasoning). Classic UI continues
 * at {@code UX-006} in {@link UxJourneyClassicUiTest}.
 *
 * <h2>WebKit</h2>
 * The Mac, iPad and iPhone cases need WebKit installed ({@code npx playwright install webkit});
 * see {@link org.sengmean.base.BaseTest#webkitOrAbort()}.
 */
public class UxJourneyNewUiTest extends BaseTest {

    @Test
    @DisplayName("UX-001: New UI end-to-end member journey on PC (Windows, Chrome, 1920x1080)")
    void onPc() {
        journey(Device.DESKTOP_PC);
    }

    @Test
    @DisplayName("UX-002: New UI end-to-end member journey on Mac (macOS, Safari/WebKit, 1440x900)")
    void onMac() {
        journey(Device.DESKTOP_MAC);
    }

    @Test
    @DisplayName("UX-003: New UI end-to-end member journey on iPad (iPadOS, Safari/WebKit, 820x1180, touch)")
    void onIpad() {
        journey(Device.IPAD);
    }

    @Test
    @DisplayName("UX-004: New UI end-to-end member journey on iPhone (iOS, Safari/WebKit, 390x844, touch)")
    void onIphone() {
        journey(Device.IPHONE);
    }

    @Test
    @DisplayName("UX-005: New UI end-to-end member journey on Android (Pixel 7, Chrome, 412x915, touch)")
    void onAndroid() {
        journey(Device.ANDROID);
    }

    // =====================================================================

    /** The five-step journey described in this class's Javadoc, run on one device. */
    private void journey(Device device) {
        Page on = openOn(device);
        CaseLog.data("Device", device + " in " + device.engine);
        HomePage home = new HomePage(on);
        LoginPage login = new LoginPage(on);

        // Step 1: arrive as a guest.
        CaseLog.step("Arriving as a guest on " + device.label);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();
        assertTrue(home.isNewUiChosen(), "Expected New Interface to be the chosen UI version on "
                + device.label + " - ui-version is \"" + home.uiVersion() + "\"");
        assertFalse(login.isSignedIn(), "A fresh visit on " + device.label
                + " should not already show the signed-in header");
        assertNoHorizontalOverflow(on, device, "The landing page");
        assertTrue(home.hasHeroBanner(), "The hero banner should render under the nav on "
                + device.label + " as a guest");
        capture(on, device, "landing");

        // Step 2: browse - follow a link away from home (unconfirmed addition, see class Javadoc).
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
            CaseLog.evidence("No followable internal link was found on the New UI home page on "
                    + device.label + " - this project has no confirmed navigation locator, so the "
                    + "browse step is logged rather than asserted this run.");
        }
        home.navigateTo();
        home.dismissOverlays();

        // Step 3: sign in.
        CaseLog.step("Signing in as " + USERNAME + " on " + device.label);
        signIn(on);
        assertTrue(login.isSignedIn(), "Signing in on " + device.label + " did not leave a signed-in header");
        assertTrue(home.isNewUiChosen(), "Expected New Interface to still be the chosen UI version "
                + "on " + device.label + " after signing in - ui-version is \"" + home.uiVersion() + "\"");
        assertNoHorizontalOverflow(on, device, "The signed-in landing page");
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
     * locator-free check {@code ResponsiveLayoutNewUiTest} uses.
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
     * link. Returns false, having clicked nothing, if no such link is found - see this class's
     * Javadoc, "Unconfirmed addition", for why this exists and why it does not fail the case.
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
