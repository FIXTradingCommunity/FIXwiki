/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixwiki;

/**
 * @author John Cameron
 */
public class FIXVersionInfo {
  public String version;
  public int maxTag;
  public String suffix;

  public FIXVersionInfo(String version, String suffix, int maxTag) {
    this.version = version;
    this.suffix = suffix;
    this.maxTag = maxTag;
  }
}
