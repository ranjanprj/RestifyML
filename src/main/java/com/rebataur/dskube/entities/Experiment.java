package com.rebataur.dskube.entities;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
public class Experiment implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @NotBlank(message = "Name is mandatory")
    private String name,outcome;
 
    @OneToOne
    private DataSource datasource;
    @OneToOne
    private DataColumn dataColumn;
    
    private String modelAlgo ;
    private int seed;
    private double trainProportion;
 
}