package org.sengmean.tests.payment;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.payment.WithdrawalPage;
import org.sengmean.page.wallet.HomePage;
import org.sengmean.report.CaseLog;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The member withdrawal page, on Classic UI, as answered by this project's own UI-version
 * picker ({@link org.sengmean.base.BasePage#chooseClassicUi()}).
 *
 * <p>The other half of the New UI / Classic UI split, running the identical 12 checks as
 * {@link WithdrawalNewUiTest} under Classic Interface instead - see that class's Javadoc for the
 * full case-by-case mapping back to the sibling NewWeibo_automation project's
 * {@code WithdrawalPageTest}, the "valid means pending, not completed" ruling each case built on
 * {@link WithdrawalPage#isConfirmationPrompt()} carries over, and the complete list of the other
 * 52 NewWeibo cases not ported here and why (tied to a named bank/channel/option, tied to a field
 * quirk observed and ruled on for Weibo Empire specifically, tied to an unconfirmed text label, or
 * needing {@code BaseTest#ALT_USERNAME}, which this project does not have).
 *
 * <h2>What every case does</h2>
 * Signs the shared QA account in, opens the home page, answers the UI-version picker with Classic
 * Interface, confirms the choice stuck ({@link org.sengmean.base.BasePage#isClassicUiChosen()}),
 * then navigates to the withdrawal page - mirroring {@link
 * org.sengmean.tests.layout.ResponsiveLayoutClassicUiTest}'s visitor-then-member walk. The one
 * exception is WITHDRAWAL-013, which is deliberately a guest case and never signs in.
 *
 * <h2>Case ids</h2>
 * {@code WITHDRAWAL-013} through {@code WITHDRAWAL-024}, continuing {@link WithdrawalNewUiTest}'s
 * {@code WITHDRAWAL-001}-{@code WITHDRAWAL-012} under the same invented prefix:
 * <pre>
 * WITHDRAWAL-013  guest is redirected away from the withdrawal page         (mirrors WITHDRAWAL-001 / HKDWITHDRA-914)
 * WITHDRAWAL-014  a valid withdrawal at the stated minimum reaches a       (mirrors WITHDRAWAL-002 / HKDWITHDRA-901)
 *                 confirmation/pending prompt, not an immediate payout
 * WITHDRAWAL-015  cancelling the prompt withdraws nothing, keeps the amount (mirrors WITHDRAWAL-003 / HKDWITHDRA-903)
 * WITHDRAWAL-016  an amount below the page's stated minimum is refused     (mirrors WITHDRAWAL-004 / HKDWITHDRA-904)
 * WITHDRAWAL-017  an amount above the page's stated maximum is refused     (mirrors WITHDRAWAL-005 / HKDWITHDRA-905)
 * WITHDRAWAL-018  no offered option accepts below its own stated minimum   (mirrors WITHDRAWAL-006 / HKDWITHDRA-0055)
 * WITHDRAWAL-019  no offered option accepts above its own stated maximum   (mirrors WITHDRAWAL-007 / HKDWITHDRA-0059)
 * WITHDRAWAL-020  submitting with the amount field empty is refused        (mirrors WITHDRAWAL-008 / HKDWITHDRA-906)
 * WITHDRAWAL-021  letters are not accepted as an amount                    (mirrors WITHDRAWAL-009 / HKDWITHDRA-907)
 * WITHDRAWAL-022  exponent notation is not silently read as a bigger amount (mirrors WITHDRAWAL-010 / HKDWITHDRA-908)
 * WITHDRAWAL-023  an amount above the balance (within the stated maximum)  (mirrors WITHDRAWAL-011 / HKDWITHDRA-912)
 *                 is refused rather than offered for confirmation
 * WITHDRAWAL-024  withdraw option labels are unique                       (mirrors WITHDRAWAL-012 / HKDWITHDRA-918)
 * </pre>
 */
public class WithdrawalClassicUiTest extends BaseTest {

    @Test
    @DisplayName("WITHDRAWAL-013: a guest is redirected away from the withdrawal page")
    void guestCannotReachWithdrawalPage() {
        WithdrawalPage withdrawal = openGuestWithdrawal();
        page().waitForTimeout(2_000);

        assertTrue(withdrawal.redirectedAway(),
                "The withdrawal page was served to a guest. Current URL: " + page().url());
    }

    @Test
    @DisplayName("WITHDRAWAL-014: a valid withdrawal at the stated minimum reaches a confirmation prompt")
    void validWithdrawalReachesConfirmationPrompt() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        double minimum = withdrawal.statedMinimum();
        assumeTrue(!Double.isNaN(minimum),
                "The amount field's placeholder stated no readable minimum, so there is no "
                        + "page-stated figure to build a valid amount from (see WithdrawalPage's "
                        + "Selector confidence note on statedMinimum()).");

        // Generalized: NewWeibo's HKDWITHDRA-901 used its own confirmed minimum, 100. Here the
        // amount is read from the page itself rather than carried over as a literal.
        String amount = String.valueOf((long) Math.ceil(minimum));
        withdrawal.enterAmount(amount);
        withdrawal.submit();

        boolean offeredToConfirm = withdrawal.isConfirmationPrompt();
        String popup = withdrawal.popupText();
        withdrawal.dismissPopup();

        assertTrue(offeredToConfirm, "SUBMIT did not offer to confirm a valid amount (" + amount
                + ", the page's own stated minimum). Popup said: '" + popup + "'");
    }

    @Test
    @DisplayName("WITHDRAWAL-015: cancelling the confirmation prompt withdraws nothing and keeps the amount")
    void cancellingConfirmationWithdrawsNothing() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        double minimum = withdrawal.statedMinimum();
        assumeTrue(!Double.isNaN(minimum),
                "No readable stated minimum to build a valid amount from.");
        double balanceBefore = withdrawal.mainBalance();

        String amount = String.valueOf((long) Math.ceil(minimum));
        withdrawal.enterAmount(amount);
        withdrawal.submit();
        assumeTrue(withdrawal.isConfirmationPrompt(),
                "SUBMIT did not offer to confirm. Popup said: '" + withdrawal.popupText() + "'");

        withdrawal.cancelConfirmation();

        assertCancelledCleanly(withdrawal, amount, balanceBefore);
    }

    private void assertCancelledCleanly(WithdrawalPage withdrawal, String amount, double balanceBefore) {
        org.junit.jupiter.api.Assertions.assertAll("cancelling backs out cleanly",
                () -> assertFalse(withdrawal.isConfirmationPrompt(),
                        "Cancel left the confirmation prompt on screen"),
                () -> assertEquals(amount, withdrawal.amountValue(),
                        "Cancel cleared the amount the customer had typed"),
                () -> {
                    double balanceAfter = withdrawal.mainBalance();
                    if (balanceBefore >= 0 && balanceAfter >= 0) {
                        assertEquals(balanceBefore, balanceAfter, 0.001,
                                "The balance moved even though the withdrawal was cancelled");
                    }
                });
    }

    @Test
    @DisplayName("WITHDRAWAL-016: an amount below the page's stated minimum is refused on submit")
    void amountBelowStatedMinimumIsRejected() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        double minimum = withdrawal.statedMinimum();
        assumeTrue(!Double.isNaN(minimum) && minimum > 0,
                "No readable stated minimum to build a below-minimum amount from.");

        String belowMinimum = String.valueOf(Math.max(0, (long) Math.floor(minimum) - 1));
        withdrawal.enterAmount(belowMinimum);
        withdrawal.submit();
        String popup = withdrawal.popupText();
        withdrawal.dismissPopup();

        assertFalse(popup.toLowerCase().contains("confirm"),
                "SUBMIT offered to confirm " + belowMinimum + ", which is below the page's own "
                        + "stated minimum of " + minimum + ". Popup said: '" + popup + "'");
    }

    @Test
    @DisplayName("WITHDRAWAL-017: an amount above the page's stated maximum is refused on submit")
    void amountAboveStatedMaximumIsRejected() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        double maximum = withdrawal.statedMaximum();
        assumeTrue(!Double.isNaN(maximum),
                "No readable stated maximum to build an above-maximum amount from.");

        String aboveMaximum = String.valueOf((long) Math.ceil(maximum) + 1);
        withdrawal.enterAmount(aboveMaximum);
        withdrawal.submit();
        String popup = withdrawal.popupText();
        withdrawal.dismissPopup();

        assertFalse(popup.toLowerCase().contains("confirm"),
                "SUBMIT offered to confirm " + aboveMaximum + ", which is above the page's own "
                        + "stated maximum of " + maximum + ". Popup said: '" + popup + "'");
    }

    /** Walks every currently offered option and reports any that accepts an out-of-range amount. */
    private List<String> optionsAcceptingOutOfRange(WithdrawalPage withdrawal, boolean below) {
        withdrawal.dismissPopup();
        List<String> offenders = new ArrayList<>();
        List<String> checked = new ArrayList<>();

        for (String option : List.copyOf(withdrawal.optionLabels())) {
            if (option == null || option.isBlank()) {
                continue;
            }
            try {
                withdrawal.selectOption(option);
                if (!withdrawal.hasFreeEntryAmount()) {
                    continue;
                }
                double bound = below ? withdrawal.statedMinimum() : withdrawal.statedMaximum();
                if (Double.isNaN(bound)) {
                    continue;
                }
                long amount = below
                        ? Math.max(0, (long) Math.floor(bound) - 1)
                        : (long) Math.ceil(bound) + 1;

                withdrawal.enterAmount(String.valueOf(amount));
                withdrawal.submit();
                checked.add(option + "(" + amount + ")");
                if (withdrawal.isConfirmationPrompt()) {
                    offenders.add(option + " accepted " + amount
                            + " against a stated bound of " + bound);
                    withdrawal.cancelConfirmation();
                }
                withdrawal.dismissPopup();
            } catch (RuntimeException unreachable) {
                // One awkward option must not fail the whole walk.
                try {
                    withdrawal.dismissPopup();
                } catch (RuntimeException ignored) {
                    // Nothing to clear.
                }
            }
        }
        CaseLog.data("Options checked", String.valueOf(checked));
        assumeTrue(!checked.isEmpty(),
                "No currently offered option stated a readable range, so there was no rule to test.");
        return offenders;
    }

    @Test
    @DisplayName("WITHDRAWAL-018: no currently offered withdraw option accepts an amount below its own stated minimum")
    void noOptionAcceptsBelowItsOwnMinimum() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        List<String> offenders = optionsAcceptingOutOfRange(withdrawal, true);
        assertTrue(offenders.isEmpty(), "WITHDRAWAL-018: " + offenders);
    }

    @Test
    @DisplayName("WITHDRAWAL-019: no currently offered withdraw option accepts an amount above its own stated maximum")
    void noOptionAcceptsAboveItsOwnMaximum() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        List<String> offenders = optionsAcceptingOutOfRange(withdrawal, false);
        assertTrue(offenders.isEmpty(), "WITHDRAWAL-019: " + offenders);
    }

    @Test
    @DisplayName("WITHDRAWAL-020: submitting with the amount field empty is refused")
    void emptyAmountIsRejected() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        withdrawal.clearAmount();
        withdrawal.submit();
        String popup = withdrawal.popupText();
        withdrawal.dismissPopup();

        assertFalse(popup.toLowerCase().contains("confirm"),
                "SUBMIT offered to confirm a withdrawal with no amount. Popup said: '" + popup + "'");
    }

    @Test
    @DisplayName("WITHDRAWAL-021: letters are not accepted as an amount")
    void lettersAreRejected() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        withdrawal.typeAmount("abc");
        String valueAfterTyping = withdrawal.amountValue();
        withdrawal.submit();
        String popup = withdrawal.popupText();
        withdrawal.dismissPopup();

        assertFalse(popup.toLowerCase().contains("confirm"),
                "SUBMIT offered to confirm the non-numeric amount 'abc' (field held '"
                        + valueAfterTyping + "'). Popup said: '" + popup + "'");
    }

    @Test
    @DisplayName("WITHDRAWAL-022: exponent notation is not silently read as a bigger amount")
    void exponentNotationIsNotSilentlyAccepted() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        withdrawal.typeAmount("1e5");
        String valueAfterTyping = withdrawal.amountValue();
        withdrawal.submit();
        boolean offeredToConfirm = withdrawal.isConfirmationPrompt();
        String popup = withdrawal.popupText();
        withdrawal.dismissPopup();

        // Generalized from NewWeibo's HKDWITHDRA-908, which checked the summary did not literally
        // read "Total: 100000". Here the field holding "1e5" is not expected to reach a
        // confirmation prompt at all, since there is no confirmed, page-stated figure "1e5" would
        // correctly parse to for this project to assert against instead.
        assertFalse(offeredToConfirm,
                "'1e5' (field held '" + valueAfterTyping + "') reached a confirmation prompt: '"
                        + popup + "'");
    }

    @Test
    @DisplayName("WITHDRAWAL-023: an amount above the balance, within the stated maximum, is refused rather than offered to confirm")
    void amountAboveBalanceIsRejected() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        double balance = withdrawal.mainBalance();
        assumeTrue(balance >= 0, "The wallet balance could not be read, so no over-balance amount "
                + "can be built.");
        double maximum = withdrawal.statedMaximum();

        long overBalance = (long) Math.ceil(balance) + 1;
        if (!Double.isNaN(maximum)) {
            assumeTrue(overBalance < maximum,
                    "The balance of " + balance + " is too close to the stated maximum of "
                            + maximum + " for this case to build a distinct over-balance amount.");
        }

        withdrawal.enterAmount(String.valueOf(overBalance));
        withdrawal.submit();
        String popup = withdrawal.popupText();
        withdrawal.dismissPopup();

        assertFalse(popup.toLowerCase().contains("confirm"),
                "WITHDRAWAL-023: SUBMIT offered to confirm a withdrawal of " + overBalance
                        + " against a balance of " + balance + ". Popup said: '" + popup + "'");
    }

    @Test
    @DisplayName("WITHDRAWAL-024: withdraw option labels are unique")
    void withdrawOptionLabelsAreUnique() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        List<String> labels = withdrawal.optionLabels();
        List<String> duplicates = labels.stream()
                .filter(l -> java.util.Collections.frequency(labels, l) > 1)
                .distinct()
                .toList();

        assertTrue(duplicates.isEmpty(), "WITHDRAWAL-024: duplicated withdraw option labels: "
                + duplicates);
    }

    // ----------------------------------------------------------------- setup

    /** Opens the home page as a fresh visitor, answers the picker with Classic Interface, confirms
     *  it stuck, then opens the withdrawal page without signing in - the one guest-only case. */
    private WithdrawalPage openGuestWithdrawal() {
        Page on = page();
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseClassicUi();
        home.dismissOverlays();
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to be the chosen UI "
                + "version - ui-version is \"" + home.uiVersion() + "\"");

        WithdrawalPage withdrawal = new WithdrawalPage(on);
        withdrawal.open();
        return withdrawal;
    }

    /** As {@link #openGuestWithdrawal()}, then signs the shared QA account in, returns to the
     *  home page, confirms both the signed-in state and the UI choice held, then opens the
     *  withdrawal page. Mirrors {@link org.sengmean.tests.layout.ResponsiveLayoutClassicUiTest}'s
     *  member walk. */
    private WithdrawalPage openMemberWithdrawal() {
        Page on = page();
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseClassicUi();
        home.dismissOverlays();
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to be the chosen UI "
                + "version - ui-version is \"" + home.uiVersion() + "\"");

        signIn(on);

        home.navigateTo();
        home.dismissOverlays();
        boolean signedIn = new LoginPage(on).isSignedIn();
        CaseLog.data("Signed in", String.valueOf(signedIn));
        assertTrue(signedIn, "Expected the header to show the signed-in state after returning to "
                + "the home page.");
        assertTrue(home.isClassicUiChosen(), "Expected Classic Interface to still be the chosen "
                + "UI version after signing in - ui-version is \"" + home.uiVersion() + "\"");

        WithdrawalPage withdrawal = new WithdrawalPage(on);
        withdrawal.open();
        assertTrue(withdrawal.isLoaded(), "The withdrawal form did not render for " + USERNAME
                + ". Current URL: " + on.url());
        return withdrawal;
    }
}
