/*
 * Copyright 2009 LugIron Software, Inc. All Rights Reserved.
 *
 * $Id$
 */

package io.pgmigrate;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import io.pgmigrate.packaging.Packager;
import io.pgmigrate.packaging.utils.ClassLoaderResolver;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class CommandLine implements Constants {


    public static String PACKAGED_SOURCE = ".";

    public static void main(String[] args) throws SQLException, ParseException {

        if(args.length < 1) {
            usage(true);
        }

        String command = args[0];

        String[] restOfArgs = null;
        if(args.length > 1) {
            restOfArgs = Arrays.copyOfRange(args, 1, args.length);
        }
        else {
            restOfArgs = new String[]{};
        }
        if(command == "help") {
            usage(false);
        }
        else if(command == "up") {
            up(restOfArgs);
        }
        else if(command == "down") {
            throw new NotImplementedException();
        }
        else if(command.equals("build")) {
            build(restOfArgs);
        }
        else if(command.equals("package")) {
            packageIt(restOfArgs);
        }
        else {
            usage(true);
        }

    }

    public static void usage(boolean error) {

        System.out.println("Tasks:\n" +
               "  pg_migrate build                                 # processes a pg_migrate source directory and places the result i...\n" +
               "  pg_migrate down                                  # not implemented\n" +
               "  pg_migrate help [TASK]                           # Describe available tasks or one specific task\n" +
               "  pg_migrate up -c, --connopts=jdbc_url            # migrates the database forwards, applying migrations found in th...\n" +
               "  pg_migrate package                               # packages a built pg_migrate project into a jar");

        System.exit(error ? 1 : 0);
    }

    public static void up(String[] args) throws ParseException, SQLException {

        CommandLineParser upParser = new PosixParser();
        Options options = new Options();
        options.addOption("s", "source", true, "a pg_migrate built manifest. Should contain your processed manifest and up|down|test folders");
        options.addOption("c", "connopts", true, "database connection options used by gem 'pg': dbname|host|hostaddr|port|user|password|connection_timeout|options|sslmode|krbsrvname|gsslib|service");
        options.addOption("v", "verbose", false, "set to raise verbosity");

        options.getOption("s").setRequired(false);
        options.getOption("c").setRequired(false);
        options.getOption("v").setRequired(false);

        org.apache.commons.cli.CommandLine cli = upParser.parse(options, args);

        String source = cli.getOptionValue("s");
        if (source == null || source.isEmpty()) {
            source = PACKAGED_SOURCE;
        }

        String connopts = cli.getOptionValue("c", null);
        Boolean verbose = cli.hasOption("v") ? true : null;

        Properties properties = loadPgMigrate(source, "up");

        if (connopts == null) {
            connopts = properties.getProperty("connopts", null);
        }

        if(connopts == null) {
            System.err.println("-c, --connopts must be specified");
            System.exit(1);
        }

        if(verbose == null) {
            verbose = Boolean.parseBoolean(properties.getProperty("verbose", "false"));
        }

        configureLogging(verbose);

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(connopts);
        }
        catch (SQLException e) {
            System.err.println("unable to acquire connection to database");
            e.printStackTrace();
            System.exit(1);
        }

        Migrator migrator = new Migrator(new ManifestReader(), new SqlReader());

        try {
            migrator.migrate(source, connection);
        }
        catch (Exception e) {
            System.err.println("error in migrating database");
            e.printStackTrace();
            System.exit(1);
        }
        finally {
            connection.close();
        }

        System.exit(0);
    }

    public static void build(String[] args) throws ParseException {

        CommandLineParser buildParser = new PosixParser();
        Options options = new Options();
        options.addOption("s", "source", true, "the input directory containing a manifest file and up|down|test folders");
        options.addOption("o", "out", true, "where the processed migrations will be placed");
        options.addOption("f", "force", false, "if specified, the out directory will be created before processing occurs, replacing any existing directory");
        options.addOption("v", "verbose", false, "set to raise verbosity");

        options.getOption("s").setRequired(false);
        options.getOption("o").setRequired(false);
        options.getOption("f").setRequired(false);
        options.getOption("v").setRequired(false);

        org.apache.commons.cli.CommandLine cli = buildParser.parse(options, args);

        String source = cli.getOptionValue("s");
        if (source == null || source.isEmpty()) {
            source = PACKAGED_SOURCE;
        }

        String out = cli.getOptionValue("o", null);
        Boolean force = cli.hasOption("f") ? false : null;
        Boolean verbose = cli.hasOption("v") ? true : null;

        Properties properties = loadPgMigrate(source, "up");

        if (out == null) {
            out = properties.getProperty("out", null);
        }

        if(out == null || out.isEmpty()) {
            System.err.println("-o, --out must be specified");
            System.exit(1);
        }

        if(force == null) {
            force = Boolean.parseBoolean(properties.getProperty("force", "false"));
        }

        if(verbose == null) {
            verbose = Boolean.parseBoolean(properties.getProperty("verbose", "false"));
        }

        configureLogging(verbose);

        Builder builder = new Builder(new ManifestReader(), new SqlReader());

        try {
            builder.build(source, out, force);
        }
        catch (Exception e) {
            System.err.println("error in building migrations");
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    public static void packageIt(String[] args) throws ParseException {

        CommandLineParser packageParser = new PosixParser();
        Options options = new Options();
        options.addOption("s", "source", true, "the input directory containing a manifest file and up|down|test folders that has been previously built by pg_migrate build");
        options.addOption("o", "out", true, "where the jar will be placed (as well as the exploded jar's src and built classes)");
        options.addOption("n", "name", true, "the name of the schema jar.  Use fully-qualified name of custom migration class: com.corp.MyMigrator");
        options.addOption("e", "version", true, "the version of the schema jar, i.e., 1.0");
        options.addOption("f", "force", false, "if specified, the out directory will be created before processing occurs, replacing any existing directory");
        options.addOption("v", "verbose", false, "set to raise verbosity");

        options.getOption("s").setRequired(false);
        options.getOption("o").setRequired(false);
        options.getOption("n").setRequired(false);
        options.getOption("e").setRequired(false);
        options.getOption("f").setRequired(false);
        options.getOption("v").setRequired(false);

        org.apache.commons.cli.CommandLine cli = packageParser.parse(options, args);

        String source = cli.getOptionValue("s");
        if (source == null || source.isEmpty()) {
            source = PACKAGED_SOURCE;
        }

        String out = cli.getOptionValue("o", null);
        String name = cli.getOptionValue("n", null);
        String version = cli.getOptionValue("v", null);
        Boolean force = cli.hasOption("f") ? false : null;
        Boolean verbose = cli.hasOption("v") ? true : null;

        Properties properties = loadPgMigrate(source, "up");

        if (out == null || out.isEmpty()) {
            out = properties.getProperty("out", null);
        }

        if(out == null) {
            System.err.println("-o, --out must be specified");
            System.exit(1);
        }

        if(name == null) {
            name = properties.getProperty("name", null);
        }

        if(name == null || name.isEmpty()) {
            System.err.println("-n, --name must be specified");
            System.exit(1);
        }

        if(version == null) {
            version = properties.getProperty("version", null);
        }

        if(version == null || version.isEmpty()) {
            System.err.println("-e, --version must be specified");
            System.exit(1);
        }


        if(force == null) {
            force = Boolean.parseBoolean(properties.getProperty("force", "false"));
        }

        if(verbose == null) {
            verbose = Boolean.parseBoolean(properties.getProperty("verbose", "false"));
        }

        configureLogging(verbose);

        Packager packager = new Packager(new ManifestReader());

        try {
            packager.packageJar(source, out, name, version, force);
        }
        catch (Exception e) {
            System.err.println("error in packaging migrations in to jar");
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    private static void configureLogging(final Boolean verbose) {
        // assume SLF4J is bound to logback in the current environment
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
          JoranConfigurator configurator = new JoranConfigurator();
          configurator.setContext(context);
          // Call context.reset() to clear any previous configuration, e.g. default
          // configuration. For multi-step configuration, omit calling context.reset().
          context.reset();
          configurator.doConfigure(ClassLoaderResolver.getClassLoader().getResourceAsStream("io/pgmigrate/logback-" + (verbose ? "verbose" : "info") + ".xml"));
        } catch (JoranException je) {
          // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    private static Properties loadPgMigrate(String source, String context) {
        try {

            Properties properties = new Properties();

            File pgConfig = new File(source, PG_CONFIG);
            if (pgConfig.exists()) {
                properties.load(new FileInputStream(pgConfig));
            }

            Properties modified = new Properties();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                modified.setProperty((String) entry.getKey(), (String) entry.getValue());

                String prefix = context + ".";
                if (((String) entry.getKey()).startsWith(prefix)) {
                    modified.setProperty(((String) entry.getKey()).substring(prefix.length()), (String) entry.getValue());
                }
            }

            return modified;
        }
        catch (IOException e) {
            System.err.println("can not load .pg_migrate file");
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }


}
