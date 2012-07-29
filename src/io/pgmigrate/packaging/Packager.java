/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate.packaging;

import io.pgmigrate.FileIO;
import io.pgmigrate.ManifestReader;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class Packager {

    final ManifestReader manifestReader;
    final String templateDir;
    final VelocityEngine velocity;

    PackageContext context;

    private static final Logger LOG = LoggerFactory.getLogger(Packager.class);

    public Packager(final ManifestReader manifestReader) {
        this.manifestReader = manifestReader;
        this.templateDir = "io/pgmigrate/packaging/templates";

        this.velocity = new VelocityEngine();

        Properties properties = new Properties();
        properties.setProperty("resource.loader", "class");
        properties.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocity.init(properties);
    }

    public void packageJar(String builtMigrationPath, String outputDir, String name, String version, Boolean force) throws Exception {

        this.context = new PackageContext(builtMigrationPath, outputDir, name, version);

        createCode(force);

        compileCode();

        copySchema();

        packageCode();
    }

    void createCode(Boolean force) throws Exception {

        LOG.debug("generating source in {}", this.context.getSrcDir());

        prepareOutput(force);

        File srcDir = this.context.getSrcDir();

        Template migrator = velocity.getTemplate(templateDir + "/Migrator.java.vm");

        File migratorFile = new File(srcDir, this.context.getClassName() + ".java");

        VelocityContext velocityContext = new VelocityContext(this.context.getMap());
        FileWriter writer = new FileWriter(migratorFile);
        migrator.merge(velocityContext, writer);
        writer.close();

        context.addJavaFile(migratorFile);
    }

    private void copySchema() throws IOException {
        LOG.debug("copying schema to built classes directory");

        FileUtils.copyDirectory(this.context.getInputDir(), this.context.getSchemasDir());
    }

    void prepareOutput(final Boolean force) throws Exception {
        LOG.debug("preparting output directories");

        File output = context.getPackageDir();

        if (!output.exists()) {
            if (force == null && !force) {
                throw new Exception("Output directory " + output + " does not exist.  Create it or specify force=true");
            }
            else {
                output.mkdirs();
            }
        }
        else {
            // verify that it's a directory
            if (!output.isDirectory()) {
                throw new Exception("outputDir " + output + " is a file; not a directory");
            }
            else {
                LOG.debug("deleting & recreating existing outputDir {}", output);
                FileIO.delete(output);
                output.mkdir();
            }
        }


        context.getSrcDir().mkdirs();
        context.getFqClassDir().mkdirs();
        context.getSchemasDir().mkdir();
    }

    void compileCode() throws IOException {

        LOG.debug("compiling generated source and placing in {}", this.context.getFqClassDir());

        StandardJavaFileManager fileManager = null;

        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            fileManager = compiler.getStandardFileManager(null, Locale.getDefault(), null);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

            java.lang.Iterable<? extends javax.tools.JavaFileObject> src = fileManager.getJavaFileObjects(this.context.getGeneratedJavaFiles().toArray(new File[]{}));

            String[] compilerOptions = new String[]{"-d", context.getClassDir().getPath(), "-g"};
            List<String> compilerOptionsIterator = Arrays.asList(compilerOptions);

            JavaCompiler.CompilationTask compilerTask = compiler.getTask(null, fileManager, diagnostics, compilerOptionsIterator, null, src);

            boolean result = compilerTask.call();

            if (!result) {
                for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                    LOG.error("Error {}, on line {}, in {}", new Object[]{diagnostic.getMessage(Locale.getDefault()), diagnostic.getLineNumber(), diagnostic});
                }

                throw new IOException("unable to compile code");
            }
        }
        finally {
            if (fileManager != null) {
                fileManager.close();
            }
        }
    }

    private void packageCode() throws IOException {

        LOG.debug("packaging to jar and placing in {}", this.context.getPackageDir());

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, this.context.getName());
        JarOutputStream output = new JarOutputStream(new FileOutputStream(this.context.getJar()), manifest);

        for(File toJar : this.context.getClassDir().listFiles()) {
            addJarEntry(this.context.getClassDir().getPath(), toJar, output);
        }
    }

    private void addJarEntry(String prefix, File source, JarOutputStream outputStream) throws IOException {
        BufferedInputStream in = null;

        try {
            if (source.isDirectory()) {
                String name = source.getPath().replace("\\", "/");
                if (!name.isEmpty()) {
                    if (!name.endsWith("/")) {
                        name += "/";
                    }
                    JarEntry entry = new JarEntry(name.substring(prefix.length() + 1));
                    entry.setTime(source.lastModified());
                    outputStream.putNextEntry(entry);
                    outputStream.closeEntry();
                }

                for (File nestedFile : source.listFiles()) {
                    addJarEntry(prefix, nestedFile, outputStream);
                }
            }
            else {
                JarEntry entry = new JarEntry(source.getPath().replace("\\", "/").substring(prefix.length() + 1));
                entry.setTime(source.lastModified());
                outputStream.putNextEntry(entry);
                in = new BufferedInputStream(new FileInputStream(source));

                byte[] buffer = new byte[2048];
                while(true) {
                    int count = in.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    outputStream.write(buffer, 0, count);
                }

                outputStream.closeEntry();
            }



        }
        finally {
            if (in != null) {
                in.close();
            }
        }
    }

}
