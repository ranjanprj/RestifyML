package com.rebataur.restifyml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import smile.regression.OLS;

import tech.tablesaw.api.Table;

public class Testing {

    public static void main(String[] args) throws IOException {
        print("==============================");
        Table table = Table.read().csv("C:\\3Projects\\RestifyML\\uploads\\corona_ref_data.csv");
        print(table.column("gender").unique().asList().stream().filter(x -> x.toString().length() > 0).collect(Collectors.toList()));
        List<?> list = table.column("gender").unique().asList().stream().filter(x -> x.toString().length() > 0).collect(Collectors.toList());
         
        List<?> newList;
      
        print("==============================");
    }

    public static void print(Object obj) {
        System.out.println(obj);
    }
}
