package org.sengmean.suites.payment;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.payment.WithdrawalClassicUiTest;
import org.sengmean.tests.payment.WithdrawalNewUiTest;

/** Run with: ./mvnw test -Dsuite=WithdrawalSuite */
@Suite
@SelectClasses({WithdrawalNewUiTest.class, WithdrawalClassicUiTest.class})
public class WithdrawalSuite {
}
