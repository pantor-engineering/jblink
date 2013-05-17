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

import java.io.*;
import java.util.*;

public class PerfTest
{
   // com.pantor.blink.PerfTest decode|roundtrip <schema> <wrapper> <input>
   
   public static void main (String... args) throws Exception
   {
      DefaultObjectModel om = new DefaultObjectModel (args [1]);
      om.setWrapper (Class.forName (args [2]));
      
      CompactReader rd = new CompactReader (om);
      String task = args [0].intern ();

      if (task == "decode")
      {
	 byte [] src = readBytesFromFile (args [3]);

	 DefaultBlock result = new DefaultBlock ();
	 int count = 0;
	 long t1 = System.currentTimeMillis ();
	 for (int i = 0; i < src.length;)
	 {
	    int toRead = Math.min (4096, src.length - i);
	    result.clear ();
	    rd.read (src, i, toRead, result);
	    count += result.size ();
	    i += toRead;
	 }
	 long t2 = System.currentTimeMillis ();

	 long elapsed = t2 - t1;
	 System.out.printf ("Decoded %d msgs in %d ms (%.2f msgs/s)%n",
			    count, elapsed,
			    1000 * (double)count/(double)elapsed);
      }
      else if (task == "roundtrip")
      {
	 byte [] src = readBytesFromFile (args [3]);

	 ReusingBlock result = new ReusingBlock ();
//	 DefaultBlock result = new DefaultBlock ();
	 ByteArrayOutputStream os = new ByteArrayOutputStream ();
	 CompactWriter wr = new CompactWriter (om, os);
	 int count = 0;
	 long decTime = 0;
	 long encTime = 0;
	 
	 for (int i = 0; i < src.length;)
	 {
	    int toRead = Math.min (4096, src.length - i);
	    result.clear ();
	    os.reset ();
	    long t1 = System.currentTimeMillis ();
	    rd.read (src, i, toRead, result);
	    long t2 = System.currentTimeMillis ();
	    for (Object o : result)
	       wr.write (o);
	    wr.flush ();
	    long t3 = System.currentTimeMillis ();
	    decTime += t2 - t1;
	    encTime += t3 - t2;
	    count += result.size ();
	    i += toRead;
	 }

	 System.out.printf ("Decoded %d msgs in %d ms (%.2f msgs/s)%n",
			    count, decTime,
			    1000 * (double)count/(double)decTime);
	 System.out.printf ("Encoded %d msgs in %d ms (%.2f msgs/s)%n",
			    count, encTime,
			    1000 * (double)count/(double)encTime);
      }
      else if (task == "streaming-roundtrip")
      {
	 DefaultBlock result = new DefaultBlock ();
	 ByteArrayOutputStream os = new ByteArrayOutputStream ();
	 CompactWriter wr = new CompactWriter (om, os);
	 int count = 0;
	 long decTime = 0;
	 long encTime = 0;
	 
	 FileInputStream is = new FileInputStream (args [3]);
	 
	 byte [] buf = new byte [4096];

	 try
	 {
	    for (;;)
	    {
	       int n = is.read (buf);
	       
	       if (n == -1)
		  break;

	       result.clear ();
	       os.reset ();

	       long t1 = System.currentTimeMillis ();
	       rd.read (buf, 0, n, result);
	       long t2 = System.currentTimeMillis ();
	       for (Object o : result)
		  wr.write (o);
	       wr.flush ();
	       long t3 = System.currentTimeMillis ();
	       decTime += t2 - t1;
	       encTime += t3 - t2;
	       count += result.size ();
	    }
	 }
	 finally
	 {
	    is.close ();
	 }

	 System.out.printf ("Decoded %d msgs in %d ms (%.2f msgs/s)%n",
			    count, decTime,
			    1000 * (double)count/(double)decTime);
	 System.out.printf ("Encoded %d msgs in %d ms (%.2f msgs/s)%n",
			    count, encTime,
			    1000 * (double)count/(double)encTime);
      }
      else
	 throw new RuntimeException ("Unknown PerfTest task: " + task);
   }

   private static byte [] readBytesFromFile (String f) throws IOException
   {
       ByteArrayOutputStream bytes = new ByteArrayOutputStream ();
       FileInputStream is = new FileInputStream (f);
       byte [] buf = new byte [4096];
       try
       {
	  for (;;)
	  {
	     int n = is.read (buf);
	     if (n != -1)
		bytes.write (buf, 0, n);
	     else
		break;
	  }
       }
       finally
       {
	  is.close ();
	  bytes.close ();
       }
       return bytes.toByteArray ();
   }

   private static class ReusingBlock implements Block, Iterable<Object>
   {
      public void append (Object o)
      {
	 objs.add (o);
      }
   
      public Object [] refill (Creator ctor, Object [] o)
	 throws BlinkException.Binding
      {
	 Object [] pool = poolMap.get (ctor.getType ());
	 if (pool == null)
	 {
	    pool = new Object [1024];
	    for (int i = 0; i < pool.length; ++ i)
	       pool [i] = ctor.newInstance ();
	    poolMap.put (ctor.getType (), pool);
	 }
	 return pool;
      }
   
      public void reclaim (Object [] o, int from, int len)
      {
      }

      public void clear () { objs.clear (); }
      public boolean empty () { return objs.isEmpty (); }
      public int size () { return objs.size (); }
   
      public List<Object> getObjects () { return objs; }
      public Iterator<Object> iterator () { return objs.iterator (); }
   
      private final ArrayList<Object> objs = new ArrayList<Object> ();
      private final HashMap<Class<?>, Object []> poolMap =
	 new HashMap<Class<?>, Object []> ();
   }
}
