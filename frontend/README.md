## Introduction

A **blog website** developed based on the **Zaaac-Vue** frontend-backend separation platform.

👍👍 Thanks to Zaaac ❤️❤️ 

**Zaaac-Vue** is a rapid development platform for Java EE enterprises, leveraging a classic technology stack (Spring Boot, Spring Security, MyBatis, Jwt, Vue). It comes with built-in modules such as department management, roles and users, menu and button permissions, data permissions, system parameters, log management, code generation, and online scheduling configuration. It supports clustering, multiple data sources, and distributed transactions.

## Quick Overview

Blog is developed based on the Zaaac-Vue platform.  
Zaaac-Vue is a frontend-backend separation framework for rapid backend development based on Spring Boot and Vue.

## System Requirements

- JDK >= 1.8
- MySQL >= 5.7
- Maven >= 3.0
- Node >= 12

## Technology Stack

1. **System Environment**
   - Java EE 8
   - Servlet 3.0
   - Apache Maven 3

2. **Main Framework**
   - Spring Boot 2.2.x
   - Spring Framework 5.2.x
   - Spring Security 5.2.x

3. **Persistence Layer**
   - Apache MyBatis 3.5.x
   - Hibernate Validation 6.0.x
   - Alibaba Druid 1.2.x

4. **View Layer**
   - Vue 2.6.x
   - Axios 0.21.x
   - Element 2.15.x

## Built-In Features

1. **User Management**: Configure system users, the primary operators of the system.
2. **Department Management**: Configure organizational structures (companies, departments, teams) displayed in a tree structure, supporting data permissions.
3. **Position Management**: Configure the roles users hold in the system.
4. **Menu Management**: Configure system menus, operation permissions, button permission identifiers, etc.
5. **Role Management**: Assign role menu permissions and set role-based data scope permissions.
6. **Dictionary Management**: Maintain commonly used fixed data in the system.
7. **Parameter Management**: Dynamically configure common parameters in the system.
8. **Notification Announcements**: Publish and maintain system notification announcements.
9. **Operation Logs**: Record and query normal and abnormal system operation logs.
10. **Login Logs**: Record and query system login logs, including login exceptions.
11. **Online Users**: Monitor the status of active users in the system.
12. **Scheduled Tasks**: Add, modify, delete, and query task scheduling and execution result logs online.
13. **Code Generation**: Generate frontend and backend code (Java, HTML, XML, SQL) for CRUD operations.
14. **System API**: Automatically generate API documentation based on business code.
15. **Service Monitoring**: Monitor CPU, memory, disk, heap, and other system-related information.
16. **Cache Monitoring**: Query, delete, and clear system cache operations.
17. **Online Builder**: Drag and drop form elements to generate corresponding HTML code.
18. **Connection Pool Monitoring**: Monitor the current system's database connection pool status and analyze SQL to identify performance bottlenecks.

## New Features

1. **Content Management**: Manage all content on the blog website.
2. **Article Management**: Manage blog article content.
3. **Essay Management**: Manage blog essays.
4. **Category Management**: Manage blog categories.
5. **Tag Management**: Manage blog tags.
6. **Message Management**: Manage messages on the blog website.
7. **Comment Management**: Manage blog comments.
8. **File Management**: Manage all uploaded files on the blog website.

## Run the Project

```bash
# Navigate to the project directory
cd frontend

# Install dependencies
npm install

# Start the service
npm run dev
```

Access the site at [http://localhost:80](http://localhost:80)

## Deployment

```bash
# Build for the testing environment
npm run build:stage

# Build for the production environment
npm run build:prod
```