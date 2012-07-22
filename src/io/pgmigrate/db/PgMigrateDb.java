/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate.db;

import java.sql.*;

public class PgMigrateDb {

    protected Connection conn;

    public PgMigrateDb() {
        this.conn = null;
    }
    public PgMigrateDb(final Connection conn) {
        this.conn = conn;
    }



    /**
     * Utility wrapper; guaranteed close of PreparedStatement
     */
    public ResultSet query(String sql, String[] parameters) throws SQLException {
        PreparedStatement statement = null;

        try {
            if (parameters != null) {
                statement = conn.prepareStatement(sql, parameters);
            }
            else {
                statement = conn.prepareStatement(sql);
            }

            return statement.executeQuery();

        }
        finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    public boolean exec(String sql) throws SQLException  {
        return exec(sql, null, false);
    }
    
    /**
     * Utility wrapper; guaranteed close of Statement.  Uses normal statement.
     */
    public boolean exec(String sql, String[] parameters) throws SQLException {
        return exec(sql, parameters, false);
    }

    /**
     * Utility wrapper; guaranteed close of Statement.  Allows caller to decide if PerparedStatement or Statement
     */
    public boolean exec(String sql, String[] parameters, boolean usePrepared) throws SQLException {
        Statement statement = null;

        try {
            if (usePrepared) {
                if (parameters != null) {
                    statement = conn.prepareStatement(sql, parameters);
                }
                else {
                    statement = conn.prepareStatement(sql);
                }
                return statement.execute(sql);
            }
            else {
                statement = conn.createStatement();
                if (parameters != null) {
                    return statement.execute(sql, parameters);
                }
                else {
                    return statement.execute(sql);
                }
            }

        }
        finally {
            if (statement != null) {
                statement.close();
            }
        }
    }
}
