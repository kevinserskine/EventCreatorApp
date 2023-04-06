package com.example.mobileappproject.models;

import com.google.android.gms.maps.model.LatLng;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class User {

    private String name;
    private LatLng location;

}
