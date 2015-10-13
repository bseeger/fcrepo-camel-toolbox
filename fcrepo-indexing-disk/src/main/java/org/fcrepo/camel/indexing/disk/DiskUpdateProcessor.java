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

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.camel.processor.ProcessorUtils.langFromMimeType;
import static org.fcrepo.camel.processor.ProcessorUtils.getSubjectUri;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import com.hp.hpl.jena.rdf.model.Model;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

/**
 * A processor for serializing Fedora objects to RDF files.
 *
 * @author escowles@princeton.edu
 * @since 2015-10-13
 */
public class DiskUpdateProcessor implements Processor {
    private File baseDir;
    private String extension;
    private String serialization;

    /**
     * Constructor with base directory specified.
     * @param baseDir Base of directory tree where RDF files should be written.
     * @param extension Filename extension to use for RDF files.
     * @param serialization RDF serialization to use (e.g., "JSON-LD", "N3", "N-TRIPLE", "RDF/XML",
     *    "RDF/XML-ABBREV", or "TURTLE").
    **/
    public DiskUpdateProcessor(final String baseDir, final String extension, final String serialization) {
        this.baseDir = new File(baseDir);
        this.extension = extension;
        this.serialization = serialization;
    }

    /**
     * Process a message.
     * @param exchange the current camel message exchange.
    **/
    public void process(final Exchange exchange) throws IOException {
        final Message in = exchange.getIn();

        final String subject = getSubjectUri(in);
        final Model model = createDefaultModel().read(in.getBody(InputStream.class), subject,
                langFromMimeType(in.getHeader(Exchange.CONTENT_TYPE, String.class)));

        final FileOutputStream serializedGraph = new FileOutputStream(fileFor(subject));
        model.write(serializedGraph, serialization);
        serializedGraph.close();
    }

    /**
     * Map a URI to a File object.
     * @param id The record's URI
     * @return the File where a given record should be serialized
     * @throws IOException on error creating directory structure
    **/
    private File fileFor(final String uri) throws IOException {
        final String sep = "//";
        final String fn = uri.substring(uri.indexOf(sep) + sep.length()).replaceAll(":", "/");
        final File f = new File(baseDir, URLEncoder.encode(fn, "UTF-8").replaceAll("%2F", "/") + extension);

        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        return f;
    }

}
