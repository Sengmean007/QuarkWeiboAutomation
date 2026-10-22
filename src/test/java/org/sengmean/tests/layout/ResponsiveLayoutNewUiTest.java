package org.sengmean.tests.layout;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.wallet.HomePage;
import org.sengmean.report.CaseLog;
import org.sengmean.support.Device;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The home page's layout on the New UI, as a visitor and as a signed-in member, across the five
 * {@link Device} profiles this project tests against: a Windows PC and an Android phone in
 * Chromium, a Mac, an iPad and an iPhone in WebKit, since Safari is WebKit and a Chromium window
 * pretending to be an iPhone exercises the wrong engine.
 *
 * <p>Split from the earlier single {@code ResponsiveLayoutTest} so the New UI and Classic UI
 * cases are independent suites - a device case under one UI can fail without implying anything
 * about the other, and each can be run on its own with {@code -Dsuite=ResponsiveSuite}.
 *
 * <h2>What every case checks</h2>
 * Every case opens the home page as a fresh visitor, clears the VIP promo popup
 * ({@link org.sengmean.base.BasePage#dismissOverlays()}), answers the UI-version picker with
 * New Interface ({@link org.sengmean.base.BasePage#chooseNewUi()}), clears the chat panel that
 * opens afterwards, then confirms New Interface is actually the chosen option
 * ({@link org.sengmean.base.BasePage#isNewUiChosen()} - the picker is down and {@code ui-version}
 * reads {@code "new"}, not just that the picker was answered) before asserting the two layout
 * checks: nothing runs off the side of the screen, and the hero banner renders
 * ({@link HomePage#hasHeroBanner()}). The no-overflow check reads
 * {@code document.body.scrollWidth} against the device's own viewport width via
 * {@code page.evaluate(...)} rather than a CSS locator - a deliberately robust, locator-free
 * check, given this project's own README caveat ("Selector confidence") that a handful of CSS
 * selectors here, including the hero banner's own, were never confirmed against the live DOM.
 * The chosen-option check is run again after the member rows return to the home page, so a UI
 * choice that silently reverted after sign-in is caught rather than assumed.
 *
 * <h2>Visitor vs. member</h2>
 * The visitor rows ({@code onPc}..{@code onAndroid}) check the layout before any sign-in. The
 * member rows ({@code memberOnPc}..{@code memberOnAndroid}) sign the shared QA account in first
 * ({@link org.sengmean.base.BaseTest#signIn(Page)}) - which answers the UI-version picker and
 * clears overlays again as part of signing in - then return to the home page and run the same
 * two checks, plus confirm the header still shows the signed-in state and that {@code ui-version}
 * survived the sign-in round trip. This mirrors the visitor/member walk the sibling Admin_weibo
 * project runs for its own client-side UI cases.
 *
 * <h2>Case ids</h2>
 * {@code UI-001} through {@code UI-010}: visitor {@code UI-001}-{@code UI-005}, member
 * {@code UI-006}-{@code UI-010}. Classic UI continues at {@code UI-011} in
 * {@code ResponsiveLayoutClassicUiTest}. Invented for this class, as there is no client
 * test-case workbook for this project to take ids from.
 *
 * <h2>WebKit</h2>
 * The Mac, iPad and iPhone cases need WebKit installed ({@code npx playwright install webkit});
 * see {@link org.sengmean.base.BaseTest#webkitOrAbort()}. A case whose engine cannot launch
 * aborts (Not run) naming the install it needs, rather than failing the whole class.
 */
public class ResponsiveLayoutNewUiTest extends BaseTest {

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

    /** The visitor walk: open as a visitor, then check the layout. */
    private void layoutHolds(Device device) {
        Page on = openAsVisitor(device);
        assertLayoutHolds(on, device, "New UI");
        capture(on, device, "home");
    }

    /** The member walk: open as a visitor (answering the picker), sign in, return to the home
     *  page, then check the layout the same way. */
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

    /** The three checks every row runs: the chosen UI version held, nothing overflows, and the
     *  hero banner renders. */
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
}
