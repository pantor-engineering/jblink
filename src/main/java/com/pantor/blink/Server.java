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

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public final class Server
{
   public static interface Session extends Runnable
   {
      void send (Object obj) throws BlinkException, IOException;
      void send (Object [] objs) throws BlinkException, IOException;
      void send (Object [] objs, int from, int len)
         throws BlinkException, IOException;
      
      void addObserver (Object obj) throws BlinkException;
      void addObserver (Object obj, String prefix) throws BlinkException;
      void addObserver (NsName name, Observer obs);

      void close () throws IOException;
      void start ();
      void readLoop () throws BlinkException, IOException;
   }
   
   public static interface ConnectionObserver
   {
      void onConnect (Session s);
   }
   
   public Server (int port, ObjectModel om, ConnectionObserver cobs)
   {
      this.om = om;
      this.cobs = cobs;
      this.port = port;
   }

   public void run () throws IOException
   {
      ServerSocket ss = new ServerSocket (port);
      for (;;)
      {
         Socket sock = ss.accept ();
         log.info ("Accepted connection from " + sock);
         SessionImpl sn = new SessionImpl (sock, om);
         cobs.onConnect (sn);
      }
   }

   private static class SessionImpl implements Session
   {
      SessionImpl (Socket sock, ObjectModel om) throws IOException
      {
         this.sock = sock;
         this.om = om;
         this.os = sock.getOutputStream ();
         this.wr = new CompactWriter (om, os);
         this.oreg = new DefaultObsRegistry (om);
      }

      @Override
      public void send (Object obj) throws BlinkException, IOException
      {
         wr.write (obj);
         wr.flush ();
      }
      
      @Override
      public void send (Object [] objs) throws BlinkException, IOException
      {
         wr.write (objs);
         wr.flush ();
      }
      
      @Override
      public void send (Object [] objs, int from, int len)
         throws BlinkException, IOException
      {
         wr.write (objs, from, len);
         wr.flush ();
      }
      
      @Override
      public void addObserver (Object obj) throws BlinkException
      {
         oreg.addObserver (obj);
      }

      @Override
      public void addObserver (Object obs, String prefix) throws BlinkException
      {
         oreg.addObserver (obs, prefix);
      }

      @Override
      public void addObserver (NsName name, Observer obs)
      {
         oreg.addObserver (name, obs);
      }


      @Override
      public void close () throws IOException
      {
         os.close ();
      }

      @Override
      public void run ()
      {
         try
         {
            readLoop ();
         }
         catch (Throwable e)
         {
            while (e.getCause () != null)
               e = e.getCause ();
            log.severe (String.format ("%s: %s", sock, e));
         }
      }

      @Override
      public void start ()
      {
         new Thread (this).start ();
      }

      @Override
      public void readLoop () throws BlinkException, IOException
      {
         InputStream is = null;
         try
         {
            is = sock.getInputStream ();
            CompactReader rd = new CompactReader (om, oreg);
            byte [] buf = new byte [4096];
            for (;;)
            {
               int n = is.read (buf);
               if (n == -1)
                  break;
               rd.read (buf, 0, n);
            }

            log.info (sock + ": closed");
         }
         finally
         {
            os.close ();
            if (is != null)
               is.close ();
            sock.close ();
         }
      }

      private final Socket sock;
      private final ObjectModel om;
      private final CompactWriter wr;
      private final DefaultObsRegistry oreg;
      private final OutputStream os;
   }
   

   private final ObjectModel om;
   private final ConnectionObserver cobs;
   private final int port;

   private static final Logger log = Logger.getLogger (Server.class.getName ());
}
