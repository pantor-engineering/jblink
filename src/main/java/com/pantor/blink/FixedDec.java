// Copyright (c) 2015, Pantor Engineering AB
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

import java.math.BigDecimal;

/**
   The {@code FixedDec} represents a Blink fixedDec(N) value. It has
   an 64-bit significand and a fixed scale.

   <p>Instances of {@code FixedDec} are immutable.</p>
*/

public abstract class FixedDec implements Comparable<FixedDec>
{   
   /**
      Creates a decimal with the value {@code significand} * 10^-N

      @param significand a significand
   */

   FixedDec (long significand) { this.significand = significand; }

   /**
      Returns the scale

      @return the scale
   */

   public abstract int getScale ();
   
   /**
      Returns the significand

      @return the significand
   */

   public long getSignificand ()
   {
      return significand;
   }

   /**
      Compares this fixed decimal with the specified object for
      equality. Two fixed decimals are considered equal only if the
      have the same scale and the same significand.

      @param other object to compare with

      @return true if and only if both the significands and the scales
      are equal
   */
   
   @Override
   public final boolean equals (Object other)
   {
      if (other instanceof FixedDec)
      {
         FixedDec o = (FixedDec)other;
         return significand == o.significand && getScale () == o.getScale ();
      }
      else
         return false;
   }

   /**
      Returns a hash for this fixed decimal. Two fixed decimals with different
      scales but that are numerically equal can have different hash codes

      @return hash code for this fixed decimal
   */

   @Override
   public final int hashCode ()
   {
      return (int)significand * 31 + getScale ();
   }

   private final static int compareSigs (long s1, long s2)
   {
      return s1 == s2 ? 0 : (s1 < s2 ? -1 : 1);
   }
   
   @Override
   public final int compareTo (FixedDec other)
   {
      int scale = getScale ();
      int otherScale = other.getScale ();
      long otherSig = other.getSignificand ();

      if (scale == otherScale)
         return compareSigs (significand, otherSig);
      else if (significand == 0 && otherSig == 0)
         return 0;
      else if (significand < 0 && otherSig >= 0)
         return -1;
      else if (significand >= 0 && otherSig < 0)
         return 1;
      else if (scale < otherScale)
         // FIXME: Deal with precision loss
         return compareSigs (rescale (significand, scale, otherScale), otherSig);
      else
         // FIXME: Deal with precision loss
         return compareSigs (significand, rescale (otherSig, otherScale, scale));
   }
   
   @Override
   public final String toString ()
   {
      int scale = getScale ();
      if (scale == 0)
         return String.valueOf (significand) + "E0";
      else
         return String.valueOf (significand) + "E-" + String.valueOf (scale);
   }

   /**
      Returns a double representation of this value

      @return the double value of this fixed decimal
   */

   public final double doubleValue ()
   {
      return (double)significand * Math.pow (10, - getScale ()); 
   }

   /**
      Returns a long representation of this value

      @return the long value of this fixed decimal
   */

   public final long longValue ()
   {
      return rescale (significand, getScale (), 0);
   }

   /**
      Returns a decimal representation of this value

      @return the decimal value of this fixed decimal
   */

   public final Decimal decimalValue ()
   {
      return Decimal.valueOf (significand, - getScale ());
   }

   /**
      Creates a fixedDec (0) instance from an integer

      @param value an long integer
      @return a fixed decimal value
   */

   public static FixedDec valueOf (long value)
   {
      return new _0 (value);
   }

   /**
      Creates a fixedDec({@code scale}) instance from a scaled integer

      @param value an long integer
      @param fromScale the scale of the input long integer
      @param toScale the scale of the returned fixed decimal, the scale
      must be >= 0
      @return a fixed decimal value
   */

   public static FixedDec valueOf (long value, int fromScale, int toScale)
   {
      return getInstance (rescale (value, fromScale, toScale), toScale);
   }

   /**
      Creates a fixedDec({@code scale}) instance from significand and
      a scale

      @param significand a significand
      @param scale the scale which must be >= 0
      @return a fixed decimal value
   */
   
   public static FixedDec getInstance (long significand, int scale)
   {
      switch (scale)
      {
       case 0: return new _0 (significand);
       case 1: return new _1 (significand);
       case 2: return new _2 (significand);
       case 3: return new _3 (significand);
       case 4: return new _4 (significand);
       case 5: return new _5 (significand);
       case 6: return new _6 (significand);
       case 7: return new _7 (significand);
       case 8: return new _8 (significand);
       case 9: return new _9 (significand);
       case 10: return new _10 (significand);
       default:
         if (scale >= 0)
            return new _N (significand, scale);
         else
            throw new IllegalArgumentException (
               "FixedDec scale must be >= 0: " + String.valueOf (scale));
      }
   }

   /**
      Creates a fixedDec({@code scale}) instance from a Decimal

      @param value a decimal
      @return a fixed decimal value
   */

   public static FixedDec valueOf (Decimal value)
   {
      return valueOf (value, - value.getExponent ()); 
   }
   
   /**
      Creates a fixedDec({@code scale}) instance from a Decimal

      @param value a decimal
      @param scale a scale, the scale must be >= 0
      @return a fixed decimal value
   */

   public static FixedDec valueOf (Decimal value, int scale)
   {
      int fromScale = - value.getExponent ();
      return valueOf (value.getSignificand (), fromScale, scale);
   }

   /**
      Creates a fixed decimal based on another fixed decimal value,
      possibly rescaled.

      @param other a fixed decimal
      @param scale a scale
      @return a fixed decimal value. If the scale of the {@code other}
      parameter is the same as the specified {@code scale}, the same instance
      is returned
   */

   public static FixedDec valueOf (FixedDec other, int scale)
   {
      int otherScale = other.getScale ();
      if (scale == otherScale)
         return other;
      else
         return valueOf (other.getSignificand (), otherScale, scale);
   }

   /**
      Creates a fixed decimal instance by parsing the specified string.

      @param s a string on the same format accepted by {@link Decimal}
      @param scale a scale
      @return a fixed decimal value
   */

   public static FixedDec valueOf (String s, int scale)
   {
      BigDecimal bd = new BigDecimal (s);
      int fromScale = bd.scale ();
      if (fromScale < 0)
      {
         bd = bd.setScale (0);
         fromScale = 0;
      }

      return valueOf (bd.unscaledValue ().longValue (), fromScale, scale);
   }

   /**
      Creates a fixed decimal instance by parsing the specified string.

      @param s a string on the same format accepted by {@link Decimal}
      @return a fixed decimal value
   */
   
   public static FixedDec valueOf (String s)
   {
      BigDecimal bd = new BigDecimal (s);
      int fromScale = bd.scale ();
      if (fromScale < 0)
      {
         bd = bd.setScale (0);
         fromScale = 0;
      }

      return valueOf (bd.unscaledValue ().longValue (), fromScale, fromScale);
   }

   /**
      Rescales a significand between two scales

      @param significand a significand
      @param fromScale a source scale
      @param toScale a target scale
      @return a rescaled significand
   */
   
   public static long rescale (long significand, int fromScale, int toScale)
   {
      int diff = toScale - fromScale;
      if (diff == 0 || significand == 0)
      {
         return significand;
      }
      else if (diff > 0)
      {
         if (diff == 7)
            return significand * 10000000L;
         else if (diff == 2)
            return significand * 100L;
         else if (diff == 8)
            return significand * 100000000L;
         else if (diff == 1)
            return significand * 10L;
         else if (diff == 3)
            return significand * 1000L;
         else if (diff == 4)
            return significand * 10000L;
         else if (diff == 5)
            return significand * 100000L;
         else if (diff == 6)
            return significand * 1000000L;
         else if (diff == 9)
            return significand * 1000000000L;
         else
            return significand * (long)Math.pow (10, diff);
      }
      else
      {
         if (diff == -7)
            return significand / 10000000L;
         else if (diff == -2)
            return significand / 100L;
         else if (diff == -8)
            return significand / 100000000L;
         else if (diff == -1)
            return significand / 10L;
         else if (diff == -3)
            return significand / 1000L;
         else if (diff == -4)
            return significand / 10000L;
         else if (diff == -5)
            return significand / 100000L;
         else if (diff == -6)
            return significand / 1000000L;
         else if (diff == -9)
            return significand / 1000000000L;
         else
            return significand / (long)Math.pow (10, - diff);
      }
   }

   public static final class _0 extends FixedDec
   {
      public final static int Scale = 0;
      public _0 (long sig) { super (sig); }
      @Override public int getScale () { return 0; }
      public static _0 valueOf (long value)
      {
         return new _0 (value);
      }

      public static _0 valueOf (Decimal v)
      {
         return new _0 (rescale (v.getSignificand (), - v.getExponent (), 0));
      }
   
      public static _0 valueOf (FixedDec v)
      {
         if (v instanceof _0)
            return (_0)v;
         else
            return new _0 (rescale (v.getSignificand (), v.getScale (), 0));
      }
   
      public static _0 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (0);
         return new _0 (bd.unscaledValue ().longValue ());
      }

      public static _0 getInstance (long significand)
      {
         return new _0 (significand);
      }
   }

   public static final class _1 extends FixedDec
   {
      public final static int Scale = 1;
      public _1 (long sig) { super (sig); }
      @Override public int getScale () { return 1; }
      public static _1 valueOf (long value)
      {
         return new _1 (rescale (value, 0, 1));
      }

      public static _1 valueOf (Decimal v)
      {
         return new _1 (rescale (v.getSignificand (), - v.getExponent (), 1));
      }
   
      public static _1 valueOf (FixedDec v)
      {
         if (v instanceof _1)
            return (_1)v;
         else
            return new _1 (rescale (v.getSignificand (), v.getScale (), 1));
      }
   
      public static _1 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (1);
         return new _1 (bd.unscaledValue ().longValue ());
      }

      public static _1 getInstance (long significand)
      {
         return new _1 (significand);
      }
   }

   public static final class _2 extends FixedDec
   {
      public final static int Scale = 2;
      public _2 (long sig) { super (sig); }
      @Override public int getScale () { return 2; }
      public static _2 valueOf (long value)
      {
         return new _2 (rescale (value, 0, 2));
      }

      public static _2 valueOf (Decimal v)
      {
         return new _2 (rescale (v.getSignificand (), - v.getExponent (), 2));
      }
   
      public static _2 valueOf (FixedDec v)
      {
         if (v instanceof _2)
            return (_2)v;
         else
            return new _2 (rescale (v.getSignificand (), v.getScale (), 2));
      }
   
      public static _2 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (2);
         return new _2 (bd.unscaledValue ().longValue ());
      }

      public static _2 getInstance (long significand)
      {
         return new _2 (significand);
      }
   }

   public static final class _3 extends FixedDec
   {
      public final static int Scale = 3;
      public _3 (long sig) { super (sig); }
      @Override public int getScale () { return 3; }
      public static _3 valueOf (long value)
      {
         return new _3 (rescale (value, 0, 3));
      }

      public static _3 valueOf (Decimal v)
      {
         return new _3 (rescale (v.getSignificand (), - v.getExponent (), 3));
      }
   
      public static _3 valueOf (FixedDec v)
      {
         if (v instanceof _3)
            return (_3)v;
         else
            return new _3 (rescale (v.getSignificand (), v.getScale (), 3));
      }
   
      public static _3 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (3);
         return new _3 (bd.unscaledValue ().longValue ());
      }

      public static _3 getInstance (long significand)
      {
         return new _3 (significand);
      }
   }

   public static final class _4 extends FixedDec
   {
      public final static int Scale = 4;
      public _4 (long sig) { super (sig); }
      @Override public int getScale () { return 4; }
      public static _4 valueOf (long value)
      {
         return new _4 (rescale (value, 0, 4));
      }

      public static _4 valueOf (Decimal v)
      {
         return new _4 (rescale (v.getSignificand (), - v.getExponent (), 4));
      }
   
      public static _4 valueOf (FixedDec v)
      {
         if (v instanceof _4)
            return (_4)v;
         else
            return new _4 (rescale (v.getSignificand (), v.getScale (), 4));
      }
   
      public static _4 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (4);
         return new _4 (bd.unscaledValue ().longValue ());
      }

      public static _4 getInstance (long significand)
      {
         return new _4 (significand);
      }
   }

   public static final class _5 extends FixedDec
   {
      public final static int Scale = 5;
      public _5 (long sig) { super (sig); }
      @Override public int getScale () { return 5; }
      public static _5 valueOf (long value)
      {
         return new _5 (rescale (value, 0, 5));
      }

      public static _5 valueOf (Decimal v)
      {
         return new _5 (rescale (v.getSignificand (), - v.getExponent (), 5));
      }
   
      public static _5 valueOf (FixedDec v)
      {
         if (v instanceof _5)
            return (_5)v;
         else
            return new _5 (rescale (v.getSignificand (), v.getScale (), 5));
      }
   
      public static _5 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (5);
         return new _5 (bd.unscaledValue ().longValue ());
      }

      public static _5 getInstance (long significand)
      {
         return new _5 (significand);
      }
   }

   public static final class _6 extends FixedDec
   {
      public final static int Scale = 6;
      public _6 (long sig) { super (sig); }
      @Override public int getScale () { return 6; }
      public static _6 valueOf (long value)
      {
         return new _6 (rescale (value, 0, 6));
      }

      public static _6 valueOf (Decimal v)
      {
         return new _6 (rescale (v.getSignificand (), - v.getExponent (), 6));
      }
   
      public static _6 valueOf (FixedDec v)
      {
         if (v instanceof _6)
            return (_6)v;
         else
            return new _6 (rescale (v.getSignificand (), v.getScale (), 6));
      }
   
      public static _6 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (6);
         return new _6 (bd.unscaledValue ().longValue ());
      }

      public static _6 getInstance (long significand)
      {
         return new _6 (significand);
      }
   }

   public static final class _7 extends FixedDec
   {
      public final static int Scale = 7;
      public _7 (long sig) { super (sig); }
      @Override public int getScale () { return 7; }
      public static _7 valueOf (long value)
      {
         return new _7 (rescale (value, 0, 7));
      }

      public static _7 valueOf (Decimal v)
      {
         return new _7 (rescale (v.getSignificand (), - v.getExponent (), 7));
      }
   
      public static _7 valueOf (FixedDec v)
      {
         if (v instanceof _7)
            return (_7)v;
         else
            return new _7 (rescale (v.getSignificand (), v.getScale (), 7));
      }
   
      public static _7 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (7);
         return new _7 (bd.unscaledValue ().longValue ());
      }

      public static _7 getInstance (long significand)
      {
         return new _7 (significand);
      }
   }

   public static final class _8 extends FixedDec
   {
      public final static int Scale = 8;
      public _8 (long sig) { super (sig); }
      @Override public int getScale () { return 8; }
      public static _8 valueOf (long value)
      {
         return new _8 (rescale (value, 0, 8));
      }

      public static _8 valueOf (Decimal v)
      {
         return new _8 (rescale (v.getSignificand (), - v.getExponent (), 8));
      }
   
      public static _8 valueOf (FixedDec v)
      {
         if (v instanceof _8)
            return (_8)v;
         else
            return new _8 (rescale (v.getSignificand (), v.getScale (), 8));
      }
   
      public static _8 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (8);
         return new _8 (bd.unscaledValue ().longValue ());
      }

      public static _8 getInstance (long significand)
      {
         return new _8 (significand);
      }
   }

   public static final class _9 extends FixedDec
   {
      public final static int Scale = 9;
      public _9 (long sig) { super (sig); }
      @Override public int getScale () { return 9; }
      public static _9 valueOf (long value)
      {
         return new _9 (rescale (value, 0, 9));
      }

      public static _9 valueOf (Decimal v)
      {
         return new _9 (rescale (v.getSignificand (), - v.getExponent (), 9));
      }
   
      public static _9 valueOf (FixedDec v)
      {
         if (v instanceof _9)
            return (_9)v;
         else
            return new _9 (rescale (v.getSignificand (), v.getScale (), 9));
      }
   
      public static _9 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (9);
         return new _9 (bd.unscaledValue ().longValue ());
      }

      public static _9 getInstance (long significand)
      {
         return new _9 (significand);
      }
   }

   public static final class _10 extends FixedDec
   {
      public final static int Scale = 10;
      public _10 (long sig) { super (sig); }
      @Override public int getScale () { return 10; }
      public static _10 valueOf (long value)
      {
         return new _10 (rescale (value, 0, 10));
      }

      public static _10 valueOf (Decimal v)
      {
         return new _10 (rescale (v.getSignificand (), - v.getExponent (), 10));
      }
   
      public static _10 valueOf (FixedDec v)
      {
         if (v instanceof _10)
            return (_10)v;
         else
            return new _10 (rescale (v.getSignificand (), v.getScale (), 10));
      }
   
      public static _10 valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         bd = bd.setScale (10);
         return new _10 (bd.unscaledValue ().longValue ());
      }

      public static _10 getInstance (long significand)
      {
         return new _10 (significand);
      }
   }

   public static final class _N extends FixedDec
   {
      public _N (long sig, int scale) { super (sig); this.scale = scale; }
      @Override public int getScale () { return scale; }

      public static _N valueOf (long value)
      {
         return new _N (value, 0);
      }

      public static _N valueOf (Decimal v)
      {
         return new _N (v.getSignificand (), - v.getExponent ());
      }
   
      public static _N valueOf (FixedDec v)
      {
         if (v instanceof _N)
            return (_N)v;
         else
            return new _N (v.getSignificand (), v.getScale ());
      }
   
      public static _N valueOf (String s)
      {
         BigDecimal bd = new BigDecimal (s);
         return new _N (bd.unscaledValue ().longValue (), bd.scale ());
      }

      public static _N getInstance (long significand, int scale)
      {
         return new _N (significand, scale);
      }

      private final int scale;
   }

   private final long significand;
}
