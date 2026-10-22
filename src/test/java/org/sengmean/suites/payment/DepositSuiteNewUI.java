package org.sengmean.suites.payment;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.payment.DepositNewUiTest;

/** The deposit cases, New UI only. See {@link DepositSuiteClassicUI} for the other half and
 *  {@link DepositSuite} for both together. Run with: ./mvnw test -Dsuite=DepositSuiteNewUI */
@Suite
@SelectClasses({DepositNewUiTest.class})
public class DepositSuiteNewUI {
}
