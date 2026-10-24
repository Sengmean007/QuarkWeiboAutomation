package org.sengmean.report;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;

/**
 * Opens and closes {@link CaseLog}'s trail for every test method, and narrates the case's own
 * outcome into it - "PASS", "FAIL: &lt;reason&gt;", "SKIP: &lt;reason&gt;" or
 * "ERROR: &lt;reason&gt;" - so a reader of the report's own step panel sees the verdict next to
 * the evidence for it, not just the row's colour.
 *
 * <h2>Confirmed live 2026-10-25: every suite's step trail has been empty since this project
 * started</h2>
 * {@link CaseLog}'s own class Javadoc already describes exactly this responsibility, under the
 * name {@code ScreenshotOnFinish.afterTestExecution} calling {@link CaseLog#start(String)} before
 * a case and {@link CaseLog#finish()} after - but no such class was ever written anywhere in this
 * project. Every one of the 173 cases across every one of this project's own generated reports
 * read "No step trail was recorded for this case", including cases whose page objects call
 * {@link CaseLog#step}/{@link CaseLog#data} on every action (confirmed: {@code TransferPage},
 * {@code DepositPage} and others all call these already) - because {@link CaseLog#start(String)}
 * was never called for any of them, so every {@link CaseLog#add} found an empty
 * {@code ThreadLocal} and discarded the line without recording anything. Si asked for this
 * specifically for {@code ManualTransferNewUiTest}/{@code ManualTransferClassicUiTest}'s own
 * missing pass/fail log; the gap turned out to be this class never existing at all, project-wide,
 * so the fix is here rather than in either of those two classes.
 *
 * <h2>Registered on {@code BaseTest}, not auto-detected</h2>
 * This project does not set {@code junit.jupiter.extensions.autodetection.enabled=true} in its
 * {@code junit-platform.properties}, so the extension needs an explicit {@code @ExtendWith} -
 * added once on {@link org.sengmean.base.BaseTest} rather than on every individual suite, since
 * every suite in this project already extends it.
 *
 * <h2>Naming matches what the report generator already expects</h2>
 * {@link HtmlReportGenerator#readStepLogs(java.nio.file.Path)} parses each log file's name as
 * {@code Suite.method.log} into the key {@code Suite#method} - the same key
 * {@code HtmlReportListener} already uses for the row's own Pass/Fail/Error/Skip status, read off
 * the top-level test class (not a {@code @Nested} one) so the two stay joined on one case.
 */
public class CaseTrailExtension implements BeforeEachCallback, AfterTestExecutionCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        CaseLog.start(caseName(context));
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        context.getExecutionException().ifPresentOrElse(
                throwable -> CaseLog.step(outcome(throwable) + ": " + describe(throwable)),
                () -> CaseLog.step("PASS"));
        CaseLog.finish();
    }

    /** {@code Suite.method} - see this class's own Javadoc for why it has to be the top-level
     *  class's simple name. */
    private static String caseName(ExtensionContext context) {
        Class<?> topLevel = context.getRequiredTestClass();
        while (topLevel.getEnclosingClass() != null) {
            topLevel = topLevel.getEnclosingClass();
        }
        return topLevel.getSimpleName() + "." + context.getRequiredTestMethod().getName();
    }

    /** The same assertion-vs-abort-vs-error split {@code HtmlReportListener#status} already makes
     *  for the row's own colour, so the step trail's last line always agrees with it. */
    private static String outcome(Throwable throwable) {
        if (throwable instanceof TestAbortedException) {
            return "SKIP";
        }
        String type = throwable.getClass().getName();
        boolean assertion = throwable instanceof AssertionError
                || type.startsWith("org.opentest4j.")
                || type.startsWith("com.microsoft.playwright.assertions.");
        return assertion ? "FAIL" : "ERROR";
    }

    private static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
