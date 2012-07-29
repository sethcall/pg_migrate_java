/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate.packaging;

import io.pgmigrate.*;
import io.pgmigrate.db.PgMigrateDb;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static org.testng.Assert.*;

public class PackageTest implements Constants {

    private SqlReader sqlReader;
    private ManifestReader manifestReader;
    private Builder builder;
    private Migrator migrator;
    private Packager packager;
    private TestUtil util;

    @BeforeMethod
    public void before() {
        sqlReader = new SqlReader();
        manifestReader = new ManifestReader();
        builder = new Builder(manifestReader, sqlReader);
        migrator = new Migrator(manifestReader, sqlReader);
        packager = new Packager(manifestReader);
        util = new TestUtil(new Properties());
    }

    @Test
    public void compileCode() throws Exception {
        this.packager.context = new PackageContext("target/input", "target/output", "com.corp.Migrator", "1.0");

        this.packager.createCode(true);
    }


    public void migrateIt(final PackagedMigrator migrator) throws Exception {
        util.connectTestDatabase(new SingleManifestMigration(migrator));
    }

    @Test
    public void packageSingleManifest() throws Exception {

        File src = new File("test/input_manifests/single_migration");
        File dest = new File("target/input_single_migration");
        File buildout = new File("target/output_single_migration");
        File packageout = new File("target/package_single_migration");

        FileUtils.deleteDirectory(dest);
        FileUtils.deleteDirectory(buildout);
        FileUtils.deleteDirectory(packageout);

        dest.mkdirs();
        TestUtil.copyDirectory(src, dest);

        builder.build(dest.getPath(), buildout.getPath(), true);

        packager.packageJar(buildout.getPath(), packageout.getPath(), "com.corp.Migrator", "1.0", true);

        File jar = new File(packageout, "Migrator-1.0.jar");

        assertTrue(jar.exists());

        JarInputStream input = null;

        try {
            input = new JarInputStream(new FileInputStream(jar));

            Manifest manifest = input.getManifest();
            Attributes attributes = manifest.getMainAttributes();

            assertEquals(attributes.getValue(Attributes.Name.MANIFEST_VERSION), "1.0");
            assertEquals(attributes.getValue(Attributes.Name.MAIN_CLASS), "com.corp.Migrator");

            Map<String, JarEntry> entries = new HashMap<String, JarEntry>();

            while (true) {
                JarEntry entry = input.getNextJarEntry();
                if (entry == null) {
                    break;
                }
                entries.put(entry.getName(), entry);
            }

            assertTrue(entries.containsKey("com/corp/Migrator.class"));
            assertTrue(entries.containsKey("com/corp/schemas/"));
            assertTrue(entries.containsKey("com/corp/schemas/manifest"));
            assertTrue(entries.containsKey("com/corp/schemas/up/"));
            assertTrue(entries.containsKey("com/corp/schemas/up/single1.sql"));
            assertTrue(entries.containsKey("com/corp/schemas/up/bootstrap.sql"));
        }
        finally {
            if (input != null) {
                input.close();
            }
        }
        util.createNewTestDatabase();

        // TODO: find way to dynamically load jar into test context.
        // Tried a number of ways - jcl-core, ClassPathHack, others; couldn't get anything to load both classes and resources
    }

    class SingleManifestMigration extends TestUtil.ConnectionBlock {

        private PackagedMigrator migrator;

        public SingleManifestMigration (PackagedMigrator migrator) {
            this.migrator = migrator;
        }

        @Override public void exec(final Connection connection) throws Exception {
            this.migrator.migrate(connection);

            PgMigrateDb dbConn = new PgMigrateDb(connection);
            List<Map<String, Object>> result = dbConn.query("SELECT table_name FROM information_schema.tables WHERE table_name = ?", new String[]{"emp"});

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

            result = dbConn.query("SELECT * FROM pg_migrate WHERE id = ?", new Object[]{pgMigrationId});

            assertEquals(result.size(), 1);
            assertEquals(result.get(0).get("template_version"), "0.0.1");
            assertEquals(result.get(0).get("builder_version"), "pg_migrate_java-" + Version.PG_MIGRATE);
            assertEquals(result.get(0).get("migrator_version"), "pg_migrate_java-" + Version.PG_MIGRATE);
            String databaseVersion = (String) result.get(0).get("database_version");

            assertNotNull(databaseVersion);
            // totally arbitrary, but in my experience this string is 100 characters or so
            assertTrue(databaseVersion.length() > 5);
        }
    }

    ;


}
