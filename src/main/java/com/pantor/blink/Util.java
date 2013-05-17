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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;

public final class Util
{
   public static String displayStr (String s, int limit)
   {
      StringBuilder result = new StringBuilder ();
      result.append ('"');
      int len = s.length ();
      int n = Math.min (len, limit);
      for (int i = 0; i < n; ++ i)
      {
	 char c = s.charAt (i);
	 if (c > 0xff)
	    result.append (String.format ("\\u%04x", c));
	 else if (c < 0x20 || c > 0x7f)
	    result.append (String.format ("\\x%02x", c));
	 else if (c == '"')
	    result.append ("\\\"");
	 else
	    result.append (c);
      }

      if (n < len)
	 result.append ("\" ...");
      else
	 result.append ('"');

      return result.toString ();
   }

   public static String displayStr (String s)
   {
      return displayStr (s, 30);
   }

   public static String capitalize (String s)
   {
      int len = s.length ();
      if (len == 0)
	 return s;
      else if (len == 1)
	 return Character.toUpperCase (s.charAt (0)) + "";
      else
	 return Character.toUpperCase (s.charAt (0)) + s.substring (1);
   }

   public static String decapitalize (String s)
   {
      int len = s.length ();
      if (len == 0)
	 return s;
      else if (len == 1)
	 return Character.toLowerCase (s.charAt (0)) + "";
      else
	 return Character.toLowerCase (s.charAt (0)) + s.substring (1);
   }

   public static <T> String join (Iterable <T> a, String sep)
   {
      StringBuilder sb = new StringBuilder ();
      int pos = 0;
      for (T i : a)
      {
	 if (pos > 0)
	    sb.append (sep);
	 sb.append (i);
	 ++ pos;
      }
      return sb.toString ();
   }

   public static String [] splitCamelback (String s)
   {
      return s.split ("(?<=[a-z])(?=[A-Z])");
   }

   public static String splitCamelback (String s, String sep)
   {
      return join (Arrays.asList (splitCamelback (s)), sep);
   }

   public static String normalizeSpace (String s)
   {
      return s.replaceAll ("\\s+", " ").trim ();
   }

   public static long u32ToLong (int v)
   {
      return (long)v & 0xffffffffL;
   }

   public static long parseU64 (String val, int radix)
   {
      return (new BigInteger (val, radix)).longValue ();
   }

   public static long parseU64 (String val)
   {
      return parseU64 (val, 10);
   }

   public static long parseU64IdAnnot (String id)
   {
      if (id.startsWith ("0x"))
	 return parseU64 (id.substring (2), 16);
      else
	 return parseU64 (id);
   }

   public static int parseI32IdAnnot (String id)
   {
      if (id.startsWith ("0x"))
	 return Integer.parseInt (id.substring (2), 16);
      else
	 return Integer.parseInt (id);
   }
   
   public static String toU64Str (long val)
   {
      if (val > 0)
	 return String.valueOf (val);
      else
	 return BigInteger.valueOf (val).add (Two64).toString ();
   }

   public static boolean isSet (String s)
   {
      return s != null && ! s.equals ("");
   }

   public static String escName (String n)
   {
      if (n.endsWith ("_") || JavaKeywords.contains (n))
	 return n + "_";
      else
	 return n;
   }

   public static String escMethodName (String n)
   {
      if (n.endsWith ("_") || n.equals ("Class"))
	 return n + "_";
      else
	 return n;
   }

   private final static HashSet<String> JavaKeywords = new HashSet<String> ();
   
   static
   {
      String [] kwds = {
	 "abstract", "continue", "for", "new", "switch", "assert", "default", 
	 "goto", "package", "synchronized", "boolean", "do", "if", "private", 
	 "this", "break", "double", "implements", "protected", "throw", "byte", 
	 "else", "import", "public", "throws", "case", "enum", "instanceof", 
	 "return", "transient", "catch", "extends", "int", "short", "try",
	 "char", "final", "interface", "static", "void", "class", "finally",
	 "long", "strictfp", "volatile", "const", "float", "native", "super",
	 "while", "true", "false", "null" 
      };

      JavaKeywords.addAll (Arrays.asList (kwds));
   }
   
   private static final BigInteger Two64 = BigInteger.ONE.shiftLeft (64);
}
