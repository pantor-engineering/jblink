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

public class ConciseLogger implements Logger
{
   public ConciseLogger (String name, Logger.Level level, int indent)
   {
      this.level = level;
      this.name = name;
      this.indent = indent == 0 ? "" : String.format ("%" + indent + "s", "");
   }

   public ConciseLogger (Class cl, Logger.Level level, int indent)
   {
      this (cl.getName (), level, indent);
   }

   @Override public synchronized void fatal (String msg, Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Fatal))
         System.err.printf (tsp () + " FATAL: " + indent + msg + "%n", args);
   }
   
   @Override public synchronized void fatal (Throwable e, String msg,
                                             Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Fatal))
         System.err.printf (tsp () + " FATAL: " + indent +
                            format (msg, args, e));
   }

   @Override public synchronized void error (String msg, Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Error))
         System.err.printf (tsp () + " ERROR: " + indent + msg + "%n", args);
   }

   @Override public synchronized void error (Throwable e, String msg,
                                             Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Error))
         System.err.printf (tsp () + " ERROR: " + indent +
                            format (msg, args, e));
   }

   @Override public synchronized void warn (String msg, Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Warn))
         System.err.printf (tsp () + "  WARN: " + indent + msg + "%n", args);
   }

   @Override public synchronized void warn (Throwable e, String msg,
                                            Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Warn))
         System.err.printf (tsp () + "  WARN: " +
                            indent + format (msg, args, e));
   }

   @Override public synchronized void info (String msg, Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Info))
         System.err.printf (tsp () + "  INFO: " + indent + msg + "%n", args);
   }
   
   @Override public synchronized void info (Throwable e, String msg,
                                            Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Info))
         System.err.printf (tsp () + "  INFO: " + indent +
                            format (msg, args, e));
   }

   @Override public synchronized void debug (String msg, Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Debug))
         System.err.printf (tsp () + " DEBUG: " + indent + msg + "%n", args);
   }

   @Override public synchronized void debug (Throwable e, String msg,
                                             Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Debug))
         System.err.printf (tsp () + " DEBUG: " + indent +
                            format (msg, args, e));
   }

   @Override public synchronized void trace (String msg, Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Trace))
         System.err.printf (tsp () + " TRACE: " + indent + msg + "%n", args);
   }

   @Override public synchronized void trace (Throwable e, String msg,
                                             Object... args)
   {
      if (isActiveAtLevel (Logger.Level.Trace))
         System.err.printf (tsp () + " TRACE: " + indent +
                            format (msg, args, e));
   }

   @Override public boolean isActiveAtLevel (Logger.Level level)
   {
      return level.cmp (this.level) <= 0;
   }

   private static String tsp ()
   {
      return String.format ("%tF %<tT.%<tL", new java.util.Date ());
   }
   
   private static String format (String msg, Object [] args, Throwable e)
   {
      StringWriter sw = new StringWriter ();
      PrintWriter pw = new PrintWriter (sw);
      e.printStackTrace (pw);
      return String.format (msg, args) + ": " + sw.toString ();
   }

   public static class Factory implements Logger.Factory
   {
      public Factory (Logger.Level level)
      {
         this (level, 0);
      }

      public Factory (Logger.Level level, int indent)
      {
         this.level = level;
         this.indent = indent;
      }

      public Factory ()
      {
         this (Logger.Level.Info);
      }
      
      @Override public Logger getLogger (String name)
      {
         return new ConciseLogger (name, level, indent);
      }
            
      @Override public Logger getLogger (Class cl)
      {
         return new ConciseLogger (cl, level, indent);
      }

      private final Logger.Level level;
      private final int indent;
   }

   private final String name;
   private final Logger.Level level;
   private final String indent;
}
