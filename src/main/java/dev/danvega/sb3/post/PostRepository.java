package dev.danvega.sb3.post;

import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface PostRepository extends ListCrudRepository<Post,Integer> {

    List<Post> findByUserId(Integer userId);

}
