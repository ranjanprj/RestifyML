/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.rebataur.restifyml;

import lombok.Data;

@Data
public class ColumnCleansingDTO {
    private Long columnId;
    private String experimentId, actualType,treatmentType;
    
    
}
