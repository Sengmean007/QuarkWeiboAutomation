package org.sengmean.page.wallet;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.sengmean.base.BasePage;

/**
 * The wallet currency dropdown in the signed-in header - the small "▼ HKD 365.37" control that
 * opens a list of every currency the account holds (USDT, TRX, THB, HKD, USD on qa00002,
 * confirmed 2026-10-16) with its balance.
 *
 * <p>Same selector-confidence caveat as {@link org.sengmean.page.auth.LoginPage}: the dropdown
 * trigger is matched on the currency code text rather than a confirmed class name.
 */
public class CurrencySwitcher extends BasePage {

    public CurrencySwitcher(Page page) {
        super(page);
    }

    private Locator trigger() {
        // The header shows "<code> <balance>" next to a small down-caret; matching on the
        // balance row's container by its nearest currency-code text is the only trait seen in
        // the manual session.
        return page.locator("[class*='currency' i], [class*='wallet' i]").first();
    }

    public void open() {
        trigger().click();
    }

    /** Picks a currency by its three-letter code, e.g. "THB". Opens the dropdown if needed. */
    public void selectCurrency(String code) {
        open();
        page.getByText(code, new Page.GetByTextOptions().setExact(true)).first().click();
        // The site re-renders the whole lobby on a currency switch - confirmed 2026-10-16 to take
        // a couple of seconds and, on THB/USD/TRX/USDT, to leave the hero banner missing or wrong;
        // see HomePage.hasHeroBanner for the regression this timing exists to let settle first.
        page.waitForTimeout(2_000);
    }

    /** The currency code currently shown in the header, e.g. "HKD". */
    public String currentCurrency() {
        return trigger().innerText().trim();
    }
}
