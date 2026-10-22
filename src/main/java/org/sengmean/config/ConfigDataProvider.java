package org.sengmean.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Every setting a run can be pointed at, read from {@code Config/} rather than from the POM.
 *
 * <p>Precedence, highest first: {@code -Dkey=value} on the command line, then the matching
 * environment variable ({@code base.url} reads {@code BASE_URL}), then
 * {@code Config/secrets.properties} (not committed), then {@code Config/config.properties}
 * (committed), then the fallback the caller passes.
 *
 * <p>Loading this class also seeds every value it found into {@link System#getProperties()},
 * skipping a key that is already set so a {@code -D} is never overwritten - that is what lets
 * the rest of the project read {@code System.getProperty} without being rewritten.
 *
 * <p>Ported from the sibling NewWeibo_automation / Admin_weibo projects' class of the same name;
 * see those for the fuller history of why each piece of this is the way it is.
 */
public final class ConfigDataProvider {

    private static final Path DIR = Path.of(System.getProperty("config.dir", "Config"));
    private static final String CONFIG_FILE = "config.properties";
    private static final String SECRETS_FILE = "secrets.properties";

    private static final Map<String, String> VALUES = load();

    static {
        seedSystemProperties();
    }

    private ConfigDataProvider() {
    }

    public static String get(String key, String fallback) {
        String property = System.getProperty(key);
        if (isSet(property)) {
            return property.trim();
        }
        String environment = System.getenv(environmentName(key));
        if (isSet(environment)) {
            return environment.trim();
        }
        String configured = VALUES.get(key);
        return isSet(configured) ? configured.trim() : fallback;
    }

    public static String get(String key) {
        return get(key, "");
    }

    /** {@code true} only for the word "true", so an absent or misspelt setting means false. */
    public static boolean flag(String key, boolean fallback) {
        return Boolean.parseBoolean(get(key, String.valueOf(fallback)));
    }

    /** A number setting, falling back rather than throwing when the value is not one. */
    public static double number(String key, double fallback) {
        String value = get(key, "");
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException notANumber) {
            System.out.println("Config: " + key + " is not a number (" + value
                    + "), using " + fallback);
            return fallback;
        }
    }

    public static Map<String, String> all() {
        return Map.copyOf(VALUES);
    }

    private static String environmentName(String key) {
        return key.toUpperCase().replace('.', '_').replace('-', '_');
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private static Map<String, String> load() {
        Map<String, String> values = new LinkedHashMap<>();
        readInto(values, DIR.resolve(CONFIG_FILE));
        readInto(values, DIR.resolve(SECRETS_FILE));
        return values;
    }

    private static void readInto(Map<String, String> values, Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException cannotRead) {
            System.out.println("Config: could not read " + file + " (" + cannotRead.getMessage()
                    + "), carrying on with the defaults");
            return;
        }
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
    }

    private static void seedSystemProperties() {
        List<String> seeded = new ArrayList<>();
        for (Map.Entry<String, String> entry : VALUES.entrySet()) {
            String key = entry.getKey();
            if (isSet(System.getProperty(key)) || !isSet(entry.getValue())) {
                continue;
            }
            System.setProperty(key, get(key, entry.getValue().trim()));
            seeded.add(key);
        }
        if (!seeded.isEmpty() && flag("config.verbose", false)) {
            System.out.println("Config: " + DIR + " supplied " + seeded);
        }
    }
}
