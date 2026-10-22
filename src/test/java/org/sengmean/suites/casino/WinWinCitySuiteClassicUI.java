package org.sengmean.suites.casino;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.casino.WinWinCityClassicUiTest;

/** The WinWin City cases, Classic UI only. See {@link WinWinCitySuiteNewUI} for the other half
 *  and {@link WinWinCitySuite} for both together. Run with: ./mvnw test -Dsuite=WinWinCitySuiteClassicUI */
@Suite
@SelectClasses({WinWinCityClassicUiTest.class})
public class WinWinCitySuiteClassicUI {
}
