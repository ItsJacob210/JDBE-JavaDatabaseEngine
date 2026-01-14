# JavaDatabaseEngine

JDBE is a work-in-progress relational database engine implemented entirely in Java. It features a custom functional pipeline query language, page-based disk storage, B+ tree indexing, query optimization, & transactions with write-ahead logging and crash recovery. JDBE is currently being tested under 35 thousand records spanned across two tables.

### Key Features

- **Custom Functional Pipeline Language** - Natural, easy to learn, composable query syntax
- **Complete Compilation Pipeline** - Lexer → Parser → Semantic Analysis → Optimizer → Executor
- **Page-Based Storage Engine** - 4KB pages with buffer pool management
- **B+ Tree Indexing** - Automatic index selection during query optimization
- **Query Optimization** - Rule-based optimizer (filter pushdown, projection pruning, index selection)
- **ACID Transactions** - BEGIN/COMMIT/ABORT with Write-Ahead Logging
- **Crash Recovery** - Redo-based recovery for durability
- **Graphical User Interface** - Swing-based GUI with pre-loaded sample data, sytnax reference, table output & console log

### Requirements

- Java 21 or higher
- Gradle 8.4+ 

### Runningg GUI 

```bash
.\run-gui.bat
```

The GUI launches with **10,000 sample users and 25,000 sample products** 

### Supported Operations

| Operation | Description | Example |
|-----------|-------------|---------|
| `project` | Select specific columns | `project(id, name)` |
| `filter` | Filter rows by predicate | `filter(age >= 18)` |
| `sort` | Sort results | `sort(age desc)` |
| `limit` / `take` | Limit number of rows | `limit(10)` |
| `skip` | Skip rows (offset) | `skip(5)` |
| `modify` | Update rows | `modify(age = 30)` |
| `remove` | Delete rows | `remove` |
| `explain` | Show execution plan | `explain <query>` |

### Operators

**Comparison:** `==`, `!=`, `>`, `>=`, `<`, `<=`  
**Logical:** `and`, `or`  
**Data Types:** INTEGER, STRING, BOOLEAN

### Transactions

```sql
begin
users |> filter(id == 1) |> modify(balance = 100)
products |> filter(id == 5) |> modify(stock = 50)
commit
```

## Architecture

### System Layers

```
┌─────────────────────────────────────────────────────────┐
│                 GUI Interface                           │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│              Query Compilation Pipeline                 │
│   Lexer → Parser → Semantic → Optimizer → Planner       │
└─────────────────────┬───────────────────────────────────┘
                      │
    ┌─────────────────┴─────────────────┐
    │                                   │
┌───▼──────────────┐         ┌──────────▼──────────┐
│ Execution Engine │         │  Transaction Manager│
│ (Volcano Model)  │         │  + WAL Logging      │
└────────┬─────────┘         └──────────┬──────────┘
         │                              │
    ┌────▼──────────────────────────────▼────┐
    │         Storage Layer                  │
    │  ┌─────────────┐   ┌──────────────┐    │
    │  │ Buffer Pool │   │  B+ Tree     │    │
    │  │   (LRU)     │   │  Indexes     │    │
    │  └──────┬──────┘   └──────────────┘    │
    │         │                              │
    │  ┌──────▼──────┐   ┌──────────────┐    │
    │  │ Disk Manager│   │  Heap Pages  │    │
    │  └─────────────┘   └──────────────┘    │
    └────────────────────────────────────────┘
```

### Component Overview

| Component | Location | Description |
|-----------|----------|-------------|
| **Language Layer** | `com.dbengine.lang.*` | Lexer, parser, AST (sealed interfaces & records) |
| **Semantic Analysis** | `com.dbengine.semantic.*` | Type checking, catalog, schema management |
| **Query Optimizer** | `com.dbengine.planner.*` | Rule-based optimization, physical planning |
| **Execution Engine** | `com.dbengine.exec.*` | Volcano iterator model operators |
| **Storage Layer** | `com.dbengine.storage.*` | Pages, buffer pool, disk manager, heap files |
| **Indexing** | `com.dbengine.index.*` | B+ tree implementation |
| **Transactions** | `com.dbengine.txn.*` | WAL, log manager, recovery |
| **Interfaces** | `com.dbengine.gui.*` | GUI |

### Execution Operators

- **SeqScanOperator** - Sequential table scan
- **IndexScanOperator** - B+ tree index scan
- **FilterOperator** - Predicate evaluation
- **ProjectionOperator** - Column projection
- **SortOperator** - In-memory sorting
- **LimitOperator / SkipOperator** - Result limiting/offsetting
- **ModifyOperator** - Update operations
- **RemoveOperator** - Delete operations

## Storage Engine Details

### Page Structure (4KB Fixed Size)

```
┌─────────────────────────────────────────┐
│ Page Header (9 bytes)                   │
│  ├─ Page Type (1 byte)                  │
│  ├─ Tuple Count (4 bytes)               │
│  └─ Free Space Pointer (4 bytes)        │
├─────────────────────────────────────────┤
│ Slot Directory (grows downward)         │
│  ├─ Slot 0: [Offset | Length]           │
│  ├─ Slot 1: [Offset | Length]           │
│  └─ ...                                 │
├─────────────────────────────────────────┤
│ Free Space                              │
├─────────────────────────────────────────┤
│ Tuple Data (grows upward)               │
│  └─ [Tuple N]...[Tuple 1][Tuple 0]      │
└─────────────────────────────────────────┘
```

### Buffer Pool

- **Policy**: LRU eviction
- **Features**: Pin/unpin semantics, dirty page tracking, automatic disk I/O
- **Size**: Configurable (default: 100 pages = 400KB)

### Write-Ahead Logging 

1. Before modifying page → Write log record
2. Log contains: TxnID, PageID, Before/After images
3. On commit → Flush log to disk
4. Dirty pages flushed lazily

## GUI Features

The GUI provides a interface with:

- **Query Editor** - Syntax-highlighted input area
- **Results Table** - Clean tabular display
- **Output Log** - Terminal-style execution feedback
- **Syntax Reference** - Built-in help panel
- **Pre-loaded Data** - 10 users + 10 products ready to query
- **Example Buttons** - One-click query templates
- **Explain Plans** - Visual query plan display

### Sample Data Included

**Users Table**: 10,000 records with columns (id, name, age, active)  
**Products Table**: 25,000 records with columns (id, name, price, stock)

## Testing

```bash
.\gradlew.bat test
```

Tests includes:
- Unit tests (Lexer, Parser, Storage, Operators)
- Integration tests (End-to-end query execution)
- Edge cases (Empty results, large datasets, transactions)

## Project Structure

```
JavaDBEngine/
├── src/
│   ├── main/java/com/dbengine/
│   │   ├── Database.java           # Main engine coordinator
│   │   ├── lang/                   # Lexer, parser, AST
│   │   │   ├── lexer/              # Tokenization
│   │   │   ├── parser/             # Recursive-descent parser
│   │   │   └── ast/                # AST nodes (sealed interfaces)
│   │   ├── semantic/               # Type checking & catalog
│   │   ├── planner/                # Optimization & physical planning
│   │   ├── exec/                   # Physical operators
│   │   ├── storage/                # Pages, buffer pool, disk manager
│   │   ├── index/                  # B+ tree
│   │   ├── txn/                    # Transactions, WAL, recovery
│   │   └── gui/                    # Graphical interface
│   └── test/java/com/dbengine/    # Comprehensive tests
├── build.gradle                    # Gradle build configuration
├── gradlew / gradlew.bat          # Gradle wrapper scripts
├── run-gui.bat                    # Quick GUI launcher (Windows)
└── db_data/                       # Database files (auto-generated)
```

## Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| Sequential Scan | O(n) pages | Full table scan |
| Index Lookup | O(log n) | B+ tree height |
| Range Scan | O(log n + k) | k = result size |
| Sort | O(n log n) | In-memory |
| Filter | O(n) | Tuples scanned |

**Space Complexity:**
- Buffer Pool: Fixed size
- B+ Tree: O(n) for n keys
- Sort: O(n) materialization
