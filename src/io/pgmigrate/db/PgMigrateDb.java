/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PgMigrateDb {

    protected Connection conn;

    public PgMigrateDb() {
        this.conn = null;
    }

    public PgMigrateDb(final Connection conn) {
        this.conn = conn;
    }


    public List<Map<String,Object>> query(String sql) throws SQLException {
        return query(sql, null);
    }
    /** Utility wrapper; guaranteed close of PreparedStatement */
    public List<Map<String, Object>> query(String sql, Object[] parameters) throws SQLException {
        PreparedStatement statement = null;

        try {
            statement = conn.prepareStatement(sql);

            if(parameters != null) {
                for(int i = 0; i < parameters.length; i++) {
                    statement.setObject(i + 1, parameters[i]);
                }
            }

            List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
            ResultSet rs = statement.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<String, Object>();

                for (int i = 0; i < md.getColumnCount(); i++) {
                    row.put(md.getColumnName(i + 1), rs.getObject(i + 1));
                }
                rows.add(row);
            }
            return rows;

        }
        finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    public boolean exec(String sql) throws SQLException {
        return exec(sql, null, false);
    }

    /** Utility wrapper; guaranteed close of Statement.  Uses normal statement. */
    public boolean exec(String sql, String[] parameters) throws SQLException {
        return exec(sql, parameters, false);
    }

    /** Utility wrapper; guaranteed close of Statement.  Allows caller to decide if PerparedStatement or Statement */
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
