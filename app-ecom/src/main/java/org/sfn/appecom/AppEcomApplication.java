package org.sfn.appecom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.client.KeycloakClientRequestFactory;
import org.keycloak.adapters.springsecurity.client.KeycloakRestTemplate;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;
    private String name ;
    private double price ;
}

interface ProoductRepository extends JpaRepository<Product,Long>{
}
@Controller
class ProductController{
    private ProoductRepository prooductRepository;
    private AdapterDeploymentContext adapterDeployementContext;

    ProductController(ProoductRepository prooductRepository, AdapterDeploymentContext adapterDeployementContext) {
        this.prooductRepository = prooductRepository;
        this.adapterDeployementContext = adapterDeployementContext;
    }
    @GetMapping(path = "/")
    public String index(){
        return "index";
    }

    @GetMapping(path = "/products")
    public String products(Model model){
        model.addAttribute("products",prooductRepository.findAll());
        return "products";
    }
    @GetMapping(path = "/logout")
    public String logout(HttpServletRequest request) throws ServletException {
        request.logout();
        return "redirect:/";
    }
    @GetMapping(path = "/changePassword")
    public String changePassword(RedirectAttributes attributes , HttpServletRequest request, HttpServletResponse response) throws ServletException {
        HttpFacade facade = new SimpleHttpFacade(request,response);
        KeycloakDeployment deployment = adapterDeployementContext.resolveDeployment(facade);
        attributes.addAttribute("referrer",deployment.getResourceName());
        attributes.addAttribute("referrer_uri",request.getHeader("referrer"));
        return "redirect:"+deployment.getAccountUrl();
    }
}

@Configuration
class KeycloakConfig{
    @Bean
    KeycloakSpringBootConfigResolver configResolver(){
        return new KeycloakSpringBootConfigResolver();
    }
    @Bean
    KeycloakRestTemplate keycloakRestTemplate( KeycloakClientRequestFactory keycloakClientRequestFactory){
        return new KeycloakRestTemplate(keycloakClientRequestFactory);
    }
}

@SpringBootApplication
public class AppEcomApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppEcomApplication.class, args);
    }
    @Bean
    CommandLineRunner start(ProoductRepository prooductRepository){
        return args -> {
            prooductRepository.save(new Product(null,"product01",100));
            prooductRepository.save(new Product(null,"product02",200));
            prooductRepository.save(new Product(null,"product03",300));
            prooductRepository.findAll().forEach(p->{
                System.out.println(p.toString());
            });
        };
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
        http.authorizeRequests().antMatchers("/products/**").authenticated();
    }
}


@Controller
class SupplierController{
    @Autowired
    private KeycloakRestTemplate keycloakRestTemplate ;
    @GetMapping(path = "/suppliers")
    public String suppliers(Model model){
        ResponseEntity<PagedModel<Supplier>> response=
                keycloakRestTemplate.exchange("http://localhost:8083/suppliers", HttpMethod.GET, null, new ParameterizedTypeReference<PagedModel<Supplier>>() {});
        model.addAttribute("suppliers",response.getBody().getContent());
        return "suppliers";
    }
    @ExceptionHandler(Exception.class)
    public String  exceptionHandler(){
        return "errors";
    }

}
@Data
class Supplier{
    private Long id;
    private String name;
    private String email;
}