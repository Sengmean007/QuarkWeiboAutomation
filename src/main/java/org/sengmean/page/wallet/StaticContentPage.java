package org.sengmean.page.wallet;

import com.microsoft.playwright.Page;
import org.sengmean.base.BasePage;
import org.sengmean.support.StaticPage;

/**
 * One of the site's static content pages (About Us, a legal page, a How-to guide) - see
 * {@link StaticPage} for how each path was confirmed. Opened by a direct
 * {@link #navigateTo(StaticPage)}, the same shape {@link GameCategoryPage} already uses and for
 * the same reason: reliable across every device viewport, with no dependency on finding and
 * clicking a footer link that New UI does not even render.
 */
public class StaticContentPage extends BasePage {

    public StaticContentPage(Page page) {
        super(page);
    }

    public void navigateTo(StaticPage staticPage) {
        open(staticPage.path);
    }
}
