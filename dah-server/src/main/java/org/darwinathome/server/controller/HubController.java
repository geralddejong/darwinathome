// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.server.controller;

import org.apache.log4j.Logger;
import org.darwinathome.body.Target;
import org.darwinathome.genetics.Genome;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.network.Failure;
import org.darwinathome.network.Hub;
import org.darwinathome.server.persistence.Player;
import org.darwinathome.server.persistence.Storage;
import org.darwinathome.server.persistence.WorldHistory;
import org.darwinathome.universe.SpeechChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

@Controller
public class HubController implements ServletContextAware {
    private static final int TOKEN_SIZE = 24;
    private static final long SESSION_MILLIS = 60000;
    private static final String DOC_DIR = "docs";
    private static final String DOC_FILE_SUFFIX = ".txt";
    private Logger log = Logger.getLogger(getClass());
    private ServletContext servletContext;
    private Map<String, PlayerSession> sessionMap = new ConcurrentHashMap<String, PlayerSession>();

    @Value("#{storage.worldHistory}")
    private WorldHistory worldHistory;

    @Autowired
    private Storage storage;

    @RequestMapping(Hub.AUTHENTICATE_SERVICE)
    public void authenticate(
            @RequestParam(Hub.PARAM_BODY_NAME) String bodyName,
            @RequestParam(Hub.PARAM_PASSWORD) String password,
            OutputStream outputStream
    ) throws IOException {
        log.info(Hub.AUTHENTICATE_SERVICE);
        DataOutputStream dos = new DataOutputStream(outputStream);
        Player player = storage.authenticate(bodyName, password);
        if (player != null) {
            PlayerSession session = new PlayerSession(player);
            sessionMap.put(session.getToken(), session);
            dos.writeUTF(Hub.SUCCESS);
            dos.writeUTF(session.getToken());
        }
        else {
            dos.writeUTF(Hub.FAILURE);
            dos.writeUTF(Failure.AUTHENTICATION.toString());
        }
        dos.close();
    }

    @RequestMapping(Hub.GET_WORLD_SERVICE)
    public void getWorld(
            @PathVariable("session") String session,
            OutputStream outputStream
    ) throws IOException {
        log.info(Hub.GET_WORLD_SERVICE + " " + session);
        DataOutputStream dos = new DataOutputStream(outputStream);
        PlayerSession playerSession = sessionMap.get(session);
        if (playerSession != null) {
            dos.writeUTF(Hub.SUCCESS);
            dos.write(worldHistory.getFrozenWorld().getWorld());
            Iterator<PlayerSession> sessionWalk = sessionMap.values().iterator();
            while (sessionWalk.hasNext()) {
                if (sessionWalk.next().isExpired()) {
                    sessionWalk.remove();
                }
            }
        }
        else {
            dos.writeUTF(Hub.FAILURE);
            dos.writeUTF(Failure.SESSION.toString());
        }
    }

    @RequestMapping(Hub.CREATE_BEING_SERVICE)
    @ResponseBody
    public String createBeing(
            @PathVariable("session") String session,
            @RequestParam(Hub.PARAM_LOCATION_X) double x,
            @RequestParam(Hub.PARAM_LOCATION_Y) double y,
            @RequestParam(Hub.PARAM_LOCATION_Z) double z
    ) {
        log.info(Hub.CREATE_BEING_SERVICE + " " + session);
        try {
            Arrow location = new Arrow(x, y, z);
            worldHistory.createBeing(getEmail(session), location, new Arrow().random());
            return Hub.SUCCESS;
        }
        catch (NoSessionException e) {
            return Hub.FAILURE + Failure.SESSION;
        }
    }

    @RequestMapping(Hub.SET_SPEECH_SERVICE)
    @ResponseBody
    public String setSpeech(
            @PathVariable("session") String session,
            @RequestParam(Hub.PARAM_SPEECH) String speech
    ) {
        log.info(Hub.SET_SPEECH_SERVICE + " " + session);
        try {
            worldHistory.setSpeech(getEmail(session), speech);
            return Hub.SUCCESS;
        }
        catch (NoSessionException e) {
            return Hub.FAILURE + Failure.SESSION;
        }
    }

    @RequestMapping(Hub.GET_SPEECH_SINCE_SERVICE)
    public void getSpeechSince(
            @PathVariable("session") String session,
            @RequestParam(Hub.PARAM_TIME) long time,
            OutputStream outputStream
    ) throws IOException {
        log.info(Hub.GET_SPEECH_SINCE_SERVICE + " " + session);
        DataOutputStream dos = new DataOutputStream(outputStream);
        try {
            List<SpeechChange> changes = worldHistory.getSpeechSince(getEmail(session), time);
            dos.writeUTF(Hub.SUCCESS);
            SpeechChange.write(dos, changes);
        }
        catch (NoSessionException e) {
            dos.writeUTF(Hub.FAILURE);
            dos.writeUTF(Failure.SESSION.toString());
        }
    }

    @RequestMapping(Hub.SET_GENOME_SERVICE)
    @ResponseBody
    public String setGenome(
            @PathVariable("session") String session,
            HttpEntity<byte[]> entity
    ) {
        log.info(Hub.SET_GENOME_SERVICE + " " + session);
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(entity.getBody()));
            Genome genome = Genome.read(dis, worldHistory.getNoise());
            worldHistory.setGenome(getEmail(session), genome);
            return Hub.SUCCESS;
        }
        catch (IOException e) {
            log.warn("Unable to read genome", e);
            return Hub.FAILURE + Failure.MARSHALLING;
        }
        catch (NoSessionException e) {
            return Hub.FAILURE + Failure.SESSION;
        }
    }

    @RequestMapping(Hub.SET_TARGET_SERVICE)
    public void setTarget(
            @PathVariable("session") String session,
            @RequestParam(Hub.PARAM_LOCATION_X) double x,
            @RequestParam(Hub.PARAM_LOCATION_Y) double y,
            @RequestParam(Hub.PARAM_LOCATION_Z) double z,
            @RequestParam(Hub.PARAM_PREY_NAME) String preyName,
            OutputStream outputStream
    ) throws IOException {
        log.info(Hub.SET_TARGET_SERVICE);
        DataOutputStream dos = new DataOutputStream(outputStream);
        try {
            Arrow location = new Arrow(x, y, z);
            Target target = new Target(location, preyName);
            WorldHistory.Frozen frozen = worldHistory.setTarget(getEmail(session), target);
            dos.writeUTF(Hub.SUCCESS);
            dos.write(frozen.getWorld());
        }
        catch (NoSessionException e) {
            dos.writeUTF(Hub.FAILURE);
            dos.writeUTF(Failure.SESSION.toString());
        }
    }

    @RequestMapping(Hub.DOCUMENTATION_SERVICE)
    @ResponseBody
    public void getDocumentation(
            @RequestParam(Hub.PARAM_TITLE) String title,
            OutputStream outputStream
    ) {
        log.info(Hub.DOCUMENTATION_SERVICE);
        DataOutputStream dos = new DataOutputStream(outputStream);
        File docsDirectory = new File(servletContext.getRealPath(DOC_DIR));
        try {
            if (!docsDirectory.isDirectory()) {
                log.fatal("Docs directory not found!");
                dos.writeUTF(Hub.FAILURE);
                dos.writeUTF(Failure.SYSTEM.toString());
                return;
            }
            if (title.isEmpty()) {
                File[] textFiles = docsDirectory.listFiles(new TextFilter());
                dos.writeUTF(Hub.SUCCESS);
                dos.writeUTF(title);
                dos.writeShort(textFiles.length);
                for (File docFile : textFiles) {
                    dos.writeUTF(docFile.getName().substring(0, docFile.getName().length() - DOC_FILE_SUFFIX.length()));
                }
            }
            else {
                File docFile = new File(docsDirectory, title + DOC_FILE_SUFFIX);
                if (!docFile.exists()) {
                    dos.writeUTF(Hub.FAILURE);
                    dos.writeUTF(Failure.SYSTEM.toString());
                    return;
                }
                List<String> lines = getLines(docFile);
                dos.writeUTF(Hub.SUCCESS);
                dos.writeUTF(title);
                dos.writeShort(lines.size());
                for (String line : lines) {
                    dos.writeUTF(line);
                }
            }
        }
        catch (IOException e) {
            log.error("Problem delivering documentation", e);
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    private static class NoSessionException extends Exception {
    }

    private String getEmail(String session) throws NoSessionException {
        PlayerSession playerSession = sessionMap.get(session);
        if (playerSession == null) {
            throw new NoSessionException();
        }
        playerSession.used();
        return playerSession.getEmail();
    }

    private List<String> getLines(File docFile) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new FileReader(docFile));
        String line;
        while ((line = in.readLine()) != null) {
            lines.add(line);
        }
        in.close();
        return lines;
    }

    private class TextFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(DOC_FILE_SUFFIX);
        }
    }

    private class PlayerSession {
        private Player player;
        private long lastUsed = System.currentTimeMillis();
        private String token;

        private PlayerSession(Player player) {
            this.player = player;
            StringBuilder out = new StringBuilder();
            for (int walk = 0; walk < TOKEN_SIZE; walk++) {
                out.append((char) ('a' + (int) (Math.random() * 26)));
            }
            token = out.toString();
        }

        private void used() {
            lastUsed = System.currentTimeMillis();
            log.info("session " + token + " is " + player.getEmail());
        }

        public String getEmail() {
            return player.getEmail();
        }

        public String getToken() {
            return token;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastUsed > SESSION_MILLIS;
        }
    }
}