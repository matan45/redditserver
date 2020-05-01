package com.matan.redditclone.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matan.redditclone.dto.AuthenticationResponse;
import com.matan.redditclone.dto.LoginRequest;
import com.matan.redditclone.dto.RefreshTokenRequest;
import com.matan.redditclone.dto.RegisterRequest;

import com.matan.redditclone.exception.SpringRedditException;
import com.matan.redditclone.model.NotificationEmail;
import com.matan.redditclone.model.User;
import com.matan.redditclone.model.VerificationToken;
import com.matan.redditclone.repository.UserRepository;
import com.matan.redditclone.repository.VerificationTokenRepository;
import com.matan.redditclone.security.JwtProvider;

import freemarker.template.TemplateException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final VerificationTokenRepository verificationTokenRepository;
	private final AuthenticationManager authenticationManager;
	private final JwtProvider jwtProvider;
	private final MailContentBuilder mailContentBuilder;
	private final MailService mailService;
	private final RefreshTokenService refreshTokenService;
	private final String ACTIVATION_EMAIL = "http://localhost:8080/api/auth/accountVerification";

	@Transactional
	public void signup(RegisterRequest registerRequest) {
		// TODO: check if the email is already exit
		User user = new User();
		user.setUsername(registerRequest.getUsername());
		user.setEmail(registerRequest.getEmail());
		user.setPassword(encodePassword(registerRequest.getPassword()));
		user.setCreated(Instant.now());
		user.setEnabled(false);

		userRepository.save(user);

		String token = generateVerificationToken(user);

		String message;
		try {
			message = mailContentBuilder.build(
					"Thank you for signing up to Spring Reddit, please click on the below url to activate your account: "
							+ ACTIVATION_EMAIL + "/" + token);
			mailService.sendmailsendgrid(new NotificationEmail("Please Activate your account", user.getEmail(), message));
		} catch (IOException | TemplateException e) {
			throw new SpringRedditException("Exception occurred when sending mail signup " + user.getEmail());
		}

	}

	@Transactional(readOnly = true)
	public User getCurrentUser() {
		org.springframework.security.core.userdetails.User principal = (org.springframework.security.core.userdetails.User) SecurityContextHolder
				.getContext().getAuthentication().getPrincipal();
		return userRepository.findByUsername(principal.getUsername())
				.orElseThrow(() -> new UsernameNotFoundException("User name not found - " + principal.getUsername()));
	}

	private String generateVerificationToken(User user) {
		String token = UUID.randomUUID().toString();
		VerificationToken verificationToken = new VerificationToken();
		verificationToken.setToken(token);
		verificationToken.setUser(user);
		verificationTokenRepository.save(verificationToken);
		return token;
	}

	private String encodePassword(String password) {
		return passwordEncoder.encode(password);
	}

	public void verifyAccount(String token) {
		Optional<VerificationToken> verificationTokenOptional = verificationTokenRepository.findByToken(token);
		verificationTokenOptional.orElseThrow(() -> new SpringRedditException("Invalid Token"));
		fetchUserAndEnable(verificationTokenOptional.get());
	}

	@Transactional
	private void fetchUserAndEnable(VerificationToken verificationToken) {
		String username = verificationToken.getUser().getUsername();
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new SpringRedditException("User Not Found with id - " + username));
		user.setEnabled(true);
		userRepository.save(user);
	}

	public AuthenticationResponse login(LoginRequest loginRequest) {
		Authentication authenticate = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authenticate);
		String Token = jwtProvider.generateToken(authenticate);
		return AuthenticationResponse.builder().authenticationToken(Token)
				.refreshToken(refreshTokenService.generateRefreshToken().getToken())
				.expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
				.username(loginRequest.getUsername()).build();
	}

	public AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
		refreshTokenService.validateRefreshToken(refreshTokenRequest.getRefreshToken());
		String token = jwtProvider.generateTokenWithUserName(refreshTokenRequest.getUsername());
		return AuthenticationResponse.builder().authenticationToken(token)
				.refreshToken(refreshTokenRequest.getRefreshToken())
				.expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
				.username(refreshTokenRequest.getUsername()).build();
	}
	
	public boolean isLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated();
    }
}