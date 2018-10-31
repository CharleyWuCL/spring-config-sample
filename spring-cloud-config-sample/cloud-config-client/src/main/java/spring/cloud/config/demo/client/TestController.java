/*
 * Copyright (c) 2018 Oracle. All rights reserved.
 *
 * This material is the confidential property of Oracle Corporation or its
 * licensors and may be used, reproduced, stored or transmitted only in
 * accordance with a valid Oracle license or sublicense agreement.
 */

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
