package org.sengmean.suites.casino;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.casino.WinWinCityNewUiTest;

/** The WinWin City cases, New UI only. See {@link WinWinCitySuiteClassicUI} for the other half
 *  and {@link WinWinCitySuite} for both together. Run with: ./mvnw test -Dsuite=WinWinCitySuiteNewUI */
@Suite
@SelectClasses({WinWinCityNewUiTest.class})
public class WinWinCitySuiteNewUI {
}
