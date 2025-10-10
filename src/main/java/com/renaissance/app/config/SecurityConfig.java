package com.renaissance.app.config;

import com.renaissance.app.security.JwtAuthenticationFilter;
import com.renaissance.app.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private final UserDetailsServiceImpl userDetailsService;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(UserDetailsServiceImpl userDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.userDetailsService = userDetailsService;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.cors(cors -> {
		}) // allow CORS, handled by WebConfig
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// ✅ Swagger / OpenAPI should be public
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

						// ✅ Public APIs
						.requestMatchers("/api/auth/**","/api/otp/**").permitAll()

						// ✅ Role-based access
						.requestMatchers("/api/users/**").hasAnyAuthority("ADMIN", "SUB_ADMIN")
						.requestMatchers("/api/tasks/create", "/api/tasks/bulk-upload")
						.hasAnyRole("ADMIN", "SUB_ADMIN", "HOD").requestMatchers("/api/tasks/approve/**")
						.hasAnyRole("ADMIN", "SUB_ADMIN").requestMatchers("/api/tasks/**").authenticated()
						.requestMatchers("/api/ratings/**").hasAnyRole("ADMIN", "SUB_ADMIN")
						.requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "SUB_ADMIN", "HOD")
						// ✅ Any other request requires authentication
						.anyRequest().authenticated())
				// ✅ JWT filter
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				// ✅ Custom error handling
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> response
								.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
						.accessDeniedHandler((request, response, accessDeniedException) -> response
								.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied")))
				.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager(); // uses UserDetailsService + PasswordEncoder automatically
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12); // strong BCrypt hashing
	}
}
