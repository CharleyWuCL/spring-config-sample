### OverView

Spring Cloud作为时下微服务开发最为流行的框架，除了提供服务发现、网关、负载均衡与熔断等组件外，另外也提供了如配置中心，分布式追踪等辅助组件。今天就来介绍下Spring Cloud下的配置中心组件。

Spring Cloud下配置中心有两种实现方式，一种是`Spring-cloud-config`，另一种则是`spring-cloud-zookeeper-config`。



### Cloud Config VS Cloud Zookeeper

**Spring Cloud Config，版本控制**

由Server、Client、Git (SVN|File System|DB) 三个部分来实现配置中心。Server负责将Git中存储配置文件发布成REST接口，为防止Git仓库故障无法获取配置文件，Server会在Git本地缓存暂存配置；Client调用Server的REST接口获取配置；Git则是用来存储配置文件，并且通过Git可以实现配置文件多版本控制。

由于客户端不能主动感知配置文件的变化，只能调用客户端的`/refresh`接口刷新配置，因此要想实现自动刷新的功能需要其他组件配合实现。一种是通过Git的WebHook功能进关联，但此方式需要维护客户端服务列表；另一种是通过Spring Cloud Bus下发变更通知。因此`sping-cloud-config`在配置自动刷新方面实现起来还是相对较为复杂的。

**Spring Cloud Zookeeper，自动刷新**

此项目使用Zookeeper作为配置中心协调者，Zookeeper作为配置中心的服务端，客户端通过`spring-cloud-zookeeper-config`实现配置自动加载与刷新，能够与SpringBoot无缝结合。Zookeeper提供了分层的命名空间，允许客户端自由存储配置信息。但是Zookeeper没有版本控制的功能，需要自行实现。



### Part1: Spring Cloud Config

#### 系统架构

![SpringCloudConfig](https://raw.githubusercontent.com/CharleyWuCL/charleywucl.github.io/master/images/blog/config/SpringCloudConfig.png)

#### 配置拉取

**Server端代码**

build.gradle

```javascript
buildscript {
    ext {
        springBootVersion = '2.0.1.RELEASE'
    }
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
    }
}

apply plugin: 'java'
apply plugin: 'org.springframework.boot'
apply plugin: 'io.spring.dependency-management'

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Finchley.SR1'
    }
}

dependencies {
    compile 'org.springframework.cloud:spring-cloud-config-server'
    compile 'org.springframework.cloud:spring-cloud-starter-bus-kafka'
    compile 'org.springframework.boot:spring-boot-starter-actuator'
}
```

> **特别注意：**SpringBoot与SpringCloud的版本要比配，否则会出现自动刷新不可用的问题。这里用到的版本是SpringBoot: `2.0.1.RELEASE`及SpringCloud: `Finchley.SR1`，其他版本未详细测试。

application.yml

```yml
server:
  port: 8888

spring:
  application:
    name: config-server
  cloud:
  	bus:
  	  #如果需要追踪事件，可以启用trace端点，在/bus-refresh后调用/trace
  	  trace: true
    config:
      server:
        git:
          uri: https://github.com.com/xxx/spring-config-repository.git
          search-paths: config/
          username: xxx
          password: xxx
    stream:
      kafka:
        binder:
          zk-nodes: localhost:2181
          brokers: localhost:9092

#SpringBoot 2.0 需要开启端点才能使用 POST http://localhost:8888/actuator/bus-refresh
management:
  endpoints:
    web:
      exposure:
        include: bus-refresh
```

ConfigServerApplication.java

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigServerApplication.class, args);
  }

}
```



**Client端代码**

build.gradle

```javascript
buildscript {
    ext {
        springBootVersion = '2.0.1.RELEASE'
    }
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
    }
}

apply plugin: 'java'
apply plugin: 'org.springframework.boot'
apply plugin: 'io.spring.dependency-management'

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Finchley.SR1'
    }
}

dependencies {
    compile 'org.springframework.boot:spring-boot-starter-web'
    compile 'org.springframework.cloud:spring-cloud-starter-config'
    compile 'org.springframework.cloud:spring-cloud-starter-bus-kafka'
    compile 'org.springframework.boot:spring-boot-starter-actuator'
}
```

bootstrap.yml

```yml
spring:
  application:
    name: config-client
  cloud:
    config:
      uri: http://localhost:8888
      profile: test
    stream:
      kafka:
        binder:
          zk-nodes: localhost:2181
          brokers: localhost:9092
```

ConfigClientApplication.java

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RefreshScope //自动刷新注解
@RestController
public class ConfigClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigClientApplication.class, args);
  }

  @Value("${text}")
  private String text;

  @GetMapping("/text")
  public String text(){
    return text;
  }
}
```



**Git仓库配置**

config/config-client-test.yml

```yml
server:
    port: 10001

#自动刷新测试配置
text: Hello World
```



**Client端运行结果**

```verilog
2018-10-26 14:12:43.587  INFO 1200 --- [           main] c.c.c.ConfigServicePropertySourceLocator : Fetching config from server at : http://localhost:8888
2018-10-26 14:12:58.076  INFO 1200 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 10001 (http)
2018-10-26 14:12:58.123  INFO 1200 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2018-10-26 14:12:58.124  INFO 1200 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet Engine: Apache Tomcat/8.5.34
2018-10-26 14:13:00.352  INFO 1200 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 10001 (http) with context path ''
2018-10-26 14:13:00.358  INFO 1200 --- [           main] s.c.c.d.client.ConfigClientApplication   : Started ConfigClientApplication in 20.583 seconds (JVM running for 21.917)
```



#### 配置刷新

1. 启动ConfigServer及Client，调用Client的`text`API，输出结果为`Hello World`。

2. 在Git上修改`text`值为`Hello World 1234`并提交；

3. 用`POST`方式调用ConfigServer的`/actuator/bus-refresh`端点刷新配置；
4. 再次调用Client的`/text`API，输出结果为`Hello World 1234`；

> 刷新命令：`curl -X POST http://configServer:8888/actuator/bus-refresh`
>
> 可以通过Git的`WebHook`方式自动触发刷新动作。



### Part2: Spring Cloud Zookeeper

#### 系统架构图

![系统架构](https://raw.githubusercontent.com/CharleyWuCL/charleywucl.github.io/master/images/blog/zookeeper/arch.png)

#### 依赖引入

Maven

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zookeeper-config</artifactId>
    <version>2.0.0.RELEASE</version>
</dependency>
```

Gradle

```javascript
compile('org.springframework.cloud:spring-cloud-starter-zookeeper-config:2.0.0.RELEASE')
```



#### 系统配置

bootstarp.yml

```yml
spring:
  cloud:
    zookeeper:
      enabled: true #Is Zookeeper enabled
      connect-string: localhost:2181 #Connection string to the Zookeeper cluster
      max-retries: 3 #Max number of times to retry
      config:
        root: config/root #Root folder where the configuration for Zookeeper is kept
        defaultContext: application #The name of the default context
```



#### 代码演示

启动时加载配置

```java
public class Example {
    //Server level switch
    @Value("${server}")
    private String serverEnabled;

    //Feature level switch
    @Value("${feature.log}")
    private String featureLogEnabled;
}
```

配置更新时使用`@RefreshScope`自动刷新

```java
@RefreshScope
public class Example {
    //Server level switch
    @Value("${server}")
    private String serverEnabled;

    //Feature level switch
    @Value("${feature.log}")
    private String featureLogEnabled;
}
```

变量的节点需要事先在Zookeeper上配置好。



#### Zookeeper树

![zk结构](https://raw.githubusercontent.com/CharleyWuCL/charleywucl.github.io/master/images/blog/zookeeper/Zookeeper-tree.png)

#### 注意事项

由于应用启动需要从ZK Server上去拉取配置，所以数据节点要预先在ZK上设置好，或者通过application.yml文件给定一个默认值，否则启动会报错。



#### 关于Zookeeper版本

**3.5.x**

Spring-cloud-zookeeper目前最新版本依赖的zookeeper-client依赖树如下：

```javascript
spring-cloud-starter-zookeeper-config:2.0.0.RELEASE
	|
	|--curator-recipes:4.0.1
	|
	|--curator-framework:4.0.1
	|		|
	|		|--zookeeper:3.5.3-beta
	|
	|--curator-test:4.0.1
	|		|
	|		|--zookeeper:3.5.3-beta
```

如果Zookeeper Server版本为3.5.x，则直接使用就可以。

**3.4.x**

如果Zookeeper Server版本为3.4.X，则需要手动进行Client断兼容。方法如下：

Maven

```xml
<!-- Curator Recipes -->
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
    <version>4.0.1</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Curator Test -->
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-test</artifactId>
    <version>2.12.0</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
        </exclusion>
    </exclusions>
    <scope>test</scope>
</dependency>

<!-- Zookeeper -->
<dependency>
	<groupId>org.apache.zookeeper</groupId>
	<artifactId>zookeeper</artifactId>
	<version>3.4.x</version>
</dependency>
```

Gradle

```
compile('org.apache.curator:curator-recipes:4.0.1) {
  exclude group: 'org.apache.zookeeper', module: 'zookeeper'
}
testCompile('org.apache.curator:curator-test:2.12.0') {
  exclude group: 'org.apache.zookeeper', module: 'zookeeper'
}
compile('org.apache.zookeeper:zookeeper:3.4.x')
```

>  **特别注意：**如果要使用3.4.x版本的Zookeeper Client， 那么在使用`TestingServer`时只能依赖`org.apache.curator:curator-test:2.12.0`