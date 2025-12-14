package cinema.ticket.booking.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cinema.ticket.booking.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, String> {
	@Query(value = "SELECT p FROM Payment p WHERE p.booking.user.id = :user_id")
	public List<Payment> findAllByUserId(@Param("user_id") String user_id);

	@Query(value = "SELECT p FROM Payment p WHERE p.booking.id = :booking_id")
	public List<Payment> findAllByBookingId(@Param("booking_id") String booking_id);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = cinema.ticket.booking.model.enumModel.PaymentStatus.PAID")
    Double findTotalRevenue();

    @Query(value = "SELECT DATE(p.create_at) as date, SUM(p.amount) as revenue FROM payment p WHERE p.status = 'PAID' AND p.create_at >= CURDATE() - INTERVAL 12 DAY GROUP BY DATE(p.create_at) ORDER BY DATE(p.create_at)", nativeQuery = true)
    List<Map<String, Object>> findRevenueByDate();
}
