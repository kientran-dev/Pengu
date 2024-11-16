package cinema.ticket.booking.service.impl;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import cinema.ticket.booking.exception.MyBadRequestException;
import cinema.ticket.booking.exception.MyNotFoundException;
import cinema.ticket.booking.model.Movie;
import cinema.ticket.booking.model.Genre;
import cinema.ticket.booking.repository.GenreReposity;
import cinema.ticket.booking.repository.MovieRepo;
import cinema.ticket.booking.response.MyApiResponse;
import cinema.ticket.booking.response.MovieInfoResponse;
import cinema.ticket.booking.security.InputValidationFilter;
import cinema.ticket.booking.service.MovieService;
import org.springframework.security.core.parameters.P;

class MovieServiceImplTest {
    @Mock
    private MovieRepo mRepo;

    @Mock
    private GenreReposity genreReposity;

    @Mock
    private InputValidationFilter inputValidationSER;

    @InjectMocks
    private MovieServiceImpl movieService;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
    }

    //chức năng lấy danh sách phim theo trang
    @Test
    void getMovies() {
        int pageNumber = 0;
        int pageSize = 10;
        List<Movie> movies = Arrays.asList(new Movie(), new Movie());
        when(mRepo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(movies));
        List<MovieInfoResponse> result = movieService.getMovies(pageNumber, pageSize);
        assertEquals(2, result.size());
        verify(mRepo, times(1)).findAll(any(Pageable.class));
    }

    //chức năng lưu phim
    //case 1:tiêu đề đã tồn tại.
    @Test
    void saveMovie() {
        Movie existingMovie = new Movie();
        existingMovie.setTitle("Existing Movie");

        when(mRepo.existsByTitle("Existing Movie")).thenReturn(true);

        assertThrows(MyBadRequestException.class, () -> { movieService.saveMovie(existingMovie); });
    }

    //case 2:tiêu đề chưa có.
    @Test
    void saveMovieNotTitleExisting(){
        Movie newMovie = new Movie();
        newMovie.setTitle("New Movie");
        when(mRepo.existsByTitle("New Movie")).thenReturn(false);
        when(mRepo.save(newMovie)).thenReturn(newMovie);

        Movie savedMovie = movieService.saveMovie(newMovie);

        assertNotNull(savedMovie);
        assertEquals("New Movie", savedMovie.getTitle());
        verify(mRepo, times(1)).save(newMovie);
    }
    //chức năng tìm kiếm phim theo tên
    @Test
    void getMatchingName() {
        String keyWord = "Test";
        int pageNumber = 0;
        int pageSize = 10;

        List<Movie> movies = Arrays.asList(new Movie(), new Movie());

        when(inputValidationSER.sanitizeInput(keyWord)).thenReturn(keyWord);
        when(inputValidationSER.checkInput(keyWord)).thenReturn(true);
        when(mRepo.findByTitleContaining(eq(keyWord), any(Pageable.class))).thenReturn(movies);

        List<MovieInfoResponse> result = movieService.getMatchingName(keyWord, pageNumber, pageSize);

        assertEquals(2, result.size());
        verify(inputValidationSER, times(1)).sanitizeInput(keyWord);
        verify(inputValidationSER, times(1)).checkInput(keyWord);
        verify(mRepo, times(1)).findByTitleContaining(eq(keyWord), any(Pageable.class));
    }
    //chức năng tìm kiếm phim theo thể loại
    @Test
    void getMatchingGenre() {
        String keyWord = "Test Two";
        int pageNumber = 1;
        int pageSize = 8;
        Movie movie1 = new Movie();
        movie1.setId(1L);
        movie1.setTitle("Movie 1");

        Movie movie2 = new Movie();
        movie2.setId(2L);
        movie2.setTitle("Movie 2");

        List<Movie> movies = new ArrayList<>();
        movies.add(movie1);
        movies.add(movie2);

        Genre genre = new Genre();
        genre.setGenre("Action");
        genre.setId(1L);
        genre.setTitleList(movies);

        List<Genre> genres = List.of(genre);

        when(inputValidationSER.sanitizeInput(keyWord)).thenReturn(keyWord);
        when(inputValidationSER.checkInput(keyWord)).thenReturn(true);
        when(genreReposity.findByGenreContaining(keyWord)).thenReturn(genres);

        Object[] result = movieService.getMatchingGenre(keyWord, pageNumber, pageSize);

        assertNotNull(result);
        assertEquals(2, result.length);
        verify(inputValidationSER, times(1)).sanitizeInput(keyWord);
        verify(inputValidationSER, times(1)).checkInput(keyWord);
        verify(genreReposity, times(1)).findByGenreContaining(keyWord);
    }

    //chức năng lấy thông tin phim
    //case 1: trường hợp phim tồn tại
    @Test
    void getMovie() {
        Long movieID = 1L;
        Movie mf = new Movie();
        mf.setTitle("Movie Find");
        mf.setId(movieID);
        when(mRepo.findById(movieID)).thenReturn(Optional.of(mf));

        MovieInfoResponse response = movieService.getMovie(movieID);

        assertNotNull(response);
        assertEquals("Movie Find", response.getTitle());
        verify(mRepo, times(1)).findById(movieID);
    }


    //case 2: trường hợp phim không tồn tại
    @Test
    void getMovieNotExisting(){
        Long movieID = 1L;

        when(mRepo.findById(movieID)).thenReturn(Optional.empty());
        assertThrows(MyNotFoundException.class, () -> {
            movieService.getMovie(movieID);
        });
        verify(mRepo, times(1)).findById(movieID);
    }

    //chức năng xóa phim
    //case 1: phim đã tồn tại để xóa.
    @Test
    void deleteMovie() {
        Long movieID = 1L;
        Movie movie = new Movie();
        movie.setId(movieID);
        when(mRepo.findById(movieID)).thenReturn(Optional.of(movie));

        MyApiResponse response = movieService.deleteMovie(movieID);

        assertNotNull(response);
        assertEquals("Deleted movie ID 1" , response.getMessage());
        verify(mRepo, times(1)).findById(movieID);
        verify(mRepo, times(1)).deleteById((movieID));
    }

    //case 2:phim chưa tồn tại để xóa
    @Test
    void deleteMovieNotExisting(){
        Long movieID = 1L;

        when(mRepo.findById(movieID)).thenReturn(Optional.empty());

        assertThrows(MyNotFoundException.class, () -> {
            movieService.deleteMovie(movieID);
        });

        verify(mRepo, times(1)).findById(movieID);
        verify(mRepo, never()).deleteById(movieID);
    }


    ///chức năng cập nhật phim
    @Test
    void updateMovie() {
        Long movieID = 1L;
        Movie movieExisting = new Movie();
        movieExisting.setId(movieID);
        movieExisting.setTitle("Update Movie");

        when(mRepo.save(movieExisting)).thenReturn(movieExisting);

        Movie result = movieService.updateMovie(movieExisting);

        assertNotNull(result);
        assertEquals("Update Movie", result.getTitle());
        verify(mRepo, times(1)).save(movieExisting);
    }

    // chức năng lưu danh sách phim
    //case 1: trong danh sách phim chưa tồn tại 2 tiêu đề phim này
    @Test
    void saveMovieList() {
        Movie movie1 = new Movie();
        movie1.setTitle("Movie 1");


        Movie movie2 = new Movie();
        movie2.setTitle("Movie 2");

        Genre genre1 = new Genre();
        genre1.setGenre("Genre 1");
        movie1.setGenres(Arrays.asList(genre1));

        Genre genre2 = new Genre();
        genre2.setGenre("Genre 2");
        movie2.setGenres(Arrays.asList(genre2));

        List<Movie> movies = Arrays.asList(movie1 ,movie2);

        when(mRepo.existsByTitle("Movie 1")).thenReturn(false);
        when(mRepo.existsByTitle("Movie 2")).thenReturn(false);
        when(genreReposity.findByGenre("Genre 1")).thenReturn(genre1);
        when(genreReposity.findByGenre("Genre 2")).thenReturn(genre2);

        MyApiResponse response = movieService.saveMovieList(movies);

        assertNotNull(response);
        assertEquals("Success", response.getMessage());
        verify(mRepo, times(1)).save(movie1);
        verify(mRepo, times(1)).save(movie2);
    }
    //case 2: trong danh phim đã tồn tại phim số 2
    @Test
    void saveMovieList_withExistingTitles(){
        Movie movie1 = new Movie();
        movie1.setTitle("Movie 1");


        Movie movie2 = new Movie();
        movie2.setTitle("Movie 2");

        Genre genre1 = new Genre();
        genre1.setGenre("Genre 1");
        movie1.setGenres(Arrays.asList(genre1));

        Genre genre2 = new Genre();
        genre2.setGenre("Genre 2");
        movie2.setGenres(Arrays.asList(genre2));

        List<Movie> movies = Arrays.asList(movie1 ,movie2);

        when(mRepo.existsByTitle("Movie 1")).thenReturn(false);
        when(mRepo.existsByTitle("Movie 2")).thenReturn(true);
        when(genreReposity.findByGenre("Genre 1")).thenReturn(genre1);
        when(genreReposity.findByGenre("Genre 2")).thenReturn(genre2);

        MyApiResponse response = movieService.saveMovieList(movies);
        assertNotNull(response);
        assertEquals("Success", response.getMessage());
        verify(mRepo, times(1)).save(movie1);
        verify(mRepo, never()).save(movie2);
    }
}