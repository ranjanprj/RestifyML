package com.rebataur.dskube.entities;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
public class DataSource implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    @NotBlank(message = "Name is mandatory")
    private String filename;
    private String modelDataFileName,modelFileName;
  
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DataColumn> dataColumn;
  

  

  

  
}