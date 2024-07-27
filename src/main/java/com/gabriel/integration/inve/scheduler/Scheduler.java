package com.gabriel.integration.inve.scheduler;

import com.gabriel.integration.inve.model.Category;
import com.gabriel.integration.inve.service.CategoryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class Scheduler {
    @Scheduled(cron = "*/10 * * * * *") //10seconds
    public void scehduleTask() throws MalformedURLException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd-MM-yyyy HH:mm:ss.SSS");

        String strDate = dateFormat.format(new Date());

        System.out.println(
                "Cron job Scheduler: Job running at - "
                        + strDate);

        CategoryService category = new CategoryService();
       String port = category.getCategoryURL().toString();

        System.out.println("This job is running every 10 seconds" +"\n ");


    }
}