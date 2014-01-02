// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================

package org.darwinathome.server.email;

import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * A mail sender which is clever about using Gmail
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class EnhancedJavaMailSenderImpl extends JavaMailSenderImpl {

    @Override
    public void setUsername(String userName) {
        if (userName.contains("gmail.com")) {
            Properties props = new Properties();
            props.put("mail.smtp.host","smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            super.setJavaMailProperties(props);
        }
        super.setUsername(userName);
    }
}
