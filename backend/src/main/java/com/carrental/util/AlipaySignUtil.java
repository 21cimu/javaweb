package com.carrental.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Minimal RSA2 sign/verify utility compatible with Alipay OpenAPI.
 *
 * Rules:
 * - Sign (merchant request): exclude sign only
 * - Verify (Alipay callback): exclude sign and sign_type (V1), fallback to exclude sign only (V2)
 * - Exclude null/empty values
 * - Sort by ASCII (natural String order)
 * - Build query string: k1=v1&k2=v2...
 */
public class AlipaySignUtil {

    private AlipaySignUtil() {}

    /**
     * Build sign content for merchant -> Alipay requests.
     *
     * Alipay official SDK includes {@code sign_type} in the signed content and excludes {@code sign} only.
     */
    public static String buildRequestSignContent(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : keys) {
            if (key == null) continue;
            if ("sign".equals(key)) continue;

            String value = params.get(key);
            if (value == null) continue;
            value = value.trim();
            if (value.isEmpty()) continue;

            if (!first) sb.append('&');
            sb.append(key).append('=').append(value);
            first = false;
        }
        return sb.toString();
    }

    public static String sign(Map<String, String> params, String privateKey, String charset) {
        String signContent = buildRequestSignContent(params);
        return signContent(signContent, privateKey, charset);
    }

    /**
     * Build sign-check content for Alipay -> merchant callbacks (V1).
     *
     * V1 excludes both {@code sign} and {@code sign_type}.
     */
    public static String buildSignCheckContentV1(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : keys) {
            if (key == null) continue;
            if ("sign".equals(key) || "sign_type".equals(key)) continue;

            String value = params.get(key);
            if (value == null) continue;
            value = value.trim();
            if (value.isEmpty()) continue;

            if (!first) sb.append('&');
            sb.append(key).append('=').append(value);
            first = false;
        }
        return sb.toString();
    }

    public static String signContent(String content, String privateKey, String charset) {
        try {
            PrivateKey priKey = loadPrivateKey(privateKey);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(priKey);
            signature.update(content.getBytes(charset == null ? StandardCharsets.UTF_8.name() : charset));
            byte[] signed = signature.sign();
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign content", e);
        }
    }

    public static boolean verify(Map<String, String> params, String alipayPublicKey, String charset) {
        String sign = params.get("sign");
        if (sign == null || sign.isBlank()) return false;

        // V1: exclude sign and sign_type (most common)
        String contentV1 = buildSignCheckContentV1(params);
        if (verifyContent(contentV1, sign, alipayPublicKey, charset)) {
            return true;
        }

        // V2 fallback: exclude sign only (some Alipay endpoints/signature versions)
        String contentV2 = buildRequestSignContent(params);
        return verifyContent(contentV2, sign, alipayPublicKey, charset);
    }

    public static boolean verifyContent(String content, String signBase64, String alipayPublicKey, String charset) {
        try {
            PublicKey pubKey = loadPublicKey(alipayPublicKey);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(pubKey);
            signature.update(content.getBytes(charset == null ? StandardCharsets.UTF_8.name() : charset));
            return signature.verify(Base64.getDecoder().decode(signBase64));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Build application/x-www-form-urlencoded query string (URL-encoded) from params.
     */
    public static String buildUrlEncodedQuery(Map<String, String> params, String charset) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : keys) {
            String value = params.get(key);
            if (value == null) continue;
            if (!first) sb.append('&');
            sb.append(urlEncode(key, charset)).append('=').append(urlEncode(value, charset));
            first = false;
        }
        return sb.toString();
    }

    public static String urlEncode(String s, String charset) {
        try {
            return URLEncoder.encode(s, charset == null ? StandardCharsets.UTF_8.name() : charset);
        } catch (Exception e) {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        }
    }

    public static String urlDecode(String s, String charset) {
        try {
            return URLDecoder.decode(s, charset == null ? StandardCharsets.UTF_8.name() : charset);
        } catch (Exception e) {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        }
    }

    private static PrivateKey loadPrivateKey(String privateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(normalizeKey(privateKey));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private static PublicKey loadPublicKey(String publicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(normalizeKey(publicKey));
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private static String normalizeKey(String key) {
        if (key == null) return "";
        return key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }
}

