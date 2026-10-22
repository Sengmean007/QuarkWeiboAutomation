package org.sengmean.support;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.launcher.PostDiscoveryFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs a single case by its client case id: {@code ./mvnw test -Dcase=LGN-002}.
 *
 * <h2>Why a filter and not -Dtest</h2>
 * A case id lives in the {@code @DisplayName} - "LGN-002: an empty form cannot be submitted" -
 * and nowhere else. Surefire's {@code -Dtest} is a class and <b>method name</b> filter, so
 * {@code -Dtest=LGN-002} matches no method and reports the same misleading "Tests run: 0" that
 * the POM's {@code -Dsuite} comment describes. Selecting the case therefore means knowing which
 * Java method carries it, which is exactly the lookup a case id is supposed to save.
 *
 * The JUnit Platform applies a {@link PostDiscoveryFilter} after the engines have resolved the
 * tree, when display names exist, so the id can be matched directly. Registered through
 * {@code META-INF/services}, alongside the report listeners, so it works wherever the suites are
 * launched from - Maven, the IDE, a run configuration.
 *
 * <h2>Usage</h2>
 * <pre>
 *   ./mvnw test -Dcase=LGN-002                      one case, found wherever it lives
 *   ./mvnw test -Dcase=LGN-002,LGN-003              several at once
 *   ./mvnw test -Dsuite=AuthSuite -Dcase=LGN-002    narrow the scan first, for a faster run
 * </pre>
 *
 * With no {@code -Dcase} the filter includes everything, so ordinary runs are untouched.
 *
 * <h2>What counts as a match</h2>
 * The display name must <i>start</i> with the id, and the character after it must not be one that
 * could continue an id. That is what keeps {@code -Dcase=LGN-002} off a hypothetical LGN-0021:
 * a plain {@code startsWith} would quietly run both. Matching is case-insensitive so the ids can
 * be typed in upper or lower case.
 *
 * Every selected case is printed as it is included, because a filter that selects nothing
 * otherwise looks identical to a suite that passed.
 */
public class CaseIdFilter implements PostDiscoveryFilter {

    /** System property carrying the case ids, comma separated. */
    public static final String PROPERTY = "case";

    private final List<String> ids = read();

    /**
     * Case ids already announced. The filter is applied once per engine tree - the suite engine
     * resolves the same case the Jupiter engine does - so without this the selection is printed
     * twice for a suite run and reads as two cases.
     */
    private final Set<String> announced = ConcurrentHashMap.newKeySet();

    private static List<String> read() {
        String raw = System.getProperty(PROPERTY);
        List<String> parsed = new ArrayList<>();
        if (raw == null) {
            return parsed;
        }
        for (String part : raw.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                parsed.add(id.toUpperCase(Locale.ROOT));
            }
        }
        return parsed;
    }

    @Override
    public FilterResult apply(TestDescriptor descriptor) {
        // Containers must survive, or the tests underneath them are never reached. The platform
        // prunes the ones that end up empty, so no class is opened for a case it does not carry.
        if (ids.isEmpty() || !descriptor.isTest()) {
            return FilterResult.included(null);
        }
        String displayName = descriptor.getDisplayName();
        for (String id : ids) {
            if (matches(displayName, id)) {
                if (announced.add(displayName)) {
                    System.out.println("[case] " + id + " -> " + displayName);
                }
                return FilterResult.included("matched -D" + PROPERTY + "=" + id);
            }
        }

        return FilterResult.excluded("no -D" + PROPERTY + " id matches " + displayName);
    }

    private static boolean matches(String displayName, String id) {
        String upper = displayName.toUpperCase(Locale.ROOT);
        if (!upper.startsWith(id)) {
            return false;
        }
        if (upper.length() == id.length()) {
            return true;
        }
        char next = upper.charAt(id.length());
        return !Character.isLetterOrDigit(next) && next != '-';
    }
}
