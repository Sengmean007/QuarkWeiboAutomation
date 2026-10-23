package org.sengmean.base;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import org.sengmean.config.ConfigDataProvider;
import org.sengmean.report.CaseLog;
import org.sengmean.support.Language;

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

    /**
     * Navigates to a path relative to the base URL, e.g. "/en/log-in/", then waits for the page
     * to actually be ready - see {@link #awaitReady()} for why {@code domcontentloaded} alone is
     * not enough to call a freshly navigated page rendered.
     */
    protected void open(String path) {
        page.navigate(url(path), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        awaitReady();
    }

    public static String url(String path) {
        return BASE_URL + path;
    }

    /**
     * Waits until the document is ready, then gives {@code load} a brief, best-effort chance to
     * arrive, then a short fixed settle - the fix for a case that went on (reading state,
     * dismissing overlays, asserting layout, taking its screenshot) before the page had actually
     * rendered.
     *
     * <h2>Why {@code domcontentloaded} alone was not enough</h2>
     * {@link #open(String)} navigates with {@code domcontentloaded}, which fires as soon as the
     * HTML document is parsed - before this site's React shell has hydrated, before the
     * UI-version picker, the welcome banner or the Daily Mission panel have actually painted.
     * A case that called {@link #dismissOverlays()} or read {@link #uiVersion()} right after
     * {@code open()} could find nothing there yet, not because nothing was coming but because it
     * had not arrived - and the case would finish (and {@code capture()} its screenshot) against
     * that half-rendered page. Waiting for the {@code load} event too - ported from the sibling
     * Admin_kuake project's {@code BasePage.awaitReady()}, measured against this same site - gets
     * past most of that gap; it is kept best-effort (a short timeout it is allowed to miss) rather
     * than strict, because a slow third-party beacon (chat widget, analytics) must never fail a
     * case that is otherwise ready. The fixed settle on top covers the rest: this site's own
     * content (the picker, the banners) can still paint a moment after {@code load}.
     *
     * <p>Override either wait with {@code -Dtest.settleAfterReady=<ms>} (default 5000) or
     * {@code -Dtest.renderSettle=<ms>} (default 1500).
     */
    protected void awaitReady() {
        awaitReady(page);
    }

    /** As {@link #awaitReady()}, for a page this object does not own. */
    public static void awaitReady(Page target) {
        target.waitForLoadState(LoadState.DOMCONTENTLOADED);
        try {
            target.waitForLoadState(LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(SETTLE_AFTER_READY_MS));
        } catch (PlaywrightException stillLoading) {
            // Expected on this site and not a problem by itself: the document is ready and the
            // render settle below, plus each case's own waits, take it from here. Recorded rather
            // than swallowed, because a page that never reaches `load` is worth seeing in the
            // trail if a case does go wrong later.
            CaseLog.step("The page was ready but had not finished loading subresources within "
                    + (SETTLE_AFTER_READY_MS / 1000) + "s; carrying on.");
        }
        target.waitForTimeout(RENDER_SETTLE_MS);
    }

    /** How long {@link #awaitReady()} lets {@code load} run over before carrying on regardless. */
    private static final double SETTLE_AFTER_READY_MS =
            ConfigDataProvider.number("test.settleAfterReady", 5_000);

    /** How long {@link #awaitReady()} gives the page's own content a fixed chance to paint after
     *  {@code load} (or after giving up on it) before the caller is allowed to go on. */
    private static final double RENDER_SETTLE_MS =
            ConfigDataProvider.number("test.renderSettle", 1_500);

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
     * Closes the welcome banner, the Daily Mission panel, the VIP promo popup and the support
     * chat panel if any is up. The welcome banner is closed first: ported from the sibling
     * Admin_weibo project, its near-maximum z-index wins the hit test over everything else on the
     * page, including the Daily Mission panel, the VIP popup and the UI-version picker's own
     * buttons. Best-effort for the VIP popup and the chat panel: logs what it found rather than
     * failing the case, since neither is part of the screen under test (see
     * weibo-hk-client-ui-check-scope for the sibling Admin_weibo project's same rule about the
     * Lucky Spin widget on this site). Every one of the four is a check-and-close: closed when it
     * is up, left alone - not an error, not even logged as a miss - when it is not.
     */
    public void dismissOverlays() {
        dismissWelcomeBanner();
        dismissDailyMission();
        dismissIfPresent(VIP_POPUP_CLOSE, "VIP promo popup");
        dismissIfPresent(CHAT_TOGGLE, "support chat panel");
    }

    /**
     * Closes the first <b>visible</b> match of {@code selector}, if any.
     *
     * <h2>Why {@code filter(visible)} before {@code .first()}, not after</h2>
     * This used to take the first DOM match and only then check whether <i>that one</i> was
     * visible - silently doing nothing otherwise, even with a visible match elsewhere in the same
     * selector. Confirmed live (2026-10-23) exactly this way for {@link #CHAT_TOGGLE}: Chatwoot
     * renders two bubble buttons for its two states (closed/open), both always present, and the
     * closed-state one (permanently {@code display:none}-equivalent while the panel is open) sits
     * first in the DOM - so {@code .first()} landed on the invisible one, {@code isVisible()} was
     * false, and the chat panel's real close control (the second match, visible, the one that
     * would have actually closed it) was never clicked. That silent no-op is what left the panel
     * open and covering the language menu in {@code BasePage.selectLanguage}, timing out every
     * case in {@code LoginMultiLanguageTest} on a row click the panel was sitting on top of.
     * Filtering to visible elements first, then taking {@code .first()} of what is left, picks
     * whichever match is actually on screen regardless of which one the DOM lists first.
     */
    private void dismissIfPresent(String selector, String label) {
        try {
            Locator close = page.locator(selector)
                    .filter(new Locator.FilterOptions().setVisible(true)).first();
            if (close.count() > 0) {
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
    // The Daily Mission panel
    // ---------------------------------------------------------------

    /**
     * The Daily Mission panel a signed-in member can be met with: "Complete N days of 5 days"
     * with a Claim reward button and an X. Ported from the sibling Admin_kuake project (same
     * site, test-v2.138hk.vip): its "DAILY MISSION" title renders as an image, so the panel
     * cannot be found by that text - it is known instead by the words it does render, which is
     * why {@link #isDailyMissionUp()} checks several phrases rather than one heading.
     */
    private static final String DAILY_MISSION_PHRASE_1 = "daily mission";
    private static final String DAILY_MISSION_PHRASE_2 = "days of 5 days";
    private static final String DAILY_MISSION_PHRASE_3 = "claim reward";

    /** True while the Daily Mission panel is covering the page. */
    public boolean isDailyMissionUp() {
        return hasVisibleText(DAILY_MISSION_PHRASE_1) || hasVisibleText(DAILY_MISSION_PHRASE_2)
                || hasVisibleText(DAILY_MISSION_PHRASE_3);
    }

    /**
     * Closes the Daily Mission panel if it is up, and reports whether the page is now clear of
     * it. <b>True both when a panel was found and closed, and when none was there to begin
     * with</b> - this is a check-and-close action, not an assertion that one existed.
     */
    public boolean dismissDailyMission() {
        if (!isDailyMissionUp()) {
            CaseLog.step("Daily Mission: none on the page.");
            return true;
        }
        CaseLog.step("Daily Mission: panel found; closing it.");
        for (int attempt = 0; attempt < 2 && isDailyMissionUp(); attempt++) {
            closeTopmostPanel("the Daily Mission panel");
        }
        if (isDailyMissionUp()) {
            CaseLog.evidence("The Daily Mission panel offered no close control that worked after 2 attempts.");
        }
        return !isDailyMissionUp();
    }

    /** True when a visible element on the page carries this text (case-insensitive). */
    private boolean hasVisibleText(String text) {
        try {
            Object raw = page.evaluate(
                    "t => [...document.querySelectorAll('div, section, h1, h2, h3, span, p')]"
                            + ".some(e => e.offsetParent !== null && e.children.length < 40"
                            + " && (e.innerText || '').toLowerCase().includes(t))", text);
            return Boolean.TRUE.equals(raw);
        } catch (PlaywrightException navigating) {
            return false;
        }
    }

    /**
     * Clicks the close control of whatever panel is on top: an element whose own text is an X,
     * or whose aria-label, class or alt names "close", nearest the smallest visible candidate
     * that has one; presses Escape when none is found. Ported from the sibling Admin_kuake
     * project's {@code closeTopmost}, used there for the same panel.
     */
    private void closeTopmostPanel(String what) {
        Object done = page.evaluate("""
                () => {
                  const isClose = e => {
                    const t = (e.innerText || e.textContent || '').trim();
                    const cls = e.className && e.className.baseVal !== undefined ? e.className.baseVal : String(e.className || '');
                    return /^[×✕✖x]$/i.test(t) || /close/i.test(e.getAttribute('aria-label') || '')
                        || /close/i.test(cls) || /close/i.test(e.getAttribute('alt') || '');
                  };
                  const closes = [...document.querySelectorAll('button, span, div, svg, img, a')]
                    .filter(e => e.offsetParent !== null || e.tagName === 'svg')
                    .filter(isClose)
                    .filter(e => { const r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0 && r.width < 80 && r.height < 80; });
                  if (!closes.length) return 'none';
                  const pick = closes.reverse().find(e => { let n = e; while (n && n !== document.body) {
                    const p = getComputedStyle(n).position; if (p === 'fixed' || p === 'absolute') return true; n = n.parentElement; } return false; }) || closes[0];
                  pick.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true}));
                  return 'clicked ' + pick.tagName.toLowerCase();
                }""");
        if ("none".equals(done)) {
            CaseLog.step("No close control found for " + what + " - pressing Escape.");
            page.keyboard().press("Escape");
        }
        page.waitForTimeout(900);
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

    /**
     * The UI version this visitor is on ("new", "old"), or empty when none has been chosen yet.
     *
     * <p>Confirmed live against test-v2.138hk.vip on 2026-10-22: {@code localStorage['ui-version']}
     * is a JSON object, {@code {"version":"new"}} / {@code {"version":"old"}}, not the bare string
     * this method's callers compare against - a site-side change made sometime between the
     * 11:02 and 11:50 runs that same day (earlier same-day runs still saw the bare string and
     * passed). The {@code "version"} field is unwrapped here so every caller's bare "new"/"old"
     * comparison keeps working without having to know the storage shape; the bare-string form is
     * still read as a fallback in case the site reverts or another environment still uses it.
     */
    public String uiVersion() {
        Object raw = page.evaluate(
                "() => {\n"
                        + "  try {\n"
                        + "    const stored = localStorage.getItem('ui-version');\n"
                        + "    if (!stored) return '';\n"
                        + "    try {\n"
                        + "      const parsed = JSON.parse(stored);\n"
                        + "      if (parsed && typeof parsed === 'object' && 'version' in parsed) {\n"
                        + "        return parsed.version || '';\n"
                        + "      }\n"
                        + "    } catch (notJson) {\n"
                        + "      // Bare string form - used as-is below.\n"
                        + "    }\n"
                        + "    return stored;\n"
                        + "  } catch (e) { return ''; }\n"
                        + "}");
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

    /**
     * Answers the picker with New Interface when it is up, then falls back to the header's own
     * UI toggle if the wrong version is still showing afterward - see {@link #ensureUiVersion}.
     */
    public void chooseNewUi() {
        answerUiVersionPicker("Choose New");
        ensureUiVersion(true);
    }

    /** As {@link #chooseNewUi()}, for Classic Interface. */
    public void chooseClassicUi() {
        answerUiVersionPicker("Choose Classic");
        ensureUiVersion(false);
    }

    /**
     * Falls back to the header's own New UI / Classic UI toggle - confirmed live (2026-10-23) to
     * exist on every page, independent of the first-visit picker - for when the picker never
     * appeared at all.
     *
     * <h2>Why this was needed</h2>
     * Confirmed live: {@code isUiVersionPickerUp()} can be false from the very first check on
     * what Playwright considers a brand-new, cookie-less context - the picker simply never shows.
     * {@code answerUiVersionPicker}'s retry loop only re-clicks while the picker <i>is</i> up, so
     * when it is never up to begin with, nothing is ever clicked and the page is left on whatever
     * version the site defaults a seemingly-returning visitor to - which broke every case in
     * {@code LoginNewUiTest} in one real run (every failure landing in 2-4s, far too fast for the
     * picker-retry loop to have run at all, confirming it skipped straight past). The site
     * evidently recognises "has visited before" by something other than cookies/localStorage - an
     * IP or device fingerprint is the likely candidate, not confirmed further than that.
     *
     * <p>The toggle's own text names the <b>other</b> option, confirmed live - it reads "New UI"
     * while Classic Interface is active, "Classic UI" while New Interface is active - so wanting
     * New Interface means looking for a control that says "New UI", not "Classic UI".
     */
    private void ensureUiVersion(boolean wantNew) {
        if (wantNew ? isNewUiChosen() : isClassicUiChosen()) {
            return;
        }
        String toggleLabel = wantNew ? "New UI" : "Classic UI";
        Locator toggle = page.getByText(toggleLabel, new Page.GetByTextOptions().setExact(true))
                .filter(new Locator.FilterOptions().setVisible(true));
        if (toggle.count() == 0) {
            return;
        }
        toggle.first().click();
        page.waitForTimeout(2_000);
        awaitReady();
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
            // Not a bare page.waitForLoadState(): that defaults to `load`, which this site's own
            // slow third parties (chat widget, analytics) can miss well past any sane timeout -
            // see awaitReady()'s Javadoc. awaitReady() waits for it best-effort instead.
            awaitReady();
            page.waitForTimeout(3_000);
        }
    }

    // ---------------------------------------------------------------
    // The language menu
    // ---------------------------------------------------------------

    /**
     * Opens the header's language menu by clicking its toggle - found by what it looks like
     * <b>and</b> where it is, not by what it looks like alone.
     *
     * <h2>Why the first version of this broke every case in {@link LoginMultiLanguageTest}</h2>
     * It located the toggle as "any button on the page with an {@code <svg>} descendant and no
     * text of its own" - true of the toggle, confirmed live, but <b>also</b> true of other
     * icon-only controls this page carries further down (the hero carousel's prev/next arrows
     * among them). Unscoped, {@code .first()} did not reliably land on the toggle, nothing opened,
     * and every one of the twelve cases timed out waiting for a language row that was never going
     * to appear - all twelve read "Error" in the same local run, not just the ones for languages
     * that might plausibly fail to translate, which is what pointed at the opener itself rather
     * than at anything language-specific.
     *
     * <p>The fix scopes the same look (an {@code <svg>}, no own text) to <i>where</i> the toggle
     * actually is - the header band, confirmed live at y&lt;100px - which the carousel arrows and
     * everything else on the page are not. Done through a page script rather than a Locator
     * chain because Playwright's locator API has no bounding-box filter to express "in the header
     * band" directly; the same technique {@link #closeTopmostPanel} already uses in this class for
     * a control with no stable class, aria-label or alt text to go by either.
     */
    private void openLanguageMenu() {
        Object opened = page.evaluate("""
                () => {
                  const toggle = [...document.querySelectorAll('button')].find(b => {
                    if (!b.querySelector('svg') || (b.innerText || '').trim()) return false;
                    const r = b.getBoundingClientRect();
                    return b.offsetParent !== null && r.y >= 0 && r.y < 100;
                  });
                  if (!toggle) return false;
                  toggle.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true}));
                  return true;
                }""");
        if (!Boolean.TRUE.equals(opened)) {
            throw new PlaywrightException("The header's language toggle was not found - no "
                    + "icon-only button (an <svg>, no text of its own) sits in the header band "
                    + "(y<100px) on this page.");
        }
    }

    /**
     * Every language row currently listed, in whatever script each one prints its own name.
     *
     * <h2>Two classes, not one</h2>
     * Confirmed live (2026-10-23), on the same page, minutes apart, with nothing else changed:
     * the row's class was {@code HeaderTop_title__<hash>} in one load and
     * {@code HeaderTop_language-name__<hash>} in the next - not a selector-confidence guess that
     * turned out wrong once, but the site itself rendering this row under two different classes
     * across loads. Matching only the first is exactly what made every case in
     * {@code LoginMultiLanguageTest} this unreliable: whichever class that particular run's page
     * happened not to use, the row search timed out, and every step downstream of it (the login
     * form never opening, {@code fillUsername} waiting on a field that was never going to appear)
     * failed as a consequence - not because any of those later steps were themselves broken.
     * Matching both is the fix, not a third guess at "the real one".
     */
    private static final String LANGUAGE_ROW =
            "[class*='HeaderTop_title' i], [class*='HeaderTop_language-name' i]";

    /**
     * Opens the language menu and chooses a row by its English name (e.g. "Traditional Chinese"),
     * the half of each row's text that reads the same regardless of which script the rest of the
     * row is written in. Confirmed live (2026-10-23): choosing a row changes the page's own
     * locale segment ({@code /en/} became {@code /hk/} for Traditional Chinese) and re-translates
     * the page outright - header controls, nav labels and, critically, server-rendered validation
     * boards like the login form's wrong-credentials message, not just static UI chrome.
     *
     * <p>This project had twice concluded there was "no evidence" the site offered more than
     * English (see {@code HomePageNewUiTest}'s Javadoc and the README) - both times without ever
     * opening this menu. It offers twelve: see {@link Language}.
     *
     * <h2>The chat panel sits on top of this menu</h2>
     * Confirmed live (2026-10-23): the support chat panel, which opens itself a second or two
     * after any page load, renders over the same part of the header this menu drops down into -
     * wide enough to cover the row list entirely, and it can open itself again in the few seconds
     * between one dismissal and the next click, including between this method's own steps.
     * {@link #dismissIfPresent} runs before every attempt below rather than once up front, and a
     * row click that still does not land is retried - re-dismissing the chat panel and, if the
     * menu itself closed in the meantime (confirmed live: an outside interaction, the chat panel
     * opening among them, can do that too), reopening it - up to three attempts, forcing the
     * click on the last one. The same "force on the last try" shape the sibling Admin_kuake
     * project already uses for this identical kind of problem on this identical site.
     */
    public void selectLanguage(Language language) {
        Locator row = page.locator(LANGUAGE_ROW)
                .filter(new Locator.FilterOptions().setHasText(language.label))
                .filter(new Locator.FilterOptions().setVisible(true));

        PlaywrightException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            dismissIfPresent(CHAT_TOGGLE, "support chat panel");
            if (row.count() == 0) {
                openLanguageMenu();
                page.waitForTimeout(600);
                dismissIfPresent(CHAT_TOGGLE, "support chat panel");
            }
            try {
                row.first().click(new Locator.ClickOptions()
                        .setTimeout(attempt < 3 ? 4_000 : 8_000).setForce(attempt >= 3));
                page.waitForTimeout(2_000);
                awaitReady();
                return;
            } catch (PlaywrightException notClickable) {
                lastFailure = notClickable;
            }
        }
        throw lastFailure;
    }
}
