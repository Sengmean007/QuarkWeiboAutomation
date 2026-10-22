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
 * The member withdrawal page, on the New UI, as answered by this project's own UI-version
 * picker ({@link org.sengmean.base.BasePage#chooseNewUi()}).
 *
 * <p>Ported from the sibling NewWeibo_automation project's {@code WithdrawalPageTest} (64 cases,
 * against Weibo Empire's own withdrawal screen). <b>This class ports the shape of 12 of those 64,
 * never their literals</b>: Quark Dynasty's real bank names, channel names, minimum/maximum
 * amounts and validation wording are unknown - nobody has inspected this project's live
 * withdrawal screen yet - so every amount used below is read from the page's own stated limit at
 * run time ({@link WithdrawalPage#statedMinimum()} / {@link WithdrawalPage#statedMaximum()} /
 * {@link WithdrawalPage#mainBalance()}) rather than a number carried over from Weibo Empire's
 * confirmed 100/100,000. A case that cannot read the figure it needs skips, naming why, rather
 * than falling back to a guessed literal.
 *
 * <h2>What every case does</h2>
 * Signs the shared QA account in, opens the home page, answers the UI-version picker with New
 * Interface, confirms the choice stuck ({@link org.sengmean.base.BasePage#isNewUiChosen()}),
 * then navigates to the withdrawal page - mirroring {@link
 * org.sengmean.tests.layout.ResponsiveLayoutNewUiTest}'s visitor-then-member walk. The one
 * exception is WITHDRAWAL-001, which is deliberately a guest case and never signs in.
 *
 * <h2>Case ids and what each ports</h2>
 * Invented for this project, as there is no client test-case workbook to take ids from (see
 * {@link org.sengmean.tests.layout.ResponsiveLayoutNewUiTest}'s own {@code UI-} prefix for the
 * precedent). {@code WITHDRAWAL-001} through {@code WITHDRAWAL-012} here; Classic UI continues
 * at {@code WITHDRAWAL-013} in {@link WithdrawalClassicUiTest}, running the identical 12 checks
 * under Classic Interface instead.
 * <pre>
 * WITHDRAWAL-001  guest is redirected away from the withdrawal page         (HKDWITHDRA-914)
 * WITHDRAWAL-002  a valid withdrawal at the stated minimum reaches a        (HKDWITHDRA-901, generalized -
 *                 confirmation/pending prompt, not an immediate payout       see "Valid means pending" below)
 * WITHDRAWAL-003  cancelling the prompt withdraws nothing, keeps the amount (HKDWITHDRA-903)
 * WITHDRAWAL-004  an amount below the page's stated minimum is refused      (HKDWITHDRA-904, generalized)
 * WITHDRAWAL-005  an amount above the page's stated maximum is refused      (HKDWITHDRA-905, generalized)
 * WITHDRAWAL-006  no offered option accepts below its own stated minimum    (HKDWITHDRA-0055, already generic)
 * WITHDRAWAL-007  no offered option accepts above its own stated maximum    (HKDWITHDRA-0059, already generic)
 * WITHDRAWAL-008  submitting with the amount field empty is refused         (HKDWITHDRA-906)
 * WITHDRAWAL-009  letters are not accepted as an amount                     (HKDWITHDRA-907)
 * WITHDRAWAL-010  exponent notation is not silently read as a bigger amount (HKDWITHDRA-908, generalized)
 * WITHDRAWAL-011  an amount above the balance (within the stated maximum)   (HKDWITHDRA-912, generalized)
 *                 is refused rather than offered for confirmation
 * WITHDRAWAL-012  withdraw option labels are unique                        (HKDWITHDRA-918, already generic)
 * </pre>
 *
 * <h2>"Valid" means pending, not completed</h2>
 * WITHDRAWAL-002 is built on {@link WithdrawalPage#isConfirmationPrompt()}, which only asks
 * whether SUBMIT offered to take the request to confirmation - never on whether the balance
 * moved. This carries over the sibling project's own 2026-09-29 ruling that a "valid withdrawal"
 * means the request is now pending an administrator's decision, not that money has moved; see
 * {@code WithdrawalPage#confirmWithdrawal()}'s Javadoc for why that step - the only one that would
 * actually test "completed" - is never called from any case here.
 *
 * <h2>NewWeibo cases not ported, and why</h2>
 * The remaining 52 of the 64 fall into three groups, none of which has a sensible Quark-side
 * generalization:
 * <ul>
 *   <li><b>Tied to a specific bank/channel/option name</b> - HKDWITHDRA-902 (names "Bank Name"/
 *       "Bank Number" specifically), -916 (the five named withdrawal channels), -917 (the "Main
 *       Balance" wallet selector and literal "Min"/"Max" hint text), -0087/-0089 (the charge
 *       summary's "Request amount:"/"Service charge:"/"Total charge:" row labels), the whole
 *       {@code WithdrawOptions} nested class (-923 through -936, one case per named option/
 *       channel) and the whole {@code ConfirmedWithdrawals} nested class (-921/-922/-941 through
 *       -952/-943/-00131/-00119, one per named option, all of them also pressing Confirm for
 *       real).</li>
 *   <li><b>Tied to a specific observed field quirk, ruled on by Si for Weibo Empire specifically
 *       and not something this project can assume without its own observation</b> -
 *       HKDWITHDRA-909 (the minus sign is stripped), -910/-911 (the decimal point is dropped),
 *       -913 (the field declares no min/max/required attribute). Writing any of these for Quark
 *       would mean asserting a literal about Quark's own field behaviour nobody has watched it
 *       show.</li>
 *   <li><b>Tied to a specific text label never confirmed to exist on Quark</b> - HKDWITHDRA-915
 *       (a "Daily withdrawal frequency" notice).</li>
 *   <li><b>Needs an account this project has no equivalent for</b> - HKDWITHDRA-919/-920 (a real
 *       confirmed over-balance/under-turnover withdrawal) and the {@code RequiredFields} nested
 *       class (-0057/-0061/-0093), which all depend on {@code BaseTest#ALT_USERNAME} - a second,
 *       intentionally-unbound QA account the sibling project has and this one does not. Adding
 *       one would mean editing {@code BaseTest}, outside this task's scope.</li>
 * </ul>
 */
public class WithdrawalNewUiTest extends BaseTest {

    @Test
    @DisplayName("WITHDRAWAL-001: a guest is redirected away from the withdrawal page")
    void guestCannotReachWithdrawalPage() {
        WithdrawalPage withdrawal = openGuestWithdrawal();
        page().waitForTimeout(2_000);

        assertTrue(withdrawal.redirectedAway(),
                "The withdrawal page was served to a guest. Current URL: " + page().url());
    }

    @Test
    @DisplayName("WITHDRAWAL-002: a valid withdrawal at the stated minimum reaches a confirmation prompt")
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
    @DisplayName("WITHDRAWAL-003: cancelling the confirmation prompt withdraws nothing and keeps the amount")
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
    @DisplayName("WITHDRAWAL-004: an amount below the page's stated minimum is refused on submit")
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
    @DisplayName("WITHDRAWAL-005: an amount above the page's stated maximum is refused on submit")
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
    @DisplayName("WITHDRAWAL-006: no currently offered withdraw option accepts an amount below its own stated minimum")
    void noOptionAcceptsBelowItsOwnMinimum() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        List<String> offenders = optionsAcceptingOutOfRange(withdrawal, true);
        assertTrue(offenders.isEmpty(), "WITHDRAWAL-006: " + offenders);
    }

    @Test
    @DisplayName("WITHDRAWAL-007: no currently offered withdraw option accepts an amount above its own stated maximum")
    void noOptionAcceptsAboveItsOwnMaximum() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        List<String> offenders = optionsAcceptingOutOfRange(withdrawal, false);
        assertTrue(offenders.isEmpty(), "WITHDRAWAL-007: " + offenders);
    }

    @Test
    @DisplayName("WITHDRAWAL-008: submitting with the amount field empty is refused")
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
    @DisplayName("WITHDRAWAL-009: letters are not accepted as an amount")
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
    @DisplayName("WITHDRAWAL-010: exponent notation is not silently read as a bigger amount")
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
    @DisplayName("WITHDRAWAL-011: an amount above the balance, within the stated maximum, is refused rather than offered to confirm")
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
                "WITHDRAWAL-011: SUBMIT offered to confirm a withdrawal of " + overBalance
                        + " against a balance of " + balance + ". Popup said: '" + popup + "'");
    }

    @Test
    @DisplayName("WITHDRAWAL-012: withdraw option labels are unique")
    void withdrawOptionLabelsAreUnique() {
        WithdrawalPage withdrawal = openMemberWithdrawal();
        List<String> labels = withdrawal.optionLabels();
        List<String> duplicates = labels.stream()
                .filter(l -> java.util.Collections.frequency(labels, l) > 1)
                .distinct()
                .toList();

        assertTrue(duplicates.isEmpty(), "WITHDRAWAL-012: duplicated withdraw option labels: "
                + duplicates);
    }

    // ----------------------------------------------------------------- setup

    /** Opens the home page as a fresh visitor, answers the picker with New Interface, confirms it
     *  stuck, then opens the withdrawal page without signing in - the one guest-only case. */
    private WithdrawalPage openGuestWithdrawal() {
        Page on = page();
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();
        assertTrue(home.isNewUiChosen(), "Expected New Interface to be the chosen UI version - "
                + "ui-version is \"" + home.uiVersion() + "\"");

        WithdrawalPage withdrawal = new WithdrawalPage(on);
        withdrawal.open();
        return withdrawal;
    }

    /** As {@link #openGuestWithdrawal()}, then signs the shared QA account in, returns to the
     *  home page, confirms both the signed-in state and the UI choice held, then opens the
     *  withdrawal page. Mirrors {@link org.sengmean.tests.layout.ResponsiveLayoutNewUiTest}'s
     *  member walk. */
    private WithdrawalPage openMemberWithdrawal() {
        Page on = page();
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();
        assertTrue(home.isNewUiChosen(), "Expected New Interface to be the chosen UI version - "
                + "ui-version is \"" + home.uiVersion() + "\"");

        signIn(on);

        home.navigateTo();
        home.dismissOverlays();
        boolean signedIn = new LoginPage(on).isSignedIn();
        CaseLog.data("Signed in", String.valueOf(signedIn));
        assertTrue(signedIn, "Expected the header to show the signed-in state after returning to "
                + "the home page.");
        assertTrue(home.isNewUiChosen(), "Expected New Interface to still be the chosen UI "
                + "version after signing in - ui-version is \"" + home.uiVersion() + "\"");

        WithdrawalPage withdrawal = new WithdrawalPage(on);
        withdrawal.open();
        assertTrue(withdrawal.isLoaded(), "The withdrawal form did not render for " + USERNAME
                + ". Current URL: " + on.url());
        return withdrawal;
    }
}
