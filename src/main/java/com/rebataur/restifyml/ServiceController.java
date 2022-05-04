package com.rebataur.restifyml;

import com.rebataur.restifyml.ml.Regression;
import com.rebataur.restifyml.ml.Classification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import com.rebataur.restifyml.entities.DataColumn;
import com.rebataur.restifyml.entities.DataSource;
import com.rebataur.restifyml.entities.Experiment;
import com.rebataur.restifyml.repositories.ColumnRepository;
import com.rebataur.restifyml.repositories.DataSourceRepository;
import com.rebataur.restifyml.repositories.ExperimentRepository;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.tribuo.MutableDataset;
import org.tribuo.Prediction;
import org.tribuo.Trainer;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.evaluation.TrainTestSplitter;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.api.ScatterPlot;
import tech.tablesaw.plotly.components.Figure;

@RestController
public class ServiceController {

    @Autowired
    private ExperimentRepository experimentRepository;
    @Autowired
    private DataSourceRepository dataSourceRepository;
    @Autowired
    private ColumnRepository columnRepository;

    @Autowired
    private Classification classification;
    @Autowired
    private Regression regression;

    private final String UPLOAD_DIR = "..\\restifyml_datastore\\uploads\\";

    @PostMapping("/experiment/{id}/datacleansing")
    public ColumnCleansingDTO experimentDataCleansingColumn(@PathVariable("id") long id, @RequestBody ColumnCleansingDTO dcDTO) throws IOException {
        System.out.println(dcDTO);
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        DataColumn dc = columnRepository.findById(dcDTO.getColumnId()).orElseThrow(() -> new IllegalArgumentException("Invalid DataColumn Id:" + id));
        dc.setActualType(dcDTO.getActualType());
        columnRepository.save(dc);
        return dcDTO;

    }

    @GetMapping("/experiment/{id}/datacleansing/cleanallcolumns")
    public ResponseEntity<String> cleanData(@PathVariable("id") long id) throws IOException {
        System.out.println("CLENAING DATA*******************");
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));

        List<DataColumn> dcs = experiment.getDatasource().getDataColumn();

        Table t = Table.read().file(UPLOAD_DIR + experiment.getDatasource().getFilename());

        // First remove all rows with missing values in column
        for (DataColumn col : dcs) {
            if (col.getActualType() != null) {
                if (col.getActualType().equals("Integer")) {
                    Pattern p = Pattern.compile("\\d+");
                    Integer[] newList = new Integer[t.column("cough").size()];
                    for (int i = 0; i < t.rowCount(); i++) {
                        String v = t.column(col.getName()).get(i).toString();
                        System.out.println(v);

                        Matcher m = p.matcher(v);
                        while (m.find()) {
                            System.out.println(m.group());
                            newList[i] = Integer.parseInt(m.group());
                        }
                    }
                    t.removeColumns(col.getName());
                    IntColumn column = IntColumn.create(col.getName(), newList);
                    t.addColumns(column);

                }
            }
        }
        String cleansedFileName = "CLEANSED_" + experiment.getDatasource().getFilename();
        t.write().csv(UPLOAD_DIR + cleansedFileName);
        DataSource ds = dataSourceRepository.findById(experiment.getDatasource().getId()).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));;
        ds.setFilename(cleansedFileName);
        dataSourceRepository.save(ds);

        return new ResponseEntity<>(HttpStatus.CREATED);

    }

    @GetMapping("/experiment/{id}/dataexploration/plot/{plotType}/{xaxis}/{yaxis}")
    public String experimentDataExploration(@PathVariable("id") long id, @PathVariable("plotType") String plotType, @PathVariable("xaxis") String xaxis, @PathVariable("yaxis") String yaxis, Model model) throws IOException {
        System.out.println(id + plotType + xaxis + yaxis);
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        model.addAttribute("experiment", experiment);
        Table t = Table.read().file(UPLOAD_DIR + experiment.getDatasource().getFilename()).sampleN(100);
//        Plot.show(ScatterPlot.create("Wins vs BA", baseball, "BA", "W"));
        Figure plot = ScatterPlot.create(xaxis + " vs " + yaxis, t, xaxis, yaxis);
        return plot.asJavascript("test_div");
    }

    @PostMapping("/experiment/{id}/featurebuilding")
    public ColumnCleansingDTO featureBuildingColumnTreatment(@PathVariable("id") long id, @RequestBody ColumnCleansingDTO dcDTO) throws IOException {
        System.out.println(dcDTO);
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        DataColumn dc = columnRepository.findById(dcDTO.getColumnId()).orElseThrow(() -> new IllegalArgumentException("Invalid DataColumn Id:" + id));
        dc.setTreatmentType(dcDTO.getTreatmentType());
        columnRepository.save(dc);

        return dcDTO;
    }

    @GetMapping("/experiment/{id}/featurebuilding/treatcolumns")
    public ResponseEntity<String> featureBuilding(@PathVariable("id") long id) throws IOException {
        System.out.println("CLENAING DATA*******************");
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));

        List<DataColumn> dcs = experiment.getDatasource().getDataColumn();
        Table t = Table.read().file(UPLOAD_DIR + experiment.getDatasource().getFilename());
        // First remove all columns 
        for (DataColumn col : dcs) {
            if (col.getTreatmentType() != null && col.getTreatmentType().equals("remove")) {
                t.removeColumns(col.getName());
            }

            if (col.getTreatmentType() != null && col.getTreatmentType().equals("split")) {
                List<?> list = t.column(col.getName()).unique().asList().stream().filter(x -> x.toString().length() > 0).collect(Collectors.toList());
                Integer[] newList = new Integer[t.column(col.getName()).size()];
                System.out.println(list);
//            if(dc.getActualType()=="string"){
//                newList = new ArrayList<String>();
//            }else if(dc.getActualType().equals("integer")){
//                 newList = new ArrayList<Integer>();
//            }else if(dc.getActualType().equals("double")){
//                 newList = new ArrayList<Double>();
//            }else if(dc.getActualType().equals("date")){
//                 newList = new ArrayList<Date>();
//            }
//                if (false && list.size() == 2) {
//                    // binary convert to 0's and 1's
//                    for (int i = 0; i < t.rowCount(); i++) {
//                        String v = t.column(col.getName()).get(i).toString();
//                        if (v.equals(list.get(0))) {
//                            newList[i] = 0;
//                        } else {
//                            newList[i] = 1;
//                        }
//
//                    }
//                    t.removeColumns(col.getName());
//                    IntColumn column = IntColumn.create(col.getName(), newList);
//                    t.addColumns(column);
//
//                } else if (true && list.size() > 2) {

                    // for each item create new columns
                    for (int k = 0; k < list.size(); k++) {
                        newList = new Integer[t.column(col.getName()).size()];
                        for (int i = 0; i < t.rowCount(); i++) {
//                            System.out.println(t.columns());
//                            System.out.println(col.getName());
//                            System.out.println(t.column("test_indication"));
                            String v = t.column(col.getName()).get(i).toString();
//                            System.out.println(v);

                            if (v.equals(list.get(k))) {
                                newList[i] = 1;
                            } else {
                                newList[i] = 0;
                            }

                        }
                        IntColumn column = IntColumn.create(col.getName() + "_" + list.get(k), newList);
                        t.addColumns(column);
                    }
                    t.removeColumns(col.getName());
//                }
            }
        }

        String cleansedFileName = "FEATURE_" + experiment.getDatasource().getFilename();
        t.write().csv(UPLOAD_DIR + cleansedFileName);
        DataSource ds = dataSourceRepository.findById(experiment.getDatasource().getId()).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));;
        ds.setFilename(cleansedFileName);
        dataSourceRepository.save(ds);

        return new ResponseEntity<>(HttpStatus.CREATED);

    }

    @PostMapping("/experiment/{id}/modelbuilding/{mltype}")
    public ModelBuildingDTO modelBuilding(@PathVariable("id") long id, @RequestBody ModelBuildingDTO mDTO, @PathVariable("mltype") String mlType) throws IOException, ClassNotFoundException, com.opencsv.exceptions.CsvException {
        System.out.println(mlType);
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        experiment.setType(mlType);
        experimentRepository.save(experiment);
        if (mlType.equals("classify")) {
            return classification.classify(id, mDTO);
        } else if (mlType.equals("regression")) {
            return regression.regression(id, mDTO);
        }

        return mDTO;
    }

    @PostMapping("/experiment/{id}/invokemodel")
    public String invokeModel(@PathVariable("id") long id, @RequestBody String json) throws IOException, ClassNotFoundException, com.opencsv.exceptions.CsvException {
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        List<DataColumn> dcs = experiment.getDatasource().getDataColumn();

        if (experiment.getType().equals("classify")) {
            return classification.invokeModel(id, json);
        } else if (experiment.getType().equals("regression")) {
            return regression.invokeModel(id, json);
        }
        return null;
//        return regression.invokeModel(id, json);
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode node = mapper.readTree(json);
//
//        var labelFactory = new LabelFactory();
//        var csvLoader = new CSVLoader<>(labelFactory);
//        var irisHeaders = new String[dcs.size()];
//        String[] csvData = new String[dcs.size()];
//        String target = "";
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
//
//        FileWriter outputfile = new FileWriter(UPLOAD_DIR + "temprequestfile.csv");
//        CSVWriter writer = new CSVWriter(outputfile);
//
//        writer.writeNext(csvData);
//        writer.close();
//        System.out.println(target);
//        var irisesSource = csvLoader.loadDataSource(Paths.get(UPLOAD_DIR + "temprequestfile.csv"), target, irisHeaders);
////        var irisSplitter = new TrainTestSplitter<>(irisesSource, mDTO.getTrainProportion() / 100, 1l);
////        var trainingDataset = new MutableDataset<>(irisSplitter.getTrain());
//        MutableDataset<Label> testingDataset = new MutableDataset<>(irisesSource);
//        System.out.println(testingDataset.getExample(0));
////
//        String filterPattern = Files.readAllLines(Paths.get(UPLOAD_DIR + "jep-290-filter.txt")).get(0);
//        ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(filterPattern);
//        org.tribuo.Model<Label> loadedModel;
//        try ( ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(UPLOAD_DIR + experiment.getDatasource().getModelFileName())))) {
//            ois.setObjectInputFilter(filter);
//            loadedModel = (org.tribuo.Model<Label>) ois.readObject();
//        }
//        Prediction<Label> prediction = loadedModel.predict(testingDataset.getExample(0));
//        System.out.println(prediction.getOutput());
//        return prediction.getOutput().toString();
//        return "dd";
    }

    @GetMapping("/experiment/{id}/modeltrainingbuilding/{split}")
    public String modelTrainingBuilding(@PathVariable("id") long id) throws IOException {
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        List<DataColumn> dcs = experiment.getDatasource().getDataColumn();
        var labelFactory = new LabelFactory();
        var csvLoader = new CSVLoader<>(labelFactory);
        var irisHeaders = new String[dcs.size()];
        var target = "";
        for (int i = 0; i < dcs.size(); i++) {
            irisHeaders[i] = dcs.get(i).getName();
            if (dcs.get(i).getTreatmentType().equals("target")) {
                target = dcs.get(i).getName();
            }
        }
//        Table t = Table.read().file(UPLOAD_DIR + experiment.getDatasource().getFilename());
        // First remove all columns 

        var irisesSource = csvLoader.loadDataSource(Paths.get(UPLOAD_DIR + experiment.getDatasource().getFilename()), target, irisHeaders);
        var irisSplitter = new TrainTestSplitter<>(irisesSource, 0.7, 1L);
        var trainingDataset = new MutableDataset<>(irisSplitter.getTrain());
        var testingDataset = new MutableDataset<>(irisSplitter.getTest());

        Trainer<Label> trainer = new LogisticRegressionTrainer();
//        System.out.println(trainer.toString());
        org.tribuo.Model<Label> irisModel = trainer.train(trainingDataset);

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
        org.tribuo.Model<Label> linear = linearTrainer.train(trainingDataset);

// Finally we make predictions on unseen data
// Each prediction is a map from the output names (i.e. the labels) to the scores/probabilities
        Prediction<Label> prediction = linear.predict(testingDataset.getExample(0));

        System.out.println("====================================================");
        System.out.println(testingDataset.getExample(0));
        System.out.println(prediction.getOutput());

// Or we can evaluate the full test dataset, calculating the accuracy, F1 etc.
        evaluation = new LabelEvaluator().evaluate(linear, testingDataset);
// we can inspect the evaluation manually
        double acc = evaluation.accuracy();
        return null;
    }
}
