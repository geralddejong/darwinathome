// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================

package org.darwinathome.server.persistence;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

@XStreamAlias("game-configuration")
public class Configuration {
    private static final String UNKNOWN = "???UNKNOWN???";

    public static Configuration create() {
        Configuration config = new Configuration();
        config.administratorEmail = config.smtpUser = config.smtpPassword = UNKNOWN;
        config.players = new ArrayList<Player>();
        config.dirty = true;
        config.finish();
        return config;
    }

    @XStreamOmitField
    private boolean dirty;

    @XStreamOmitField
    private Map<String, Player> playerMap;

    private String administratorEmail;

    private String smtpUser;

    private String smtpPassword;

    private String testPlayerEmail;

    private List<Player> players;

    public String getAdministratorEmail() {
        return administratorEmail;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public String getTestPlayerEmail() {
        return testPlayerEmail;
    }

    public Player get(String email) {
        return playerMap.get(email);
    }

    public Player authenticate(String email, String password) {
        Player player = playerMap.get(email);
        return player != null && player.authenticate(password) ? player : null;
    }

    public void put(Player player) {
        Player existing = playerMap.get(player.getEmail());
        if (existing != null) {
            players.remove(existing);
        }
        playerMap.put(player.getEmail(), player);
        players.add(player);
        dirty = true;
    }

    public int getPopulation() {
        return players.size();
    }

    public void finish() {
        playerMap = new HashMap<String, Player>();
        for (Player player : players) {
            playerMap.put(player.getEmail(), player);
        }
    }

    public boolean hasUnknowns() {
        return UNKNOWN.equals(administratorEmail) || UNKNOWN.equals(smtpUser) || UNKNOWN.equals(smtpPassword);
    }

    public void clean() {
        dirty = false;
    }

    public boolean isDirty() {
        return dirty;
    }

}