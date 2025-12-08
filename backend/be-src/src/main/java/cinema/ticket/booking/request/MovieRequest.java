package cinema.ticket.booking.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieRequest {
	
	
	@JsonProperty("title")
	private String title;
	
	@JsonProperty("description")
	private String description;
	
	@JsonProperty("durationInMins")
	private int durationInMins;
	
	@JsonProperty("language")
	private String language;
	
	@JsonProperty("releaseDate")
	private String releaseDate;
	
	@JsonProperty("country")
	private String country;
	
	@JsonProperty("genre")
	@ManyToMany
	private List<String> genre;
	
	@JsonProperty("image")
	private String image;

	@JsonProperty("large_image")
	private String large_image;
	
	@JsonProperty("trailer")
	private String trailer;
	
	@JsonProperty("actors")
	private String actors;

	@JsonProperty("priceCoefficient")
	private double priceCoefficient = 1.0;


	public String getLargeImage() {
		return this.large_image;
	}
	public void setLargeImage(String large_image) {
		this.large_image = image;
	}
}
