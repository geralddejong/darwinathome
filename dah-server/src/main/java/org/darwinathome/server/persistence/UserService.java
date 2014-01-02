// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================

package org.darwinathome.server.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class UserService implements UserDetailsService {

    @Autowired
    private Storage storage;

    public interface PlayerHolder {
        Player getPlayer();
        void setPlayer(Player player);
    }

    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException, DataAccessException {
        try {
            Player player = storage.get(email);
            if (player == null) {
                throw new UsernameNotFoundException("Never heard of "+email);
            }
            player.appeared();
            return new PlayerUserDetails(player);
        }
        catch (Exception e) {
            throw new DataRetrievalFailureException("Storage problem", e);
        }
    }

    private static class PlayerUserDetails implements UserDetails, PlayerHolder {
        private static final long serialVersionUID = -320923097325501004L;
        private Player player;
        private List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

        private PlayerUserDetails(Player player) {
            this.player = player;
            this.authorities.add(new DaoGrantedAuthority());
        }

        public Player getPlayer() {
            return player;
        }

        public void setPlayer(Player player) {
            this.player = player;
        }

        public Collection<GrantedAuthority> getAuthorities() {
            return authorities;
        }

        public String getPassword() {
            return player.getHashedPassword();
        }

        public String getUsername() {
            return player.getEmail();
        }

        public boolean isAccountNonExpired() {
            return true;
        }

        public boolean isAccountNonLocked() {
            return true;
        }

        public boolean isCredentialsNonExpired() {
            return true;
        }

        public boolean isEnabled() {
            return true;
        }

        public String toString() {
            return "User: "+player.getEmail();
        }
    }

    private static class DaoGrantedAuthority implements GrantedAuthority {
        private static final long serialVersionUID = 3815326614077754513L;

        public String getAuthority() {
            return "ROLE_PLAYER";
        }
    }
}
