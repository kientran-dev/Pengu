package cinema.ticket.booking.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cinema.ticket.booking.exception.MyBadRequestException;
import cinema.ticket.booking.exception.MyNotFoundException;
import cinema.ticket.booking.model.Genre;
import cinema.ticket.booking.repository.GenreReposity;
import cinema.ticket.booking.response.MyApiResponse;
import cinema.ticket.booking.service.GenreService;
class GenreServiceImplTest {
    @Mock
    private GenreReposity gReposity;

    @InjectMocks
    private GenreServiceImpl genreService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void getGenres() {
        Genre genre1 = new Genre();
        genre1.setGenre("Genre 1");

        Genre genre2 = new Genre();
        genre2.setGenre("Genre 2");

        List<Genre> genres = Arrays.asList(genre1, genre2);

        when(gReposity.findAll()).thenReturn(genres);

        List<Genre> result = genreService.getGenres();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(gReposity, times(1)).findAll();
    }

    @Test
    void saveGenre() {
        //case 1: trường hợp đã tồn tại genre
        Genre existingGenre = new Genre();
        existingGenre.setGenre("Action");

        when(gReposity.existsByGenre("Action")).thenReturn(true);

        assertThrows(MyBadRequestException.class, () -> {
            genreService.saveGenre(existingGenre);
        });

        // case 2: trường hợp chưa tồn tại genre
        Genre newGenre = new Genre();
        newGenre.setGenre("Comedy");

        when(gReposity.existsByGenre("Comedy")).thenReturn(false);
        when(gReposity.save(newGenre)).thenReturn(newGenre);

        Genre savedGenre = genreService.saveGenre(newGenre);

        assertNotNull(savedGenre);
        assertEquals("Comedy", savedGenre.getGenre());
        verify(gReposity, times(1)).save(newGenre);
    }

    @Test
    void saveListGenres() {
        Genre genre1 = new Genre();
        genre1.setGenre("Action");
        Genre genre2 = new Genre();
        genre2.setGenre("Comedy");

        List<Genre> genres = Arrays.asList(genre1, genre2);

        when(gReposity.existsByGenre("Action")).thenReturn(false);// giả sử trong danh sách chưa tồn tại genre1
        when(gReposity.existsByGenre("Comedy")).thenReturn(true);//giả sử trong danh sách đã tồn tại genre2
        when(gReposity.save(genre1)).thenReturn(genre1);

        MyApiResponse response = genreService.saveListGenres(genres);

        assertNotNull(response);
        assertEquals("Success", response.getMessage());
        verify(gReposity, times(1)).save(genre1); // Chỉ gọi save với genre "Action"
        verify(gReposity, never()).save(genre2); // Không gọi save với genre "Comedy"
    }

    //case 1: nếu genre đã tồn tại
    @Test
    void getGenre(){
        Long genreID = 1L;
        Genre genre = new Genre();
        genre.setId(genreID);
        genre.setGenre("Action");

        when(gReposity.findById(genreID)).thenReturn(Optional.of(genre));

        Genre result = genreService.getGenre(genreID);

        assertNotNull(result);
        assertEquals("Action", result.getGenre());
        verify(gReposity, times(1)).findById(genreID);
    }

    //case 2: nếu genre chưa tồn tại
    @Test
    void getGenreNotExisting(){
        Long genreID = 1L;

        when(gReposity.findById(genreID)).thenReturn(Optional.empty());

        assertThrows(MyNotFoundException.class, () ->{
            genreService.getGenre(genreID);
        });
        verify(gReposity, times(1)).findById(genreID);
    }

    //case 1: nếu genre đã tồn tại
    @Test
    void deleteGenre(){
        Long genreID = 1L;
        Genre genre = new Genre();
        genre.setId(genreID);
        genre.setGenre("Action");

        when(gReposity.existsById(genreID)).thenReturn(true);

        MyApiResponse response = genreService.deleteGenre(genreID);

        assertNotNull(response);
        assertEquals("Delete genre ID " + genreID, response.getMessage());
        verify(gReposity, times(1)).existsById(genreID);
        verify(gReposity, times(1)).deleteById(genreID);
    }

    //case 2: nếu genre chưa tồn tại.
    @Test
    void deleteGenreNotExisting() {
        Long genreID = 1L;

        when(gReposity.existsById(genreID)).thenReturn(false);

        assertThrows(MyNotFoundException.class, () -> {
            genreService.deleteGenre(genreID);
        });
        verify(gReposity, times(1)).existsById(genreID);
        verify(gReposity, never()).deleteById(genreID);
    }


}