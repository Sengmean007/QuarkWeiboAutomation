package org.sengmean.page.wallet;

import com.microsoft.playwright.Page;
import org.sengmean.base.BasePage;
import org.sengmean.support.GameCategory;

/**
 * One of the site's top-level game-category pages (Slots, Horse Race, Lives Casino, ...).
 *
 * <p>Opened by a direct {@link #navigateTo(GameCategory)} rather than by clicking the nav bar
 * button a device case would otherwise have to find - the same {@code page.navigate()} shape
 * {@link org.sengmean.page.auth.LoginPage#navigateTo()} and {@link HomePage#navigateTo()} already
 * use. Confirmed live against test-v2.138hk.vip on 2026-10-22: landing on a category path cold,
 * with no {@code ui-version} choice in {@code localStorage} yet, renders Classic UI silently with
 * no picker at all - unlike the home page, which always asks. So a case that needs a specific UI
 * version still has to answer the picker on the home page first and only then navigate on to the
 * category page; see {@code ResponsiveGameCategoriesNewUiTest}'s Javadoc for the full shape.
 */
public class GameCategoryPage extends BasePage {

    public GameCategoryPage(Page page) {
        super(page);
    }

    public void navigateTo(GameCategory category) {
        open(category.path);
    }
}
