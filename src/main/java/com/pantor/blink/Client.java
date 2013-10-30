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

import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

/**
   The {@code Client} class provides a basic Blink-capable UDP or TCP client.

   <p>It sends and receives messages in the Blink compact binary format.</p>

   <p>You send messages through the {@code send} method and the client
   dispatches any recieved messages to matching observers as added
   through the {@code addObserver} methods or passed to the
   constructor.</p>

   <p>Assuming an observer class:</p>
   
   <pre><blockquote>public class MyObs
{
   public void onLogonAck (LogonAck lgn)
   {
      // Process the logged in ack
   }

   public void onSomeMsg (SomeMsg msg)
   {
      // Process the received message
   }
}</pre></blockquote>

   <p>then a basic setup of a {@code Client} can look like this:</p>

   <pre><blockquote>{@link DefaultObjectModel} om = new DefaultObjectModel (schemaFile);
Client clnt = new Client ("localhost:1234", om, new MyObs ());
clnt.start ();
clnt.send (new Logon ("me", "abracadabra"));</pre></blockquote>

   <p>The client is thread based and can either be started through the
   {@code start} method that will spawn a new thread, or it can be
   integrated more flexible with custom created threads through {@code
   Runnable} the interface or the {@code readLoop} method.</p>
 */

public final class Client implements Runnable
{
   /**
      Creates a client that will connect to the specified address.
      It will map messages as defined by the specified object model.
      The third argument is an observer object that will receive any
      decoded messages that has matching observer method. The observer
      will be added to a {@link DefaultObsRegistry} managed by the
      client.

      @param addr an server address on the form 'host:port'
      @param om an object model
      @param obs an observer
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a socket problem
    */
   
   public Client (String addr, ObjectModel om, Object obs)
      throws BlinkException, IOException
   {
      this (addr, om);
      addObserver (obs);
   }
   
   /**
      Creates a client that will connect to the specified host and
      port. It will map messages as defined by the specified object
      model.

      @param host a host name
      @param port a TCP port
      @param om an object model
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a socket problem
    */
   
   public Client (String host, int port, ObjectModel om)
      throws BlinkException, IOException
   {
      this (new Socket (host, port), om);
   }

   /**
      Creates a client that will connect to the specified address.
      It will map messages as defined by the specified object model.

      @param addr an server address on the form 'host:port'
      @param om an object model
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a socket problem
    */
   
   public Client (String addr, ObjectModel om)
      throws BlinkException, IOException
   {
      String [] parts = addr.split (":");
      if (parts.length != 2)
         throw new IllegalArgumentException (
            "Address must be on the form 'host:port'");
      this.sock = new Socket (parts [0], Integer.parseInt (parts [1]));
      this.om = om;
      this.oreg = new DefaultObsRegistry (om);
      this.os = sock.getOutputStream ();
      this.wr = new CompactWriter (om, os);
      this.udpsock = null;
      this.bs = null;
   }

   /**
      Creates a client that communicate over the specified socket. It
      will map messages as defined by the specified object model.

      @param sock a TCP socket
      @param om an object model
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a socket problem
    */
   
   public Client (Socket sock, ObjectModel om)
      throws BlinkException, IOException
   {
      this.sock = sock;
      this.om = om;
      this.oreg = new DefaultObsRegistry (om);
      this.os = sock.getOutputStream ();
      this.wr = new CompactWriter (om, os);
      this.udpsock = null;
      this.bs = null;
   }

   /**
      Creates a client that communicate over the specified datagram socket. It
      will map messages as defined by the specified object model.

      @param sock a UDP socket
      @param om an object model
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a socket problem
    */
   
   public Client (DatagramSocket sock, ObjectModel om)
      throws BlinkException, IOException
   {
      this.udpsock = sock;
      this.om = om;
      this.oreg = new DefaultObsRegistry (om);
      this.bs = new ByteArrayOutputStream (1500);
      this.os = bs;
      this.wr = new CompactWriter (om, os);
      this.sock = null;
   }

   /**
      Adds an observer for received messages. The observer will be
      added to a {@link DefaultObsRegistry} managed by the client.
      The prefix when looking up matching observer methods will be "on".

      @param obs an observer to add
      @throws BlinkException if there is a schema or binding problem
   */
   
   public void addObserver (Object obs) throws BlinkException
   {
      oreg.addObserver (obs);
   }

   /**
      Adds an observer for received messages. The observer will be
      added to a {@link DefaultObsRegistry} managed by the client.
      The names of methods considered as observer methods must start
      with the specified prefix.

      @param obs an observer to add
      @param prefix the prefix used when looking up observer methods
      @throws BlinkException if there is a schema or binding problem
   */
   
   public void addObserver (Object obs, String prefix) throws BlinkException
   {
      oreg.addObserver (obs, prefix);
   }

   /**
      Adds an observer for received messages. The observer will be
      added to a {@link DefaultObsRegistry} managed by the client.
      The observer will receive messages matching the specified name.

      @param name the name of the blink message type to observe
      @param obs an observer
   */

   public void addObserver (NsName name, Observer obs)
   {
      oreg.addObserver (name, obs);
   }

   /**
      Runs the {@code readLoop}
    */
   
   @Override public void run ()
   {
      try
      {
         readLoop ();
      }
      catch (Throwable e)
      {
         while (e.getCause () != null)
            e = e.getCause ();
         log.fatal (String.format ("%s: %s", sock, e), e);
      }
   }

   /**
      Decodes incoming messages from the socket. It will run
      indefinitely and will only return if the socket is closed or if
      an exception occurs.

      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a socket or communications problem
    */

   public void readLoop () throws BlinkException, IOException
   {
      InputStream is = null;
      try
      {
         CompactReader rd = new CompactReader (om, oreg);
         if (sock != null)
         {
            is = sock.getInputStream ();
            Buf buf = DirectBuf.newInstance (4096);
            for (;;)
            {
               if (! buf.fillFrom (is))
                  break;
               buf.flip ();
               rd.read (buf);
            }
            
            log.info (sock + ": closed");
         }
         else
         {
            byte [] buf = new byte [1500];
            DatagramPacket p = new DatagramPacket (buf, buf.length);
            for (;;)
            {
               udpsock.receive (p);
               rd.read (buf, 0, p.getLength ());
            }
         }
      }
      finally
      {
         os.close ();
         if (is != null)
            is.close ();
         if (sock != null)
            sock.close ();
         if (udpsock != null)
            udpsock.close ();
      }
   }

   /** Starts the {@code readLoop} of this client by creating a new thread */
   
   public void start ()
   {
      new Thread (this).start ();
   }

   /**
      Sends a message to the server

      @param obj the message to send
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a socket or communications problem
   */
   
   public void send (Object obj) throws BlinkException, IOException
   {
      wr.write (obj);
      wr.flush ();

      if (udpsock != null)
      {
	 byte [] data = bs.toByteArray ();
	 bs.reset ();
	 DatagramPacket p =
	    new DatagramPacket (data, data.length,
				udpsock.getRemoteSocketAddress ());
	 udpsock.send (p);
      }
   }

   /**
      Sends an array of messages to the server

      @param objs the messages to send
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a socket or communications problem
   */

   public void send (Object [] objs) throws BlinkException, IOException
   {
      wr.write (objs);
      wr.flush ();

      if (udpsock != null)
      {
	 byte [] data = bs.toByteArray ();
	 bs.reset ();
	 DatagramPacket p =
	    new DatagramPacket (data, data.length,
				udpsock.getRemoteSocketAddress ());
	 udpsock.send (p);
      }
   }

   /**
      Sends a slice of messages from an array to the server

      @param objs the messages to send
      @param from the index of the first message to send from the array
      @param len the number of messages to send
      
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a socket or communications problem
   */

   public void send (Object [] objs, int from, int len)
      throws BlinkException, IOException
   {
      wr.write (objs, from, len);
      wr.flush ();

      if (udpsock != null)
      {
	 byte [] data = bs.toByteArray ();
	 bs.reset ();
	 DatagramPacket p =
	    new DatagramPacket (data, data.length,
				udpsock.getRemoteSocketAddress ());
	 udpsock.send (p);
      }
   }

   /**
      Sends a collection of messages to the server

      @param objs the messages to send
      @throws BlinkException if there is a schema or binding problem
      @throws IOException if there is a socket or communications problem
   */

   public void send (Iterable<?> objs) throws BlinkException, IOException
   {
      wr.write (objs);
      wr.flush ();

      if (udpsock != null)
      {
	 byte [] data = bs.toByteArray ();
	 bs.reset ();
	 DatagramPacket p =
	    new DatagramPacket (data, data.length,
				udpsock.getRemoteSocketAddress ());
	 udpsock.send (p);
      }
   }

   
   /**
      Closes this client by closing the output stream

      @throws IOException if there is a socket or communications problem
   */
   
   public void close () throws IOException
   {
      os.close ();
   }

   private final Socket sock;
   private final DatagramSocket udpsock;
   private final ObjectModel om;
   private final DefaultObsRegistry oreg;
   private final OutputStream os;
   private final CompactWriter wr;
   private final ByteArrayOutputStream bs;
   private final Logger log = Logger.Manager.getLogger (Client.class);
}
