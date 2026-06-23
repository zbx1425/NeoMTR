package com.lx862.tprobe3.servlet;

import mtr.MTR;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class FrontendServlet extends DefaultServlet {
    private static final String BASE_RESOURCE = "/assets/mtrtv/web/index.html";
    private static URI baseUrl;

    public static void probeSiteFile() throws URISyntaxException {
        // Must resolve the parent from an existing file, not directory. Otherwise this can cause issue in NF
        baseUrl = new URI(MTR.class.getResource(BASE_RESOURCE).toString().replace("/index.html", ""));
    }

    @Override
    public org.eclipse.jetty.util.resource.Resource getResource(String pathInContext) {
        String relPath = pathInContext.replace("/tviewer", ""); // TODO: Better way to strip
        String jarPath = baseUrl.toString() + relPath;

        try {
            URL url = new URI(jarPath).toURL();
            return Resource.newResource(url);
        } catch (Exception e) {
            return null;
        }
    }
}
