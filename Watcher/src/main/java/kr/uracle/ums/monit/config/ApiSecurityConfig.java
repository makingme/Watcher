package kr.uracle.ums.monit.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;



@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter {

	/**
	 * Spring-security 예외처리
	 */
	@Override
	public void configure(WebSecurity web) throws Exception {

		web.ignoring()
			.antMatchers(
				"**",
				"/api/**/**"
			);
	}

	
	
}
