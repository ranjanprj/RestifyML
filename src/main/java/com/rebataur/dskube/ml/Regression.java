/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.rebataur.dskube.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.rebataur.dskube.ModelBuildingDTO;
import com.rebataur.dskube.entities.DataColumn;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.tribuo.*;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.evaluation.TrainTestSplitter;
import org.tribuo.regression.*;
import org.tribuo.regression.evaluation.*;
import org.tribuo.regression.xgboost.XGBoostRegressionTrainer;
import org.tribuo.util.Util;

/**
 *
 * @author rebataur
 */
@Component
@Slf4j
public class Regression {

    private final String UPLOAD_DIR = "..\\dskube_datastore\\uploads\\";

    @Autowired
    private ExperimentRepository experimentRepository;
    @Autowired
    private DataSourceRepository dataSourceRepository;
    @Autowired
    private ColumnRepository columnRepository;

    public ModelBuildingDTO regression(long id, ModelBuildingDTO mDTO) throws IOException, ClassNotFoundException, com.opencsv.exceptions.CsvException {

        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        List<DataColumn> dcs = experiment.getDatasource().getDataColumn();
        experiment.setModelAlgo(mDTO.getModelAlgo());
        experiment.setSeed(mDTO.getSeed());
        experiment.setTrainProportion(mDTO.getTrainProportion());

        var regressionFactory = new RegressionFactory();
        var csvLoader = new CSVLoader<>(regressionFactory);
        var wineSource = csvLoader.loadDataSource(Paths.get(UPLOAD_DIR + experiment.getDatasource().getFilename()), mDTO.getTarget());
        var splitter = new TrainTestSplitter<>(wineSource, mDTO.getTrainProportion() / 100, 0L);
        Dataset<Regressor> trainData = new MutableDataset<>(splitter.getTrain());
        Dataset<Regressor> evalData = new MutableDataset<>(splitter.getTest());
        Regressor r = trainData.getExample(0).getOutput();
        System.out.println("Num dimensions = " + r.size());

        String[] dimNames = r.getNames();
        System.out.println("Dimension name: " + dimNames[0]);

        double[] regressedValues = r.getValues();
        System.out.println("Dimension value: " + regressedValues[0]);

// getDimension(String) returns an Optional<DimensionTuple>
        Regressor.DimensionTuple tuple = r.getDimension("DIM-0").get();
        System.out.println("Tuple = [" + tuple + "]");

// getDimension(int) throws IndexOutOfBoundsException if you give it a negative index
// or one greater than or equal to r.size()
//        Regressor.DimensionTuple tupleI = r.getDimension(0);
//        System.out.println("Regressor[0] = " + tupleI);
//
//        var lrsgd = new LinearSGDTrainer(
//                new SquaredLoss(), // loss function
//                SGD.getLinearDecaySGD(0.01), // gradient descent algorithm
//                10, // number of training epochs
//                trainData.size() / 4,// logging interval
//                1, // minibatch size
//                1L // RNG seed
//        );
//        var lrada = new LinearSGDTrainer(
//                new SquaredLoss(),
//                new AdaGrad(0.01),
//                10,
//                trainData.size() / 4,
//                1,
//                1L
//        );
//        var cart = new CARTRegressionTrainer(6);
        var xgb = new XGBoostRegressionTrainer(50);

//        var lrsgdModel = train("Linear Regression (SGD)", lrsgd, trainData);
//        
//        evaluate(lrsgdModel, evalData);
//        var lradaModel = train("Linear Regression (AdaGrad)", lrada, trainData);
//        evaluate(lradaModel, evalData);
//
//        var cartModel = train("CART", cart, trainData);
//        evaluate(cartModel, evalData);
        Model<Regressor> xgbModel = train("XGBoost", xgb, trainData);
        String regressionEvaluation = evaluate(xgbModel, evalData);
        Prediction<Regressor> prediction = xgbModel.predict(trainData.getExample(mDTO.getTestSampleRow()));
        mDTO.setRegressionOutput(prediction);
        mDTO.setRegressionExample(trainData.getExample(mDTO.getTestSampleRow()));
        mDTO.setRegressionEvaluation(regressionEvaluation);

        String modelFileName = experiment.getName() + "_MODEL.ser";
        File tmpFile = new File(UPLOAD_DIR + modelFileName);
        try ( ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmpFile))) {
            oos.writeObject(xgbModel);
        }
        com.rebataur.dskube.entities.DataSource ds = dataSourceRepository.findById(experiment.getDatasource().getId()).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));;
//        ds.setModelDataFileName(modelDataFileName);
//        dataSourceRepository.save(ds);
        ds.setModelFileName(modelFileName);
        dataSourceRepository.save(ds);

        return mDTO;

    }

    public Model<Regressor> train(String name, Trainer<Regressor> trainer, Dataset<Regressor> trainData) {
        // Train the model
        var startTime = System.currentTimeMillis();
        Model<Regressor> model = trainer.train(trainData);
        var endTime = System.currentTimeMillis();
        System.out.println("Training " + name + " took " + Util.formatDuration(startTime, endTime));
        // Evaluate the model on the training data
        // This is a useful debugging tool to check the model actually learned something
        RegressionEvaluator eval = new RegressionEvaluator();
        var evaluation = eval.evaluate(model, trainData);
        // We create a dimension here to aid pulling out the appropriate statistics.
        // You can also produce the String directly by calling "evaluation.toString()"
        var dimension = new Regressor("DIM-0", Double.NaN);
        System.out.printf("Evaluation (train):%n  RMSE %f%n  MAE %f%n  R^2 %f%n",
                evaluation.rmse(dimension), evaluation.mae(dimension), evaluation.r2(dimension));
        return model;
    }

    public String evaluate(Model<Regressor> model, Dataset<Regressor> testData) {
        // Evaluate the model on the test data
        RegressionEvaluator eval = new RegressionEvaluator();
        var evaluation = eval.evaluate(model, testData);
        // We create a dimension here to aid pulling out the appropriate statistics.
        // You can also produce the String directly by calling "evaluation.toString()"
        var dimension = new Regressor("DIM-0", Double.NaN);
        System.out.printf("Evaluation (test):%n  RMSE %f%n  MAE %f%n  R^2 %f%n",
                evaluation.rmse(dimension), evaluation.mae(dimension), evaluation.r2(dimension));
        return String.format("Evaluation (test):%n  RMSE %f%n  MAE %f%n  R^2 %f%n",
                evaluation.rmse(dimension), evaluation.mae(dimension), evaluation.r2(dimension));
    }

    public String invokeModel(long experimentId, String json) throws IOException, ClassNotFoundException, com.opencsv.exceptions.CsvException {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + experimentId));
        List<DataColumn> dcs = experiment.getDatasource().getDataColumn();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        var regressionFactory = new RegressionFactory();
        var csvLoader = new CSVLoader<>(regressionFactory);
        var irisHeaders = new String[dcs.size()];
        String[] csvData = new String[dcs.size()];
        String target = "";
        for (int i = 0; i < dcs.size(); i++) {
            irisHeaders[i] = dcs.get(i).getName();
            JsonNode nodeValue = node.at("/" + dcs.get(i).getName());
            csvData[i] = nodeValue.toString();
            if (nodeValue.toString().replaceAll("\"", "").equals("target")) {
                target = dcs.get(i).getName();
                csvData[i] = String.valueOf(-9999);
                System.out.println(dcs.get(i).getName());
                System.out.println(target);
            }
            System.out.println(nodeValue);
        }

        FileWriter outputfile = new FileWriter(UPLOAD_DIR + "temprequestfile.csv");
        CSVWriter writer = new CSVWriter(outputfile);
        writer.writeNext(irisHeaders);
        writer.writeNext(csvData);
        writer.close();
        System.out.println(target);
        var irisesSource = csvLoader.loadDataSource(Paths.get(UPLOAD_DIR + "temprequestfile.csv"), target);
//        var irisSplitter = new TrainTestSplitter<>(irisesSource, mDTO.getTrainProportion() / 100, 1l);
//        var trainingDataset = new MutableDataset<>(irisSplitter.getTrain());
        MutableDataset<Regressor> testingDataset = new MutableDataset<Regressor>(irisesSource);
        System.out.println(testingDataset.getExample(0));
//

        String filterPattern = Files.readAllLines(Paths.get(UPLOAD_DIR + "jep-290-filter.txt")).get(0);
        ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(filterPattern);
        org.tribuo.Model<Regressor> loadedModel;
        try ( ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(UPLOAD_DIR + experiment.getDatasource().getModelFileName())))) {
            ois.setObjectInputFilter(filter);
            loadedModel = (org.tribuo.Model<Regressor>) ois.readObject();
        }
        Prediction<Regressor> prediction = loadedModel.predict(testingDataset.getExample(0));
        System.out.println(prediction.getOutput());
        return prediction.getOutput().toString();
//        return "dd";
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, CsvException {
        new Regression().regression(1l, new ModelBuildingDTO());
    }
}
