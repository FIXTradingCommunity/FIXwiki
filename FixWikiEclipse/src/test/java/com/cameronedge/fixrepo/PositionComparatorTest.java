/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixrepo;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author John Cameron
 */
public class PositionComparatorTest {
  @Test
  public void testCompare() {
    PositionComparator cmp = new PositionComparator();

    assertEquals(cmp.compare("1", "2"), -1);
    assertEquals(cmp.compare("1", "1"), 0);
    assertEquals(cmp.compare("2", "1"), 1);

    assertEquals(cmp.compare("1.1", "2.1"), -1);
    assertEquals(cmp.compare("1.2", "1.2"), 0);
    assertEquals(cmp.compare("2.1", "1.3"), 1);

    assertEquals(cmp.compare("1", "1.1"), -1);
    assertEquals(cmp.compare("1.1", "1"), 1);
  }
}
