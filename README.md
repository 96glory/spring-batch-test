# Spring Batch - Tasklets vs Chunks

> 참고 및 번역 : [https://www.baeldung.com/spring-batch-tasklet-chunk](https://www.baeldung.com/spring-batch-tasklet-chunk) <br>
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

  <!-- csv 읽기 위한 의존성 -->
  <dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>4.1</version>
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
- 위 요구사항을 Tasklet 방식과 Chunk 방식으로 처리해보고자 한다.

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

- 위와 같이, 모든 step에는 `Tasklet` 인터페이스를 구현해야 한다. `execute` 함수를 오버라이드하여 각 step에서 수행할 일을 구현한다.
  ```java
  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    // ...
  }
  ```

### 4.2 Configuration

- tasklet job을 정의하는 config 파일을 만든다.
- **4.1** 과정에서 생성한 class를 `@Bean`으로 설정한 뒤, job 순서 및 내용을 정의하면 된다.

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

### 4.3 Model and Utils

#### 4.3.1 클래스 `Line`

- CSV 파일을 조작하기 위한 class `Line`을 생성한다.
- `Line`은 반드시 `Serializable`을 인터페이스 받아야 한다. `Line`은 Tasklet의 step 사이에서 DTO의 역할을 하기 때문이다. Spring Batch의 step 간 전송되는 객체는 serializable 가능해야 한다.

```java
package com.glory.springbatch.model;

// import 생략

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Line implements Serializable {

    private String name;
    private LocalDate dob;
    private Long age;

    public Line(String name, LocalDate dob) {
        this.name = name;
        this.dob = dob;
    }
}
```

#### 4.3.2 유틸리티 `FileUtils`

- CSV 파일로부터 Line을 읽거나, CSV 파일에 Line을 쓸 수 있는 클래스 `FileUtils`을 생성한다.

```java
package com.glory.springbatch.util;

// import 생략

public class FileUtils {

    private final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    private String fileName;
    private CSVReader CSVReader;
    private CSVWriter CSVWriter;
    private FileReader fileReader;
    private FileWriter fileWriter;
    private File file;

    public FileUtils(String fileName) {
        this.fileName = fileName;
    }

    // readNext의 전후처리를 위해, wrapper method로 구현되었음.
    public Line readLine() {
        try {
            if (CSVReader == null) {
                initReader();
            }

            String[] line = CSVReader.readNext();

            if (line == null) return null;

            return new Line(line[0], LocalDate.parse(line[1], DateTimeFormatter.ofPattern("MM/dd/yyyy")));
        } catch (Exception e) {
            logger.error("Error while reading line in file: " + this.fileName);
            return null;
        }
    }

    // writeNext의 전후처리를 위해, wrapper method로 구현되었음.
    public void writeLine(Line line) {
        try {
            if (CSVWriter == null) {
                initWriter();
            }

            String[] lineStr = new String[2];
            lineStr[0] = line.getName();
            lineStr[1] = line.getAge().toString();

            CSVWriter.writeNext(lineStr);
        } catch (Exception e) {
            logger.error("Error while writing line in file: " + this.fileName);
        }
    }

    private void initReader() throws Exception {
        ClassLoader classLoader = this
                .getClass()
                .getClassLoader();

        if (file == null) {
            file = new File(classLoader
                    .getResource(fileName)
                    .getFile());
        }

        if (fileReader == null) fileReader = new FileReader(file);
        if (CSVReader == null) CSVReader = new CSVReader(fileReader);
    }

    private void initWriter() throws Exception {
        if (file == null) {
            file = new File(fileName);
            file.createNewFile();
        }

        if (fileWriter == null) fileWriter = new FileWriter(file, true);
        if (CSVWriter == null) CSVWriter = new CSVWriter(fileWriter);
    }

    public void closeWriter() {
        try {
            CSVWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            logger.error("Error while closing writer.");
        }
    }

    public void closeReader() {
        try {
            CSVReader.close();
            fileReader.close();
        } catch (IOException e) {
            logger.error("Error while closing reader.");
        }
    }

}
```

### 4.4 `LinesReader`

- `Tasklet`에 의해 오버라이드된 메서드 : `execute`
  - `execute` : step에서 행해야 할 작업을 구현한다.
- `StepExecutionListener`에 의해 오버라이드된 메서드 : `beforeStep`, `afterStep`
  - `beforeStep` : `execute`가 실행되기 전 실행되는 메서드
  - `afterStep` : `execute`가 실행된 후 실행되는 메서드

```java
package com.glory.springbatch.batch.tasklet.step;

// import 생략

public class LinesReader implements Tasklet, StepExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(LinesReader.class);

    private List<Line> lines;
    private FileUtils fu;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        lines = new ArrayList<>();
        fu = new FileUtils("input/test.csv"); // resource 디렉토리 하위의 파일을 읽는다.
        logger.debug("Lines Reader initialized.");
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        Line line = fu.readLine();
        while (line != null) {
            lines.add(line);
            logger.debug("Read line: " + line.toString());
            line = fu.readLine();
        }
        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        fu.closeReader();
        stepExecution
                .getJobExecution()
                .getExecutionContext()
                .put("lines", this.lines);
        logger.debug("Lines Reader ended.");

        return ExitStatus.COMPLETED;
    }

}
```

- step 간 데이터를 주고받기 위해서, key-value 형태로 값을 저장하고, 다음 step에서 key를 통해 접근한다.
  ```java
  stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put("lines", this.lines); // key = lines, value = this.lines
  ```

### 4.5 `LinesProcessor`

```java
package com.glory.springbatch.batch.tasklet.step;

// import 생략

public class LinesProcessor implements Tasklet, StepExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(LinesReader.class);

    private List<Line> lines;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext executionContext = stepExecution
                .getJobExecution()
                .getExecutionContext();
        lines = (List<Line>) executionContext.get("lines");
        logger.debug("Lines Processor initialized.");
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        lines.forEach(line -> {
            long age = ChronoUnit.YEARS.between(line.getDob(), LocalDate.now());
            logger.debug("Calculated age " + age + " for line " + line.toString());
            line.setAge(age);
        });

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        logger.debug("Lines Processor ended.");
        return ExitStatus.COMPLETED;
    }

}
```

- 이전 step에서 데이터를 꺼내는 코드에 주목하자.
  ```java
  ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
  lines = (List<Line>) executionContext.get("lines");
  ```

## 4.6 `LinesWriter`

```java
package com.glory.springbatch.batch.tasklet.step;

// import 생략

public class LinesWriter implements Tasklet, StepExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(LinesReader.class);

    private List<Line> lines;
    private FileUtils fu;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
        this.lines = (List<Line>) executionContext.get("lines");
        fu = new FileUtils("output.csv");
        logger.debug("Lines Writer initialized.");
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        lines.forEach(line -> {
            fu.writeLine(line);
            logger.debug("Wrote line " + line.toString());
        });

        return RepeatStatus.FINISHED;
    }


    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        fu.closeWriter();
        logger.debug("Lines Writer ended.");
        return ExitStatus.COMPLETED;
    }

}
```

### 4.7 테스트 코드 작성

- `@ContextConfiguration` 은 job 정의가 존재하는 Spring Context Configuration 클래스를 가리키고 있다.

```java
package com.glory.springbatch.batch.tasklet;

// import 생략

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TaskletsConfig.class)
public class TaskletsConfigTest {

    @Autowired
    JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    public void testTaskletsJob() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

}
```

## 5. Chunk 방식

### 5.1 Chunk 소개

- Chunk는 job의 단위를 step으로 나누는 것이 아니라, job이 수행될 데이터를 고정된 크기로 나누어 Batch를 실행하는 방식이다.
- 고정된 크기로 나누어진 데이터를 Chunk라고 불린다.
- Chunk가 없어질 때까지 job을 수행한다.
- Tasklet 방식과는 수도코드가 약간 바뀌게 된다.

  ```
  Chunk의 크기를 X로 지정

  line을 모두 소모할 때 까지:
    X개의 line만큼 아래 작업 수행:
      1개의 line을 읽는다.
      1개의 line에 대해 작업을 수행한다. (위 예시에서는, age로 변환하는 작업이 되겠다.)
    X개의 line을 output 파일에 작성한다.
  ```

  ```java
  public class LinesReader implements Tasklet {
      // ...
  }
  ```

  ```java
  public class LinesProcessor implements Tasklet {
      // ...
  }
  ```

  ```java
  public class LinesWriter implements Tasklet {
      // ...
  }
  ```

### 5.2 Configuration

- 아래 tasklet은 오로지 1 step으로 이루어져 있다. 하지만, 기존 tasklet과는 달리 데이터 chunk들을 오가며 작동할 reader, writer, processor를 정의한다.
- commit 간격은 1개의 chunk 단위로 지정된다. 아래 예시에는 2로 설정하였다.

```java
package com.glory.springbatch.batch.chunk;

// import 생략

@Configuration
@EnableBatchProcessing
public class ChunksConfig {

    @Autowired
    private JobBuilderFactory jobs;

    @Autowired
    private StepBuilderFactory steps;

    @Bean
    public JobLauncherTestUtils jobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }

    @Bean
    public ItemReader<Line> itemReader() {
        return new LinesReader();
    }

    @Bean
    public ItemProcessor<Line, Line> itemProcessor() {
        return new LinesProcessor();
    }

    @Bean
    public ItemWriter<Line> itemWriter() {
        return new LinesWriter();
    }

    @Bean
    protected Step processLines(ItemReader<Line> reader, ItemProcessor<Line, Line> processor, ItemWriter<Line> writer) {
        return steps.get("processLines").<Line, Line> chunk(2) // chunk 단위 설정
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job job() {
        return jobs
                .get("chunksJob")
                .start(processLines(itemReader(), itemProcessor(), itemWriter()))
                .build();
    }
}
```

### 5.3 `LineReader`

- `LineReader`는 CSV 파일로부터 1 줄을 읽고, `Line`으로 변환하는 작업을 한다. 파일을 읽기 위해 `ItemReader` interface를 구현하였고, `read` 메서드를 오버라이드 하였다.

```java
package com.glory.springbatch.batch.chunk.step;

// import 생략

public class LinesReader implements ItemReader<Line>, StepExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(LinesReader.class);
    private FileUtils fu;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        fu = new FileUtils("input/test.csv");
        logger.debug("Line Reader initialized.");
    }

    @Override
    public Line read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        Line line = fu.readLine();
        if (line != null) {
            logger.debug("Read line : " + line.toString());
        }
        return line;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        fu.closeReader();
        logger.debug("Line Reader ended.");
        return ExitStatus.COMPLETED;
    }
}
```

### 5.4 `LineProcessor`

- `LineProcessor`는 `ItemProcessor`를 상속받아 `process` 메서드를 오버라이드 한다.
- tasklet 방식과는 달리, step 간 데이터를 주고받기 위해 `stepExecution.getJobExecution().getExecutionContext()`를 수행하여 key-value 형태로 가져왔었어야 했다. `itemReader()`, `itemProcessor()`, `itemWriter()` 끼리 아규먼트로 값을 주고받기 때문에, chunk 방식에서는 해당 코드는 생략될 수 있었고, 더욱 간편히 코드를 작성할 수 있다.

```java
package com.glory.springbatch.batch.chunk.step;

// import 생략

public class LinesProcessor implements ItemProcessor<Line, Line>, StepExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(LinesReader.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.debug("Lines Processor initialized.");
    }

    @Override
    public Line process(Line line) throws Exception {
        long age = ChronoUnit.YEARS.between(line.getDob(), LocalDate.now());
        logger.debug("Calculated age " + age + " for line " + line.toString());
        line.setAge(age);
        return line;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        logger.debug("Lines Processor ended.");
        return ExitStatus.COMPLETED;
    }

}
```

### 5.5 `LinesWriter`

- 1 Line씩 처리하던 reader와 processor와는 달리, `LinesWriter`는 청크 단위의 라인의 List<Line>를 받는다.

```java
package com.glory.springbatch.batch.chunk.step;

// import 생략

import java.util.List;

public class LinesWriter implements ItemWriter<Line>, StepExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(LinesReader.class);

    private FileUtils fu;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        fu = new FileUtils("output.csv");
        logger.debug("Lines Writer initialized.");
    }

    @Override
    public void write(List<? extends Line> list) throws Exception {
        list.forEach(line -> {
            fu.writeLine(line);
            logger.debug("Wrote line " + line.toString());
        });
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        fu.closeWriter();
        logger.debug("Lines Writer ended.");
        return ExitStatus.COMPLETED;
    }

}
```

### 5.6 테스트 코드 작성

```java
package com.glory.springbatch.batch.chunk;

// import 생략

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ChunksConfig.class)
public class ChunksConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    public void testChunksJob() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

}
```

## 6. 결론

- Tasklet 방식은 작업의 순서가 바뀔 수 있는 Job에 사용하면 유지보수가 편하다.
- Chunk 방식은 특정 테이블을 페이지 방식으로 읽는 Job이나, 한번에 데이터가 너무 많이 처리되어 메모리에 무리가 갈 수 있어 Chunk 단위로 나눠 메모리의 과부화를 방지하기 위해 사용한다.
