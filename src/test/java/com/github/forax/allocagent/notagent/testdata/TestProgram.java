package com.github.forax.allocagent.notagent.testdata;

public class TestProgram {
  public static void main(String[] args) {
    new Object();
    new Object();
    var as = new String[5];
    var ai = new int[10];
    var aii = new int[2][3];
  }
}