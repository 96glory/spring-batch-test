package com.glory.springbatch.batch.tasklet;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.jupiter.api.Assertions.*;

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