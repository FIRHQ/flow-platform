/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.api.util;

import com.flow.platform.api.domain.EmailSettingContent;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * @author yh@firim
 */
public class SmtpUtil {

    public static void sendEmail(EmailSettingContent emailSetting, String acceptor, String subject, String body) {
        Properties props = buildProperty(emailSetting);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String username = null;
                String password = null;
                if (emailSetting.isAuthenticated()) {
                    username = emailSetting.getUsername();
                    password = emailSetting.getPassword();
                }
                return new PasswordAuthentication(username, password);
            }
        });
        try {

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailSetting.getSender()));
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(acceptor));

            message.setSubject(subject, "utf8");
            message.setContent(body, "text/html;charset=utf8");
            Transport.send(message);

        } catch (Throwable throwable) {
        }
    }

    /**
     * authentication
     */
    public static Boolean authentication(EmailSettingContent emailSetting) {

        Properties props = buildProperty(emailSetting);

        Session session = Session.getInstance(props, null);
        try {
            Transport transport = session.getTransport("smtp");
            String username = null;
            String password = null;
            if (emailSetting.isAuthenticated()) {
                username = emailSetting.getUsername();
                password = emailSetting.getPassword();
            }
            transport.connect(emailSetting.getSmtpUrl(), emailSetting.getSmtpPort(), username,
                password);
            transport.close();
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static Properties buildProperty(EmailSettingContent emailSetting) {
        Properties props = new Properties();
        props.put("mail.smtp.host", emailSetting.getSmtpUrl());
        props.put("mail.smtp.socketFactory.port", emailSetting.getSmtpPort().toString());
        props.put("mail.smtp.socketFactory.class",
            "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", emailSetting.getSmtpPort().toString());
        return props;
    }
}
