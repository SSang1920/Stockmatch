package com.stockmatch.stock.importer;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public final class ImportChecksumUtils {

    private ImportChecksumUtils() {}

    public static String sha256Hex(Resource resource) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = resource.getInputStream();
                 DigestInputStream din = new DigestInputStream(in, md)) {
                byte[] buf = new byte[8192];
                while (din.read(buf) != -1) {
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 계산 실패: " + resource.getDescription(), e);
        }
    }
}
