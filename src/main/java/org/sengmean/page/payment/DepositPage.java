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
 * <h2>Selector confidence</h2>
 * None of the locators in this class have been confirmed against the live DOM. Specifically:
 * <ul>
 *   <li>{@link #amountField()} - tries the labelled field next to visible text "Amount" first
 *       ({@code fieldNear}, this project's house pattern), falling back to the one native
 *       {@code input[type='number']} on the page, mirroring the Weibo original's note that a
 *       deposit form's amount control is often the only native input on an otherwise div-built
 *       form.</li>
 *   <li>{@link #PAYMENT_OPTION} - a tolerant, comma-separated CSS guess at whatever tiles or
 *       tabs list the payment options, since the real channel/option structure (if Quark even
 *       has the Channel/Option split Weibo does) is unknown.</li>
 *   <li>{@link #submitButton()} - a role-based button matched on a name of "submit", "deposit"
 *       or "confirm" (case-insensitive), since the real button's accessible name has not been
 *       read.</li>
 *   <li>{@link #POPUP} - a tolerant guess at whatever dialog/toast/overlay the form raises on
 *       submit, modelled on this project's existing {@code VIP_POPUP_CLOSE} /
 *       {@code CHAT_TOGGLE} guesses in {@code BasePage}.</li>
 *   <li>{@link #PRESET_HINT} - a guess that a preset-amount control (if one exists at all,
 *       mirroring Weibo's dropdown ladder) shows a "Please select"-style placeholder.</li>
 * </ul>
 * <b>Confirm every one of these against the live DOM before trusting a red case built on it</b> -
 * exactly the caveat this project's README already carries for its other guessed locators.
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

    /** Tolerant guess at whatever renders the list of payment options/channels/tiles. */
    private static final String PAYMENT_OPTION =
            "[class*='payment' i][class*='option' i], [class*='deposit' i][class*='channel' i], "
                    + "[class*='deposit' i][class*='method' i], [role='tab']";

    /** Tolerant guess at a preset-amount control's own placeholder text, mirroring the Weibo
     *  original's "Please select" dropdown - unconfirmed whether Quark has such a control at all. */
    private static final String PRESET_HINT = "Please select";

    /** Tolerant guess at whatever dialog/overlay the form raises on submit. */
    private static final String POPUP =
            "[role='dialog'], [class*='modal' i], [class*='popup' i], [class*='alert' i]";

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
                .setName(Pattern.compile("submit|deposit|confirm", Pattern.CASE_INSENSITIVE)));
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
