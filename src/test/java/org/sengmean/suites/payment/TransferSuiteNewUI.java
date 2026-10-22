package org.sengmean.suites.payment;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.payment.ManualTransferNewUiTest;

/** The manual-transfer cases, New UI only. See {@link TransferSuiteClassicUI} for the other
 *  half and {@link TransferSuite} for both together. Run with: ./mvnw test -Dsuite=TransferSuiteNewUI */
@Suite
@SelectClasses({ManualTransferNewUiTest.class})
public class TransferSuiteNewUI {
}
