package com.example.sensorproject;

import android.util.Log;

import java.util.ArrayList;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

import static weka.core.SerializationHelper.read;

public class Weka {

    public HydrationLevel classification( double min, double max, double var, double std) {
        double [] predicted_class = new double[2];
        predicted_class[0] = 0.0;
        predicted_class[1] = 0.0;

        try {
            Classifier cls;
            cls = (Classifier) read(App.getContext().getAssets().open("WEKA_RandomForest_model1.model"));

            ArrayList<Attribute> attributes = new ArrayList<>();

            // TODO : Verify that these are correct attributes
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

            Log.d("WEKA", "Predicted class: " + predicted_class[0] + " Confidence score: " + predicted_class[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return HydrationLevel.convertFromReading( predicted_class[0] );
    }
}
