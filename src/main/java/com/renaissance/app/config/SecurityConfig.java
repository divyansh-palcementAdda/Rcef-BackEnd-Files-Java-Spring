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
        http
            // CORS (handled by WebConfig, but allow config)
            .cors(cors -> cors.configure(http))

            // CSRF: Disable for APIs & WebSocket
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/api/**",
                    "/ws-notifications/**"
                )
            )

            // Session: Stateless (JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Public Endpoints
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/auth/**",
                    "/api/otp/**"
                ).permitAll()

                // WebSocket Handshake (allow unauth for /info, but filter applies)
                .requestMatchers("/ws-notifications/**").permitAll()

                // Role-based
                .requestMatchers("/api/users/**").hasAnyAuthority("ADMIN", "SUB_ADMIN")
                .requestMatchers("/api/tasks/create", "/api/tasks/bulk-upload")
                    .hasAnyRole("ADMIN", "SUB_ADMIN", "HOD")
                .requestMatchers("/api/tasks/approve/**")
                    .hasAnyRole("ADMIN", "SUB_ADMIN")
                .requestMatchers("/api/tasks/**").authenticated()
                .requestMatchers("/api/ratings/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "SUB_ADMIN", "HOD")

                // All other requests
                .anyRequest().authenticated()
            )

            // JWT Filter: Applied to ALL requests (including WebSocket handshake)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Exception Handling
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) ->
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                .accessDeniedHandler((req, res, accessEx) ->
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied"))
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}