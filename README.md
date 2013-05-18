jblink 
======

Java implementation of the Blink protocol (http://blinkprotocol.org)
by Pantor Engineering AB (http://www.pantor.com).

Documentation including an introduction to jblink can be found at
build/docs/javadoc/index.html.

BUILD
=====

Build jblink

   gradle build

RUN SAMPLES
===========

Build the sample code.

   gradle compilesampleJava

Start the test server that responds to 'ping' with 'pong', using the
blink schema pingpong.blink, using tcp port 4711:

   java -cp build/libs/jblink.jar:build/classes/sample com.pantor.test.TestServer src/sample/pingpong.blink 4711

Start the test client that sends 'ping' messages to the server:

   java -cp build/libs/jblink.jar:build/classes/sample com.pantor.test.TestClient src/sample/pingpong.blink localhost:4711

