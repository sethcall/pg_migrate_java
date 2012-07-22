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
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

public class Builder implements Constants {

    private static final Logger LOG = LoggerFactory.getLogger(Builder.class);

    private ManifestReader manifestReader;
    private SqlReader sqlReader;
    private String templateDir;

    public Builder(final ManifestReader manifestReader, final SqlReader sqlReader) {
        this.manifestReader = manifestReader;
        this.sqlReader = sqlReader;
        this.templateDir = "templates"; //relate to this file, but using Class resources
    }


    // input_dir is root path, contains file 'manifest' and 'migrations'
    // output_dir will have a manifest and migrations folder, but processed
    // force will create the output dir if needed, and *delete an existing directory* if it's in the way
    public void build(final String inputDir, final String outputDir, Boolean force) throws Exception {
        if (inputDir.equals(outputDir)) {
            throw new Exception("inputDir can not be the same as outputDir: " + inputDir);
        }

        LOG.debug("building migration directory {} and placing result at: {}", inputDir, outputDir);

        File output = new File(outputDir);

        if (!output.exists()) {
            if (force == null && !force) {
                throw new Exception("Output directory " + outputDir + " does not exist.  Create it or specify force=true");
            }
            else {
                output.mkdirs();
            }
        }
        else {
            // verify that it's a directory
            if (!output.isDirectory()) {
                throw new Exception("outputDir " + outputDir + " is a file; not a directory");
            }
            else {
                LOG.debug("deleting & recreating existing outputDir {}", outputDir);
                FileIO.delete(output);
                output.mkdir();
            }
        }


        // manifest always goes over mostly as-is,
        // just with a comment added at top indicating our version

        final String inputManifest = FileIO.combine(inputDir, MANIFEST_FILENAME);
        final String outputManifest = FileIO.combine(outputDir, MANIFEST_FILENAME);

        FileInputStream inputStream = null;
        DataInputStream dataInputStream = null;
        BufferedReader reader = null;
        FileOutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;
        BufferedWriter writer = null;
        try {
            inputStream = new FileInputStream(inputManifest);
            dataInputStream = new DataInputStream(inputStream);
            reader = new BufferedReader(new InputStreamReader(dataInputStream));
            outputStream = new FileOutputStream(outputManifest);
            dataOutputStream = new DataOutputStream(outputStream);
            writer = new BufferedWriter(new OutputStreamWriter(dataOutputStream));

            String line;

            writer.write(String.format("%spg_migrate_java-%s\n", BUILDER_VERSION_HEADER, Version.PG_MIGRATE));
            while ((line = reader.readLine()) != null) {
                writer.write(line + "\n");
            }

            // in order array of manifest declarations
            // loaded_manifest = @manifest_reader.load_input_manifest(input_dir)
            // hashed on migration name hash of manifest

            SortedSet<Migration> loadedManifest = manifestReader.loadInputManifest(inputManifest);
            manifestReader.validateMigrationPaths(inputDir, new ArrayList<Migration>(loadedManifest));

            buildUp(inputDir, outputDir, loadedManifest);
        }
        finally {
            // inputs
            if (reader != null) {
                reader.close();
            }
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }

            // outputs
            if (writer != null) {
                writer.close();
            }
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    private void recurseFiles(final String path, final String migrationsOutput, final SortedSet<Migration> loadedManifest) throws IOException {
        final File pathFile = new File(path);

        for (String nestedPath : pathFile.list()) {
            LOG.debug("building {}", path);

            // create relative bit

            final String relativePath = nestedPath;

            // create the filename correct for the input directory, for this file
            final String migrationInPath = nestedPath;

            // create the filename correct for the output directory, for this file
            final String migrationOutPath = FileIO.combine(migrationsOutput, relativePath);

            Map<String, Migration> migrationLookup = new HashMap<String, Migration>();

            for(Migration migration : loadedManifest) {
                migrationLookup.put(migration.getName(), migration);
            }

            processAndCopyUp(migrationInPath, migrationOutPath, relativePath, migrationLookup);

            recurseFiles(nestedPath, migrationsOutput, loadedManifest);
        }
    }

    private void createWrappedUpMigration(final String migrationInFilepath, final String migrationOutputFilepath, final Migration migration) throws IOException {
             final String builderVersion ="pg_migrate_java-" + Version.PG_MIGRATE;
        final String migrationContent = FileIO.readAll(new FileInputStream(migrationInFilepath));

        final Map<String, Object> binding = new HashMap<String, Object>();
        binding.put("build_version", builderVersion);
        binding.put("migration_content", migrationContent);
        binding.put("migration_def.name", migration.getName());
        binding.put("migration_def.ordinal", migration.getOrdinal());

        runTemplate("up.erb", binding, migrationOutputFilepath);   
    }
    private void processAndCopyUp(final String migrationInPath, final String migrationsOutput, final String relativePath, final Map<String, Migration> loadedManifest) throws IOException {

        File migrationInFile = new File(migrationInPath);
        File migrationOutpath = new File(migrationsOutput);

        if(migrationInFile.isDirectory()) {
            migrationOutpath.mkdirs();
        }
        else {
            if(relativePath.endsWith(".sql")) {
               // if a .sql file, then copy & process

               // create the the 'key' version of this name, which is basically the filepath
               // of the .sql file relative without the leading '/' directory

               String migrationName = relativePath.substring(1);

                LOG.debug("retrieving manifest definition for {}", migrationName);

                final Migration migration = loadedManifest.get(migrationName);

                createWrappedUpMigration(migrationInPath, migrationOutpath.getPath(), migration);
            }
        }
    }

    private void createBootstrapScript(final String migrationsOutput) throws IOException {
        runTemplate("bootstrap.erb", new HashMap<String, Object>(), FileIO.combine(migrationsOutput, BOOTSTRAP_FILENAME));
    }


    // given an input template and binding, writes to an output file
    // process template
    // strings to support:
    // migration_content = original content
    // migration_def.name = migration name
    // migration_def.ordinal = migration ordinal
    // builder_version = builder version
    private void runTemplate(final String template, Map<String, Object> binding, final String outputFilePath) throws IOException {
        String bootstrapTemplate = FileIO.readAll(ClassLoader.getSystemClassLoader().getResourceAsStream(templateDir + "/" + template));

        FileOutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;
        BufferedWriter writer = null;
        try {
            outputStream = new FileOutputStream(outputFilePath);
            dataOutputStream = new DataOutputStream(outputStream);
            writer = new BufferedWriter(new OutputStreamWriter(dataOutputStream));

            for (Map.Entry<String, Object> entry : binding.entrySet()) {
                bootstrapTemplate = bootstrapTemplate.replaceAll("<%= " + entry.getKey() + " %>", entry.getValue().toString());
            }

            writer.write(bootstrapTemplate);
        }
        finally {

            // outputs
            if (writer != null) {
                writer.close();
            }
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }


    private void buildUp(final String inputDir, final String outputDir, final SortedSet<Migration> loadedManifest) throws IOException {


        final String migrationsInput = FileIO.combine(inputDir, UP_DIRNAME);
        final String migrationsOutput = FileIO.combine(outputDir, UP_DIRNAME);

        // iterate through files in input migrations path, wrapping files with transactions and other required bits
        recurseFiles(migrationsInput, migrationsOutput, loadedManifest);

        // create static bootstrap file
        createBootstrapScript(migrationsOutput);
    }

}
