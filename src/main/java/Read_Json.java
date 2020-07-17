import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.washington.cs.knowitall.extractor.ReVerbExtractor;
import edu.washington.cs.knowitall.extractor.conf.ConfidenceFunction;
import edu.washington.cs.knowitall.extractor.conf.ReVerbOpenNlpConfFunction;
import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.ChunkedSentenceReader;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;
import edu.washington.cs.knowitall.util.DefaultObjects;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.uni_mannheim.minie.MinIE;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Read_Json {

    public static void main(String[] args) throws IOException, ParseException {

        JSONParser parser = new JSONParser();

        try (Reader reader = new FileReader("data\\BM25RelevantResult\\output_query_json_file_5.json")) {

            JSONArray jsonArray = (JSONArray) parser.parse(reader);
            JSONObject jsonObject = null;

            JSONArray employeeList = new JSONArray();
            int num = 0;
            System.err.print("Initializing extractor...");
            ReVerbExtractor extractor = new ReVerbExtractor();
            System.err.println("Done.");

            System.err.print("Initializing confidence function...");
            ConfidenceFunction scoreFunc = new ReVerbOpenNlpConfFunction();
            System.err.println("Done.");

            System.err.print("Initializing NLP tools...");
            ChunkedSentenceReader sentReader = null;
            System.err.println("Done.");
            for(int i = 0; i < jsonArray.size(); i++){
                JSONArray arr = new JSONArray();
                JSONObject obj1 = new JSONObject();

                JSONObject obj3 = new JSONObject();
                jsonObject = (JSONObject) jsonArray.get(i);
                StringReader read = new StringReader((String)jsonObject.get("contexttext"));
                String qid = (String)jsonObject.get("queryid");
                String cid = (String)jsonObject.get("contextid");
                String ctext = (String)jsonObject.get("contexttext");
                num++;
                //ReVerbExtractor extractor = new ReVerbExtractor();
                System.out.println("Paragraph: " + (i+1));
                //System.out.println(extractor.extractFromString((String)jsonObject.get("contexttext")));

                sentReader = DefaultObjects.getDefaultSentenceReader(read);
                Joiner joiner = Joiner.on("\t");
                obj1.put("queryid", qid);
                obj1.put("contextid", cid);
                obj1.put("contexttext", ctext);

                for (ChunkedSentence sent : sentReader.getSentences()) {



                    String sentString = sent.getTokensAsString();
                    System.out.println(sentString);

                    for (ChunkedBinaryExtraction extr : extractor.extract(sent)) {

                        double score = scoreFunc.getConf(extr);
                        JSONObject obj2 = new JSONObject();

                        String arg1 = extr.getArgument1().toString();
                        String rel = extr.getRelation().toString();
                        String arg2 = extr.getArgument2().toString();

                        String extrString = "(" + arg1 + ", " + rel + ", " + arg2 + ")";

                        System.out.println(extrString  + " Score: " + score);
                        //arr.add(sentString);
                        obj2.put("subject", arg1);
                        obj2.put("predicate", rel);
                        obj2.put("object", arg2);
                        obj2.put("confidencescore", score);
                        arr.add(obj2);

                    }
                    obj1.put("reverbtriples", arr);

                }
                employeeList.add(obj1);
            }

            System.out.println(num);



            try (FileWriter file = new FileWriter("data\\Reverb_Triples_json_file_5.json")) {

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je = jp.parse(employeeList.toJSONString());
                file.write(gson.toJson(je));
                file.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }




    }

}
