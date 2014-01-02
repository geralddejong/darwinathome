// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================

package org.darwinathome.server.persistence;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.darwinathome.persistence.HexPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;

import java.util.Date;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

@XStreamAlias("player")
public class Player {

    @XStreamAsAttribute 
    private String email;

    @XStreamAsAttribute
    private String password;

    @XStreamAsAttribute
    private String parent;

    @XStreamAsAttribute
    private String seen;

    @XStreamAsAttribute
    private boolean beingOwner;

    @XStreamOmitField
    private String password2;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getHashedPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = hashPassword(password);
    }

    public String getPassword() {
        return password;
    }

    public String getPassword2() {
        return password2;
    }

    public boolean authenticate(String password) {
        return encryptor().checkPassword(password, this.password) || password.equals(this.password);
    }

    public String getHashedPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = hashPassword(password2);
    }

    private String hashPassword(String password) {
        return encryptor().encryptPassword(password);
    }

    public String getSeen() {
        return seen;
    }

    public boolean isBeingOwner() {
        return beingOwner;
    }

    public void setBeingOwner(boolean beingOwner) {
        this.beingOwner = beingOwner;
    }

    private PasswordEncryptor encryptor() {
        return new HexPasswordEncryptor();
    }

    public void appeared() {
        seen = new Date().toString();
    }
}
