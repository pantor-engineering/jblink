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

package com.pantor.blink;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;

/**
   The {@code Broadcaster} class provides a basic Blink-capable UDP broadcaster.

   <p>It sends datagrams containing messages encoded in the Blink
   compact binary format. Typically a broadcaster is used with a IP multicast
   address.</p>

   <p>You send messages through the {@code send} method.</p>

   <p>A basic setup of a {@code Broadcaster} can look like this:</p>

   <pre><blockquote>{@link DefaultObjectModel} om = new DefaultObjectModel (schemaFile);
Broadcaster bc = new Broadcaster ("224.0.0.1:1234", om);
bc.send (new News ("All your base are belong to us"));</pre></blockquote>
 */

public final class Broadcaster
{
   /**
      Creates a broadcaster that will broadcast to the specified host and
      port. It will map messages as defined by the specified object
      model.

      @param host a host name
      @param port a TCP port
      @param om an object model
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a channel problem
    */
   
   public Broadcaster (String host, int port, ObjectModel om)
      throws BlinkException, IOException
   {
      this (DatagramChannel.open ().connect (
               new InetSocketAddress (host, port)), om);
   }

   /**
      Creates a broadcaster that will broadcast to the specified address.
      It will map messages as defined by the specified object model.

      @param addr an server address on the form 'host:port'
      @param om an object model
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a channel problem
    */
   
   public Broadcaster (String addr, ObjectModel om)
      throws BlinkException, IOException
   {
      this (DatagramChannel.open (), om);
      String [] parts = addr.split (":");
      if (parts.length != 2)
         throw new IllegalArgumentException (
            "Address must be on the form 'host:port'");
      cnl.connect (
         new InetSocketAddress (parts [0], Integer.parseInt (parts [1])));
   }

   /**
      Creates a broadcaster that communicate over the specified
      datagram channel. It will map messages as defined by the
      specified object model.

      @param channel a connected datagram channel
      @param om an object model
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a channel problem
    */
   
   public Broadcaster (DatagramChannel channel, ObjectModel om)
      throws BlinkException, IOException
   {
      this.bb = ByteBuffer.allocate (1500);
      this.buf = new ByteBuf (bb.array ());
      this.wr = new CompactWriter (om, buf);
      this.cnl = channel;
      this.om = om;
   }

   /**
      Sends a message in a single datagram

      @param obj the message to send
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a channel or communications problem
   */
   
   public void send (Object obj) throws BlinkException, IOException
   {
      wr.write (obj);
      flush ();
   }

   /**
      Sends two messages in the same datagram

      @param o1 the first message to send
      @param o2 the second message to send
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a channel or communications problem
   */
   
   public void send (Object o1, Object o2) throws BlinkException, IOException
   {
      wr.write (o1);
      wr.write (o2);
      flush ();
   }

   /**
      Sends an array of messages i a single datagram

      @param objs the messages to send
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a channel or communications problem
   */

   public void send (Object [] objs) throws BlinkException, IOException
   {
      wr.write (objs);
      flush ();
   }

   /**
      Sends a slice of messages from an array in a single datagram

      @param objs the messages to send
      @param from the index of the first message to send from the array
      @param len the number of messages to send
      
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a channel or communications problem
   */

   public void send (Object [] objs, int from, int len)
      throws BlinkException, IOException
   {
      wr.write (objs, from, len);
      flush ();
   }

   /**
      Sends a collection of messages in a single datagram

      @param objs the messages to send
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a channel or communications problem
   */

   public void send (Iterable<?> objs) throws BlinkException, IOException
   {
      wr.write (objs);
      flush ();
   }

   /**
      Disconnects the underlying datagram channel

      @throws IOException if there is a channel problem
   */
   
   public void disconnect () throws IOException
   {
      cnl.disconnect ();
   }

   private void flush () throws IOException
   {
      buf.flip ();
      bb.limit (buf.size ());
      bb.position (0);
      cnl.write (bb);
      buf.clear ();
   }
   
   private final ByteBuffer bb;
   private final ByteBuf buf;
   private final DatagramChannel cnl;
   private final ObjectModel om;
   private final CompactWriter wr;
}
