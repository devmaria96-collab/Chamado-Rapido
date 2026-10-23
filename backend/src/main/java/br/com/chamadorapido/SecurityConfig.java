package br.com.chamadorapido;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.filter.OncePerRequestFilter;
@Configuration
public class SecurityConfig {
  public record Principal(Long id,String email,String password,String role,int version,boolean enabled) implements UserDetails {
    public String getUsername(){return email;}
    public String getPassword(){return password;}
    public Collection<? extends GrantedAuthority> getAuthorities(){return List.of(new SimpleGrantedAuthority("ROLE_"+role));}
    public boolean isEnabled(){return enabled;}
    public boolean isAccountNonExpired(){return true;}
    public boolean isAccountNonLocked(){return true;}
    public boolean isCredentialsNonExpired(){return true;}
  }
  @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
  @Bean UserDetailsService userDetailsService(UserRepository users){return email->{var u=users.findByEmailIgnoreCase(email.trim()).orElseThrow(()->new UsernameNotFoundException("Credenciais inválidas"));return new Principal(u.id,u.email,u.passwordHash,u.role.name(),u.authVersion,u.active);};}
  private final LoginThrottle throttle=new LoginThrottle();
  @Bean SecurityFilterChain security(HttpSecurity http, UserService accounts) throws Exception {
    http.csrf(c->c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
      .authorizeHttpRequests(a->a.requestMatchers("/api/auth/csrf","/api/public/info","/api/public/sectors").permitAll().requestMatchers("/api/**").authenticated().anyRequest().permitAll())
      .formLogin(f->f.loginProcessingUrl("/api/auth/login").usernameParameter("email").successHandler((req,res,auth)->{throttle.clear(req);json(res,200,"{\"ok\":true}");}).failureHandler((req,res,e)->{throttle.fail(req);json(res,401,"{\"message\":\"Setor ou senha inválidos.\"}");}).permitAll())
      .logout(l->l.logoutUrl("/api/auth/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,auth)->json(res,200,"{\"ok\":true}")))
      .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->json(res,401,"{\"message\":\"Sua sessão expirou. Entre novamente.\"}")).accessDeniedHandler((req,res,ex)->json(res,403,"{\"message\":\"Operação não autorizada ou sessão de segurança expirada. Atualize a página.\"}")))
      .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'")))
      .addFilterBefore(new SectorLoginFilter(accounts),UsernamePasswordAuthenticationFilter.class)
      .addFilterBefore(throttle,UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
  static void json(HttpServletResponse r,int code,String text)throws IOException{r.setStatus(code);r.setContentType("application/json;charset=UTF-8");r.getWriter().write(text);}
  static final class SectorLoginFilter extends OncePerRequestFilter {
    private final UserService accounts;
    SectorLoginFilter(UserService accounts){this.accounts=accounts;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
      if("POST".equals(req.getMethod())&&"/api/auth/login".equals(req.getServletPath())&&req.getParameter("sector")!=null){
        String resolved=accounts.emailForSector(req.getParameter("sector"));
        if(resolved.isBlank())resolved="invalid@invalid.invalid";
        String email=resolved;
        chain.doFilter(new HttpServletRequestWrapper(req){
          @Override public String getParameter(String name){return "email".equals(name)?email:super.getParameter(name);}
          @Override public String[] getParameterValues(String name){return "email".equals(name)?new String[]{email}:super.getParameterValues(name);}
          @Override public Map<String,String[]> getParameterMap(){Map<String,String[]> map=new HashMap<>(super.getParameterMap());map.put("email",new String[]{email});return map;}
        },res);
        return;
      }
      chain.doFilter(req,res);
    }
  }
  /** Limite local por IP. Em cluster, aplicar limite compartilhado no proxy. */
  static final class LoginThrottle extends OncePerRequestFilter {
    record Counter(int count,long until){}
    private final ConcurrentHashMap<String,Counter> failures=new ConcurrentHashMap<>();
    void fail(HttpServletRequest r){long now=System.currentTimeMillis();failures.compute(r.getRemoteAddr(),(k,c)->c==null||c.until<now?new Counter(1,now+300000):new Counter(c.count+1,c.until));}
    void clear(HttpServletRequest r){failures.remove(r.getRemoteAddr());}
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
      if(req.getMethod().equals("POST")&&req.getServletPath().equals("/api/auth/login")){
        long now=System.currentTimeMillis();failures.entrySet().removeIf(e->e.getValue().until<now);
        Counter c=failures.get(req.getRemoteAddr());
        if(c!=null&&c.count>=8){res.setHeader("Retry-After","300");json(res,429,"{\"message\":\"Muitas tentativas. Aguarde cinco minutos.\"}");return;}
      }
      chain.doFilter(req,res);
    }
  }
}
