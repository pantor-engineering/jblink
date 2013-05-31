// Copyright (c), Pantor Engineering AB, 2013-
// All rights reserved

package com.pantor.test;

import com.pantor.blink.DefaultObjectModel;
import com.pantor.blink.Server;

public class TestServer implements Server.ConnectionObserver
{
   public static class MsgObs
   {
      MsgObs (Server.Session sn) { this.sn = sn; }
      
      public void onPing (TestMessages.Ping ping)
      {
         try
         {
            int val = ping.getValue ();
            System.err.printf ("Got ping (%d), sending pong (%d)%n", val, val);
            sn.send (new TestMessages.Pong (val));
         }
         catch (Throwable e)
         {
            while (e.getCause () != null)
               e = e.getCause ();
            System.err.println (e);
         }
      }

      private final Server.Session sn;
   }

   @Override 
   public void onConnect (Server.Session sn)
   {
      try
      {
         MsgObs obs = new MsgObs (sn);
         sn.addObserver (obs);
         sn.start ();
      }
      catch (Throwable e)
      {
         while (e.getCause () != null)
            e = e.getCause ();
         System.err.println (e);
      }
   }
   
   public static void main (String... args) throws Exception
   {
      DefaultObjectModel om = new DefaultObjectModel (args [0]);
      om.setWrapper (TestMessages.class);
      int port = Integer.parseInt (args [1]);
      Server s = new Server (port, om, new TestServer ());
      System.err.printf ("Starting server on port %d%n", port);
      s.run ();
   }
}
