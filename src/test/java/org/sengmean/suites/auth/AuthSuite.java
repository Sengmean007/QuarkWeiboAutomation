package org.sengmean.suites.auth;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.auth.LoginClassicUiTest;
import org.sengmean.tests.auth.LoginMultiLanguageTest;
import org.sengmean.tests.auth.LoginNewUiTest;
import org.sengmean.tests.auth.RegisterTest;

/**
 * Run with: ./mvnw test -Dsuite=AuthSuite
 *
 * <p>{@code LoginTest} was split into {@link LoginNewUiTest} / {@link LoginClassicUiTest} - see
 * either class's Javadoc. {@link LoginMultiLanguageTest} confirms the login form's own validation
 * message is translated, not just the page chrome, for every language the header's language menu
 * offers - see that class's Javadoc. {@code RegisterTest} remains the README's one exception to
 * the UI split: no evidence the registration form differs by UI version either, and nothing here
 * has tested that claim yet the way this split now does for Login.
 */
@Suite
@SelectClasses({LoginNewUiTest.class, LoginClassicUiTest.class, LoginMultiLanguageTest.class, RegisterTest.class})
public class AuthSuite {
}
