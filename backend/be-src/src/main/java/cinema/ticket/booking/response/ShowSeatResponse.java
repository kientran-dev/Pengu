package cinema.ticket.booking.response;

import java.time.LocalDateTime;

import cinema.ticket.booking.model.Movie;
import cinema.ticket.booking.model.ShowSeat;

public class ShowSeatResponse {
	private String seat_id;
	private String status;
	private String type;
	private String name;
	private int row_index;
	private int col_index;
	private double price;

	public ShowSeatResponse(ShowSeat showSeat) {
		this.seat_id = showSeat.getId();
		this.status = showSeat.getStatus();
		this.type = showSeat.getCinemaSeat().getSeatType();
		this.name = showSeat.getCinemaSeat().getName();
		this.row_index = showSeat.getCinemaSeat().getRowIndex();
		this.col_index = showSeat.getCinemaSeat().getColIndex();

		// --- SỬA LOGIC TÍNH GIÁ Ở ĐÂY CHO KHỚP VỚI BOOKING ---

		// 1. Lấy giá gốc
		double originalPrice = showSeat.getCinemaSeat().getPrice();

		// 2. Lấy thông tin Phim và Giờ chiếu từ ShowSeat -> Show
		Movie movie = showSeat.getShow().getMovie();
		LocalDateTime startTime = showSeat.getShow().getStartTime();

		// 3. Hệ số Phim (Lấy từ Movie)
		double movieFactor = movie.getPriceCoefficient();
		if (movieFactor <= 0) movieFactor = 1.0;

		// 4. Hệ số Giờ chiếu (Time Factor)
		double timeFactor = 1.0;
		int hour = startTime.getHour();

		if (hour >= 18 && hour <= 22) {
			timeFactor = 1.1; // Tối đắt hơn 10%
		} else if (hour < 10) {
			timeFactor = 0.9; // Sáng rẻ hơn 10%
		}

		// 5. Tính giá cuối cùng hiển thị lên sơ đồ ghế
		this.price = Math.round((originalPrice * movieFactor * timeFactor) * 100.0) / 100.0;
	}

	public String getSeatId() {
		return this.seat_id;
	}

	public String getStatus() {
		return this.status;
	}

	public String getType() {
		return this.type;
	}

	public String getName() {
		return this.name;
	}

	public int getRowIndex() {
		return this.row_index;
	}

	public int getColIndex() {
		return this.col_index;
	}

	public double getPrice() {
		return this.price;
	}
}