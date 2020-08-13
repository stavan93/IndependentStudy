import de.uni_mannheim.minie.MinIE;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ie.util.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.*;
import edu.stanford.nlp.trees.*;
import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.ChunkedSentenceReader;
import edu.washington.cs.knowitall.util.DefaultObjects;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;


public class Coreference {

    /*public static String text = "Joe Smith was born in California. " +
            "In 2017, he went to Paris, France in the summer. " +
            "His flight left at 3:00pm on July 10th, 2017. " +
            "After eating some escargot for the first time, Joe said, \"That was delicious!\" " +
            "He sent a postcard to his sister Jane Smith. " +
            "After hearing about Joe's trip, Jane decided she might go to France one day.";*/
    public static String text = "Agar dilution is a method used by researchers to determine the resistance of pathogens to antibiotics. It is the dilution method most frequently used to test the effectiveness of new antibiotics.";

    private Coreference() { } // static main

    public static void main(String[] args) throws IOException {
        // set up pipeline properties
        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,kbp,quote");
        // set a property for an annotator, in this case the coref annotator is being set to use the neural algorithm
        props.setProperty("coref.algorithm", "neural");
        // build pipeline*/
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // create a document object
        CoreDocument document = new CoreDocument(text);
        // annnotate the document
        pipeline.annotate(document);

        List<CoreSentence> sent = document.sentences();
        List<String> ll = new ArrayList<>();
        for(int i = 0; i<sent.size(); i++){
            ll.add(sent.get(i).toString());
        }
        for(int i = 0; i<ll.size(); i++){
            System.out.println(ll.get(i));
        }
        System.out.println();

       // get document wide coref info
        Map<Integer, CorefChain> corefChains = document.corefChains();
        //System.out.println("Example: coref chains for document");
        for(Map.Entry<Integer, CorefChain> s : corefChains.entrySet()){
            //System.out.println(s.getValue().getRepresentativeMention().mentionSpan);
            String representative_mention = s.getValue().getRepresentativeMention().mentionSpan;
            for(int i = 0; i<s.getValue().getMentionsInTextualOrder().size(); i++){
                //System.out.println(s.getValue().getMentionsInTextualOrder().get(i).mentionSpan + " " + (s.getValue().getMentionsInTextualOrder().get(i).sentNum-1));
                int num = (s.getValue().getMentionsInTextualOrder().get(i).sentNum-1);
                //System.out.println(num + " " + sent.get(num));
                String textt = ll.get(num).replaceAll(("\\b"+s.getValue().getMentionsInTextualOrder().get(i).mentionSpan+"\\b"),(s.getValue().getRepresentativeMention().mentionSpan));
                //System.out.println(textt);
                ll.set(num,textt);
            }
            System.out.println();
        }
        StringBuilder context = new StringBuilder();
        for(int i = 0; i<ll.size(); i++){
            System.out.println(ll.get(i));
            context.append(ll.get(i)).append(" ");
        }
        //StanfordCoreNLP.clearAnnotatorPool();
        //Runtime.getRuntime().gc();
        System.out.println(context.toString());
        System.out.println("--------------------------------------------------------------------------------------------");
        System.out.println();

        StringReader read = new StringReader(context.toString());
        System.out.println(read);
        MinIE minie = new MinIE();
        SemanticGraph sg;
        ChunkedSentenceReader sentReader = DefaultObjects.getDefaultSentenceReader(read);
        for(ChunkedSentence sentt : sentReader.getSentences()){
            String sentence = sentt.getTokensAsString();
            System.out.println(sentence);
            sg = CoreNLPUtils.parse(CoreNLPUtils.StanfordDepNNParser(), sentence);
            if(sentence == null){
                System.out.println("Sentence null");
                System.exit(1);
            }
            if(sg == null){
                System.out.println("SG null");
                System.exit(1);
            }
            minie.minimize(sentence, sg, MinIE.Mode.SAFE);
            for(int i = 0; i<minie.getPropositions().size(); i++){
                System.out.println("Triples:   " + minie.getSubject(i) + "," + minie.getRelation(i) + "," + minie.getObject(i));
            }
            minie.clear();
        }
    }
}