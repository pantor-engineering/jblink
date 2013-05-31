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
   A name value pair representing an annotation in a blink schema.
 */

public final class Annotation
{
   /**
      Creates an annotation

      @param name the name of the annotation
      @param value the value of the annotation
   */
   
   public Annotation (NsName name, String value)
   {
      this.name = name;
      this.value = value;
   }

   /**
      Returns the name of this annotation

      @return the annotation name
    */
   
   public NsName getName () { return name; }

   /**
      Returns the value of this annotation

      @return the annotation value
    */

   public String getValue () { return value; }

   /**
      Returns a string representation of this annotation. The output syntax
      is similar to the Blink schema syntax.

      @return a string representation of the annotation
    */
   
   @Override
   public String toString ()
   {
      StringBuilder sb = new StringBuilder ();
      sb.append ('@');
      sb.append (name);
      sb.append ('=');
      String [] segs = value.split ("\"", -1 /* Unlimited */);
      int pos = 0;
      for (String s : segs)
      {
         if (pos > 0)
            sb.append (" '\"' ");
         sb.append ('"' + s + '"');
         ++ pos;
      }
      return sb.toString ();
   }
   
   private final NsName name;
   private final String value;
}
