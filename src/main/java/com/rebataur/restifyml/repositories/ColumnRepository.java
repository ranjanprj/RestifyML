/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.rebataur.restifyml.repositories;

import com.rebataur.restifyml.entities.DataColumn;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface ColumnRepository extends CrudRepository<DataColumn, Long> {
    
    
    
}