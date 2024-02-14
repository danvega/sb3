package dev.danvega.sb3.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/posts")
class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private final PostRepository postRepository;

    PostController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GetMapping("")
    List<Post> findAll() {
        log.info("Thread: {}", Thread.currentThread());
        return postRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    List<Post> findByUserId(@PathVariable Integer userId) {
        return postRepository.findByUserId(userId);
    }

}
