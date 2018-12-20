package spring.cloud.zookeeper.demo.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Desc...
 *
 * @author Charley Wu
 */
@SpringBootApplication
@RefreshScope
@RestController
public class ZookeeperConfigApplication {

  public static void main(String[] args) {
    SpringApplication.run(ZookeeperConfigApplication.class, args);
  }

  @Value("${text}")
  private String text;

  @GetMapping("/text")
  public String text(){
    return text;
  }

}

