package org.sengmean.suites.layout;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.layout.ResponsiveLayoutClassicUiTest;
import org.sengmean.tests.layout.ResponsiveLayoutNewUiTest;

/** Run with: ./mvnw test -Dsuite=ResponsiveSuite */
@Suite
@SelectClasses({ResponsiveLayoutNewUiTest.class, ResponsiveLayoutClassicUiTest.class})
public class ResponsiveSuite {
}
