package dev.danvega.sb3;

import dev.danvega.sb3.post.Post;
import dev.danvega.sb3.post.PostClient;
import dev.danvega.sb3.post.PostRepository;
import jakarta.servlet.ServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

@SpringBootApplication
public class Application {

	private static final Logger log = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	PostClient postClient() {
		RestClient restClient = RestClient.create("https://jsonplaceholder.typicode.com/");
		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
		return factory.createClient(PostClient.class);
	}

	@Bean
	CommandLineRunner commandLineRunner(PostRepository postRepository, PostClient postClient) {
		return args -> {
			List<Post> posts = postClient.findAll();
			postRepository.saveAll(posts);

			Post byId = postClient.findById(1);
			log.info("Post with id 1: {}", byId);
		};
	}
}
