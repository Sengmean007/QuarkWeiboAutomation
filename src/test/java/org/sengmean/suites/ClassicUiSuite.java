package org.sengmean.suites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.auth.LoginClassicUiTest;
import org.sengmean.tests.casino.WinWinCityClassicUiTest;
import org.sengmean.tests.home.HomePageClassicUiTest;
import org.sengmean.tests.layout.ResponsiveLayoutClassicUiTest;
import org.sengmean.tests.payment.DepositClassicUiTest;
import org.sengmean.tests.payment.ManualTransferClassicUiTest;
import org.sengmean.tests.payment.WithdrawalClassicUiTest;
import org.sengmean.tests.ux.UxJourneyClassicUiTest;

/**
 * Every area's Classic UI half, run on its own - see {@link NewUiSuite} for the New Interface
 * half and the rationale. The same generalizations/unconfirmed-feature caveats documented in
 * each individual class's Javadoc (README "Ported-but-unconfirmed areas") still apply here;
 * this suite only changes what gets run together, not what any case actually checks.
 * {@code RegisterTest} is left out, same as {@code AuthSuite} leaves it out of either UI half -
 * it has no UI split; see {@link org.sengmean.tests.auth.LoginClassicUiTest}'s Javadoc.
 *
 * Run with: ./mvnw test -Dsuite=ClassicUiSuite
 */
@Suite
@SelectClasses({
        LoginClassicUiTest.class,
        ResponsiveLayoutClassicUiTest.class,
        HomePageClassicUiTest.class,
        DepositClassicUiTest.class,
        WithdrawalClassicUiTest.class,
        ManualTransferClassicUiTest.class,
        WinWinCityClassicUiTest.class,
        UxJourneyClassicUiTest.class
})
public class ClassicUiSuite {
}
