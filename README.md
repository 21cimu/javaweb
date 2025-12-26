# Car Rental System

A full-stack car rental management system built with Java Servlets and Vue.js, featuring user management, vehicle management, order processing, and payment integration.

## 🚗 Project Overview

This is a comprehensive car rental system that includes:
- User registration, authentication, and profile management
- Vehicle browsing, searching, and booking
- Order management and tracking
- Payment integration (Alipay sandbox)
- Admin dashboard for system management
- User verification and KYC (Know Your Customer) features
- Real-time order status updates

## 🛠️ Technology Stack

### Backend
- **Java 17**
- **Jakarta Servlet API 6.0** (Tomcat 11)
- **MySQL 8.0+**
- **Maven** - Build tool
- **HikariCP** - Connection pooling
- **JWT** - Authentication
- **Gson** - JSON processing
- **SLF4J** - Logging

### Frontend
- **Vue 3** - Progressive JavaScript framework
- **Vite** - Build tool
- **Element Plus** - UI component library
- **Vue Router** - Routing
- **Pinia** - State management
- **Axios** - HTTP client
- **ECharts** - Data visualization
- **Day.js** - Date manipulation

## 📋 Prerequisites

Before you begin, ensure you have the following installed:
- **Java Development Kit (JDK) 17** or higher
- **Apache Maven 3.6+**
- **Node.js 16+** and **npm**
- **MySQL 8.0+**
- **Apache Tomcat 11** (for deployment)

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/21cimu/javaweb.git
cd javaweb
```

### 2. Database Setup

1. Start your MySQL server

2. Create the database and tables:
```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

Or manually create the database:
```sql
CREATE DATABASE IF NOT EXISTS car_rental DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Then run the schema file in the `backend/src/main/resources/schema.sql`

3. Update database credentials in `backend/src/main/resources/application.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/car_rental?useSSL=false&serverTimezone=Asia/Shanghai
db.username=root
db.password=your_password
```

### 3. Backend Setup

Navigate to the backend directory:
```bash
cd backend
```

Build the project:
```bash
mvn clean install
```

Deploy the generated WAR file to Tomcat:
```bash
# The WAR file will be in target/car-rental.war
# Copy it to your Tomcat's webapps directory
cp target/car-rental.war $TOMCAT_HOME/webapps/
```

Or run with Maven Tomcat plugin (if configured):
```bash
mvn tomcat7:run
```

The backend API will be available at: `http://localhost:8080/car-rental/`

### 4. Frontend Setup

Navigate to the frontend directory:
```bash
cd frontend
```

Install dependencies:
```bash
npm install
```

Start the development server:
```bash
npm run dev
```

The frontend will be available at: `http://localhost:3000`

Build for production:
```bash
npm run build
```

The production files will be in the `dist/` directory.

## 📁 Project Structure

```
javaweb/
├── backend/                    # Java backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/carrental/
│   │   │   │       ├── dao/           # Data Access Objects
│   │   │   │       ├── filter/        # Servlet filters (CORS, Auth)
│   │   │   │       ├── model/         # Entity classes
│   │   │   │       ├── servlet/       # API endpoints
│   │   │   │       └── util/          # Utility classes
│   │   │   ├── resources/
│   │   │   │   ├── application.properties  # Configuration
│   │   │   │   └── schema.sql             # Database schema
│   │   │   └── webapp/
│   │   │       └── WEB-INF/
│   │   │           └── web.xml        # Servlet configuration
│   │   └── test/                      # Unit tests
│   └── pom.xml                        # Maven configuration
│
└── frontend/                   # Vue.js frontend
    ├── public/                 # Static assets
    ├── src/
    │   ├── api/               # API service layer
    │   ├── assets/            # Images, styles
    │   ├── components/        # Reusable components
    │   ├── router/            # Vue Router configuration
    │   ├── stores/            # Pinia stores
    │   ├── views/             # Page components
    │   ├── App.vue            # Root component
    │   └── main.js            # Application entry point
    ├── index.html             # HTML template
    ├── package.json           # npm dependencies
    └── vite.config.js         # Vite configuration
```

## 🔑 Key Features

### User Features
- User registration and login with JWT authentication
- Profile management and KYC verification
- Vehicle browsing with search and filters
- Real-time vehicle availability
- Online booking and order management
- Multiple payment methods (Alipay integration)
- Order history and tracking
- User balance and points system

### Admin Features
- User management (verification, status control)
- Vehicle inventory management
- Order management and monitoring
- System operation logs
- Financial flow tracking
- After-sales service management
- Dashboard with statistics and charts

## 🔧 Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.properties`:

**Database Settings:**
```properties
db.url=jdbc:mysql://localhost:3306/car_rental
db.username=root
db.password=123456
```

**JWT Settings:**
```properties
jwt.secret=your-secret-key
jwt.expiration=86400000
```

**Alipay Settings (Sandbox):**
```properties
alipay.gatewayUrl=https://openapi-sandbox.dl.alipaydev.com/gateway.do
alipay.appId=your-app-id
alipay.privateKey=your-private-key
alipay.alipayPublicKey=alipay-public-key
```

### Frontend Configuration

The frontend API base URL can be configured in the Axios service files located in `frontend/src/api/`.

Default backend URL: `http://localhost:8080/car-rental/api/`

## 🧪 Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
The project uses Vite for building. For testing:
```bash
cd frontend
npm run build
npm run preview
```

## 📦 Deployment

### Backend Deployment
1. Build the WAR file: `mvn clean package`
2. Deploy to Tomcat 11 or higher
3. Ensure MySQL is running and accessible
4. Update configuration for production environment

### Frontend Deployment
1. Build for production: `npm run build`
2. Deploy the `dist/` directory to a web server (Nginx, Apache, etc.)
3. Configure reverse proxy to backend API
4. Update API endpoints for production

## 🔐 Security Notes

- Change default database credentials in production
- Use strong JWT secret keys
- For Alipay integration, use production credentials (not sandbox)
- Implement HTTPS in production
- Review and update CORS settings in production
- Never commit sensitive keys to version control

## 📄 API Endpoints

The backend provides RESTful APIs under `/api/`:

- `/api/auth/*` - Authentication endpoints
- `/api/users/*` - User management
- `/api/vehicles/*` - Vehicle operations
- `/api/orders/*` - Order management
- `/api/pay/*` - Payment processing
- `/api/admin/*` - Admin operations

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is available for educational and personal use.

## 👥 Authors

- Project maintained by [21cimu](https://github.com/21cimu)

## 📧 Contact

For questions or support, please open an issue in the GitHub repository.

---

**Note**: This is a demonstration project. For production use, ensure proper security measures, testing, and compliance with local regulations regarding car rental services and payment processing.
