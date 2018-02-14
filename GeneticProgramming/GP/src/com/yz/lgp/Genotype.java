package com.yz.lgp;

import com.yz.lgp.Function.*;
import java.util.ArrayList;


public class Genotype {
    ArrayList<Function> individual;
    public double [] desRegister;
    public double fitness;
    private final int maxProgLength, minProgLength, numOfdesRegister;
    final String[] functions = {"Add", "Minus", "Multiply", "Divide"};
}
