package org.sengmean.page.payment;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import org.sengmean.base.BasePage;
import org.sengmean.report.CaseLog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page Object for the member deposit page, {@code /en/member/deposit/}.
 *
 * <p>Ported from the sibling NewWeibo_automation project's {@code DepositPage} (same shape:
 * a page reachable only while signed in, a list of payment options, a free-entry or preset
 * amount control, a SUBMIT that opens some kind of confirmation before anything is committed).
 * That project's page object - and the 68-case test class built on it - is written entirely
 * against Weibo Empire's own configuration: named channels ("6Pay-Bank", "WPay", "UMPay",
 * "USDT(UPAY)"), a game-points/turnover wallet model, and specific minimum/maximum figures
 * read live off that site. None of that can be assumed here. Quark Dynasty's deposit page has
 * not been visited at all, so <b>every locator below is a guess</b>, written ahead of the live
 * UI the same way this project's other page objects were (see {@code LoginPage}, {@code
 * CurrencySwitcher}, {@code HomePage} - and the project's {@code tests-written-ahead-of-ui}
 * convention).
 *
 * <h2>Selector confidence - confirmed live 2026-10-24 against test-v2.138hk.vip</h2>
 * Driven manually (both New UI and Classic UI, signed in as qa00002):
 * <ul>
 *   <li>Quark's deposit page <b>does</b> have the same Channel/Option split Weibo has: a row of
 *       "Deposit Channel" tiles (NEW, AI_PAY, 6pay, Channel, USDT, WPay, USDT(UPAY), UMPay) above
 *       a "Deposit Options" list (e.g. UMPAY PP EN, Bank transfer PP, Bank transfer 6P, FPS PP,
 *       FPS 6P) - the options list is what this class treats as "payment option", since selecting
 *       a row there is what reveals the amount field and any required account-binding field.</li>
 *   <li>{@link #amountField()} - the page labels this "Game Points", not "Amount", so
 *       {@code fieldNear("Amount")} correctly finds nothing and every case runs on the
 *       {@code input[type='number']} fallback. Its placeholder reads e.g.
 *       "Min : 100.00 ~ Max : 5000000.00", which {@link #advertisedLimit} already parses.</li>
 *   <li>{@link #PAYMENT_OPTION} - was a guess that matched <b>zero</b> elements live (no class on
 *       this page names "payment", "option", "deposit", "channel" or "method", and the options
 *       are not ARIA tabs). The real options are {@code <li>} rows of the page's one
 *       {@code ul.list-none} list; {@link #PAYMENT_OPTION} now targets that directly.</li>
 *   <li>{@link #submitButton()} - was a guess matched against an accessible name of "submit",
 *       "deposit" or "confirm". Confirmed live this was a real bug, not just an unconfirmed
 *       guess: the top nav's own "Deposit" button and the sidebar's "Deposit" link both match
 *       "deposit" too and sort before the real control in DOM order, so {@code .first()} clicked
 *       the nav link - which does nothing on a page already on {@link #PATH} - instead of ever
 *       submitting the form. The real control's accessible name is the exact text "SUBMIT"; the
 *       pattern below is anchored to match only that.</li>
 *   <li>{@link #POPUP} - was a guess that missed the real validation/confirmation popup
 *       entirely (confirmed live: submitting an empty amount produced no match for this
 *       selector). The real popup renders under a class containing "AlertBoard" - the same
 *       pattern {@link org.sengmean.page.payment.WithdrawalPage}'s own {@code BOARD_MESSAGE}
 *       already guessed correctly; {@link #POPUP} now includes it.</li>
 *   <li>{@link #PRESET_HINT} - still unconfirmed; every option seen live offered a free-entry
 *       amount field, never a preset-amount control, so this path has not actually been
 *       exercised against the live DOM.</li>
 * </ul>
 *
 * <h2>Confirmed live: the first-offered option can require an unbound account</h2>
 * qa00002 has no UMPay account bound, so selecting the Deposit Options list's first row
 * (UMPAY PP EN) and submitting a valid amount returns "UMPay Account is required" rather than
 * reaching a confirmation prompt - not a defect, just this account's own binding state. Bank
 * transfer PP/6P already have a bank account bound for this account and reach "Confirm Deposit?"
 * cleanly. See {@link #currentOptionNeedsBinding()} and the test classes' {@code firstUsableOption}
 * helper, which walks the option list for one that does not need a binding instead of assuming
 * the first option is always submittable.
 *
 * <h2>What was deliberately left out of the port</h2>
 * No method here commits a real deposit by itself: {@link #confirm()} exists only for shape
 * parity with the Weibo original and is not called by any test in {@code DepositNewUiTest} /
 * {@code DepositClassicUiTest}. See those classes' Javadoc for the full list of Weibo-specific
 * cases (named channels, required-field-per-channel, the WPay/confirmed-deposit groups) that
 * were not ported at all because they have no honest Quark equivalent.
 */
public class DepositPage extends BasePage {

    public static final String PATH = "/en/member/deposit/";

    /** The Deposit Options list's own rows - confirmed live 2026-10-24, see the class Javadoc. */
    private static final String PAYMENT_OPTION = "ul.list-none > li";

    /** Tolerant guess at a preset-amount control's own placeholder text, mirroring the Weibo
     *  original's "Please select" dropdown - unconfirmed whether Quark has such a control at all. */
    private static final String PRESET_HINT = "Please select";

    /** The dialog/toast the form raises on submit - confirmed live 2026-10-24, see the class
     *  Javadoc; the tolerant guesses are kept as a fallback in case a different channel/option
     *  renders its popup under a different component. */
    private static final String POPUP =
            "[class*='AlertBoard' i], [role='dialog'], [class*='modal' i], [class*='popup' i], "
                    + "[class*='alert' i]";

    /** The placeholder text shown in an unbound account field, e.g. "Click [Add] to bind Banking
     *  details" - confirmed live 2026-10-24 for a UMPay account field, a bank account field and a
     *  crypto wallet-address field, so this is read as a generic "this option needs binding
     *  first" signal rather than naming any one field specifically. */
    private static final String UNBOUND_ACCOUNT_HINT = "Click [Add] to bind";

    /**
     * The Deposit Channel tile grid - confirmed live 2026-10-24: a grid of {@code role="button"}
     * {@code div}s, one per channel, the only such group on the page. Eight channels were offered
     * that day - NEW, AI_PAY, 6pay, Channel, USDT, WPay, USDT(UPAY), UMPay - each with its own,
     * differently-sized {@link #PAYMENT_OPTION} list (0 for 6pay that day, up to 17 for AI_PAY);
     * see {@link #channelLabels()} rather than this list for what is actually offered on a given
     * run.
     *
     * <p><b>Was a real bug, caught and fixed 2026-10-24 the same day it was written</b>: the first
     * version of this selector was {@code div.grid.grid-cols-4 > div}, which matched correctly on
     * New UI but found <i>zero</i> tiles on Classic UI - its grid container's own class is
     * {@code grid grid-cols-3 lg:grid-cols-4 gap-2} (a responsive 3-then-4-column grid, not a
     * literal {@code grid-cols-4}), so the exact-class match missed it entirely. Confirmed live
     * on all four combinations (Deposit/Withdrawal x New UI/Classic UI) that the tiles' own
     * parent always carries the plain class {@code grid} regardless of its column-count classes,
     * which is what the selector below matches on instead.
     */
    private static final String CHANNEL_TILE = "div.grid > div[role='button']";

    public DepositPage(Page page) {
        super(page);
    }

    // ----------------------------------------------------------------- navigation

    public void navigateTo() {
        open(PATH);
    }

    /** True when the app has bounced this page off the deposit page, which it should for a guest. */
    public boolean redirectedAway() {
        return !page.url().contains(PATH);
    }

    /**
     * True once the deposit form has rendered: either kind of amount control, or at least one
     * payment option, is visible. Generalised from the Weibo original's {@code isLoaded()},
     * which waited specifically for its own "Deposit Channel" / "Deposit Options" headings -
     * headings this project cannot assume exist.
     */
    public boolean isLoaded() {
        try {
            long deadline = System.currentTimeMillis() + 15_000;
            while (System.currentTimeMillis() < deadline) {
                if (hasFreeEntryAmount() || hasPresetAmount() || paymentOptionLabels().size() > 0) {
                    return true;
                }
                page.waitForTimeout(250);
            }
            return hasFreeEntryAmount() || hasPresetAmount() || paymentOptionLabels().size() > 0;
        } catch (PlaywrightException navigating) {
            return false;
        }
    }

    // ----------------------------------------------------------------- payment options

    /** The input directly below the given visible label - this project's house pattern, see
     *  {@code LoginPage.fieldNear}. */
    private Locator fieldNear(String label) {
        return page.locator("xpath=//*[normalize-space(text())='" + label
                + "']/following::input[1]");
    }

    /** Every payment option label currently on offer, in whatever order the page renders them. */
    public List<String> paymentOptionLabels() {
        Locator options = page.locator(PAYMENT_OPTION);
        List<String> labels = new ArrayList<>();
        int count = options.count();
        for (int i = 0; i < count; i++) {
            try {
                String text = options.nth(i).innerText();
                if (text != null && !text.isBlank()) {
                    labels.add(text.trim());
                }
            } catch (PlaywrightException notReadable) {
                // Skipped rather than failing the whole read - one awkward tile must not hide
                // what the others have to say.
            }
        }
        return labels;
    }

    public boolean isOptionOffered(String label) {
        return page.getByText(label, new Page.GetByTextOptions().setExact(false)).count() > 0;
    }

    public void selectPaymentOption(String label) {
        CaseLog.step("Selecting payment option '" + label + "'");
        page.getByText(label, new Page.GetByTextOptions().setExact(false)).first().click();
        page.waitForTimeout(1_000);
    }

    // ----------------------------------------------------------------- channels

    /** Every deposit channel currently on offer, in whatever order the page renders them - see
     *  {@link #CHANNEL_TILE}. */
    public List<String> channelLabels() {
        Locator tiles = page.locator(CHANNEL_TILE);
        List<String> labels = new ArrayList<>();
        int count = tiles.count();
        for (int i = 0; i < count; i++) {
            try {
                String text = tiles.nth(i).innerText();
                if (text != null && !text.isBlank()) {
                    labels.add(text.trim());
                }
            } catch (PlaywrightException notReadable) {
                // Skipped rather than failing the whole read - one awkward tile must not hide
                // what the others have to say.
            }
        }
        return labels;
    }

    /** Selects a deposit channel by its exact tile label, re-rendering {@link #PAYMENT_OPTION}
     *  to that channel's own option list. */
    public void selectChannel(String label) {
        CaseLog.step("Selecting deposit channel '" + label + "'");
        Locator tiles = page.locator(CHANNEL_TILE);
        int count = tiles.count();
        for (int i = 0; i < count; i++) {
            if (label.equals(tiles.nth(i).innerText().trim())) {
                tiles.nth(i).click();
                page.waitForTimeout(800);
                return;
            }
        }
        throw new PlaywrightException("No deposit channel tile labelled '" + label
                + "' - offered: " + channelLabels());
    }

    // ----------------------------------------------------------------- amount

    private Locator amountField() {
        Locator byLabel = fieldNear("Amount");
        return byLabel.count() > 0 ? byLabel.first() : page.locator("input[type='number']").first();
    }

    public boolean hasFreeEntryAmount() {
        try {
            Locator field = amountField();
            return field.count() > 0 && field.isVisible();
        } catch (PlaywrightException notThere) {
            return false;
        }
    }

    /** True when the selected option shows a preset-amount hint instead of a free-entry field.
     *  Unconfirmed whether Quark has an equivalent to Weibo's preset dropdown ladder at all. */
    public boolean hasPresetAmount() {
        return !hasFreeEntryAmount() && page.getByText(PRESET_HINT).count() > 0;
    }

    /** The hint text the amount field advertises, e.g. a "Min ... Max ..." style placeholder,
     *  or "" when there is no free-entry field or it carries no hint. */
    public String amountPlaceholder() {
        if (!hasFreeEntryAmount()) {
            return "";
        }
        String placeholder = amountField().getAttribute("placeholder");
        return placeholder == null ? "" : placeholder;
    }

    /**
     * The minimum the amount field advertises for whichever option is selected, read at run
     * time rather than assumed - unlike the Weibo original, which could fall back on a known
     * constant (100) when the attribute read failed. There is no such fallback here: a figure
     * that cannot be read comes back {@code Double.NaN}, and callers must treat that as "no
     * rule to test" rather than invent a number.
     * <p>Tried first against the native {@code min}/{@code max} HTML attributes, which are
     * structural rather than copy and so survive a wording change; the placeholder text (e.g.
     * "Min : 100.00 ~ Max : 5000000.00", the Weibo original's own format) is the fallback.
     */
    public double advertisedMinimum() {
        return advertisedLimit("min", "Min");
    }

    public double advertisedMaximum() {
        return advertisedLimit("max", "Max");
    }

    private double advertisedLimit(String attribute, String placeholderLabel) {
        if (!hasFreeEntryAmount()) {
            return Double.NaN;
        }
        String raw = amountField().getAttribute(attribute);
        if (raw != null) {
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException notNumeric) {
                // Fall through to the placeholder guess below.
            }
        }
        Matcher matcher = Pattern.compile(
                placeholderLabel + "\\s*:?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)")
                .matcher(amountPlaceholder());
        if (!matcher.find()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException notNumeric) {
            return Double.NaN;
        }
    }

    public void enterAmount(String amount) {
        CaseLog.step("Entering amount " + amount);
        amountField().fill("");
        amountField().fill(amount);
        page.waitForTimeout(500);
    }

    public void clearAmount() {
        amountField().fill("");
        page.waitForTimeout(500);
    }

    public String amountValue() {
        return hasFreeEntryAmount() ? amountField().inputValue() : "";
    }

    // ----------------------------------------------------------------- submit / popup

    private Locator submitButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^submit$", Pattern.CASE_INSENSITIVE)));
    }

    /** True when the currently selected option shows an unbound-account field that SUBMIT will
     *  refuse until it is filled in - see the class Javadoc's "first-offered option can require
     *  an unbound account" note. */
    public boolean currentOptionNeedsBinding() {
        return page.getByText(UNBOUND_ACCOUNT_HINT, new Page.GetByTextOptions().setExact(false))
                .count() > 0;
    }

    /** Presses the submit control. Never commits anything by itself - whatever appears next is
     *  read with {@link #popupText()} / {@link #isConfirmationPrompt()}. */
    public void submit() {
        CaseLog.step("Submitting the deposit form");
        submitButton().first().click();
        page.waitForTimeout(1_500);
    }

    public boolean isPopupVisible() {
        Locator popup = page.locator(POPUP).first();
        try {
            return popup.count() > 0 && popup.isVisible();
        } catch (PlaywrightException notThere) {
            return false;
        }
    }

    /** The text of whatever popup/overlay submit raised, or "" when none appeared. */
    public String popupText() {
        if (!isPopupVisible()) {
            return "";
        }
        try {
            return page.locator(POPUP).first().innerText().trim();
        } catch (PlaywrightException notReadable) {
            return "";
        }
    }

    /** True when the popup reads as an offer to confirm rather than a refusal - generalised
     *  from the Weibo original's exact "Confirm Deposit?" wording to a looser "contains the
     *  word confirm" match, since the real wording is unknown. */
    public boolean isConfirmationPrompt() {
        return popupText().toLowerCase(java.util.Locale.ROOT).contains("confirm");
    }

    public boolean hasConfirmButton() {
        return popupButton("confirm|yes|ok").count() > 0;
    }

    private Locator popupButton(String namePattern) {
        return page.locator(POPUP).first().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions()
                .setName(Pattern.compile(namePattern, Pattern.CASE_INSENSITIVE)));
    }

    /**
     * Commits the deposit by pressing whatever the popup's confirm control is.
     * <b>Not called by any ported test</b> - see the class Javadoc. Kept only so the page
     * object has the same shape as the Weibo original's, for whoever wires up a real,
     * deliberately-tagged "creates-data" case later once the live confirm control is known.
     * @return false when no popup, or no confirm control inside it, was on screen
     */
    public boolean confirm() {
        if (!isPopupVisible() || !hasConfirmButton()) {
            return false;
        }
        popupButton("confirm|yes|ok").first().click();
        page.waitForTimeout(2_000);
        return true;
    }

    /** Backs out of the confirmation popup without depositing. */
    public void cancelConfirmation() {
        Locator cancel = popupButton("cancel|close|no");
        if (cancel.count() > 0) {
            cancel.first().click();
            page.waitForTimeout(500);
        }
    }

    /** Clears whatever popup is on screen, by whichever button it offers, cancel preferred. */
    public void dismissPopup() {
        if (!isPopupVisible()) {
            return;
        }
        cancelConfirmation();
        if (isPopupVisible()) {
            Locator anyButton = page.locator(POPUP).first().getByRole(AriaRole.BUTTON);
            if (anyButton.count() > 0) {
                try {
                    anyButton.first().click();
                } catch (PlaywrightException notClickable) {
                    // Nothing left to try; the caller's own isLoaded()/isPopupVisible() checks
                    // will say so.
                }
            }
        }
    }
}
