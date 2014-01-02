// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.server.email;

import org.apache.log4j.Logger;
import org.darwinathome.body.Being;
import org.darwinathome.universe.World;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class NotificationSender implements World.Listener {
    private Logger log = Logger.getLogger(getClass());
    private String testPlayerEmail, from;
    private EmailSender birthSender, deathSender;

    public void setTestPlayerEmail(String testPlayerEmail) {
        this.testPlayerEmail = testPlayerEmail;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setBirthSender(EmailSender birthSender) {
        this.birthSender = birthSender;
    }

    public void setDeathSender(EmailSender deathSender) {
        this.deathSender = deathSender;
    }

    @Override
    public void beingBorn(Being being) {
        Map<String, Object> model = new TreeMap<String, Object>();
        model.put("being", being);
        try {
            this.birthSender.sendEmail(getEmailAddress(being), from, "Tetragotchi Born!", model);
            log.info("Sent birth notification to "+being);
        }
        catch (Exception e) {
            log.error("Unable to send email to player", e);
        }
    }

    @Override
    public void beingDied(Being being) {
        Map<String, Object> model = new TreeMap<String, Object>();
        model.put("being", being);
        try {
            this.deathSender.sendEmail(getEmailAddress(being), from, "Tetragotchi Perished!", model);
            log.info("Sent death notification to "+being);
        }
        catch (Exception e) {
            log.error("Unable to send email to player", e);
        }
    }

    private String getEmailAddress(Being being) {
        if (testPlayerEmail != null) {
            return testPlayerEmail;
        }
        else {
            return being.getEmail();
        }
    }
}
