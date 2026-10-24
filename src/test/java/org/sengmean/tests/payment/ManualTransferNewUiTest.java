package org.sengmean.tests.payment;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.*;
import org.sengmean.base.BaseTest;
import org.sengmean.base.ScreenshotOnFinish;
import org.sengmean.page.payment.TransferPage;
import org.sengmean.page.wallet.HomePage;
import org.sengmean.report.CaseLog;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Manual Transfer on Quark Dynasty's <b>New UI</b> - moving balance between the main wallet and
 * the game wallets.
 *
 * <h2>Confirmed live 2026-10-24 against test-v2.138hk.vip</h2>
 * The feature exists and matches the shape this class was ported ahead of confirmation to expect.
 * Driven manually as qa00002: every structural assertion below (defaults, list ordering, the
 * negative/decimal/zero-amount refusals, the over-balance disabled-Submit behaviour, a real $1
 * transfer in and back out, and a "Transfer All Out" sweep) was exercised against the live site
 * and either confirmed correct or fixed - see {@link TransferPage}'s own class Javadoc for the
 * full, case-by-case account of which locators and wordings were guesses that held up, and which
 * two were real bugs (the Submit button locator, and the "From"/"To" box locator on Classic UI).
 *
 * <h2>Where the cases come from</h2>
 * Ported from {@code NewWeibo_automation}'s {@code ManualTransferTest} (22 cases, for Weibo
 * Empire - a different client site with its own wallet model, its own game list and its own
 * promotion rules). Only the cases that test something generic about a main-wallet/game-wallet
 * transfer form were ported, with Weibo Empire's own game names and amount rules replaced by a
 * single placeholder wallet and {@link TransferPage#PLACEHOLDER_AMOUNT}. Every ported case below
 * carries a comment naming the Weibo Empire row it was generalized from.
 *
 * <p><b>Skipped entirely</b> - inseparable from Weibo Empire's own game list, admin panel or
 * promotion arithmetic, so there is nothing generic in them to port:
 * <ul>
 *   <li><b>HKDTRANSFER-0011</b>'s full form - walking all ~52 of Weibo Empire's own games with
 *       rate-limiter backoff/pacing. The <em>generic</em> half of this row (a valid transfer from
 *       the main wallet into a game wallet succeeds) is ported below as TRANSFER-010, against one
 *       placeholder wallet; the throttling machinery is a response to a rate limiter observed on
 *       a different site's backend and was not ported.</li>
 *   <li><b>HKDTRANSFER-0012</b> - a member put into Weibo Empire's "Suspense betting" admin state
 *       by that client's own admin panel, using a separately prepared account
 *       ({@code qa000091}). Nothing establishes Quark has an equivalent admin state, account or
 *       panel.</li>
 *   <li><b>HKDTRANSFER-0023</b> - the promotion notice shown for Weibo Empire's own named
 *       "block-promotion" games ({@code BLOCK_PROMOTION_GAMES}), an admin flag specific to that
 *       client and those games.</li>
 *   <li>All six {@code @Disabled} promotion-arithmetic rows (HKDTRANSFER-0024, -0026, -0029,
 *       -0034, -0041 and their "also covers" siblings) - these need Weibo Empire's own admin panel
 *       to grant/block promotion balances, set turnover targets and approve forced withdrawals.
 *       They were already manual-only on the source project; nothing here makes them any more
 *       reachable on a site whose transfer feature is not even confirmed to exist.</li>
 * </ul>
 *
 * <h2>Case ids</h2>
 * {@code TRANSFER-001} through {@code TRANSFER-013}, continuing this project's per-area
 * {@code UI-}/{@code LOGIN-}/{@code REGISTER-} convention with a new invented prefix (no client
 * workbook exists for this project to take one from):
 * <ul>
 *   <li>TRANSFER-001: "From" defaults to the main wallet and shows its balance (HKDTRANSFER-001)</li>
 *   <li>TRANSFER-002: the "From" list leads with the main wallet, then other wallets (HKDTRANSFER-002)</li>
 *   <li>TRANSFER-003: "To" defaults to "Please select" (HKDTRANSFER-005)</li>
 *   <li>TRANSFER-004: the "To" list offers the enabled wallets, paginated (HKDTRANSFER-006)</li>
 *   <li>TRANSFER-005: a negative amount cannot be typed (HKDTRANSFER-0017)</li>
 *   <li>TRANSFER-006: more than two decimal places cannot be entered (HKDTRANSFER-0018)</li>
 *   <li>TRANSFER-007: a zero amount is refused (HKDTRANSFER-0020)</li>
 *   <li>TRANSFER-008: an amount above the main wallet cannot be submitted (HKDTRANSFER-0021)</li>
 *   <li>TRANSFER-009: the same wallet cannot be both "From" and "To" (HKDTRANSFER-0015)</li>
 *   <li>TRANSFER-010: a nominal amount from the main wallet into a game wallet succeeds -
 *       {@code creates-data} (generalized from HKDTRANSFER-0011)</li>
 *   <li>TRANSFER-011: that same wallet transfers its balance back to the main wallet -
 *       {@code creates-data} (generalized from HKDTRANSFER-0013)</li>
 *   <li>TRANSFER-012: more than a game wallet's balance is refused (generalized from HKDTRANSFER-0014)</li>
 *   <li>TRANSFER-013: a "Transfer All Out" sweep, if the control exists, empties every game
 *       wallet back to the main wallet - {@code creates-data} (generalized from HKDTRANSFER-901).
 *       Runs <b>last</b> ({@code @Order(14)}), not in id order - see its own Javadoc.</li>
 *   <li>TRANSFER-014: a nominal amount from the main wallet into <i>every</i> offered game
 *       wallet is either accepted or refused, never stuck - {@code creates-data}, added
 *       2026-10-25 at Si's request; no Weibo Empire analogue (that project's own
 *       HKDTRANSFER-0011 walked every game too, but with rate-limiter pacing this project never
 *       ported - see {@link TransferPage}'s class Javadoc). Runs at {@code @Order(13)}, right
 *       before TRANSFER-013, which now sweeps what this case leaves funded.</li>
 * </ul>
 * Classic UI continues at {@code TRANSFER-015} in {@link ManualTransferClassicUiTest}.
 *
 * <h2>Why one shared, signed-in session for the whole class</h2>
 * TRANSFER-010 through TRANSFER-012, and now TRANSFER-014, move balance between wallets,
 * mirroring the ordering dependency the Weibo Empire original documents for its own
 * HKDTRANSFER-0011 / -0013 / -901 - TRANSFER-010 funds a wallet, TRANSFER-011 transfers it back
 * out, TRANSFER-012 needs a wallet with a readable balance to test an over-balance refusal on.
 * TRANSFER-013's sweep was originally ordered right after TRANSFER-012 for the same reason, but
 * confirmed live 2026-10-24 that TRANSFER-011 leaves every game wallet empty again before it
 * could run - so it needed to fund a wallet itself just to have something to sweep. Si asked for
 * it to run last instead ("arrange TRANSFER-013 to last in testing - that's meant waiting for all
 * game is fill with balance, then transfer all out"): moved to {@code @Order(14)}, after
 * TRANSFER-014 (now {@code @Order(13)}), which no longer sweeps its own wallets back out at the
 * end - see both cases' own Javadoc. Mirroring the Weibo Empire project's structure rather than
 * this project's own {@code ResponsiveLayoutNewUiTest} (whose rows are independent by design):
 * the shared QA account signs in once in {@code @BeforeAll}, New Interface is chosen and
 * confirmed to have stuck both before and after sign-in, and every case reuses that one session,
 * re-opening the transfer page in {@code @BeforeEach}. {@code @TestMethodOrder} keeps the
 * money-moving rows in the sequence they depend on.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ManualTransferNewUiTest extends BaseTest {

    private static BrowserContext memberContext;
    private static Page memberPage;
    private static TransferPage transfer;

    @BeforeAll
    static void signInAndChooseNewUi() {
        memberContext = newContext();
        memberPage = memberContext.newPage();

        HomePage home = new HomePage(memberPage);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();
        assertTrue(home.isNewUiChosen(), "Expected New Interface to be the chosen UI version "
                + "before running the Manual Transfer cases - ui-version is \"" + home.uiVersion()
                + "\"");

        signIn(memberPage);

        home.navigateTo();
        home.dismissOverlays();
        assertTrue(home.isNewUiChosen(), "Expected New Interface to still be the chosen UI "
                + "version after signing in - ui-version is \"" + home.uiVersion() + "\"");

        transfer = new TransferPage(memberPage);
    }

    @AfterAll
    static void closeMemberContext() {
        if (memberContext != null) {
            memberContext.close();
            memberContext = null;
        }
    }

    @BeforeEach
    void openTransferPage() {
        transfer.openTransfer();
    }

    // ================================================================= form structure

    @Test
    @Order(1)
    @DisplayName("TRANSFER-001: \"From\" defaults to the main wallet and shows its balance")
    void defaultFromIsTheMainWallet() {
        String from = transfer.fromSelection();

        assertTrue(from.toLowerCase().startsWith("main wallet"),
                "TRANSFER-001: \"From\" should default to the main wallet. It showed: " + from);
        assertFalse(transfer.mainBalance().isBlank(),
                "TRANSFER-001: \"From\" defaulted to the main wallet but carried no balance "
                        + "figure. It showed: " + from);
    }

    @Test
    @Order(2)
    @DisplayName("TRANSFER-002: the \"From\" list leads with the main wallet, then the other wallets")
    void fromListLeadsWithTheMainWallet() {
        List<String> offered = transfer.openFromOptions();

        assertFalse(offered.isEmpty(), "TRANSFER-002: the \"From\" list offered nothing at all");
        assertTrue(offered.get(0).toLowerCase().startsWith("main wallet"),
                "TRANSFER-002: the main wallet should be on top of the \"From\" list. "
                        + "The list read: " + offered);

        List<String> full = transfer.expandOptions();
        assertTrue(full.size() > 1,
                "TRANSFER-002: the \"From\" list offered the main wallet and no other wallets: "
                        + full);
    }

    @Test
    @Order(3)
    @DisplayName("TRANSFER-003: \"To\" defaults to \"Please select\"")
    void defaultToIsPleaseSelect() {
        String to = transfer.toSelection();

        assertTrue(to.startsWith(TransferPage.NOTHING_SELECTED),
                "TRANSFER-003: \"To\" should default to \"" + TransferPage.NOTHING_SELECTED
                        + "\". It showed: " + to);
    }

    @Test
    @Order(4)
    @DisplayName("TRANSFER-004: the \"To\" list offers the wallets admin has enabled")
    void toListOffersTheEnabledWallets() {
        List<String> firstPage = transfer.openToOptions();
        List<String> full = transfer.expandOptions();

        long wallets = full.stream()
                .filter(label -> !label.startsWith(TransferPage.NOTHING_SELECTED))
                .filter(label -> !label.toLowerCase().startsWith("main wallet"))
                .filter(label -> !label.startsWith("Show More"))
                .count();

        assertTrue(wallets > 0,
                "TRANSFER-004: the \"To\" list offered no game wallet at all. It read: " + full);

        // Whether a game is enabled is an admin setting a browser cannot read, so what is
        // checkable is only that "Show More" (if offered at all) genuinely reveals more entries.
        //
        // Fixed 2026-10-25: this used to assert the OPPOSITE - that "Show More" was still the
        // list's last entry after expandOptions() ran. That could only ever pass by accident:
        // expandOptions() clicks "Show More" in a loop specifically until it stops being offered,
        // so a fully expanded list should no longer carry one at all. Confirmed live the same day
        // against qa00002's own 47-wallet "To" list (needing several real "Show More" clicks to
        // reach in full) that the assertion as originally written could never pass once there was
        // real pagination to exhaust - it had just never been exercised against an account with
        // enough wallets to trigger it before. The real check: growth happened (more than the
        // first page, proving "Show More" existed and did something), and the fully-expanded list
        // no longer offers it (proving expansion actually ran to completion).
        if (full.size() > firstPage.size()) {
            assertFalse(full.get(full.size() - 1).startsWith("Show More"),
                    "TRANSFER-004: \"Show More\" was still the last entry after expanding every "
                            + "page - expansion should have run until it stopped being offered. "
                            + "The list read: " + full);
        }
    }

    // ================================================================= refusals

    @Test
    @Order(5)
    @DisplayName("TRANSFER-005: a negative amount cannot be typed")
    void negativeAmountCannotBeTyped() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);
        transfer.enterAmount("-10");

        String held = transfer.amountValue();
        assertFalse(held.startsWith("-"),
                "TRANSFER-005: the amount field accepted a negative number. It holds: '"
                        + held + "'");
    }

    @Test
    @Order(6)
    @DisplayName("TRANSFER-006: more than two decimal places cannot be entered")
    void moreThanTwoDecimalsCannotBeEntered() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);
        transfer.enterAmount("1.3949");

        String held = transfer.amountValue();
        int decimals = held.contains(".") ? held.length() - held.indexOf('.') - 1 : 0;
        assertTrue(decimals <= 2,
                "TRANSFER-006: the amount field kept " + decimals + " decimal places. "
                        + "It holds: '" + held + "'");
    }

    @Test
    @Order(7)
    @DisplayName("TRANSFER-007: a zero amount is refused")
    void zeroAmountIsRefused() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);
        transfer.enterAmount("0");

        // GUESS: "greater than 0" is the wording Weibo Empire's site uses; Quark's own wording,
        // if this control exists at all, is unconfirmed. Judged by submitting rather than only by
        // reading the button, as the source case explains: a form that simply disables its button
        // has not shown a message either way.
        if (transfer.isSubmitEnabled()) {
            transfer.submit();
        }
        boolean refused = transfer.waitForMessage("greater than 0", 10_000);

        assertTrue(refused,
                "TRANSFER-007: a zero transfer should be refused. The page showed: "
                        + transfer.observedMessages());
    }

    @Test
    @Order(8)
    @DisplayName("TRANSFER-008: an amount above the main wallet cannot be submitted")
    void amountAboveTheMainWalletIsRefused() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        double balance = transfer.mainBalanceValue();
        assumeTrue(balance >= 0, "The main wallet balance could not be read from the page.");

        transfer.chooseTo(wallet);

        transfer.enterAmount("1");
        boolean enabledForAnAffordableAmount = transfer.isSubmitEnabled();

        transfer.enterAmount(String.valueOf((long) balance + 1000));
        boolean enabledForAnUnaffordableAmount = transfer.isSubmitEnabled();

        assertTrue(enabledForAnAffordableAmount,
                "TRANSFER-008: Submit was disabled even for 1 against a balance of " + balance
                        + ", so this case cannot tell prevention from a form that never submits.");
        assertFalse(enabledForAnUnaffordableAmount,
                "TRANSFER-008: transferring more than the main wallet holds (" + balance
                        + ") should not be submittable, but Submit was enabled. The page showed: "
                        + transfer.observedMessages());
    }

    @Test
    @Order(9)
    @DisplayName("TRANSFER-009: the same wallet cannot be both \"From\" and \"To\"")
    void theSameWalletCannotBeBothEnds() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);

        List<String> fromOptions = transfer.openFromOptions();
        boolean stillOffered = fromOptions.stream().anyMatch(label -> label.startsWith(wallet));

        assertFalse(stillOffered,
                "TRANSFER-009: '" + wallet + "' is chosen in \"To\" but the \"From\" list "
                        + "still offers it, so the same wallet can be set at both ends. The "
                        + "\"From\" list read: " + fromOptions);
    }

    // ================================================================= transfers that move money

    /**
     * Generalized from HKDTRANSFER-0011, with Weibo Empire's walk over every one of its ~52 games
     * reduced to one placeholder wallet - see this class's Javadoc for why the throttling
     * machinery that walk needed was not ported at all.
     */
    @Test
    @Tag("creates-data")
    @Order(10)
    @DisplayName("TRANSFER-010: a nominal amount from the main wallet into a game wallet succeeds")
    void mainWalletToAGameWalletTransfers() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");
        assumeTrue(transfer.mainBalanceValue() >= 1,
                "The main wallet holds " + transfer.mainBalanceValue() + ", too little to transfer.");

        transfer.chooseTo(wallet);
        transfer.enterAmount(TransferPage.PLACEHOLDER_AMOUNT);
        assumeTrue(transfer.isSubmitEnabled(),
                "Submit never became enabled for a nominal amount into '" + wallet + "'.");

        transfer.submit();
        boolean accepted = transfer.waitForMessage("success", 15_000);
        if (!accepted) {
            // A "fail" refusal (the wallet happens to be under maintenance that day, see
            // TransferPage's class Javadoc) is an accepted outcome here too, the same as
            // mainWalletToEveryGameWalletTransfers() already treats it - only genuine silence,
            // neither a success nor an explicit refusal, is a defect worth failing on.
            assertTrue(transfer.observedMessages().toLowerCase().contains("fail"),
                    "TRANSFER-010: transferring " + TransferPage.PLACEHOLDER_AMOUNT + " into '"
                            + wallet + "' should report success or an explicit refusal, never "
                            + "silence. The page showed: " + transfer.observedMessages());
        }
    }

    /**
     * Generalized from HKDTRANSFER-0013. Depends on TRANSFER-010 having just funded a wallet;
     * falls back to looking for any wallet already holding enough, the same "take what is already
     * funded before spending more" preference the source case documents.
     */
    @Test
    @Tag("creates-data")
    @Order(11)
    @DisplayName("TRANSFER-011: a funded game wallet transfers its balance back to the main wallet")
    void aGameWalletTransfersBackToTheMainWallet() {
        String source = transfer.firstWalletHoldingAtLeast(1);
        assumeTrue(source != null,
                "No game wallet holds at least 1, so there is nothing to transfer back - this "
                        + "normally runs right after TRANSFER-010 funded one.");

        double inWallet = transfer.walletBalanceOf(source);
        double sendBack = Math.min(1, inWallet);

        transfer.chooseFrom(source);
        transfer.chooseTo("Main wallet");
        transfer.enterAmount(String.valueOf(sendBack));
        transfer.submit();

        boolean accepted = transfer.waitForMessage("success", 15_000);
        assertTrue(accepted,
                "TRANSFER-011: transferring " + sendBack + " out of '" + source + "', which held "
                        + inWallet + ", should report success. The page showed: "
                        + transfer.observedMessages());
    }

    /** Generalized from HKDTRANSFER-0014. */
    @Test
    @Order(12)
    @DisplayName("TRANSFER-012: transferring more than a game wallet's balance is refused")
    void moreThanTheWalletBalanceIsRefused() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer from.");

        double inWallet = transfer.walletBalanceOf(wallet);
        assumeTrue(inWallet >= 0,
                "'" + wallet + "' carries no readable balance figure in the \"To\" list.");
        String tooMuch = String.valueOf((long) inWallet + 1000);

        transfer.chooseFrom(wallet);
        transfer.chooseTo("Main wallet");
        transfer.enterAmount(tooMuch);

        if (transfer.isSubmitEnabled()) {
            transfer.submit();
            // GUESS: "fail" is the wording Weibo Empire's site uses ("Transfer point fail").
            // Not expected to run in practice - confirmed live 2026-10-24 that Submit disables
            // itself for an over-balance amount rather than submitting and having the server
            // refuse it - see TransferPage's class Javadoc; kept as a fallback in case a
            // differently-shaped game wallet behaves otherwise.
            boolean refused = transfer.waitForMessage("fail", 12_000);
            assertTrue(refused,
                    "TRANSFER-012: transferring " + tooMuch + " out of '" + wallet
                            + "', which holds " + inWallet + ", should be refused. The page "
                            + "showed: " + transfer.observedMessages());
        } else {
            // The form prevented it instead of letting the server refuse - the same shape as
            // TRANSFER-008 on the main wallet.
            assertFalse(transfer.isSubmitEnabled(),
                    "TRANSFER-012: Submit should stay disabled for " + tooMuch + " against a "
                            + "wallet balance of " + inWallet + ".");
        }
    }

    /**
     * Generalized from HKDTRANSFER-901. Confirmed live 2026-10-24: {@link TransferPage}'s
     * "Transfer All Out" control exists on this site, so this no longer aborts on
     * {@link TransferPage#hasTransferAllOut()} in practice - the assumption is kept as a safety
     * net in case a future account/build genuinely lacks the control.
     *
     * <h2>Ordered last at Si's request: wait for every game to hold balance, then sweep</h2>
     * Moved from {@code @Order(13)} to run after {@link #mainWalletToEveryGameWalletTransfers()}
     * rather than before it, at Si's direction ("arrange TRANSFER-013 to last in testing - that's
     * meant waiting for all game is fill with balance, then transfer all out"). That case no
     * longer sweeps its own wallets back out at the end - this one is now the single sweep for
     * everything the class funded, run once, last, rather than each money-moving case cleaning up
     * only after itself. The fallback below (funding one wallet first when none already holds a
     * balance) is kept as a safety net for when an earlier case's own assumptions failed and left
     * nothing funded, not the expected path any more.
     */
    @Test
    @Tag("creates-data")
    @Order(14)
    @DisplayName("TRANSFER-013: a \"Transfer All Out\" sweep, if present, empties every game "
            + "wallet back to the main wallet")
    void transferAllOutSweepsEveryWallet() {
        assumeTrue(transfer.hasTransferAllOut(),
                "No control resembling \"Transfer All Out\" was found on this page - whether "
                        + "Quark has a sweep control at all is unconfirmed; see TransferPage's "
                        + "class Javadoc.");

        if (transfer.firstWalletHoldingAtLeast(1) == null) {
            String wallet = transfer.firstGameOffered();
            assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                    + "nothing to fund before sweeping.");
            assumeTrue(transfer.mainBalanceValue() >= 1,
                    "The main wallet holds " + transfer.mainBalanceValue()
                            + ", too little to fund a wallet to sweep.");

            transfer.chooseTo(wallet);
            transfer.enterAmount(TransferPage.PLACEHOLDER_AMOUNT);
            assumeTrue(transfer.isSubmitEnabled(),
                    "Submit never became enabled for a nominal amount into '" + wallet + "'.");
            transfer.submit();
            assumeTrue(transfer.waitForMessage("success", 15_000),
                    "Funding '" + wallet + "' before the sweep did not report success - the "
                            + "page showed: " + transfer.observedMessages());
        }

        transfer.transferAllOut();
        boolean accepted = transfer.waitForMessage("success", 15_000);
        String stillFunded = null;
        if (!accepted) {
            // Confirmed live 2026-10-24: "Transfer All Out" completes with no popup at all more
            // often than not, unlike an individual Submit - a silent UI here is not by itself
            // proof the sweep did not happen. Before calling that a failure, check what the sweep
            // actually promises - every game wallet left empty - rather than only the popup.
            transfer.dismissAlert();
            stillFunded = transfer.firstWalletHoldingAtLeast(1);
            accepted = stillFunded == null;
        }

        assertTrue(accepted,
                "TRANSFER-013: \"Transfer All Out\" should report success, or at least empty "
                        + "every game wallet. The page showed: " + transfer.observedMessages()
                        + (stillFunded == null ? "" : "; '" + stillFunded
                                + "' still holds a balance after the sweep"));
    }

    /**
     * Added 2026-10-25 at Si's request: walks every game wallet {@link TransferPage#allOfferedGames()}
     * offers - not just the one placeholder wallet TRANSFER-010 exercises - transferring a nominal
     * $1 from the main wallet into each. 47 wallets were offered on qa00002's account that day.
     *
     * <h2>A "fail" refusal is an accepted outcome, not a defect</h2>
     * Confirmed live 2026-10-25: a game the live "Game Balance" table shows as "Maintenance"
     * (WORLD ENTERTAINMENT that day) refuses a nominal transfer with "The point transfer fail" -
     * Submit stays enabled and the server does the refusing, the one refusal shape in this class
     * that is not a client-side disable - see {@link TransferPage}'s class Javadoc. Only the
     * Game Balance table's own first page is confirmed to carry a maintenance flag at all, and
     * this walk goes well past that page, so this case cannot tell in advance which of the 47
     * wallets (if any) are down today. It instead accepts <i>either</i> "success" or a "fail"
     * refusal for a given wallet as a real answer, and only flags a wallet whose submit produced
     * neither - that is the actual anomaly worth reporting, not a maintenance game behaving
     * exactly as {@link TransferPage}'s own Javadoc says it does.
     *
     * <h2>A missing popup is checked against the wallet's own balance before being called stuck</h2>
     * Si watched a run where no popup appeared for a wallet at all and the walk moved straight on
     * to the next one - worth confirming before assuming that means the transfer never happened:
     * a popup on this site is not guaranteed proof of an effect, only a convenience. When neither
     * "success" nor "fail" is observed, this case now reads the wallet's own balance
     * ({@link TransferPage#walletBalanceOf(String)}) before deciding - if the nominal amount
     * actually landed, it counts as a success despite the silent UI, and only a wallet whose
     * balance <i>also</i> shows nothing happened is reported as the real anomaly.
     *
     * <h2>Self-funding; cleanup is deferred to TRANSFER-013, which now runs after this</h2>
     * Needs no leftover state from an earlier case: reads the main wallet's own balance before
     * spending from it. No longer sweeps its own wallets back out at the end - {@link
     * #transferAllOutSweepsEveryWallet()} was moved to {@code @Order(14)}, after this case, at
     * Si's request specifically so every wallet this case funds stays funded for that one final
     * sweep to empty, rather than each case cleaning up only after itself.
     */
    @Test
    @Tag("creates-data")
    @Order(13)
    @DisplayName("TRANSFER-014: a nominal amount from the main wallet into every offered game wallet is either accepted or refused, never stuck")
    void mainWalletToEveryGameWalletTransfers() {
        List<String> offered = transfer.allOfferedGames();
        assumeTrue(!offered.isEmpty(), "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        List<String> succeeded = new ArrayList<>();
        List<String> refused = new ArrayList<>();
        List<String> anomalies = new ArrayList<>();
        for (String label : offered) {
            String wallet = TransferPage.gameName(label);
            double balance = transfer.mainBalanceValue();
            if (balance < 1) {
                String why = wallet + ": the main wallet ran out of balance (" + balance
                        + ") partway through the walk";
                anomalies.add(why);
                checkAndCapture("FAIL - " + why);
                break;
            }

            transfer.chooseTo(wallet);
            transfer.enterAmount(TransferPage.PLACEHOLDER_AMOUNT);
            if (!transfer.isSubmitEnabled()) {
                String why = wallet + ": Submit never became enabled for a nominal amount";
                anomalies.add(why);
                checkAndCapture("FAIL - " + why);
                continue;
            }

            transfer.submit();
            // 30s rather than the usual 15s: Si asked for more headroom here specifically, since
            // this loop can be deep into a 47-wallet walk by the time any one submit is checked.
            boolean accepted = transfer.waitForMessage("success", 50_000);
            String verdict;
            if (accepted) {
                succeeded.add(wallet);
                verdict = "PASS - " + wallet + ": accepted";
            } else if (transfer.observedMessages().toLowerCase().contains("fail")) {
                refused.add(wallet + ": " + transfer.observedMessages());
                verdict = "PASS - " + wallet + ": refused (" + transfer.observedMessages() + ")";
            } else {
                // No popup was observed either way - Si watched a run where this happened and the
                // walk moved straight on to the next wallet with nothing to go on. Before calling
                // that an anomaly, check the wallet's own balance: if the nominal amount actually
                // landed, the transfer silently succeeded (no popup is not the same as no effect)
                // and is not a defect worth failing the case over.
                transfer.dismissAlert();
                double landed = transfer.walletBalanceOf(wallet);
                if (landed >= 1) {
                    succeeded.add(wallet);
                    verdict = "PASS - " + wallet + ": no popup appeared, but its balance ("
                            + landed + ") confirms the transfer landed anyway";
                } else {
                    String why = wallet + ": neither a success nor a 'fail' refusal was shown, "
                            + "and its balance (" + landed + ") doesn't show the transfer landed "
                            + "either - " + transfer.observedMessages();
                    anomalies.add(why);
                    verdict = "FAIL - " + why;
                }
            }
            // Captured before dismissAlert() clears the result, so the evidence shows the actual
            // popup (or lack of one) the verdict above was read from, not an already-cleared page.
            checkAndCapture(verdict);
            transfer.dismissAlert();
        }
        CaseLog.data("Succeeded", succeeded.size() + ": " + succeeded);
        CaseLog.data("Refused (e.g. a wallet under maintenance)", refused.size() + ": " + refused);

        assertTrue(anomalies.isEmpty(), "TRANSFER-014: " + anomalies);
        assertFalse(succeeded.isEmpty(), "TRANSFER-014: not one of the " + offered.size()
                + " offered wallets accepted the transfer. Refused: " + refused);
    }

    /**
     * Records one wallet's own verdict in the step trail and captures a screenshot of the page it
     * was read from - added at Si's request for a per-wallet breakdown with its own evidence,
     * inside this one case, rather than splitting the walk into 47 separate report rows (which
     * this project's report generator cannot do for a runtime-discovered list without hardcoding
     * it - see {@code quark-report-per-wallet-evidence}).
     *
     * <p>Always exactly one {@link CaseLog#check} paired with exactly one
     * {@link ScreenshotOnFinish#captureNow(Page, String)} per wallet, in that order, which is what
     * lets {@code HtmlReportGenerator#stepsPanel} show this case's own "walk over N games" layout
     * (one screenshot inlined beside the trail line it belongs to) instead of a pile of
     * near-identical thumbnails. A line beginning {@code "PASS - "} or {@code "FAIL - "} is what
     * that same generator highlights red on "FAIL" - success and an accepted "fail" refusal both
     * read {@code PASS}, since neither is the defect this case is actually checking for.
     */
    private void checkAndCapture(String verdict) {
        CaseLog.check(verdict);
        ScreenshotOnFinish.captureNow(memberPage, verdict);
    }
}
