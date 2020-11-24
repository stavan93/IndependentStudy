import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
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
import java.util.*;

public class Read_Json {

    public static void main(String[] args) throws IOException, ParseException {

        JSONParser parser = new JSONParser();

        try (Reader reader = new FileReader("data\\BM25RelevantResult\\output_query_json_file_1.json")) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            props.setProperty("tokenize.options", "splitHyphenated=false,americanize=false");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

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
                Map<String, Integer> map = new HashMap<>();
                jsonObject = (JSONObject) jsonArray.get(i);
                JSONArray arr = new JSONArray();
                JSONObject obj1 = new JSONObject();


                String qid = (String)jsonObject.get("queryid");
                String cid = (String)jsonObject.get("contextid");
                String ctext = (String)jsonObject.get("contexttext");
                String crank = (String)jsonObject.get("contextrank");
                String cscore = (String)jsonObject.get("contextscore");
                num++;
                CoreDocument doc = new CoreDocument(ctext);
                pipeline.annotate(doc);
                for (CoreLabel tok : doc.tokens()) {
                    map.put(tok.word(), tok.beginPosition());
                }
                //ReVerbExtractor extractor = new ReVerbExtractor();
                System.out.println("Paragraph: " + (i+1));
                //System.out.println(extractor.extractFromString((String)jsonObject.get("contexttext")));
                StringReader read = new StringReader((String)jsonObject.get("contexttext"));
                sentReader = DefaultObjects.getDefaultSentenceReader(read);
                Joiner joiner = Joiner.on("\t");
                obj1.put("queryid", qid);
                obj1.put("contextid", cid);
                obj1.put("contexttext", ctext);
                obj1.put("contextrank", crank);
                obj1.put("contextscore", cscore);


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
                        obj2.put("relation", rel);
                        obj2.put("object", arg2);
                        obj2.put("confidencescore", score);
                        arr.add(obj2);

                        String[] argg1 = arg1.split(" ");
                        JSONArray arr1 = new JSONArray();
                        for(String s : argg1){
                            JSONObject obj3 = new JSONObject();
                            for(Map.Entry<String, Integer> entry : map.entrySet()){
                                if(s.equals(entry.getKey())){
                                    obj3.put("token", s);
                                    obj3.put("charOffsetBegin", Integer.toString(entry.getValue()));
                                    obj3.put("charOffsetEnd",
                                            (Integer.toString(entry.getValue() + entry.getKey().length())));
                                    arr1.add(obj3);
                                }
                            }
                        }
                        obj2.put("subjectTokens", arr1);

                        String[] rell = rel.split(" ");
                        JSONArray arr2 = new JSONArray();
                        for(String s : rell){
                            JSONObject obj3 = new JSONObject();
                            for(Map.Entry<String, Integer> entry : map.entrySet()){
                                if(s.equals(entry.getKey())){
                                    obj3.put("token", s);
                                    obj3.put("charOffsetBegin", Integer.toString(entry.getValue()));
                                    obj3.put("charOffsetEnd",
                                            Integer.toString(entry.getValue() + entry.getKey().length()));
                                    arr2.add(obj3);
                                }
                            }
                        }
                        obj2.put("relationTokens", arr2);

                        String[] argg2 = arg2.split(" ");
                        JSONArray arr3 = new JSONArray();
                        for(String s : argg2){
                            JSONObject obj3 = new JSONObject();
                            for(Map.Entry<String, Integer> entry : map.entrySet()){
                                if(s.equals(entry.getKey())){
                                    obj3.put("token", s);
                                    obj3.put("charOffsetBegin", Integer.toString(entry.getValue()));
                                    obj3.put("charOffsetEnd",
                                            Integer.toString(entry.getValue() + entry.getKey().length()));
                                    arr3.add(obj3);
                                }
                            }
                        }
                        obj2.put("objectTokens", arr3);
                    }
                    obj1.put("relationTriples", arr);

                }
                employeeList.add(obj1);
            }

            System.out.println(num);



            try (FileWriter file = new FileWriter("data\\ReverbTriples\\Reverb_Triples_json_file_1.json")) {

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
