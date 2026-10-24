package org.sengmean.tests.payment;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.payment.WithdrawalPage;
import org.sengmean.page.wallet.HomePage;
import org.sengmean.report.CaseLog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for the withdrawal form's own account-binding validation - see the sibling
 * {@link DepositBankBindingValidationTest}'s Javadoc for the full rationale (why a second
 * account, {@link BaseTest#ALT_USERNAME}, was needed; what this deliberately does not test).
 * Confirmed live 2026-10-24 against test-v2.138hk.vip (New UI, qa000051): the default-selected
 * option, UMPAY PP EN, refuses SUBMIT with "UMPay Account is required"; Bank transfer PP refuses
 * with "Bank Name is required"; a USDT option refuses with "Wallet address is required" - the
 * same "&lt;field&gt; is required" shape {@link org.sengmean.page.payment.DepositPage} shows.
 *
 * <h2>Covers every channel and option the live page actually offers, under HKD</h2>
 * Same approach as {@link DepositBankBindingValidationTest}, walking
 * {@link WithdrawalPage#channelLabels()} (the "Withdrawal Channel" tiles - confirmed live
 * 2026-10-24: the same eight channels the deposit page offers under HKD, NEW, AI_PAY, 6pay,
 * Channel, USDT, WPay, USDT(UPAY), UMPay, though the Withdraw Options each one lists are not
 * identical to Deposit's own - e.g. "Visa Gift Card" and "Jockey Club Cash Vouchers" appear only
 * here) and, for each, every row {@link WithdrawalPage#optionLabels()} offers - nothing
 * hard-coded, so a channel or option added, removed or renamed later is picked up automatically.
 * "6pay" offered zero options for this account that day and is skipped, as is any option -
 * cash-style ones like "ATM Machine" confirmed live - that renders no account/wallet field to
 * bind at all.
 *
 * <p><b>"Every channel and option" means HKD's own set specifically</b> - see
 * {@link DepositBankBindingValidationTest}'s own Javadoc note: the channel/option list is a
 * completely different, smaller set under another currency, confirmed live 2026-10-25.
 * {@link #openWithdrawal(boolean)} pins the account to HKD before reading anything.
 *
 * <h2>Case ids</h2>
 * {@code WITHDRAWALBIND-001} (New UI) and {@code WITHDRAWALBIND-002} (Classic UI) - see
 * {@link DepositBankBindingValidationTest}'s own "Case ids" Javadoc for why both UI versions are
 * exercised in this one file rather than split into sibling classes, and for the channel-tile
 * bug (New UI only at first, confirmed live to miss every tile on Classic UI) that is exactly why
 * this case needed a Classic UI half at all.
 */
public class WithdrawalBankBindingValidationTest extends BaseTest {

    private static final Pattern REQUIRED_FIELD_MESSAGE =
            Pattern.compile("is required", Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("WITHDRAWALBIND-001: every withdrawal channel's own options needing an unbound account refuse SUBMIT naming the missing field (New UI)")
    void everyUnboundOptionAcrossEveryChannelNamesItsOwnMissingFieldNewUi() {
        sweepChannelsAndOptions(openWithdrawal(true), "WITHDRAWALBIND-001");
    }

    @Test
    @DisplayName("WITHDRAWALBIND-002: every withdrawal channel's own options needing an unbound account refuse SUBMIT naming the missing field (Classic UI)")
    void everyUnboundOptionAcrossEveryChannelNamesItsOwnMissingFieldClassicUi() {
        sweepChannelsAndOptions(openWithdrawal(false), "WITHDRAWALBIND-002");
    }

    private void sweepChannelsAndOptions(WithdrawalPage withdrawal, String caseId) {
        List<String> channels = withdrawal.channelLabels();
        Assumptions.assumeFalse(channels.isEmpty(),
                "No withdrawal channel is offered - nothing to check binding on.");

        List<String> checked = new ArrayList<>();
        List<String> offenders = new ArrayList<>();
        for (String channel : channels) {
            withdrawal.selectChannel(channel);
            List<String> options = withdrawal.optionLabels();
            if (options.isEmpty()) {
                CaseLog.step("Channel '" + channel + "' offers no withdraw option for "
                        + ALT_USERNAME + " - skipping.");
                continue;
            }

            for (String option : options) {
                withdrawal.selectOption(option);
                if (!withdrawal.currentOptionNeedsBinding()) {
                    CaseLog.step("'" + channel + "' / '" + option + "' needs no binding (already "
                            + "bound, or not an account-based option) for " + ALT_USERNAME
                            + " - skipping.");
                    continue;
                }
                if (!withdrawal.hasFreeEntryAmount()) {
                    continue;
                }
                double min = withdrawal.statedMinimum();
                if (Double.isNaN(min)) {
                    continue;
                }

                String amount = String.valueOf((long) Math.ceil(min));
                withdrawal.enterAmount(amount);
                withdrawal.submit();
                String popup = withdrawal.popupText();
                checked.add(channel + " / " + option + " -> '" + popup + "'");

                if (withdrawal.isConfirmationPrompt()
                        || !REQUIRED_FIELD_MESSAGE.matcher(popup).find()) {
                    offenders.add(channel + " / " + option + ": expected a '<field> is "
                            + "required' refusal, got '" + popup + "'");
                }
                withdrawal.dismissPopup();
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

    /** As {@link WithdrawalNewUiTest#openMemberWithdrawal()}/{@link WithdrawalClassicUiTest}'s
     *  own helper, but signed in as {@link BaseTest#ALT_USERNAME} rather than the shared
     *  {@code USERNAME}, and without that class's {@code selectUsableOption} step - every option
     *  is expected to need binding here. */
    private WithdrawalPage openWithdrawal(boolean newUi) {
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

        WithdrawalPage withdrawal = new WithdrawalPage(on);
        withdrawal.open();
        assertTrue(withdrawal.isLoaded(), "The withdrawal form did not render for " + ALT_USERNAME
                + ". Current URL: " + on.url());
        return withdrawal;
    }
}
