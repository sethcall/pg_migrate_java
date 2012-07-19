/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import java.sql.Timestamp;

public class Migration implements Comparable<Migration> {

    private String name;
    private Integer ordinal;
    private Timestamp created;
    private Boolean production;
    private String filepath;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(final Integer ordinal) {
        this.ordinal = ordinal;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(final Timestamp created) {
        this.created = created;
    }

    public Boolean getProduction() {
        return production;
    }

    public void setProduction(final Boolean production) {
        this.production = production;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(final String filepath) {
        this.filepath = filepath;
    }

    public Migration(final Integer ordinal, final String name, final String filepath) {
        this.ordinal = ordinal;
        this.name = name;
        this.filepath = filepath;
    }

    @Override public int compareTo(final Migration migration) {
        return this.ordinal.compareTo(migration.getOrdinal());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Migration migration = (Migration) o;

        if (name != null ? !name.equals(migration.name) : migration.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
