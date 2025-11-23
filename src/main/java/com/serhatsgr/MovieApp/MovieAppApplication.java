package com.serhatsgr.MovieApp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@ComponentScan(basePackages = {"com.serhatsgr"}) //Autowired enjeksiyonlarını görmesi için
@EntityScan(basePackages = {"com.serhatsgr"})  //entity anotasyonuyla işaretlenmiş sınıfın beanlarini görmesi için
@EnableJpaRepositories(basePackages = {"com.serhatsgr"})   //repository jpa interfaccesini görmesi için
@SpringBootApplication
public class MovieAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovieAppApplication.class, args);
	}

}




