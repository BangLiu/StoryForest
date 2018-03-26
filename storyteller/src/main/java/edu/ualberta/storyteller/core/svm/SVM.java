package edu.ualberta.storyteller.core.svm;

import libsvm.*;
import java.util.StringTokenizer;

/**
 * This class contains some methods for using SVM classifier.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1220
 */
public class SVM {

    public static void train(String[] arg) throws Exception  {
        svm_train.main(arg);
    }

    public static void predict(String[] parg) throws Exception  {
        svm_predict.main(parg);
    }

    public static double predict_x(svm_model model, String input) throws Exception {
        // 测试单个sample
        int svm_type = svm.svm_get_svm_type(model);
        double[] prob_estimates = null;

        StringTokenizer st = new StringTokenizer(input," \t\n\r\f:");

        double target = Double.valueOf(st.nextToken()).doubleValue();
        int m = st.countTokens()/2;
        libsvm.svm_node[] x = new libsvm.svm_node[m];
        for(int j=0;j<m;j++)
        {
            x[j] = new libsvm.svm_node();
            x[j].index = Integer.parseInt(st.nextToken());
            x[j].value = Double.valueOf(st.nextToken()).doubleValue();
        }

        double v;
        if (model.param.probability==1 && (svm_type==svm_parameter.C_SVC || svm_type==model.param.NU_SVC))
        {
            v = svm.svm_predict_probability(model,x,prob_estimates);
        }
        else {
            v = svm.svm_predict(model, x);
        }

        return v;
    }

}
