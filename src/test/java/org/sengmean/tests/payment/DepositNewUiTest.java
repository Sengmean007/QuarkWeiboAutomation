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
 * Coverage for the member deposit page, {@code /en/member/deposit/}, under New UI.
 *
 * <p>Ported from the sibling NewWeibo_automation project's {@code DepositPageTest} (68 cases
 * against Weibo Empire's own deposit page). That page runs on a completely different
 * configuration - named payment channels ("6Pay-Bank", "WPay", "UMPay", "USDT(UPAY)"), a
 * game-points/turnover wallet model, per-channel minimums the client's own workbook states
 * (100 for the fiat options, 20 for USDT(UPAY), 500 for WPay) - none of which can be assumed to
 * exist on Quark Dynasty's deposit page, which has never been visited by this project. Per the
 * porting brief, the literals were not guessed at; only the generic shape of each case survived,
 * with every bound (minimum, maximum, "has a hint at all") read from the page itself at run time
 * rather than hard-coded.
 *
 * <h2>Why this exists alongside {@link DepositClassicUiTest}</h2>
 * The deposit flow is not expected to render differently under New UI vs. Classic UI - nothing
 * read while porting this suggests otherwise - but that expectation has not been confirmed
 * either, and confirming it is the entire point of running the same case set twice. This mirrors
 * {@link org.sengmean.tests.layout.ResponsiveLayoutNewUiTest} /
 * {@code ResponsiveLayoutClassicUiTest}: two independent classes so a failure under one UI
 * version says nothing about the other.
 *
 * <h2>What every case does</h2>
 * {@link #openAsVisitorChoosingNewUi()} opens the home page as a fresh visitor, clears the
 * overlays, answers the UI-version picker with New Interface and asserts it stuck
 * ({@link org.sengmean.base.BasePage#isNewUiChosen()}). {@link #openDeposit()} builds on that:
 * signs the shared QA account in, returns to the home page, confirms the header still shows
 * signed in and that {@code ui-version} survived the sign-in round trip, then navigates to the
 * deposit page. Every case beyond {@code DEPOSIT-001} (the guest case, which deliberately never
 * signs in) goes through {@link #openDeposit()}.
 *
 * <h2>Case ids: DEPOSIT-001 - DEPOSIT-010 here, continuing at DEPOSIT-011 in
 * {@link DepositClassicUiTest}</h2>
 * Invented for this project, as there is no client test-case workbook to take ids from (same
 * situation as the {@code UI-} prefix).
 * <table border="1">
 * <caption>Mapping from the NewWeibo_automation case(s) each id generalises</caption>
 * <tr><th>Quark id</th><th>What it checks</th><th>Generalised from (NewWeibo_automation)</th></tr>
 * <tr><td>DEPOSIT-001</td><td>A guest is redirected away from the deposit page</td>
 *     <td>DEPOACCESS-001 / HKDDEPO-944 - page access, no literal to generalise</td></tr>
 * <tr><td>DEPOSIT-002</td><td>The deposit form renders for a signed-in member</td>
 *     <td>No direct analogue - added because the UI-split's whole purpose is confirming the
 *         page isn't broken under this UI version at all</td></tr>
 * <tr><td>DEPOSIT-003</td><td>At least one payment option is offered</td>
 *     <td>DEPOACCESS-002 / HKDDEPO-945, narrowed from "all five named channels" (Channel, WPay,
 *         USDT(UPAY), UMPay, USDT) to "at least one option", since Quark's own channel list -
 *         if it has one at all - is unknown</td></tr>
 * <tr><td>DEPOSIT-004</td><td>Payment option labels are unique</td>
 *     <td>DEPOACCESS-005 / HKDDEPO-948 (DEP-13) - structural check, no literal to generalise</td></tr>
 * <tr><td>DEPOSIT-005</td><td>Selecting the first offered option renders an amount control</td>
 *     <td>The whole {@code PaymentChannelOptions} group (DEP-001..015 / HKDDEPO-913..927, 15
 *         cases, one per named channel) collapsed to one case against whichever option the page
 *         actually offers first, since the 15 channel names cannot be assumed</td></tr>
 * <tr><td>DEPOSIT-006</td><td>The amount field carries some hint about its range, when it has a
 *     free-entry field at all</td>
 *     <td>DEPOACCESS-003 / HKDDEPO-946 and DEPINVALID-08 / HKDDEPO-910, loosened from "the hint
 *         contains the word Min/Max" to "the hint is non-blank", since the real wording is
 *         unconfirmed</td></tr>
 * <tr><td>DEPOSIT-007</td><td>Submitting with the amount field empty is refused</td>
 *     <td>DEPINVALID-04 / HKDDEPO-002 - fully generic, no literal involved</td></tr>
 * <tr><td>DEPOSIT-008</td><td>An amount one below the option's own advertised minimum is
 *     refused</td>
 *     <td>DEPINVALID-01 / HKDDEPO-005 and the sweep DEPINVALID-09 / HKDDEPO-911, narrowed from
 *         "every option on the page" to "the first offered option", and with the bound read live
 *         from the field rather than Weibo's constant 100</td></tr>
 * <tr><td>DEPOSIT-009</td><td>An amount one above the option's own advertised maximum is
 *     refused</td>
 *     <td>DEPINVALID-02 / HKDDEPO-006 and the sweep DEPINVALID-10 / HKDDEPO-912, same narrowing
 *         and live-read bound as DEPOSIT-008</td></tr>
 * <tr><td>DEPOSIT-010</td><td>Submitting the option's own advertised minimum reaches a
 *     confirmation prompt, which is cancelled - never confirmed</td>
 *     <td>DEPVALID-01 / HKDDEPO-003 and DEPVALID-05 / HKDDEPO-904, with the amount read from the
 *         field instead of Weibo's constant 100</td></tr>
 * </table>
 *
 * <h2>NewWeibo_automation cases skipped outright</h2>
 * Everything below names a specific Weibo Empire channel, a specific gateway, a specific wallet
 * concept, or commits a real deposit, with no honest Quark equivalent to generalise to:
 * <ul>
 *   <li><b>{@code formStatesTotalAndTurnover} (HKDDEPO-901) and
 *       {@code summaryIsWithheldForAnOutOfRangeAmount} (HKDDEPO-906)</b> - built entirely on
 *       Weibo's "game points" / "turnover requirement" wallet model, which there is no reason to
 *       assume Quark shares at all.</li>
 *   <li><b>{@code everyChannelOptionAcceptsValidAmount} (HKDDEPO-902) and
 *       {@code everyChannelAcceptsAValidDeposit} (HKDDEPO-903)</b> - named channels and
 *       per-channel minimums (Bank Transfer 100, WPay 500, USDT(UPAY) 20, UMPay 100, USDT 100).</li>
 *   <li><b>{@code confirmingADepositCreatesARecord} (HKDDEPO-905, {@code creates-data})</b> -
 *       files a real order and checks it against a {@code RecordPage} this project has no
 *       equivalent of; also exactly the real-money risk this port was told to design away from.</li>
 *   <li><b>{@code lettersAreRejected} (HKDDEPO-907)</b> - the case exists to document Weibo's own
 *       observed keystroke-filtering behaviour; nothing read here gives a basis to assume Quark's
 *       field filters input the same way, so asserting either outcome would be a guess dressed up
 *       as a finding.</li>
 *   <li><b>{@code negativeAmountIsNotSilentlyCorrected} (HKDDEPO-908) and
 *       {@code decimalAmountIsPreserved} (HKDDEPO-909)</b>, both {@code known-defect} - these
 *       encode specific defects observed on Weibo Empire's own field (a stripped minus sign, "."
 *       silently dropped turning 150.50 into 15050); there is nothing to say Quark's field
 *       behaves the same way, buggy or not.</li>
 *   <li><b>The whole {@code RequiredFields} group</b> (6 cases: Bank Name, FPS Account, Alipay
 *       Account, UMPay Account, crypto Wallet address, 6Pay-Bank Account) - every one is a named
 *       channel's own second field, exercised against a second, unbound test account
 *       ({@code ALT_USERNAME}) this project has no reason to assume it needs or has.</li>
 *   <li><b>The whole {@code WPayDeposits} group</b> (10 cases, one per WPay currency,
 *       {@code creates-data}) - a single named gateway fronting ten currencies that may not
 *       exist on Quark at all, and each case commits a real deposit.</li>
 *   <li><b>The whole {@code ConfirmedDeposits} group</b> (16 cases, {@code creates-data}) - one
 *       real, confirmed deposit per named channel, capturing whichever payment gateway tab it
 *       hands off to.</li>
 *   <li><b>{@code boundWalletShowsOnFirstRender} (HKDDEPO-947, known-defect)</b> - a timing
 *       defect specific to Weibo's UMPay channel re-rendering its bound-account state.</li>
 * </ul>
 */
public class DepositNewUiTest extends BaseTest {

    @Test
    @DisplayName("DEPOSIT-001: a guest is redirected away from the deposit page (New UI)")
    void guestIsRedirectedAway() {
        openAsVisitorChoosingNewUi();

        DepositPage guestView = new DepositPage(page());
        guestView.navigateTo();
        page().waitForTimeout(2_000);

        assertTrue(guestView.redirectedAway(),
                "The deposit page was served to a guest under New UI. Current URL: " + page().url());
    }

    @Test
    @DisplayName("DEPOSIT-002: the deposit form renders for a signed-in member (New UI)")
    void depositFormRendersForMember() {
        DepositPage view = openDeposit();

        assertTrue(view.isLoaded(),
                "The deposit form did not render under New UI for a signed-in member. Current "
                        + "URL: " + page().url());
    }

    @Test
    @DisplayName("DEPOSIT-003: the deposit form offers at least one payment option (New UI)")
    void offersAtLeastOnePaymentOption() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());

        List<String> options = view.paymentOptionLabels();
        CaseLog.data("Payment options found", String.valueOf(options.size()));

        assertFalse(options.isEmpty(),
                "Expected the deposit form to offer at least one payment option under New UI; "
                        + "found none with DepositPage's own guessed option locator - see its "
                        + "Selector confidence note before treating this as a real defect.");
    }

    @Test
    @DisplayName("DEPOSIT-004: payment option labels are unique (New UI)")
    void paymentOptionLabelsAreUnique() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());

        List<String> labels = view.paymentOptionLabels().stream()
                .map(String::trim)
                .filter(label -> !label.isEmpty())
                .toList();
        Assumptions.assumeFalse(labels.isEmpty(),
                "No payment option labels were read, so there is nothing to check for "
                        + "duplicates - see DEPOSIT-003.");

        Set<String> seen = new HashSet<>();
        List<String> duplicates = labels.stream().filter(label -> !seen.add(label)).distinct().toList();

        assertTrue(duplicates.isEmpty(),
                "Duplicated payment option labels under New UI: " + duplicates);
    }

    @Test
    @DisplayName("DEPOSIT-005: selecting the first offered option renders an amount control (New UI)")
    void selectingAnOptionRendersAnAmountControl() {
        DepositPage view = openDeposit();
        Assumptions.assumeTrue(view.isLoaded(), notRenderedMessage());

        String option = firstOption(view);

        boolean hasControl = view.hasFreeEntryAmount() || view.hasPresetAmount();
        assertTrue(hasControl, "'" + option + "' offered neither a free-entry amount field nor a "
                + "preset-amount hint under New UI, so a deposit cannot be started on it at all.");
    }

    @Test
    @DisplayName("DEPOSIT-006: the amount field carries a hint about its range, when it has one (New UI)")
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
                        + "New UI; found none. Generalised from the Weibo original's HKDDEPO-946 "
                        + "/ -910, which expected the literal words \"Min\"/\"Max\" - only a "
                        + "non-blank hint is required here, since Quark's own wording is unconfirmed.");
    }

    @Test
    @DisplayName("DEPOSIT-007: submitting with the amount field empty is refused (New UI)")
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
                "SUBMIT offered to confirm a deposit with no amount under New UI. Popup said: '"
                        + popup + "'");
    }

    @Test
    @DisplayName("DEPOSIT-008: an amount below the option's own advertised minimum is refused (New UI)")
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
                belowMin + " is below the advertised minimum of " + min + " under New UI; SUBMIT "
                        + "should not have offered to confirm it. Popup said: '" + popup + "'");
    }

    @Test
    @DisplayName("DEPOSIT-009: an amount above the option's own advertised maximum is refused (New UI)")
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
                aboveMax + " is above the advertised maximum of " + max + " under New UI; SUBMIT "
                        + "should not have offered to confirm it. Popup said: '" + popup + "'");
    }

    @Test
    @DisplayName("DEPOSIT-010: the option's own advertised minimum reaches a confirmation prompt, "
            + "which is cancelled without depositing (New UI)")
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
                "Submitting the option's own advertised minimum (" + amount + ") under New UI "
                        + "did not reach a confirmation prompt. Popup said: '" + popup + "'. "
                        + "Nothing was confirmed either way.");
    }

    // ----------------------------------------------------------------- helpers

    /** Opens the home page as a fresh visitor, answers the picker with New Interface, and
     *  asserts it stuck. */
    private Page openAsVisitorChoosingNewUi() {
        Page on = page();
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();

        assertTrue(home.isNewUiChosen(),
                "Expected New Interface to be the chosen UI version - ui-version is \""
                        + home.uiVersion() + "\"");
        return on;
    }

    /** The visitor walk, then signed in, then back on the home page with the choice re-confirmed,
     *  then navigated to the deposit page. Mirrors {@code ResponsiveLayoutNewUiTest}'s member walk. */
    private DepositPage openDeposit() {
        Page on = openAsVisitorChoosingNewUi();
        signIn(on);

        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();

        boolean signedIn = new LoginPage(on).isSignedIn();
        CaseLog.data("Signed in", String.valueOf(signedIn));
        assertTrue(signedIn,
                "Expected the header to still show the signed-in state after returning to the "
                        + "home page under New UI.");
        assertTrue(home.isNewUiChosen(),
                "Expected New Interface to still be the chosen UI version after signing in - "
                        + "ui-version is \"" + home.uiVersion() + "\"");

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
                "No payment option is offered to select - see DEPOSIT-003.");
        for (String option : options) {
            view.selectPaymentOption(option);
            if (!view.currentOptionNeedsBinding()) {
                return option;
            }
        }
        return options.get(0);
    }

    private String notRenderedMessage() {
        return "The deposit form did not render under New UI - DEPOSIT-002 covers that render "
                + "check on its own; skipping this case rather than reporting a false "
                + "specific-behaviour failure.";
    }
}
