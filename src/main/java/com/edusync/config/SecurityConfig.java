package com.edusync.config;

import com.edusync.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for EduSync.
 * - JWT stateless authentication
 * - Role-based route protection
 * - CORS enabled for HTML frontend (any origin in dev)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Value("${edusync.cors.allowed-origins}")
    private String allowedOriginsRaw;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ─── CORS ───
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ─── CSRF disabled (stateless JWT API) ───
            .csrf(csrf -> csrf.disable())

            // ─── Route authorization ───
            .authorizeHttpRequests(auth -> auth

                // Public routes — no token needed
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/frontend-connected.html",
                    "/college-platform-connected.html",
                    "/college-platform-review-no-login.html",
                    "/static/**",
                    "/assets/**",
                    "/*.html",
                    "/*.css",
                    "/*.js",
                    "/*.ico",
                    "/*.png",
                    "/*.jpg",
                    "/*.svg",
                    "/favicon.ico",
                    "/api/auth/**",
                    "/api/users/faculty-advisors",
                    "/api/notifications/**",
                    "/api/notices/public",
                    "/api/placement/drives/public",
                    "/api/leaves/*/pdf",
                    "/api/leaves/*/pdf/**",
                    "/api/reports/**",
                    "/api/ai/**"
                ).permitAll()

                // Admin only
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Principal
                .requestMatchers("/api/principal/**").hasAnyRole("PRINCIPAL", "ADMIN")

                // HOD
                .requestMatchers("/api/hod/**").hasAnyRole("HOD", "ADMIN")

                // Student & Faculty Advisor Requests (students submit/check status, faculty manage)
                .requestMatchers("/api/advisor/requests/**").authenticated()

                // Faculty, HOD, Principal, Admin (Mentorship Hub & Early-Warning Monitoring)
                .requestMatchers("/api/faculty/**").hasAnyRole("FACULTY", "HOD", "PRINCIPAL", "ADMIN")
                .requestMatchers("/api/advisor/**").hasAnyRole("FACULTY", "HOD", "PRINCIPAL", "ADMIN")

                // Placement officer
                .requestMatchers("/api/placement/manage/**").hasAnyRole("PLACEMENT_OFFICER", "ADMIN")

                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/industry/internships/open").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/industry/internships/my-applications").hasRole("STUDENT")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/industry/internships/*/apply").hasRole("STUDENT")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/industry/overview").hasAnyRole("INDUSTRIAL_CONNECT_HEAD", "PRINCIPAL", "HOD", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/industry/companies", "/api/industry/companies/**").hasAnyRole("INDUSTRIAL_CONNECT_HEAD", "PLACEMENT_OFFICER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/industry/visits/propose").hasAnyRole("FACULTY", "HOD")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/industry/visits/*/hod-action").hasAnyRole("HOD", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/industry/visits/*/principal-action").hasAnyRole("PRINCIPAL", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/industry/visits", "/api/industry/visits/**").hasAnyRole("FACULTY", "HOD", "PRINCIPAL", "INDUSTRIAL_CONNECT_HEAD", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/industry/reports/**").hasAnyRole("INDUSTRIAL_CONNECT_HEAD", "PRINCIPAL", "HOD", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/industry/lectures", "/api/industry/lectures/**").hasAnyRole("FACULTY", "HOD", "PRINCIPAL", "INDUSTRIAL_CONNECT_HEAD", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/industry/projects", "/api/industry/projects/**").hasAnyRole("FACULTY", "HOD", "PRINCIPAL", "INDUSTRIAL_CONNECT_HEAD", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/industry/advisory", "/api/industry/advisory/**").hasAnyRole("FACULTY", "HOD", "PRINCIPAL", "INDUSTRIAL_CONNECT_HEAD", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/industry/alumni", "/api/industry/alumni/**").hasAnyRole("FACULTY", "HOD", "PRINCIPAL", "INDUSTRIAL_CONNECT_HEAD", "ADMIN")
                .requestMatchers("/api/industry/**").hasAnyRole("INDUSTRIAL_CONNECT_HEAD", "ADMIN")

                // All authenticated users
                .anyRequest().authenticated()
            )

            // ─── Stateless session (JWT) ───
            .sessionManagement(sess ->
                sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Return JSON for auth failures instead of Spring Security's blank 403 page.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"success\":false,\"message\":\"Authentication required. Please log in.\",\"data\":null}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"success\":false,\"message\":\"Access denied: your account does not have permission for this action.\",\"data\":null}"
                    );
                })
            )

            // ─── Auth provider ───
            .authenticationProvider(authenticationProvider())

            // ─── JWT filter before username/password filter ───
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow frontend origins (HTML file opened locally, IntelliJ preview, or dev server)
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "Accept",
            "X-Requested-With", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
