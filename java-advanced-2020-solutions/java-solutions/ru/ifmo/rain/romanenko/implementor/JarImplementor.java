package ru.ifmo.rain.romanenko.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Implementation class for {@link JarImpler}. Generated .jar file.
 *
 * @author Demian Romanenko (mrnearall@gmail.com)
 */
public class JarImplementor extends Implementor implements JarImpler {
    /**
     * Separates directories and class name in package.
     */
    protected final static char PACKAGE_SEPARATOR = '.';

    /**
     * Separates directories and class in path.
     */
    protected final static char DIRECTORY_SEPARATOR = '/';

    /**
     * Creates new instance of {@link JarImplementor}
     */
    public JarImplementor() {
        super();
    }

    /**
     * Creates a temporary directory next to {@code path}.
     *
     * @param path a {@link Path}.
     * @return a temporary directory next to {@code path}.
     * @throws ImplerException if can not create temporary directory.
     */
    protected static Path createTempDirectory(final Path path) throws ImplerException {
        try {
            return Files.createTempDirectory(path.toAbsolutePath().getParent(), "temporary");
        } catch (final IOException e) {
            throw new ImplerException("Can't create temporary directory", e);
        }
    }

    /**
     * Compiles implemented class for {@code token}.
     *
     * @param token       type token to create implementation for.
     * @param packageRoot a package root directory.
     * @throws ImplerException if can not compile files.
     */
    protected static void compile(final Class<?> token, final Path packageRoot) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String[] args = new String[]{
                "-cp",
                packageRoot.toString() + File.pathSeparator + System.getProperty("java.class.path") +  File.pathSeparator + getClassPath(token),
                packageRoot.resolve(getImplementationPath(token, false)).toString()
        };
        if (compiler == null || compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Unable to compile generated files");
        }
    }

    private static String getClassPath(final Class<?> token) throws ImplerException {
        try {
            final CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                throw new ImplerException("Code Source is null");
            }
            final URL url = codeSource.getLocation();
            if (url == null) {
                throw new ImplerException("Code source's location is null ");
            }
            return Path.of(url.toURI()).toString();
        } catch (final SecurityException | URISyntaxException e) {
            throw new ImplerException("Class path has not found");
        }
    }

    /**
     * Converts compiled implementing for {@code token} to jar file.
     *
     * @param token       type token to create implementation for.
     * @param packageRoot a package root directory of compiled implementing.
     * @param jarFile     target <var>.jar</var> file.
     * @throws ImplerException if an I/O error occurs.
     */
    protected static void toJar(final Class<?> token, final Path packageRoot, final Path jarFile) throws ImplerException {
        try (final JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(jarFile))) {
            outputStream.putNextEntry(new JarEntry(token.getPackageName().replace(PACKAGE_SEPARATOR, DIRECTORY_SEPARATOR) +
                    DIRECTORY_SEPARATOR + getClassName(token) + ".class"));
            Files.copy(packageRoot.resolve(getImplementationPath(token, true)), outputStream);
        } catch (final IOException e) {
            throw new ImplerException("Can't write jar file", e);
        }
    }

    /**
     * Deletes the directory with contents.
     *
     * @param dir a directory for deleting.
     * @throws ImplerException if an I/O error occurs.
     */
    protected static void deleteDirectory(final Path dir) throws ImplerException {
        try {
            Files.walkFileTree(dir, new Cleaner());
        } catch (final IOException e) {
            throw new ImplerException("Can't delete temporary directory", e);
        }
    }

    /**
     * Used for recursive deleting directories and files.
     */
    private static class Cleaner extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * @see JarImpler#implementJar(Class, Path)
     */
    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Argument can't be null");
        }

        createParentDirectories(jarFile);
        final Path temporaryPath = createTempDirectory(jarFile);

        try {
            implement(token, temporaryPath);
            compile(token, temporaryPath);
            toJar(token, temporaryPath, jarFile);
        } finally {
            deleteDirectory(temporaryPath);
        }
    }

    /**
     * Main. Gets args from console for {@link Implementor}
     * <ul>
     *  <li>
     *      2-args {@code className outPath} creates file
     *      {@code .java} file in {@code outPath} by {@link #implement(Class, Path)}
     *  </li>
     *  <li>
     *      3-args {@code -jar className outPath} creates file
     *       {@code .jar} file in {@code outPath} by {@link #implementJar(Class, Path)}
     *  </li>
     * </ul>
     *
     * @param args arguments for application
     */
    public static void main(final String[] args) {
        if (args == null || args.length > 3 || args.length < 2) {
            System.err.println("Expected 2 or 3 args: (-jar)? classname, outPath");
            return;
        }
        for (final String arg : args) {
            if (arg == null) {
                System.out.println(Arrays.toString(args) + " is null.");
            }
        }

        try {
            final JarImplementor jarImplementor = new JarImplementor();
            if (args.length == 2) {
                jarImplementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else if (args[0].equals("-jar") || args[0].equals("--jar")) {
                jarImplementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                System.err.println("args[0] should be -jar or --jar, but " + args[0] + " found");
            }
        } catch (final ClassNotFoundException e) {
            System.err.println("Invalid class name: " + e.getMessage());
        } catch (final ImplerException e) {
            System.err.println("Error with implementing: " + e.getMessage());
        }
    }
}
