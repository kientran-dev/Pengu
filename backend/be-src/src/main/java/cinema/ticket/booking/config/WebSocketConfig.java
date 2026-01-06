package cinema.ticket.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Client sẽ kết nối vào đường dẫn này: http://localhost:8080/ws-cinema
        registry.addEndpoint("/ws-cinema")
                .setAllowedOriginPatterns("*") // Cho phép Frontend (React/Vue/Thymeleaf) kết nối
                .withSockJS(); // Hỗ trợ trình duyệt cũ
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix cho các tin nhắn Server gửi ra (Client đăng ký lắng nghe cái này)
        registry.enableSimpleBroker("/topic");

        // Prefix cho các tin nhắn Client gửi lên (Mapping vào Controller)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
