/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.rebataur.dskube.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.rebataur.dskube.ModelBuildingDTO;
import com.rebataur.dskube.entities.DataColumn;
import com.rebataur.dskube.entities.DataSource;
import com.rebataur.dskube.entities.Experiment;
import com.rebataur.dskube.repositories.ColumnRepository;
import com.rebataur.dskube.repositories.DataSourceRepository;
import com.rebataur.dskube.repositories.ExperimentRepository;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tribuo.MutableDataset;
import org.tribuo.Prediction;
import org.tribuo.Trainer;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.evaluation.LabelEvaluation;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.evaluation.TrainTestSplitter;

/**
 *
 * @author rebataur
 */
@Component
public class Classification {

    @Autowired
    private ExperimentRepository experimentRepository;
    @Autowired
    private DataSourceRepository dataSourceRepository;
    @Autowired
    private ColumnRepository columnRepository;

    private final String UPLOAD_DIR = "..\\dskube_datastore\\uploads\\";

    public ModelBuildingDTO classify(long id, ModelBuildingDTO mDTO) throws IOException, ClassNotFoundException, com.opencsv.exceptions.CsvException {
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        List<DataColumn> dcs = experiment.getDatasource().getDataColumn();
        experiment.setModelAlgo(mDTO.getModelAlgo());
        experiment.setSeed(mDTO.getSeed());
        experiment.setTrainProportion(mDTO.getTrainProportion());

        var labelFactory = new LabelFactory();
        var csvLoader = new CSVLoader<>(labelFactory);
        var irisHeaders = new String[dcs.size()];
        for (int i = 0; i < dcs.size(); i++) {
            irisHeaders[i] = dcs.get(i).getName();

        }

        // Remove header
        CSVReader reader2 = new CSVReader(new java.io.FileReader(UPLOAD_DIR + experiment.getDatasource().getFilename()));
        List<String[]> allElements = reader2.readAll();
        allElements.remove(0);
        String modelDataFileName = "MODEL_" + experiment.getDatasource().getFilename();
        FileWriter sw = new FileWriter(UPLOAD_DIR + modelDataFileName);
        CSVWriter writer = new CSVWriter(sw);
        writer.writeAll(allElements);
        writer.close();
        DataSource ds = dataSourceRepository.findById(experiment.getDatasource().getId()).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));;
        ds.setModelDataFileName(modelDataFileName);
        dataSourceRepository.save(ds);

        var irisesSource = csvLoader.loadDataSource(Paths.get(UPLOAD_DIR + experiment.getDatasource().getModelDataFileName()), mDTO.getTarget(), irisHeaders);
        var irisSplitter = new TrainTestSplitter<>(irisesSource, mDTO.getTrainProportion() / 100, mDTO.getSeed());
        var trainingDataset = new MutableDataset<>(irisSplitter.getTrain());
        MutableDataset<Label> testingDataset = new MutableDataset<>(irisSplitter.getTest());

        Trainer<Label> trainer = new LogisticRegressionTrainer();
//        System.out.println(trainer.toString());
        org.tribuo.Model<Label> irisModel = trainer.train(trainingDataset);
        var evaluator = new LabelEvaluator();
        LabelEvaluation evaluation = evaluator.evaluate(irisModel, testingDataset);

//        System.out.println(evaluation.toString());
//        System.out.println(evaluation.getConfusionMatrix().toString());
//        var featureMap = irisModel.getFeatureIDMap();
//        for (var v : featureMap) {
//            System.out.println(v.toString());
//            System.out.println();
//        }
        var linearTrainer = new LogisticRegressionTrainer();
        org.tribuo.Model<Label> linear = linearTrainer.train(trainingDataset);

// Finally we make predictions on unseen data
// Each prediction is a map from the output names (i.e. the labels) to the scores/probabilities
        Prediction<Label> prediction = linear.predict(testingDataset.getExample(mDTO.getTestSampleRow()));
        mDTO.setExample(testingDataset.getExample(mDTO.getTestSampleRow()));
        mDTO.setOutput(prediction.getOutput());

        experimentRepository.save(experiment);
        mDTO.setConfusionMatrix(evaluation.getConfusionMatrix().toString());
        String modelFileName = experiment.getName() + "_MODEL.ser";
        File tmpFile = new File(UPLOAD_DIR + modelFileName);
        try ( ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmpFile))) {
            oos.writeObject(irisModel);
        }
        ds.setModelFileName(modelFileName);
        dataSourceRepository.save(ds);
        return mDTO;
    }
    
     public String invokeModel(long experimentId, String json) throws IOException, ClassNotFoundException, com.opencsv.exceptions.CsvException {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + experimentId));
        List<DataColumn> dcs = experiment.getDatasource().getDataColumn();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        var labelFactory = new LabelFactory();
        var csvLoader = new CSVLoader<>(labelFactory);
        var newheaders = new String[dcs.size()];
        String[] csvData = new String[dcs.size()];
        String target = "";
//        for (int i = 0; i < dcs.size(); i++) {
//            irisHeaders[i] = dcs.get(i).getName();
//            JsonNode nodeValue = node.at("/" + dcs.get(i).getName());
//            csvData[i] = nodeValue.toString();
//            if (nodeValue.toString().replaceAll("\"", "").equals("target")) {
//                target = dcs.get(i).getName();
//                System.out.println(dcs.get(i).getName());
//                System.out.println(target);
//            }
//            System.out.println(nodeValue);
//        }
        
            for (int i = 0; i < dcs.size(); i++) {
            newheaders[i] = dcs.get(i).getName();
            String colname = "";
            String colvalue = "";
             JsonNode nodeValue = null;
            if (dcs.get(i).getName().contains("_")) {
                colname = dcs.get(i).getName().split("_")[0];
                colvalue = dcs.get(i).getName().split("_")[1];
                nodeValue = node.at("/" + colname);
                if (nodeValue.equals(colvalue)) {
                    csvData[i] = "1";
                } else {
                    csvData[i] = "0";
                }

            } else {
                nodeValue = node.at("/" + dcs.get(i).getName());

                csvData[i] = nodeValue.toString();
            }
            // put random number to suffice the model execution
            if (nodeValue.toString().replaceAll("\"", "").equals("target")) {
                target = dcs.get(i).getName();               
                System.out.println(dcs.get(i).getName());
                System.out.println(target);
            }
            System.out.println(nodeValue);
        }

        FileWriter outputfile = new FileWriter(UPLOAD_DIR + "temprequestfile.csv");
        CSVWriter writer = new CSVWriter(outputfile);

        writer.writeNext(csvData);
        writer.close();
        System.out.println(target);
        var irisesSource = csvLoader.loadDataSource(Paths.get(UPLOAD_DIR + "temprequestfile.csv"), target, newheaders);
//        var irisSplitter = new TrainTestSplitter<>(irisesSource, mDTO.getTrainProportion() / 100, 1l);
//        var trainingDataset = new MutableDataset<>(irisSplitter.getTrain());
        MutableDataset<Label> testingDataset = new MutableDataset<>(irisesSource);
        System.out.println(testingDataset.getExample(0));
//
        String filterPattern = Files.readAllLines(Paths.get(UPLOAD_DIR + "jep-290-filter.txt")).get(0);
        ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(filterPattern);
        org.tribuo.Model<Label> loadedModel;
        try ( ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(UPLOAD_DIR + experiment.getDatasource().getModelFileName())))) {
            ois.setObjectInputFilter(filter);
            loadedModel = (org.tribuo.Model<Label>) ois.readObject();
        }
        Prediction<Label> prediction = loadedModel.predict(testingDataset.getExample(0));
        System.out.println(prediction.getOutput());
        return prediction.getOutput().toString();
//        return "dd";
    }
}
