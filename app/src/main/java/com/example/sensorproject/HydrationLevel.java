package com.example.sensorproject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum HydrationLevel {
    WELL_HYDRATED(R.string.well_hydrated,0),
    VERY_DEHYDRATED(R.string.very_dehydrated, 1),
    UNKNOWN_LEVEL(R.string.unknown_level, 2);

    final int resourceId;

    final int label;

    HydrationLevel( int resourceId, int label ) {
        this.resourceId = resourceId;
        this.label = label;
    }

    public int getLabel() {
        return label;
    }

    public int getResourceId() { return resourceId; }

    public static HydrationLevel convertFromReading( double reading) {
        if (reading == 0.0) {
            return WELL_HYDRATED;
        } else if (reading == 1.0) {
            return VERY_DEHYDRATED;
        } else { return UNKNOWN_LEVEL; }
    }

    public static HydrationLevel convert( int label ) {
        switch(label) {
            case 0:
                return WELL_HYDRATED;
            case 1:
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
