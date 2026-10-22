package org.sengmean.page.auth;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.sengmean.base.BasePage;

import java.util.regex.Pattern;

/**
 * The Log In form - a modal over the landing page on a desktop viewport, a dedicated
 * {@code /en/log-in/} route on a narrow one. Both share the same field labels, so one page
 * object drives either.
 *
 * <h2>Selector confidence</h2>
 * Written from a manual exploratory session (2026-10-16) driven by screenshots and coordinate
 * clicks, not from a captured accessibility tree. Field locators go by the label text visible
 * directly above each input, which is how the form actually reads; button locators go by
 * accessible name with a case-insensitive match since the visible text is rendered in caps by
 * CSS and the true accessible name was not confirmed. <b>Verify every locator here against the
 * live DOM before trusting a red case.</b>
 */
public class LoginPage extends BasePage {

    public LoginPage(Page page) {
        super(page);
    }

    public void navigateTo() {
        open("/en/log-in/");
    }

    /** The input directly below the given visible label - this form's only confirmed pattern. */
    private Locator fieldNear(String label) {
        return page.locator("xpath=//*[normalize-space(text())='" + label
                + "']/following::input[1]");
    }

    public void fillUsername(String username) {
        fieldNear("Username").fill(username);
    }

    public void fillPassword(String password) {
        fieldNear("Password").fill(password);
    }

    public void fillVerificationCode(String code) {
        fieldNear("Verification Code").fill(code);
    }

    private Locator submitButton() {
        return page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("log\\s*in", Pattern.CASE_INSENSITIVE)));
    }

    public void submit() {
        submitButton().first().click();
    }

    /** Fills every field and submits - the shape every login test case in this project shares. */
    public void loginWithAllFields(String username, String password, String verificationCode) {
        navigateTo();
        fillUsername(username);
        fillPassword(password);
        fillVerificationCode(verificationCode);
        submit();
    }

    /**
     * Clicks "Log In" without failing the case when the form refuses to let the click land -
     * e.g. because the button is disabled while required fields are still empty.
     *
     * <p>An unconditional {@code click()} on a button Playwright cannot act on waits out this
     * context's full action timeout ({@code BaseTest}'s {@code test.actionTimeout}, 10s by
     * default) and then throws, which would fail an incomplete-form case with a stack trace
     * instead of the assertion it was trying to make. Returns whether the click actually landed.
     */
    private boolean attemptSubmit() {
        try {
            submitButton().first().click(new Locator.ClickOptions().setTimeout(5_000));
            return true;
        } catch (com.microsoft.playwright.PlaywrightException notClickable) {
            return false;
        }
    }

    /**
     * Fills everything but the verification code and attempts to submit.
     *
     * @return whether the click actually landed - false means the form itself refused it.
     */
    public boolean loginWithoutVerificationCode(String username, String password) {
        navigateTo();
        fillUsername(username);
        fillPassword(password);
        return attemptSubmit();
    }

    /** Fills only the username and attempts to submit; see {@link #loginWithoutVerificationCode}. */
    public boolean loginWithUsernameOnly(String username) {
        navigateTo();
        fillUsername(username);
        return attemptSubmit();
    }

    /**
     * The site's own dark message board over a dimmed page - "Wrong username/password, please
     * re-enter.", "Wrong verification code, please fill in again." - rather than a toast.
     */
    public String errorMessage() {
        Locator board = page.locator("text=Wrong username/password").or(page.locator("text=Wrong verification code"));
        if (board.count() == 0) {
            return "";
        }
        return board.first().innerText();
    }

    public boolean hasErrorMessage() {
        return !errorMessage().isEmpty();
    }

    public void closeErrorMessage() {
        Locator close = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("close", Pattern.CASE_INSENSITIVE)));
        if (close.count() > 0) {
            close.first().click();
        }
    }

    /** True once the header shows the signed-in state rather than Log In / Register. */
    public boolean isSignedIn() {
        return page.getByText("Log out", new Page.GetByTextOptions().setExact(true)).count() > 0;
    }

    /**
     * Signs the member out via the header's "Log out" control - the same text {@link #isSignedIn()}
     * already matches on. Added for {@code org.sengmean.tests.ux}'s end-to-end journey, which needs
     * to close the session it opens; no other case in this project signs out today, so this carries
     * the same unconfirmed-selector caveat as the rest of this class.
     */
    public void signOut() {
        page.getByText("Log out", new Page.GetByTextOptions().setExact(true)).first().click();
        page.waitForLoadState();
        page.waitForTimeout(2_000);
    }
}
