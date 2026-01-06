package cinema.ticket.booking.controller;

import cinema.ticket.booking.request.SocketSeatRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class SeatWebSocketController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Client gửi tin nhắn tới: /app/select-seat
     * Payload: { "showId": 1, "seatId": 50, "status": "SELECTED", "userId": 10 }
     */
    @MessageMapping("/select-seat")
    public void handleSeatSelection(@Payload SocketSeatRequest seatRequest) {

        // 1. XỬ LÝ LOGIC (Optional)
        // Tại đây bạn có thể gọi Service để lưu trạng thái ghế vào DB hoặc Redis
        // Ví dụ: seatService.lockSeat(seatRequest.getSeatId(), seatRequest.getUserId());
        System.out.println("User " + seatRequest.getUserId() + " đổi trạng thái ghế " + seatRequest.getSeatId() + " sang " + seatRequest.getStatus());

        // 2. GỬI LẠI CHO TẤT CẢ NGƯỜI DÙNG KHÁC
        // Chúng ta gửi vào topic riêng của Suất chiếu đó: /topic/show/{showId}
        // Để người xem phim A không bị nhảy ghế của phim B.

        String destination = "/topic/show/" + seatRequest.getShowId();

        // Gửi object này đi, tất cả client đang subscribe destination này sẽ nhận được
        messagingTemplate.convertAndSend(destination, seatRequest);
    }
}
