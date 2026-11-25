import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Fails when the plugin JAR uses a class, method or field that one server's
 * compile classpath does not provide.
 *
 * <p>The release JAR is compiled against the oldest supported server and Towny
 * APIs and loaded on every newer version. A method whose signature changed in a
 * newer API still compiles against that API, but the old JAR would throw
 * NoSuchMethodError at runtime. This check finds that without starting a server.
 *
 * <p>Usage: {@code java scripts/ApiLinkCheck.java <plugin.jar> <classpath file>},
 * where the classpath file holds Maven's {@code dependency:build-classpath} output.
 */
public final class ApiLinkCheck {
    private static final String USAGE = "Usage: java ApiLinkCheck.java <plugin.jar> <classpath file>";
    private static final String PLUGIN_PACKAGE = "com/TownyDiscordChat/TownyDiscordChat/";
    private static final String BUNDLED_PACKAGE = PLUGIN_PACKAGE + "shaded/";
    private static final List<String> PLATFORM_PREFIXES =
            List.of("java/", "javax/", "jdk/", "sun/", "com/sun/");
    private static final String CLASS_SUFFIX = ".class";
    private static final String METADATA_FOLDER = "META-INF/";
    private static final String MODULE_INFO = "module-info.class";
    private static final int USAGE_EXIT = 2;
    private static final int FAILURE_EXIT = 1;

    private ApiLinkCheck() {
    }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 2) {
            System.err.println(USAGE);
            System.exit(USAGE_EXIT);
        }
        List<Path> classpath = readClasspath(Path.of(arguments[1]));
        try (ClassIndex index = new ClassIndex()) {
            index.addJar(Path.of(arguments[0]));
            for (Path jar : classpath) {
                index.addJar(jar);
            }
            Map<String, Set<String>> problems = findProblems(index);
            report(problems, index.pluginClassNames().size(), classpath.size());
            if (!problems.isEmpty()) {
                System.exit(FAILURE_EXIT);
            }
        }
    }

    private static List<Path> readClasspath(Path classpathFile) throws IOException {
        String content = Files.readString(classpathFile, StandardCharsets.UTF_8).trim();
        List<Path> jars = new ArrayList<>();
        for (String entry : content.split(File.pathSeparator)) {
            if (entry.endsWith(".jar")) {
                jars.add(Path.of(entry));
            }
        }
        return jars;
    }

    private static Map<String, Set<String>> findProblems(ClassIndex index) throws IOException {
        Map<String, Set<String>> problems = new TreeMap<>();
        for (String pluginClassName : index.pluginClassNames()) {
            ParsedClass pluginClass = index.find(pluginClassName).orElseThrow();
            for (String problem : problemsIn(pluginClass, index)) {
                problems.computeIfAbsent(problem, ignored -> new TreeSet<>()).add(pluginClassName);
            }
        }
        return problems;
    }

    private static List<String> problemsIn(ParsedClass pluginClass, ClassIndex index) throws IOException {
        List<String> problems = new ArrayList<>();
        for (String referenced : pluginClass.referencedClasses()) {
            String className = elementClass(referenced);
            if (className != null && !isPlatformClass(className) && index.find(className).isEmpty()) {
                problems.add("missing class " + className);
            }
        }
        for (MemberReference reference : pluginClass.memberReferences()) {
            if (needsCheck(reference, index) && !resolves(reference, index)) {
                problems.add("missing " + reference.describe());
            }
        }
        return problems;
    }

    // A missing owner is already reported as a missing class.
    private static boolean needsCheck(MemberReference reference, ClassIndex index) throws IOException {
        String owner = reference.owner();
        return !owner.startsWith("[") && !isPlatformClass(owner) && index.find(owner).isPresent();
    }

    private static boolean resolves(MemberReference reference, ClassIndex index) throws IOException {
        Deque<String> pending = new ArrayDeque<>(List.of(reference.owner()));
        Set<String> visited = new HashSet<>();
        while (!pending.isEmpty()) {
            String className = pending.poll();
            Optional<ParsedClass> found = visited.add(className) ? index.find(className) : Optional.empty();
            if (found.isEmpty()) {
                continue;
            }
            if (found.get().declares(reference)) {
                return true;
            }
            pending.addAll(found.get().supertypes());
        }
        return false;
    }

    private static String elementClass(String referenced) {
        String element = referenced.replaceFirst("^\\[+", "");
        if (element.equals(referenced)) {
            return referenced;
        }
        if (element.startsWith("L") && element.endsWith(";")) {
            return element.substring(1, element.length() - 1);
        }
        return null;
    }

    private static boolean isPlatformClass(String className) {
        return PLATFORM_PREFIXES.stream().anyMatch(className::startsWith);
    }

    private static void report(Map<String, Set<String>> problems, int pluginClasses, int jars) {
        for (Map.Entry<String, Set<String>> problem : problems.entrySet()) {
            System.out.println("[LINK] " + problem.getKey() + " (used by "
                    + String.join(", ", problem.getValue()).replace('/', '.') + ")");
        }
        String summary = pluginClasses + " plugin classes against " + jars + " classpath JARs";
        System.out.println(problems.isEmpty()
                ? "[LINK] PASS - every reference resolves: " + summary
                : "[LINK] FAIL - " + problems.size() + " unresolved references: " + summary);
    }

    private record MemberReference(String owner, String name, String descriptor, boolean isField) {
        String key() {
            return memberKey(isField, name, descriptor);
        }

        String describe() {
            String member = isField ? "field " + name + " " + descriptor : "method " + name + descriptor;
            return member + " in " + owner;
        }
    }

    private record ParsedClass(String name, String superName, List<String> interfaces,
                               Set<String> fields, Set<String> methods,
                               Set<String> referencedClasses, Set<MemberReference> memberReferences) {
        boolean declares(MemberReference reference) {
            return (reference.isField() ? fields : methods).contains(reference.key());
        }

        List<String> supertypes() {
            List<String> supertypes = new ArrayList<>(interfaces);
            if (superName != null) {
                supertypes.add(0, superName);
            }
            return supertypes;
        }
    }

    private static String memberKey(boolean isField, String name, String descriptor) {
        return isField ? name + ":" + descriptor : name + descriptor;
    }

    /** Parses only the parts of a class file that linking needs (JVMS chapter 4). */
    private static final class ClassFileReader {
        private static final int MAGIC = 0xCAFEBABE;

        private ClassFileReader() {
        }

        static ParsedClass parse(byte[] bytes) throws IOException {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
            if (input.readInt() != MAGIC) {
                throw new IOException("Not a class file");
            }
            input.skipNBytes(Short.BYTES * 2);
            ConstantPool pool = ConstantPool.read(input);
            input.skipNBytes(Short.BYTES);
            String name = pool.className(input.readUnsignedShort());
            int superIndex = input.readUnsignedShort();
            String superName = superIndex == 0 ? null : pool.className(superIndex);
            List<String> interfaces = readInterfaces(input, pool);
            Set<String> fields = readMembers(input, pool, true);
            Set<String> methods = readMembers(input, pool, false);
            return new ParsedClass(name, superName, interfaces, fields, methods,
                    pool.classNames(name), pool.memberReferences());
        }

        private static List<String> readInterfaces(DataInputStream input, ConstantPool pool) throws IOException {
            int count = input.readUnsignedShort();
            List<String> interfaces = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                interfaces.add(pool.className(input.readUnsignedShort()));
            }
            return interfaces;
        }

        private static Set<String> readMembers(DataInputStream input, ConstantPool pool,
                                               boolean isField) throws IOException {
            int count = input.readUnsignedShort();
            Set<String> members = new HashSet<>();
            for (int i = 0; i < count; i++) {
                input.skipNBytes(Short.BYTES);
                String name = pool.utf8(input.readUnsignedShort());
                String descriptor = pool.utf8(input.readUnsignedShort());
                members.add(memberKey(isField, name, descriptor));
                skipAttributes(input);
            }
            return members;
        }

        private static void skipAttributes(DataInputStream input) throws IOException {
            int count = input.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                input.skipNBytes(Short.BYTES);
                input.skipNBytes(Integer.toUnsignedLong(input.readInt()));
            }
        }
    }

    private static final class ConstantPool {
        private static final int UTF8 = 1;
        private static final int INTEGER = 3;
        private static final int FLOAT = 4;
        private static final int LONG = 5;
        private static final int DOUBLE = 6;
        private static final int CLASS = 7;
        private static final int STRING = 8;
        private static final int FIELD_REF = 9;
        private static final int METHOD_REF = 10;
        private static final int INTERFACE_METHOD_REF = 11;
        private static final int NAME_AND_TYPE = 12;
        private static final int METHOD_HANDLE = 15;
        private static final int METHOD_TYPE = 16;
        private static final int DYNAMIC = 17;
        private static final int INVOKE_DYNAMIC = 18;
        private static final int MODULE = 19;
        private static final int PACKAGE = 20;

        private final int[] tags;
        private final String[] utf8Values;
        private final int[] firstIndexes;
        private final int[] secondIndexes;

        private ConstantPool(int count) {
            tags = new int[count];
            utf8Values = new String[count];
            firstIndexes = new int[count];
            secondIndexes = new int[count];
        }

        static ConstantPool read(DataInputStream input) throws IOException {
            ConstantPool pool = new ConstantPool(input.readUnsignedShort());
            for (int index = 1; index < pool.tags.length; index++) {
                int tag = input.readUnsignedByte();
                pool.tags[index] = tag;
                if (pool.readEntry(tag, index, input)) {
                    index++;
                }
            }
            return pool;
        }

        /** Returns true for the eight-byte constants, which take two pool slots. */
        private boolean readEntry(int tag, int index, DataInputStream input) throws IOException {
            switch (tag) {
                case UTF8 -> utf8Values[index] = input.readUTF();
                case CLASS, STRING, METHOD_TYPE, MODULE, PACKAGE -> firstIndexes[index] = input.readUnsignedShort();
                case FIELD_REF, METHOD_REF, INTERFACE_METHOD_REF, NAME_AND_TYPE, DYNAMIC, INVOKE_DYNAMIC -> {
                    firstIndexes[index] = input.readUnsignedShort();
                    secondIndexes[index] = input.readUnsignedShort();
                }
                case INTEGER, FLOAT -> input.skipNBytes(Integer.BYTES);
                case LONG, DOUBLE -> {
                    input.skipNBytes(Long.BYTES);
                    return true;
                }
                case METHOD_HANDLE -> {
                    input.skipNBytes(Byte.BYTES);
                    firstIndexes[index] = input.readUnsignedShort();
                }
                default -> throw new IOException("Unknown constant pool tag " + tag);
            }
            return false;
        }

        String utf8(int index) {
            return utf8Values[index];
        }

        String className(int classIndex) {
            return utf8Values[firstIndexes[classIndex]];
        }

        Set<String> classNames(String ownName) {
            Set<String> names = new LinkedHashSet<>();
            for (int index = 1; index < tags.length; index++) {
                if (tags[index] == CLASS && !className(index).equals(ownName)) {
                    names.add(className(index));
                }
            }
            return names;
        }

        Set<MemberReference> memberReferences() {
            Set<MemberReference> references = new LinkedHashSet<>();
            for (int index = 1; index < tags.length; index++) {
                int tag = tags[index];
                if (tag == FIELD_REF || tag == METHOD_REF || tag == INTERFACE_METHOD_REF) {
                    int nameAndType = secondIndexes[index];
                    references.add(new MemberReference(className(firstIndexes[index]),
                            utf8(firstIndexes[nameAndType]), utf8(secondIndexes[nameAndType]),
                            tag == FIELD_REF));
                }
            }
            return references;
        }
    }

    /** Finds classes lazily: the first JAR that holds a class wins, as on a classpath. */
    private static final class ClassIndex implements Closeable {
        private final Map<String, JarFile> classLocations = new HashMap<>();
        private final Map<String, Optional<ParsedClass>> parsedClasses = new HashMap<>();
        private final List<JarFile> openJars = new ArrayList<>();
        private final Set<String> pluginClassNames = new TreeSet<>();

        void addJar(Path jar) throws IOException {
            JarFile jarFile = new JarFile(jar.toFile());
            boolean isPluginJar = openJars.isEmpty();
            openJars.add(jarFile);
            for (JarEntry entry : (Iterable<JarEntry>) jarFile.stream()::iterator) {
                String className = classNameOf(entry);
                if (className != null && classLocations.putIfAbsent(className, jarFile) == null
                        && isPluginJar && isPluginCode(className)) {
                    pluginClassNames.add(className);
                }
            }
        }

        Set<String> pluginClassNames() {
            return pluginClassNames;
        }

        Optional<ParsedClass> find(String className) throws IOException {
            Optional<ParsedClass> cached = parsedClasses.get(className);
            if (cached != null) {
                return cached;
            }
            Optional<ParsedClass> parsed = Optional.ofNullable(readBytes(className)).map(ClassIndex::parseOrNull);
            parsedClasses.put(className, parsed);
            return parsed;
        }

        private byte[] readBytes(String className) throws IOException {
            JarFile jarFile = classLocations.get(className);
            String resource = className + CLASS_SUFFIX;
            if (jarFile != null) {
                try (InputStream stream = jarFile.getInputStream(jarFile.getJarEntry(resource))) {
                    return stream.readAllBytes();
                }
            }
            if (!isPlatformClass(className)) {
                return null;
            }
            try (InputStream stream = ClassLoader.getSystemResourceAsStream(resource)) {
                return stream == null ? null : stream.readAllBytes();
            }
        }

        private static ParsedClass parseOrNull(byte[] bytes) {
            try {
                return ClassFileReader.parse(bytes);
            } catch (IOException malformed) {
                return null;
            }
        }

        private static String classNameOf(JarEntry entry) {
            String name = entry.getName();
            if (!name.endsWith(CLASS_SUFFIX) || name.startsWith(METADATA_FOLDER) || name.endsWith(MODULE_INFO)) {
                return null;
            }
            return name.substring(0, name.length() - CLASS_SUFFIX.length());
        }

        private static boolean isPluginCode(String className) {
            return className.startsWith(PLUGIN_PACKAGE) && !className.startsWith(BUNDLED_PACKAGE);
        }

        @Override
        public void close() throws IOException {
            for (JarFile jarFile : openJars) {
                jarFile.close();
            }
        }
    }
}
