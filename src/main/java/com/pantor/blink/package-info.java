// Copyright (c) 2013, Pantor Engineering AB
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//
//  * Redistributions in binary form must reproduce the above
//    copyright notice, this list of conditions and the following
//    disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
//  * Neither the name of Pantor Engineering AB nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
//
// IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
// OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
// BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
// USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
// DAMAGE.

/**
   <p>Contains classes for encoding and decoding Blink protocol
   messages as defined at <a
   href="http://blinkprotocol.org/">blinkprotocol.org</a></p>

   <p>The structures of Blink messages are formally defined in a
   schema. A schema is represented by the class {@link
   com.pantor.blink.Schema}. A {@code Schema} instance can hold
   messages defined in multiple schema files.</p>

   <p>This implementation maps messages defined in the schema to
   classes of plain old java objects (POJOs). The mapping is handled
   by an implementation of the {@code ObjectModel} interface.</p>

   <p>The class {@link com.pantor.blink.DefaultObjectModel} provides a default
   implementaion of an object model. It also manages a schema instance
   for convenience.</p>

   <p>Once an object model is setup, it is used by decoders and
   encoders to turn encoded messages to objects and vice versa.</p>

   <p>Assuming a class</p>
   
   <blockquote><pre>public class Foo
{
   public void setValue (String val) { this.val = val; }
   public String getValue () { return val; }
   private String val;
}</pre></blockquote>

   <p>then we can setup an object model with a simple schema. We also create
   a reader for the Blink compact format.</p>

   <blockquote><pre>DefaultObjectModel om = new DefaultObjectModel ();
{@code om.loadSchemaFromString ("Foo/1 -> string Value");}
{@link com.pantor.blink.CompactReader} rd = new CompactReader (om);</pre></blockquote>

   <p>Now we can read some encoded bytes, resulting in an instance of Foo:</p>
   
   <blockquote><pre>DefaultBlock block = new DefaultBlock ();
byte [] data = { 0x07, 0x01, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f {@literal }};
rd.read (data, block);</pre></blockquote>

   <p>The decoder appends decoded messages to a {@code Block} where we can
   later retrieve them throught the {@code getObjects} method:</p>
   
   <blockquote><pre>Foo foo = (Foo)block.getObjects ().get (0);
System.out.println (foo.getValue ()); // prints Hello
</pre></blockquote>

   <p>As an alternative to collecting decoded objects in a {@code
   Block} we can have them dispatched to type-aware observers. It
   works by registering one or more observers with an {@code
   ObserverRegistry}. In its most basic form an observer is an
   implementation of the {@link com.pantor.blink.Observer}
   interface. However, a more flexible and powerful model is provided
   through the {@link com.pantor.blink.DefaultObsRegistry} class.
   This class allows us to register POJOs with mathing observer
   methods.</p>

   <p>Assuming an observer like this:</p>

   <blockquote><pre>public class MyObs
{
   public void onFoo (Foo foo)
   {
      System.out.println (foo.getValue ());
   }
}</pre></blockquote>

   <p>we can setup a reader as follows:</p>

   <blockquote><pre>DefaultObsRegsitry oreg = new DefaultObsRegsitry (om);
MyObs obs = new MyObs ();
oreg.addObserver (obs);
CompactReader rd = new CompactReader (om, oreg);</pre></blockquote>

   <p>When we decode a {@code Foo} message using this reader, the
   decoded object will be routed to the {@code onFoo} method in
   the observer object since it has a signature the matches the
   decoded type.</p>

   <p>In addition to the basic components for handling the schema and
   object model, encoders and decoders, and type dispatching, this
   package contains a {@link com.pantor.blink.Client} and a {@link
   com.pantor.blink.Server} component. They can be used for setting up
   simple Blink-capable network components.</p>

   <h3>Limitations</h3>

   <p>This version has limitations and lacks some features that will
   be added or fixed in subsequent releases:</p>
   
   <ul>
   <li>Message extensions</li>
   <li>Encoding of messages larger than 16383 bytes</li>
   <li>Support for dynamic schema exchange</li>
   </ul>

   <p>Only the compact binary format has been implemented so
   far.</p>

   <h3>Planned work</h3>

   <p>The following list enumerates planned additions and changes for
   subsequent releases:</p>

   <ul>
   <li>Provide build scripts for Gradle</li>
   <li>JSON writer and reader</li>
   <li>Blink Tag format writer and reader</li>
   <li>Allow sequences to be represented as {@code Collections} as
   an alternative to arrays</li>
   <li>Support binding for decimal fields to methods that take the
   mantissa and exponent as separate components</li>
   <li>Allow optional POD typed fields to be represented using boxed types</li>
   <li>Turn Buf into an interface to allow for other, possibly native,
   implementations</li>
   </ul>
*/

package com.pantor.blink;
