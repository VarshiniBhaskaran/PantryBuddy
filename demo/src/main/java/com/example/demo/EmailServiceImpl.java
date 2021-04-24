package com.example.demo;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailServiceImpl {
	
	static Session setPropertiesAndFetchSession() throws Exception {
		Properties props = getEmailProperties();
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("your.pantrybuddy@gmail.com", "pantryBuddy@135");
			}
		});
		return session;
	}
	
	static void sendWelcomeEmail(String emailId) throws Exception{
		Session session = setPropertiesAndFetchSession();
		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress("noreply-pantrybuddy", false));
		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailId, false));
		msg.setSubject("Welcome to Pantry Buddy");

		// message body.
		Multipart mp = new MimeMultipart("related");

		String cid = "Welcome";

		MimeBodyPart pixPart = new MimeBodyPart();
		pixPart.attachFile("PantryBuddy.png");
		pixPart.setContentID("<" + cid + ">");
		pixPart.setDisposition(MimeBodyPart.INLINE);

		MimeBodyPart textPart = new MimeBodyPart();
		textPart.setText("<html>" + "<div><img src=cid:Welcome style=width:100%; height:auto; border:none;\"/></div></html>", "US-ASCII", "html");

		// Attach text and image to message body
		mp.addBodyPart(textPart);
		mp.addBodyPart(pixPart);
		msg.setContent(mp);

		Transport.send(msg);
	}

	private static Properties getEmailProperties() {
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
		return props;
	}
}