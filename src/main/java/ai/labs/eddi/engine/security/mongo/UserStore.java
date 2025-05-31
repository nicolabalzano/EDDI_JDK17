package ai.labs.eddi.engine.security.mongo;

import ai.labs.eddi.datastore.IResourceStore;
import ai.labs.eddi.datastore.IResourceStore.ResourceAlreadyExistsException;
import ai.labs.eddi.datastore.serialization.IDocumentBuilder;
import ai.labs.eddi.datastore.serialization.IJsonSerialization;
import ai.labs.eddi.engine.security.IUserStore;
import ai.labs.eddi.engine.security.model.User;
import ai.labs.eddi.utils.RuntimeUtilities;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.rxjava3.core.Observable;
import org.bson.Document;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;

/**
 * MongoDB implementation of user store for authentication system
 * 
 * @author eddi-system
 */
@ApplicationScoped
public class UserStore implements IUserStore {
    private static final String COLLECTION_USERS = "users";
    private static final String USERNAME_FIELD = "username";
    private static final String LAST_LOGIN_FIELD = "lastLoginAt";
    
    private final MongoCollection<Document> collection;
    private final IDocumentBuilder documentBuilder;
    private final IJsonSerialization jsonSerialization;
    private final UserResourceStore userStore;
    
    private static final Logger log = Logger.getLogger(UserStore.class);

    @Inject
    public UserStore(MongoDatabase database,
                     IJsonSerialization jsonSerialization,
                     IDocumentBuilder documentBuilder) {
        this.jsonSerialization = jsonSerialization;
        RuntimeUtilities.checkNotNull(database, "database");
        this.collection = database.getCollection(COLLECTION_USERS);
        this.documentBuilder = documentBuilder;
        this.userStore = new UserResourceStore();
        
        // Create unique index on username
        Observable.fromPublisher(
                collection.createIndex(Indexes.ascending(USERNAME_FIELD), new IndexOptions().unique(true))
        ).blockingFirst();
        
        log.info("UserStore initialized with MongoDB backend");
    }

    @Override
    public User findUserByUsername(String username) throws IResourceStore.ResourceStoreException {
        RuntimeUtilities.checkNotNull(username, USERNAME_FIELD);
        return userStore.findUserByUsername(username);
    }

    @Override
    public void createUser(User user) throws ResourceAlreadyExistsException, IResourceStore.ResourceStoreException {
        RuntimeUtilities.checkNotNull(user, "user");
        RuntimeUtilities.checkNotNull(user.getUsername(), "user.username");
        RuntimeUtilities.checkNotNull(user.getPasswordHash(), "user.passwordHash");
        
        userStore.createUser(user);
    }

    @Override
    public void updateUser(User user) throws IResourceStore.ResourceStoreException, IResourceStore.ResourceNotFoundException {
        RuntimeUtilities.checkNotNull(user, "user");
        RuntimeUtilities.checkNotNull(user.getUsername(), "user.username");
        
        userStore.updateUser(user);
    }

    @Override
    public void deleteUser(String username) throws IResourceStore.ResourceStoreException {
        RuntimeUtilities.checkNotNull(username, USERNAME_FIELD);
        userStore.deleteUser(username);
    }

    @Override
    public void updateLastLogin(String username) throws IResourceStore.ResourceStoreException, IResourceStore.ResourceNotFoundException {
        RuntimeUtilities.checkNotNull(username, USERNAME_FIELD);
        userStore.updateLastLogin(username);
    }

    private class UserResourceStore {
        
        User findUserByUsername(String username) throws IResourceStore.ResourceStoreException {
            Document filter = new Document(USERNAME_FIELD, username);

            try {
                Document document = Observable.fromPublisher(collection.find(filter).first()).blockingFirst();
                return documentBuilder.build(document, User.class);
            } catch (NoSuchElementException e) {
                return null;
            } catch (IOException e) {
                throw new IResourceStore.ResourceStoreException(e.getLocalizedMessage(), e);
            }
        }

        void createUser(User user) throws IResourceStore.ResourceStoreException, ResourceAlreadyExistsException {
            // Check if user already exists
            User existingUser = findUserByUsername(user.getUsername());
            if (existingUser != null) {
                String message = "User with username=%s already exists";
                message = String.format(message, user.getUsername());
                throw new ResourceAlreadyExistsException(message);
            }

            try {
                Document userDocument = createDocument(user);
                Observable.fromPublisher(collection.insertOne(userDocument)).blockingFirst();
                log.info("User created successfully: " + user.getUsername());
            } catch (Exception e) {
                log.error("Error creating user: " + user.getUsername(), e);
                throw new IResourceStore.ResourceStoreException(e.getLocalizedMessage(), e);
            }
        }

        void updateUser(User user) throws IResourceStore.ResourceStoreException, IResourceStore.ResourceNotFoundException {
            // Check if user exists
            User existingUser = findUserByUsername(user.getUsername());
            if (existingUser == null) {
                String message = "User with username=%s not found";
                message = String.format(message, user.getUsername());
                throw new IResourceStore.ResourceNotFoundException(message);
            }

            try {
                Document userDocument = createDocument(user);
                Document filter = new Document(USERNAME_FIELD, user.getUsername());
                Observable.fromPublisher(collection.replaceOne(filter, userDocument)).blockingFirst();
                log.info("User updated successfully: " + user.getUsername());
            } catch (Exception e) {
                log.error("Error updating user: " + user.getUsername(), e);
                throw new IResourceStore.ResourceStoreException(e.getLocalizedMessage(), e);
            }
        }

        void deleteUser(String username) throws IResourceStore.ResourceStoreException {
            try {
                Document filter = new Document(USERNAME_FIELD, username);
                Observable.fromPublisher(collection.deleteOne(filter)).blockingFirst();
                log.info("User deleted successfully: " + username);
            } catch (Exception e) {
                log.error("Error deleting user: " + username, e);
                throw new IResourceStore.ResourceStoreException(e.getLocalizedMessage(), e);
            }
        }

        void updateLastLogin(String username) throws IResourceStore.ResourceStoreException, IResourceStore.ResourceNotFoundException {
            // Check if user exists
            User existingUser = findUserByUsername(username);
            if (existingUser == null) {
                String message = "User with username=%s not found";
                message = String.format(message, username);
                throw new IResourceStore.ResourceNotFoundException(message);
            }

            try {
                Document filter = new Document(USERNAME_FIELD, username);
                Document update = new Document("$set", new Document(LAST_LOGIN_FIELD, new Date()));
                Observable.fromPublisher(collection.updateOne(filter, update)).blockingFirst();
                log.debug("Last login updated for user: " + username);
            } catch (Exception e) {
                log.error("Error updating last login for user: " + username, e);
                throw new IResourceStore.ResourceStoreException(e.getLocalizedMessage(), e);
            }
        }

        private Document createDocument(User user) throws IResourceStore.ResourceStoreException {
            try {
                return jsonSerialization.deserialize(jsonSerialization.serialize(user), Document.class);
            } catch (IOException e) {
                throw new IResourceStore.ResourceStoreException(e.getLocalizedMessage(), e);
            }
        }
    }
}
