package org.sengmean.tests.wallet;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sengmean.base.BaseTest;
import org.sengmean.page.wallet.CurrencySwitcher;
import org.sengmean.page.wallet.HomePage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression cases for the hero-banner defect found manually on 2026-10-16: the landing page's
 * promo banner renders correctly only while the wallet is on HKD. See
 * {@link org.sengmean.page.wallet.HomePage} for the full write-up of what each currency showed.
 *
 * <p>These cases assert the banner <b>should</b> be there, so they currently fail on USDT, TRX,
 * THB and USD - that failure is the regression signal. They will go green on their own the day
 * the defect is fixed, with nothing here needing to change.
 */
public class CurrencyBannerTest extends BaseTest {

    @ParameterizedTest(name = "hero banner is missing or broken on {0} (known defect)")
    @DisplayName("Known defect: hero banner breaks on every currency except HKD")
    @ValueSource(strings = {"USDT", "TRX", "THB", "USD"})
    void heroBanner_missingOnNonHkdCurrencies(String currency) {
        Page page = page();
        signIn(page);
        new CurrencySwitcher(page).selectCurrency(currency);

        HomePage home = new HomePage(page);
        page.navigate(org.sengmean.base.BasePage.url("/en/"));
        page.waitForLoadState();

        assertTrue(home.hasHeroBanner(),
                "Expected a hero banner under " + currency + " - known defect as of 2026-10-16: "
                        + "USDT/TRX/USD show no banner at all, THB shows a broken placeholder "
                        + "image (a ladybug on desktop, a QR code on mobile). Reproduced after a "
                        + "full page reload, so this is not a transient re-render glitch.");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Baseline: hero banner renders correctly on HKD")
    void heroBanner_rendersOnHkd() {
        Page page = page();
        signIn(page);
        new CurrencySwitcher(page).selectCurrency("HKD");

        HomePage home = new HomePage(page);
        page.navigate(org.sengmean.base.BasePage.url("/en/"));
        page.waitForLoadState();

        assertTrue(home.hasHeroBanner(), "Expected the hero banner on HKD, the account default.");
    }
}
