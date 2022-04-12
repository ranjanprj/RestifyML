/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.rebataur.imbuedintelligence;

import java.io.IOException;
import tech.tablesaw.api.ColumnType;
import static tech.tablesaw.api.ColumnType.DOUBLE;
import static tech.tablesaw.api.ColumnType.STRING;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvReader;

public class Test {

    private final static String UPLOAD_DIR = "C:\\3Projects\\imbuedintelligence\\uploads\\";

    public static void main(String[] args) throws IOException {
        Table t = Table.read().csv(UPLOAD_DIR + "corona_ref_data.csv");
        t.columns().forEach((c)->{
            System.out.println(c.isMissing());
        });
        Column<?> cr = t.column("corona_result");
        System.out.println(cr.unique().asList());
        cr = t.column("age_60_and_above");
        System.out.println(cr.unique().asList());
        cr = t.column("gender");
        System.out.println(cr.unique().asList());
        cr = t.column("test_indication");
        System.out.println(cr.unique().asList());
        System.out.println(t.column("gender").isMissing());
        t = t.dropRowsWithMissingValues();
        System.out.println(t.column("gender").isMissing() + " " + t.column("gender").size());

//        for (int i = 0; i < t.rowCount(); i++) {
//            boolean skipRow = false;
//            for (Column c : t.columns()) {
//                if (c.isMissing(i)) {
//                    skipRow = true;
//                }
//            }
//            if (!skipRow) {
//                nt.addRow(i, t);
//                skipRow = false;
//            }
//        }
//        System.out.println(t.column("gender").isMissing());
//        System.out.println(nt.column("gender").isMissing());
    }

}
