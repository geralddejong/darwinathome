// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.server.email;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class EmailSender {

    @Autowired
    private MailSender mailSender;

    private Template template;

    public void setTemplate(String templateName) throws IOException {
        this.template = getResourceTemplate(templateName);
    }

    public void sendEmail(String toEmail, String fromEmail, String subject, Map<String,Object> model) throws IOException, TemplateException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(subject);
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        String emailText = createEmailText(model);
        message.setText(emailText);
        mailSender.send(message);
    }

    private String createEmailText(Map<String,Object> model) throws IOException, TemplateException {
        StringWriter out = new StringWriter();
        template.process(model, out);
        return out.toString();
    }

    private Template getResourceTemplate(String fileName) throws IOException {
        return getTemplate(fileName, new InputStreamReader(getClass().getResourceAsStream(fileName)));
    }

    private Template getTemplate(String name, Reader reader) throws IOException {
        Configuration configuration = new Configuration();
        configuration.setLocale(new Locale("nl"));
        configuration.setObjectWrapper(new DefaultObjectWrapper());
        return new Template(name, reader, configuration);
    }
}
