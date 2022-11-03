package com.cloudminds.grpcweaknettest.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

public class CertUtils {
    private static final String TAG = "CertUtils";
    private static final String BEGIN_MARKER = "-----BEGIN CERTIFICATE-----";
    private static final String END_MARKER = "-----END CERTIFICATE-----";

    public static byte[] getPublicKeySha256(Certificate cert) {
        try {
            byte[] publicKey = cert.getPublicKey().getEncoded();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(publicKey);
        } catch (NoSuchAlgorithmException ex) {
            // This exception should never happen since SHA-256 is known algorithm
            throw new RuntimeException(ex);
        }
    }

    public static byte[] pemToDer(Context context, String pemPathname) throws IOException {
        InputStream is = context.getAssets().open(pemPathname);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder builder = new StringBuilder();
        // Skip past leading junk lines, if any.
        String line = reader.readLine();
        while (line != null && !line.contains(BEGIN_MARKER)) line = reader.readLine();
        // Then skip the BEGIN_MARKER itself, if present.
        while (line != null && line.contains(BEGIN_MARKER)) line = reader.readLine();
        // Now gather the data lines into the builder.
        while (line != null && !line.contains(END_MARKER)) {
            builder.append(line.trim());
            line = reader.readLine();
        }
        reader.close();
        return Base64.decode(builder.toString(), Base64.DEFAULT);
    }


    public static X509Certificate readCertFromFileInPemFormat(String certFileName) throws Exception {
        byte[] certDer = CertUtils.pemToDer(ContextUtils.getContext().getApplicationContext(), certFileName);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certDer));
    }

    public static Set<byte[]> getPinsSha256() {
        //服务器证书
        Set<byte[]> pinsSha256 = new HashSet<byte[]>();
        X509Certificate cert = null;
        String crt_name = "server_self_signed_crt.crt";
        try {
            cert = readCertFromFileInPemFormat(crt_name);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "crt_name：" + crt_name + "    cert: " + cert.toString());
        byte[] matchingHash = CertUtils.getPublicKeySha256(cert);
        //将信任的服务器证书的公钥导入
        pinsSha256.add(matchingHash);
        return pinsSha256;
    }
}