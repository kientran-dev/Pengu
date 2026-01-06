package cinema.ticket.booking.service.impl;

import java.util.List;
import java.time.Duration;
import java.util.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import cinema.ticket.booking.exception.MyBadRequestException;
import cinema.ticket.booking.exception.MyConflictExecption;
import cinema.ticket.booking.exception.MyLockedException;
import cinema.ticket.booking.exception.MyNotFoundException;
import cinema.ticket.booking.model.Account;
import cinema.ticket.booking.model.Booking;
import cinema.ticket.booking.model.CinemaShow;
import cinema.ticket.booking.model.Payment;
import cinema.ticket.booking.model.ShowSeat;
import cinema.ticket.booking.model.enumModel.BookingStatus;
import cinema.ticket.booking.model.enumModel.ESeatStatus;
import cinema.ticket.booking.model.enumModel.PaymentStatus;
import cinema.ticket.booking.repository.BookingRepository;
import cinema.ticket.booking.repository.CinemaShowRepository;
import cinema.ticket.booking.repository.PaymentRepository;
import cinema.ticket.booking.repository.ShowSeatRepository;
import cinema.ticket.booking.repository.UserRepository;
import cinema.ticket.booking.request.BookingRequest;
import cinema.ticket.booking.request.PaymentRequest;
import cinema.ticket.booking.response.MyApiResponse;
import cinema.ticket.booking.response.BookingResponse;
import cinema.ticket.booking.service.BookingService;
import cinema.ticket.booking.service.PaymentService;
import cinema.ticket.booking.service.UserService;
import cinema.ticket.booking.utils.VNPay;

@Service
public class BookingServiceImpl implements BookingService {

	final private int MAX_TICKETS_PER_SHOW = 5; // for per user
	final private int TIMEOUT = 15; // in minutes
	final private long CHECK_PENDING_BOOKING_IS_TIMEOUT = 60000; // in miliseconds

	@Autowired
	private UserRepository userREPO;

	@Autowired
	private ShowSeatRepository showSeatREPO;

	@Autowired
	private CinemaShowRepository showREPO;

	@Autowired
	private UserService userSER;

	@Autowired
	private BookingRepository bookingREPO;

	@Autowired
	private PaymentRepository paymentREPO;

	@Autowired
	private PaymentService paymentSER;

	private boolean seatsAreFull(CinemaShow show) {
		int bookedSeat = showSeatREPO.countByShowIdAndStatus(show.getId(), ESeatStatus.AVAILABLE);
		return bookedSeat == 0;
	}

	private ShowSeat getSeatFromStatus(String seat_id, CinemaShow show, ESeatStatus status) {
		ShowSeat seat = showSeatREPO.findByIdAndShowId(seat_id, show.getId()).orElseThrow(() -> new MyNotFoundException("Not found seat id: " + seat_id));
		if (seat.getStatus().equals(status.name()))
			return seat;
		return null;
	}

	// Helper function để set trạng thái cho cả Booking và các Ghế liên quan
	private void setStatusForBookingAndSeats(Booking booking, BookingStatus bookingStatus, ESeatStatus seatStatus) {
		booking.setStatus(bookingStatus);
		for (ShowSeat seat : booking.getSeats()) {
			seat.setStatus(seatStatus);
			showSeatREPO.save(seat);
		}

		bookingREPO.save(booking);
	}

	private void cancleBookingFromID(Booking booking) {
		this.setStatusForBookingAndSeats(booking, BookingStatus.CANCELED, ESeatStatus.AVAILABLE);
	}

	private String[] removeDuplicate(List<String> array) {
		Set<String> set = new HashSet<>(array);
		return set.toArray(new String[0]);
	}

	@Override
	public BookingResponse createBooking(String username, BookingRequest bookingReq) {
		if (bookingReq.getSeatsId().size() > 4)
			throw new MyBadRequestException("You can not reverse more than 4 seats at the time.");

		Account user = userREPO.getByUsername(username).orElseThrow(() -> new MyNotFoundException("User is not found"));

		CinemaShow show = showREPO.findById(bookingReq.getShowId()).orElseThrow(() -> new MyNotFoundException("Show is not found"));
		int total_tickets_of_user_from_show = bookingREPO.countByShowId(show.getId());
		if (total_tickets_of_user_from_show == this.MAX_TICKETS_PER_SHOW)
			throw new MyLockedException("You have already " + this.MAX_TICKETS_PER_SHOW + " tickets in this show, so you can pay no more tickets");
		if (this.seatsAreFull(show))
			throw new MyLockedException("Sorry, seats of this show are full. Please choose another show");

		List<ShowSeat> seats = new ArrayList<>();
		for (String seat_id : this.removeDuplicate(bookingReq.getSeatsId())) {
			ShowSeat seat = this.getSeatFromStatus(seat_id, show, ESeatStatus.AVAILABLE);
			if (seat == null)
				throw new MyConflictExecption("Seat ID " + seat_id + " is reserved");

			seat.setStatus(ESeatStatus.PENDING);
			ShowSeat seatSaved = showSeatREPO.save(seat);
			seats.add(seatSaved);
		}

		Booking booking = new Booking(user, show, seats);
		Booking bookingSaved = bookingREPO.save(booking);
		return new BookingResponse(bookingSaved);
	}

	@Override
	public MyApiResponse cancleBooking(String username, String booking_id) {
		Account user = userREPO.getByUsername(username).orElseThrow(() -> new MyNotFoundException("User is not found"));
		Booking booking = bookingREPO.findById(booking_id).orElseThrow(() -> new MyNotFoundException("Booking ticket is not found"));

		if (!user.getId().equals(booking.getUser().getId()))
			throw new MyConflictExecption("This ticket does not belong to user " + user.getUsername());

		if (booking.getStatus().equals(BookingStatus.CANCELED) || booking.getStatus().equals(BookingStatus.BOOKED))
			throw new MyBadRequestException("This ticket can not be cancled");

		this.cancleBookingFromID(booking);
		return new MyApiResponse("Done");
	}

	@Override
	public List<BookingResponse> listOfBooking(String username) {
		Account user = userREPO.getByUsername(username).orElseThrow(() -> new MyNotFoundException("User is not found"));
		List<Booking> listBooking = bookingREPO.findAllByUserId(user.getId());

		List<BookingResponse> info = new ArrayList<>();
		for (Booking booking : listBooking) {
			info.add(new BookingResponse(booking));
		}
		return info;
	}

	@Override
	public List<BookingResponse> getAllBookings() {
		// SỬA ĐOẠN NÀY: Gọi hàm Query trực tiếp, không dùng Sort.by nữa
		List<Booking> listBooking = bookingREPO.findAllOrderedByDateDesc();

		List<BookingResponse> info = new ArrayList<>();
		for (Booking booking : listBooking) {
			try {
				if (booking != null && booking.getUser() != null && booking.getShow() != null) {
					info.add(new BookingResponse(booking));
				}
			} catch (Exception e) {
				// Log lỗi nếu có bản ghi data bẩn
				System.err.println("Skipping invalid booking: " + (booking != null ? booking.getId() : "null"));
			}
		}
		return info;
	}

	@Override
	public BookingResponse getBookingFromID(String username, String booking_id) {
		Account user = userREPO.getByUsername(username).orElseThrow(() -> new MyNotFoundException("User is not found"));
		Booking booking = bookingREPO.findByIdAndUserId(booking_id, user.getId()).orElseThrow(() -> new MyNotFoundException("Ticket is not found"));
		return new BookingResponse(booking);
	}

	// --- PHẦN LOGIC ĐÃ ĐƯỢC SỬA LẠI HOÀN TOÀN ---
	@Override
	public MyApiResponse setBookingStatus(String username, String booking_id, String status) {
		Account user = userREPO.getByUsername(username).orElseThrow(() -> new MyNotFoundException("User is not found"));
		Booking booking = bookingREPO.findByIdAndUserId(booking_id, user.getId()).orElseThrow(() -> new MyNotFoundException("Ticket is not found"));

		status = status.toUpperCase();
		if (booking.getStatus().name().equals(status))
			throw new MyBadRequestException("This ticket already have this status");

		switch (status) {
			case "PENDING":
				// Logic: Xóa booking cũ -> Tạo booking mới để reset thời gian đếm ngược
				// QUAN TRỌNG: Phải set lại ghế thành PENDING (để giữ chỗ), KHÔNG được set AVAILABLE

				// 1. Lấy danh sách ghế đang giữ
				List<ShowSeat> currentSeats = booking.getSeats();

				// 2. Tạm thời set ghế thành Available để xóa booking không bị lỗi ràng buộc (nếu có)
				// Tuy nhiên, logic đúng là xóa booking entity, sau đó tạo mới và gán lại ghế.
				// Ở đây ta xóa booking ID cũ đi
				bookingREPO.deleteById(booking_id);

				// 3. Tạo booking mới
				Booking newBooking = new Booking(booking); // Copy thông tin
				bookingREPO.save(newBooking);

				// 4. Cập nhật lại trạng thái ghế thành PENDING (Giữ chỗ) cho Booking mới
				for (ShowSeat seat : currentSeats) {
					seat.setStatus(ESeatStatus.PENDING);
					showSeatREPO.save(seat);
				}
				break;

			case "CANCELED":
				// Logic: Hủy vé -> Set Booking CANCELED, Ghế AVAILABLE
				// Không gọi this.cancleBooking() vì nó chặn hủy vé đã BOOKED.
				// Admin có quyền hủy vé đã BOOKED (ví dụ hoàn tiền).
				this.setStatusForBookingAndSeats(booking, BookingStatus.CANCELED, ESeatStatus.AVAILABLE);

				// Nếu cần: Xử lý Payment thành CANCELED hoặc REFUNDED (tùy logic Payment)
				List<Payment> pCancels = paymentREPO.findAllByBookingId(booking.getId());
				if (!pCancels.isEmpty()) {
					Payment p = pCancels.get(0);
					p.setStatus(PaymentStatus.CANCELED); // Đánh dấu thanh toán đã hủy
					paymentREPO.save(p);
				}
				break;

			case "BOOKED":
				// Logic: Xác nhận vé -> Set Booking BOOKED, Ghế BOOKED, Payment PAID
				this.setStatusForBookingAndSeats(booking, BookingStatus.BOOKED, ESeatStatus.BOOKED);

				// Xử lý Payment: Tạo mới nếu chưa có, hoặc cập nhật thành PAID nếu đã có
				List<Payment> payments = paymentREPO.findAllByBookingId(booking.getId());
				if (payments.isEmpty()) {
					PaymentRequest req = new PaymentRequest(booking_id, "");
					try {
						// Tạo payment mới dạng PAID (giả lập)
						paymentSER.create(username, req, "127.0.0.1");
						// Sau khi tạo, lấy lại để set PAID
						List<Payment> newPs = paymentREPO.findAllByBookingId(booking.getId());
						if (!newPs.isEmpty()) {
							Payment newP = newPs.get(0);
							newP.setStatus(PaymentStatus.PAID);
							paymentREPO.save(newP);
						}
					} catch (Exception e) {
						// Bỏ qua lỗi tạo payment nếu có
					}
				} else {
					// Đã có payment (ví dụ PENDING), cập nhật thành PAID
					Payment existingPayment = payments.get(0);
					existingPayment.setStatus(PaymentStatus.PAID);
					paymentREPO.save(existingPayment);
				}
				break;

			default:
				throw new MyBadRequestException("Not found status " + status);
		}

		return new MyApiResponse("Success");
	}

	@Scheduled(fixedDelay = CHECK_PENDING_BOOKING_IS_TIMEOUT)
	public void autoCancleBooking() {
		//System.out.println("==> Check");
		List<Booking> bookingList = bookingREPO.findAllByStatus(BookingStatus.PENDING);

		LocalDateTime now = LocalDateTime.now();
		for (Booking booking : bookingList) {
			Date createDate = booking.getCreateAt();
			LocalDateTime toLocalDateTime = createDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			Duration duration = Duration.between(toLocalDateTime, now);
			long minutes = duration.toMinutes() % 60;

			List<Payment> payments = paymentREPO.findAllByBookingId(booking.getId());
			Payment payment = null;
			boolean existPayment = false;
			if (payments.size() != 0) {
				payment = payments.get(0);
				existPayment = true;
			}

			if (minutes >= this.TIMEOUT || (existPayment && payment.getStatus() != PaymentStatus.PENDING )) {
				// Check Again when timeout and payment is
				if (minutes >= this.TIMEOUT && existPayment && payment.getStatus() == PaymentStatus.PENDING ) {
					try {
						Integer paid = VNPay.verifyPay(payment);
						if (paid == 0) {
							payment.setStatus(PaymentStatus.PAID);
							paymentREPO.save(payment);
						}
						else if (paid == 2) {
							payment.setStatus(PaymentStatus.CANCELED);
							paymentREPO.save(payment);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (existPayment && payment.getStatus() == PaymentStatus.PAID) {
					booking.setStatus(BookingStatus.BOOKED);
					bookingREPO.save(booking);

					for (ShowSeat seat : booking.getSeats()) {
						seat.setStatus(ESeatStatus.BOOKED);
						showSeatREPO.save(seat);
					}

					paymentSER.addPaymentMail(payment);
					System.out.println("--> Send ticket of booking " + booking.getId());
				}
				else {
					this.cancleBookingFromID(booking);
					System.out.println("--> Delete status of booking " + booking.getId());
				}
			}
		}
	}
}