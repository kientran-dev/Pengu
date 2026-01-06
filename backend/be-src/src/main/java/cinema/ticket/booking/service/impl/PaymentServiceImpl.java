package cinema.ticket.booking.service.impl;

import java.util.List;
import java.util.Queue;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import cinema.ticket.booking.exception.MyBadRequestException;
import cinema.ticket.booking.exception.MyNotFoundException;
import cinema.ticket.booking.model.Account;
import cinema.ticket.booking.model.Booking;
import cinema.ticket.booking.model.Payment;
import cinema.ticket.booking.model.enumModel.BookingStatus;
import cinema.ticket.booking.model.enumModel.PaymentStatus;
import cinema.ticket.booking.repository.BookingRepository;
import cinema.ticket.booking.repository.PaymentRepository;
import cinema.ticket.booking.repository.UserRepository;
import cinema.ticket.booking.request.HashRequest;
import cinema.ticket.booking.request.PaymentRequest;
import cinema.ticket.booking.response.PaymentResponse;
import cinema.ticket.booking.response.MyApiResponse;
import cinema.ticket.booking.service.EmailService;
import cinema.ticket.booking.service.PaymentService;
import cinema.ticket.booking.utils.HashUtil;
import cinema.ticket.booking.utils.VNPay;

@Service
public class PaymentServiceImpl implements PaymentService {
	
	final private int SEND_MAIL_SCHEDULE = 1000;
	
	Queue<PaymentResponse> sendEmail = new LinkedList<>();
	
	@Autowired
	private UserRepository userREPO;
	
	@Autowired
	private BookingRepository bookingREPO;
	
	
	@Autowired
	private PaymentRepository paymentREPO;
	
	@Autowired
	private EmailService emailSER;
	
	@Override
	public PaymentResponse create(String username, PaymentRequest request, String ip_addr) {
		Booking booking = bookingREPO.findById(request.getBookingId()).orElseThrow(() -> new MyNotFoundException("Ticket ID " + request.getBookingId() + " is not found"));
		if (!booking.getStatus().equals(BookingStatus.PENDING))
			throw new MyBadRequestException("This ticket have been already paid or canceled before.");
		
		List<Payment> payments = paymentREPO.findAllByBookingId(booking.getId());
		if (payments.size() != 0) 
			throw new MyBadRequestException("This ticket have been already pending for payment.");

		String userFromBooking = booking.getUser().getUsername();
		if (!username.equals(userFromBooking))
			throw new MyNotFoundException("Ticket ID " + request.getBookingId() + " is not found");
		
		double price = booking.getPriceFromListSeats();

		Payment payment = new Payment(booking, price);
		Payment save = paymentREPO.save(payment);

		String bankCode = request.getPaymentType();

		// Validate và set mặc định
		if (bankCode == null || bankCode.trim().isEmpty()) {
			bankCode = "VNPAY"; // Để người dùng chọn ngân hàng trên trang VNPay
		}

		String res = "none";
		try {
			res = VNPay.createPay(payment, bankCode, ip_addr);
		} catch (Exception e) {
			payment.setStatus(PaymentStatus.CANCELED);
			System.out.println("Lỗi tạo thanh toán: " + e.toString());
		}

		save = paymentREPO.save(save);
		PaymentResponse resp = new PaymentResponse(save);
		resp.setPaymentUrl(res);
		return resp;
	}

	@Override
	public PaymentResponse getFromId(String username, String payment_id) {
		Payment payment = paymentREPO.findById(payment_id).orElseThrow(() -> new MyNotFoundException("Payment ID not found"));
		String userOfpayment = payment.getBooking().getUser().getUsername();
		if (username.equals(userOfpayment))
			return new PaymentResponse(payment);
		throw new MyNotFoundException("Payment ID not found");
	}
	
	@Override
	public MyApiResponse verifyPayment(String username, String payment_id) {
		Payment payment = paymentREPO.findById(payment_id).orElseThrow(() -> new MyNotFoundException("Payment ID not found"));
		String userOfpayment = payment.getBooking().getUser().getUsername();
		// --- THÊM ĐOẠN DEBUG NÀY ---
		System.out.println("=== DEBUG VERIFY PAYMENT ===");
		System.out.println("Payment ID: " + payment_id);
		System.out.println("User đang login (Token): " + username);
		System.out.println("User tạo đơn (Database): " + userOfpayment);
		System.out.println("Kết quả so sánh: " + username.equals(userOfpayment));
		System.out.println("============================");
		if (username.equals(userOfpayment)) {
			if (payment.getStatus() != PaymentStatus.PENDING)
				return new MyApiResponse("This ticket have been already paid or canceled before.");

			try {
				Integer paid = VNPay.verifyPay(payment);

				if (paid == 0) {
					payment.setStatus(PaymentStatus.PAID);
					paymentREPO.save(payment);
					return new MyApiResponse("Ticket is paid. You will receive this email", "PAID");
				}
				else if (paid == 2) {
					payment.setStatus(PaymentStatus.CANCELED);
					paymentREPO.save(payment);
					return new MyApiResponse("Ticket is unpaid", "UNPAID");
				}
				
				return new MyApiResponse("Ticket is pending", "PENDING");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		throw new MyNotFoundException("Payment ID not found");


	}
	
	@Override
	public List<PaymentResponse> getAllPaymentsOfUser(String username) {
		Account user = userREPO.getByUsername(username).orElseThrow(() -> new MyNotFoundException("User not found"));
		List<Payment> payments = paymentREPO.findAllByUserId(user.getId());
		
		List<PaymentResponse> resps = new ArrayList<PaymentResponse>();
		for (Payment p : payments)
			resps.add(new PaymentResponse(p));
		return resps;
	}
	
	@Override
	public boolean checkPaymentInfo(PaymentRequest request) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void addPaymentMail(Payment payment) {
		PaymentResponse resp = new PaymentResponse(payment);
		this.sendEmail.offer(resp);
	}

	@Scheduled(fixedDelay = SEND_MAIL_SCHEDULE)
	private void sendPaymentViaMail() {
		while (this.sendEmail.size() != 0) {
			PaymentResponse data = this.sendEmail.poll();

			// Tạo nội dung HTML
			String htmlContent = buildHtmlEmail(data);

			String subject = "Xác nhận thanh toán vé xem phim thành công!";

			// Gọi hàm gửi mail HTML vừa viết ở Bước 1
			emailSER.sendHtmlMail(data.getEmail(), subject, htmlContent);
		}
	}

	// Hàm phụ trợ để xây dựng giao diện HTML
	private String buildHtmlEmail(PaymentResponse data) {
		return "<!DOCTYPE html>"
				+ "<html>"
				+ "<head>"
				+ "<style>"
				+ "body {font-family: Arial, sans-serif; line-height: 1.6; color: #333;}"
				+ ".container {width: 100%; max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;}"
				+ ".header {background-color: #4CAF50; color: white; padding: 20px; text-align: center;}"
				+ ".content {padding: 20px;}"
				+ ".info-table {width: 100%; border-collapse: collapse; margin-top: 10px;}"
				+ ".info-table td {padding: 8px; border-bottom: 1px solid #eee;}"
				+ ".info-table td:first-child {font-weight: bold; width: 40%;}"
				+ ".footer {background-color: #f4f4f4; text-align: center; padding: 10px; font-size: 12px; color: #777;}"
				+ ".total-price {color: #e91e63; font-weight: bold; font-size: 18px;}"
				+ "</style>"
				+ "</head>"
				+ "<body>"
				+ "<div class='container'>"
				+ "  <div class='header'>"
				+ "    <h2>Thanh Toán Thành Công!</h2>"
				+ "  </div>"
				+ "  <div class='content'>"
				+ "    <p>Xin chào,</p>"
				+ "    <p>Cảm ơn bạn đã đặt vé. Dưới đây là thông tin chi tiết vé của bạn:</p>"
				+ "    <table class='info-table'>"
				+ "      <tr><td>Mã thanh toán:</td><td>" + data.getId() + "</td></tr>"
				+ "      <tr><td>Phim:</td><td>" + data.getDetai().getMovieName() + "</td></tr>"
				+ "      <tr><td>Rạp:</td><td>" + data.getDetai().getHallName() + "</td></tr>"
				+ "      <tr><td>Thời gian:</td><td>" + data.getDetai().getStartTime() + "</td></tr>"
				+ "      <tr><td>Ghế:</td><td>" + String.join(", ", data.getDetai().getSeats()) + "</td></tr>"
				+ "      <tr><td>Tổng tiền:</td><td class='total-price'>" + data.getPrice() + " VND</td></tr>"
				+ "      <tr><td>Ngày đặt:</td><td>" + data.getCreateOn() + "</td></tr>"
				+ "    </table>"
				+ "    <p>Vui lòng đưa mã thanh toán hoặc email này cho nhân viên tại quầy vé.</p>"
				+ "  </div>"
				+ "  <div class='footer'>"
				+ "    <p>&copy; 2024 Cinema Booking System. All rights reserved.</p>"
				+ "  </div>"
				+ "</div>"
				+ "</body>"
				+ "</html>";
	}

	@Override
	public String createHash(HashRequest rawdata) {
		try {
			HashUtil hashUtil = new HashUtil();
			String data = rawdata.getBookingId() + "&" + rawdata.getCardID() 
						+ "&" + rawdata.getCardName() + "&" + rawdata.getCVCNumber();
			String hash = hashUtil.calculateHash(data);
			return hash;
			
		} catch (NoSuchAlgorithmException e) {
            return null;
        }
	}
}
