/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anna
 */
public class Parser {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        String[] filenames = {"devicestatus_2017-07-10_to_2017-11-08.json.gz",
            "entries_2017-07-10_to_2017-11-08.json.gz",
            "profile_2017-07-10_to_2017-11-08.json.gz",
            "treatments_2017-07-10_to_2017-11-08.json.gz"};
        File baseDir = new File("/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31");
        File currentFile;

        String charset = "UTF-8"; // Should be verified. Might vary in Nightscout data.
//        String[] fileContent = new String[4];
        //int cntLine;
        //String encoding;
        Writer writer;
        String content;
        for (int i = 2; i < 3; i++) {
            InputStream fileStream = null;
            try {
                currentFile = new File(baseDir, filenames[i]);
                fileStream = new FileInputStream(currentFile);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                Reader reader = new InputStreamReader(gzipStream, charset);
                writer = new StringWriter();
                char[] buffer = new char[10240];
                //cntLine = 0;
                for (int length = 0; (length = reader.read(buffer)) > 0;) {
                    //System.out.println(content);
                    //cntLine++;
                    writer.write(buffer, 0, length);
                }
                
                content = writer.toString();
                //System.out.println(content);
                Gson gson = new Gson();
                JsonObject o;
                JsonElement[] elements = gson.fromJson(content, JsonElement[].class);
                for (int j = 0; j < elements.length; j++){
                    o = elements[j].getAsJsonObject();
                    if(j == 0){
                        System.out.println(o);
                    }
                }
                //System.out.println(element.getAsJsonArray());
//                fileContent[i] = content;
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fileStream.close();
                } catch (IOException ex) {
                    Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Deserializer for JSON data.
     *
     * @param element The JSON element to deserialize.
     * @return De-serialized JSON element.
     * @throws JsonParseException Thrown if JSON element is faulty.
     */
    public void deserialize(final JsonElement element)
            throws JsonParseException {
        JsonObject obj = element.getAsJsonObject();
        //obj.get("tp").getAsInt()
        //new Date(obj.get("ts").getAsLong())
        //obj.get("v1").getAsDouble()
        //obj.get("v1").getAsDouble();

        //return entry;
    }

    public void readDeviceStatus() {

    }

    public void readEntries() {

    }

    public void readProfile() {

    }

    public void readTreatments() {

    }
}
