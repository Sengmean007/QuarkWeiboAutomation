package org.sengmean.support;

/**
 * The site's static content pages - About Us, legal, and the How-to guides.
 *
 * <h2>Selector confidence</h2>
 * Confirmed live against test-v2.138hk.vip on 2026-10-22, read as real {@code <a href>} links out
 * of Classic UI's own footer ("SITE MAPS" / "QUICK LINK" / "HELP CENTER"), not guessed - unlike
 * most of this project's locators. New UI's home page renders no footer at all and links to none
 * of these pages anywhere, but every path here was confirmed to still render correctly under New
 * UI when opened directly (same site-wide behaviour as {@link GameCategory}'s paths) - see
 * {@code ResponsiveStaticPagesNewUiTest}'s Javadoc.
 */
public enum StaticPage {

    ABOUT_US("About Us", "/en/about-us/"),
    RESPONSIBLE_GAMING("Responsible Gaming", "/en/betting-responsibility/"),
    BANKING("Banking Options", "/en/bank/"),
    TERMS_OF_USE("Terms of Use", "/en/rules-terms/"),
    PRIVACY_POLICY("Privacy Policy", "/en/privacy-policy/"),
    HOW_TO_REGISTER("How to Register", "/en/how-to-register/"),
    HOW_TO_DEPOSIT("How to Deposit", "/en/how-to-deposit/"),
    HOW_TO_WITHDRAW("How to Withdrawal", "/en/how-to-withdrawal/");

    /** The footer link's own visible label. */
    public final String label;

    /** Path relative to {@code BasePage.BASE_URL}, e.g. "/en/about-us/". */
    public final String path;

    StaticPage(String label, String path) {
        this.label = label;
        this.path = path;
    }

    @Override
    public String toString() {
        return label;
    }
}
