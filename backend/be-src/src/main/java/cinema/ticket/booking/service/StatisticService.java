package cinema.ticket.booking.service;

import java.util.List;
import java.util.Map;

public interface StatisticService {
    Map<String, Object> getSummaryStatistics();
    List<Map<String, Object>> getRevenueTrend();
    List<Map<String, Object>> getTopMovies();
}
