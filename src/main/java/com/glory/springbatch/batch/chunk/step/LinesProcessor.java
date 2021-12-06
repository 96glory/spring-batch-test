package com.glory.springbatch.batch.chunk.step;

import com.glory.springbatch.model.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
