package org.sengmean.suites.payment;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.payment.DepositClassicUiTest;

/** The deposit cases, Classic UI only. See {@link DepositSuiteNewUI} for the other half and
 *  {@link DepositSuite} for both together. Run with: ./mvnw test -Dsuite=DepositSuiteClassicUI */
@Suite
@SelectClasses({DepositClassicUiTest.class})
public class DepositSuiteClassicUI {
}
