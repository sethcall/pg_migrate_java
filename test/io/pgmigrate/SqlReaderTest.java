/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class SqlReaderTest {

    private SqlReader sqlReader;

    @BeforeTest
    public void before() {
        sqlReader = new SqlReader();
    }

    @Test
    public void loadSingleMigration() throws IOException {
        List<String> statements = sqlReader.loadMigration("test/input_manifests/single_migration/up/single1.sql");

        int line = 0;
        assertEquals(statements.size(), 7);
        assertEquals(statements.get(line++), " select 1");
        assertEquals(statements.get(line++), " select 2");
        assertEquals(statements.get(line++), " select 3");
        assertEquals(statements.get(line++), " create table emp(id BIGSERIAL PRIMARY KEY, name varchar(255))");
        assertEquals(statements.get(line++), " CREATE FUNCTION clean_emp() RETURNS void AS ' DELETE FROM emp; ' LANGUAGE SQL");
        assertEquals(statements.get(line++), " CREATE FUNCTION clean_emp2() RETURNS void AS 'DELETE FROM emp;' LANGUAGE SQL");
        assertEquals(statements.get(line++), " CREATE FUNCTION populate() RETURNS integer AS $$ DECLARE BEGIN PERFORM clean_emp2(); END; $$ LANGUAGE plpgsql");
    }
}
