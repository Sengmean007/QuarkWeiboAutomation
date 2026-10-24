package org.sengmean.page.wallet;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import org.sengmean.base.BasePage;

import java.util.regex.Pattern;

/**
 * The currency badge in the signed-in header - "USDT", "HKD", etc. - and the "Fiat Currency
 * Games" dropdown it opens, listing every currency wallet the account holds (USDT, TRX, THB,
 * HKD, USD on qa00002) with its own balance, one row per currency.
 *
 * <h2>Rewritten 2026-10-24: the previous version only ever worked on New UI's wallet page</h2>
 * The original implementation (confirmed live 2026-10-25 against Classic UI, at the time) was
 * built entirely around the always-visible "Account / Balance / Remain T.O" side panel
 * ({@code CurrencyCard_row}). That panel turned out to be <b>New UI only</b> - confirmed live
 * 2026-10-24 that {@code [class*='CurrencyCard_row' i]} matches zero elements anywhere on
 * Classic UI (checked both its home page and its wallet page), so every Classic UI suite that
 * called {@link #selectCurrency(String)} - including the per-class calls this class's own
 * Javadoc used to credit as "confirmed live" - was silently doing nothing, the whole time. Those
 * suites only ever looked like they worked because the account's currency is shared, account-
 * level state: whichever currency a New UI suite happened to leave it on carried over and made a
 * Classic UI run coincidentally pass, right up until a run order left the account on something
 * other than HKD and nothing on the Classic UI side was able to correct it.
 *
 * <p>Confirmed live the header currency badge - a small "USDT"/"HKD"/etc. control, always
 * present once signed in, {@code role="button"} - and the dropdown it opens exist identically on
 * <b>both</b> UI versions and on <b>every</b> signed-in page tried (home, wallet), not just the
 * wallet page the old panel needed. Rewritten entirely around that badge instead - see
 * {@link #trigger()} and {@link #DROPDOWN_ROW} - so a switch works from wherever
 * {@code BaseTest#signIn} is, immediately after logging in, on either UI version.
 */
public class CurrencySwitcher extends BasePage {

    /** Every currency code this account is confirmed to hold - also what the header badge's own
     *  text is anchored against, so a stray badge from an unrelated control is never matched. */
    private static final Pattern CURRENCY_CODE = Pattern.compile("^(HKD|USDT|TRX|THB|USD)$");

    /** A row in the "Fiat Currency Games" dropdown the badge opens - confirmed live on both UI
     *  versions under this one shared class prefix, different hash suffix per version. */
    private static final String DROPDOWN_ROW = "[class*='Header_auto-payout-currency-list' i]";

    public CurrencySwitcher(Page page) {
        super(page);
    }

    /**
     * The header currency badge - confirmed live to render twice (a desktop and a mobile copy,
     * same as the old trigger this replaces), only one of which is visible at a time depending on
     * viewport, hence filtering to the visible one rather than taking {@code .first()} blind.
     */
    private Locator trigger() {
        return page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName(CURRENCY_CODE))
                .filter(new Locator.FilterOptions().setVisible(true));
    }

    /** A dropdown row by its own code, anchored so "USD" cannot also match "USDT". */
    private Locator row(String code) {
        return page.locator(DROPDOWN_ROW)
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^" + Pattern.quote(code) + "\\b",
                                Pattern.CASE_INSENSITIVE)));
    }

    /**
     * Picks a currency by its code, e.g. "HKD" - a no-op once the badge already reads it. Opens
     * the dropdown by clicking the badge, clicks the matching row, and confirms the badge itself
     * now reads the new code before returning, retrying rather than trusting one click - the same
     * "confirm, don't assume" shape {@code TransferPage#dismissAlert} already uses on this site.
     *
     * <p>Bounded at 10 attempts with a short wait between them, rather than giving up on the
     * first look - confirmed live 2026-10-24 that calling this immediately after signing in, with
     * no navigation or settle time first, can hit a page where the badge has not rendered yet;
     * waiting and rechecking costs nothing on the common case (the badge is already there) and
     * recovers that one instead of silently doing nothing for the rest of the run. If it still
     * cannot switch after every attempt, it says so on the console rather than staying silent.
     *
     * <h2>Every click is wrapped: a blocked click must never crash the caller</h2>
     * {@code BaseTest#signIn} calls this right after dismissing the site's own overlays, and this
     * site is confirmed (see {@code TransferPage#dismissAlert}) to sometimes leave one open a
     * little longer than expected, or to raise a new one (a promo popup, "Lucky Spin") on its own
     * timer. A click Playwright cannot complete because something is covering the badge would
     * otherwise throw after this project's full {@code test.actionTimeout} and take the whole
     * {@code @BeforeAll} down with it - every case in the class, not just the currency switch.
     * Every click here is caught for exactly that reason: a failed click just means "try again
     * next attempt", never an exception out of this method.
     */
    public void selectCurrency(String code) {
        for (int attempt = 1; attempt <= 10; attempt++) {
            if (code.equalsIgnoreCase(currentCurrency())) {
                return;
            }
            Locator badge = trigger();
            if (badge.count() == 0 || !click(badge.first())) {
                page.waitForTimeout(500);
                continue;
            }
            page.waitForTimeout(400);
            Locator target = row(code);
            if (target.count() > 0 && click(target.first())) {
                // The site re-renders the whole lobby on a currency switch - confirmed
                // 2026-10-16 to take a couple of seconds and, on THB/USD/TRX/USDT, to leave the
                // hero banner missing or wrong; see HomePage.hasHeroBanner for the regression
                // this timing exists to let settle first.
                page.waitForTimeout(2_000);
            } else {
                // Nothing matched, or the row click failed - close the dropdown again rather than
                // leaving it open for the next attempt's click on the badge to fight with.
                click(badge.first());
                page.waitForTimeout(300);
            }
        }
        System.out.println("CurrencySwitcher: could not switch to \"" + code + "\" - no "
                + "matching dropdown row was ever clickable (currently on \"" + currentCurrency()
                + "\"). Either this account does not hold that currency, or the header badge "
                + "never rendered on this page.");
    }

    /** Clicks {@code target}, swallowing a timeout from something covering it - see
     *  {@link #selectCurrency(String)}'s own Javadoc for why this must never throw. Returns
     *  whether the click actually happened. */
    private boolean click(Locator target) {
        try {
            target.click(new Locator.ClickOptions().setTimeout(3_000));
            return true;
        } catch (PlaywrightException notClickable) {
            return false;
        }
    }

    /** The currency code the header badge currently reads, e.g. "HKD", or "" if the badge is not
     *  on screen at all (signed out, or a page this has not been confirmed on). */
    public String currentCurrency() {
        Locator badge = trigger();
        if (badge.count() == 0) {
            return "";
        }
        String text = badge.first().innerText();
        return text == null ? "" : text.trim();
    }
}
