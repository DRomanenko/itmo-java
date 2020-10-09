package ru.ifmo.rain.romanenko.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation class for {@link Impler}. Provides public methods for abstract class generation or
 * basic implementation interface.
 *
 * @author Demian Romanenko (mrnearall@gmail.com)
 */
public class Implementor implements Impler {
    /**
     * Line separator token.
     */
    private static final String EOL = System.lineSeparator();
    /**
     * Empty string token.
     */
    private static final String EMPTY = "";
    /**
     * Single space token.
     */
    private static final String SPACE = " ";
    /**
     * Comma with space token.
     */
    private static final String COMMA = ", ";
    /**
     * End of instruction token.
     */
    private static final String EOI = ";";

    /**
     * Interface implementation declaration token.
     */
    private static final String IMPLEMENTS = "implements";
    /**
     * Class declaration token.
     */
    private static final String CLASS = "class";
    /**
     * Possible exceptions declaration token.
     */
    private static final String THROWS = "throws";
    /**
     * Super class extension declaration token.
     */
    private static final String EXTENDS = "extends";
    /**
     * Super class reference token.
     */
    private static final String SUPER = "super";
    /**
     * <code>null</code> token.
     */
    private static final String NULL = "null";
    /**
     * <code>false</code> token.
     */
    private static final String FALSE = "false";
    /**
     * <code>0</code> token.
     */
    private static final String ZERO = "0";
    /**
     * Return value token.
     */
    private static final String RETURN = "return";


    /**
     * Default constructor.
     */
    public Implementor() {
    }

    /**
     * Returns parts, joined with separator.
     *
     * @param separator {@link String} that will separate parts.
     * @param parts     strings that will be joined.
     * @return {@link String} of all non-empty words in parts, separated by {@code separator}.
     */
    static String mergeWithEOL(final String separator, final String... parts) {
        return Arrays.stream(parts).filter(s -> !EMPTY.equals(s)).collect(Collectors.joining(separator));
    }

    /**
     * Return merged {@code elements}, transformed by {@code transform}, concatenated by COMMA.
     *
     * @param <T>       type of items
     * @param elements  array of args to merge
     * @param transform func, that transform <T> to {@link String}
     * @return {@link String} from elements, transformed by {@code transform}
     */
    static <T> String merge(final T[] elements, final Function<T, String> transform) {
        return Arrays.stream(elements).map(transform).collect(Collectors.joining(COMMA));
    }

    /**
     * Return {@link String} package for {@code token}, if it's null, return EMPTY.
     *
     * @param token instance of {@link Class}.
     * @return {@link String} full package of {@code token}, if it's null return EMPTY.
     */
    private static String getImplPackage(final Class<?> token) {
        return token.getPackageName() == null ? EMPTY : mergeWithEOL(SPACE, token.getPackage() + EOI);
    }

    /**
     * Checks whether {@link Class} attributes and modifiers for the ability to implement {@code token}.
     *
     * @param token {@link Class} type token to create implementation for.
     * @throws ImplerException if {@code token} can not be implemented.
     */
    private void checkTypesBeforeCreate(final Class<?> token) throws ImplerException {
        if (token.isPrimitive() || token.isArray() || Enum.class.equals(token)) {
            throw new ImplerException(token.getTypeName() + " is an unsupported token");
        }
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Final class is an unsupported token");
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Private class is an unsupported token");
        }
    }

    /**
     * Provides valid source code directories.
     *
     * @param token    {@link Class} which implementation is required.
     * @param compiled {@code true} if need {@link Path} to compiled file.
     * @return {@link Path} to implementing class.
     */
    static Path getImplementationPath(final Class<?> token, final boolean compiled) {
        return Paths.get(String.join(File.separator, token.getPackageName().split("\\.")) + File.separator +
                getClassName(token) + (compiled ? ".class" : ".java"));
    }

    /**
     * Creates missing parent directories of given path.
     *
     * @param path file to create parent directory.
     * @throws ImplerException if an I/O error occurs.
     */
    static void createParentDirectories(final Path path) throws ImplerException {
        final Path parentPath = path.toAbsolutePath().normalize().getParent();
        if (parentPath != null) {
            try {
                Files.createDirectories(parentPath);
            } catch (final IOException e) {
                throw new ImplerException("Failed to prepare source code directory", e);
            }
        }
    }

    /**
     * Creates {@link Path} of implementation source code and create missing parent directories.
     * Located in directory represented by root.
     *
     * @param token {@link Class} which implementation is required.
     * @param root  Root {@link Path} for implementation files.
     * @return {@link Path} to implementation.
     * @throws ImplerException In case generated path is invalid.
     */
    static Path getImplClassPath(final Class<?> token, final Path root) throws ImplerException {
        final Path sourceCodePath = getImplementationPath(token, false);
        final Path path;
        try {
            path = root.resolve(sourceCodePath);
        } catch (final InvalidPathException e) {
            throw new ImplerException("Invalid path", e);
        }
        createParentDirectories(path);
        return path;
    }

    /**
     * Return joins THROWS and {@code itemList} with separator {@code System.lineSeparator} if itemList isn't empty.
     *
     * @param itemList string, that will merge prefix, if its't empty.
     * @return EMPTY if {@code itemList} isn't empty, otherwise concatenation of THROWS and {@code itemList} with {@code System.lineSeparator}.
     */
    private static String getIfNotEmpty(final String itemList) {
        if (!itemList.isEmpty()) {
            return mergeWithEOL(EOL, THROWS, itemList);
        }
        return EMPTY;
    }

    /**
     * Returns {@link String} of Exceptions, if they aren't null.
     *
     * @param executable an instance of {@link Executable} (method or constructor).
     * @return EMPTY if there're no {@link Exception} in {@code executable}, THROWS + list of exceptions, otherwise separated by COMMA.
     */
    private String getExecutableExceptions(final Executable executable) {
        return getIfNotEmpty(merge(executable.getExceptionTypes(), Class::getCanonicalName));
    }

    /**
     * Return all Modifiers of class {@code executable}:
     * {@link Modifier#ABSTRACT}, {@link Modifier#NATIVE}, {@link Modifier#TRANSIENT}.
     *
     * @param executable instance of {@link Executable}.
     * @return {@link String} of token's Modifiers.
     */
    private String getExecutableModifiers(final Executable executable) {
        return Modifier.toString(executable.getModifiers() & ~Modifier.NATIVE & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT);
    }

    /**
     * Return a list of types and names using {@link #merge}.
     *
     * @param executable method or constructor, instance of {@link Executable}.
     * @return {@link String} from list of types from {@code executable}.
     */
    private String getExecutableArguments(final Executable executable) {
        final Class<?>[] elems = executable.getParameterTypes();
        final String[] str = new String[elems.length];
        IntStream.range(0, elems.length).forEach(i -> str[i] = elems[i].getCanonicalName() + " _" + i);
        return "(" + String.join(COMMA, str) + ")";
    }

    /**
     * Return a list of types and names using {@link #merge}.
     *
     * @param executable method or constructor, instance of {@link Executable}.
     * @return {@link String} from list of types from {@code executable}.
     */
    private String getExecutableArgumentsNames(final Executable executable) {
        final Class<?>[] elems = executable.getParameterTypes();
        final String[] str = new String[elems.length];
        IntStream.range(0, elems.length).forEach(i -> str[i] = "_" + i);
        return "(" + String.join(COMMA, str) + ")";
    }

    /**
     * Return default value for {@link #getMethod(Method)}.
     *
     * @param clazz method for default value.
     * @return {@link String} representing default value of {@code clazz}.
     */
    private String getDefaultValue(final Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return NULL;
        } else if (clazz.equals(void.class)) {
            return EMPTY;
        } else if (clazz.equals(boolean.class)) {
            return FALSE;
        } else {
            return ZERO;
        }
    }

    /**
     * Return default method body using {@link #getDefaultValue(Class)}.
     *
     * @param method {@link Method} from where {@link #getDefaultValue(Class)} get its value.
     * @return {@link String} RETURN + {@link #getDefaultValue(Class)} + EOI - default {@link Method} body.
     */
    private String getMethodBody(final Method method) {
        return mergeWithEOL(SPACE,
                RETURN,
                getDefaultValue(method.getReturnType())) + EOI;
    }

    /**
     * Builder of Method. It includes {@link #getExecutableModifiers(Executable)} {@link Method#getReturnType()}
     * {@link Method#getName()} and {@link #getExecutableExceptions(Executable)} {@link #getMethodBody(Method)}.
     *
     * @param method {@link Method} method, which implementation is needed.
     * @return {@link String} implemented method {@link Method}.
     */
    private String getMethod(final Method method) {
        return mergeWithEOL(SPACE,
                getExecutableModifiers(method),
                method.getReturnType().getCanonicalName(),
                method.getName() + getExecutableArguments(method),
                getExecutableExceptions(method),
                mergeWithEOL(EOL, "{", getMethodBody(method) + '}'));
    }

    /**
     * Return default {@link Constructor} body code, using {@link #getExecutableArgumentsNames(Executable)}.
     *
     * @param constructor {@link Constructor} from where {@link #getExecutableArgumentsNames(Executable)} get it's value.
     * @return {@link String} SUPER + {@link #getExecutableArgumentsNames(Executable)} + EOI - default {@link Constructor} body.
     */
    private String getConstructorBody(final Constructor<?> constructor) {
        return SUPER + getExecutableArgumentsNames(constructor) + EOI;
    }

    /**
     * Filters modifiers for implementation class. Excludes modifiers of {@link Class}:
     * {@link Modifier#INTERFACE}, {@link Modifier#ABSTRACT},
     * {@link Modifier#STATIC}, {@link Modifier#PROTECTED} excluded.
     *
     * @param token instance of {@link Class}.
     * @return {@link String} of filtered Modifier's.
     */
    private String getClassModifiers(final Class<?> token) {
        return Modifier.toString(token.getModifiers() & ~Modifier.INTERFACE & ~Modifier.ABSTRACT & ~Modifier.STATIC & ~Modifier.PROTECTED);
    }

    /**
     * Return {@link Class#getSimpleName()} with word "Impl"
     * used to generate name for implemented class.
     *
     * @param token instance of {@link Class}.
     * @return String of Simple name for the answer.
     */
    static String getClassName(final Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Return the full name of the class {@code token} with includes
     * modifiers {@link #getClassModifiers(Class)},
     * name {@link #getClassName(Class)} and superclass.
     *
     * @param token instance of {@link Class}.
     * @return {@link String} full class name.
     */
    private String getClassDefinition(final Class<?> token) {
        return mergeWithEOL(SPACE,
                getClassModifiers(token),
                CLASS,
                getClassName(token),
                mergeWithEOL(SPACE, token.isInterface() ? IMPLEMENTS : EXTENDS, token.getCanonicalName()));
    }

    /**
     * Generated {@link Constructor} code. it combine {@link #getExecutableModifiers(Executable)} {@link #getClassName(Class)}
     * and {@link #getExecutableExceptions(Executable)} {@link #getConstructorBody(Constructor)}.
     *
     * @param constructor {@link Constructor} which implementation is needed.
     * @return Body implementation {@link String} of required {@link Constructor}.
     */
    private String getConstructor(final Constructor<?> constructor) {
        return mergeWithEOL(SPACE,
                getExecutableModifiers(constructor),
                getClassName(constructor.getDeclaringClass()) + getExecutableArguments(constructor),
                getExecutableExceptions(constructor),
                mergeWithEOL(EOL, "{", getConstructorBody(constructor), "}")
        );
    }

    /**
     * Generates all available constructors source code for the class.
     *
     * @param token instance of {@link Class}.
     * @return {@link String} of constructors of {@code token}.
     * @throws ImplerException {@link Class} don't have no non-private constructors.
     */
    private String getImplConstructors(final Class<?> token) throws ImplerException {
        if (token.isInterface()) {
            return EMPTY;
        }
        final List<Constructor<?>> nonPrivateConstructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .collect(Collectors.toList());
        if (nonPrivateConstructors.isEmpty()) {
            throw new ImplerException("Class don't have no non-private constructors");
        }
        return nonPrivateConstructors
                .stream()
                .map(this::getConstructor)
                .collect(Collectors.joining(EOL));
    }


    /**
     * Static class for correct compaction {@link Method}.
     */
    private static class SignatureComparator {
        /**
         * Enclosed {@link Method} object.
         */
        private final Method method;

        /**
         * Polynomial hash power.
         */
        private final int POW = 31;

        /**
         * Prime module used in hashing.
         */
        private final int MOD = 1000000007;

        /**
         * Wrapping constructor.
         *
         * @param method instance of {@link Method} class to be wrapped.
         */
        SignatureComparator(final Method method) {
            this.method = method;
        }

        /**
         * Method getter.
         *
         * @return wrapped {@link #method}.
         */
        Method getMethod() {
            return method;
        }

        /**
         * Hash code polynomial calculator.
         * Calculates polynomial hash of {@link #method} signature.
         *
         * @return integer hash code value.
         */
        @Override
        public int hashCode() {
            int hash = method.getReturnType().hashCode() % MOD;
            hash = (hash + POW * method.getName().hashCode()) % MOD;
            hash = (hash + (POW * POW) % MOD * Arrays.hashCode(method.getParameterTypes()) % MOD);
            return hash;
        }

        /**
         * Checker for equals for to objects.
         *
         * @param o {@link Object} to compare with.
         * @return {@code true} if objects are equal, {@code false} otherwise.
         */
        @Override
        public boolean equals(final Object o) {
            if (o instanceof SignatureComparator) {
                final SignatureComparator casted = (SignatureComparator) o;
                return method.getName().equals(casted.method.getName()) &&
                        Arrays.equals(method.getParameterTypes(), casted.method.getParameterTypes()) &&
                        method.getReturnType().equals(casted.method.getReturnType());
            }
            return false;
        }
    }

    /**
     * Collects {@link Set}. Removes overridden {@link Method}.
     *
     * @param methods {@link Method} array that should be filtered.
     * @return {@link Set} of final versions of each {@link Method}.
     */
    private static Set<SignatureComparator> getClearSignatureMethods(final Method[] methods) {
        return Arrays.stream(methods)
                .map(SignatureComparator::new)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Returns string of methods with default implementation {@link #getMethod(Method)}.
     *
     * @param token instance of {@link Class}.
     * @return {@link String} all implementations of abstract non private methods.
     */
    private String getImplMethods(Class<?> token) {
        final Set<SignatureComparator> methods = getClearSignatureMethods(token.getMethods());
        for (; token != null; token = token.getSuperclass()) {
            Arrays.stream(token.getDeclaredMethods()).map(SignatureComparator::new).forEach(methods::add);
        }
        return methods.stream().filter(a -> Modifier.isAbstract(a.getMethod().getModifiers()))
                .map(a -> getMethod(a.getMethod()))
                .collect(Collectors.joining(EOL));
    }

    /**
     * Returns a {@link Class} implementation declaration: package {@link #getImplPackage(Class)},
     * name, extended class or implemented interface {@link #getClassDefinition(Class)},
     * constructors {@link #getImplConstructors(Class)} and methods {@link #getImplMethods(Class)}.
     *
     * @param token {@link Class} to create implementation for.
     * @return {@link String} containing complete generated source code.
     * @throws ImplerException if there are exceptions in {@link #getImplConstructors(Class)} or {@link #getImplMethods(Class)}.
     */
    private String getImplFullClass(final Class<?> token) throws ImplerException {
        return mergeWithEOL(EOL,
                getImplPackage(token),
                mergeWithEOL(SPACE, getClassDefinition(token), "{"),
                getImplConstructors(token),
                getImplMethods(token),
                "}"
        );
    }

    /**
     * Converts {@code String} to unicode escaping.
     *
     * @param string the {@link String} to be converted.
     * @return converted {@link String} to unicode.
     */
    private static String toUnicode(final String string) {
        final StringBuilder builder = new StringBuilder();
        for (final char c : string.toCharArray()) {
            builder.append(c >= 128 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
        }
        return builder.toString();
    }

    /**
     * @see Impler#implement(Class, Path)
     */
    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Arguments must not be null");
        }
        checkTypesBeforeCreate(token);
        final Path implClassPath = getImplClassPath(token, root);
        try (final BufferedWriter output = Files.newBufferedWriter(implClassPath)) {
            output.write(toUnicode(getImplFullClass(token)));
        } catch (final IOException e) {
            throw new ImplerException("I/O error occurred", e);
        }
    }

    /**
     * Main function to provide console interface of the program.
     * <p>
     * Gets args from console for {@link Implementor} 2-args {@code className outPath} creates file
     * {@code .java} file in {@code outPath} by {@link #implement(Class, Path)}
     * <p>
     * Any errors and warnings are printed to <code>STDOUT</code> and <code>STDERR</code>.
     *
     * @param args arguments for application
     */
    public static void main(final String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Expected 2 args: classname, outPath.");
            return;
        }

        for (final String arg : args) {
            if (arg == null) {
                System.out.println(Arrays.toString(args) + " is null.");
            }
        }
        try {
            new Implementor().implement(Class.forName(args[0]), Path.of(args[1]));
        } catch (final ClassNotFoundException e) {
            System.err.println("Class not Found" + e.getMessage());
        } catch (final ImplerException e) {
            System.err.println("Failed to Implement" + e.getMessage());
        } catch (final InvalidPathException e) {
            System.err.println("Failed to make Path" + e.getMessage());
        }
    }
}