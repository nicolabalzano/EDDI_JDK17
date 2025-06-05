# Database Security Configuration

## Password Setup

1. **Copy the environment template:**
   ```bash
   cp .env.example .env
   ```

2. **Edit the `.env` file** and set secure passwords:
   ```env
   MONGO_INITDB_ROOT_USERNAME=admin
   MONGO_INITDB_ROOT_PASSWORD=your_secure_mongo_password_here
   MONGO_DB_NAME=eddi
   SQLITE_PASSWORD=your_secure_sqlite_password_here
   QUARKUS_PROFILE=prod
   ```

## Security Requirements

### MongoDB Password
- **Minimum length:** 12 characters
- **Required characters:** Uppercase, lowercase, numbers, special characters
- **Example:** `M0ng0Db$ecur3P@ssw0rd!`

### SQLite Password
- Currently used for additional security layers
- Same requirements as MongoDB password
- **Example:** `$qL1t3$3cur3P@ssw0rd!`

## Important Security Notes

⚠️ **NEVER commit `.env` file to version control**
✅ **Always use strong, unique passwords**
✅ **Change default passwords in production**
✅ **Regularly rotate passwords**

## Usage

After setting up the `.env` file, start the application with:

```bash
docker-compose up -d
```

The containers will automatically read the environment variables and configure secure database access.

## Database Access

### MongoDB (with authentication)
```bash
# Connect to MongoDB with authentication
docker exec -it mongodb mongo -u admin -p your_password --authenticationDatabase admin
```

### SQLite
```bash
# Access SQLite database
docker exec -it sqlite-db sqlite3 /shared/reviews.db
```

## Troubleshooting

### Connection Issues
1. Verify environment variables are loaded:
   ```bash
   docker-compose config
   ```

2. Check container logs:
   ```bash
   docker-compose logs mongodb
   docker-compose logs eddi
   ```

### Password Changes
1. Update `.env` file
2. Recreate containers:
   ```bash
   docker-compose down
   docker-compose up -d
   ```
