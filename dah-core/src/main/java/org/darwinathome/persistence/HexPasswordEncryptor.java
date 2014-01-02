package org.darwinathome.persistence;

import org.jasypt.digest.StandardStringDigester;
import org.jasypt.util.password.PasswordEncryptor;

/**
 * A variation on the basic password encryptor which uses hex instead of base64
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class HexPasswordEncryptor implements PasswordEncryptor {
    // The internal digester used
    private final StandardStringDigester digester;

    /**
     * Creates a new instance of <tt>BasicPasswordEncryptor</tt>
     *
     */
    public HexPasswordEncryptor() {
        this.digester = new StandardStringDigester();
        this.digester.setStringOutputType("hexadecimal");
        this.digester.initialize();
    }

    /**
     * Encrypts (digests) a password.
     *
     * @param password the password to be encrypted.
     * @return the resulting digest.
     * @see StandardStringDigester#digest(String)
     */
    public String encryptPassword(String password) {
        return this.digester.digest(password);
    }

    /**
     * Checks an unencrypted (plain) password against an encrypted one
     * (a digest) to see if they match.
     *
     * @param plainPassword the plain password to check.
     * @param encryptedPassword the digest against which to check the password.
     * @return true if passwords match, false if not.
     * @see org.jasypt.digest.StandardStringDigester#matches(String, String)
     */
    public boolean checkPassword(String plainPassword,
            String encryptedPassword) {
        return this.digester.matches(plainPassword, encryptedPassword);
    }
}
