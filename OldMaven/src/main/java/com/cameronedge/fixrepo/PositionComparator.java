/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixrepo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Sorts by Position property where property has form:
 * a.b.c etc
 * eg
 * 3.2.1
 * <p>
 * So ordering might be:
 * 1
 * 2
 * 3.1
 * 3.2
 * 3.3
 * 4
 * 5
 * @author John Cameron
 */
public class PositionComparator implements Comparator<String> {
  @Override
  public int compare(String o1, String o2) {
    List<Integer> p1 = parsePosition(o1);
    List<Integer> p2 = parsePosition(o2);


    Iterator<Integer> iter2 = p2.iterator();
    for (int n1 : p1) {
      if (iter2.hasNext()) {
        int n2 = iter2.next();
        if (n1 != n2) {
          if (n1 < n2) {
            return -1;
          } else {
            return 1;
          }
        }
      } else {
        //p2 is shorter. eg p1 is 3.1.2 and p2 is 3.1.
        //So p1 is greater than p2
        return 1;
      }
    }

    //p2 is longer. eg p1 is 3.1 and p2 is 3.1.2.
    //So p1 is less than p2.
    if (iter2.hasNext()) {
      return -1;
    }
    return 0;
  }

  static List<Integer> parsePosition(String position) {
    List<Integer> parsed = new ArrayList<Integer>();

    StringTokenizer stok = new StringTokenizer(position, ".");
    while (stok.hasMoreTokens()) {
      parsed.add(Integer.parseInt(stok.nextToken()));
    }

    return parsed;
  }

}
