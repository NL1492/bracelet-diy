# 手串 DIY 定制平台

面向文玩手串爱好者的在线 DIY 定制平台：用户在浏览器中拖拽搭配珠子设计专属手串，保存方案、加入购物车并完成下单；商家通过管理端完成经营总览、订单、素材、灵感与用户的日常运营。

## 功能特性

### C 端（`/`）

- **手串设计器** — 拖拽/替换珠子、尺寸适配、实时预览与价格计算
- **设计方案** — 保存 / 重命名 / 删除，可分享给他人查看
- **购物车** — 方案加购、勾选结算
- **订单中心** — 下单 → 支付 → 发货 → 收货 → 完成/退款 的完整状态机，环形缩略图回显
- **收货地址簿** — 预先填写常用地址，结算时自动填充默认地址，支持多地址切换 / 新增 / 编辑 / 删除 / 设默认
- **灵感社区** — 灵感发布、浏览与审核展示

### 管理端（`/admin.html`）

- **经营总览** — 用户 / 订单 / GMV / 待办的真实统计，7 日趋势与热销排行
- **订单管理** — 状态筛选、发货、退款审核
- **素材库** — 珠子素材维护
- **灵感管理** — 审核与推荐
- **用户中心** — 用户查询、新增、重置密码

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3 · Vite · Vue Router（C 端单页 + 管理端子应用） |
| 后端 | Spring Boot 3.2 · MyBatis-Plus · MySQL 8 |
| 认证 | JWT（jjwt）+ BCrypt 密码加密 |
| 接口文档 | springdoc-openapi（Swagger UI） |

## 项目结构

```
├── index.html / admin.html    # 双入口：C 端商城 + 管理端
├── src/
│   ├── App.vue                # C 端主应用
│   ├── admin/                 # 管理端外壳与视图
│   ├── api/                   # 接口封装
│   └── materials/             # 珠子素材目录
├── server/                    # Spring Boot 后端
│   └── src/main/java/com/bracelet/
│       ├── controller/ service/ mapper/
│       ├── entity/ dto/ vo/ config/ util/
├── database/                  # schema.sql + seed.sql + add_*.sql 增量脚本
└── image/                     # 静态素材（Vite publicDir，/images/ 访问）
```

## 快速开始

### 1. 数据库

安装 MySQL 8 后，依次执行 `database/` 下的脚本：

```
schema.sql → seed.sql → 其余 add_*.sql 增量脚本
```

### 2. 后端（端口 8080）

按需修改 [server/src/main/resources/application.yml](server/src/main/resources/application.yml) 中的数据库连接，然后：

```bash
cd server
mvn spring-boot:run
```

### 3. 前端（端口 5173）

```bash
npm install
npm run dev
```

开发模式下 `/api` 自动代理到 `http://localhost:8080`。

### 4. 访问

- C 端商城：<http://localhost:5173>
- 管理端：<http://localhost:5173/admin.html>
- 接口文档：<http://localhost:8080/swagger-ui.html>

默认管理员账号：`admin / admin123`（仅用于本地演示）。
