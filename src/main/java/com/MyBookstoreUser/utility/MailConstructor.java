package com.MyBookstoreUser.utility;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import com.MyBookstoreUser.model.User;

@Component
public class MailConstructor {

	@Autowired
	private Environment env;
	
	
	public SimpleMailMessage constructResetTokenEmail(
			String contextPath, Locale locale, String token, User user, String password) 
	{
		
		String url = contextPath + "/newUser?token="+token;
		String message = "Please click on this link to verify your email and edit your personal information.\n\nYour temporary generated password is: "+password+"\n";
		SimpleMailMessage email = new SimpleMailMessage();
		email.setTo(user.getEmail());
		email.setSubject("Online Bookstore - Account Verification");
		email.setText(message+url);
		email.setFrom(env.getProperty("support.email"));
		return email;
	}
	
}
