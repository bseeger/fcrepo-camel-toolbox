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

import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.fcrepo.camel.JmsHeaders.EVENT_TYPE;
import static org.fcrepo.camel.JmsHeaders.IDENTIFIER;
import static org.fcrepo.camel.RdfNamespaces.INDEXING;
import static org.fcrepo.camel.RdfNamespaces.RDF;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.slf4j.Logger;

/**
 * A content router for handling JMS events.
 *
 * @author escowles@princeton.edu
 * @since 2015-10-13
 */
public class DiskRouter extends RouteBuilder {

    private static final Logger logger = getLogger(DiskRouter.class);

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", RDF);
        ns.add("indexing", INDEXING);

        final XPathBuilder indexable = new XPathBuilder(
                String.format("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='%s']", INDEXING + "Indexable"));
        indexable.namespaces(ns);

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .log("Index Routing Error: ${routeId}");

        /**
         * route anything other than a DELETE operation to the index queue.
         */
        from("{{input.stream}}")
            .routeId("FcrepoDiskRouter")
            .filter(not(header(EVENT_TYPE).isEqualTo(REPOSITORY + "NODE_REMOVED")))
                .to("direct:index.disk");

        /**
         * Handle re-index events
         */
        from("{{disk.reindex.stream}}")
            .routeId("FcrepoTriplestoreReindex")
            .to("direct:index.disk");

        /**
         * Based on an item's metadata, determine if it is indexable.
         */
        from("direct:index.disk")
            .routeId("FcrepoDiskIndexer")
            .filter(not(or(header(IDENTIFIER).startsWith(simple("{{audit.container}}/")),
                    header(IDENTIFIER).isEqualTo(simple("{{audit.container}}")))))
            .removeHeaders("CamelHttp*")
            .to("fcrepo:{{fcrepo.baseUrl}}")
            .filter(or(simple("{{indexing.predicate}} != 'true'"), indexable))
                .to("direct:update.disk");

        /**
         * Perform the sparql update.
         */
        from("direct:update.disk")
            .routeId("FcrepoDiskUpdater")
            .to("fcrepo:{{fcrepo.baseUrl}}?accept=application/n-triples")
            .process(new DiskUpdateProcessor(prop("disk.baseDir"), prop("disk.extension"), prop("disk.serialization")))
            .log(LoggingLevel.INFO, logger, "Indexing Object to Disk: ${headers[org.fcrepo.jms.baseUrl]}" +
                    "${headers[org.fcrepo.jms.identifier]}");
    }
    private String prop(final String key) throws Exception {
        return getContext().resolvePropertyPlaceholders("{{" + key + "}}");
    }
}
