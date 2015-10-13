#Fedora Indexing Service (Disk)

This application serializes RDF to disk for [Fedora4](http://fcrepo.org) objects that are
updated.

Additional background information on this service is available on the Fedora Wiki on the
[Integration Services page](https://wiki.duraspace.org/display/FEDORA4x/Integration+Services).

##Building

To build this project use

    MAVEN_OPTS="-Xmx1024m" mvn install

##Running from the command line

To run the project you can execute the following Maven goal

    mvn camel:run

##Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or 
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.camel/fcrepo-camel-toolbox/LATEST/xml/features
    feature:install fcrepo-indexing-disk

##Deploying in Tomcat/Jetty

If you intend to deploy this application in a web container such as Tomcat or Jetty,
please refer to the documentation in the
[fcrepo-camel-webapp](https://github.com/fcrepo4-exts/fcrepo-camel-toolbox/tree/master/fcrepo-camel-webapp)
project.

##Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.indexing.disk.cfg`. The following
values are available for configuration:

In the event of failure, the maximum number of times a redelivery will be attempted.

    error.maxRedeliveries=10

If the fedora repository requires authentication, the following values
can be set:

    fcrepo.authUsername=<username>
    fcrepo.authPassword=<password>
    fcrepo.authHost=<host realm>

The baseUrl for the fedora repository.

    fcrepo.baseUrl=localhost:8080/fcrepo/rest

If you would like to index only those objects with a type `indexing:Indexable`,
set this property to `true`

    indexing.predicate=false

The JMS connection URI, used for connecting to a local or remote ActiveMQ broker.

    jms.brokerUrl=tcp://localhost:61616

The camel URI for the incoming message stream.

    input.stream=activemq:topic:fedora

The camel URI for handling reindexing events.

    disk.reindex.stream=activemq:queue:disk.reindex

The base directory where RDF should be written.

    disk.baseDir=/path/to/directory

The filename extension to use for RDF files.

    disk.extension=ttl

The RDF serialization to use (one of JSON-LD, N3, N-TRIPLE, RDF/XML, RDF/XML-ABBREV, TURTLE".

    disk.serialization=TURTLE

The location of the internal Audit trail, if using the `fcrepo-audit` extension module.
Nodes at this location will not be indexed.

    audit.container=/audit

By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

