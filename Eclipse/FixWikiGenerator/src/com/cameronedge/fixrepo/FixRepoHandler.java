/*
 * Copyright (c) 2009 Cameron Edge Pty Ltd. All rights reserved.
 * Reproduction in whole or in part in any form or medium without express
 * written permission of Cameron Edge Pty Ltd is strictly prohibited.
 */

package com.cameronedge.fixrepo;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Basic handler used for SAX parsing of FIX repository files.
 *
 * @author John Cameron
 */
public class FixRepoHandler extends DefaultHandler {

  private Map<String, List<Properties>> infos = new HashMap<String, List<Properties>>();

  private Properties currentProps;
  private String currentElement;
  private String currentText;

  private int depth = 0;
  private String startElement;
  private String indexElement;
  private boolean multiValued;

  /**
   * Processes lists of XML elements named startElement.
   * <p/>
   * Stores their attributes and sub element contents as Properties.
   * <p/>
   * Data is stored in a Map indexed by the property named indexElement.
   * <p/>
   * The values for each index are stored in a List of Properties (which allows for muliple values for the
   * name index value).
   *
   * @param startElement Name of top level information element in XML file.
   * @param indexElement Name of element used to index the parsed data. Used as key in output infos Map.
   * @param multiValued  True if mutiple values are allowed for the given index. If false, the List<Properties>
   *                     lists in the returned infos Map will all only contain one element (ie one Properties).
   */
  public FixRepoHandler(String startElement, String indexElement, boolean multiValued) {
    this.startElement = startElement;
    this.indexElement = indexElement;
    this.multiValued = multiValued;
  }

  private void addProp(String key, String value) {
    if (value != null) {

      if (key == null || key.length()==0) {
        System.out.println("No key for value " + value + " in " + currentProps);        
      }

      //Clean up values.
      String clean = RepoUtil.cleanText(value);

      Object old = currentProps.setProperty(key, clean);
      if (old != null) {
        throw new RuntimeException("Overwriting property - " + key + " in " + currentElement);
      }
    }
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
//    System.out.println("Element start");

    //In this simplistic model of representing the data as a simple flat List or Properties we must ignore elements
    // that are nested beyond 2 deep.
    if (depth > 2) {
//      throw new RuntimeException("Element depth exceeded - " + qName);
      depth++;
    } else {
      currentElement = qName;

      if (attributes.getLength() > 0) {
        //Check that we have no attributes on lower level elements.
        if (depth > 1) {
          throw new RuntimeException("Attributes of nested element - " + qName);
        }
      }


      depth++;

      if (qName.equals(startElement)) {
        currentProps = new Properties();

        //Process attributes.
        for (int i = 0; i < attributes.getLength(); i++) {
          addProp(attributes.getQName(i), attributes.getValue(i));
        }
      }
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (currentText == null) {
      currentText = new String(ch, start, length);
    } else {
      currentText += new String(ch, start, length);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {  
    if (qName.equals(startElement) && depth == 2) { //Need the depth check because element MsgType also has subelement MsgType!
      String indexElementValue = currentProps.getProperty(indexElement);
      String elementIndex = indexElementValue.trim();
      
      if (!indexElementValue.equals(elementIndex)) {
        currentProps.setProperty(indexElement, elementIndex);
      }

      List<Properties> propList = infos.get(elementIndex);
      if (propList == null) {
        propList = new ArrayList<Properties>();
        infos.put(elementIndex, propList);
      }
      if (!multiValued) {
        if (propList.size() != 0) {
          throw new RuntimeException("Multiple value for - " + elementIndex + ". Value " + currentProps);
        }
      }
      propList.add(currentProps);
    } else {  		
      if (currentElement != null && depth == 3) {   	  
        addProp(currentElement, currentText.trim());
      }
    }
    currentElement = null;
    currentText = null;

    depth--;
  }

  public Map<String, List<Properties>> getInfos() {
    return infos;
  }
}
