package dev.danvega.sb3.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostRepository {

    private static final Logger log = LoggerFactory.getLogger(PostRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public PostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(Post post) {
        int update = jdbcTemplate.update(
                "INSERT INTO Post(id,user_id,title,body) VALUES(?,?,?,?)",
                post.id(), post.userId(), post.title(), post.body()
        );
        log.info("Inserted {} rows", update);
    }

    public List<Post> findAll() {
        return jdbcTemplate.query(
                "SELECT id, user_id, title, body FROM post",
                (rs, rowNum) -> new Post(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("title"),
                        rs.getString("body"),
                        null
                )
        );
    }
}