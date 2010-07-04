/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixwiki;

import static org.junit.Assert.*;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;

/**
 * LinkDetector test case.
 *
 * @author John Cameron
 */
public class LinkDetectorTestCase {
  private LinkDetector linkDetector;
  private Map<String, String> linkNames1, linkNames2;

  @Before
  public void setUp() throws Exception {
    linkNames1 = new HashMap<String, String>();
    linkNames1.put("John", "John");
    linkNames1.put("Alexander", "Alexander");
    linkNames1.put("Duncan", "Duncan");
    linkNames1.put("Cameron", "Cameron");

    linkNames2 = new HashMap<String, String>();
    linkNames2.put("Dingo", "Baby:Dingo");
    linkNames2.put("John", "Baby:John");
    
    linkDetector = new LinkDetector(linkNames1, linkNames2);
  }

  @After
  public void tearDown() throws Exception {
    linkDetector = null;
  }
  
  @Test
  public void testConvert() {
    String s;
    
    s = linkDetector.convert("      Able was   --  I --   John [[John]] ere Dingo I saw Alexander(39) Elba       Duncan (54) instead Cameron");
    System.out.println(s);
    assertEquals( "      Able was   --  I --   [[John]] [[John]] ere [[Baby:Dingo|Dingo]] I saw [[Alexander]](39) Elba       [[Duncan]] (54) instead [[Cameron]]",
            s);
  }
  
}
