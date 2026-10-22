package org.sengmean.suites.auth;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sengmean.tests.auth.LoginTest;
import org.sengmean.tests.auth.RegisterTest;

/** Run with: ./mvnw test -Dsuite=AuthSuite */
@Suite
@SelectClasses({LoginTest.class, RegisterTest.class})
public class AuthSuite {
}
