// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.server.controller;

import org.apache.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

@Controller
public class WebstartController implements ServletContextAware {
    private Logger log = Logger.getLogger(getClass());
    private ServletContext servletContext;

    @RequestMapping("/{jarFile}.jar")
    public void jarFile(@PathVariable String jarFile, HttpServletResponse response) {
        log.info("Request for " + jarFile);
        String realFileName = servletContext.getRealPath(jarFile + ".jar");
        File realFile = new File(realFileName);
        response.setContentType("application/java-archive");
        response.setContentLength((int) realFile.length());
        try {
            InputStream in = new FileInputStream(realFileName);
            OutputStream out = response.getOutputStream();
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
            }
            finally {
                in.close();
            }
        }
        catch (SocketException e) {
            log.info("While writing jar: "+e.toString());
        }
        catch (EOFException e) {
            log.info("Broken pipe while requesting "+jarFile);
        }
        catch (IOException e) {
            log.warn("Problem requesting "+jarFile, e);
        }
    }

    @RequestMapping("/{user}/{pass}/tetragotchi.jnlp")
    public ResponseEntity<String> jnlpFile(HttpServletRequest request, @PathVariable String user, @PathVariable String pass) throws IOException {
        String from = String.format("%s/%s/tetragotchi.jnlp", user, pass);
        String base = request.getRequestURL().toString().replace(from, "");
        String realFileName = servletContext.getRealPath("tetragotchi.jnlp");
        BufferedReader in = new BufferedReader(new FileReader(realFileName));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            if (line.contains("@@")) {
                line = line.replace("@@BASE@@", base);
                line = line.replace("@@USER@@", user);
                line = line.replace("@@PASS@@", pass);
            }
            out.append(line).append('\n');
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "java-jnlp-file"));
        return new ResponseEntity<String>(out.toString(), headers, HttpStatus.OK);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

}