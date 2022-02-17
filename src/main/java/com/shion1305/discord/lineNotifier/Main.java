package com.shion1305.discord.lineNotifier;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class Main implements ServletContextListener {
    static MainWorker worker;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        worker = new MainWorker();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        worker.stop();
        worker = null;
    }
}
