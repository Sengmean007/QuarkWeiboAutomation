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
 * <b>whether Quark Dynasty offers any live-casino game at all - under Classic UI, under New UI,
 * or under either - is not confirmed.</b> A failing case here may simply mean the feature does
 * not exist, which is itself the finding this class exists to surface; see
 * {@code tests-written-ahead-of-ui}. This is the other half of the split from
 * {@link WinWinCityNewUiTest} - read that class first for the full rationale, the mapping to
 * {@code NewWeibo_automation}'s original eight cases, and what was deliberately not ported
 * (WWC-05/HKDGAMELAUNCH-906 and WWC-06/HKDGAMELAUNCH-907).
 * =====================================================================================
 *
 * <h2>What every case does, and what differs from the New UI class</h2>
 * Same shared QA account, same home page, same speculative catalog/launch flow - the only
 * difference is the UI-version picker is answered with Classic Interface
 * ({@link org.sengmean.base.BasePage#chooseClassicUi()}) and the choice is confirmed with
 * {@link org.sengmean.base.BasePage#isClassicUiChosen()} instead. Whether Quark's Classic UI
 * even renders the same (hypothetical) catalog entry point as New UI, let alone the same game,
 * is exactly as unconfirmed as everything else here - see {@code ResponsiveLayoutClassicUiTest}
 * for the sibling precedent of a Classic UI case that may simply have no equivalent to assert.
 *
 * <h2>Case ids and what each maps to</h2>
 * <pre>
 * WINWIN-007  catalog entry point lists the placeholder game    <- WWC-01 / HKDGAMELAUNCH-901
 * WINWIN-008  selecting the game asks for confirmation          <- WWC-02 / HKDGAMELAUNCH-902
 * WINWIN-009  cancelling the confirmation launches nothing      <- WWC-03 / HKDGAMELAUNCH-903
 * WINWIN-010  a guest is told to log in, not offered the game   <- WWC-08 / HKDGAMELAUNCH-904
 * WINWIN-011  confirming opens the game in a tab, same context  <- WWC-04 / HKDGAMELAUNCH-905
 * WINWIN-012  a visible countdown ticks while bets are open     <- WWC-07 / HKDGAMELAUNCH-908
 * </pre>
 * Continuing immediately after {@link WinWinCityNewUiTest}'s {@code WINWIN-001}-{@code 006},
 * under the same invented {@code WINWIN-} prefix - this project has no client test-case
 * workbook, same as every other id in it.
 */
public class WinWinCityClassicUiTest extends BaseTest {

    private static final double ACTION_TIMEOUT_MS = 30_000;
    private static final double COUNTDOWN_WATCH_MS = 20_000;

    private static BrowserContext memberContext;
    private static Page memberPage;

    /** The game tab opened by WINWIN-011/WINWIN-012, tracked and closed deliberately - see
     *  WinWinCityNewUiTest's Javadoc note on why a launched game must not be left dangling. */
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

    /** Opens the home page, answers the picker with Classic Interface, and confirms it stuck. */
    private void openHomeOnClassicUi() {
        HomePage home = new HomePage(memberPage);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseClassicUi();
        home.dismissOverlays();
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to be the chosen UI "
                + "version - ui-version is \"" + home.uiVersion() + "\"");
    }

    @Test
    @DisplayName("WINWIN-007: the (speculative) catalog entry point lists the placeholder live-casino game, under Classic UI")
    void catalogListsTheGame() {
        openHomeOnClassicUi();
        boolean enteredCatalog = WinWinCityGamePage.openCatalogEntryPoint(memberPage);
        CaseLog.data("Catalog entry point found", String.valueOf(enteredCatalog));
        assertTrue(enteredCatalog, "No nav item named like '" + WinWinCityGamePage.CATALOG_ENTRY_LABEL
                + "' was found on the Classic UI home page - Quark may have no game catalog at "
                + "all under this UI version either; see this class's Javadoc.");
        assertTrue(WinWinCityGamePage.catalogListsGame(memberPage),
                "The catalog entry point did not list '" + WinWinCityGamePage.GAME_LABEL
                        + "' (a placeholder name - substitute the real one once a game is confirmed).");
    }

    @Test
    @DisplayName("WINWIN-008: selecting the game asks for confirmation before launching, under Classic UI")
    void selectingTheGameAsksForConfirmation() {
        openHomeOnClassicUi();
        assumeCatalogReachable();
        WinWinCityGamePage.selectGame(memberPage);
        assertTrue(WinWinCityGamePage.isStartDialogVisible(memberPage),
                "Selecting '" + WinWinCityGamePage.GAME_LABEL + "' did not raise a dialog containing '"
                        + WinWinCityGamePage.START_DIALOG_TEXT + "'. The board said: '"
                        + WinWinCityGamePage.popupText(memberPage) + "'.");
    }

    @Test
    @DisplayName("WINWIN-009: cancelling the confirmation launches nothing, under Classic UI")
    void cancellingLaunchesNothing() {
        openHomeOnClassicUi();
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
    @DisplayName("WINWIN-010: a guest is told to log in rather than being offered the game, under Classic UI")
    void aGuestIsToldToLogIn() {
        // Deliberately not the member session - this is the one case that wants a guest.
        Page guestPage = context().newPage();
        HomePage guestHome = new HomePage(guestPage);
        guestHome.navigateTo();
        guestHome.dismissOverlays();
        guestHome.chooseClassicUi();
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
    @DisplayName("WINWIN-011: confirming opens the game in a new tab of the same browser context, under Classic UI")
    void confirmingOpensTheGameInATab() {
        openHomeOnClassicUi();
        assumeCatalogReachable();
        WinWinCityGamePage.selectGame(memberPage);
        assumeTrue(WinWinCityGamePage.isStartDialogVisible(memberPage),
                "No start dialog appeared, so there is no Confirm to press.");

        gameTab = WinWinCityGamePage.launchGame(memberPage, ACTION_TIMEOUT_MS);
        assertNotNull(gameTab, "Confirm did not open a new tab within "
                + (long) (ACTION_TIMEOUT_MS / 1000) + "s.");
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
    @DisplayName("WINWIN-012: a visible countdown ticks while bets are open and is gone between rounds, under Classic UI")
    void aVisibleCountdownTicksWhileBettingIsOpen() {
        openHomeOnClassicUi();
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
                        + game.saveScreenshot("winwin-classicui-012-countdown"));
    }

    /** Skips rather than fails a case that cannot even reach the catalog. */
    private void assumeCatalogReachable() {
        boolean reached = WinWinCityGamePage.openCatalogEntryPoint(memberPage);
        assumeTrue(reached, "No nav item named like '" + WinWinCityGamePage.CATALOG_ENTRY_LABEL
                + "' was found - this case cannot proceed until a catalog entry point is confirmed "
                + "to exist on Quark; see WINWIN-007.");
    }
}
