package cinema.ticket.booking.service.impl;

import cinema.ticket.booking.repository.BookingRepository;
import cinema.ticket.booking.repository.MovieRepo;
import cinema.ticket.booking.repository.PaymentRepository;
import cinema.ticket.booking.repository.UserRepository;
import cinema.ticket.booking.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticServiceImpl implements StatisticService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepo movieRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public Map<String, Object> getSummaryStatistics() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", paymentRepository.findTotalRevenue());
        summary.put("ticketsSold", bookingRepository.countConfirmedBookings());
        summary.put("customers", userRepository.count());
        summary.put("movies", movieRepository.count());
        return summary;
    }

    @Override
    public List<Map<String, Object>> getRevenueTrend() {
        return paymentRepository.findRevenueByDate();
    }

    @Override
    public List<Map<String, Object>> getTopMovies() {
        return bookingRepository.findTopMovies();
    }
}
