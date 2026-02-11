# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Zeze is a distributed transaction framework based on cache coherence. It provides automatic transaction management, multi-threading safety with optimistic locking (no deadlocks), and automatic memory-database synchronization across multiple backend databases (MySQL, PostgreSQL, MongoDB, Redis, TiKV, DynamoDB, FoundationDB, etc.).

## Build Commands

### Java (Primary Language)
- **Build all Java modules**: `cd ZezeJava/ZezeJava && mvn clean package`
- **Run tests**: `cd ZezeJava/ZezeJavaTest && mvn test`
- **Run single test**: `cd ZezeJava/ZezeJavaTest && mvn test -Dtest=ClassName#methodName`
- **Deploy to Maven Central**: `cd ZezeJava/ZezeJava && mvn deploy` (requires GPG signing and Sonatype setup)
- **Install locally**: `mvn install`

### TypeScript Client
- **First-time setup**: `cd TypeScript && npm install --save-dev typescript`
- **Compile**: `node_modules\.bin\tsc.cmd` or use Visual Studio with `TypeScript.njsproj`
- **Run tests**: Set `NODE_PATH=.` environment variable, then `node app.js`

### C++
- **Build**: Uses Makefile in `cxx/` directory

### Code Generation
- The framework uses code generation from XML definitions (solution.xml)
- Generated files are typically in `Gen/` directories and should not be manually edited

## Project Structure

```
ZezeJava/              # Java implementation (main)
  ├─ ZezeJava/         # Core framework
  ├─ ZezeJavaTest/     # Tests
  ├─ ZezexJava/        # Extensions (client, linkd, server examples)
  └─ ZokerManager/     # Process management
cxx/                   # C++ implementation (client-side)
TypeScript/            # TypeScript/JavaScript client
python/                # Code generation tools
doc/Writerside/        # Comprehensive documentation
Gen/                   # Code generation (C#)
confcs/                # Configuration (C#)
```

## Core Architecture

### Key Java Packages
- **`Zeze.Application`** - Main framework entry point, manages all components
- **`Zeze.Arch`** - Provider-Linkd architecture for distributed services
  - `LinkdApp`, `LinkdProvider` - Connection load distribution server
  - `ProviderApp`, `ProviderImplement` - Logic service providers
  - `Online` - Account-based online management with reliable messaging
- **`Zeze.Transaction`** - Transaction management
  - `Procedure` - Stored procedures with automatic locking
  - `Database`* - Database abstractions (RocksDb, MySQL, PostgreSQL, MongoDB, etc.)
  - `Table` - Table storage with automatic synchronization
- **`Zeze.Dbh2`** - Database abstraction layer (legacy name, persists)
- **`Zeze.Net` / `Zeze.Netty`** - Networking layer using Netty
- **`Zeze.Serialize`** - Binary serialization (`ByteBuffer`)
- **`Zeze.Collections`** - Persistent collections (List, Set, Map, etc.)
- **`Zeze.Component`** - Framework components (Timer, AutoKey, DelayRemove)
- **`Zeze.Services`** - Distributed services
  - `GlobalCacheManager` - Cache synchronization
  - `ServiceManager` - Service discovery
  - `Daemon` - Background services
- **`Zeze.Raft`** - Raft consensus implementation
- **`Zeze.Hot`** - Hot reload and class reloading

### Package Naming Convention
**Important**: Java packages use `Zeze.*` (capital Z), NOT `com.zeze.*`. The Maven groupId is `com.zezeno` but the actual Java package is simply `Zeze`.

### Arch: Provider-Linkd Architecture
- **Linkd**: Connection load balancer that sits between clients and providers
- **Provider (Server)**: Actual logic implementation, runs as independent processes
- **Module**: Logical unit within a provider (like a microservice)
- **Protocol Types**:
  - `@RedirectHash` - Redirect to specific server based on hash
  - `@RedirectAll` - Broadcast to all servers (MapReduce-like)
  - `@RedirectToServer` - Direct redirect to specific server by ID

### Transaction Model
- Uses optimistic locking - no deadlocks possible
- `Procedure` class encapsulates transactional logic
- Transactions automatically commit/rollback based on success/failure
- Data automatically syncs between memory and configured databases

## Development Notes

### Module System
- Modules are defined in `solution.xml` files
- Code generation creates `Gen/` directories with boilerplate
- Do not modify generated code directly
- Module IDs must be unique across the entire system

### Database Support
The framework supports multiple database backends simultaneously:
- Default: `Memory` (in-memory for development)
- Production: RocksDB, MySQL, PostgreSQL, MongoDB, Redis, TiKV, DynamoDB, FoundationDB
- Configuration in XML files using `<DatabaseConf>` tags

### Service Discovery
- Uses `ServiceManager` component for service registration and discovery
- Services are identified by name and identity (ServerId/IP:Port)
- Linkd discovers Servers through the service manager

### Testing
- Test files are in `ZezeJava/ZezeJavaTest/`
- Uses JUnit 4.13.2
- Tests may require database configuration

### Maven Publishing
- Requires GPG signing setup
- Requires Sonatype account with domain verification
- See `doc/MavenDeploy.md` for detailed instructions

### Configuration Files
- Server configs are XML files (e.g., `server.xml`, `linkd.xml`)
- Config loaded via `Config.Load(filePath)`
- `<ServiceConf>` defines network services
- `<DatabaseConf>` defines database connections

## Important Constraints

1. **Java Version**: Target Java 11
2. **Package Structure**: Always use `Zeze.*` imports, not `com.zeze.*`
3. **Generated Code**: Files in `Gen/` directories are auto-generated, do not edit
4. **Maven Coordinates**: Published as `com.zezeno:zeze-java:1.6.1`
5. **Dependencies**: Most dependencies are marked as `<scope>provided</scope>` - applications must include them

## Documentation

Comprehensive documentation is available in `doc/Writerside/topics/`:
- `Arch.md` - Architecture details
- `Transaction.md` - Transaction system
- `Quick-Start.md` - Getting started guide
- `Bean.md`, `Collections.md`, `Component.md` - Framework components
- Online docs: https://stallboy.github.io/zezedocs/
