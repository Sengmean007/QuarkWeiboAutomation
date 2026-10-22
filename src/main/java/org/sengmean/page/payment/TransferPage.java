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
 * Page Object for a <b>Manual Transfer</b> feature on Quark Dynasty - moving balance between a
 * member's main wallet and their individual game wallets - <b>if this site has one at all.</b>
 *
 * <h2>THIS WHOLE CLASS IS UNCONFIRMED - READ BEFORE TRUSTING ANY RED OR GREEN ROW</h2>
 * Nobody has looked at a Manual Transfer screen on {@code test-v2.138hk.vip}. This project's own
 * README ("Known defects this project exists to track") says nothing about one, and the only
 * areas scaffolded so far are login/register/currency-switch/responsive-layout. This class exists
 * because the user explicitly asked for this area to be ported ahead of anyone confirming it, not
 * because its existence or shape has been observed. <b>Every locator below is a structural guess
 * copied from how the sibling NewWeibo_automation project's own {@code TransferPage} (for Weibo
 * Empire, a different client site) addresses its own confirmed Manual Transfer page.</b> Treat
 * this whole file as a template: correct it - or delete it - once a human has actually opened
 * Quark Dynasty's member wallet and looked at what, if anything, is there.
 *
 * <h2>What this is a port of</h2>
 * Ported from {@code NewWeibo_automation/.../page/payment/TransferPage.java} (1256 lines) and the
 * 22 cases in its sibling {@code ManualTransferTest}. That page moves balance between a main
 * wallet and per-game wallets via two dropdowns ("From"/"To"), an amount field and a Submit
 * button, plus a "Transfer All Out" sweep control. Deliberately <b>not</b> ported:
 * <ul>
 *   <li>The balance table under the form ({@code gameBalances()}, {@code awaitGameBalances()},
 *       {@code gamesUnderMaintenance()} and friends) - that table's existence on Quark is even
 *       less certain than the transfer form itself, and the elaborate polling it needed on Weibo
 *       Empire (its figures arrive one game at a time over ~25 seconds) is a fact about that
 *       site's own backend, not something to assume here. This page instead reads a wallet's
 *       balance off the figure already carried in the dropdown's own option label - see
     *       {@link #walletBalanceOf(String)} - the same trick {@link #mainBalance()} already uses
     *       for the main wallet.</li>
 *   <li>Network-level outcome detection ({@code TransferOutcome}, {@code SweepOutcome}, watching
 *       {@code POST /api/member/game/trans_point} and {@code trans_all_point} to tell a rate-limit
 *       429 apart from a real refusal). Those are Weibo Empire's own backend routes; there is no
 *       evidence Quark's API is shaped the same way, so this page judges a submit purely by what
 *       the page itself shows - the same approach {@link org.sengmean.page.auth.LoginPage} already
 *       takes for this site.</li>
 *   <li>The throttling/pacing machinery {@code ManualTransferTest} needed to walk all ~52 of Weibo
 *       Empire's games without tripping its rate limiter. That machinery is a response to a
 *       specific, observed rate limiter on a different site; nothing here assumes Quark has one
 *       at all, so the ported cases exercise a single placeholder wallet rather than walking
 *       every wallet the dropdown offers.</li>
 * </ul>
 *
 * <h2>Selector confidence - every one of these is a guess</h2>
 * <ul>
 *   <li>{@link #WALLET_PATH} - copied verbatim from Weibo Empire's own path
 *       ({@code /en/member/wallet/}). Quark's equivalent page, if one exists, may live anywhere.</li>
 *   <li>{@link #NOTHING_SELECTED} - the placeholder text Weibo Empire's "To" box shows unchosen
 *       ("Please select"). Copied as a guess; Quark's own wording, if any, is unconfirmed.</li>
 *   <li>{@link #fromBox()} / {@link #toBox()} - assume two wallet pickers rendered as clickable
 *       {@code div}s (not {@code <select>} elements) in source order, exactly as Weibo Empire's
 *       are, addressed by a generic "looks like a dropdown trigger" CSS guess
 *       ({@code div[class*='select' i], div[class*='dropdown' i]}), {@code nth(0)} for "From" and
 *       {@code nth(1)} for "To". Nothing confirms Quark's transfer form (if any) uses two divs in
 *       this order, or divs at all.</li>
 *   <li>{@link #OPTION_PANEL} / {@link #OPTION} - assume opening a box renders its choices into a
 *       floating panel holding one element per option, matched by a generic "looks like an
 *       option" class guess. Copied from Weibo Empire's own Tailwind-utility-soup pattern, which
 *       has no reason to apply to Quark's own front end.</li>
 *   <li>{@link #amountField()} - uses {@link #fieldNear(String)}, the one convention this
 *       project's own {@code LoginPage} already relies on for this exact site (the input directly
 *       below a visible label), guessing the label reads "Amount". Of everything in this class,
 *       this is the locator with the most actual precedent - the mechanism is confirmed to work
 *       on this site's login form - but the label text itself is not confirmed for a transfer
 *       form that has not been seen.</li>
 *   <li>{@link #submitButton()} / {@link #transferAllOutButton()} - role-based button lookups by
 *       accessible name, matching "submit"/"transfer" and "transfer all out" case-insensitively -
 *       the same role-based pattern {@code LoginPage.submitButton()} uses, guessing at wording
 *       rather than at markup. Whether a "Transfer All Out" control (or any sweep control) exists
 *       on Quark at all is itself unconfirmed; {@link #hasTransferAllOut()} is there precisely so
 *       a case can find out and abort cleanly rather than assume.</li>
 *   <li>{@link #confirmButton()} - a plain "Confirm" text match, ported from the same idiom
 *       Weibo Empire's deposit, withdrawal and transfer pages all share (submitting opens a
 *       Confirm/Cancel dialog rather than refusing outright). Unconfirmed whether Quark's own
 *       forms raise an equivalent dialog.</li>
 *   <li>{@link #MESSAGE_BOARD} - a generic "looks like an alert/toast/message modal" CSS guess,
 *       used only as a visible-text source; every assertion built on it also falls back to
 *       {@link #pageText()} (the whole visible body, one line), the same robust, locator-free
 *       fallback {@code ResponsiveLayoutNewUiTest} already relies on elsewhere in this project.</li>
 * </ul>
 * One thing in this file <b>is</b> confirmed rather than guessed: the currency codes
 * {@link #TRAILING_BALANCE} strips from an option label - HKD, USDT, TRX, USD, THB - are exactly
 * the wallet currencies this project's own {@code CurrencyBannerTest} already exercises against
 * this site, per the README's "Known defects" section. Everything else here is not.
 */
public class TransferPage extends BasePage {

    /** Copied from Weibo Empire's own path. See the class Javadoc - entirely unconfirmed here. */
    public static final String WALLET_PATH = "/en/member/wallet/";

    /** The placeholder "To" shows before anything has been chosen - a guess, see class Javadoc. */
    public static final String NOTHING_SELECTED = "Please select";

    private static final String SELECT_BOX = "div[class*='select' i], div[class*='dropdown' i]";
    private static final String OPTION_PANEL = "div[class*='absolute' i][class*='z-' i]";
    private static final String OPTION = "[class*='option' i]";
    private static final String MESSAGE_BOARD =
            "[class*='alert' i], [class*='toast' i], [class*='modal' i][class*='message' i]";

    /** A nominal amount used by every ported case that must type *something* - not a figure taken
     *  from any workbook, since this project has none; see {@link #enterAmount(String)} callers. */
    public static final String PLACEHOLDER_AMOUNT = "1";

    /** Which box opened the currently visible option list, so {@link #closeOptions()} can toggle it shut. */
    private Locator openBox;

    /** Messages seen during the last {@link #waitForMessage} call, in the order they appeared. */
    private final List<String> observed = new ArrayList<>();

    public TransferPage(Page page) {
        super(page);
    }

    // ------------------------------------------------------------------ navigation

    /**
     * Opens the guessed Manual Transfer path and waits for the amount field to render.
     *
     * <p>No wallets-loaded network response is awaited here, unlike the Weibo Empire original -
     * this page has no confirmed API call to wait for. A short settle pause stands in for it.
     */
    public void openTransfer() {
        open(WALLET_PATH);
        try {
            amountField().first().waitFor(new Locator.WaitForOptions().setTimeout(15_000));
        } catch (PlaywrightException notThere) {
            CaseLog.step("No amount field appeared at " + WALLET_PATH + " within 15s - this page, "
                    + "or this whole feature, may not exist on Quark. See TransferPage's class "
                    + "Javadoc.");
        }
        dismissWelcomeBanner();
        page.waitForTimeout(1_000);
    }

    /** True while something resembling the transfer form is on screen. */
    public boolean isOpen() {
        return amountField().count() > 0;
    }

    // ------------------------------------------------------------------ field/button lookups

    /** The input directly below the given visible label - ported from {@code LoginPage}'s one
     *  confirmed convention on this site; see the class Javadoc for what is and is not confirmed
     *  about using it here. */
    private Locator fieldNear(String label) {
        return page.locator("xpath=//*[normalize-space(text())='" + label
                + "']/following::input[1]");
    }

    /** The clickable dropdown trigger directly below the given visible label - the same
     *  "following the label" idiom as {@link #fieldNear(String)}, aimed at a {@code div} instead
     *  of an {@code input} since Weibo Empire's own two wallet pickers are divs, not selects. */
    private Locator dropdownNear(String label) {
        return page.locator("xpath=//*[normalize-space(text())='" + label
                + "']/following::div[contains(@class,'select') or contains(@class,'dropdown')][1]");
    }

    private Locator amountField() {
        Locator byLabel = fieldNear("Amount");
        if (byLabel.count() > 0) {
            return byLabel;
        }
        // Fallback the label-based lookup cannot be relied on to find at all on a page that has
        // never been seen: any lone numeric input on the form.
        return page.locator("input[type='number']");
    }

    private Locator fromBox() {
        Locator byLabel = dropdownNear("From");
        if (byLabel.count() > 0) {
            return byLabel.first();
        }
        return page.locator(SELECT_BOX).nth(0);
    }

    private Locator toBox() {
        Locator byLabel = dropdownNear("To");
        if (byLabel.count() > 0) {
            return byLabel.first();
        }
        return page.locator(SELECT_BOX).nth(1);
    }

    private Locator submitButton() {
        return page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("submit|transfer",
                        Pattern.CASE_INSENSITIVE)));
    }

    private Locator transferAllOutButton() {
        return page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("transfer all out",
                        Pattern.CASE_INSENSITIVE)));
    }

    private Locator confirmButton() {
        return page.getByText("Confirm", new Page.GetByTextOptions().setExact(true));
    }

    // ------------------------------------------------------------------ the two dropdowns

    /** What "From" currently shows, e.g. {@code "Main wallet $254.35"}. */
    public String fromSelection() {
        return flatten(fromBox().innerText());
    }

    /** What "To" currently shows, e.g. {@code "Please select"}. */
    public String toSelection() {
        return flatten(toBox().innerText());
    }

    /** Opens the "From" list and returns the labels it offers. */
    public List<String> openFromOptions() {
        return openOptions(fromBox());
    }

    /** Opens the "To" list and returns the labels it offers. */
    public List<String> openToOptions() {
        return openOptions(toBox());
    }

    private List<String> openOptions(Locator box) {
        closeOptions();
        box.click();
        openBox = box;
        page.waitForTimeout(600);
        return optionLabels();
    }

    /**
     * Closes whichever option list is open, if any, by clicking the box that opened it - ported
     * unchanged from Weibo Empire's own page object, which found that the two pickers share one
     * floating panel and that clicking elsewhere is swallowed by an overlay while the list is
     * open. Unconfirmed whether Quark's own transfer form (if any) behaves the same way; if it
     * does not, this is simply a harmless extra click.
     */
    public void closeOptions() {
        if (page.locator(OPTION_PANEL + " " + OPTION).count() == 0) {
            openBox = null;
            return;
        }
        if (openBox != null) {
            openBox.click();
            page.waitForTimeout(300);
        }
        openBox = null;
    }

    /** The labels currently offered by whichever list is open. */
    public List<String> optionLabels() {
        Locator options = page.locator(OPTION_PANEL + " " + OPTION);
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < options.count(); i++) {
            labels.add(flatten(options.nth(i).innerText()));
        }
        return labels;
    }

    /**
     * Clicks a "Show More"-like entry until it stops being offered, then returns the full list.
     * Bounded, as the Weibo Empire original is, so a list that never stops growing is a finding
     * to report rather than a loop to run out the clock.
     */
    public List<String> expandOptions() {
        for (int guard = 0; guard < 10; guard++) {
            Locator more = page.locator(OPTION_PANEL + " " + OPTION)
                    .filter(new Locator.FilterOptions().setHasText("Show More"));
            if (more.count() == 0) {
                break;
            }
            more.first().click();
            page.waitForTimeout(500);
        }
        return optionLabels();
    }

    /** Opens "From" and picks the option whose label starts with the given text. */
    public void chooseFrom(String labelPrefix) {
        closeOptions();
        fromBox().click();
        openBox = fromBox();
        page.waitForTimeout(600);
        chooseOption(labelPrefix);
    }

    /** Opens "To" and picks the option whose label starts with the given text. */
    public void chooseTo(String labelPrefix) {
        closeOptions();
        toBox().click();
        openBox = toBox();
        page.waitForTimeout(600);
        chooseOption(labelPrefix);
    }

    private void chooseOption(String labelPrefix) {
        if (clickMatchingOption(labelPrefix)) {
            return;
        }
        expandOptions();
        if (clickMatchingOption(labelPrefix)) {
            return;
        }
        throw new PlaywrightException("No option starting with '" + labelPrefix
                + "' even after expanding the list. Offered: " + optionLabels());
    }

    private boolean clickMatchingOption(String labelPrefix) {
        Locator options = page.locator(OPTION_PANEL + " " + OPTION);
        for (int i = 0; i < options.count(); i++) {
            String label = flatten(options.nth(i).innerText());
            if (label.toLowerCase().startsWith(labelPrefix.toLowerCase())) {
                CaseLog.step("Choosing '" + label + "'");
                options.nth(i).click();
                openBox = null;
                page.waitForTimeout(500);
                return true;
            }
        }
        return false;
    }

    /**
     * The "To" list with its furniture removed - the placeholder, the main wallet and a
     * "Show More" pager - leaving the raw wallet labels (name and balance together) in the order
     * offered. Generalized from Weibo Empire's {@code offeredGames()}: nothing here names an
     * actual game, since which games (if any) Quark offers is unknown.
     */
    public List<String> offeredGames() {
        List<String> wallets = new ArrayList<>();
        List<String> offered = openToOptions();
        closeOptions();
        for (String label : offered) {
            if (!label.startsWith(NOTHING_SELECTED)
                    && !label.toLowerCase().startsWith("main wallet")
                    && !label.startsWith("Show More")) {
                wallets.add(label);
            }
        }
        return wallets;
    }

    /** The first wallet the "To" list offers, stripped of its balance, or null if none is offered. */
    public String firstGameOffered() {
        List<String> offered = offeredGames();
        return offered.isEmpty() ? null : gameName(offered.get(0));
    }

    /**
     * A wallet's name with its trailing balance figure stripped - "Example Game $0.00" becomes
     * "Example Game". Ported unchanged from Weibo Empire's {@code gameName()}: matching on the
     * part of the label that cannot change between a read and a later click is what makes
     * selection stable, whichever site this runs against.
     */
    public static String gameName(String label) {
        if (label == null) {
            return null;
        }
        return TRAILING_BALANCE.matcher(label).replaceAll("").trim();
    }

    /**
     * The balance figure a dropdown option label carries, read directly off the label rather than
     * off a separate balance table - see the class Javadoc for why this page has no such table.
     * Looks the wallet up by name in the <b>raw</b> (balance-carrying) "To" labels.
     *
     * @return the figure, or -1 when the wallet is not currently offered or carries no readable
     *         figure
     */
    public double walletBalanceOf(String walletName) {
        for (String label : offeredGames()) {
            if (gameName(label).equalsIgnoreCase(walletName)) {
                return balanceFromLabel(label);
            }
        }
        return -1;
    }

    /** The first wallet the "To" list offers whose own label-carried balance is at least
     *  {@code minimum}, or null when none qualifies. */
    public String firstWalletHoldingAtLeast(double minimum) {
        for (String label : offeredGames()) {
            if (balanceFromLabel(label) >= minimum) {
                return gameName(label);
            }
        }
        return null;
    }

    private static double balanceFromLabel(String label) {
        if (label == null) {
            return -1;
        }
        Matcher figure = Pattern.compile("(?:\\$|USDT|TRX|THB|HKD|USD)\\s*([0-9][0-9,]*\\.?[0-9]*)\\s*$")
                .matcher(label);
        if (!figure.find()) {
            return -1;
        }
        try {
            return Double.parseDouble(figure.group(1).replace(",", ""));
        } catch (NumberFormatException unreadable) {
            return -1;
        }
    }

    /**
     * The balance suffix an option label carries, in any of the wallet currencies this project
     * has actually exercised against this site (see {@code CurrencyBannerTest}) - the one part of
     * this class that is confirmed rather than guessed; see the class Javadoc.
     */
    private static final Pattern TRAILING_BALANCE =
            Pattern.compile("(?:\\s*\\$|\\s+(?:USDT|TRX|THB|HKD|USD))\\s*-?[\\d,]*\\.?\\d+\\s*$");

    // ------------------------------------------------------------------ amount and submit

    /**
     * Types an amount. Clears and <em>types</em> rather than filling the value directly, so that
     * cases probing what the field will and will not accept as keystrokes (a minus sign, a third
     * decimal place) are actually exercising the field's own input filtering rather than bypassing
     * it - the same reasoning Weibo Empire's own page object gives for the same choice.
     */
    public void enterAmount(String amount) {
        Locator field = amountField().first();
        field.fill("");
        field.type(amount);
        page.waitForTimeout(300);
        CaseLog.data("amount typed", amount);
    }

    /** What the amount field actually holds - which is not always what was typed. */
    public String amountValue() {
        return amountField().first().inputValue();
    }

    /** True while Submit will accept a click. */
    public boolean isSubmitEnabled() {
        try {
            Locator submit = submitButton().first();
            submit.waitFor(new Locator.WaitForOptions().setTimeout(5_000));
            return submit.isEnabled();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Presses Submit. This is a guess at what happens next on a page nobody has seen, so any
     *  case calling it with an amount that could be accepted belongs in {@code creates-data}. */
    public void submit() {
        CaseLog.step("Submitting the transfer");
        submitButton().first().click();
    }

    /** Presses Confirm on the dialog Submit may raise, if one appeared. Returns whether it did. */
    public boolean confirmIfAsked(double timeoutMs) {
        Locator confirm = confirmButton();
        long deadline = System.currentTimeMillis() + (long) timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (confirm.count() > 0 && confirm.first().isVisible()) {
                CaseLog.step("Confirming on the dialog Submit raised");
                confirm.first().click();
                page.waitForTimeout(500);
                return true;
            }
            page.waitForTimeout(200);
        }
        return false;
    }

    /** True while a control resembling "Transfer All Out" is on the page - unconfirmed whether
     *  Quark has any such sweep control at all; a case should check this before relying on one. */
    public boolean hasTransferAllOut() {
        return transferAllOutButton().count() > 0;
    }

    /** Presses whatever looks like a "Transfer All Out" control. This moves real money if it
     *  works at all, so any case calling it belongs in {@code creates-data}. */
    public void transferAllOut() {
        CaseLog.step("Pressing the guessed 'Transfer All Out' control");
        transferAllOutButton().first().click();
    }

    // ------------------------------------------------------------------ balances

    /** The main wallet figure shown in "From", digits and separators only. */
    public String mainBalance() {
        Matcher m = Pattern.compile("([0-9][0-9,]*\\.?[0-9]*)").matcher(fromBox().innerText());
        return m.find() ? m.group(1) : "";
    }

    /** The main wallet figure as a number, or -1 when the page is not showing one. */
    public double mainBalanceValue() {
        String raw = mainBalance().replace(",", "");
        try {
            return raw.isBlank() ? -1 : Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ------------------------------------------------------------------ messages

    /**
     * Waits for the given wording to appear anywhere on the page, keeping every message it saw.
     * Same reasoning as {@code LoginPage.errorMessage()}: this family of sites tends to deliver
     * refusals as transient boards rather than a persistent toast, so a case that submits, sleeps
     * and only then looks can find an empty page and report that nothing was shown.
     */
    public boolean waitForMessage(String fragment, double timeoutMs) {
        observed.clear();
        long deadline = System.currentTimeMillis() + (long) timeoutMs;
        do {
            String alert = alertText();
            if (!alert.isBlank() && !observed.contains(alert)) {
                observed.add(alert);
            }
            if (pageText().toLowerCase().contains(fragment.toLowerCase())) {
                return true;
            }
            page.waitForTimeout(200);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    /** The distinct messages seen during the last {@link #waitForMessage} call. */
    public String observedMessages() {
        return observed.isEmpty() ? "<the page showed no message at all>" : String.join(" | ", observed);
    }

    private String alertText() {
        Locator alerts = page.locator(MESSAGE_BOARD);
        for (int i = 0; i < alerts.count(); i++) {
            Locator alert = alerts.nth(i);
            if (alert.isVisible()) {
                String text = flatten(alert.innerText());
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    /** Everything the page is currently showing, on one line, for an assertion message. */
    public String pageText() {
        try {
            return flatten(page.innerText("body"));
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String flatten(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
