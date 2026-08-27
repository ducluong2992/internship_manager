package com.qlnv.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private String getFrontendStaticPath() {
        File f1 = new File("../frontend/static");
        if (f1.exists()) return f1.getAbsolutePath() + File.separator;
        File f2 = new File("frontend/static");
        if (f2.exists()) return f2.getAbsolutePath() + File.separator;
        File f3 = new File("static");
        if (f3.exists()) return f3.getAbsolutePath() + File.separator;
        return "file:../frontend/static/";
    }

    private String getFrontendPath() {
        File f1 = new File("../frontend");
        if (f1.exists()) return f1.getAbsolutePath() + File.separator;
        File f2 = new File("frontend");
        if (f2.exists()) return f2.getAbsolutePath() + File.separator;
        return "file:../frontend/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String staticPath = getFrontendStaticPath();
        String frontendPath = getFrontendPath();

        // Serve /static/**
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + (staticPath.endsWith(File.separator) ? staticPath : staticPath + File.separator))
                .setCacheControl(CacheControl.noStore());

        // Serve root & SPA static files
        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + (frontendPath.endsWith(File.separator) ? frontendPath : frontendPath + File.separator))
                .setCacheControl(CacheControl.noStore())
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        // If not an API route and not a static file, fallback to index.html for SPA
                        if (!resourcePath.startsWith("api") &&
                            !resourcePath.startsWith("auth") &&
                            !resourcePath.startsWith("admin") &&
                            !resourcePath.startsWith("users") &&
                            !resourcePath.startsWith("employees") &&
                            !resourcePath.startsWith("schedule") &&
                            !resourcePath.startsWith("overtime") &&
                            !resourcePath.startsWith("documents") &&
                            !resourcePath.startsWith("ai-config") &&
                            !resourcePath.startsWith("chat")) {
                            return location.createRelative("index.html");
                        }
                        return null;
                    }
                });
    }
}
