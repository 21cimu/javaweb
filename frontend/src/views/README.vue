<template>
  <div class="readme-page">
    <el-card class="readme-card">
      <template #header>
        <div class="card-header-title">
          <h1>📖 项目说明文档</h1>
        </div>
      </template>
      
      <div class="readme-content">
        <section class="section">
          <h2>🚗 畅行租车系统</h2>
          <p>欢迎使用畅行租车平台！这是一个现代化的汽车租赁管理系统，为用户提供便捷的租车服务。</p>
        </section>

        <section class="section">
          <h2>✨ 主要功能</h2>
          <ul>
            <li><strong>车辆浏览</strong>：浏览各类车型，支持按分类、价格等条件筛选</li>
            <li><strong>在线预订</strong>：快速预订心仪车辆，支持自定义取还车时间</li>
            <li><strong>订单管理</strong>：查看订单状态、支付信息、取还车记录</li>
            <li><strong>优惠活动</strong>：查看和使用优惠券，参与各类促销活动</li>
            <li><strong>用户中心</strong>：管理个人信息、实名认证、查看租车记录</li>
            <li><strong>管理后台</strong>：完善的后台管理系统，支持车辆、订单、用户等全方位管理</li>
          </ul>
        </section>

        <section class="section">
          <h2>🏗️ 技术栈</h2>
          <div class="tech-stack">
            <div class="tech-group">
              <h3>前端技术</h3>
              <ul>
                <li>Vue 3 - 渐进式 JavaScript 框架</li>
                <li>Vue Router - 官方路由管理器</li>
                <li>Pinia - 状态管理库</li>
                <li>Element Plus - 组件库</li>
                <li>Axios - HTTP 客户端</li>
                <li>Vite - 构建工具</li>
              </ul>
            </div>
            <div class="tech-group">
              <h3>后端技术</h3>
              <ul>
                <li>Java - 编程语言</li>
                <li>Spring Boot - 应用框架</li>
                <li>MyBatis - 持久层框架</li>
                <li>MySQL - 关系型数据库</li>
                <li>Maven - 项目管理工具</li>
              </ul>
            </div>
          </div>
        </section>

        <section class="section">
          <h2>📁 项目结构</h2>
          <pre class="code-block">javaweb/
├── frontend/          # 前端项目
│   ├── src/
│   │   ├── api/       # API 接口
│   │   ├── assets/    # 静态资源
│   │   ├── components/# 公共组件
│   │   ├── router/    # 路由配置
│   │   ├── stores/    # 状态管理
│   │   └── views/     # 页面视图
│   ├── package.json
│   └── vite.config.js
└── backend/           # 后端项目
    ├── src/
    │   └── main/
    │       ├── java/  # Java 源代码
    │       └── resources/ # 配置文件
    └── pom.xml</pre>
        </section>

        <section class="section">
          <h2>🚀 快速开始</h2>
          <div class="code-section">
            <h3>前端开发</h3>
            <pre class="code-block"># 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build</pre>
          </div>
          
          <div class="code-section">
            <h3>后端开发</h3>
            <pre class="code-block"># 进入后端目录
cd backend

# 使用 Maven 构建
mvn clean install

# 运行应用
mvn spring-boot:run</pre>
          </div>
        </section>

        <section class="section">
          <h2>👥 用户角色</h2>
          <div class="roles-grid">
            <div class="role-card">
              <h3>🙋 普通用户</h3>
              <p>可以浏览车辆、预订租车、管理订单、参与优惠活动等</p>
            </div>
            <div class="role-card">
              <h3>🛠️ 管理员</h3>
              <p>拥有完整的后台管理权限，可以管理车辆、订单、用户等所有资源</p>
            </div>
          </div>
        </section>

        <section class="section">
          <h2>📞 联系我们</h2>
          <p>如有任何问题或建议，请随时联系我们：</p>
          <ul>
            <li>📧 邮箱：support@changxing-rental.com</li>
            <li>📱 电话：400-123-4567</li>
            <li>💬 在线客服：工作日 9:00-18:00</li>
          </ul>
        </section>

        <section class="section">
          <h2>📄 许可证</h2>
          <p>本项目仅供学习和研究使用。</p>
        </section>

        <div class="footer-info">
          <p>© 2024 畅行租车 - 让出行更简单</p>
          <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// This is a simple README page component
</script>

<style scoped>
.readme-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.readme-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.card-header-title h1 {
  margin: 0;
  color: #303133;
  font-size: 28px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.readme-content {
  line-height: 1.8;
  color: #606266;
}

.section {
  margin-bottom: 40px;
}

.section h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409EFF;
}

.section h3 {
  font-size: 18px;
  color: #409EFF;
  margin-bottom: 12px;
  margin-top: 20px;
}

.section p {
  margin-bottom: 12px;
  font-size: 15px;
}

.section ul {
  list-style: none;
  padding-left: 0;
}

.section ul li {
  padding: 8px 0;
  padding-left: 24px;
  position: relative;
  font-size: 15px;
}

.section ul li:before {
  content: "▸";
  position: absolute;
  left: 0;
  color: #409EFF;
  font-weight: bold;
}

.tech-stack {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30px;
  margin-top: 20px;
}

.tech-group {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
  border-left: 4px solid #409EFF;
}

.tech-group h3 {
  margin-top: 0;
  color: #303133;
  font-size: 16px;
}

.tech-group ul {
  margin-top: 12px;
}

.code-block {
  background: #282c34;
  color: #abb2bf;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.6;
  margin-top: 12px;
}

.code-section {
  margin-bottom: 24px;
}

.roles-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 20px;
}

.role-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.role-card h3 {
  margin: 0 0 12px 0;
  color: #fff;
  font-size: 20px;
}

.role-card p {
  margin: 0;
  opacity: 0.95;
  font-size: 14px;
}

.footer-info {
  margin-top: 50px;
  padding-top: 30px;
  border-top: 1px solid #DCDFE6;
  text-align: center;
}

.footer-info p {
  margin-bottom: 20px;
  color: #909399;
  font-size: 14px;
}

@media screen and (max-width: 768px) {
  .tech-stack,
  .roles-grid {
    grid-template-columns: 1fr;
  }
  
  .readme-page {
    padding: 10px;
  }
  
  .card-header-title h1 {
    font-size: 22px;
  }
  
  .section h2 {
    font-size: 20px;
  }
}
</style>
