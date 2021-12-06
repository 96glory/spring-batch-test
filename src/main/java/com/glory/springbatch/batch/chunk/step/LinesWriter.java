package com.glory.springbatch.batch.chunk.step;

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
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;

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
