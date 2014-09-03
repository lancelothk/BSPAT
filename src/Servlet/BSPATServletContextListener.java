package Servlet;

import BSPAT.Utilities;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by lancelothk on 3/15/14.
 * change script permission when context initialized
 */
@WebListener
public class BSPATServletContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        String toolsPath = servletContextEvent.getServletContext().getRealPath("") + "/tools/";
        try {
            Utilities.callCMD(Arrays.asList("chmod u+x", "setFileExecution.sh"), new File(toolsPath), null);
            Utilities.callCMD(Arrays.asList("./setFileExecution.sh", toolsPath), new File(toolsPath), null);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("chmod failed!");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}