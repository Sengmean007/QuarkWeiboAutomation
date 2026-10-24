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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for the deposit form's own account-binding validation - the "&lt;field&gt; is
 * required" refusal an option with no bound payment account shows on SUBMIT ("UMPay Account is
 * required", "Bank Name is required", "Wallet address is required", confirmed live for three
 * different field shapes - see below). Confirmed live 2026-10-24 against test-v2.138hk.vip (New
 * UI, qa000051).
 *
 * <h2>Why a second account</h2>
 * {@link DepositNewUiTest} / {@link DepositClassicUiTest} run as {@code USERNAME} (qa00002),
 * which already has a Bank of China account bound for Bank transfer PP/6P - see
 * {@link DepositPage}'s "first-offered option can require an unbound account" Javadoc note. That
 * made it impossible to exercise this validation path at all: every option those suites' own
 * {@code firstOption} helper could reach already had something bound. {@link BaseTest#ALT_USERNAME}
 * (qa000051, given by Si on 2026-10-24) has nothing bound anywhere - confirmed live: HKD 0.00,
 * and every option's own account field still read "Click [Add] to bind Banking details" - exactly
 * the state this validation needs to exercise.
 *
 * <h2>Covers every channel and option the live page actually offers, under HKD</h2>
 * This walks {@link DepositPage#channelLabels()} (the "Deposit Channel" tiles - NEW, AI_PAY,
 * 6pay, Channel, USDT, WPay, USDT(UPAY), UMPay under HKD, confirmed live 2026-10-24) and, for
 * each, every row {@link DepositPage#paymentOptionLabels()} offers once that channel is selected
 * - up to 17 options on a single channel (AI_PAY) that day, none of them hard-coded here: a
 * channel or option added, removed or renamed on a later run is picked up automatically, exactly
 * the run-time-read approach the rest of this project already takes with minimums/maximums.
 *
 * <p><b>"Every channel and option" means HKD's own set specifically</b> - confirmed live
 * 2026-10-25 that the Deposit Channel/Option list is not a constant the account's active currency
 * happens to decorate; it is a completely different, and much smaller, set under another
 * currency (qa00002 under USDT saw only 3 channels - USDT, WPay, PLAYPLAY - against HKD's 8).
 * {@link #openDeposit(boolean)} pins the account to HKD before reading anything, for exactly this
 * reason - see its own comment and {@code quark-currency-defaults-to-hkd}.
 * <ul>
 *   <li>A channel that offers no option at all for this account (confirmed live: "6pay" offered
 *       zero that day) is skipped, not failed - the "Deposit Options" heading itself does not
 *       render, so there is nothing to check binding on.</li>
 *   <li>An option that does not need binding - confirmed live for several of AI_PAY/Channel's
 *       cash-style options ("ATM Machine", "Octopus Pay", "Cash Payment", "Convenience store cash
 *       recharge" and similar: no account/wallet field renders at all for them - is skipped, the
 *       same way an already-bound option is.</li>
 *   <li>An option that does need binding is submitted at its own advertised minimum and must
 *       refuse with a message matching {@code /is required/i}, whatever field it names - a crypto
 *       option's "Wallet address is required" generalizes exactly as well as a bank option's
 *       "Bank Name is required" under this one pattern, confirmed live for both plus a UMPay
 *       account field.</li>
 * </ul>
 *
 * <h2>What this does not test</h2>
 * Never presses the "Add" control to actually bind an account - that would be real account
 * mutation against a shared QA account, outside this case's purpose, and it is not clear from the
 * live DOM alone what a safe test value for every field (UMPay Account, Bank Name, Bank User,
 * Bank Number, FPS Account, Wallet Address) would be. Only the refusal itself is asserted.
 *
 * <h2>Case ids</h2>
 * {@code DEPOSITBIND-001} (New UI) and {@code DEPOSITBIND-002} (Classic UI), a separate prefix
 * from {@code DEPOSIT-}/{@code DEPOSIT-01x} since this concern (account binding state) is
 * otherwise orthogonal to the New UI / Classic UI split those cases cover. Both UI versions are
 * exercised here, in one file, rather than split into sibling {@code NewUiTest}/
 * {@code ClassicUiTest} classes the way {@link DepositNewUiTest}/{@link DepositClassicUiTest} are -
 * this is one narrow concern, not a full suite, so one shared sweep method with two thin setup
 * helpers was simpler than duplicating the whole file. <b>This split earned its keep</b>: the
 * channel-tile locator ({@link DepositPage#channelLabels()}/{@code selectChannel}) was written
 * and confirmed against New UI only at first, then found live the same day to match <i>zero</i>
 * channel tiles on Classic UI - its grid container renders a different, UI-version-specific
 * Tailwind column-count class - before this Classic UI case existed to catch it. See
 * {@link DepositPage}'s {@code CHANNEL_TILE} Javadoc for the fix.
 */
public class DepositBankBindingValidationTest extends BaseTest {

    private static final Pattern REQUIRED_FIELD_MESSAGE =
            Pattern.compile("is required", Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("DEPOSITBIND-001: every deposit channel's own options needing an unbound account refuse SUBMIT naming the missing field (New UI)")
    void everyUnboundOptionAcrossEveryChannelNamesItsOwnMissingFieldNewUi() {
        sweepChannelsAndOptions(openDeposit(true), "DEPOSITBIND-001");
    }

    @Test
    @DisplayName("DEPOSITBIND-002: every deposit channel's own options needing an unbound account refuse SUBMIT naming the missing field (Classic UI)")
    void everyUnboundOptionAcrossEveryChannelNamesItsOwnMissingFieldClassicUi() {
        sweepChannelsAndOptions(openDeposit(false), "DEPOSITBIND-002");
    }

    private void sweepChannelsAndOptions(DepositPage view, String caseId) {
        List<String> channels = view.channelLabels();
        Assumptions.assumeFalse(channels.isEmpty(),
                "No deposit channel is offered - nothing to check binding on.");

        List<String> checked = new ArrayList<>();
        List<String> offenders = new ArrayList<>();
        for (String channel : channels) {
            view.selectChannel(channel);
            List<String> options = view.paymentOptionLabels();
            if (options.isEmpty()) {
                CaseLog.step("Channel '" + channel + "' offers no deposit option for "
                        + ALT_USERNAME + " - skipping.");
                continue;
            }

            for (String option : options) {
                view.selectPaymentOption(option);
                if (!view.currentOptionNeedsBinding()) {
                    CaseLog.step("'" + channel + "' / '" + option + "' needs no binding (already "
                            + "bound, or not an account-based option) for " + ALT_USERNAME
                            + " - skipping.");
                    continue;
                }
                if (!view.hasFreeEntryAmount()) {
                    continue;
                }
                double min = view.advertisedMinimum();
                if (Double.isNaN(min)) {
                    continue;
                }

                String amount = String.valueOf((long) Math.ceil(min));
                view.enterAmount(amount);
                view.submit();
                String popup = view.popupText();
                checked.add(channel + " / " + option + " -> '" + popup + "'");

                if (view.isConfirmationPrompt() || !REQUIRED_FIELD_MESSAGE.matcher(popup).find()) {
                    offenders.add(channel + " / " + option + ": expected a '<field> is "
                            + "required' refusal, got '" + popup + "'");
                }
                view.dismissPopup();
            }
        }
        CaseLog.data("Channel/option combinations checked", String.valueOf(checked.size()));
        CaseLog.data("Combinations checked", String.valueOf(checked));
        Assumptions.assumeFalse(checked.isEmpty(),
                "No channel offered an option that both needed a binding and had a readable "
                        + "amount to submit.");

        assertTrue(offenders.isEmpty(), caseId + ": " + offenders);
    }

    // ----------------------------------------------------------------- setup

    /** As {@link DepositNewUiTest#openDeposit()}/{@link DepositClassicUiTest}'s own helper, but
     *  signed in as {@link BaseTest#ALT_USERNAME} rather than the shared {@code USERNAME}. */
    private DepositPage openDeposit(boolean newUi) {
        Page on = page();
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        if (newUi) {
            home.chooseNewUi();
        } else {
            home.chooseClassicUi();
        }
        home.dismissOverlays();
        boolean chosen = newUi ? home.isNewUiChosen() : home.isClassicUiChosen();
        assertTrue(chosen, "Expected " + (newUi ? "New" : "Classic") + " Interface to be the "
                + "chosen UI version - ui-version is \"" + home.uiVersion() + "\"");

        signIn(on, ALT_USERNAME, ALT_PASSWORD);

        home.navigateTo();
        home.dismissOverlays();
        boolean signedIn = new LoginPage(on).isSignedIn();
        CaseLog.data("Signed in", String.valueOf(signedIn));
        assertTrue(signedIn, "Expected the header to show the signed-in state for " + ALT_USERNAME
                + " after returning to the home page.");
        chosen = newUi ? home.isNewUiChosen() : home.isClassicUiChosen();
        assertTrue(chosen, "Expected " + (newUi ? "New" : "Classic") + " Interface to still be "
                + "the chosen UI version after signing in - ui-version is \"" + home.uiVersion()
                + "\"");

        DepositPage deposit = new DepositPage(on);
        deposit.navigateTo();
        assertTrue(deposit.isLoaded(), "The deposit form did not render for " + ALT_USERNAME
                + ". Current URL: " + on.url());
        return deposit;
    }
}
