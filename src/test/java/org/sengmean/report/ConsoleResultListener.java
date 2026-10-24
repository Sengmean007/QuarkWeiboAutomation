package org.sengmean.report;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prints each test case's pass/fail result to the console as it finishes, rather than only
 * after the whole run through the HTML or Extent reports.
 *
 * <p>Registered through {@code META-INF/services} alongside the other listeners, so it fires
 * on every run - {@code ./mvnw test}, one suite, a single method from the IDE - with nothing
 * extra to turn on.
 */
public class ConsoleResultListener implements TestExecutionListener {

    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();

    @Override
    public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
        if (!isCase(identifier)) {
            return;
        }
        switch (result.getStatus()) {
            case SUCCESSFUL -> {
                passed.incrementAndGet();
                System.out.println("[PASS] " + identifier.getDisplayName());
            }
            case FAILED -> {
                failed.incrementAndGet();
                System.out.println("[FAIL] " + identifier.getDisplayName()
                        + " - " + message(result));
            }
            case ABORTED -> {
                skipped.incrementAndGet();
                System.out.println("[SKIPPED] " + identifier.getDisplayName()
                        + " - " + message(result));
            }
            default -> { }
        }
    }

    @Override
    public void executionSkipped(TestIdentifier identifier, String reason) {
        if (!isCase(identifier)) {
            return;
        }
        skipped.incrementAndGet();
        System.out.println("[SKIPPED] " + identifier.getDisplayName() + " - " + reason);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        System.out.println("Result: " + passed.get() + " passed, " + failed.get()
                + " failed, " + skipped.get() + " skipped");
    }

    /** Only a method result is a case; a class or the engine itself is not one to print. */
    private static boolean isCase(TestIdentifier identifier) {
        return identifier.getSource().filter(MethodSource.class::isInstance).isPresent();
    }

    private static String message(TestExecutionResult result) {
        return result.getThrowable().map(throwable -> {
            String text = throwable.getMessage();
            return text == null || text.isBlank() ? throwable.getClass().getSimpleName() : text;
        }).orElse("");
    }
}
