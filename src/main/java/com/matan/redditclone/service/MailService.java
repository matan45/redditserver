package com.matan.redditclone.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.matan.redditclone.exception.SpringRedditException;
import com.matan.redditclone.model.NotificationEmail;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailService {

	private final JavaMailSender mailSender;
	@Value("${sendgrid.apikey}")
	private String SendGridAPIKey;

	public MailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Async
	void sendMailSpringBoot(NotificationEmail notificationEmail) {
		MimeMessagePreparator messagePreparator = mimeMessage -> {
			MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage,
					MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

			messageHelper.setFrom("matan33214@gmail.com");
			messageHelper.setTo(notificationEmail.getRecipient());
			messageHelper.setSubject(notificationEmail.getSubject());
			messageHelper.setText(notificationEmail.getBody(), true);
		};
		try {
			mailSender.send(messagePreparator);
			log.info("Activation email sent!!");
		} catch (MailException e) {
			e.printStackTrace();
			throw new SpringRedditException(
					"Exception occurred when sending mail to " + notificationEmail.getRecipient());
		}
	}
	
	@Async
	void sendmailsendgrid(NotificationEmail notificationEmail) {
		Email from = new Email("SpringReddit@protonmail.com");
		String subject = notificationEmail.getSubject();
		Email to = new Email(notificationEmail.getRecipient());
		Content content = new Content("text/html", notificationEmail.getBody());

		Mail mail = new Mail(from, subject, to, content);

		SendGrid sg = new SendGrid(SendGridAPIKey);
		Request request = new Request();
		try {
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			sg.api(request);
			/*Response response = sg.api(request);
			System.out.println(response.getStatusCode());
			System.out.println(response.getBody());
			System.out.println(response.getHeaders());*/
		} catch (IOException e) {
			throw new SpringRedditException(
					"Exception occurred when sending mail to " + notificationEmail.getRecipient());
		}
	}

}
