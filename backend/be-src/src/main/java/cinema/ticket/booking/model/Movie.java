package cinema.ticket.booking.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import cinema.ticket.booking.request.MovieRequest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "Movie",
	uniqueConstraints = { @UniqueConstraint(columnNames = { "title" ,"id"})
	})
@Getter
@Setter
public class Movie{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	
	@CreationTimestamp
	@Column(name = "CreatedAt", updatable = false)
	private Date createdAt;
	
	@UpdateTimestamp
	@Column(name = "lastUpdated")
	private Date lastUpdated;
	
	@Column(name = "title")
	private String title;
	
	@Column(name = "description", length = 3000)
	private String description;
	
	@Column(name = "durationInMins")
	private int durationInMins;
	
	@Column(name = "language")
	private String language;
	
	@Column(name = "releaseDate")
	private String releaseDate;
	
	@Column(name = "country")
	private String country;

	@Column(name = "price_coefficient")
	private double priceCoefficient = 1.0;
	
	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)	
	@JoinTable(name = "Movie_Genre", 
		joinColumns = {
				@JoinColumn(name = "movie_id", referencedColumnName = "id")
				
		},
		inverseJoinColumns = {
				@JoinColumn(name = "genre_id", referencedColumnName = "id")
		}
		)
	@JsonProperty(value = "genres")
	private List<Genre> genres;
	
	@OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
	
	@Column(name = "image")
	private String image;

	@Column(name = "large_image")
	private String large_image;
	
	@Column(name = "trailer")
	private String trailer;
	
	@Column(name = "actors")
	private String actors;
	
	public Movie() {}
	
	public Movie(MovieRequest req) {
		this.title = req.getTitle();
		this.description = req.getDescription();
		this.durationInMins = req.getDurationInMins();
		this.language = req.getLanguage();
		this.releaseDate = req.getReleaseDate();
		this.country = req.getCountry();
		this.image = req.getImage();
		this.large_image = req.getLargeImage();
		this.priceCoefficient = req.getPriceCoefficient();
	}

	public String getLargeImage() {
		return this.large_image;
	}
	
	public void setLargeImage(String image) {
		this.large_image = image;
	}
	
	public void addComment(Comment comment) {
        this.comments.add(comment);
    }

    public void removeComment(Comment comment) {
        this.comments.remove(comment);
    }
}
