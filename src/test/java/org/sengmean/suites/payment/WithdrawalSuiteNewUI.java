package org.sengmean.suites.payment;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.payment.WithdrawalNewUiTest;

/** The withdrawal cases, New UI only. See {@link WithdrawalSuiteClassicUI} for the other half
 *  and {@link WithdrawalSuite} for both together. Run with: ./mvnw test -Dsuite=WithdrawalSuiteNewUI */
@Suite
@SelectClasses({WithdrawalNewUiTest.class})
public class WithdrawalSuiteNewUI {
}
