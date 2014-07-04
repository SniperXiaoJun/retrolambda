// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.Opcodes;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Config {

    private static final String PREFIX = "retrolambda.";
    private static final String BYTECODE_VERSION = PREFIX + "bytecodeVersion";
    private static final String INPUT_DIR = PREFIX + "inputDir";
    private static final String OUTPUT_DIR = PREFIX + "outputDir";
    private static final String CLASSPATH = PREFIX + "classpath";
    private static final String CHANGED = PREFIX + "changed";

    private static final List<String> requiredProperties = new ArrayList<>();
    private static final List<String> requiredProperitesHelp = new ArrayList<>();
    private static final List<String> optionalPropertiesHelp = new ArrayList<>();
    private static final Map<Integer, String> bytecodeVersionNames = new HashMap<>();

    static {
        bytecodeVersionNames.put(Opcodes.V1_1, "Java 1.1");
        bytecodeVersionNames.put(Opcodes.V1_2, "Java 1.2");
        bytecodeVersionNames.put(Opcodes.V1_3, "Java 1.3");
        bytecodeVersionNames.put(Opcodes.V1_4, "Java 1.4");
        bytecodeVersionNames.put(Opcodes.V1_5, "Java 5");
        bytecodeVersionNames.put(Opcodes.V1_6, "Java 6");
        bytecodeVersionNames.put(Opcodes.V1_7, "Java 7");
        bytecodeVersionNames.put(Opcodes.V1_7 + 1, "Java 8");
    }

    private final Properties p;

    public Config(Properties p) {
        this.p = p;
    }

    public boolean isFullyConfigured() {
        return hasAllRequiredProperties() && PreMain.isAgentLoaded();
    }

    private boolean hasAllRequiredProperties() {
        for (String requiredParameter : requiredProperties) {
            if (p.getProperty(requiredParameter) == null) {
                return false;
            }
        }
        return true;
    }


    // bytecode version

    static {
        optionalParameterHelp(BYTECODE_VERSION,
                "Major version number for the generated bytecode. For a list, see",
                "offset 7 at http://en.wikipedia.org/wiki/Java_class_file#General_layout",
                "Default value is " + Opcodes.V1_7 + " (i.e. Java 7)");
    }

    public int getBytecodeVersion() {
        return Integer.parseInt(p.getProperty(BYTECODE_VERSION, "" + Opcodes.V1_7));
    }

    public String getJavaVersion() {
        return bytecodeVersionNames.getOrDefault(getBytecodeVersion(), "unknown version");
    }


    // input dir

    static {
        requiredParameterHelp(INPUT_DIR,
                "Input directory from where the original class files are read.");
    }

    public Path getInputDir() {
        return Paths.get(getRequiredProperty(INPUT_DIR));
    }


    // output dir

    static {
        optionalParameterHelp(OUTPUT_DIR,
                "Output directory into where the generated class files are written.",
                "Defaults to same as " + INPUT_DIR);
    }

    public Path getOutputDir() {
        String outputDir = p.getProperty(OUTPUT_DIR);
        if (outputDir == null) {
            return getInputDir();
        }
        return Paths.get(outputDir);
    }


    // classpath

    static {
        requiredParameterHelp(CLASSPATH,
                "Classpath containing the original class files and their dependencies.");
    }

    public String getClasspath() {
        return getRequiredProperty(CLASSPATH);
    }

    private String getRequiredProperty(String key) {
        String value = p.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return value;
    }

    // incremental files

    static {
        optionalParameterHelp(CHANGED,
                "A list of all the files that have changed since last run.",
                "This is useful for a build tool to support incremental compilation.");
    }

    public List<Path> getChangedFiles() {
        String files = p.getProperty(CHANGED);
        if (files == null) return null;
        return Arrays.asList(files.split(File.pathSeparator)).stream()
                .map(Paths::get).collect(Collectors.toList());
    }

    // help

    public String getHelp() {
        String options = requiredProperties.stream()
                .map(key -> "-D" + key + "=?")
                .reduce((a, b) -> a + " " + b)
                .get();
        return "Usage: java " + options + " -javaagent:retrolambda.jar -jar retrolambda.jar\n" +
                "\n" +
                "Retrolambda is a backporting tool for classes which use lambda expressions\n" +
                "and have been compiled with Java 8, to run on Java 7 (maybe even Java 5).\n" +
                "See https://github.com/orfjackal/retrolambda\n" +
                "\n" +
                "Copyright (c) 2013-2014  Esko Luontola <www.orfjackal.net>\n" +
                "This software is released under the Apache License 2.0.\n" +
                "The license text is at http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "Required system properties:\n" +
                "\n" +
                requiredProperitesHelp.stream().reduce((a, b) -> a + "\n" + b).get() +
                "\n" +
                "Optional system properties:\n" +
                "\n" +
                optionalPropertiesHelp.stream().reduce((a, b) -> a + "\n" + b).get();
    }

    private static void requiredParameterHelp(String key, String... lines) {
        requiredProperties.add(key);
        requiredProperitesHelp.add(formatPropertyHelp(key, lines));
    }

    private static void optionalParameterHelp(String key, String... lines) {
        optionalPropertiesHelp.add(formatPropertyHelp(key, lines));
    }

    private static String formatPropertyHelp(String key, String... lines) {
        String s = "  " + key + "\n";
        for (String line : lines) {
            s += "      " + line + "\n";
        }
        return s;
    }
}
