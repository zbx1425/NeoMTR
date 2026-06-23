package com.lx862.tprobe3.servlet;

import mtr.MTR;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;

import java.net.URL;

public class FrontendServlet extends DefaultServlet {
    private static final String BASE_RESOURCE = "/assets/mtrtv/web";

    @Override
    public org.eclipse.jetty.util.resource.Resource getResource(String pathInContext) {
        String relPath = pathInContext.replace("/tviewer", ""); // TODO: Better way to strip
        String jarPath = BASE_RESOURCE + relPath;

        try {
            URL url = MTR.class.getResource(jarPath);
            if (url == null) return null; // Non-existent file
            return Resource.newResource(url);
        } catch (Exception e) {
            return null;
        }
    }
}
