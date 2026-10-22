package org.sengmean.suites.home;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.home.HomePageClassicUiTest;
import org.sengmean.tests.home.HomePageNewUiTest;

/** Run with: ./mvnw test -Dsuite=HomeSuite */
@Suite
@SelectClasses({HomePageNewUiTest.class, HomePageClassicUiTest.class})
public class HomeSuite {
}
