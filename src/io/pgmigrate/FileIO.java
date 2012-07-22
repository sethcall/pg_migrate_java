/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import java.io.*;

public class FileIO {

    public static String combine(String... paths) {
        if(paths == null || paths.length < 2) {
            throw new IllegalArgumentException("at least 2 paths must be specified");
        }
        File last = new File(paths[0]);

        for(int i = 1; i < paths.length; i++) {
            last = new File(last, paths[1]);
        }

        return last.getPath();
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
