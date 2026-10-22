package org.sengmean.suites.payment;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.payment.WithdrawalClassicUiTest;

/** The withdrawal cases, Classic UI only. See {@link WithdrawalSuiteNewUI} for the other half
 *  and {@link WithdrawalSuite} for both together. Run with: ./mvnw test -Dsuite=WithdrawalSuiteClassicUI */
@Suite
@SelectClasses({WithdrawalClassicUiTest.class})
public class WithdrawalSuiteClassicUI {
}
