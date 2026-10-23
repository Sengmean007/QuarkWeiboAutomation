package org.sengmean.tests.auth;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.wallet.HomePage;
import org.sengmean.report.CaseLog;
import org.sengmean.support.Language;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms the login form's own validation message is <b>actually translated</b>, not just the
 * surrounding chrome, for every language the site's header offers.
 *
 * <h2>Why this exists</h2>
 * This project had twice concluded there was "no evidence" Quark Dynasty offered more than
 * English ({@code HomePageNewUiTest}'s Javadoc, the README's "Ported-but-unconfirmed areas") -
 * both times without opening the header's own language menu, confirmed live (2026-10-23) to
 * offer twelve: English and {@link Language}'s other eleven. A UI can translate every static
 * label on the page - headings, button text, placeholders - while a server-rendered validation
 * message (an alert board, a toast) still comes back hard-coded in one language, because that
 * string is not necessarily driven by the same i18n pipeline as the page's own markup. That gap
 * is exactly what this class checks for the login form's wrong-credentials board, the one
 * validation message every {@code LoginNewUiTest}/{@code LoginClassicUiTest} case already
 * exercises in English (LOGIN-003/015).
 *
 * <h2>How a case gets there</h2>
 * {@link #verifyTranslated(Language)} opens the home page as a visitor, calls
 * {@link org.sengmean.base.BasePage#selectLanguage(Language)} to choose the language under test,
 * then opens the login form through the header's own control
 * ({@link LoginPage#openFromHeaderInCurrentLanguage()}) rather than
 * {@link LoginPage#navigateTo()} - that method's path is hard-coded to {@code /en/log-in/} and
 * confirmed live to silently force the page back to English, which would have made every
 * non-English case in this class pass for the wrong reason (the page never left English at all).
 * Wrong credentials are submitted, and {@link LoginPage#errorMessage()} - now read by a
 * structural, language-agnostic selector rather than by matching the English wording itself, see
 * that method's own Javadoc - is checked against the known English wording ("wrong username"):
 * present for the {@link Language#ENGLISH} control case, absent for every other language.
 *
 * <h2>Case ids</h2>
 * {@code LOGIN-LANG-001} ({@link Language#ENGLISH}, the control) through
 * {@code LOGIN-LANG-012} ({@link Language#BENGALI}), in {@link Language}'s own declaration order.
 * Invented, as there is no client test-case workbook to take ids from (same situation as the
 * {@code LOGIN-}/{@code UI-} prefixes). One case per language, rather than one looping case,
 * mirrors how {@code ResponsiveLayoutNewUiTest} gives each device its own row - a language that
 * fails to translate shows up as that language's own red row, not as a single case whose failure
 * message has to be read to learn which of twelve languages was the problem.
 */
public class LoginMultiLanguageTest extends BaseTest {

    private static final String WRONG_USERNAME = "wronguser1";
    private static final String WRONG_PASSWORD = "wrongpass";

    @Test
    @DisplayName("LOGIN-LANG-001: the wrong-credentials message shows its known English wording under English (control)")
    void english() {
        verifyTranslated(Language.ENGLISH);
    }

    @Test
    @DisplayName("LOGIN-LANG-002: the wrong-credentials message is translated under Thai")
    void thai() {
        verifyTranslated(Language.THAI);
    }

    @Test
    @DisplayName("LOGIN-LANG-003: the wrong-credentials message is translated under Traditional Chinese")
    void traditionalChinese() {
        verifyTranslated(Language.TRADITIONAL_CHINESE);
    }

    @Test
    @DisplayName("LOGIN-LANG-004: the wrong-credentials message is translated under Simplified Chinese")
    void simplifiedChinese() {
        verifyTranslated(Language.SIMPLIFIED_CHINESE);
    }

    @Test
    @DisplayName("LOGIN-LANG-005: the wrong-credentials message is translated under Japanese")
    void japanese() {
        verifyTranslated(Language.JAPANESE);
    }

    @Test
    @DisplayName("LOGIN-LANG-006: the wrong-credentials message is translated under Vietnamese")
    void vietnamese() {
        verifyTranslated(Language.VIETNAMESE);
    }

    @Test
    @DisplayName("LOGIN-LANG-007: the wrong-credentials message is translated under Spanish")
    void spanish() {
        verifyTranslated(Language.SPANISH);
    }

    @Test
    @DisplayName("LOGIN-LANG-008: the wrong-credentials message is translated under Portuguese")
    void portuguese() {
        verifyTranslated(Language.PORTUGUESE);
    }

    @Test
    @DisplayName("LOGIN-LANG-009: the wrong-credentials message is translated under Malay")
    void malay() {
        verifyTranslated(Language.MALAY);
    }

    @Test
    @DisplayName("LOGIN-LANG-010: the wrong-credentials message is translated under Indonesian")
    void indonesian() {
        verifyTranslated(Language.INDONESIAN);
    }

    @Test
    @DisplayName("LOGIN-LANG-011: the wrong-credentials message is translated under Filipino")
    void filipino() {
        verifyTranslated(Language.FILIPINO);
    }

    @Test
    @DisplayName("LOGIN-LANG-012: the wrong-credentials message is translated under Bengali")
    void bengali() {
        verifyTranslated(Language.BENGALI);
    }

    // ----------------------------------------------------------------- helper

    /**
     * Chooses {@code language} from a fresh visitor's home page, opens the login form through the
     * header (not {@link LoginPage#navigateTo()} - see this class's own Javadoc for why),
     * submits wrong credentials, and checks the board that comes back: non-blank always, and -
     * for every language but the English control - not the known English wording, which is what
     * actually confirms a translation happened rather than the site silently falling back to
     * English for this one string while everything else around it translated.
     */
    private void verifyTranslated(Language language) {
        Page page = page();
        HomePage home = new HomePage(page);
        home.navigateTo();
        home.dismissOverlays();
        // The UI-version picker's backdrop sits above everything, the language toggle included -
        // confirmed live (2026-10-23): a click aimed at the toggle while the picker is still up
        // lands on the backdrop instead (z-index 2147483003), not the button underneath it. Every
        // other UI-split suite answers this picker before touching anything else on the page for
        // the same reason; New UI vs Classic UI is not what this class is checking, so New UI is
        // just the fixed choice made here.
        home.chooseNewUi();
        home.dismissOverlays();
        CaseLog.data("Language", language.label);
        home.selectLanguage(language);

        LoginPage login = new LoginPage(page);
        login.openFromHeaderInCurrentLanguage();
        login.fillUsername(WRONG_USERNAME);
        login.fillPassword(WRONG_PASSWORD);
        login.fillVerificationCode(CAPTCHA);
        login.submit();
        page.waitForTimeout(1_500);

        String message = login.errorMessage();
        CaseLog.data("Wrong-credentials board", message.isBlank() ? "(blank)" : message);
        assertFalse(message.isBlank(),
                "Expected a wrong-credentials message board to show under " + language.label
                        + " - none was found (or it was not structurally an AlertBoard/Toast/"
                        + "role=alert element, which is how this case looks for one regardless "
                        + "of language).");

        boolean englishWording = message.toLowerCase().contains("wrong username");
        if (language == Language.ENGLISH) {
            assertTrue(englishWording,
                    "Control case: expected the known English wording (\"Wrong username/"
                            + "password...\") while English is selected, to confirm this case's "
                            + "own harness actually works before trusting a translated-language "
                            + "result - got: " + message);
        } else {
            assertFalse(englishWording,
                    "Expected the wrong-credentials message to be translated under "
                            + language.label + " rather than staying in English - got: " + message);
        }
    }
}
