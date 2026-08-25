package com.techcombank.qe.sut.capability.authz;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * TST-040 Spring Security resource-server wiring for the reference SUT.
 *
 * <p>Stateless bearer-token auth only -- no form login, no HTTP Basic, no
 * session. Only {@code /protected/**} is access-controlled; every other path
 * (including {@code /auth/**}, and this SUT's own {@code /_capabilities}/
 * {@code /_test/defect} meta-endpoints from earlier tasks) is
 * {@code permitAll} -- TST-040 owns the authorization matrix on the
 * protected surface, not a blanket lockdown of the whole reference service.
 *
 * <p>{@link JwtAuthenticationFilter} and {@link AuthzDecisionFilter} are
 * constructed directly here, not autowired as {@code @Component} beans: a
 * filter added to this chain via {@code addFilterBefore} that is *also* a
 * component-scanned {@code Filter} bean gets registered a second time by
 * Spring Boot's generic servlet-filter auto-registration, running it twice
 * per request -- once bare, once again inside Spring Security's own
 * {@code FilterChainProxy}. Constructing them inline avoids that.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/protected/read").hasAnyRole("READER", "WRITER", "ADMIN")
                .requestMatchers("/protected/write").hasAnyRole("WRITER", "ADMIN")
                .requestMatchers("/protected/admin").hasRole("ADMIN")
                .anyRequest().permitAll())
            .exceptionHandling(ex -> ex
                // setStatus, never sendError: sendError commits the response immediately,
                // and AuthzDecisionFilter (added below) needs the response still open
                // afterwards so it can add X-Authz-Decision once the outcome is resolved.
                .authenticationEntryPoint((request, response, authException) -> response.setStatus(401))
                .accessDeniedHandler((request, response, accessDeniedException) -> response.setStatus(403)))
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new AuthzDecisionFilter(), ExceptionTranslationFilter.class);

        return http.build();
    }
}
