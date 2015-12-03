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

import java.math.BigDecimal;

/**
   The {@code Decimal} represents a Blink decimal value. It has an
   64-bit significand and logically it has an 8-bit exponent even
   though an {@code int} is used for the representation here.

   <p>Instances of {@code Decimal} are immutable.</p>
*/

public final class Decimal
{
   /**
      Creates a decimal with the value 0 * 10^0
   */

   public Decimal () { this (0); }

   /**
      Creates a decimal with the value {@code significand} * 10^0

      @param significand a significand
   */

   public Decimal (long significand) { this (significand, 0); }

   /**
      Creates a decimal with the value {@code}significand * 10^{@code exponent}

      @param significand a significand
      @param exponent an exponent
   */

   public Decimal (long significand, int exponent)
   {
      this.exponent = exponent;
      this.significand = significand;
   }
   
   /**
      Returns the exponent

      @return the exponent
   */

   public int getExponent ()
   {
      return exponent;
   }
   
   /**
      Returns the significand

      @return the significand
   */

   public long getSignificand ()
   {
      return significand;
   }

   @Override
   public boolean equals (Object other)
   {
      if (other instanceof Decimal)
      {
         Decimal o = (Decimal)other;
         return significand == o.significand && exponent == o.exponent;
      }
      else
         return false;
   }

   @Override
   public int hashCode ()
   {
      return (int)significand * 31 + exponent;
   }
   
   @Override
   public String toString ()
   {
      return String.valueOf (significand) + "E" + String.valueOf (exponent);
   }

   /**
      Returns a double representation of this value

      @return the double value of this decimal
   */

   public double doubleValue ()
   {
      return (double)significand * Math.pow (10, exponent); 
   }

   /**
      Creates a decimal instance for the value {@code significand} * 10^0

      @param significand a significand
      @return a decimal value
   */

   public static Decimal valueOf (long significand)
   {
      return valueOf (significand, 0);
   }

   /**
      Creates a decimal instance for the value {@code significand} *
      10^{@code exponent}

      @param significand a significand
      @param exponent an exponent
      @return a decimal value
   */

   public static Decimal valueOf (long significand, int exponent)
   {
      return new Decimal (significand, exponent);
   }


   /**
      Creates a decimal instance by parsing the specified string.

      @param s a string on the same format accepted by {@link
      java.math.BigDecimal}
      @return a decimal value
   */
   
   public static Decimal valueOf (String s)
   {
      BigDecimal bd = new BigDecimal (s);
      return valueOf (bd.unscaledValue ().longValue (), - bd.scale ());
   }
   
   private final int exponent;
   private final long significand;
}
