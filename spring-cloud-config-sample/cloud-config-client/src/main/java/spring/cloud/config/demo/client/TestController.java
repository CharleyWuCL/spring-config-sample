package spring.cloud.config.demo.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Desc...
 *
 * @author Charley Wu
 */
@RefreshScope
@RestController
public class TestController {

  @Value("${text}")
  private String text;

  @GetMapping("/text")
  public String text(){
    return text;
  }
}
