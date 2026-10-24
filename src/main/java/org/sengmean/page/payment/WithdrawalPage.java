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
 * The member withdrawal page, {@code /en/member/withdrawal/}.
 *
 * <p>Ported from the sibling NewWeibo_automation project's {@code WithdrawalPage} (same page
 * concept, a different client site - Weibo Empire there, Quark Dynasty here). That class had the
 * benefit of 64 cases' worth of confirmed, read-off-the-live-DOM locators and known limits
 * ("Min 100.00 ~ Max 100000.00", five named channels, specific bank fields). None of that carries
 * over: this project was scaffolded from a manual screenshot-driven session (see
 * {@link org.sengmean.page.auth.LoginPage}'s own caveat) and nobody has yet inspected Quark's real
 * withdrawal screen, so every locator below is a guess and every limit is read from whatever the
 * page itself says at run time rather than hard-coded.
 *
 * <h2>Selector confidence - confirmed live 2026-10-24 against test-v2.138hk.vip</h2>
 * Driven manually (both New UI and Classic UI, signed in as qa00002):
 * <ul>
 *   <li>{@link #PATH} - confirmed correct as guessed: {@code /en/member/withdrawal/} is the real
 *       route and the top nav's own Withdraw link lands on it.</li>
 *   <li>{@link #AMOUNT_INPUT} - confirmed correct: the page's one
 *       {@code input[type='number']} is the "Withdrawal Game Points" field, with a placeholder
 *       like "Min : 100.00 ~ Max : 100000.00" that {@link #limitFromPlaceholder} already parses.</li>
 *   <li>{@link #SUBMIT_BUTTON_NAME} - was a guess matched against an accessible name of "submit"
 *       or "withdraw", confirmed live to be a real bug: the top nav's own "Withdraw" button (New
 *       UI) / "Withdrawal" link (Classic UI) both match "withdraw" too and sort before the real
 *       control in DOM order, so {@code .first()} clicked the nav link - which does nothing on a
 *       page already on {@link #PATH} - instead of ever submitting the form. The real control's
 *       accessible name is the exact text "SUBMIT"; the pattern below is anchored to match only
 *       that.</li>
 *   <li>{@link #OPTION_TILE} - was a guess that matched <b>zero</b> elements live (no class on
 *       this page names "option", "channel" or "method", and the rows are not ARIA radios). The
 *       real withdraw options (UMPAY PP EN, Bank transfer PP, Bank transfer 6P, FPS PP, FPS 6P)
 *       are {@code <li>} rows of the page's one {@code ul.list-none} list, the same structure the
 *       sibling {@link org.sengmean.page.payment.DepositPage} uses for its own options;
 *       {@link #OPTION_TILE} now targets that directly.</li>
 *   <li>{@link #BOARD_MESSAGE} - confirmed correct as guessed: both the "minimum withdrawal"
 *       refusal and the "Confirm Withdrawal?" prompt render under a class containing
 *       "AlertBoard".</li>
 *   <li>{@link #CONFIRM_BUTTON_NAME} / {@link #CANCEL_BUTTON_NAME} - confirmed correct: the
 *       confirmation prompt's own two buttons are the exact text "Confirm" and "Cancel".</li>
 *   <li>{@link #BALANCE_PATTERN} - confirmed correct: the header's own wallet line
 *       ("HKD 365.37") matches it.</li>
 * </ul>
 *
 * <h2>Confirmed live: the first-offered option can require an unbound account</h2>
 * qa00002 has no UMPay account bound, so selecting the Withdraw Options list's first row
 * (UMPAY PP EN) and submitting a valid amount returns "UMPay Account is required" rather than
 * reaching a confirmation prompt - not a defect, just this account's own binding state. Bank
 * transfer PP/6P already have a bank account bound for this account and reach "Confirm
 * Withdrawal?" cleanly. See {@link #currentOptionNeedsBinding()} and the test classes'
 * {@code firstUsableOption} helper, which walks the option list for one that does not need a
 * binding instead of assuming the first option is always submittable.
 *
 * <h2>Validation timing is assumed, not confirmed</h2>
 * The sibling project's form validates on submit rather than on entry - the amount field accepts
 * an out-of-range value with no complaint, and {@code SUBMIT} stays enabled regardless. This
 * class and the cases built on it assume Quark's form behaves the same way, since nothing else
 * has been observed; if that assumption is wrong for Quark, the cases that submit-then-read-the-
 * popup still work (they never relied on {@link #isSubmitEnabled()} for a verdict), but any case
 * that expected the field itself to show a validation message inline would need rewriting.
 *
 * <h2>Nothing here commits a real withdrawal</h2>
 * {@link #confirmWithdrawal()} exists, for the same documentation reason the sibling project kept
 * its own version: a page object for this screen is incomplete without a way to describe what
 * Confirm does. No test in this project's {@code WithdrawalNewUiTest} or
 * {@code WithdrawalClassicUiTest} calls it. Confirming is expected to place a real request against
 * the shared QA account's balance - see the sibling project's own defects (WDR-01, HKDWITHDRA-919)
 * for what happens when that step runs unexamined - and this project has no group-exclusion wired
 * into its Surefire configuration the way the sibling project's {@code -DexcludedGroups=...} is,
 * so a method that commits money has to stay uncalled rather than merely tagged.
 */
public class WithdrawalPage extends BasePage {

    /** Guessed by analogy with this project's other member routes; unconfirmed. */
    public static final String PATH = "/en/member/withdrawal/";

    private static final String AMOUNT_INPUT = "input[type='number']";

    /** Anchored to match only the exact accessible name "SUBMIT" - see the class Javadoc for why
     *  the previous unanchored "submit|withdraw" pattern was a real bug, not just unconfirmed. */
    private static final String SUBMIT_BUTTON_NAME = "^submit$";

    /** The Withdraw Options list's own rows - confirmed live 2026-10-24, see the class Javadoc. */
    private static final String OPTION_TILE = "ul.list-none > li";

    private static final String BOARD_MESSAGE =
            "[class*='AlertBoard' i], [class*='Toast' i], [class*='modal' i], [role='alert'], [role='dialog']";
    private static final String CONFIRM_BUTTON_NAME = "confirm";
    private static final String CANCEL_BUTTON_NAME = "(cancel|close)";

    /** The placeholder text shown in an unbound account field, e.g. "Click [Add] to bind Banking
     *  details" - confirmed live 2026-10-24, same text as the sibling
     *  {@link org.sengmean.page.payment.DepositPage}'s own unbound-account hint. */
    private static final String UNBOUND_ACCOUNT_HINT = "Click [Add] to bind";

    /** The Withdrawal Channel tile grid - same confirmed shape, and the same fixed New-UI-only
     *  bug, as the sibling {@link org.sengmean.page.payment.DepositPage#CHANNEL_TILE}: a grid of
     *  {@code role="button"} {@code div}s, one per channel, found on all four combinations of
     *  Deposit/Withdrawal x New UI/Classic UI by matching on the tiles' parent's plain
     *  {@code grid} class rather than its (UI-version-specific) column-count class. */
    private static final String CHANNEL_TILE = "div.grid > div[role='button']";

    /** A currency code followed by a figure, e.g. "HKD 365.37" - see the class Javadoc caveat. */
    private static final Pattern BALANCE_PATTERN =
            Pattern.compile("([A-Z]{3,4})\\D{0,10}([\\d,]+\\.\\d{2})");

    private final Locator amountInput;
    private final Locator submitButton;

    public WithdrawalPage(Page page) {
        super(page);
        this.amountInput = page.locator(AMOUNT_INPUT).first();
        this.submitButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile(SUBMIT_BUTTON_NAME, Pattern.CASE_INSENSITIVE)));
    }

    // ----------------------------------------------------------- navigation

    public void open() {
        open(PATH);
        dismissWelcomeBanner();
        CaseLog.step("Opened the withdrawal page: " + page.url());
    }

    /** True when the page did not keep us on the withdrawal route - the guest-redirect signal. */
    public boolean redirectedAway() {
        return !page.url().contains("/member/withdrawal");
    }

    /** True once the amount field and SUBMIT have both rendered. */
    public boolean isLoaded() {
        try {
            amountInput.first().waitFor(new Locator.WaitForOptions().setTimeout(20_000));
            return submitButton.first().isVisible();
        } catch (RuntimeException notThere) {
            return false;
        }
    }

    // ------------------------------------------------------------- amount

    public boolean hasFreeEntryAmount() {
        return amountInput.count() > 0 && amountInput.isVisible();
    }

    public String amountPlaceholder() {
        return hasFreeEntryAmount() ? String.valueOf(amountInput.getAttribute("placeholder")) : "";
    }

    public String amountAttribute(String name) {
        return hasFreeEntryAmount() ? amountInput.getAttribute(name) : null;
    }

    /**
     * The minimum/maximum the amount field states for whichever option is selected right now,
     * read from its own placeholder - e.g. "Min 50.00 ~ Max 20000.00" - rather than from a
     * constant. {@code Double.NaN} when the placeholder does not carry a parseable figure for the
     * requested bound, which is the honest answer when the page states no limit this way.
     */
    public double statedMinimum() {
        return limitFromPlaceholder("min");
    }

    public double statedMaximum() {
        return limitFromPlaceholder("max");
    }

    private double limitFromPlaceholder(String label) {
        Matcher matcher = Pattern.compile(label + "\\D{0,5}([0-9][0-9,]*(?:\\.[0-9]+)?)",
                Pattern.CASE_INSENSITIVE).matcher(amountPlaceholder());
        if (!matcher.find()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException notANumber) {
            return Double.NaN;
        }
    }

    /** Replaces whatever the field holds with {@code amount} in one step. */
    public void enterAmount(String amount) {
        amountInput.fill("");
        amountInput.fill(amount);
        page.waitForTimeout(1_000);
        CaseLog.data("Amount entered", amount);
    }

    /**
     * Types {@code amount} keystroke by keystroke rather than setting the value in one go, so any
     * character the field silently drops or rewrites shows up in {@link #amountValue()} the way a
     * real customer would see it - see the sibling project's WDR-02/HKDWITHDRA-909/910 findings,
     * which this project cannot assume but should be able to detect if they recur here.
     */
    public void typeAmount(String amount) {
        amountInput.click();
        amountInput.press("Control+a");
        amountInput.press("Delete");
        amountInput.type(amount);
        page.waitForTimeout(1_000);
    }

    public String amountValue() {
        return hasFreeEntryAmount() ? amountInput.inputValue() : "";
    }

    public void clearAmount() {
        amountInput.fill("");
        page.waitForTimeout(500);
    }

    /**
     * Whether SUBMIT is enabled. Kept for parity with the sibling project's page object, which
     * found this never changes for an out-of-range amount and is therefore no basis for a
     * verdict; see the class Javadoc. No case in this project judges validity by it either.
     */
    public boolean isSubmitEnabled() {
        return submitButton.isEnabled();
    }

    // ------------------------------------------------------------- options

    /**
     * Every withdraw option label currently offered, read off whatever the live page renders -
     * see {@link #OPTION_TILE}'s Selector confidence note. No option name is assumed in advance,
     * so this reflects Quark's real option set whatever it turns out to be.
     */
    public List<String> optionLabels() {
        Locator tiles = page.locator(OPTION_TILE);
        List<String> labels = new ArrayList<>();
        int count = tiles.count();
        for (int i = 0; i < count; i++) {
            try {
                if (!tiles.nth(i).isVisible()) {
                    continue;
                }
                String text = tiles.nth(i).innerText();
                if (text != null && !text.isBlank()) {
                    labels.add(text.trim());
                }
            } catch (RuntimeException unreadable) {
                // One awkward tile must not stop the walk.
            }
        }
        return labels;
    }

    public boolean isOptionOffered(String label) {
        return page.getByText(label, new Page.GetByTextOptions().setExact(false)).count() > 0;
    }

    public void selectOption(String label) {
        page.getByText(label, new Page.GetByTextOptions().setExact(false)).last().click();
        page.waitForTimeout(1_000);
        CaseLog.step("Selected withdraw option '" + label + "'");
    }

    /** True when the currently selected option shows an unbound-account field that SUBMIT will
     *  refuse until it is filled in - see the class Javadoc's "first-offered option can require
     *  an unbound account" note. */
    public boolean currentOptionNeedsBinding() {
        return page.getByText(UNBOUND_ACCOUNT_HINT, new Page.GetByTextOptions().setExact(false))
                .count() > 0;
    }

    // ------------------------------------------------------------- channels

    /** Every withdrawal channel currently on offer, in whatever order the page renders them - see
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
            } catch (RuntimeException unreadable) {
                // One awkward tile must not stop the walk.
            }
        }
        return labels;
    }

    /** Selects a withdrawal channel by its exact tile label, re-rendering {@link #OPTION_TILE}
     *  to that channel's own option list. */
    public void selectChannel(String label) {
        CaseLog.step("Selecting withdrawal channel '" + label + "'");
        Locator tiles = page.locator(CHANNEL_TILE);
        int count = tiles.count();
        for (int i = 0; i < count; i++) {
            if (label.equals(tiles.nth(i).innerText().trim())) {
                tiles.nth(i).click();
                page.waitForTimeout(800);
                return;
            }
        }
        throw new PlaywrightException("No withdrawal channel tile labelled '" + label
                + "' - offered: " + channelLabels());
    }

    // ------------------------------------------------------------- balance

    /** The full page text, used for a balance read that does not depend on a confirmed layout. */
    private String pageText() {
        String text = page.locator("body").textContent();
        return text == null ? "" : text;
    }

    /** The first currency-plus-figure pair found on the page, or -1 when none can be read. */
    public double mainBalance() {
        Matcher matcher = BALANCE_PATTERN.matcher(pageText());
        if (!matcher.find()) {
            return -1;
        }
        try {
            return Double.parseDouble(matcher.group(2).replace(",", ""));
        } catch (NumberFormatException notANumber) {
            return -1;
        }
    }

    // --------------------------------------------------------- submit/popup

    public void submit() {
        submitButton.click();
        page.waitForTimeout(2_000);
        CaseLog.step("Pressed SUBMIT on the withdrawal form");
    }

    /** The text of whatever popup/board SUBMIT raised, or "" if none appeared. */
    public String popupText() {
        Locator board = page.locator(BOARD_MESSAGE);
        int count = board.count();
        for (int i = 0; i < count; i++) {
            try {
                if (board.nth(i).isVisible()) {
                    String text = board.nth(i).innerText();
                    if (text != null && !text.isBlank()) {
                        return text;
                    }
                }
            } catch (RuntimeException unreadable) {
                // Try the next candidate board.
            }
        }
        return "";
    }

    public boolean isPopupVisible() {
        return !popupText().isEmpty();
    }

    /**
     * True once SUBMIT has produced a prompt that asks the customer to confirm a withdrawal
     * request, rather than a refusal - read as "the popup is up and talks about confirming or a
     * pending request", since the exact wording Quark uses is unconfirmed.
     *
     * <h2>Encodes "valid means pending, not completed"</h2>
     * A case built on this method is not asserting that money has moved - only that the request
     * was accepted far enough to be offered for confirmation. See the class Javadoc and this
     * project's own withdrawal cases for why: in this family of projects, a "valid withdrawal"
     * means the request is now pending an administrator's decision, never an immediately
     * completed payment, and {@link #confirmWithdrawal()} - the one step that would actually move
     * money - is never called here.
     */
    public boolean isConfirmationPrompt() {
        String text = popupText().toLowerCase();
        return !text.isBlank() && (text.contains("confirm") || text.contains("pending"));
    }

    public boolean hasConfirmButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile(CONFIRM_BUTTON_NAME, Pattern.CASE_INSENSITIVE))).count() > 0;
    }

    /**
     * Presses Confirm, completing the withdrawal request for real.
     *
     * <p><b>Not called by any case in this project.</b> See the class Javadoc: Quark has no
     * group-exclusion configured the way the sibling project's {@code -DexcludedGroups=} is, so
     * the only safe rule here is that nothing calls this method at all, rather than tagging a case
     * and trusting every future run to exclude the tag.
     */
    public void confirmWithdrawal() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile(CONFIRM_BUTTON_NAME, Pattern.CASE_INSENSITIVE))).first().click();
        page.waitForTimeout(2_000);
        CaseLog.step("Pressed Confirm - a real withdrawal request has been filed");
    }

    /** Dismisses the confirmation prompt without confirming anything. */
    public void cancelConfirmation() {
        Locator cancel = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile(CANCEL_BUTTON_NAME, Pattern.CASE_INSENSITIVE)));
        if (cancel.count() > 0 && cancel.first().isVisible()) {
            cancel.first().click();
            page.waitForTimeout(500);
        }
    }

    /**
     * Clears whatever popup is on screen so the next case starts from a clean form. Tries Cancel
     * first, then re-opening the page as a last resort - never Confirm, for the same reason
     * {@link #confirmWithdrawal()} is never called from a case.
     */
    public void dismissPopup() {
        if (!isPopupVisible()) {
            return;
        }
        cancelConfirmation();
        if (isPopupVisible()) {
            Locator close = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                    .setName(Pattern.compile("close", Pattern.CASE_INSENSITIVE)));
            if (close.count() > 0) {
                close.first().click();
                page.waitForTimeout(500);
            }
        }
        if (isPopupVisible()) {
            open();
        }
    }
}
