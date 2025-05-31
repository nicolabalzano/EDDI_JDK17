package ai.labs.eddi.engine.security;

import ai.labs.eddi.datastore.IResourceStore;
import ai.labs.eddi.engine.security.model.User;

import static ai.labs.eddi.datastore.IResourceStore.ResourceAlreadyExistsException;

/**
 * Interface for user data store operations
 * 
 * @author eddi-system
 */
public interface IUserStore {
    
    /**
     * Find a user by username
     * 
     * @param username the username to search for
     * @return the user if found, null otherwise
     * @throws IResourceStore.ResourceStoreException if there's an error accessing the store
     */
    User findUserByUsername(String username) throws IResourceStore.ResourceStoreException;
    
    /**
     * Create a new user
     * 
     * @param user the user to create
     * @throws ResourceAlreadyExistsException if a user with the same username already exists
     * @throws IResourceStore.ResourceStoreException if there's an error accessing the store
     */
    void createUser(User user) throws ResourceAlreadyExistsException, IResourceStore.ResourceStoreException;
    
    /**
     * Update an existing user
     * 
     * @param user the user to update
     * @throws IResourceStore.ResourceStoreException if there's an error accessing the store
     * @throws IResourceStore.ResourceNotFoundException if the user doesn't exist
     */
    void updateUser(User user) throws IResourceStore.ResourceStoreException, IResourceStore.ResourceNotFoundException;
    
    /**
     * Delete a user by username
     * 
     * @param username the username of the user to delete
     * @throws IResourceStore.ResourceStoreException if there's an error accessing the store
     */
    void deleteUser(String username) throws IResourceStore.ResourceStoreException;
    
    /**
     * Update the last login timestamp for a user
     * 
     * @param username the username of the user
     * @throws IResourceStore.ResourceStoreException if there's an error accessing the store
     * @throws IResourceStore.ResourceNotFoundException if the user doesn't exist
     */
    void updateLastLogin(String username) throws IResourceStore.ResourceStoreException, IResourceStore.ResourceNotFoundException;
}
