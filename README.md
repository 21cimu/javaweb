# 租车平台 (Car Rental System)

基于 Java Servlet + Vue.js 的全栈租车管理系统

## 技术栈

### 后端
- Java 17
- Jakarta Servlet 6.0 (Tomcat 11)
- MySQL 8.0
- HikariCP 连接池
- Gson JSON 处理

### 前端
- Vue 3
- Vue Router
- Axios
- Element Plus UI 组件库
- Vite 构建工具

## 项目结构

```
├── backend/                 # 后端项目
│   ├── src/main/java/      # Java 源代码
│   │   └── com/carrental/
│   │       ├── dao/        # 数据访问层
│   │       ├── filter/     # 过滤器（CORS等）
│   │       ├── model/      # 实体类
│   │       ├── servlet/    # Servlet 控制器
│   │       └── util/       # 工具类
│   ├── src/main/resources/ # 配置文件
│   │   ├── db.properties   # 数据库配置
│   │   └── schema.sql      # 数据库初始化脚本
│   └── pom.xml             # Maven 配置
│
├── frontend/               # 前端项目
│   ├── src/
│   │   ├── api/           # API 接口
│   │   ├── router/        # 路由配置
│   │   ├── views/         # 页面组件
│   │   │   ├── admin/     # 管理后台
│   │   │   └── ...        # 用户前台
│   │   ├── App.vue        # 主组件
│   │   └── main.js        # 入口文件
│   ├── package.json
│   └── vite.config.js
│
└── README.md
```

## 功能模块

### 用户前台
- 用户注册/登录
- 车辆浏览与搜索
- 车辆详情与预订
- 订单管理
- 个人中心

### 管理后台
- 仪表盘数据概览
- 车辆管理（CRUD）
- 订单管理（处理、取还车）

## 快速开始

### 1. 数据库配置

```sql
-- 创建数据库并导入 schema
mysql -u root -p < backend/src/main/resources/schema.sql
```

修改 `backend/src/main/resources/db.properties` 配置数据库连接：

```properties
db.url=jdbc:mysql://localhost:3306/car_rental
db.username=root
db.password=your_password
```

### 2. 后端部署

```bash
cd backend
mvn package

# 将生成的 car-rental.war 部署到 Tomcat 11
cp target/car-rental.war $TOMCAT_HOME/webapps/
```

### 3. 前端开发

```bash
cd frontend
npm install
npm run dev
```

### 4. 前端构建

```bash
npm run build
# 构建产物在 dist/ 目录
```

## API 接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/logout` | POST | 用户登出 |
| `/api/auth/check` | GET | 检查登录状态 |
| `/api/users` | GET | 获取用户列表 |
| `/api/users/{id}` | GET/PUT/DELETE | 用户操作 |
| `/api/vehicles` | GET/POST | 车辆列表/新增 |
| `/api/vehicles/{id}` | GET/PUT/DELETE | 车辆操作 |
| `/api/orders` | GET/POST | 订单列表/新增 |
| `/api/orders/{id}` | GET/PUT/DELETE | 订单操作 |
| `/api/orders/{id}/pay` | PUT | 支付订单 |
| `/api/orders/{id}/pickup` | PUT | 确认取车 |
| `/api/orders/{id}/return` | PUT | 确认还车 |
| `/api/orders/{id}/cancel` | PUT | 取消订单 |

## 开发说明

- 后端使用 Jakarta Servlet API，兼容 Tomcat 11
- 前端开发时可通过 Vite 代理访问后端 API
- 生产部署时需配置跨域或将前端构建产物部署到同一服务器
