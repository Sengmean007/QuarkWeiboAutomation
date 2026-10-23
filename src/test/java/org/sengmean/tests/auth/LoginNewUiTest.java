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
 * The Log In form on https://test-v2.138hk.vip, under <b>New UI</b> - covering the cases run
 * manually on 2026-10-16 against qa00002 / password / captcha 1111.
 *
 * <h2>Why this exists alongside {@link LoginClassicUiTest}</h2>
 * This project's README used to carry Login and Register as the two exceptions to the New UI /
 * Classic UI split, on the grounds that "the login/register forms are reached independently of
 * the home page's UI-version picker, with no evidence either form differs by UI version" - which
 * was true as far as it went, but was never actually confirmed either way for Login. This class
 * and {@link LoginClassicUiTest} close that gap for Login specifically: each one answers the
 * home page's picker first ({@link #openAsVisitorChoosingNewUi()} / that class's Classic Interface
 * twin) before ever touching the login form, the same shape {@code DepositNewUiTest} /
 * {@code ClassicUiTest} and the rest of the UI-split suites already use. Register is left as the
 * README's one remaining exception; nothing here changes that.
 *
 * <h2>Welcome banner and Daily Mission - closed if up, ignored if not</h2>
 * Every entry point into the login form now clears both unprompted overlays rather than assuming
 * neither is up: {@link org.sengmean.base.BasePage#dismissOverlays()} - called on the home page
 * before and after answering the picker, and again by {@link LoginPage#navigateTo()} right after
 * it lands on the login route - closes the welcome banner and the Daily Mission panel when either
 * is covering the page, and does nothing (not even a log line beyond "none on the page") when
 * neither is. {@code LoginPage.submit()}/{@code attemptSubmit()} clear them once more immediately
 * before clicking, since either can still be the reason a click never reaches the real LOG IN
 * button - see {@code LoginPage.submitButton()}'s own Javadoc for the other reason a click could
 * silently miss it (the header's own "Log In" control matching the same lookup).
 *
 * <h2>LOGIN-007 onward - ported from the sibling NewWeibo_automation project</h2>
 * NewWeibo_automation's own {@code LoginTest} (14 cases, a different client site) was read in
 * full and compared against the six cases above. Six of its fourteen are near-duplicates of what
 * is already here and were left out:
 * <ul>
 *   <li>its empty-form case duplicates LOGIN-001;</li>
 *   <li>its unknown-username case and its wrong-password case both duplicate LOGIN-003, which
 *       already covers a generic wrong-username/wrong-password refusal on this site;</li>
 *   <li>its invalid-verification-code case duplicates LOGIN-004.</li>
 * </ul>
 * Its three "Other login methods" cases (Google, Line, Telegram, WhatsApp QR panels) are not
 * ported either, but not as duplicates - this project has found no evidence the Quark Dynasty
 * login form offers any social sign-in at all, and inventing a case for a control that may not
 * exist would be a guess, not coverage. If the form does offer one, this is worth revisiting.
 *
 * <p>The other five cases below are new coverage NewWeibo_automation had and this project did
 * not: a partial form (missing only the verification code, or only a username on its own) cannot
 * be submitted; a username carrying characters the site does not allow is refused (a different
 * guard than LOGIN-002's length check); repeated wrong passwords against an expendable account
 * eventually stop a brute-force rather than silently letting it through; an account that was
 * never activated is refused. A sixth, LOGIN-012, is new coverage this project's own LOGIN-005
 * never checked: that a successful sign-in does not leave the member's own credentials sitting in
 * a cookie.
 *
 * <h2>Case ids: LOGIN-001 - LOGIN-012 here, continuing at LOGIN-013 in {@link LoginClassicUiTest}</h2>
 */
public class LoginNewUiTest extends BaseTest {

    /** LOGIN-001 - submitting with every field empty shows no feedback at all. Known UX gap. */
    @Test
    @DisplayName("LOGIN-001: empty form shows no validation message (known gap) (New UI)")
    void emptyForm_showsNoFeedback() {
        Page page = openAsVisitorChoosingNewUi();
        LoginPage login = new LoginPage(page);
        login.navigateTo();
        login.submit();
        page.waitForTimeout(1_000);
        // Documents the gap rather than asserting it should stay this way: there is currently no
        // inline "required" message and no error board, so the form simply does nothing visible.
        assertFalse(login.hasErrorMessage(),
                "Expected no board message for an empty submit (none is shown today) - "
                        + "if this now fails, the site has started validating empty fields, "
                        + "which is the fix this case exists to catch.");
    }

    /** LOGIN-002 - a username outside 6-12 chars shows the length rule inline. */
    @Test
    @DisplayName("LOGIN-002: username outside 6-12 characters is rejected inline (New UI)")
    void usernameTooLong_showsLengthError() {
        Page page = openAsVisitorChoosingNewUi();
        LoginPage login = new LoginPage(page);
        login.navigateTo();
        login.fillUsername("notarealuser999");
        login.fillPassword(PASSWORD);
        login.fillVerificationCode(CAPTCHA);
        page.waitForTimeout(500);
        assertTrue(page.locator("text=6-12 characters").count() > 0,
                "Expected the 6-12 character rule to show for a 16-character username.");
    }

    /** LOGIN-003 - a valid-length wrong username or wrong password both show the same board. */
    @Test
    @DisplayName("LOGIN-003: wrong username/password shows 'Wrong username/password, please re-enter.' (New UI)")
    void wrongCredentials_showsGenericError() {
        Page page = openAsVisitorChoosingNewUi();
        LoginPage login = new LoginPage(page);
        login.loginWithAllFields("wronguser1", "wrongpass", CAPTCHA);
        page.waitForTimeout(1_000);
        assertTrue(login.hasErrorMessage(), "Expected a board message for wrong credentials.");
        assertTrue(login.errorMessage().toLowerCase().contains("wrong username"),
                "Expected the generic wrong-credentials wording, got: " + login.errorMessage());
    }

    /**
     * LOGIN-004 - a wrong verification code is rejected on its own, which is what confirms 1111 is
     * a dedicated QA bypass rather than the environment reading the captcha image.
     */
    @Test
    @DisplayName("LOGIN-004: wrong verification code is rejected even with correct credentials (New UI)")
    void wrongCaptcha_isRejected() {
        Page page = openAsVisitorChoosingNewUi();
        LoginPage login = new LoginPage(page);
        login.loginWithAllFields(USERNAME, PASSWORD, "9999");
        page.waitForTimeout(1_000);
        assertTrue(login.hasErrorMessage(), "Expected a board message for the wrong captcha.");
        assertTrue(login.errorMessage().toLowerCase().contains("verification"),
                "Expected the verification-code wording, got: " + login.errorMessage());
    }

    /**
     * LOGIN-005 - valid credentials sign the member in, <b>and</b> New Interface is still the
     * chosen UI version afterward - sign-in is itself a navigation, and this site's UI-version
     * picker is documented ({@link org.sengmean.base.BasePage#chooseNewUi()}) to sometimes
     * re-ask after one. Every other UI-split suite in this project ({@code DepositNewUiTest}'s
     * {@code openDeposit()}, {@code ResponsiveLayoutNewUiTest}'s {@code memberLayoutHolds()})
     * re-checks this after signing in; the login suite should not be the one place that assumes
     * it instead.
     */
    @Test
    @DisplayName("LOGIN-005: valid credentials sign the member in (New UI)")
    void validCredentials_signIn() {
        Page page = openAsVisitorChoosingNewUi();
        LoginPage login = new LoginPage(page);
        login.loginWithAllFields(USERNAME, PASSWORD, CAPTCHA);
        page.waitForTimeout(2_000);
        assertSignedInAndNewUiChosen(page, login, "signing in");
    }

    /**
     * LOGIN-006 - the session survives a full page reload, <b>and</b> the reload did not
     * silently revert the UI choice either - same reasoning as LOGIN-005's own fix. A plain
     * {@code page.navigate()} like this one is exactly the kind of navigation the picker can
     * re-ask on; this case used to check only {@code isSignedIn()}, which would have stayed
     * green even if the picker had come back over the page, since
     * {@code LoginPage.isSignedIn()} matches "Log out" by text without checking visibility.
     */
    @Test
    @DisplayName("LOGIN-006: session persists across a page reload (New UI)")
    void session_persistsAcrossReload() {
        Page page = openAsVisitorChoosingNewUi();
        signIn(page);
        page.navigate(org.sengmean.base.BasePage.url("/en/"));
        // Not a bare page.waitForLoadState(): that defaults to `load`, which this site's own
        // slow third parties (chat widget, analytics) can miss well past a sane timeout - see
        // BasePage.awaitReady()'s Javadoc. awaitReady() waits for it best-effort instead.
        org.sengmean.base.BasePage.awaitReady(page);
        LoginPage login = new LoginPage(page);
        login.dismissOverlays();
        assertSignedInAndNewUiChosen(page, login, "reloading");
    }

    /**
     * LOGIN-007 - ported from NewWeibo_automation's LOGIN-901. A login missing only the
     * verification code must not be accepted as if it were a complete, valid submission.
     */
    @Test
    @DisplayName("LOGIN-007: a login without a verification code cannot be submitted (New UI)")
    void missingVerificationCode_cannotSignIn() {
        Page page = openAsVisitorChoosingNewUi();
        LoginPage login = new LoginPage(page);
        boolean clicked = login.loginWithoutVerificationCode(USERNAME, PASSWORD);
        page.waitForTimeout(1_000);
        assertFalse(clicked && login.isSignedIn(),
                "A login without a verification code signed the member in - either the button "
                        + "accepted the click and the server accepted the submission, or both.");
    }

    /**
     * LOGIN-008 - ported from NewWeibo_automation's LOGIN-906. A username on its own, with no
     * password and no verification code, must not be accepted.
     */
    @Test
    @DisplayName("LOGIN-008: a username on its own cannot be submitted (New UI)")
    void usernameOnly_cannotSignIn() {
        Page page = openAsVisitorChoosingNewUi();
        LoginPage login = new LoginPage(page);
        boolean clicked = login.loginWithUsernameOnly(USERNAME);
        page.waitForTimeout(1_000);
        assertFalse(clicked && login.isSignedIn(),
                "A login with only a username signed the member in.");
    }

    /**
     * A username carrying characters the site does not allow, at a length it does (eight
     * characters, inside LOGIN-002's confirmed 6-12 rule) - so what this exercises is the
     * character guard, not the length guard LOGIN-002 already covers.
     */
    private static final String SPECIAL_CHARACTER_USERNAME = "qa@#$%^&";

    /**
     * LOGIN-009 - ported from NewWeibo_automation's LOGIN-007. Refused either by the same "6-12
     * characters" board LOGIN-002 shows, or by the generic wrong-username/password board -
     * either is a refusal; this does not pin the exact wording, which is unconfirmed for a
     * character-set guard on this site.
     */
    @Test
    @DisplayName("LOGIN-009: a username with special characters is refused (New UI)")
    void specialCharacterUsername_isRefused() {
        Page page = openAsVisitorChoosingNewUi();
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

    /** A password that is certainly not the account's, well inside the length rule LOGIN-002 covers. */
    private static final String WRONG_PASSWORD = "wrongpw123";

    private void requireExpendableAccount() {
        assumeTrue(!WRONG_PASSWORD_ACCOUNT.isBlank(),
                "Needs a real account this case may lock out: -Dtest.wrongpass.username="
                        + "<username>. There is no default, because repeated wrong passwords may "
                        + "lock the account and the shared qa00002 account must stay usable for "
                        + "every other suite.");
    }

    /**
     * LOGIN-010 - ported from NewWeibo_automation's LOGIN-004 (there tagged {@code creates-data}
     * and left locked on purpose). NewWeibo_automation's own site answered a fourth wrong
     * password with a confirmed lockout message ("more than 3 times"); this project has never
     * seen Quark Dynasty's own wording for that, if it has one at all, so this does not assert a
     * specific message. What it does assert is the thing a brute-force guard exists to guarantee:
     * after four wrong passwords in a row, the account is still not signed in.
     */
    @Test
    @Tag("creates-data")
    @DisplayName("LOGIN-010: repeated wrong passwords never eventually let a brute-force in (New UI)")
    void repeatedWrongPasswords_neverSignIn() {
        requireExpendableAccount();
        Page page = openAsVisitorChoosingNewUi();
        LoginPage login = new LoginPage(page);
        String lastMessage = "";
        for (int attempt = 1; attempt <= 4; attempt++) {
            login.loginWithAllFields(WRONG_PASSWORD_ACCOUNT, WRONG_PASSWORD, CAPTCHA);
            page.waitForTimeout(1_000);
            lastMessage = login.errorMessage();
            System.out.println("LOGIN-010 attempt " + attempt + ": " + lastMessage);
        }
        assertFalse(login.isSignedIn(),
                "After four wrong passwords in a row the account should still not be signed in. "
                        + "Last board message: " + lastMessage);
    }

    /**
     * An account that exists but has never been activated, for LOGIN-011. No default: this
     * project has no such account confirmed in this environment. Override with
     * {@code -Dtest.unactivated.username=<username>} to run this case.
     */
    private static final String UNACTIVATED_ACCOUNT =
            System.getProperty("test.unactivated.username", "");

    /**
     * LOGIN-011 - ported from NewWeibo_automation's LOGIN-009. That project's site answered with
     * a confirmed "Account suspended, please contact Customer Service" board; Quark Dynasty's own
     * wording for an unactivated account is unconfirmed, so this only asserts the refusal itself
     * and reports whatever board message the site actually showed.
     */
    @Test
    @DisplayName("LOGIN-011: an account that was never activated is refused (New UI)")
    void unactivatedAccount_isRefused() {
        assumeTrue(!UNACTIVATED_ACCOUNT.isBlank(),
                "Needs a real never-activated account: -Dtest.unactivated.username=<username>. "
                        + "No default - this project has no such account confirmed in this "
                        + "environment.");
        Page page = openAsVisitorChoosingNewUi();
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
     * LOGIN-012 - new coverage this project's own LOGIN-005 never checked, ported from the second
     * half of NewWeibo_automation's LOGIN-001. A session token in a cookie is expected and fine;
     * what would not be is the member's own username or password appearing in clear in a cookie
     * name or value, which would let anyone reading the jar sign in as this member.
     */
    @Test
    @DisplayName("LOGIN-012: a valid sign-in leaves no credentials in cookies (New UI)")
    void validSignIn_leavesNoCredentialsInCookies() {
        Page page = openAsVisitorChoosingNewUi();
        LoginPage login = new LoginPage(page);
        login.loginWithAllFields(USERNAME, PASSWORD, CAPTCHA);
        page.waitForTimeout(2_000);
        assertSignedInAndNewUiChosen(page, login, "signing in");

        List<Cookie> cookies = context().cookies();
        List<String> leaking = cookies.stream()
                .filter(cookie -> containsCredential(cookie.name) || containsCredential(cookie.value))
                .map(cookie -> cookie.name)
                .toList();

        assertTrue(leaking.isEmpty(),
                "LOGIN-012: the sign-in left the member's own credentials in cookies " + leaking
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
     * state, <b>and</b> New Interface is still the chosen UI version - see LOGIN-005's own
     * Javadoc for why the second half is not redundant.
     */
    private void assertSignedInAndNewUiChosen(Page page, LoginPage login, String after) {
        assertTrue(login.isSignedIn(),
                "Expected the header to show the signed-in state after " + after + ".");
        HomePage home = new HomePage(page);
        assertTrue(home.isNewUiChosen(),
                "Expected New Interface to still be the chosen UI version after " + after
                        + " - ui-version is \"" + home.uiVersion() + "\"");
    }

    /**
     * Opens the home page as a fresh visitor, clears the welcome banner and the Daily Mission
     * panel if either is up (a no-op when neither is), answers the picker with New Interface,
     * clears both again (the picker's own overlay can outlive the click by a moment), and asserts
     * the choice actually stuck before any login case touches the form.
     */
    private Page openAsVisitorChoosingNewUi() {
        Page on = page();
        HomePage home = new HomePage(on);
        home.navigateTo();
        home.dismissOverlays();
        home.chooseNewUi();
        home.dismissOverlays();

        assertTrue(home.isNewUiChosen(),
                "Expected New Interface to be the chosen UI version - ui-version is \""
                        + home.uiVersion() + "\"");
        return on;
    }
}
