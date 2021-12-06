# Spring Batch - Tasklets vs Chunks

> 참고 및 번역 : [https://www.baeldung.com/spring-batch-tasklet-chunk](https://www.baeldung.com/spring-batch-tasklet-chunk)
> 작성한 코드 : [https://github.com/96glory/spring-boot-test](https://github.com/96glory/spring-boot-test)

## 1. 소개

- Spring Batch는 tasklet 방식과 chunk 방식을 제공한다.

## 2. Dependencies

- `spring-batch-core`와 `spring-batch-test` 의존성이 필요하다.
- maven:

  ```xml
  <!-- ... -->

  <dependency>
    <groupId>org.springframework.batch</groupId>
    <artifactId>spring-batch-core</artifactId>
    <version>4.3.0</version>
  </dependency>
  <dependency>
    <groupId>org.springframework.batch</groupId>
    <artifactId>spring-batch-test</artifactId>
    <version>4.3.0</version>
    <scope>test</scope>
  </dependency>

  <!-- ... -->
  ```

## 3. 요구사항

- 아래와 같이 이름과 생년월일이 작성된 CSV 파일을
  ```
  Mae Hodges,10/22/1972
  Gary Potter,02/22/1953
  Betty Wise,02/17/1968
  Wayne Rose,04/06/1977
  Adam Caldwell,09/27/1995
  Lucille Phillips,05/14/1992
  ```
  나이를 계산한 CSV 파일로 변환하는 batch job을 만들고자 한다.
  ```
  Mae Hodges,45
  Gary Potter,64
  Betty Wise,49
  Wayne Rose,40
  Adam Caldwell,22
  Lucille Phillips,25
  ```

## 4. Tasklet 방식

### 4.1 Tasklet 소개

- Tasklet 이란 한 step 내에서 단일 task를 수행하는 것이다. Tasklet Job은 여러 step으로 구성된다. 긱 step은 정의된 task 단 하나만 수행해야 한다.
- 요구사항대로 step을 정의하면 크게 3 step으로 나눌 수 있다.

  1. input CSV 파일로부터 라인들을 읽어온다.

  ```java
  public class LinesReader implements Tasklet {
      // ...
  }
  ```

  2. input CSV 파일 내의 모든 사람들의 나이를 계산한다.

  ```java
  public class LinesProcessor implements Tasklet {
      // ...
  }
  ```

  3. output CSV 파일에 사람들의 이름과 나이를 작성한다.

  ```java
  public class LinesWriter implements Tasklet {
      // ...
  }
  ```

- 위와 같이, 모든 step에는 `Tasklet` 인터페이스를 구현해야 한다. `execute` 함수를 오버라이드하여 각 step에서 할 일을 구현한다.
  ```java
  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    // ...
  }
  ```

### 4.2 Configuration

- tasklet job을 정의하는 config 파일을 만든다.

```java
package com.glory.springbatch.batch.tasklet;

// import 생략

@Configuration
@EnableBatchProcessing
public class TaskletsConfig {

    @Autowired
    private JobBuilderFactory jobs;

    @Autowired
    private StepBuilderFactory steps;

    // JUnit 테스트를 위한 파일
    @Bean
    public JobLauncherTestUtils jobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }

    @Bean
    public LinesReader linesReader() {
        return new LinesReader();
    }

    @Bean
    public LinesProcessor linesProcessor() {
        return new LinesProcessor();
    }

    @Bean
    public LinesWriter linesWriter() {
        return new LinesWriter();
    }

    // 일반 클래스를 Step 으로 구현
    @Bean
    protected Step readLines() {
        return steps
                .get("readLines")
                .tasklet(linesReader())
                .build();
    }

    @Bean
    protected Step processLines() {
        return steps
                .get("processLines")
                .tasklet(linesProcessor())
                .build();
    }

    @Bean
    protected Step writeLines() {
        return steps
                .get("writeLines")
                .tasklet(linesWriter())
                .build();
    }

    // Step의 순서 및 실제 Job을 구현한 Job
    @Bean
    public Job job() {
        return jobs
                .get("taskletsJob")
                .start(readLines())
                .next(processLines())
                .next(writeLines())
                .build();
    }

}
```
