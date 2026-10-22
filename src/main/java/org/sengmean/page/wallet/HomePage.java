package org.sengmean.page.wallet;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.sengmean.base.BasePage;

/**
 * The landing page, scoped here to just the one element the currency-switch regression cases
 * need: the hero promo banner carousel directly under the top nav.
 *
 * <h2>Known defect (2026-10-16)</h2>
 * On the account used for manual testing (qa00002), the hero banner renders correctly only on
 * HKD, the account's default currency. Switching to USDT, TRX or USD leaves the banner section
 * empty - it collapses cleanly, no broken layout, just no image. Switching to THB is worse: the
 * banner shows a broken placeholder image unrelated to the site (a stock photo of a ladybug on a
 * desktop viewport, a QR code image on a phone viewport), reproducible after a full page reload.
 * See {@code org.sengmean.tests.wallet.CurrencyBannerTest} for the regression cases this backs.
 */
public class HomePage extends BasePage {

    public HomePage(Page page) {
        super(page);
    }

    public void navigateTo() {
        open("/en/");
    }

    private static final String HERO_BANNER =
            "div:has(> img):below(nav), [class*='banner' i][class*='hero' i], [class*='carousel' i]";

    /** True when a hero banner image is actually rendered under the nav - not just present as a
     *  collapsed, zero-height container. */
    public boolean hasHeroBanner() {
        Locator banner = page.locator(HERO_BANNER).first();
        if (banner.count() == 0) {
            return false;
        }
        var box = banner.boundingBox();
        return box != null && box.height > 40;
    }
}
