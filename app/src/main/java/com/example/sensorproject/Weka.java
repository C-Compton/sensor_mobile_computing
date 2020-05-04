package com.example.sensorproject;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

import static weka.core.SerializationHelper.read;

public class Weka {

    public Pair<Double, Double> classification( double min, double max, double var, double std) {
        double [] predicted_class = new double[2];
        predicted_class[0] = 0.0;
        predicted_class[1] = 0.0;

        try {
            Classifier cls;
            cls = (Classifier) read(App.getContext().getAssets().open("weka_DT_model2_0_0_1_1.model"));

            ArrayList<Attribute> attributes = new ArrayList<>();

            attributes.add(new Attribute("min", 0));
            attributes.add(new Attribute("max", 1));
            attributes.add(new Attribute("var", 2));
            attributes.add(new Attribute("std", 3));

            attributes.add(new Attribute("label", HydrationLevel.getLabels()));

            Instance instance = new SparseInstance( 4);
            instance.setValue(attributes.get(0), min);
            instance.setValue(attributes.get(1), max);
            instance.setValue(attributes.get(2), var);
            instance.setValue(attributes.get(3), std);

            Instances dataSetConfiguration;

            dataSetConfiguration = new Instances("level.hydration", attributes, 0);

            dataSetConfiguration.setClassIndex( 4 );
            instance.setDataset(dataSetConfiguration);

            double[] distribution;
            distribution = cls.distributionForInstance( instance );
            predicted_class[0] = cls.classifyInstance( instance );
            predicted_class[1] = Math.max(distribution[0], distribution[1]) * 100;

            Log.d("WEKA", "Predicted class: "
                          + HydrationLevel.convert( (int)predicted_class[0] ).name()
                          + " Confidence score: " + predicted_class[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Pair<>(predicted_class[0], predicted_class[1]);
    }
}
