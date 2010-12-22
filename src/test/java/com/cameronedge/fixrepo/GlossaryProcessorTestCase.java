/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixrepo;

import static org.junit.Assert.*;
import org.junit.*;

import java.util.List;
import java.io.StringReader;
import java.io.IOException;

/**
 * GlossaryProcessor test case.
 *
 * @author John Cameron
 */
public class GlossaryProcessorTestCase {
  private GlossaryProcessor gp = new GlossaryProcessor();

  @Before
  public void setUp() throws Exception {
    gp = new GlossaryProcessor();
  }

  @After
  public void tearDown() throws Exception {
    gp = null;
  }

  @Test
  public void testExtraction() throws IOException {
    String s =
    "Term	Definition	Field where used\n"+
    "Acceptable Counterparty	A counterparty eligible for trading with the order or quote Initiatior.	[PartyRole]\n"+
    "Accrued Interest Rate	The amount the buyer compensates the seller for the portion of the next coupon interest payment the seller has earned but will not receive from the issuer because the issuer will send the next coupon payment to the buyer.  Accrued Interest Rate is the annualized Accrued Interest amount divided by the purchase price of the bond.\t\n"+
    "ACPN	Accrued Coupon (ACPN) is a pro-rated amount from the prior coupon date to the current business date which is collateralized by the clearing house\n"+
    "[from EP83]	[PosAmtTyp]\n"+
    "After Tax Yield	Municipals.  The yield on the bond net of any tax consequences from holding the bond.  The discount on municipal securities can be subject to both capital gains taxes and ordinary income taxes.  Calculated from dollar price.	[YieldType]\n"+
    "All or None	A round-lot market or limit-price order that must be executed in its entirety or not at all; unlike Fill or Kill orders, AON orders are not treated as canceled if they are not executed as soon as represented in the Trading Crowd.	[ExecInst]\n"+
    "Allowances	Under an emissions cap and trade program, each allowance entitles the holder to emit some amount of gas such as carbon.  Sources that emit less than their emissions cap can sell allowances to those sources needing to purchase additional allowances to comply with the cap.  Emission sources can then decide whether to control emissions through control technology or through allowance surrender to meet compliance.\n"+
    "[from EP89]	[UnitOfMeasure]\n"+
    "American style option	An option that can be exercised at anytime before its expiration date.\n"+
    "Source: www.investopedia.com and www.investorwords.com\n"+
    "	[ExerciseStyle]\n";

    StringReader is = new StringReader(s);

    List<GlossaryEntry> data = gp.processGlossaryText(is);

    

  }

}
