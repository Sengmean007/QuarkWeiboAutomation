package org.sengmean.suites.payment;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.payment.ManualTransferClassicUiTest;

/** The manual-transfer cases, Classic UI only. See {@link TransferSuiteNewUI} for the other
 *  half and {@link TransferSuite} for both together. Run with: ./mvnw test -Dsuite=TransferSuiteClassicUI */
@Suite
@SelectClasses({ManualTransferClassicUiTest.class})
public class TransferSuiteClassicUI {
}
