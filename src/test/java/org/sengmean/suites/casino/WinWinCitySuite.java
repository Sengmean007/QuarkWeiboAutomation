package org.sengmean.suites.casino;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.casino.WinWinCityClassicUiTest;
import org.sengmean.tests.casino.WinWinCityNewUiTest;

/** Run with: ./mvnw test -Dsuite=WinWinCitySuite */
@Suite
@SelectClasses({WinWinCityNewUiTest.class, WinWinCityClassicUiTest.class})
public class WinWinCitySuite {
}
