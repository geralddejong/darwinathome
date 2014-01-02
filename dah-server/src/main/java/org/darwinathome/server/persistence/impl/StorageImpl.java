// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================

package org.darwinathome.server.persistence.impl;

import org.apache.log4j.Logger;
import org.darwinathome.server.persistence.Home;
import org.darwinathome.server.persistence.Player;
import org.darwinathome.server.persistence.Storage;
import org.darwinathome.server.persistence.WorldHistory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Stores players using XStream
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class StorageImpl implements Storage {
    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private Home home;

    @Autowired
    private WorldHistory worldHistory;

    @Override
    public synchronized Player get(String email) {
        Player player = home.getConfiguration().get(email);
        if (player != null) {
            player.setBeingOwner(worldHistory.beingExists(email));
        }
        return player;
    }

    @Override
    public synchronized Player authenticate(String email, String password) {
        Player player = home.getConfiguration().authenticate(email, password);
        if (player != null) {
            player.setBeingOwner(worldHistory.beingExists(email));
        }
        return player;
    }

    @Override
    public synchronized void put(Player player) {
        home.getConfiguration().put(player);
    }

    @Override
    public int getPopulation() {
        return home.getConfiguration().getPopulation();
    }

    @Override
    public WorldHistory getWorldHistory() { // WH itself is synchronized
        return worldHistory;
    }

}
