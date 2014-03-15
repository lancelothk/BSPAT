package web;

import BSPAT.Utilities;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.io.IOException;

/**
 * Created by lancelothk on 3/15/14.
 * change script permission when context initialized
 */
@WebListener
public class BSPATServletContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        String toolsPath = servletContextEvent.getServletContext().getRealPath("") + "/tools/";
        String setFileExecution = String.format("./setFileExecution.sh %s", toolsPath);
        try {
            Utilities.callCMD("chmod u+x setFileExecution.sh", new File(toolsPath));
            Utilities.callCMD(setFileExecution, new File(toolsPath));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
