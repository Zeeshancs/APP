# Second-Hand Marketplace Application

A Java Swing desktop application integrated with MongoDB for managing a second-hand marketplace. The platform supports role-based portals for buyers and sellers, product listings, order processing, stock management, and interactive sales analytics powered by JFreeChart.

## Overview

This application provides a complete marketplace workflow for a second-hand goods platform. Buyers can register, browse products, place orders, and view their purchase history, while sellers can manage listings, track inventory, and review sales performance through analytics dashboards.

## Key Features

### Buyer Portal
- Buyer registration and login
- Product catalog browsing with availability, category, price, stock, and descriptions
- Purchase workflow with automatic inventory reduction
- Order generation and receipt tracking
- Purchase history visualized through bar charts using JFreeChart

### Seller Portal
- Seller registration and login
- Product listing creation with price validation and stock management
- Sales analytics and performance summaries
- Metrics such as total orders, units sold, total revenue, and average order value
- Per-product analysis reports and sales timeline charts

## Tech Stack

- Language: Java 17
- GUI Framework: Java Swing
- Database: MongoDB (MongoDB Java Driver 4.11.1)
- Visualization: JFreeChart 1.5.4
- Logging: SLF4J Simple 2.0.9
- Build Tool: Apache Maven

## Architecture and Design Patterns

- Singleton pattern for database connectivity through `MongoConnection`
- Repository pattern for data access via `UserRepository`, `ProductRepository`, and `OrderRepository`
- Object-oriented design with abstract `User` class extended by `Buyer` and `Seller`
- Custom exception handling for invalid login, invalid price, and out-of-stock scenarios

## Database Schema

The application uses MongoDB collections for users, products, and orders.

### `users`
- `_id` (String UUID)
- `name` (String)
- `email` (String)
- `password` (String)
- `role` ("BUYER" or "SELLER")

### `products`
- `_id` (String UUID)
- `sellerId` (String)
- `name` (String)
- `description` (String)
- `category` (String)
- `price` (Double)
- `quantity` (Integer)

### `orders`
- `_id` (String UUID)
- `buyerId` (String)
- `productId` (String)
- `quantity` (Integer)
- `totalPrice` (Double)
- `timestamp` (String)

## Prerequisites

Before running the app, ensure you have:

- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.6+
- MongoDB Atlas cluster or a local MongoDB instance

## Installation and Setup

### 1. Clone the repository

```bash
git clone <repository-url>
cd secondhand-marketplace
```

### 2. Configure the MongoDB connection

Set a MongoDB connection string in the `MONGO_URI` environment variable.

#### Linux/macOS

```bash
export MONGO_URI="mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<database>"
```

#### Windows (Command Prompt)

```cmd
set MONGO_URI="mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<database>"
```

#### Windows (PowerShell)

```powershell
$env:MONGO_URI="mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<database>"
```

## Running the Application

Build and run the project with Maven:

```bash
mvn clean compile exec:java
```

## Project Structure

```text
secondhand-marketplace/
├── src/
├── pom.xml
├── README.md
└── .gitignore
```

## Usage

1. Start the application.
2. Register a buyer or seller account.
3. Log in with the created account.
4. Buyers can browse products and place orders.
5. Sellers can list products and review their sales analytics.

## Features Summary

- Role-based access control for buyers and sellers
- Product management with stock updates
- Transaction tracking for purchases
- Revenue and sales reporting dashboards
- Interactive visual charts using JFreeChart

## License

This project is currently provided as a starter application and can be customized or extended according to your project requirements.

## Notes

This README is designed as a polished project overview for a second-hand marketplace application. You can later customize it with your actual repository name, screenshots, demo information, and deployment instructions.
