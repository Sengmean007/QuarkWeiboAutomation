package org.sengmean.suites.layout;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.layout.ResponsiveLayoutNewUiTest;

/** The responsive-layout cases, New UI only. See {@link ResponsiveSuiteClassicUI} for the other
 *  half and {@link ResponsiveSuite} for both together. Run with: ./mvnw test -Dsuite=ResponsiveSuiteNewUI */
@Suite
@SelectClasses({ResponsiveLayoutNewUiTest.class})
public class ResponsiveSuiteNewUI {
}
