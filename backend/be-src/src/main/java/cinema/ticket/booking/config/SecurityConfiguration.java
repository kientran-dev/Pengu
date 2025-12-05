package cinema.ticket.booking.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import cinema.ticket.booking.exception.CustomAccessDeniedHandler;
import cinema.ticket.booking.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
		securedEnabled = true,
		jsr250Enabled = true,
		prePostEnabled = true)
public class SecurityConfiguration {
	
	@Autowired
	private JwtAuthenticationFilter jwtAuthFilter;

	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors()
				.and()
			.csrf()
	        .disable()
	        .authorizeHttpRequests()
				.requestMatchers("/api/auth/**", "/api/statistics/**") // Thêm endpoint statistics vào đây cho rõ ràng
				.permitAll()
			.requestMatchers("/api/auth/**")
				.permitAll()
			.anyRequest()
				.permitAll()
	        .and()
	            .exceptionHandling()
	            	.authenticationEntryPoint(customAccessDeniedHandler())
	        .and()
	        	.sessionManagement()
	          	.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	        .and()
        		.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        	;
		
		return http.build();
	} 
	
	@Bean
	public AuthenticationEntryPoint customAccessDeniedHandler() {
		return new CustomAccessDeniedHandler();
	}
}

