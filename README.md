# Spring Boot 3 and Beyond

This is the demo code for my talk, Spring Boot 3 and Beyond.

## Agenda

- Spring Initializr
  - web,postgresql,docker-compose,jdbc,devtools, actuator,graalvm
- Open the project in IntelliJ
  - pom.xml
  - Docker Composer
- Run the application
  - Tomcat 10.1 & Servlet API 6.0 (Jakarta EE 10)
- Model
  - Post Package
  - Post Class-
  - Post Record
  - Validation
- Database Abstractions (Picking the correct one for your project)
  - Connect to a database
  - No tables (yet)
  - `schema.sql` / `application.properties`
  - JdbcTemplate
    - create new run
    - find all runs
    - find one run
    - PostController -> findAll()
  - Jdbc Client
    - convert to Jdbc Client
  - Spring Data JDBC
    - convert to Spring Data JDBC
- Rest Client
  - Rest Template
  - Rest Client
  - Http Interfaces
- Production
  - Virtual Threads
  - JAR 
  - Container
  - Native Image

## Spring Initializr

- [Spring Initializr](https://start.spring.io/)
- [This Project](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.2.2&packaging=jar&jvmVersion=21&groupId=dev.danvega&artifactId=sb3&name=sb3&description=Demo%20project%20for%20Spring%20Boot&packageName=dev.danvega.sb3&dependencies=web,postgresql,docker-compose,jdbc,validation,native,devtools,actuator)

## Open the project in IntelliJ

Examine the following files: 

- `pom.xml`
- `docker-compose.yml`

## Run the Application 

Take notice that we are using Tomcat 10.1 / Servlet 6.0 (Jakarta EE 10)

```properties
2024-02-13T13:54:45.913-05:00  INFO 97480 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.18]
```

## Database Abstractions

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
        log.info("Inserted {} rows", update);
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
  private final JdbcTemplate jdbcTemplate;

  public PostRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
    this.jdbcClient = jdbcClient;
    this.jdbcTemplate = jdbcTemplate;
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

  List<Post> findByUserId(Integer userId) {
    return jdbcClient.sql("SELECT * FROM post WHERE user_id = :userId")
            .param("userId", userId)
            .query(Post.class)
            .list();
  }

  public void saveAllPosts(List<Post> posts) {
    posts.forEach(this::create);
  }

  public void saveAll(List<Post> posts) {
    for (Post p : posts) {
      jdbcTemplate.update("INSERT INTO post (id,user_id,title,body) " +
                      "VALUES (?, ?, ?,?)",
              p.id(),
              p.userId(),
              p.title(),
              p.body());
    }
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


## Rest Clients

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

## Production 

Production is the greatest place on the web! We should all want to go there, but we get it, it can be a scary place.

### Virtual Threads

Virtual Threads were introduced in JDK 21 as a solution to the challenges of asynchronous programming. These lightweight threads do not block platform threads, making them highly efficient. In fact, you can spawn millions of Virtual Threads without needing to worry about thread pooling.

Enabling Virtual Threads in your Spring Boot application couldn't be easier. Open the application.properties file and set the following property.

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

Run the application and inspect the logs to see the Virtual Threads in action.

```properties
2024-02-13T16:12:28.117-05:00  INFO 11869 --- [omcat-handler-0] dev.danvega.sb3.post.PostController      : Thread: VirtualThread[#76,tomcat-handler-0]/runnable@ForkJoinPool-1-worker-1
```

### JAR

The spring-boot-loader modules lets Spring Boot support executable jar and war files. If you use the Maven plugin or the Gradle plugin, executable jars are automatically generated, and you generally do not need to know the details of how they work.

To create an executable JAR run the following command:

```shell
./mvnw clean package
```

- clean: Deletes the target folder
- package: Invoke Maven's package phase, which will cause verify, compile and test phases to be invoked in the correct order.

To run the executable JAR with Java run the following command:

```shell
java -jar target/sb3-0.0.1-SNAPSHOT.jar
```

https://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html#appendix.executable-jar

### Containers

In the previous section you learned how to build an Uber JAR and run it on any machine that has a JDK. What if there is no JDK?

This is where containers and specifically Docker containers can help us out. You have already seen some examples of us running a Docker Compose file to run a production grade database locally.

You can create a production version of your Spring Boot application using Dockerfiles, or by using Cloud Native Buildpacks to create optimized docker compatible container images that you can run anywhere.

https://docs.spring.io/spring-boot/docs/current/reference/html/container-images.html#container-images

As long as you have Docker desktop running you can run the following command to create an OCI image:

```shell
./mvnw spring-boot:build-image
```

```shell
docker image ls -a
docker run -it -p8080:8080 sb3:0.0.1-SNAPSHOT
```

To learn more about Packaging OCI Images check out the [documentation](https://docs.spring.io/spring-boot/docs/3.0.1/maven-plugin/reference/htmlsingle/#build-image)

If you want to do "./mvnw -Pnative spring-boot:build-image" on your Apple Silicon or Raspberry Pi then [this is your solution](https://github.com/dashaun/paketo-arm64).

## Native Images

Spring Boot 3.0 applications can now be converted into GraalVM native images which can provide significant memory and startup-up performance improvements. Spring Boot requires GraalVM 22.3 or later and Native Build Tools Plugin 0.9.17 or later to build native images. If you would like to learn more about GraalVM Native Image Support you can check out the [reference documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/native-image.html).

With the native profile active, you can invoke the native:compile goal to trigger native-image compilation:

```shell
mvn -Pnative native:compile
```
The result will be a native executable in the `target/` directory.

**Notes**
- There is a bug with Spring Data JDBC and native images that you can read about [here](https://github.com/spring-projects/spring-data-commons/issues/3025).

```java
@ImportRuntimeHints(MyRuntimeHints.ResourcesRegistrar.class)
@Configuration
public class MyRuntimeHints {

    static class ResourcesRegistrar implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            try {
                hints.reflection().registerType(TypeReference.of("org.springframework.data.domain.Unpaged"),
                        builder -> builder
                                .withMembers(MemberCategory.values()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
```