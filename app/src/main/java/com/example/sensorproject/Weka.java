package com.example.sensorproject;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

import static weka.core.SerializationHelper.read;

public class Weka {

    public HydrationLevel classification( float eda) {
        double [] predicted_class = new double[2];
        predicted_class[0] = 0.0;
        predicted_class[1] = 0.0;

        // TODO : Uncomment when able
//        try {
//
//            Classifier cls;
//
//            // TODO : Generate model and place in resources / assets directory
//            cls = (Classifier) read(App.getContext().getAssets().open("rf.model"));
//
//            ArrayList<Attribute> attributes = new ArrayList<>();
//
//            // TODO : Verify that these are correct attributes
//            attributes.add(new Attribute("eda", 0));
//            attributes.add(new Attribute("label", HydrationLevel.getLabels()));
//
//            Instance instance = new SparseInstance( 1);
//            instance.setValue(attributes.get(0), eda);
//
//            Instances dataSetConfiguration;
//
//            // TODO : Verify instance name is correct
//            dataSetConfiguration = new Instances("level.hydration", attributes, 0);
//
//            dataSetConfiguration.setClassIndex( 1 );
//
//            double[] distribution;
//            distribution = cls.distributionForInstance( instance );
//            predicted_class[0] = cls.classifyInstance( instance );
//            predicted_class[1] = Math.max(distribution[0], distribution[1]) * 100;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return HydrationLevel.convertFromReading( predicted_class[0] );

        int r = ThreadLocalRandom.current().nextInt() % 5;

        return HydrationLevel.convert( r );
    }
}
