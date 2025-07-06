// Copyright  (c) 2022 -  Luis Iñesta Gelabert  <luiinge@gmail.com>

package org.myjtools.jexten.internal;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;


public final class InternalUtil {

    private InternalUtil() {
        /* avoid instantiation */
    }




    public static String formatList(List<?> list) {
        return list.stream().map(String::valueOf).collect(Collectors.joining("\n\t","\n\t","\n"));
    }


    public static Path createFolder(Path folder) throws IOException {
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }
        return folder;
    }


    public static boolean deleteDirectory(Path folder, Logger logger) {
        try (var walker = Files.walk(folder)) {
            return walker
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .map(File::delete)
                .reduce(Boolean::logicalAnd)
                .orElse(Boolean.FALSE);
        } catch (IOException e) {
            logger.error("Problem deleting directory {} : {}", folder, e.getMessage());
            logger.debug(e.toString(), e);
            return false;
        }
    }


    public static void deleteSubdirectories(Path folder, Logger logger) throws IOException {
        try (var walker = Files.walk(folder, 1)) {
            walker.forEach( it -> deleteDirectory(it, logger));
        }
    }


    public static void copyDirectoryContents(Path source, Path target) throws IOException {
        createFolder(target);
        try (var walker = Files.walk(source)) {
            var files  = walker.toList();
            for (Path file : files) {
                Path relativeFile = source.relativize(file);
                Path targetFile = target.resolve(relativeFile);
                if (Files.isDirectory(file)) {
                    createFolder(targetFile);
                } else {
                    Files.copy(file, targetFile);
                }
            }
        }
    }

}
