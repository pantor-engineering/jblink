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
   The {@code Decimal} represents a Blink decimal value. It has an
   64-bit mantisssa and logically it has an 8-bit exponent even though
   an {@code int} is used for the representation here.

   <p>Instances of {@code Decimal} are immutable.</p>
*/

public final class Decimal
{
   /**
      Creates a decimal with the value 0 * 10^0
   */

   public Decimal () { this (0); }

   /**
      Creates a decimal with the value {@code mantissa} * 10^0

      @param mantissa a mantissa
   */

   public Decimal (long mantissa) { this (mantissa, 0); }

   /**
      Creates a decimal with the value {@code}mantissa * 10^{@code exponent}

      @param mantissa a mantissa
      @param exponent an exponent
   */

   public Decimal (long mantissa, int exponent)
   {
      this.exponent = exponent;
      this.mantissa = mantissa;
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
      Returns the mantissa

      @return the mantissa
   */

   public long getMantissa ()
   {
      return mantissa;
   }

   @Override
   public String toString ()
   {
      return String.valueOf (mantissa) + "*10^" + String.valueOf (exponent);
   }

   /**
      Returns a double representation of this value

      @return the double value of this decimal
   */

   public double toDouble ()
   {
      return (double)mantissa * Math.pow (10, exponent); 
   }

   /**
      Creates a decimal instance for the value {@code mantissa} * 10^0

      @param mantissa a mantissa
      @return a decimal value
   */

   public static Decimal valueOf (long mantissa)
   {
      return valueOf (mantissa, 0);
   }

   /**
      Creates a decimal instance for the value {@code mantissa} *
      10^{@code exponent}

      @param mantissa a mantissa
      @param exponent an exponent
      @return a decimal value
   */

   public static Decimal valueOf (long mantissa, int exponent)
   {
      return new Decimal (mantissa, exponent);
   }
   
   private final int exponent;
   private final long mantissa;
}
