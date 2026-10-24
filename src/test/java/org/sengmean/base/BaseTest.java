package org.sengmean.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sengmean.config.ConfigDataProvider;
import org.sengmean.page.auth.LoginPage;
import org.sengmean.page.wallet.CurrencySwitcher;
import org.sengmean.report.CaseLog;
import org.sengmean.report.CaseTrailExtension;
import org.sengmean.support.Device;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sets up and tears down the Playwright browser for a test class. One browser per class
 * ({@link #launchBrowser()}), one isolated context and page per test ({@link #page()}).
 *
 * <h2>WebKit, for the {@link Device} profiles</h2>
 * {@link #browser} is Chromium, launched eagerly for every test class whether it ever
 * runs a device case. The Mac, iPad and iPhone {@link Device} profiles are Safari, which means
 * WebKit rather than Chromium pretending to be one - see {@link Device} for why that distinction
 * matters - so WebKit is launched separately, lazily, and only by a class that actually calls
 * {@link #openOn(Device)}. Ported from the sibling Admin_weibo project's
 * {@code UiOtherCategoryClientHK}, which has the same {@code webkitOrAbort()} / {@code capture()}
 * shape; lifted here rather than copied per test class so every device-aware test in this
 * project shares one launch and one teardown.
 */
@ExtendWith({CaseTrailExtension.class, ScreenshotOnFinish.class})
public class BaseTest {

    public static final String BASE_URL = BasePage.BASE_URL;
    public static final String USERNAME = ConfigDataProvider.get("test.username", "qa00002");
    public static final String PASSWORD = ConfigDataProvider.get("test.password", "password");
    public static final String CAPTCHA = ConfigDataProvider.get("test.captcha", "1111");

    /**
     * A second QA account, given by Si on 2026-10-24, for cases that specifically need an account
     * in a different state from the shared {@link #USERNAME} - starting with the deposit/withdraw
     * bank/account-binding validation cases, which need an account with no payment method bound
     * yet (unlike qa00002, which already has a Bank of China account bound - see
     * {@code quark-deposit-withdrawal-locators-confirmed}). Mirrors the sibling NewWeibo_automation
     * project's own {@code ALT_USERNAME}.
     */
    public static final String ALT_USERNAME = ConfigDataProvider.get("test.altUsername", "qa000051");
    public static final String ALT_PASSWORD = ConfigDataProvider.get("test.altPassword", "password");

    /**
     * The currency every signed-in suite is switched to right after signing in - see
     * {@link #signIn(Page, String, String)}. Defaults to HKD per Si's direction ("the currency
     * should be HKD by default whenever run the test"), overridable with {@code -Dtest.currency=}
     * for a run that specifically wants a different one, the same override shape every other
     * setting in this class already follows. See {@code quark-currency-defaults-to-hkd}: the
     * active currency is shared, account-level state, not browser-session state, so a suite that
     * signs in fresh still inherits whatever a previous suite or run last left the account on -
     * this exists to make every run start from the same, known currency rather than leaving that
     * to chance.
     */
    public static final String CURRENCY = ConfigDataProvider.get("test.currency", "HKD");

    public static final boolean HEADLESS = ConfigDataProvider.flag("headless", true);
    public static final double SLOW_MO = ConfigDataProvider.number("slowMo", 0);

    private static final double ACTION_TIMEOUT_MS =
            ConfigDataProvider.number("test.actionTimeout", 20_000);
    private static final double NAVIGATION_TIMEOUT_MS =
            ConfigDataProvider.number("test.navigationTimeout", 30_000);
    private static final double LOGIN_SETTLE_MS =
            ConfigDataProvider.number("test.login.settle", 3_000);

    protected static Playwright playwright;
    protected static Browser browser;

    /** Launched the first time a WebKit {@link Device} row runs; closed with the class. */
    protected static Browser webkit;

    private BrowserContext context;
    private Page page;

    /** The device row's own context, opened by {@link #openOn(Device)} and closed after the test. */
    private BrowserContext deviceContext;

    /** A fresh, unauthenticated context at the desktop viewport used throughout the manual pass. */
    public static BrowserContext newContext() {
        return newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080));
    }

    /** As {@link #newContext()}, for a specific viewport - a responsive breakpoint. */
    public static BrowserContext newContext(Browser.NewContextOptions options) {
        return newContext(browser, options);
    }

    /** As {@link #newContext(Browser.NewContextOptions)}, on a specific engine - {@link #webkit}. */
    public static BrowserContext newContext(Browser on, Browser.NewContextOptions options) {
        if (on == null) {
            throw new IllegalStateException(
                    "Browser not launched. Ensure the test class extends BaseTest.");
        }
        BrowserContext ctx = on.newContext(options);
        ctx.setDefaultTimeout(ACTION_TIMEOUT_MS);
        ctx.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MS);
        return ctx;
    }

    /**
     * A page on the device's own engine, viewport, pixel density, touch and user agent.
     *
     * <p>The caller owns nothing to close: the context opened here is the same one
     * {@link #tearDown()} closes after the test, alongside the ordinary {@link #context()}.
     */
    protected Page openOn(Device device) {
        Browser on = device.engine == Device.Engine.WEBKIT ? webkitOrAbort() : browser;
        deviceContext = newContext(on, device.contextOptions());
        return deviceContext.newPage();
    }

    /**
     * WebKit, launched once per class and shared by every device row in it. A case whose device
     * is WebKit aborts - rather than fails - naming the installation it needs, when WebKit is not
     * installed in this environment; see {@link Device} for why Safari must be WebKit rather
     * than Chromium pretending to be one.
     */
    protected static Browser webkitOrAbort() {
        if (webkit == null) {
            try {
                webkit = playwright.webkit().launch(
                        new BrowserType.LaunchOptions().setHeadless(HEADLESS).setSlowMo(SLOW_MO));
            } catch (RuntimeException notInstalled) {
                String why = notInstalled.getMessage() == null
                        ? "" : notInstalled.getMessage().split("\\R", 2)[0];
                Assumptions.abort("This case runs in WebKit, which could not be launched (" + why
                        + "). Install it with `npx playwright install webkit` (or "
                        + "./mvnw exec:java -Dexec.mainClass=com.microsoft.playwright.CLI "
                        + "-Dexec.args=\"install webkit\") and rerun.");
            }
        }
        return webkit;
    }

    /**
     * Keeps a screenshot of the device's screen beside the generated report's own evidence, named
     * {@code <thisSuite>.<device>.<what>.png} so {@code HtmlReportGenerator} can pick it up.
     */
    protected void capture(Page on, Device device, String what) {
        try {
            Path shots = Path.of(System.getProperty("report.shots", "target/case-screenshots"));
            Files.createDirectories(shots);
            Path file = shots.resolve(
                    getClass().getSimpleName() + "." + device.name().toLowerCase() + "." + what + ".png");
            on.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(false));
            CaseLog.evidence(device.label + " - " + what + ": " + file);
        } catch (Exception couldNotCapture) {
            CaseLog.evidence("No screenshot of " + what + " on " + device.label + ": "
                    + couldNotCapture.getMessage());
        }
    }

    /**
     * Signs the shared QA account in, clears the overlays the site raises after login (the chat
     * panel; the Daily Mission popup if it appears for this account), and switches the account to
     * {@link #CURRENCY}. Centralized here - rather than left to each test class to call
     * {@code CurrencySwitcher} itself - so every signed-in suite gets it the same way with nothing
     * to remember at the call site; previously a handful of suites each added their own
     * {@code new CurrencySwitcher(...).selectCurrency("HKD")} line, which meant a new suite had to
     * know to do the same.
     */
    public static void signIn(Page page) {
        signIn(page, USERNAME, PASSWORD);
    }

    public static void signIn(Page page, String username, String password) {
        new LoginPage(page).loginWithAllFields(username, password, CAPTCHA);
        page.waitForTimeout(LOGIN_SETTLE_MS);
        new LoginPage(page).dismissOverlays();
        new CurrencySwitcher(page).selectCurrency(CURRENCY);
    }

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        System.out.println("Launching chromium: headless=" + HEADLESS + " slowMo=" + SLOW_MO);
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(HEADLESS).setSlowMo(SLOW_MO));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (webkit != null) {
            webkit.close();
            webkit = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }

    /** The per-test page, opened on first use so a test that never asks for one costs nothing. */
    protected Page page() {
        if (page == null) {
            page = context().newPage();
        }
        return page;
    }

    protected BrowserContext context() {
        if (context == null) {
            context = newContext();
        }
        return context;
    }

    @BeforeEach
    void setUp() {
        // Deliberately empty; page() / context() open on first use.
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
            context = null;
        }
        if (deviceContext != null) {
            deviceContext.close();
            deviceContext = null;
        }
        page = null;
    }
}
