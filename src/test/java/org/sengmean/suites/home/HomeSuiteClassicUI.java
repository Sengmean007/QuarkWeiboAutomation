package org.sengmean.suites.home;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.home.HomePageClassicUiTest;

/** The home-page cases, Classic UI only. See {@link HomeSuiteNewUI} for the other half and
 *  {@link HomeSuite} for both together. Run with: ./mvnw test -Dsuite=HomeSuiteClassicUI */
@Suite
@SelectClasses({HomePageClassicUiTest.class})
public class HomeSuiteClassicUI {
}
