/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate.packaging;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.net.URI;

class CompiledJavaCodeObject extends SimpleJavaFileObject {
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    CompiledJavaCodeObject(String name, Kind kind) {
        super(URI.create("memo:///" + name.replace('.', '/') + kind.extension), kind);
    }

    byte[] toByteArray() {
        return this.baos.toByteArray();
    }

    @Override
    public ByteArrayOutputStream openOutputStream() {
        return this.baos;
    }
}
