/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate.packaging;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;

/**
 * Creates a dynamic source code file object
 *
 * This is an example of how we can prepare a dynamic java source code for compilation.
 * This class reads the java code from a string and prepares a JavaFileObject
 *
 */
class StringJavaCodeObject extends SimpleJavaFileObject {
    private String qualifiedName ;
    private String sourceCode ;

    /**
     * Converts the name to an URI, as that is the format expected by JavaFileObject
     *
     *
     * @param name fully qualified name given to the class file
     * @param code the source code string
     */
    protected StringJavaCodeObject(String name, String code) {
        super(URI.create("string:///" +name.replaceAll("\\.", "/") + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
        this.qualifiedName = name ;
        this.sourceCode = code ;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors)
            throws IOException {
        return sourceCode ;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }
}