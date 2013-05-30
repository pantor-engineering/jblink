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

/**
   This class is used for any Blink related exceptions. Inner subclasses
   provide more specific details for different contexts.
 */

public class BlinkException extends Exception
{
   /**
      Creates an exception with the specified message.

      @param what a descriptive exception message
   */
   
   public BlinkException (String what)
   {
      super (what);
   }

   /**
      Creates a chained exception

      @param cause the next exception in the chain
   */
   
   public BlinkException (Throwable cause)
   {
      super (cause);
   }

   /**
      A {@code Schema} exception is thrown for any schema related issues
    */
   
   public static class Schema extends BlinkException
   {
      /**
	 Creates a schema exception

	 @param msg a descriptive message
	 @param loc the location in the schema file related to this exception
      */
   
      public Schema (String msg, Location loc)
      {
	 super (msg);
	 this.msg = msg;
	 this.loc = loc;
      }
      
      @Override
      public String toString ()
      {
	 return String.format ("%s: error: %s", loc, msg);
      }

      private final String msg;
      private final Location loc;
   }

   /**
      A {@code Binding} exception is thrown for any issues related
      to binding a schema to the object model
   */
   
   public static class Binding extends BlinkException
   {
      /**
	 Creates a binding exception

	 @param msg a descriptive message
	 @param loc a location in the schema file releated to this exception
      */
      
      public Binding (String msg, Location loc)
      {
	 super (msg);
	 this.msg = msg;
	 this.loc = loc;
      }

      /**
	 Creates a chained exception

	 @param cause the next exception in the chain
      */
   
      public Binding (Throwable cause)
      {
	 super (cause);
	 this.msg = null;
	 this.loc = null;
      }

      /**
	 Creates a binding exception

	 @param msg a descriptive message
      */
      
      public Binding (String msg)
      {
	 this (msg, null);
      }
      
      @Override
      public String toString ()
      {
	 if (msg == null)
	    return super.toString ();
	 else if (loc != null)
	    return String.format ("%s: error: %s", loc, msg);
	 else
	    return msg;
      }

      private final String msg;
      private final Location loc;
   }

   /**
      A {@code Decode} exception is thrown for any issues related
      to decoding blink data
   */
   
   public static class Decode extends BlinkException
   {
      /**
	 Creates a decoding exception

	 @param msg a descriptive message
	 @param src the buffer in use when the error appeared
      */
      
      public Decode (String msg, ByteSource src)
      {
	 super (msg);
	 this.src = src;
      }

      /**
	 Creates a decoding exception

	 @param msg a descriptive message
      */
      
      public Decode (String msg) { this (msg, null); }

      /**
	 Returns the buffer in use when the error appeared

	 @return the buffer in use or {@code null} if not available
      */
      
      public ByteSource getContext () { return src; }
      private final ByteSource src;
   }

   /**
      A {@code Encode} exception is thrown for any issues related
      to encoding blink data
   */

   public static class Encode extends BlinkException
   {
      /**
	 Creates an encoding exception

	 @param msg a descriptive message
      */
      
      public Encode (String msg) { super (msg); }
   }
}
