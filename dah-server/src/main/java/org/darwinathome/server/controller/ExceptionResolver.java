// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.server.controller;

import org.darwinathome.server.email.EmailSender;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Snap up the exception and send them to a view
 * 
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class ExceptionResolver implements HandlerExceptionResolver {
    private Logger log = Logger.getLogger(getClass());
    private EmailSender emailSender;
    private String targetEmailAddress;

    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void setTargetEmailAddress(String targetEmailAddress) {
        this.targetEmailAddress = targetEmailAddress;
    }

    public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object, Exception exception) {
        String stackTrace = getStackTrace(exception);
        try {
            Map<String,Object> model = new TreeMap<String,Object>();
            model.put("stackTrace", stackTrace);
            emailSender.sendEmail(targetEmailAddress, "noreply@darwinathome.org", "Exception Occurred", model);
        }
        catch (Exception e) {
            log.warn("Unable to send email to "+targetEmailAddress, e);
        }
        ModelAndView mav = new ModelAndView("exception");
        mav.addObject("exception", exception);
        mav.addObject("stackTrace", stackTrace);
        return mav;
    }

    private String getStackTrace(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        return stringWriter.toString();
    }
}
