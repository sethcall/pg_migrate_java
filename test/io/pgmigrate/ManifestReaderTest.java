package io.pgmigrate;/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

import io.pgmigrate.ManifestReader;
import io.pgmigrate.Migration;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.SortedSet;

public class ManifestReaderTest {

    private ManifestReader manifestReader;

    @BeforeTest
    public void before() {
        manifestReader = new ManifestReader();
    }

    @Test
    public void loadSingleManifest() throws Exception {
        final SortedSet<Migration> manifest = manifestReader.loadInputManifest("test/input_manifests/single_migration");
        assertEquals(manifest.size(), 1);
        assertEquals(manifest.iterator().next().getName(), "single1.sql");
    }

    @Test
    public void loadSingleManifestViaClasspath() throws Exception {
        final SortedSet<Migration> manifest = manifestReader.loadInputManifest("classpath://input_manifests/single_migration");
        assertEquals(manifest.size(), 1);
        assertEquals(manifest.iterator().next().getName(), "single1.sql");
    }

    @Test(expectedExceptions=IOException.class)
    public void failOnBadManifestReference() throws IOException {
        manifestReader.validateMigrationPaths("absolutely_nowhere_real", new ArrayList<Migration>() { { add(new Migration(0, "migration1", "blahpath"));} });
    }

    @Test(expectedExceptions=IOException.class)
    public void failOnBadManifestReferenceViaClasspath() throws IOException {
        manifestReader.validateMigrationPaths("classpath://absolutely_nowhere_real", new ArrayList<Migration>() { { add(new Migration(0, "migration1", "blahpath"));} });
    }
}
