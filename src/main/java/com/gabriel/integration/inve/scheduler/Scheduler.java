package com.gabriel.integration.inve.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class Scheduler {
    @Scheduled(cron = "*/10 * * * * *") //10seconds
    public void scehduleTask()
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd-MM-yyyy HH:mm:ss.SSS");

        String strDate = dateFormat.format(new Date());

        System.out.println(
                "Cron job Scheduler: Job running at - "
                        + strDate);

        System.out.println("This job is running every 10 seconds" +"\n current Port is" + System.getProperty("server.port"));


    }
}