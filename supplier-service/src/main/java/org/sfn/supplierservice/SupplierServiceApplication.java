package org.sfn.supplierservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.CrossOrigin;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.stream.Stream;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class Supplier{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
}

@RepositoryRestResource
@CrossOrigin(origins = "*")
interface SupplierRepository extends JpaRepository<Supplier,Long> { }
@SpringBootApplication
public class SupplierServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupplierServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner start(SupplierRepository supplierRepository){
        return args->{
            Stream.of("SUPP01","SUPP02","SUPP3").forEach(s->{
                supplierRepository.save(new Supplier(null,s,"contact@"+s+".com"));
            });
        };
    }
}


@Configuration
class KeycloakConfig{
    @Bean
    KeycloakSpringBootConfigResolver configResolver(){
        return new KeycloakSpringBootConfigResolver();
    }

}

@KeycloakConfiguration
class KeycloakSpringSecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(keycloakAuthenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.authorizeRequests().antMatchers("/suppliers/**").hasAuthority("app-manager");
    }
}
