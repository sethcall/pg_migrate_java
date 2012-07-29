/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import io.pgmigrate.db.PgMigrateDb;
import io.pgmigrate.packaging.Packager;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.*;

public class MigratorTest implements Constants {

    private SqlReader sqlReader;
    private ManifestReader manifestReader;
    private Builder builder;
    private Migrator migrator;
    private Packager packager;
    private TestUtil util;

    @BeforeTest
    public void before() {
        sqlReader = new SqlReader();
        manifestReader = new ManifestReader();
        builder = new Builder(manifestReader, sqlReader);
        migrator = new Migrator(manifestReader, sqlReader);
        packager = new Packager(manifestReader);
        util = new TestUtil(new Properties());
    }

    @AfterTest
    public void after() {

    }

    @BeforeMethod
    public void beforeMethod() {

    }

    public void migrateIt(final String build_path) throws Exception {

        util.connectTestDatabase(new TestUtil.ConnectionBlock() {
            @Override public void exec(final Connection connection) throws Exception {
                migrator.migrate(build_path, connection);

                PgMigrateDb dbConn = new PgMigrateDb(connection);
                List<Map<String, Object>> result = dbConn.query("SELECT table_name FROM information_schema.tables WHERE table_name = ?", new String[] {"emp"});

                assertEquals(result.size(), 1);
                assertEquals(result.get(0).get("table_name"), "emp");

                Long pgMigrationId = null;

                result = dbConn.query("SELECT * FROM pg_migrations");
                assertEquals(result.size(), 1);
                assertEquals(result.get(0).get("name"), "single1.sql");
                assertEquals(result.get(0).get("ordinal"), 0);
                pgMigrationId = (Long) result.get(0).get("pg_migrate_id");

                assertNotNull(pgMigrationId);

                // verify that a database row in pg_migrate was created as side-effect

                result = dbConn.query("SELECT * FROM pg_migrate WHERE id = ?", new Object[] {pgMigrationId});

                assertEquals(result.size(), 1);
                assertEquals(result.get(0).get("template_version"), "0.0.1");
                assertEquals(result.get(0).get("builder_version"), "pg_migrate_java-" + Version.PG_MIGRATE);
                assertEquals(result.get(0).get("migrator_version"), "pg_migrate_java-" + Version.PG_MIGRATE);
                String databaseVersion = (String) result.get(0).get("database_version");

                assertNotNull(databaseVersion);
                // totally arbitrary, but in my experience this string is 100 characters or so
                assertTrue(databaseVersion.length() > 5);
            }
        });
    }

    @Test
    public void migrateSingleMigration() throws Exception {

        File src = new File("test/input_manifests/single_migration");
        File dest = new File("target/input_single_migration");
        File buildout = new File("target/output_single_migration");
        FileUtils.forceDeleteOnExit(buildout);

        dest.mkdirs();
        TestUtil.copyDirectory(src, dest);

        builder.build(dest.getPath(), buildout.getPath(), true);

        util.createNewTestDatabase();

        migrateIt(buildout.getPath());
        migrateIt(buildout.getPath());
    }

    @Test
    public void migrateSingleMigrationViaClasspath() throws Exception {

        File src = new File("test/input_manifests/single_migration");
        File dest = new File("target/input_single_migration");
        File buildout = new File("target/output_single_migration");
        File packageout = new File("target/package_single_migration");
        FileUtils.deleteDirectory(dest);
        FileUtils.deleteDirectory(buildout);
        FileUtils.forceDeleteOnExit(buildout);

        dest.mkdirs();
        TestUtil.copyDirectory(src, dest);

        builder.build(dest.getPath(), buildout.getPath(), true);

        packager.packageJar(buildout.getPath(), packageout.getPath(), "org.corp.Migrator", "1.0", true);

        assertTrue(new File(packageout, "Migrator-1.0.jar").exists());

        // load jar
        
        util.createNewTestDatabase();

        //migrateIt(buildout.get);
        //migrateIt(buildout);
    }

}
