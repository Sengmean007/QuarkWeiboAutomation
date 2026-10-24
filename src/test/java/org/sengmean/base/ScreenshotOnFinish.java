package org.sengmean.base;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sengmean.config.ConfigDataProvider;
import org.sengmean.report.CaseLog;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Captures the final state of every open page at the end of each case - <b>passed or failed</b> -
 * so the HTML report carries a picture of what the browser was showing when the case finished.
 * Ported from the sibling {@code NewWeibo_automation} project's class of the same name, confirmed
 * 2026-10-24 to never have existed in this project at all - every case's Evidence column read
 * "No screenshot captured", pass or fail, because nothing was ever capturing one.
 *
 * <h2>Passes are photographed too, and that is the point</h2>
 * This fires on every case, not only on failure. A reader reviewing a QA report wants to see what
 * a passing case actually looked like, not only take its word for it - a screenshot is the
 * difference between "the suite says the transfer succeeded" and seeing "Transfer Successful!"
 * on screen.
 *
 * <h2>Skipped cases are not photographed</h2>
 * A case skipped by a failed assumption ends with an exception in hand just as a failure does, so
 * the two have to be told apart explicitly. A skip never ran, so a picture beside it would be of
 * whatever the browser happened to be showing - misleading in exactly the place a reader is
 * trying to work out why a row is empty.
 *
 * <h2>Leaner than the NewWeibo original: no step-trail bracketing, no console verdict</h2>
 * The original class also opened/closed {@code CaseLog}'s trail and printed a one-line verdict
 * per case - both already exist here under different names ({@code CaseTrailExtension} for the
 * trail, {@code ConsoleResultListener} for the console line), ported separately and earlier. This
 * class keeps only what neither of those does: the screenshot itself.
 *
 * <h2>Registered after {@code CaseTrailExtension}, not before</h2>
 * JUnit runs "after" callbacks in <i>reverse</i> declaration order, so with
 * {@code @ExtendWith({CaseTrailExtension.class, ScreenshotOnFinish.class})} on {@link BaseTest},
 * this class's {@link #afterTestExecution} fires first - while {@code CaseLog}'s trail is still
 * open, so the "Captured the closing screenshot" line this writes actually lands in it - and
 * {@code CaseTrailExtension}'s own {@code CaseLog.finish()} runs last, after.
 *
 * <p>It walks the browser's contexts instead of reading one Page field because the suites do not
 * agree on where their page lives - some suites use {@code BaseTest}'s per-test page, some keep a
 * long-lived signed-in session page of their own in a static field. Every open page is worth
 * having.
 *
 * <h2>Exactly one image per case</h2>
 * The page captured is the newest one open when the case ends - see {@link #pageWorthCapturing()}.
 *
 * <p>Files land in {@code target/case-screenshots} as {@code <TopLevelSuite>.<method>.png} -
 * {@code org.sengmean.report.HtmlReportGenerator} already expects exactly this naming and already
 * matches them back to cases by it (confirmed: {@code HtmlReportGenerator}'s own screenshot-
 * copying code splits on the first {@code '.'}), so no change was needed on that side to make
 * this show up in the report.
 */
public class ScreenshotOnFinish implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final Path OUTPUT =
            Path.of(System.getProperty("report.shots", "target/case-screenshots"));

    /**
     * How long the closing capture waits for a loading indicator to clear before it shoots.
     * Override with {@code -Dtest.capture.settle=<milliseconds>}.
     */
    private static final long CAPTURE_SETTLE_MS =
            Long.parseLong(ConfigDataProvider.get("test.capture.settle", "10000"));

    /**
     * A short pause after the page has settled, so animated content - a popup fading in, a
     * balance counting up - has finished moving. Override with {@code -Dtest.capture.pause=}.
     */
    private static final long CAPTURE_PAUSE_MS =
            Long.parseLong(ConfigDataProvider.get("test.capture.pause", "1500"));

    /**
     * True while something that looks like a loading indicator is visible. This site's spinners
     * carry CSS-module class names, whose hash changes every build, so this matches by the
     * readable part of the name - "loading" or "spin" - rather than a full class.
     */
    private static final String LOADING_VISIBLE_JS =
            "() => [...document.querySelectorAll(\"[class*='loading' i], [class*='spin' i]\")]"
                    + ".some(e => { const r = e.getBoundingClientRect(); const s = getComputedStyle(e);"
                    + " return r.width > 0 && r.height > 0 && s.display !== 'none'"
                    + " && s.visibility !== 'hidden' && s.opacity !== '0'; })";

    /**
     * The file-name stem of the case now running, and how many mid-case captures it has taken -
     * see {@link #captureNow(Page)} for why a case needs this ahead of its own closing capture.
     * Held per thread because JUnit can run cases in parallel; a shared field would file one
     * case's picture under another's name.
     */
    private static final ThreadLocal<String> CURRENT_CASE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> STEP_COUNT = ThreadLocal.withInitial(() -> 0);

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        CURRENT_CASE.set(fileNameBase(context));
        STEP_COUNT.set(0);
    }

    /**
     * Photographs a page mid-case, in addition to the one taken when the case ends.
     *
     * <p>Use it for a state the reader needs to see and the final screenshot cannot show - a form
     * with an amount filled in, before Submit replaces it with a result. The report groups every
     * image whose name shares the case's stem, so these land in the same Evidence cell as the
     * closing capture.
     *
     * <p>Named {@code <case>-001.png}, {@code -002} and so on - zero-padded to three digits,
     * which is not cosmetic: the report copies screenshots in <i>lexicographic</i> filename order,
     * so an unpadded {@code -10} would sort before {@code -2} and a case with more than nine
     * captures would render its evidence out of sequence. This sorts <i>before</i> the closing
     * {@code <case>.png} too - a hyphen precedes a full stop - so the cell reads in the order the
     * case went through.
     *
     * <p>Never throws. A picture that could not be taken must not fail the case it documents.
     */
    public static void captureNow(Page page) {
        captureNow(page, null);
    }

    /** As {@link #captureNow(Page)}, labelling what the picture is of in the step trail. */
    public static void captureNow(Page page, String label) {
        String base = CURRENT_CASE.get();
        if (base == null || page == null || page.isClosed()) {
            return;
        }
        try {
            int step = STEP_COUNT.get() + 1;
            STEP_COUNT.set(step);
            Files.createDirectories(OUTPUT);
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(OUTPUT.resolve(String.format("%s-%03d.png", base, step)))
                    .setFullPage(false));
            CaseLog.evidence(label == null || label.isBlank()
                    ? "Captured screenshot " + step + " on " + page.url()
                    : "Screenshot " + step + ": " + label);
        } catch (Exception e) {
            System.out.println("Could not capture a step for " + base + ": " + e.getMessage());
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        // Skipped cases only. A present exception no longer means "failed", because this fires
        // for passes as well, so the abort check is the whole test rather than a guard on top of
        // one.
        if (context.getExecutionException()
                .filter(org.opentest4j.TestAbortedException.class::isInstance)
                .isPresent()) {
            CaseLog.step("Case aborted by a failed assumption; no closing screenshot is taken.");
            return;
        }
        if (BaseTest.browser == null || !BaseTest.browser.isConnected()) {
            return;
        }

        String base = fileNameBase(context);
        try {
            Page subject = pageWorthCapturing();
            if (subject == null) {
                return;
            }
            // The address the case finished on, recorded before the picture is taken so it
            // survives a screenshot that fails.
            CaseLog.step("Case ended on " + subject.url());
            settleBeforeCapture(subject);
            Files.createDirectories(OUTPUT);
            Path file = OUTPUT.resolve(base + ".png");
            subject.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(false));
            CaseLog.evidence("Captured the closing screenshot.");
        } catch (Exception e) {
            // A screenshot is evidence about a case, never a failure of its own: a page that
            // navigated away, closed itself or is mid-load must not turn a green case red or one
            // red case into two.
            System.out.println("Could not capture a screenshot for " + base + ": " + e.getMessage());
        }
    }

    /**
     * Gives the page time to draw the result before it is photographed: its {@code load} event,
     * then any loading indicator clearing, then {@link #CAPTURE_PAUSE_MS}. Every wait is bounded
     * and none can fail the case - a page that never settles is photographed as it is, with a
     * line in the trail saying so.
     */
    private static void settleBeforeCapture(Page page) {
        try {
            page.waitForLoadState(LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(CAPTURE_SETTLE_MS));
        } catch (RuntimeException stillLoading) {
            // Third-party beacons can hold `load` back indefinitely; the indicator check below
            // is the one that says whether the page itself is ready.
        }
        long deadline = System.currentTimeMillis() + CAPTURE_SETTLE_MS;
        try {
            while (Boolean.TRUE.equals(page.evaluate(LOADING_VISIBLE_JS))) {
                if (System.currentTimeMillis() >= deadline) {
                    CaseLog.step("A loading indicator was still showing after "
                            + (CAPTURE_SETTLE_MS / 1000) + "s; capturing the page as it is.");
                    break;
                }
                page.waitForTimeout(250);
            }
            page.waitForTimeout(CAPTURE_PAUSE_MS);
        } catch (RuntimeException e) {
            // A page navigating or closing under the check is photographed as it stands.
        }
    }

    /**
     * The one page worth photographing: the newest tab if the case opened one, otherwise the
     * page it worked on. Null when there is nothing worth a picture.
     *
     * <p>Pages are taken in creation order - {@code browser.contexts()} and {@code ctx.pages()}
     * both preserve it - so the last non-blank one is the most recently opened.
     */
    private static Page pageWorthCapturing() {
        Page newest = null;
        for (BrowserContext ctx : BaseTest.browser.contexts()) {
            for (Page page : ctx.pages()) {
                if (!page.isClosed() && !isBlank(page)) {
                    newest = page;
                }
            }
        }
        return newest;
    }

    /**
     * True for a page that has never been navigated, so nothing is learned by photographing it -
     * {@link BaseTest#page()} and {@link BaseTest#openOn(org.sengmean.support.Device)} both open
     * a fresh context lazily, which can leave a blank page sitting alongside a suite's own
     * long-lived signed-in page; skipping blanks means the useful page is the one photographed.
     */
    private static boolean isBlank(Page page) {
        try {
            String url = page.url();
            return url == null || url.isBlank() || "about:blank".equals(url);
        } catch (RuntimeException e) {
            // A page being torn down cannot be read, and cannot usefully be photographed either.
            return true;
        }
    }

    /** {@code org.sengmean.tests.payment.ManualTransferNewUiTest$Nested#foo} becomes
     *  {@code ManualTransferNewUiTest.foo} - the top-level class's simple name, matching the key
     *  {@code HtmlReportListener}/{@code CaseTrailExtension} already use for this same case. */
    private static String fileNameBase(ExtensionContext context) {
        Class<?> topLevel = context.getTestClass().orElse(null);
        while (topLevel != null && topLevel.getEnclosingClass() != null) {
            topLevel = topLevel.getEnclosingClass();
        }
        String simple = topLevel == null ? "UnknownClass" : topLevel.getSimpleName();
        String method = context.getTestMethod()
                .map(java.lang.reflect.Method::getName)
                .orElse("unknownTest");
        return (simple + "." + method).replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
