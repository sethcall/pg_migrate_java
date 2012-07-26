/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import io.pgmigrate.db.PgMigrateDb;
import org.apache.commons.io.FileUtils;
import org.postgresql.ds.PGConnectionPoolDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class TestUtil {

    public static String DEFAULT_TEST_DB_NAME = "pg_migrate_java_test";

    private PGConnectionPoolDataSource superDbPool;
    private PGConnectionPoolDataSource testDbPool;

    private Properties properties;

    public TestUtil(Properties properties) {
        this.properties = properties;

        superDbPool = new PGConnectionPoolDataSource();
        testDbPool = new PGConnectionPoolDataSource();

        configurePool(properties, superDbPool);
        // most db installs have user/pass/dbname of postgres;
        // the more correct thing to do is add a way to configure
        // tests locally for the developer.
        superDbPool.setDatabaseName("postgres");

        configurePool(properties, testDbPool);
    }

    private void configurePool(Properties properties, PGConnectionPoolDataSource ds) {
        ds.setDatabaseName(properties.getProperty("dbname", DEFAULT_TEST_DB_NAME));
        ds.setPassword(properties.getProperty("dbpassword", "postgres"));
        ds.setUser(properties.getProperty("dbuser", "postgres"));
        ds.setPortNumber(new Integer(properties.getProperty("dbport", "5432")));
        ds.setServerName(properties.getProperty("dbhost", "localhost"));
        ds.setDefaultAutoCommit(false);
    }

    public void connectTestDatabase(ConnectionBlock block) throws Exception {

        Connection connection = null;
        try {

            connection = testDbPool.getConnection();
            connection.setAutoCommit(false);
            block.exec(connection);
        }
        finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    public void createNewTestDatabase() throws SQLException {
        Connection connection = null;
        try {

            connection = superDbPool.getConnection();

            PgMigrateDb dbConn = new PgMigrateDb(connection);
            dbConn.exec("DROP DATABASE IF EXISTS " + properties.getProperty("dbname", DEFAULT_TEST_DB_NAME));
            dbConn.exec("CREATE DATABASE " + properties.getProperty("dbname", DEFAULT_TEST_DB_NAME));
        }
        finally {
            if (connection != null) {
                connection.close();
            }
        }
    }


    /** Recursive copy of directory, with deletion on exit of destDir */
    public static void copyDirectory(File srcDir, File destDir) throws IOException {
        FileUtils.copyDirectory(srcDir, destDir);
        FileUtils.forceDeleteOnExit(destDir);
    }


    public abstract static class ConnectionBlock {

        public ConnectionBlock() {

        }


        abstract void exec(Connection connection) throws Exception;


    }
}
