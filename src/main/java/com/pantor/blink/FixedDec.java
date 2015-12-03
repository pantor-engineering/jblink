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
      Creates a fixedDec({@code scale}) instance from an integer

      @param value an long integer
      @param scale a scale, the scale must be >= 0
      @return a fixed decimal value
   */

   public static FixedDec valueOf (long value, int scale)
   {
      return valueOf (value, 0, scale);
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
      switch (toScale)
      {
       case 0: return new _0 (rescale (value, fromScale, 0));
       case 1: return new _1 (rescale (value, fromScale, 1));
       case 2: return new _2 (rescale (value, fromScale, 2));
       case 3: return new _3 (rescale (value, fromScale, 3));
       case 4: return new _4 (rescale (value, fromScale, 4));
       case 5: return new _5 (rescale (value, fromScale, 5));
       case 6: return new _6 (rescale (value, fromScale, 6));
       case 7: return new _7 (rescale (value, fromScale, 7));
       case 8: return new _8 (rescale (value, fromScale, 8));
       case 9: return new _9 (rescale (value, fromScale, 9));
       case 10: return new _10 (rescale (value, fromScale, 10));
       default:
         if (toScale >= 0)
            return new _N (rescale (value, fromScale, toScale), toScale);
         else
            throw new IllegalArgumentException (
               "FixedDec scale must be >= 0: " +
               String.valueOf (toScale));
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
      public _0 (long sig) { super (sig); }
      @Override public int getScale () { return 0; }
   }

   public static final class _1 extends FixedDec
   {
      public _1 (long sig) { super (sig); }
      @Override public int getScale () { return 1; }
   }
   
   public static final class _2 extends FixedDec
   {
      public _2 (long sig) { super (sig); }
      @Override public int getScale () { return 2; }
   }
   
   public static final class _3 extends FixedDec
   {
      public _3 (long sig) { super (sig); }
      @Override public int getScale () { return 3; }
   }
   
   public static final class _4 extends FixedDec
   {
      public _4 (long sig) { super (sig); }
      @Override public int getScale () { return 4; }
   }
   
   public static final class _5 extends FixedDec
   {
      public _5 (long sig) { super (sig); }
      @Override public int getScale () { return 5; }
   }

   public static final class _6 extends FixedDec
   {
      public _6 (long sig) { super (sig); }
      @Override public int getScale () { return 6; }
   }
   
   public static final class _7 extends FixedDec
   {
      public _7 (long sig) { super (sig); }
      @Override public int getScale () { return 7; }
   }
   
   public static final class _8 extends FixedDec
   {
      public _8 (long sig) { super (sig); }
      @Override public int getScale () { return 8; }
   }

   public static final class _9 extends FixedDec
   {
      public _9 (long sig) { super (sig); }
      @Override public int getScale () { return 9; }
   }
   
   public static final class _10 extends FixedDec
   {
      public _10 (long sig) { super (sig); }
      @Override public int getScale () { return 10; }
   }
   
   public static final class _N extends FixedDec
   {
      public _N (long sig, int scale) { super (sig); this.scale = scale; }
      @Override public int getScale () { return scale; }
      private final int scale;
   }

   private final long significand;
}
