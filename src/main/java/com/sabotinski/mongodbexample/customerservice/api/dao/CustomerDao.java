package com.sabotinski.mongodbexample.customerservice.api.dao;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.sabotinski.mongodbexample.customerservice.api.models.Customer;

import org.bson.Document;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.codecs.DecoderContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CustomerDao {

    private static String CUSTOMER_COLLECTION_NAME = "customers_rev";
    private MongoCollection<Customer> customerCollection;
    private CodecRegistry pojoCodecRegistry;

    private final CodecProvider customerCodecProvider = PojoCodecProvider.builder()
            .register(Customer.class.getPackage().getName()).build();

    @Autowired
    public CustomerDao(MongoClient mongoClient, @Value("${db.databasename}") String databaseName) {
        this.pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(customerCodecProvider),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        customerCollection = mongoClient.getDatabase(databaseName)
                .getCollection(CUSTOMER_COLLECTION_NAME, Customer.class).withCodecRegistry(pojoCodecRegistry);
    }

    public List<Customer> getCustomers() {
        var docs = new ArrayList<Document>();
        customerCollection.find(Document.class).iterator().forEachRemaining(docs::add);
        var customers = docs.stream().map(this::decode).collect(toList());
        return customers;
    }

    public Customer getCustomer(String customerId) {
        var customer = customerCollection.find(eq("customerid", customerId)).first();
        return customer;
    }

    public void createCustomer(Customer newCustomer) {
        customerCollection.insertOne(newCustomer);
    }

    private Customer decode(Document doc) { 

        var didUpgrade = handleUpgrade(doc);

        var reader = new BsonDocumentReader(doc.toBsonDocument(BsonDocument.class, pojoCodecRegistry));
        var codec = customerCodecProvider.get(Customer.class, pojoCodecRegistry);
        var customer = codec.decode(reader, DecoderContext.builder().build());

        if (didUpgrade) { 
            customerCollection.replaceOne(eq("_id", doc.get("_id", new ObjectId())), customer);
        }
        return customer;
    }

    private boolean handleUpgrade(Document doc) { 
        var schemaVersion = doc.get("schemaVersion", 1).intValue();
        if (schemaVersion == Customer.CURRENT_SCHEMA_VERSION) { 
            return false;
        }
        
        switch (schemaVersion) {
            case 1: upgradeFromV1(doc); break;
        }
        return true;
    }

    private void upgradeFromV1(Document doc) {
        doc.append("customerType", "B2C");
        doc.put("schemaVersion", 2);
    }

}