package org.sengmean.tests.casino;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.*;
import org.sengmean.base.BaseTest;
import org.sengmean.page.casino.WinWinCityGamePage;
import org.sengmean.page.wallet.HomePage;
import org.sengmean.report.CaseLog;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * =====================================================================================
 * UNCONFIRMED FEATURE - this whole class is speculative, more so than any other suite in
 * this project. See {@link WinWinCityGamePage}'s class Javadoc for the full caveat; in short:
 * <b>whether Quark Dynasty offers any live-casino game at all - under the New UI, under
 * Classic UI, or under either - is not confirmed.</b> Nothing already in this project (login,
 * register, currency switch, responsive layout) touches a game catalog, and this class was
 * written without visiting the live site. A failing case here may simply mean the feature does
 * not exist, which is itself the finding this class exists to surface; see
 * {@code tests-written-ahead-of-ui}.
 * =====================================================================================
 *
 * <h2>Ported from</h2>
 * {@code NewWeibo_automation}'s {@code WinWinCityBettingTest} / {@code WinWinCityGamePage}
 * (a confirmed game, "WINWIN CITY", on the unrelated site test-v2.1083.city). Only the
 * <b>generic</b> half of that suite's eight cases is ported - the half that is really about
 * launching a game in general, not about WINWIN CITY's own mechanics. See "What was not ported"
 * below.
 *
 * <h2>What every case does</h2>
 * Signs the shared QA account in once for the class, opens the home page, answers the UI-version
 * picker with New Interface ({@link org.sengmean.base.BasePage#chooseNewUi()}) and confirms the
 * choice stuck ({@link org.sengmean.base.BasePage#isNewUiChosen()}), then attempts the
 * (speculative) game-launch flow via the static helpers on {@link WinWinCityGamePage}.
 *
 * <h2>Case ids and what each maps to</h2>
 * <pre>
 * WINWIN-001  catalog entry point lists the placeholder game    <- WWC-01 / HKDGAMELAUNCH-901
 * WINWIN-002  selecting the game asks for confirmation          <- WWC-02 / HKDGAMELAUNCH-902
 * WINWIN-003  cancelling the confirmation launches nothing      <- WWC-03 / HKDGAMELAUNCH-903
 * WINWIN-004  a guest is told to log in, not offered the game   <- WWC-08 / HKDGAMELAUNCH-904
 * WINWIN-005  confirming opens the game in a tab, same context  <- WWC-04 / HKDGAMELAUNCH-905
 * WINWIN-006  a visible countdown ticks while bets are open     <- WWC-07 / HKDGAMELAUNCH-908
 * </pre>
 * {@code WINWIN-001} through {@code WINWIN-006} are New UI; {@link WinWinCityClassicUiTest}
 * continues immediately at {@code WINWIN-007} with the same six cases under Classic UI. Both
 * prefixes are invented - this project has no client test-case workbook, same as every other id
 * in it (see {@code weibo-test-case-id-prefixes} / the README "A note on case ids" section).
 *
 * <h2>What was not ported, and why</h2>
 * <ul>
 *   <li><b>WWC-05 / HKDGAMELAUNCH-906</b> (three confirmed minimum bets debit their stakes) -
 *       this is WINWIN CITY's own betting mechanics end to end: a specific three-sides cycle, a
 *       specific chip tray, and a bet-landed detector built from pixel bands measured against
 *       that one game's own counters. There is no generic "a bet is accepted" case left once
 *       those specifics are stripped out. It is also the one case in the original suite that
 *       stakes real money - confirming a bet via blind coordinate clicks against a UI that is
 *       not even confirmed to exist would risk a real debit against whatever happens to be
 *       under those coordinates on the live site, which is a strictly worse failure mode than
 *       "the assertion was wrong". Skipped on both grounds.</li>
 *   <li><b>WWC-06 / HKDGAMELAUNCH-907</b> (a table opens from the Game Hall into a table view) -
 *       this is WINWIN CITY's own internal structure, a lobby-within-the-game listing multiple
 *       live tables. Not every live-casino game has such a sub-lobby (a single-table or
 *       arcade-style game would not), so there is no generic "game launching" lesson to carry
 *       over; it is specific to this one game's own navigation.</li>
 * </ul>
 */
public class WinWinCityNewUiTest extends BaseTest {

    /** How long a launch/open action is given before the case gives up. */
    private static final double ACTION_TIMEOUT_MS = 30_000;

    /** How long WINWIN-006 watches the placeholder countdown for. Short on purpose: a case this
     *  speculative should fail fast rather than spend minutes confirming nothing is there. */
    private static final double COUNTDOWN_WATCH_MS = 20_000;

    private static BrowserContext memberContext;
    private static Page memberPage;

    /** The game tab opened by WINWIN-005/WINWIN-006, tracked and closed deliberately so it does
     *  not outlive the case that opened it - mirrors NewWeibo_automation's WinWinCityBettingTest,
     *  which notes a game launch has to be tracked and closed rather than left dangling. */
    private Page gameTab;

    @BeforeAll
    static void loginOnce() {
        memberContext = newContext();
        memberPage = memberContext.newPage();
        signIn(memberPage);
    }

    @AfterAll
    static void closeMemberSession() {
        if (memberContext != null) {
            memberContext.close();
        }
    }

    @AfterEach
    void closeGameTab() {
        if (gameTab != null && !gameTab.isClosed()) {
            gameTab.close();
        }
        gameTab = null;
    }

    /** Opens the home page, answers the picker with New Interface, and confirms it stuck. */
    private void openHomeOnNewUi() {
        HomePage home = new HomePage(memberPage);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();
        assertTrue(home.isNewUiChosen(), "Expected New Interface to be the chosen UI version - "
                + "ui-version is \"" + home.uiVersion() + "\"");
    }

    @Test
    @DisplayName("WINWIN-001: the (speculative) catalog entry point lists the placeholder live-casino game")
    void catalogListsTheGame() {
        openHomeOnNewUi();
        boolean enteredCatalog = WinWinCityGamePage.openCatalogEntryPoint(memberPage);
        CaseLog.data("Catalog entry point found", String.valueOf(enteredCatalog));
        assertTrue(enteredCatalog, "No nav item named like '" + WinWinCityGamePage.CATALOG_ENTRY_LABEL
                + "' was found on the New UI home page - Quark may have no game catalog at all; "
                + "see this class's Javadoc.");
        assertTrue(WinWinCityGamePage.catalogListsGame(memberPage),
                "The catalog entry point did not list '" + WinWinCityGamePage.GAME_LABEL
                        + "' (a placeholder name - substitute the real one once a game is confirmed).");
    }

    @Test
    @DisplayName("WINWIN-002: selecting the game asks for confirmation before launching")
    void selectingTheGameAsksForConfirmation() {
        openHomeOnNewUi();
        assumeCatalogReachable();
        WinWinCityGamePage.selectGame(memberPage);
        assertTrue(WinWinCityGamePage.isStartDialogVisible(memberPage),
                "Selecting '" + WinWinCityGamePage.GAME_LABEL + "' did not raise a dialog containing '"
                        + WinWinCityGamePage.START_DIALOG_TEXT + "'. The board said: '"
                        + WinWinCityGamePage.popupText(memberPage) + "'.");
    }

    @Test
    @DisplayName("WINWIN-003: cancelling the confirmation launches nothing")
    void cancellingLaunchesNothing() {
        openHomeOnNewUi();
        assumeCatalogReachable();
        int before = memberPage.context().pages().size();
        WinWinCityGamePage.selectGame(memberPage);
        WinWinCityGamePage.cancelStartDialog(memberPage);
        memberPage.waitForTimeout(2_000);
        assertEquals(before, memberPage.context().pages().size(),
                "Cancel opened a page. It should dismiss the dialog and launch nothing.");
        assertFalse(WinWinCityGamePage.isStartDialogVisible(memberPage),
                "Cancel left the start dialog on screen.");
    }

    @Test
    @DisplayName("WINWIN-004: a guest is told to log in rather than being offered the game")
    void aGuestIsToldToLogIn() {
        // Deliberately not the member session - this is the one case that wants a guest.
        Page guestPage = context().newPage();
        HomePage guestHome = new HomePage(guestPage);
        guestHome.navigateTo();
        guestHome.dismissOverlays();
        guestHome.chooseNewUi();
        guestHome.dismissOverlays();

        boolean enteredCatalog = WinWinCityGamePage.openCatalogEntryPoint(guestPage);
        assumeTrue(enteredCatalog, "No catalog entry point was found for a guest either - "
                + "this case cannot proceed until one is confirmed to exist.");

        int before = guestPage.context().pages().size();
        WinWinCityGamePage.selectGame(guestPage);

        assertTrue(WinWinCityGamePage.isGuestRefusal(guestPage),
                "A guest should be refused with '" + WinWinCityGamePage.GUEST_REFUSAL
                        + "'. The board said: '" + WinWinCityGamePage.popupText(guestPage) + "'.");
        assertEquals(before, guestPage.context().pages().size(),
                "A guest selecting the game opened a page. Nothing should be launched.");
    }

    @Test
    @DisplayName("WINWIN-005: confirming opens the game in a new tab of the same browser context")
    void confirmingOpensTheGameInATab() {
        openHomeOnNewUi();
        assumeCatalogReachable();
        WinWinCityGamePage.selectGame(memberPage);
        assumeTrue(WinWinCityGamePage.isStartDialogVisible(memberPage),
                "No start dialog appeared, so there is no Confirm to press.");

        gameTab = WinWinCityGamePage.launchGame(memberPage, ACTION_TIMEOUT_MS);
        assertNotNull(gameTab, "Confirm did not open a new tab within "
                + (long) (ACTION_TIMEOUT_MS / 1000) + "s.");
        // The point of this case: a tab of the browser the run already owns, not a launcher
        // popup sized by the site into a separate window outside the context's own viewport.
        assertTrue(memberPage.context().pages().contains(gameTab),
                "The game did not open as a page of the member context - it should share the "
                        + "run's own browser context rather than opening elsewhere.");
        WinWinCityGamePage game = new WinWinCityGamePage(gameTab);
        boolean loaded = game.waitForLoad(ACTION_TIMEOUT_MS);
        CaseLog.data("Game canvas loaded", String.valueOf(loaded));
        assertTrue(loaded, "The opened tab never settled on a loaded canvas within "
                + (long) (ACTION_TIMEOUT_MS / 1000) + "s - either nothing real was launched, or "
                + "whatever Quark's equivalent game renders is not a canvas at all (see class "
                + "Javadoc on WinWinCityGamePage).");
    }

    @Test
    @DisplayName("WINWIN-006: a visible countdown ticks while bets are open and is gone between rounds")
    void aVisibleCountdownTicksWhileBettingIsOpen() {
        openHomeOnNewUi();
        assumeCatalogReachable();
        WinWinCityGamePage.selectGame(memberPage);
        assumeTrue(WinWinCityGamePage.isStartDialogVisible(memberPage),
                "No start dialog appeared, so there is nothing to confirm into a game.");

        gameTab = WinWinCityGamePage.launchGame(memberPage, ACTION_TIMEOUT_MS);
        assumeTrue(gameTab != null, "Confirm did not open a tab, so there is no game to watch.");
        WinWinCityGamePage game = new WinWinCityGamePage(gameTab);
        assumeTrue(game.waitForLoad(ACTION_TIMEOUT_MS),
                "The game tab never settled on a loaded canvas, so there is nothing to watch.");

        WinWinCityGamePage.CountdownObservation seen = game.watchCountdown(COUNTDOWN_WATCH_MS, 3);
        CaseLog.data("Countdown observation", String.valueOf(seen));
        assertTrue(seen.run() >= 3 || seen.ticks() > 0,
                () -> "No countdown-like change was ever observed at the placeholder position in "
                        + (long) (COUNTDOWN_WATCH_MS / 1000) + "s (" + seen + "). Either no betting "
                        + "window opened, the countdown is drawn somewhere else, or this game does "
                        + "not render a countdown the way WINWIN CITY does. Screenshot: "
                        + game.saveScreenshot("winwin-newui-006-countdown"));
    }

    /** Skips rather than fails a case that cannot even reach the catalog - there is no point
     *  failing every downstream case the same way once the first one has already said so. */
    private void assumeCatalogReachable() {
        boolean reached = WinWinCityGamePage.openCatalogEntryPoint(memberPage);
        assumeTrue(reached, "No nav item named like '" + WinWinCityGamePage.CATALOG_ENTRY_LABEL
                + "' was found - this case cannot proceed until a catalog entry point is confirmed "
                + "to exist on Quark; see WINWIN-001.");
    }
}
