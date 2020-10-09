//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package info.kgeorgiy.java.advanced.base;

import org.junit.Assert;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class CertificateGenerator implements CG {
    private static final int DIGEST_SIZE = 11;
    public static final String DIGEST_CHARS = " .:oX|-\\/#@";

    public CertificateGenerator() {
    }

    public void certify(Class<?> var1, String var2) {
        try {
            System.out.println("Class: " + var1.getSimpleName());
            System.out.println("Salt: " + var2);
            MessageDigest var3 = MessageDigest.getInstance("SHA1");
            addString(var3, var2);
            addString(var3, var1.getCanonicalName());
            addJar(var3, CertificateGenerator.class);
            addJar(var3, Assert.class);
            addJar(var3, var1);
            this.certificate(var3.digest());
        } catch (URISyntaxException | IOException | NoSuchAlgorithmException var4) {
            var4.printStackTrace();
        }

    }

    private static void addString(MessageDigest var0, String var1) {
        var0.update(var1.getBytes(StandardCharsets.UTF_8));
    }

    private static void addJar(MessageDigest var0, Class<?> var1) throws IOException, URISyntaxException {
        Module var2 = var1.getModule();
        if (var2.isNamed()) {
            addString(var0, var2.getName());
        }

        URL var3 = var1.getProtectionDomain().getCodeSource().getLocation();
        Path var4 = Paths.get(var3.toURI());
        if (!Files.isRegularFile(var4, new LinkOption[0])) {
            System.err.println("Error: Cannot find jar file at " + var3);
        } else {
            System.out.println(var4);
            var0.update(Files.readAllBytes(var4));
        }

    }

    private void certificate(byte[] var1) {
        System.out.println("Certificate: " + Base64.getEncoder().encodeToString(var1));
        printImage(this.generateImage(var1));
    }

    private static void printImage(int[][] var0) {
        String var10000 = new String(new char[11]);
        String var1 = "+" + var10000.replace('\u0000', '-') + "+";
        System.out.println(var1);
        int[][] var2 = var0;
        int var3 = var0.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            int[] var5 = var2[var4];
            System.out.print("|");
            int[] var6 = var5;
            int var7 = var5.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                int var9 = var6[var8];
                System.out.print(" .:oX|-\\/#@".charAt(var9 % " .:oX|-\\/#@".length()));
            }

            System.out.println("|");
        }

        System.out.println(var1);
    }

    private int[][] generateImage(byte[] var1) {
        int[][] var2 = new int[11][11];
        int var3 = 11;
        int var4 = 5;
        byte[] var5 = var1;
        int var6 = var1.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            byte var8 = var5[var7];

            for(int var9 = 0; var9 < 4; ++var9) {
                var8 = (byte)(var8 >>> 2);
                var3 = Math.max(0, Math.min(21, var3 + ((var8 & 1) == 0 ? 1 : -1)));
                var4 = Math.max(0, Math.min(10, var4 + ((var8 & 2) == 0 ? 1 : -1)));
                ++var2[var4][var3 / 2];
            }
        }

        return var2;
    }
}
