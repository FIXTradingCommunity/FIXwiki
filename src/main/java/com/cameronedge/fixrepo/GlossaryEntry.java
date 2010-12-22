/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixrepo;

/**
 * @author John Cameron
 */
public class GlossaryEntry {
  public String fieldName;
  public String valueName;
  public String description;

  public String toString() {
    return fieldName + ":" + valueName + "\n" + description;
  }
}
