// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.server.email;

import freemarker.template.TemplateException;
import org.darwinathome.server.persistence.Player;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle registration email sending
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class RegistrationEmailSender {
    private static final int TOKEN_LENGTH = 32;
    private static final long MAX_TOKEN_AGE = 1000L * 60 * 60 * 24;
    private static Map<String, Invitation> LIVE_TOKENS = new ConcurrentHashMap<String, Invitation>();
    private EmailSender emailSender;
    private String from, subject;

    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String sendRegistrationEmail(Player player, String emailAddress, String confirmationUrl) throws IOException, TemplateException {
        Invitation invitation = new Invitation(player, emailAddress);
        Map<String, Object> model = new TreeMap<String, Object>();
        model.put("player", player);
        model.put("uniqueToken", invitation.getUnique());
        model.put("confirmationUrl", confirmationUrl);
        emailSender.sendEmail(emailAddress, from, subject, model);
        LIVE_TOKENS.put(invitation.getUnique(), invitation);
        return invitation.getUnique();
    }

    public Invitation getInvitation(String tokenString) {
        Invitation invitation = LIVE_TOKENS.get(tokenString);
        Iterator<Map.Entry<String, Invitation>> walk = LIVE_TOKENS.entrySet().iterator();
        while (walk.hasNext()) {
            Map.Entry<String, Invitation> entry = walk.next();
            if (entry.getValue().isOlderThan(MAX_TOKEN_AGE)) {
                walk.remove();
            }
        }
        return invitation;
    }

    public class Invitation {
        private Player player;
        private String email;
        private String unique;
        private long created;

        private Invitation(Player player, String email) {
            this.player = player;
            this.email = email;
            StringBuilder out = new StringBuilder(TOKEN_LENGTH);
            for (int walk = 0; walk < TOKEN_LENGTH; walk++) {
                out.append((char) ('A' + ((int) (Math.random() * 26))));
            }
            this.unique = out.toString();
            this.created = System.currentTimeMillis();
        }

        public String getEmail() {
            return email;
        }

        public String getUnique() {
            return unique;
        }

        public Player getPlayer() {
            return player;
        }

        public boolean isOlderThan(long time) {
            return (System.currentTimeMillis() - created) > time;
        }
    }
}
