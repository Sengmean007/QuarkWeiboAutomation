package org.sengmean.suites.ux;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.ux.UxJourneyClassicUiTest;
import org.sengmean.tests.ux.UxJourneyNewUiTest;

/** Run with: ./mvnw test -Dsuite=UxJourneySuite */
@Suite
@SelectClasses({UxJourneyNewUiTest.class, UxJourneyClassicUiTest.class})
public class UxJourneySuite {
}
