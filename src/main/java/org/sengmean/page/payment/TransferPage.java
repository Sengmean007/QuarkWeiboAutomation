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
 * Page Object for the <b>Manual Transfer</b> feature on Quark Dynasty - moving balance between a
 * member's main wallet and their individual game wallets, at {@code /en/member/wallet/}.
 *
 * <h2>Confirmed live 2026-10-24 against test-v2.138hk.vip (both New UI and Classic UI)</h2>
 * The feature exists, at exactly the guessed {@link #WALLET_PATH}, and matches the shape this
 * class was ported from closely: a "From"/"To" pair of custom dropdowns, a free-entry amount
 * field, a Submit button, a "Transfer All Out" sweep, and - contrary to this class's original
 * uncertainty - a real "Game Balance" table (Game / Balance / Operate, one row per game wallet,
 * a per-row "Transfer"/"Transfer Out" button, "Maintenance" in place of that button for a game
 * that is down). That table is still not read by this class - see "What this is a port of" below
 * for why {@link #walletBalanceOf(String)} reading the dropdown label remains the simpler choice -
 * but it is no longer a guess that the table exists at all.
 *
 * <p>Driven manually, signed in as qa00002: a $1 transfer into a game wallet and back out again,
 * a $0 transfer, an over-balance transfer in both directions, and a "Transfer All Out" sweep.
 * Every locator and wording guess this uncovered as correct or wrong is called out in "Selector
 * confidence" below, case by case, rather than left as a blanket disclaimer - this file is no
 * longer a template to correct on sight; it is confirmed, with two real bugs now fixed.
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
 * <h2>Selector confidence - confirmed live 2026-10-24, both UI versions unless noted</h2>
 * <ul>
 *   <li>{@link #WALLET_PATH} - <b>confirmed correct</b>: the top nav's own Transfer link lands
 *       here on both UI versions.</li>
 *   <li>{@link #NOTHING_SELECTED} - <b>confirmed correct</b>: "To" reads exactly "Please select"
 *       until something is chosen.</li>
 *   <li>{@link #fromBox()} / {@link #toBox()} - <b>was a real bug, now fixed.</b> The two pickers
 *       are indeed {@code div}s in source order (From first, To second), but the previous
 *       {@code dropdownNear(String)} guess - "the div after the label text whose class contains
 *       'select' or 'dropdown'" - only matched by coincidence on New UI, where the box happens to
 *       carry a Tailwind class named {@code bg-background-selectInput}. Classic UI's box carries
 *       no such class at all (an arbitrary-value Tailwind color instead), so the guess matched
 *       nothing there and silently fell back to a page-wide CSS guess that does not reliably find
 *       either box. Both boxes carry one stable class across <b>both</b> UI versions regardless of
 *       which Tailwind utility classes ride along with it:
 *       {@code [class*='Wallet_transfer-label' i]}, exactly two matches, {@code nth(0)} = From,
 *       {@code nth(1)} = To - confirmed live under both. {@link #fromBox()}/{@link #toBox()} now
 *       use that directly.</li>
 *   <li>{@link #OPTION_PANEL} / {@link #OPTION} - <b>confirmed correct</b> on both UI versions:
 *       the floating panel's own class contains "absolute" and a "z-" utility, and each row -
 *       including "Show More" - carries a class containing "option"
 *       ({@code Wallet_dynamic-option__<hash>}, with a different hash per UI version but the same
 *       substring). Opening the panel needs a real, trusted click; a script-dispatched
 *       {@code element.click()} did not open it in testing - {@link #openOptions(Locator)} already
 *       uses Playwright's own {@code click()}, which is trusted, so this only matters if a future
 *       change swaps in a script-level click.</li>
 *   <li>{@link #amountField()} - <b>confirmed correct</b>: the field directly below the visible
 *       "Amount" label is the one native {@code input[type='number']} on the page, on both UI
 *       versions. It starts pre-filled with the <i>main wallet's own balance</i> (not empty) -
 *       harmless for every ported case, since {@link #enterAmount(String)} clears it first, but
 *       worth knowing if a future case reads the field before typing into it.</li>
 *   <li>{@link #submitButton()} - <b>was a real bug, now fixed</b>, the same shape as the one
 *       found and fixed in {@link org.sengmean.page.payment.DepositPage}/{@code WithdrawalPage}
 *       the same day: the old pattern matched any button whose name <i>contained</i> "submit" or
 *       "transfer", which also matches the top nav's own "Transfer" control. On New UI that nav
 *       control happens to be an {@code <a>} (no {@code role="button"}), so the bug stayed latent
 *       there by accident; on Classic UI it is a real {@code <button>} that sorts before the real
 *       Submit in DOM order, so {@code .first()} clicked the nav link - which does nothing on a
 *       page already at {@link #WALLET_PATH} - instead of ever submitting the form. The real
 *       button's accessible name is "Submit" (New UI) / "SUBMIT" (Classic UI); the pattern below
 *       is anchored to match only that, case-insensitively, on either.</li>
 *   <li>{@link #transferAllOutButton()} - <b>confirmed correct</b>: a "Transfer All Out" control
 *       exists on both UI versions, with exactly that accessible name, and {@link #submit()}'s
 *       collision does not apply to it - "transfer all out" is not a substring of the nav
 *       control's name "Transfer".</li>
 *   <li>{@link #confirmButton()} / {@link #confirmIfAsked(double)} - <b>confirmed unconfirmable as
 *       written: Quark's transfer form does not raise one at all.</b> Every submit observed live -
 *       a successful $1 transfer, a successful transfer back, a refused $0 amount - went straight
 *       from Submit to a result, with no intermediate Confirm/Cancel dialog. {@link #confirmButton()}
 *       is kept for shape parity, and {@link #confirmIfAsked(double)} is harmless to keep calling
 *       (it waits out its timeout and returns false) but is not expected to ever find anything on
 *       this page.</li>
 *   <li>{@link #MESSAGE_BOARD} - <b>confirmed correct</b>: every popup observed - the $0 refusal,
 *       the success board, and the transient lock described below - renders under a class
 *       containing "AlertBoard", which {@code [class*='alert' i]} matches, on both UI versions.</li>
 *   <li>The refusal wording this class generalizes against is <b>confirmed exact</b>, not a
 *       guess that happened to work: a $0 amount is refused with precisely "The transfer amount
 *       must be greater than 0" (the "greater than 0" fragment {@link #waitForMessage} cases match
 *       on), and a successful transfer reads "Transfer Successful!" (the "success" fragment those
 *       cases match on) - both confirmed identical on New UI and Classic UI.</li>
 *   <li>Submit's <b>disabled</b>, rather than refused-after-submitting, for an amount beyond the
 *       source wallet's balance - confirmed live in <i>both</i> directions (main wallet to a game
 *       wallet, and a game wallet back to the main wallet), matching the shape
 *       {@code ManualTransferNewUiTest#amountAboveTheMainWalletIsRefused} /
 *       {@code #moreThanTheWalletBalanceIsRefused} already assume - so that case's own
 *       submit-and-look-for-"fail" branch is not expected to run for an over-balance amount
 *       specifically.</li>
 *   <li>The "fail" wording itself <b>is</b> confirmed reachable, just not from an over-balance
 *       amount: submitting into a game wallet the live "Game Balance" table shows as
 *       "Maintenance" (confirmed live 2026-10-25, WORLD ENTERTAINMENT that day) refuses with
 *       precisely "The point transfer fail" - Submit stays enabled and the server does the
 *       refusing, unlike every other refusal in this class. A case walking every offered wallet
 *       (see {@link #allOfferedGames()}) treats a "fail" refusal as an acceptable outcome for a
 *       wallet, not a defect, for exactly this reason.</li>
 * </ul>
 *
 * <h2>Confirmed live: a transient "Data is being processed" lock after a transfer</h2>
 * Submitting a transfer (or pressing "Transfer All Out") within a couple of seconds of a previous
 * one on the same account is refused with "Data is being processed. Please try again shortly." -
 * even though both the earlier and the attempted transfer are individually valid. This resolved
 * on its own after a few seconds plus a page refresh in manual testing. {@link #submit()} and
 * {@link #transferAllOut()} now retry through this specific refusal a bounded number of times
 * with a short pause, rather than reporting it as a refusal of the transfer itself - see
 * {@link #clickAndRetryIfProcessing(Locator, String)}.
 *
 * <p>One thing in this file was already confirmed rather than guessed before today: the currency
 * codes {@link #TRAILING_BALANCE} strips from an option label - HKD, USDT, TRX, USD, THB - are
 * exactly the wallet currencies this project's own {@code CurrencyBannerTest} already exercises
 * against this site, per the README's "Known defects" section.
 *
 * <h2>2026-10-24: {@link #dismissAlert()} was a real bug, now fixed</h2>
 * Si reported real failures from a local run of {@code ManualTransferNewUiTest}/
 * {@code ManualTransferClassicUiTest}: TRANSFER-013/014 (New UI) threw Playwright
 * {@code TimeoutError}s clicking "Transfer All Out"/the "To" box - "intercepts pointer events"
 * against a still-open {@code AlertBoard_bg} backdrop - and TRANSFER-028 (Classic UI, the
 * every-wallet walk) reported "Submit never became enabled" for 46 of 47 wallets after the first
 * one reported neither a success nor a "fail" message. Confirmed live against both UI versions:
 * {@code dismissAlert()}'s {@code getByRole(BUTTON, name="close")} lookup, scoped inside
 * {@code board.first()}, always found zero matches - not because no Close control exists, but
 * because this site renders every {@code AlertBoard} (the backdrop <i>and</i> the wrapper holding
 * its Close control(s)) under {@code aria-hidden="true"}, on both UI versions. {@code aria-hidden}
 * removes the whole subtree from the accessibility tree that role-based locators query, so the
 * lookup silently found nothing, the alert was never actually closed, and the next click this
 * method exists to protect hung against the still-visible backdrop until its caller's full action
 * timeout. Confirmed live that the backdrop never auto-dismisses on its own (watched one sit open
 * for 5+ seconds) and that it covers the <i>entire</i> page, not just the dialog box - a click
 * anywhere else on the page while it is open does not reach its target. Fixed by replacing the
 * {@code getByRole} lookup with a plain CSS {@code button} search filtered by the button's own
 * rendered text ({@code hasText("close")}), which reads the DOM directly and is unaffected by
 * {@code aria-hidden} - confirmed this finds the real Close control on both UI versions (New UI
 * carries two "Close"-named controls inside the board, an icon and a text button; Classic UI
 * carries exactly one, with no distinguishing class at all, so matching by rendered text rather
 * than by class was also the only approach that works on both).
 */
public class TransferPage extends BasePage {

    /** Copied from Weibo Empire's own path. See the class Javadoc - entirely unconfirmed here. */
    public static final String WALLET_PATH = "/en/member/wallet/";

    /** The placeholder "To" shows before anything has been chosen - a guess, see class Javadoc. */
    public static final String NOTHING_SELECTED = "Please select";

    /** The "From"/"To" boxes' own stable class, confirmed live on both UI versions - see the
     *  class Javadoc's {@link #fromBox()}/{@link #toBox()} entry for why the previous guess
     *  (a CSS match on "select"/"dropdown") only worked on New UI by coincidence. */
    private static final String TRANSFER_LABEL_BOX = "[class*='Wallet_transfer-label' i]";

    private static final String OPTION_PANEL = "div[class*='absolute' i][class*='z-' i]";
    private static final String OPTION = "[class*='option' i]";
    private static final String MESSAGE_BOARD =
            "[class*='AlertBoard' i], [class*='alert' i], [class*='toast' i], "
                    + "[class*='modal' i][class*='message' i]";

    /** The site's own transient lock, shown when a transfer is submitted too soon after another -
     *  see the class Javadoc's "Confirmed live: a transient 'Data is being processed' lock" note. */
    private static final Pattern DATA_PROCESSING =
            Pattern.compile("being processed", Pattern.CASE_INSENSITIVE);

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
        return page.locator(TRANSFER_LABEL_BOX).nth(0);
    }

    private Locator toBox() {
        return page.locator(TRANSFER_LABEL_BOX).nth(1);
    }

    private Locator submitButton() {
        return page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("^submit$",
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
     * The "To" list's own first page, with its furniture removed - the placeholder, the main
     * wallet and a "Show More" pager - leaving the raw wallet labels (name and balance together)
     * in the order offered. Generalized from Weibo Empire's {@code offeredGames()}: nothing here
     * names an actual game, since which games (if any) Quark offers is unknown.
     *
     * <p><b>First page only</b> - confirmed live 2026-10-25: this account's "To" list offers far
     * more than fits on one page (47 game wallets across several "Show More" clicks that day), and
     * this method never clicks "Show More", so it reads only however many the page renders before
     * any expansion. See {@link #allOfferedGames()} for the fully-expanded list.
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

    /**
     * As {@link #offeredGames()}, but clicks every "Show More" page first, so the result is
     * every game wallet the "To" list offers at all - not just whatever fit on the first page.
     */
    public List<String> allOfferedGames() {
        openToOptions();
        List<String> full = expandOptions();
        closeOptions();
        List<String> wallets = new ArrayList<>();
        for (String label : full) {
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

    /** Presses Submit, retrying through the site's own transient "Data is being processed" lock
     *  if it appears - see the class Javadoc. Any case calling this with an amount that could be
     *  accepted belongs in {@code creates-data}. */
    public void submit() {
        CaseLog.step("Submitting the transfer");
        clickAndRetryIfProcessing(submitButton(), "Submit");
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

    /** Presses "Transfer All Out", retrying through the site's own transient "Data is being
     *  processed" lock if it appears - see the class Javadoc. This moves real money if it works
     *  at all, so any case calling it belongs in {@code creates-data}. */
    public void transferAllOut() {
        CaseLog.step("Pressing 'Transfer All Out'");
        clickAndRetryIfProcessing(transferAllOutButton(), "Transfer All Out");
    }

    /**
     * Clicks {@code button}, and if the result is the site's own transient "Data is being
     * processed. Please try again shortly." lock - confirmed live 2026-10-24, see the class
     * Javadoc - dismisses it, waits for the lock to clear, and tries again, up to 3 attempts
     * total. Any other outcome (success, a real refusal, nothing at all) is left exactly as the
     * click produced it, for the caller's own {@link #waitForMessage}/{@link #isSubmitEnabled()}
     * to judge.
     *
     * <h2>Clears a leftover alert before clicking, not only after</h2>
     * Confirmed live 2026-10-24: a result alert from an earlier action (a prior submit's own
     * "Transfer Successful!", say) can still be on screen, at near-maximum z-index, when this is
     * called - every case in this family of tests reads that earlier alert with
     * {@link #waitForMessage} before moving on, but reading it does not close it. An alert in
     * that state intercepts the click on {@code button} outright, so Playwright's own auto-wait
     * retries against it until its full action timeout and throws, long before this method's own
     * "Data is being processed" handling ever gets a chance to run. {@link #dismissAlert()} is
     * called first for exactly this reason, not only inside the retry branch below.
     */
    private void clickAndRetryIfProcessing(Locator button, String what) {
        dismissAlert();
        for (int attempt = 1; attempt <= 3; attempt++) {
            button.first().click();
            page.waitForTimeout(1_200);
            String popup = alertText();
            if (!DATA_PROCESSING.matcher(popup).find()) {
                return;
            }
            CaseLog.step("'" + what + "' hit the site's transient \"Data is being processed\" "
                    + "lock (attempt " + attempt + "/3) - waiting and retrying.");
            dismissAlert();
            page.waitForTimeout(3_000);
        }
    }

    /**
     * Closes whichever {@link #MESSAGE_BOARD} alert is up, by its own Close control, and waits
     * until none is visible anymore before returning - public so a case walking several transfers
     * in a row (e.g. one into every game wallet) can clear a result before starting the next one.
     *
     * <p><b>Polls rather than clicking once</b> - confirmed live 2026-10-25: a single click
     * followed by a short fixed wait left the alert still visible, and still intercepting clicks,
     * often enough to time out the very next action in a fast-moving loop (every TRANSFER-013/014
     * style case that funds a wallet and immediately acts again). The same reasoning
     * {@code BasePage#dismissWelcomeBanners} already applies to this site's welcome banner:
     * closing is not instantaneous, so confirm it rather than assume it. Bounded at 5 attempts so
     * a genuinely stuck alert surfaces as a loud timeout from whatever called this, rather than
     * this method looping forever.
     */
    public void dismissAlert() {
        for (int attempt = 0; attempt < 5; attempt++) {
            Locator board = page.locator(MESSAGE_BOARD).filter(new Locator.FilterOptions().setVisible(true));
            if (board.count() == 0) {
                return;
            }
            // Not getByRole: confirmed live 2026-10-24 that this site renders every AlertBoard -
            // both the backdrop and the wrapper around its Close control(s) - under
            // aria-hidden="true", on both UI versions. aria-hidden removes that whole subtree
            // from the accessibility tree, so a role-based lookup inside it always finds zero
            // matches, silently - the alert was never actually closed, and the click this method
            // exists to protect then hangs for the caller's full action timeout against the
            // still-open backdrop. A plain CSS lookup, filtered by the button's own rendered
            // text, reads the DOM directly and is unaffected by aria-hidden.
            Locator close = board.first().locator("button")
                    .filter(new Locator.FilterOptions().setHasText(
                            Pattern.compile("close", Pattern.CASE_INSENSITIVE)));
            if (close.count() > 0) {
                try {
                    close.first().click(new Locator.ClickOptions().setTimeout(3_000));
                } catch (PlaywrightException notClickable) {
                    // Try again next loop, or fall through to the bounded exit if attempts run out.
                }
            }
            page.waitForTimeout(400);
        }
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
