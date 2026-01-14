package org.myjtools.jexten;

import java.util.*;
import java.util.stream.Stream;

/**
 * Simplistic implementation of a version number that follows the Semantic Versioning
 * naming (<a href="https://semver.org/">...</a>).
 */
public record Version(int major, int minor, String patch) implements Comparable<Version> {



    /**
     * Creates a new instance from a string representation
     * @param version The string representation of the version, like <tt>2.4.1-SNAPSHOT</tt>
     * @throws IllegalArgumentException if the version format is ill-formed
     * @return A new semantic version instance
     */
    public static Version of(String version) {
        if (version == null || version.isBlank()) {
           throw new IllegalArgumentException("Version string cannot be null or blank");
        }
        try {
            var parts = Stream.of(version.split("\\.")).iterator();
            int major = Integer.parseInt(parts.next());
            int minor = parts.hasNext() ? Integer.parseInt(parts.next()) : 0;
            String patch = parts.hasNext() ? parts.next() : "";
            return new Version(major, minor, patch);
        }  catch (NoSuchElementException | NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Not valid version number %s : %s".formatted(version, e.getMessage())
            );
        }
    }


    /**
     * Check whether the given string representation is a valid semantic version
     */
    public static boolean validate(String version) {
        try {
            of(version);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    private static final Comparator<Version> COMPARATOR = Comparator
        .comparingInt(Version::major)
        .thenComparingInt(Version::minor)
        .thenComparing(Version::patch);







    /**
     * Check whether this version is compatible with the one passed as argument.
     * <p>
     * One version is compatible with other if the major part is the same and the minor part is greater or equal
     */
    public boolean isCompatibleWith(Version otherVersion) {
        return major == otherVersion.major && minor >= otherVersion.minor;
    }


    @Override
    public String toString() {
        return patch.isBlank() ? major+"."+minor : major+"."+minor+"."+patch;
    }


    @Override
    public int compareTo(Version other) {
        return COMPARATOR.compare(this, other);
    }


}