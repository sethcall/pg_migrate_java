/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class ManifestReader implements Constants {

    private static final Logger LOG = LoggerFactory.getLogger(ManifestReader.class);

    public SortedSet<Migration> loadInputManifest(final String manifestPath) throws Exception {
        return loadManifest(manifestPath, false);
    }

    public SortedSet<Migration> loadOutputManifest(final String manifestPath) throws Exception {
        return loadManifest(manifestPath, true);
    }

    /** Verify that the migration files exists referenced by the manifest */
    public void validateMigrationPaths(final String manifestPath, final List<Migration> loadedManifest) throws IOException {
        // each item in the manifest should be a valid file

        for (Migration migration : loadedManifest) {
            final String migrationPath = buildMigrationPath(manifestPath, migration.getName());
            if (!FileIO.exists(migrationPath)) {
                throw new IOException(String.format("manifest reference %s does not exist at path %s", migration.getName(), migrationPath));
            }
        }
    }

    /** construct a migration file path location based on the manifest basedir and the name of the migration */
    public String buildMigrationPath(final String manifestPath, final String migrationName) {
        return FileIO.combine(FileIO.combine(manifestPath, UP_DIRNAME), migrationName);
    }

    private SortedSet<Migration> loadManifest(final String manifestPath, boolean isOutput) throws Exception {
        final SortedSet<Migration> manifest = new TreeSet<Migration>();
        String version = null;

        final String manifestFilePath = FileIO.combine(manifestPath, MANIFEST_FILENAME);

        LOG.debug("loading manifest from {}", manifestFilePath);

        // there should be a file called 'manifest' at this location
        if (!FileIO.exists(manifestFilePath)) {
            throw new IOException(String.format("ManifestReader: code=unloadable_manifest: manifest not found at %s", manifestFilePath));
        }

        InputStream inputStream = null;
        DataInputStream dataInputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = FileIO.getInputStream(manifestFilePath);
            dataInputStream = new DataInputStream(inputStream);
            reader = new BufferedReader(new InputStreamReader(dataInputStream));
            String line;

            int ordinal = 0;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                final String migrationName = line.trim();

                LOG.debug("processing line:{} {}", index, line);

                // output files must have a version header as 1st line o file
                if (isOutput) {
                    if (index == 0) {
                        //  the first line must be the version comment. if not, error out.
                        if (migrationName.indexOf(BUILDER_VERSION_HEADER) == 0 && migrationName.length() > BUILDER_VERSION_HEADER.length()) {
                            version = migrationName.substring(BUILDER_VERSION_HEADER.length());
                            LOG.debug("manifest has builder_version {}", version);
                        }
                        else {
                            throw new Exception(String.format("manifest invalid: missing/malformed version.  expecting '# pg_migrate-VERSION' to begin first line '%s' of manifest file: '%s'", line, manifestFilePath));
                        }
                    }
                }


                // ignore comments
                if (migrationName.length() == 0 || migrationName.startsWith("#")) {
                    // ignored !
                }
                else {
                    LOG.debug("adding migraton {} with ordinal {}", migrationName, ordinal);
                    manifest.add(new Migration(ordinal, migrationName, buildMigrationPath(manifestPath, migrationName)));
                    ordinal += 1;
                }

                if (isOutput) {
                    if (version == null) {
                        throw new Exception("manifest invalid: empty");
                    }
                }

                index++;
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


        return manifest;
    }


}
