package com.glory.springbatch.batch.chunk.step;

import com.glory.springbatch.model.Line;
import com.glory.springbatch.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

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
