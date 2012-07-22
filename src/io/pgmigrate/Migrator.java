/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import io.pgmigrate.db.PgMigrateDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

public class Migrator extends PgMigrateDb implements Constants {

    private static final Logger LOG = LoggerFactory.getLogger(Migrator.class);

    private ManifestReader manifestReader;
    private SqlReader sqlReader;
    private String manifestPath;
    private SortedSet<Migration> manifest;

    public Migrator(final ManifestReader manifestReader, final SqlReader sqlReader) {
        this.manifestReader = manifestReader;
        this.sqlReader = sqlReader;
    }

    /**
     * Given a base path to a manifest and up/down/test directories, attempt to migrate up.
     * @param manifestPath directory path to manifest and up/down/test directories
     * @param conn an already open connection.  Autocommit should be off.
     * @throws SQLException thrown if invalid SQL is contained in the migration file.
     */
    public void migrate(final String manifestPath, final Connection conn) throws Exception {
        validateArgumentsForMigrate(manifestPath, conn);

        this.manifestPath = manifestPath;
        this.conn = conn;

        // this is used to record the version of the 'migrator' in the pg_migrate table
        exec("SET application_name = 'pg_migrate_java-" + Version.PG_MIGRATE + "'");

        processManifest();

        runMigrations();
    }

    // load the manifest's migration declarations, and validate that each migration points to a real file
    public void processManifest() throws Exception {
        manifest = manifestReader.loadOutputManifest(manifestPath);
        manifestReader.validateMigrationPaths(manifestPath, new ArrayList<Migration>(manifest));
    }

    // run all necessary migrations
    void runMigrations() throws Exception {

        // run bootstrap template before user migrations to prepare database
        runBootstrap();

        //loop through the manifest, executing migrations in turn
        for (Migration migration : manifest) {
            executeMigration(migration.getName(), migration.getFilepath());
        }

    }

    // executes the bootstrap template
    void runBootstrap() throws Exception {
        String bootstrap = FileIO.combine(manifestPath, UP_DIRNAME, BOOTSTRAP_FILENAME);
        executeMigration("bootstrap.sql", bootstrap);
    }

    // execute a single migration by loading it's statements from file, and then executing each
    void executeMigration(final String name, final String filepath) throws Exception {
        LOG.debug("executing migration {}", filepath);

        List<String> statements = sqlReader.loadMigration(filepath);
        if (statements.size() == 0) {
            throw new Exception("no statements found in migration " + filepath);
        }
        runMigration(name, statements);
    }

    // execute all the statements of a single migration
    void runMigration(String name, List<String> statements) throws SQLException {

        try {
            for (String statement : statements) {
                exec(statement);
            }
        }
        catch (SQLException e) {
            // we make a special allowance for one exception; the 'migration_exists' exception.
            // This exception means this migration
            // has already occurred, and we should just treat it like a continue
            if (e.getMessage().indexOf("pg_migrate: code=migration_exists") < 0) {
                conn.rollback();
                throw e;
            }
            else {
                conn.rollback();
                LOG.info("migration {} already run", name);
            }
        }
    }

    /** Validate arguments for migrate */
    private void validateArgumentsForMigrate(final String manifestPath, final Connection conn) throws SQLException {
        if (manifestPath == null) {
            throw new IllegalArgumentException("manifestPath can not be null");
        }
        File manifestPathFile = new File(manifestPath);
        if (!manifestPathFile.exists()) {
            throw new IllegalArgumentException("manifestPath must exist: " + manifestPath);
        }
        if (!manifestPathFile.isDirectory()) {
            throw new IllegalArgumentException("manifestPath must be a directory: " + manifestPath);
        }

        if (conn == null) {
            throw new IllegalArgumentException("connection can't be null");
        }
        if (conn.isClosed()) {
            throw new IllegalArgumentException("connection can not be closed");
        }
        if (conn.getAutoCommit()) {
            throw new IllegalArgumentException("connection must have autocommit off");
        }
    }


}
