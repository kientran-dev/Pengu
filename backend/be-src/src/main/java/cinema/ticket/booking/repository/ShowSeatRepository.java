package cinema.ticket.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cinema.ticket.booking.model.ShowSeat;
import cinema.ticket.booking.model.enumModel.ESeatStatus;
import jakarta.transaction.Transactional;

@Transactional
@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, String> {
	int countByShowIdAndStatus(String show_id, ESeatStatus status);
	List<ShowSeat> findByShowId(String showId);
	void deleteAllByShowId(String show_id);
	@Query("SELECT s FROM ShowSeat s " +
			"WHERE s.show.id = :showId " +
			"ORDER BY s.cinemaSeat.rowIndex ASC, s.cinemaSeat.colIndex ASC")
	List<ShowSeat> findAllByCinemaShowIdSorted(@Param("showId") String showId);
	Optional<ShowSeat> findByIdAndShowId(String id, String showId);
}

