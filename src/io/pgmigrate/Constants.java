/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

public interface Constants {

    /** name of the manifest file */
    public final String MANIFEST_FILENAME = "manifest";
    /** name of the 'forward' migration folder */
    public final String UP_DIRNAME = "up";
    /** name of the 'backwards' migration folder */
    public final String DOWN_DIRNAME = "down";
    /** name of the 'test' migration folder */
    public final String TESTDIRNAME = "test";
    /** name of the bootstrap.sql file */
    public final String BOOTSTRAP_FILENAME = "bootstrap.sql";
    /** built manifest version header */
    public final String  BUILDER_VERSION_HEADER="# pg_migrate-";

    ///// SQL CONSTANTS /////
    public final String PG_MIGRATE_TABLE = "pg_migrate";
    public final String PG_MIGRATIONS_TABLE = "pg_migrations";

}
