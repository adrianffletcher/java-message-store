package com.example.services;

import com.example.models.Digest;
import com.example.models.Message;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.codec.binary.Hex;
import org.bson.Document;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class MessageService is a JAX-JS RESTful web service which has allows clients to submit messages and receive a
 * SHA-256 hash value and then subsequently provide the hash value to receive the original message.
 */
@Path("/messages")
public class MessageService {

    public static final int DEFAULT_REDIS_MAX_CONNECTIONS = 20;

    // the URI for the instance of MongoDB that we will use for persistence
    private static MongoClientURI mongoUrl = new MongoClientURI(System.getenv("MONGODB_URI"));

    // the mongo client library which all threads can access as it supports connection pooling
    private static MongoClient mongoClient = new MongoClient(mongoUrl);

    // the redis connection pool for the redis instance we use as an LRU cache
    private static JedisPool jedisPool = createRedisConnectionPool(System.getenv("REDIS_URL"));

    /**
     * Service method which takes a message and generates the SHA-256 hash value. Firstly we check our Redis least
     * recently used (LRU) cache to see if we have this message in the cache and if so we simply return the hash. If not we
     * check the MongoDB to see if we have already seen this message and if so the hash is simply returned. If we have
     * not seen this message before then the message is persisted in MongoDB and added to the Redis cache.
     * @param message the message to be stored
     * @return the response object containing the hash value
     * @throws NoSuchAlgorithmException if SHA-256 is not supported
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Message message) throws NoSuchAlgorithmException {

        // generate the SHA-256 hash in hex
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(message.getMessage().getBytes(StandardCharsets.UTF_8));
        String hash = new String(Hex.encodeHex(encodedHash));

        // check if we have this hash in our Redis LRU cache
        Jedis redisClient = jedisPool.getResource();
        Boolean hashExists = redisClient.exists("hash:" + hash);

        // if not in the Redis cache then check MongoDB for the hash
        if (!hashExists) {
            MongoDatabase db = mongoClient.getDatabase(mongoUrl.getDatabase());
            MongoCollection<Document> messages = db.getCollection("messages");
            String existingMessage = getMessageByHash(messages, hash);

            // only store in MongoDB if we do not have this message already
            if (existingMessage == null) {
                Map<String, Object> attributeMap = new HashMap<String, Object>();
                attributeMap.put("hash", hash);
                attributeMap.put("message", message.getMessage());
                messages.insertOne(new Document(attributeMap));
            }

            // populate the Redis LRU cache
            redisClient.hset("hash:" + hash, "message", message.getMessage());
        }
        // return redis connection to the connection pool
        redisClient.close();
        // return the hash value
        return Response.ok(new Digest(hash), MediaType.APPLICATION_JSON).build();
    }

    /**
     * Service method which takes a SHA-256 hash value returns the original message if we have seen this hash before.
     * We check our Redis LRU cache first and then MongoDB to see if we have this hash and either return the associated
     * message or a HTTP 404 error code.
     * @param hash the hash of the message being requested
     * @return the associated original message for this hash value
     */
    @GET
    @Path("/{hash}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("hash") String hash) {

        // check if we have this hash in our Redis LRU cache
        Jedis redisClient = jedisPool.getResource();
        String message = (String) redisClient.hget("hash:" + hash, "message");

        if (message == null) {
            // check if we have the hash value in MongoDB
            MongoDatabase db = mongoClient.getDatabase(mongoUrl.getDatabase());
            MongoCollection<Document> messages = db.getCollection("messages");
            message = getMessageByHash(messages, hash);
            if (message != null) {
                // populate the Redis LRU cache
                redisClient.hset("hash:" + hash, "message", message);
            }
        }

        // return redis connection to the connection pool
        redisClient.close();

        if (message == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok(new Message(message), MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * getMessageByHash is a utility method to search for a message in MongoDB by hash value
     * @param messages the MongoDB database containing the messages
     * @param hash the hash value we are searching for
     * @return the message if we locate it or null if not
     */
    private String getMessageByHash(MongoCollection<Document> messages, String hash) {
        BasicDBObject searchObject = new BasicDBObject();
        searchObject.put("hash", hash);
        Iterator<Document> results = messages.find(searchObject).iterator();
        String message = null;
        if (results.hasNext()) {
            message = results.next().getString("message");
        }
        return message;
    }

    /**
     * createRedisConnectionPool is a static initialisation method to configure the Redis connection pool
     * @param redisUrlStr a valid URL for a running Redis instance
     * @return the configured Redis connection pool
     */
    private static JedisPool createRedisConnectionPool(String redisUrlStr) {

        // create the Redis URI
        URI redisUri = null;
        try {
            redisUri = new URI(redisUrlStr);
        } catch(URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid URI: " + redisUrlStr, ex);
        }
        // default maximum connections if not specified
        int maxConnections = DEFAULT_REDIS_MAX_CONNECTIONS;
        String maxConnectionsStr = System.getenv("REDIS_MAX_CONNECTIONS");
        if (maxConnectionsStr != null) {
            maxConnections = Integer.parseInt(maxConnectionsStr);
        }
        // configure the Redis connection pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxConnections);
        poolConfig.setMaxIdle(maxConnections);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return new JedisPool(poolConfig, redisUri);
    }
}
