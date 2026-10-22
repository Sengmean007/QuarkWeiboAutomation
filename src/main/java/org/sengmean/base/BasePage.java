package org.sengmean.base;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitUntilState;
import org.sengmean.config.ConfigDataProvider;
import org.sengmean.report.CaseLog;

/**
 * Shared behaviour for all page objects: holds the Playwright {@link Page}, resolves the
 * environment base URL, and clears the two overlays this site raises unprompted.
 */
public class BasePage {

    /** Overridable with -Dbase.url=..., set in Config/config.properties for the usual case. */
    public static final String BASE_URL = ConfigDataProvider.get("base.url", "https://test-v2.138hk.vip");

    protected Page page;

    public BasePage() {
    }

    public BasePage(Page page) {
        this.page = page;
    }

    /** Navigates to a path relative to the base URL, e.g. "/en/log-in/". */
    protected void open(String path) {
        page.navigate(url(path), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    public static String url(String path) {
        return BASE_URL + path;
    }

    /**
     * The VIP promo popup a fresh visitor is shown on the landing page, closed with the round
     * pink X button above the banner image.
     *
     * <h2>Selector confidence</h2>
     * This project was scaffolded from a manual exploratory session (2026-10-16) driven by
     * screenshots and coordinates rather than by reading the live DOM, so there was no
     * accessibility tree captured for this popup. The locator below targets the close button by
     * its only confirmed trait - an SVG/icon-only button with no accessible text, positioned
     * above the dialog - via a tolerant CSS attribute match. <b>Verify against the live DOM on
     * first run</b> and tighten this once the real markup is known.
     */
    private static final String VIP_POPUP_CLOSE =
            "button[class*='close' i], [class*='modal' i] [class*='close' i], "
                    + "[aria-label*='close' i]";

    /**
     * The Chatwoot-style support chat panel that opens by itself right after sign-in. Same
     * selector-confidence caveat as {@link #VIP_POPUP_CLOSE}: written from what a screenshot
     * showed (a round toggle button in the bottom-right corner that turns the panel into an X),
     * not from the live DOM.
     */
    private static final String CHAT_TOGGLE =
            "[class*='chat' i] button, [class*='woot' i] button, [id*='chat' i] button";

    /**
     * Closes the welcome banner, the VIP promo popup and the support chat panel if any is up.
     * The welcome banner is closed first: ported from the sibling Admin_weibo project, its
     * near-maximum z-index wins the hit test over everything else on the page, including the
     * VIP popup and the UI-version picker's own buttons. Best-effort for the latter two: logs
     * what it found rather than failing the case, since neither is part of the screen under test
     * (see weibo-hk-client-ui-check-scope for the sibling Admin_weibo project's same rule about
     * the Daily Mission popup and the Lucky Spin widget on this site).
     */
    public void dismissOverlays() {
        dismissWelcomeBanner();
        dismissIfPresent(VIP_POPUP_CLOSE, "VIP promo popup");
        dismissIfPresent(CHAT_TOGGLE, "support chat panel");
    }

    private void dismissIfPresent(String selector, String label) {
        try {
            Locator close = page.locator(selector).first();
            if (close.count() > 0 && close.isVisible()) {
                close.click(new Locator.ClickOptions().setTimeout(3_000));
                System.out.println("  Closed the " + label + ".");
            }
        } catch (PlaywrightException notThere) {
            // Nothing to close, or the page moved on under us - either way not a case failure.
        }
    }

    public String currentUrl() {
        return page.url();
    }

    // ---------------------------------------------------------------
    // The promotional welcome banner
    // ---------------------------------------------------------------

    /**
     * The promotional welcome banner, ported from the sibling Admin_weibo project (same site,
     * test-v2.138hk.vip). It renders a picture in the middle of the screen with a pink X above
     * it, which reads as something to ignore - it is not: the element is
     * {@code position:fixed; top:0; left:0; width:100%; height:100%} at a near-maximum z-index,
     * so while it is up it is the hit-test winner for every point on the page, including controls
     * that look sharp and clickable in a screenshot. A click underneath it does not fail with a
     * useful message: Playwright waits for the control to become clickable, the overlay never
     * yields, and the case dies on a timeout naming the control it wanted rather than the banner
     * sitting on top of it.
     *
     * <p>Its close button wraps {@code <img alt="Ads close">}; {@link #WELCOME_BANNER_CLOSE}
     * matches on that, because the button's own classes are utility soup and the banner image
     * itself (which opens the promotion) would match several of them. A JavaScript
     * {@code element.click()} does not dismiss it - it wants a trusted event, so it has to be a
     * real Playwright click. The site can stack more than one, so dismissal closes them in a
     * loop rather than once.
     */
    protected static final String WELCOME_BANNER = "div[class*='z-welcomeBannerModal']";

    /** The welcome banner's close control; see {@link #WELCOME_BANNER}. */
    protected static final String WELCOME_BANNER_CLOSE =
            "div[class*='z-welcomeBannerModal'] button:has(img[alt='Ads close'])";

    /** How long a clicked banner is given to stop covering the page. Override with -Dtest.welcomeBanners.goneTimeout=<ms>. */
    private static final double BANNER_GONE_TIMEOUT_MS =
            Double.parseDouble(ConfigDataProvider.get("test.welcomeBanners.goneTimeout", "6000"));

    /** How many stacked welcome banners are cleared before giving up. Override with -Dtest.welcomeBanners.max=<count>. */
    private static final int MAX_WELCOME_BANNERS =
            Math.max(1, Integer.parseInt(ConfigDataProvider.get("test.welcomeBanners.max", "5")));

    /** True while the promotional welcome banner is covering the page. */
    public boolean isWelcomeBannerOpen() {
        return isWelcomeBannerOpen(page);
    }

    /** As {@link #isWelcomeBannerOpen()}, for a page this object does not own. */
    public static boolean isWelcomeBannerOpen(Page target) {
        try {
            Locator banner = target.locator(WELCOME_BANNER);
            return banner.count() > 0 && banner.first().isVisible();
        } catch (PlaywrightException navigating) {
            return false;
        }
    }

    /** How many welcome banners are on the page right now. */
    public static int welcomeBannerCount(Page target) {
        try {
            return target.locator(WELCOME_BANNER).count();
        } catch (PlaywrightException navigating) {
            return 0;
        }
    }

    /**
     * Closes the welcome banner if it is up, and reports whether the page is now clear of it.
     * <b>True both when a banner was found and closed, and when none was there to begin with</b> -
     * this is a check-and-close action, not an assertion that one existed.
     */
    public boolean dismissWelcomeBanner() {
        return dismissWelcomeBanners(page, true);
    }

    /** As {@link #dismissWelcomeBanner()}, for a page this object does not own. */
    public static boolean dismissWelcomeBanners(Page target, boolean verbose) {
        int found = welcomeBannerCount(target);
        if (found == 0) {
            if (verbose) {
                CaseLog.step("Welcome banner: none on the page.");
            }
            return true;
        }
        CaseLog.step("Welcome banner: " + found + " found; closing " + (found == 1 ? "it." : "them all."));

        // Closed in a loop, not once: the site raises these per promotion, so a load can bring up
        // more than one stacked over each other, and dismissing the top one only reveals the
        // next. Bounded rather than "until none", so a close that silently does nothing cannot
        // spin here forever.
        int closed = 0;
        for (int attempt = 0; attempt < MAX_WELCOME_BANNERS && isWelcomeBannerOpen(target); attempt++) {
            try {
                target.locator(WELCOME_BANNER_CLOSE).first().click(new Locator.ClickOptions().setTimeout(5_000));
                // Gone means "no longer covering the page", not specifically "removed from the
                // DOM" - polling on visibility accepts either outcome, which is the one the
                // caller cares about.
                if (!awaitBannerGone(target)) {
                    throw new PlaywrightException("The banner was clicked but is still covering the page after "
                            + (long) (BANNER_GONE_TIMEOUT_MS / 1000) + "s.");
                }
                closed++;
            } catch (PlaywrightException stillThere) {
                CaseLog.step("Welcome banner " + (closed + 1) + " would not close after " + closed + " of "
                        + found + ": " + stillThere.getMessage());
                return false;
            }
        }
        return !isWelcomeBannerOpen(target);
    }

    /** Polls until no welcome banner is covering the page, or the budget runs out. */
    private static boolean awaitBannerGone(Page target) {
        long deadline = System.currentTimeMillis() + (long) BANNER_GONE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!isWelcomeBannerOpen(target)) {
                return true;
            }
            target.waitForTimeout(250);
        }
        return false;
    }

    // ---------------------------------------------------------------
    // The New UI / Classic UI picker
    // ---------------------------------------------------------------

    /**
     * A fresh visitor on this site is met by "Choose your preferred interface to get started.",
     * with a Classic Interface card ("Choose Classic") and a New Interface card ("Choose New");
     * the choice persists in {@code localStorage} as {@code ui-version}. Ported from the sibling
     * Admin_weibo project's {@code ClientCategoryHomePage}, which answers this picker with New
     * Interface only (the Other Category sections it tests render only there); this project
     * needs both answers, since {@link org.sengmean.tests.layout.ResponsiveLayoutNewUiTest} and
     * {@link org.sengmean.tests.layout.ResponsiveLayoutClassicUiTest} each check one.
     *
     * <h2>Selector confidence</h2>
     * The heading text, the two button names and the checkbox were confirmed against this exact
     * site (test-v2.138hk.vip) by Admin_weibo on 2026-10-08. Still unconfirmed here: whether
     * Classic UI's own home page renders a hero banner the same way New UI's does - see
     * {@code ResponsiveLayoutClassicUiTest}'s Javadoc.
     *
     * <h2>It may re-ask on every navigation</h2>
     * Admin_weibo measured that answering once was not always enough - the picker came back
     * after a plain navigation even after choosing New Interface. Ticking "Don't ask again
     * today" is the first line of defence; the retry loop here is the second.
     */
    private static final String UI_VERSION_HEADING = "Choose your preferred interface";

    /** True while the "Choose your preferred interface" picker is up. */
    public boolean isUiVersionPickerUp() {
        Locator picker = page.getByText(UI_VERSION_HEADING);
        return picker.count() > 0 && picker.first().isVisible();
    }

    /** The UI version this visitor is on ("new", "old"), or empty when none has been chosen yet. */
    public String uiVersion() {
        Object raw = page.evaluate(
                "() => { try { return localStorage.getItem('ui-version') || ''; } catch (e) { return ''; } }");
        return raw == null ? "" : String.valueOf(raw);
    }

    /**
     * True once <b>some</b> option has been chosen: the picker is no longer up and
     * {@code ui-version} carries a value. False while the picker is still up, or still empty
     * for any other reason (a fresh page that has not been offered the picker at all, say).
     */
    public boolean isUiVersionChosen() {
        return !isUiVersionPickerUp() && !uiVersion().isEmpty();
    }

    /** True once New Interface specifically is the chosen option ({@code ui-version == "new"}). */
    public boolean isNewUiChosen() {
        return "new".equalsIgnoreCase(uiVersion());
    }

    /** True once Classic Interface specifically is the chosen option ({@code ui-version == "old"}). */
    public boolean isClassicUiChosen() {
        return "old".equalsIgnoreCase(uiVersion());
    }

    /** Answers the picker with New Interface when it is up; harmless otherwise. */
    public void chooseNewUi() {
        answerUiVersionPicker("Choose New");
    }

    /** Answers the picker with Classic Interface when it is up; harmless otherwise. */
    public void chooseClassicUi() {
        answerUiVersionPicker("Choose Classic");
    }

    private void answerUiVersionPicker(String buttonName) {
        for (int attempt = 1; attempt <= 3 && isUiVersionPickerUp(); attempt++) {
            Locator dontAskAgain = page.getByRole(AriaRole.CHECKBOX)
                    .filter(new Locator.FilterOptions().setVisible(true));
            if (dontAskAgain.count() > 0 && !dontAskAgain.first().isChecked()) {
                dontAskAgain.first().check();
            }
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(buttonName).setExact(true))
                    .first().click();
            page.waitForLoadState();
            page.waitForTimeout(3_000);
        }
    }
}
