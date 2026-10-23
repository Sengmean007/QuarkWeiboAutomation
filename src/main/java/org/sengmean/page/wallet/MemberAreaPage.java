package org.sengmean.page.wallet;

import com.microsoft.playwright.Page;
import org.sengmean.base.BasePage;
import org.sengmean.support.MemberPage;

/**
 * One of the signed-in member area's own pages (Wallet, Deposit, Records, ...) - see
 * {@link MemberPage} for how each path was confirmed. Opened by a direct
 * {@link #navigateTo(MemberPage)} after the caller has already signed in
 * ({@link org.sengmean.base.BaseTest#signIn(Page)}), the same shape {@link GameCategoryPage} and
 * {@link StaticContentPage} already use.
 */
public class MemberAreaPage extends BasePage {

    public MemberAreaPage(Page page) {
        super(page);
    }

    public void navigateTo(MemberPage memberPage) {
        open(memberPage.path);
    }
}
