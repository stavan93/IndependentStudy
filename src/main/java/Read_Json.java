import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.stanford.nlp.coref.data.CorefChain;
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

        try (Reader reader = new FileReader("data/BM25RelevantResult/output_query_json_file_4.json")) {
            Properties props = new Properties();
            // set the list of annotators to run
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,kbp,quote");
            // set a property for an annotator, in this case the coref annotator is being set to use the neural algorithm
            props.setProperty("coref.algorithm", "neural");
            // build pipeline*/
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
                ctext = ctext.replaceAll("[\\(]", "").replaceAll("[\\)]", "").replaceAll("[\\\\]", "");
                CoreDocument document = new CoreDocument(ctext);
                // annnotate the document
                pipeline.annotate(document);
                List<CoreSentence> sentt = document.sentences();
                List<String> ll = new ArrayList<>();
                for(int ii = 0; ii<sentt.size(); ii++){
                    ll.add(sentt.get(ii).toString());
                }
                Map<Integer, CorefChain> corefChains = document.corefChains();
                //System.out.println("Example: coref chains for document");
                for(Map.Entry<Integer, CorefChain> s : corefChains.entrySet()){
                    //System.out.println(s.getValue().getRepresentativeMention().mentionSpan);
                    String representative_mention = s.getValue().getRepresentativeMention().mentionSpan;
                    for(int k = 0; k<s.getValue().getMentionsInTextualOrder().size(); k++){
                        //System.out.println(s.getValue().getMentionsInTextualOrder().get(i).mentionSpan + " " + (s.getValue().getMentionsInTextualOrder().get(i).sentNum-1));
                        int numm = (s.getValue().getMentionsInTextualOrder().get(k).sentNum-1);
                        //System.out.println(num + " " + sent.get(num));
                        String textt = ll.get(numm).replaceAll(("\\b"+s.getValue().getMentionsInTextualOrder().get(k).mentionSpan+"\\b"),(s.getValue().getRepresentativeMention().mentionSpan));
                        //System.out.println(textt);
                        ll.set(numm,textt);
                    }
                    System.out.println();
                }
                StringBuilder context = new StringBuilder();
                for(int x = 0; x<ll.size(); x++){
                    //System.out.println(ll.get(x));
                    context.append(ll.get(x)).append(" ");
                }
                //ReVerbExtractor extractor = new ReVerbExtractor();
                System.out.println("Paragraph: " + (i+1));
                //System.out.println(extractor.extractFromString((String)jsonObject.get("contexttext")));
                StringReader read = new StringReader(context.toString());
                sentReader = DefaultObjects.getDefaultSentenceReader(read);
                Joiner joiner = Joiner.on("\t");
                obj1.put("queryid", qid);
                obj1.put("contextid", cid);
                obj1.put("contexttext", ctext);
                obj1.put("contextrank", crank);
                obj1.put("contextscore", cscore);

                Offset off = new Offset();
                map = off.get_offset(ctext);

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

                        String[] argg1 = arg1.split(" ");
                        JSONArray arr1 = new JSONArray();
                        for(String s : argg1){
                            JSONObject obj3 = new JSONObject();
                            for(Map.Entry<String, Integer> entry : map.entrySet()){
                                if(s.equals(entry.getKey())){
                                    obj3.put("token", s);
                                    obj3.put("charOffsetBegin", entry.getValue());
                                    obj3.put("charOffsetEnd",
                                            ((int)(entry.getKey().length())+entry.getValue()-1));
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
                                    obj3.put("charOffsetBegin", entry.getValue());
                                    obj3.put("charOffsetEnd",
                                            ((int)(entry.getKey().length())+entry.getValue()-1));
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
                                    obj3.put("charOffsetBegin", entry.getValue());
                                    obj3.put("charOffsetEnd",
                                            ((int)(entry.getKey().length())+entry.getValue()-1));
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



            try (FileWriter file = new FileWriter("data/ReverbTriples/Reverb_Triples_json_file_4.json")) {

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
