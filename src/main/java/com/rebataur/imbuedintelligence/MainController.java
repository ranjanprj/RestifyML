package com.rebataur.imbuedintelligence;

import com.rebataur.imbuedintelligence.entities.DataColumn;
import com.rebataur.imbuedintelligence.entities.DataSource;
import com.rebataur.imbuedintelligence.entities.Experiment;
import com.rebataur.imbuedintelligence.repositories.ColumnRepository;
import com.rebataur.imbuedintelligence.repositories.DataSourceRepository;
import com.rebataur.imbuedintelligence.repositories.ExperimentRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvReader;

@Controller
public class MainController {

    @Autowired
    private ExperimentRepository experimentRepository;
    @Autowired
    private DataSourceRepository dataSourceRepository;
    @Autowired
    private ColumnRepository columnRepository;

    // relative path
    private final String UPLOAD_DIR = "..\\imbuedintelligence_datastore\\uploads\\";

//    @Autowired
//    public MainController(ExperimentRepository experimentRepository, DataSourceRepository dataSourceRepository, ColumnRepository columnRepository) {
//        this.experimentRepository = experimentRepository;
//        this.dataSourceRepository = dataSourceRepository;
//        this.columnRepository = columnRepository;
//    }
    @GetMapping("/")
    public String index(Model model) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        model.addAttribute("name", "ranjan");
        var experiments = experimentRepository.findAll();
        model.addAttribute("experiments", experiments);

//                 Testing.main(new String[]{});
//                 TribuoTest.main(new String[]{});
        return "index";
    }

    @GetMapping("/newexperiment")
    public String newexperiment(Model model) throws IOException {
        model.addAttribute("name", "ranjan");
        model.addAttribute("experiment", new Experiment());
        // test

        return "newexperiment";
    }

    @PostMapping("/createnewexperiment")
    public String createNewExperiment(@Valid Experiment experiment, BindingResult result, Model model) throws IOException {
        System.out.println(experiment);
        experimentRepository.save(experiment);
        return "redirect:/";
    }

    @GetMapping("/experiments")
    public String experiments(Model model) throws IOException {
        model.addAttribute("name", "ranjan");

        return "experiments";
    }

    @GetMapping("/experiment/{id}")
    public String experimentDetails(@PathVariable("id") long id, Model model) {
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        model.addAttribute("experiment", experiment);
//        
        return "experiment";
    }

    @GetMapping("/experiment/{id}/datasource")
    public String experimentDataSource(@PathVariable("id") long id, Model model) {
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        model.addAttribute("experiment", experiment);
        return "datascience/datasource";
    }

    @GetMapping("/experiment/{id}/datacleansing")
    public String experimentDataCleansing(@PathVariable("id") long id, Model model) throws IOException {
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        DataSource ds = experiment.getDatasource();

        model.addAttribute("experiment", experiment);

//         delete all teh columns
//       experiment.setDatasource(null);
//       experimentRepository.save(experiment);
        if (ds != null && ds.getDataColumn() != null) {
            List<DataColumn> dc = ds.getDataColumn();
            ds.getDataColumn().removeAll(dc);
            dataSourceRepository.save(ds);
            columnRepository.deleteAll(dc);
        }

        String fileName = experiment.getDatasource().getFilename();

        if (ds.getDataColumn() != null && ds.getDataColumn().size() == 0) {

            CsvReadOptions.Builder builder
                    = CsvReadOptions.builder(UPLOAD_DIR + fileName)
                            .separator(',') // table is tab-delimited
                            .header(true);									// no header
            CsvReadOptions options = builder.build();
            String[][] column = new MyCsvReader().getColumnNameAndTypes(options);

            for (int i = 0; i < column.length; i++) {
                DataColumn col = new DataColumn();
                col.setName(column[i][2]);
                col.setType(column[i][1]);
                columnRepository.save(col);
                ds.getDataColumn().add(col);
            }

//            Table t = Table.read().file(UPLOAD_DIR + fileName);
//            t.columns().forEach(c -> {
//
//                DataColumn col = new DataColumn();
//                col.setName(c.name());
//                col.setType(c.type().getPrinterFriendlyName());
//                columnRepository.save(col);
//                ds.getDataColumn().add(col);
//
//            });
        }
        dataSourceRepository.save(ds);
        model.addAttribute("columns", ds.getDataColumn());
        model.addAttribute("tempColumn", new DataColumn());
        return "datascience/datacleansing";
    }

    @GetMapping("/experiment/{id}/dataexploration")
    public String experimentDataExploration(@PathVariable("id") long id, Model model) throws IOException {
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        model.addAttribute("experiment", experiment);
        model.addAttribute("columns", experiment.getDatasource().getDataColumn());
        return "datascience/dataexploration";
    }
    
    
    @GetMapping("/experiment/{id}/featurebuilding")
    public String featureBuilding(@PathVariable("id") long id, Model model) throws IOException {
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        DataSource ds = experiment.getDatasource();

        model.addAttribute("experiment", experiment);

//         delete all teh columns
//       experiment.setDatasource(null);
//       experimentRepository.save(experiment);
        if (ds != null && ds.getDataColumn() != null) {
            List<DataColumn> dc = ds.getDataColumn();
            ds.getDataColumn().removeAll(dc);
            dataSourceRepository.save(ds);
            columnRepository.deleteAll(dc);
        }

        String fileName = experiment.getDatasource().getFilename();

        if (ds.getDataColumn() != null && ds.getDataColumn().size() == 0) {

            CsvReadOptions.Builder builder
                    = CsvReadOptions.builder(UPLOAD_DIR + fileName)
                            .separator(',') // table is tab-delimited
                            .header(true);									// no header
            CsvReadOptions options = builder.build();
            String[][] column = new MyCsvReader().getColumnNameAndTypes(options);

            for (int i = 0; i < column.length; i++) {
                DataColumn col = new DataColumn();
                col.setName(column[i][2]);
                col.setType(column[i][1]);
                columnRepository.save(col);
                ds.getDataColumn().add(col);
            }

//            Table t = Table.read().file(UPLOAD_DIR + fileName);
//            t.columns().forEach(c -> {
//
//                DataColumn col = new DataColumn();
//                col.setName(c.name());
//                col.setType(c.type().getPrinterFriendlyName());
//                columnRepository.save(col);
//                ds.getDataColumn().add(col);
//
//            });
        }
        dataSourceRepository.save(ds);
        model.addAttribute("columns", ds.getDataColumn());
        model.addAttribute("tempColumn", new DataColumn());
        return "datascience/featurebuilding";
    }


    
    @GetMapping("/experiment/{id}/modelbuilding")
    public String modelBuilding(@PathVariable("id") long id, Model model) throws IOException {
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));

        model.addAttribute("experiment", experiment);
        model.addAttribute("columns", experiment.getDatasource().getDataColumn());
        return "datascience/modelbuilding";
    }

      @GetMapping("/experiment/{id}/modelbuilding/{algo}")
    public String modelBuildingAlgo(@PathVariable("id") long id, @PathVariable("algo") String algo,Model model) throws IOException {
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        DataSource ds = experiment.getDatasource();
        model.addAttribute("columns", experiment.getDatasource().getDataColumn());
        model.addAttribute("experiment", experiment);

        return "datascience/types/" + algo;
    }

    
    
    @PostMapping("/upload/experiment/{id}")
    public String uploadFile(@PathVariable("id") long id, Model model, @RequestParam("file") MultipartFile file, RedirectAttributes attributes) {
        // check if file is empty
        if (file.isEmpty()) {
            attributes.addFlashAttribute("message", "Please select a file to upload.");
            return "redirect:/";
        }

        // normalize the file path
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
        if (experiment.getDatasource() == null) {
            DataSource ds = new DataSource();
            ds.setFilename(fileName);
            dataSourceRepository.save(ds);
            experiment.setDatasource(ds);
            experimentRepository.save(experiment);
        } else {
            DataSource ds = dataSourceRepository.findById(experiment.getDatasource().getId()).orElseThrow(() -> new IllegalArgumentException("Invalid experiment Id:" + id));
            ds.setFilename(fileName);
            dataSourceRepository.save(ds);
        }

        // save the file on the local file system
        try {
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // return success response
        attributes.addFlashAttribute("message", "You successfully uploaded " + fileName + '!');

        return "redirect:/experiment/" + id + "/datasource";
    }
}
