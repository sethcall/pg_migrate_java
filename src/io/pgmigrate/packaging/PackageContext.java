/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate.packaging;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageContext {

    private String packageName;
    private String className;
    private String version;

    /** The value passed in; this ends p as the Main-Class of the jar */
    private String name;

    /** Base directory of built schemas */
    private File inputDir;

    /** Output directory of generated java source code */
    private File srcDir;

    /** Output directory of built java source code and resources */
    private File classDir;

    /** Output directory of the Migrator class java source code */
    private File fqClassDir;

    /** Where the schemas go.  This in within the fqClassDir */
    private File schemasDir;

    /** Where the jar file goes */
    private File packageDir;

    /** Output jar file.  Inside packageDir */
    private File jar;

    private List<File> generatedJavaFiles;


    public PackageContext(String builtMigrationPath, String outputDir, String name, String version) {

        generatedJavaFiles = new ArrayList<File>();

        this.name = name;

        inputDir = new File(builtMigrationPath);

        int lastDot = name.lastIndexOf('.');
        if (lastDot > -1) {
            packageName = name.substring(0, lastDot);
            className = name.substring(lastDot + 1);
        }
        else {
            packageName = null;
            className = name;
        }

        srcDir = new File(outputDir, "src");
        if (packageName != null) {
            srcDir = new File(srcDir, packageName.replace(".", File.separator));
        }
        classDir = new File(outputDir, "classes");
        if (packageName != null) {
            fqClassDir = new File(classDir, packageName.replace(".", File.separator));
        }

        schemasDir = new File(fqClassDir, "schemas");
        packageDir = new File(outputDir);

        this.version = version;

        this.jar = new File(packageDir, className + "-" + version + ".jar");
    }


    public File getJar() {
        return this.jar;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getVersion() {
        return version;
    }

    public File getSrcDir() {
        return srcDir;
    }

    public File getFqClassDir() {
        return fqClassDir;
    }

    public File getPackageDir() {
        return packageDir;
    }

    public File getInputDir() {
        return inputDir;
    }

    public File getSchemasDir() {
        return schemasDir;
    }

    public List<File> getGeneratedJavaFiles() {
        return generatedJavaFiles;
    }

    public File getClassDir() {
        return classDir;
    }

    public String getName() {
        return name;
    }

    /**
     * Used as context within velocity
     * @return map of name/values
     */
    public Map<String, Serializable> getMap() {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("inputdir", inputDir);
        map.put("packagedir", packageDir);
        map.put("classdir", classDir);
        map.put("fqclassdir", fqClassDir);
        map.put("schemasDir", schemasDir);
        map.put("srcdir", srcDir);
        map.put("classname", className);
        map.put("packagename", packageName);
        map.put("packagenamepath", packageName.replace(".", "/"));
        map.put("version", version);
        return map;
    }

    public void addJavaFile(final File migratorFile) {
        this.generatedJavaFiles.add(migratorFile);
    }

}
