package org.sengmean.page.casino;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.sengmean.base.BasePage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * =====================================================================================
 * UNCONFIRMED FEATURE - READ THIS BEFORE TRUSTING ANYTHING BELOW
 * =====================================================================================
 * <p><b>Whether Quark Dynasty (https://test-v2.138hk.vip) offers any live-casino game at all -
 * let alone one resembling Weibo Empire's "WINWIN CITY" - has not been confirmed.</b> Nothing
 * in this project's README, its "Known defects" section, or any existing page object
 * (login, register, currency switch, responsive layout) mentions a game catalog or a
 * live-casino product of any kind. This class was written by porting the <i>shape</i> of
 * {@code NewWeibo_automation}'s {@code WinWinCityGamePage} and {@code WinWinCityBettingTest}
 * (which cover a confirmed game on a different site, test-v2.1083.city) without ever visiting
 * Quark's live site to check. Of every area ported into this project today, this is the least
 * confirmed by a wide margin: the other areas at least port a UI pattern (a picker, a layout
 * breakpoint) that plausibly exists everywhere; this one ports an entire product category that
 * may simply not exist here.
 *
 * <p>Every identifier, label and coordinate below that looks specific - {@link #GAME_LABEL},
 * {@link #CATALOG_ENTRY_LABEL}, {@link #START_DIALOG_TEXT}, the {@link BetSide} fractions, the
 * chip values, the Confirm/Cancel coordinates - is a <b>placeholder copied from Weibo Empire's
 * own measured WINWIN CITY layout</b>, kept only so the shape of a canvas-driven live-casino
 * game is on record. None of it has been measured, or even observed to exist, on Quark. If
 * Quark turns out to offer no live-casino product at all, every test built on this class is
 * expected to fail, and that failure is the answer to the open question, not a defect in the
 * port. If Quark does offer one, every placeholder constant here needs remeasuring against it
 * before any of this is trustworthy - see {@code weibo-characterise-current-behaviour} /
 * {@code tests-written-ahead-of-ui} for why writing it this way ahead of a confirmed UI is the
 * house style rather than a shortcut.
 *
 * <h2>Swap point</h2>
 * {@link #GAME_LABEL} and {@link #CATALOG_ENTRY_LABEL} are the two constants to change once
 * Quark's own equivalent game (if any) is confirmed to exist - everything else keys off them.
 * They deliberately do not say "WINWIN CITY" anywhere, to avoid baking in a game name that may
 * turn out to belong to a different site entirely.
 *
 * <h2>Why the launch/catalog helpers and the in-game canvas object live in one file</h2>
 * {@code NewWeibo_automation} splits this concern across three classes - {@code GameLobbyPage}
 * (the shared tile/confirm-dialog/launch machinery), {@code LivesCasinoPage} (the category page
 * that lists WINWIN CITY) and {@code WinWinCityGamePage} (the canvas once the game has opened).
 * This port is restricted to a single new file in {@code org.sengmean.page.casino}, so the
 * static helpers in the first half of this class stand in for the first two; the instance half
 * below them is the direct port of the real {@code WinWinCityGamePage}.
 *
 * <h2>The in-game canvas, once reached</h2>
 * Weibo Empire's game is one {@code <canvas>} element with no DOM to query - every interaction
 * is a click at a point and every read is a pixel, because the balance, the staked chip and the
 * countdown are painted, not rendered as text. That is <i>also</i> unconfirmed for whatever
 * Quark may or may not offer: a different vendor could easily render its game as ordinary DOM,
 * in which case none of the canvas machinery below would even apply. It is kept here anyway, as
 * the most literal "shape, not literals" port available, and flagged throughout.
 *
 * <h2>Safety</h2>
 * {@link #confirmBet()} stakes the selected chip against the member's real balance the instant
 * it is clicked, exactly as it does on Weibo Empire. No test case in this port's two suites
 * calls it - see {@code WinWinCityNewUiTest}'s Javadoc for why the stake-debiting case was judged
 * too specific, and too risky given blind pixel clicks against an unconfirmed UI, to port. It
 * remains here only so a future author who confirms a real game exists has the shape to build
 * from; it must stay behind a "creates-data" tag if it is ever called from a test.
 */
public class WinWinCityGamePage extends BasePage {

    // =================================================================================
    // Catalog / launch flow - speculative, static. Operates on the member's own page
    // (the home page / wherever a catalog entry point is hoped to be), not the game tab.
    // =================================================================================

    /**
     * The placeholder name this port gives the game. <b>Not a real product name</b> - Weibo
     * Empire's own tile reads "WINWIN CITY"; that label is deliberately not reused here so this
     * class does not read as evidence that Quark has a WINWIN CITY tile. Replace with the real
     * accessible name once a live-casino product is confirmed to exist on Quark.
     */
    public static final String GAME_LABEL = "Live Casino Game A";

    /**
     * A guessed accessible name for whatever nav item might lead to a game catalog, modelled on
     * Weibo Empire's "Lives Casino" category entry at {@code /en/3/}. Unconfirmed: Quark's own
     * navigation has not been inspected for anything resembling a game catalog at all.
     */
    public static final String CATALOG_ENTRY_LABEL = "Live Casino";

    /**
     * The wording Weibo Empire's start-game dialog shows, ported verbatim as a guess. Quark may
     * phrase this differently, skip a confirmation step entirely, or have no such dialog because
     * it has no such catalog.
     */
    public static final String START_DIALOG_TEXT = "Confirm to start the game";

    /** Ported verbatim from Weibo Empire's guest refusal; Quark's own wording is unconfirmed. */
    public static final String GUEST_REFUSAL = "Please login first";

    /** How long a tile click or a dialog action is given before the case gives up on it. */
    private static final double CATALOG_ACTION_TIMEOUT_MS = 10_000;

    /**
     * Best-effort click on whatever on the current page might be a catalog entry point, matched
     * case-insensitively against {@link #CATALOG_ENTRY_LABEL} as a link or a button - Weibo
     * Empire's category pages are reached by a left-nav link, but whether Quark's equivalent (if
     * one exists) is a link, a button or something else entirely is unconfirmed.
     *
     * @return true if something matching was found and clicked; false if nothing did, which on
     *         an unconfirmed feature is the expected outcome until proven otherwise
     */
    public static boolean openCatalogEntryPoint(Page page) {
        Pattern name = Pattern.compile(Pattern.quote(CATALOG_ENTRY_LABEL), Pattern.CASE_INSENSITIVE);
        Locator entry = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(name))
                .or(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name)));
        if (entry.count() == 0) {
            return false;
        }
        try {
            entry.first().click(new Locator.ClickOptions().setTimeout(CATALOG_ACTION_TIMEOUT_MS));
        } catch (com.microsoft.playwright.PlaywrightException notClickable) {
            return false;
        }
        page.waitForTimeout(1_500);
        return true;
    }

    /** Case-insensitive text search for {@link #GAME_LABEL} on the current page. */
    public static boolean catalogListsGame(Page page) {
        String body = page.locator("body").textContent();
        return body != null && body.toLowerCase().contains(GAME_LABEL.toLowerCase());
    }

    /**
     * Clicks the tile for {@link #GAME_LABEL}, matched by accessible name as a button - the
     * pattern Weibo Empire's own tiles use. If Quark renders this as something other than a
     * button (a card, a link, an image), this finds nothing and the case fails saying so, which
     * is the correct outcome for an unconfirmed locator.
     */
    public static void selectGame(Page page) {
        Pattern name = Pattern.compile(Pattern.quote(GAME_LABEL), Pattern.CASE_INSENSITIVE);
        Locator tile = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name));
        tile.first().click(new Locator.ClickOptions().setTimeout(CATALOG_ACTION_TIMEOUT_MS));
        page.waitForTimeout(1_500);
    }

    /** Whatever dialog/board text is visible on the page right now, or "" if none is. */
    public static String popupText(Page page) {
        try {
            Locator dialog = page.getByRole(AriaRole.DIALOG);
            if (dialog.count() > 0 && dialog.first().isVisible()) {
                return dialog.first().innerText();
            }
        } catch (com.microsoft.playwright.PlaywrightException ignored) {
            // Falls through to "".
        }
        return "";
    }

    public static boolean isStartDialogVisible(Page page) {
        return popupText(page).contains(START_DIALOG_TEXT);
    }

    public static boolean isGuestRefusal(Page page) {
        return popupText(page).contains(GUEST_REFUSAL);
    }

    /** Clicks a button named "Cancel" inside whatever dialog is up. */
    public static void cancelStartDialog(Page page) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel")).first()
                .click(new Locator.ClickOptions().setTimeout(CATALOG_ACTION_TIMEOUT_MS));
        page.waitForTimeout(1_000);
    }

    /**
     * Clicks a button named "Confirm" and waits for the resulting page to open <b>as a tab of
     * the same browser context</b>, which is the generic, portable half of Weibo Empire's
     * launch case (HKDGAMELAUNCH-905/WWC-04): launching a game should not navigate the lobby
     * itself away, and should not need a separate browser window.
     *
     * <h2>Deliberately simpler than NewWeibo's {@code GameLobbyPage.confirmStartGame}</h2>
     * The real site needed to intercept {@code window.open} because its launcher assigns the
     * real game URL to the opened window only after a {@code /loading/} stub, and because
     * Chromium opens a window-featured popup as a separate OS window rather than a context tab
     * unless something intervenes. Neither quirk is confirmed to apply here - there is no
     * confirmed launcher at all - so this uses Playwright's ordinary {@code waitForPopup}, which
     * already returns a {@link Page} that belongs to the same {@link com.microsoft.playwright.BrowserContext}.
     * If Quark's real launcher (once one exists) turns out to share Weibo Empire's stub/redirect
     * quirks, port the fuller interception from {@code GameLobbyPage} at that point.
     *
     * @return the new tab, loaded to whatever it navigated to
     */
    public static Page launchGame(Page page, double timeoutMs) {
        return page.waitForPopup(new Page.WaitForPopupOptions().setTimeout(timeoutMs), () ->
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirm")).first()
                        .click(new Locator.ClickOptions().setTimeout(CATALOG_ACTION_TIMEOUT_MS)));
    }

    // =================================================================================
    // The in-game canvas, once a game tab has (speculatively) been reached.
    // Everything below is a literal port of NewWeibo_automation's WinWinCityGamePage,
    // with every measured constant replaced by an explicitly unmeasured placeholder.
    // =================================================================================

    /**
     * The aspect ratio the game is assumed to draw at. Weibo Empire's WINWIN CITY measured
     * 1285x804 (1.6); this is an arbitrary 16:9 placeholder, not a measurement of anything.
     */
    public static final double CONTENT_ASPECT = 16.0 / 9.0;

    /**
     * The outcomes a round might be bet on, modelled on WINWIN CITY's Player Win / Tie / Banker
     * Win. <b>Every fraction below is an unmeasured placeholder</b> - Weibo Empire's own values
     * (0.2755/0.4981/0.7222) are not reused, precisely so a case built on this enum cannot be
     * mistaken for one that was actually measured against anything.
     */
    public enum BetSide {
        PLAYER_WIN(0.30, 0.60),
        TIE(0.50, 0.60),
        BANKER_WIN(0.70, 0.60);

        final double fx;
        final double fy;

        BetSide(double fx, double fy) {
            this.fx = fx;
            this.fy = fy;
        }
    }

    /** The chip tray's denominations - copied from Weibo Empire's as a structural placeholder. */
    private static final int[] CHIP_VALUES = {10, 20, 30, 40, 50, 60};
    private static final double CHIP_FIRST_X = 0.57, CHIP_STEP_X = 0.075, CHIP_Y = 0.91;

    // Betting controls, as fractions of the content box - unmeasured placeholders.
    private static final double CONFIRM_X = 0.57, CONFIRM_Y = 0.78;
    private static final double CANCEL_X = 0.89, CANCEL_Y = 0.78;
    private static final double EXIT_X = 0.04, EXIT_Y = 0.03;

    /** Two whole-canvas readings within this much of each other mean the game has settled. */
    private static final double SETTLED_TOLERANCE = 1.5;

    /** How much the whole-canvas reading must move to count as one view replacing another. */
    private static final double VIEW_CHANGE_THRESHOLD = 6.0;

    // The betting countdown hexagon - position and tick/still bands are unmeasured placeholders.
    private static final double COUNTDOWN_X = 0.69, COUNTDOWN_Y = 0.12, COUNTDOWN_HALF = 0.02;
    private static final double COUNTDOWN_TICK_MIN = 5, COUNTDOWN_TICK_MAX = 50, COUNTDOWN_STILL_MAX = 1;

    private final Locator canvas;

    public WinWinCityGamePage(Page page) {
        super(page);
        this.canvas = page.locator("canvas").first();
    }

    /** True once a canvas has been attached and given a size. Says nothing about what it draws -
     *  if Quark's game (should one exist) is ordinary DOM rather than a canvas, this is false and
     *  every method below this point simply does not apply; see class Javadoc. */
    public boolean isLoaded() {
        try {
            canvas.waitFor();
            Box box = contentBox();
            return box.width() > 0 && box.height() > 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Blocks until the canvas is up and has stopped repainting, or gives up. Ported from Weibo
     *  Empire's same-named method; see that class for why settling alone is not enough there -
     *  unconfirmed whether a splash-then-hall transition exists here at all. */
    public boolean waitForLoad(double timeoutMs) {
        long deadline = System.currentTimeMillis() + (long) timeoutMs;
        while (System.currentTimeMillis() < deadline && !isLoaded()) {
            page.waitForTimeout(1_000);
        }
        if (!isLoaded()) {
            return false;
        }
        return waitUntilSettled(deadline - System.currentTimeMillis());
    }

    /** Blocks until the artwork changes substantially, i.e. one view replaces another. */
    public boolean waitForRepaint(double timeoutMs) {
        long deadline = System.currentTimeMillis() + (long) timeoutMs;
        double previous = canvasLuminance();
        while (System.currentTimeMillis() < deadline) {
            page.waitForTimeout(1_500);
            double current = canvasLuminance();
            if (Math.abs(current - previous) >= VIEW_CHANGE_THRESHOLD) {
                return true;
            }
            previous = current;
        }
        return false;
    }

    /** Blocks until the artwork stops changing, i.e. two consecutive readings agree. */
    public boolean waitUntilSettled(double timeoutMs) {
        long deadline = System.currentTimeMillis() + (long) timeoutMs;
        double previous = -1;
        while (System.currentTimeMillis() < deadline) {
            double current = canvasLuminance();
            if (previous >= 0 && Math.abs(current - previous) <= SETTLED_TOLERANCE) {
                return true;
            }
            previous = current;
            page.waitForTimeout(1_500);
        }
        return false;
    }

    /** The region the game is assumed to draw in, fitting {@link #CONTENT_ASPECT} inside the
     *  canvas box. Returns a zero box if there is no canvas, rather than throwing. */
    public Box contentBox() {
        var box = canvas.boundingBox();
        if (box == null) {
            return new Box(0, 0, 0, 0);
        }
        double width = box.width;
        double height = box.height;
        if (width / height > CONTENT_ASPECT) {
            double drawn = height * CONTENT_ASPECT;
            return new Box(box.x + (width - drawn) / 2, box.y, drawn, height);
        }
        double drawn = width / CONTENT_ASPECT;
        return new Box(box.x, box.y + (height - drawn) / 2, width, drawn);
    }

    /** Clicks a point given as a fraction of the drawn content. */
    public void clickContent(double fx, double fy) {
        Box box = contentBox();
        page.mouse().click(box.x() + box.width() * fx, box.y() + box.height() * fy);
    }

    /** Selects a chip denomination. Placeholder tray positions - see {@link #CHIP_VALUES}. */
    public void selectChip(int value) {
        for (int i = 0; i < CHIP_VALUES.length; i++) {
            if (CHIP_VALUES[i] == value) {
                clickContent(CHIP_FIRST_X + CHIP_STEP_X * i, CHIP_Y);
                page.waitForTimeout(300);
                return;
            }
        }
        throw new IllegalArgumentException("The placeholder chip tray offers " + java.util.Arrays
                .toString(CHIP_VALUES) + ", not " + value);
    }

    /** Stages the selected chip on one side. Nothing is committed until {@link #confirmBet()}. */
    public void placeOn(BetSide side) {
        clickContent(side.fx, side.fy);
        page.waitForTimeout(300);
    }

    /**
     * Commits the staged bet. <b>This debits the member balance immediately, against
     * coordinates that have never been measured on anything Quark actually shows.</b> Not called
     * by any test case in this port - see this class's Javadoc. Reserved for a future author who
     * has confirmed a real game exists and remeasured every placeholder above; must stay behind
     * a "creates-data" tag if it is ever called from a test.
     */
    public void confirmBet() {
        clickContent(CONFIRM_X, CONFIRM_Y);
        page.waitForTimeout(2_000);
    }

    /** Clears a staged bet without committing it. */
    public void cancelBet() {
        clickContent(CANCEL_X, CANCEL_Y);
        page.waitForTimeout(500);
    }

    /** Leaves the current view, on the (unmeasured) assumption of an exit control in this spot. */
    public void exitTable() {
        clickContent(EXIT_X, EXIT_Y);
        page.waitForTimeout(3_000);
    }

    /** Mean luminance of the whole drawn area - used to tell a settled view from a changing one. */
    public double canvasLuminance() {
        Box box = contentBox();
        return meanLuminance(box.x(), box.y(), box.width(), box.height());
    }

    private double meanLuminance(double x, double y, double width, double height) {
        byte[] png = page.screenshot(new Page.ScreenshotOptions().setClip(x, y, width, height));
        BufferedImage image = decode(png, "The sampled screenshot could not be decoded");
        long total = 0, pixels = 0;
        for (int row = 0; row < image.getHeight(); row += 4) {
            for (int column = 0; column < image.getWidth(); column += 4) {
                int rgb = image.getRGB(column, row);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                total += (long) (0.299 * r + 0.587 * g + 0.114 * b);
                pixels++;
            }
        }
        return pixels == 0 ? 0 : (double) total / pixels;
    }

    /**
     * What a betting countdown did while it was watched - ported from Weibo Empire's hexagon
     * timer for the generic, speculative case that <i>some</i> round-based live-casino game
     * shows a visible countdown while betting is open and nothing while it is not. The position
     * and the tick/still bands are placeholders; see class Javadoc.
     */
    public record CountdownObservation(int samples, int ticks, int stills, int run) {
    }

    /** Watches the placeholder countdown area until it has both been seen counting and been
     *  seen still, or the budget runs out. */
    public CountdownObservation watchCountdown(double timeoutMs, int ticksWanted) {
        long deadline = System.currentTimeMillis() + (long) timeoutMs;
        BufferedImage previous = null;
        int samples = 0, ticks = 0, stills = 0, run = 0, longest = 0;
        while (System.currentTimeMillis() < deadline && (longest < ticksWanted || stills == 0)) {
            BufferedImage current = capture(COUNTDOWN_X - COUNTDOWN_HALF, COUNTDOWN_Y - COUNTDOWN_HALF,
                    COUNTDOWN_HALF * 2, COUNTDOWN_HALF * 2);
            samples++;
            if (previous != null) {
                double moved = meanDifference(previous, current);
                if (moved >= COUNTDOWN_TICK_MIN && moved <= COUNTDOWN_TICK_MAX) {
                    ticks++;
                    run++;
                    longest = Math.max(longest, run);
                } else {
                    run = 0;
                    if (moved <= COUNTDOWN_STILL_MAX) {
                        stills++;
                    }
                }
            }
            previous = current;
            page.waitForTimeout(1_000);
        }
        return new CountdownObservation(samples, ticks, stills, longest);
    }

    private BufferedImage capture(double fx, double fy, double fw, double fh) {
        Box box = contentBox();
        byte[] png = page.screenshot(new Page.ScreenshotOptions().setClip(
                box.x() + box.width() * fx, box.y() + box.height() * fy,
                box.width() * fw, box.height() * fh));
        return decode(png, "A capture of the table could not be decoded");
    }

    private static BufferedImage decode(byte[] png, String failureMessage) {
        try {
            return ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            throw new UncheckedIOException(failureMessage, e);
        }
    }

    /** Mean per-pixel colour difference, 0 to 255, of two captures of the same area. */
    private static double meanDifference(BufferedImage a, BufferedImage b) {
        long total = 0, pixels = 0;
        for (int y = 0; y < Math.min(a.getHeight(), b.getHeight()); y++) {
            for (int x = 0; x < Math.min(a.getWidth(), b.getWidth()); x++) {
                int p = a.getRGB(x, y);
                int q = b.getRGB(x, y);
                total += Math.abs(((p >> 16) & 0xFF) - ((q >> 16) & 0xFF))
                        + Math.abs(((p >> 8) & 0xFF) - ((q >> 8) & 0xFF))
                        + Math.abs((p & 0xFF) - (q & 0xFF));
                pixels++;
            }
        }
        return pixels == 0 ? 0 : (double) total / pixels / 3;
    }

    /**
     * Saves a screenshot of the game tab under target/game-screenshots/, returning the path.
     * Worth having even for a speculative page object: a failure here is a failure about pixels,
     * and the picture is the only thing that can show what was actually on screen instead of
     * whatever placeholder coordinate was clicked.
     */
    public String saveScreenshot(String name) {
        Path file = Path.of("target", "game-screenshots", name + ".png");
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create the screenshot directory", e);
        }
        page.screenshot(new Page.ScreenshotOptions().setPath(file));
        return file.toAbsolutePath().toString();
    }

    /** A rectangle in page coordinates. */
    public record Box(double x, double y, double width, double height) {
    }
}
