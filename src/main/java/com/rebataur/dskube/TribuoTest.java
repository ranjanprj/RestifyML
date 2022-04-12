/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.rebataur.dskube;

import java.nio.file.Paths;
import java.nio.file.Files;

import org.tribuo.*;
import org.tribuo.evaluation.TrainTestSplitter;
import org.tribuo.data.csv.CSVDataSource;
import org.tribuo.classification.*;
import org.tribuo.classification.evaluation.*;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;

import com.fasterxml.jackson.databind.*;
import com.oracle.labs.mlrg.olcut.provenance.ProvenanceUtil;
import com.oracle.labs.mlrg.olcut.config.json.*;
import java.io.IOException;
import org.tribuo.data.csv.CSVLoader;


public class TribuoTest {

    public static void main(String[] args) throws IOException, IOException, IOException {
        // Load labelled iris data
        var labelFactory = new LabelFactory();
        var csvLoader = new CSVLoader<>(labelFactory);
        var irisHeaders = new String[]{"sepalLength", "sepalWidth", "petalLength", "petalWidth", "species"};

        var irisesSource = csvLoader.loadDataSource(Paths.get("C:\\Users\\pranjan24\\Downloads\\bezdekIris.data"), "species", irisHeaders);
        var irisSplitter = new TrainTestSplitter<>(irisesSource, 0.7, 1L);
        var trainingDataset = new MutableDataset<>(irisSplitter.getTrain());
        var testingDataset = new MutableDataset<>(irisSplitter.getTest());

        Trainer<Label> trainer = new LogisticRegressionTrainer();
//        System.out.println(trainer.toString());
        Model<Label> irisModel = trainer.train(trainingDataset);

        var evaluator = new LabelEvaluator();
        var evaluation = evaluator.evaluate(irisModel, testingDataset);

//        System.out.println(evaluation.toString());
//        System.out.println(evaluation.getConfusionMatrix().toString());
        var featureMap = irisModel.getFeatureIDMap();
//        for (var v : featureMap) {
//            System.out.println(v.toString());
//            System.out.println();
//        }

// Or a logistic regression
        var linearTrainer = new LogisticRegressionTrainer();
        Model<Label> linear = linearTrainer.train(trainingDataset);

// Finally we make predictions on unseen data
// Each prediction is a map from the output names (i.e. the labels) to the scores/probabilities
        Prediction<Label> prediction = linear.predict(testingDataset.getExample(0));
       
        System.out.println("====================================================");
        System.out.println(testingDataset.getExample(0));
        System.out.println( prediction.getOutput());

// Or we can evaluate the full test dataset, calculating the accuracy, F1 etc.
        evaluation = new LabelEvaluator().evaluate(linear, testingDataset);
// we can inspect the evaluation manually
        double acc = evaluation.accuracy();
// which returns 0.978
// or print a formatted evaluation string
//        System.out.println(evaluation.toString());
//        var provenance = irisModel.getProvenance();
//        System.out.println(ProvenanceUtil.formattedProvenanceString(provenance.getDatasetProvenance().getSourceProvenance()));
    }
}
