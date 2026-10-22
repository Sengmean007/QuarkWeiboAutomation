package org.sengmean.suites.wallet;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.wallet.CurrencyBannerTest;

/** Run with: ./mvnw test -Dsuite=WalletSuite */
@Suite
@SelectClasses({CurrencyBannerTest.class})
public class WalletSuite {
}
