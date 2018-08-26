package com.vexsoftware.votifier.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.logging.Logger;

public class RSAKeygen {
    private static final Logger LOG = Logger.getLogger("Votifier");

    public static KeyPair generate(int bits) throws Exception {
        LOG.info("Votifier is generating an RSA key pair...");
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(bits, RSAKeyGenParameterSpec.F4);
        keygen.initialize(spec);
        return keygen.generateKeyPair();
    }
}

