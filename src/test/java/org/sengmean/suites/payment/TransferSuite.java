package org.sengmean.suites.payment;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.payment.ManualTransferClassicUiTest;
import org.sengmean.tests.payment.ManualTransferNewUiTest;

/** Run with: ./mvnw test -Dsuite=TransferSuite */
@Suite
@SelectClasses({ManualTransferNewUiTest.class, ManualTransferClassicUiTest.class})
public class TransferSuite {
}
