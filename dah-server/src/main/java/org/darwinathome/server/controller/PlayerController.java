// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.server.controller;

import freemarker.template.TemplateException;
import org.apache.log4j.Logger;
import org.darwinathome.server.email.EmailSender;
import org.darwinathome.server.email.RegistrationEmailSender;
import org.darwinathome.server.persistence.Player;
import org.darwinathome.server.persistence.Storage;
import org.darwinathome.server.persistence.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

@Controller
public class PlayerController {
    private static final String EMAIL_REGEXP = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[_A-Za-z0-9-]+)";
    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private RegistrationEmailSender registrationEmailSender;

    @Autowired
    private Storage storage;

    @Autowired
    @Qualifier("emailSenderForRegisterNotify")
    private EmailSender notifyEmailSender;

    @Value("#{home.configuration.administratorEmail}")
    private String administratorEmail;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setValidator(new RegistrationFormValidator());
    }

    @RequestMapping("/index.html")
    public String index(
            HttpServletRequest request,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "minutes", required = false) String minutes,
            Map<String, Object> model
    ) throws IOException, TemplateException {
        boolean failureFormat = false;
        boolean failureExists = false;
        boolean success = false;
        Player player = getPlayer();
        model.put("player", player);
        if (minutes != null && player.getParent() == null) {
            storage.getWorldHistory().advanceMinutes(Integer.parseInt(minutes));
        }
        if (email != null) { // todo: can somebody fake this?
            log.info("email submitted: [" + email + "]");
            if (validEmailAddress(email)) {
                if (emailExists(email) && player.getParent() != null) {
                    failureExists = true;
                }
                else {
                    String uri = request.getRequestURL().toString();
                    int lastSlash = uri.lastIndexOf("/");
                    uri = uri.substring(0, lastSlash + 1) + "register.html";
                    registrationEmailSender.sendRegistrationEmail(player, email.toLowerCase(), uri);
                    success = true;
                }
            }
            else {
                failureFormat = true;
            }
        }
        model.put("email", email);
        model.put("success", success);
        model.put("failure", failureFormat || failureExists);
        model.put("failureFormat", failureFormat);
        model.put("failureExists", failureExists);
        return "index";
    }

    @RequestMapping("/login.html")
    public ModelAndView login(
            @RequestParam(value = "email", required = false) String email
    ) {
        if (email == null) {
            email = "";
        }
        return new ModelAndView("login", "email", email);
    }

    @RequestMapping("/logout.html")
    public String logout() {
        removePlayer();
        return "logout";
    }

    @RequestMapping(value = "/register.html", method = RequestMethod.GET)
    public String handleGet(
            @RequestParam(value = "invitation", required = false) String token,
            @ModelAttribute("command") Player player
    ) {
        removePlayer();
        if (token == null) {
            return "redirect:/login.html";
        }
        log.info("creating a member object");
        RegistrationEmailSender.Invitation invitation = registrationEmailSender.getInvitation(token);
        if (invitation != null) {
            player.setEmail(invitation.getEmail());
            player.setParent(invitation.getPlayer().getEmail());
        }
        return "register";
    }

    @RequestMapping(value = "/register.html", method = RequestMethod.POST)
    public String doSubmitAction(
            @ModelAttribute("command") Player player
    ) throws Exception {
        log.info("submitting member " + player.getEmail());
        try {
            Map<String, Object> model = new TreeMap<String, Object>();
            model.put("player", player);
            int beingCount = storage.getPopulation();
            notifyEmailSender.sendEmail(administratorEmail, "noreply@tetragotchi.com", "Tetragotchi #"+beingCount, model);
        }
        catch (Exception e) {
            log.warn("Unable to send email to " + administratorEmail, e);
        }
        storage.put(player);
        return "redirect:/login.html?email="+player.getEmail();
    }

    private boolean emailExists(String email) {
        return (storage.get(email) != null);
    }

    public static boolean validEmailAddress(String emailAddress) {
        return emailAddress.matches(EMAIL_REGEXP);
    }

    public static class RegistrationFormValidator implements Validator {

        public boolean supports(Class aClass) {
            return Player.class.equals(aClass);
        }

        public void validate(Object o, Errors errors) {
            Player player = (Player) o;
            // todo: emailExists should be here?
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "hashedPassword", "password.required", "Password is required");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "hashedPassword2", "password2.required", "Repeat Password is required");
            if (!player.getHashedPassword().equals(player.getHashedPassword2())) {
                errors.rejectValue("password", "password.mismatch", "Passwords do not match");
            }
        }
    }

    private void removePlayer() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private static Player getPlayer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserService.PlayerHolder) {
            UserService.PlayerHolder playerHolder = (UserService.PlayerHolder) authentication.getPrincipal();
            return playerHolder.getPlayer();
        }
        else {
            return null;
        }
    }
}
