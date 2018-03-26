package edu.ualberta.storyteller.keywordlayer;

import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.dataloader.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by bangliu on 2017-12-23.
 */
public class KeywordLayer {

    /**
     * OShow sample usage of keyword layer.
     * @param args Program arguments.
     * @throws Exception
     */
    public static void main(String args[]) throws Exception{
        // load params
        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));
        String fNews = "../test_data/2017-08-17.txt";
        String fParameters = "conf/ChineseNewsParameters.txt";

        // initialization
        Parameters parameters = new Parameters(fParameters);
        DataLoader loader = new DataLoader(parameters);

        // load corpus
        Corpus corpus = loader.loadCorpus(fNews);

        // filter corpus by 1st topic
        HashSet<String> filterTopics = new HashSet<>();
        filterTopics.add("141");
        filterTopics.add("142");
        filterTopics.add("116");
        filterTopics.add("117");
        filterTopics.add("124");
        ArrayList<String> toRemove = new ArrayList<>();
        for (String key: corpus.docs.keySet()) {
            Document d = corpus.docs.get(key);
            if (filterTopics.contains(d.topic)) {
                toRemove.add(key);
            }
        }
        for (String key: toRemove) {
            corpus.docs.remove(key);
        }
        System.out.println("Corpus size is " + corpus.docs.size() + " after filter by topics.");
    }

}
