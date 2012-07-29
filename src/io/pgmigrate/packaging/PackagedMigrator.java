/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate.packaging;

import java.sql.Connection;

public interface PackagedMigrator {

    public void migrate(Connection connection) throws Exception;
}
