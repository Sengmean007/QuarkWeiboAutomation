package org.sengmean.page.auth;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.sengmean.base.BasePage;

import java.util.regex.Pattern;

/**
 * The registration form and the SMS activation step that follows it. Same selector-confidence
 * caveat as {@link LoginPage} - verify against the live DOM before trusting a red case.
 */
public class RegisterPage extends BasePage {

    public RegisterPage(Page page) {
        super(page);
    }

    /** Navigates to the register route and clears the welcome banner and the Daily Mission panel
     *  if either is up - a check-and-close each time: closed when one is up, left alone when it
     *  is not. Never the full {@link #dismissOverlays()}: confirmed live (2026-10-23) this form
     *  shares the same generic {@code LoginModal_container__<hash>} wrapper and
     *  {@code Modal_close__<hash>} close button {@link LoginPage} does, so the VIP-popup sweep
     *  would close this modal by its own X the same way it closed the login form - see
     *  {@code LoginPage.navigateTo()}'s Javadoc for the full story of that bug. */
    public void navigateTo() {
        open("/en/register/");
        dismissWelcomeBanner();
        dismissDailyMission();
    }

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

    public void fillRepeatPassword(String password) {
        fieldNear("Repeat password").fill(password);
    }

    /**
     * The contact number field, without the country-code dropdown beside it. 2026-10-16 found
     * an 8-digit number (no leading 0) is what the site accepts; a 7-digit or oddly-formatted
     * one answers the raw backend code "Status error -201" instead of a readable message - see
     * RegisterTest.invalidContactNumber_showsRawBackendErrorCode for the regression case.
     */
    public void fillContactNumber(String number) {
        fieldNear("Contact Number").fill(number);
    }

    public void fillVerificationCode(String code) {
        fieldNear("Verification Code").fill(code);
    }

    /**
     * The form's own REGISTER control - <b>not</b> the header's "Register" opener, which stays on
     * screen underneath the modal and matches a plain {@code getByRole(BUTTON, name="register")}
     * lookup just as well as the real submit button does. Confirmed live (2026-10-23): both are
     * real, visible, accessible-name "Register"/"REGISTER" buttons at once, the header's one
     * first in DOM order - the exact same ambiguity {@link LoginPage#submitButton()} was fixed
     * for, ported the same way: scope the search to the nearest ancestor of the Username field
     * that itself contains a "register" button, falling back to the old page-wide lookup only if
     * that scoped search finds nothing.
     */
    private Locator submitButton() {
        Pattern register = Pattern.compile("register", Pattern.CASE_INSENSITIVE);
        Locator scoped = fieldNear("Username")
                .locator("xpath=ancestor::*[.//button[contains(translate(normalize-space(string(.)),"
                        + " 'REGISTER', 'register'), 'register')]][1]")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(register));
        if (scoped.count() > 0) {
            return scoped;
        }
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(register));
    }

    /** Clears the welcome banner and the Daily Mission panel if either is up before clicking -
     *  never the full {@link #dismissOverlays()}; see {@link #navigateTo()}'s Javadoc for why. */
    public void submit() {
        dismissWelcomeBanner();
        dismissDailyMission();
        submitButton().first().click();
    }

    /** Fills every field in the shape every register case in this project shares. */
    public void registerWithAllFields(String username, String password, String contactNumber,
                                       String verificationCode) {
        navigateTo();
        fillUsername(username);
        fillPassword(password);
        fillRepeatPassword(password);
        fillContactNumber(contactNumber);
        fillVerificationCode(verificationCode);
        submit();
    }

    /** The inline field error under Username/Password/Repeat password, if any is showing. */
    public String fieldError(String nearLabel) {
        Locator error = page.locator(
                "xpath=//*[normalize-space(text())='" + nearLabel + "']/following::*[contains(@class,'error') or contains(@class,'Error')][1]");
        return error.count() > 0 ? error.first().innerText() : "";
    }

    /** The site's dark message board - "Account has been used.", "Message sent", etc. */
    public String boardMessage() {
        Locator board = page.locator("[class*='AlertBoard' i], [class*='Toast' i], [role='alert']");
        for (int i = 0; i < board.count(); i++) {
            if (board.nth(i).isVisible()) {
                return board.nth(i).innerText();
            }
        }
        return "";
    }

    public void closeBoardMessage() {
        Locator close = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("close", Pattern.CASE_INSENSITIVE)));
        if (close.count() > 0) {
            close.first().click();
        }
    }

    /** Fills the four-digit SMS activation code - 1111 is accepted by this test environment. */
    public void fillActivationCode(String code) {
        Locator boxes = page.locator("input[maxlength='1'], input[type='tel']");
        if (boxes.count() >= code.length()) {
            for (int i = 0; i < code.length(); i++) {
                boxes.nth(i).fill(String.valueOf(code.charAt(i)));
            }
        } else {
            // Fallback: a single combined OTP input rather than one box per digit.
            page.locator("input").first().fill(code);
        }
    }

    public void submitActivation() {
        page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName(Pattern.compile("(activate|submit|confirm)", Pattern.CASE_INSENSITIVE)))
                .first().click();
    }
}
