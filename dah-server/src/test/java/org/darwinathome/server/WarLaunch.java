// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.server;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Bootstrap the jetty server
 * @author Gerald de Jong <geralddejong@gmail.com>
 *
 */
public class WarLaunch {
	public static void main(String... args) throws Exception {
		WebAppContext webAppContext = new WebAppContext("dah-server/target/tetragotchi.war", "/");
		Server server = new Server(2010);
		server.setHandler(webAppContext);
		server.start();
	}
}