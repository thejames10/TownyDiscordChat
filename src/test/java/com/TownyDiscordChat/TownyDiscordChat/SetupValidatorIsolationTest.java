package com.TownyDiscordChat.TownyDiscordChat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Local startup must not resolve the DiscordSRV classes supplied by a soft dependency.
 */
public class SetupValidatorIsolationTest {
    private static final String VALIDATOR_CLASS = SetupValidator.class.getName();

    @Test
    public void localValidationDoesNotReferenceDiscordOrTownyBytecode() throws Exception {
        byte[] classBytes = readClassBytes(VALIDATOR_CLASS);
        String constantPool = new String(classBytes, StandardCharsets.ISO_8859_1);

        assertFalse(constantPool.contains("github/scarsz/discordsrv"));
        assertFalse(constantPool.contains("com/palmergames/bukkit/towny"));
    }

    @Test
    public void localValidationWorksWithoutOptionalDependencyClasses() throws Exception {
        ClassLoader isolated = new OptionalDependenciesAbsentClassLoader(
                getClass().getClassLoader());
        Class<?> validator = isolated.loadClass(VALIDATOR_CLASS);
        Method validateLocal = validator.getMethod(
                "validateLocal", org.bukkit.configuration.file.FileConfiguration.class);
        Object result = validateLocal.invoke(null, new YamlConfiguration());
        Method valid = result.getClass().getMethod("valid");

        assertFalse((Boolean) valid.invoke(result));
        assertTrue(validator.getClassLoader() == isolated);
    }

    private static byte[] readClassBytes(String className) throws IOException {
        String resource = className.replace('.', '/') + ".class";
        try (InputStream input = SetupValidatorIsolationTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing class file: " + resource);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static final class OptionalDependenciesAbsentClassLoader extends ClassLoader {
        private OptionalDependenciesAbsentClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (name.startsWith("github.scarsz.discordsrv.")
                    || name.startsWith("com.palmergames.bukkit.towny.")) {
                throw new ClassNotFoundException("Optional dependency deliberately unavailable");
            }
            if (!name.equals(VALIDATOR_CLASS) && !name.startsWith(VALIDATOR_CLASS + "$")) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = defineLocalClass(name);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private Class<?> defineLocalClass(String name) throws ClassNotFoundException {
            try {
                byte[] bytes = readClassBytes(name);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException exception) {
                throw new ClassNotFoundException(name, exception);
            }
        }
    }
}
