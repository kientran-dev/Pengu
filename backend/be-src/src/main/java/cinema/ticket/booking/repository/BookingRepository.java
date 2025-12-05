package cinema.ticket.booking.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cinema.ticket.booking.model.Booking;
import cinema.ticket.booking.model.enumModel.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, String> {
	List<Booking> findAllByStatus(BookingStatus status);
	List<Booking> findAllByUserId(String user_id);
	
	Optional<Booking> findByIdAndUserId(String booking_id, String user_id);
	
	@Query("SELECT b FROM Booking b WHERE b.user.id = :user_id AND b.show.movie.id = :movie_id AND b.status = :status")
	Optional<Booking> findByUserIdAndMovieIdAndStatus(@Param("user_id") String user_id, @Param("movie_id") long movie_id, @Param("status") BookingStatus status);
	
	int countByShowId(String show_id);

    @Query("SELECT COUNT(ss) FROM Booking b JOIN b.seats ss WHERE b.status = cinema.ticket.booking.model.enumModel.BookingStatus.BOOKED")
    long countConfirmedBookings();

	@Query(value = "SELECT m.title as name, COUNT(bs.seats_id) as tickets_sold " + // Sửa m.name -> m.title
			"FROM booking b " +
			"JOIN booking_seats bs ON b.id = bs.booking_id " +
			"JOIN cinema_show s ON b.show_id = s.id " +
			"JOIN movie m ON s.movie_id = m.id " +
			"WHERE b.status = 'BOOKED' " + // Kiểm tra lại status, thường là 'BOOKED' thay vì 'CONFIRMED'
			"AND b.create_at >= CURDATE() - INTERVAL 1 MONTH " +
			"GROUP BY m.title " + // Sửa group by theo m.title
			"ORDER BY tickets_sold DESC LIMIT 5", nativeQuery = true)
	List<Map<String, Object>> findTopMovies();
}
