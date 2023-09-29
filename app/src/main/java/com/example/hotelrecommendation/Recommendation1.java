package com.example.hotelrecommendation;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class Recommendation1 {
    private String name;
    private String link;
    private String address;
    private String contactNumber;
    private String food;
    private float rating;
    private String imageUrl;

    private String location;
    private String timings;
    private String placeId;
    private String foodType;

    public Recommendation1() {
        // Default constructor required for calls to DataSnapshot.getValue(Recommendation1.class)
    }

    public Recommendation1(String name, String link, String address, String contactNumber, String food, String location, float rating, String imageUrl, String timings, String placeId, String foodType) {
        this.name = name;
        this.link = link;
        this.address = address;
        this.contactNumber = contactNumber;
        this.food = food;
        this.location = location;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.timings = timings;
        this.placeId = placeId;
        this.foodType = foodType;
    }


    @PropertyName("name")
    public String getName() {
        return name;
    }

    @PropertyName("link")
    public String getLink() {
        return link;
    }

    @PropertyName("address")
    public String getAddress() {
        return address;
    }


    @PropertyName("contactNumber")
    public String getContactNumber() {
        return contactNumber;
    }

    @PropertyName("food")
    public String getFood() {
        return food;
    }

    @PropertyName("rating")
    public float getRating() {
        return rating;
    }

    @PropertyName("imageUrl")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("location")
    public String getLocation() {
        return location;
    }

    @PropertyName("timings")
    public String getTimings() {
        return timings;
    }
    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }
}
