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

    /** Navigates to the login route and clears the welcome banner and the Daily Mission panel if
     *  either is up - a check-and-close each time: closed when one is up, left alone when it is
     *  not, so a fresh page with nothing to clear costs nothing.
     *
     * <h2>Why not the full {@link #dismissOverlays()}</h2>
     * Confirmed live against both UI versions (2026-10-23): this site reuses one generic "Modal"
     * component for every popup it has - the VIP promo popup, the login form itself, the register
     * form - so its close button is always {@code Modal_close__<hash>} regardless of which modal
     * it belongs to. {@link #dismissOverlays()}'s VIP-popup sweep matches any button whose class
     * contains "close" with no way to tell those apart, so calling it here closed the login modal
     * by its own × the instant it opened, before a single field could be filled - every case that
     * reached {@link #fillUsername(String)} timed out waiting on a field that had just been
     * closed out from under it. Confirmed identical on New UI ({@code LoginModal_container__2Bgsw})
     * and Classic UI ({@code LoginModal_container__ybPt_}) - different build hash, same collision.
     * The welcome banner and the Daily Mission panel are closed by their own specific selectors
     * ({@link #dismissWelcomeBanner()}, {@link #dismissDailyMission()}), neither of which matches
     * anything inside this form, so only those two run here. */
    public void navigateTo() {
        open("/en/log-in/");
        dismissWelcomeBanner();
        dismissDailyMission();
    }

    /**
     * Opens this form through the header's own Login control, in whatever language is currently
     * selected - unlike {@link #navigateTo()}, whose path is hard-coded to {@code /en/log-in/}
     * and so forces the page back to English on every call, confirmed live (2026-10-23): landing
     * on that path while Traditional Chinese was selected re-rendered the whole page in English,
     * exactly as if the language had never been changed. This method is for callers that already
     * put the page in a specific language ({@link org.sengmean.base.BasePage#selectLanguage}) and
     * need the login form to open without losing it.
     *
     * <h2>Why by class, not by text - and why two different classes</h2>
     * The header's three auth controls (Login/Register/Account Activation) all translate, so
     * none can be found by English text once a language has been chosen. Confirmed live
     * (2026-10-23) that New UI and Classic UI do not even share a markup convention here: New
     * UI gives each control its own semantic modifier class ({@code Header_btn-login__<hash>},
     * {@code Header_btn-register__<hash>}, {@code Header_btn-activate__<hash>}) - matched first,
     * since it names Login unambiguously regardless of DOM order. Classic UI gives all three the
     * exact same class ({@code Header_btn__<hash> Header_desktop__<hash>}, a same-labelled mobile
     * copy carrying the same class too but hidden) with nothing to tell them apart but position -
     * Login first - so that is the fallback when the New UI class is not there to match.
     */
    public void openFromHeaderInCurrentLanguage() {
        Locator byModifierClass = page.locator("button[class*='Header_btn-login' i]")
                .filter(new Locator.FilterOptions().setVisible(true));
        Locator opener = byModifierClass.count() > 0 ? byModifierClass
                : page.locator("button[class*='Header_btn' i][class*='Header_desktop' i]")
                        .filter(new Locator.FilterOptions().setVisible(true));
        opener.first().click();
        page.waitForTimeout(1_500);
        dismissWelcomeBanner();
        dismissDailyMission();
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

    /**
     * The form's own LOG IN control - <b>not</b> the header's "Log In" opener, which stays on
     * screen underneath the modal (desktop) or above the dedicated route's own form (narrow
     * viewport) and matches a plain {@code getByRole(BUTTON, name="log in")} lookup just as well
     * as the real submit button does.
     *
     * <h2>Why this mattered</h2>
     * The previous version of this method returned that plain page-wide lookup, and
     * {@code .first()} picked whichever of the two came first in DOM order - which on this site
     * is the header's control, not the form's. The click still "succeeds": the header's control
     * is a perfectly real, clickable button, so nothing throws and the case goes on as if the
     * form had been submitted, when the actual LOG IN button inside the modal/form was never
     * clicked at all - exactly the "fills the fields, never clicks Login" symptom this was found
     * from. Ported from the sibling Admin_kuake project, which hit this identical ambiguity on
     * this identical site ({@code test-v2.138hk.vip}) and fixed it the same way: scope the search
     * to the nearest ancestor of the Username field that itself contains a "log in" button - the
     * modal or the route's own form, never the header above it. Falls back to the old page-wide
     * lookup only if that scoped search finds nothing, so a site change that breaks the ancestor
     * walk degrades rather than breaking every caller outright.
     */
    private Locator submitButton() {
        Pattern logIn = Pattern.compile("log\\s*in", Pattern.CASE_INSENSITIVE);
        Locator scoped = fieldNear("Username")
                .locator("xpath=ancestor::*[.//button[contains(translate(normalize-space(string(.)),"
                        + " 'LOGIN', 'login'), 'log in')]][1]")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(logIn));
        if (scoped.count() > 0) {
            return scoped;
        }
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(logIn));
    }

    /** Clears the welcome banner and the Daily Mission panel if either is up - never the full
     *  {@link #dismissOverlays()}; see {@link #navigateTo()}'s Javadoc for why that would close
     *  this very form instead of clicking it. */
    public void submit() {
        dismissWelcomeBanner();
        dismissDailyMission();
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
     * context's full action timeout ({@code BaseTest}'s {@code test.actionTimeout}, 20s by
     * default) and then throws, which would fail an incomplete-form case with a stack trace
     * instead of the assertion it was trying to make. Returns whether the click actually landed.
     */
    private boolean attemptSubmit() {
        try {
            dismissWelcomeBanner();
            dismissDailyMission();
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
     *
     * <h2>Language-agnostic by a structural selector, not by English text</h2>
     * This used to match on the English wording itself ({@code text=Wrong username/password}),
     * which only ever worked in English and would have silently reported "no error" for every
     * other language - the opposite of useful for confirming a message is actually translated.
     * Confirmed live (2026-10-23) under Traditional Chinese: the board carries no English text at
     * all ("帳號/密碼有誤，請重新輸入"), but its wrapper is still {@code AlertBoard_bg__<hash>}
     * regardless of language - the same generic alert component {@code WithdrawalPage} already
     * matches this way for its own board. Every caller that compares this against English wording
     * (e.g. {@code errorMessage().toLowerCase().contains("wrong username")}) still works exactly
     * as before when the page is actually in English; the only change is how the board itself is
     * found.
     */
    public String errorMessage() {
        Locator board = page.locator("[class*='AlertBoard' i], [class*='Toast' i], [role='alert']")
                .filter(new Locator.FilterOptions().setVisible(true));
        if (board.count() == 0) {
            return "";
        }
        return board.first().innerText().trim();
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
