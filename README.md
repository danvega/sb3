# Spring Boot 3 and Beyond

This is the demo code for my talk, Spring Boot 3 and Beyond.

## Agenda

- Spring Initializr
  - web,postgresql, docker-compose, jdbc, devtools, actuator, graalvm native support
- Open the project in IntelliJ
  - pom.xml
  - Docker Composer
- Run the application
  - Tomcat 10.1 & Servlet API 6.0 (Jakarta EE 10)
- Model
  - Post Package
  - Post Class
  - Post Record
  - Validation
- Database Abstractions (Picking the correct one for your project)
  - Connect to a database
  - No tables (yet)
  - `schema.sql` / `application.properties`
  - JdbcTemplate
    - create
    - read
    - PostController -> findAll()
  - Jdbc Client
  - Spring Data JDBC
- Rest Client
  - Rest Template
  - Rest Client
  - Http Interfaces
- Virtual Threads
- Native Image

## Spring Initializr

- [Spring Initializr](https://start.spring.io/)
- [This Project](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.2.2&packaging=jar&jvmVersion=21&groupId=dev.danvega&artifactId=sb3&name=sb3&description=Demo%20project%20for%20Spring%20Boot&packageName=dev.danvega.sb3&dependencies=web,postgresql,docker-compose,jdbc,validation,native,devtools,actuator)


## Open the project in IntelliJ

Examine the following files: 

- `pom.xml`
- `docker-compose.yml`

## Run the Application 

Servlet 6.0 (Jakarta EE 10)

```properties
2024-02-13T13:54:45.913-05:00  INFO 97480 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.18]
```

## Database Abstraction Level

Picking the correct abstraction level for the project.

### Jdbc Template

```java
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
        if (update == 1) {
            log.info("Post created: {}", post);
        } else {
            log.error("Post not created: {}", post);
        }
    }

    public List<Post> findAll() {
        return jdbcTemplate.query(
                "SELECT id, user_id, title, body FROM Post",
                (rs, rowNum) -> new Post(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("title"),
                        rs.getString("body")
                )
        );
    }
}
```

### Jdbc Client

```java
@Repository
public class PostRepository {

    private static final Logger log = LoggerFactory.getLogger(PostRepository.class);
    private final JdbcClient jdbcClient;

    public PostRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<Post> findAll() {
        return jdbcClient.sql("SELECT * FROM post")
                .query(Post.class)
                .list();
    }

    public Optional<Post> findById(String id) {
        return jdbcClient.sql("SELECT id,user_id,title,body FROM post WHERE id = :id")
                .param("id", id)
                .query(Post.class)
                .optional();
    }

    public void create(Post post) {
        int update = jdbcClient.sql("INSERT INTO post (id, user_id, title, body) VALUES (?, ?, ?, ?)")
                .params(List.of(post.id(), post.userId(), post.title(), post.body()))
                .update();
        log.info("Inserted {} rows", update);
    }

    void update(Post post, String id) {
        var updated = jdbcClient.sql("UPDATE post SET user_id = ?, title = ?, body = ? where id = ?")
                .params(List.of(post.userId(),post.title(),post.body(), id))
                .update();

        log.info("Updated {} row(s)", updated);
    }

    void delete(String id) {
        var updated = jdbcClient.sql("DELETE from post where id = :id")
                .param("id", id)
                .update();

        log.info("Deleted {} row(s)", updated);
    }
}

```

### Spring Data JDBC

```java
@RestController
@RequestMapping("/posts")
class PostController {

    private final PostRepository postRepository;

    PostController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GetMapping("")
    List<Post> findAll() {
        return postRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    List<Post> findByUserId(@PathVariable Integer userId) {
        return postRepository.findByUserId(userId);
    }

}
```

```java
public interface PostRepository extends ListCrudRepository<Post,Integer> {

    List<Post> findByUserId(Integer userId);

}
```

```java
public record Post(
        @Id
        Integer id,
        Integer userId,
        @NotEmpty
        String title,
        String body,
        @Version
        Integer version
) {

}
```


## Rest Client

### RestTemplate

```java
@Component
public class PostClient {

    private final RestTemplate restTemplate;

    public PostClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    /*
     * Call JsonPlaceholderService to get a list of posts
     */
    public List<Post> findAll() {
        return restTemplate.exchange(
                "https://jsonplaceholder.typicode.com/posts",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Post>>() {
                }
        ).getBody();
    }

}
```

### RestClient 


```java
@Component
public class PostClient {

    private final RestClient restClient;

    public PostClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://jsonplaceholder.typicode.com/")
                .build();
    }

    public List<Post> findAll() {
        return restClient.get()
                .uri("/posts")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

}
```

### Http Interfaces

```java
public interface PostClient {

    @GetExchange("/posts")
    List<Post> findAll();

    @GetExchange("/posts/{id}")
    Post findById(@PathVariable Integer id);

}
```

```java
@Bean
PostClient postClient() {
    RestClient restClient = RestClient.create("https://jsonplaceholder.typicode.com/");
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
    return factory.createClient(PostClient.class);
}
```

## Virtual Threads

```properties
spring.threads.virtual.enabled=true
```

```java
    @GetMapping("")
    List<Post> findAll() {
        log.info("Thread: {}", Thread.currentThread());
        return postRepository.findAll();
    }
```

```properties
2024-02-13T16:12:28.117-05:00  INFO 11869 --- [omcat-handler-0] dev.danvega.sb3.post.PostController      : Thread: VirtualThread[#76,tomcat-handler-0]/runnable@ForkJoinPool-1-worker-1
```

## Native Images

https://docs.spring.io/spring-boot/docs/current/reference/html/native-image.html

mvn -Pnative native:compile