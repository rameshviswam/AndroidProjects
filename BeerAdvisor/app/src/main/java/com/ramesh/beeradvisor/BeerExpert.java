package com.ramesh.beeradvisor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rviswana on 6/30/2015.
 */
public class BeerExpert {
    public List<String> getBrands(String color) {
        List<String> retStr = new ArrayList<String>();
        if(color.equals("amber")) {
            retStr.add("Jack Amber");
            retStr.add("Red Moose");
        } else {
            retStr.add("Jail Pale Ale");
            retStr.add("Gout Stout");
        }
        return retStr;
    }
}
