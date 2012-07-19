/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SqlReader {

    private static final Logger LOG = LoggerFactory.getLogger(SqlReader.class);
    private static Pattern START_FUNCTION = Pattern.compile("^\\s*CREATE\\s+(OR\\s+REPLACE\\s+)?FUNCTION", Pattern.CASE_INSENSITIVE);
    private static Pattern END_FUNCTION = Pattern.compile("(plpgsql|plperl|plpythonu|pltcl|sql)\\s*;$", Pattern.CASE_INSENSITIVE);

    public List<String> loadManifest(String manifestPath) throws IOException {
        final List<String> statements = new ArrayList<String>();

        FileInputStream inputStream = null;
        DataInputStream dataInputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = new FileInputStream(manifestPath);
            dataInputStream = new DataInputStream(inputStream);
            reader = new BufferedReader(new InputStreamReader(dataInputStream));
            String line;
            String currentStatement = "";

            while ((line = reader.readLine()) != null) {

                final String lineStripped = line.trim();

                if (lineStripped.length() == 0 || lineStripped.startsWith("--")) {
                    //it's a comment; ignore'
                }
                else if (lineStripped.startsWith("\\")) {
                    // it's a psql command; ignore
                }
                else {
                    currentStatement += " " + lineStripped;
                    if (lineStripped.endsWith(";")) {
                        if (START_FUNCTION.matcher(currentStatement).matches()) {
                            // if we are in a function, a ';' isn't enough to end.  We need to see if the last word was one of
                            // pltcl, plperl, plpgsql, plpythonu, sql.
                            // you can extend languages in postgresql; detecting these isn't supported yet.
                            if (END_FUNCTION.matcher(currentStatement).matches()) {
                                statements.add(currentStatement.substring(0, currentStatement.length() - 1)); // strip off last ;
                                currentStatement = "";
                            }
                        }
                        else {
                            statements.add(currentStatement.substring(0, currentStatement.length() - 1)); // strip off last ;
                            currentStatement = "";
                        }
                    }
                }
            }
        }
        finally {
            if (reader != null) {
                reader.close();
            }
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return statements;
    }
}
