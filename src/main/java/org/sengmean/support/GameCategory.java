package org.sengmean.support;

/**
 * The top-level game categories in the main nav bar, common to both New UI and Classic UI.
 *
 * <h2>Selector confidence</h2>
 * Confirmed live against test-v2.138hk.vip on 2026-10-22 by clicking every nav button in turn and
 * reading back the resulting {@code location.href}. The numeric path is the site's own category
 * id and is stable across UI version - New UI and Classic UI render the same path under a
 * different theme, confirmed by switching UI version on the same path and re-reading it.
 *
 * <h2>Why nine, not Classic UI's own tenth</h2>
 * Classic UI's nav bar (and its footer "Site Maps" section) carries a tenth entry, Promotions,
 * that New UI's nav does not expose at all. Left out here so a suite built from this enum covers
 * the same nine screens on both UI versions, rather than New UI silently covering fewer pages
 * than Classic UI; see {@code ResponsiveGameCategoriesNewUiTest}'s Javadoc.
 */
public enum GameCategory {

    LIVE_STREAM("真人直播", "/en/79/"),
    SLOTS("Slots Game", "/en/17/"),
    HORSE_RACE("Horse Race", "/en/11/"),
    LIVE_CASINO("Lives Casino", "/en/12/"),
    E_SPORT("E-Sport", "/en/13/"),
    FISH_GAME("Fish Game", "/en/14/"),
    LOTTERIES("Lotteries", "/en/15/"),
    SPORT("Sport", "/en/10/"),
    CARD_GAME("Card Game", "/en/16/");

    /** The nav button's own visible label. */
    public final String label;

    /** Path relative to {@code BasePage.BASE_URL}, e.g. "/en/17/" - confirmed stable across UI version. */
    public final String path;

    GameCategory(String label, String path) {
        this.label = label;
        this.path = path;
    }

    @Override
    public String toString() {
        return label;
    }
}
