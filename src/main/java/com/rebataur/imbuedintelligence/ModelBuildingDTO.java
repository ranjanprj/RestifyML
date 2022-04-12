/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.rebataur.imbuedintelligence;

import lombok.Data;
import org.tribuo.Example;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.regression.Regressor;

@Data
public class ModelBuildingDTO {
    private Long experimentId;
    private String modelAlgo,target;
    private double trainProportion;
    private int seed;
    private String confusionMatrix;
    private int testSampleRow;
    private Example<Label> example;
    private Label output;    
    private String mlType;
    private String regressionEvaluation;
    private Prediction<Regressor> regressionOutput;
    private Example<Regressor> regressionExample;
    
    
}
