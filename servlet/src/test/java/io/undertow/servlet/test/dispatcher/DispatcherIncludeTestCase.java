/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.undertow.servlet.test.dispatcher;

import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;

import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.test.SimpleServletServerTestCase;
import io.undertow.servlet.test.runner.HttpClientUtils;
import io.undertow.servlet.test.runner.ServletServer;
import io.undertow.servlet.test.util.MessageFilter;
import io.undertow.servlet.test.util.MessageServlet;
import io.undertow.servlet.test.util.TestClassIntrospector;
import io.undertow.servlet.test.util.TestResourceLoader;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(ServletServer.class)
public class DispatcherIncludeTestCase {


    @BeforeClass
    public static void setup() throws ServletException {

        final PathHandler root = new PathHandler();
        final ServletContainer container = ServletContainer.Factory.newInstance(root);

        DeploymentInfo builder = new DeploymentInfo()
                .setClassLoader(SimpleServletServerTestCase.class.getClassLoader())
                .setContextPath("/servletContext")
                .setClassIntrospecter(TestClassIntrospector.INSTANCE)
                .setDeploymentName("servletContext.war")
                .setResourceLoader(TestResourceLoader.NOOP_RESOURCE_LOADER)
                .addServlet(
                        new ServletInfo("include", MessageServlet.class)
                                .addInitParam(MessageServlet.MESSAGE, "included")
                                .addMapping("/include"))
                .addServlet(
                        new ServletInfo("dispatcher", IncludeServlet.class)
                                .addMapping("/dispatch"))
                .addFilter(
                        new FilterInfo("notIncluded", MessageFilter.class)
                                .addInitParam(MessageFilter.MESSAGE, "Not Included"))
                .addFilter(
                        new FilterInfo("inc", MessageFilter.class)
                                .addInitParam(MessageFilter.MESSAGE, "Path!"))
                .addFilter(
                        new FilterInfo("nameFilter", MessageFilter.class)
                                .addInitParam(MessageFilter.MESSAGE, "Name!"))
                .addFilterUrlMapping("notIncluded", "/include", DispatcherType.REQUEST)
                .addFilterUrlMapping("inc", "/include", DispatcherType.INCLUDE)
                .addFilterServletNameMapping("nameFilter", "include", DispatcherType.INCLUDE);


        DeploymentManager manager = container.addDeployment(builder);
        manager.deploy();
        manager.start();

        ServletServer.setRootHandler(root);
    }

    @Test
    public void testPathBasedInclude() throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpGet get = new HttpGet(ServletServer.getDefaultServerAddress() + "/servletContext/dispatch");
            get.setHeader("include", "/include");
            HttpResponse result = client.execute(get);
            Assert.assertEquals(200, result.getStatusLine().getStatusCode());
            final String response = HttpClientUtils.readResponse(result);
            Assert.assertEquals(IncludeServlet.MESSAGE + "Path!Name!included", response);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Test
    public void testNameBasedInclude() throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpGet get = new HttpGet(ServletServer.getDefaultServerAddress() + "/servletContext/dispatch");
            get.setHeader("include", "include");
            get.setHeader("name", "true");
            HttpResponse result = client.execute(get);
            Assert.assertEquals(200, result.getStatusLine().getStatusCode());
            final String response = HttpClientUtils.readResponse(result);
            Assert.assertEquals(IncludeServlet.MESSAGE + "Name!included", response);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }


}
