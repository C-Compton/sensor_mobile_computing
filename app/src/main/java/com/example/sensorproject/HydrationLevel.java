package com.example.sensorproject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum HydrationLevel {
    WELL_HYDRATED(App.getContext().getString(R.string.well_hydrated),0),
    SLIGHTLY_HYDRATED(App.getContext().getString(R.string.slightly_hydrated),1),
    SLIGHTLY_DEHYDRATED(App.getContext().getString(R.string.slightly_dehydrated),2),
    VERY_DEHYDRATED(App.getContext().getString(R.string.very_dehydrated), 3),
    UNKNOWN_LEVEL(App.getContext().getString(R.string.unknown_level), 4);

    final String value;

    final int label;

    HydrationLevel( String value, int label ) {
        this.value = value;
        this.label = label;
    }

    public int getLabel() {
        return label;
    }

    public String getValue() { return value; }

    public static HydrationLevel convertFromReading( double reading) {
        if (reading == 0.0) {
            return WELL_HYDRATED;
        } else if (reading == 1.0) {
            return SLIGHTLY_HYDRATED;
        } else if (reading == 2.0) {
            return SLIGHTLY_DEHYDRATED;
        } else if (reading == 3.0) {
            return VERY_DEHYDRATED;
        } else { return UNKNOWN_LEVEL; }
    }

    public static HydrationLevel convert( int label ) {
        switch(label) {
            case 0:
                return WELL_HYDRATED;
            case 1:
                return SLIGHTLY_HYDRATED;
            case 2:
                return SLIGHTLY_DEHYDRATED;
            case 3:
                return VERY_DEHYDRATED;
            case 4:
            default:
                return UNKNOWN_LEVEL;
        }
    }

    public static List<String> getLabels() {
        return Arrays
                .stream( HydrationLevel.values() )
                .filter( d -> !HydrationLevel.UNKNOWN_LEVEL.equals( d ) )
                .map( HydrationLevel::getLabel )
                .sorted()
                .map( String::valueOf )
                .collect( Collectors.toList() );
    }

}
