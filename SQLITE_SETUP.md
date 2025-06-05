# SQLite Reviews Database - Docker Setup

## Panoramica

Il sistema delle recensioni ora utilizza un servizio SQLite separato nel Docker Compose. Questo permette:

- **Persistenza dei dati**: Le recensioni sono memorizzate in un volume Docker condiviso
- **Accesso separato**: È possibile accedere al database SQLite indipendentemente dall'applicazione principale
- **Verifiche semplici**: Script dedicati per interrogare il database

## Architettura

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   EDDI App      │    │   MongoDB       │    │   SQLite DB     │
│   Container     │    │   Container     │    │   Container     │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ReviewResource│ │    │ │Primary Data │ │    │ │reviews.db   │ │
│ │             │ │    │ │             │ │    │ │             │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                                              │
         └──────────────────────────────────────────────┘
                    /shared/reviews.db volume
```

## Servizi Docker

### sqlite-db
- **Immagine**: alpine:latest con SQLite installato
- **Volume**: `sqlite_data:/shared`
- **Database**: `/shared/reviews.db`
- **Tabella**: `reviews (id, username, email, review)`

### eddi
- **Volume condiviso**: `sqlite_data:/shared` 
- **Variabile d'ambiente**: `SQLITE_DB_PATH=/shared/reviews.db`
- **Configurazione**: `sqlite.db.path` in application.properties

## Come usare

### 1. Avviare i servizi
```bash
docker-compose up -d
```

### 2. Verificare che i container siano attivi
```bash
docker-compose ps
```

### 3. Accedere al database SQLite

#### Opzione A: Script PowerShell (Windows)
```powershell
# Accesso interattivo
.\query-reviews.ps1

# Query diretta
.\query-reviews.ps1 "SELECT * FROM reviews;"
.\query-reviews.ps1 "SELECT COUNT(*) FROM reviews;"
```

#### Opzione B: Script Bash (Linux/Mac)
```bash
# Accesso interattivo
./query-reviews.sh

# Query diretta  
./query-reviews.sh "SELECT * FROM reviews;"
./query-reviews.sh "SELECT COUNT(*) FROM reviews;"
```

#### Opzione C: Comando Docker diretto
```bash
# Accesso interattivo
docker exec -it sqlite-db sqlite3 /shared/reviews.db

# Query diretta
docker exec -it sqlite-db sqlite3 /shared/reviews.db "SELECT * FROM reviews;"
```

### 4. Esempi di query utili

```sql
-- Visualizzare tutte le recensioni
SELECT * FROM reviews;

-- Contare il numero totale di recensioni  
SELECT COUNT(*) FROM reviews;

-- Cercare recensioni per username
SELECT username, review FROM reviews WHERE username LIKE '%test%';

-- Recensioni più recenti (ordinate per ID)
SELECT * FROM reviews ORDER BY id DESC LIMIT 5;

-- Verificare la struttura della tabella
.schema reviews

-- Informazioni sulla tabella
.tables
```

## Test del sistema

### 1. Inserire una recensione di test
Andare su: http://localhost:7070/review.html

### 2. Verificare l'inserimento
```powershell
.\query-reviews.ps1 "SELECT * FROM reviews ORDER BY id DESC LIMIT 1;"
```

### 3. Visualizzare tutte le recensioni
Andare su: http://localhost:7070/reviews.html

## Troubleshooting

### Container non avviato
```bash
docker-compose logs sqlite-db
docker-compose logs eddi
```

### Database non accessibile
```bash
# Verificare che il volume sia montato
docker exec -it sqlite-db ls -la /shared/

# Verificare i permessi del database
docker exec -it sqlite-db ls -la /shared/reviews.db

# Ricreare il database se necessario
docker exec -it sqlite-db sqlite3 /shared/reviews.db "CREATE TABLE IF NOT EXISTS reviews (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, email TEXT, review TEXT);"
```

### Reset completo
```bash
# Fermare i servizi
docker-compose down

# Rimuovere il volume SQLite
docker volume rm eddi_sqlite_data

# Riavviare
docker-compose up -d
```

## Sicurezza

⚠️ **Nota**: Il codice attuale è volutamente vulnerabile a SQL Injection per scopi educativi. In produzione, utilizzare prepared statements:

```java
String sql = "INSERT INTO reviews (username, email, review) VALUES (?, ?, ?)";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setString(1, username);
pstmt.setString(2, email);  
pstmt.setString(3, review);
pstmt.executeUpdate();
```
