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
 * Manual Transfer on Quark Dynasty's <b>Classic UI</b> - the other half of the split from
 * {@link ManualTransferNewUiTest}. See that class's Javadoc for the full rationale: where these
 * cases come from, which Weibo Empire rows were skipped and why, and why this class shares one
 * signed-in session across its money-moving rows rather than opening a fresh one per case.
 *
 * <h2>Confirmed live 2026-10-24 against test-v2.138hk.vip</h2>
 * Classic UI renders the same feature as New UI - same component, same CSS-module class names
 * underneath a different Tailwind skin (see {@link TransferPage}'s class Javadoc) - with one real
 * difference this class's shared {@link TransferPage} had to be fixed for: the "From"/"To" boxes
 * carried no "select"/"dropdown"-looking class here at all, unlike New UI's coincidental match, so
 * the old locator found nothing on Classic UI specifically. Every other locator and wording this
 * class relies on was confirmed identical to New UI, including the exact refusal/success wording
 * and the Submit-button bug - Classic UI's own top-nav "Transfer" link is a real {@code <button>}
 * (New UI's is an {@code <a>}), so the old unanchored "submit|transfer" pattern broke here in
 * practice, not just in theory.
 *
 * <h2>Case ids</h2>
 * {@code TRANSFER-015} through {@code TRANSFER-028}, continuing {@link ManualTransferNewUiTest}'s
 * {@code TRANSFER-001}-{@code TRANSFER-014} under the same prefix, in the same order:
 * <ul>
 *   <li>TRANSFER-015: "From" defaults to the main wallet and shows its balance (HKDTRANSFER-001)</li>
 *   <li>TRANSFER-016: the "From" list leads with the main wallet, then other wallets (HKDTRANSFER-002)</li>
 *   <li>TRANSFER-017: "To" defaults to "Please select" (HKDTRANSFER-005)</li>
 *   <li>TRANSFER-018: the "To" list offers the enabled wallets, paginated (HKDTRANSFER-006)</li>
 *   <li>TRANSFER-019: a negative amount cannot be typed (HKDTRANSFER-0017)</li>
 *   <li>TRANSFER-020: more than two decimal places cannot be entered (HKDTRANSFER-0018)</li>
 *   <li>TRANSFER-021: a zero amount is refused (HKDTRANSFER-0020)</li>
 *   <li>TRANSFER-022: an amount above the main wallet cannot be submitted (HKDTRANSFER-0021)</li>
 *   <li>TRANSFER-023: the same wallet cannot be both "From" and "To" (HKDTRANSFER-0015)</li>
 *   <li>TRANSFER-024: a nominal amount from the main wallet into a game wallet succeeds -
 *       {@code creates-data} (generalized from HKDTRANSFER-0011)</li>
 *   <li>TRANSFER-025: that same wallet transfers its balance back to the main wallet -
 *       {@code creates-data} (generalized from HKDTRANSFER-0013)</li>
 *   <li>TRANSFER-026: more than a game wallet's balance is refused (generalized from HKDTRANSFER-0014)</li>
 *   <li>TRANSFER-027: a "Transfer All Out" sweep, if the control exists, empties every game
 *       wallet back to the main wallet - {@code creates-data} (generalized from HKDTRANSFER-901).
 *       Runs <b>last</b> ({@code @Order(14)}), not in id order - see its own Javadoc.</li>
 *   <li>TRANSFER-028: a nominal amount from the main wallet into <i>every</i> offered game
 *       wallet is either accepted or refused, never stuck - {@code creates-data}, added
 *       2026-10-25 at Si's request; see {@link ManualTransferNewUiTest}'s own TRANSFER-014. Runs
 *       at {@code @Order(13)}, right before TRANSFER-027, which now sweeps what it leaves funded.</li>
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ManualTransferClassicUiTest extends BaseTest {

    private static BrowserContext memberContext;
    private static Page memberPage;
    private static TransferPage transfer;

    @BeforeAll
    static void signInAndChooseClassicUi() {
        memberContext = newContext();
        memberPage = memberContext.newPage();

        HomePage home = new HomePage(memberPage);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseClassicUi();
        home.dismissOverlays();
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to be the chosen UI "
                + "version before running the Manual Transfer cases - ui-version is \""
                + home.uiVersion() + "\"");

        signIn(memberPage);

        home.navigateTo();
        home.dismissOverlays();
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to still be the chosen "
                + "UI version after signing in - ui-version is \"" + home.uiVersion() + "\"");

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
    @DisplayName("TRANSFER-015: \"From\" defaults to the main wallet and shows its balance")
    void defaultFromIsTheMainWallet() {
        String from = transfer.fromSelection();

        assertTrue(from.toLowerCase().startsWith("main wallet"),
                "TRANSFER-015: \"From\" should default to the main wallet. It showed: " + from);
        assertFalse(transfer.mainBalance().isBlank(),
                "TRANSFER-015: \"From\" defaulted to the main wallet but carried no balance "
                        + "figure. It showed: " + from);
    }

    @Test
    @Order(2)
    @DisplayName("TRANSFER-016: the \"From\" list leads with the main wallet, then the other wallets")
    void fromListLeadsWithTheMainWallet() {
        List<String> offered = transfer.openFromOptions();

        assertFalse(offered.isEmpty(), "TRANSFER-016: the \"From\" list offered nothing at all");
        assertTrue(offered.get(0).toLowerCase().startsWith("main wallet"),
                "TRANSFER-016: the main wallet should be on top of the \"From\" list. "
                        + "The list read: " + offered);

        List<String> full = transfer.expandOptions();
        assertTrue(full.size() > 1,
                "TRANSFER-016: the \"From\" list offered the main wallet and no other wallets: "
                        + full);
    }

    @Test
    @Order(3)
    @DisplayName("TRANSFER-017: \"To\" defaults to \"Please select\"")
    void defaultToIsPleaseSelect() {
        String to = transfer.toSelection();

        assertTrue(to.startsWith(TransferPage.NOTHING_SELECTED),
                "TRANSFER-017: \"To\" should default to \"" + TransferPage.NOTHING_SELECTED
                        + "\". It showed: " + to);
    }

    @Test
    @Order(4)
    @DisplayName("TRANSFER-018: the \"To\" list offers the wallets admin has enabled")
    void toListOffersTheEnabledWallets() {
        List<String> firstPage = transfer.openToOptions();
        List<String> full = transfer.expandOptions();

        long wallets = full.stream()
                .filter(label -> !label.startsWith(TransferPage.NOTHING_SELECTED))
                .filter(label -> !label.toLowerCase().startsWith("main wallet"))
                .filter(label -> !label.startsWith("Show More"))
                .count();

        assertTrue(wallets > 0,
                "TRANSFER-018: the \"To\" list offered no game wallet at all. It read: " + full);

        // Fixed 2026-10-25 - see ManualTransferNewUiTest#toListOffersTheEnabledWallets for why
        // this was inverted: expandOptions() clicks "Show More" until it is exhausted, so a
        // fully expanded list should no longer carry one, not still end with one.
        if (full.size() > firstPage.size()) {
            assertFalse(full.get(full.size() - 1).startsWith("Show More"),
                    "TRANSFER-018: \"Show More\" was still the last entry after expanding every "
                            + "page - expansion should have run until it stopped being offered. "
                            + "The list read: " + full);
        }
    }

    // ================================================================= refusals

    @Test
    @Order(5)
    @DisplayName("TRANSFER-019: a negative amount cannot be typed")
    void negativeAmountCannotBeTyped() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);
        transfer.enterAmount("-10");

        String held = transfer.amountValue();
        assertFalse(held.startsWith("-"),
                "TRANSFER-019: the amount field accepted a negative number. It holds: '"
                        + held + "'");
    }

    @Test
    @Order(6)
    @DisplayName("TRANSFER-020: more than two decimal places cannot be entered")
    void moreThanTwoDecimalsCannotBeEntered() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);
        transfer.enterAmount("1.3949");

        String held = transfer.amountValue();
        int decimals = held.contains(".") ? held.length() - held.indexOf('.') - 1 : 0;
        assertTrue(decimals <= 2,
                "TRANSFER-020: the amount field kept " + decimals + " decimal places. "
                        + "It holds: '" + held + "'");
    }

    @Test
    @Order(7)
    @DisplayName("TRANSFER-021: a zero amount is refused")
    void zeroAmountIsRefused() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);
        transfer.enterAmount("0");

        // GUESS: see ManualTransferNewUiTest#zeroAmountIsRefused for the wording caveat.
        if (transfer.isSubmitEnabled()) {
            transfer.submit();
        }
        boolean refused = transfer.waitForMessage("greater than 0", 10_000);

        assertTrue(refused,
                "TRANSFER-021: a zero transfer should be refused. The page showed: "
                        + transfer.observedMessages());
    }

    @Test
    @Order(8)
    @DisplayName("TRANSFER-022: an amount above the main wallet cannot be submitted")
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
                "TRANSFER-022: Submit was disabled even for 1 against a balance of " + balance
                        + ", so this case cannot tell prevention from a form that never submits.");
        assertFalse(enabledForAnUnaffordableAmount,
                "TRANSFER-022: transferring more than the main wallet holds (" + balance
                        + ") should not be submittable, but Submit was enabled. The page showed: "
                        + transfer.observedMessages());
    }

    @Test
    @Order(9)
    @DisplayName("TRANSFER-023: the same wallet cannot be both \"From\" and \"To\"")
    void theSameWalletCannotBeBothEnds() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);

        List<String> fromOptions = transfer.openFromOptions();
        boolean stillOffered = fromOptions.stream().anyMatch(label -> label.startsWith(wallet));

        assertFalse(stillOffered,
                "TRANSFER-023: '" + wallet + "' is chosen in \"To\" but the \"From\" list "
                        + "still offers it, so the same wallet can be set at both ends. The "
                        + "\"From\" list read: " + fromOptions);
    }

    // ================================================================= transfers that move money

    /** Generalized from HKDTRANSFER-0011 - see {@link ManualTransferNewUiTest}'s Javadoc. */
    @Test
    @Tag("creates-data")
    @Order(10)
    @DisplayName("TRANSFER-024: a nominal amount from the main wallet into a game wallet succeeds")
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

        assertTrue(accepted,
                "TRANSFER-024: transferring " + TransferPage.PLACEHOLDER_AMOUNT + " into '"
                        + wallet + "' should report success. The page showed: "
                        + transfer.observedMessages());
    }

    /** Generalized from HKDTRANSFER-0013 - see {@link ManualTransferNewUiTest}'s Javadoc. */
    @Test
    @Tag("creates-data")
    @Order(11)
    @DisplayName("TRANSFER-025: a funded game wallet transfers its balance back to the main wallet")
    void aGameWalletTransfersBackToTheMainWallet() {
        String source = transfer.firstWalletHoldingAtLeast(1);
        assumeTrue(source != null,
                "No game wallet holds at least 1, so there is nothing to transfer back - this "
                        + "normally runs right after TRANSFER-024 funded one.");

        double inWallet = transfer.walletBalanceOf(source);
        double sendBack = Math.min(1, inWallet);

        transfer.chooseFrom(source);
        transfer.chooseTo("Main wallet");
        transfer.enterAmount(String.valueOf(sendBack));
        transfer.submit();

        boolean accepted = transfer.waitForMessage("success", 15_000);
        assertTrue(accepted,
                "TRANSFER-025: transferring " + sendBack + " out of '" + source + "', which held "
                        + inWallet + ", should report success. The page showed: "
                        + transfer.observedMessages());
    }

    /** Generalized from HKDTRANSFER-0014. */
    @Test
    @Order(12)
    @DisplayName("TRANSFER-026: transferring more than a game wallet's balance is refused")
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
            // GUESS: see ManualTransferNewUiTest#moreThanTheWalletBalanceIsRefused for the
            // wording caveat and why this branch is not expected to run in practice.
            boolean refused = transfer.waitForMessage("fail", 12_000);
            assertTrue(refused,
                    "TRANSFER-026: transferring " + tooMuch + " out of '" + wallet
                            + "', which holds " + inWallet + ", should be refused. The page "
                            + "showed: " + transfer.observedMessages());
        } else {
            assertFalse(transfer.isSubmitEnabled(),
                    "TRANSFER-026: Submit should stay disabled for " + tooMuch + " against a "
                            + "wallet balance of " + inWallet + ".");
        }
    }

    /**
     * Generalized from HKDTRANSFER-901 - see
     * {@link ManualTransferNewUiTest#transferAllOutSweepsEveryWallet()}'s own Javadoc for the
     * full rationale, including why this is now ordered <b>last</b> ({@code @Order(14)}) at Si's
     * request, sweeping whatever TRANSFER-028 (ordered right before this case) leaves funded,
     * rather than running before it and needing to fund its own wallet first.
     */
    @Test
    @Tag("creates-data")
    @Order(14)
    @DisplayName("TRANSFER-027: a \"Transfer All Out\" sweep, if present, empties every game "
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
                "TRANSFER-027: \"Transfer All Out\" should report success, or at least empty "
                        + "every game wallet. The page showed: " + transfer.observedMessages()
                        + (stillFunded == null ? "" : "; '" + stillFunded
                                + "' still holds a balance after the sweep"));
    }

    /**
     * Added 2026-10-25 at Si's request - see
     * {@link ManualTransferNewUiTest#mainWalletToEveryGameWalletTransfers()}'s own Javadoc for
     * the full rationale (why a "fail" refusal is an accepted outcome, not a defect; why a missing
     * popup is checked against the wallet's own balance before being called stuck, same as that
     * case). Same walk, same account, Classic UI only difference being the UI version this
     * session is signed into. No longer sweeps its own wallets back out at the end - ordered at
     * {@code @Order(13)}, right before TRANSFER-027, which now sweeps what this case leaves
     * funded, at Si's request.
     */
    @Test
    @Tag("creates-data")
    @Order(13)
    @DisplayName("TRANSFER-028: a nominal amount from the main wallet into every offered game wallet is either accepted or refused, never stuck")
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
            boolean accepted = transfer.waitForMessage("success", 30_000);
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

        assertTrue(anomalies.isEmpty(), "TRANSFER-028: " + anomalies);
        assertFalse(succeeded.isEmpty(), "TRANSFER-028: not one of the " + offered.size()
                + " offered wallets accepted the transfer. Refused: " + refused);
    }

    /**
     * Records one wallet's own verdict in the step trail and captures a screenshot of the page it
     * was read from - see {@link ManualTransferNewUiTest#checkAndCapture(String)}'s own Javadoc
     * for the full rationale (why this case stays one report row rather than 47, how the pairing
     * with {@code HtmlReportGenerator#stepsPanel} works, and the PASS/FAIL wording convention).
     */
    private void checkAndCapture(String verdict) {
        CaseLog.check(verdict);
        ScreenshotOnFinish.captureNow(memberPage, verdict);
    }
}
