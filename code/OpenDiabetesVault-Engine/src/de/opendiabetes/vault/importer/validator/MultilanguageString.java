/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.opendiabetes.vault.importer.validator;

import java.util.HashMap;

/**
 *
 * @author mswin
 */
public class MultilanguageString {
    public enum Language {
        EN, DE;
    }
    
    private final HashMap<Language, String> content = new HashMap<>();
    
    public MultilanguageString(String stringEN, String stringDE){
        content.put(Language.EN, stringEN);
        content.put(Language.DE, stringDE);
    }
    
    public String getStringForLanguage(Language lang){
        String returnValue = content.get(lang);
        if (returnValue == null || returnValue.isEmpty()){
            return "";
        } else {
            return returnValue;
        }
    }
    
}
