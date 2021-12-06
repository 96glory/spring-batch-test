package com.glory.springbatch.batch.tasklet.step;

import com.glory.springbatch.model.Line;
import com.glory.springbatch.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

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
