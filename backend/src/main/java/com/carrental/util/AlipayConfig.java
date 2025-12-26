package com.carrental.util;

/**
 * Alipay configuration loaded from application.properties.
 *
 * Note: For security, do NOT commit real production keys. Sandbox only.
 */
public class AlipayConfig {

    private AlipayConfig() {}

    public static String getGatewayUrl() {
        return DatabaseUtil.getProperty("alipay.gatewayUrl");
    }

    public static String getAppId() {
        return DatabaseUtil.getProperty("alipay.appId");
    }

    /**
     * Merchant's RSA2 private key in PKCS8 format (single-line base64 or PEM without headers).
     */
    public static String getPrivateKey() {
        return DatabaseUtil.getProperty("alipay.privateKey");
    }

    /**
     * Alipay RSA2 public key (single-line base64 or PEM without headers).
     */
    public static String getAlipayPublicKey() {
        return DatabaseUtil.getProperty("alipay.alipayPublicKey");
    }

    public static String getNotifyUrl() {
        return DatabaseUtil.getProperty("alipay.notifyUrl");
    }

    public static String getReturnUrl() {
        return DatabaseUtil.getProperty("alipay.returnUrl");
    }

    public static String getReturnRedirectUrl() {
        return DatabaseUtil.getProperty("alipay.returnRedirectUrl", "http://localhost:3000/orders");
    }

    public static String getCharset() {
        return DatabaseUtil.getProperty("alipay.charset", "utf-8");
    }

    public static String getSignType() {
        return DatabaseUtil.getProperty("alipay.signType", "RSA2");
    }

    public static String getProductCode() {
        // Common values: QUICK_WAP_WAY (mobile) / FAST_INSTANT_TRADE_PAY (PC)
        return DatabaseUtil.getProperty("alipay.productCode", "FAST_INSTANT_TRADE_PAY");
    }

    public static String getSubjectPrefix() {
        return DatabaseUtil.getProperty("alipay.subjectPrefix", "CarRental-");
    }
}

