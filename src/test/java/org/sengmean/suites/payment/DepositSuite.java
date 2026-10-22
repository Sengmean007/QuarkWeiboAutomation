package org.sengmean.suites.payment;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.payment.DepositClassicUiTest;
import org.sengmean.tests.payment.DepositNewUiTest;

/** Run with: ./mvnw test -Dsuite=DepositSuite */
@Suite
@SelectClasses({DepositNewUiTest.class, DepositClassicUiTest.class})
public class DepositSuite {
}
