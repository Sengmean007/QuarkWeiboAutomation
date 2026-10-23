package org.sengmean.suites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.auth.LoginNewUiTest;
import org.sengmean.tests.casino.WinWinCityNewUiTest;
import org.sengmean.tests.home.HomePageNewUiTest;
import org.sengmean.tests.layout.ResponsiveLayoutNewUiTest;
import org.sengmean.tests.payment.DepositNewUiTest;
import org.sengmean.tests.payment.ManualTransferNewUiTest;
import org.sengmean.tests.payment.WithdrawalNewUiTest;
import org.sengmean.tests.ux.UxJourneyNewUiTest;

/**
 * Every area's New UI half, run on its own - the New Interface side of the per-area suites
 * ({@code AuthSuite}, {@code ResponsiveSuite}, {@code HomeSuite}, {@code DepositSuite},
 * {@code WithdrawalSuite}, {@code TransferSuite}, {@code WinWinCitySuite},
 * {@code UxJourneySuite}), gathered here so a run can cover "does New UI hold up everywhere"
 * without also running the Classic UI half of each. {@code RegisterTest} is left out, same as
 * {@code AuthSuite} leaves it out of either UI half - it has no UI split; see
 * {@link org.sengmean.tests.auth.LoginNewUiTest}'s Javadoc. See {@link ClassicUiSuite} for the
 * other half.
 *
 * Run with: ./mvnw test -Dsuite=NewUiSuite
 */
@Suite
@SelectClasses({
        LoginNewUiTest.class,
        ResponsiveLayoutNewUiTest.class,
        HomePageNewUiTest.class,
        DepositNewUiTest.class,
        WithdrawalNewUiTest.class,
        ManualTransferNewUiTest.class,
        WinWinCityNewUiTest.class,
        UxJourneyNewUiTest.class
})
public class NewUiSuite {
}
