package dev.danvega.sb3.post;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends ListCrudRepository<Post,Integer> {

    List<Post> findByUserId(Integer userId);

}