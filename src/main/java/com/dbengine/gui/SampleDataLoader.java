package com.dbengine.gui;

import com.dbengine.semantic.Schema;
import com.dbengine.storage.TableHeap;
import com.dbengine.storage.Tuple;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Utility class to load scaled sample data into tables for GUI demonstration.
 * Generates 10,000 users and 25,000 products with randomized realistic data.
 */
public class SampleDataLoader {
    
    public interface ProgressCallback {
        void onProgress(int current, int total, String message);
    }
    
    private static final Random rand = new Random(42); //fixed seed for reproducibility
    
    //names for user generation
    private static final String[] FIRST_NAMES = {
        "Emma", "Liam", "Olivia", "Noah", "Ava", "Ethan", "Sophia", "Mason", "Isabella", "William",
        "Mia", "James", "Charlotte", "Benjamin", "Amelia", "Lucas", "Harper", "Henry", "Evelyn", "Alexander",
        "Abigail", "Sebastian", "Emily", "Jack", "Elizabeth", "Aiden", "Sofia", "Matthew", "Avery", "Samuel",
        "Ella", "David", "Scarlett", "Joseph", "Grace", "Carter", "Chloe", "Owen", "Victoria", "Wyatt",
        "Riley", "John", "Aria", "Dylan", "Lily", "Luke", "Aurora", "Gabriel", "Zoey", "Anthony"
    };
    
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
        "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
        "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson",
        "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
        "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts"
    };
    
    //product categories and items
    private static final String[][] PRODUCTS = {
        //electronics
        {"Laptop", "Mouse", "Keyboard", "Monitor", "Webcam", "Headphones", "USB Cable", "Phone Stand", 
         "Tablet", "Smart Watch", "USB Hub", "HDMI Cable", "Microphone", "Speakers", "Charging Station"},
        //office
        {"Desk Lamp", "Office Chair", "Standing Desk", "Notebook", "Pen Set", "Desk Organizer", 
         "File Cabinet", "Whiteboard", "Stapler", "Paper Shredder", "Calendar", "Bookshelf", "Mouse Pad"},
        //accessories
        {"Laptop Bag", "Phone Case", "Screen Protector", "Cable Organizer", "Laptop Stand", 
         "Wrist Rest", "Desk Mat", "Monitor Arm", "Adapter", "Power Bank"},
        //home
        {"Coffee Maker", "Water Bottle", "Desk Plant", "Air Purifier", "Humidifier", 
         "Desk Fan", "Storage Box", "Cable Sleeve", "LED Strip", "Clock"}
    };
    
    public static void loadUsersData(TableHeap tableHeap, Schema schema) throws IOException, InterruptedException {
        loadUsersData(tableHeap, schema, null);
    }
    
    public static void loadUsersData(TableHeap tableHeap, Schema schema, ProgressCallback callback) throws IOException, InterruptedException {
        Map<String, Integer> columnMap = new HashMap<>();
        columnMap.put("id", 0);
        columnMap.put("name", 1);
        columnMap.put("age", 2);
        columnMap.put("active", 3);
        
        int total = 100000;
        //generate 10,000 users
        for (int i = 1; i <= total; i++) {
            String firstName = FIRST_NAMES[rand.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[rand.nextInt(LAST_NAMES.length)];
            String fullName = firstName + " " + lastName;
            int age = 18 + rand.nextInt(65); //ages 18-82
            boolean active = rand.nextDouble() < 0.75; //75% active
            
            Object[] values = new Object[4];
            values[0] = i;
            values[1] = fullName;
            values[2] = age;
            values[3] = active;
            
            Tuple tuple = new Tuple(values, columnMap);
            tableHeap.insertTuple(tuple);
            
            //report progress every 1000 records
            if (callback != null && i % 1000 == 0) {
                callback.onProgress(i, total, "Loading users...");
                Thread.sleep(1); //small delay to allow GUI updates
            }
        }
        
        if (callback != null) {
            callback.onProgress(total, total, "Users loaded");
        }
        System.out.println("Loaded 10,000 users");
    }
    
    public static void loadProductsData(TableHeap tableHeap, Schema schema) throws IOException, InterruptedException {
        loadProductsData(tableHeap, schema, null);
    }
    
    public static void loadProductsData(TableHeap tableHeap, Schema schema, ProgressCallback callback) throws IOException, InterruptedException {
        Map<String, Integer> columnMap = new HashMap<>();
        columnMap.put("id", 0);
        columnMap.put("name", 1);
        columnMap.put("price", 2);
        columnMap.put("stock", 3);
        
        int total = 250000;
        //generate 25,000 products
        for (int i = 1; i <= total; i++) {
            //pick random category and product
            String[] category = PRODUCTS[rand.nextInt(PRODUCTS.length)];
            String productName = category[rand.nextInt(category.length)];
            
            //add variant suffix for uniqueness
            String fullName = productName;
            if (i % 100 > 10) { //most products get variants
                String[] variants = {"Pro", "Plus", "Elite", "Standard", "Basic", "Premium", "Deluxe"};
                fullName = productName + " " + variants[rand.nextInt(variants.length)];
            }
            
            //realistic pricing based on product type
            int basePrice = productName.contains("Laptop") || productName.contains("Monitor") || productName.contains("Desk") ? 200 : 10;
            int price = basePrice + rand.nextInt(basePrice * 4);
            
            //stock levels
            int stock = rand.nextInt(100);
            
            Object[] values = new Object[4];
            values[0] = i;
            values[1] = fullName;
            values[2] = price;
            values[3] = stock;
            
            Tuple tuple = new Tuple(values, columnMap);
            tableHeap.insertTuple(tuple);
            
            //report progress every 2500 records  
            if (callback != null && i % 2500 == 0) {
                callback.onProgress(i, total, "Loading products...");
                Thread.sleep(1); //small delay to allow GUI updates
            }
        }
        
        if (callback != null) {
            callback.onProgress(total, total, "Products loaded");
        }
        System.out.println("Loaded 25,000 products");
    }
}
