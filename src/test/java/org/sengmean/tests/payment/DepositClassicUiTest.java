package org.sengmean.tests.payment;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.payment.DepositPage;
import org.sengmean.page.wallet.HomePage;
import org.sengmean.report.CaseLog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for the member deposit page, {@code /en/member/deposit/}, under Classic UI.
 *
 * <p>The other half of the split from {@link DepositNewUiTest} - see that class's Javadoc for
 * the full rationale (why the same case set runs under both UI versions even though the deposit
 * flow is not expected to differ), the exact id-to-NewWeibo-case mapping, and the full list of
 * NewWeibo_automation cases skipped outright because they name a Weibo Empire-specific channel,
 * gateway or wallet concept with no honest Quark equivalent.
 *
 * <h2>Case ids: DEPOSIT-011 - DEPOSIT-020</h2>
 * Continuing {@link DepositNewUiTest}'s {@code DEPOSIT-001}-{@code DEPOSIT-010} under the same
 * invented {@code DEPOSIT-} prefix, same order, same ten checks - just answering the UI-version
 * picker with Classic Interface instead of New Interface.
 */
public class DepositClassicUiTest extends BaseTest {

    @Test
    @DisplayName("DEPOSIT-011: a guest is redirected away from the deposit page (Classic UI)")
    void guestIsRedirectedAway() {
        openAsVisitorChoosingClassicUi();

        DepositPage guestView = new DepositPage(page());
        guestView.navigateTo();
        page().waitForTimeout(2_000);

        assertTrue(guestView.redirectedAway(),
                "The deposit page was served to a guest under Classic UI. Current URL: "
                        + page().url());
    }

    @Test
    @DisplayName("DEPOSIT-012: the deposit form renders for a signed-in member (Classic UI)")
    void depositFormRendersForMember() {
        DepositPage view = openDeposit();

        assertTrue(view.isLoaded(),
                "The deposit form did not render under Classic UI for a signed-in member. "
                        + "Current URL: " + page().url());
    }

    @Test
    @DisplayName("DEPOSIT-013: the deposit form offers at least one payment option (Classic UI)")
    void offersAtLeastOnePaymentOption() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());

        List<String> options = view.paymentOptionLabels();
        CaseLog.data("Payment options found", String.valueOf(options.size()));

        assertFalse(options.isEmpty(),
                "Expected the deposit form to offer at least one payment option under Classic "
                        + "UI; found none with DepositPage's own guessed option locator - see "
                        + "its Selector confidence note before treating this as a real defect.");
    }

    @Test
    @DisplayName("DEPOSIT-014: payment option labels are unique (Classic UI)")
    void paymentOptionLabelsAreUnique() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());

        List<String> labels = view.paymentOptionLabels().stream()
                .map(String::trim)
                .filter(label -> !label.isEmpty())
                .toList();
        Assumptions.assumeFalse(labels.isEmpty(),
                "No payment option labels were read, so there is nothing to check for "
                        + "duplicates - see DEPOSIT-013.");

        Set<String> seen = new HashSet<>();
        List<String> duplicates = labels.stream().filter(label -> !seen.add(label)).distinct().toList();

        assertTrue(duplicates.isEmpty(),
                "Duplicated payment option labels under Classic UI: " + duplicates);
    }

    @Test
    @DisplayName("DEPOSIT-015: selecting the first offered option renders an amount control (Classic UI)")
    void selectingAnOptionRendersAnAmountControl() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());

        String option = firstOption(view);

        boolean hasControl = view.hasFreeEntryAmount() || view.hasPresetAmount();
        assertTrue(hasControl, "'" + option + "' offered neither a free-entry amount field nor a "
                + "preset-amount hint under Classic UI, so a deposit cannot be started on it at all.");
    }

    @Test
    @DisplayName("DEPOSIT-016: the amount field carries a hint about its range, when it has one (Classic UI)")
    void amountFieldAdvertisesItsRangeWhenItHasOne() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());
        firstOption(view);
        Assumptions.assumeTrue(view.hasFreeEntryAmount(),
                "The first offered option has no free-entry amount field to advertise a range on.");

        String hint = view.amountPlaceholder();
        CaseLog.data("Amount field hint", hint);

        assertFalse(hint.isBlank(),
                "Expected the amount field to carry some hint about the range it accepts under "
                        + "Classic UI; found none. Generalised from the Weibo original's "
                        + "HKDDEPO-946 / -910, which expected the literal words \"Min\"/\"Max\" - "
                        + "only a non-blank hint is required here, since Quark's own wording is "
                        + "unconfirmed.");
    }

    @Test
    @DisplayName("DEPOSIT-017: submitting with the amount field empty is refused (Classic UI)")
    void emptyAmountIsRejected() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());
        firstOption(view);
        Assumptions.assumeTrue(view.hasFreeEntryAmount(),
                "The first offered option has no free-entry amount field to leave empty.");

        view.clearAmount();
        view.submit();
        boolean offeredToConfirm = view.isConfirmationPrompt();
        String popup = view.popupText();
        view.dismissPopup();

        assertFalse(offeredToConfirm,
                "SUBMIT offered to confirm a deposit with no amount under Classic UI. Popup "
                        + "said: '" + popup + "'");
    }

    @Test
    @DisplayName("DEPOSIT-018: an amount below the option's own advertised minimum is refused (Classic UI)")
    void amountBelowAdvertisedMinimumIsRejected() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());
        firstOption(view);
        Assumptions.assumeTrue(view.hasFreeEntryAmount(),
                "The first offered option has no free-entry amount field to test a minimum on.");

        double min = view.advertisedMinimum();
        Assumptions.assumeFalse(Double.isNaN(min),
                "The first offered option advertises no minimum to test against, so there is no "
                        + "rule here to check.");
        long belowMin = Math.max(0, (long) Math.floor(min) - 1);

        view.enterAmount(String.valueOf(belowMin));
        view.submit();
        boolean offeredToConfirm = view.isConfirmationPrompt();
        String popup = view.popupText();
        view.dismissPopup();

        assertFalse(offeredToConfirm,
                belowMin + " is below the advertised minimum of " + min + " under Classic UI; "
                        + "SUBMIT should not have offered to confirm it. Popup said: '" + popup + "'");
    }

    @Test
    @DisplayName("DEPOSIT-019: an amount above the option's own advertised maximum is refused (Classic UI)")
    void amountAboveAdvertisedMaximumIsRejected() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());
        firstOption(view);
        Assumptions.assumeTrue(view.hasFreeEntryAmount(),
                "The first offered option has no free-entry amount field to test a maximum on.");

        double max = view.advertisedMaximum();
        Assumptions.assumeFalse(Double.isNaN(max),
                "The first offered option advertises no maximum to test against, so there is no "
                        + "rule here to check.");
        long aboveMax = (long) Math.ceil(max) + 1;

        view.enterAmount(String.valueOf(aboveMax));
        view.submit();
        boolean offeredToConfirm = view.isConfirmationPrompt();
        String popup = view.popupText();
        view.dismissPopup();

        assertFalse(offeredToConfirm,
                aboveMax + " is above the advertised maximum of " + max + " under Classic UI; "
                        + "SUBMIT should not have offered to confirm it. Popup said: '" + popup + "'");
    }

    @Test
    @DisplayName("DEPOSIT-020: the option's own advertised minimum reaches a confirmation prompt, "
            + "which is cancelled without depositing (Classic UI)")
    void validMinimumReachesConfirmationAndIsCancelled() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());
        firstOption(view);
        Assumptions.assumeTrue(view.hasFreeEntryAmount(),
                "The first offered option has no free-entry amount field to submit a valid amount on.");

        double min = view.advertisedMinimum();
        Assumptions.assumeFalse(Double.isNaN(min),
                "The first offered option advertises no minimum, so there is no amount here "
                        + "that is known to be valid.");
        String amount = String.valueOf((long) Math.ceil(min));

        view.enterAmount(amount);
        view.submit();
        boolean offeredToConfirm = view.isConfirmationPrompt();
        String popup = view.popupText();

        // Cancelled either way, and Confirm is never pressed - see DepositPage.confirm()'s own
        // Javadoc for why this port leaves that control entirely unexercised.
        if (offeredToConfirm) {
            view.cancelConfirmation();
        } else {
            view.dismissPopup();
        }

        assertTrue(offeredToConfirm,
                "Submitting the option's own advertised minimum (" + amount + ") under Classic "
                        + "UI did not reach a confirmation prompt. Popup said: '" + popup + "'. "
                        + "Nothing was confirmed either way.");
    }

    // ----------------------------------------------------------------- helpers

    /** Opens the home page as a fresh visitor, answers the picker with Classic Interface, and
     *  asserts it stuck. */
    private Page openAsVisitorChoosingClassicUi() {
        Page on = page();
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseClassicUi();
        home.dismissOverlays();

        assertTrue(home.isClassicUiChosen(),
                "Expected Classic Interface to be the chosen UI version - ui-version is \""
                        + home.uiVersion() + "\"");
        return on;
    }

    /** The visitor walk, then signed in, then back on the home page with the choice re-confirmed,
     *  then navigated to the deposit page. Mirrors {@code ResponsiveLayoutClassicUiTest}'s
     *  member walk. */
    private DepositPage openDeposit() {
        Page on = openAsVisitorChoosingClassicUi();
        signIn(on);

        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();

        boolean signedIn = new LoginPage(on).isSignedIn();
        CaseLog.data("Signed in", String.valueOf(signedIn));
        assertTrue(signedIn,
                "Expected the header to still show the signed-in state after returning to the "
                        + "home page under Classic UI.");
        assertTrue(home.isClassicUiChosen(),
                "Expected Classic Interface to still be the chosen UI version after signing in "
                        + "- ui-version is \"" + home.uiVersion() + "\"");

        DepositPage deposit = new DepositPage(on);
        deposit.navigateTo();
        return deposit;
    }

    /**
     * The first offered option that does not need an unbound account to submit on - selected as
     * a side effect, so callers must not re-select it - falling back to the first option at all
     * when every one of them needs one. See {@link DepositPage}'s "first-offered option can
     * require an unbound account" Javadoc note - confirmed live 2026-10-24: the first row on this
     * page's own Deposit Options list, UMPAY PP EN, needs a UMPay account this QA account does
     * not have, while Bank transfer PP/6P already have a bank account bound.
     */
    private String firstOption(DepositPage view) {
        List<String> options = view.paymentOptionLabels();
        Assumptions.assumeFalse(options.isEmpty(),
                "No payment option is offered to select - see DEPOSIT-013.");
        for (String option : options) {
            view.selectPaymentOption(option);
            if (!view.currentOptionNeedsBinding()) {
                return option;
            }
        }
        return options.get(0);
    }

    private String notRenderedMessage() {
        return "The deposit form did not render under Classic UI - DEPOSIT-012 covers that "
                + "render check on its own; skipping this case rather than reporting a false "
                + "specific-behaviour failure.";
    }
}
