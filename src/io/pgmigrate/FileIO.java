/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import io.pgmigrate.packaging.utils.ClassLoaderResolver;

import java.io.*;
import java.net.URL;

public class FileIO {

    public static InputStream getInputStream(String path) throws FileNotFoundException {
        if (path.startsWith("classpath://")) {
            InputStream stream = ClassLoaderResolver.getClassLoader().getResourceAsStream(path.substring("classpath://".length()));
            if (stream == null) {
                throw new FileNotFoundException("classpath entry: " + path + " not found");
            }
            return stream;
        }
        else {
            return new FileInputStream(path);
        }
    }

    public static boolean exists(String path) {
        if (path.startsWith("classpath://")) {
            URL resource = ClassLoaderResolver.getClassLoader().getResource(path.substring("classpath://".length()));
            return resource != null;
        }
        else {
            return new File(path).exists();
        }
    }


    public static String combine(String... paths) {
        if (paths == null || paths.length < 2) {
            throw new IllegalArgumentException("at least 2 paths must be specified");
        }

        boolean isClasspath = paths[0].startsWith("classpath://");

        if (isClasspath) {

            String path = paths[0];

            for (int i = 1; i < paths.length; i++) {
                if (isClasspath) {
                    path = path + "/" + paths[i];
                }
            }
            return path;
        }
        else {
            File last = new File(paths[0]);


            for (int i = 1; i < paths.length; i++) {
                last = new File(last, paths[i]);
            }

            return last.getPath();
        }
    }

    public static void delete(File directory) throws IOException {
        if (directory.isDirectory()) {
            for (File c : directory.listFiles()) {
                delete(c);
            }
        }
        if (!directory.delete()) {
            throw new FileNotFoundException("Failed to delete file: " + directory);
        }
    }


    public static String readAll(final InputStream input) throws IOException {
        final StringBuilder data = new StringBuilder();
        final byte[] buffer = new byte[2048];
        int read = 0;
        do {
            data.append(new String(buffer), 0, read);
            read = input.read(buffer);
        } while (read >= 0);
        return data.toString();
    }

}
