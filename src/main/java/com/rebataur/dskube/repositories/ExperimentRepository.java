/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.rebataur.dskube.repositories;

import com.rebataur.dskube.entities.Experiment;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface ExperimentRepository extends CrudRepository<Experiment, Long> {
    
    List<Experiment> findByName(String name);
    
}