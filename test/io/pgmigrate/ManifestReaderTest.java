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
}
