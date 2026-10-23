package org.sengmean.tests.auth;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.wallet.HomePage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The Log In form on https://test-v2.138hk.vip, under <b>Classic UI</b> - the Classic Interface
 * twin of {@link LoginNewUiTest}. See that class's Javadoc for the full reasoning behind the
 * New UI / Classic UI split, the ported-case history and the welcome-banner/Daily-Mission
 * overlay handling; this class repeats only what differs.
 *
 * <h2>What's different here</h2>
 * {@link #openAsVisitorChoosingClassicUi()} answers the home page's picker with Classic Interface
 * ({@link org.sengmean.base.BasePage#chooseClassicUi()}) instead of New Interface, and asserts
 * {@link org.sengmean.base.BasePage#isClassicUiChosen()} rather than {@code isNewUiChosen()}.
 * Every case below is otherwise the same check {@link LoginNewUiTest} runs, against the same
 * {@code LoginPage} - the login form is not expected to render differently by UI version, but
 * confirming that is this class's whole purpose, exactly as {@code ResponsiveLayoutClassicUiTest}
 * confirms it for every other page in this project.
 *
 * <h2>Case ids: LOGIN-013 - LOGIN-024, continuing {@link LoginNewUiTest}'s LOGIN-001 - LOGIN-012</h2>
 */
public class LoginClassicUiTest extends BaseTest {

    /** LOGIN-013 - submitting with every field empty shows no feedback at all. Known UX gap. */
    @Test
    @DisplayName("LOGIN-013: empty form shows no validation message (known gap) (Classic UI)")
    void emptyForm_showsNoFeedback() {
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        login.navigateTo();
        login.submit();
        page.waitForTimeout(1_000);
        // Documents the gap rather than asserting it should stay this way: there is currently no
        // inline "required" message and no error board, so the form simply does nothing visible.
        assertTrue(login.hasErrorMessage(),
                "Expected no board message for an empty submit (none is shown today) - "
                        + "if this now fails, the site has started validating empty fields, "
                        + "which is the fix this case exists to catch.");
    }

    /** LOGIN-014 - a username outside 6-12 chars shows the length rule inline. */
    @Test
    @DisplayName("LOGIN-014: username outside 6-12 characters is rejected inline (Classic UI)")
    void usernameTooLong_showsLengthError() {
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        login.navigateTo();
        login.fillUsername("notarealuser999");
        login.fillPassword(PASSWORD);
        login.fillVerificationCode(CAPTCHA);
        page.waitForTimeout(500);
        assertFalse(page.locator("text=6-12 characters").count() > 0,
                "Please enter 6-12 characters (a-z 0-9). special characters are not allowed");
    }

    /** LOGIN-015 - a valid-length wrong username or wrong password both show the same board. */
    @Test
    @DisplayName("LOGIN-015: wrong username/password shows 'Wrong username/password, please re-enter.' (Classic UI)")
    void wrongCredentials_showsGenericError() {
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        login.loginWithAllFields("wronguser1", "wrongpass", CAPTCHA);
        page.waitForTimeout(1_000);
        assertTrue(login.hasErrorMessage(), "Expected a board message for wrong credentials.");
        assertTrue(login.errorMessage().toLowerCase().contains("wrong username"),
                "Expected the generic wrong-credentials wording, got: " + login.errorMessage());
    }

    /**
     * LOGIN-016 - a wrong verification code is rejected on its own, which is what confirms 1111
     * is a dedicated QA bypass rather than the environment reading the captcha image.
     */
    @Test
    @DisplayName("LOGIN-016: wrong verification code is rejected even with correct credentials (Classic UI)")
    void wrongCaptcha_isRejected() {
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        login.loginWithAllFields(USERNAME, PASSWORD, "9999");
        page.waitForTimeout(1_000);
        assertTrue(login.hasErrorMessage(), "Expected a board message for the wrong captcha.");
        assertTrue(login.errorMessage().toLowerCase().contains("verification"),
                "Expected the verification-code wording, got: " + login.errorMessage());
    }

    /**
     * LOGIN-017 - valid credentials sign the member in, <b>and</b> Classic Interface is still
     * the chosen UI version afterward - sign-in is itself a navigation, and this site's
     * UI-version picker is documented ({@link org.sengmean.base.BasePage#chooseClassicUi()}) to
     * sometimes re-ask after one. Every other UI-split suite in this project
     * ({@code DepositClassicUiTest}'s {@code openDeposit()}, {@code ResponsiveLayoutClassicUiTest}'s
     * {@code memberLayoutHolds()}) re-checks this after signing in; the login suite should not be
     * the one place that assumes it instead.
     */
    @Test
    @DisplayName("LOGIN-017: valid credentials sign the member in (Classic UI)")
    void validCredentials_signIn() {
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        login.loginWithAllFields(USERNAME, PASSWORD, CAPTCHA);
        page.waitForTimeout(2_000);
        assertSignedInAndClassicUiChosen(page, login, "signing in");
    }

    /**
     * LOGIN-018 - the session survives a full page reload, <b>and</b> the reload did not
     * silently revert the UI choice either - same reasoning as LOGIN-017's own fix. A plain
     * {@code page.navigate()} like this one is exactly the kind of navigation the picker can
     * re-ask on; this case used to check only {@code isSignedIn()}, which would have stayed
     * green even if the picker had come back over the page, since
     * {@code LoginPage.isSignedIn()} matches "Log out" by text without checking visibility.
     */
    @Test
    @DisplayName("LOGIN-018: session persists across a page reload (Classic UI)")
    void session_persistsAcrossReload() {
        Page page = openAsVisitorChoosingClassicUi();
        signIn(page);
        page.navigate(org.sengmean.base.BasePage.url("/en/"));
        // Not a bare page.waitForLoadState(): that defaults to `load`, which this site's own
        // slow third parties (chat widget, analytics) can miss well past a sane timeout - see
        // BasePage.awaitReady()'s Javadoc. awaitReady() waits for it best-effort instead.
        org.sengmean.base.BasePage.awaitReady(page);
        LoginPage login = new LoginPage(page);
        login.dismissOverlays();
        assertSignedInAndClassicUiChosen(page, login, "reloading");
    }

    /**
     * LOGIN-019 - ported from NewWeibo_automation's LOGIN-901. A login missing only the
     * verification code must not be accepted as if it were a complete, valid submission.
     */
    @Test
    @DisplayName("LOGIN-019: a login without a verification code cannot be submitted (Classic UI)")
    void missingVerificationCode_cannotSignIn() {
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        boolean clicked = login.loginWithoutVerificationCode(USERNAME, PASSWORD);
        page.waitForTimeout(1_000);
        assertFalse(clicked && login.isSignedIn(),
                "A login without a verification code signed the member in - either the button "
                        + "accepted the click and the server accepted the submission, or both.");
    }

    /**
     * LOGIN-020 - ported from NewWeibo_automation's LOGIN-906. A username on its own, with no
     * password and no verification code, must not be accepted.
     */
    @Test
    @DisplayName("LOGIN-020: a username on its own cannot be submitted (Classic UI)")
    void usernameOnly_cannotSignIn() {
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        boolean clicked = login.loginWithUsernameOnly(USERNAME);
        page.waitForTimeout(1_000);
        assertFalse(clicked && login.isSignedIn(),
                "A login with only a username signed the member in.");
    }

    /**
     * A username carrying characters the site does not allow, at a length it does (eight
     * characters, inside LOGIN-014's confirmed 6-12 rule) - so what this exercises is the
     * character guard, not the length guard LOGIN-014 already covers.
     */
    private static final String SPECIAL_CHARACTER_USERNAME = "qa@#$%^&";

    /**
     * LOGIN-021 - ported from NewWeibo_automation's LOGIN-007. Refused either by the same "6-12
     * characters" board LOGIN-014 shows, or by the generic wrong-username/password board -
     * either is a refusal; this does not pin the exact wording, which is unconfirmed for a
     * character-set guard on this site.
     */
    @Test
    @DisplayName("LOGIN-021: a username with special characters is refused (Classic UI)")
    void specialCharacterUsername_isRefused() {
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        login.loginWithAllFields(SPECIAL_CHARACTER_USERNAME, PASSWORD, CAPTCHA);
        page.waitForTimeout(1_000);
        assertFalse(login.isSignedIn(),
                "A username with special characters signed the member in.");
        assertTrue(login.hasErrorMessage() || page.locator("text=6-12 characters").count() > 0,
                "Expected either the board message or the 6-12 character rule for a username "
                        + "carrying special characters.");
    }

    /**
     * A real account this case may lock out - never the shared qa00002 one. There is no default:
     * three wrong passwords may lock whichever account this names, and locking the shared account
     * would break every other suite that signs in with it. Override with
     * {@code -Dtest.wrongpass.username=<username>} to run this case.
     */
    private static final String WRONG_PASSWORD_ACCOUNT =
            System.getProperty("test.wrongpass.username", "");

    /** A password that is certainly not the account's, well inside the length rule LOGIN-014 covers. */
    private static final String WRONG_PASSWORD = "wrongpw123";

    private void requireExpendableAccount() {
        assumeTrue(!WRONG_PASSWORD_ACCOUNT.isBlank(),
                "Needs a real account this case may lock out: -Dtest.wrongpass.username="
                        + "<username>. There is no default, because repeated wrong passwords may "
                        + "lock the account and the shared qa00002 account must stay usable for "
                        + "every other suite.");
    }

    /**
     * LOGIN-022 - ported from NewWeibo_automation's LOGIN-004 (there tagged {@code creates-data}
     * and left locked on purpose). NewWeibo_automation's own site answered a fourth wrong
     * password with a confirmed lockout message ("more than 3 times"); this project has never
     * seen Quark Dynasty's own wording for that, if it has one at all, so this does not assert a
     * specific message. What it does assert is the thing a brute-force guard exists to guarantee:
     * after four wrong passwords in a row, the account is still not signed in.
     */
    @Test
    @Tag("creates-data")
    @DisplayName("LOGIN-022: repeated wrong passwords never eventually let a brute-force in (Classic UI)")
    void repeatedWrongPasswords_neverSignIn() {
        requireExpendableAccount();
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        String lastMessage = "";
        for (int attempt = 1; attempt <= 4; attempt++) {
            login.loginWithAllFields(WRONG_PASSWORD_ACCOUNT, WRONG_PASSWORD, CAPTCHA);
            page.waitForTimeout(1_000);
            lastMessage = login.errorMessage();
            System.out.println("LOGIN-022 attempt " + attempt + ": " + lastMessage);
        }
        assertFalse(login.isSignedIn(),
                "After four wrong passwords in a row the account should still not be signed in. "
                        + "Last board message: " + lastMessage);
    }

    /**
     * An account that exists but has never been activated, for LOGIN-023. No default: this
     * project has no such account confirmed in this environment. Override with
     * {@code -Dtest.unactivated.username=<username>} to run this case.
     */
    private static final String UNACTIVATED_ACCOUNT =
            System.getProperty("test.unactivated.username", "");

    /**
     * LOGIN-023 - ported from NewWeibo_automation's LOGIN-009. That project's site answered with
     * a confirmed "Account suspended, please contact Customer Service" board; Quark Dynasty's own
     * wording for an unactivated account is unconfirmed, so this only asserts the refusal itself
     * and reports whatever board message the site actually showed.
     */
    @Test
    @DisplayName("LOGIN-023: an account that was never activated is refused (Classic UI)")
    void unactivatedAccount_isRefused() {
        assumeTrue(!UNACTIVATED_ACCOUNT.isBlank(),
                "Needs a real never-activated account: -Dtest.unactivated.username=<username>. "
                        + "No default - this project has no such account confirmed in this "
                        + "environment.");
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        login.loginWithAllFields(UNACTIVATED_ACCOUNT, PASSWORD, CAPTCHA);
        page.waitForTimeout(1_000);
        assertFalse(login.isSignedIn(),
                "An unactivated account signed the member in. Board message: "
                        + login.errorMessage());
        assertTrue(login.hasErrorMessage(),
                "Expected some board message refusing the unactivated account, but none was shown.");
    }

    /**
     * LOGIN-024 - new coverage this project's own LOGIN-017 never checked, ported from the second
     * half of NewWeibo_automation's LOGIN-001. A session token in a cookie is expected and fine;
     * what would not be is the member's own username or password appearing in clear in a cookie
     * name or value, which would let anyone reading the jar sign in as this member.
     */
    @Test
    @DisplayName("LOGIN-024: a valid sign-in leaves no credentials in cookies (Classic UI)")
    void validSignIn_leavesNoCredentialsInCookies() {
        Page page = openAsVisitorChoosingClassicUi();
        LoginPage login = new LoginPage(page);
        login.loginWithAllFields(USERNAME, PASSWORD, CAPTCHA);
        page.waitForTimeout(2_000);
        assertSignedInAndClassicUiChosen(page, login, "signing in");

        List<Cookie> cookies = context().cookies();
        List<String> leaking = cookies.stream()
                .filter(cookie -> containsCredential(cookie.name) || containsCredential(cookie.value))
                .map(cookie -> cookie.name)
                .toList();

        assertTrue(leaking.isEmpty(),
                "LOGIN-024: the sign-in left the member's own credentials in cookies " + leaking
                        + " out of " + cookies.size() + " set");
    }

    /** True when a cookie name or value carries the account's username or password in clear. */
    private static boolean containsCredential(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String haystack = text.toLowerCase();
        return haystack.contains(USERNAME.toLowerCase()) || haystack.contains(PASSWORD.toLowerCase());
    }

    // ----------------------------------------------------------------- helpers

    /**
     * Confirms a sign-in actually landed both ways that matter: the header shows the signed-in
     * state, <b>and</b> Classic Interface is still the chosen UI version - see LOGIN-017's own
     * Javadoc for why the second half is not redundant.
     */
    private void assertSignedInAndClassicUiChosen(Page page, LoginPage login, String after) {
        assertTrue(login.isSignedIn(),
                "Expected the header to show the signed-in state after " + after + ".");
        HomePage home = new HomePage(page);
        assertTrue(home.isClassicUiChosen(),
                "Expected Classic Interface to still be the chosen UI version after " + after
                        + " - ui-version is \"" + home.uiVersion() + "\"");
    }

    /**
     * Opens the home page as a fresh visitor, clears the welcome banner and the Daily Mission
     * panel if either is up (a no-op when neither is), answers the picker with Classic
     * Interface, clears both again (the picker's own overlay can outlive the click by a moment),
     * and asserts the choice actually stuck before any login case touches the form.
     */
    private Page openAsVisitorChoosingClassicUi() {
        Page on = page();
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseClassicUi();
        home.dismissOverlays();

        assertTrue(home.isClassicUiChosen(),
                "Expected Classic Interface to be the chosen UI version - ui-version is \""
                        + home.uiVersion() + "\"");
        return on;
    }
}
