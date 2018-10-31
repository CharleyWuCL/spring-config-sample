/*
 * Copyright (c) 2018 Oracle. All rights reserved.
 *
 * This material is the confidential property of Oracle Corporation or its
 * licensors and may be used, reproduced, stored or transmitted only in
 * accordance with a valid Oracle license or sublicense agreement.
 */

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

