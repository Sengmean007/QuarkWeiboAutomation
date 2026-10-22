package org.sengmean.suites.ux;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.ux.UxJourneyNewUiTest;

/** The UX journey cases, New UI only. See {@link UxJourneySuiteClassicUI} for the other half
 *  and {@link UxJourneySuite} for both together. Run with: ./mvnw test -Dsuite=UxJourneySuiteNewUI */
@Suite
@SelectClasses({UxJourneyNewUiTest.class})
public class UxJourneySuiteNewUI {
}
