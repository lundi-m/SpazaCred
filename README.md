# SpazaCred

**Small Business Credit & Payment Management Platform**

SpazaCred is a backend system designed to help small businesses, particularly spaza shops, manage customer credit
accounts, credit purchases, payments, and outstanding balances.

The project is being built as a production-style **Spring Boot REST API**, with a focus on clean architecture, proper 
business rules, security, persistence, testing, and deployment.

## Project Goal

The goal of SpazaCred is to provide a structured way for a small business to manage the complete lifecycle of customer
credit.

Instead of relying on notebooks, spreadsheets, or manual calculations, the system will maintain a central record of:

* Customers
* Credit accounts
* Credit purchases
* Payments
* Outstanding balances
* Transaction history

## Planned Features

### Customer Management

* Create and manage customers
* View customer details
* Activate or deactivate customers
* View customer credit history

### Credit Account Management

* Create credit accounts
* Set credit limits
* Track outstanding balances
* Monitor account status

### Credit Purchases

* Record purchases made on credit
* Associate purchases with customer accounts
* Track purchase amounts and dates
* Apply relevant business rules such as credit limits

### Payment Management

* Record customer payments
* Track payment methods
* Update outstanding balances
* Maintain payment history

### Authentication & Authorization

* User authentication
* Role-based access control
* Secure API endpoints

### Reporting & History

* View customer transaction history
* Track credit and payment activity
* Provide account-level financial information

## Planned Technology Stack

* **Java 21**
* **Spring Boot**
* **Spring Data JPA / Hibernate**
* **Spring Security**
* **JWT**
* **PostgreSQL**
* **Flyway**
* **Maven**
* **Docker**
* **JUnit / Mockito**
* **GitHub Actions**

## Project Status

**Early Development**

The project is currently in the initial setup and domain-design stage. Features will be implemented incrementally as 
the system evolves.

## Architecture

The application will follow a layered, domain-oriented structure designed to keep business logic separate from
controllers, persistence, security, and infrastructure concerns.

The architecture will evolve throughout development as the requirements and domain become clearer.