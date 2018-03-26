package edu.ualberta.storyteller.core.parameter;

import edu.ualberta.storyteller.core.util.*;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

public class Parameters implements Serializable {

    //! Language.
    public String language;

    //! Data type.
    public String dataType;

    //! TF boost rate for normal words.
	//! NOTICE: currently, if it is not zero, the program will be out of memory!!!
    public double boostRateNormalWord = 0;

    //! TF boost rate for document's main keywords.
    public double boostRateMainKeyword = 3;

    //! TF boost rate for document's normal keywords.
    public double boostRateNormalKeyword = 1;

    //! Minimum cosine similarity between a keyword graph and its related document.
    //! If the similarity is bigger than this threshold, the document will be included
    //! in the topic.
    public double minSimDoc2KeyGraph = .25;

    //! Algorithm for keyword graph community detection.
    public String communityDetectAlg = "Betweenness";

    //! Algorithm for event splitting.
    //! Currently: "DocRelation", "DocGraph", "Rule"
    public String eventSplitAlg = "DocRelation";

    //! Minimum number of related documents for a topic.
    //! Detected topics with a size smaller than it will be filtered.
    public int minTopicSize = 5;

    //! Minimum document frequency of keyword graph nodes.
    //! Nodes in a keyword graph will a smaller DF will be filtered (too rare).
    public int minNodeDF = 4;

	//! Minimum document frequency of keyword graph edges.
	//! Edges in a keyword graph will a smaller DF will be filtered (too rare).
	public int minEdgeDF = 3;

	//! Minimum edge correlation of keyword graph edges.
	//! KeywordEdge (n1, n2) will smaller correlation will be filtered
	//! (which means node n1 n2 may also be connected with a lot of other nodes).
    //! NOTICE: this param influence the final clusters a lot.
	public double minEdgeCorrelation = .15;

    //! Maximum document frequency percentage of keyword graph nodes.
    //! Nodes appear in over than this percentage documents will be filtered (too normal).
    public double maxNodeDFPercent = .3;

    //! Minimum number of nodes for a cluster (a keyword graph that represents a topic).
    //! Clusters with a smaller size will be filtered.
    public int minClusterNodeSize = 3;

    //! Maximum number of nodes for a cluster (a keyword graph that represents a topic).
    //! Clusters with a bigger size will continue be split by community detection algorithm.
    public int maxClusterNodeSize = 800;

    //! Minimum number of keywords for a document.
    //! Documents that contains less keywords will be filtered.
    public int minDocKeywordSize = 5;

    //! Merge two document clusters if their intersect proportion is bigger than a threshold
    public double minIntersectPercentToMergeCluster = .3;

    //! Minimum conditional probability to duplicate an edge rather than delete it from keyword graph.
    //! Duplicate the edge if the conditional probability is higher than this threshold.
    public double minCpToDuplicateEdge = 1.7;

    //! Whether use document's topic to split a document cluster into sub clusters.
    public boolean useDocumentTopic = false;

    //! Whether use document's title common word's count to split a document cluster into sub clusters.
    public boolean useDocumentTitleCommonWords = false;

    //! Minimum number of common words in title to taken two documents into one group.
    public int minTitleCommonWordsSize = 2;

    //! Minimum percentage of common words in title to taken two documents into one group.
    public double minTitleCommonWordsPercent = .4;

    //! The file that contains stop words. Each line is a word.
    public String fStopwords;
    public HashSet<String> stopwords;

    //! The file that contains SVM model to determine whether two documents are of same event.
    public String fModel;
    public libsvm.svm_model model;

    public String fSameStoryModel;
    public libsvm.svm_model sameStoryModel;

    //! Minimum key graph compatibility for matching a document cluster to an existing story tree.
    public double minKeygraphCompatibilityDc2St = .6;

    //! Minimum compatibility for matching a document cluster to an existing story tree's node.
    public double minCompatibilityDc2Sn = .3;

    //! Minimum tf cosine similarity for matching a document cluster to an existing story tree's node.
    public double minTFCosineSimilarityDc2Sn = .02;

    //! Used to control the influence of timestamp difference between new document and old document.
    //! Assume if time gap is bigger, the possibility that two documents are connected will decay by exponential func.
    //! If this param is bigger, the decay will be faster; otherwise, the decay is smoother.
    //! Calculate time proximity.
    public double deltaTimeGap = .5;

    //! Used to control the influence of document distribution.
    //! Calculate document distributional proximity.
    public double deltaDocDistribution = .5;

    //! How many day's data to keep.
    public int historyLength = 3;

    //! Whether use extra title for topic match.
    public boolean useRelatedNewsTitlesForMatch = false;

    //! The file that contains SVM model to determine whether a document matches a query.
    public String fQueryDocMatchModel;
    public libsvm.svm_model queryDocMatchModel;

    //! Maximum number of docs to match for a topic.
    public int maxMatchedDocsSize = 20;

    /**
     * Parametric constructor.
     * Create Parameters instance from file.
     * @param paramsFile File name of parameters' file.
     * @throws Exception
     */
	public Parameters(String paramsFile) throws Exception {
		load(new DataInputStream(new FileInputStream(paramsFile)));
	}

    /**
     * Load parameters from parameter file.
     * @param in Parameter file input stream.
     * @throws Exception
     */
	public void load(DataInputStream in) throws Exception {
        // create variable to save parameters
		HashMap<String, String> conf = new HashMap<String, String>();

        // read parameter file and parse each line.
        // lines started with "//" are considered to be comments
		String line = null;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			if (line.startsWith("//") || line.length() == 0) {
                continue;
            }

			StringTokenizer st = new StringTokenizer(line, "= ;");
			conf.put(st.nextToken(), st.nextToken());
		}

        // parameters for experimental setup
        language = conf.get("language");
        dataType = conf.get("dataType");

        // parameters to boost word tf
        boostRateNormalWord = Double.parseDouble(conf.get("boostRateNormalWord"));
        boostRateMainKeyword = Double.parseDouble(conf.get("boostRateMainKeyword"));
        boostRateNormalKeyword = Double.parseDouble(conf.get("boostRateNormalKeyword"));

        // parameters to filter documents
        minDocKeywordSize = Integer.parseInt(conf.get("minDocKeywordSize"));

        // parameters to filter keyword graph nodes
        minNodeDF = Integer.parseInt(conf.get("minNodeDF"));
		maxNodeDFPercent = Double.parseDouble(conf.get("maxNodeDFPercent"));

        // parameters to filter keyword graph edges
		minEdgeDF = Integer.parseInt(conf.get("minEdgeDF"));
        minEdgeCorrelation = Double.parseDouble(conf.get("minEdgeCorrelation"));

        // parameters to detect keyword graph communities
        communityDetectAlg = conf.get("communityDetectAlg");

        // parameters to split or filter keyword graphs
		maxClusterNodeSize = Integer.parseInt(conf.get("maxClusterNodeSize"));
		minClusterNodeSize = Integer.parseInt(conf.get("minClusterNodeSize"));
		minIntersectPercentToMergeCluster = Double.parseDouble(conf.get("minIntersectPercentToMergeCluster"));
        minCpToDuplicateEdge = Double.parseDouble(conf.get("minCpToDuplicateEdge"));

        // parameters to assign document to keyword graphs
        minSimDoc2KeyGraph = Double.parseDouble(conf.get("minSimDoc2KeyGraph"));

        // parameters to filter document clusters
        minTopicSize = Integer.parseInt(conf.get("minTopicSize"));

        // parameters to processing document clusters
        useDocumentTopic = Boolean.parseBoolean(conf.get("useDocumentTopic"));
        useDocumentTitleCommonWords = Boolean.parseBoolean(conf.get("useDocumentTitleCommonWords"));
        minTitleCommonWordsSize = Integer.parseInt(conf.get("minTitleCommonWordsSize"));
        minTitleCommonWordsPercent = Double.parseDouble(conf.get("minTitleCommonWordsPercent"));
        fStopwords = conf.get("fStopwords");
        stopwords = NlpUtils.importStopwords(fStopwords, language);
        eventSplitAlg = conf.get("eventSplitAlg");

        // parameters to merge new documents wit stories
        minKeygraphCompatibilityDc2St = Double.parseDouble(conf.get("minKeygraphCompatibilityDc2St"));
        minCompatibilityDc2Sn = Double.parseDouble(conf.get("minCompatibilityDc2Sn"));
        minTFCosineSimilarityDc2Sn = Double.parseDouble(conf.get("minTFCosineSimilarityDc2Sn"));
        deltaTimeGap = Double.parseDouble(conf.get("deltaTimeGap"));
        deltaDocDistribution = Double.parseDouble(conf.get("deltaDocDistribution"));

        // parameters for filter corpora
        historyLength = Integer.parseInt(conf.get("historyLength"));

        // parameters related to event classification supervised learning
        fModel = conf.get("fModel");
        model = libsvm.svm.svm_load_model(fModel);
        fSameStoryModel = conf.get("fSameStoryModel");
        sameStoryModel = libsvm.svm.svm_load_model(fSameStoryModel);

        // parameters for query doc matching
        useRelatedNewsTitlesForMatch = Boolean.parseBoolean(conf.get("useRelatedNewsTitlesForMatch"));
        fQueryDocMatchModel = conf.get("fQueryDocMatchModel");
        queryDocMatchModel = libsvm.svm.svm_load_model(fQueryDocMatchModel);
        maxMatchedDocsSize = Integer.parseInt(conf.get("maxMatchedDocsSize"));

    }

}
