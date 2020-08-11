package com.MyBookstoreUser.service;

import java.util.Set;

import com.MyBookstoreUser.model.User;
import com.MyBookstoreUser.model.security.PasswordResetToken;
import com.MyBookstoreUser.model.security.UserRole;

public interface UserService {

	PasswordResetToken getPasswordResetToken(final String token);
	
	void createPasswordResetTokenForUser(final User user, final String token);
	
	User findByUsername(String username);
	
	User findByEmail (String email);
	
	User createUser(User user, Set<UserRole> userRoles) throws Exception;
	
	User save(User user);

}
