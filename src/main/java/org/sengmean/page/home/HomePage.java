package org.sengmean.page.home;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import org.sengmean.base.BasePage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The landing page's header furniture, the auth controls it offers a visitor, and the signed-in
 * account chip - everything {@link org.sengmean.tests.home.HomePageNewUiTest} and
 * {@link org.sengmean.tests.home.HomePageClassicUiTest} need, independent of either UI version.
 *
 * <h2>Why this is not {@code org.sengmean.page.wallet.HomePage}</h2>
 * That class exists for one narrow purpose - the hero-banner-breaks-by-currency regression
 * {@code CurrencyBannerTest} tracks, plus the two bounding-box checks
 * {@code ResponsiveLayoutNewUiTest}/{@code ResponsiveLayoutClassicUiTest} already depend on
 * ({@code navigateTo()}, {@code hasHeroBanner()}). Growing it with a dozen more methods about
 * header buttons, the login/register hand-off and the signed-in wallet list would blur a class
 * that is deliberately scoped to one known defect, and risks a change here breaking three
 * existing callers this port must not touch. This project's own convention - shared/base classes
 * get their own package rather than being folded into whatever used them first - argues for a
 * new class the same way; {@code wallet.HomePage} is left exactly as it was.
 *
 * <h2>Selector confidence - same caveat as every other page object in this project</h2>
 * Nothing below has been confirmed against Quark Dynasty's live DOM. It is written from the
 * shape of the sibling NewWeibo_automation project's own {@code page.home.HomePage} (a different
 * client site), generalised away from that site's specific header labels, class names and
 * literals wherever this project has no confirmed equivalent - see the Javadoc on each method,
 * and {@code HomePageNewUiTest}/{@code HomePageClassicUiTest} for what was ported, generalised or
 * skipped outright. <b>Verify every locator here against the live DOM before trusting a red
 * case.</b>
 */
public class HomePage extends BasePage {

    /** The two auth controls this project has confirmed the header offers - see {@code LoginPage}
     *  and {@code RegisterPage}, whose routes these labels match. Unlike NewWeibo_empire's own
     *  three ({@code LOG_IN}, {@code REGISTER}, {@code ACCOUNT_ACTIVATION}), there is no evidence
     *  Quark's header carries a third "Account Activation" control - its registration flow folds
     *  activation into the Register form itself (an SMS step, see {@code RegisterPage}), not a
     *  separate account state with its own header link - so only two are carried over. */
    public static final String LOG_IN = "Log In";
    public static final String REGISTER = "Register";

    /** A currency code as the wallet panel writes it - the five confirmed on qa00002, see
     *  {@code org.sengmean.page.wallet.CurrencySwitcher}. */
    private static final Pattern WALLET_CODE = Pattern.compile("(USDT|USD|HKD|THB|TRX)");

    public HomePage(Page page) {
        super(page);
    }

    public void navigateTo() {
        open("/en/");
    }

    /** True once the page shows either a guest's "Log In" or a member's "Log out" - whichever
     *  state this visit is in, something in the header has rendered. */
    public boolean isLoaded() {
        try {
            page.getByText(LOG_IN, new Page.GetByTextOptions().setExact(true))
                    .or(page.getByText("Log out", new Page.GetByTextOptions().setExact(true)))
                    .first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(15_000));
            return true;
        } catch (PlaywrightException notThere) {
            return false;
        }
    }

    // ------------------------------------------------------------------ logo

    /** The site logo - several copies may exist on the page; the first is the header's. */
    private Locator logo() {
        return page.locator("img[src*='logo' i], img[alt*='logo' i]").first();
    }

    public String logoSrc() {
        try {
            String src = logo().getAttribute("src");
            return src == null ? "" : src;
        } catch (PlaywrightException notThere) {
            return "<no logo element on the page>";
        }
    }

    /**
     * True once the logo image has actually decoded, not merely once the tag exists. Polled: the
     * tag is in the markup before the bytes arrive, so a single read taken the moment the header
     * appears can return zero on a logo about to render perfectly well.
     */
    public boolean logoIsRendered() {
        long deadline = System.currentTimeMillis() + 10_000;
        do {
            try {
                Object width = logo().evaluate("e => e.naturalWidth");
                if (width instanceof Number number && number.intValue() > 0) {
                    return true;
                }
            } catch (PlaywrightException ignored) {
                // Not in the DOM yet; keep waiting rather than deciding on it.
            }
            page.waitForTimeout(500);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    public boolean logoIsTopLeft() {
        return isTopLeft(logo());
    }

    public String logoBox() {
        return boxOf(logo());
    }

    // ------------------------------------------------------------------ header auth controls

    /** The visible control carrying this exact label, e.g. "Log In". Unconfirmed whether the
     *  header renders a desktop and a mobile copy the way NewWeibo_automation's does - filtering
     *  to {@code setVisible(true)} is this project's best-effort guard against clicking a hidden
     *  duplicate either way. */
    private Locator headerControl(String label) {
        return page.getByText(label, new Page.GetByTextOptions().setExact(true))
                .filter(new Locator.FilterOptions().setVisible(true));
    }

    public boolean isHeaderButtonVisible(String label) {
        try {
            return headerControl(label).first().isVisible();
        } catch (PlaywrightException notThere) {
            return false;
        }
    }

    public boolean isHeaderButtonOnTheRight(String label) {
        return isOnTheRight(headerControl(label).first());
    }

    public String headerButtonColour(String label) {
        return textColourOf(headerControl(label).first());
    }

    public String headerButtonBox(String label) {
        return boxOf(headerControl(label).first());
    }

    /**
     * Clicks a header auth control and gives whatever it opens - a modal, or a navigation to a
     * dedicated route, this project has not confirmed which - a moment to settle. {@code LoginPage}
     * itself always navigates straight to {@code /en/log-in/} rather than going through the
     * header, so this is the one place in the project that actually exercises the header control.
     */
    public void openModalFrom(String headerLabel) {
        headerControl(headerLabel).first().click();
        page.waitForTimeout(1_500);
    }

    /**
     * True once the login form's own fields are reachable, however they got there - a modal over
     * this page, or a navigation to {@code /en/log-in/}. Uses the same "field below its visible
     * label" pattern {@code LoginPage.fieldNear} already relies on, so this does not invent a new
     * unconfirmed locator, only reuses the one already trusted there.
     */
    public boolean loginFormIsReachable() {
        return isFieldVisible("Username") && isFieldVisible("Password");
    }

    /**
     * True once the registration form's own fields are reachable. "Repeat password" is what tells
     * this apart from the login form, which has no such field.
     */
    public boolean registerFormIsReachable() {
        return isFieldVisible("Repeat password");
    }

    private boolean isFieldVisible(String label) {
        try {
            return page.locator("xpath=//*[normalize-space(text())='" + label + "']/following::input[1]")
                    .first().isVisible();
        } catch (PlaywrightException notThere) {
            return false;
        }
    }

    // ------------------------------------------------------------------ signed-in header

    /**
     * The currency codes visible anywhere on the page right now, de-duplicated in the order first
     * seen. Meant to be read right after something has opened the wallet/currency panel - see
     * {@code org.sengmean.page.wallet.CurrencySwitcher#open()} - rather than owning that trigger
     * itself, so this class does not duplicate that selector.
     */
    public List<String> currencyCodesOnScreen() {
        String text;
        try {
            text = page.innerText("body");
        } catch (PlaywrightException notThere) {
            return List.of();
        }
        List<String> currencies = new ArrayList<>();
        Matcher matcher = WALLET_CODE.matcher(text);
        while (matcher.find()) {
            if (!currencies.contains(matcher.group(1))) {
                currencies.add(matcher.group(1));
            }
        }
        return currencies;
    }

    /**
     * True when the given text appears anywhere in the page's rendered body text. Used to look
     * for the signed-in account's own name without pinning an unconfirmed header-chip selector -
     * NewWeibo_automation's equivalent reads a specific {@code Header_wallet-text} element, which
     * has no known match here.
     */
    public boolean showsText(String text) {
        try {
            return page.innerText("body").contains(text);
        } catch (PlaywrightException notThere) {
            return false;
        }
    }

    // ------------------------------------------------------------------ generic geometry helpers
    // Ported verbatim from NewWeibo_automation's page.home.HomePage - pure Playwright geometry,
    // no business-specific literal in any of them.

    private String boxOf(Locator element) {
        try {
            var box = element.boundingBox();
            return box == null ? "<not laid out>"
                    : "x=" + Math.round(box.x) + " y=" + Math.round(box.y)
                            + " w=" + Math.round(box.width) + " h=" + Math.round(box.height);
        } catch (PlaywrightException notThere) {
            return "<no such element>";
        }
    }

    private String textColourOf(Locator element) {
        return String.valueOf(element.evaluate("e => getComputedStyle(e).color"));
    }

    private boolean isOnTheRight(Locator element) {
        var box = element.boundingBox();
        double viewport = ((Number) page.evaluate("() => window.innerWidth")).doubleValue();
        return box != null && box.x + box.width / 2 > viewport / 2;
    }

    private boolean isTopLeft(Locator element) {
        var box = element.boundingBox();
        double viewport = ((Number) page.evaluate("() => window.innerWidth")).doubleValue();
        return box != null && box.x < viewport / 3 && box.y < 200;
    }
}
