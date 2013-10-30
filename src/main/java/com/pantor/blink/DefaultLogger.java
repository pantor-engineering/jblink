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

import java.io.PrintWriter;
import java.io.StringWriter;

public class DefaultLogger implements Logger
{
   public DefaultLogger (String name)
   {
      log = java.util.logging.Logger.getLogger (name);
   }

   public DefaultLogger (Class cl)
   {
      this (cl.getName ());
   }

   @Override public void fatal (String msg)
   {
      log.severe (msg);
   }
   
   @Override public void fatal (String msg, Throwable e)
   {
      log.severe (format (msg, e));
   }

   @Override public void error (String msg)
   {
      log.severe (msg);
   }

   @Override public void error (String msg, Throwable e)
   {
      log.severe (format (msg, e));
   }

   @Override public void warn (String msg)
   {
      log.warning (msg);
   }

   @Override public void warn (String msg, Throwable e)
   {
      log.warning (format (msg, e));
   }

   @Override public void info (String msg)
   {
      log.info (msg);
   }
   
   @Override public void info (String msg, Throwable e)
   {
      log.info (format (msg, e));
   }

   @Override public void debug (String msg)
   {
      log.fine (msg);
   }

   @Override public void debug (String msg, Throwable e)
   {
      log.fine (format (msg, e));
   }

   @Override public void trace (String msg)
   {
      log.finest (msg);
   }
   
   @Override public void trace (String msg, Throwable e)
   {
      log.finest (format (msg, e));
   }

   @Override public boolean isActiveAtLevel (Logger.Level level)
   {
      switch (level)
      {
       case Off:
         return false;
       case Fatal: case Error:
         return log.isLoggable (java.util.logging.Level.SEVERE);
       case Warn:
         return log.isLoggable (java.util.logging.Level.WARNING);
       case Info:
         return log.isLoggable (java.util.logging.Level.INFO);
       case Debug:
         return log.isLoggable (java.util.logging.Level.FINE);
       case Trace: case All: default:
         return log.isLoggable (java.util.logging.Level.FINEST);
      }
   }

   private static String format (String msg, Throwable e)
   {
      StringWriter sw = new StringWriter ();
      PrintWriter pw = new PrintWriter (sw);
      e.printStackTrace (pw);
      return sw.toString ();
   }
   
   private final java.util.logging.Logger log;

   public static class Factory implements Logger.Factory
   {
      @Override public Logger getLogger (String name)
      {
         return new DefaultLogger (name);
      }
            
      @Override public Logger getLogger (Class cl)
      {
         return new DefaultLogger (cl);
      }
   }
}
