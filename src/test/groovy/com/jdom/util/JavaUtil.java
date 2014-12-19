package com.jdom.util;

import java.util.List;

/**
 * Created by djohnson on 12/19/14.
 */
public class JavaUtil {

   static long[] convertLongListToPrimitivateArray(List<Long> list) {
      long[] vals = new long[list.size()];

      int i = 0;
      for (Long object : list) {
         vals[i++] = object;
      }

      return vals;
   }
}
