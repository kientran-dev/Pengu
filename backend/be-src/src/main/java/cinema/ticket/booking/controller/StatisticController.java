package cinema.ticket.booking.controller;

import cinema.ticket.booking.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticController {

    @Autowired
    private StatisticService statisticService;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummaryStatistics() {
        return ResponseEntity.ok(statisticService.getSummaryStatistics());
    }

    @GetMapping("/revenue-trend")
    public ResponseEntity<?> getRevenueTrend() {
        return ResponseEntity.ok(statisticService.getRevenueTrend());
    }

    @GetMapping("/top-movies")
    public ResponseEntity<?> getTopMovies() {
        return ResponseEntity.ok(statisticService.getTopMovies());
    }
}
