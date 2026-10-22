package org.sengmean.tests.auth;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sengmean.base.BaseTest;
import org.sengmean.page.auth.RegisterPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Register form and SMS activation step, covering the cases run manually on 2026-10-16.
 */
public class RegisterTest extends BaseTest {

    /** REGISTER-001: a 2-character username is rejected inline, real-time on blur. */
    @Test
    @DisplayName("REGISTER-001: username shorter than 6 characters is rejected inline")
    void usernameTooShort_showsLengthError() {
        Page page = page();
        RegisterPage register = new RegisterPage(page);
        register.navigateTo();
        register.fillUsername("ab");
        page.waitForTimeout(500);
        assertTrue(page.locator("text=6-12 characters").count() > 0,
                "Expected the 6-12 character rule to show for a 2-character username.");
    }

    /** REGISTER-002: password and repeat password must match. */
    @Test
    @DisplayName("REGISTER-002: mismatched password and repeat password is rejected")
    void passwordMismatch_showsError() {
        Page page = page();
        RegisterPage register = new RegisterPage(page);
        register.navigateTo();
        register.fillUsername("qatestnewXX");
        register.fillPassword("testpass1");
        register.fillRepeatPassword("testpass2");
        page.waitForTimeout(500);
        assertTrue(page.locator("text=same as the password").count() > 0,
                "Expected the repeat-password mismatch message.");
    }

    /**
     * REGISTER-003: a malformed contact number ("12345678", no leading digit the site accepts)
     * submits through to the backend and surfaces the raw, untranslated error code instead of a
     * readable message. Known defect, 2026-10-16: contrast with the duplicate-username case
     * below, which DOES get a proper message, so the error-mapping is incomplete rather than
     * entirely absent.
     */
    @Test
    @DisplayName("REGISTER-003: invalid contact number shows raw 'Status error -201' (known defect)")
    void invalidContactNumber_showsRawBackendErrorCode() {
        Page page = page();
        RegisterPage register = new RegisterPage(page);
        register.registerWithAllFields("qatestnewXX", "testpass1", "12345678", CAPTCHA);
        page.waitForTimeout(1_000);
        assertTrue(page.locator("text=Status error").count() > 0,
                "Expected the unmapped 'Status error -201' text - if this now fails because a "
                        + "readable message appears instead, the defect has been fixed and this "
                        + "assertion should invert.");
    }

    /**
     * REGISTER-004: registering with an already-used username is rejected with a proper,
     * readable message - unlike the contact-number case above.
     */
    @Test
    @DisplayName("REGISTER-004: a username already in use is rejected with a readable message")
    void duplicateUsername_showsReadableError() {
        Page page = page();
        RegisterPage register = new RegisterPage(page);
        register.registerWithAllFields(USERNAME, "testpass9", "81234568", CAPTCHA);
        page.waitForTimeout(1_000);
        assertTrue(page.locator("text=Account has been used").count() > 0,
                "Expected 'Account has been used' for a duplicate username.");
    }

    /**
     * REGISTER-005: the full registration -> SMS activation flow, using the 1111 bypass code this
     * test environment accepts in place of a real SMS OTP.
     *
     * <p>Disabled by default because it creates a real, persistent account on every run (qa00002
     * logged one such account - qatestnew01 / testpass9 / 81234568 - on 2026-10-16; see today's
     * session notes). Enable deliberately, with a fresh username each time.
     */
    @Test
    @Disabled("Creates a real account every run - see class Javadoc")
    @DisplayName("REGISTER-005: full registration and SMS activation (creates a real account)")
    void fullRegistration_activatesWithBypassCode() {
        Page page = page();
        RegisterPage register = new RegisterPage(page);
        String username = "qatest" + System.currentTimeMillis() % 100_000_000L;
        register.registerWithAllFields(username, "testpass1", "81234567", CAPTCHA);
        page.waitForTimeout(1_000);
        register.fillActivationCode("1111");
        register.submitActivation();
        page.waitForTimeout(1_000);
        assertTrue(page.locator("text=successfully activated").count() > 0,
                "Expected the account-activated confirmation.");
    }
}
