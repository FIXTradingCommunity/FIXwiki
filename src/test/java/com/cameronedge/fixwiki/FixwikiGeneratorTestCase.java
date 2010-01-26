package com.cameronedge.fixwiki;

import static org.junit.Assert.*;
import org.junit.*;
import com.cameronedge.fixrepo.Util;

/**
 * FixwikiGenerator test case.
 *
 * @author John Cameron
 */
public class FixwikiGeneratorTestCase {
  
  //TODO JC Complete test cases 

  @Test
  public void testExtractUnit() {

    String s = FixwikiGenerator.extractHint("I was > than I'd thought but < than I'd wished");

    assertEquals( s.indexOf('\''), -1);
    assertEquals( s.indexOf('>'), -1);
    assertEquals( s.indexOf('<'), -1);

  }
}
