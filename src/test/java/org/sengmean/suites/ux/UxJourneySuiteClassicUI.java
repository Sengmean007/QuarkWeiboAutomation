package org.sengmean.suites.ux;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.ux.UxJourneyClassicUiTest;

/** The UX journey cases, Classic UI only. See {@link UxJourneySuiteNewUI} for the other half
 *  and {@link UxJourneySuite} for both together. Run with: ./mvnw test -Dsuite=UxJourneySuiteClassicUI */
@Suite
@SelectClasses({UxJourneyClassicUiTest.class})
public class UxJourneySuiteClassicUI {
}
