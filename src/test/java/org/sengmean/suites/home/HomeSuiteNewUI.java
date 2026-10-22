package org.sengmean.suites.home;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.home.HomePageNewUiTest;

/** The home-page cases, New UI only. See {@link HomeSuiteClassicUI} for the other half and
 *  {@link HomeSuite} for both together. Run with: ./mvnw test -Dsuite=HomeSuiteNewUI */
@Suite
@SelectClasses({HomePageNewUiTest.class})
public class HomeSuiteNewUI {
}
