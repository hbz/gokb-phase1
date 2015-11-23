package com.k_int.gokb.refine;

import com.k_int.gokb.refine.notifications.Notification;

public class ValidationMessage extends Notification {
  private String severity;
  private String type;
  private String col;
  private String sub_type;
  private String facetValue;
  private String transformation;
  private String facetName;

  public String getSeverity () {
    return severity;
  }
  public void setSeverity (String severity) {
    this.severity = severity;
  }
  public String getType () {
    return type;
  }
  public void setType (String type) {
    this.type = type;
  }
  public String getTitle () {
    return getCol();
  }
  public void setTitle (String title) {
    setCol(type);
  }
  public String getCol () {
    return col;
  }
  public void setCol (String col) {
    this.col = col;
  }
  public void setSub_type(String stype) {
    this.sub_type = stype;
  }

  public String getSub_type(){
    return sub_type;
  }

  public String getFacetValue(){
    return facetValue;
  }
  public void setFacetValue(String val){
    this.facetValue = val;
  }
  public String getFacetName(){
    return facetName;
  }
  public void setFacetName(String val){
    this.facetName = val;
  }
  public String getTransformation(){
    return transformation;
  }
  public void setTransformation(String val){
    this.transformation = val;
  }
}
