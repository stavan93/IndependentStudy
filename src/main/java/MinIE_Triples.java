import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.uni_mannheim.minie.MinIE;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;
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
import java.util.Arrays;
import java.util.List;

public class MinIE_Triples {
    public void extract() throws FileNotFoundException {
        JSONParser parser = new JSONParser();

        try(Reader reader = new FileReader("data/BM25RelevantResult/output_query_json_file_1.json")){
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
                JSONArray arr = new JSONArray();
                JSONObject obj1 = new JSONObject();
                jsonObject = (JSONObject) jsonArray.get(k);
                StringReader read = new StringReader((String)jsonObject.get("contexttext"));
                String qid = (String)jsonObject.get("queryid");
                String cid = (String)jsonObject.get("contextid");
                String ctext = (String)jsonObject.get("contexttext");
                num++;

                sentReader = DefaultObjects.getDefaultSentenceReader(read);

                obj1.put("queryid", qid);
                obj1.put("contextid", cid);
                obj1.put("contexttext", ctext);


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
                    }
                    obj1.put("minietriples", arr);
                    minie.clear();
                }
                employeeList.add(obj1);
            }
            System.out.println(num);

            try (FileWriter file = new FileWriter("data/MinIETriples/Minie_Triples_json_file_1.json")) {

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
