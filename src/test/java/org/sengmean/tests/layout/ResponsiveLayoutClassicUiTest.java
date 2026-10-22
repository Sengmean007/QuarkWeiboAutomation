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
 * The home page's layout on Classic UI, as a visitor and as a signed-in member, across the same
 * five {@link Device} profiles as {@link ResponsiveLayoutNewUiTest} - see that class for the
 * device/engine rationale, the no-overflow check's reasoning, and the visitor/member walk this
 * mirrors. This is the other half of the split from the earlier single
 * {@code ResponsiveLayoutTest}.
 *
 * <h2>What every case checks - and what it deliberately does not</h2>
 * Every case opens the home page as a fresh visitor, clears the VIP promo popup, answers the
 * UI-version picker with Classic Interface ({@link org.sengmean.base.BasePage#chooseClassicUi()}),
 * clears the chat panel, confirms Classic Interface is actually the chosen option
 * ({@link org.sengmean.base.BasePage#isClassicUiChosen()} - the picker is down and
 * {@code ui-version} reads {@code "old"}, not just that the picker was answered), then asserts
 * there is no horizontal overflow at that viewport - the same robust, locator-free check as the
 * New UI class. The member rows additionally sign the shared QA account in, return to the home
 * page, confirm the header still shows the signed-in state, and run the chosen-option check
 * again so a choice that silently reverted after sign-in is caught rather than assumed.
 *
 * <p><b>No case here asserts a hero banner must render.</b> {@link HomePage#hasHeroBanner()} was
 * written and confirmed against the New UI's home page only; whether Classic UI's own home page
 * carries an equivalent banner, and whether the same locator would even find it, is unconfirmed
 * (see this project's README "Selector confidence" section) - for either a visitor or a member.
 * Asserting it here could manufacture a false defect on the very first real run. The finding is
 * logged to each case's evidence trail instead, so a human reviewing the report can see what
 * Classic UI actually showed and promote this to a real assertion once confirmed.
 *
 * <h2>Case ids</h2>
 * {@code UI-011} through {@code UI-020}, continuing the New UI class's {@code UI-001}-
 * {@code UI-010} under the same invented {@code UI-} prefix: visitor {@code UI-011}-
 * {@code UI-015}, member {@code UI-016}-{@code UI-020}.
 *
 * <h2>WebKit</h2>
 * The Mac, iPad and iPhone cases need WebKit installed ({@code npx playwright install webkit});
 * see {@link org.sengmean.base.BaseTest#webkitOrAbort()}.
 */
public class ResponsiveLayoutClassicUiTest extends BaseTest {

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

    /** The visitor walk: open as a visitor, then check the layout. */
    private void layoutHolds(Device device) {
        Page on = openAsVisitor(device);
        assertLayoutHolds(on, device, "Classic UI");
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
}
