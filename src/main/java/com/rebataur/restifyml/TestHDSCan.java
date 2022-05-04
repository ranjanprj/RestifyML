/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.rebataur.restifyml;

/**
 *
 * @author rebataur
 */
import java.nio.file.Paths;
import java.util.*;

import org.tribuo.Dataset;
import org.tribuo.MutableDataset;
import org.tribuo.clustering.ClusterID;
import org.tribuo.clustering.ClusteringFactory;
import org.tribuo.clustering.hdbscan.HdbscanTrainer;
import org.tribuo.data.columnar.FieldProcessor;
import org.tribuo.data.columnar.ResponseProcessor;
import org.tribuo.data.columnar.RowProcessor;
import org.tribuo.data.columnar.processors.field.DoubleFieldProcessor;
import org.tribuo.data.columnar.processors.response.EmptyResponseProcessor;
import org.tribuo.data.csv.CSVDataSource;
import org.tribuo.util.Util;

public class TestHDSCan {

    public static void main(String[] args) {
        ClusteringFactory clusteringFactory = new ClusteringFactory();
        ResponseProcessor<ClusterID> emptyResponseProcessor = new EmptyResponseProcessor<>(clusteringFactory);
        Map<String, FieldProcessor> regexMappingProcessors = new HashMap<>();
        regexMappingProcessors.put("BALANCE", new DoubleFieldProcessor("BALANCE"));
        regexMappingProcessors.put("BALANCE_FREQUENCY", new DoubleFieldProcessor("BALANCE_FREQUENCY"));
        regexMappingProcessors.put("PURCHASES", new DoubleFieldProcessor("PURCHASES"));
        regexMappingProcessors.put("ONEOFF_PURCHASES", new DoubleFieldProcessor("ONEOFF_PURCHASES"));
        regexMappingProcessors.put("INSTALLMENTS_PURCHASES", new DoubleFieldProcessor("INSTALLMENTS_PURCHASES"));
        regexMappingProcessors.put("CASH_ADVANCE", new DoubleFieldProcessor("CASH_ADVANCE"));
        regexMappingProcessors.put("PURCHASES_FREQUENCY", new DoubleFieldProcessor("PURCHASES_FREQUENCY"));
        regexMappingProcessors.put("ONEOFF_PURCHASES_FREQUENCY", new DoubleFieldProcessor("ONEOFF_PURCHASES_FREQUENCY"));
        regexMappingProcessors.put("PURCHASES_INSTALLMENTS_FREQUENCY", new DoubleFieldProcessor("PURCHASES_INSTALLMENTS_FREQUENCY"));
        regexMappingProcessors.put("CASH_ADVANCE_FREQUENCY", new DoubleFieldProcessor("CASH_ADVANCE_FREQUENCY"));
        regexMappingProcessors.put("CASH_ADVANCE_TRX", new DoubleFieldProcessor("CASH_ADVANCE_TRX"));
        regexMappingProcessors.put("PURCHASES_TRX", new DoubleFieldProcessor("PURCHASES_TRX"));
        regexMappingProcessors.put("CREDIT_LIMIT", new DoubleFieldProcessor("CREDIT_LIMIT"));
        regexMappingProcessors.put("PAYMENTS", new DoubleFieldProcessor("PAYMENTS"));
        regexMappingProcessors.put("MINIMUM_PAYMENTS", new DoubleFieldProcessor("MINIMUM_PAYMENTS"));
        regexMappingProcessors.put("PRC_FULL_PAYMENT", new DoubleFieldProcessor("PRC_FULL_PAYMENT"));
        regexMappingProcessors.put("TENURE", new DoubleFieldProcessor("TENURE"));

        RowProcessor<ClusterID> rowProcessor = new RowProcessor<>(emptyResponseProcessor, regexMappingProcessors);
        CSVDataSource<ClusterID> csvDataSource = new CSVDataSource<>(Paths.get("C:\\Users\\pranjan24\\Downloads\\cleanedCC.csv"), rowProcessor, false);
        Dataset<ClusterID> dataset = new MutableDataset<>(csvDataSource);

        System.out.println(String.format("Data size = %d, number of features = %d", dataset.size(), dataset.getFeatureMap().size()));

        var trainer = new HdbscanTrainer(5, HdbscanTrainer.Distance.EUCLIDEAN, 5, 4);
        var startTime = System.currentTimeMillis();
        var model = trainer.train(dataset);
        var endTime = System.currentTimeMillis();
        System.out.println(model.getClusterLabels());
        System.out.println(model.predict(dataset.getExample(0)));
        System.out.println("Training took " + Util.formatDuration(startTime, endTime));
    }
}
