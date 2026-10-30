package br.com.chamadorapido;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
@Configuration
@EnableWebSocket
public class DeskSocket extends TextWebSocketHandler implements WebSocketConfigurer {
  private final DeskHub hub;
  private final String[] origins;
  public DeskSocket(DeskHub hub,@Value("${app.websocket-origins:http://127.0.0.1:*,http://localhost:*}") String origins){this.hub=hub;this.origins=origins.split(",");}
  @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(this,"/ws/desk").setAllowedOriginPatterns(origins).addInterceptors(new StaffHandshake());}
  @Override public void afterConnectionEstablished(WebSocketSession session){hub.register(session);}
  @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status){hub.drop(session);}
  static final class StaffHandshake implements HandshakeInterceptor {
    @Override public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Map<String,Object> attributes){
      Authentication auth=SecurityContextHolder.getContext().getAuthentication();
      return auth!=null&&auth.getPrincipal() instanceof SecurityConfig.Principal principal&&principal.enabled()&&!"COLABORADOR".equals(principal.role());
    }
    @Override public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Exception exception){}
  }
}
@Component
class DeskHub {
  private final Set<WebSocketSession> sessions=ConcurrentHashMap.newKeySet();
  void register(WebSocketSession session){sessions.add(session);}
  void drop(WebSocketSession session){sessions.remove(session);}
  @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
  public void on(DeskEvent event){
    if (event.ticketId()==null||!event.type().matches("[a-z]+")) return;
    TextMessage message=new TextMessage("{\"type\":\""+event.type()+"\",\"ticketId\":"+event.ticketId()+"}");
    for (WebSocketSession session:sessions) {
      if (!session.isOpen()){sessions.remove(session);continue;}
      try {session.sendMessage(message);} catch (IOException e){sessions.remove(session);}
    }
  }
}
