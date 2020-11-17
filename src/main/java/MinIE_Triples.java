import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.uni_mannheim.minie.MinIE;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import de.uni_mannheim.minie.annotation.AnnotatedProposition;
import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.ChunkedSentenceReader;
import edu.washington.cs.knowitall.util.DefaultObjects;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

public class MinIE_Triples {
    public void extract() throws FileNotFoundException {
        JSONParser parser = new JSONParser();

        try(Reader reader = new FileReader("data\\BM25RelevantResult\\output_query_json_file_1.json")){
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            props.setProperty("tokenize.options", "splitHyphenated=false,americanize=false");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            JSONArray jsonArray = (JSONArray) parser.parse(reader);
            JSONObject jsonObject = null;
            JSONArray employeeList = new JSONArray();

            int num = 0;

            ChunkedSentenceReader sentReader = null;

            // Initialize the parser (this may take a while)
            StanfordCoreNLP parserr = CoreNLPUtils.StanfordDepNNParser();
            MinIE minie = new MinIE();
            // Parse the sentence with CoreNLP
            SemanticGraph sg;

            for(int k = 0; k < jsonArray.size(); k++){
                Map<String, Integer> map = new HashMap<>();
                JSONArray arr = new JSONArray();
                JSONObject obj1 = new JSONObject();
                jsonObject = (JSONObject) jsonArray.get(k);
                StringReader read = new StringReader((String)jsonObject.get("contexttext"));
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
                sentReader = DefaultObjects.getDefaultSentenceReader(read);

                obj1.put("queryid", qid);
                obj1.put("contextid", cid);
                obj1.put("contexttext", ctext);
                obj1.put("contextrank", crank);
                obj1.put("contextscore", cscore);


                for(ChunkedSentence sent : sentReader.getSentences()){
                    String sentence = sent.getTokensAsString();
                    System.out.println(sentence);

                    sg = CoreNLPUtils.parse(parserr, sentence);
                    minie.minimize(sentence, sg, MinIE.Mode.SAFE);


                    // Generate the extractions (with "safe mode")
                    //MinIE minie = new MinIE(sentence, sg, MinIE.Mode.SAFE);

                    for(int i = 0; i<minie.getPropositions().size(); i++){
                        JSONObject obj2 = new JSONObject();
                        System.out.println("Triples:   " + minie.getSubject(i) + "," + minie.getRelation(i) + "," + minie.getObject(i));
                        String subject = minie.getSubject(i).toString();
                        String relation = minie.getRelation(i).toString();
                        String object = minie.getObject(i).toString();
                        obj2.put("subject", subject);
                        obj2.put("predicate", relation);
                        obj2.put("object", object);
                        arr.add(obj2);

                        String[] argg1 = subject.split(" ");
                        JSONArray arr1 = new JSONArray();
                        for(String s : argg1){
                            JSONObject obj3 = new JSONObject();
                            for(Map.Entry<String, Integer> entry : map.entrySet()){
                                if(s.equals(entry.getKey())){
                                    obj3.put("token", s);
                                    obj3.put("charOffsetBegin", entry.getValue());
                                    obj3.put("charOffsetEnd",
                                            ((entry.getValue() + entry.getKey().length())));
                                    arr1.add(obj3);
                                }
                            }
                        }
                        obj2.put("subjectTokens", arr1);

                        String[] rell = relation.split(" ");
                        JSONArray arr2 = new JSONArray();
                        for(String s : rell){
                            JSONObject obj3 = new JSONObject();
                            for(Map.Entry<String, Integer> entry : map.entrySet()){
                                if(s.equals(entry.getKey())){
                                    obj3.put("token", s);
                                    obj3.put("charOffsetBegin", entry.getValue());
                                    obj3.put("charOffsetEnd",
                                            (entry.getValue() + entry.getKey().length()));
                                    arr2.add(obj3);
                                }
                            }
                        }
                        obj2.put("relationTokens", arr2);

                        String[] argg2 = object.split(" ");
                        JSONArray arr3 = new JSONArray();
                        for(String s : argg2){
                            JSONObject obj3 = new JSONObject();
                            for(Map.Entry<String, Integer> entry : map.entrySet()){
                                if(s.equals(entry.getKey())){
                                    obj3.put("token", s);
                                    obj3.put("charOffsetBegin", entry.getValue());
                                    obj3.put("charOffsetEnd",
                                            (entry.getValue() + entry.getKey().length()));
                                    arr3.add(obj3);
                                }
                            }
                        }
                        obj2.put("objectTokens", arr3);
                    }
                    obj1.put("minietriples", arr);
                    minie.clear();
                }
                employeeList.add(obj1);
            }
            System.out.println(num);

            try (FileWriter file = new FileWriter("data\\MinIETriples\\Minie_Triples_json_file_1.json")) {

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
