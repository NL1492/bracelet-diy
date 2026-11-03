# 手串DIY定制 — 后端服务

SpringBoot 3.2 + MyBatis-Plus 3.5 + MySQL 8.0

---

## 1. 项目结构

```
server/
├── pom.xml
└── src/main/
    ├── java/com/bracelet/
    │   ├── BraceletApplication.java        # 启动类
    │   ├── config/
    │   │   └── CorsConfig.java             # 全局跨域配置
    │   ├── entity/
    │   │   ├── BeadInfo.java               # 珠子信息实体
    │   │   ├── BraceletDesign.java         # 设计方案实体
    │   │   └── BraceletCart.java           # 购物车实体
    │   ├── dto/
    │   │   ├── BeadItemDTO.java            # 珠子入参
    │   │   ├── SaveDesignDTO.java          # 保存设计入参
    │   │   └── AddCartDTO.java             # 加购入参
    │   ├── vo/
    │   │   └── OrderCountVO.java           # 订单统计返回
    │   ├── mapper/
    │   │   ├── BeadInfoMapper.java
    │   │   ├── BraceletDesignMapper.java
    │   │   └── BraceletCartMapper.java
    │   ├── service/
    │   │   ├── BeadInfoService.java
    │   │   ├── BraceletDesignService.java
    │   │   ├── BraceletCartService.java
    │   │   └── impl/
    │   │       ├── BeadInfoServiceImpl.java
    │   │       ├── BraceletDesignServiceImpl.java  # ★核心统计逻辑
    │   │       └── BraceletCartServiceImpl.java
    │   ├── controller/
    │   │   └── BraceletController.java     # ★3个API接口
    │   └── util/
    │       └── Result.java                 # 统一返回 {code, msg, data}
    └── resources/
        └── application.yml                 # 数据库 & MyBatis-Plus 配置
```

---

## 2. 启动步骤

```bash
# 1. 先创建数据库并导入种子数据
mysql -u root -p < ../database/schema.sql
mysql -u root -p < ../database/seed.sql

# 2. 修改 application.yml 中的数据库密码

# 3. 启动后端（在 server/ 目录下）
mvn spring-boot:run
```

---

## 3. 接口文档

统一返回格式: `{ "code": 200, "msg": "success", "data": {...} }`

### 接口1 — 实时统计

```
POST /api/bracelet/getCountData
入参: [{ "beadId": 1, "size": 10 }, ...]
返回: { code, msg, data: { beadCount, maxBeads, totalCircumference, wristMin, wristMax, totalPrice } }
```

### 接口2 — 保存设计（新建 / 覆盖）

```
POST /api/bracelet/saveDesign
入参: { "designId": null, "designName": "方案名", "braceleted": true, "beads": [...] }
返回: { code, msg, data: 方案ID }
```

### 接口3 — 加入购物车

```
POST /api/bracelet/addCart
入参: { "designId": 1, "beads": [...] }
返回: { code, msg, data: 购物车ID }
```

### 接口4 — 查询方案列表

```
GET /api/bracelet/designs
返回: { code, msg, data: [{ id, designName, beadCount, totalPrice, beadListJson, createTime }, ...] }
```

### 接口5 — 查询方案详情

```
GET /api/bracelet/designs/{id}
返回: { code, msg, data: { id, designName, beadCount, totalPrice, beadListJson, createTime } }
```

---

## 4. 业务规则

| 规则 | 值 |
|------|-----|
| 珠子数量上限 | 25 颗 |
| 总周长计算 | Σ (每颗珠子 size_mm / 10) cm |
| 最小手围 | 总周长 - 5.0 cm |
| 最大手围 | 总周长 - 4.3 cm |
| 总价 | Σ 每颗珠子 price |

---

## 5. 前端联动时机

前端应在以下时机调用 `POST /api/bracelet/getCountData` 刷新订单卡片:

1. ✅ 新增珠子后
2. ✅ 删除珠子后
3. ✅ 收拢成串动画完成后
4. ✅ 修改珠子尺寸后

两个按钮的绑定:
- 「加入到购物车」→ `POST /api/bracelet/addCart`
- 「保存设计记录」→ `POST /api/bracelet/saveDesign`

前端 API 代理配置 (vite.config.js):
```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

前端 API 调用示例:
```js
import { braceletApi } from './api/index.js'

// 刷新订单卡
const res = await braceletApi.getCountData(beads)
// res.data → { beadCount, totalCircumference, wristMin, wristMax, totalPrice }

// 保存设计
await braceletApi.saveDesign({ designName: '方案名', beads })

// 加入购物车
await braceletApi.addCart({ beads })
```
