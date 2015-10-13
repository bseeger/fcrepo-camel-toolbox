/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.camel.indexing.disk;

import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.IOUtils;
import org.fcrepo.camel.JmsHeaders;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author escowles@princeton.edu
 * @since 2015-10-13
 */
public class RouteTest extends CamelBlueprintTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private static final String baseURL = "http://localhost/rest";
    private static final String fileID = "/file1";
    private static final long timestamp = 1428360320168L;
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String auditContainer = "/audit";

    private static final String removed = REPOSITORY + "NODE_REMOVED";
    private static final String added = REPOSITORY + "NODE_ADDED";

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
         final Properties props = new Properties();
         props.put("indexing.predicate", "true");
         props.put("audit.container", auditContainer);
         props.put("input.stream", "seda:foo");
         props.put("reindex.stream", "seda:bar");
         props.put("indexing.predicate", "true");
         return props;
    }


    @Test
    public void testDeletesNotRouted() throws Exception {
        testRoute(fileID, removed, "indexable.rdf", "FcrepoDiskRouter", "mock:direct:index.disk", 0);
    }


    @Test
    public void testUpdatesAreRouted() throws Exception {
        testRoute(fileID, added, "indexable.rdf", "FcrepoDiskRouter", "mock:direct:index.disk", 1);
    }


    @Test
    public void testAuditFilter() throws Exception {
        testRoute(auditContainer + fileID, added, "indexable.rdf", "FcrepoDiskIndexer", "mock:direct:update.disk", 0);
    }

    @Test
    public void testAuditFilterExactMatch() throws Exception {
        testRoute(auditContainer, added, "indexable.rdf", "FcrepoDiskIndexer", "mock:direct:update.disk", 0);
    }

    @Test
    public void testAuditFilterNearMatch() throws Exception {
        testRoute(auditContainer + "orium" + fileID, added, "indexable.rdf", "FcrepoDiskIndexer",
                  "mock:direct:update.disk", 1);
    }

    @Test
    public void testIndexRouterIndexable() throws Exception {
        testRoute(fileID, added, "indexable.rdf", "FcrepoDiskIndexer", "mock:direct:update.disk", 1);
    }

    private static Map<String,Object> createEvent(final String identifier, final String eventTypes,
            final String eventProperties) {

        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsHeaders.BASE_URL, baseURL);
        headers.put(JmsHeaders.IDENTIFIER, identifier);
        headers.put(JmsHeaders.TIMESTAMP, timestamp);
        headers.put(JmsHeaders.USER, userID);
        headers.put(JmsHeaders.USER_AGENT, userAgent);
        headers.put(JmsHeaders.EVENT_TYPE, eventTypes);
        headers.put(JmsHeaders.PROPERTIES, eventProperties);
        return headers;
    }

    private void testRoute(final String path, final String eventTypes, final String dataFile, final String sourceRoute,
                           final String destinationRoute, final int expected) throws Exception {

        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition(sourceRoute).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        getMockEndpoint(destinationRoute).expectedMessageCount(expected);

        template.sendBodyAndHeaders(
                IOUtils.toString(ObjectHelper.loadResourceAsStream(dataFile), "UTF-8"),
                createEvent(path, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }

}
