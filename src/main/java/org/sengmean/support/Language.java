package org.sengmean.support;

/**
 * The languages this site's header offers, confirmed live against test-v2.138hk.vip on
 * 2026-10-23 (clicking the globe icon in the header - not discoverable any other way; nothing
 * in this project's earlier Javadoc or README had looked for it before, and both had concluded
 * there was "no evidence" the site offered more than English). Eleven besides English: Thai,
 * Traditional Chinese, Simplified Chinese, Japanese, Vietnamese, Spanish, Portuguese, Malay,
 * Indonesian, Filipino, Bengali.
 *
 * <p>{@link #label} is the language's own English name as the dropdown row prints it before the
 * "|"/"│" separator (e.g. "Traditional Chinese│繁體") - stable across whichever script the rest
 * of the row is written in, which is why {@link org.sengmean.base.BasePage#selectLanguage} matches
 * on it rather than on the native-script half.
 */
public enum Language {
    ENGLISH("English"),
    THAI("Thai"),
    TRADITIONAL_CHINESE("Traditional Chinese"),
    SIMPLIFIED_CHINESE("Simplified Chinese"),
    JAPANESE("Japanese"),
    VIETNAMESE("Vietnamese"),
    SPANISH("Spanish"),
    PORTUGUESE("Portuguese"),
    MALAY("Malay"),
    INDONESIAN("Indonesian"),
    FILIPINO("Filipino"),
    BENGALI("Bengali");

    /** The dropdown row's own English name for this language, e.g. "Traditional Chinese". */
    public final String label;

    Language(String label) {
        this.label = label;
    }
}
