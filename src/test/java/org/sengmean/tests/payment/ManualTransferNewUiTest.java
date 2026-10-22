package org.sengmean.tests.payment;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.*;
import org.sengmean.base.BaseTest;
import org.sengmean.page.payment.TransferPage;
import org.sengmean.page.wallet.HomePage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Manual Transfer on Quark Dynasty's <b>New UI</b> - moving balance between the main wallet and
 * the game wallets - <b>if this feature exists on this site at all.</b>
 *
 * <h2>Read this before trusting any row in this class</h2>
 * Nobody has opened a Manual Transfer screen on {@code test-v2.138hk.vip}. This project's own
 * README lists only login, register, currency-switch and responsive-layout as scaffolded so far,
 * and says nothing about a wallet-to-wallet or game-wallet transfer feature in either its
 * "Layout" or "Known defects" sections. Every id below is scaffolding the user explicitly asked
 * for ahead of that confirmation, not a report of something observed. {@link TransferPage}'s own
 * class Javadoc lists exactly which locators are guesses (nearly all of them) and what was
 * deliberately left unported from the Weibo Empire original. <b>Treat every case here as a
 * template to correct, or delete, once someone has actually looked at Quark's own transfer UI -
 * if one exists - rather than as a finding about the site.</b>
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
 *       wallet back to the main wallet - {@code creates-data} (generalized from HKDTRANSFER-901)</li>
 * </ul>
 * Classic UI continues at {@code TRANSFER-014} in {@link ManualTransferClassicUiTest}.
 *
 * <h2>Why one shared, signed-in session for the whole class</h2>
 * TRANSFER-010 through TRANSFER-013 move balance between wallets and depend on each other - the
 * sweep in TRANSFER-013 has nothing to sweep unless TRANSFER-010 funded a wallet first, exactly
 * the ordering dependency the Weibo Empire original documents for its own HKDTRANSFER-0011 /
 * -0013 / -901. So, mirroring that project's structure rather than this project's own
 * {@code ResponsiveLayoutNewUiTest} (whose rows are independent by design): the shared QA account
 * signs in once in {@code @BeforeAll}, New Interface is chosen and confirmed to have stuck both
 * before and after sign-in, and every case reuses that one session, re-opening the transfer page
 * in {@code @BeforeEach}. {@code @TestMethodOrder} keeps the money-moving rows in the sequence
 * they depend on.
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
        if (full.size() >= firstPage.size()) {
            assertTrue(full.get(full.size() - 1).startsWith("Show More"),
                    "TRANSFER-004: the last entry in the \"To\" list should be \"Show More\" "
                            + "when there are more wallets to reveal. The list read: " + full);
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
        transfer.confirmIfAsked(6_000);
        boolean accepted = transfer.waitForMessage("success", 15_000);

        assertTrue(accepted,
                "TRANSFER-010: transferring " + TransferPage.PLACEHOLDER_AMOUNT + " into '"
                        + wallet + "' should report success. The page showed: "
                        + transfer.observedMessages());
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
        transfer.confirmIfAsked(6_000);

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
            transfer.confirmIfAsked(6_000);
            // GUESS: "fail" is the wording Weibo Empire's site uses ("Transfer point fail").
            // Quark's own wording, if any, is unconfirmed.
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
     * Generalized from HKDTRANSFER-901. Aborts cleanly rather than failing when no sweep control
     * is found - unlike Weibo Empire, Quark is not even confirmed to have one; see
     * {@link TransferPage#hasTransferAllOut()}.
     */
    @Test
    @Tag("creates-data")
    @Order(13)
    @DisplayName("TRANSFER-013: a \"Transfer All Out\" sweep, if present, empties every game "
            + "wallet back to the main wallet")
    void transferAllOutSweepsEveryWallet() {
        assumeTrue(transfer.hasTransferAllOut(),
                "No control resembling \"Transfer All Out\" was found on this page - whether "
                        + "Quark has a sweep control at all is unconfirmed; see TransferPage's "
                        + "class Javadoc.");

        transfer.transferAllOut();
        transfer.confirmIfAsked(6_000);
        boolean accepted = transfer.waitForMessage("success", 15_000);

        assertTrue(accepted,
                "TRANSFER-013: \"Transfer All Out\" should report success. The page showed: "
                        + transfer.observedMessages());
    }
}
