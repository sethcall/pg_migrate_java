/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class BuilderTest implements Constants {

    private SqlReader sqlReader;
    private ManifestReader manifestReader;
    private Builder builder;


    @BeforeTest
    public void before() {
        sqlReader = new SqlReader();
        manifestReader = new ManifestReader();
        builder = new Builder(manifestReader, sqlReader);
    }

    @Test
    public void createBootstrap() throws IOException {

        File src = new File("test/input_manifests/single_migration");
        File dest = new File("target/bootstrap_test");
        dest.mkdirs();
        TestUtil.copyDirectory(src, dest);
        builder.createBootstrapScript(dest.getPath());

        // the .sql file should exist after building
        File bootstrapFile = new File(dest, BOOTSTRAP_FILENAME);
        assertTrue(bootstrapFile.exists());

        // dynamic content should be in the file
        String content = FileUtils.readFileToString(bootstrapFile);

        assertTrue(content.startsWith("-- pg_migrate bootstrap"));
        assertTrue(content.contains("COMMIT;"));
    }
}
