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
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class Time
{
   // Converts a blink date value to a Java Date in the local timezone
   
   public static Date toDate (int daysSinceEpoch)
   {
      Calendar cal = Calendar.getInstance ();
      setDate (daysSinceEpoch, cal);
      return cal.getTime ();
   }

   // Converts a blink date value to a Java Date in the spezified timezone

   public static Date toDate (int daysSinceEpoch, TimeZone tz)
   {
      Calendar cal = Calendar.getInstance (tz);
      setDate (daysSinceEpoch, cal);
      return cal.getTime ();
   }

   // Converts a java date to a blink date
   
   public static int toDaysSinceEpoch (Date d)
   {
      Calendar cal = Calendar.getInstance ();
      cal.setTime (d);
      return toDaysSinceEpoch (cal.get (Calendar.YEAR),
                               cal.get (Calendar.MONTH) - 1,
                               cal.get (Calendar.DAY_OF_MONTH));
   }

   // Converts a java date to a blink date

   public static int toDaysSinceEpoch (Date d, TimeZone tz)
   {
      Calendar cal = Calendar.getInstance (tz);
      cal.setTime (d);
      return toDaysSinceEpoch (cal.get (Calendar.YEAR),
                               cal.get (Calendar.MONTH) - 1,
                               cal.get (Calendar.DAY_OF_MONTH));
   }

   public static Date toDate (int date, int timeOfDayMilli)
   {
      // FIXME
      return null;
   }

   public static Date toDate (int date, int timeOfDayMilli, TimeZone tz)
   {
      // FIXME
      return null;
   }

   public static int toMillisecsSinceMidnight (Date d)
   {
      // FIXME
      return 0;
   }

   public static int toMillisecsSinceMidnight (Date d, TimeZone tz)
   {
      // FIXME
      return 0;
   }
   
   public static final int EPOCH_OFFSET = 730425;
   
   public static void setDate (int daysSinceEpoch, Calendar cal)
   {
      long days = daysSinceEpoch + EPOCH_OFFSET;
      long y = (10000*days + 14780) / 3652425;
      long ddd = days - (365*y + y/4 - y/100 + y/400);
      if (ddd < 0)
      {
         -- y;
         ddd = days - (365*y + y/4 - y/100 + y/400);
      }
      long mi = (100*ddd + 52) / 3060;
      long mm = (mi + 2) % 12 + 1;
      y = y + (mi + 2) / 12;
      long dd = ddd - (mi*306 + 5) / 10 + 1;
      
      cal.set ((int)y, (int)mm, (int)dd);
   }

   public static int toDaysSinceEpoch (int y_, int mm, int dd)
   {
      long m = (mm + 9) % 12;
      long y = y_ - m / 10;
      long days = 365*y + y/4 - y/100 + y/400 + (m*306 + 5)/10 + (dd - 1);
      return (int)days - EPOCH_OFFSET;
   }
}
