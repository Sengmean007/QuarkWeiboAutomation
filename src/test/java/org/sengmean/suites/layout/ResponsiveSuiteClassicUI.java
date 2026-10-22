package org.sengmean.suites.layout;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.layout.ResponsiveLayoutClassicUiTest;

/** The responsive-layout cases, Classic UI only. See {@link ResponsiveSuiteNewUI} for the other
 *  half and {@link ResponsiveSuite} for both together. Run with: ./mvnw test -Dsuite=ResponsiveSuiteClassicUI */
@Suite
@SelectClasses({ResponsiveLayoutClassicUiTest.class})
public class ResponsiveSuiteClassicUI {
}
