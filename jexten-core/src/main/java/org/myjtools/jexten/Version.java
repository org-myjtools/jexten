package org.myjtools.jexten;

import java.util.*;
import java.util.stream.Stream;

/**
 * Simplistic implementation of a version number that follows the Semantic Versioning
 * naming (<a href="https://semver.org/">...</a>).
 */
public class Version implements Comparable<Version> {


    private static final WeakHashMap<String,Version> CACHE = new WeakHashMap<>();

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
        return CACHE.computeIfAbsent(version, Version::new);
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


    private final int major;
    private final int minor;
    private final String patch;


    private Version(String version) {
        var parts = Stream.of(version.split("\\.")).iterator();
        try {
            this.major = Integer.parseInt(parts.next());
            this.minor = parts.hasNext() ? Integer.parseInt(parts.next()) : 0;
            this.patch = parts.hasNext() ? parts.next() : "";
        } catch (NoSuchElementException | NumberFormatException e) {
            throw new IllegalArgumentException(
                "Not valid version number %s : %s".formatted(version, e.getMessage())
            );
        }
    }


    /**
     * Return the major part of the version
     */
    public int major() {
        return major;
    }


    /**
     * Return the minor part of the version (<tt>0</tt> if not present)
     */
    public int minor() {
        return minor;
    }


    /**
     * Return the patch part of the version (or empty string if not present)
     */
    public String patch() {
        return patch;
    }


    /**
     * Check whether this version is compatible with the one passed as argument.
     * <p>
     * One version is compatible with other if the major part is the same and the minor part is greater or equal
     */
    public boolean isCompatibleWith(Version otherVersion) {
        return (major == otherVersion.major && minor >= otherVersion.minor);
    }


    @Override
    public String toString() {
        return patch.isBlank() ? major+"."+minor : major+"."+minor+"."+patch;
    }


    @Override
    public int compareTo(Version other) {
        return COMPARATOR.compare(this, other);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version that = (Version) o;
        return major == that.major && minor == that.minor && Objects.equals(patch, that.patch);
    }


    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

}