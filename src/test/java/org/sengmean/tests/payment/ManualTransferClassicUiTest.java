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
 * Manual Transfer on Quark Dynasty's <b>Classic UI</b> - the other half of the split from
 * {@link ManualTransferNewUiTest}. See that class's Javadoc for the full rationale: where these
 * cases come from, which Weibo Empire rows were skipped and why, and why this class shares one
 * signed-in session across its money-moving rows rather than opening a fresh one per case.
 *
 * <h2>Read this before trusting any row in this class</h2>
 * Same caveat as {@link ManualTransferNewUiTest}, repeated because it cannot be said too many
 * times: nobody has opened a Manual Transfer screen on {@code test-v2.138hk.vip} in either UI
 * version, and this project's README does not mention the feature at all. Every locator lives in
 * {@link TransferPage}, shared unchanged between both UI versions since nothing about the ported
 * cases is specific to New UI - whether Classic UI's own transfer screen (if either exists)
 * shares the same markup is exactly as unconfirmed as everything else in this area.
 *
 * <h2>Case ids</h2>
 * {@code TRANSFER-014} through {@code TRANSFER-026}, continuing {@link ManualTransferNewUiTest}'s
 * {@code TRANSFER-001}-{@code TRANSFER-013} under the same prefix, in the same order:
 * <ul>
 *   <li>TRANSFER-014: "From" defaults to the main wallet and shows its balance (HKDTRANSFER-001)</li>
 *   <li>TRANSFER-015: the "From" list leads with the main wallet, then other wallets (HKDTRANSFER-002)</li>
 *   <li>TRANSFER-016: "To" defaults to "Please select" (HKDTRANSFER-005)</li>
 *   <li>TRANSFER-017: the "To" list offers the enabled wallets, paginated (HKDTRANSFER-006)</li>
 *   <li>TRANSFER-018: a negative amount cannot be typed (HKDTRANSFER-0017)</li>
 *   <li>TRANSFER-019: more than two decimal places cannot be entered (HKDTRANSFER-0018)</li>
 *   <li>TRANSFER-020: a zero amount is refused (HKDTRANSFER-0020)</li>
 *   <li>TRANSFER-021: an amount above the main wallet cannot be submitted (HKDTRANSFER-0021)</li>
 *   <li>TRANSFER-022: the same wallet cannot be both "From" and "To" (HKDTRANSFER-0015)</li>
 *   <li>TRANSFER-023: a nominal amount from the main wallet into a game wallet succeeds -
 *       {@code creates-data} (generalized from HKDTRANSFER-0011)</li>
 *   <li>TRANSFER-024: that same wallet transfers its balance back to the main wallet -
 *       {@code creates-data} (generalized from HKDTRANSFER-0013)</li>
 *   <li>TRANSFER-025: more than a game wallet's balance is refused (generalized from HKDTRANSFER-0014)</li>
 *   <li>TRANSFER-026: a "Transfer All Out" sweep, if the control exists, empties every game
 *       wallet back to the main wallet - {@code creates-data} (generalized from HKDTRANSFER-901)</li>
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
    @DisplayName("TRANSFER-014: \"From\" defaults to the main wallet and shows its balance")
    void defaultFromIsTheMainWallet() {
        String from = transfer.fromSelection();

        assertTrue(from.toLowerCase().startsWith("main wallet"),
                "TRANSFER-014: \"From\" should default to the main wallet. It showed: " + from);
        assertFalse(transfer.mainBalance().isBlank(),
                "TRANSFER-014: \"From\" defaulted to the main wallet but carried no balance "
                        + "figure. It showed: " + from);
    }

    @Test
    @Order(2)
    @DisplayName("TRANSFER-015: the \"From\" list leads with the main wallet, then the other wallets")
    void fromListLeadsWithTheMainWallet() {
        List<String> offered = transfer.openFromOptions();

        assertFalse(offered.isEmpty(), "TRANSFER-015: the \"From\" list offered nothing at all");
        assertTrue(offered.get(0).toLowerCase().startsWith("main wallet"),
                "TRANSFER-015: the main wallet should be on top of the \"From\" list. "
                        + "The list read: " + offered);

        List<String> full = transfer.expandOptions();
        assertTrue(full.size() > 1,
                "TRANSFER-015: the \"From\" list offered the main wallet and no other wallets: "
                        + full);
    }

    @Test
    @Order(3)
    @DisplayName("TRANSFER-016: \"To\" defaults to \"Please select\"")
    void defaultToIsPleaseSelect() {
        String to = transfer.toSelection();

        assertTrue(to.startsWith(TransferPage.NOTHING_SELECTED),
                "TRANSFER-016: \"To\" should default to \"" + TransferPage.NOTHING_SELECTED
                        + "\". It showed: " + to);
    }

    @Test
    @Order(4)
    @DisplayName("TRANSFER-017: the \"To\" list offers the wallets admin has enabled")
    void toListOffersTheEnabledWallets() {
        List<String> firstPage = transfer.openToOptions();
        List<String> full = transfer.expandOptions();

        long wallets = full.stream()
                .filter(label -> !label.startsWith(TransferPage.NOTHING_SELECTED))
                .filter(label -> !label.toLowerCase().startsWith("main wallet"))
                .filter(label -> !label.startsWith("Show More"))
                .count();

        assertTrue(wallets > 0,
                "TRANSFER-017: the \"To\" list offered no game wallet at all. It read: " + full);

        if (full.size() >= firstPage.size()) {
            assertTrue(full.get(full.size() - 1).startsWith("Show More"),
                    "TRANSFER-017: the last entry in the \"To\" list should be \"Show More\" "
                            + "when there are more wallets to reveal. The list read: " + full);
        }
    }

    // ================================================================= refusals

    @Test
    @Order(5)
    @DisplayName("TRANSFER-018: a negative amount cannot be typed")
    void negativeAmountCannotBeTyped() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);
        transfer.enterAmount("-10");

        String held = transfer.amountValue();
        assertFalse(held.startsWith("-"),
                "TRANSFER-018: the amount field accepted a negative number. It holds: '"
                        + held + "'");
    }

    @Test
    @Order(6)
    @DisplayName("TRANSFER-019: more than two decimal places cannot be entered")
    void moreThanTwoDecimalsCannotBeEntered() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);
        transfer.enterAmount("1.3949");

        String held = transfer.amountValue();
        int decimals = held.contains(".") ? held.length() - held.indexOf('.') - 1 : 0;
        assertTrue(decimals <= 2,
                "TRANSFER-019: the amount field kept " + decimals + " decimal places. "
                        + "It holds: '" + held + "'");
    }

    @Test
    @Order(7)
    @DisplayName("TRANSFER-020: a zero amount is refused")
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
                "TRANSFER-020: a zero transfer should be refused. The page showed: "
                        + transfer.observedMessages());
    }

    @Test
    @Order(8)
    @DisplayName("TRANSFER-021: an amount above the main wallet cannot be submitted")
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
                "TRANSFER-021: Submit was disabled even for 1 against a balance of " + balance
                        + ", so this case cannot tell prevention from a form that never submits.");
        assertFalse(enabledForAnUnaffordableAmount,
                "TRANSFER-021: transferring more than the main wallet holds (" + balance
                        + ") should not be submittable, but Submit was enabled. The page showed: "
                        + transfer.observedMessages());
    }

    @Test
    @Order(9)
    @DisplayName("TRANSFER-022: the same wallet cannot be both \"From\" and \"To\"")
    void theSameWalletCannotBeBothEnds() {
        String wallet = transfer.firstGameOffered();
        assumeTrue(wallet != null, "No game wallet is offered on this account, so there is "
                + "nothing to transfer to.");

        transfer.chooseTo(wallet);

        List<String> fromOptions = transfer.openFromOptions();
        boolean stillOffered = fromOptions.stream().anyMatch(label -> label.startsWith(wallet));

        assertFalse(stillOffered,
                "TRANSFER-022: '" + wallet + "' is chosen in \"To\" but the \"From\" list "
                        + "still offers it, so the same wallet can be set at both ends. The "
                        + "\"From\" list read: " + fromOptions);
    }

    // ================================================================= transfers that move money

    /** Generalized from HKDTRANSFER-0011 - see {@link ManualTransferNewUiTest}'s Javadoc. */
    @Test
    @Tag("creates-data")
    @Order(10)
    @DisplayName("TRANSFER-023: a nominal amount from the main wallet into a game wallet succeeds")
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
                "TRANSFER-023: transferring " + TransferPage.PLACEHOLDER_AMOUNT + " into '"
                        + wallet + "' should report success. The page showed: "
                        + transfer.observedMessages());
    }

    /** Generalized from HKDTRANSFER-0013 - see {@link ManualTransferNewUiTest}'s Javadoc. */
    @Test
    @Tag("creates-data")
    @Order(11)
    @DisplayName("TRANSFER-024: a funded game wallet transfers its balance back to the main wallet")
    void aGameWalletTransfersBackToTheMainWallet() {
        String source = transfer.firstWalletHoldingAtLeast(1);
        assumeTrue(source != null,
                "No game wallet holds at least 1, so there is nothing to transfer back - this "
                        + "normally runs right after TRANSFER-023 funded one.");

        double inWallet = transfer.walletBalanceOf(source);
        double sendBack = Math.min(1, inWallet);

        transfer.chooseFrom(source);
        transfer.chooseTo("Main wallet");
        transfer.enterAmount(String.valueOf(sendBack));
        transfer.submit();
        transfer.confirmIfAsked(6_000);

        boolean accepted = transfer.waitForMessage("success", 15_000);
        assertTrue(accepted,
                "TRANSFER-024: transferring " + sendBack + " out of '" + source + "', which held "
                        + inWallet + ", should report success. The page showed: "
                        + transfer.observedMessages());
    }

    /** Generalized from HKDTRANSFER-0014. */
    @Test
    @Order(12)
    @DisplayName("TRANSFER-025: transferring more than a game wallet's balance is refused")
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
            // GUESS: see ManualTransferNewUiTest#moreThanTheWalletBalanceIsRefused for the
            // wording caveat.
            boolean refused = transfer.waitForMessage("fail", 12_000);
            assertTrue(refused,
                    "TRANSFER-025: transferring " + tooMuch + " out of '" + wallet
                            + "', which holds " + inWallet + ", should be refused. The page "
                            + "showed: " + transfer.observedMessages());
        } else {
            assertFalse(transfer.isSubmitEnabled(),
                    "TRANSFER-025: Submit should stay disabled for " + tooMuch + " against a "
                            + "wallet balance of " + inWallet + ".");
        }
    }

    /** Generalized from HKDTRANSFER-901 - see {@link ManualTransferNewUiTest}'s Javadoc. */
    @Test
    @Tag("creates-data")
    @Order(13)
    @DisplayName("TRANSFER-026: a \"Transfer All Out\" sweep, if present, empties every game "
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
                "TRANSFER-026: \"Transfer All Out\" should report success. The page showed: "
                        + transfer.observedMessages());
    }
}
